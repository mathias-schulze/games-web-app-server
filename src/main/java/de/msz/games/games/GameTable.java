package de.msz.games.games;

import java.util.ArrayList;
import java.util.List;

import de.msz.games.games.player.PlayerService.Player;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@SuperBuilder(toBuilder=true)
public abstract class GameTable {
	
	private final List<Player> players;
	
	/**
	 * Creates a list of the players sorted like they are sitting on a table clockwise starting from the current players
	 * position.  
	 * 
	 * @param currentPlayerPosition position of the current play at the table
	 * @return list of the other players sorted
	 */
	public List<Player> getOtherPlayersSorted(int currentPlayerPosition) {
		
		List<Player> otherPlayersSorted = new ArrayList<>(players);
		
		if (currentPlayerPosition == 0
				|| currentPlayerPosition == players.size()-1) {
			otherPlayersSorted.remove(currentPlayerPosition);
		} else {
			List<Player> playersBefore = players.subList(0, currentPlayerPosition);
			List<Player> playersAfter = players.subList(currentPlayerPosition+1, players.size());
			otherPlayersSorted.addAll(playersAfter);
			otherPlayersSorted.addAll(playersBefore);
		}
		
		return otherPlayersSorted;
	}
}
