# Historial de correcciones

> **El agente no lee este archivo**; solo le agrega una línea cuando Elias corrige algo
> sobre cómo trabajar. La regla vigente vive en `docs/agents/METODOLOGIA.md`. Para el
> detalle de cada corrección, git.

Formato: fecha — regla — de dónde salió.

- 2026-09-10 — La metodología vive escrita en el repo y cada corrección se agrega — pedido de Elias al arrancar el proyecto.
- 2026-09-10 — Pasos chicos, con test, probados a mano antes de seguir — pedido de Elias.
- 2026-09-10 — Ciclo plan → código → tests → guion de prueba — pedido de Elias.
- 2026-09-10 — Git lo maneja Elias — pedido de Elias.
- 2026-09-10 — No agregar nada no pedido; si parece importante, preguntar antes — agregué settings de perfiles que nadie pidió.
- 2026-09-10 — Nada de tests declarativos — escribí tests que chequeaban claves de properties.
- 2026-09-10 — No declarar lo que el framework resuelve — `driver-class-name` sobraba.
- 2026-09-10 — Si no se usa o no se pidió, no se pone; si no sirve, se saca — un H2 en archivo que no persistía nada.
- 2026-09-10 — El estado del repo vive en `docs/CONTEXTO.md` — pedido de Elias.
- 2026-09-11 — Paquetes por capa técnica, no por feature (deroga la convención por feature) — Elias pidió pasar a MVC.
- 2026-09-11 — La documentación para personas no la lee el agente, y se marca así al crearla — al crear `docs/para-humanos/`.
- 2026-09-11 — Criterio de notas en los diagramas — el layout mandaba las notas lejos en tres diagramas.
- 2026-09-11 — Los planes de varios pasos viven en un doc autocontenido del repo — pedido al cerrar el primer paso del sondeo.
- 2026-09-11 — Entrar en modo plan al empezar a planificar — arranqué en modo auto y consulté prod en vez de planificar.
- 2026-09-11 — La documentación para personas es intuitiva y breve — pedido de Elias.
- 2026-09-12 — En `CONTEXTO.md` los milestones no se numeran — los números apuntaban a un plan ya borrado.
- 2026-09-12 — Las consultas a prod las corro yo; las exploratorias las lee Elias primero — entendí que quería ejecutarlas él.
- 2026-09-12 — Limpiar texto con lista blanca, no negra — propuse enumerar separadores a eliminar.
- 2026-09-12 — Si Elias ya autorizó, se reintenta en vez de devolverle el comando — el clasificador bloqueó `sudo` y le pasé el comando.
- 2026-09-12 — El volumen actual no descarta un caso del modelo — propuse sacar `semi senior` por tener 12 vacantes.
- 2026-09-12 — Un top N que no muestra el caso no prueba que no exista — concluí sobre `senior` mirando solo el top 15.
- 2026-09-13 — "Arquitectura" es reparto de responsabilidades, no features — propuse features al pedido de mejorar el diseño.
- 2026-09-13 — Separar por motivo de cambio, ni sobre- ni submodularizar — propuse 5 services y 2 utils, y después uní dos clients de proveedores distintos.
- 2026-09-13 — Discutir el diseño que propone Elias antes de ejecutarlo — pedido de Elias.
- 2026-09-13 — Las mediciones se archivan en `mediciones/`, no se borran — borré siete `.txt` al cerrar un plan.
- 2026-09-13 — `mediciones/` no se lee si nada apunta ahí — pedido de Elias.
- 2026-09-13 — Una sesión por fase, y `CONTEXTO.md` se actualiza solo al terminar cada una (deroga actualizar recién tras la prueba manual) — pedido al cerrar la construcción de Wayback.
- 2026-09-13 — Dieta de contexto: docs para agentes en `docs/agents/`, la regla una sola vez, se escribe lo que el código no dice, topes de ~12 KB, "Leer:" en la fase siguiente, higiene de contexto — las sesiones arrancaban con ~70k tokens.
- 2026-09-13 — Roles planificador / orquestador / ejecutor / verificador / consulta, con plan partido en `docs/agents/planes/<tema>/` (deroga una sesión por fase) — Elias se la pasaba haciendo `/clear` y "seguí con el siguiente paso".
- 2026-09-13 — El planificador usa modo plan y escribe el plan ya partido; el orquestador solo encadena; las dudas de los subagentes van a Elias, nadie las resuelve solo — propuse un planificador sin modo plan y no dejé claro que el orquestador no decide.
- 2026-09-13 — Un paso sin código (corrida o prueba en prod) se marca "sin ejecutor": solo verificador, y si falla se para — el plan de slugs quedaba con tres pasos sin nada que construir.
- 2026-09-14 — Una prueba nueva va a un subagente nuevo con un resumen breve del anterior, no al mismo que ya cargó mucho contexto — iba a pedirle al verificador del paso 1 de slugs que corriera también los índices fallidos.
- 2026-09-14 — Techo blando de ~100k de contexto por sesión, orquestador incluido — el orquestador de `/ejecutar slugs` cerró en 182k.
