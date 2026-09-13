# Medición — cobertura del descubrimiento de Greenhouse (2026-09-13)

Prod tenía **4.046 empresas de Greenhouse**, todas salidas de un único índice de
CommonCrawl (`CC-MAIN-2026-34`). Afuera se habla de ~11.000 clientes activos de
Greenhouse. Se midió **por qué faltaban slugs**, con tres hipótesis:

1. **H1** — los que faltan están en índices **históricos** de CommonCrawl, no en el último.
2. **H2** — los que faltan **no están en CommonCrawl**; otro índice (Wayback Machine) los tiene.
3. **H3** — los URLs de **iframe embebido** (`/embed/job_board?for=X`) no se extraen bien.

**Resultado: H1 y H2 se confirman, H3 no.**

Ojo con la escala del 11.000: muchos clientes de Greenhouse tienen el careers en su
propio dominio (`stripe.com/jobs?gh_jid=...`) y ningún índice de boards los muestra.
Lo medido acá es lo que se puede alcanzar por esta vía.

## Cómo se midió

Todo fuera de la app, con scripts en Python sobre las capturas bajadas de los dos
índices, aplicando **exactamente las reglas de `GreenhouseBoardUrl.slugFrom`**: solo
`https`, los dos dominios (`boards.` y `job-boards.greenhouse.io`), primer segmento del
path o `for=` si es `embed`, validación `[A-Za-z0-9_-]+`. Los slugs de prod se bajaron
con un `select slug from company where ats='GREENHOUSE'` antes de escribir nada.

**Control de sanidad:** los números de agosto y julio reprodujeron exactos los ya
medidos en `CONTEXTO.md` (4.046, 4.348, 1.269 nuevas, 5.315 en la unión). Y al correr
después los POST reales de cuatro índices en prod, la tabla quedó en **6.988**, que es
exactamente la unión calculada offline para esos cinco índices.

## H1 — índices históricos de CommonCrawl: se confirma

Muestra de 16 índices espaciados, de agosto 2026 a diciembre 2023 (hay 127 en
`collinfo.json`). "No en prod" es contra las 4.046 originales.

| índice | capturas | slugs | no en prod | unión acumulada | unión no en prod |
|---|---:|---:|---:|---:|---:|
| CC-MAIN-2026-34 | 76.765 | 4.046 | 0 | 4.046 | 0 |
| CC-MAIN-2026-30 | 77.083 | 4.348 | 1.269 | 5.315 | 1.269 |
| CC-MAIN-2026-25 | 66.274 | 3.774 | 1.155 | 5.952 | 1.906 |
| CC-MAIN-2026-21 | 72.048 | 3.953 | 1.273 | 6.485 | 2.439 |
| CC-MAIN-2026-17 | 68.078 | 4.008 | 1.385 | 6.988 | 2.942 |
| CC-MAIN-2026-12 | 68.939 | 3.856 | 1.307 | 7.366 | 3.320 |
| CC-MAIN-2026-08 | 70.533 | 3.944 | 1.378 | 7.711 | 3.665 |
| CC-MAIN-2026-04 | 84.496 | 4.245 | 1.586 | 8.075 | 4.029 |
| CC-MAIN-2025-51 | 76.551 | 3.947 | 1.416 | 8.328 | 4.282 |
| CC-MAIN-2025-38 | 70.111 | 4.010 | 1.524 | 8.614 | 4.568 |
| CC-MAIN-2025-26 | 74.830 | 4.009 | 1.647 | 8.853 | 4.807 |
| CC-MAIN-2025-13 | 65.902 | 3.915 | 1.712 | 9.118 | 5.072 |
| CC-MAIN-2024-51 | 67.308 | 3.810 | 1.798 | 9.446 | 5.400 |
| CC-MAIN-2024-33 | 41.258 | 2.978 | 1.345 | 9.646 | 5.600 |
| CC-MAIN-2024-18 | 27.113 | 2.542 | 1.190 | 9.843 | 5.797 |
| CC-MAIN-2023-50 | 30.168 | 2.502 | 1.278 | 10.086 | 6.040 |

- Cada índice trae ~4.000 slugs pero **solo ~3.000 se repiten con el siguiente**: el
  crawl no ve siempre las mismas empresas.
- La unión **no se aplanó**: los primeros índices sumaban ~500 nuevos cada uno, los
  de 2024 todavía ~200-300. Quedan 111 índices sin medir, y por la regla de no
  concluir por ausencia no se puede afirmar dónde se corta.
- Los 4.046 de prod aparecen todos en la muestra.
- Una curiosidad: la página del índice se cortó con HTTP 200 varias veces durante la
  bajada (1,4 MB y 4,3 MB en vez de ~8 MB). Es el agujero ya anotado en
  `CONTEXTO.md`; los scripts lo detectaban porque la última línea no parseaba como JSON.

## H2 — Wayback Machine: se confirma

Wayback CDX (`web.archive.org/cdx/search/cdx`) sobre los dos dominios, **todas** las
páginas: `boards.greenhouse.io/` = 286, `job-boards.greenhouse.io/` = 153.

