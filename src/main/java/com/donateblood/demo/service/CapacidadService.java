package com.donateblood.demo.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.donateblood.demo.model.Cita;
import com.donateblood.demo.model.Usuario;
import com.donateblood.demo.repository.CitaRepository;
import com.donateblood.demo.repository.UsuarioRepository;
import com.donateblood.demo.util.CitasYSangre;

/**
 * Control de cupos de las citas: cada banco de sangre atiende, en una misma
 * fecha y hora, como máximo tantos donantes como indique su capacidad de atención.
 * Las citas CANCELADAS liberan el cupo; PENDIENTE y REALIZADA lo ocupan.
 */
@Service
public class CapacidadService {

    /** Capacidad usada si un banco aún no ha configurado la suya. */
    public static final int CAPACIDAD_POR_DEFECTO = 5;
    public static final int CAPACIDAD_MAXIMA = 100;

    private final CitaRepository citaRepository;
    private final UsuarioRepository usuarioRepository;

    // Evita que dos solicitudes simultáneas tomen el último cupo (una sola instancia de la app).
    private final Object lock = new Object();

    public CapacidadService(CitaRepository citaRepository, UsuarioRepository usuarioRepository) {
        this.citaRepository = citaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** "lugar" de la cita es el nombre del banco (o su username si no tiene nombre). */
    private Usuario buscarBanco(String lugar) {
        if (lugar == null || lugar.isBlank()) return null;
        Usuario banco = usuarioRepository.findFirstByRolAndNombre("ROLE_BANCO", lugar);
        if (banco == null) {
            Usuario porUsername = usuarioRepository.findByUsername(lugar);
            if (porUsername != null && "ROLE_BANCO".equals(porUsername.getRol())) {
                banco = porUsername;
            }
        }
        return banco;
    }

    public int capacidadDe(String lugar) {
        Usuario banco = buscarBanco(lugar);
        Integer c = banco == null ? null : banco.getCapacidadAtencion();
        return (c != null && c > 0) ? c : CAPACIDAD_POR_DEFECTO;
    }

    public int capacidadDeUsername(String username) {
        Usuario banco = usuarioRepository.findByUsername(username);
        Integer c = banco == null ? null : banco.getCapacidadAtencion();
        return (c != null && c > 0) ? c : CAPACIDAD_POR_DEFECTO;
    }

    private long ocupados(String lugar, String fecha, String hora, String excluirId) {
        return citaRepository.findByLugarAndFecha(lugar, fecha).stream()
                .filter(c -> hora.equals(c.getHora()))
                .filter(c -> !"CANCELADA".equals(c.getEstado()))
                .filter(c -> excluirId == null || !excluirId.equals(c.getId()))
                .count();
    }

    public int cuposLibres(String lugar, String fecha, String hora, String excluirId) {
        if (lugar == null || fecha == null || hora == null) return 0;
        return (int) Math.max(0, capacidadDe(lugar) - ocupados(lugar, fecha, hora, excluirId));
    }

    /** Todas las franjas válidas con la cantidad de cupos que les quedan ese día. */
    public Map<String, Integer> cuposPorHora(String lugar, String fecha, String excluirId) {
        Map<String, Integer> resultado = new LinkedHashMap<>();
        int capacidad = capacidadDe(lugar);
        Map<String, Long> usados = new LinkedHashMap<>();
        for (Cita c : citaRepository.findByLugarAndFecha(lugar, fecha)) {
            if ("CANCELADA".equals(c.getEstado())) continue;
            if (excluirId != null && excluirId.equals(c.getId())) continue;
            usados.merge(c.getHora(), 1L, Long::sum);
        }
        for (String hora : CitasYSangre.franjasValidas()) {
            resultado.put(hora, (int) Math.max(0, capacidad - usados.getOrDefault(hora, 0L)));
        }
        return resultado;
    }

    public List<String> horasConCupo(String lugar, String fecha, String excluirId) {
        List<String> libres = new ArrayList<>();
        cuposPorHora(lugar, fecha, excluirId).forEach((hora, cupos) -> {
            if (cupos > 0) libres.add(hora);
        });
        return libres;
    }

    /** Guarda la cita solo si la hora aún tiene cupo. Devuelve false si ya está llena. */
    public boolean guardarSiHayCupo(Cita cita) {
        synchronized (lock) {
            if (cuposLibres(cita.getLugar(), cita.getFecha(), cita.getHora(), cita.getId()) <= 0) {
                return false;
            }
            citaRepository.save(cita);
            return true;
        }
    }

    public void actualizarCapacidad(String username, int capacidad) {
        Usuario banco = usuarioRepository.findByUsername(username);
        if (banco == null || !"ROLE_BANCO".equals(banco.getRol())) {
            throw new IllegalArgumentException("El usuario no es un banco de sangre");
        }
        if (capacidad < 1 || capacidad > CAPACIDAD_MAXIMA) {
            throw new IllegalArgumentException("La capacidad debe estar entre 1 y " + CAPACIDAD_MAXIMA);
        }
        banco.setCapacidadAtencion(capacidad);
        usuarioRepository.save(banco);
    }
}
