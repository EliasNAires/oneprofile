# Paso 02 — logs de avance del sondeo y la carga

## Qué queda funcionando
Mientras corren el sondeo (`POST /admin/probe/greenhouse`) y la carga de vacantes
(`POST /admin/vacancies/greenhouse`), el log muestra cada N empresas cuántas van, el total,
el %, las fallas acumuladas, el ritmo por minuto y el tiempo transcurrido. Si la corrida
revienta, la última línea dice `aborted` (no `failed`), para que un `grep` de fin no se
confunda con los WARN de una empresa que falló.

## Contexto necesario
- Hoy el sondeo (~18.000 empresas, ~130/min, ~2 h 20 min) y la carga (~6.900 `ACTIVE`,
  ~62/min, ~1 h 50 min) solo loguean `started` y `finished` desde su controller; el avance se
  deducía con SQL. Elias quiere ver el avance para confirmar que va bien o frenar a mano: la
  app **no** aborta sola.
- Líneas actuales (prod, 2026-09-14):
  `Greenhouse board probe finished: 18051 companies, 9787 not found, 1381 empty, 6883 active, 0 failed`
  `Greenhouse vacancy sweep finished: 6883 companies, 216867 fetched, 89376 inserted, 127491 updated, 1461 deleted, 0 failed`.
  Esas líneas `finished` se conservan tal cual.
- WARN por empresa que ya existen: `Probing the Greenhouse board of {} failed: {}` y
  `Loading the Greenhouse openings of {} failed: {}`. Contienen `failed`: por eso el cierre
  por excepción pasa a `aborted`.
- Intervalo por **cantidad**, no por tiempo (se testea sin reloj real): sondeo cada **1.000**
  empresas (~8 min), carga cada **500** (~8 min). No se loguea avance en la última (ya está
  `finished`).
- Formato de la línea (igual para todos los procesos; el paso de descubrimiento lo reusa):
  `<proceso> progress: <hechos> of <total> <unidad> (<pct>%), <fallas> failed, <ritmo> per min, <transcurrido> elapsed`
  p. ej. `Greenhouse board probe progress: 1000 of 18051 companies (5%), 0 failed, 128 per min, 7m49s elapsed`.
- Diseño: un util nuevo `oneprofile.backend.util.ProgressLog` (funciones puras/helpers sin
  Spring; `slf4j` está permitido, como en `HttpRetry`), porque lo usan sondeo, carga y después
  descubrimiento. El tiempo entra por `java.time.Clock` para que el test no espere; los
  services mantienen el patrón de constructor público `@Autowired` + constructor de paquete
  para el test (ver `GreenhouseBoardProbeService`).
- Reglas del repo: si no se usa o no se pidió, no se pone; ante una duda, parar y preguntar.
  Tests solo de comportamiento.

Leer antes de tocar: `service/GreenhouseBoardProbeService.java`,
`service/GreenhouseVacancySweepService.java`, `controller/BoardProbeController.java`,
`controller/VacancyController.java`, `util/HttpRetry.java` (estilo de util con logger) y sus
tests en `src/test/java/oneprofile/backend/`.

## Qué se toca
- `util/ProgressLog.java` (nuevo): se crea con nombre del proceso, unidad, total, intervalo y
  `Clock`; se le avisa cada ítem hecho (y si falló) y loguea en INFO según el formato de arriba.
- `GreenhouseBoardProbeService.probeAll`: avisa a `ProgressLog` por empresa (intervalo 1.000).
- `GreenhouseVacancySweepService.syncAllActive`: ídem (intervalo 500).
- `BoardProbeController` y `VacancyController`: el `logger.error(... failed, ex)` del cierre
  pasa a `... aborted`. `started` y `finished` no cambian.

## Tests
- `ProgressLogTest`: con total 25 e intervalo 10 loguea a los 10 y 20, no en otros; cuenta
  fallas; calcula % y ritmo con un `Clock` fijo que avanza; no loguea en el último ítem.
  Capturar el log con `OutputCaptureExtension` de Spring Boot (o equivalente), sin cambiar la
  API del util por el test.
- `GreenhouseBoardProbeServiceTest` / `GreenhouseVacancySweepServiceTest`: con intervalo chico
  inyectado, una corrida con una falla deja líneas de avance con las fallas acumuladas.
- Tests de controller existentes siguen pasando.
- `./mvnw -q test` verde.

## Guion de prueba
Local (dev; la base de dev arranca vacía, `./mvnw spring-boot:run` levanta Postgres por
`compose.yaml` de la raíz):
1. `./mvnw -q test 2>&1 | tail -40` → sin fallas.
2. `grep -rn 'aborted' src/main/java/oneprofile/backend/controller/{BoardProbeController,VacancyController}.java`
   → una línea en cada uno; `grep -rn '" failed"\|probe failed\|sweep failed' src/main/java/oneprofile/backend/controller/`
   → sin el cierre viejo.
3. Levantar en dev con el log a un archivo: `./mvnw spring-boot:run > <scratchpad>/dev.log 2>&1 &`
   y esperar `Started BackendApplication` (`grep -m1`).
4. Insertar 3 empresas:
   `docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "insert into company (id, ats, slug) values (nextval(''company_seq''), ''GREENHOUSE'', ''figma''), (nextval(''company_seq''), ''GREENHOUSE'', ''splice''), (nextval(''company_seq''), ''GREENHOUSE'', ''no-existe-xyz'')"'`
5. `curl -i -X POST localhost:8080/admin/probe/greenhouse` → 202; en el log
   `Greenhouse board probe finished: 3 companies, 1 not found, ...`. (Con intervalo 1.000 no
   hay línea de avance con 3 empresas: el avance se prueba en los tests.)
6. `curl -i -X POST localhost:8080/admin/vacancies/greenhouse` → 202; en el log
   `Greenhouse vacancy sweep finished: 2 companies, ...`.
7. Parar la app.

## Preguntas abiertas
ninguna
