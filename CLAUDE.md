# oneprofile / backend

Backend en Java + Spring Boot. El objetivo final es armar una buena lista de
vacantes laborales que matcheen con el perfil del usuario. Se construye por pasos
chicos y probados, no de una sola vez.

## Modo de la sesión

Cada sesión principal trabaja en un modo, con su skill, que dice qué leer:

- **`/planificar`** — conversar un problema con Elias y dejar el plan partido en archivos.
- **`/ejecutar <tema>`** — orquestar un plan de `docs/agents/planes/<tema>/`.
- **`/consultar`** — preguntar algo sobre el código o el estado del repo.

Si el primer mensaje no usa ninguna, **lo primero es preguntarle a Elias qué modo quiere**
(y si es ejecutar, qué plan: las carpetas de `docs/agents/planes/`), sin leer nada antes.
Los subagentes `ejecutor` y `verificador` no preguntan el modo: su rol está en su definición.

## Lo esencial de la metodología

Detalle en `docs/agents/METODOLOGIA.md`; cada rol lee solo las secciones que le tocan.

- **Pasos chicos.** Un paso = algo que se pueda probar a mano en pocos minutos.
- **Si no se usa o no se pidió, no se pone. Si algo no sirve, se saca.** Vale para
  dependencias, properties, clases, métodos, archivos y tests. Nada "por las dudas".
- **Ante la duda, preguntar antes.** Nadie la resuelve solo: un subagente para y la
  devuelve, y le llega a Elias.
- **Tests en el mismo paso.** Un paso sin tests no está terminado.
- **Reporte honesto.** Si algo falla o quedó afuera, decilo con la salida real.
- Si Elias corrige algo sobre **cómo trabajar**, en la misma respuesta: la regla va a su
  sección de `docs/agents/METODOLOGIA.md` y una línea a
  `docs/agents/historial-correcciones.md`.

## Stack y comandos

Java 25, Spring Boot 4.1.1, Spring Data JPA, Spring Web MVC, PostgreSQL (runtime).

```bash
./mvnw test              # tests
./mvnw spring-boot:run   # levantar la app
./mvnw -q verify         # build completo
```

## Convenciones

- Conversación y documentos en español; código e identificadores en inglés.
- Paquetes bajo `oneprofile.backend`, organizados por capa técnica
  (`model`, `repository`, `service`, `controller`, `client`, `util`).
- Diseño: se separa por motivo de cambio (un client por proveedor, un service por caso
  de uso, util solo si se usa en más de un lugar).
- Sin credenciales ni secretos en el repo.
- Git lo maneja Elias: no inicializar, commitear ni pushear sin pedido explícito.
