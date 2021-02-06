package de.msz.games.base.firebase;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

import de.msz.games.base.firebase.FirebaseService.FirestoreCollectionName;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class FirebaseAuthService {
	
	private static final String USERS = FirestoreCollectionName.USERS.getName();
	private static final String VERIFIED = "verified";
	
	@Autowired
	private FirebaseService firebaseService;
	
	private FirebaseAuth firebaseAuth;
	
	private Firestore firestore;
	
	private Set<String> verifiedUsers = new HashSet<>();
	
	@PostConstruct
	private void init() {
		firebaseAuth = firebaseService.getFirebaseAuth();
		firestore = firebaseService.getFirestore();
	}
	
	public boolean verifyIdToken(String idToken) {
		
		if (verifiedUsers.contains(idToken)) {
			return true;
		}
		
		FirebaseToken decodedToken;
		try {
			decodedToken = firebaseAuth.verifyIdToken(idToken);
			if (isUserVerified(decodedToken) ) {
				verifiedUsers.add(idToken);
				return true;
			} else {
				return false;
			}
		} catch (FirebaseAuthException e) {
			return false;
		}
	}
	
	private boolean isUserVerified(FirebaseToken token) {
		
		ApiFuture<DocumentSnapshot> future = firestore.collection(USERS).document(token.getUid()).get();
		DocumentSnapshot documentSnapshot;
		try {
			documentSnapshot = future.get();
			if (!documentSnapshot.exists()) {
				return false;
			}
		} catch (InterruptedException | ExecutionException e) {
			log.error("error on getting user data", e);
			return false;
		}
		
		return Optional.ofNullable((Boolean)documentSnapshot.get(VERIFIED)).orElse(false);
	}
}
