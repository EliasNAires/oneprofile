# Contexto — estado actual del repo

> Se actualiza después de cada milestone alcanzado (ver `docs/METODOLOGIA.md`).
> Describe lo que **hay hoy**, no lo que se planea.

**Última actualización:** 2026-09-11
**Último milestone:** Paso 4 — se voló H2; la app corre sobre PostgreSQL real
(container levantado por `spring-boot-docker-compose` en dev, Testcontainers en
tests) y el esquema lo maneja **Flyway**. `./mvnw test` en verde con 3 tests.

> **Hay un plan en curso, aprobado y a medio ejecutar:** `docs/PLAN-DESCUBRIMIENTO.md`.
> Contiene los **Pasos 5, 6 y 7** (prod con `docker-compose up`, extracción de
> slugs, y el script de descubrimiento sobre CommonCrawl con su endpoint).
> **El próximo paso es el 5.** Ese archivo se borra cuando el plan termine.

## Qué es esto

Backend de `oneprofile`. El objetivo final es producir una buena lista de
vacantes laborales que matcheen con el perfil del usuario.

## El plan general (decidido, todavía no implementado)

Para tener vacantes hace falta primero saber **qué empresas usan cada ATS**. Eso
se resuelve con un proceso de descubrimiento sobre **CommonCrawl**: se le pide al
índice todas las URLs que matcheen el patrón de board de un ATS
(`boards.greenhouse.io/*`) y de cada URL se extrae el identificador de la
empresa. Con ese identificador se le pega después a la API del ATS para traer las
vacantes.

Como el descubrimiento es caro y el índice tiene límites de consulta, la idea es
correrlo **una sola vez por ATS** y guardar el resultado en la base. Por eso el
script vive en este mismo repo (en Java, contra el esquema JPA) y no en archivos
sueltos.

Hechos que condicionan el diseño y conviene no olvidar:

- **El JSON de job postings NO es estándar entre ATS.** Greenhouse, Lever, Ashby,
  Workable y SmartRecruiters tienen cada uno su propio formato propietario. Lo
  único semi-estandarizado es el JSON-LD `schema.org/JobPosting` que algunos
  boards embeben en el HTML, y es opcional. Consecuencia: hace falta **un cliente
  por ATS** que mapee a un modelo canónico propio.
- **La ubicación viene como texto libre** en Greenhouse y Lever
  (`"Buenos Aires, Argentina"`, `"Remote - LATAM"`). No hay campo `country`
  confiable, así que filtrar por Argentina es una heurística sobre strings.
- **Decisión tomada:** el descubrimiento guarda **todas** las empresas del ATS, no
  solo las que hoy tienen vacantes en Argentina. Una empresa sin vacantes AR hoy
  puede tenerlas mañana, y descartarla obligaría a volver a pegarle a CommonCrawl.
  El filtro por país es una consulta sobre las vacantes, no un descarte en la carga.
- **Greenhouse tiene dos dominios de board** —`boards.greenhouse.io/<slug>` y
  `job-boards.greenhouse.io/<slug>`— que son el **mismo ATS**: mismo slug y misma
  API de vacantes. Son dos patrones de URL de `Ats.GREENHOUSE`, no dos ATS.

## Qué es un slug

El identificador corto de la empresa dentro de la URL de su board:
`boards.greenhouse.io/mercadolibre` → slug `mercadolibre`. Es lo único que el
índice de CommonCrawl provee (el nombre legible no aparece ahí, sale recién al
consultar la API del ATS), y con él se arma la URL del endpoint de vacantes:
`boards-api.greenhouse.io/v1/boards/mercadolibre/jobs`.

**No es único globalmente, solo dentro de su ATS.** El mismo slug puede existir en
Greenhouse y en Lever siendo empresas distintas. Por eso la identidad de una
empresa en este sistema es el par **(ats, slug)**.

## Stack

- Java 25, Spring Boot 4.1.1 (generado con Spring Initializr).
- `spring-boot-starter-data-jpa`, `spring-boot-starter-webmvc`.
- **PostgreSQL** (driver en `runtime`) — la única base, en dev, tests y prod.
- **`spring-boot-docker-compose`** (runtime/optional) — levanta el Postgres de dev.
- **`spring-boot-starter-flyway`** + **`flyway-database-postgresql`** — migraciones.
- `spring-boot-devtools` (runtime/optional) — hot reload.
- Tests: `spring-boot-starter-data-jpa-test`, `spring-boot-starter-webmvc-test`,
  **`spring-boot-testcontainers`** y **`org.testcontainers:testcontainers-postgresql`**.
- Build con el wrapper: `./mvnw`.

**Trampas de Spring Boot 4 que ya nos costaron tiempo:**

