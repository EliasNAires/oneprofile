# Paso 06 — blacklist de slugs

## Qué queda funcionando
Existe la tabla `blacklisted_slug` y el descubrimiento (CommonCrawl y Wayback) no guarda como
empresa ningún slug que esté en ella. Todavía nadie la llena (lo hace el paso siguiente); en
este paso se prueba insertando a mano.

## Contexto necesario
- Motivo: slugs truncados (`mercadol` frente a `mercadolibre`) que salen de líneas cortadas de
  Wayback o de índices cortados en un salto de línea. El sondeo los marca `NOT_FOUND`; se van a
  borrar de `company` y, si no hubiera blacklist, el descubrimiento siguiente los volvería a
  insertar. **Decisión de Elias:** tabla blacklist aparte (no un estado en `company`).
- Identidad de un slug: el par `(ats, slug)` (un slug es único solo dentro de su ATS). `Ats` es
  enum guardado como `varchar` (`@Enumerated(STRING)`).
- Migraciones Flyway en `src/main/resources/db/migration/` (`V1`..`V4`); `ddl-auto=validate`:
  entidad y migración tienen que coincidir o la app no levanta. `company` usa
  `create sequence company_seq start with 1 increment by 50` e `@Id @GeneratedValue`.
- El descubrimiento arma el conjunto de "conocidos" con
  `companyRepository.findSlugsByAts(Ats.GREENHOUSE)` en dos lugares: `saveNew` (CommonCrawl) y
  `discoverOnWayback`. Devuelve **slugs, no entidades**, a propósito (no hidratar miles de filas):
  lo mismo para la blacklist.
- Paquetes por capa: `model`, `repository`, `service`.
- Reglas del repo: si no se usa o no se pidió, no se pone (nada de fecha de alta ni motivo si
  nadie los lee); ante una duda, parar y preguntar. `@DataJpaTest` para repositorios.

Leer antes de tocar: `model/Company.java`, `repository/CompanyRepository.java`,
`db/migration/V1__create_company.sql`, `service/GreenhouseDiscoveryService.java` y su test,
`test/.../repository/CompanyRepositoryTest.java`.

## Qué se toca
- `db/migration/V5__create_blacklisted_slug.sql`: secuencia y tabla `blacklisted_slug`
  (`id`, `ats`, `slug`, `unique (ats, slug)`), al molde de `V1`.
- `model/BlacklistedSlug.java`: entidad `(ats, slug)`.
- `repository/BlacklistedSlugRepository.java`: `findSlugsByAts(Ats)` que devuelve slugs.
- `GreenhouseDiscoveryService`: el conjunto de conocidos suma los slugs de la blacklist, en
  CommonCrawl y en Wayback.

## Tests
- `BlacklistedSlugRepositoryTest` (`@DataJpaTest`): `findSlugsByAts` devuelve los slugs
  guardados; guardar dos veces el mismo `(ats, slug)` falla.
- `GreenhouseDiscoveryServiceTest`: un slug en la blacklist que aparece en CommonCrawl no se
  guarda y no cuenta como nueva empresa; ídem en Wayback.
- `./mvnw -q test` verde.

## Guion de prueba
Local:
1. `./mvnw -q test 2>&1 | tail -40` → sin fallas.
2. `./mvnw spring-boot:run > <scratchpad>/dev.log 2>&1 &` → llega a `Started BackendApplication`
   (valida entidad contra migración); en el log, Flyway aplica `V5`.
3. `docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "\d blacklisted_slug"'`
   → columnas `id`, `ats`, `slug` y la restricción única. Parar la app.

## Preguntas abiertas
ninguna
