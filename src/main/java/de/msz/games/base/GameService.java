package de.msz.games.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;

import de.msz.games.base.firebase.FirebaseService;
import de.msz.games.base.firebase.FirebaseService.FirestoreCollectionName;

@Component
public class GameService {
	
	@Autowired
	private FirebaseService firebaseService;
	
	private Firestore firestore;
	
	@Autowired
	private UserService userService;
	
	@PostConstruct
	private void init() {
		firestore = firebaseService.getFirestore();
	}
	
	public String createNewGame(Game game) throws InterruptedException, ExecutionException {
		
		Map<String, Object> newGameValues = new HashMap<>();
		newGameValues.put("game", "" + game.getParameter().getId());
		newGameValues.put("stage", "" + Stage.NEW.name());
		
		List<String> players = new ArrayList<>(1);
		players.add(userService.getCurrentUser());
		newGameValues.put("players", players);
		
		ApiFuture<DocumentReference> newGameFuture = 
				firestore.collection(FirestoreCollectionName.GAMES.getName()).add(newGameValues);
		
		return newGameFuture.get().getId();
	}
}
