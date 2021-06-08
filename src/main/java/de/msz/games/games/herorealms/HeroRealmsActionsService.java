package de.msz.games.games.herorealms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import de.msz.games.base.NotificationService;
import de.msz.games.base.NotificationService.NotificationType;
import de.msz.games.base.UserService;
import de.msz.games.games.Deck;
import de.msz.games.games.GameService;
import de.msz.games.games.herorealms.HeroRealmsTable.PlayerArea;
import de.msz.games.games.player.Player;

@Service
public class HeroRealmsActionsService {
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private NotificationService notificationService;
	
	@Autowired
	private GameService gameService;
	
	@Autowired
	private HeroRealmsService heroRealmsService;
	
	@Autowired
	private HeroRealmsTableService heroRealmsTableService;
	
	@Autowired
	private MessageSource messageSource;
	
	void playCard(HeroRealmsTable table, String cardId) {
		
		heroRealmsTableService.checkIsPlayerActive(table);
		
		Player activePlayer = table.getActivePlayer();
		PlayerArea playerArea = table.getPlayerAreas().get(activePlayer.getId());
		List<HeroRealmsCard> hand = playerArea.getHand();
		HeroRealmsCard card = hand.stream()
				.filter(handCard -> handCard.getId().equals(cardId))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException("unknown card '" + cardId + "'"));
		
		processCardAbilities(table, playerArea, card);
		
		hand.remove(card);
		
