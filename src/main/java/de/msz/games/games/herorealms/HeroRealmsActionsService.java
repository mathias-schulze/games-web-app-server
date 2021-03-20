package de.msz.games.games.herorealms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
					.filter(champion -> !champion.isReady()).collect(Collectors.toList());
			ListUtils.union(playedChampions, area.getPlayedCards()).stream()
				.filter(playedCard -> playedCard.getFaction() == faction)
				.forEach(otherCard -> {
					HeroRealmsCardAbilities otherCardAbilities = heroRealmsService.getCardAbilities(otherCard.getName());
					processCardAbilities(area, card, otherCardAbilities.getAllyAbility());
				});
		}
	}
	
	private void processCardAbilities(PlayerArea area, HeroRealmsCard card, HeroRealmsAbilitySet abilitieSet) {
		
		if (abilitieSet == null) {
			return;
		}
		
		if (abilitieSet.getLinkage() == HeroRealmsAbilityLinkage.OR) {
			notificationService.addNotification(NotificationType.ERROR, 
					"ability linkage OR not implemented");
			return;
		}
		
		abilitieSet.getAbilities().forEach(ability -> processCardAbility(area, card, ability));
	}
	
	private void processCardAbility(PlayerArea area, HeroRealmsCard card, HeroRealmsAbility ability) {
		
		switch (ability.getType()) {
			case HEALTH:
				area.setHealth(area.getHealth()+ability.getValue());
				break;
			case GOLD:
				area.setGold(area.getGold()+ability.getValue());
				break;
			case COMBAT:
				area.setCombat(area.getCombat()+ability.getValue());
				break;
			case HEALTH_EACH_CHAMPION:
				area.setHealth(area.getHealth() + (area.getChampions().size() * ability.getValue()));
				break;
			case COMBAT_EACH_CHAMPION:
				addCombatEachChampion(area, card, ability.getValue(), false, false);
				break;
			case COMBAT_EACH_OTHER_CHAMPION:
				addCombatEachChampion(area, card, ability.getValue(), false, true);
				break;
			case COMBAT_EACH_OTHER_GUARD:
				addCombatEachChampion(area, card, ability.getValue(), true, true);
				break;
			case COMBAT_EACH_OTHER_FACTION:
				addCombatEachOtherFaction(area, card, ability.getValue());
				break;
			case DRAW_CARD:
				area.getHand().addAll(area.getDeck().draw(ability.getValue()));
				break;
			default:
				notificationService.addNotification(NotificationType.ERROR, 
						"ability " + ability.getType() + " not implemented");
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
					"Kein Angriff möglich! Es ist ein Wächter vorhanden!");
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
		
		Deck<HeroRealmsCard> discardPile = playerArea.getDiscardPile();
		
		List<HeroRealmsCard> playedCards = playerArea.getPlayedCards();
		discardPile.addCards(playedCards);
		playedCards.clear();
		
		List<HeroRealmsCard> hand = playerArea.getHand();
		discardPile.addCards(hand);
		hand.clear();
		
		hand.addAll(draw(playerArea, 5));
		
		playerArea.getChampions().forEach(champion -> champion.setReady(true));
		
		playerArea.setFactionCountGuild(0);
		playerArea.setFactionCountImperial(0);
		playerArea.setFactionCountNecros(0);
		playerArea.setFactionCountWild(0);
		
		activateNextPlayer(table, activePlayer);
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
