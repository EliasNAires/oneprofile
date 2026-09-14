# Tema — descubrimiento, sondeo y companies

De un archivo web (CommonCrawl, Wayback) a filas en `company`, y del sondeo a saber cuáles
siguen vivas. Lo que el código no dice: porqués, lo medido y lo descartado. Los números
de cobertura están en `docs/agents/MEDICION-SLUGS.md`; el plan en curso, en
`docs/agents/planes/slugs/`.

## Hechos que condicionan el diseño

- **El JSON de postings no es estándar entre ATS** (Greenhouse, Lever, Ashby, Workable,
  SmartRecruiters): un client por ATS que mapee a un modelo propio. JSON-LD
  `schema.org/JobPosting` es opcional.
- **Se guardan todas las empresas del ATS**, no solo las que hoy tienen vacantes en
  Argentina: descartarlas obligaría a volver a CommonCrawl. El país se filtra al consultar.
- **Un slug es único solo dentro de su ATS**: la identidad es el par `(ats, slug)`. Es lo
  único que da el índice; el nombre sale recién de la API.
- **Greenhouse tiene dos dominios** (`boards.` y `job-boards.greenhouse.io`) que son el
  mismo ATS y el mismo slug: dos patrones de `Ats.GREENHOUSE`.
- **Los archivos son fotos viejas**: un board visto puede dar 404 hoy. Para eso está el sondeo.
- El descubrimiento es caro y con límites de consulta: se corre a mano y queda en la base,
  por eso vive en este repo en Java.
- `Ats` es **enum y no entidad**: conjunto cerrado, cada ATS igual necesita su clase, y da
  `switch` exhaustivo.

## `GreenhouseBoardUrl`: de URL a slug

- **Solo `https`** (decisión de Elias). Medido: las 76.765 capturas de `CC-MAIN-2026-34`
  son https. Wayback sí trae `http://`, que se descarta; 77 slugs solo aparecen así y se
  decidió no aflojar.
- **Los iframes `embed/job_board?for=X` y `embed/job_app?for=X` cuentan**: son una porción
  grande de las capturas.
- **`[A-Za-z0-9_-]+` descarta la basura sin listas negras** (host pelado, `robots.txt`).
  Tira a sabiendas slugs reales con `&` o `)` (`1pyra)mid_health&care`), por volumen.

## `HttpRetry` y los clients

Reintenta 5xx y red con backoff; **ningún 4xx** salvo el 429 si el client lo habilita.
Cada client pone sus números según lo que cuesta perder un request:

| Client | Intentos / primera espera | Por qué |
|---|---|---|
| CommonCrawl | 4 / 2 s | pocos requests enormes; el índice da 504 de a ratos |
| Greenhouse | 3 / 1 s | miles de requests chicos; perder uno cuesta una empresa, que vuelve la próxima |
| Wayback | 4 / 30 s, **con 429** | un rate limit del Internet Archive dura minutos (decisión de Elias) |

Timeouts de 10 s / 2 min para que una corrida colgada muera. Los constructores de paquete
existen para que el test ponga esperas en cero y bindee `MockRestServiceServer`; con dos
constructores con argumentos, el público necesita `@Autowired` o el contexto no levanta.
`HttpRetry` entró a `util` porque los reintentos estaban copiados en dos clients.

**CommonCrawl:**

- `collinfo.json` viene del más nuevo al más viejo (verificado 2026-09-13) y se pide en
  cada corrida: **ningún id hardcodeado**; además la numeración salta (`CC-MAIN-2026-26`
  no existe).
- Una página son ~9 MB y ~12.000 líneas: **se lee en streaming**, con `.exchange()`.
- El patrón viaja percent-encodeado y el índice lo acepta (verificado contra el real).
- Un 4xx no se reintenta (índice inexistente, patrón sin capturas); antes de rendirse,
  `HttpRetry` loguea la página pedida y el cuerpo del error (`... Giving up`).
- **Una página cortada a mitad de línea sí se reintenta.** El índice saturado corta el cuerpo
  con 200, `Transfer-Encoding: chunked` y sin `Content-Length`, así que HTTP no lo detecta. Es
  transitorio (bajada a mano minutos después, completa). En Jackson 3 el error de parseo no es
  `RestClientException` y `HttpRetry` no lo veía: se convierte en `ResourceAccessException`.

**Wayback** (medido en `MEDICION-SLUGS.md`):

- El conteo se pide **sin `fl`**: con `fl` contesta `-`.
- Páginas en texto plano, una URL por línea.
- **2 s de pausa por página**: el ritmo al que se bajó el archivo entero sin 429 ni 5xx.
- Página fuera de rango da 400, que no se reintenta.

## `GreenhouseDiscoveryService`

**Sobre CommonCrawl (`discoverOnRecentCommonCrawl`)**: los **10 índices más recientes**, de a
uno, guardando lo nuevo antes de pasar al siguiente.

- **10 y no los 127** (Elias): Wayback cubre muchísimo; lo que importaba era no quedarse con
  un solo índice, que ve una fracción (~4.000 slugs; ~3.000 se repiten con el siguiente;
  16 índices unen 10.086 y no se aplanó).
- **Un índice que falla no corta la corrida**: `WARN` y a `failedIndexes`.
- **No es `@Transactional`**: la lectura es tiempo de red; `saveAll` abre su transacción
  corta y sigue en lotes JDBC de 50.

**Sobre Wayback (`discoverOnWayback`)**: carga una vez los slugs conocidos y guarda **en
lotes de 500 mientras lee** (Elias): la lectura dura ~40 min y guardar al final perdería
todo si falla cerca del final. Lo guardado queda; la corrida siguiente lo saltea.

