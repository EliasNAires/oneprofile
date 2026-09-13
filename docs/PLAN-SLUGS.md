# Plan — traer a prod todos los slugs de Greenhouse que se pueden descubrir

> Plan de varios pasos, autocontenido: se retoma leyendo este archivo,
> `docs/METODOLOGIA.md` y `docs/CONTEXTO.md`. Los números y el porqué de cada decisión
> están en **`docs/MEDICION-SLUGS.md`**; leelo antes de empezar. Se borra al terminar
> el plan y lo que quede pasa a `CONTEXTO.md`.

## Estado del plan

- **Hecho:** la medición (`docs/MEDICION-SLUGS.md`) y cuatro índices extra de
  CommonCrawl corridos en prod (`CC-MAIN-2026-30`, `-25`, `-21`, `-17`) con el endpoint
  que ya existe. `company` quedó en **6.988**: 3.121 `ACTIVE`, 217 `EMPTY`,
  708 `NOT_FOUND` y **2.942 sin sondear**.
- **No hay código escrito.** En la sesión de la medición se empezó un cliente de
  Wayback en una branch y **se descartó a propósito**, para construirlo en una sesión
  limpia. No hay nada que rescatar.
- **Por dónde retomar:** el paso 1.

## El objetivo y la escala

Pasar de 6.988 empresas a las **~18.000** que suman CommonCrawl y Wayback. No todas
están vivas: se espera del orden de **~1.850 `ACTIVE` extra solo por Wayback**, más las
que den los índices de CommonCrawl que faltan, cuya tasa de vida no se midió.

## Decisiones ya tomadas por Elias

- **Wayback tiene endpoint propio**, `POST /admin/discovery/greenhouse/wayback` y sin
  parámetros. El de CommonCrawl queda igual, con `?index=` obligatorio.
- **El dominio EU (`job-boards.eu.greenhouse.io`, 848 slugs) queda afuera** por ahora.
- **Las reglas de `GreenhouseBoardUrl` no se tocan:** siguen aceptando solo https y
  sin `&` ni `)` en el slug. Se pierden 77 y ~3 slugs respectivamente; el volumen no lo
  justifica.
- **Los índices de CommonCrawl los corre el agente en prod, a su criterio.**

## Paso 1 — Terminar CommonCrawl con los índices que faltan (sin código)

Con el endpoint que ya existe, uno por vez (da 409 si hay una corrida en curso):

```bash
docker compose exec app curl -i -X POST 'localhost:8080/admin/discovery/greenhouse?index=<id>'
```

- Los ids salen de `https://index.commoncrawl.org/collinfo.json`; no hay que
  inventarlos. Faltan los 12 de la muestra medida (`CC-MAIN-2026-12` a
  `CC-MAIN-2023-50`, ver la tabla de H1) y el resto de los 127.
- Cada corrida tarda **15-50 s**. Se encadenan con un script en el servidor que hace
  el POST y espera en el log la línea `Greenhouse discovery finished` o
  `... failed` antes de seguir. Ese script **tiene que usar paths absolutos**
  (`/home/eliasaires/oneprofile`): corre con `sudo` y con `~` termina en `/root`, donde
  no hay `compose.yaml`. Ya pasó una vez.
- **Criterio de corte:** anotar por índice las empresas nuevas y seguir hacia atrás
  hasta que varios índices seguidos aporten casi nada. **Pregunta para Elias:** ¿qué
  cuenta como "casi nada"? Propuesta: parar después de 5 índices seguidos con menos
  de 20 nuevas cada uno.
- Verificación: `select count(*) from company` sube lo que dicen los logs.

## Paso 2 — Cliente de Wayback y endpoint

Calcado del descubrimiento por CommonCrawl, que ya está probado.

**`service/WaybackCdxClient`** (nuevo), con el mismo molde que `CommonCrawlIndexClient`:

- Un solo método, `forEachUrl(String pattern, Consumer<String> onUrl)`: pide
  `showNumPages=true` y recorre `page=0..N-1`, leyendo **línea por línea** con
  `.exchange()`.
- **URL de las páginas:**
  `https://web.archive.org/cdx/search/cdx?url={pattern}&matchType=prefix&fl=original&page=N`.
  Devuelve una URL por línea, **sin JSON**.
- **El conteo de páginas se pide sin `fl`**: con `fl` Wayback contesta `-` en vez del
  número. La respuesta es un entero en texto plano.
- **Reintentos:** 5xx y errores de red se reintentan con backoff (4 intentos: 2 s,
  4 s, 8 s). Un 4xx no se reintenta.
