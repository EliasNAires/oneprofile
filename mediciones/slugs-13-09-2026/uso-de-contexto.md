# Uso de contexto — `/ejecutar slugs` (2026-09-13/14)

Primera ejecución real de un plan con orquestador + verificadores. Todos corrieron en Opus 5.
`subagent_tokens` es el valor de la última notificación de cada subagente (su contexto final).

| Agente | Tarea | Contexto final | Herramientas | Notas |
|---|---|---|---|---|
| Verificador 1 | Paso 1: CommonCrawl en prod + investigación del 504 | 71.646 | 27 | Retomado 4 veces con mensajes |
| Verificador 2 | Correr solo los 4 índices fallidos | 38.488 | 4 | Paró enseguida: no existe el endpoint |
| Verificador 3 | Paso 2: Wayback en prod | 50.837 | 13 | ~98 min de corrida |
| Verificador 4 | Paso 3: sondeo + vacantes + normalización | 81.120 | 30 | ~11 h de reloj, mayoría esperando |
| Orquestador | Toda la sesión | **182.200** al cerrar (73.500 a mitad del paso 1) | — | ~35k fijos (sistema, herramientas, memoria, skills) + 146.900 de mensajes. Sin el cierre del plan (lectura de docs de tema, ediciones y reporte final, ~30–35k estimados) quedaba en **~150k** |

Lectura:

- **El piso de un subagente ronda ~35–40k** aunque la tarea sea trivial (el verificador 2 hizo
  4 llamadas y terminó en 38k): es el sistema, las definiciones de herramientas y la lectura
  inicial. Es el argumento para probar Sonnet en ejecutor y verificador.
- Los verificadores de corridas largas crecieron poco por la corrida en sí; lo que más sumó
  fueron las investigaciones pedidas en el medio (504, ritmo del sondeo, avance con `xmin`).
- **El orquestador se desbordó** respecto de lo que esperaba Elias: ~150k antes del cierre, **más que cualquier verificador**. Lo
  que más sumó: ~40 notificaciones de avance reenviadas una por una, los reportes completos
  de cada verificador, las consultas propias a prod cuando los monitores no avisaban y
  mantener `orquestador.md` y `CONTEXTO.md` al día en cada desvío. El orquestador no debería
  ser el que más contexto junta: los avisos de avance tendrían que llegarle solo al terminar o
  al fallar.
