package com.donateblood.demo.Seguridad;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import jakarta.servlet.ServletException;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtService jwtService;
    private final JwtCookieService jwtCookieService;

    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    public SecurityConfig(
            CustomUserDetailsService customUserDetailsService,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtService jwtService,
            JwtCookieService jwtCookieService,
            OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler
    ) {
        this.customUserDetailsService = customUserDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtService = jwtService;
        this.jwtCookieService = jwtCookieService;
        this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(encoder);
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, UserDetailsService userDetailsService) throws Exception {

        http

            .cors(cors -> {})

            .csrf(csrf -> csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            )

            .sessionManagement(session ->
                    session.sessionCreationPolicy(
                            SessionCreationPolicy.IF_REQUIRED
                    )
            )

            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/",
                        "/info",

                        "/login",
                        "/register",

                        "/error",

                        "/css/**",
                        "/js/**",

                        "/img/**",
                        "/images/**",

                        "/oauth2/**",
                        "/login/oauth2/**"
                ).permitAll()

                .requestMatchers("/admin/**").hasRole("ADMIN")

                .requestMatchers("/banco/pedidos").hasAnyRole("BANCO", "ADMIN", "CENTRO")

                .requestMatchers("/banco/**").hasAnyRole("BANCO", "ADMIN")

                .requestMatchers("/realizarCita/**").hasAnyRole("BANCO", "ADMIN")

                .requestMatchers("/centro/**").hasAnyRole("CENTRO", "ADMIN")

                .requestMatchers(
                        "/donantePanel",
                        "/perfilDonante",
                        "/verCitas",
                        "/agendarCita",
                        "/misCitas"
                ).hasAnyRole("USER", "ADMIN")

                .anyRequest().authenticated()
            )

            .userDetailsService(userDetailsService)

            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(successHandler())

                .failureHandler((request, response, exception) -> {
                    response.sendRedirect("/login?error=true");
                })
                .permitAll()
            )

            .oauth2Login(oauth -> oauth
                .loginPage("/login")
                .successHandler(oAuth2AuthenticationSuccessHandler)
                .failureHandler((request, response, exception) -> response.sendRedirect("/login?oauthError=true"))
            )

            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler((request, response, authentication) -> {
                    SecurityContextHolder.clearContext();
                    jwtCookieService.clearJwtCookie(response);
                    response.sendRedirect("/login?logout");
                })
                .permitAll()
            );

        http.addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {

        UserDetails admin = User.builder()
                .username(adminUsername)
                .password(encoder.encode(adminPassword))
                .roles("ADMIN")
                .build();

        InMemoryUserDetailsManager memory = new InMemoryUserDetailsManager(admin);

        return username -> {
            try {
                return memory.loadUserByUsername(username);
            } catch (UsernameNotFoundException e) {
                return customUserDetailsService.loadUserByUsername(username);
            }
        };
    }

    @Bean
    public AuthenticationSuccessHandler successHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    org.springframework.security.core.Authentication authentication)
                    throws IOException, ServletException {

                try {

                    UserDetails userDetails = (UserDetails) authentication.getPrincipal();

                    String token = jwtService.generateToken(userDetails);

                    jwtCookieService.addJwtCookie(response, token);

                    var authorities = authentication.getAuthorities();

                    for (var auth : authorities) {
                        if (auth.getAuthority().equals("ROLE_ADMIN")) {
                            response.sendRedirect("/admin/panel");
                            return;
                        }
                        if (auth.getAuthority().equals("ROLE_BANCO")) {
                            response.sendRedirect("/banco/panel");
                            return;
                        }
                        if (auth.getAuthority().equals("ROLE_CENTRO")) {
                            response.sendRedirect("/centro/panel");
                            return;
                        }
                        if (auth.getAuthority().equals("ROLE_USER")) {
                            response.sendRedirect("/donantePanel");
                            return;
                        }
                    }
                    response.sendRedirect("/login");
                } catch (Exception e) {
                    SecurityContextHolder.clearContext();
                    jwtCookieService.clearJwtCookie(response);
                    response.sendRedirect("/login?error=true");
                }
            }
        };
    }
}
