# Plan en curso — prod con Docker y el script de descubrimiento

> Plan acordado con Elias y aprobado. Los **Pasos 4, 5 y 6 ya están hechos y
> verificados** (ver `docs/CONTEXTO.md`); acá queda el paso **7**, pendiente.
> Este archivo se borra cuando el plan esté terminado.

## Por qué

El objetivo inmediato es descubrir qué empresas usan Greenhouse, consultando el
índice de CommonCrawl y guardando el slug de cada una como `Company`. Para eso
hacía falta primero una base persistente y un esquema versionado (Paso 4, hecho),
y hace falta después poder correr la app entera en prod y disparar el script a
mano.

## Decisiones tomadas

| Tema | Decisión |
|---|---|
| Base de prod | Postgres en `prod/compose.yaml`, **con volumen nombrado** |
| Imagen de prod | Dockerfile propio multi-stage |
| Disparo del script | Endpoint HTTP, **sin protección**, puerto de la app no publicado |
| Ejecución | Asíncrona: 202 inmediato, resultado y errores por log |
| Índice CommonCrawl | Uno por corrida, configurable (default: el más reciente) |
| Dominios Greenhouse | Los dos, bajo el mismo `Ats.GREENHOUSE` |

### Por qué los dos dominios no son dos ATS

`boards.greenhouse.io/<slug>` y `job-boards.greenhouse.io/<slug>` son dos dominios
del **mismo** ATS: el slug es el mismo identificador en los dos y la API de
vacantes es única (`boards-api.greenhouse.io/v1/boards/<slug>/jobs`). Modelarlos
como dos valores de `Ats` haría entrar a la misma empresa dos veces con
identidades distintas. Así que `GREENHOUSE` sigue siendo **un valor con dos
patrones de URL**.

### Cómo se manejan los duplicados

En tres niveles:

1. **Dentro del índice.** El CDX devuelve una línea por *captura*, no por URL: la
   misma empresa aparece cientos de veces (una por página del board y por
   timestamp). Se deduplica en memoria con un `Set<String>` de slugs.
2. **Entre los dos dominios.** Ambos alimentan *ese mismo* `Set`, así que una
   empresa presente en los dos queda una sola vez.
3. **Contra la base.** Se cargan los slugs ya guardados del ATS y se filtran antes
   de insertar. La `@UniqueConstraint (ats, slug)` de `Company` es la red final.

---

## Paso 5 — Prod con `docker-compose up` ✅ HECHO (2026-09-11)

> **Cerrado y probado a mano por Elias.** Se implementó como estaba planeado, con
> tres desvíos acordados sobre la marcha:
>
> - El compose de prod **no quedó en la raíz como `compose.prod.yaml`**, sino en
>   `prod/compose.yaml`, para que el comando sea `docker compose up` sin `-f`.
>   El `.env` vive en `prod/` por la misma razón, y el `Dockerfile` se queda en la
>   raíz con `build: context: ..`.
> - El build de la imagen corre `package` con **`-DskipTests`**: los tests usan
>   Testcontainers y necesitarían un Docker dentro del stage de build.
> - Se agregaron **`name: oneprofile-prod`** (sin él, prod recreaba el container de
>   Postgres de dev) y un **`healthcheck`** en Postgres con
>   `depends_on: condition: service_healthy` (sin él la app arranca antes de que la
>   base acepte conexiones y Flyway muere).
>
> El detalle de cómo quedó está en `docs/CONTEXTO.md`; lo de abajo es el plan
> original, conservado como registro.


**`Dockerfile`** (nuevo, multi-stage): stage de build con Maven + JDK 25 que corre
`./mvnw package`, stage de runtime con JRE 25 que solo copia el jar. No copia
`compose.yaml`, así que el soporte de docker-compose queda inerte en prod sin
necesidad de desactivarlo por configuración.

**`prod/compose.yaml`** (nuevo): servicio `postgres` con **volumen nombrado** y
servicio `app` construido desde el `Dockerfile`, con `depends_on` y
`SPRING_PROFILES_ACTIVE=prod`. El puerto de la app **no se publica** (decisión de
seguridad del endpoint); el de Postgres tampoco.

**Secretos:** las credenciales van por variables de entorno leídas de un `.env`
**no versionado**; al repo va un `.env.example` con las claves, y `.env` entra en
`.gitignore`. `application-prod.properties` se queda **vacío**: Spring Boot ya
bindea `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` por relaxed binding.

**Sin tests automáticos**: es empaquetado, no comportamiento.

**Prueba manual:** `cd prod`, `cp .env.example .env` y completarlo →
`docker compose up --build` → la app arranca y Flyway aplica
`V1` → `down` y `up` de nuevo, y la tabla sigue ahí con sus datos (eso prueba el
volumen).

**Punto abierto, señalado y no resuelto:** "sin protección + puerto no publicado"
funciona hoy porque la app no expone ningún otro endpoint. Cuando exista el
endpoint de vacantes habrá que publicar el puerto, y ahí el de descubrimiento
queda expuesto. Se decide en ese momento.

---

## Paso 6 — Extracción de slugs (puro, sin red) ✅ HECHO (2026-09-11)

