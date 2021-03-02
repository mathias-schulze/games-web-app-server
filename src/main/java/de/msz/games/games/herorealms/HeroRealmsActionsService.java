package de.msz.games.games.herorealms;

import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
			default:
				throw new NotImplementedException();
		}
	}
	
	void endTurn(HeroRealmsTable table) {
		
		heroRealmsTableService.checkIsPlayerActive(table);
		
		Player activePlayer = table.getActivePlayer();
		table.getPlayerAreas().get(activePlayer.getId()).setActive(false);
		
		List<Player> allPlayers = table.getPlayers();
		
		int indexActivePlayer = allPlayers.indexOf(activePlayer);
		if (indexActivePlayer == allPlayers.size()-1) {
			indexActivePlayer = 0;
		} else {
			indexActivePlayer++;
		}
		
		activePlayer = allPlayers.get(indexActivePlayer);
		table.setActivePlayer(activePlayer);
		table.getPlayerAreas().get(activePlayer.getId()).setActive(true);
	}
}
