# Plan — traer a prod los slugs de Greenhouse: 10 índices de CommonCrawl y Wayback

> Plan de varios pasos, autocontenido: se retoma leyendo este archivo,
> `docs/agents/METODOLOGIA.md` y `docs/agents/CONTEXTO.md`. Los números y el porqué de cada decisión
> están en **`docs/agents/MEDICION-SLUGS.md`**; leelo antes de empezar. Se borra al terminar
> el plan y lo que quede pasa a `CONTEXTO.md`.

## Estado del plan

- **Hecho:** la medición (`docs/agents/MEDICION-SLUGS.md`) y cuatro índices extra de
  CommonCrawl corridos en prod (`CC-MAIN-2026-30`, `-25`, `-21`, `-17`) con el endpoint
  de hoy. `company` quedó en **6.988**: 3.121 `ACTIVE`, 217 `EMPTY`, 708 `NOT_FOUND` y
  **2.942 sin sondear**.
- **Hecho (2026-09-13):** el diseño, acordado con Elias (abajo).
- **Paso 1: HECHO y probado por Elias** (`mvn test`, 107 en verde). El reintento del 429
  **no** entró en `HttpRetry`: nadie lo usaba todavía, entra en el paso 3 con Wayback.
- **Paso 2: ESCRITO**, `./mvnw test` da **110 en verde**. Falta la prueba en prod, que
  necesita que Elias commitee y pushee para que el pipeline publique la imagen. Por
  ahora el endpoint tiene un solo lugar donde dispararse, así que no tiene
  `private start(...)`; ese método entra en el paso 3, con el segundo endpoint.
- **Paso 3: construcción cerrada por Elias** (2026-09-13), `./mvnw test` da **122 en
  verde**. Falta la prueba en prod, que es la fase siguiente (guion en "Qué sigue" de
  `docs/agents/CONTEXTO.md`). **No desplegar mientras corre el paso 2**: `docker compose up -d` reinicia la app y
  mata esa corrida.
- **Paso 2, prueba en prod (2026-09-13): FALLÓ.** 3 índices leídos, 7 fallidos, 475
  empresas nuevas; `company` en **7.463** (3.417 sin sondear). Causa y corrección en
  **`docs/agents/PLAN-FALLAS-COMMONCRAWL.md`**.
- **Corrección de esas fallas: CONSTRUIDA** (2026-09-13), `./mvnw test` da **126 en verde**.
- **Desplegada y corrida en prod (2026-09-14): FALLÓ por causa externa.** CommonCrawl devolvió
  504 en los 10 índices; 0 leídos, `company` sigue en 7.463. Hay que repetirla. Wayback, desplegado
  pero sin correr. Detalle en `docs/agents/CONTEXTO.md` y el log en
  `mediciones/slugs-prod-13-09-2026/commoncrawl.txt`.
- **Por dónde retomar:** repetir la prueba en prod del paso 2 con el guion de la sección C de
  `docs/agents/PLAN-FALLAS-COMMONCRAWL.md`, y después la del paso 3.

## El objetivo y la escala

Pasar de 6.988 empresas a **~18.000**. No se busca ser exhaustivo: con los **10 índices
más recientes** de CommonCrawl y Wayback alcanza, porque Wayback casi contiene a
CommonCrawl y tiene el doble. Se espera del orden de **~1.850 `ACTIVE` extra solo por
Wayback**.

## Relación con la normalización

La normalización de los títulos ya terminó (ver `docs/agents/tema/normalizacion.md`). Sus archivos —`V4`,
`VacancyRepository`, `NormalizedVacancy*`, `VacancyNormalizationService` y
`NormalizationController`— **no usan los clients** (verificado con grep), así que este plan
no los toca y no necesita migración.

## El criterio de diseño

**Se separa por motivo de cambio** (está en las convenciones de `METODOLOGIA.md`):

- **Un client por proveedor.** CommonCrawl y Wayback cambian por razones distintas: rate
  limit, 429, forma de contar páginas.
- **Un service y un controller por caso de uso.** "Descubrir empresas" es uno solo: los
  dos orígenes son métodos de `GreenhouseDiscoveryService` y endpoints de
  `DiscoveryController`.
