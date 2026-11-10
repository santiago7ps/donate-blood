package com.donateblood.demo.seed;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.donateblood.demo.model.Usuario;
import com.donateblood.demo.repository.UsuarioRepository;
import com.donateblood.demo.service.CapacidadService;

/**
 * Bancos que ya existían en la base antes de esta funcionalidad no tienen capacidad;
 * se les asigna la capacidad por defecto para que el control de cupos funcione.
 */
@Component
@Order(1)
public class CapacidadInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;

    public CapacidadInitializer(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void run(String... args) {
        List<Usuario> sinCapacidad = usuarioRepository.findByRol("ROLE_BANCO").stream()
                .filter(b -> b.getCapacidadAtencion() == null)
                .toList();
        if (sinCapacidad.isEmpty()) return;
        sinCapacidad.forEach(b -> b.setCapacidadAtencion(CapacidadService.CAPACIDAD_POR_DEFECTO));
        usuarioRepository.saveAll(sinCapacidad);
    }
}
