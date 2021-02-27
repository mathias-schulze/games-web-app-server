package de.msz.games.games.herorealms;

import java.util.List;

import org.springframework.stereotype.Service;

import de.msz.games.games.player.Player;

@Service
public class HeroRealmsActionsService {
	
	void endTurn(HeroRealmsTable table) {
		
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
