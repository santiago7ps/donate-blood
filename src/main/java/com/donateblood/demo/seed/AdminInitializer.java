package com.donateblood.demo.seed;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.donateblood.demo.model.Usuario;
import com.donateblood.demo.repository.UsuarioRepository;

@Component
public class AdminInitializer {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_USERNAME:admin}")
    private String adminUsername;

    @Value("${ADMIN_PASSWORD:12345678}")
    private String adminPassword;

    public AdminInitializer(UsuarioRepository usuarioRepository,
                            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void crearAdminSiNoExiste() {

        if (!usuarioRepository.findByRol("ROLE_ADMIN").isEmpty()) {
            System.out.println("ADMIN OMITIDO: ya existe un administrador.");
            return;
        }

        if (!usuarioRepository.findAllByUsername(adminUsername).isEmpty()) {
            System.out.println("ADMIN OMITIDO: el usuario '" + adminUsername
                    + "' ya existe con otro rol.");
            return;
        }

        Usuario admin = new Usuario();
        admin.setNombre("Administrador");
        admin.setUsername(adminUsername);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setEmail(adminUsername + "@donateblood.local");
        admin.setRol("ROLE_ADMIN");

        usuarioRepository.save(admin);

        System.out.println("ADMIN CREADO en la base de datos.");
    }
}