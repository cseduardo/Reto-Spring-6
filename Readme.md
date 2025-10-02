# Documentación técnica — Citas Service

> Contenido: diagramas (ER, flujo, clases, casos de uso, secuencia, UML), descripción de funcionamiento, reglas de negocio, retroalimentación, trabajo a futuro y conclusión.

---

## 1. Resumen

Aplicación backend REST (Java y Spring Boot) para gestión de **citas** con autenticación basada en Spring Security y persistencia en PostgreSQL.

---
## 2. Descripción de Funcionamiento y Reglas de Negocio

### Funcionamiento General Frontend
El sistema opera como una aplicación web de página única (SPA) para el administrador. Tras una autenticación exitosa, el administrador accede a un panel donde puede visualizar, crear, modificar y eliminar citas. La interfaz es dinámica, se comunica con el backend a través de una API REST y se actualiza en tiempo real sin necesidad de recargar la página. La orquestación mediante Docker Compose permite levantar todo el entorno (backend y base de datos) con un único comando.

### Reglas de Negocio
* Acceso Restringido: Solo los usuarios autenticados con el rol ADMIN pueden acceder a las funcionalidades de gestión de citas.

* Unicidad de Clientes: Un cliente se considera único por su dirección de correo electrónico. Si se intenta crear una cita para un correo ya existente, el sistema reutiliza el cliente en lugar de crear un duplicado.

* Estado por Defecto: Toda nueva cita se crea con el estado PENDIENTE.

* Validación de Fechas Pasadas: El sistema no permite agendar citas en una fecha u hora anterior al momento actual.

* Validación de Antelación Mínima: Es obligatorio agendar las citas con al menos una hora de antelación respecto a la hora actual del servidor.

---
## 3. Modelo de datos (Descripción y ER)

### 3.1 Tablas principales (resumen)

El modelo de datos es el núcleo del sistema y está diseñado para ser simple pero robusto. Se compone de tres entidades principales: Usuarios, Clientes y Citas, gestionadas en una base de datos relacional PostgreSQL.

Usuarios: Representa a los operadores del sistema (actualmente solo el rol de ADMIN). Es responsable de la autenticación y autorización.

Clientes: Almacena la información de las personas que solicitan las citas. El correo es un campo único para identificar y evitar clientes duplicados.

Citas: Contiene la información de la cita en sí, incluyendo fecha, hora, motivo y estado. Cada cita está obligatoriamente asociada a un único cliente.

La relación clave es entre Clientes y Citas, donde un cliente puede tener múltiples citas (uno a muchos).

### 3.2 Reglas de integridad importantes

* `clientes.correo` es único.
* `citas.cliente_id` no puede ser NULL (relación ManyToOne obligatoria).
* `usuarios.username` único.

### 3.3 Diagrama Entidad-Relación (Mermaid)

```mermaid
erDiagram
    USUARIOS {
        bigint id PK
        varchar(255) username UK
        varchar(255) password
        varchar(50) rol
    }
    CLIENTES {
        bigint id PK
        varchar(255) nombre
        varchar(255) apellidos
        varchar(255) correo UK
        varchar(20) telefono
        varchar(255) direccion
    }
    CITAS {
        bigint id PK
        timestamp fecha_hora
        varchar(255) motivo
        varchar(50) estado
        bigint cliente_id FK
    }

    CLIENTES ||--o{ CITAS : "tiene"
```

---

## 4. Diagramas de Arquitectura y Flujo (UML y otros)

### 4.1 Diagrama de Clases

Descripción: Muestra la estructura estática de las clases principales del backend, sus atributos, métodos clave y las relaciones entre ellas. Se centra en las capas de Controlador, Repositorio y Modelo (Entidades).

