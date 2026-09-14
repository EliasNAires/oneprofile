# Contexto — tablero del repo

> Estado actual y fase siguiente. Lo actualiza el planificador al volcar un plan y el
> orquestador al parar o terminar (ver "Roles" en `docs/agents/METODOLOGIA.md`). Tope ~12 KB (`wc -c`). El detalle vive
> en los docs de tema y se lee solo si "Leer:" lo pide. **`docs/para-humanos/` no se lee.**

**Última actualización:** 2026-09-14

## Qué es

Backend de `oneprofile`: descubre qué empresas usan un ATS (hoy solo Greenhouse), sondea
cuáles siguen vivas, carga sus vacantes y normaliza los títulos. El objetivo final es una
lista de vacantes que matcheen con el perfil del usuario.

## Estado

- **Descubrimiento y sondeo** andan en prod. `company` tiene **18.051** filas (índices de
  CommonCrawl, con 4 de los 10 recientes sin leer, más Wayback), todas sondeadas el 2026-09-14: ACTIVE 6.883,
  NOT_FOUND 9.787, EMPTY 1.381.
- **Vacantes:** el recorrido del 2026-09-14 (12:40–14:31 UTC) dejó **216.868 vacantes sobre
  6.879 empresas** (89.376 nuevas, 127.491 actualizadas, 1.461 borradas, 0 fallidas).
- **Normalización del título** (limpieza, seniority, modalidad): corrida entera el 2026-09-13
  en 24 s (títulos distintos de 87.647 a 77.630); el 2026-09-14 se normalizaron las 89.376
  nuevas en 18 s y no quedan vacantes sin normalizar.
- **Plan de slugs terminado y borrado** (2026-09-14): CommonCrawl 6/10 índices (+590),
  Wayback +9.998, sondeo y carga. **+3.762 `ACTIVE` y +87.915 vacantes (+68%)**: 54% de las
  vacantes nuevas salió de índices viejos de CommonCrawl nunca sondeados, 43% de Wayback, 4%
  de los índices del plan. Wayback trae la mitad de las activas nuevas pero el 75% de sus
  slugs está muerto. Detalle en "Probado en prod" de `descubrimiento`; crudos, revisión y uso
  de contexto en `mediciones/slugs-13-09-2026/`.
- **La dieta de contexto terminó** (2026-09-13): docs de agentes en `docs/agents/`, este
  tablero y cuatro temas. Falta la prueba real: `/context` después de la lectura inicial de
  la sesión siguiente debería rondar ~35k en vez de ~70k.

## Qué sigue

**Fase siguiente: corrida en prod de descubrimiento → sondeo → carga para medir** lo de
`descubrimiento-v2` (pasadas, EU, limpieza), con `/planificar`.

**Los 7 pasos están hechos (2026-09-14), sin commit:** parado en la puerta "después" del
paso 07 (commit y push de Elias). Lo decidido ya pasó a `descubrimiento` y `vacantes`; la
carpeta del plan se borró y sus desvíos por paso están abajo. Lo principal:
- Subagentes en Sonnet (el orquestador pasa `model: sonnet` explícito); `.claude/settings.json`
  con allowlist (`./mvnw`, `ssh elitedesk1 docker compose logs/ps/exec`).
- `util/ProgressLog`; sondeo, carga y descubrimiento cierran con `finished`/`aborted`.
- CommonCrawl en 3 pasadas. En dev (14:02 UTC-3) la pasada 1 falló en los 10 índices
  (504/502); en otra corrida (14:39) leyó los 10 sin fallas. Mirar la tasa de fallo en prod.
- Dominio EU (`job-boards.eu.`, `boards.eu.`): `proton` cargó 69 vacantes por la misma API.
- Tabla `blacklisted_slug` (V5); al final del sondeo los truncados `NOT_FOUND` se borran
  (con sus vacantes) y van a la blacklist. 189 tests.
- Dev: el clasificador bloquea a los subagentes `truncate` y `docker compose down` aunque
  Elias apruebe con `/permissions`; la base de dev no tiene volumen y se vacía reiniciando
  el container. Los guiones con `sh -c '...'` y `''` adentro no corren: comillas dobles afuera.

