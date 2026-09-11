# oneprofile / backend

Backend en Java + Spring Boot. El objetivo final es armar una buena lista de
vacantes laborales que matcheen con el perfil del usuario. Se construye por pasos
chicos y probados, no de una sola vez.

## Antes de tocar nada

1. Leé **`docs/METODOLOGIA.md`** — cómo trabajamos — y seguila.
2. Leé **`docs/CONTEXTO.md`** — en qué estado está el repo hoy.
3. Si Elias te corrige algo sobre **cómo trabajar**, agregá esa corrección al
   registro de `docs/METODOLOGIA.md` en la misma respuesta.
4. Después de cada milestone que Elias dé por probado, actualizá `docs/CONTEXTO.md`.

## Lo esencial de la metodología

- **Pasos chicos.** Un paso = algo que se pueda probar a mano en pocos minutos.
- **Plan antes de código.** Qué archivos se tocan y qué va a quedar funcionando.
- **Si no se usa o no se pidió, no se pone. Si algo no sirve, se saca.** Vale para
  dependencias, properties, clases, métodos, archivos y tests. Nada "por las dudas".
- **Ante la duda, preguntar antes.** Si algo te parece importante y no te lo
  pidieron, se pregunta; no se agrega para explicarlo después.
- **Tests en el mismo paso.** Un paso sin tests no está terminado.
- **Prueba manual para cerrar.** Entregá los comandos exactos y qué tiene que
  verse; el paso cierra cuando Elias lo probó.
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
- Paquetes bajo `oneprofile.backend`, organizados por feature (`vacancy`,
  `profile`, `matching`), no por capa técnica.
- Sin credenciales ni secretos en el repo.
- Git lo maneja Elias: no inicializar, commitear ni pushear sin pedido explícito.
