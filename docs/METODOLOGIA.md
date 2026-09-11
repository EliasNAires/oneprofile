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
- **Paquetes:** todo bajo `oneprofile.backend`, organizado **por feature**
  (`vacancy`, `profile`, `matching`, ...), no por capa técnica
  (nada de un `controllers/` global con todo adentro).
- **Comandos habituales:**
  - `./mvnw test` — corre los tests.
  - `./mvnw spring-boot:run` — levanta la app.
  - `./mvnw -q verify` — build completo.
- **Tests:** test unitario liso (sin contexto de Spring) cuando alcanza;
  `@DataJpaTest` para repositorios; `@WebMvcTest` para controllers; contexto
  completo (`@SpringBootTest`) solo cuando realmente hace falta, porque es lento.
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
- **2026-09-10 — Archivo de contexto.** El estado del repo vive en
  `docs/CONTEXTO.md` y se actualiza después de cada milestone alcanzado, para
  poder retomar sin reconstruir todo leyendo código.

## Documentos del repo

- `CLAUDE.md` — resumen corto en la raíz, se carga solo en cada sesión.
- `docs/METODOLOGIA.md` — este archivo: cómo trabajamos.
- `docs/CONTEXTO.md` — estado actual del repo, actualizado tras cada milestone.
