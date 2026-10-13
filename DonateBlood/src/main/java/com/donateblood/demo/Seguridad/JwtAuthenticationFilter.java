package com.donateblood.demo.Seguridad;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String jwt = null;
        String username = null;

        try {

            String path = request.getServletPath();

            if (path.equals("/login")
                    || path.equals("/error")
                    || path.startsWith("/css")
                    || path.startsWith("/js")
                    || path.startsWith("/img")
                    || path.startsWith("/images")
                    || path.startsWith("/oauth2")
                    || path.startsWith("/login/oauth2")
                    || path.equals("/")) {

                filterChain.doFilter(request, response);
                return;
            }

            if (request.getCookies() != null) {

                for (Cookie cookie : request.getCookies()) {

                    if ("jwt".equals(cookie.getName())) {

                        jwt = cookie.getValue();
                        break;
                    }
                }
            }

            if (jwt == null || jwt.isBlank()) {

                filterChain.doFilter(request, response);
                return;
            }

            username = jwtService.extractUsername(jwt);

            if (username != null
                    && SecurityContextHolder
                    .getContext()
                    .getAuthentication() == null) {

                UserDetails userDetails;

                if (adminUsername.equals(username)) {

                    userDetails = User.builder()
                            .username(adminUsername)
                            .password("")
                            .roles("ADMIN")
                            .build();

                } else {

                    userDetails =
                            userDetailsService
                                    .loadUserByUsername(username);
                }

                if (jwtService.isTokenValid(jwt, userDetails)) {

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities());

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request));

                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(authToken);
                }
            }

        } catch (UsernameNotFoundException e) {

            System.out.println("Usuario de sesión previa no encontrado - Continuando de forma anónima");
            SecurityContextHolder.clearContext();

        } catch (Exception e) {
            System.out.println("Error JWT: " + e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
