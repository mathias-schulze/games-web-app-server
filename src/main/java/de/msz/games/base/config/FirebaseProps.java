package de.msz.games.base.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "firebase")
@Data
public class FirebaseProps {
	
	private FirestoreProps firestore;
	
	@Data
	public static class FirestoreProps {
		
		private String databaseUrl;
	}
}
