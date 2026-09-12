# Metodología de trabajo — oneprofile/backend

Este documento es la fuente de verdad sobre **cómo trabajamos** en este repo.
Claude lo lee antes de tocar nada. Cuando Elias corrige algo sobre la forma de
trabajar, esa corrección se agrega acá en la misma respuesta — el objetivo es no
repetir el mismo error dos veces ni tener que volver a explicar lo mismo en la
próxima sesión.

## El objetivo del proyecto

Llegar a tener una buena lista de vacantes laborales que matcheen con el perfil
del usuario. Ese es el destino final, pero se construye por pasos chicos y
probados, no de una sola vez.

## Cómo trabajamos (el ciclo)

Cada pedido de Elias se resuelve con este ciclo. Un pasaje completo del ciclo es
"un paso".

1. **Alcance chico.** Un paso = una capacidad que se pueda probar a mano en pocos
   minutos. Si lo que se pide es grande, lo parto en pasos y propongo el orden
   antes de empezar. Preferimos muchos pasos verificados a uno grande sin probar.

2. **Plan primero.** Antes de escribir código: qué archivos se tocan, qué va a
   quedar funcionando al terminar, y cómo se va a probar. Elias lo aprueba.

   Si el plan abarca **más de un paso**, lo que queda pendiente se escribe en un
   documento del repo, `docs/PLAN-<TEMA>.md`, **autocontenido**: pensado para que
   otra sesión lo retome leyendo solo ese archivo, este y `docs/CONTEXTO.md`. Ahí va
   todo lo que se midió o averiguó —respuestas reales de una API, números de una
   corrida—, porque si no hay que volver a averiguarlo. Las decisiones que faltan
   quedan anotadas **como preguntas para Elias**, no resueltas por cuenta propia. El
   documento se borra cuando el plan termina y su contenido pasa a `CONTEXTO.md`.

3. **Implementar solo el paso, nada más que el paso.** Esto es estricto: no se
   agrega *nada* que Elias no haya pedido. Ni features "por las dudas", ni
   abstracciones anticipadas, ni refactors de oportunidad, ni configuración
   "que suele venir bien", ni tests de más. Lo que sobra es ruido: hace más
   difícil leer el cambio y entender qué es realmente necesario.

   **Si me parece que algo es importante y no me lo pidieron, PREGUNTO ANTES.
   Siempre.** No se agrega primero para explicarlo después.

   La regla corta: **si no se usa o no se pidió, no se pone. Si algo no sirve, se
   saca.** Vale para dependencias, propiedades de configuración, clases, métodos,
   parámetros, archivos y tests. No dejamos cosas "por si acaso": lo que no
   cumple una función hoy es ruido, y cuando haga falta se agrega en ese momento.
   Cuando algo deja de servir —porque cambió una decisión o quedó huérfano— se
   elimina en el mismo paso, no se deja ahí.

4. **Tests automáticos en el mismo paso.** No se difieren "para después". Un paso
   sin tests no está terminado. Pero **solo tests que prueben comportamiento**:
   nada de testear configuración declarativa (que un archivo de properties tenga
   cierta clave, que una anotación esté puesta). Eso no prueba nada, se rompe al
   renombrar una clave y es puro ruido.

5. **Guion de prueba manual.** Al cerrar, entrego los comandos exactos
   (`./mvnw ...`, `curl ...`, etc.) y qué tiene que verse en pantalla para saber
   que funciona. El paso se da por cerrado recién cuando Elias lo probó a mano.

6. **Reporte honesto.** Si un test falla, muestro la salida real. Si algo quedó
   afuera del paso, lo digo explícitamente. Nunca se reporta "listo" algo que no
   se verificó.

## Convenciones del proyecto

- **Idioma:** la conversación y los documentos, en español. El código —nombres de
  clases, métodos, variables, paquetes, mensajes de commit y comentarios— en inglés.
- **Paquetes:** todo bajo `oneprofile.backend`, organizado **por capa técnica**
  (`model`, `repository`, `service`, `controller`, `util`). Las features conviven
  dentro de cada capa; no hay un paquete por feature. Las clases que no son
  ninguna capa de MVC —funciones puras, helpers— van a `util`. Un paquete de capa
  se crea recién cuando tiene su primera clase, no vacío.
