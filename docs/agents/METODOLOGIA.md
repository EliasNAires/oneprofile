# Metodología — oneprofile/backend

Fuente de verdad sobre **cómo trabajamos**. Cuando Elias corrige algo sobre la forma de
trabajar, en la misma respuesta: la regla se integra **en su sección de este archivo**, en
forma corta, y se agrega **una línea** a `docs/agents/historial-correcciones.md`. Tope de
este archivo: ~12 KB (`wc -c`); si se pasa, se poda.

## Objetivo

Una buena lista de vacantes laborales que matcheen con el perfil del usuario, construida
por pasos chicos y probados.

## Qué leer al empezar

1. `CLAUDE.md` (se carga solo) y este archivo.
2. `docs/agents/CONTEXTO.md`: estado actual y fase siguiente.
3. Lo que diga la línea **"Leer:"** de la fase siguiente (docs de tema, un plan,
   archivos puntuales). **Nada más**, salvo que la tarea lo pida.

No se leen:

- `docs/para-humanos/`: es para personas y duplica lo que ya está para agentes. Solo si
  Elias pide escribirla o actualizarla.
- `mediciones/`: salidas crudas; se abre solo el archivo que un plan o un pedido nombre.
- `docs/agents/historial-correcciones.md`: solo se le agrega una línea.

## El ciclo (un pasaje = un paso)

1. **Alcance chico.** Un paso = una capacidad que se prueba a mano en pocos minutos. Si
   el pedido es grande, se parte en pasos y se propone el orden antes de empezar.
2. **Plan primero, en modo plan.** Al empezar a planificar se entra en modo plan sin
   esperar a que Elias lo active: mientras se diseña no se toca nada del sistema. El plan
   dice qué archivos se tocan, qué queda funcionando y cómo se prueba; Elias lo aprueba.
   - Un plan de **más de un paso** se escribe en `docs/agents/PLAN-<TEMA>.md`, **autocontenido**
     (se retoma leyendo solo ese archivo, este y `CONTEXTO.md`): lleva todo lo medido o
     averiguado, y las decisiones pendientes **como preguntas para Elias**. Cada paso
     tiene que **entrar en el presupuesto de una sesión**.
   - Al terminar el plan: el `PLAN-*.md` se borra y lo que vale pasa a `CONTEXTO.md`
     (o a su doc de tema); las salidas crudas **se archivan** en
     `mediciones/<tema>-dd-mm-yyyy/`. Un archivo sin versionar con datos medidos no se
     borra sin preguntar: git no lo devuelve.
3. **Implementar solo el paso.** **Si no se usa o no se pidió, no se pone. Si algo no
   sirve, se saca** (en el mismo paso en que se detecta). Vale para dependencias,
   properties, clases, métodos, parámetros, archivos y tests. **Si algo parece
   importante y no se pidió, se pregunta antes, siempre**; agregarlo y avisar después no
   cuenta. No se declara lo que el framework ya resuelve: antes de escribir configuración,
   verificar el default.
   - La regla es sobre features y código especulativo, **no sobre la cobertura del
     dominio**. Que un caso tenga poco volumen hoy no lo descarta del modelo (el dataset
     sale de un solo ATS, sesgado al inglés): se le presenta a Elias con su número,
     avisando si es de otro idioma. La pregunta es "¿el modelo da un dato equivocado
     cuando aparece?", no "¿cuántos hay?".
4. **Tests en el mismo paso.** Un paso sin tests no está terminado. Solo tests de
   **comportamiento**: nada de testear configuración declarativa (claves de properties,
   anotaciones).
5. **Guion de prueba.** Comandos exactos y qué tiene que verse. La prueba la corre otra
   sesión.
6. **Reporte honesto.** Salida real si algo falla; lo que quedó afuera se dice. Nada se
   reporta "listo" sin verificar.

## Una sesión por fase

Analizar, construir, probar y corregir van **cada una en una sesión nueva**. Las encadena
`CONTEXTO.md`, que **la sesión actualiza sola al terminar su fase**: qué hizo, resultados
reales y la fase siguiente con su línea "Leer:".

- **Construir** deja lo construido, los tests que dieron y el guion. Sigue probar.
- **Probar** corre y analiza. Si anda, cierra el tema; si falla, deja un **reporte**
  (qué se corrió, salida real, causa si se encontró). Sigue corregir.
- **Corregir** parte del reporte y vuelve a probar.

## Diseño

- **"Arquitectura" o "diseño" es reparto de responsabilidades, composición y reuso**, no
  comportamiento: cuando Elias pide mejorar el diseño, no se proponen features.
- **Se separa por motivo de cambio**, ni sobre- ni submodularizando: un client por
  proveedor externo (dos proveedores son dos clients aunque se parezcan); un service y un
  controller por caso de uso, con sus variantes como métodos; un util solo si lo usa más
  de un lugar. Partir por técnica (reintentos, paginado) no es motivo de cambio. Lo nuevo
  queda armonioso con lo existente.
