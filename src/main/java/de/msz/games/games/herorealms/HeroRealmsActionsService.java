package de.msz.games.games.herorealms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import de.msz.games.base.NotificationService;
import de.msz.games.base.NotificationService.NotificationType;
import de.msz.games.games.Deck;
import de.msz.games.games.GameService;
import de.msz.games.games.herorealms.HeroRealmsTable.PlayerArea;
import de.msz.games.games.player.Player;

@Service
public class HeroRealmsActionsService {
	
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
		
		processCardAbilities(playerArea, card);
		
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
		processCardAbilities(playerArea, champion);
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
				makeDecisionSelect(playerArea, madeDecision, optionId);
				break;
			case OPTIONAL:
				makeDecisionOptional(playerArea, madeDecision);
				break;
			default:
				break;
		}
		
		playerArea.getDecisions().remove(madeDecision);
	}
	
	void makeDecisionSelect(PlayerArea area, HeroRealmsDecision decision, String optionId) {
		
		HeroRealmsDecisionOption selectedOption = decision.getOptions().stream()
				.filter(option -> option.getId().equals(optionId))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException(
						"unknown option '" + optionId + "' for decision '" + decision.getId() + "'"));
		
		HeroRealmsCard card4Decision = getCard4Decision(area, decision);
		
		processCardAbility(area, card4Decision, selectedOption.getAbilityType(), selectedOption.getValue());
	}
	
	private void makeDecisionOptional(PlayerArea area, HeroRealmsDecision decision) {
		
		HeroRealmsDecisionOption selectedOption = decision.getOptions().get(0);
		HeroRealmsCard card4Decision = getCard4Decision(area, decision);
		
		HeroRealmsAbilityType abilityType = selectedOption.getAbilityType();
		switch (abilityType) {
			case DRAW_DISCARD_CARD:
				area.getHand().add(draw(area));
				area.setActionMode(HeroRealmsSpecialActionMode.DISCARD);
				break;
			case PREPARE_CHAMPION:
				area.setActionMode(HeroRealmsSpecialActionMode.PREPARE_CHAMPION);
				break;
			case SACRIFICE_HAND_OR_DISCARD_PILE:
				area.setActionMode(HeroRealmsSpecialActionMode.SACRIFICE);
				break;
			case SACRIFICE_HAND_OR_DISCARD_PILE_COMBAT:
				area.setActionMode(HeroRealmsSpecialActionMode.SACRIFICE);
				processCardAbility(area, card4Decision, HeroRealmsAbilityType.COMBAT, selectedOption.getValue());
				break;
			default:
				notificationService.addNotification(NotificationType.ERROR, "ability " + abilityType + " not implemented");
		}
	}
	
	private static HeroRealmsCard getCard4Decision(PlayerArea area, HeroRealmsDecision decision) {
		
		return CollectionUtils.union(area.getPlayedCards(), area.getChampions()).stream()
				.filter(card -> card.getId().equals(decision.getCardId()))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException(
						"unknown card '" + decision.getCardId() + "' for decision '" + decision.getId() + "'"));
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
			processSacrificeAbilities(playerArea, card);
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
		
		heroRealmsTableService.checkIsPlayerActive(table);
		
		Player activePlayer = table.getActivePlayer();
		PlayerArea playerArea = table.getPlayerAreas().get(activePlayer.getId());
		List<HeroRealmsCard> hand = playerArea.getHand();
		HeroRealmsCard card = hand.stream()
				.filter(handCard -> handCard.getId().equals(cardId))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException("unknown card '" + cardId + "'"));
		
		hand.remove(card);
		playerArea.getDiscardPile().addCard(card);
		playerArea.setActionMode(null);
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
	
	private void processCardAbilities(PlayerArea area, HeroRealmsCard card) {
		
		HeroRealmsCardAbilities cardAbilities = heroRealmsService.getCardAbilities(card.getName());
		processCardAbilities(area, card, cardAbilities.getPrimaryAbility());
		
		if (card.getFaction() != null) {
			switch(card.getFaction()) {
				case GUILD:
					int factionCountGuild = area.getFactionCountGuild();
					area.setFactionCountGuild(++factionCountGuild);
					processAllyAbilities(area, card, cardAbilities, HeroRealmsFaction.GUILD, factionCountGuild);
					break;
				case IMPERIAL:
					int factionCountImperial = area.getFactionCountImperial();
					area.setFactionCountImperial(++factionCountImperial);
					processAllyAbilities(area, card, cardAbilities, HeroRealmsFaction.IMPERIAL, factionCountImperial);
					break;
				case NECROS:
					int factionCountNecros = area.getFactionCountNecros();
					area.setFactionCountNecros(++factionCountNecros);
					processAllyAbilities(area, card, cardAbilities, HeroRealmsFaction.NECROS, factionCountNecros);
					break;
				case WILD:
					int factionCountWild = area.getFactionCountWild();
					area.setFactionCountWild(++factionCountWild);
					processAllyAbilities(area, card, cardAbilities, HeroRealmsFaction.WILD, factionCountWild);
					break;
				default:
					break;
			}
		}
	}
	
	private void processAllyAbilities(PlayerArea area, HeroRealmsCard card, HeroRealmsCardAbilities cardAbilities,
			HeroRealmsFaction faction, int factionCount) {
		
		if (factionCount > 1) {
			processCardAbilities(area, card, cardAbilities.getAllyAbility());
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
					processCardAbilities(area, card, otherCardAbilities.getAllyAbility());
				});
		}
	}
	
	private void processSacrificeAbilities(PlayerArea area, HeroRealmsCard card) {
		
		HeroRealmsCardAbilities cardAbilities = heroRealmsService.getCardAbilities(card.getName());
		HeroRealmsAbilitySet sacrificeAbilities = cardAbilities.getSacrificeAbility();
		
		if (sacrificeAbilities.getAbilities().isEmpty()) {
			throw new IllegalArgumentException("no sacrifice ability for card '" + card.getId() + "'");
		}
		
		processCardAbilities(area, card, sacrificeAbilities);
	}
	
	private void processCardAbilities(PlayerArea area, HeroRealmsCard card, HeroRealmsAbilitySet abilitieSet) {
		
		if (abilitieSet == null) {
			return;
		}
		
		if (abilitieSet.getLinkage() == HeroRealmsAbilityLinkage.OR) {
			addOrDecisionOptions(area, card, abilitieSet);
		} else {
			abilitieSet.getAbilities().forEach(ability -> processCardAbility(area, card, ability));
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
	
	private void processCardAbility(PlayerArea area, HeroRealmsCard card, HeroRealmsAbility ability) {
		processCardAbility(area, card, ability.getType(), ability.getValue());
	}
	
	private void processCardAbility(PlayerArea area, HeroRealmsCard card, HeroRealmsAbilityType type, int value) {
		
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
			case DRAW_CARD:
				area.getHand().addAll(draw(area, value));
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
			case SACRIFICE_HAND_OR_DISCARD_PILE_COMBAT:	
				addOptionalDecision(area, card, type, value);
				break;
			default:
				notificationService.addNotification(NotificationType.ERROR, "ability " + type + " not implemented");
		}
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
	
	private void addOptionalDecision(PlayerArea area, HeroRealmsCard card, HeroRealmsAbilityType type, int value) {
		
		HeroRealmsDecisionOption option = HeroRealmsDecisionOption.builder()
				.id(UUID.randomUUID().toString())
				.text(getAbilityMessageText(type, value))
				.abilityType(type)
				.value(value)
				.build();
		
		HeroRealmsDecision decision = HeroRealmsDecision.builder()
				.id(UUID.randomUUID().toString())
				.cardId(card.getId())
				.type(HeroRealmsDecisionType.OPTIONAL)
				.text(getAbilityMessageText(type, value))
				.options(Arrays.asList(option))
				.build();
		
		area.getDecisions().add(decision);
	}
	
	void attack(HeroRealmsTable table, String playerId, String championId, int value)
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
		
		if (hasGuard(otherPlayerArea) && (champion == null || champion.getType() != HeroRealmsCardType.GUARD)) {
			notificationService.addNotification(NotificationType.WARNING, 
					"Kein Angriff m�glich! Es ist ein W�chter vorhanden!");
			return;
		}
		
		if (champion != null && champion.getDefense() > value) {
			notificationService.addNotification(NotificationType.WARNING, 
					"Nicht genug Schaden (" + value + ") um Champion zu besiegen (" + champion.getDefense() + ")!");
			return;
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
			
			activePlayerArea.setCombat(activePlayerArea.getCombat() - attackPlayerValue);
		} else {
			int defense = champion.getDefense();
			int attackChampionValue = NumberUtils.min(defense, value);
			
			otherPlayerArea.getChampions().remove(champion);
			otherPlayerArea.getDiscardPile().addCard(champion);
			
			activePlayerArea.setCombat(activePlayerArea.getCombat() - attackChampionValue);
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
		
		if (playerArea.getGold() < card.getCost()) {
			notificationService.addNotification(NotificationType.WARNING, 
					"nicht genug Gold (Preis: " + card.getCost() + ", Gold: " + playerArea.getGold() + ")");
			return;
		}
		playerArea.setGold(playerArea.getGold()-card.getCost());
		
		market.set(market.indexOf(card), table.getMarketDeck().draw());
		
		playerArea.getDiscardPile().addCard(card);
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
		
		playerArea.getDiscardPile().addCard(fireGemsDeck.draw());
	}
	
	void endTurn(HeroRealmsTable table) {
		
		heroRealmsTableService.checkIsPlayerActive(table);
		
		Player activePlayer = table.getActivePlayer();
		PlayerArea playerArea = table.getPlayerAreas().get(activePlayer.getId());
		playerArea.setActive(false);
		playerArea.setGold(0);
		playerArea.setCombat(0);
		playerArea.setActionMode(null);
		
		Deck<HeroRealmsCard> discardPile = playerArea.getDiscardPile();
		
		List<HeroRealmsCard> playedCards = playerArea.getPlayedCards();
		discardPile.addCards(playedCards);
		playedCards.clear();
		
		List<HeroRealmsCard> hand = playerArea.getHand();
		discardPile.addCards(hand);
		hand.clear();
		
		hand.addAll(draw(playerArea, 5));
		
		playerArea.getChampions().forEach(champion -> champion.setReady(true));
		
		playerArea.getDecisions().clear();
		
		playerArea.setFactionCountGuild(0);
		playerArea.setFactionCountImperial(0);
		playerArea.setFactionCountNecros(0);
		playerArea.setFactionCountWild(0);
		
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
