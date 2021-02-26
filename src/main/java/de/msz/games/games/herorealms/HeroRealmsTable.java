package de.msz.games.games.herorealms;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.msz.games.games.Card;
import de.msz.games.games.Deck;
import de.msz.games.games.GameTable;
import de.msz.games.games.player.Player;
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
	
	@SuppressWarnings("unchecked")
	public static HeroRealmsTable from(Map<String, Object> map) {
		
		List<Player> players = ((List<Object>)map.get("players")).stream()
				.map(player -> Player.from((Map<String, Object>) player))
				.collect(Collectors.toList());
		
		List<HeroRealmsCard> market = ((List<Map<String, Object>>) map.get("market")).stream()
				.map(card -> Card.from(card, HeroRealmsCard.class))
				.collect(Collectors.toList());
		
		Map<String, PlayerArea> playerAreas = ((Map<String, Map<String, Object>>) map.get("playerAreas")).values().stream()
				.map(area -> PlayerArea.from(area))
				.collect(Collectors.toMap(PlayerArea::getPlayerId, Function.identity()));
		
		return HeroRealmsTable.builder()
				.players(players)
				.activePlayer(Player.from((Map<String, Object>) map.get("activePlayer")))
				.cardBack((String) map.get("cardBack"))
				.emptyDeck((String) map.get("emptyDeck"))
				.fireGemsDeck(FireGemsDeck.fromSub((Map<String, Object>) map.get("fireGemsDeck")))
				.market(market)
				.marketDeck(Deck.from((Map<String, Object>) map.get("marketDeck"), HeroRealmsCard.class))
				.sacrificePile(Deck.from((Map<String, Object>) map.get("sacrificePile"), HeroRealmsCard.class))
				.playerAreas(playerAreas)
				.build();
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
		
		@SuppressWarnings("unchecked")
		public static PlayerArea from(Map<String, Object> map) {
			
			List<HeroRealmsCard> hand = ((List<Map<String, Object>>) map.get("hand")).stream()
					.map(card -> Card.from(card, HeroRealmsCard.class))
					.collect(Collectors.toList());
			
			List<HeroRealmsCard> playedCards = ((List<Map<String, Object>>) map.get("playedCards")).stream()
					.map(card -> Card.from(card, HeroRealmsCard.class))
					.collect(Collectors.toList());
			
			List<HeroRealmsCard> champions = ((List<Map<String, Object>>) map.get("champions")).stream()
					.map(card -> Card.from(card, HeroRealmsCard.class))
					.collect(Collectors.toList());
			
			return PlayerArea.builder()
					.playerId((String) map.get("playerId"))
					.playerName((String) map.get("playerName"))
					.active((boolean) map.get("active"))
					.position(Long.valueOf((long) map.get("position")).intValue())
					.health(Long.valueOf((long) map.get("health")).intValue())
					.combat(Long.valueOf((long) map.get("combat")).intValue())
					.gold(Long.valueOf((long) map.get("gold")).intValue())
					.handSize(Long.valueOf((long) map.get("handSize")).intValue())
					.hand(hand)
					.playedCards(playedCards)
					.deck(Deck.from((Map<String, Object>) map.get("deck"), HeroRealmsCard.class))
					.discardPile(Deck.from((Map<String, Object>) map.get("discardPile"), HeroRealmsCard.class))
					.champions(champions)
					.build();
		}
	}
}