- Los paquetes de test se modularizaron respecto de Boot 3. Van
  `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`,
  `org.springframework.boot.jpa.test.autoconfigure.TestEntityManager` y
  `org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase`
  (en Boot 3 los dos primeros vivían en `org.springframework.boot.test.autoconfigure.orm.jpa`).
- **`flyway-core` solo no alcanza.** La autoconfiguración de Flyway vive en el
  módulo `spring-boot-flyway`, que `flyway-core` no arrastra: con solo
  `flyway-core`, Flyway **no corre y no avisa**. Por eso va el starter
  `spring-boot-starter-flyway`, que trae los dos.
- **`flyway-database-postgresql` es obligatorio.** Sin él, Flyway falla al arrancar
  con `Unsupported Database: PostgreSQL 18.6`. Verificado sacándolo.
- **Testcontainers acá es 2.0.5**, que renombró los artefactos: es
  `org.testcontainers:testcontainers-postgresql` (no `postgresql`) y la clase es
  `org.testcontainers.postgresql.PostgreSQLContainer`.
- No hace falta `testcontainers-junit-jupiter`: con el container declarado como
  `@Bean @ServiceConnection`, el ciclo de vida lo maneja Spring Boot. Tampoco hace
  falta `spring-boot-starter-flyway-test`: el starter principal ya mete Flyway en
  el slice de `@DataJpaTest`. Las dos se probaron y se sacaron.

## El entorno: Docker rootless

La máquina de Elias corre **Docker en modo rootless**, porque su usuario no está
en el grupo `docker` (y el grupo `docker` equivale a root). Se configuró así:

- Paquetes de Arch: `docker` + `docker-rootless-extras`. **El paquete de Arch no
  trae el `dockerd-rootless-setuptool.sh` de upstream**: trae directamente las
  unidades de systemd de usuario, así que el setup fue
  `systemctl --user enable --now docker.socket` y `systemctl --user start docker.service`.
- Contexto `rootless` apuntando a `unix:///run/user/1000/docker.sock`, seleccionado
  con `docker context use rootless`. Linger habilitado para el usuario.
- Sin `slirp4netns` ni `fuse-overlayfs`: usa `overlayfs` nativo del kernel.
- Consecuencia: **las imágenes y volúmenes viven en `~/.local/share/docker`** y no
  se comparten con ningún daemon rootful. Publicar puertos ≥1024 funciona normal.

Tanto Testcontainers como `spring-boot-docker-compose` lo detectan solos.

## Configuración por perfiles

La configuración se mantiene **mínima**: solo va al archivo lo que el default de
Spring Boot no cubre.

`src/main/resources/application.properties` — común a todos los perfiles:

```properties
spring.application.name=backend
spring.profiles.default=dev
spring.jpa.hibernate.ddl-auto=validate
```

`ddl-auto=validate` está porque el esquema ahora lo genera Flyway y no Hibernate:
`validate` no toca la base, solo compara las entidades contra las tablas reales y
**falla al arrancar** si alguien cambió una entidad y se olvidó la migración.

`src/main/resources/application-dev.properties` — **vacío**. Con `compose.yaml` en
la raíz, `spring-boot-docker-compose` levanta el Postgres y deriva el datasource
del container: no hay nada que declarar.

`src/main/resources/application-prod.properties` — **vacío**. Cuando prod sea real
las credenciales entran por `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`, que Spring
Boot bindea solo por relaxed binding.

`src/test/resources/application.properties`:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Este archivo existe para **tapar** al de `src/main/resources`, así que los tests no
activan el perfil `dev`; como efecto colateral tampoco heredaban `validate`, y por
eso la línea está repetida acá. Se verificó que sirve: al renombrar una columna en
`V1`, los 3 tests fallan por schema validation.

`compose.yaml` (raíz) — un servicio `postgres:18-alpine`, **sin volumen**, con el
5432 publicado y usuario/base/password `oneprofile`. Son credenciales de una base
local y efímera, Elias las dio por aceptables en el repo.

**Ojo con el ciclo de vida:** al apagar la app, Spring Boot hace `docker compose
stop`, **no `down`**. El container se conserva con sus datos hasta que corras
`docker compose down` a mano.

## Qué existe hoy

