package de.msz.games.games.player;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;

import de.msz.games.base.firebase.FirebaseService;
import de.msz.games.base.firebase.FirebaseService.FirestoreCollectionName;

@Component
public class PlayerService {
	
	@Autowired
	private FirebaseService firebaseService;
	
	private Firestore firestore;
	
	@PostConstruct
	private void init() {
		firestore = firebaseService.getFirestore();
	}
	
	public List<Player> getPlayers(List<String> ids) {
		
		return ids.stream()
				.map(playerId -> getPlayer(playerId)).collect(Collectors.toList());
		
	}
	
	private Player getPlayer(String id) {
		
		ApiFuture<DocumentSnapshot> playersFuture = 
				firestore.collection(FirestoreCollectionName.USERS.getName()).document(id).get();
		
		try {
			return Player.builder()
					.id(id)
					.name(playersFuture.get().getString("name"))
					.build();
		} catch (InterruptedException | ExecutionException e) {
			Thread.currentThread().interrupt();
			
			return Player.builder()
					.id(id)
					.name(e.getMessage())
					.build();
		}
	}
}
