package eec.mx.citas_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Deshabilitar CSRF para APIs REST
            .authorizeHttpRequests(auth -> auth
                // Permite el acceso al formulario de login y a los archivos estáticos
                .requestMatchers("/login.html", "/css/**", "/js/**").permitAll()
                // El endpoint de la API para crear citas requiere el rol de ADMIN
                .requestMatchers(HttpMethod.POST, "/api/citas").hasRole("ADMIN")
                // El endpoint para ver citas también requiere ser ADMIN (o se puede cambiar a permitAll())
                .requestMatchers(HttpMethod.GET, "/api/citas").hasRole("ADMIN")
                // Cualquier otra petición (incluyendo index.html) necesita autenticación
                .anyRequest().authenticated()
            )
            // Configuración del formulario de login
            .formLogin(form -> form
                .loginPage("/login.html") // Página de login personalizada
                .loginProcessingUrl("/login") // URL que procesa el login (default de Spring Security)
                .defaultSuccessUrl("/index.html", true) // A dónde redirigir tras un login exitoso
                .failureUrl("/login.html?error=true") // A dónde redirigir si falla
                .permitAll()
            )
            // Configuración del logout
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login.html?logout=true")
                .permitAll()
            );

        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Usa BCrypt para encriptar contraseñas
        return new BCryptPasswordEncoder(); 
    }
}