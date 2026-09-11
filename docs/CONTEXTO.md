# Contexto — estado actual del repo

> Se actualiza después de cada milestone alcanzado (ver `docs/METODOLOGIA.md`).
> Describe lo que **hay hoy**, no lo que se planea.

**Última actualización:** 2026-09-11
**Último milestone:** Paso 7 — el descubrimiento anda de punta a punta. Un POST al
endpoint consulta el índice de CommonCrawl, extrae los slugs de Greenhouse y los
guarda. Probado a mano por Elias en prod: **4.046 empresas cargadas** desde
`CC-MAIN-2026-34`. Con eso **terminó el plan de descubrimiento**, y
`docs/PLAN-DESCUBRIMIENTO.md` se borró.

**No hay ningún plan en curso.** El próximo paso se decide de cero; hay candidatos
al final de este documento.

## Qué es esto

Backend de `oneprofile`. El objetivo final es producir una buena lista de
vacantes laborales que matcheen con el perfil del usuario.

## El plan general

Para tener vacantes hace falta primero saber **qué empresas usan cada ATS**. Eso
se resuelve con un proceso de descubrimiento sobre **CommonCrawl**: se le pide al
índice todas las URLs que matcheen el patrón de board de un ATS
(`boards.greenhouse.io/*`) y de cada URL se extrae el identificador de la
empresa. Con ese identificador se le pega después a la API del ATS para traer las
vacantes. **Para Greenhouse esto ya está hecho y funcionando** (ver más abajo).

