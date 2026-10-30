# 🥗 Clínica NutriVida

Sistema web de una clínica de nutrición:

- Los **pacientes** reservan horas.
- La **secretaria** asigna un nutricionista a cada reserva.
- El **nutricionista** registra fichas nutricionales.
- El **administrador** gestiona a los profesionales.

Por debajo usa servicios cloud: login con **Azure**, API en **AWS**, mensajería con **RabbitMQ** y eventos con **Kafka**.

> 📌 **Si es tu primera vez en el proyecto**, lee primero las secciones 1, 2 y 3. Con eso entiendes el 80 %.

---

## Índice

1. [¿Qué piezas tiene el sistema?](#1-qué-piezas-tiene-el-sistema)
2. [¿Dónde está cada cosa?](#2-dónde-está-cada-cosa)
3. [Cómo levantar todo en tu PC](#3-cómo-levantar-todo-en-tu-pc)
4. [Recorrido completo: ¿qué pasa cuando un paciente reserva?](#4-recorrido-completo-qué-pasa-cuando-un-paciente-reserva)
5. [Seguridad: login y permisos](#5-seguridad-login-y-permisos)
6. [RabbitMQ: las colas](#6-rabbitmq-las-colas)
7. [Kafka: los eventos](#7-kafka-los-eventos)
8. [Admin-service: el panel de control de la mensajería](#8-admin-service-el-panel-de-control-de-la-mensajería)
9. [Configuración (`application.yml`)](#9-configuración-applicationyml)
10. [Pruebas](#10-pruebas)
11. [Glosario](#11-glosario)
12. [Problemas comunes](#12-problemas-comunes)

---

## 1. ¿Qué piezas tiene el sistema?

```
 👤 Usuario
    │
    ▼
┌──────────────┐  1. login   ┌───────────────────┐
│   FRONTEND   │ ──────────▶ │  AZURE ENTRA ID   │  entrega un "token" (pase de acceso)
│   (React)    │ ◀────────── │  (login Microsoft)│
└──────┬───────┘             └───────────────────┘
       │ 2. llama a la API con el token
       ▼
┌──────────────┐
│ API GATEWAY  │  (AWS) puerta de entrada: revisa el token y deja pasar
└──────┬───────┘
       ▼
┌──────────────┐  guarda datos  ┌─────────┐
│   BACKEND    │ ─────────────▶ │  MySQL  │
│ (Spring Boot)│                └─────────┘
└──┬────────┬──┘
   │        │ 3. avisa que pasó algo ("evento")
   ▼        ▼
┌────────┐ ┌────────┐
│RabbitMQ│ │ Kafka  │   mensajería: otros procesos reaccionan al evento
└────────┘ └────────┘
   ▲        ▲
   └───┬────┘
┌──────────────┐
│ADMIN-SERVICE │  crea y borra colas y topics (solo administradores)
└──────────────┘
```

| Pieza | Qué hace, en una frase | Tecnología | Puerto local |
|---|---|---|---|
| **Frontend** | La página web que ven los usuarios | React + TypeScript + Vite | `80` |
| **Backend** | Recibe las peticiones, valida, guarda en la base de datos y publica eventos | Java 21 + Spring Boot | `5000` |
| **Admin-service** | API para administrar RabbitMQ y Kafka (crear y borrar colas y topics) | Java 21 + Spring Boot | `5001` |
| **MySQL** | Base de datos (en la nube es AWS RDS) | MySQL 8 | `3306` |
| **RabbitMQ** | Cola de mensajes: «avísale a alguien que hay una reserva nueva» | RabbitMQ 4.2 (3 nodos) | `5672` / panel `15672` |
| **Kafka** | Registro de eventos: «guarda el historial de lo que pasó para que otros lo procesen» | Kafka 7.6 (2 brokers + Zookeeper) | `9092`, `9093` |
| **Kafka-UI** | Página para ver Kafka por dentro | provectuslabs/kafka-ui | `8080` |
| **Azure Entra ID** | Hace el login con cuenta Microsoft | Azure | — |
| **API Gateway** | Puerta de entrada pública a la API | AWS (Terraform) | — |

---

## 2. ¿Dónde está cada cosa?

```
clinica-nutrivida/
├── frontend/                  ← Página web (React)
│   └── src/
│       ├── App.tsx            ← Pantallas: inicio, reservas, panel nutricionista, secretaria y admin
│       ├── api.ts             ← Llamadas al backend (agrega el token automáticamente)
│       ├── auth.ts            ← Configuración del login de Azure (MSAL)
│       ├── roles.ts           ← Decide el rol del usuario (Paciente, Secretaria...)
│       ├── ProtectedRoute.tsx ← Bloquea páginas si no tienes sesión o rol
│       └── environments/environment.ts ← URLs e IDs de configuración
│
├── backend/                   ← API principal (Spring Boot)
│   └── src/main/java/com/nutrivida/backend/
│       ├── controller/        ← Endpoints REST (/api/reservas, /api/fichas, ...)
│       ├── service/           ← Lógica de negocio (validar horarios, calcular IMC)
│       ├── model/             ← Tablas de la base de datos (Reserva, FichaNutricional...)
│       ├── repository/        ← Acceso a la base de datos (JPA)
│       ├── config/            ← Seguridad, RabbitMQ y Kafka
│       ├── security/          ← Lectura de roles del token
│       └── messaging/         ← Todo lo de mensajería
│           ├── ClinicaEventPublisher.java  ← Único punto por donde se publican eventos
│           ├── event/         ← Formato de los mensajes (JSON)
│           ├── rabbitmq/      ← Productor y consumidores de RabbitMQ
│           └── kafka/         ← Productor y consumidores de Kafka
│
├── admin-service/             ← Microservicio administrador de RabbitMQ y Kafka
├── terraform/                 ← Infraestructura AWS (API Gateway)
├── infra/rabbitmq/            ← Configuración del clúster RabbitMQ
├── docs/kafka-casos-de-uso.md ← Detalle de los mensajes Kafka
└── docker-compose.yml         ← Levanta TODO en local con un comando
```

**Regla de oro del backend:** cada carpeta tiene una sola responsabilidad.

```
controller  →  service  →  repository  →  MySQL
                  │
                  └──→  messaging (RabbitMQ / Kafka)
```

El `controller` solo recibe la petición, el `service` decide y el `repository` guarda. El `service` **no sabe** cómo funcionan RabbitMQ ni Kafka: solo llama a `ClinicaEventPublisher`.

---

## 3. Cómo levantar todo en tu PC

### Requisitos
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) abierto
- (Opcional, para programar) Java 21, Maven y Node.js 22

### Un solo comando

```bash
docker compose up -d --build
```

Espera 1 o 2 minutos y abre:

| Qué | URL | Usuario |
|---|---|---|
| Página web | http://localhost | Tu cuenta Microsoft |
| Panel de RabbitMQ | http://localhost:15672 | `admin` / `admin123` |
| Kafka-UI | http://localhost:8080 | — |
| Backend | http://localhost:5000/api | Necesita token |

### Comandos útiles

```bash
docker compose ps                         # ver qué está corriendo
docker logs -f nutrivida-backend          # ver los logs del backend en vivo
docker compose up -d --build backend      # reconstruir solo el backend tras un cambio
docker compose down                       # apagar todo (los datos se conservan)
docker compose down -v                    # apagar y BORRAR los datos
```

### Programar sin Docker (más rápido para probar cambios)

```bash
# Frontend
cd frontend
npm install
npm start                # http://localhost:5173

# Backend (necesita MySQL, RabbitMQ y Kafka; puedes levantarlos con docker compose)
cd backend
mvn spring-boot:run      # http://localhost:5000
```

---

## 4. Recorrido completo: ¿qué pasa cuando un paciente reserva?

Esta es la mejor forma de entender cómo se conectan todas las piezas.

```
Paciente      Frontend         Backend              MySQL     RabbitMQ      Kafka
   │  clic       │                │                    │          │            │
   │──"Reservar"▶│                │                    │          │            │
   │             │ POST /reservas │                    │          │            │
   │             │ + token ──────▶│ ¿token válido?     │          │            │
   │             │                │ ¿horario válido?   │          │            │
   │             │                │── guardar ────────▶│          │            │
   │             │                │── evento ──────────┼─────────▶│            │
   │             │                │── evento ──────────┼──────────┼───────────▶│
   │             │◀─── 200 OK ────│                    │          │            │
   │◀─ "¡Listo!"─│                │                    │   consumidor   consumidor
   │             │                │                    │   envía aviso  genera notificación
```

### Paso 1: el frontend envía la reserva con el token

`frontend/src/api.ts`

```ts
// Cada llamada al backend pasa por aquí y se le adjunta el token de Azure
async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers);
  headers.set('Content-Type', 'application/json');

  const accessToken = await getAccessToken();          // pide el token a Azure
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`);

  const response = await fetch(`${apiUrl}/api${path}`, { ...init, headers });
  // ...
}

export const clinicApi = {
  crearReserva: (reserva: Reserva) =>
    request<Reserva>('/reservas', { method: 'POST', body: JSON.stringify(reserva) }),
  // ...
};
```

### Paso 2: el controller la recibe

`backend/.../controller/ClinicaController.java`

```java
@PostMapping("/reservas")
public ResponseEntity<?> crearReserva(@RequestBody Reserva reserva) {
    try {
        Reserva nuevaReserva = clinicaService.guardarReserva(reserva);
        return ResponseEntity.ok(nuevaReserva);
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());   // 400 si los datos no son válidos
    }
}
```

### Paso 3: el service valida, guarda y publica el evento

`backend/.../service/ClinicaService.java`

```java
private Reserva validarYGuardar(Reserva reserva, TipoEvento tipoEvento) {
    if (reserva.getFecha().isBefore(LocalDate.now())) {
        throw new IllegalArgumentException("La fecha de reserva no puede ser en el pasado.");
    }
    // ... valida el horario de 09:00 a 20:00

    Reserva reservaGuardada = reservaRepository.save(reserva);                        // → MySQL
    eventPublisher.publicarReserva(ReservaEvent.from(reservaGuardada, tipoEvento));  // → RabbitMQ y Kafka
    return reservaGuardada;
}
```

### Paso 4: el publicador lo manda a RabbitMQ y a Kafka

`backend/.../messaging/ClinicaEventPublisher.java`

```java
public void publicarReserva(ReservaEvent event) {
    rabbitProducer.enviarNotificacionReserva(event);   // RabbitMQ
    kafkaProducer.ifAvailable(producer ->              // Kafka (solo si está activado)
        producer.publicar(reservasTopic, String.valueOf(event.reservaId()), event));
}
```

### Paso 5: los consumidores reaccionan

Cada broker tiene un consumidor que procesa el evento por su cuenta. Se explican en las secciones [6](#6-rabbitmq-las-colas) y [7](#7-kafka-los-eventos).

> 💡 **¿Por qué usar mensajería y no hacerlo todo directo?** Si enviar el aviso al paciente falla o tarda, la reserva igual queda guardada y el usuario recibe su respuesta al instante. El aviso se procesa aparte.

---

## 5. Seguridad: login y permisos

### ¿Cómo funciona el token?

1. El usuario inicia sesión con Microsoft (Azure Entra ID).
2. Azure le entrega un **token JWT**: un texto firmado que dice «este es Juan y tiene el rol X».
3. El frontend envía ese token en cada petición (`Authorization: Bearer <token>`).
4. El backend **verifica** el token antes de responder.

### Frontend: pedir el token

`frontend/src/api.ts`

```ts
export async function getAccessToken(): Promise<string | null> {
  const account = msalInstance.getActiveAccount() ?? msalInstance.getAllAccounts()[0];
  if (!account) return null;

  try {
    // Intenta obtenerlo sin molestar al usuario
    const tokenResponse = await msalInstance.acquireTokenSilent({ scopes: environment.apiScopes, account });
    return tokenResponse.accessToken;
  } catch (error) {
    if (!(error instanceof InteractionRequiredAuthError)) throw error;
    // Si Azure pide confirmación, abre una ventana emergente
    const tokenResponse = await msalInstance.acquireTokenPopup({ scopes: environment.apiScopes, account });
    return tokenResponse.accessToken;
  }
}
```

### Frontend: proteger páginas

`frontend/src/ProtectedRoute.tsx`

```tsx
export function ProtectedRoute({ roles, children }: ProtectedRouteProps) {
  const { accounts, inProgress } = useMsal();
  const isAuthenticated = useIsAuthenticated();

  if (inProgress !== InteractionStatus.None) return null;
  if (!isAuthenticated) return <Navigate to="/iniciar-sesion" replace />;                  // sin sesión → login
  if (roles && !roles.includes(roleOf(accounts[0]))) return <Navigate to="/" replace />;  // sin rol → inicio

  return <>{children}</>;
}

// Uso en App.tsx: solo el Administrador puede entrar a /admin
<Route path="/admin" element={<ProtectedRoute roles={['Administrador']}><AdminPage /></ProtectedRoute>} />
```

### Backend: verificar el token

`backend/.../config/SecurityConfig.java` revisa 4 cosas de cada token:

| Revisión | Pregunta | Si falla |
|---|---|---|
| **Firma** | ¿Lo firmó realmente Microsoft? | 401 |
| **Vigencia** | ¿Ya expiró? | 401 |
| **Issuer** | ¿Viene de nuestro tenant de Azure? | 401 |
| **Audience** | ¿Es un token para *esta* API? | 401 |

```java
static OAuth2TokenValidator<Jwt> tokenValidator(SecurityProperties securityProperties) {
    OAuth2TokenValidator<Jwt> timestamp = new JwtTimestampValidator(Duration.ofSeconds(60));          // vigencia
    OAuth2TokenValidator<Jwt> issuer = new JwtClaimValidator<String>("iss", issuers::contains);       // emisor
    OAuth2TokenValidator<Jwt> audience = jwt -> /* ¿aud está en la lista permitida? */;               // destinatario
    return new DelegatingOAuth2TokenValidator<>(timestamp, issuer, audience);
}
```

### Backend: permisos por rol

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()                                   // preflight CORS
    .requestMatchers(HttpMethod.POST, "/api/nutricionistas/**").hasRole(ADMINISTRADOR)
    .requestMatchers(HttpMethod.DELETE, "/api/nutricionistas/**").hasRole(ADMINISTRADOR)
    .requestMatchers(HttpMethod.GET, "/api/nutricionistas/**").hasAnyRole(SECRETARIA, ADMINISTRADOR)
    .requestMatchers("/api/fichas/**").hasAnyRole(NUTRICIONISTA, ADMINISTRADOR)
    .requestMatchers(HttpMethod.POST, "/api/reservas").hasAnyRole(PACIENTE, SECRETARIA, ADMINISTRADOR)
    .requestMatchers("/api/reservas/**").hasAnyRole(SECRETARIA, ADMINISTRADOR)
    .anyRequest().authenticated()                                                             // nada es anónimo
)
```

En una tabla:

| Acción | Paciente | Nutricionista | Secretaria | Administrador |
|---|:-:|:-:|:-:|:-:|
| Crear reserva | ✅ | | ✅ | ✅ |
| Ver y reagendar reservas | | | ✅ | ✅ |
| Fichas nutricionales | | ✅ | | ✅ |
| Ver nutricionistas | | | ✅ | ✅ |
| Crear y borrar nutricionistas | | | | ✅ |
| Admin-service (colas y topics) | | | | ✅ |

**Códigos de respuesta:**
- `401 Unauthorized`: no hay token, es falso o ya expiró.
- `403 Forbidden`: el token es válido, pero tu rol no tiene permiso.

### ¿De dónde sale el rol?

`backend/.../security/AzureJwtRolesConverter.java` lo busca en este orden:

1. El claim `roles` del token (si en Azure se asignaron App Roles).
2. Si no viene, el **correo** del usuario en `app.security.role-mappings` (`application.yml`).
3. Si tampoco aparece ahí, el usuario es **Paciente**.

---

## 6. RabbitMQ: las colas

**Idea simple:** RabbitMQ es como un buzón. El backend deja una carta (mensaje) y un consumidor la retira y la procesa. Si la carta viene dañada, va a un buzón de «cartas con problemas» (DLQ).

```
Productor ──▶ Exchange ──(routing key)──▶ clinica.reserva.queue ──▶ ReservaRabbitConsumer
                                                 │                       │
                                                 │                  ✅ ACK (procesado)
                                                 │                  ❌ NACK (falló)
                                                 ▼                       │
                              clinica.reserva.dlx ◀──────────────────────┘
                                                 │
                                                 ▼
                                    clinica.reserva.dlq ──▶ ReservaDlqConsumer (lo anota en el log)
```

### Los nombres se definen en un solo lugar

`backend/src/main/resources/application.yml`

```yaml
rabbitmq:
  queue:
    reserva:
      main: clinica.reserva.queue
      dlq: clinica.reserva.dlq
  exchange:
    reserva:
      main: clinica.reserva.exchange
      dlx: clinica.reserva.dlx
  routing:
    reserva:
      main: reserva.routing.key
      dlq: reserva.dlq.routing.key
```

`config/RabbitMQConfig.java` crea las colas con esos nombres. La cola principal se enlaza con la DLQ así:

```java
@Bean
public Queue reservaQueuePrincipal() {
    return QueueBuilder.durable(reservaQueue)
            .withArgument("x-dead-letter-exchange", reservaDlx)          // si un mensaje se rechaza...
            .withArgument("x-dead-letter-routing-key", reservaDlqRoutingKey)  // ...va a la DLQ
            .build();
}
```

### El consumidor confirma a mano (ACK / NACK)

`backend/.../messaging/rabbitmq/reserva/ReservaRabbitConsumer.java`

```java
@RabbitListener(id = "reserva-rabbit-consumer", queues = "${rabbitmq.queue.reserva.main}")
public void recibirNotificacionReserva(Message message, Channel channel,
                                       @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
    try {
        ReservaEvent event = objectMapper.readValue(message.getBody(), ReservaEvent.class);
        log.info("[RabbitMQ] Reserva {} {} para {} ...", event.reservaId(), event.tipoEvento(), ...);

        channel.basicAck(tag, false);          // ✅ "lo procesé bien, bórralo de la cola"
    } catch (Exception e) {
        log.error("[RabbitMQ] Mensaje no procesado, se envía a la DLQ. Motivo: {}", e.getMessage());
        channel.basicNack(tag, false, false);  // ❌ "falló; no lo reintentes, mándalo a la DLQ"
    }
}
```

### Para verlo funcionar
1. Abre http://localhost:15672 (`admin` / `admin123`).
2. En la pestaña **Queues** verás `clinica.reserva.queue` y `clinica.reserva.dlq`.
3. En **Overview → Nodes** verás los 3 nodos del clúster.

---

## 7. Kafka: los eventos

**Idea simple:** Kafka es como un diario de vida del sistema. Cada vez que pasa algo importante se escribe una línea (evento), y los consumidores leen el diario y reaccionan. A diferencia de RabbitMQ, los eventos **no se borran al leerse**: quedan guardados 7 días.

### Los 2 casos de uso (uno por topic)

| | Caso 1: Reservas | Caso 2: Fichas nutricionales |
|---|---|---|
| **Topic** | `clinica.reservas.eventos` | `clinica.fichas.eventos` |
| **¿Cuándo?** | Se crea o reagenda una reserva | El nutricionista registra una ficha |
| **Entra** | `ReservaEvent` | `FichaEvent` |
| **El consumidor...** | arma la notificación para el paciente | calcula el **IMC** y lo guarda en la ficha |
| **Sale** | `NotificacionReserva` | `EvaluacionNutricional` |
| **Si falla** | `clinica.reservas.eventos.DLT` | `clinica.fichas.eventos.DLT` |

📄 Todos los JSON de entrada y salida están en [`docs/kafka-casos-de-uso.md`](docs/kafka-casos-de-uso.md).

### Ejemplo: caso 2 completo

**1. Mensaje de entrada** (lo publica el backend al guardar una ficha):

```json
{ "fichaId": 7, "pacienteRut": "11.111.111-1", "pesoActual": 90.0, "estatura": 170 }
```

**2. El consumidor lo procesa**

`backend/.../messaging/kafka/ficha/FichaKafkaConsumer.java`

```java
@KafkaListener(id = "ficha-kafka-consumer",
        topics = "${app.kafka.topics.fichas}",
        groupId = "${app.kafka.consumer-groups.fichas}",
        properties = "spring.json.value.default.type=com.nutrivida.backend.messaging.event.FichaEvent")
public void evaluarFicha(@Payload FichaEvent event, ...) {
    EvaluacionNutricional evaluacion = calculator.evaluar(event);   // calcula el IMC

    fichaRepository.findById(event.fichaId()).ifPresentOrElse(ficha -> {
        ficha.setImc(evaluacion.imc());                             // lo guarda en la ficha
        ficha.setClasificacionImc(evaluacion.clasificacion());
        fichaRepository.save(ficha);
    }, () -> log.warn(...));
}
```

**3. El cálculo** (`service/EvaluacionNutricionalCalculator.java`):

```java
double metros = ficha.estatura() / 100.0;
double imc = redondear(ficha.pesoActual() / (metros * metros));   // 90 / 1.70² = 31.1
// 31.1 → "Obesidad grado I"
```

**4. Mensaje de salida:**

```json
{ "fichaId": 7, "imc": 31.1, "clasificacion": "Obesidad grado I", "pesoSaludableMin": 53.5, "pesoSaludableMax": 72.0 }
```

**5. En la web:** el nutricionista ve el IMC en su tabla de fichas unos segundos después.

### ¿Qué pasa si algo falla?

`backend/.../config/KafkaConfig.java`

```java
ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);  // hasta 3 reintentos
backOff.setInitialInterval(1000);   // espera 1 s, luego 2 s, luego 4 s...
backOff.setMultiplier(2.0);

DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);  // recoverer = enviar al DLT
errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);         // datos malos: no reintentar
```

| Tipo de error | Ejemplo | ¿Qué hace? |
|---|---|---|
| Datos inválidos | Ficha sin estatura | Va **directo** al DLT |
| JSON roto | `"esto no es json"` | Va **directo** al DLT |
| Error temporal | La base de datos no responde | **Reintenta** 3 veces y después va al DLT |

Todo lo que llega a un DLT queda en el log con el motivo (`DeadLetterKafkaConsumer`).

### Para verlo funcionar
1. Abre Kafka-UI: http://localhost:8080
2. En **Topics** verás los 4 topics (cada uno con 3 particiones y 2 réplicas).
3. En **Consumers** verás los grupos y su *lag* (mensajes pendientes por leer).

O publica un evento a mano:

```bash
echo '33|{"fichaId":33,"pacienteRut":"33.333.333-3","pesoActual":80}' \
  | docker exec -i kafka1 kafka-console-producer --bootstrap-server kafka1:29092 \
      --topic clinica.fichas.eventos --property parse.key=true --property key.separator='|'

docker logs nutrivida-backend | grep "Kafka DLT"     # como falta la estatura, aparece en el DLT
```

> ⚙️ Kafka se activa con la variable `APP_KAFKA_ENABLED=true` (ya viene así en `docker-compose.yml`). Si está en `false`, el backend funciona igual, pero sin Kafka.

---

## 8. Admin-service: el panel de control de la mensajería

Es un microservicio aparte (puerto `5001`) que permite **crear, ver y borrar** colas y topics por medio de una API REST. Solo lo puede usar el rol **Administrador**.

### RabbitMQ

| Método | Ruta | Para qué |
|---|---|---|
| `POST` | `/api/admin/rabbitmq/queues` | Crear una cola |
| `GET` | `/api/admin/rabbitmq/queues/{name}` | Ver cuántos mensajes y consumidores tiene |
| `DELETE` | `/api/admin/rabbitmq/queues/{name}` | Borrar una cola |
| `POST` / `DELETE` | `/api/admin/rabbitmq/exchanges` | Crear o borrar un exchange |
| `POST` / `DELETE` | `/api/admin/rabbitmq/bindings` | Conectar o desconectar un exchange con una cola |

### Kafka

| Método | Ruta | Para qué |
|---|---|---|
| `POST` | `/api/admin/kafka/topics` | Crear un topic (particiones, réplicas, retención, cleanup policy) |
| `GET` | `/api/admin/kafka/topics` | Listar los topics |
| `GET` / `DELETE` | `/api/admin/kafka/topics/{name}` | Ver o borrar un topic |
| `PUT` | `/api/admin/kafka/topics/{name}/configs` | Cambiar la retención o la cleanup policy |
| `POST` | `/api/admin/kafka/topics/{name}/partitions` | Aumentar las particiones |
| `GET` | `/api/admin/kafka/consumer-groups` | Listar los consumer groups |
| `GET` / `DELETE` | `/api/admin/kafka/consumer-groups/{id}` | Ver (miembros, offsets, lag) o borrar un consumer group |

### Ejemplos

```bash
# Crear un topic con 3 particiones, 2 réplicas y retención de 1 día
curl -X POST http://localhost:5001/api/admin/kafka/topics \
  -H "Authorization: Bearer <TOKEN_ADMIN>" -H "Content-Type: application/json" \
  -d '{"name":"clinica.pagos","partitions":3,"replicationFactor":2,"retentionMs":86400000,"cleanupPolicy":"delete"}'

# Crear una cola durable que manda sus errores al DLX de reservas
curl -X POST http://localhost:5001/api/admin/rabbitmq/queues \
  -H "Authorization: Bearer <TOKEN_ADMIN>" -H "Content-Type: application/json" \
  -d '{"name":"pagos.queue","deadLetterExchange":"clinica.reserva.dlx"}'
```

### Las entradas se validan

`admin-service/.../kafka/dto/KafkaDtos.java`

```java
public record CreateTopicRequest(
        @NotBlank @Pattern(regexp = TOPIC_REGEX, message = "Nombre de tópico inválido") String name,
        @NotNull @Min(1) @Max(1000) Integer partitions,
        @NotNull @Min(1) @Max(10) Short replicationFactor,
        @Min(-1) Long retentionMs,
        @Pattern(regexp = "delete|compact|compact,delete|delete,compact") String cleanupPolicy,
        Map<String, String> configs) { }
```

| Respuesta | Cuándo |
|---|---|
| `201` / `200` / `204` | Todo bien |
| `400` | Datos inválidos (nombre con espacios, réplicas > brokers...) |
| `404` | La cola, el topic o el grupo no existe |
| `409` | Ya existe, o el grupo tiene consumidores activos |
| `503` | RabbitMQ o Kafka no responde |

---

## 9. Configuración (`application.yml`)

Toda la configuración del backend está en `backend/src/main/resources/application.yml`. Los valores con `${VARIABLE:valor}` se pueden cambiar con variables de entorno **sin tocar el código**:

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:mysql://...rds.amazonaws.com:3306/nutrivida}
  rabbitmq:
    addresses: ${SPRING_RABBITMQ_ADDRESSES:3.84.123.87:5672}
  kafka:
    bootstrap-servers: ${SPRING_KAFKA_BOOTSTRAP_SERVERS:localhost:9092,localhost:9093}

app:
  kafka:
    enabled: ${APP_KAFKA_ENABLED:false}
```

| Variable | Para qué | Ejemplo |
|---|---|---|
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | Base de datos | `jdbc:mysql://mysql-db:3306/clinica_nutrivida` |
| `SPRING_RABBITMQ_ADDRESSES` | Nodos de RabbitMQ | `rabbitmq1:5672,rabbitmq2:5672` |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Brokers de Kafka | `kafka1:29092,kafka2:29092` |
| `APP_KAFKA_ENABLED` | Activar Kafka | `true` |
| `APP_CORS_ALLOWED_ORIGINS` | Desde qué páginas se puede llamar a la API | `http://localhost,https://midominio.cl` |
| `AZURE_TENANT_ID` / `AZURE_CLIENT_ID` | Datos de la app registrada en Azure | — |

En el **frontend**, las variables van en build: `VITE_API_URL`, `VITE_AZURE_CLIENT_ID` y `VITE_AZURE_TENANT_ID` (ver `src/environments/environment.ts`).

---

## 10. Pruebas

```bash
cd backend       && mvn test   # 27 pruebas
cd admin-service && mvn test   # 5 pruebas
cd frontend      && npm test   # 8 pruebas
```

Ninguna necesita base de datos ni brokers encendidos: usan *mocks* (objetos falsos que simulan las dependencias).

| Archivo de prueba | Qué comprueba |
|---|---|
| `SecurityConfigTest` | 401 sin token, 403 con rol incorrecto, issuer/audience/expiración y CORS |
| `AzureJwtRolesConverterTest` | Que el rol se lea bien del token o del correo |
| `ClinicaServiceTest` | Que no se pueda reservar en el pasado ni fuera de horario, y que se publiquen los eventos |
| `EvaluacionNutricionalCalculatorTest` | El cálculo del IMC y su clasificación OMS |
| `ConsumidoresTest` | ACK/NACK de RabbitMQ y que los datos malos de Kafka vayan al DLT |
| `AdminSecurityTest` | Permisos y validaciones del admin-service |
| `api.test.ts` | Que el frontend envíe el token y maneje los errores |

Ejemplo de prueba, `ClinicaServiceTest.java`:

```java
@Test
void rechazaReservaEnElPasadoOFueraDeHorario() {
    assertThatThrownBy(() -> clinicaService.guardarReserva(reserva(LocalDate.now().minusDays(1), LocalTime.of(10, 0))))
            .isInstanceOf(IllegalArgumentException.class);   // ayer → error
    assertThatThrownBy(() -> clinicaService.guardarReserva(reserva(LocalDate.now().plusDays(1), LocalTime.of(21, 0))))
            .isInstanceOf(IllegalArgumentException.class);   // 21:00 → error

    verify(reservaRepository, never()).save(any());           // no se guardó nada
    verifyNoInteractions(eventPublisher);                     // y no se publicó ningún evento
}
```

---

## 11. Glosario

| Término | En palabras simples |
|---|---|
| **JWT / Token** | «Pase de acceso» firmado por Microsoft que dice quién eres y qué rol tienes |
| **MSAL** | Librería de Microsoft que hace el login en el frontend |
| **BFF** | *Backend For Frontend*: el backend que atiende directamente a la web |
| **API Gateway** | Puerta de entrada pública de AWS hacia el backend |
| **CORS** | Regla del navegador que define qué páginas pueden llamar a la API |
| **Exchange** | (RabbitMQ) «Cartero» que decide a qué cola va cada mensaje |
| **Queue / Cola** | (RabbitMQ) Buzón donde esperan los mensajes |
| **Routing key** | (RabbitMQ) «Dirección» que usa el exchange para elegir la cola |
| **ACK / NACK** | «Lo procesé bien» / «falló» |
| **DLQ / DLT** | Dead Letter Queue / Topic: donde terminan los mensajes que fallaron |
| **Topic** | (Kafka) Canal temático de eventos, como un «diario» por tema |
| **Partición** | (Kafka) Un topic se divide en partes para procesarse en paralelo |
| **Réplica** | (Kafka) Copia de una partición en otro broker, por si uno se cae |
| **Consumer group** | (Kafka) Equipo de consumidores que se reparten las particiones |
| **Lag** | (Kafka) Cuántos mensajes le faltan por leer a un grupo |
| **Retención** | (Kafka) Cuánto tiempo se guardan los eventos (aquí, 7 días) |

---

## 12. Problemas comunes

| Síntoma | Causa probable | Solución |
|---|---|---|
| `401` en todas las llamadas | No iniciaste sesión o el token expiró | Cierra sesión y vuelve a entrar |
| `403` en una pantalla | Tu cuenta no tiene ese rol | Revisa `role-mappings` en `application.yml` y `roles.ts` |
| Error de CORS en la consola del navegador | La página no está en la lista de orígenes permitidos | Agrégala en `APP_CORS_ALLOWED_ORIGINS` |
| La columna IMC dice «En cálculo» | Kafka está apagado o la ficha no tiene estatura | Revisa `APP_KAFKA_ENABLED` y `docker logs nutrivida-backend \| grep Kafka` |
| `curl localhost` no responde en Windows | `localhost` se resuelve a IPv6 | Usa `127.0.0.1` |
| Un nodo de RabbitMQ queda *unhealthy* | Los volúmenes tienen un clúster viejo | `docker compose down -v` y vuelve a levantar |
| Los puertos `80`, `3306` o `8080` están ocupados | Otro programa los usa | Ciérralo o cambia el puerto en `docker-compose.yml` |
