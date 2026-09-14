# Contexto — tablero del repo

> Estado actual y fase siguiente. Lo actualiza el planificador al volcar un plan y el
> orquestador al parar o terminar (ver "Roles" en `docs/agents/METODOLOGIA.md`). Tope ~12 KB (`wc -c`). El detalle vive
> en los docs de tema y se lee solo si "Leer:" lo pide. **`docs/para-humanos/` no se lee.**

**Última actualización:** 2026-09-14

## Qué es

Backend de `oneprofile`: descubre qué empresas usan un ATS (hoy solo Greenhouse), sondea
cuáles siguen vivas, carga sus vacantes y normaliza los títulos. El objetivo final es una
lista de vacantes que matcheen con el perfil del usuario.

## Estado

- **Descubrimiento y sondeo** andan en prod. `company` tiene **18.051** filas (índices de
  CommonCrawl, con 4 de los 10 recientes sin leer, más Wayback), todas sondeadas el 2026-09-14: ACTIVE 6.883,
  NOT_FOUND 9.787, EMPTY 1.381.
- **Vacantes:** el recorrido del 2026-09-14 (12:40–14:31 UTC) dejó **216.868 vacantes sobre
  6.879 empresas** (89.376 nuevas, 127.491 actualizadas, 1.461 borradas, 0 fallidas).
- **Normalización del título** (limpieza, seniority, modalidad): corrida entera el 2026-09-13
  en 24 s (títulos distintos de 87.647 a 77.630); el 2026-09-14 se normalizaron las 89.376
  nuevas en 18 s y no quedan vacantes sin normalizar.
- **Plan de slugs terminado y borrado** (2026-09-14): CommonCrawl 6/10 índices (+590),
  Wayback +9.998, sondeo y carga. **+3.762 `ACTIVE` y +87.915 vacantes (+68%)**: 54% de las
  vacantes nuevas salió de índices viejos de CommonCrawl nunca sondeados, 43% de Wayback, 4%
  de los índices del plan. Wayback trae la mitad de las activas nuevas pero el 75% de sus
  slugs está muerto. Detalle en "Probado en prod" de `descubrimiento`; crudos, revisión y uso
  de contexto en `mediciones/slugs-13-09-2026/`.
- **La dieta de contexto terminó** (2026-09-13): docs de agentes en `docs/agents/`, este
  tablero y cuatro temas. Falta la prueba real: `/context` después de la lectura inicial de
  la sesión siguiente debería rondar ~35k en vez de ~70k.

## Qué sigue

**Fase siguiente: probar la metodología nueva.** Roles planificador / orquestador /
ejecutor / verificador / consulta, construidos el 2026-09-13 (sección "Roles" de
`METODOLOGIA.md`, `.claude/skills/` y `.claude/agents/`), sin probar.

**Leer:** nada; el guion se sigue en sesiones nuevas.

1. Las skills `planificar`, `ejecutar`, `consultar` y los agentes `ejecutor`, `verificador`
   aparecen en `/skills` y `/agents`.
2. Sesión nueva sin comando → el agente pregunta el modo antes de leer nada.
3. `/consultar ¿qué hace GreenhouseDiscoveryService?` → contesta sin leer planes ni
   metodología; `/context` bajo ~35k.
4. `/planificar` con un tema chico (p. ej. categorización tech/no tech) → conversa, entra en
   modo plan, y el plan aprobado se vuelca a `docs/agents/planes/<tema>/` con puertas.
5. ~~`/ejecutar <tema>`~~ → **probado con `slugs` el 2026-09-14** (solo verificadores, sin
   ejecutor): anduvo, con los problemas de "Flujo de trabajo" abajo. Falta probarlo con un
   paso que lleve ejecutor.

Después, sin priorizar ni planificar: **categorizar las vacantes en tech-adyacentes y no**
(por tokens del título, no por diccionario: casi la mitad de los títulos no se repite),
perfil de usuario, primer matching simple, endpoint que devuelva vacantes, y un cron que
encadene sondeo → vacantes → normalización.

## Qué NO existe todavía

- Categorización tech / no tech; normalización de departamento y ubicación.
- Perfil de usuario y matching.
- Ningún endpoint devuelve datos: todos disparan procesos.
- Ningún ATS además de Greenhouse.
- Ningún cron: todo se dispara a mano (`last_probed_at` espera a que exista).
- Tag por SHA (solo `latest`, no se puede revertir un deploy), despliegue automático,
  HTTPS, dominio, reverse proxy.

## Puntos abiertos

El detalle de cada uno está en la sección "Abierto" de su tema.

**Pendientes de diseñar (Elias, 2026-09-14):**

- **Reintento de índices abortados** en el descubrimiento de CommonCrawl: 4 quedaron sin leer
  (`-2026-25`, `-2026-04`, `-2025-51`, `-2025-47`) y no hay endpoint para índices sueltos.
  → `descubrimiento`
- **Borrar slugs truncados después del sondeo**: todo truncado `NOT_FOUND` con su versión no
  truncada `ACTIVE`, para evitar falsos positivos y achicar los sondeos. → `descubrimiento`
- **Logs de avance** en descubrimiento, sondeo y carga (~2 h cada uno, hoy solo `started` y
  `finished`): confirmar que va bien o fallar temprano. → `descubrimiento`, `vacantes`

**Flujo de trabajo** (visto en `/ejecutar slugs`; uso de contexto en
`mediciones/slugs-13-09-2026/uso-de-contexto.md`):