Como el descubrimiento es caro y el índice tiene límites de consulta, se corre a
mano cuando hace falta y el resultado queda en la base. Por eso vive en este mismo
repo (en Java, contra el esquema JPA) y no en archivos sueltos.

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
spring.jpa.properties.hibernate.jdbc.batch_size=50
```

`ddl-auto=validate` está porque el esquema ahora lo genera Flyway y no Hibernate:
`validate` no toca la base, solo compara las entidades contra las tablas reales y
**falla al arrancar** si alguien cambió una entidad y se olvidó la migración.

`batch_size=50` está por el descubrimiento, que inserta miles de empresas de una.
El 50 no es un número al azar: es el mismo `increment by 50` de `company_seq`, así
que cada lote de inserts se corresponde con un `nextval`.

`src/main/resources/application-dev.properties` — **vacío**. Con `compose.yaml` en
la raíz, `spring-boot-docker-compose` levanta el Postgres y deriva el datasource
del container: no hay nada que declarar.

`src/main/resources/application-prod.properties` — **vacío**, y se queda así: en
prod las credenciales entran por `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`, que
`prod/compose.yaml` le pasa como variables de entorno y Spring Boot bindea solo
por relaxed binding.

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

## Prod

Prod es una carpeta aparte, `prod/`, y se levanta entrando en ella:

```bash
cd prod
cp .env.example .env          # solo la primera vez, y después completarlo
docker compose up --build -d  # --build cada vez que cambie el código
docker compose logs -f app
docker compose down           # conserva los datos
docker compose down -v        # los borra
```

Está en una subcarpeta con el nombre `compose.yaml` a propósito: así el comando es
`docker compose up` pelado, sin `-f`. El nombre `compose.yaml` en la raíz no se
podía usar porque lo ocupa el de dev, que `spring-boot-docker-compose` busca ahí
por convención.

- **`Dockerfile`** (en la raíz, junto al código) — multi-stage:
  `eclipse-temurin:25-jdk` corre `./mvnw -DskipTests package`, y
  `eclipse-temurin:25-jre` solo copia el jar. Copia únicamente `.mvn`, `mvnw`,
  `pom.xml` y `src`, así que **`compose.yaml` no entra a la imagen** y el soporte
  de docker-compose queda inerte en prod sin desactivarlo por configuración.
  Los tests se saltean **porque usan Testcontainers**, que necesitaría un Docker
  disponible dentro del stage de build; se corren aparte con `./mvnw test`.
  La imagen de runtime instala **`curl`** por `apt-get`: es la única forma de
  pegarle al endpoint de descubrimiento, porque el puerto de la app no se publica
  y hay que entrar con `docker compose exec app`.
- **`prod/compose.yaml`** — `name: oneprofile-prod`, servicio `postgres` con
  volumen nombrado y servicio `app` con `build: context: ..` (el Dockerfile vive
  en la raíz) y `SPRING_PROFILES_ACTIVE=prod`. **Ningún puerto publicado**, ni el
  de la app ni el de la base.
- **`prod/.env.example`** — plantilla versionada con `POSTGRES_DB`,
  `POSTGRES_USER` y `POSTGRES_PASSWORD` vacías. El `prod/.env` real tiene los
  valores y **no va al repo** (`.env` está en `.gitignore`). Compose lo lee del
  directorio desde donde corrés el comando, por eso vive en `prod/`.

Tres detalles que costaron descubrir y no son obvios:

- **El volumen se monta en `/var/lib/postgresql`, no en `/var/lib/postgresql/data`.**
  Postgres 18 movió el `PGDATA` a `/var/lib/postgresql/18/docker` y declara el
  `VOLUME` en el directorio padre. Con el path viejo los datos **no persisten**.
- **`name: oneprofile-prod` no es cosmético.** Sin él, Compose deriva el nombre de
  proyecto del directorio y los dos stacks se pisan: un `up` de prod llegó a
  **recrear el container de Postgres de dev**.
- **`healthcheck` + `depends_on: condition: service_healthy`.** Con `depends_on`
  pelado la app arranca antes de que Postgres acepte conexiones y Flyway muere.

## Qué existe hoy

```
CLAUDE.md
compose.yaml
Dockerfile
prod/compose.yaml
prod/.env.example
docs/METODOLOGIA.md
docs/CONTEXTO.md
pom.xml
src/main/java/oneprofile/backend/BackendApplication.java
src/main/java/oneprofile/backend/model/Ats.java
src/main/java/oneprofile/backend/model/Company.java
src/main/java/oneprofile/backend/repository/CompanyRepository.java
src/main/java/oneprofile/backend/service/CommonCrawlIndexClient.java
src/main/java/oneprofile/backend/service/GreenhouseDiscoveryService.java
src/main/java/oneprofile/backend/controller/DiscoveryController.java
src/main/java/oneprofile/backend/util/GreenhouseBoardUrl.java
src/main/resources/application.properties
src/main/resources/application-dev.properties       (vacío)
src/main/resources/application-prod.properties      (vacío)
src/main/resources/db/migration/V1__create_company.sql
src/test/java/oneprofile/backend/BackendApplicationTests.java
src/test/java/oneprofile/backend/TestcontainersConfiguration.java
src/test/java/oneprofile/backend/repository/CompanyRepositoryTest.java
src/test/java/oneprofile/backend/service/CommonCrawlIndexClientTest.java
src/test/java/oneprofile/backend/service/GreenhouseDiscoveryServiceTest.java
src/test/java/oneprofile/backend/controller/DiscoveryControllerTest.java
src/test/java/oneprofile/backend/util/GreenhouseBoardUrlTest.java
src/test/resources/application.properties
```

### Organización de los paquetes

El código se organiza **por capa técnica**, no por feature: `model`, `repository`,
`service`, `controller` y `util` cuelgan directo de `oneprofile.backend`. Ya
existen las cinco.

`util` quedó reservado para **funciones puras sin dependencias** —hoy solo
`GreenhouseBoardUrl`—. Un componente que hace I/O va a la capa que le corresponde,
aunque sea un colaborador y no lógica de negocio: por eso `CommonCrawlIndexClient`
está en `service` y no en `util`.

### La clase `GreenhouseBoardUrl` (paquete `util`)

Clase final sin instancias con un solo método público,
`static Optional<String> slugFrom(String url)`. Es una **función pura, sin red y
sin Spring**: recibe una URL tal como la devuelve el índice de CommonCrawl y
devuelve el slug de la empresa, o vacío si esa URL no identifica a ninguna.

Cómo trabaja: matchea la URL contra
`^https://(?:www\.)?(?:job-)?boards\.greenhouse\.io/(.*)$` —una sola regex que
cubre los dos dominios de Greenhouse—, corta el fragment, separa path de query y
toma el primer segmento del path. Si ese segmento es `embed`, el slug sale del
parámetro `for` de la query. El candidato se valida contra `[A-Za-z0-9_-]+`.

