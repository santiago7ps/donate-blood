package com.donateblood.demo.Seguridad;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.stereotype.Service;

import com.donateblood.demo.model.Usuario;
import com.donateblood.demo.repository.UsuarioRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        if (username == null || username.trim().isEmpty()) {

            throw new UsernameNotFoundException(
                    "Username inválido"
            );
        }

        java.util.List<Usuario> encontrados = usuarioRepo.findAllByUsername(username);
        Usuario usuario = encontrados.isEmpty() ? null : encontrados.get(0);

        if (usuario == null) {

            throw new UsernameNotFoundException(
                    "Usuario no encontrado"
            );
        }

        if (usuario.getRol() == null
                || usuario.getRol().trim().isEmpty()) {

            throw new UsernameNotFoundException(
                    "Rol inválido"
            );
        }

        String passwordFinal = (usuario.getPassword() == null) ? "" : usuario.getPassword();

        return User.builder()

                .username(usuario.getUsername())

                .password(passwordFinal)

                .authorities(usuario.getRol())

                .build();
    }
}
