# Paso 03 — hasta 3 pasadas sobre los índices fallidos de CommonCrawl

## Qué queda funcionando
`discoverOnRecentCommonCrawl` recorre los 10 índices más recientes (pasada 1); los que
fallaron se vuelven a pedir en la pasada 2, y los que fallen ahí, en la pasada 3. Solo quedan
en `failedIndexes` los que fallaron las tres.

## Contexto necesario
- Hoy: `GreenhouseDiscoveryService.discoverOnRecentCommonCrawl` pide
  `indexClient.latestIndexIds(10)`, lee cada índice con `discoverOnIndex`, y un índice que
  tira `RuntimeException` va a `WARN` y `failedIndexes` sin cortar la corrida.
- El 2026-09-14 fallaron 4 de 10 (`-2026-25` por `502`, tres por
  `header parser received no bytes`): CommonCrawl degradado, transitorio. No hay endpoint para
  índices sueltos.
- **Decisión de Elias:** 3 pasadas. **Los reintentos de `HttpRetry` no se tocan**: "fallido"
  es un índice que agotó sus reintentos.
- **Va en el service, no en el client:** el client lee un índice; recorrer los índices y
  juntar los fallidos ya es del service.
- Cada índice guarda lo suyo al terminar (`saveNew`), así que releer uno fallido no duplica:
  `company` tiene `unique (ats, slug)` y `saveNew` resta los conocidos.
- Sin espera entre pasadas (no se pidió). El resultado (`CommonCrawlResult`) mantiene su forma
  salvo lo que haga falta para que el controller siga logueando lo mismo; si hace falta
  cambiarla, es duda: preguntar.
- Reglas del repo: si no se usa o no se pidió, no se pone; ante una duda, parar y preguntar.

Leer antes de tocar: `service/GreenhouseDiscoveryService.java`,
`controller/DiscoveryController.java`, `test/.../service/GreenhouseDiscoveryServiceTest.java`,
`test/.../controller/DiscoveryControllerTest.java`.

## Qué se toca
- `GreenhouseDiscoveryService.discoverOnRecentCommonCrawl`: hasta 3 pasadas; la pasada N+1
  recibe solo los fallidos de la N; si una pasada no deja fallidos, no hay siguiente. El WARN
  por índice dice en qué pasada falló.
- Constante `PASSES = 3` con un comentario del porqué (CommonCrawl degradado es transitorio).

## Tests
En `GreenhouseDiscoveryServiceTest` (client falso como ya hacen los tests):
- Un índice que falla en la pasada 1 y anda en la 2 → sus empresas se guardan y no está en
  `failedIndexes`.
- Un índice que falla las 3 → está en `failedIndexes` y se pidió exactamente 3 veces.
- Todos andan en la 1 → cada índice se pide una sola vez.
- Un índice que falla en la 1 y la 2 y anda en la 3 → guardado, no fallido.
- `./mvnw -q test` verde.

## Guion de prueba
Local:
1. `./mvnw -q test 2>&1 | tail -40` → sin fallas.
2. `./mvnw -q test -Dtest=GreenhouseDiscoveryServiceTest 2>&1 | tail -20` → verde, y en el
   código de test se ven los cuatro casos de arriba.
(Correr el descubrimiento real en dev no forma parte: tarda y no fuerza fallas.)

## Preguntas abiertas
ninguna
