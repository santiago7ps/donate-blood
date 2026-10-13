package com.donateblood.demo.Seguridad;

import com.donateblood.demo.model.Usuario;
import com.donateblood.demo.repository.UsuarioRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final JwtCookieService jwtCookieService;

    public OAuth2AuthenticationSuccessHandler(
            UsuarioRepository usuarioRepository,
            JwtService jwtService,
            JwtCookieService jwtCookieService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
        this.jwtCookieService = jwtCookieService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        String email = oauthUser.getAttribute("email");
        String nombre = oauthUser.getAttribute("name");

        if (email == null || email.isBlank()) {
            response.sendRedirect("/login?error");
            return;
        }

        Optional<Usuario> usuarioExistente = usuarioRepository.findByEmail(email);
        Usuario usuario;

        if (usuarioExistente.isPresent()) {
            usuario = usuarioExistente.get();
            boolean cambio = false;
            if (usuario.getUsername() == null || usuario.getUsername().isBlank()) {
                usuario.setUsername(email);
                cambio = true;
            }
            if (usuario.getRol() == null || usuario.getRol().isBlank()) {
                usuario.setRol("ROLE_USER");
                cambio = true;
            }
            if (cambio) {
                usuario = usuarioRepository.save(usuario);
            }
        } else {
            usuario = new Usuario();
            usuario.setEmail(email);
            usuario.setNombre(nombre);
            usuario.setUsername(email);
            usuario.setRol("ROLE_USER");
            usuario = usuarioRepository.save(usuario);
        }

        String username = (usuario.getUsername() == null || usuario.getUsername().isBlank()) ? email : usuario.getUsername();

        var userDetails = User.withUsername(username)
                .password("")
                .authorities(usuario.getRol() == null || usuario.getRol().isBlank() ? "ROLE_USER" : usuario.getRol())
                .build();

        String jwt = jwtService.generateToken(userDetails);

        jwtCookieService.addJwtCookie(response, jwt);

        var authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities());

        // Reemplaza la autenticacion OAuth2 de la sesion (que solo tiene ROLE_OAUTH2_USER)
        // por la del usuario de la app con su rol real; si no, el acceso a los paneles da 403.
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authToken);
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, request, response);

        String rolFinal = userDetails.getAuthorities().iterator().next().getAuthority();
        String destino = switch (rolFinal) {
            case "ROLE_ADMIN" -> "/admin/panel";
            case "ROLE_BANCO" -> "/banco/panel";
            case "ROLE_CENTRO" -> "/centro/panel";
            default -> "/donantePanel";
        };
        response.sendRedirect(destino);
    }
}