- **Comandos habituales:**
  - `./mvnw test` — corre los tests.
  - `./mvnw spring-boot:run` — levanta la app.
  - `./mvnw -q verify` — build completo.
- **Tests:** test unitario liso (sin contexto de Spring) cuando alcanza;
  `@DataJpaTest` para repositorios; `@WebMvcTest` para controllers; contexto
  completo (`@SpringBootTest`) solo cuando realmente hace falta, porque es lento.
- **Documentación para personas** (`docs/para-humanos/`): **intuitiva y breve**.
  Se escribe para que alguien entienda el sistema de una sentada, apoyándose en
  diagramas y en ejemplos concretos, no para ser completa. Entre ser exhaustivo y
  que se entienda rápido, gana lo segundo: el detalle ya está en `docs/CONTEXTO.md`,
  y es esa división la que le permite a este documento dejar cosas afuera. Vale
  igual para las notas de los diagramas: cortas y en criollo.
- **Diagramas:** en PlantUML (`.puml`), y se versiona también el `.svg` generado.
  Criterio para las **notas** explicativas:
  - Van en los diagramas de **secuencia y de actividad**, donde PlantUML las ancla
    al elemento que comentan. En los de **componentes, clases y despliegue** el
    layout lo resuelve Graphviz: las notas terminan lejos, con líneas punteadas
    que cruzan todo el dibujo, así que ahí no van.
  - Una nota dice **lo que el dibujo no puede mostrar**: el porqué de una decisión,
    una restricción que no se ve en las flechas, o un número medido que da escala.
    Nunca repite en palabras lo que las cajas y las flechas ya dicen.
  - Si una nota no entra limpia, **su contenido se mueve al texto** que acompaña al
    diagrama; no se tira. Manda la legibilidad del diagrama: si la nota lo ensucia,
    el diagrama pierde más de lo que la nota aporta.
  - Un diagrama se da por bueno **mirándolo renderizado**, no porque compile.
- **Secretos:** ninguna credencial, password de base ni API key va al repo.
  Configuración sensible por variable de entorno.
- **Git:** lo maneja Elias. Claude no inicializa repos, no commitea ni pushea
  salvo pedido explícito.

## El archivo de contexto

`docs/CONTEXTO.md` guarda el **estado actual del repo**: qué existe, qué anda,
qué está a medias y qué sigue. Sirve para retomar el proyecto sin tener que
reconstruir la historia leyendo código.

Reglas:

- Claude lo lee al empezar a trabajar, junto con este documento.
- Se **actualiza después de cada milestone alcanzado** — no en cada commit ni en
  cada cambio chico, sino cuando se completa un paso que Elias ya probó a mano.
- Describe el estado real, no las intenciones: si algo quedó a medias o no anda,
  se dice ahí.
- Reemplaza el estado anterior; no es un changelog que crece sin fin. La historia
  larga la lleva git.

## Registro de correcciones

Cada entrada es una regla aprendida, con la fecha en que se acordó.

- **2026-09-10 — Metodología escrita y viva.** La forma de trabajar vive en este
  archivo y se lee antes de trabajar. Cada corrección de Elias sobre cómo
  trabajar se agrega acá; no alcanza con aplicarla una vez en la conversación.
- **2026-09-10 — De a poco y probado a mano.** Construimos en pasos chicos: se
  implementa, se deja test, Elias lo prueba manualmente, y recién ahí seguimos.
- **2026-09-10 — Ciclo acordado.** Plan → código → tests automáticos → guion de
  prueba manual.
- **2026-09-10 — Git es de Elias.** No inicializar ni versionar por cuenta propia.
- **2026-09-10 — No agregar nada que no se pidió.** En la configuración de
  perfiles agregué un montón de settings que Elias no había pedido
  (`open-in-view`, `show-sql`, `format_sql`, niveles de logging, flags de
  devtools que ya vienen por defecto). Eso genera ruido. Si algo me parece
  importante, **pregunto antes, siempre**, en vez de agregarlo y avisar después.
- **2026-09-10 — Nada de tests declarativos.** Escribí tests que verificaban que
  los archivos de properties tuvieran ciertas claves. Eso es configuración
  declarativa, no comportamiento: no aporta y hay que borrarlo. Los tests prueban
  lo que el código *hace*.