### `descubrimiento-v2`: estado y desvíos por paso

- Paso 01 (2026-09-14): PASÓ. Allowlist por decisión de Elias: los tres propuestos más
  `Bash(ssh elitedesk1 docker compose exec:*)` para psql (no se limita a solo lectura).
  `.claude/settings.json` lo escribió el orquestador (al ejecutor le bloquearon la escritura).
  El ejecutor corrió en Opus; desde el verificador del 01, subagentes con `model: sonnet`
  explícito en la llamada (no depende de reiniciar la sesión).
- Paso 02 (2026-09-14): PASÓ. `util/ProgressLog` nuevo; sondeo y carga cierran con
  `finished`/`aborted`. Guion en dev, dos cosas que valen para los pasos siguientes:
  (1) `sh -c` con comillas simples y `''` adentro no anda: Elias eligió comillas dobles
  afuera; (2) la base de dev no arranca vacía (volumen persistente: `splice`, `discord`,
  `figma` y 205 vacantes): Elias eligió reusar filas existentes y solo agregar las que faltan.
- Paso 03 (2026-09-14): PASÓ, sin desvíos. Solo `GreenhouseDiscoveryService` y su test;
  `CommonCrawlResult` mantiene su forma (`indexes`, `failedIndexes`). Loguea WARN
  `failed on pass N` por índice.
- Paso 04 (2026-09-14): verificación **cortada por Elias** (lo importante era validar las
  pasadas). Tests del ejecutor verdes. Desvíos del ejecutor (no preguntados): Awaitility
  transitivo en el test del controller; formatos `CommonCrawl pass {} of {}: {} indexes {}`,
  `CommonCrawl index {} pass {}: {} slugs found, {} new companies saved`, avance Wayback
  `"Wayback " + pattern`. Corrida real de CommonCrawl en dev (arranque 17:02 UTC, cortada
  17:33 UTC sin línea final): pasada 1 falló en los **10 de 10** índices (8× 504, 1× 502,
  1× I/O error), ~17 min; la pasada 2 arrancó con los 10 y va 1 leído (`CC-MAIN-2026-21`:
  3953 slugs, 3950 companies nuevas) y 5 fallidos (2× 504, 2× 400 "Connection aborted",
  1× página cortada). Las pasadas y sus logs andan; llama la atención la tasa de fallo del
  índice (para mirar en la corrida de prod). Wayback y el cierre `finished` no se vieron.
  **Efecto en dev:** la base quedó con ~3950 companies más (antes: splice, discord, figma).
- Orquestación: el verificador esperaba con sus propios monitores y reenviaba cada WARN;
  se le pidió cortar y la espera quedó en el Monitor del orquestador.
- Paso 05 (2026-09-14): PASÓ. Solo `GreenhouseBoardUrl` (4 patrones) y tests; el service no
  cambió. CommonCrawl en dev esta vez completo: 27 min, 10 índices leídos, 5024 nuevas,
  0 fallidos. Sondeo de los 6 EU: 6 ACTIVE; carga de `proton`: 69 vacantes con URL
  `job-boards.eu.`. **Dev:** Elias eligió vaciar la base; el clasificador bloqueó al
  verificador truncate y `docker compose down` pese a `/permissions` (4+1 intentos); el
  orquestador corrió `down`/`up -d` por pedido de Elias (dev no tiene volumen). Base de dev
  ahora: solo los 6 slugs EU y 69 vacantes de proton. Para los pasos siguientes: vaciar la
  base de dev = reiniciar el container, sin query.
- Paso 06 (2026-09-14): PASÓ, sin desvíos. `V5__create_blacklisted_slug.sql` (`id`, `ats`,
  `slug`, unique `(ats, slug)`), `model/BlacklistedSlug`, `repository/BlacklistedSlugRepository`;
  `GreenhouseDiscoveryService` la saltea. 183 tests verdes; Flyway en dev llega a v5.
