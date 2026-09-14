# Paso 03 — sondear todo, cargar vacantes y normalizar lo nuevo, en prod

## Qué queda funcionando
Todas las empresas de `company` sondeadas (ninguna con `board_status` nulo), las vacantes de
las `ACTIVE` cargadas y normalizadas. Es lo que convierte los slugs nuevos en vacantes. Este
paso no lleva código.

## Contexto necesario
- **Los tres procesos tienen que ir en este orden:** sondeo → vacantes → normalización. No
  hay cron: nada fuerza el orden, cada uno se dispara a mano cuando el anterior logueó
  `finished`. Los tres contestan 202, corren en un hilo, dan 409 si ya corren, y el log es el
  único canal de resultado.
- **Sondeo** (`POST /admin/probe/greenhouse`): recorre **todas** las empresas, no solo las
  nuevas, con 200 ms entre boards. Las 4.046 tardaron ~30 min, así que ~18.000 serán **~2 h 15
  a 2 h 30**. Log: `Greenhouse board probe finished: N companies, A not found, B empty, C active, D failed`.
  Una empresa que falla no aborta el sondeo y conserva su estado.
- **Tasas esperadas:** de las 4.046 originales de CommonCrawl, el 77% salió `ACTIVE`. De una
  muestra de 60 que solo trae Wayback, 23% `ACTIVE`, 8% `EMPTY` y 68% `NOT_FOUND` (orden de
  magnitud). Se esperan **~1.850 `ACTIVE` extra solo por Wayback**. Las nuevas de CommonCrawl
  no se sondearon nunca.
- **Vacantes** (`POST /admin/vacancies/greenhouse`): recorre todas las `ACTIVE`. La corrida
  anterior dio 128.953 vacantes sobre 3.118 empresas y duró toda una noche. Esta tiene más
  empresas. Log: `Greenhouse vacancy sweep finished: ...`. Avance:
  `select count(*), count(distinct company_id) from vacancy`.
- **Normalización** (`POST /admin/normalization/vacancies/missing`): normaliza las vacantes
  que no tienen fila en `normalized_vacancy` (columna `vacancy_id`). La total de 128.953 tardó
  24 s. Log: `Vacancy normalization (missing) finished: N inserted, M updated`.
- Prod: `ssh elitedesk1`, `~/oneprofile`, Docker sin sudo, endpoints por
  `docker compose exec app curl`. **`docker compose up -d` mata la corrida en curso**: no
  desplegar mientras corre.
- Leer antes: la sección "Mediciones y prod" de `docs/agents/METODOLOGIA.md`.

Las consultas `select` del guion se corren así:
`docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "<sql>"'`
(si el sql lleva comillas simples, pasarlo por un heredoc a `psql` en vez de `-c`).

## Qué se toca
Nada del código.

## Tests
No aplica.

## Guion de prueba
Prod (`ssh elitedesk1`, en `~/oneprofile`), sin `pull` ni `up -d`:
1. No hay ninguna corrida en curso (`docker compose logs --since 1h app | grep -E 'started|finished'`).
2. Antes:
   `select coalesce(board_status,'(sin sondear)'), count(*) from company group by 1` y
   `select count(*), count(distinct company_id) from vacancy`. Anotar los números.
3. `docker compose exec app curl -i -X POST 'localhost:8080/admin/probe/greenhouse'` → `202`.
   Esperar con `Monitor` hasta `Greenhouse board probe finished`. Tiene que verse `N companies`
   igual a `count(*)` de `company`, y `failed` chico (menos del 1%; si es más, **PREGUNTA**).
   Después, la consulta de estados **sin ninguna fila `(sin sondear)`**.
4. `docker compose exec app curl -i -X POST 'localhost:8080/admin/vacancies/greenhouse'` → `202`.
   Esperar con `Monitor` (horas; revisar cada ~20 min con el `select` de avance) hasta
   `Greenhouse vacancy sweep finished`. Tiene que verse que la cantidad de empresas con
   vacantes es cercana a las `ACTIVE` del punto 3 y que `vacancy` creció respecto del punto 2.
5. `docker compose exec app curl -i -X POST 'localhost:8080/admin/normalization/vacancies/missing'`
   → `202`, y `Vacancy normalization (missing) finished`. Después,
   `select count(*) from vacancy v where not exists (select 1 from normalized_vacancy n where n.vacancy_id = v.id)`
   → **0**.

Guardar las tres líneas `finished` y las consultas de antes y después en
`mediciones/slugs-13-09-2026/sondeo-y-carga.txt`.

## Preguntas abiertas
ninguna
