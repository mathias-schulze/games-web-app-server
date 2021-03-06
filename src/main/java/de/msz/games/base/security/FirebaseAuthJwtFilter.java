package de.msz.games.base.security;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import de.msz.games.base.UserService;
import de.msz.games.base.firebase.FirebaseAuthService;

@Component
public class FirebaseAuthJwtFilter extends OncePerRequestFilter {
	
	@Autowired
	private FirebaseAuthService firebaseAuthService;
	
	@Autowired
	private UserService userService;
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		
		String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (authHeader != null
				&& authHeader.startsWith("Bearer ")) {
			String uid = firebaseAuthService.verifyIdToken(authHeader.substring(7));
			if (uid != null) {
				userService.setCurrentUser(uid);
				filterChain.doFilter(request, response);
				return;
			}
		}
		
		response.setStatus(HttpStatus.FORBIDDEN.value());
	}
}
