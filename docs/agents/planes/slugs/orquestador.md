# Plan — slugs de Greenhouse: CommonCrawl, Wayback y sondeo en prod

## Objetivo
Llevar `company` de 7.463 a ~18.000 empresas de Greenhouse (los 10 índices más recientes de
CommonCrawl más Wayback) y dejarlas sondeadas, con sus vacantes cargadas y normalizadas. Así la
lista de vacantes crece con las empresas que un solo índice no ve. Todo el código está en
`origin/main` (`7f33a53`); lo que falta son corridas en prod.

## Decisiones de Elias
- Los tres pasos son **sin ejecutor**: no llevan código, corre solo el verificador y, si falla,
  se para (no hay corrección).
- Orden: CommonCrawl → Wayback → sondeo y carga. CommonCrawl primero porque su resultado se
  compara contra la medición; después de Wayback daría casi 0 empresas nuevas y no probaría nada.
- Un 400 de CommonCrawl **no se reintenta**. Si vuelve, el paso falla y arreglarlo se planifica
  aparte.
- Quedan afuera: el dominio EU, el corte silencioso en un salto de línea, el slug falso de
  Wayback por una línea cortada y el log por índice mientras corre.
- Las corridas en prod las hace el agente (`ssh elitedesk1`).

## Pasos
- [ ] `paso-01-commoncrawl-en-prod.md` — los 10 índices recientes, 0 fallidos. **Sin ejecutor.**
  Puertas: antes: ninguna (el código ya está pusheado). Antes de verificar: ninguna. Después:
  Elias mira el resultado antes de Wayback.
- [ ] `paso-02-wayback-en-prod.md` — Wayback entero (~40 min). **Sin ejecutor.** Puertas:
  antes: ninguna. Antes de verificar: ninguna. Después: Elias da el ok para sondear (son horas
  de corrida).
- [ ] `paso-03-sondeo-y-carga.md` — sondeo de todo, carga de vacantes de las `ACTIVE` y
  normalización de lo nuevo. **Sin ejecutor.** Puertas: antes: ok de Elias. Antes de verificar:
  ninguna. Después: ninguna.

## Estado y desvíos
<lo llena el orquestador>

## Al terminar
- `docs/agents/tema/descubrimiento.md` ("Probado en prod"): resultado de los 10 índices, de
  Wayback y del sondeo, con sus números reales.
- `docs/agents/tema/vacantes.md`: el recorrido sobre las empresas nuevas.
- `CONTEXTO.md`: los totales de `company` y `vacancy`, y sacar el plan en curso.
- Archivar los logs crudos que guardaron los verificadores en `mediciones/slugs-dd-mm-yyyy/`.
- Preguntar a Elias antes de borrar la carpeta. `MEDICION-SLUGS.md` se queda.
