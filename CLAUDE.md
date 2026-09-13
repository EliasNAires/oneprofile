# oneprofile / backend

Backend en Java + Spring Boot. El objetivo final es armar una buena lista de
vacantes laborales que matcheen con el perfil del usuario. Se construye por pasos
chicos y probados, no de una sola vez.

## Antes de tocar nada

1. Leé **`docs/agents/METODOLOGIA.md`** — cómo trabajamos — y seguila. Su sección
   "Qué leer al empezar" dice qué más cargar; nada más que eso.
2. Leé **`docs/agents/CONTEXTO.md`** — estado actual y fase siguiente.
3. Si Elias te corrige algo sobre **cómo trabajar**, en la misma respuesta: la regla va
   a su sección de `docs/agents/METODOLOGIA.md` y una línea a
   `docs/agents/historial-correcciones.md`.
4. **Una sesión por fase** (analizar, construir, probar, corregir). Al terminar la
   tuya, actualizá `docs/agents/CONTEXTO.md` sin que te lo pidan: qué hiciste, los
   resultados reales y la fase siguiente con su línea "Leer:".

## Lo esencial de la metodología

- **Pasos chicos.** Un paso = algo que se pueda probar a mano en pocos minutos.
- **Plan antes de código.** Qué archivos se tocan y qué va a quedar funcionando.
- **Si no se usa o no se pidió, no se pone. Si algo no sirve, se saca.** Vale para
  dependencias, properties, clases, métodos, archivos y tests. Nada "por las dudas".
- **Ante la duda, preguntar antes.** Si algo te parece importante y no te lo
  pidieron, se pregunta; no se agrega para explicarlo después.
- **Tests en el mismo paso.** Un paso sin tests no está terminado.
- **Guion de prueba para cerrar.** Entregá los comandos exactos y qué tiene que
  verse; la prueba la corre la sesión siguiente.
- **Reporte honesto.** Si algo falla o quedó afuera, decilo con la salida real.

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