- **Las propuestas de diseño de Elias se discuten** antes de ejecutarlas: con qué acuerdo,
  dónde veo un problema y con qué argumento. Decide él; no se aplica la alternativa por
  cuenta propia.

## Mediciones y prod

- **Las consultas a prod las corro yo** (`ssh elitedesk1`); no se le devuelven comandos a
  Elias. Si él ya autorizó y la herramienta bloquea algo (p. ej. `sudo`), se busca otra
  forma razonable de ejecutarlo en vez de delegar.
- **Quién lee primero:** verificar un número puntual lo analizo y lo cuento. **Explorar
  la distribución de un dataset para decidir un modelo la lee Elias antes**: el
  entregable es el `.txt` crudo, más si dieron los controles de sanidad, y el análisis se
  guarda hasta que él lo mire. El plan dice qué contesta cada consulta y por qué.
- **Una ventana que no muestra el caso no prueba que no exista.** Si una medición tiene
  `limit`, top N o umbral y el caso buscado no aparece adentro, se amplía y se remide.
- **Limpiar texto: lista blanca, no negra.** Se saca todo lo que no sea letra, dígito o
  espacio salvo una lista chica y explícita, propuesta caracter por caracter con el
  ejemplo de qué se rompe sin él. Un caracter puede conservarse solo en cierta posición
  (el punto en `.net`, no en `Engineer.`).

## Convenciones

- **Idioma:** conversación y documentos en español; código, identificadores, comentarios
  y commits en inglés.
- **Paquetes:** bajo `oneprofile.backend`, **por capa técnica** (`model`, `repository`,
  `service`, `controller`, `client`, `util`), no por feature. `client` = clases que
  hablan con un sistema externo. `util` = funciones puras y helpers sin Spring. Un paquete
  se crea con su primera clase.
- **Comandos:** `./mvnw test`, `./mvnw spring-boot:run`, `./mvnw -q verify`.
- **Tests:** unitario liso cuando alcanza; `@DataJpaTest` para repositorios;
  `@WebMvcTest` para controllers; `@SpringBootTest` solo si hace falta.
- **Secretos:** nada de credenciales en el repo ni en memorias; por variable de entorno.
- **Git:** lo maneja Elias. No se inicializa, commitea ni pushea sin pedido explícito.

## Documentación

- **Para agentes se escribe lo que el código no dice:** el porqué de una decisión, lo
  medido, lo descartado y por qué, las restricciones del entorno. Firmas, campos,
  constructores y listas o conteos de tests no se escriben: se leen del código o de
  `./mvnw test`.
- **`CONTEXTO.md`** es el estado actual, no un changelog: reemplaza lo anterior (la
  historia la lleva git), dice lo que está a medias o no anda, y lo único que mira hacia
  adelante es la fase siguiente con "Leer:". Los milestones se nombran **por lo que
  son**, nunca por número de paso: el plan que numeraba se borra. Tope ~12 KB.
- **`docs/para-humanos/`** es **intuitiva y breve**, apoyada en diagramas y ejemplos:
  entre exhaustiva y que se entienda de una sentada, gana lo segundo. Toda documentación
  para personas se marca como off-limits para el agente en el mismo paso en que se crea,
  en el propio documento y en `CONTEXTO.md`.
- **Diagramas** en PlantUML, versionando también el `.svg`. Se dan por buenos **mirándolos
  renderizados**, no porque compilen. Notas:
  - Van en diagramas de secuencia y actividad, donde se anclan al elemento. En
    componentes, clases y despliegue no: Graphviz las manda lejos.
  - Dicen lo que el dibujo no muestra (porqué, restricción, número medido), cortas y en
    criollo; nunca repiten las flechas.
  - Si una nota no entra limpia, su contenido pasa al texto que acompaña al diagrama.

## Higiene de contexto durante la sesión

- Salidas largas, filtradas: `./mvnw -q test`, `... | tail -40`; consultas de prod a un
  `.txt` y `grep`/`head` sobre él.
- Búsquedas amplias por el código con el subagente `Explore`: vuelve solo la conclusión.
- Docs largos: leer solo el rango que hace falta (`grep -n '^##'` da las secciones).
- `/context` para ver el consumo real.

## Documentos

- `CLAUDE.md` — resumen en la raíz, se carga solo.
- `docs/agents/METODOLOGIA.md` — este archivo.
- `docs/agents/historial-correcciones.md` — una línea por corrección; no se lee.
- `docs/agents/CONTEXTO.md` — estado actual y fase siguiente.
- `docs/agents/tema/<tema>.md` — detalle de un tema; se lee solo si "Leer:" lo pide.
- `docs/agents/PLAN-<TEMA>.md` — plan en curso, si hay; se borra al terminarlo.
- `docs/agents/MEDICION-*.md` — números medidos que un plan o tema cita.
- `mediciones/` — salidas crudas archivadas; no se lee.
- `docs/para-humanos/` — para personas; no se lee.
