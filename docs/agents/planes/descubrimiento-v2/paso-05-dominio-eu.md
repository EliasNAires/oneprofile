# Paso 05 — descubrir el dominio EU de Greenhouse

## Qué queda funcionando
El descubrimiento (CommonCrawl y Wayback) reconoce también los boards de
`job-boards.eu.greenhouse.io` y `boards.eu.greenhouse.io`, y guarda sus slugs como empresas
de `Ats.GREENHOUSE`. Sondeo y carga no cambian.

## Contexto necesario
- Medido al planificar (2026-09-14):
  - `boards-api.eu.greenhouse.io` **no resuelve**. La API de siempre
    (`https://boards-api.greenhouse.io/v1/boards/<slug>/jobs`) contesta boards EU: 15 slugs
    EU probados (`growe`, `openup`, `imc`, `flaconi`, `nice`, `gropyus`, `ionos`...), los
    vivos 200 con `absolute_url` en `job-boards.eu.greenhouse.io`/`boards.eu.greenhouse.io`,
    los muertos 404. Un slug es un espacio de nombres único para ambas regiones.
  - En `CC-MAIN-2026-34`: `job-boards.eu.` 5.565 capturas y `boards.eu.` 471; 373 slugs EU
    distintos. `boards.eu.greenhouse.io/<slug>` redirige (301) a `job-boards.eu...`.
    Formato de URL igual al de los dominios actuales (incluidos los `embed/job_board?for=X`).
  - No hay otras regiones: `ca`, `apac`, `au`, `uk`, `jp` no resuelven; `job-boards.us`
    redirige a la web de la empresa y no está en el índice.
  - **Vacantes EU, verificado:** `…/v1/boards/<slug>/jobs?content=true&pay_transparency=true`
    (la URL de la carga) trae los boards EU completos: `jobs` = `meta.total` y todas con
    `content` en proton 69, imc 173, flaconi 11, growe 18, ionos 32, nice 173; coincide con el
    board EU público donde no pagina (growe, ionos, nice). `job-boards.eu.greenhouse.io/v1/...`
    da 404: no hay API EU. **Sondeo y carga no cambian de código**; Elias pidió que traigan
    los EU, y eso se prueba en el guion.
- `GreenhouseBoardUrl` tiene el regex de URL (`^https://(?:www\.)?(?:job-)?boards\.greenhouse\.io/(.*)$`)
  y `INDEX_PATTERNS` (prefijos que piden CommonCrawl y Wayback). Wayback usa los mismos
  patrones, así que **también empieza a traer EU** (decisión: es el mismo cambio).
- Reglas de `GreenhouseBoardUrl` que se mantienen: solo `https`, embeds cuentan, slug
  `[A-Za-z0-9_-]+`.
- Reglas del repo: si no se usa o no se pidió, no se pone; ante una duda, parar y preguntar.

Leer antes de tocar: `util/GreenhouseBoardUrl.java`, `test/.../util/GreenhouseBoardUrlTest.java`,
y en `test/.../service/GreenhouseDiscoveryServiceTest.java` cómo se usan los patrones.

## Qué se toca
- `GreenhouseBoardUrl`: el regex acepta `eu.` opcional entre `boards.` y `greenhouse.io`
  (`(?:job-)?boards\.(?:eu\.)?greenhouse\.io`); `INDEX_PATTERNS` suma
  `boards.eu.greenhouse.io/` y `job-boards.eu.greenhouse.io/`. Comentarios actualizados
  ("cuatro dominios", y por qué no cambia la API).
- Tests del service que dependan de que los patrones sean dos: ajustarlos.

## Tests
- `GreenhouseBoardUrlTest`: `https://job-boards.eu.greenhouse.io/proton/jobs/123` → `proton`;
  `https://boards.eu.greenhouse.io/embed/job_board?for=nice` → `nice`;
  `http://job-boards.eu.greenhouse.io/proton` → vacío (solo https);
  `https://job-boards.us.greenhouse.io/stripe` → vacío; `indexPatterns()` trae los cuatro.
- `GreenhouseDiscoveryServiceTest`: un slug visto por `job-boards.greenhouse.io` y por
  `job-boards.eu.greenhouse.io` se guarda una sola vez.
- `./mvnw -q test` verde.

## Guion de prueba
Local:
1. `./mvnw -q test 2>&1 | tail -40` → sin fallas.
2. Dev: `./mvnw spring-boot:run > <scratchpad>/dev.log 2>&1 &`, esperar `Started`,
   `curl -i -X POST localhost:8080/admin/discovery/greenhouse/commoncrawl` → 202, esperar
   `grep -m1 -E 'Greenhouse discovery on CommonCrawl (finished|aborted)'` (5–25 min).
3. `docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "select count(*) from company where slug in (''proton'',''imc'',''flaconi'',''growe'',''gropyus'',''ionos'')"'`
   → al menos 4 (pueden faltar si el índice que los tenía falló las 3 pasadas; entonces citar
   `failedIndexes` del log).
4. `grep -c 'eu.greenhouse.io' <scratchpad>/dev.log` → aparecen pedidos a los patrones EU
   (líneas de página o de reintento).
5. Sondeo y carga traen los EU sin cambios de código:
   `curl -i -X POST localhost:8080/admin/probe/greenhouse` → 202; esperar
   `grep -m1 -E 'Greenhouse board probe (finished|aborted)'` (con lo descubierto pueden ser
   miles de empresas: ~130/min; si son más de ~2.000, en vez de esperar el sondeo entero,
   vaciar `company` antes del paso 2 y dejar solo los 6 slugs EU de arriba insertados a mano).
   `psql ... -c "select slug, board_status, name from company where slug in (''proton'',''imc'',''ionos'')"`
   → `ACTIVE` con nombre (`Proton`, `IMC`, `IONOS DE`).
6. `curl -i -X POST localhost:8080/admin/vacancies/greenhouse/proton` → 200 con `fetched` > 0;
   `psql ... -c "select count(*), min(url) from vacancy v join company c on c.id = v.company_id where c.slug = ''proton''"`
   → mismas filas que `fetched` y URL en `job-boards.eu.greenhouse.io`. Parar la app.

## Preguntas abiertas
ninguna