- **Un util solo si lo usa más de un lugar.** Los reintentos los usan los tres clients.
- **Nada de comportamiento nuevo**: ni sondeo incremental, ni detección de páginas
  truncadas. Suman ruido cuando todo corra con crons.

```
util/HttpRetry                       reintentos con backoff (intentos, primer delay, nombre para el log,
                                     si reintenta 429), errorFor(status, text), requestFactory(connect, read)
client/CommonCrawlIndexClient        (movido) forEachUrl(indexId, pattern, onUrl) + latestIndexIds(count)
client/WaybackCdxClient              (nuevo)  forEachUrl(pattern, onUrl)
client/GreenhouseBoardClient         (movido) sin cambios de comportamiento
service/GreenhouseDiscoveryService   discoverOnRecentCommonCrawl() + discoverOnWayback()
                                     + private saveNew(slugs); sin @Transactional
controller/DiscoveryController       /commoncrawl y /wayback, mismo executor y AtomicBoolean,
                                     private start(...) como el de NormalizationController
```

## Decisiones ya tomadas por Elias

- Paquete nuevo **`client`** (no `component`: en Spring todo es un `@Component`).
- `withRetries` / `errorFor` / `requestFactory`, hoy copiados en los dos clients, salen a
  `util/HttpRetry`.
- CommonCrawl: un endpoint que recorre los **10 índices más recientes**, no los 127. Se
  **saca** `POST /admin/discovery/greenhouse?index=`.
- **Cómo no se hardcodean los índices:** en cada corrida la app pide
  `https://index.commoncrawl.org/collinfo.json` y toma los primeros 10 `id`. Verificado el
  2026-09-13: el JSON es un array de objetos `{id, name, timegate, cdx-api, from, to}`
  **ordenado del más nuevo al más viejo** (arranca en `CC-MAIN-2026-34`). Lo único fijo en
  el código es el número 10.
- Si un índice falla después de sus reintentos, **se sigue con los demás** (WARN y
  contador de fallidos), igual que el sondeo y la carga.
- Wayback: **lectura sin transacción y guardado en lotes de 500 empresas mientras se lee**
  (cambiado el 2026-09-13; antes era todo al final), así una falla a los 38 minutos no
  tira lo ya leído; **2 s de pausa** entre páginas (la condición medida, sin 429 ni 5xx);
  el **429 se reintenta solo en Wayback**, con **4 intentos desde 30 s** (30, 60, 120 s),
  porque un rate limit del Internet Archive dura minutos.
- Wayback tiene endpoint propio, sin parámetros, y **comparte el lock** con CommonCrawl.
- El dominio EU (`job-boards.eu.greenhouse.io`, 848 slugs) queda afuera por ahora.
- Las reglas de `GreenhouseBoardUrl` no se tocan (se pierden 77 slugs solo-http y ~3 con
  `&` o `)`).
- Las corridas en prod las hace el agente.

## Paso 1 — Paquete `client` y `util/HttpRetry` (refactor puro)

- Mover `CommonCrawlIndexClient` y `GreenhouseBoardClient` (y sus tests) de `service` a
  `client`; ajustar los imports de `GreenhouseDiscoveryService`,
  `GreenhouseBoardProbeService`, `GreenhouseVacancySyncService` y sus tests.
- Extraer `util/HttpRetry`: los dos clients lo usan y pierden sus copias. Cada uno
  conserva sus números (CommonCrawl 4 intentos desde 2 s; Greenhouse 3 desde 1 s) y su
  delay inyectable por el constructor de paquete para los tests.
- Tests: `HttpRetryTest` unitario (reintenta 5xx y error de red, no reintenta 4xx, el 429
  solo si está habilitado, se rinde en el último intento). Los tests de los dos clients
  **no cambian sus aserciones**: que sigan verdes es la prueba del refactor.
- Prueba manual: `./mvnw test`.

## Paso 2 — Los 10 índices recientes de CommonCrawl

