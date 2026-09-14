# Paso 02 — descubrimiento sobre Wayback, en prod

## Qué queda funcionando
`POST /admin/discovery/greenhouse/wayback` recorre el CDX de Wayback para los dos dominios de
Greenhouse y `company` llega a ~18.000. Nunca se corrió en prod. Este paso no lleva código.

## Contexto necesario
- **Qué hace (commit `7f33a53`):** pide el conteo de páginas **sin `fl`**, porque con `fl`
  contesta `-`. Después lee cada página en texto plano, una URL por línea, con **2 s de pausa**
  entre páginas. El 429 se reintenta **solo en Wayback**: 4 intentos desde 30 s (30, 60, 120 s).
  Guarda las empresas nuevas **en lotes de 500 mientras lee**, así lo guardado queda aunque la
  lectura falle.
- **Medido (2026-09-13, fuera de la app):** 286 páginas de `boards.greenhouse.io/` y 153 de
  `job-boards.greenhouse.io/` (**439**). Bajaron en ~40 min sin ningún 429 ni 5xx. Salieron
  **17.730 slugs** con las reglas de la app. La unión con los 16 índices de CommonCrawl
  medidos es **18.121**.
- El archivo crece con el tiempo, así que un número algo mayor al medido no es falla.
- Una página fuera de rango da 400, que no se reintenta.
- **Punto abierto conocido, que no hace fallar el paso:** si una línea llega cortada se guarda
  un slug recortado (`mercadol`). El sondeo lo marca después como `NOT_FOUND`.
- Prod: `ssh elitedesk1`, `~/oneprofile`, Docker sin sudo, endpoints por
  `docker compose exec app curl`. **`docker compose up -d` mata la corrida en curso.** El lock
  es compartido con CommonCrawl: 409 si esa corrida sigue.
- Leer antes: la sección "Mediciones y prod" de `docs/agents/METODOLOGIA.md`.

## Qué se toca
Nada del código.

## Tests
No aplica (`./mvnw test` daba 126 en verde al construirse).

## Guion de prueba
Prod (`ssh elitedesk1`, en `~/oneprofile`). **No hacer `pull` ni `up -d`**: la imagen ya es la
del paso anterior.
1. `docker compose logs --since 3h app | grep -E 'started|finished'`: no hay corrida en curso.
   Si hay una, **PREGUNTA**.
2. Contar antes: `docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "select count(*) from company"'`
   y anotar el número.
3. `docker compose exec app curl -i -X POST 'localhost:8080/admin/discovery/greenhouse/wayback'` → `202`.
4. Esperar con `Monitor` (~40 min) hasta `Greenhouse discovery on Wayback finished`, mirando
   el conteo de `company` cada ~5 min: tiene que subir de a lotes. Guardar el log en
   `<scratchpad>/wayback.txt`. Tiene que verse:
   - `Greenhouse discovery on Wayback finished: N slugs found, M new companies saved`, con N
     **≥ ~17.700**.
   - Ningún `WARN` que termine en falla. Los `Retrying` que después se recuperan están bien.
5. Contar después → **~18.000** (entre 17.500 y 18.500).

**Falla** si la corrida termina con error o N queda muy por debajo de 17.730. En ese caso,
copiá al reporte el `WARN` completo y la última página leída; lo ya guardado queda en la base.
**Pregunta** si N está bien pero `company` queda fuera del rango.

Guardar el log crudo en `mediciones/slugs-13-09-2026/log-wayback-prod.txt`.

## Preguntas abiertas
ninguna