```
CLAUDE.md
compose.yaml
docs/METODOLOGIA.md
docs/CONTEXTO.md
docs/PLAN-DESCUBRIMIENTO.md                        (plan en curso)
pom.xml
src/main/java/oneprofile/backend/BackendApplication.java
src/main/java/oneprofile/backend/company/Ats.java
src/main/java/oneprofile/backend/company/Company.java
src/main/java/oneprofile/backend/company/CompanyRepository.java
src/main/resources/application.properties
src/main/resources/application-dev.properties       (vacío)
src/main/resources/application-prod.properties      (vacío)
src/main/resources/db/migration/V1__create_company.sql
src/test/java/oneprofile/backend/BackendApplicationTests.java
src/test/java/oneprofile/backend/TestcontainersConfiguration.java
src/test/java/oneprofile/backend/company/CompanyRepositoryTest.java
src/test/resources/application.properties
```

### El paquete `company`

`Ats` es un **enum**, no una entidad. Se decidió así porque es un conjunto cerrado
que define el código y no cargan usuarios: da chequeo del compilador y `switch`
exhaustivo cuando se sumen ATS. La flexibilidad de una tabla sería ilusoria,
porque cada ATS necesita igual su propia clase para parsear su JSON, así que un
INSERT no evitaría recompilar. Hoy tiene **un solo valor, `GREENHOUSE`**, y
ningún campo ni método.

`Company` tiene `id`, `ats` y `slug`, con `@UniqueConstraint` sobre el par
`(ats, slug)`. Constructor sin argumentos `protected` para JPA más uno público
`(Ats, String)`, y getters sin setters. El enum se mapea con
`@Enumerated(EnumType.STRING)`.

**No tiene campo `name` a propósito**: el índice de CommonCrawl da el slug pero no
el nombre legible, así que el nombre se agrega en el paso donde se consulte la API
del ATS, que es cuando el dato existe.

`CompanyRepository` es un `JpaRepository<Company, Long>` pelado, sin métodos
propios.

### Migraciones

El esquema vive en `src/main/resources/db/migration/` y lo aplica Flyway, que
anota lo ya corrido en la tabla `flyway_schema_history`. **Cuando cambie el
esquema no se reescribe `V1`: se agrega `V2` con el `ALTER TABLE`**, y los datos
existentes sobreviven. Hoy hay una sola migración:

```sql
-- V1__create_company.sql
create sequence company_seq start with 1 increment by 50;

create table company (
    id bigint not null,
    ats varchar(255),
    slug varchar(255),
    primary key (id),
    unique (ats, slug)
);
```

El `increment by 50` no es decorativo: es el `allocationSize` por defecto que
Hibernate 6/7 espera para `@GeneratedValue`, y si no coincide `validate` falla.

## Estado verificado

- `./mvnw test` → **3 tests, 0 fallas**: `BackendApplicationTests.contextLoads` más
  los dos de `CompanyRepositoryTest`. Ambas clases importan
  `TestcontainersConfiguration`, que declara un `PostgreSQLContainer` como
  `@Bean @ServiceConnection`; `CompanyRepositoryTest` lleva además
  `@AutoConfigureTestDatabase(replace = NONE)`, porque sin base embebida en el
  classpath `@DataJpaTest` falla al intentar reemplazar el datasource.
- Los tests corren contra **Postgres real y contra el esquema que creó Flyway**, no
  contra uno generado por Hibernate.
- `CompanyRepositoryTest` prueba comportamiento, no anotaciones:
  1. `savesAndReadsBackAts` — guarda, hace flush y clear, relee por id y verifica
     que vuelven el mismo `ats` y el mismo `slug`.
  2. `rejectsSameSlugTwiceWithinAnAts` — al guardar dos veces el mismo par
     `(ats, slug)` salta `DataIntegrityViolationException`.
- `./mvnw spring-boot:run` → levanta el container `backend-postgres-1`, Flyway
  aplica `V1`, Hibernate valida el esquema y Tomcat arranca en 8080. Verificado en
  la base: tabla `company` con `company_pkey` y `company_ats_slug_key`, secuencia
  `company_seq`, y `flyway_schema_history` con `1 | create company | t`.

## Qué NO existe todavía

- **El script de descubrimiento sobre CommonCrawl** y todo el paquete `discovery`.
- Ningún cliente de API de ATS, ninguna vacante, ninguna lógica de matching.
- Ningún servicio ni endpoint HTTP propio.
- Ninguna forma de cargar empresas: la tabla `company` existe pero arranca vacía.
- **Prod no existe**: no hay `Dockerfile` ni `compose.prod.yaml`, y el perfil prod
  nunca se probó. Es el Paso 5 del plan en curso.

## Puntos abiertos

- `pom.xml` tiene la metadata (`name`, `description`, `url`, `licenses`,
  `developers`, `scm`) vacía, tal como la dejó el Initializr.
- En la máquina de Elias, `docker-rootless-extras` quedó en 29.8.0 y `docker` en
  29.7.2 (actualización parcial). Funciona; se empareja en el próximo `pacman -Syu`.