- Paso 07 (2026-09-14): PASÓ. `GreenhouseTruncatedSlugCleanupService` (nuevo), llamado desde
  `BoardProbeController` al terminar el sondeo. Decisión de Elias: un truncado `NOT_FOUND` con
  vacantes se borra con sus vacantes en la misma transacción (`normalized_vacancy` cae por
  cascade). Log `Greenhouse truncated slug cleanup finished: {} companies removed and
  blacklisted`. Test del controller espera el log en vez de contar invocaciones. Total real:
  **189 tests**, 0 fallas (los 156/183 reportados por ejecutores estaban mal). Guion en dev:
  `figm` borrado y en blacklist, `figma` ACTIVE, `no-existe-xyz` NOT_FOUND queda. El guion
  volvió a traer el `''` dentro de `sh -c '...'`: se aplicó la decisión del paso 02.
  Dev quedó con 8 companies (6 EU + figma + no-existe-xyz).

Prueba de la metodología nueva (roles construidos el 2026-09-13), estado:

1. Las skills `planificar`, `ejecutar`, `consultar` y los agentes `ejecutor`, `verificador`
   aparecen en `/skills` y `/agents`.
2. Sesión nueva sin comando → el agente pregunta el modo antes de leer nada.
3. `/consultar ¿qué hace GreenhouseDiscoveryService?` → contesta sin leer planes ni
   metodología; `/context` bajo ~35k.
4. ~~`/planificar`~~ → **probado con `descubrimiento-v2` el 2026-09-14**: conversó, entró en
   modo plan y volcó el plan aprobado.
5. ~~`/ejecutar <tema>`~~ → **probado con `slugs` el 2026-09-14** (solo verificadores, sin
   ejecutor) y **con ejecutores en `descubrimiento-v2`** el mismo día: anduvo, con los
   problemas de "Flujo de trabajo" abajo.

Después, sin priorizar ni planificar: **categorizar las vacantes en tech-adyacentes y no**
(por tokens del título, no por diccionario: casi la mitad de los títulos no se repite),
perfil de usuario, primer matching simple, endpoint que devuelva vacantes, y un cron que
encadene sondeo → vacantes → normalización.

## Qué NO existe todavía

- Categorización tech / no tech; normalización de departamento y ubicación.
- Perfil de usuario y matching.
- Ningún endpoint devuelve datos: todos disparan procesos.
- Ningún ATS además de Greenhouse.
- Ningún cron: todo se dispara a mano (`last_probed_at` espera a que exista).
- Tag por SHA (solo `latest`, no se puede revertir un deploy), despliegue automático,
  HTTPS, dominio, reverse proxy.

## Puntos abiertos

El detalle de cada uno está en la sección "Abierto" de su tema.

**Flujo de trabajo** (monitores, permisos, hora en reportes, estimaciones y Sonnet se
resolvieron en el paso 01 de `descubrimiento-v2`; queda lo visto al ejecutarlo):

- **Verificadores esperando corridas**: aunque la regla dice que espera el orquestador, uno
  armó monitores propios y reenvió cada WARN (~15 avisos) hasta que se le pidió cortar. Al
  pasarle el protocolo en el prompt (devolver log, patrón y hora UTC, y cortar) anduvo.
- **Clasificador de permisos**: bloqueó a subagentes `truncate` y `docker compose down` en dev
  aunque Elias aprobara con `/permissions` (5 intentos); lo destrabó el orquestador.
- **Guiones con `sh -c '...'` y `''` adentro** no corren (pasos 02 y 07): comillas dobles
  afuera. Lo tiene que evitar el planificador al escribir el guion.
- **Conteos de tests mal reportados** por ejecutores (156/183; real 189): el verificador cuenta
  en `surefire-reports`.
- **Ejecutores que deciden formatos no fijados** (paso 04, textos de log): conviene que el paso
  los traiga cerrados o que el ejecutor pregunte.
- **Tamaño de pasos: bien** (Elias y orquestador, en `slugs`). Techo blando de ~100k por
  sesión (regla en `METODOLOGIA.md`, "Higiene de contexto").