- **Timeouts** explícitos.
- **Constructores:** uno público sin argumentos y otro de paquete
  `(RestClient.Builder, Duration)` para el test.

**`GreenhouseDiscoveryService`**:
- Recibe también el cliente de Wayback.
- Gana `discoverOnWayback()`, que vuelca los dos patrones de
  `GreenhouseBoardUrl.indexPatterns()` al mismo `Set`.
- La resta de lo ya guardado y el `saveAll` se comparten con `discover(indexId)` en
  un método privado.
- Devuelve el mismo `DiscoveryResult`.

**`DiscoveryController`**:
- Suma `POST /admin/discovery/greenhouse/wayback`.
- **Comparte el `AtomicBoolean` y el executor** con el de CommonCrawl, porque dos
  corridas a la vez se pelearían por las mismas empresas.
- La línea de log `Greenhouse discovery finished: N slugs found, M new companies saved`
  queda **idéntica**, y el script del paso 1 depende de ella.

**Tests:**
- `WaybackCdxClientTest` con `MockRestServiceServer`: recorre las páginas que dice el
  conteo, no pide ninguna si dice 0, saltea líneas vacías, se recupera de 504/503, se
  rinde al cuarto intento y un 400 falla sin reintentar. Además verifica que las
  páginas llevan `fl=original`.
- `GreenhouseDiscoveryServiceTest`:
  - Suma un doble a mano de `WaybackCdxClient`, igual al que ya tiene para
    CommonCrawl.
  - Caso nuevo: Wayback trae una empresa guardada y una nueva, con la nueva por los
    dos dominios; da `DiscoveryResult(2, 1)`.
  - Los tests existentes pasan a construir el servicio con los dos dobles.
- `DiscoveryControllerTest`: 202 y delegación para el endpoint de Wayback, y **409 si
  hay una corrida de CommonCrawl en curso**, que prueba que el lock es compartido.

**Preguntas para Elias antes de escribirlo:**
- **La transacción.** `discover` es `@Transactional` entero, y una corrida de Wayback
  son 450 páginas, **~40 minutos con pausa**. Así retiene una conexión todo ese tiempo
  aunque solo escriba al final. ¿Se acepta igual o se separa la lectura (sin
  transacción) del guardado?
- **La pausa entre páginas.** La medición usó 2 s y no vio ningún 429. ¿Se pone la
  pausa en el cliente, o se prueba sin ella y se agrega si aparece un 429?
- **El 429.** ¿Se reintenta como un 5xx? Hoy un 4xx no se reintenta.

**Prueba manual:** `pull` + `up -d` en el servidor, el POST de Wayback, y en el log
`N slugs found` cerca de **17.730**, con `M new companies saved` cerca de la cantidad de
slugs de Wayback que todavía no estén en `company` después del paso 1 (con la muestra
de CommonCrawl completa serían ~8.035).

## Paso 3 — Sondear lo nuevo

`POST /admin/probe/greenhouse` (ya existe) **sondea todas las empresas cada vez**. Con
~18.000 a ~5 por segundo más la latencia de la API son **~2 horas y media** (4.046
tardaron 30 min), y la mayoría ya tendría estado.

**Pregunta para Elias:** ¿se corre entero así, o antes se agrega sondear solo las que
nunca se sondearon (`board_status is null`)? `CONTEXTO.md` ya lo anota como punto
abierto: los datos para filtrar están, la consulta no.

Verificación: `select coalesce(board_status,'(sin sondear)'), count(*) from company group by 1`
sin filas sin sondear. En la muestra de Wayback, 23% dio `ACTIVE`.

## Paso 4 — Cargar las vacantes de las nuevas `ACTIVE`

`POST /admin/vacancies/greenhouse` (ya existe) recorre todas las `ACTIVE`, **las viejas
también**. Hoy son 3.121 y la corrida tardó horas, con 500 ms entre empresas. Con unas
2.000-3.000 `ACTIVE` más, crece en proporción.

**Pregunta para Elias:** ¿recorrido entero o solo las empresas sin vacantes cargadas?
Ojo con el plan de normalización que corre en paralelo: si su proceso ya recorre
`vacancy`, conviene que la carga termine antes, o volver a correrlo después.

## Al terminar

- `docs/CONTEXTO.md`: el cliente de Wayback, el endpoint, los números finales de
  `company` y de `vacancy`, y el dominio EU como punto abierto.
- Borrar este archivo. `docs/MEDICION-SLUGS.md` se queda, igual que
  `MEDICION-VACANTES.md`.