```mermaid
classDiagram
    class CitaController {
        +ClienteRepository clienteRepository
        +CitaRepository citaRepository
        +obtenerTodasLasCitas() List~Cita~
        +crearCita(CitaRequest) ResponseEntity
        +actualizarEstadoCita(Long, Map) ResponseEntity
        +eliminarCita(Long) ResponseEntity
        +obtenerCitasPorCliente(Long) ResponseEntity
    }
    class CitaRepository {
        <<Interface>>
        +findAllWithCliente() List~Cita~
        +findByClienteIdWithCliente(Long) List~Cita~
        +findByIdWithCliente(Long) Optional~Cita~
    }
    class ClienteRepository {
        <<Interface>>
        +findByCorreo(String) Optional~Cliente~
    }
    class Cita {
        -Long id
        -LocalDateTime fechaHora
        -String motivo
        -String estado
        -Cliente cliente
    }
    class Cliente {
        -Long id
        -String nombre
        -String correo
        -Set~Cita~ citas
    }
    class Usuario {
        -Long id
        -String username
        -String rol
    }

    CitaController ..> CitaRepository : usa
    CitaController ..> ClienteRepository : usa
    CitaRepository ..> Cita : gestiona
    ClienteRepository ..> Cliente : gestiona
    Cita "1" -- "1" Cliente : pertenece a
    Cliente "1" -- "0..*" Cita : tiene
```

### 4.2 Diagrama de Casos de Uso

Descripción: Define las interacciones entre el actor (el Administrador) y el sistema, mostrando las funcionalidades clave desde la perspectiva del usuario.

```mermaid
graph TD;
    Admin["👤 Admin"];
    UC1["Iniciar Sesión"];
    UC2["Gestionar Citas"];
    UC3["Ver Lista de Citas"];
    UC4["Crear Cita"];
    UC5["Actualizar Estado"];
    UC6["Eliminar Cita"];
    UC7["Filtrar por Cliente"];

    Admin --> UC1;
    UC1 --> UC2;
    UC2 --> UC3;
    UC2 --> UC4;
    UC2 --> UC5;
    UC2 --> UC6;
    UC2 --> UC7;

    style Admin fill:#f9f,stroke:#333,stroke-width:2px

```
### 4.3 Diagrama de Flujo: Creación de una Cita

Descripción: Este diagrama ilustra el flujo completo de eventos, desde la interacción del administrador en el frontend hasta el almacenamiento de los datos en la base de datos, incluyendo la lógica de validación.

```mermaid
graph TD
    A[👤 Admin inicia sesión] --> B{Accede al Panel de Citas};
    B --> C[Presiona el botón 'Agregar Cita'];
    C --> D[👨‍💻 Llena el formulario con datos del cliente y la cita];
    D --> E{Presiona el botón 'Guardar Cita'};
    subgraph "Frontend (Navegador)"
        E --> F[JavaScript valida formato y crea un objeto JSON];
        F --> G[🚀 Petición POST a /api/citas];
    end
    subgraph "Backend (Spring Boot)"
        G --> H[CitaController recibe la petición];
        H --> I{"¿La fecha/hora es válida? (>= ahora + 1h)"};
        I -- No --> J[🔴 Retorna Error 400 Bad Request];
        I -- Sí --> K[Busca cliente por correo en BD];
        K -- "No existe" --> L[Crea y guarda nuevo cliente];
        K -- "Sí existe" --> M[Usa cliente existente];
        L --> M;
        M --> N[Crea la entidad Cita y la asocia al cliente];
        N --> O[💾 Guarda la Cita en la BD];
        O --> P[✅ Retorna Éxito 201 Created con datos de la cita];
    end
    subgraph "Respuesta"
        J --> Q[Frontend muestra mensaje de error de validación];
        P --> R[Frontend recarga la lista de citas y muestra la nueva];
    end
```
### 4.4 Diagrama de Secuencia: Creación de una Nueva Cita