**`DiscoveryController`**: `POST /admin/discovery/greenhouse/commoncrawl` y `.../wayback`,
sin parámetros. 202 al toque, un executor de un hilo, y **comparten el `AtomicBoolean`**
(409 si corre cualquiera, porque escriben la misma tabla). **El log es el único canal de
resultado.** El endpoint viejo con `?index=` se sacó: el nuevo lo cubre y descubrir es
idempotente.

```bash
docker compose exec app curl -i -X POST 'localhost:8080/admin/discovery/greenhouse/commoncrawl'
docker compose exec app curl -i -X POST 'localhost:8080/admin/discovery/greenhouse/wayback'
```

## La API de Greenhouse para el sondeo (medida a mano)

- **Board inexistente → 404** `{"status":404,"error":"Job not found"}`, no 400.
- **Board vivo → 200** con todo en una respuesta: **no hay paginación**.
- Cada job trae `company_name`: el nombre sale gratis. `GET /v1/boards/<slug>` da el
  nombre pero **no se usa** (duplicaría requests); precio aceptado: las `EMPTY` quedan sin
  nombre.

Lo medido de `/jobs` para cargar vacantes (`content`, salario, ids) va en el tema de vacantes.

## El sondeo

`GreenhouseBoardClient.probe` → `NOT_FOUND` / `EMPTY` / `ACTIVE`. **El 404 es una
respuesta, no un error**: no se reintenta, y por eso `.exchange()` en vez de `.retrieve()`.

`GreenhouseBoardProbeService.probeAll()`:

- **No es `@Transactional`**: un UPDATE cada 200 ms durante media hora retendría una
  conexión y, si muere, perdería todo. Un `save()` por empresa conserva lo avanzado.
- **Una empresa que falla no aborta**: `WARN`, `failed`, conserva su estado anterior.
- **200 ms entre boards** es el piso; la corrida real de 4.046 tardó ~30 min, no ~14.

`POST /admin/probe/greenhouse`, sin parámetros, calca a `DiscoveryController` a propósito.
Resultado: `select board_status, count(*) from company group by board_status;`

## `Company`

- `recordProbe(status, name, probedAt)`: **un nombre `null` no pisa el guardado** (una
  empresa que cierra búsquedas queda `EMPTY` sin perder que se llama "Globant").
- `board_status is null` significa *nunca sondeada*; con `last_probed_at` el cron futuro
  puede preguntar "¿nunca?" y "¿hace más de N días?". Hoy nadie lo lee.
- `findSlugsByAts` devuelve **slugs, no entidades**: el descubrimiento solo resta de un
  `Set` y así no hidrata miles de filas. `findByAts` devuelve entidades porque el sondeo
  las actualiza.
- El test de `findSlugsByAts` no prueba que *filtra* por ATS: `Ats` tiene un solo valor. La
  aserción entra con el segundo ATS.

## Probado en prod

- **Descubrimiento con el endpoint viejo**: `CC-MAIN-2026-34` dio `4046 slugs found, 4046 new
  companies saved`. Verificado offline con las mismas reglas: 76.765 capturas → exactamente
  4.046 slugs; solo 229 URLs no dan slug, y corresponde. **La app no pierde nada de lo que lee.**
  El 2026-09-13 se corrieron `-30`, `-25`, `-21`, `-17`: 1.269, 637, 533 y 503 nuevas, 15-50 s c/u.
- **Sondeo** (4.046 empresas): **3.121 `ACTIVE`, 708 `NOT_FOUND`, 217 `EMPTY`**. El 77% sigue
  vivo con vacantes; se esperaba más mortandad.
- **Los 10 índices recientes fallaron** el 2026-09-13: 3 leídos, 7 fallidos, 475 nuevas,
  `company` en 7.463 (5 por página cortada, 2 por `400`). Corrección construida, sin probar
  en prod: `docs/agents/planes/slugs/`. Esperado: `company` entre 8.328 y ~8.650 (`2025-47` no
  se midió).
- **Wayback: construido, sin correr en prod.** Esperado: ~40 min (439 páginas), N cerca de
  17.730 slugs, `company` cerca de 18.000.

## Abierto

- **Dos `400 Bad Request` de CommonCrawl sin causa confirmada** (`-21` y `-04`, 2026-09-13),
  cuando los conteos eran normales. Hipótesis: bajo carga `showNumPages` dio páginas de más
  (una página fuera de rango da 400). No se reintentan (Elias); la línea `Giving up` dirá el
  motivo. Log crudo en `mediciones/fallas-commoncrawl-13-09-2026/`.
- **Respuesta truncada del índice justo en un salto de línea**: HTTP 200 con cuerpo corto
  (pasó 3 veces bajando a mano) se acepta en silencio. Elias lo dejó abierto el 2026-09-13.
- **Wayback con una línea cortada guarda un slug falso** (`mercadol`); el sondeo lo marca
  `NOT_FOUND`. Anotado sin arreglar (Elias).
- **Dominio EU** (`job-boards.eu.greenhouse.io`, 848 slugs, 93 ya en prod por el otro) no se
  descubre; tocaría sondeo y carga (`boards-api.eu.greenhouse.io`). Afuera por ahora (Elias).
- **El sondeo se corre entero cada vez**: los datos para filtrar están, la consulta no. Se
  agrega con el cron.
- **Orden sondeo → vacantes → normalización** no lo fuerza nada; importa cuando haya cron.
