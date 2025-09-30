package eec.mx.citas_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import eec.mx.citas_service.model.Usuario;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    // Método para buscar un usuario por su nombre de usuario
    Optional<Usuario> findByUsername(String username);
}
