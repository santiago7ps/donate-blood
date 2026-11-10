package com.donateblood.demo.Seguridad;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.donateblood.demo.model.Usuario;
import com.donateblood.demo.repository.UsuarioRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Si un donante (por ejemplo, uno que entró con Google) todavía no completó su
 * documento, teléfono y edad, lo envía a /completarPerfil antes de usar la app.
 */
@Component
public class PerfilCompletoInterceptor implements HandlerInterceptor {

    private final UsuarioRepository usuarioRepository;

    public PerfilCompletoInterceptor(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return true;
        }

        boolean esDonante = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_USER".equals(a.getAuthority()));
        if (!esDonante) {
            return true;
        }

        List<Usuario> encontrados = usuarioRepository.findAllByUsername(auth.getName());
        if (encontrados.isEmpty() || encontrados.get(0).tienePerfilCompleto()) {
            return true;
        }

        response.sendRedirect("/completarPerfil");
        return false;
    }
}