- `CommonCrawlIndexClient.latestIndexIds(int count)`.
- `GreenhouseDiscoveryService.discoverOnRecentCommonCrawl()`, con `RECENT_INDEXES = 10`:
  por índice junta los slugs de los dos patrones en un `Set` y llama a
  `private saveNew(Set<String>)`, que resta `findSlugsByAts` y hace `saveAll`. Devuelve el
  resultado por índice y los fallidos. **Sin `@Transactional`**: el `saveAll` de
  `SimpleJpaRepository` abre su propia transacción y conserva los lotes de 50. Se saca
  `discover(indexId)`.
- `DiscoveryController`: `POST /admin/discovery/greenhouse/commoncrawl` sin parámetros;
  log por índice y total. Se saca el endpoint con `?index=`.
- Tests: client, `latestIndexIds(2)` sobre un `collinfo.json` de mentira; servicio, se
  conservan los 4 casos actuales y se suman "recorre los índices recientes" y "un índice
  que falla no impide guardar los demás"; controller, 202 + delegación y 409.
- Prueba manual en prod: `docker compose pull && docker compose up -d`, el POST, y en el
  log los 10 índices. Los 5 ya corridos dan 0 nuevas; los otros 5, cerca de la unión de
  H1 (378, 345, 364, 253 y 286). `select count(*) from company` ≈ **8.614**.

## Paso 3 — Wayback

- `client/WaybackCdxClient.forEachUrl(pattern, onUrl)`:
  - Páginas: `https://web.archive.org/cdx/search/cdx?url={pattern}&matchType=prefix&fl=original&page=N`,
    texto plano, **una URL por línea**, leídas en streaming con `.exchange()`.
  - **El conteo se pide sin `fl`** (`&showNumPages=true`): con `fl` contesta `-` en vez
    del número. La respuesta es un entero pelado (`286\n`).
  - Una página fuera de rango da **400**.
  - 2 s antes de cada página, `HttpRetry` con el 429 habilitado (4 intentos desde 30 s),
    timeouts explícitos (10 s / 2 min).
- `HttpRetry` ganó el flag `retriesTooManyRequests`; los otros dos clients pasan `false`.
- `GreenhouseDiscoveryService.discoverOnWayback()`: carga una vez los slugs conocidos, lee
  los dos patrones y guarda las empresas nuevas **en lotes de 500** (`WAYBACK_BATCH`) a
  medida que aparecen; si la lectura falla, los lotes guardados quedan. Devuelve
  `DiscoveryResult(slugs distintos, empresas nuevas)`.
- `POST /admin/discovery/greenhouse/wayback`, con el mismo lock; `DiscoveryController`
  ganó `private start(source, run)`.
- Tests: `WaybackCdxClientTest` con `MockRestServiceServer` (recorre lo que dice el
  conteo, 0 páginas, conteo sin `fl` y páginas con `fl=original`, saltea líneas vacías,
  reintenta 503 y 429, un 400 no reintenta); `HttpRetryTest`, el 429 solo si está
  habilitado; servicio, Wayback trae una empresa guardada y una nueva por los dos
  dominios → `(2, 1)`, y con lote 2 una falla a mitad deja guardado el primer lote;
  controller, 202 y delegación, y 409 si hay una corrida de CommonCrawl en curso.
- Prueba manual en prod: el POST, ~40 minutos (450 páginas), log con `slugs found` ≈
  **17.730** y `company` ≈ **18.000**.

## Paso 4 — Sondear y cargar lo nuevo (sin código)

- `POST /admin/probe/greenhouse`: sondea todo, ~18.000 empresas, **~2 h 30**.
- Después `POST /admin/vacancies/greenhouse`: recorre todas las `ACTIVE`.
- Coordinar con la normalización: su `POST /admin/normalization/vacancies/missing`
  normaliza lo nuevo después de la carga.
- Verificación:
  `select coalesce(board_status,'(sin sondear)'), count(*) from company group by 1` sin
  filas sin sondear (en la muestra de Wayback, 23% dio `ACTIVE`).

## Al terminar

- `docs/agents/tema/descubrimiento.md` y el tablero `docs/agents/CONTEXTO.md`: el paquete `client`, `HttpRetry`, los endpoints nuevos, los números
  finales de `company` y `vacancy`, y el dominio EU como punto abierto.
- Borrar este archivo. `docs/agents/MEDICION-SLUGS.md` se queda.
