package de.msz.games.base.security;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.google.common.collect.ImmutableList;

import de.msz.games.base.config.ManagementProps;
import de.msz.games.base.config.ManagementProps.CorsProps;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
	
	@Autowired
	private ManagementProps managementProps;
	
	@Override
    protected void configure(HttpSecurity http) throws Exception {
		
		http.cors();
    }
	
	/**
	 * @see https://stackoverflow.com/a/43559288
	 * @see WebConfig
	 */
	@Bean
    public CorsConfigurationSource corsConfigurationSource() {
		
		CorsProps corsProps = managementProps.getEndpoints().getWeb().getCors();
		
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(corsProps.getAllowedOrigins()));
        configuration.setAllowedMethods(Arrays.asList(corsProps.getAllowedMethods()));
        configuration.setAllowCredentials(true);
        configuration.setAllowedHeaders(ImmutableList.of(
        		HttpHeaders.AUTHORIZATION, HttpHeaders.CACHE_CONTROL, HttpHeaders.CONTENT_TYPE));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
