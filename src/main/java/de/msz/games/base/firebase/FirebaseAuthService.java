package de.msz.games.base.firebase;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
	
	private Map<String, String> verifiedUsers = new HashMap<>();
	
	@PostConstruct
	private void init() {
		firebaseAuth = firebaseService.getFirebaseAuth();
		firestore = firebaseService.getFirestore();
	}
	
	public String verifyIdToken(String idToken) {
		
		String verifiedUser = verifiedUsers.get(idToken);
		if (verifiedUser != null) {
			return verifiedUser;
		}
		
		FirebaseToken decodedToken;
		try {
			decodedToken = firebaseAuth.verifyIdToken(idToken);
			Optional<String> uid = Optional.ofNullable(getVerifiedUser(decodedToken));
			if (uid.isPresent()) {
				verifiedUsers.put(idToken, uid.get());
				return uid.get();
			}
			
			return null;
		} catch (@SuppressWarnings("unused") FirebaseAuthException e) {
			return null;
		}
	}
	
	private String getVerifiedUser(FirebaseToken token) {
		
		String uid = token.getUid();
		
		ApiFuture<DocumentSnapshot> future = firestore.collection(USERS).document(uid).get();
		DocumentSnapshot documentSnapshot;
		try {
			documentSnapshot = future.get();
			if (!documentSnapshot.exists()) {
				return null;
			}
		} catch (InterruptedException | ExecutionException e) {
			log.error("error on getting user data", e);
			return null;
		}
		
		if (Optional.ofNullable((Boolean)documentSnapshot.get(VERIFIED)).isPresent()) {
			return uid;
		}
		
		return null;
	}
}
