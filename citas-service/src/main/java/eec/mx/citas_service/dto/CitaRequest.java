package eec.mx.citas_service.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CitaRequest {
    // Datos del cliente
    private String nombre;
    private String apellidos;
    private String correo;
    private String telefono;
    private String direccion;

    // Datos de la cita
    private LocalDateTime fechaHora;
    private String motivo;
}