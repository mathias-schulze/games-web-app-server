package de.msz.games.base.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import de.msz.games.base.config.ManagementProps;

@Configuration
public class WebConfig implements WebMvcConfigurer {
	
	@Autowired
	private ManagementProps managementProps;
	
	/**
	 * @see https://stackoverflow.com/a/43559288
	 * @see WebSecurityConfig
	 */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
    	
        registry.addMapping("/**")
        	.allowedMethods(managementProps.getEndpoints().getWeb().getCors().getAllowedMethods());
    }
}