Tres decisiones que no son obvias leyendo el código:

- **Solo `https`.** Decisión explícita de Elias: los boards de Greenhouse hoy son
  https y aceptar http agrega ruido. Quedaba la duda de si el CDX, que es agnóstico
  al esquema, traería capturas `http://` que se descartarían en silencio.
  **Ya se midió y no cuesta nada:** de las 76.765 capturas de `CC-MAIN-2026-34`,
  **las 76.765 son https**. La duda está cerrada.
- **Las URLs de iframe embebido sí cuentan.** `boards.greenhouse.io/embed/job_board?for=X`
  y `/embed/job_app?for=X&token=...` son una porción grande de las capturas —son
  el board metido en la página de carreras propia de la empresa— y su `for=` es el
  mismo identificador. Descartarlas perdería muchas empresas.
- **La validación `[A-Za-z0-9_-]+` es la que descarta la basura**, sin listas
  negras: rechaza el vacío (host pelado) y cualquier cosa con punto
  (`robots.txt`, `favicon.ico`), porque un slug nunca lleva punto.

### El descubrimiento: cliente, servicio y endpoint

Tres clases encadenadas que van del índice de CommonCrawl a filas en `company`.

**`service/CommonCrawlIndexClient`** habla con el índice (CDX). Su único método es
`forEachUrl(indexId, pattern, onUrl)`: pide primero `showNumPages=true` para saber
cuántas páginas hay y después recorre `page=0..N-1`, entregando cada URL a medida
que la lee. Detalles que importan:

- **Lee línea por línea** desde el `InputStream` de la respuesta, con `.exchange()`.
  No es una optimización opcional: una página son ~9 MB y ~12.000 líneas.
- **Reintenta con backoff** —hasta 4 intentos, esperando 2s, 4s y 8s— ante 5xx o
  error de red, y deja un `WARN` en cada reintento. El índice se satura y contesta
  504 de a ratos; sin esto, un solo 504 tira abajo una corrida de siete requests.
  **Un 4xx no se reintenta**: ahí el índice dice que la request está mal (índice
  inexistente, patrón sin capturas) y repetirla no cambia nada.
- **Timeouts explícitos** (10 s de connect, 2 min de read) para que una corrida
  colgada muera con excepción en vez de esperar para siempre.
- El patrón viaja **percent-encodeado** en la query (`boards.greenhouse.io%2F`) y
  el índice lo decodifica sin problema. Está verificado contra el servicio real.
- Tiene dos constructores: el público sin argumentos que usa Spring, y uno de
  paquete que recibe el `RestClient.Builder` y el delay del primer reintento. Ese
  segundo existe para el test: permite bindearle `MockRestServiceServer` y poner el
  delay en cero.

**`service/GreenhouseDiscoveryService.discover(indexId)`** vuelca los dos patrones
de `GreenhouseBoardUrl.indexPatterns()` en **un único `Set<String>`** (eso deduplica
las capturas repetidas y unifica los dos dominios), le resta lo que devuelve
`findSlugsByAts(GREENHOUSE)` y hace `saveAll` de los nuevos. Devuelve
`DiscoveryResult(slugsFound, newCompanies)`. Es **`@Transactional`**: sin eso cada
empresa se insertaría en su propia transacción, que son miles de round trips.

**`controller/DiscoveryController`** expone
`POST /admin/discovery/greenhouse?index=<id>`. El parámetro `index` es
**obligatorio a propósito**: un default hardcodeado envejecería solo y en silencio.
Contesta **202 al toque** y el trabajo corre en un executor de un solo hilo; un
`AtomicBoolean` da **409** si ya hay una corrida en curso. **El log es el único
canal de resultado**: el índice al arrancar, los números al terminar, y la
excepción si falla.

### Cómo se corre el descubrimiento

El puerto de la app no se publica, así que se entra al container:

