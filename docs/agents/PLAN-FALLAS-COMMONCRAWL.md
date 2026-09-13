# Plan — por qué falló el descubrimiento sobre 10 índices de CommonCrawl, y cómo se corrige

> Plan autocontenido: se retoma leyendo este archivo, `docs/agents/METODOLOGIA.md` y
> `docs/agents/CONTEXTO.md`. Es un desvío dentro del paso 2 de `docs/agents/PLAN-SLUGS.md` (los 10
> índices recientes): ese paso no se da por probado hasta que esto esté corregido y la
> corrida en prod salga limpia. El log crudo de la corrida está archivado en
> `mediciones/fallas-commoncrawl-13-09-2026/log-descubrimiento-prod.txt`. Se borra al
> terminar y lo que quede pasa a `CONTEXTO.md`.

## Estado

- **Corrección CONSTRUIDA (2026-09-13):** A y B, tal como se describen abajo. `./mvnw test`
  da **126 en verde** (122 + 4 nuevos). **Falta la prueba en prod** (sección C), que
  necesita que Elias commitee y pushee.
- Las preguntas del final ya tienen respuesta de Elias; están anotadas ahí mismo.

## Qué pasó

El 2026-09-13 se desplegó en prod la imagen con el paso 2 (y el 3) y se corrió
`POST /admin/discovery/greenhouse/commoncrawl`. Arrancó a las 18:00:31 UTC y terminó a
las 18:08:31 UTC:

```
Greenhouse discovery on CommonCrawl finished: 3 indexes read, 475 new companies saved,
7 indexes failed [CC-MAIN-2026-34, CC-MAIN-2026-25, CC-MAIN-2026-21, CC-MAIN-2026-12,
CC-MAIN-2026-08, CC-MAIN-2026-04, CC-MAIN-2025-47]
```

| Índice | Resultado | Esperado |
|---|---|---|
| `CC-MAIN-2026-34` | falló: `Unexpected end-of-input: was expecting closing quote for a string value` | 0 nuevas (ya corrido) |
| `CC-MAIN-2026-30` | 4.348 slugs, **0 nuevas** | 0 ✔ |
| `CC-MAIN-2026-25` | falló: `Unexpected end-of-input…` | 0 (ya corrido) |
| `CC-MAIN-2026-21` | falló: `400 Bad Request` | 0 (ya corrido) |
| `CC-MAIN-2026-17` | 4.008 slugs, **0 nuevas** | 0 ✔ |
| `CC-MAIN-2026-12` | falló: `Unexpected end-of-input…` | ~378 |
| `CC-MAIN-2026-08` | falló: `Unexpected end-of-input…` | ~345 |
| `CC-MAIN-2026-04` | falló: `400 Bad Request` | ~364 |
| `CC-MAIN-2025-51` | 3.947 slugs, **475 nuevas** | ~253–286 |
| `CC-MAIN-2025-47` | falló: `Unexpected end-of-input in property name` | ~286 |

En el medio hubo varios `502 Bad Gateway` y un `504 Gateway Timeout` sueltos, que
`HttpRetry` reintentó y recuperó.

`company` pasó de **6.988** a **7.463** filas (3.417 sin sondear); se esperaba ~**8.614**.
Las 475 de `2025-51` superan lo esperado porque los índices intermedios que fallaron no
le "ganaron" antes esas empresas: no es un error.

Lo que **sí quedó probado**: la idempotencia (los índices ya cargados dan 0 nuevas), que
un índice que falla no corta la corrida, y la lectura de los tres que salieron bien.

## Por qué falló

### 1. Página cortada a mitad de una línea (5 de los 7)

`CommonCrawlIndexClient.readPage` lee la página en streaming y parsea cada línea con
`this.json.readTree(line)`. Cuando el índice, saturado, **cierra la respuesta a mitad del
cuerpo**, la última línea llega incompleta y Jackson tira
`tools.jackson.core.exc.UnexpectedEndOfInputException`.

Esa excepción **no es una `RestClientException`**: en Jackson 3 `JacksonException` extiende
`RuntimeException` directamente (verificado con `javap` sobre `jackson-core-3.1.5`). Así que:

