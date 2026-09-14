# Plan — descubrimiento-v2

## Objetivo
Que el descubrimiento de CommonCrawl reintente los índices fallidos en hasta 3 pasadas, que
se descubra el dominio EU de Greenhouse, que los slugs truncados se borren al final del
sondeo y no vuelvan (blacklist), y que descubrimiento, sondeo y carga logueen su avance con
una línea final inconfundible. Antes, ajustar la orquestación con lo aprendido en `slugs`.
Solo implementación: tests y dev; nada en prod.

## Decisiones de Elias
- Pasadas de CommonCrawl: 3. Pasada 1 con los 10 índices; los que fallan (agotados sus
  reintentos, que no se tocan) van a la pasada 2; los de la 2, a la 3.
- Truncados: se borran de `company` y van a una tabla blacklist que el descubrimiento
  saltea. La limpieza corre sola al final del sondeo.
- Fail fast = verlo en el log (avance y fallas acumuladas); la app no aborta sola.
- Dominio EU sí, en descubrimiento, sondeo y carga; otras regiones no existen (investigado).
  Sondeo y carga ya los traen: la API de siempre contesta los boards EU (verificado), así que
  solo cambia el descubrimiento.
- Ejecutor y verificador en Sonnet.
- Verificación del plan: tests + dev local; sin corridas en prod.

## Pasos
- [ ] `paso-01-orquestacion.md` — Sonnet en subagentes, esperas con Monitor del orquestador, hora en reportes, margen en estimaciones, allowlist de permisos. Puertas: antes: ninguna. Antes de verificar: ninguna. Después: Elias revisa los cambios de metodología; las definiciones nuevas (Sonnet) rigen recién en una sesión nueva: Elias decide si reinicia `/ejecutar descubrimiento-v2`.
- [ ] `paso-02-logs-sondeo-y-carga.md` — util de avance; sondeo y carga loguean avance y cierran con `finished`/`aborted`. Puertas: ninguna.
- [ ] `paso-03-pasadas-commoncrawl.md` — hasta 3 pasadas sobre los índices fallidos. Puertas: ninguna.
- [ ] `paso-04-logs-descubrimiento.md` — avance por índice/pasada y por páginas de Wayback; cierre `finished`/`aborted`. Puertas: ninguna.
- [ ] `paso-05-dominio-eu.md` — patrones `job-boards.eu.` y `boards.eu.`; sondeo y carga traen los EU por la misma API (verificado, sin cambio de código). Puertas: ninguna.
- [ ] `paso-06-blacklist.md` — tabla `blacklisted_slug`; el descubrimiento la saltea. Puertas: ninguna.
- [ ] `paso-07-limpieza-truncados.md` — al final del sondeo, truncados `NOT_FOUND` a la blacklist. Puertas: después: commit y push de Elias.

## Estado y desvíos
<lo llena el orquestador después de cada paso>

## Al terminar
- `tema/descubrimiento.md`: sacar de "Abierto" reintento de índices, truncados, logs y
  dominio EU; agregar lo decidido (pasadas, blacklist, EU por la misma API, formato de logs).
  Corregir la afirmación de que EU necesita `boards-api.eu`.
- `tema/vacantes.md`: el punto de logs de la carga.
- `CONTEXTO.md`: sacar esos pendientes y los de "Flujo de trabajo" resueltos en el paso 01;
  fase siguiente: corrida en prod (descubrimiento → sondeo → carga) para medir, a planificar.
- No hay mediciones que archivar.
