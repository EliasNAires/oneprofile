# Contexto — tablero del repo

> Estado actual y fase siguiente. Lo actualiza el planificador al volcar un plan y el
> orquestador al parar o terminar (ver "Roles" en `docs/agents/METODOLOGIA.md`). Tope ~12 KB (`wc -c`). El detalle vive
> en los docs de tema y se lee solo si "Leer:" lo pide. **`docs/para-humanos/` no se lee.**

**Última actualización:** 2026-09-13

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
- **Plan en curso: traer todos los slugs de Greenhouse** (`docs/agents/PLAN-SLUGS.md`). El
  descubrimiento sobre los 10 índices recientes **falló en prod** (3 leídos, 7 fallidos); la
  corrección está construida. El descubrimiento sobre **Wayback** está construido. Los dos
  están en el commit `7f33a53`, ya en `origin/main`, y **ninguno se probó en prod**.
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
5. `/ejecutar <tema>` → ejecutor y verificador por paso, resumen después de cada uno, las
   dudas de los subagentes llegan como preguntas, para en la primera puerta; anotar el
   `/context` del orquestador al final.

**Pendiente después: probar en prod el descubrimiento sobre Wayback y la corrección de
CommonCrawl**, una después de la otra, en cualquier orden. Comparten el lock (la segunda da
409 hasta que la primera loguee `finished`) y un `up -d` mata la corrida en curso.

Para esa, **Leer:** `docs/agents/tema/descubrimiento.md`, `docs/agents/PLAN-SLUGS.md` y, para
CommonCrawl, la sección C de `docs/agents/PLAN-FALLAS-COMMONCRAWL.md` (su guion).

Wayback:

1. Confirmar que el pipeline de `7f33a53` (o posterior) terminó en verde y que no hay una
   corrida de CommonCrawl en curso.
2. Por `ssh elitedesk1`, con `sudo`:
   ```bash
   cd ~/oneprofile && sudo docker compose pull && sudo docker compose up -d
   sudo docker compose exec app curl -i -X POST 'localhost:8080/admin/discovery/greenhouse/wayback'   # 202
   sudo docker compose logs -f app
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
