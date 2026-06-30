package com.github.dghng36.eauction.infra.config.security;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.github.dghng36.eauction.core.utils.ConstantsUtils;
import com.github.dghng36.eauction.modules.identity.auth.service.InvalidatedTokenProcessor;
import com.github.dghng36.eauction.modules.identity.auth.service.jwt.JwtAdapter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Component
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter{
    InvalidatedTokenProcessor invalidatedTokenProcessor;

    JwtAdapter jwtAdapter;
    
    HandlerExceptionResolver resolver;

    public JwtAuthFilter(
            InvalidatedTokenProcessor invalidatedTokenProcessor,
            JwtAdapter jwtAdapter,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.invalidatedTokenProcessor = invalidatedTokenProcessor;
        this.jwtAdapter = jwtAdapter;
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException 
    {
        try {
            // Get access token from header
            String authHeader = request.getHeader(
                ConstantsUtils.AuthenticationConstants.AUTH_HEADER
            );

            if (authHeader == null || !authHeader.startsWith(ConstantsUtils.AuthenticationConstants.TOKEN_PREFIX)) {
                filterChain.doFilter(request, response);
                return;
            }

            String accessToken = authHeader.substring(
                ConstantsUtils.AuthenticationConstants.TOKEN_PREFIX.length()
            );

            // Validate access token
            if (!jwtAdapter.validateAccessToken(accessToken)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // Check if the token is invalidated
            if (invalidatedTokenProcessor.isTokenInvalidated(
                jwtAdapter.extractData(accessToken, "jti"))
            ) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // Get user details from access token
            String userId = jwtAdapter.extractData(accessToken, "userId");
            String username = jwtAdapter.extractData(accessToken, "username");
            String role = jwtAdapter.extractData(accessToken, "role");

            // Create authorities list
            var authorities = AuthorityUtils.createAuthorityList(role);

            // Set authentication in security context
            Map<String, Object> principal = Map.of(
                "userId", userId, 
                "username", username, 
                "role", role);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch(IOException | ServletException e) {
            resolver.resolveException(request, response, null, e);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();

        return path.equals("/api/v1/users/register") 
            || path.equals("/api/v1/auth/login")
            || path.equals("/api/v1/auth/refresh")
            || path.startsWith("/actuator/");
    }
}
