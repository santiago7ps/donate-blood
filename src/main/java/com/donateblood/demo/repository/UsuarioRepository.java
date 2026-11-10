package com.donateblood.demo.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.donateblood.demo.model.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends MongoRepository<Usuario, String> {

    Usuario findByUsername(String username);

    List<Usuario> findAllByUsername(String username);

    Optional<Usuario> findByEmail(String email);

    List<Usuario> findByRol(String rol);

    Usuario findFirstByRolAndNombre(String rol, String nombre);

    void deleteByRol(String rol);
}