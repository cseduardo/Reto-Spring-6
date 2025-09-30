package eec.mx.citas_service.repository;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import eec.mx.citas_service.model.Cita;

public interface CitaRepository extends JpaRepository<Cita, Long> {
    @Query("SELECT c FROM Cita c JOIN FETCH c.cliente")
    List<Cita> findAllWithCliente();

    @Query("SELECT c FROM Cita c JOIN FETCH c.cliente WHERE c.cliente.id = :clienteId")
    List<Cita> findByClienteIdWithCliente(@Param("clienteId") Long clienteId);

    @Query("SELECT c FROM Cita c JOIN FETCH c.cliente WHERE c.id = :id")
    Optional<Cita> findByIdWithCliente(@Param("id") Long id);
}
