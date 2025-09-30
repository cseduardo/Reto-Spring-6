package eec.mx.citas_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import eec.mx.citas_service.model.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    // Método para buscar un cliente por su correo electrónico
    Optional<Cliente> findByCorreo(String correo);
}
