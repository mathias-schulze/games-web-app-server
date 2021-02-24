package de.msz.games.games.herorealms;

import java.util.List;
import java.util.Map;

import de.msz.games.games.Deck;
import de.msz.games.games.GameTable;
import de.msz.games.games.player.PlayerService.Player;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper=true)
@Setter(value = AccessLevel.PROTECTED)
@SuperBuilder(toBuilder=true)
public class HeroRealmsTable extends GameTable {
	
	private String cardBack;
	
	private String emptyDeck;
	
	private FireGemsDeck fireGemsDeck;
	
	private List<HeroRealmsCard> market;
	
	private Deck<HeroRealmsCard> marketDeck;
	
	private Deck<HeroRealmsCard> sacrificePile;
	
	private Map<String, PlayerArea> playerAreas;
	
	HeroRealmsTable(List<Player> players) {
		super(players);
	}
	
	@Data
	@Setter(value = AccessLevel.PROTECTED)
	@AllArgsConstructor
	@Builder(toBuilder=true)
	public static class PlayerArea {
		
		private String playerId;
		
		private String playerName;
		
		private boolean active;
		
		private int position;
		
		private int health;
		
		private int combat;
		
		private int gold;
		
		private int handSize;
		
		private List<HeroRealmsCard> hand;
		
		private Deck<HeroRealmsCard> deck;
		
		private Deck<HeroRealmsCard> discardPile;
		
		private List<HeroRealmsCard> playedCards;
		
		private List<HeroRealmsCard> champions;
		
		public PlayerArea(Player player, int position) {
			this.playerId = player.getId();
			this.playerName = player.getName();
			this.position = position;
		}
	}
}