- **Los monitores no despiertan al subagente**: vieron cada `finished` a tiempo, pero el aviso
  le llegó recién cuando el orquestador le escribió. Se perdieron 6 h 20 min antes de la carga
  y 35 min antes de la normalización; Wayback, 25 min. Dos monitores además tenían mal el
  filtro (cortaban con el WARN de un reintento, o el `tail -N` dejaba afuera el `finished`).
- **Permisos**: `gh` sin sesión; el clasificador bloqueó `psql` por ssh y consultas de solo
  lectura varias veces. Cada bloqueo paró la corrida hasta que Elias dio el permiso.
- **Reportes viejos**: un verificador contestó el estado de hacía 1 h 15 min sin darse cuenta.
- **El orquestador se desbordó**: 182k al cerrar (~150k sin el cierre), más que cualquier
  verificador (38–81k). Sobre todo por ~40 avisos de avance de 5 en 5 min, cada uno
  reenviado, y por consultar prod él mismo cuando los monitores no avisaban. Elias busca un
  **techo blando de ~100k por sesión** (regla en `METODOLOGIA.md`, "Higiene de contexto").
- **Estimaciones del plan desfasadas**: rango de `company` y duraciones (Wayback 40 → 98 min)
  sin margen para fuentes externas degradadas; un endpoint que se creía existente no estaba.
- **Por evaluar: Sonnet en ejecutor y verificador** para ahorrar tokens: el piso de un
  subagente ronda ~38k por las definiciones de herramientas, aunque la tarea sea trivial.
  Opinión del orquestador: probarlo primero en verificadores (siguen un guion con criterio
  claro) y dejar Opus en ejecutores que diseñan código.
- **Tamaño de pasos: bien** (Elias y orquestador): cada uno terminó con un resultado claro
  para mirar en la puerta. Opinión del orquestador: lo caro fue la espera, no el tamaño; un
  paso sin ejecutor con corridas de horas conviene partirlo en "lanzar" y "verificar al
  terminar", con una espera que despierte al agente, en vez de un verificador vivo horas.

- **Imagen de prod:** Elias corre sesiones en paralelo; antes de concluir de una prueba en
  prod, confirmar qué commit se publicó. → `prod-y-despliegue`
- Endpoints de administración sin protección (importa al publicar uno que devuelva datos).
  → `prod-y-despliegue`
- Respuesta truncada del índice justo en un salto de línea se acepta en silencio (Elias lo
  dejó abierto). → `descubrimiento`
- Wayback con una línea cortada guarda un slug falso (el sondeo lo marca `NOT_FOUND`; lo
  cubriría el borrado de truncados). → `descubrimiento`
- 4 `ACTIVE` sin vacantes y `fetched` una fila menos que `vacancy` (2026-09-14). → `vacantes`
- Dominio EU de Greenhouse no se descubre (848 slugs), afuera por ahora. → `descubrimiento`
- Reglas de slug descartan algunas empresas reales (`&`, `)`, 77 solo `http://`), a
  sabiendas. → `descubrimiento`
- El sondeo se corre entero cada vez; nada fuerza el orden sondeo → vacantes →
  normalización. → `descubrimiento`
- El sync de vacantes no normaliza (anotado a pedido de Elias). → `vacantes`
- Sin tope de tamaño en la respuesta de un board. → `vacantes`
- `MEDICION-VACANTES.md` (85.050 títulos distintos) no coincide con la base (87.647). →
  `vacantes`
- La base de dev arranca vacía; hay que insertar la empresa a mano. → `vacantes`
- Modalidad: 37 falsos positivos y 31 falsos negativos (`remotely`), sin guarda por decisión
  de Elias; `FULLY_REMOTE` conservador a propósito. → `normalizacion`
- `senior` en cuidado domiciliario (37), sin guarda por decisión de Elias. → `normalizacion`
- ~650 filas que no son vacantes (talent pool, general application). → `normalizacion`
- Contrato/jornada y ubicación estructurada merecen paso propio. → `normalizacion`
- Sin regla: nivel `i`, `associate`, `virtual`; conector suelto en rangos de nivel. →
  `normalizacion`
- `pom.xml` con metadata vacía; `docker-rootless-extras` desparejo con `docker` en la
  máquina de Elias. → `prod-y-despliegue`

## Docs de agentes

- `docs/agents/METODOLOGIA.md` — cómo trabajamos.
- `docs/agents/tema/prod-y-despliegue.md` — trampas de Boot 4, entorno rootless,
  configuración, migraciones, prod, pipeline, mudanza de datos.
- `docs/agents/tema/descubrimiento.md` — CommonCrawl, Wayback, `GreenhouseBoardUrl`,
  reintentos, sondeo, `Company`.
- `docs/agents/tema/vacantes.md` — API de `/jobs`, vacantes viejas, sync y recorrido masivo.
- `docs/agents/tema/normalizacion.md` — extractores del título, tabla y corrida en prod.
- `docs/agents/MEDICION-SLUGS.md`, `docs/agents/MEDICION-VACANTES.md` — números medidos.
- `mediciones/` — salidas crudas; no se lee salvo que un plan nombre un archivo.

## `docs/para-humanos/`

Para personas, **no se lee** salvo que Elias pida escribirla o actualizarla: README, un
archivo por proceso, `despliegue.md` y ocho diagramas PlantUML (`.puml` + `.svg`),
actualizada el 2026-09-13. PlantUML no está instalado como comando; se usa el jar:
`java -jar ~/.vscode/extensions/jebbs.plantuml-2.18.1/plantuml.jar -tsvg <archivo>.puml`.
Para mirar un diagrama se genera un PNG fuera del repo
(`rsvg-convert -z 2 x.svg -o <scratchpad>/x.png`) y se abre.
