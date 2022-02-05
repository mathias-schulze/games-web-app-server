package de.msz.games.games.herorealms;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
	
	private Deck<HeroRealmsCard> fireGemsDeck;
	
	private List<HeroRealmsCard> market;
	
	private Deck<HeroRealmsCard> marketDeck;
	
	private Deck<HeroRealmsCard> sacrificePile;
	
	private Map<String, PlayerArea> playerAreas;
	
	HeroRealmsTable(String gameId, List<Player> players) {
		super(gameId, players);
	}
	
	@SuppressWarnings("unchecked")
	public static HeroRealmsTable from(String gameId, Map<String, Object> map) {
		
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
				.gameId(gameId)
				.players(players)
				.activePlayer(Player.from((Map<String, Object>) map.get("activePlayer")))
				.round(((Long) Optional.ofNullable(map.get("round")).orElse(0)).intValue())
				.cardBack((String) map.get("cardBack"))
				.emptyDeck((String) map.get("emptyDeck"))
				.fireGemsDeck(Deck.from((Map<String, Object>) map.get("fireGemsDeck"), HeroRealmsCard.class))
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
		
		private HeroRealmsSpecialActionMode actionMode;
		
		@Builder.Default
		private boolean selected4Discard = false;
		
		private HeroRealmsBuyModeTarget buyModeTarget;
		
		private HeroRealmsBuyModeDiscount buyModeDiscount;
		
		private boolean killed;
		
		private int position;
		
		private HeroRealmsCharacterPack character;
		
		@Builder.Default
		private boolean characterRoundAbilityActive = true;
		
		private String characterRoundAbilityImage;
		
		private String characterOneTimeAbilityImage;
		
		private int health;
		
		private int combat;
		
		private int gold;
		
		private int handSize;
		
		private List<HeroRealmsCard> hand;
		
		private Deck<HeroRealmsCard> deck;
		
		private Deck<HeroRealmsCard> discardPile;
		
		private List<HeroRealmsCard> playedCards;
		
		private List<HeroRealmsCard> champions;
		
		private List<HeroRealmsDecision> decisions;
		
		@Builder.Default
		private int factionCountGuild = 0;
		
		@Builder.Default
		private int factionCountImperial = 0;
		
		@Builder.Default
		private int factionCountNecros = 0;
		
		@Builder.Default
		private int factionCountWild = 0;
		
		@Builder.Default
		private boolean blessed = false;
		
		@Builder.Default
		private boolean blessedThisTurn = false;
		
		private List<HeroRealmsCard> rangerTrackCards;
		
		private int rangerTrackDiscardCount;
		
		public PlayerArea(Player player, int position) {
			this.playerId = player.getId();
			this.playerName = player.getName();
			this.position = position;
		}
		
		@SuppressWarnings("unchecked")
		public static PlayerArea from(Map<String, Object> map) {
			
			HeroRealmsCharacterPack character = Optional.ofNullable((String) map.get("character"))
					.map(characterString -> HeroRealmsCharacterPack.valueOf(characterString))
					.orElse(null);
			
			HeroRealmsSpecialActionMode actionMode = Optional.ofNullable((String) map.get("actionMode"))
					.map(actionModeString -> HeroRealmsSpecialActionMode.valueOf(actionModeString))
					.orElse(null);
			
			HeroRealmsBuyModeTarget buyModeTarget = Optional.ofNullable((String) map.get("buyModeTarget"))
					.map(buyModeTargetString -> HeroRealmsBuyModeTarget.valueOf(buyModeTargetString))
					.orElse(null);
			
			HeroRealmsBuyModeDiscount buyModeDiscount = Optional.ofNullable((String) map.get("buyModeDiscount"))
					.map(buyModeDiscountString -> HeroRealmsBuyModeDiscount.valueOf(buyModeDiscountString))
					.orElse(null);
			
			List<HeroRealmsCard> hand = ((List<Map<String, Object>>) map.get("hand")).stream()
					.map(card -> Card.from(card, HeroRealmsCard.class))
					.collect(Collectors.toList());
			
			List<HeroRealmsCard> playedCards = ((List<Map<String, Object>>) map.get("playedCards")).stream()
					.map(card -> Card.from(card, HeroRealmsCard.class))
					.collect(Collectors.toList());
			
			List<HeroRealmsCard> champions = ((List<Map<String, Object>>) map.get("champions")).stream()
					.map(card -> Card.from(card, HeroRealmsCard.class))
					.collect(Collectors.toList());
			
			List<HeroRealmsDecision> decisions = Optional.ofNullable((List<Map<String, Object>>) map.get("decisions"))
			 		.orElse(Collections.emptyList()).stream()
					.map(decision -> HeroRealmsDecision.from(decision))
					.collect(Collectors.toList());
			
			List<HeroRealmsCard> rangerTrackCards = Optional.ofNullable((List<Map<String, Object>>) map.get("rangerTrackCards"))
			 		.orElse(Collections.emptyList()).stream()
					.map(card -> Card.from(card, HeroRealmsCard.class))
					.collect(Collectors.toList());
			
			return PlayerArea.builder()
					.playerId((String) map.get("playerId"))
					.playerName((String) map.get("playerName"))
					.character(character)
					.characterRoundAbilityActive(Optional.ofNullable((Boolean) map.get("characterRoundAbilityActive")).orElse(false))
					.characterRoundAbilityImage((String) map.get("characterRoundAbilityImage"))
					.characterOneTimeAbilityImage((String) map.get("characterOneTimeAbilityImage"))
					.active((boolean) map.get("active"))
					.actionMode(actionMode)
					.selected4Discard(Optional.ofNullable((Boolean) map.get("selected4Discard")).orElse(false))
					.buyModeTarget(buyModeTarget)
					.buyModeDiscount(buyModeDiscount)
					.killed((boolean) map.get("killed"))
					.position(((Long) map.get("position")).intValue())
					.health(((Long) map.get("health")).intValue())
					.combat(((Long) map.get("combat")).intValue())
					.gold(((Long) map.get("gold")).intValue())
					.handSize(((Long) map.get("handSize")).intValue())
					.hand(hand)
					.playedCards(playedCards)
					.deck(Deck.from((Map<String, Object>) map.get("deck"), HeroRealmsCard.class))
					.discardPile(Deck.from((Map<String, Object>) map.get("discardPile"), HeroRealmsCard.class))
					.champions(champions)
					.decisions(decisions)
					.factionCountGuild(((Long) map.get("factionCountGuild")).intValue())
					.factionCountImperial(((Long) map.get("factionCountImperial")).intValue())
					.factionCountNecros(((Long) map.get("factionCountNecros")).intValue())
					.factionCountWild(((Long) map.get("factionCountWild")).intValue())
					.blessed(Optional.ofNullable((Boolean) map.get("blessed")).orElse(false))
					.blessedThisTurn(Optional.ofNullable((Boolean) map.get("blessedThisTurn")).orElse(false))
					.rangerTrackCards(rangerTrackCards)
					.rangerTrackDiscardCount((Optional.ofNullable((Long) map.get("rangerTrackDiscardCount")).orElse(0L)).intValue())
					.build();
		}
	}
}