```bash
cd prod
docker compose up --build -d
docker compose exec app curl -i -X POST \
  'localhost:8080/admin/discovery/greenhouse?index=CC-MAIN-2026-34'
docker compose logs -f app
```

Los índices disponibles salen de `https://index.commoncrawl.org/collinfo.json`.
**No todos los ids existen**: la numeración salta (`CC-MAIN-2026-26` no está), así
que hay que sacarlos de ahí y no inventarlos.

**Correrlo con varios índices es la forma de tener más empresas, y no necesita
código nuevo**: `discover` ya resta lo que está guardado, así que repetir el POST
cambiando el `index` solo agrega lo que ese crawl vio de más. Medido sobre los dos
últimos: agosto da 4.046 empresas, julio da 4.348, pero **solo 3.079 se repiten**;
julio suma **1.269 nuevas** y la unión de los dos da **5.315**. O sea que cada
índice extra aporta del orden de un 25-30% más.

### Las clases de company

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

`CompanyRepository` es un `JpaRepository<Company, Long>` con un solo método
propio:

```java
@Query("select c.slug from Company c where c.ats = :ats")
List<String> findSlugsByAts(Ats ats);
```

Devuelve **slugs y no entidades** a propósito: es lo único que el descubrimiento
necesita —restar de un `Set` lo que ya está guardado— y así no hidrata miles de
entidades en la sesión de JPA.

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

- `./mvnw test` → **28 tests, 0 fallas**: `BackendApplicationTests.contextLoads`,
  3 de `CompanyRepositoryTest`, 12 casos parametrizados de `GreenhouseBoardUrlTest`,
  6 de `CommonCrawlIndexClientTest`, 4 de `GreenhouseDiscoveryServiceTest` y 2 de
  `DiscoveryControllerTest`. Las dos primeras clases importan
  `TestcontainersConfiguration`, que declara un `PostgreSQLContainer` como
  `@Bean @ServiceConnection`; `CompanyRepositoryTest` lleva además
  `@AutoConfigureTestDatabase(replace = NONE)`, porque sin base embebida en el
  classpath `@DataJpaTest` falla al intentar reemplazar el datasource.
- Los tests corren contra **Postgres real y contra el esquema que creó Flyway**, no
  contra uno generado por Hibernate.
- **Prod anda de punta a punta.** `cd prod && docker compose up --build -d`
  construye la imagen, levanta los dos containers y la app arranca
  (`Started BackendApplication`) después de que Flyway aplica `V1`. Con un `down`
  y un `up` de nuevo, la fila insertada a mano sigue ahí y Flyway reporta
  `Current version of schema "public": 1` en vez de reaplicar la migración: el
  volumen nombrado persiste. Probado a mano por Elias.
- `CompanyRepositoryTest` prueba comportamiento, no anotaciones:
  1. `savesAndReadsBackAts` — guarda, hace flush y clear, relee por id y verifica
     que vuelven el mismo `ats` y el mismo `slug`.
  2. `rejectsSameSlugTwiceWithinAnAts` — al guardar dos veces el mismo par
     `(ats, slug)` salta `DataIntegrityViolationException`.
  3. `findsTheSlugsOfAnAts` — guarda dos empresas, flush y clear, y
     `findSlugsByAts` devuelve esos dos slugs.
     **Limitación conocida:** hoy no se puede probar que el método *filtra* por
     ATS, porque `Ats` tiene un solo valor y agregar uno falso sería meter algo
     que nadie pidió. Esa assertion se suma cuando entre el segundo ATS.
- **`CommonCrawlIndexClientTest`** usa `MockRestServiceServer`, así que verifica
  comportamiento real sin tocar la red: recorre las dos páginas que el índice dice
  tener, no pide ninguna si dice cero, saltea líneas vacías o sin `url`, se recupera
  de un 504 y de un 503, se rinde después del cuarto intento, y ante un 404 falla al
  toque **sin reintentar**.
- **`GreenhouseDiscoveryServiceTest`** es `@DataJpaTest` con un doble del cliente
  escrito a mano (una subclase que devuelve URLs de mentira): guarda una sola empresa
  por slug por más capturas que tenga, trata los dos dominios como la misma empresa,
  no reinserta las que ya estaban, e ignora las URLs que no identifican a ninguna.
