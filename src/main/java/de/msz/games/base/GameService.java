package de.msz.games.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;

import de.msz.games.base.Counter.CounterName;
import de.msz.games.base.firebase.FirebaseService;
import de.msz.games.base.firebase.FirebaseService.FirestoreCollectionName;
import lombok.Data;

@Component
public class GameService {
	
	@Autowired
	private FirebaseService firebaseService;
	
	private Firestore firestore;
	
	@Autowired
	private UserService userService;
	
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
					.whereIn("stage", Arrays.asList(Stage.NEW.name(), Stage.RUNNING.name()))
					.orderBy("created")
					.get();
		
		List<ActiveGame> activeGames = new ArrayList<>();
		
		for (DocumentSnapshot gameDocument : activeGamesFuture.get().getDocuments()) {
			String id = gameDocument.getId();
			Long no = gameDocument.getLong("no");
			Long created = gameDocument.getLong("created");
			Game game = Game.valueOf(gameDocument.getString("game"));
			
			activeGames.add(new ActiveGame(id, no, created, game.getParameter().getName()));
		}
		
		return activeGames;
	}
	
	@Data
	public static class ActiveGame {
		
		private final String id;
		private final Long no;
		private final Long created;
		private final String game;
	}
}