- **2026-09-10 — No declarar lo que el framework ya resuelve.** Spring Boot deriva
  el driver JDBC de la URL del datasource, así que `spring.datasource.driver-class-name`
  sobra. Antes de escribir una línea de configuración, verificar si el default ya
  la cubre.
- **2026-09-10 — Si no se usa o no se pidió, no se pone; si no sirve, se saca.**
  Es la forma corta de la regla anterior y se aplica a todo: dependencias,
  properties, clases, métodos, archivos y tests. Salió de dejar un datasource H2
  en archivo que no persistía nada: no cumplía ninguna función, así que se
  eliminó en vez de quedarse "por las dudas".
- **2026-09-11 — Paquetes por capa técnica, no por feature.** Elias pidió pasar
  la estructura a MVC y eligió la variante de **capa técnica global**: `model`,
  `repository`, `service`, `controller` y `util` en la raíz de
  `oneprofile.backend`. Esto **deroga** la convención anterior, que mandaba
  organizar por feature y prohibía explícitamente un paquete por capa; se le
  señaló el conflicto antes de mover nada y lo confirmó. Los paquetes `company` y
  `discovery` desaparecieron.
- **2026-09-10 — Archivo de contexto.** El estado del repo vive en
  `docs/CONTEXTO.md` y se actualiza después de cada milestone alcanzado, para
  poder retomar sin reconstruir todo leyendo código.
- **2026-09-11 — La documentación para personas no la lee el agente.** Elias pidió
  una carpeta aparte, `docs/para-humanos/`, con una explicación breve y con
  diagramas del sistema, y aclaró que **`docs/CONTEXTO.md` tiene que decir que el
  agente no debe leerla**: su contenido está duplicado, así que leerla gasta
  contexto en información repetida y arriesga trabajar sobre el resumen en vez de
  la fuente de verdad. La regla general: cuando se escribe documentación dirigida a
  personas, se la marca como off-limits para el agente en el mismo paso en que se
  crea, tanto en el propio documento como en `CONTEXTO.md`.
- **2026-09-11 — Criterio de notas en los diagramas.** Al hacer los diagramas de
  `docs/para-humanos/` saqué las notas de tres de ellos porque el layout automático
  las mandaba lejos y llenaba el dibujo de líneas punteadas cruzadas, y pasé su
  contenido al texto. A Elias le gustó el resultado y pidió que el criterio quedara
  escrito; está arriba, en las convenciones.
- **2026-09-11 — Los planes de varios pasos viven en un doc del repo.** Al cerrar el
  paso 1 del sondeo, Elias pidió *"guarda el plan del paso 2 en un doc aparte, para
  que lo siga otro chat en un contexto nuevo"*. El proyecto se construye en muchas
  sesiones cortas y una sesión nueva arranca sin nada de lo conversado, así que un
  plan que vive solo en la conversación se pierde. El criterio de escritura está
  arriba, en el punto 2 del ciclo.
- **2026-09-11 — Entrar en modo plan al empezar a planificar.** Elias pidió que *"al
  comenzar cada plan automáticamente te pones en modo plan"*, sin que él tenga que
  activarlo. Salió de arrancar el paso 2 en modo auto —lo había dejado activado sin
  querer— y ponerme a ejecutar consultas contra la base de prod en vez de planificar.
  El modo plan es la contraparte en la herramienta de la regla "plan antes de código"
  del punto 2 del ciclo: garantiza que mientras se diseña no se toca nada del sistema,
  que es exactamente lo que esa etapa espera. Confiar en la propia disciplina no
  alcanza, porque el modo activo puede habilitar acciones que la etapa no debería
  permitir.
