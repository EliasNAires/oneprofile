# Paso 07 — limpieza de slugs truncados al final del sondeo

## Qué queda funcionando
Cuando termina el sondeo (`POST /admin/probe/greenhouse`), se buscan los slugs truncados, se
borran de `company` y se agregan a `blacklisted_slug`, y el log dice cuántos. El descubrimiento
siguiente ya no los inserta (la blacklist existe desde el paso anterior).

## Contexto necesario
- **Truncado** (definición de Elias): una empresa `NOT_FOUND` cuyo slug es prefijo propio del
  slug de una empresa `ACTIVE` del mismo ATS. Ej.: `mercadol` (`NOT_FOUND`) frente a
  `mercadolibre` (`ACTIVE`). Salen de líneas cortadas de Wayback o de índices de CommonCrawl
  cortados en un salto de línea.
- Riesgo aceptado: un `NOT_FOUND` real que es prefijo de otra empresa (`acme` / `acmecorp`)
  también cae; si abre un board después, no se descubre.
- **Decisiones de Elias:** borrar de `company` + blacklist; **corre sola al final del sondeo**;
  la app no aborta sola por fallas.
- Ya existe (paso anterior): tabla `blacklisted_slug (id, ats, slug, unique (ats, slug))`,
  entidad `model/BlacklistedSlug`, `repository/BlacklistedSlugRepository`.
- `CompanyRepository.findSlugsByAtsAndBoardStatus(Ats, BoardStatus)` ya devuelve slugs por
  estado.
- **Trampas:**
  - `vacancy.company_id references company` **sin `on delete cascade`**
    (`V3__create_vacancy.sql`): borrar una empresa con vacantes falla. Una `NOT_FOUND` puede
    tener vacantes de cuando era `ACTIVE` (la carga solo pide a las `ACTIVE` y no borra las de
    las demás). Ver pregunta abierta.
  - `_` es válido en un slug y es comodín de `LIKE`: el "empieza con" **no** va con `LIKE`.
    Hacerlo en Java (p. ej. `TreeSet` de slugs `ACTIVE` y `ceiling(slug)` + `startsWith`) o con
    `starts_with()` de Postgres.
  - `saveAll` y los borrados juntos en **una** transacción corta, en un service aparte del
    sondeo (el `@Transactional` no aplica llamando dentro del mismo bean).
- Molde de log del paso de logs: el cierre del proceso es `finished` / `aborted`. El
  controller hoy loguea `Greenhouse board probe finished: ...` y, por excepción,
  `Greenhouse board probe aborted`.
- Diseño: un service por caso de uso → `service/GreenhouseTruncatedSlugCleanupService` con un
  método que devuelve cuántos borró; lo llama `BoardProbeController` después de `probeAll`, en
  el mismo hilo y bajo el mismo flag de corrida.
- Reglas del repo: si no se usa o no se pidió, no se pone; ante una duda, parar y preguntar.

Leer antes de tocar: `controller/BoardProbeController.java`,
`service/GreenhouseBoardProbeService.java`, `repository/CompanyRepository.java`,
`repository/BlacklistedSlugRepository.java`, `model/BlacklistedSlug.java`,
`db/migration/V3__create_vacancy.sql` y los tests de controller y repositorio.

## Qué se toca
- `service/GreenhouseTruncatedSlugCleanupService.java` (nuevo): busca truncados, los borra de
  `company` y los agrega a la blacklist; `@Transactional`; devuelve el conteo.
- `CompanyRepository`: lo mínimo para borrar por `(ats, slug)` si no alcanza lo existente.
- `BoardProbeController.run`: después del `finished` del sondeo, corre la limpieza y loguea
  `Greenhouse truncated slug cleanup finished: <n> companies removed and blacklisted`; si la
  limpieza revienta, `Greenhouse truncated slug cleanup aborted` (el sondeo ya quedó guardado).

## Tests
- Service (`@DataJpaTest` + Testcontainers como los demás):
  - `mercadol` `NOT_FOUND` + `mercadolibre` `ACTIVE` → `mercadol` sale de `company` y entra a la
    blacklist; `mercadolibre` queda.
  - `mercado_` `NOT_FOUND` + `mercadoX` `ACTIVE` → no se borra (el `_` no es comodín).
  - Prefijo de una `EMPTY` o de otra `NOT_FOUND` → no se borra.
  - Un `NOT_FOUND` igual a un `ACTIVE` de otro ATS: no aplica hoy (un solo ATS); no testear.
  - Comportamiento con vacantes según la respuesta a la pregunta abierta.
- Controller: después de un sondeo exitoso se llama a la limpieza y aparece su línea
  `finished`; si la limpieza tira, aparece `aborted` y el flag de corrida se libera.
- `./mvnw -q test` verde.

## Guion de prueba
Local (dev):
1. `./mvnw -q test 2>&1 | tail -40` → sin fallas.
2. `./mvnw spring-boot:run > <scratchpad>/dev.log 2>&1 &`, esperar `Started`.
3. Insertar: `figma` y `figm` (truncado de una empresa viva real) y `no-existe-xyz`:
   `docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "insert into company (id, ats, slug) values (nextval(''company_seq''), ''GREENHOUSE'', ''figma''), (nextval(''company_seq''), ''GREENHOUSE'', ''figm''), (nextval(''company_seq''), ''GREENHOUSE'', ''no-existe-xyz'')"'`
   (si `figm` resultara `ACTIVE` en la API real, elegir otro prefijo de `figma` que dé 404 con
   `curl -s -o /dev/null -w '%{http_code}' https://boards-api.greenhouse.io/v1/boards/<slug>/jobs`).
4. `curl -i -X POST localhost:8080/admin/probe/greenhouse` → 202; esperar
   `grep -m1 -E 'Greenhouse truncated slug cleanup (finished|aborted)'` → `finished: 1 companies removed and blacklisted`.
5. `psql ... -c "select slug, board_status from company order by slug"` → `figma` `ACTIVE`,
   `no-existe-xyz` `NOT_FOUND`, sin `figm`; `psql ... -c "select slug from blacklisted_slug"` → `figm`.
6. Parar la app.

## Preguntas abiertas
1. Un truncado `NOT_FOUND` que todavía tiene vacantes en `vacancy` (de cuando era `ACTIVE`):
   ¿se borran sus vacantes junto con la empresa, o se saltea (queda en `company`, se loguea
   cuántos se saltearon)?
