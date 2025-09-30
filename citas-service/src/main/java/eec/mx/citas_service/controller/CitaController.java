package eec.mx.citas_service.controller;

import eec.mx.citas_service.dto.CitaRequest;
import eec.mx.citas_service.model.Cita;
import eec.mx.citas_service.model.Cliente;
import eec.mx.citas_service.repository.CitaRepository;
import eec.mx.citas_service.repository.ClienteRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/citas")
@CrossOrigin(origins = "*") // Permite peticiones desde cualquier origen (frontend)
public class CitaController {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CitaRepository citaRepository;

    @GetMapping
    public List<Cita> obtenerTodasLasCitas() {
        return citaRepository.findAllWithCliente();
    }

    @PostMapping
    public ResponseEntity<?> crearCita(@RequestBody CitaRequest citaRequest) {
        // --- INICIO DEL BLOQUE DE VALIDACIÓN ---

        // 1. Se define la zona horaria del servidor (importante para consistencia)
        ZoneId zonaHoraria = ZoneId.of("America/Mexico_City");

        // 2. Seobtiene la fecha y hora actual en esa zona horaria
        LocalDateTime ahora = LocalDateTime.now(zonaHoraria);

        // 3. Se calcula el límite mínimo para la cita (ahora + 1 hora)
        LocalDateTime limiteMinimo = ahora.plusHours(1);

        // 4. Se compara la fecha de la cita solicitada con el límite
        if (citaRequest.getFechaHora().isBefore(limiteMinimo)) {
            // Si la fecha es inválida, se devuelve un error 400 Bad Request
            Map<String, String> error = new HashMap<>();
            error.put("mensaje", "La cita debe ser programada con al menos una hora de anticipación.");
            return ResponseEntity.badRequest().body(error);
        }

        // --- FIN DEL BLOQUE DE VALIDACIÓN ---

        // 1. Buscar si el cliente ya existe por correo, si no, crearlo.
        Cliente cliente = clienteRepository.findByCorreo(citaRequest.getCorreo())
                .orElseGet(() -> {
                    Cliente nuevoCliente = new Cliente();
                    nuevoCliente.setNombre(citaRequest.getNombre());
                    nuevoCliente.setApellidos(citaRequest.getApellidos());
                    nuevoCliente.setCorreo(citaRequest.getCorreo());
                    nuevoCliente.setTelefono(citaRequest.getTelefono());
                    nuevoCliente.setDireccion(citaRequest.getDireccion());
                    return clienteRepository.save(nuevoCliente);
                });

        // 2. Crear la cita y asociarla con el cliente
        Cita nuevaCita = new Cita();
        nuevaCita.setCliente(cliente);
        nuevaCita.setFechaHora(citaRequest.getFechaHora());
        nuevaCita.setMotivo(citaRequest.getMotivo());
        nuevaCita.setEstado("PENDIENTE");

        Cita citaGuardada = citaRepository.save(nuevaCita);
        return new ResponseEntity<>(citaGuardada, HttpStatus.CREATED);
    }

    // --- ENDPOINT: Actualizar estado de una cita ---
    @PutMapping("/{id}")
    public ResponseEntity<Cita> actualizarEstadoCita(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        Optional<Cita> citaOptional = citaRepository.findByIdWithCliente(id);
        if (citaOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String nuevoEstado = payload.get("estado");
        if (nuevoEstado == null || (!nuevoEstado.equals("COMPLETADA") && !nuevoEstado.equals("CANCELADA")
                && !nuevoEstado.equals("PENDIENTE"))) {
            return ResponseEntity.badRequest().build(); // Estado inválido
        }

        Cita cita = citaOptional.get();
        cita.setEstado(nuevoEstado);
        Cita citaActualizada = citaRepository.save(cita);
        return ResponseEntity.ok(citaActualizada);
    }

    // --- ENDPOINT: Eliminar una cita ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCita(@PathVariable Long id) {
        if (!citaRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        citaRepository.deleteById(id);
        return ResponseEntity.noContent().build(); // Status 204 No Content
    }

    // --- ENDPOINT: Obtener citas por cliente ---
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<Cita>> obtenerCitasPorCliente(@PathVariable Long clienteId) {
        List<Cita> citas = citaRepository.findByClienteIdWithCliente(clienteId);
        return ResponseEntity.ok(citas);
    }
}