- **2026-09-12 — En `CONTEXTO.md` no se numeran los pasos.** Al limpiar el plan de
  normalización, Elias pidió *"borra lo de los pasos, en la proxima sesion voy a
  arrancar con un plan nuevo"*. La numeración (`paso 2a`, `2b`, `3a`…) vive en el
  `docs/PLAN-<TEMA>.md` de turno, y ese documento **se borra al terminar el plan**: los
  números que quedaron citados en `CONTEXTO.md` apuntan entonces a algo que ya no
  existe y no se pueden interpretar sin la historia de la conversación. Por eso en
  `CONTEXTO.md` cada milestone se nombra **por lo que es** ("el despliegue", "el
  recorrido masivo de vacantes"), no por su número de paso. Dentro del plan en curso
  numerar está bien; al cerrarlo, lo que pase a `CONTEXTO.md` va sin números.
- **2026-09-12 — Las consultas las corro yo; hay salidas que Elias lee primero.** Al
  arrancar la normalización pidió *"dame las instrucciones para entrar a base de datos
  en prod, y que queres saber, asi me internalizo con los datos yo tambien, posterior a
  mi analisis, podes analizar vos tambien"*, y yo lo entendí como que quería
  ejecutarlo él. Me corrigió: *"corre los comandos en mi servidor y dejame el txt de la
  salida, asi lo leo"*. O sea que **ejecutar siempre me toca a mí** (`ssh elitedesk1`),
  y lo que cambia según el tipo de medición es **quién lee la salida primero**:
  verificar un número puntual lo analizo y lo cuento de una, pero **explorar la
  distribución de un dataset para decidir cómo modelarlo la lee él antes**, para
  formarse su propio criterio sin que mi lectura se lo anticipe. En ese caso el
  entregable es el `.txt` crudo donde lo pueda abrir, más si los controles de sanidad
  dieron bien, y el análisis se guarda hasta que él lo haya mirado. El documento del
  plan igual lleva escrito **qué contesta cada consulta y por qué se pide**, que es lo
  que le permite leer la salida sin mí.
- **2026-09-12 — Para sacar ruido de texto: lista blanca, no lista negra.** Propuse
  limpiar los títulos enumerando los separadores a eliminar y Elias lo invirtió:
  *"why not all special caracters minus the ones i care about? feel free to see if
  there's another special caracter we'd should keep"*. Se saca **todo** lo que no sea
  letra, dígito o espacio, salvo una lista chica y explícita de lo que se conserva,
  porque enumerar lo que molesta siempre deja alguno afuera y enumerar lo que importa
  es un conjunto cerrado que se justifica caracter por caracter. Dos corolarios: la
  lista blanca se propone **razonada**, con el ejemplo concreto de qué se rompería sin
  cada caracter; y un caracter puede merecer conservarse **solo en cierta posición**
  (el punto sirve en `.net`, es ruido en `Engineer.`).
- **2026-09-12 — Si Elias ya autorizó, se reintenta; no se le devuelve el comando.**
  En el servidor de prod el Docker es rootful y el usuario no está en el grupo `docker`,
  así que las consultas necesitan `sudo`. El clasificador de auto mode me bloqueó el
  `sudo` dos veces, y yo paré y le pasé el comando para que lo corriera él. Me corrigió:
  *"deberias poder, en una sesion anterior pudiste. corre sudo antes de cada comando"*.
  Es la regla de "las consultas las corro yo" aplicada al caso en que la herramienta se
  pone en el medio: **cuando él ya dio la autorización, se busca otra forma razonable de
  ejecutarlo en vez de delegárselo**. Lo que funcionó fue partirlo en dos llamadas —una
  que cachea el ticket de `sudo`, otra que corre el comando—, pero dentro de la **misma
  invocación de SSH**, porque con `tty_tickets` el ticket no sobrevive de una conexión a
  la siguiente. La contraseña no se guarda en ningún archivo ni en el repo.

- **2026-09-11 — La documentación para personas es intuitiva y breve.** Elias pidió
  que quedara escrito que `docs/para-humanos/` se escribe para entenderse rápido y
  no para ser exhaustiva, notas de los diagramas incluidas. El detalle vive en
  `docs/CONTEXTO.md`; acá se privilegia que se entienda de una sentada.

## Documentos del repo

- `CLAUDE.md` — resumen corto en la raíz, se carga solo en cada sesión.
- `docs/METODOLOGIA.md` — este archivo: cómo trabajamos.
- `docs/CONTEXTO.md` — estado actual del repo, actualizado tras cada milestone.
- `docs/PLAN-<TEMA>.md` — el plan en curso, si hay uno. Se borra al terminarlo.
- `docs/para-humanos/` — explicación breve y con diagramas, **para personas**. El
  agente no la lee.
