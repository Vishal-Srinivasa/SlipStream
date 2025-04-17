package com.example.SlipStream.security;

import com.example.SlipStream.model.User;
import com.example.SlipStream.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

public class FirebaseTokenFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseTokenFilter.class);
    private final FirebaseAuth firebaseAuth;
    private final UserRepository userRepository;

    public FirebaseTokenFilter(FirebaseAuth firebaseAuth, UserRepository userRepository) {
        this.firebaseAuth = firebaseAuth;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // Check if user is already authenticated via session
        Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
        if (existingAuth != null && existingAuth.isAuthenticated() && !"anonymousUser".equals(existingAuth.getPrincipal().toString())) {
            logger.trace("User already authenticated via session ({}). Skipping Firebase token check for {}", existingAuth.getName(), request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        // Skip filter for specific paths like login and token verification
        String path = request.getRequestURI();
        if (path.equals("/api/auth/verify-token") || path.equals("/login") || path.equals("/")) {
            logger.trace("Skipping FirebaseTokenFilter for path: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        logger.trace("Attempting Firebase token verification for path: {}", path);
        String authorizationHeader = request.getHeader("Authorization");
        String idToken = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            idToken = authorizationHeader.substring(7);
            logger.trace("Found Bearer token in Authorization header.");
        } else {
            logger.trace("No Bearer token found in Authorization header for path: {}", path);
        }

        FirebaseToken decodedToken = null;
        if (idToken != null) {
            try {
                decodedToken = firebaseAuth.verifyIdToken(idToken);
                logger.trace("Firebase token verified successfully for UID: {}", decodedToken.getUid());
            } catch (FirebaseAuthException e) {
                logger.warn("Firebase token verification failed for path {}: {}", path, e.getMessage());
            }
        }

        if (decodedToken != null) {
            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();
            logger.debug("Token details - UID: {}, Email: {}", uid, email);

            if (email != null && !email.isEmpty()) {
                logger.debug("Token verified in filter. Setting SecurityContext for user: {}", email);

                UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                        email,
                        "",
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                );

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.trace("Set SecurityContext via token for {}", email);

            } else {
                logger.warn("Firebase token for UID '{}' does not contain a valid email. Cannot set SecurityContext.", uid);
            }
        } else {
            logger.trace("No valid decoded token found for path: {}", path);
        }

        filterChain.doFilter(request, response);
    }

    public void saveUserIfNotExists(String email) {
        if (email == null || email.isEmpty()) {
            logger.warn("saveUserIfNotExists called with null or empty email.");
            return;
        }
        logger.debug("Inside saveUserIfNotExists for Email: {}", email);
        try {
            logger.debug("Checking if user exists by email: {}", email);
            User existingUser = userRepository.getUserByEmail(email);
            if (existingUser == null) {
                logger.info("User with email {} not found. Creating new user entry.", email);
                User newUser = new User(email);
                userRepository.saveUser(newUser);
                logger.info("Successfully initiated save for new user: {}", email);
            } else {
                logger.debug("User with email {} already exists in Firestore. No action needed.", email);
            }
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error accessing or saving user data in Firestore for email {}: {}", email, e.getMessage(), e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Unexpected error during saveUserIfNotExists for email {}: {}", email, e.getMessage(), e);
        }
    }
}
