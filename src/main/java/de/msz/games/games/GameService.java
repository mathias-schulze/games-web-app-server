package de.msz.games.games;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
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
import de.msz.games.base.UserService;
import de.msz.games.base.firebase.FirebaseService;
import de.msz.games.base.firebase.FirebaseService.FirestoreCollectionName;
import de.msz.games.games.player.Player;
import de.msz.games.games.player.PlayerService;
import lombok.Builder;
import lombok.Data;

@Component
public class GameService {
	
	@Autowired
	private FirebaseService firebaseService;
	
	private Firestore firestore;
	
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
	
	public List<ActiveGame> getActiveGames() throws InterruptedException, ExecutionException {
		
		ApiFuture<QuerySnapshot> activeGamesFuture = 
				firestore.collection(FirestoreCollectionName.GAMES.getName())
					.orderBy("created")
					.get();
		
		return activeGamesFuture.get().getDocuments().stream().map(gameDocument -> {
			
			@SuppressWarnings("unchecked")
			List<Player> players = playerService.getPlayers((List<String>) gameDocument.get("players"));
			
			return ActiveGame.builder()
					.id(gameDocument.getId())
					.no(gameDocument.getLong("no"))
					.created(gameDocument.getLong("created"))
					.game(Game.valueOf(gameDocument.getString("game")).getParameter().getName())
					.players(players)
					.build();
			
		}).collect(Collectors.toList());
	}
	
	@Data
	@Builder
	public static class ActiveGame {
		
		private final String id;
		private final Long no;
		private final Long created;
		private final String game;
		private final List<Player> players;
	}
	
	public void joinGame(String id) throws InterruptedException, ExecutionException {
		
		firestore.collection(FirestoreCollectionName.GAMES.getName()).document(id)
			.update("players", FieldValue.arrayUnion(userService.getCurrentUser()))
			.get();
	}
	
	public void startGame(String id) throws InterruptedException, ExecutionException {
		
		DocumentReference gameDocumentRef = firestore.collection(FirestoreCollectionName.GAMES.getName()).document(id);
		DocumentSnapshot gameDocument = gameDocumentRef.get().get();
		
		Game game = Game.valueOf(gameDocument.getString("game"));
		
		GameTableService gameTableService = gameTableServiceFactory.getService(game);
		GameTable table = gameTableService.createTable(gameDocument);
		gameTableService.storeTable(gameDocumentRef, table);
		
		gameDocumentRef.update("stage", Stage.RUNNING.name()).get();
	}
	
	public void endGame(String id) throws InterruptedException, ExecutionException {
		
		DocumentReference gameDocumentRef = firestore.collection(FirestoreCollectionName.GAMES.getName()).document(id);
		gameDocumentRef.update("stage", Stage.FINISHED.name()).get();
	}
}