- `HttpRetry.call` solo atrapa `RestClientException` → **no la reintenta**. En el log, antes
  del `WARN` de `CC-MAIN-2026-34` no hay ningún `Retrying` para esa página.
- Sube hasta `discoverOnRecentCommonCrawl`, que la atrapa como `RuntimeException` y **da por
  perdido el índice entero**, con todo lo que ya había leído de él.

**No hay forma de detectarlo a nivel HTTP**: el índice contesta `200` con
`Transfer-Encoding: chunked` y **sin `Content-Length`** (verificado con `curl -D -`), así
que el cliente HTTP no tiene contra qué comparar el largo recibido.

**Es transitorio, del lado de CommonCrawl**: a las 18:15 UTC se bajaron a mano las 6
páginas de `CC-MAIN-2026-21` (1 de `boards.greenhouse.io/`, 5 de
`job-boards.greenhouse.io/`) y todas llegaron completas: HTTP 200, terminan en `\n`, cero
líneas sin parsear. Las páginas llenas traen **exactamente 15.000 líneas**; la última,
2.696.

Este agujero ya estaba anotado en `CONTEXTO.md` ("Una respuesta truncada del índice se
aceptaría en silencio"), pero para el caso en que el corte cae justo en un salto de línea.
Lo que pasó en prod es el otro caso, el corte a mitad de línea, que no es silencioso pero
tira el índice.

### 2. `400 Bad Request` (2 de los 7) — causa sin confirmar

**No se sabe qué request dio el 400**, y el código hoy no deja averiguarlo:

- `HttpRetry.errorFor(status, statusText)` crea la excepción con **cuerpo vacío**, y el
  mensaje queda en `400 Bad Request`. El cuerpo que CommonCrawl manda sí dice el motivo.
- El `WARN` del servicio loguea el índice pero no el patrón ni la página.

Lo que se sabe:

- Los conteos de hoy son normales: `showNumPages` para `-21` da 1 y 5 páginas; para `-04`,
  2 y 5. La respuesta es un objeto `{"pages": 5, "pageSize": 5, "blocks": 21}`.
- **Una página fuera de rango da 400**, con cuerpo
  `{"message": "Page 5 invalid: First Page is 0, Last Page is 4"}` (verificado).
- Con el endpoint viejo, `-21` se leyó bien el mismo día.

Hipótesis, en orden: (a) bajo carga, `showNumPages` contestó un número de páginas mayor al
real y se pidió una página inexistente; (b) el índice saturado contesta 400 por algo que no
es la request. Ninguna está verificada. **No reintentar un 4xx es la regla acordada**, y
cambiarla para CommonCrawl sin saber la causa sería adivinar.

## Solución propuesta

Un paso, en el ciclo de siempre (plan aprobado por Elias → código → tests → prueba en prod).

### A. Reintentar la página cortada

En `CommonCrawlIndexClient.streamUrls`, si `readTree(line)` tira `JacksonException`,
convertirla en una `RestClientException` que `HttpRetry` considere transitoria —
`ResourceAccessException` (no es `HttpClientErrorException`, así que `isTransient` da
`true`)— con un mensaje que diga que la página llegó cortada. Así la página se vuelve a
pedir con el backoff de siempre (4 intentos: 2 s, 4 s, 8 s) y, si sigue fallando, el índice
cae en `failedIndexes` como hoy.

- Repetir la página reentrega las primeras URLs; el servicio las junta en un `Set`, así que
  no cuesta nada (ya está dicho en el javadoc de `readPage`).
- Una línea mal formada en el medio de una página sana también se reintentaría, y fallaría
  4 veces. Es aceptable: CommonCrawl no manda líneas rotas en páginas completas (0 en las
  ~72.000 bajadas a mano).

Tests en `CommonCrawlIndexClientTest`, con `MockRestServiceServer`:

- Una página cuyo cuerpo termina a mitad de línea y, al segundo intento, llega entera →
  devuelve las URLs y `server.verify()` confirma los dos pedidos.
- Una página que llega cortada las 4 veces → tira `ResourceAccessException`.

### B. Que un 4xx deje ver por qué

- `HttpRetry.errorFor` recibe el cuerpo de la respuesta y arma el mensaje a mano,
  `"400 Bad Request: <cuerpo>"`, con la variante `create(message, …)`. **Ojo:** la variante
  sin mensaje **no** incluye el cuerpo en `getMessage()`, aunque lo guarde. Se verificó con
  `javap` sobre spring-web 7.0.9: el mensaje queda en `"<code> <statusText>"`. En
  `CommonCrawlIndexClient.readPage` se lee el cuerpo **solo cuando el status es de error**,
  antes de tirar la excepción (el camino feliz sigue en streaming). `errorFor` también lo
  usan `WaybackCdxClient` (línea 90), `GreenhouseBoardClient` (líneas 81 y 97) y
  `HttpRetryTest` (5 llamadas): todos se adaptan a la firma nueva.
- El mensaje de la excepción dice qué se pidió (índice, patrón y página), para que el `WARN`
  de `GreenhouseDiscoveryService` lo muestre sin tocar el servicio. `HttpRetry.call` ya recibe
  `what` ("page 3 of job-boards.greenhouse.io/"). **Se hizo con un log, sin envolver la
  excepción:** antes de rendirse (error no transitorio o último intento), `HttpRetry.call`
  escribe `"<servicio> failed on <what> (attempt n of m): <mensaje>. Giving up"`. El tipo de
  la excepción no cambia.

Test: un `400` con cuerpo `{"message":"Page 5 invalid…"}` → la excepción es
`HttpClientErrorException`, no se reintenta, y su mensaje contiene `Page 5 invalid`. La
página pedida sale en la línea `Giving up` del log, que no se testea y se mira en la prueba
en prod.

### C. Prueba en prod

1. Elias commitea y pushea; el pipeline publica la imagen.
2. En el servidor (`ssh elitedesk1`, Docker con `sudo`):
   `cd ~/oneprofile && docker compose pull app && docker compose up -d`.
3. `docker compose exec app curl -i -X POST 'localhost:8080/admin/discovery/greenhouse/commoncrawl'` → `202`.
4. En `docker compose logs -f app`: los `Retrying` con `Page of the index arrived cut`
   aparecen y se recuperan. Si vuelve el 400, la línea `CommonCrawl index failed on page N of
   <patrón> … Giving up` dice qué página se pidió y qué contestó el índice.
5. Resultado esperado: 10 índices leídos, 0 fallidos. Los 6 ya cargados (`-34`, `-30`, `-25`,
   `-21`, `-17`, `2025-51`) dan 0 nuevas; los otros 4 suman lo que falta para llegar a
   `select count(*) from company` ≈ **8.614** (hoy 7.463).
6. **No desplegar el paso 3 (Wayback) ni reiniciar la app mientras corre** (`PLAN-SLUGS.md`).

## Preguntas para Elias

Respondidas el 2026-09-13: **las cuatro, como se proponía.**

1. **¿Reintentar el 400 en CommonCrawl?** La propuesta es no, hasta ver el cuerpo en la
   próxima corrida. Si resulta ser (a), el arreglo sería otro (volver a pedir el conteo), no
   reintentar a ciegas. **Respuesta: no.**
2. **¿Cerrar también el corte silencioso?** El que cae justo en un salto de línea sigue sin
   detectarse. Se podría verificar que toda página que no sea la última traiga
   `pageSize × 3.000` líneas (hoy 15.000), pero es una regla deducida de una sola medición
   del formato ZipNum del índice, no documentada por CommonCrawl. La propuesta es dejarlo
   afuera y seguir anotado como punto abierto. **Respuesta: queda como punto abierto.**
3. **¿Loguear cada índice al terminarlo, en vez de todos al final?** Hoy
   `DiscoveryController` escribe las líneas por índice recién cuando termina la corrida
   entera (8 minutos sin saber cómo va). No causó la falla; queda a criterio de Elias.
   **Respuesta: fuera de este paso.**
4. **¿Y Wayback?** `WaybackCdxClient` lee texto plano, así que una línea cortada no falla:
   llega como un slug recortado (`mercadolibre` → `mercadol`) y se guarda como una empresa
   falsa, que el sondeo después marca `NOT_FOUND`. **Respuesta: se anota como punto
   abierto en `CONTEXTO.md` y no se arregla en este paso.**
