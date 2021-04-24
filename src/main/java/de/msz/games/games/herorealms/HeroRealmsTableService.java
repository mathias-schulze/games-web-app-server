package de.msz.games.games.herorealms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;

import de.msz.games.base.UserService;
import de.msz.games.base.firebase.FirebaseService;
import de.msz.games.base.firebase.FirebaseService.FirestoreCollectionName;
import de.msz.games.games.Deck;
import de.msz.games.games.Game;
import de.msz.games.games.GameTable;
import de.msz.games.games.GameTableService;
import de.msz.games.games.herorealms.HeroRealmsTable.PlayerArea;
import de.msz.games.games.player.Player;
import de.msz.games.games.player.PlayerService;

@Service
public class HeroRealmsTableService extends GameTableService {
	
	private static final int HEALTH_START = 50;
	
	@Autowired
	private transient HeroRealmsService heroRealmsService;
	
	@Autowired
	public HeroRealmsTableService(FirebaseService firebaseService, PlayerService playerService, UserService userService) {
		super(firebaseService, playerService, userService);
	}
	
	@Override
	public HeroRealmsTable loadTable(DocumentReference gameDocumentRef) throws InterruptedException, ExecutionException {
		
		DocumentSnapshot gameDocument = 
				gameDocumentRef.collection(FirestoreCollectionName.TABLE_VIEWS.getName()).document("full").get().get();
		
		Map<String, Object> data = gameDocument.getData();
		
		return HeroRealmsTable.from(gameDocumentRef.getId(), data);
	}
	
	@Override
	public HeroRealmsTable createTable(DocumentSnapshot gameDocument) {
		
		boolean withCharacterPacks = (Game.valueOf(gameDocument.getString("game")) == Game.HERO_REALMS_CHARACTER_PACKS);
		
		List<Player> players = getPlayers(gameDocument, true);
		
		HeroRealmsTable table = new HeroRealmsTable(gameDocument.getId(), players);
		
		table.setCardBack(Game.HERO_REALMS.getParameter().getImage());
		table.setEmptyDeck("extern/hero_realms/hero_realms_empty.jpg");
		table.setFireGemsDeck(heroRealmsService.createFireGemsDeck());
		table.setSacrificePile(new Deck<>());
		
		Deck<HeroRealmsCard> marketDeck = heroRealmsService.createMarketDeck();
		table.setMarketDeck(marketDeck);
		table.setMarket(marketDeck.draw(5));
		
		List<HeroRealmsCharacterPack> characterPacks = 
				withCharacterPacks ? HeroRealmsCharacterPack.getRandomList() : Collections.emptyList();
		
		int position = 0;
		table.setPlayerAreas(new HashMap<>());
		for (Player player : players) {
			PlayerArea area = new PlayerArea(player, position++);
			area.setActive(area.getPosition() == 0);
			area.setHealth(HEALTH_START);
			
			if (withCharacterPacks) {
				HeroRealmsCharacterPack character = characterPacks.remove(0);
				area.setCharacter(character);
				area.setCharacterRoundAbilityActive(true);
				area.setCharacterRoundAbilityImage(character.getRoundAbilityImage());
				area.setCharacterOneTimeAbilityImage(character.getOneTimeAbilityImage());
			}
			
			area.setDeck(heroRealmsService.createStartingDeck(area.getCharacter()));
			area.setHandSize(getNoOfCardsStart(area.getPosition(), players.size()));
			area.setHand(area.getDeck().draw(area.getHandSize()));
			area.setDiscardPile(new Deck<>());
			area.setPlayedCards(new ArrayList<>());
			area.setChampions(new ArrayList<>());
			
			table.getPlayerAreas().put(player.getId(), area);
		}
		
		return table;
	}
	
	@Override
	public void storeTable(DocumentReference gameDocumentRef, GameTable table)
			throws InterruptedException, ExecutionException {
		
		HeroRealmsTable heroRealmsTable = (HeroRealmsTable) table;
		
		CollectionReference tableViews = gameDocumentRef.collection(FirestoreCollectionName.TABLE_VIEWS.getName());
		tableViews.document("full").set(heroRealmsTable).get();
		
		for (Player player : table.getPlayers()) {
			storePlayerView(tableViews, player.getId(), heroRealmsTable);
		}
	}
	
	private static void storePlayerView(CollectionReference tableViews, String playerId, HeroRealmsTable table)
			throws InterruptedException, ExecutionException {
		
		HeroRealmsTablePlayerView tableCopy = HeroRealmsTablePlayerView.builder()
				.players(table.getPlayers())
				.activePlayer(table.getActivePlayer())
				.cardBack(table.getCardBack())
				.emptyDeck(table.getEmptyDeck())
				.fireGemsDeck(table.getFireGemsDeck())
				.market(table.getMarket())
				.marketDeck(createHiddenDeck(table.getMarketDeck().getSize()))
				.sacrificePile(table.getSacrificePile())
				.build();
		
		PlayerArea sourceArea = table.getPlayerAreas().get(playerId);
		tableCopy.setOwnPlayerArea(createPlayerAreaView(sourceArea, true));
		
		tableCopy.setOtherPlayerAreas(table.getOtherPlayersSorted(sourceArea.getPosition()).stream()
			.map(player -> {
				PlayerArea source = table.getPlayerAreas().get(player.getId());
				return createPlayerAreaView(source, false);
			})
			.collect(Collectors.toList())
		);
		
		tableViews.document(playerId).set(tableCopy).get();
	}
	
	private static PlayerArea createPlayerAreaView(PlayerArea sourceArea, boolean own) {
		
		return sourceArea.toBuilder()
			.active(sourceArea.isActive())
			.hand(own ? sourceArea.getHand() : Collections.emptyList())
			.handSize(sourceArea.getHand().size())
			.deck(createHiddenDeck(sourceArea.getDeck().getSize()))
			.discardPile(sourceArea.getDiscardPile())
			.build();
	}
	
	private static Deck<HeroRealmsCard> createHiddenDeck(int size) {
		Deck<HeroRealmsCard> deck = new Deck<>();
		deck.setSize(size);
		return deck;
	}
	
	private static int getNoOfCardsStart(int position, int noOfPlayers) {
		
		if (position == 0) {
			return 3;
		}
		
		if (position == 1 && noOfPlayers > 2) {
			return  4;
		}
		
		return 5;
	}
}