> **Cerrado y probado a mano por Elias** (`./mvnw test` → 16 tests, 0 fallas).
> Se implementó con cuatro desvíos acordados sobre la marcha:
>
> - **Solo `https`.** El plan listaba "`http` y `https`" como casos de test, lo que
>   asumía un chequeo de esquema. Elias decidió exigir `https://` y descartar el
>   resto. Queda anotado que la consulta al CDX es agnóstica al esquema, así que
>   pueden llegar capturas `http://` y se descartan en silencio.
> - **Las URLs de iframe embebido dan slug** (`/embed/job_board?for=X`,
>   `/embed/job_app?for=X&token=...`), en vez de descartarse: son muchas capturas
>   y el `for=` es el mismo identificador.
> - **El repositorio devuelve slugs, no entidades**: `List<String> findSlugsByAts`
>   en lugar de `List<Company> findAllByAts`.
> - **`indexPatterns()` se pasó al Paso 7**, donde nace el cliente que lo consume.
>   En el Paso 6 no lo llamaba nadie y su único test posible era declarativo.
>
> Además, en la misma sesión y a pedido de Elias, **los paquetes se reorganizaron
> por capa técnica (MVC)**: por eso las rutas de abajo dicen `util/`, `service/` y
> `controller/` y no `discovery/`.
>
> El detalle de cómo quedó está en `docs/CONTEXTO.md`; lo de abajo es el plan
> original, conservado como registro.

**`oneprofile/backend/util/GreenhouseBoardUrl.java`** (nuevo)

- `static Optional<String> slugFrom(String url)` — recibe una URL del índice y
  devuelve el slug. Casos que cubren los tests: path extra
  (`/mercadolibre/jobs/123` → `mercadolibre`), query string, `http` y `https`,
  `www`, los dos dominios, y los que **no** son empresas (`/embed/job_board`,
  `robots.txt`, el host pelado sin path) → `Optional.empty()`.
- `static List<String> indexPatterns()` — los dos prefijos con los que se consulta
  el índice.

**`Ats.java`** no se toca: hoy tiene un solo valor y no hay una segunda
implementación que justifique colgarle comportamiento.

**`CompanyRepository.java`** — se agrega `List<Company> findAllByAts(Ats ats)`.

**Tests**: `GreenhouseBoardUrlTest` (unitario liso, sin Spring) y un test de
`findAllByAts` en `CompanyRepositoryTest`.

**Prueba manual:** `./mvnw test`.

---

## Paso 7 — Cliente de CommonCrawl, servicio y endpoint

**`util/GreenhouseBoardUrl.java`** (existe) — se le agrega
`static List<String> indexPatterns()`, los dos prefijos con los que se consulta el
índice (`boards.greenhouse.io/` y `job-boards.greenhouse.io/`). Venía del Paso 6 y
se postergó hasta acá, que es donde aparece el cliente que los consume.

**`util/CommonCrawlIndexClient.java`** (nuevo, `@Component`)

- `void forEachUrl(String indexId, String pattern, Consumer<String> onUrl)`
- `RestClient` contra
  `https://index.commoncrawl.org/{indexId}-index?url={pattern}&matchType=prefix&output=json`.
  Pide primero `&showNumPages=true` para saber cuántas páginas hay y después
  recorre `&page=N`, parseando cada línea del JSONL y sacando el campo `url`.
  Streaming por línea: la respuesta es enorme y no entra entera en memoria.
- **Timeouts de connect y read explícitos**, para que una corrida colgada muera con
  excepción en vez de esperar para siempre.
- El listado de índices disponibles está en `https://index.commoncrawl.org/collinfo.json`
  (el más reciente al 2026-09-11 era `CC-MAIN-2026-34`).

**`service/GreenhouseDiscoveryService.java`** (nuevo, `@Service`)

- `DiscoveryResult discover(String indexId)` — recorre los dos patrones volcando
  todo en un único `Set<String>`, resta los slugs que devuelve
  `findAllByAts(GREENHOUSE)`, y hace `saveAll` de los nuevos.
- `record DiscoveryResult(int slugsFound, int newCompanies)`.

**`controller/DiscoveryController.java`** (nuevo)

- `POST /admin/discovery/greenhouse?index=CC-MAIN-2026-34` → **202 Accepted**
  inmediato. El trabajo corre en un executor de **un solo hilo**, con un flag que
  responde **409** si ya hay una corrida en curso.
- **Logs**, único canal de resultado: índice consultado al arrancar; al terminar
  bien, `slugs encontrados` y `empresas nuevas guardadas`; al fallar (timeout de
  CommonCrawl, índice inexistente, error de base), el error con su excepción y el
  flag liberado — nunca morir en silencio.

**Tests**

- `GreenhouseDiscoveryServiceTest` (`@DataJpaTest` + un doble del cliente escrito a
  mano, sin red): deduplica capturas repetidas, unifica los dos dominios en una
  sola empresa, no reinserta las que ya estaban, y el `DiscoveryResult` reporta
  bien los números.
- `DiscoveryControllerTest` (`@WebMvcTest`): 202 y delegación en el servicio; 409
  con una corrida en curso.
- **Ningún test le pega a CommonCrawl de verdad.**

**Prueba manual:** `cd prod && docker compose up --build -d` →
`docker compose exec app curl -X POST 'localhost:8080/admin/discovery/greenhouse'`
→ 202 al toque → `docker compose logs -f app` muestra el avance y la línea final →
`select count(*) from company;` muestra las filas → un segundo POST y el conteo
casi no sube.

---

## Después de este plan (candidatos, sin priorizar)

- Agregar `name` a `Company` al implementar el cliente de la API de Greenhouse
  (con su migración `V2`).
- Modelar la vacante (`vacancy`), con relación unidireccional `Vacancy → Company`.
- Modelar el perfil del usuario (`profile`).
- Primer algoritmo de matching, simple, con tests sobre casos concretos.
- Endpoint HTTP para consultar las vacantes que matchean.
