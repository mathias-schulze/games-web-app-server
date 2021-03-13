package de.msz.games.games.herorealms;

import java.util.List;

import de.msz.games.games.player.Player;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper=true)
@Setter(value = AccessLevel.PROTECTED)
@SuperBuilder(toBuilder=true)
public class HeroRealmsTablePlayerView extends HeroRealmsTable {
	
	private PlayerArea ownPlayerArea;
	
	private List<PlayerArea> otherPlayerAreas;
	
	HeroRealmsTablePlayerView(String gameId, List<Player> players) {
		super(gameId, players);
	}
}
