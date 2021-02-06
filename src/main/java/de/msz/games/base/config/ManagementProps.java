package de.msz.games.base.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "management")
@Data
public class ManagementProps {
	
	private EndpointsProps endpoints;
	
	@Data
	public static class EndpointsProps {
		
		private WebProps web;
	}
	
	@Data
	public static class WebProps {
		
		private CorsProps cors;
	}
	
	@Data
	public static class CorsProps {
		
		private String[] allowedOrigins;
		private String[] allowedMethods;
	}
}
