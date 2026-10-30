# Casos de uso Kafka y modelos de mensajes

El backend (BFF) es el **productor** de ambos topics y aloja también sus **consumidores** (`messaging/kafka/*`).
Los mensajes viajan en JSON (`JsonSerializer` / `JsonDeserializer`) con la key indicada en cada caso.
Si un mensaje falla, se reintenta 3 veces con backoff exponencial; los datos inválidos y el JSON mal formado van directo al DLT (`<topic>.DLT`) sin reintentos y quedan registrados en el log.

| Topic | Particiones / réplicas | Retención | Consumer group | DLT |
|---|---|---|---|---|
| `clinica.reservas.eventos` | 3 / 2 | 7 días, `delete` | `nutrivida-reservas-group` | `clinica.reservas.eventos.DLT` |
| `clinica.fichas.eventos` | 3 / 2 | 7 días, `delete` | `nutrivida-fichas-group` | `clinica.fichas.eventos.DLT` |

Los dos DLT los lee `nutrivida-dlt-group`.

---

## Caso de uso 1: notificación de reservas

**Actor:** paciente o secretaria.
**Disparador:** se crea una reserva (`POST /api/reservas`) o se reagenda (`PUT /api/reservas/{id}`).

1. `ClinicaService` guarda la reserva en MySQL.
2. `ClinicaEventPublisher` publica un `ReservaEvent` en `clinica.reservas.eventos` (key = id de la reserva, así los eventos de una misma reserva conservan su orden). El mismo evento también va a RabbitMQ.
3. `ReservaKafkaConsumer` valida el evento y genera la `NotificacionReserva` para el paciente.

**Entrada:** `ReservaEvent` publicado en `clinica.reservas.eventos`

```json
{
  "reservaId": 12,
  "tipoEvento": "CREADA",
  "pacienteNombre": "Ana Pérez",
  "nutricionistaNombre": "Dra. Gómez",
  "tipoAtencion": "Control",
  "modalidad": "Online",
  "fecha": "2027-01-10",
  "hora": "10:00:00",
  "estado": "Pendiente",
  "ocurridoEn": "2026-10-30T18:00:00Z"
}
```

`tipoEvento` puede ser `CREADA` o `REAGENDADA`. Si faltan `reservaId`, `tipoEvento` o `pacienteNombre`, el evento va al DLT.

**Salida:** `NotificacionReserva` generada por el consumidor

```json
{
  "reservaId": 12,
  "destinatario": "Ana Pérez",
  "canal": "EMAIL_CON_ENLACE",
  "asunto": "Tu hora fue agendada",
  "mensaje": "Hola Ana Pérez, tu consulta de Control (Online) es el 2027-01-10 a las 10:00 con Dra. Gómez.",
  "generadaEn": "2026-10-30T18:00:01Z"
}
```

---

## Caso de uso 2: evaluación nutricional de la ficha

**Actor:** nutricionista.
**Disparador:** se registra una ficha nutricional (`POST /api/fichas`).

1. `ClinicaService` guarda la ficha y publica un `FichaEvent` en `clinica.fichas.eventos` (key = RUT del paciente).
2. `FichaKafkaConsumer` calcula el IMC, la clasificación OMS y el rango de peso saludable (`EvaluacionNutricionalCalculator`).
3. El resultado se guarda en la ficha (`imc`, `clasificacionImc`), y el panel del nutricionista lo muestra en la columna **IMC**.

**Entrada:** `FichaEvent` publicado en `clinica.fichas.eventos`

```json
{
  "fichaId": 7,
  "pacienteRut": "11.111.111-1",
  "pesoActual": 90.0,
  "estatura": 170,
  "grasaCorporal": null,
  "cintura": null,
  "motivoConsulta": "Control",
  "registradaEn": "2026-10-30T18:10:00Z"
}
```

`estatura` va en centímetros (50 a 250) y `pesoActual` en kg (debe ser mayor que 0). Si están fuera de rango, el evento va al DLT. Si falla la base de datos, el evento se reintenta.

**Salida:** `EvaluacionNutricional` calculada y guardada en la ficha

```json
{
  "fichaId": 7,
  "pacienteRut": "11.111.111-1",
  "imc": 31.1,
  "clasificacion": "Obesidad grado I",
  "pesoSaludableMin": 53.5,
  "pesoSaludableMax": 72.0,
  "evaluadaEn": "2026-10-30T18:10:01Z"
}
```

Clasificación del IMC según la OMS:

| IMC | Clasificación |
|---|---|
| < 18.5 | Bajo peso |
| 18.5 a 24.9 | Normal |
| 25 a 29.9 | Sobrepeso |
| 30 a 34.9 | Obesidad grado I |
| 35 a 39.9 | Obesidad grado II |
| ≥ 40 | Obesidad grado III |

---

## Probar a mano (docker-compose levantado)

```bash
# Caso de uso 1: evento válido
echo '12|{"reservaId":12,"tipoEvento":"CREADA","pacienteNombre":"Ana","modalidad":"Online","fecha":"2027-01-10","hora":"10:00:00"}' \
  | docker exec -i kafka1 kafka-console-producer --bootstrap-server kafka1:29092 --topic clinica.reservas.eventos --property parse.key=true --property key.separator='|'

# Caso de uso 2: evento sin estatura (va al DLT)
echo '33|{"fichaId":33,"pacienteRut":"33.333.333-3","pesoActual":80}' \
  | docker exec -i kafka1 kafka-console-producer --bootstrap-server kafka1:29092 --topic clinica.fichas.eventos --property parse.key=true --property key.separator='|'

docker logs -f nutrivida-backend | grep Kafka
```

En Kafka-UI (http://localhost:8080) se pueden ver los topics, los mensajes, los consumer groups y su lag.