		switch (card.getType()) {
			case CHAMPION:
			case GUARD:
				playerArea.getChampions().add(card);
				card.setReady(false);
				break;
			default:
				playerArea.getPlayedCards().add(card);
				break;
		}
	}

	void playChampion(HeroRealmsTable table, String championId) {
		
		heroRealmsTableService.checkIsPlayerActive(table);
		
		Player activePlayer = table.getActivePlayer();
		PlayerArea playerArea = table.getPlayerAreas().get(activePlayer.getId());
		
		HeroRealmsCard champion = playerArea.getChampions().stream()
				.filter(championCard -> championCard.getId().equals(championId))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException("unknown champion '" + championId + "'"));
		
		champion.setReady(false);
		processCardAbilities(table, playerArea, champion);
	}

	void makeDecision(HeroRealmsTable table, String decisionId, String optionId) {
		
		heroRealmsTableService.checkIsPlayerActive(table);
		
		Player activePlayer = table.getActivePlayer();
		PlayerArea playerArea = table.getPlayerAreas().get(activePlayer.getId());
		
		HeroRealmsDecision madeDecision = playerArea.getDecisions().stream()
				.filter(decision -> decision.getId().equals(decisionId))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException("unknown decision '" + decisionId + "'"));
		
		switch (madeDecision.getType()) {
			case SELECT_ONE:
				makeDecisionSelect(table, playerArea, madeDecision, optionId);
				playerArea.getDecisions().remove(madeDecision);
				break;
			case OPTIONAL:
				if (makeDecisionOptional(table, playerArea, madeDecision)) {
					playerArea.getDecisions().remove(madeDecision);
				}
				break;
			default:
				break;
		}
	}
	
	void makeDecisionSelect(HeroRealmsTable table, PlayerArea area, HeroRealmsDecision decision, String optionId) {
		
		HeroRealmsDecisionOption selectedOption = decision.getOptions().stream()
				.filter(option -> option.getId().equals(optionId))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException(
						"unknown option '" + optionId + "' for decision '" + decision.getId() + "'"));
		
		HeroRealmsCard card4Decision = getCard4Decision(area, decision);
		
		processCardAbility(table, area, card4Decision, selectedOption.getAbilityType(), selectedOption.getValue());
	}
	
	private boolean makeDecisionOptional(HeroRealmsTable table, PlayerArea area, HeroRealmsDecision decision) {
		
		HeroRealmsDecisionOption selectedOption = decision.getOptions().get(0);
		HeroRealmsCard card4Decision = getCard4Decision(area, decision);
		
		HeroRealmsAbilityType abilityType = selectedOption.getAbilityType();
		switch (abilityType) {
			case DRAW_DISCARD_CARD:
				area.getHand().add(draw(area));
				area.setActionMode(HeroRealmsSpecialActionMode.DISCARD);
				break;
			case OPPONENT_DISCARD_CARD:
				area.setActionMode(HeroRealmsSpecialActionMode.OPPONENT_DISCARD_CARD);
				break;
			case PREPARE_CHAMPION:
				if (!isPreparableChampionAvailable(area)) {
					notificationService.addNotification(NotificationType.WARNING, messageSource.getMessage(
							"hero_realms.info.no_champion_preparable", null, Locale.getDefault()));
					return false;
				}
				area.setActionMode(HeroRealmsSpecialActionMode.PREPARE_CHAMPION);
				break;
			case STUN_TARGET_CHAMPION:
				if (!isTargetChampionAvailable(table, area)) {
					notificationService.addNotification(NotificationType.WARNING, messageSource.getMessage(
							"hero_realms.info.no_target_champion", null, Locale.getDefault()));
					return false;
				}
				area.setActionMode(HeroRealmsSpecialActionMode.STUN_TARGET_CHAMPION);
				break;
			case NEXT_ACTION_TOP_DECK:
			case NEXT_ACQUIRE_TOP_DECK:
			case NEXT_ACQUIRE_HAND:
			case NEXT_ACTION_COSTS_LESS:
			case NEXT_CHAMPION_COSTS_LESS:
				setBuyMode(area, decision, abilityType);
				return false;
			case PUT_CARD_DISCARD_PILE_TOP_DECK:
				if (area.getDiscardPile().getCards().isEmpty()) {
					notificationService.addNotification(NotificationType.WARNING, messageSource.getMessage(
							"hero_realms.info.discard_pile_empty", null, Locale.getDefault()));
					return false;
				}
				area.setActionMode(HeroRealmsSpecialActionMode.PUT_CARD_DISCARD_PILE_TOP_DECK);
				break;
			case PUT_CHAMPION_DISCARD_PILE_TOP_DECK:
				if (!isChampionInDiscardPile(area)) {
					notificationService.addNotification(NotificationType.WARNING, messageSource.getMessage(
							"hero_realms.info.no_champion_discard_pile", null, Locale.getDefault()));
					return false;
				}
				area.setActionMode(HeroRealmsSpecialActionMode.PUT_CHAMPION_DISCARD_PILE_TOP_DECK);
				break;
			case SACRIFICE_HAND_OR_DISCARD_PILE:
				if (isHandAndDiscardPileEmpty(area)) {
					notificationService.addNotification(NotificationType.WARNING, messageSource.getMessage(
							"hero_realms.info.hand_and_discard_pile_empty", null, Locale.getDefault()));
					return false;
				}
				area.setActionMode(HeroRealmsSpecialActionMode.SACRIFICE);
				break;
			case SACRIFICE_HAND_OR_DISCARD_PILE_COMBAT:
				if (isHandAndDiscardPileEmpty(area)) {
					notificationService.addNotification(NotificationType.WARNING, messageSource.getMessage(
							"hero_realms.info.hand_and_discard_pile_empty", null, Locale.getDefault()));
					return false;
				}
				area.setActionMode(HeroRealmsSpecialActionMode.SACRIFICE);
				processCardAbility(table, area, card4Decision, HeroRealmsAbilityType.COMBAT, selectedOption.getValue());
				break;
			default:
				notificationService.addNotification(NotificationType.ERROR, "ability " + abilityType + " not implemented");
		}
		
		return true;
	}
	
	private static HeroRealmsCard getCard4Decision(PlayerArea area, HeroRealmsDecision decision) {
		
		return CollectionUtils.union(area.getPlayedCards(), area.getChampions()).stream()
				.filter(card -> card.getId().equals(decision.getCardId()))
				.findAny()
				.orElse(null);
	}
	
	private static void setBuyMode(PlayerArea area, HeroRealmsDecision decision,
			HeroRealmsAbilityType abilityType) {
		
		if (area.getBuyMode() != null) {
			area.getDecisions().stream()
					.filter(buyDecision -> buyDecision.isActive())
					.forEach(buyDecision -> buyDecision.setActive(false));
		}
		
		switch (abilityType) {
			case NEXT_ACTION_TOP_DECK:
				area.setBuyMode(HeroRealmsBuyMode.NEXT_ACTION_TOP_DECK);
				break;
			case NEXT_ACQUIRE_TOP_DECK:
				area.setBuyMode(HeroRealmsBuyMode.NEXT_ACQUIRE_TOP_DECK);
				break;
			case NEXT_ACQUIRE_HAND:
				area.setBuyMode(HeroRealmsBuyMode.NEXT_ACQUIRE_HAND);
				break;
			case NEXT_ACTION_COSTS_LESS:
				area.setBuyMode(HeroRealmsBuyMode.NEXT_ACTION_COSTS_LESS);
				break;
			case NEXT_CHAMPION_COSTS_LESS:
				area.setBuyMode(HeroRealmsBuyMode.NEXT_CHAMPION_COSTS_LESS);
				break;
			default:
				throw new IllegalArgumentException("unknown ability '" + abilityType + "' for buying card from market");
		}
		
		decision.setActive(true);
	}
	
	private static boolean isPreparableChampionAvailable(PlayerArea area) {
		
		return area.getChampions().stream()
				.filter(champion -> !champion.isReady())
				.findAny()
				.isPresent();
	}
	
	private static boolean isTargetChampionAvailable(HeroRealmsTable table, PlayerArea activePlayerArea) {
		
		return table.getPlayerAreas().values().stream()
				.filter(area -> !area.getPlayerId().equals(activePlayerArea.getPlayerId()))
				.flatMap(area -> area.getChampions().stream())
				.findAny()
				.isPresent();
	}
	
	private static boolean isChampionInDiscardPile(PlayerArea area) {
		
		return area.getDiscardPile().getCards().stream()
				.filter(card -> card.getType() == HeroRealmsCardType.CHAMPION
						|| card.getType() == HeroRealmsCardType.GUARD)
				.findAny()
				.isPresent();
	}
	
	private static boolean isHandAndDiscardPileEmpty(PlayerArea area) {
		
		return area.getHand().isEmpty() && area.getDiscardPile().getCards().isEmpty();
	}
	
	void sacrifice(HeroRealmsTable table, String cardId, boolean withAbility) {
		
		heroRealmsTableService.checkIsPlayerActive(table);
		
		Player activePlayer = table.getActivePlayer();
		PlayerArea playerArea = table.getPlayerAreas().get(activePlayer.getId());
		List<HeroRealmsCard> playedCards = playerArea.getPlayedCards();
		List<HeroRealmsCard> handCards = playerArea.getHand();
		Deck<HeroRealmsCard> discardPile = playerArea.getDiscardPile();
		
		Optional<HeroRealmsCard> cardOptional = playedCards.stream()
				.filter(playedCard -> playedCard.getId().equals(cardId))
				.findAny();
		
		boolean isPlayedCard = cardOptional.isPresent();
		boolean isHandCard = false;
		if (!isPlayedCard) {
			cardOptional = handCards.stream()
					.filter(handCard -> handCard.getId().equals(cardId))
					.findAny();
			
			isHandCard = cardOptional.isPresent();
			if (!isHandCard) {
				cardOptional = discardPile.getCards().stream()
						.filter(discardCard -> discardCard.getId().equals(cardId))
						.findAny();
				
				if (cardOptional.isEmpty()) {
					throw new IllegalArgumentException("unknown card '" + cardId + "'");
				}
			}
		}
		
		HeroRealmsCard card = cardOptional.get();
		if (withAbility) {
			processSacrificeAbilities(table, playerArea, card);
		} else {
			playerArea.setActionMode(null);
		}
		
		if (isPlayedCard) {
			playedCards.remove(card);
		} else if (isHandCard) {
			handCards.remove(card);
		} else {
			discardPile.removeCard(card);
		}
		
		table.getSacrificePile().addCard(card);
	}
	
	void discard(HeroRealmsTable table, String cardId) {
		
		String discardPlayerId = userService.getCurrentUser();
		PlayerArea discardPlayerArea = table.getPlayerAreas().get(discardPlayerId);
		
		Player activePlayer = table.getActivePlayer();
		PlayerArea activePlayerArea = table.getPlayerAreas().get(activePlayer.getId());
		
		boolean isSelected4Discard = discardPlayerArea.isSelected4Discard();
		if (!isSelected4Discard) {
			heroRealmsTableService.checkIsPlayerActive(table);
		}
		
		List<HeroRealmsCard> hand = discardPlayerArea.getHand();
		HeroRealmsCard card = hand.stream()
				.filter(handCard -> handCard.getId().equals(cardId))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException("unknown card '" + cardId + "'"));
		
		hand.remove(card);
		discardPlayerArea.getDiscardPile().addCard(card);
		discardPlayerArea.setActionMode(null);
		
		if (isSelected4Discard) {
			discardPlayerArea.setSelected4Discard(false);
			activePlayerArea.setActionMode(null);
		}
	}
	
	void selectPlayer4Discard(HeroRealmsTable table, String playerId) {
		
		heroRealmsTableService.checkIsPlayerActive(table);
		
		PlayerArea otherPlayerArea = table.getPlayerAreas().get(playerId);
		
		otherPlayerArea.setActionMode(HeroRealmsSpecialActionMode.DISCARD);
		otherPlayerArea.setSelected4Discard(true);
	}
	
	void prepareChampion(HeroRealmsTable table, String cardId) {
		
		heroRealmsTableService.checkIsPlayerActive(table);
		
		Player activePlayer = table.getActivePlayer();
		PlayerArea playerArea = table.getPlayerAreas().get(activePlayer.getId());
		List<HeroRealmsCard> champions = playerArea.getChampions();
		HeroRealmsCard champion = champions.stream()
			.filter(handCard -> handCard.getId().equals(cardId))
			.findAny()
			.orElseThrow(() -> new IllegalArgumentException("unknown champion '" + cardId + "'"));
		
		champion.setReady(true);
		playerArea.setActionMode(null);
	}
	
	void stunTargetChampion(HeroRealmsTable table, String playerId, String championId) {
		
		heroRealmsTableService.checkIsPlayerActive(table);
		
		Player activePlayer = table.getActivePlayer();
		PlayerArea activePlayerArea = table.getPlayerAreas().get(activePlayer.getId());
		PlayerArea otherPlayerArea = table.getPlayerAreas().get(playerId);
		List<HeroRealmsCard> champions = otherPlayerArea.getChampions();
		HeroRealmsCard champion = champions.stream()
			.filter(handCard -> handCard.getId().equals(championId))
			.findAny()
			.orElseThrow(() -> new IllegalArgumentException("unknown champion '" + championId + "'"));
		
		champion.setStunnedSinceLastTurn(true);
		champions.remove(champion);
		otherPlayerArea.getDiscardPile().addCard(champion);
		activePlayerArea.setActionMode(null);
	}
	
	void putCardTopDeck(HeroRealmsTable table, String cardId) {
		
		heroRealmsTableService.checkIsPlayerActive(table);
		
		Player activePlayer = table.getActivePlayer();
		PlayerArea playerArea = table.getPlayerAreas().get(activePlayer.getId());
		Deck<HeroRealmsCard> discardPile = playerArea.getDiscardPile();
		List<HeroRealmsCard> cards = discardPile.getCards();
		HeroRealmsCard card = cards.stream()
				.filter(discardCard -> discardCard.getId().equals(cardId))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException("unknown card '" + cardId + "'"));
		
		discardPile.removeCard(card);
		playerArea.getDeck().addCardTop(card);
		playerArea.setActionMode(null);
	}
	
	private void processCardAbilities(HeroRealmsTable table, PlayerArea area, HeroRealmsCard card) {
		
		HeroRealmsCardAbilities cardAbilities = heroRealmsService.getCardAbilities(card.getName());
		processCardAbilities(table, area, card, cardAbilities.getPrimaryAbility());
		
		if (card.getFaction() != null) {
			switch(card.getFaction()) {
				case GUILD:
					int factionCountGuild = area.getFactionCountGuild();
					area.setFactionCountGuild(++factionCountGuild);
					processAllyAbilities(table, area, card, cardAbilities, HeroRealmsFaction.GUILD, factionCountGuild);
					break;
				case IMPERIAL:
					int factionCountImperial = area.getFactionCountImperial();
					area.setFactionCountImperial(++factionCountImperial);
					processAllyAbilities(table, area, card, cardAbilities, HeroRealmsFaction.IMPERIAL, factionCountImperial);
					break;
				case NECROS:
					int factionCountNecros = area.getFactionCountNecros();
					area.setFactionCountNecros(++factionCountNecros);
					processAllyAbilities(table, area, card, cardAbilities, HeroRealmsFaction.NECROS, factionCountNecros);
					break;
				case WILD:
					int factionCountWild = area.getFactionCountWild();
					area.setFactionCountWild(++factionCountWild);
					processAllyAbilities(table, area, card, cardAbilities, HeroRealmsFaction.WILD, factionCountWild);
					break;
				default:
					break;
			}
		}
	}
	
	private void processAllyAbilities(HeroRealmsTable table, PlayerArea area, HeroRealmsCard card,
			HeroRealmsCardAbilities cardAbilities, HeroRealmsFaction faction, int factionCount) {
		
		if (factionCount > 1) {
			processCardAbilities(table, area, card, cardAbilities.getAllyAbility());
		}
		
		if (factionCount == 2) {
			List<HeroRealmsCard> playedChampions = area.getChampions().stream()
					.filter(champion -> !champion.isReady())
					.filter(champion -> !champion.getId().equals(card.getId()))
					.collect(Collectors.toList());
			ListUtils.union(playedChampions, area.getPlayedCards()).stream()
				.filter(playedCard -> playedCard.getFaction() == faction)
				.forEach(otherCard -> {
					HeroRealmsCardAbilities otherCardAbilities = heroRealmsService.getCardAbilities(otherCard.getName());
					processCardAbilities(table, area, card, otherCardAbilities.getAllyAbility());
				});
		}
	}
	
	private void processSacrificeAbilities(HeroRealmsTable table, PlayerArea area, HeroRealmsCard card) {
		
		HeroRealmsCardAbilities cardAbilities = heroRealmsService.getCardAbilities(card.getName());
		HeroRealmsAbilitySet sacrificeAbilities = cardAbilities.getSacrificeAbility();
		
		if (sacrificeAbilities.getAbilities().isEmpty()) {
			throw new IllegalArgumentException("no sacrifice ability for card '" + card.getId() + "'");
		}
		
		processCardAbilities(table, area, card, sacrificeAbilities);
	}
	
	private void processCardAbilities(HeroRealmsTable table, PlayerArea area, HeroRealmsCard card,
			HeroRealmsAbilitySet abilitieSet) {
		
		if (abilitieSet == null) {
			return;
		}
		
		HeroRealmsAbilityLinkage linkage = abilitieSet.getLinkage();
		if (linkage == HeroRealmsAbilityLinkage.BOTH_IF_2_CHAMPIONS_IN_PLAY) {
			if (area.getChampions().size() >= 2) {
				linkage = HeroRealmsAbilityLinkage.AND;
			} else {
				linkage = HeroRealmsAbilityLinkage.OR;
			}
		}
		
		if (linkage == HeroRealmsAbilityLinkage.OR) {
			addOrDecisionOptions(area, card, abilitieSet);
		} else {
			abilitieSet.getAbilities().forEach(ability -> processCardAbility(table, area, card, ability));
		}
	}
	
	private void addOrDecisionOptions(PlayerArea area, HeroRealmsCard card, HeroRealmsAbilitySet abilitieSet) {
		
		List<HeroRealmsDecisionOption> options = abilitieSet.getAbilities().stream()
				.map(ability -> {
					return HeroRealmsDecisionOption.builder()
							.id(UUID.randomUUID().toString())
							.text(getAbilityMessageText(ability.getType(), ability.getValue()))
							.abilityType(ability.getType())
							.value(ability.getValue())
							.build();
				})
				.collect(Collectors.toList());
		
		String decisionText = options.stream()
				.map(option -> option.getText())
				.collect(Collectors.joining(" oder "));
		
		HeroRealmsDecision decision = HeroRealmsDecision.builder()
				.id(UUID.randomUUID().toString())
				.cardId(card.getId())
				.type(HeroRealmsDecisionType.SELECT_ONE)
				.text(decisionText)
				.options(options)
				.build();
		
		area.getDecisions().add(decision);
	}
	
	private String getAbilityMessageText(HeroRealmsAbilityType type, int value) {
		return messageSource.getMessage("hero_realms.ability." + type, 
				(value == 0 ? null : new Integer[] {value}),
				Locale.getDefault());
	}
	
	private boolean processCardAbility(HeroRealmsTable table, PlayerArea area, HeroRealmsCard card,
			HeroRealmsAbility ability) {
		return processCardAbility(table, area, card, ability.getType(), ability.getValue());
	}
	
	private boolean processCardAbility(HeroRealmsTable table, PlayerArea area, HeroRealmsCard card,
			HeroRealmsAbilityType type, int value) {
		
		switch (type) {
			case HEALTH:
				area.setHealth(area.getHealth()+value);
				break;
			case GOLD:
				area.setGold(area.getGold()+value);
				break;
			case COMBAT:
				area.setCombat(area.getCombat()+value);
				break;
			case HEALTH_EACH_CHAMPION:
				area.setHealth(area.getHealth() + (area.getChampions().size() * value));
				break;
			case COMBAT_EACH_CHAMPION:
				addCombatEachChampion(area, card, value, false, false);
				break;
			case COMBAT_EACH_OTHER_CHAMPION:
				addCombatEachChampion(area, card, value, false, true);
				break;
			case COMBAT_EACH_OTHER_GUARD:
				addCombatEachChampion(area, card, value, true, true);
				break;
			case COMBAT_EACH_OTHER_FACTION:
				addCombatEachOtherFaction(area, card, value);
				break;
			case COMBAT_EACH_KNIFE:
				addCombatEachKnife(area, value);
				break;
			case DRAW_CARD:
				area.getHand().addAll(draw(area, value));
				break;
			case DRAW_CARD_IF_COMBAT_7:
				if (area.getCombat() >= 7) {
					area.getHand().addAll(draw(area, value));
				}
				break;
			case DRAW_CARD_IF_BOW_IN_PLAY:
				if (area.getPlayedCards().stream()
						.filter(playedCard -> playedCard.getSubType() == HeroRealmsCardSubType.BOW)
						.findAny()
						.isPresent()) {
					area.getHand().addAll(draw(area, value));
				}
				break;
			case DRAW_CARD_IF_ACTIONS_IN_PLAY:
				if (area.getPlayedCards().stream()
						.filter(playedCard -> playedCard.getType() == HeroRealmsCardType.ACTION)
						.count() >= 2) {
					area.getHand().addAll(draw(area, value));
				}
				break;
			case DRAW_DISCARD_CARD:
			case OPPONENT_DISCARD_CARD:
			case PREPARE_CHAMPION:
			case STUN_TARGET_CHAMPION:
			case PUT_CARD_DISCARD_PILE_TOP_DECK:
			case PUT_CHAMPION_DISCARD_PILE_TOP_DECK:
			case SACRIFICE_HAND_OR_DISCARD_PILE:
				for (int i=0; i<value; i++) {
					addOptionalDecision(area, card, type, value);
				}
				break;
			case NEXT_ACTION_TOP_DECK:
			case NEXT_ACQUIRE_TOP_DECK:
			case NEXT_ACQUIRE_HAND:
			case NEXT_ACTION_COSTS_LESS:
			case NEXT_CHAMPION_COSTS_LESS:
				HeroRealmsDecision decision = addOptionalDecision(area, card, type, value);
				if (area.getBuyMode() == null) {
					setBuyMode(area, decision, type);
				}
				break;
			case SACRIFICE_HAND_OR_DISCARD_PILE_COMBAT:	
				addOptionalDecision(area, card, type, value);
				break;
			case CLERIC_BLESS:
				area.setActionMode(HeroRealmsSpecialActionMode.CLERIC_BLESS);
				break;
			case PUT_CHAMPION_DISCARD_INTO_PLAY:
				if (area.getDiscardPile().getCards().stream()
						.filter(discardCard -> discardCard.isStunnedSinceLastTurn())
						.findAny().isEmpty()) {
					notificationService.addNotification(NotificationType.WARNING, messageSource.getMessage(
							"hero_realms.info.no_champion_stunned_since_last_turn", null, Locale.getDefault()));
					return false;
				}
				area.setActionMode(HeroRealmsSpecialActionMode.PUT_CHAMPION_DISCARD_INTO_PLAY);
				break;
			case RANGER_TRACK:
				if (area.getDeck().getCards().isEmpty()) {
					notificationService.addNotification(NotificationType.WARNING, messageSource.getMessage(
							"hero_realms.info.deck_empty", null, Locale.getDefault()));
					return false;
				}
				area.setActionMode(HeroRealmsSpecialActionMode.RANGER_TRACK);
				area.setRangerTrackCards(area.getDeck().draw(3));
				area.setRangerTrackDiscardCount(2);
				break;
			case ACQUIRE_OPPONENT_DISCARD:
				if (table.getPlayerAreas().values().stream()
						.filter(otherArea -> !otherArea.getPlayerId().equals(area.getPlayerId()))
						.flatMap(otherArea -> otherArea.getDiscardPile().getCards().stream())
						.filter(discardCard -> discardCard.getCost() > 0 && discardCard.getCost() <= area.getGold())
						.findAny()
						.isEmpty()) {
					notificationService.addNotification(NotificationType.WARNING, messageSource.getMessage(
							"hero_realms.info.no_card_for_acquire_discard", null, Locale.getDefault()));
					return false;
				}
				area.setActionMode(HeroRealmsSpecialActionMode.ACQUIRE_OPPONENT_DISCARD);
				break;
			case WIZARD_FIREBALL:
				area.setActionMode(HeroRealmsSpecialActionMode.WIZARD_FIREBALL);
				break;
			default:
				notificationService.addNotification(NotificationType.ERROR, "ability " + type + " not implemented");
				return false;
		}
		
		return true;
	}
	
	private static void addCombatEachChampion(PlayerArea area, HeroRealmsCard card, int value, boolean onlyGuard,
			boolean onlyOther) {
		
		int combatValue = (int) (value * area.getChampions().stream()
				.filter(champion -> !(onlyGuard && champion.getType() != HeroRealmsCardType.GUARD))
				.filter(champion -> !(onlyOther && champion.getId().equals(card.getId())))
				.count());
		
		area.setCombat(area.getCombat() + combatValue);
	}
	
	private static void addCombatEachOtherFaction(PlayerArea area, HeroRealmsCard card, int value) {
		
		HeroRealmsFaction faction = card.getFaction();
		
		int combatValue = (int) (value * CollectionUtils.union(area.getChampions(), area.getPlayedCards()).stream()
				.filter(otherCard -> faction == otherCard.getFaction())
				.filter(otherCard -> !card.getId().equals(otherCard.getId()))
				.count());
		
		area.setCombat(area.getCombat() + combatValue);
	}
	
	private static void addCombatEachKnife(PlayerArea area, int value) {
		
		int combatValue = (int) (value * area.getPlayedCards().stream()
				.filter(playedCardCard -> playedCardCard.getSubType() == HeroRealmsCardSubType.KNIFE)
				.count());
		
		area.setCombat(area.getCombat() + combatValue);
	}
	
	private HeroRealmsDecision addOptionalDecision(PlayerArea area, HeroRealmsCard card, HeroRealmsAbilityType type, int value) {
		
		HeroRealmsDecisionOption option = HeroRealmsDecisionOption.builder()
				.id(UUID.randomUUID().toString())
				.text(getAbilityMessageText(type, value))
				.abilityType(type)
				.value(value)
				.build();
		
		HeroRealmsDecision decision = HeroRealmsDecision.builder()
				.id(UUID.randomUUID().toString())
				.cardId(card == null ? null : card.getId())
				.type(HeroRealmsDecisionType.OPTIONAL)
				.text(getAbilityMessageText(type, value))
				.options(Arrays.asList(option))
				.build();
		
		area.getDecisions().add(decision);
		
		return decision;
	}
	
	void attack(HeroRealmsTable table, String playerId, String championId, int value, boolean fireball)
			throws InterruptedException, ExecutionException {
		
		heroRealmsTableService.checkIsPlayerActive(table);
		
		Player activePlayer = table.getActivePlayer();
		PlayerArea activePlayerArea = table.getPlayerAreas().get(activePlayer.getId());
		PlayerArea otherPlayerArea = table.getPlayerAreas().get(playerId);
		
		HeroRealmsCard champion = null;
		if (championId != null) {
			champion = otherPlayerArea.getChampions().stream()
					.filter(championCard -> championCard.getId().equals(championId))
					.findAny().orElse(null);
			if (champion == null) {
				notificationService.addNotification(NotificationType.ERROR, 
						"unknown champion " + championId);
				return;
			}
		}
		
		if (!fireball
				&& hasGuard(otherPlayerArea)
				&& (champion == null || champion.getType() != HeroRealmsCardType.GUARD)) {
			notificationService.addNotification(NotificationType.WARNING, 
					"Kein Angriff möglich! Es ist ein Wächter vorhanden!");
			return;
		}
		
		int championDefense = 0;
		int playerCombat = activePlayerArea.getCombat();
		if (champion != null) {
			championDefense = champion.getDefense() - champion.getDamage();
			
			if (champion.isBlessed()) {
				championDefense++;
			}
			
			if (!fireball & championDefense > playerCombat) {
				notificationService.addNotification(NotificationType.WARNING, 
						"Nicht genug Schaden (" + playerCombat + ") um Champion zu besiegen (" + championDefense + ")!");
				return;
			}
		}
		
		if (champion == null) {
			int health = otherPlayerArea.getHealth();
			int attackPlayerValue = NumberUtils.min(health, value);
			int newHealth = health - attackPlayerValue;
			
			otherPlayerArea.setHealth(newHealth);
			if (newHealth <= 0) {
				removeKilledPlayer(otherPlayerArea);
				if (isEndOfGame(table)) {
					gameService.endGame(table.getGameId());
				}
			}
			
			if (!fireball) {
				activePlayerArea.setCombat(playerCombat - attackPlayerValue);
			}
		} else {
			int attackChampionValue = NumberUtils.min(championDefense, playerCombat);
			
			if (fireball && FIREBALL_ATTACK_VALUE < championDefense) {
				champion.setDamage(FIREBALL_ATTACK_VALUE);
			} else {
				champion.setStunnedSinceLastTurn(true);
				champion.setDamage(0);
				otherPlayerArea.getChampions().remove(champion);
				otherPlayerArea.getDiscardPile().addCard(champion);
			}
			
			if (!fireball) {
				activePlayerArea.setCombat(playerCombat - attackChampionValue);
			}
		}
	}
	
	private static void removeKilledPlayer(PlayerArea area) {
		
		area.setKilled(true);
		
		area.getHand().clear();
		area.getDeck().clear();
		area.getDiscardPile().clear();
		area.getChampions().clear();
	}
	
	private static boolean isEndOfGame(HeroRealmsTable table) {
		
		return (table.getPlayerAreas().values().stream()
				.filter(area -> !area.isKilled()).count() == 1);
	}
	
	private static boolean hasGuard(PlayerArea playerArea) {
		
		return playerArea.getChampions().stream()
				.filter(champion -> champion.getType() == HeroRealmsCardType.GUARD)
				.findAny()
				.isPresent();
	}
	
	void buyMarketCard(HeroRealmsTable table, String cardId) {
		
		heroRealmsTableService.checkIsPlayerActive(table);
		
		List<HeroRealmsCard> market = table.getMarket();
		HeroRealmsCard card = market.stream()
				.filter(Objects::nonNull)
				.filter(marketCard -> marketCard.getId().equals(cardId))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException("unknown card '" + cardId + "'"));
		
		Player activePlayer = table.getActivePlayer();
		PlayerArea playerArea = table.getPlayerAreas().get(activePlayer.getId());
		
		int cost = card.getCost();
		if (playerArea.getBuyMode() == HeroRealmsBuyMode.NEXT_ACTION_COSTS_LESS
				&& card.getType() == HeroRealmsCardType.ACTION) {
			cost--;
		} else if (playerArea.getBuyMode() == HeroRealmsBuyMode.NEXT_CHAMPION_COSTS_LESS
				&& (card.getType() == HeroRealmsCardType.CHAMPION || card.getType() == HeroRealmsCardType.GUARD)) {
			cost--;
		}
		
		if (playerArea.getGold() < cost) {
			notificationService.addNotification(NotificationType.WARNING, 
					"nicht genug Gold (Preis: " + cost + ", Gold: " + playerArea.getGold() + ")");
			return;
		}
		playerArea.setGold(playerArea.getGold()-cost);
		
		market.set(market.indexOf(card), table.getMarketDeck().draw());
		putAquiredCardToDeckOrHand(playerArea, card);
	}
	
	void buyFireGem(HeroRealmsTable table) {
		
		heroRealmsTableService.checkIsPlayerActive(table);
		
		Deck<HeroRealmsCard> fireGemsDeck = table.getFireGemsDeck();
		if (fireGemsDeck.getSize() == 0) {
			throw new IllegalArgumentException("fire gems deck is empty");
		}
		
		Player activePlayer = table.getActivePlayer();
		PlayerArea playerArea = table.getPlayerAreas().get(activePlayer.getId());
		
		HeroRealmsCard card = fireGemsDeck.getCards().get(0);
		if (playerArea.getGold() < card.getCost()) {
			notificationService.addNotification(NotificationType.WARNING, 
					"nicht genug Gold (Preis: " + card.getCost() + ", Gold: " + playerArea.getGold() + ")");
			return;
		}
		playerArea.setGold(playerArea.getGold()-card.getCost());
		
		putAquiredCardToDeckOrHand(playerArea, fireGemsDeck.draw());
	}
	
	private static void putAquiredCardToDeckOrHand(PlayerArea playerArea, HeroRealmsCard card) {
		
		boolean resetBuyMode = false;
		if (playerArea.getBuyMode() == null) {
			playerArea.getDiscardPile().addCard(card);
		} else {
			switch (playerArea.getBuyMode()) {
				case NEXT_ACTION_TOP_DECK:
					if (card.getType() == HeroRealmsCardType.ACTION) {
						playerArea.getDeck().addCardTop(card);
						resetBuyMode = true;
					} else {
						playerArea.getDiscardPile().addCard(card);
					}
					break;
				case NEXT_ACQUIRE_TOP_DECK:
					playerArea.getDeck().addCardTop(card);
					resetBuyMode = true;
					break;
				case NEXT_ACQUIRE_HAND:
					playerArea.getHand().add(card);
					resetBuyMode = true;
					break;
				case NEXT_ACTION_COSTS_LESS:
					playerArea.getDiscardPile().addCard(card);
					if (card.getType() == HeroRealmsCardType.ACTION) {
						resetBuyMode = true;
					}
					break;
				case NEXT_CHAMPION_COSTS_LESS:
					playerArea.getDiscardPile().addCard(card);
					if (card.getType() == HeroRealmsCardType.CHAMPION || card.getType() == HeroRealmsCardType.GUARD) {
						resetBuyMode = true;
					}
					break;
				default:
					break;
			}
		}
		
		if (resetBuyMode) {
			resetBuyMode(playerArea);
			activateNextBuyMode(playerArea);
		}
	}
	
	private static void resetBuyMode(PlayerArea playerArea) {
	
		playerArea.setBuyMode(null);
		playerArea.getDecisions().removeIf(decision -> {
			if (decision.isActive()) {
				switch (decision.getOptions().get(0).getAbilityType()) {
				case NEXT_ACTION_TOP_DECK:
				case NEXT_ACQUIRE_TOP_DECK:
				case NEXT_ACQUIRE_HAND:
				case NEXT_ACTION_COSTS_LESS:
				case NEXT_CHAMPION_COSTS_LESS:
					return true;
				default:
					break;
				}
			}
			
			return false;
		});
	}
	
	private static void activateNextBuyMode(PlayerArea playerArea) {
		
		playerArea.getDecisions().stream()
				.filter(decision -> {
					switch (decision.getOptions().get(0).getAbilityType()) {
						case NEXT_ACTION_TOP_DECK:
						case NEXT_ACQUIRE_TOP_DECK:
						case NEXT_ACQUIRE_HAND:
						case NEXT_ACTION_COSTS_LESS:
						case NEXT_CHAMPION_COSTS_LESS:
							return true;
						default:
							return false;
						}
				})
				.findAny()
				.ifPresent(decision -> {
					setBuyMode(playerArea, decision, decision.getOptions().get(0).getAbilityType());
				});
	}
	
	private static final int CHARACTER_ROUND_ABILITY_COST = 2;
	
	void processCharacterRoundAbilities(HeroRealmsTable table) {
		
		heroRealmsTableService.checkIsPlayerActive(table);
		
		Player activePlayer = table.getActivePlayer();
		PlayerArea playerArea = table.getPlayerAreas().get(activePlayer.getId());
		
		if (!playerArea.isCharacterRoundAbilityActive()) {
			notificationService.addNotification(NotificationType.ERROR, "character round abilities already processed");
			return;
		}
		
		if (playerArea.getGold() < CHARACTER_ROUND_ABILITY_COST) {
			notificationService.addNotification(NotificationType.WARNING, 
					"nicht genug Gold (Preis: " + CHARACTER_ROUND_ABILITY_COST + ", Gold: " + playerArea.getGold() + ")");
			return;
		}
		
		Stream.of(playerArea.getCharacter().getRoundAbilities()).forEach(ability -> {
			processCardAbility(table, playerArea, null, ability);
		});
		
		playerArea.setGold(playerArea.getGold()-CHARACTER_ROUND_ABILITY_COST);
		playerArea.setCharacterRoundAbilityActive(false);
	}
	
	void processCharacterOneTimeAbilities(HeroRealmsTable table) {
		
		heroRealmsTableService.checkIsPlayerActive(table);
		
		Player activePlayer = table.getActivePlayer();
		PlayerArea playerArea = table.getPlayerAreas().get(activePlayer.getId());
		
		if (playerArea.getCharacterOneTimeAbilityImage() == null) {
			notificationService.addNotification(NotificationType.ERROR, "character one time abilities already processed");
			return;
		}
		
		for (HeroRealmsAbility ability : playerArea.getCharacter().getOneTimeAbilities()) {
			if (!processCardAbility(table, playerArea, null, ability)) {
				return;
			}
		}
		
		playerArea.setCharacterOneTimeAbilityImage(null);
	}
	
	void selectPlayer4Bless(HeroRealmsTable table, String playerId) {
		
		heroRealmsTableService.checkIsPlayerActive(table);
		
		PlayerArea targetPlayerArea = table.getPlayerAreas().get(playerId);
		targetPlayerArea.setHealth(targetPlayerArea.getHealth()+3);
		targetPlayerArea.getChampions().forEach(champion -> champion.setBlessed(true));
		
		Player activePlayer = table.getActivePlayer();
		PlayerArea activePlayerArea = table.getPlayerAreas().get(activePlayer.getId());
		activePlayerArea.setActionMode(null);
		
		if (activePlayer.getId().equals(targetPlayerArea.getPlayerId())) {
			activePlayerArea.setBlessedThisTurn(true);
		}
	}
	
	void putChampionDiscardIntoPlay(HeroRealmsTable table, String cardId) {
		
		heroRealmsTableService.checkIsPlayerActive(table);
		
		Player activePlayer = table.getActivePlayer();
		PlayerArea playerArea = table.getPlayerAreas().get(activePlayer.getId());
		
		HeroRealmsCard champion = playerArea.getDiscardPile().getCards().stream()
				.filter(discardCard -> discardCard.isStunnedSinceLastTurn())
				.filter(discardCard -> discardCard.getId().equals(cardId))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException("unknown champion '" + cardId + "'"));
		
		playerArea.getDiscardPile().removeCard(champion);
		playerArea.getHand().add(champion);
		
		playCard(table, cardId);
		
		playerArea.setActionMode(null);
	}
	
	void rangerTrackDiscard(HeroRealmsTable table, String cardId) {
		
		heroRealmsTableService.checkIsPlayerActive(table);
		
		Player activePlayer = table.getActivePlayer();
		PlayerArea playerArea = table.getPlayerAreas().get(activePlayer.getId());
		
		List<HeroRealmsCard> rangerTrackCards = playerArea.getRangerTrackCards();
		HeroRealmsCard card = rangerTrackCards.stream()
				.filter(rangerTrackCard -> rangerTrackCard.getId().equals(cardId))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException("unknown card '" + cardId + "'"));
		
		rangerTrackCards.remove(card);
		playerArea.getDiscardPile().addCard(card);
		playerArea.setRangerTrackDiscardCount(playerArea.getRangerTrackDiscardCount()-1);
	}
	
	void rangerTrackUp(HeroRealmsTable table, String cardId) {
		
		heroRealmsTableService.checkIsPlayerActive(table);
		
		Player activePlayer = table.getActivePlayer();
		PlayerArea playerArea = table.getPlayerAreas().get(activePlayer.getId());
		
		List<HeroRealmsCard> rangerTrackCards = playerArea.getRangerTrackCards();
		HeroRealmsCard card = rangerTrackCards.stream()
				.filter(rangerTrackCard -> rangerTrackCard.getId().equals(cardId))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException("unknown card '" + cardId + "'"));
		
		int cardIndex = rangerTrackCards.indexOf(card);
		rangerTrackCards.remove(cardIndex);
		rangerTrackCards.add(cardIndex-1,card);
	}
	
	void rangerTrackDown(HeroRealmsTable table, String cardId) {
		
		heroRealmsTableService.checkIsPlayerActive(table);
		
		Player activePlayer = table.getActivePlayer();
		PlayerArea playerArea = table.getPlayerAreas().get(activePlayer.getId());
		
		List<HeroRealmsCard> rangerTrackCards = playerArea.getRangerTrackCards();
		HeroRealmsCard card = rangerTrackCards.stream()
				.filter(rangerTrackCard -> rangerTrackCard.getId().equals(cardId))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException("unknown card '" + cardId + "'"));
		
		int cardIndex = rangerTrackCards.indexOf(card);
		rangerTrackCards.remove(cardIndex);
		rangerTrackCards.add(cardIndex+1,card);
	}
	
	void rangerTrackEnd(HeroRealmsTable table) {
		
		heroRealmsTableService.checkIsPlayerActive(table);
		
		Player activePlayer = table.getActivePlayer();
		PlayerArea playerArea = table.getPlayerAreas().get(activePlayer.getId());
		
		List<HeroRealmsCard> rangerTrackCards = playerArea.getRangerTrackCards();
		Collections.reverse(rangerTrackCards);
		rangerTrackCards.forEach(card -> playerArea.getDeck().addCardTop(card));
		
		playerArea.getRangerTrackCards().clear();
		playerArea.setRangerTrackDiscardCount(0);
		playerArea.setActionMode(null);
	}
	
	void acquireOpponentDiscard(HeroRealmsTable table, String playerId, String cardId) {
		
		heroRealmsTableService.checkIsPlayerActive(table);
		
		Player activePlayer = table.getActivePlayer();
		PlayerArea activePlayerArea = table.getPlayerAreas().get(activePlayer.getId());
		
		PlayerArea otherPlayerArea = table.getPlayerAreas().get(playerId);
		HeroRealmsCard card = otherPlayerArea.getDiscardPile().getCards().stream()
				.filter(discardCard -> discardCard.getId().equals(cardId))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException("unknown card '" + cardId + "'"));
		
		int cost = card.getCost();
		if (activePlayerArea.getGold() < cost) {
			notificationService.addNotification(NotificationType.WARNING, 
					"nicht genug Gold (Preis: " + cost + ", Gold: " + activePlayerArea.getGold() + ")");
			return;
		}
		activePlayerArea.setGold(activePlayerArea.getGold()-cost);
		
		otherPlayerArea.getDiscardPile().removeCard(card);
		activePlayerArea.getDiscardPile().addCard(card);
		
		activePlayerArea.setActionMode(null);
	}
	
	private static final int FIREBALL_ATTACK_VALUE = 4;
	
	void selectPlayer4Fireball(HeroRealmsTable table, String playerId) throws InterruptedException, ExecutionException {
		
		heroRealmsTableService.checkIsPlayerActive(table);
		
		Player activePlayer = table.getActivePlayer();
		PlayerArea activePlayerArea = table.getPlayerAreas().get(activePlayer.getId());
		
		PlayerArea otherPlayerArea = table.getPlayerAreas().get(playerId);
		
		attack(table, playerId, null, FIREBALL_ATTACK_VALUE, true);
		if (!CollectionUtils.isEmpty(otherPlayerArea.getChampions())) {
			for (String championId : otherPlayerArea.getChampions().stream()
					.map(champion -> champion.getId())
					.collect(Collectors.toList())) {
				attack(table, playerId, championId, FIREBALL_ATTACK_VALUE, true);
			}
		}
		
		activePlayerArea.setActionMode(null);
	}
	
	void cancelSpecialActionMode(HeroRealmsTable table) {
		
		heroRealmsTableService.checkIsPlayerActive(table);
		
		Player player = table.getActivePlayer();
		PlayerArea playerArea = table.getPlayerAreas().get(player.getId());
		
		if (playerArea.getActionMode() == null) {
			return;
		}
		
		switch (playerArea.getActionMode()) {
			case OPPONENT_DISCARD_CARD:
			case PREPARE_CHAMPION:
			case STUN_TARGET_CHAMPION:
			case PUT_CARD_DISCARD_PILE_TOP_DECK:
			case PUT_CHAMPION_DISCARD_PILE_TOP_DECK:
			case SACRIFICE:
			case CLERIC_BLESS:
				playerArea.setActionMode(null);
				break;
			default:
				throw new IllegalArgumentException("mode '" + playerArea.getActionMode() + "' not supported");
		}
	}
	
	void endTurn(HeroRealmsTable table) {
		
		heroRealmsTableService.checkIsPlayerActive(table);
		
		Player activePlayer = table.getActivePlayer();
		PlayerArea playerArea = table.getPlayerAreas().get(activePlayer.getId());
		playerArea.setActive(false);
		playerArea.setGold(0);
		playerArea.setCombat(0);
		playerArea.setActionMode(null);
		playerArea.setBuyMode(null);
		
		if (playerArea.getCharacter() != null) {
			playerArea.setCharacterRoundAbilityActive(true);
		}
		
		Deck<HeroRealmsCard> discardPile = playerArea.getDiscardPile();
		
		List<HeroRealmsCard> playedCards = playerArea.getPlayedCards();
		discardPile.addCards(playedCards);
		playedCards.clear();
		
		List<HeroRealmsCard> hand = playerArea.getHand();
		discardPile.addCards(hand);
		hand.clear();
		
		hand.addAll(draw(playerArea, 5));
		
		playerArea.getChampions().forEach(champion -> champion.setReady(true));
		playerArea.getDiscardPile().getCards().forEach(champion -> champion.setStunnedSinceLastTurn(false));
		
		if (playerArea.isBlessedThisTurn()) {
			playerArea.setBlessedThisTurn(false);
		} else {
			playerArea.getChampions().forEach(champion -> champion.setBlessed(false));
		}
		
		playerArea.getDecisions().clear();
		
		playerArea.setFactionCountGuild(0);
		playerArea.setFactionCountImperial(0);
		playerArea.setFactionCountNecros(0);
		playerArea.setFactionCountWild(0);
		
		table.getPlayerAreas().values().stream()
				.flatMap(area -> area.getChampions().stream())
				.forEach(champion -> champion.setDamage(0));
		
		activateNextPlayer(table, activePlayer);
	}
	
	private static HeroRealmsCard draw(PlayerArea playerArea) {
		return draw(playerArea, 1).get(0);
	}
	
	private static List<HeroRealmsCard> draw(PlayerArea playerArea, int number) {
		
		Deck<HeroRealmsCard> deck = playerArea.getDeck();
		
		List<HeroRealmsCard> cards = new ArrayList<>(deck.draw(number));
		
		int missingCards = (number - cards.size());
		if (missingCards > 0) {
			Deck<HeroRealmsCard> discardPile = playerArea.getDiscardPile();
			deck.setCards(discardPile.getCards(), true);
			discardPile.clear();
			
			cards.addAll(deck.draw(missingCards));
		}
		
		return cards;
	}
	
	private static void activateNextPlayer(HeroRealmsTable table, Player activePlayer) {
		
		Player nextPlayer = getNextPlayerAlive(table, activePlayer);
		table.setActivePlayer(nextPlayer);
		table.getPlayerAreas().get(nextPlayer.getId()).setActive(true);
	}
	
	private static Player getNextPlayerAlive(HeroRealmsTable table, Player activePlayer) {
		
		List<Player> allPlayers = table.getPlayers();
		Map<String, PlayerArea> playerAreas = table.getPlayerAreas();
		int indexNextPlayer = allPlayers.indexOf(activePlayer);
		
		Player nextPlayer;
		PlayerArea nextPlayerArea;
		do {
			if (indexNextPlayer == allPlayers.size()-1) {
				indexNextPlayer = 0;
			} else {
				indexNextPlayer++;
			}
			
			nextPlayer = allPlayers.get(indexNextPlayer);
			nextPlayerArea = playerAreas.get(nextPlayer.getId());
		} while (nextPlayerArea.isKilled());
		
		return nextPlayer;
	}
}
