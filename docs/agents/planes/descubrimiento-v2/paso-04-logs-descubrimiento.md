# Paso 04 — logs de avance del descubrimiento

## Qué queda funcionando
El descubrimiento en CommonCrawl loguea el comienzo de cada pasada (con qué índices) y una
línea por índice terminado; el de Wayback loguea el avance por páginas. El cierre por
excepción de ambos dice `aborted`, y ningún WARN intermedio comparte texto con el cierre.

## Contexto necesario
- Ya existe `oneprofile.backend.util.ProgressLog` (paso anterior): formato
  `<proceso> progress: <hechos> of <total> <unidad> (<pct>%), <fallas> failed, <ritmo> per min, <transcurrido> elapsed`.
  Leer su código antes de usarlo; usarlo tal como está (si no alcanza, preguntar).
- CommonCrawl ya hace hasta 3 pasadas sobre los índices fallidos (paso anterior), en
  `GreenhouseDiscoveryService.discoverOnRecentCommonCrawl`.
- **Trampa medida:** en la corrida anterior un monitor cortó antes de tiempo porque el WARN del
  service `Greenhouse discovery on CommonCrawl index X failed: ...` contiene el texto del error
  final del controller `Greenhouse discovery on CommonCrawl failed`. Tras este paso, el cierre
  es `Greenhouse discovery on <fuente> finished` o `... aborted`, y **ninguna otra línea
  contiene esas dos frases**.
- Líneas actuales que se conservan: por índice
  `CommonCrawl index {}: {} slugs found, {} new companies saved` (hoy en el controller, al
  final) y `Greenhouse discovery on CommonCrawl finished: {} indexes read, {} new companies saved, {} indexes failed [..]`;
  `Greenhouse discovery on Wayback finished: {} slugs found, {} new companies saved`.
- Tiempos medidos: un índice de CommonCrawl 15–50 s cuando anda; Wayback ~98 min con ~270
  páginas en `boards.greenhouse.io/` y menos en `job-boards.greenhouse.io/` (2 s de pausa por
  página + reintentos). Las páginas de cada patrón se conocen al empezarlo
  (`numberOfPages` en `WaybackCdxClient`).
- Diseño a respetar: el client lee; recorrer índices es del service. Wayback recorre páginas
  dentro de `WaybackCdxClient.forEachUrl`: el avance por página se loguea ahí, con
  `ProgressLog` por patrón, cada **10** páginas (~3–4 min).
- Reglas del repo: si no se usa o no se pidió, no se pone; ante una duda, parar y preguntar.

Leer antes de tocar: `service/GreenhouseDiscoveryService.java`,
`controller/DiscoveryController.java`, `client/WaybackCdxClient.java`,
`util/ProgressLog.java` y los tests de esos tres.

## Qué se toca
- `GreenhouseDiscoveryService`: al empezar cada pasada, INFO
  `CommonCrawl pass <n> of 3: <k> indexes [ids]`; al terminar cada índice, INFO con la línea
  por índice de hoy (se mueve del controller al momento en que termina el índice) más la pasada.
  WARN de índice fallido reescrito para no contener `discovery on CommonCrawl failed`
  (p. ej. `CommonCrawl index {} failed on pass {}: {}`).
- `WaybackCdxClient.forEachUrl`: `ProgressLog` por patrón, cada 10 páginas, unidad `pages`.
- `DiscoveryController`: cierre por excepción `Greenhouse discovery on {} aborted`; se saca el
  loop de líneas por índice si pasó al service; la línea `finished` queda igual.

## Tests
- Service: con un client falso, los logs muestran el comienzo de cada pasada y una línea por
  índice terminado; el WARN de fallido no contiene `discovery on CommonCrawl failed`.
- Wayback client: con `MockRestServiceServer` y 25 páginas falsas (o intervalo inyectado
  chico), hay líneas de avance a las 10 y 20.
- Controller: una excepción del service deja `aborted` en el log.
- Capturar logs con `OutputCaptureExtension` (o lo que ya usen los tests del paso anterior).
- `./mvnw -q test` verde.

## Guion de prueba
Local:
1. `./mvnw -q test 2>&1 | tail -40` → sin fallas.
2. `grep -rn 'failed' src/main/java/oneprofile/backend/service/GreenhouseDiscoveryService.java src/main/java/oneprofile/backend/controller/DiscoveryController.java`
   → ninguna línea contiene `discovery on CommonCrawl failed` ni `discovery on Wayback failed`
   ni `discovery on {} failed`.
3. Dev: `./mvnw spring-boot:run > <scratchpad>/dev.log 2>&1 &`, esperar `Started`,
   `curl -i -X POST localhost:8080/admin/discovery/greenhouse/commoncrawl` → 202. Esperar con
   `timeout 1800 grep -m1 -E 'Greenhouse discovery on CommonCrawl (finished|aborted)' <(tail -f <scratchpad>/dev.log)`
   (puede tardar 5–25 min; CommonCrawl degradado lo alarga). Tiene que verse
   `CommonCrawl pass 1 of 3: 10 indexes`, una línea por índice terminado, y el `finished`.
   Anotar la hora de cada línea citada. Parar la app.
(Wayback no se corre en dev: ~98 min.)

## Preguntas abiertas
ninguna