Descripción: Este diagrama muestra la interacción cronológica entre los diferentes componentes del sistema cuando un administrador crea una nueva cita. Detalla cada paso, desde el envío del formulario en el frontend hasta las consultas y escrituras en la base de datos, incluyendo la lógica condicional para manejar clientes nuevos o existentes.

```mermaid
    sequenceDiagram;
    participant Admin as 👤 Admin;
    participant Frontend as 🌐 Navegador;
    participant Controller as 🕹️ CitaController;
    participant ClienteRepo as 💾 ClienteRepository;
    participant CitaRepo as 💾 CitaRepository;
    participant DB as 🗄️ PostgreSQL;

    Admin->>+Frontend: Llena y envía formulario de nueva cita;
    Frontend->>+Controller: POST /api/citas (con datos de Cita y Cliente);
    
    Note right of Controller: 1. Valida que la fecha sea válida (>= ahora + 1h);
    Controller->>+ClienteRepo: findByCorreo(email);
    ClienteRepo->>+DB: SELECT * FROM clientes WHERE correo = ?;
    DB-->>-ClienteRepo: Retorna Optional de Cliente;
    ClienteRepo-->>-Controller: Devuelve el resultado;

    alt Cliente no existe;
        Controller->>+ClienteRepo: save(nuevoCliente);
        ClienteRepo->>+DB: INSERT INTO clientes (...);
        DB-->>-ClienteRepo: Retorna Cliente guardado con ID;
        ClienteRepo-->>-Controller: Devuelve el nuevo Cliente;
    end;

    Note right of Controller: 2. Asocia la Cita con el Cliente (nuevo o existente);
    Controller->>+CitaRepo: save(nuevaCita);
    CitaRepo->>+DB: INSERT INTO citas (...);
    DB-->>-CitaRepo: Retorna Cita guardada con ID;
    CitaRepo-->>-Controller: Devuelve la Cita guardada;

    Controller-->>-Frontend: Respuesta 201 CREATED (con JSON de la Cita);
    Frontend->>+Frontend: Recarga la lista de citas;
    Frontend-->>-Admin: Muestra la nueva cita en la tabla;
```
### 4.5 Diagrama de Flujo: Actualización de Estado de una Cita

Descripción:Este diagrama de flujo detalla el proceso que se desencadena cuando el administrador cambia el estado de una cita en el panel (por ejemplo, de "PENDIENTE" a "COMPLETADA"). Muestra las validaciones, la interacción con la base de datos y la actualización final de la interfaz de usuario.

```mermaid
graph TD;
    A["👤 Admin ve la lista de citas"];
    A --> B["Selecciona un nuevo estado para una cita"];

    subgraph "Frontend (Navegador)";
        B --> C{"Muestra diálogo de confirmación"};
        C -- "Sí, confirmar" --> D["🚀 Petición PUT a /api/citas/{id}"];
        C -- "No, cancelar" --> E["Acción cancelada"];
    end;

    subgraph "Backend (Spring Boot)";
        D --> F["🕹️ CitaController recibe la petición"];
        F --> G{"Busca cita por ID en la BD"};
        G -- No encontrada --> H["🔴 Retorna Error 404 Not Found"];
        G -- Encontrada --> I["Valida que el nuevo estado sea correcto"];
        I -- Inválido --> J["🔴 Retorna Error 400 Bad Request"];
        I -- Válido --> K["Actualiza el estado en el objeto Cita"];
        K --> L["💾 Guarda la cita en la BD (UPDATE)"];
        L --> M["✅ Retorna Éxito 200 OK con datos actualizados"];
    end;

    subgraph "Respuesta al Usuario";
        H --> N["Frontend muestra alerta de error"];
        J --> N;
        M --> O["Frontend recibe la confirmación"];
        O --> P["✨ Recarga y actualiza la tabla de citas"];
    end;
```

### 4.6 Diagrama de Secuencia: Actualizar Estado de una Cita

Descripción: Modela la interacción paso a paso y en orden cronológico entre los componentes del sistema para un caso de uso específico: cuando el administrador cambia el estado de una cita desde la interfaz.

