package de.msz.games.games.herorealms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;

import de.msz.games.base.firebase.FirebaseService;
import de.msz.games.base.firebase.FirebaseService.FirestoreCollectionName;
import de.msz.games.games.Deck;
import de.msz.games.games.Game;
import de.msz.games.games.GameTable;
import de.msz.games.games.GameTableService;
import de.msz.games.games.herorealms.HeroRealmsTable.PlayerArea;
import de.msz.games.games.player.PlayerService;
import de.msz.games.games.player.PlayerService.Player;

@Service
public class HeroRealmsTableService extends GameTableService {
	
	private static final int HEALTH_START = 50;
	
	@Autowired
	private transient HeroRealmsService heroRealmsService;
	
	@Autowired
	public HeroRealmsTableService(FirebaseService firebaseService, PlayerService playerService) {
		super(firebaseService, playerService);
	}
	
	@Override
	public HeroRealmsTable loadTable(DocumentSnapshot gameDocument) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public HeroRealmsTable createTable(DocumentSnapshot gameDocument) {
		
		List<Player> players = getPlayers(gameDocument, true);
		
		HeroRealmsTable table = new HeroRealmsTable(players);
		
		table.setCardBack(Game.HERO_REALMS.getParameter().getImage());
		table.setEmptyDeck("extern/hero_realms/hero_realms_empty.jpg");
		table.setFireGemsDeck(heroRealmsService.createFireGemsDeck());
		table.setSacrificePile(new Deck<>());
		
		Deck<HeroRealmsCard> marketDeck = heroRealmsService.createMarketDeck();
		List<HeroRealmsCard> market = new ArrayList<>(5);
		for (int i=0; i<5; i++) {
			market.add(marketDeck.draw());
		}
		table.setMarketDeck(marketDeck);
		table.setMarket(market);
		
		int position = 0;
		table.setPlayerAreas(new HashMap<>());
		for (Player player : players) {
			PlayerArea area = new PlayerArea(player, position++);
			area.setActive(area.getPosition() == 0);
			area.setHealth(HEALTH_START);
			
			area.setDeck(heroRealmsService.createStartingDeck());
			area.setHand(new ArrayList<>());
			for (int i=0; i<getNoOfCardsStart(area.getPosition(), players.size()); i++) {
				area.getHand().add(area.getDeck().draw());
			}
			
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
				.sacrificePile(createHiddenDeck(table.getSacrificePile().getSize()))
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
			.discardPile(createHiddenDeck(sourceArea.getDiscardPile().getSize()))
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
