package de.msz.games.games;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;

import de.msz.games.base.UserService;
import de.msz.games.base.firebase.FirebaseService;
import de.msz.games.base.firebase.FirebaseService.FirestoreCollectionName;
import de.msz.games.games.player.Player;
import de.msz.games.games.player.PlayerService;

public abstract class GameTableService {
	
	private GameTableCache cache = new GameTableCache();
	
	protected Firestore firestore;
	
	protected PlayerService playerService;
	
	private UserService userService;
	
	public GameTableService(FirebaseService firebaseService, PlayerService playerService, UserService userService) {
		
		this.firestore = firebaseService.getFirestore();
		this.playerService = playerService;
		this.userService = userService;
	}
	
	public abstract GameTable loadTable(DocumentReference gameDocumentRef)
			throws InterruptedException, ExecutionException;
	
	public abstract GameTable createTable(DocumentSnapshot gameDocument);
	
	public final void storeTable(String gameId, GameTable table) throws InterruptedException, ExecutionException {
		
		DocumentReference gameDocumentRef = 
				firestore.collection(FirestoreCollectionName.GAMES.getName()).document(gameId);
		
		storeTable(gameDocumentRef, table);
	}
	
	protected abstract void storeTable(DocumentReference gameDocumentRef, GameTable table)
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
		
		gameTable = loadGame(id);
		cache.put(id, gameTable);
		
		return gameTable;
	}
	
	private GameTable loadGame(String id) throws InterruptedException, ExecutionException {
		
		DocumentReference gameDocumentRef = 
				firestore.collection(FirestoreCollectionName.GAMES.getName()).document(id);
		
		return loadTable(gameDocumentRef);
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
	
	/**
	 * Checks if the user is the active player.
	 * 
	 * @throws IllegalCallerException if the user is not the active player
	 */
	public void checkIsPlayerActive(GameTable table) throws IllegalCallerException {
		
		if (!table.getActivePlayer().getId().equals(userService.getCurrentUser())) {
			throw new IllegalCallerException("user is not active");
		}
	}

}
