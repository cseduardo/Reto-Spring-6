package eec.mx.citas_service.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import eec.mx.citas_service.model.Usuario;
import eec.mx.citas_service.repository.UsuarioRepository;

@Component
public class AdministradorSeed implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Comprobar si ya existe un usuario administrador
        if (usuarioRepository.findByUsername("admin").isEmpty()) {
            Usuario admin = new Usuario();
            admin.setUsername("admin");
            // Encripta la contraseña antes de guardarla
            admin.setPassword(passwordEncoder.encode("password")); 
            admin.setRol("ROLE_ADMIN");

            usuarioRepository.save(admin);
            System.out.println(">>> Usuario administrador por defecto creado <<<");
        }
    }
}