| | |
|---|---:|
| capturas | 3.075.443 |
| slugs (reglas de la app) | **17.730** |
| no están en prod (4.046) | 13.881 |
| **no están en ninguno de los 16 índices de CommonCrawl** | **8.035** |
| slugs de prod que Wayback no trae | 197 |
| unión CommonCrawl (muestra) + Wayback | **18.121** |
| slugs que solo aparecen con `http://` (la regla https los tira) | 77 |

Wayback **casi contiene** a CommonCrawl (le faltan 197 de los 4.046) y tiene el doble.

**Cuántos siguen vivos.** Wayback junta años de capturas, así que se esperaba mucha
mortandad. Se sondearon **60 slugs al azar** de los que solo trae Wayback contra
`boards-api.greenhouse.io/v1/boards/<slug>/jobs`:

| resultado | cantidad |
|---|---:|
| `200` con vacantes (`ACTIVE`) | 14 (23%) |
| `200` sin vacantes (`EMPTY`) | 5 (8%) |
| `404` (`NOT_FOUND`) | 41 (68%) |

Proyectado a los 8.035: del orden de **~1.850 empresas con vacantes** que CommonCrawl no
tiene. Muestra chica, así que el número es un orden de magnitud y no una cifra. Para
comparar, de las 4.046 originales de CommonCrawl el 77% estaba `ACTIVE`.

**Las 6.040 nuevas de CommonCrawl no se sondearon**, así que su tasa de vida no se
conoce; se espera más alta que la de Wayback porque los crawls son más recientes.

## H3 — embebidos: no se confirma

Clasificando cada captura de `CC-MAIN-2026-34` por la regla que le aplica:

| tipo | capturas |
|---|---:|
| path normal | 76.287 |
| `embed` con `for=` | 249 |
| rechazadas | 229 |

- Las 249 embebidas vienen en **14 formas** distintas (`for` con `token`, `utm_*`,
  `error`, `validityToken`, `gh_src`, y en un caso `for` después de `token`): **todas
  dan slug**.
- **47 slugs de prod llegan solo por `embed`**: la regla funciona y aporta.
- En Wayback las embebidas pesan mucho más (211.952 capturas) y también se extraen.

Lo único que se pierde son **slugs con caracteres fuera de `[A-Za-z0-9_-]`**. En agosto
los rechazados distintos son 5: `robots.txt` y `nff.org` (basura), y tres que son
**empresas reales** según la API:

| slug | API |
|---|---|
| `1pyra)mid_health&care` | 200 con vacantes |
| `science&purpose` | 200, board vacío |
| `harrison&star` | 200, board vacío |

Tres de 4.046: no justifica tocar la regla. Lo demás que Wayback rechaza es texto
pegado a la URL (`crescolabs%3C/a%3E`), `%5C` al final o la ligadura `ﬁ` de un PDF
(`clearwaterbene%EF%AC%81ts`, que da 404).

## Hallazgo lateral: el dominio EU

Existe **`job-boards.eu.greenhouse.io`** (Greenhouse con residencia de datos en
Europa). No está entre los patrones, así que esas empresas no se descubren nunca.
Juntando la muestra de CommonCrawl y Wayback: **848 slugs**, de los cuales **93** ya
están en prod por el otro dominio. Su API de vacantes sería otra
(`boards-api.eu.greenhouse.io`), así que meterlo toca también el sondeo y la carga.
Elias decidió **dejarlo afuera por ahora**.

## La API de Wayback CDX, medida

Lo que hace falta saber para escribir un cliente, verificado contra el servicio real:

- `GET https://web.archive.org/cdx/search/cdx?url=<prefijo>&matchType=prefix&fl=original&page=N`
  devuelve **texto plano, una URL por línea**. Sin `fl` devuelve varias columnas
  separadas por espacio.
- `&showNumPages=true` devuelve **un entero pelado** (`286\n`). **Con `fl` presente
  contesta `-`** en vez del número: el conteo hay que pedirlo sin `fl`.
- El patrón acepta el `/` codificado (`boards.greenhouse.io%2F`), igual que CommonCrawl.
- Una página fuera de rango da **`400`**.
- Una página son ~6.000 líneas y baja en segundos. Las 450 páginas, con **2 s de
  pausa** entre requests, bajaron en unos 40 minutos **sin ningún 429 ni 5xx**.
- Trae capturas `http://` además de `https://` (a diferencia de CommonCrawl, donde las
  76.765 de agosto eran https).

## Lo que ya se escribió en prod

Durante la medición se corrieron en prod, con el endpoint existente, los POST de
`CC-MAIN-2026-30`, `-25`, `-21` y `-17`. Salidas del log:

| índice | slugs | empresas nuevas | duración |
|---|---:|---:|---|
| CC-MAIN-2026-30 | 4.348 | 1.269 | 52 s |
| CC-MAIN-2026-25 | 3.774 | 637 | 14 s |
| CC-MAIN-2026-21 | 3.953 | 533 | 15 s |
| CC-MAIN-2026-17 | 4.008 | 503 | 28 s |

`company` pasó de 4.046 a **6.988**: 3.121 `ACTIVE`, 217 `EMPTY`, 708 `NOT_FOUND` y
**2.942 sin sondear**.
