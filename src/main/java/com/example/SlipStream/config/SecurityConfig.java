package com.example.SlipStream.config;

import com.example.SlipStream.security.FirebaseTokenFilter; // Import FirebaseTokenFilter
import com.google.firebase.auth.FirebaseAuth; // Import FirebaseAuth
import com.example.SlipStream.repository.UserRepository; // Import UserRepository
import com.example.SlipStream.service.FirebaseUserDetailsService; // Import UserDetailsService
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory
import java.net.URLEncoder; // Import URLEncoder
import java.nio.charset.StandardCharsets; // Correct package for StandardCharsets
import jakarta.servlet.http.HttpServletResponse; // Import HttpServletResponse

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final FirebaseUserDetailsService firebaseUserDetailsService;
    private final FirebaseAuth firebaseAuth; // Needed to create the filter bean
    private final UserRepository userRepository; // Needed to create the filter bean

    @Autowired
    public SecurityConfig(FirebaseUserDetailsService firebaseUserDetailsService,
                          FirebaseAuth firebaseAuth, // Inject FirebaseAuth
                          UserRepository userRepository) { // Inject UserRepository
        this.firebaseUserDetailsService = firebaseUserDetailsService;
        this.firebaseAuth = firebaseAuth;
        this.userRepository = userRepository;
    }

    // Declare FirebaseTokenFilter as a Bean
    @Bean
    public FirebaseTokenFilter firebaseTokenFilter() {
        return new FirebaseTokenFilter(firebaseAuth, userRepository);
    }

    // Bean for SecurityContextRepository
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   SecurityContextRepository securityContextRepository,
                                                   FirebaseTokenFilter firebaseTokenFilter) throws Exception { // Inject the filter bean
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // Change session policy to allow session creation
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            // Explicitly set the SecurityContextRepository
            .securityContext(context -> context.securityContextRepository(securityContextRepository))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/", "/error", "/favicon.ico", "/*.png", "/*.gif", "/*.svg", "/*.jpg", "/*.html", "/*.css", "/*.js").permitAll()
                .requestMatchers("/login").permitAll()
                .requestMatchers("/api/auth/verify-token").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/view/pages/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/pages/**").permitAll()
                .requestMatchers("/dashboard").authenticated()
                .requestMatchers("/api/**").authenticated()
                .requestMatchers("/view/pages/create/**", "/view/pages/edit/**").authenticated()
                .anyRequest().authenticated()
            )
            // Configure UserDetailsService
            .userDetailsService(firebaseUserDetailsService)
            // Add the filter bean before UsernamePasswordAuthenticationFilter
            .addFilterBefore(firebaseTokenFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    String continueUrl = request.getRequestURI();
                    if (request.getQueryString() != null) {
                        continueUrl += "?" + request.getQueryString();
                    }
                    if (!request.getRequestURI().equals("/login")) {
                        logger.info("Authentication required for {}. Redirecting to login.", request.getRequestURI());
                        response.sendRedirect("/login?continue=" + URLEncoder.encode(continueUrl, StandardCharsets.UTF_8.toString()));
                    } else {
                        logger.warn("Authentication exception occurred on /login page itself: {}", authException.getMessage());
                    }
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    logger.warn("Access Denied for {}: {}", request.getRequestURI(), accessDeniedException.getMessage());
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
                })
            );

        return http.build();
    }

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class); // Add logger

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:8080", "http://127.0.0.1:8080"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
