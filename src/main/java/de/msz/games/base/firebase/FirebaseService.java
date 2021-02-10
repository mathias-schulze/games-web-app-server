package de.msz.games.base.firebase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;

import de.msz.games.base.config.FirebaseProps;
import lombok.Getter;

@Service
public class FirebaseService {
	
	@Autowired
	private FirebaseProps firebaseProps;
	
	private FirebaseApp firebaseApp;
	
	@Getter
	private FirebaseAuth firebaseAuth;
	
	@Getter
	private Firestore firestore;
	
	@PostConstruct
	private void init() throws IOException {
		
		GoogleCredentials credentials = GoogleCredentials.fromStream(getServiceAccount());
		FirebaseOptions options = FirebaseOptions.builder()
			    .setCredentials(credentials)
			    .setDatabaseUrl(firebaseProps.getFirestore().getDatabaseUrl())
			    .build();
		
		firebaseApp = FirebaseApp.initializeApp(options);
		
		firebaseAuth = FirebaseAuth.getInstance();
		firestore = FirestoreClient.getFirestore();
	}
	
	@PreDestroy
	private void shutdown() throws Exception {
		firebaseApp.delete();
		firestore.close();
	}
	
	/**
	 * @return Google Service Account from local resource (development) or environment variable (production)
	 */
	private InputStream getServiceAccount() {
		
		InputStream serviceAccount = getClass().getClassLoader().getResourceAsStream("serviceAccount.json");
		
		if (serviceAccount == null) {
			String serviceAccountJson = System.getenv("SERVICE_ACCOUNT_JSON");
			serviceAccount = new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8));
		}
		
		return serviceAccount;
	}
	
	public static enum FirestoreCollectionName {
		
		USERS("users"),
		GAMES("games"),
		COUNTERS("counters");
		
		@Getter
		private final String name;
		
		private FirestoreCollectionName(String name) {
			this.name = name;
		}
	}
}
