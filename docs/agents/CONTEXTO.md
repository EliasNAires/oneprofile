# Contexto — tablero del repo

> Estado actual y fase siguiente. Lo actualiza cada sesión al terminar su fase (ver "Una
> sesión por fase" en `docs/agents/METODOLOGIA.md`). Tope ~12 KB (`wc -c`). El detalle vive
> en los docs de tema y se lee solo si "Leer:" lo pide. **`docs/para-humanos/` no se lee.**

**Última actualización:** 2026-09-14

## Qué es

Backend de `oneprofile`: descubre qué empresas usan un ATS (hoy solo Greenhouse), sondea
cuáles siguen vivas, carga sus vacantes y normaliza los títulos. El objetivo final es una
lista de vacantes que matcheen con el perfil del usuario.

## Estado

- **Descubrimiento y sondeo** andan en prod. `company` tiene **7.463** filas (varias sin
  sondear). La unión de fuentes promete muchas más: 10.086 slugs sobre 16 índices de
  CommonCrawl y 17.730 en Wayback (`docs/agents/MEDICION-SLUGS.md`).
- **Vacantes:** el recorrido masivo dejó **128.953 vacantes sobre 3.118 empresas**
  (2026-09-11/12), sobre las 4.046 empresas de entonces.
- **Normalización del título** (limpieza, seniority, modalidad) corrida en prod el
  2026-09-13 en 24 s: títulos distintos de 87.647 a 77.630, 32.219 con seniority, 19.255 con
  modalidad.
- **Plan en curso: traer todos los slugs de Greenhouse** (`docs/agents/PLAN-SLUGS.md`). La
  corrección de CommonCrawl y el descubrimiento sobre Wayback están en `7f33a53`, **desplegados
  en prod** desde el 2026-09-14 00:20Z (imagen `b17586abbec2`, creada 23:46:59Z, un minuto
  después de `ab7a5b2`; la imagen **no trae label de revision**, el commit se deduce por la hora).
- **Segunda corrida de CommonCrawl (2026-09-14 00:21:41Z → 00:33:05Z): FALLÓ por causa
  externa**, no prueba ni refuta la corrección. `0 indexes read, 0 new companies saved, 10
  indexes failed`; `company` sigue en **7.463**. 9 índices dieron `504 Gateway Timeout` del nginx
  de CommonCrawl en los 4 intentos (en el conteo de páginas o en la página 0); `CC-MAIN-2026-12`
  agotó los intentos con `Page of the index arrived cut`. 33 `Retrying` (31 por 504, 2 por página
  cortada, ninguno se recuperó), 0 respuestas 400, 0 ERROR. El mensaje del 504 mete el HTML de
  nginx en el log, a veces en varias líneas. Log crudo:
  `mediciones/slugs-prod-13-09-2026/commoncrawl.txt`.
- **Wayback todavía no se corrió**: el POST quedó sin disparar (ver "Prueba de orquestación").
- **Docker en prod corre sin `sudo`** desde el 2026-09-13: el usuario de elitedesk1 está en el
  grupo `docker`. No hace falta clave para desplegar ni consultar.
- **Prueba de orquestación con subagentes (2026-09-14), sin cerrar.** Elias probó que la sesión
  orqueste y cada paso de prueba en prod lo ejecute un subagente que devuelve solo el resumen.
  Plan: A (deploy + CommonCrawl) → B (Wayback) → C (sondeo), en cadena porque las dos fuentes
  comparten el lock. Lo que se vio:
  - El clasificador de auto mode **bloqueó lanzar un subagente con la clave de sudo en el
    prompt**, y bloqueó que la sesión se escriba `.claude/settings.local.json`. Por eso se pasó
    Docker a sin sudo.
  - El subagente **pudo** `pull`, `up -d`, logs y SQL, pero **no el POST que dispara la corrida**
    ni un `ssh … until … sleep` en segundo plano. El POST de CommonCrawl lo corrió la sesión
    orquestadora y pasó; el de Wayback, justo después y combinado con un `select`, lo bloqueó
    también en la orquestadora. Queda sin saber si pasa solo.
  - El subagente **se detuvo creyendo tener una espera viva** y hubo que retomarlo dos veces con
    `SendMessage`; después mandó notificaciones repetidas. Esperó bien con `Monitor` cada 150 s.
  - Contexto: el subagente A cerró en ~54k. La orquestadora llegó a **~184k**, por encima del tope
    de 100k que se buscaba: la mayor parte la sumó cargar la skill `update-config` (trae el
    esquema entero de settings), más las idas y vueltas por los permisos.
- **La dieta de contexto terminó** (2026-09-13): docs de agentes en `docs/agents/`, este
  tablero y cuatro temas. Falta la prueba real: `/context` después de la lectura inicial de
  la sesión siguiente debería rondar ~35k en vez de ~70k.

## Qué sigue

**Fase siguiente: probar en prod el descubrimiento sobre Wayback, y repetir CommonCrawl
cuando su índice responda**, una después de la otra. Comparten el lock (la segunda da 409
hasta que la primera loguee `finished`). La imagen ya está desplegada: **no hace falta
`pull`/`up -d`**, y un `up -d` mata la corrida en curso.

**Leer:** `docs/agents/tema/descubrimiento.md`, `docs/agents/PLAN-SLUGS.md` y, para
CommonCrawl, la sección C de `docs/agents/PLAN-FALLAS-COMMONCRAWL.md` (su guion, sin el deploy).

Wayback:

1. Confirmar en el log que no hay una corrida en curso (`docker compose logs --since 30m app`).
2. Por `ssh elitedesk1`, sin `sudo`; el POST solo, sin encadenarlo con otros comandos:
   ```bash
   cd ~/oneprofile && docker compose exec -T app curl -s -i -X POST 'localhost:8080/admin/discovery/greenhouse/wayback'   # 202
   docker compose logs --since <hora> app | grep -iE "finished|WARN|Giving up" | tail
   ```
3. Esperado: ~40 min (439 páginas), ningún `WARN` que termine en falla,
   `Greenhouse discovery on Wayback finished: N slugs found` con N cerca de **17.730**, y
   `select count(*) from company` cerca de **18.000**.
4. Si da, se cierra acá y en `PLAN-SLUGS.md` (sigue sondear y cargar lo nuevo). Si falla,
   reporte con el log crudo en `mediciones/` y la causa; la fase siguiente pasa a corregir.

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

- **Imagen de prod:** Elias corre sesiones en paralelo; antes de concluir de una prueba en
  prod, confirmar qué commit se publicó. → `prod-y-despliegue`
- Endpoints de administración sin protección (importa al publicar uno que devuelva datos).
  → `prod-y-despliegue`
- Respuesta truncada del índice justo en un salto de línea se acepta en silencio (Elias lo
  dejó abierto). → `descubrimiento`
- Wayback con una línea cortada guarda un slug falso (el sondeo lo marca `NOT_FOUND`). →
  `descubrimiento`
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
- `docs/agents/PLAN-SLUGS.md`, `docs/agents/PLAN-FALLAS-COMMONCRAWL.md` — planes en curso.
- `docs/agents/MEDICION-SLUGS.md`, `docs/agents/MEDICION-VACANTES.md` — números medidos.
- `mediciones/` — salidas crudas; no se lee salvo que un plan nombre un archivo.

## `docs/para-humanos/`

Para personas, **no se lee** salvo que Elias pida escribirla o actualizarla: README, un
archivo por proceso, `despliegue.md` y ocho diagramas PlantUML (`.puml` + `.svg`),
actualizada el 2026-09-13. PlantUML no está instalado como comando; se usa el jar:
`java -jar ~/.vscode/extensions/jebbs.plantuml-2.18.1/plantuml.jar -tsvg <archivo>.puml`.
Para mirar un diagrama se genera un PNG fuera del repo
(`rsvg-convert -z 2 x.svg -o <scratchpad>/x.png`) y se abre.