- **`DiscoveryControllerTest`** es `@WebMvcTest`: 202 y delegación en el servicio, y
  409 cuando ya hay una corrida en curso. El doble del servicio se bloquea en un
  `CountDownLatch` para que la segunda request llegue con la primera todavía viva.
- **Ningún test le pega a CommonCrawl de verdad.**
- **El descubrimiento anda de punta a punta en prod.** Un POST con
  `index=CC-MAIN-2026-34` devolvió 202 al toque y terminó con
  `4046 slugs found, 4046 new companies saved`. Probado a mano por Elias.
  Ese número **se verificó aparte**, bajando las 7 páginas del índice y corriendo
  las mismas reglas fuera de la app: 76.765 capturas → exactamente 4.046 slugs
  distintos. La app no está perdiendo nada; 4.046 es lo que ese crawl contiene.
  Solo 229 de las 76.765 URLs no dan slug, y son las que corresponde descartar.
- `GreenhouseBoardUrlTest` no levanta contexto de Spring y corre en ~40 ms. Son dos
  `@ParameterizedTest`: 7 URLs que devuelven slug (path extra, query string, los
  dos dominios, `www.`, y las dos formas de `embed`) y 5 que devuelven vacío
  (`embed` sin `for`, `robots.txt`, host pelado, otro host, y **`http://`**, que
  documenta la decisión de aceptar solo https).
- `./mvnw spring-boot:run` → levanta el container `backend-postgres-1`, Flyway
  aplica `V1`, Hibernate valida el esquema y Tomcat arranca en 8080. Verificado en
  la base: tabla `company` con `company_pkey` y `company_ats_slug_key`, secuencia
  `company_seq`, y `flyway_schema_history` con `1 | create company | t`.

## Qué NO existe todavía

- Ningún cliente de la API de Greenhouse: de las empresas se sabe el slug y nada
  más. `Company` sigue sin campo `name`.
- Ninguna vacante, ningún perfil de usuario, ninguna lógica de matching.
- Ningún endpoint que devuelva datos: el único que hay dispara el descubrimiento.
- Ningún ATS además de Greenhouse.

## Puntos abiertos

- **El endpoint de descubrimiento no tiene ninguna protección.** Hoy no importa
  porque el puerto de la app no se publica, pero cuando exista el endpoint de
  vacantes habrá que publicarlo y ahí éste queda expuesto. Se decide en ese momento.
- **Una respuesta truncada del índice se aceptaría en silencio.** Bajando páginas a
  mano pasó tres veces que el índice cerró la conexión limpio con un cuerpo corto
  (256 KB o 560 KB en vez de ~9 MB) y HTTP 200. El cliente leería esas líneas, no
  vería ningún error y reportaría menos empresas sin avisar. Los reintentos no
  ayudan, porque el status es 200. En la corrida real no pasó —el conteo dio
  exactamente el mismo número que el cálculo offline completo— pero el agujero está.
- `pom.xml` tiene la metadata (`name`, `description`, `url`, `licenses`,
  `developers`, `scm`) vacía, tal como la dejó el Initializr.
- En la máquina de Elias, `docker-rootless-extras` quedó en 29.8.0 y `docker` en
  29.7.2 (actualización parcial). Funciona; se empareja en el próximo `pacman -Syu`.

## Candidatos para el próximo paso (sin priorizar)

- Correr el descubrimiento con más índices de CommonCrawl para engordar la tabla.
  No necesita código: es repetir el POST cambiando el `index`.
- Cliente de la API de Greenhouse (`boards-api.greenhouse.io/v1/boards/<slug>/jobs`),
  que es donde aparece el nombre legible de la empresa y las vacantes. Agregar
  `name` a `Company` con su migración `V2`.
- Modelar la vacante (`vacancy`), con relación unidireccional `Vacancy → Company`.
- Modelar el perfil del usuario (`profile`).
- Primer algoritmo de matching, simple, con tests sobre casos concretos.
- Endpoint HTTP para consultar las vacantes que matchean.
