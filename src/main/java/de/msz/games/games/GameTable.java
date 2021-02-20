package de.msz.games.games;

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
}
