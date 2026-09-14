# Paso 01 — mejoras de orquestación

## Qué queda funcionando
Las definiciones de roles recogen lo aprendido al ejecutar el plan `slugs`: subagentes en
Sonnet, esperas largas que despiertan a quien espera, reportes con hora, estimaciones con
margen y menos bloqueos de permisos. Paso sin código: **no lleva tests**.

## Contexto necesario
Problemas medidos en `/ejecutar slugs` (2026-09-13/14):
- **Los monitores no despiertan al subagente.** Un `Monitor` lanzado por un subagente vio el
  `finished` a tiempo, pero el aviso le llegó recién cuando el orquestador le escribió: se
  perdieron 6 h 20 min, 35 min y 25 min. La sesión principal (el orquestador) sí se despierta
  con las notificaciones de sus `Monitor`.
- **Filtros de monitor mal:** uno cortó con el WARN de un reintento (el texto del WARN contenía
  el del error final); otro usaba `tail -N` y el `finished` quedó afuera.
- **El orquestador llegó a 182k** (~150k sin el cierre), más que cualquier verificador
  (38–81k): ~40 avisos de avance reenviados de a uno, y consultas propias a prod cuando los
  monitores no avisaban. Techo blando buscado: ~100k por sesión.
- **Reporte viejo:** un verificador contestó el estado de hacía 1 h 15 min sin darse cuenta.
- **Permisos:** el clasificador bloqueó `psql` por ssh y consultas de solo lectura varias
  veces; cada bloqueo paró la corrida hasta que Elias dio permiso. No existe
  `.claude/settings.json` en el repo.
- **Estimaciones desfasadas:** Wayback estimado en 40 min tardó 98; un endpoint que el plan
  creía existente no estaba.
- **Piso de un subagente ~38k** aunque la tarea sea trivial. Elias decidió **Sonnet en
  ejecutor y verificador**.

Leer antes de tocar: `.claude/agents/ejecutor.md`, `.claude/agents/verificador.md`,
`.claude/skills/ejecutar/SKILL.md`, `.claude/skills/planificar/SKILL.md`, y de
`docs/agents/METODOLOGIA.md` las secciones "Roles", "Higiene de contexto durante la sesión"
y el encabezado (regla de correcciones y tope de ~12 KB).

## Qué se toca
- `.claude/agents/ejecutor.md` y `.claude/agents/verificador.md`: `model: sonnet` en el
  frontmatter.
- `.claude/agents/verificador.md`: en "Reglas", todo dato observado (log, consulta, conteo)
  va con la hora en que se observó (timestamp de la línea de log o `date -u` al consultar);
  antes de reportar un estado de algo que sigue corriendo, se vuelve a mirar.
- `.claude/skills/ejecutar/SKILL.md`: pasos **de espera**. Si `orquestador.md` marca un paso
  como espera (una corrida larga ya lanzada), el orquestador **no lanza verificador vivo**:
  arranca él un `Monitor` con el comando que trae el plan, que emite **solo** la línea final
  (`grep -m1` sobre un patrón de fin/aborto; nunca `tail -N`, nunca avisos de avance). Al
  llegar la notificación, lanza el verificador del paso "verificar al terminar". No consulta
  prod él mismo.
- `.claude/skills/planificar/SKILL.md`, sección "Cómo partir":
  - Una corrida de más de ~10 min se parte en "lanzar" (sin ejecutor, verificador que dispara
    y confirma `started`) y "verificar al terminar"; entre los dos, un paso de espera en
    `orquestador.md` con el comando exacto del `Monitor` y su patrón de fin y de aborto,
    probado contra una línea de WARN real para que no corte antes.
  - Las duraciones y rangos que dependen de fuentes externas van con margen (2–3x) y
    diciendo de dónde salen; lo que el plan da por existente (endpoint, método) se verificó
    en el código al planificar.
- `docs/agents/METODOLOGIA.md`: en "Roles", una línea con la espera por `Monitor` del
  orquestador y Sonnet en ejecutor/verificador; en "Mediciones y prod", la hora en cada dato
  observado. Respetar el tope de ~12 KB (`wc -c`).
- `docs/agents/historial-correcciones.md`: una línea con fecha 2026-09-14 resumiendo estos
  cambios (solo agregar al final; no leer el resto).
- `.claude/settings.json` (nuevo): `permissions.allow` con los comandos de las preguntas
  abiertas, una vez contestadas.

No tocar `CONTEXTO.md`, planes, código ni git.

## Tests
Ninguno: no hay código.

## Guion de prueba
Local:
1. `grep -n '^model:' .claude/agents/ejecutor.md .claude/agents/verificador.md` → las dos con
   `model: sonnet`.
2. `grep -n -i 'monitor' .claude/skills/ejecutar/SKILL.md .claude/skills/planificar/SKILL.md docs/agents/METODOLOGIA.md`
   → aparece la regla de espera en los tres.
3. `grep -n -i 'hora\|date -u' .claude/agents/verificador.md` → la regla de la hora.
4. `wc -c docs/agents/METODOLOGIA.md` → ≤ ~12.500.
5. `tail -1 docs/agents/historial-correcciones.md` → la línea nueva.
6. `python3 -m json.tool .claude/settings.json` → JSON válido con `permissions.allow`.
7. Leer los diffs (`git diff --stat` y `git diff .claude docs/agents/METODOLOGIA.md`) y
   confirmar que no se tocó nada fuera de "Qué se toca".

## Preguntas abiertas
1. Allowlist de `.claude/settings.json`: ¿cuáles entran? Propuesta:
   `Bash(./mvnw:*)`, `Bash(ssh elitedesk1 docker compose logs:*)`,
   `Bash(ssh elitedesk1 docker compose ps:*)`. `psql` por ssh no se puede acotar a solo
   lectura con un prefijo (`-c` acepta cualquier SQL): ¿entra igual, o se sigue pidiendo
   permiso?
