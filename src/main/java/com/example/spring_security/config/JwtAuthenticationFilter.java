package com.example.spring_security.config;

import com.example.spring_security.service.JwtService;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Remove @Component annotation - we'll create this as a bean in SecurityConfig
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // implementing a logger
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization"); // Fixed typo in "Authorization"
        final String jwt;
        final String username;

        // Log incoming request with path and method
        logger.debug("Processing request: {} {}", request.getMethod(), request.getRequestURI());

        // Check if Authorization header exists and starts with "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")){ // Fixed typo in "Bearer"
            logger.debug("No JWT token found in request headers");
            filterChain.doFilter(request, response);
            return;
        }

        // Extract JWT token
        jwt = authHeader.substring(7);
        logger.debug("JWT token found in request");

        try {
            // Extract username from token
            username = jwtService.extractUserName(jwt);
            logger.debug("Username extrated from JWT: {}", username);

            // If username exists and no authentication exists yet
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Load user details
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                logger.debug("User details loaded for: {}", username);

                // Validate token
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // Create authentication token
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

                    // Add request details
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // Set authentication in SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.info("Authentication successful for user: {}", username);
                    logger.debug("User roles: {}", userDetails.getAuthorities());
                } else {
                    logger.warn("Invalid JWT token for user: {}", username);
                    throw new RuntimeException("Invalid token");
                }
            }
        } catch (Exception e) {
            // Log exception but don't throw it
            logger.error("JWT token validation failed: {}",  e.getMessage(), e);

            // Send 401 Unauthorized response
            // instead of a blank response it will return an expired JSON response
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Token expired or invalid\"}");
            response.getWriter().flush();
            return;
        }

        // continue filterchain
        filterChain.doFilter(request, response);
    }
}