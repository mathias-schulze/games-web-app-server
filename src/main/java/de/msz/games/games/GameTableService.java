package de.msz.games.games;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;

import de.msz.games.base.firebase.FirebaseService;
import de.msz.games.base.firebase.FirebaseService.FirestoreCollectionName;
import de.msz.games.games.player.PlayerService;
import de.msz.games.games.player.PlayerService.Player;

public abstract class GameTableService {
	
	private GameTableCache cache = new GameTableCache();
	
	protected Firestore firestore;
	
	protected PlayerService playerService;
	
	public GameTableService(FirebaseService firebaseService, PlayerService playerService) {
		
		this.firestore = firebaseService.getFirestore();
		this.playerService = playerService;
	}
	
	public abstract GameTable loadTable(DocumentSnapshot gameDocument);
	
	public abstract GameTable createTable(DocumentSnapshot gameDocument);
	
	public abstract void storeTable(DocumentReference gameDocumentRef, GameTable table)
			throws InterruptedException, ExecutionException;
	
	protected List<Player> getPlayers(DocumentSnapshot gameDocument, boolean shuffle) {
		
		List<Player> players = new ArrayList<>();
		
		@SuppressWarnings("unchecked")
		List<String> playerIds = (List<String>) gameDocument.get("players");
		players = this.playerService.getPlayers(playerIds);
		
		if (shuffle) {
			Collections.shuffle(players);
		}
		
		return players;
	}
	
	public GameTable getGameTable(String id) throws InterruptedException, ExecutionException {
		
		GameTable gameTable = cache.get(id);
		if (gameTable != null) {
			return gameTable;
		}
		
		return loadGame(id);
	}
	
	private GameTable loadGame(String id) throws InterruptedException, ExecutionException {
		
		DocumentSnapshot gameDocument = 
				firestore.collection(FirestoreCollectionName.GAMES.getName()).document(id).get().get();
		
		return null;
	}
	
	private static class GameTableCache extends LinkedHashMap<String, GameTable> {
		
		private static final long serialVersionUID = 1L;
		
		private static final int CACHE_SIZE = 5;
		
		private GameTableCache() {
			super(16, 0.75f, true);
		}
		
		@Override
		protected boolean removeEldestEntry(java.util.Map.Entry<String, GameTable> eldest) {
			return size() > CACHE_SIZE;
		}
	}
}