```mermaid
sequenceDiagram
    participant Admin as 👤 Admin
    participant Frontend as 🌐 Navegador
    participant Controller as 🕹️ CitaController
    participant Repository as 💾 CitaRepository
    participant DB as 🗄️ PostgreSQL

    Admin->>+Frontend: Cambia estado de cita a "COMPLETADA"
    Frontend->>+Controller: Petición PUT /api/citas/{id} con {"estado":"COMPLETADA"}
    Controller->>+Repository: findByIdWithCliente(id)
    Repository->>+DB: SELECT ... FROM citas JOIN clientes WHERE id = ?
    DB-->>-Repository: Retorna Cita y Cliente
    Repository-->>-Controller: Retorna Optional<Cita>
    Controller->>Controller: Actualiza el estado del objeto Cita
    Controller->>+Repository: save(citaActualizada)
    Repository->>+DB: UPDATE citas SET estado = ? WHERE id = ?
    DB-->>-Repository: Confirmación de UPDATE
    Repository-->>-Controller: Retorna Cita actualizada
    Controller-->>-Frontend: Respuesta 200 OK con JSON de la cita actualizada
    Frontend->>Frontend: Recarga la lista de citas (llama a cargarCitas())
    Frontend-->>-Admin: Muestra la tabla actualizada con el nuevo estado
```
### 4.5 Diagrama de Flujo: Eliminar una Cita

Descripción:Este diagrama ilustra el proceso completo que sigue el sistema cuando un administrador decide eliminar una cita. Muestra los pasos de confirmación en el frontend, las validaciones en el backend, la operación en la base de datos y la actualización final de la vista para el usuario.

```mermaid
graph TD;
    A["👤 Admin ve la lista de citas en el panel"];
    A --> B["Clic en el botón 'Eliminar' de una cita"];

    subgraph "Frontend (Navegador)";
        B --> C{"Muestra diálogo: '¿Estás seguro?'"};
        C -- "Sí, eliminar" --> D["🚀 Petición DELETE a /api/citas/{id}"];
        C -- "No, cancelar" --> E["Acción cancelada"];
    end;

    subgraph "Backend (Spring Boot)";
        D --> F["🕹️ CitaController recibe la petición"];
        F --> G{"¿Existe la cita con ese ID en la BD?"};
        G -- No --> H["🔴 Retorna Error 404 Not Found"];
        G -- Sí --> I["🗑️ Ordena al repositorio eliminar la cita por ID"];
        I --> J["✅ Retorna Éxito 204 No Content (sin cuerpo)"];
    end;

    subgraph "Respuesta al Usuario";
        H --> K["Frontend muestra alerta de error"];
        J --> L["Frontend recibe la confirmación de éxito"];
        L --> M["✨ Llama a la función para recargar la tabla de citas"];
        M --> N["La cita eliminada desaparece de la lista"];
    end;
```

### 4.6 Diagrama de Secuencia: Eliminar una Cita

Descripción: Este diagrama modela la interacción cronológica y los mensajes pasados entre los componentes del sistema durante el proceso de eliminación. Es especialmente útil para ver las llamadas exactas entre el controlador, el repositorio y la base de datos.

```mermaid
sequenceDiagram;
    participant Admin as 👤 Admin;
    participant Frontend as 🌐 Navegador;
    participant Controller as 🕹️ CitaController;
    participant Repository as 💾 CitaRepository;
    participant DB as 🗄️ PostgreSQL;

    Admin->>+Frontend: Clic en botón 'Eliminar';
    Frontend->>Frontend: Muestra confirm('¿Estás seguro?');
    
    alt Usuario confirma;
        Frontend->>+Controller: DELETE /api/citas/{id};
        Controller->>+Repository: existsById(id);
        Repository->>+DB: SELECT 1 FROM citas WHERE id = ?;
        DB-->>-Repository: Retorna true;
        Repository-->>-Controller: Retorna true;

        Controller->>+Repository: deleteById(id);
        Repository->>+DB: DELETE FROM citas WHERE id = ?;
        DB-->>-Repository: Confirmación de borrado;
        Repository-->>-Controller: (void);
        Controller-->>-Frontend: Respuesta 204 No Content;
        
        Frontend->>+Frontend: Llama a cargarCitas();
        Frontend-->>-Admin: La tabla se actualiza sin la cita eliminada;
    else Usuario cancela;
        Frontend-->>-Admin: Cierra el diálogo, no pasa nada;
    end;
```
---

