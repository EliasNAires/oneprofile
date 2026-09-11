# Contexto — estado actual del repo

> Se actualiza después de cada milestone alcanzado (ver `docs/METODOLOGIA.md`).
> Describe lo que **hay hoy**, no lo que se planea.

**Última actualización:** 2026-09-10
**Último milestone:** Paso 2 — perfiles dev/prod, H2 andando en dev con hot
reload, `./mvnw test` en verde. Configuración reducida al mínimo.

## Qué es esto

Backend de `oneprofile`. El objetivo final es producir una buena lista de
vacantes laborales que matcheen con el perfil del usuario.

## Stack

- Java 25, Spring Boot 4.1.1 (generado con Spring Initializr).
- `spring-boot-starter-data-jpa`, `spring-boot-starter-webmvc`.
- **H2** (runtime) — base de desarrollo y de tests, en memoria, autoconfigurada.
- PostgreSQL, driver en scope `runtime` — pensado para prod, todavía sin configurar.
- `spring-boot-devtools` (runtime/optional) — hot reload.
- Tests: `spring-boot-starter-data-jpa-test`, `spring-boot-starter-webmvc-test`.
- Build con el wrapper: `./mvnw`.

## Configuración por perfiles

La configuración se mantiene **mínima**: solo va al archivo lo que el default de
Spring Boot no cubre. Hoy eso es una sola cosa.

`src/main/resources/application.properties` — común a todos los perfiles:

```properties
spring.application.name=backend
spring.profiles.default=dev
```

`src/main/resources/application-dev.properties` — **vacío**. Con H2 en el
classpath y sin datasource declarado, Spring Boot autoconfigura solo una base
H2 en memoria (`jdbc:h2:mem:<uuid>`), que es justo lo que queremos en dev.

`src/main/resources/application-prod.properties` — **vacío**. Prod todavía no se
usa; se completa cuando haga falta.

`src/test/resources/application.properties` — **vacío**, a propósito. Al existir
en `src/test/resources` tapa al archivo de `src/main/resources`, así que los
tests no activan el perfil dev. Es el lugar donde poner lo común a los tests
cuando aparezca.

Cosas que **no** hace falta declarar (las resuelve Spring Boot solo):

- El datasource de desarrollo — con H2 en el classpath levanta una base embebida
  en memoria sin configurar nada.
- `spring.datasource.driver-class-name` — lo deriva de la URL del datasource.
- Los flags de devtools (`restart.enabled`, `livereload.enabled`) — ya vienen
  activados por tener la dependencia.
- `spring.jpa.hibernate.ddl-auto` — con una base embebida usa `create-drop`.

**Consecuencia asumida:** en dev la base arranca vacía en cada arranque, y
también en cada hot reload, porque devtools cierra y recrea el contexto de Spring
y `create-drop` dropea el esquema al cerrarlo. Si más adelante molesta recargar
los datos a mano, ahí se decide (H2 en archivo + `ddl-auto=update`, o un cargador
de datos de prueba).

## Qué existe hoy

```
CLAUDE.md
docs/METODOLOGIA.md
docs/CONTEXTO.md
pom.xml
src/main/java/oneprofile/backend/BackendApplication.java
src/main/resources/application.properties
src/main/resources/application-dev.properties
src/main/resources/application-prod.properties
src/test/java/oneprofile/backend/BackendApplicationTests.java
src/test/resources/application.properties   (vacío)
```

- `BackendApplication` sigue siendo la clase `@SpringBootApplication` generada.
- `BackendApplicationTests.contextLoads` ahora **pasa** (antes fallaba por no
  haber datasource). Es el único test que hay.

## Estado verificado

- `./mvnw test` → 1 test, 0 fallas.
- `./mvnw spring-boot:run` → levanta en perfil `dev` ("falling back to 1 default
  profile: dev"), Tomcat en 8080, H2 en memoria, sin escribir nada en disco.
- Hot reload confirmado: al recompilar, devtools reinicia la app en ~0,4 s.

## Qué NO existe todavía

- Ninguna entidad de dominio (ni perfil, ni vacante).
- Ningún repositorio, servicio ni endpoint HTTP propio.
- Ninguna fuente de vacantes ni lógica de matching.
- Sin consola web de H2. Si alguna vez hace falta inspeccionar la base desde el
  navegador, requiere la dependencia `org.springframework.boot:spring-boot-h2console`
  (en Spring Boot 4 la consola vive en su propio módulo) más
  `spring.h2.console.enabled=true`; sin la dependencia, `/h2-console` da 404.
- Sin migraciones de esquema (Flyway/Liquibase).
- El perfil prod está vacío: hoy la app solo corre en dev.

## Puntos abiertos

- El repo **no está bajo git** todavía. Lo maneja Elias.
- `pom.xml` tiene la metadata (`name`, `description`, `url`, `licenses`,
  `developers`, `scm`) vacía, tal como la dejó el Initializr.
- El perfil prod está vacío y nunca se probó contra una base Postgres real.

## Próximos pasos (candidatos, sin priorizar — decide Elias)

- Modelar el perfil del usuario (`profile`): skills, seniority, tecnologías,
  preferencias de modalidad y ubicación.
- Modelar la vacante (`vacancy`) y persistirla.
- Cargar vacantes: primero a mano o desde archivo; después, desde una fuente real.
- Primer algoritmo de matching, simple, con tests sobre casos concretos.
- Endpoint HTTP para consultar las vacantes que matchean.
- Completar el perfil prod y elegir herramienta de migraciones cuando prod sea real.
