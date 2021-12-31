package de.msz.games.games;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;

import de.msz.games.base.Counter;
import de.msz.games.base.Counter.CounterName;
import de.msz.games.base.NotificationService;
import de.msz.games.base.NotificationService.NotificationType;
import de.msz.games.base.UserService;
import de.msz.games.base.firebase.FirebaseService;
import de.msz.games.base.firebase.FirebaseService.FirestoreCollectionName;
import de.msz.games.games.player.Player;
import de.msz.games.games.player.PlayerService;
import lombok.Builder;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class GameService {
	
	@Autowired
	private FirebaseService firebaseService;
	
	private Firestore firestore;
	
	@Autowired
	private NotificationService notificationService;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private PlayerService playerService;
	
	@Autowired
	private GameTableServiceFactory gameTableServiceFactory;
	
	@Autowired
	private Counter counter;
	
	@PostConstruct
	private void init() {
		firestore = firebaseService.getFirestore();
	}
	
	public String createNewGame(Game game) throws InterruptedException, ExecutionException {
		
		Map<String, Object> newGameValues = new HashMap<>();
		newGameValues.put("no", counter.getNextValue(CounterName.GAME));
		newGameValues.put("created", System.currentTimeMillis());
		newGameValues.put("game", "" + game.getParameter().getId());
		newGameValues.put("stage", "" + Stage.NEW.name());
		
		List<String> players = new ArrayList<>(1);
		players.add(userService.getCurrentUser());
		newGameValues.put("players", players);
		
		ApiFuture<DocumentReference> newGameFuture = 
				firestore.collection(FirestoreCollectionName.GAMES.getName()).add(newGameValues);
		
		return newGameFuture.get().getId();
	}
	
	public List<Table> getTables() throws InterruptedException, ExecutionException {
		
		ApiFuture<QuerySnapshot> tablesFuture = 
				firestore.collection(FirestoreCollectionName.GAMES.getName())
					.orderBy("created")
					.get();
		
		return tablesFuture.get().getDocuments().stream().map(gameDocument -> {
			
			@SuppressWarnings("unchecked")
			List<Player> players = playerService.getPlayers((List<String>) gameDocument.get("players"));
			
			return Table.builder()
					.id(gameDocument.getId())
					.no(gameDocument.getLong("no"))
					.created(gameDocument.getLong("created"))
					.game(Game.valueOf(gameDocument.getString("game")).getParameter().getName())
					.stage(Stage.valueOf(gameDocument.getString("stage")))
					.winner(gameDocument.getString("winner"))
					.players(players)
					.build();
			
		}).collect(Collectors.toList());
	}
	
	@Data
	@Builder
	public static class Table {
		
		private final String id;
		private final Long no;
		private final Long created;
		private final String game;
		private final Stage stage;
		private final List<Player> players;
		private final String winner;
	}
	
	public void joinGame(String id) throws InterruptedException, ExecutionException {
		
		firestore.collection(FirestoreCollectionName.GAMES.getName()).document(id)
			.update("players", FieldValue.arrayUnion(userService.getCurrentUser()))
			.get();
	}
	
	private class StartGameLockLruCache {
		
		private final Map<String, Lock> locks = new HashMap<>();
		
		private Lock getLock(String id) {
			
			cleanup();
			
			return locks.computeIfAbsent(id, k -> new ReentrantLock());
		}
		
		private void cleanup() {
			
			for (Iterator<Lock> it = locks.values().iterator(); it.hasNext();) {
				Lock lock = it.next();
				if (lock.tryLock()) {
					try {
						it.remove();
					} finally {
						lock.unlock();
					}
				}
			}
		}
	}
	
	private final StartGameLockLruCache startGameLockCache = new StartGameLockLruCache();
	
	public void startGame(String id) throws InterruptedException, ExecutionException {
		
		Lock lock = startGameLockCache.getLock(id);
		lock.lock();
		try {
			DocumentReference gameDocumentRef = firestore.collection(FirestoreCollectionName.GAMES.getName()).document(id);
			DocumentSnapshot gameDocument = gameDocumentRef.get().get();
			
			String stage = gameDocument.getString("stage");
			if (!stage.equals(Stage.NEW.name())) {
				notificationService.addNotification(NotificationType.ERROR, "Spiel wurde bereits gestartet!");
			} else {
				Game game = Game.valueOf(gameDocument.getString("game"));
				
				GameTableService gameTableService = gameTableServiceFactory.getService(game);
				GameTable table = gameTableService.createTable(gameDocument);
				gameTableService.storeTable(gameDocumentRef, table);
				
				gameDocumentRef.update("stage", Stage.RUNNING.name()).get();
			}
		} finally {
			lock.unlock();
		}
	}
	
	public void endGame(String id, String winner) throws InterruptedException, ExecutionException {
		
		DocumentReference gameDocumentRef = firestore.collection(FirestoreCollectionName.GAMES.getName()).document(id);
		gameDocumentRef.update(
				"stage", Stage.FINISHED.name(), 
				"winner", winner)
		.get();
	}
	
	public void deleteGame(String id) throws InterruptedException, ExecutionException {
		
		DocumentReference gameDocumentRef = firestore.collection(FirestoreCollectionName.GAMES.getName()).document(id);
		gameDocumentRef.delete().get();
		gameDocumentRef.listCollections().forEach(collection -> {
			try {
				collection.get().get().getDocuments().forEach(document -> document.getReference().delete());
			} catch (InterruptedException | ExecutionException e) {
				log.error("error on deleting game data", e);
				Thread.currentThread().interrupt();
			}
		});
	}
}
