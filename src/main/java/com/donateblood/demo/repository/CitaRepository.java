
package com.donateblood.demo.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.donateblood.demo.model.Cita;

import java.util.List;

public interface CitaRepository extends MongoRepository<Cita, String> {

    List<Cita> findByUsuario(String usuario);

    List<Cita> findByLugarAndFecha(String lugar, String fecha);

    // Consultas paginadas para el módulo del banco de sangre
    Page<Cita> findByTipoSangreIgnoreCase(
            String tipoSangre, Pageable pageable);

    Page<Cita> findByUsuarioContainingIgnoreCase(
            String usuario, Pageable pageable);

    Page<Cita> findByTipoSangreIgnoreCaseAndUsuarioContainingIgnoreCase(
            String tipoSangre, String usuario, Pageable pageable);
}
