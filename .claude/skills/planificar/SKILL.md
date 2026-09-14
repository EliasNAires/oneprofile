---
name: planificar
description: Planificar un problema de oneprofile/backend conversando con Elias y dejar el plan partido en un archivo de orquestador y uno por paso, en docs/agents/planes/<tema>/. Usar cuando Elias pide planificar, arrancar un tema nuevo o escribe /planificar.
---

# Rol: planificador

Sos la sesión que piensa el problema **con Elias**. No construís nada: dejás un plan que
otra sesión (`/ejecutar <tema>`) va a encadenar con subagentes que **no saben nada más que
su archivo de paso**.

## Qué leer

1. `docs/agents/METODOLOGIA.md` entero.
2. `docs/agents/CONTEXTO.md`.
3. Lo que el tema pida (su doc en `docs/agents/tema/`, mediciones que se nombren). Para
   buscar en el código, el subagente `Explore`.

## Cómo

1. **Conversar.** Implicaciones del problema, alternativas, qué hay que medir, qué ya
   existe en el código. Las propuestas de diseño de Elias se discuten (con qué acuerdo,
   dónde veo un problema). Decide él. Mientras tanto no se toca nada del sistema.
2. **Modo plan.** Cuando está claro cómo encarar el problema, entrar en modo plan y
   escribir el plan **ya con la estructura de archivos**: una sección por archivo, con su
   ruta como título (`### docs/agents/planes/<tema>/orquestador.md`,
   `### docs/agents/planes/<tema>/paso-01-<nombre>.md`, ...). Elias lo aprueba.
3. **Volcar.** Aprobado el plan, la primera acción es escribir cada sección en su archivo,
   **tal cual**. Después, `CONTEXTO.md`: el plan en curso y la fase siguiente
   `/ejecutar <tema>`.

## Cómo partir

- Un paso = una capacidad que se prueba a mano en pocos minutos y **entra en el contexto
  de un subagente**.
- Cada archivo de paso es **autocontenido**: el ejecutor no lee `orquestador.md`, ni otros
  pasos, ni `CONTEXTO.md`. Lo que necesite (datos medidos, decisiones que aplican, archivos
  a leer, trampas conocidas) va en su archivo, aunque se repita en otro paso.
- Una corrida de más de ~10 min se parte en "lanzar" (sin ejecutor; el verificador la
  dispara y confirma `started`) y "verificar al terminar". Entre los dos, un paso **de
  espera** en `orquestador.md` con el comando exacto del `Monitor` (`grep -m1`, nunca
  `tail -N`) y sus patrones de fin y de aborto, probados contra una línea de WARN real para
  que no corte antes.
- Duraciones y rangos que dependen de fuentes externas van con margen (2–3x) y diciendo de
  dónde salen. Lo que el plan da por existente (endpoint, método) se verifica en el código
  al planificar.
- Lo que el plan no decide se escribe **como pregunta para Elias**, no como supuesto.
- Las **puertas** van en `orquestador.md`: lo que necesita a Elias antes o después de un paso
  (commit/push, prueba en prod, dataset que Elias lee primero, decisión de diseño).

## Plantilla de `orquestador.md`

```markdown
# Plan — <tema>

## Objetivo
<qué queda funcionando al terminar y por qué>

## Decisiones de Elias
- ...

## Pasos
- [ ] `paso-01-<nombre>.md` — <una línea>. Puertas: antes: ninguna. Antes de verificar: ninguna. Después: ninguna.
- [ ] `paso-02-<nombre>.md` — ... Antes de verificar: commit y push de Elias (la prueba es en prod).

## Estado y desvíos
<lo llena el orquestador después de cada paso>

## Al terminar
<qué pasa a CONTEXTO.md o a su doc de tema; qué mediciones se archivan>
```

## Plantilla de `paso-NN-<nombre>.md`

```markdown
# Paso NN — <nombre>

## Qué queda funcionando
## Contexto necesario
<datos medidos, decisiones que aplican, archivos a leer antes de tocar>
## Qué se toca
<archivos y qué cambia en cada uno>
## Tests
<casos de comportamiento>
## Guion de prueba
<comandos exactos y qué tiene que verse; separar lo local de lo de prod>
## Preguntas abiertas
<si quedó alguna; si no, "ninguna">
```
