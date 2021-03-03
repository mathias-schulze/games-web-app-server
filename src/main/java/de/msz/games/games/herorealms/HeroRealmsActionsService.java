package de.msz.games.games.herorealms;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.msz.games.games.Deck;
import de.msz.games.games.herorealms.HeroRealmsTable.PlayerArea;
import de.msz.games.games.player.Player;

@Service
public class HeroRealmsActionsService {
	
	@Autowired
	private HeroRealmsService heroRealmsService;
	
	@Autowired
	private HeroRealmsTableService heroRealmsTableService;
	
	void playCard(HeroRealmsTable table, String cardId) {
		
		synchronized (cardId) {
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
					break;
				default:
					playerArea.getPlayedCards().add(card);
					break;
			}
		}
	}
	
	private void processCardAbilities(PlayerArea area, HeroRealmsCard card) {
		
		HeroRealmsCardAbilities cardAbilities = heroRealmsService.getCardAbilities(card.getName());
		processCardAbilities(area, cardAbilities.getPrimaryAbility());
	}
	
	private static void processCardAbilities(PlayerArea area, HeroRealmsAbilitySet abilitieSet) {
		
		if (abilitieSet.getLinkage() == HeroRealmsAbilityLinkage.OR) {
			throw new NotImplementedException();
		}
		
		abilitieSet.getAbilities().forEach(ability -> processCardAbility(area, ability));
	}
	
	private static void processCardAbility(PlayerArea area, HeroRealmsAbility ability) {
		
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
				throw new NotImplementedException();
		}
	}
	
	void buyMarketCard(HeroRealmsTable table, String cardId) throws NotEnoughGoldException {
		
		heroRealmsTableService.checkIsPlayerActive(table);
		
		List<HeroRealmsCard> market = table.getMarket();
		HeroRealmsCard card = market.stream()
				.filter(marketCard -> marketCard.getId().equals(cardId))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException("unknown card '" + cardId + "'"));
		
		Player activePlayer = table.getActivePlayer();
		PlayerArea playerArea = table.getPlayerAreas().get(activePlayer.getId());
		
		if (playerArea.getGold() < card.getCost()) {
			throw new NotEnoughGoldException(playerArea.getGold(), card.getCost());
		}
		playerArea.setGold(playerArea.getGold()-card.getCost());
		
		market.set(market.indexOf(card), table.getMarketDeck().draw());
		
		playerArea.getDiscardPile().addCard(card);
	}
	
	void buyFireGem(HeroRealmsTable table) throws NotEnoughGoldException {
		
		heroRealmsTableService.checkIsPlayerActive(table);
		
		Deck<HeroRealmsCard> fireGemsDeck = table.getFireGemsDeck();
		if (fireGemsDeck.getSize() == 0) {
			throw new IllegalArgumentException("fire gems deck is empty");
		}
		
		Player activePlayer = table.getActivePlayer();
		PlayerArea playerArea = table.getPlayerAreas().get(activePlayer.getId());
		
		HeroRealmsCard card = fireGemsDeck.getCards().get(0);
		if (playerArea.getGold() < card.getCost()) {
			throw new NotEnoughGoldException(playerArea.getGold(), card.getCost());
		}
		playerArea.setGold(playerArea.getGold()-card.getCost());
		
		playerArea.getDiscardPile().addCard(fireGemsDeck.draw());
	}
	
	public static class NotEnoughGoldException extends Exception {
		
		private static final long serialVersionUID = 1L;
		
		public NotEnoughGoldException(int gold, int cost) {
			super("not enough gold (" + gold + "), cost: " + cost);
		}
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
