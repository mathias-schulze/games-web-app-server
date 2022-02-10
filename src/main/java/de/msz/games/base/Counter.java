package de.msz.games.base;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;

import de.msz.games.base.firebase.FirebaseService;
import de.msz.games.base.firebase.FirebaseService.FirestoreCollectionName;
import lombok.Getter;

@Service
public class Counter {
	
	@Autowired
	private FirebaseService firebaseService;
	
	private Firestore firestore;
	
	@PostConstruct
	private void init() {
		firestore = firebaseService.getFirestore();
	}
	
	public long getNextValue(CounterName name) throws InterruptedException, ExecutionException {
		
	    DocumentReference counter = firestore.collection(FirestoreCollectionName.COUNTERS.getName())
	    		.document(name.getFirebaseCounterName());
	    
	    counter.update("value", FieldValue.increment(1)).get();
	    
	    return (long) Optional.ofNullable(counter.get().get().get("value")).orElse(0L);
	}
	
	public static enum CounterName {
		
		GAME("game_no");
		
		@Getter
		private String firebaseCounterName;
		
		private CounterName(String firebaseCounterName) {
			this.firebaseCounterName = firebaseCounterName;
		}
	}
}
