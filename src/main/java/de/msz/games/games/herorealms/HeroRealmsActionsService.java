package de.msz.games.games.herorealms;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.msz.games.base.NotificationService;
import de.msz.games.base.NotificationService.NotificationType;
import de.msz.games.games.Deck;
import de.msz.games.games.herorealms.HeroRealmsTable.PlayerArea;
import de.msz.games.games.player.Player;

@Service
public class HeroRealmsActionsService {
	
	@Autowired
	private NotificationService notificationService;
	
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
		
		processCardAbilities(playerArea, champion);
		champion.setReady(false);
	}
	
	private void processCardAbilities(PlayerArea area, HeroRealmsCard card) {
		
		HeroRealmsCardAbilities cardAbilities = heroRealmsService.getCardAbilities(card.getName());
		processCardAbilities(area, cardAbilities.getPrimaryAbility());
	}
	
	private void processCardAbilities(PlayerArea area, HeroRealmsAbilitySet abilitieSet) {
		
		if (abilitieSet.getLinkage() == HeroRealmsAbilityLinkage.OR) {
			notificationService.addNotification(NotificationType.ERROR, 
					"ability linkage OR not implemented");
		}
		
		abilitieSet.getAbilities().forEach(ability -> processCardAbility(area, ability));
	}
	
	private void processCardAbility(PlayerArea area, HeroRealmsAbility ability) {
		
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
			case DRAW_CARD:
				area.getHand().addAll(area.getDeck().draw(ability.getValue()));
				break;
			default:
				notificationService.addNotification(NotificationType.ERROR, 
						"ability " + ability.getType() + " not implemented");
		}
	}
	
	void attack(HeroRealmsTable table, String playerId, String championId, int value) {
		
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
		
		activePlayerArea.setCombat(activePlayerArea.getCombat() - value);
		if (champion == null) {
			otherPlayerArea.setHealth(otherPlayerArea.getHealth() - value);
		} else {
			otherPlayerArea.getChampions().remove(champion);
			otherPlayerArea.getDiscardPile().addCard(champion);
		}
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
		
		List<Player> allPlayers = table.getPlayers();
		
		int indexNextPlayer = allPlayers.indexOf(activePlayer);
		if (indexNextPlayer == allPlayers.size()-1) {
			indexNextPlayer = 0;
		} else {
			indexNextPlayer++;
		}
		
		Player nextPlayer = allPlayers.get(indexNextPlayer);
		table.setActivePlayer(nextPlayer);
		table.getPlayerAreas().get(nextPlayer.getId()).setActive(true);
	}
}
