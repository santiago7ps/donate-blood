package com.donateblood.demo.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.donateblood.demo.model.Usuario;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends MongoRepository<Usuario, String> {

    Usuario findByUsername(String username);

    List<Usuario> findAllByUsername(String username);

    // Busca varios usuarios de una sola vez (se usa para los correos de recordatorio)
    List<Usuario> findByUsernameIn(Collection<String> usernames);

    Optional<Usuario> findByEmail(String email);

    List<Usuario> findByRol(String rol);

    Usuario findFirstByRolAndNombre(String rol, String nombre);

    void deleteByRol(String rol);
}