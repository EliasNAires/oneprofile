# Revisión del paso 01 — CommonCrawl en prod (2026-09-14 UTC)

Log crudo: `log-commoncrawl-prod.txt` (misma carpeta).

## Corrida previa (00:21–00:33 UTC, lanzada por otro)
0 índices leídos, 0 nuevas, 10 fallidos, todos por `504 Gateway Timeout` del nginx de
CommonCrawl (~10 s por intento, incluso en el conteo de páginas). Después, a mano, los
mismos pedidos (curl desde el contenedor y `java.net.http.HttpClient` desde local, varios
User-Agent) dieron 200: el conteo en <1 s y las páginas en 3–21 s. En pedidos repetidos, la
misma página llegó con tamaños distintos y HTTP 200 (p. ej. 5.488.640 B contra 8.192.000 B):
respuestas cortadas.

## Corrida del paso (01:27:43–01:38:01 UTC)
POST `/admin/discovery/greenhouse/commoncrawl` → 202. `company` 7463 → **8053**
(esperado 8.328–8.650). 6 leídos, 590 nuevas, 4 fallidos. 25 `Retrying`, 9 de ellos
`arrived cut`, todos recuperados en los índices leídos.

| Índice | Slugs | Nuevas |
|---|---|---|
| CC-MAIN-2026-34 | 4046 | 0 |
| CC-MAIN-2026-30 | 4348 | 0 |
| CC-MAIN-2026-21 | 3953 | 0 |
| CC-MAIN-2026-17 | 4008 | 0 |
| CC-MAIN-2026-12 | 3856 | 311 |
| CC-MAIN-2026-08 | 3944 | 279 |

| Índice fallido | Dónde (intento 4 de 4) | Error |
|---|---|---|
| CC-MAIN-2026-25 | página 3 de `job-boards.greenhouse.io/` | `502 Bad Gateway` (intentos 1 y 3: `arrived cut`) |
| CC-MAIN-2026-04 | página 1 de `job-boards.greenhouse.io/` | `HTTP/1.1 header parser received no bytes` |
| CC-MAIN-2025-51 | conteo de páginas de `boards.greenhouse.io/` | `header parser received no bytes` |
| CC-MAIN-2025-47 | conteo de páginas de `boards.greenhouse.io/` | `header parser received no bytes` |

Causa según el log: CommonCrawl se degradó durante la corrida (cortes, 502 y desde ~01:37
conexiones cerradas sin bytes). No se ve un problema en la forma del pedido, sin descartarlo.

## Reintento de los 4 índices
No se corrió: no existe endpoint para índices sueltos (`DiscoveryController` solo tiene
`/commoncrawl` y `/wayback`, sin parámetros). Queda como concern.
