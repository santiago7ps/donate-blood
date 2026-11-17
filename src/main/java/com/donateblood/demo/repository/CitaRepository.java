package com.donateblood.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.donateblood.demo.model.Cita;

import java.util.List;

public interface CitaRepository extends MongoRepository<Cita, String> {

    List<Cita> findByUsuario(String usuario);

    List<Cita> findByLugarAndFecha(String lugar, String fecha);

    // Consultas paginadas por tipo de sangre
    Page<Cita> findByTipoSangreIgnoreCase(
            String tipoSangre, Pageable pageable);

    // Se conservan las consultas anteriores por usuario
    Page<Cita> findByUsuarioContainingIgnoreCase(
            String usuario, Pageable pageable);

    Page<Cita> findByTipoSangreIgnoreCaseAndUsuarioContainingIgnoreCase(
            String tipoSangre, String usuario, Pageable pageable);

    // Nuevas consultas paginadas por nombre completo
    Page<Cita> findByNombreRegex(
            String nombre, Pageable pageable);

    Page<Cita> findByTipoSangreIgnoreCaseAndNombreRegex(
            String tipoSangre, String nombre, Pageable pageable);
}