- **Imagen de prod:** Elias corre sesiones en paralelo; antes de concluir de una prueba en
  prod, confirmar qué commit se publicó. → `prod-y-despliegue`
- Endpoints de administración sin protección (importa al publicar uno que devuelva datos).
  → `prod-y-despliegue`
- Respuesta truncada del índice justo en un salto de línea se acepta en silencio (Elias lo
  dejó abierto). → `descubrimiento`
- Wayback con una línea cortada guarda un slug falso (lo limpia la limpieza de truncados si
  la versión completa está `ACTIVE`). → `descubrimiento`
- Tasa de fallo de CommonCrawl: en dev falló 10/10 en una pasada y 0/10 en otra; ver en prod
  si 3 pasadas alcanzan. → `descubrimiento`
- 4 `ACTIVE` sin vacantes y `fetched` una fila menos que `vacancy` (2026-09-14). → `vacantes`
- Reglas de slug descartan algunas empresas reales (`&`, `)`, 77 solo `http://`), a
  sabiendas. → `descubrimiento`
- El sondeo se corre entero cada vez; nada fuerza el orden sondeo → vacantes →
  normalización. → `descubrimiento`
- El sync de vacantes no normaliza (anotado a pedido de Elias). → `vacantes`
- Sin tope de tamaño en la respuesta de un board. → `vacantes`
- `MEDICION-VACANTES.md` (85.050 títulos distintos) no coincide con la base (87.647). →
  `vacantes`
- La base de dev arranca vacía (no tiene volumen: se vacía reiniciando el container); hay que
  insertar la empresa a mano. → `vacantes`
- Borrar una `Company` no borra en cascada sus `Vacancy` (ni JPA ni la FK); la limpieza de
  truncados las borra a mano. → `vacantes`
- Modalidad: 37 falsos positivos y 31 falsos negativos (`remotely`), sin guarda por decisión
  de Elias; `FULLY_REMOTE` conservador a propósito. → `normalizacion`
- `senior` en cuidado domiciliario (37), sin guarda por decisión de Elias. → `normalizacion`
- ~650 filas que no son vacantes (talent pool, general application). → `normalizacion`
- Contrato/jornada y ubicación estructurada merecen paso propio. → `normalizacion`
- Sin regla: nivel `i`, `associate`, `virtual`; conector suelto en rangos de nivel. →
  `normalizacion`
- `pom.xml` con metadata vacía; `docker-rootless-extras` desparejo con `docker` en la
  máquina de Elias. → `prod-y-despliegue`

## Docs de agentes

- `docs/agents/METODOLOGIA.md` — cómo trabajamos.
- `docs/agents/tema/prod-y-despliegue.md` — trampas de Boot 4, entorno rootless,
  configuración, migraciones, prod, pipeline, mudanza de datos.
- `docs/agents/tema/descubrimiento.md` — CommonCrawl, Wayback, `GreenhouseBoardUrl`,
  reintentos, sondeo, `Company`.
- `docs/agents/tema/vacantes.md` — API de `/jobs`, vacantes viejas, sync y recorrido masivo.
- `docs/agents/tema/normalizacion.md` — extractores del título, tabla y corrida en prod.
- `docs/agents/MEDICION-SLUGS.md`, `docs/agents/MEDICION-VACANTES.md` — números medidos.
- `mediciones/` — salidas crudas; no se lee salvo que un plan nombre un archivo.

## `docs/para-humanos/`

Para personas, **no se lee** salvo que Elias pida escribirla o actualizarla: README, un
archivo por proceso, `despliegue.md` y ocho diagramas PlantUML (`.puml` + `.svg`),
actualizada el 2026-09-13. PlantUML no está instalado como comando; se usa el jar:
`java -jar ~/.vscode/extensions/jebbs.plantuml-2.18.1/plantuml.jar -tsvg <archivo>.puml`.
Para mirar un diagrama se genera un PNG fuera del repo
(`rsvg-convert -z 2 x.svg -o <scratchpad>/x.png`) y se abre.
