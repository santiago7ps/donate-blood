package com.donateblood.demo.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.donateblood.demo.model.Cita;

import java.util.List;

public interface CitaRepository extends MongoRepository<Cita, String> {

    List<Cita> findByUsuario(String usuario);

}