## 5. Trabajo Futuro y Mejoras Propuestas

El estado actual del proyecto es un excelente Producto Mínimo Viable (MVP). Para llevarlo a un entorno de producción, se proponen las siguientes mejoras:

1. Gestión de Roles Avanzada: Introducir un rol CLIENTE que permita a los usuarios registrarse, iniciar sesión y ver/cancelar sus propias citas.

2. Sistema de Notificaciones: Implementar un servicio (posiblemente un nuevo microservicio) para enviar correos electrónicos de confirmación, recordatorio y cancelación de citas.

3. Seguridad Mejorada: Migrar de la autenticación basada en sesión a un esquema basado en tokens (JWT), que es más adecuado para arquitecturas de microservicios y clientes móviles. Al igual de la propuesta del sistema de notificaciones, se crearía en otro microservicio para unificar la gestion de roles en el cual se menciona en el punto uno y se unificaría con la creación de token para el manejo de la sesión

4. Pruebas Automatizadas: Desarrollar una suite de pruebas unitarias (con JUnit/Mockito) e de integración (con Testcontainers) para garantizar la fiabilidad del código y evitar regresiones.

5. Calendario Visual: En el frontend, reemplazar la tabla por una vista de calendario interactiva para una mejor experiencia de usuario al agendar.

6. Pipeline de CI/CD: Configurar un pipeline de Integración y Despliegue Continuo (ej. con GitHub Actions, Jenkins) para automatizar las pruebas y el despliegue de nuevas versiones.

---

## 6. Retroalimentación

El proyecto actual presenta una base sólida y sigue buenas prácticas en cuanto a tecnología y contenedorización.

### Puntos Fuertes:

* Stack Tecnológico Moderno: El uso de Spring Boot 6, Java 21 y Docker posiciona al proyecto a la vanguardia.

* Arquitectura Desacoplada: La separación clara entre frontend, backend y base de datos permite un desarrollo y escalado independientes.

* Entorno Portable: Docker Compose garantiza que cualquier desarrollador pueda levantar el sistema de forma idéntica y sin configuraciones complejas.

### Áreas de Mejora Arquitectónica:

1. Capa de Servicio (@Service): Actualmente, la lógica de negocio reside en el Controlador (@RestController). Se recomienda refactorizar esta lógica a una capa de Servicio (@Service) para mejorar la separación de responsabilidades y hacer el código más mantenible y testeable.

2. Uso de DTOs (Data Transfer Objects): La API expone directamente las entidades JPA. Esto acopla la API al modelo de datos y puede exponer información sensible. La implementación de DTOs para las respuestas y peticiones de la API es una mejora crucial.

3. Gestión de Errores Centralizada: Se debería implementar un manejador de excepciones global con @ControllerAdvice para estandarizar las respuestas de error en toda la aplicación.

---

## 7. Conclusión

El Sistema de Gestión de Citas es un prototipo funcional robusto que cumple con todos los requisitos iniciales. La arquitectura elegida es escalable y se alinea con los estándares modernos de desarrollo de software.

Implementando las mejoras sugeridas en las secciones de "Trabajo Futuro" y "Retroalimentación", el proyecto tiene un camino claro para evolucionar hacia una aplicación de nivel de producción, segura, fiable y fácil de mantener.