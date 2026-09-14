# Paso 01 — descubrimiento sobre los 10 índices recientes de CommonCrawl, en prod

## Qué queda funcionando
`POST /admin/discovery/greenhouse/commoncrawl` lee los 10 índices más recientes sin que falle
ninguno, y `company` suma lo que faltaba de esos índices. Es la prueba en prod de la corrección
de las fallas del 2026-09-13. Este paso no lleva código.

## Contexto necesario
- **Primera corrida (2026-09-13, 18:00–18:08 UTC): falló.** 3 índices leídos, 7 fallidos, 475
  nuevas, y `company` pasó de 6.988 a **7.463**. Hubo dos causas:
  1. En 5 índices, una página llegó cortada a mitad de línea (`Unexpected end-of-input`). La
     excepción de Jackson no se reintentaba.
  2. En 2 índices (`-21` y `-04`), un `400 Bad Request` sin cuerpo. **La causa no se conoce.**
- **Corrección, ya en prod-candidata (`7f33a53`):**
  - La línea cortada se convierte en `ResourceAccessException` y se reintenta (4 intentos desde
    2 s), con un mensaje que dice que la página llegó cortada (`Page of the index arrived cut`).
  - Un 4xx ahora trae el cuerpo en el mensaje, y antes de rendirse `HttpRetry` loguea
    `CommonCrawl index failed on <página y patrón> (attempt n of m): <mensaje>. Giving up`.
- **Qué ya está cargado:** `CC-MAIN-2026-34`, `-30`, `-25`, `-21`, `-17` y `2025-51`. Esos dan
  0 nuevas.
- **Número esperado.** La unión medida offline de los 9 más recientes (`-34` … `2025-51`) es
  **8.328**. El décimo, `CC-MAIN-2025-47`, no se midió (sus vecinos aportaron ~250). Se espera
  `company` **entre 8.328 y ~8.650**. Los 10 ids salen de `collinfo.json` en cada corrida; si
  CommonCrawl publicó un índice nuevo, la ventana se corre un lugar y el número puede variar.
  Anotá qué 10 ids se leyeron.
- Prod: `ssh elitedesk1`, carpeta `~/oneprofile`, Docker **sin sudo**. No hay puertos
  publicados: a los endpoints se entra con `docker compose exec app curl`.
  **`docker compose up -d` mata cualquier corrida en curso.**
- Descubrir y Wayback **comparten un lock**: si hay otra corrida, el POST da 409.
- Leer antes: la sección "Mediciones y prod" de `docs/agents/METODOLOGIA.md`.

## Qué se toca
Nada del código. Solo se despliega y se corre en prod.

## Tests
No aplica (código ya testeado: `./mvnw test` daba 126 en verde al construirse).

## Guion de prueba
Local:
1. `gh run list -w publish.yml -L 3`: la corrida del commit `dd6d387` o una posterior está en
   verde. Si no, **PREGUNTA**.

Prod (`ssh elitedesk1`, en `~/oneprofile`):
2. `docker compose logs --since 2h app | grep -E 'started|finished'`: no hay ninguna corrida en
   curso (todo `started` tiene su `finished`). Si hay una, **PREGUNTA**, sin desplegar.
3. `docker compose pull && docker compose up -d`, y después
   `docker image inspect ghcr.io/eliasnaires/oneprofile-backend:latest --format '{{.Created}}'`:
   la fecha tiene que ser posterior a la corrida de pipeline del punto 1.
4. Contar antes: `docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "select count(*) from company"'`
   → **7.463**, o anotar el número si difiere.
5. `docker compose exec app curl -i -X POST 'localhost:8080/admin/discovery/greenhouse/commoncrawl'` → `202`.
6. Guardar el log a un archivo del scratchpad
   (`docker compose logs --since 30m app > <scratchpad>/cc.txt`) y esperar con `Monitor` hasta
   que aparezca `Greenhouse discovery on CommonCrawl finished` (~8–15 min). Tiene que verse:
   - `10 indexes read`, `0 indexes failed` (o la lista vacía).
   - Una línea `CommonCrawl index <id>: N slugs found, M new companies saved` por índice. Los 6
     ya cargados dan `0 new`.
   - Si hubo cortes, `Retrying` con `arrived cut` que después se recupera.
7. Contar después (mismo comando del punto 4) → **entre 8.328 y ~8.650**.

**Falla** si queda algún índice fallido. En ese caso, copiá al reporte las líneas `Giving up`
y los `WARN` completos: dicen la página y lo que contestó el índice. **Pregunta** si todos leen
bien pero el número queda fuera del rango.

Guardar el log crudo en `mediciones/slugs-13-09-2026/log-commoncrawl-prod.txt`.

## Preguntas abiertas
ninguna
