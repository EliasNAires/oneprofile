# Contexto — estado actual del repo

> Se actualiza después de cada milestone alcanzado (ver `docs/METODOLOGIA.md`).
> Describe lo que **hay hoy**, no lo que se planea.

> **`docs/para-humanos/` no la leas.** Es documentación para personas: una versión
> resumida y con diagramas de lo que este archivo ya cuenta en detalle. Está
> duplicada, así que leerla gasta contexto en información repetida y te arriesga a
> trabajar sobre el resumen en vez de sobre la fuente de verdad, que es este
> documento. Se abre solo si Elias pide explícitamente escribir o actualizar esa
> carpeta.

**Última actualización:** 2026-09-13
**Último milestone probado:** la **tabla `normalized_vacancy`**, que guarda lo que dicen
los tres extractores del título. Corrió en prod el 2026-09-13 sobre las **128.953 vacantes
en 24 segundos**, y los números coinciden con lo medido antes de escribir las reglas: los
títulos distintos bajan de 87.647 a **77.630 (−11,4%)**, **32.219 vacantes tienen
seniority** y **19.255 declaran modalidad**.

**Milestone anterior:** los **tres extractores del título** —`TitleCleaner`,
`SeniorityExtractor` y `WorkModeExtractor`—, funciones puras que limpian el título y le
sacan el seniority y la modalidad a campos propios.

**El recorrido masivo de vacantes terminó.** Corrió entero en el servidor y dejó
**128.953 vacantes sobre 3.118 empresas**. El número importa porque **desmiente la
proyección**: se esperaban ~250.000, o sea el doble. La media de la muestra (80 vacantes
por empresa) tiraba para arriba; la mediana (17) era la guía correcta.

**La normalización de los títulos terminó** y su plan se cerró el 2026-09-13. Lo que vale
la pena conservar de él —las reglas, las decisiones de modelo y lo que quedó sin resolver—
está en este documento. Lo único pendiente es revisar los falsos positivos de la modalidad
(ver "Puntos abiertos"). La medición anterior
de la tabla `vacancy`, sobre departamento e idioma, vive aparte en
`docs/MEDICION-VACANTES.md`.

**Hay un segundo plan, independiente: traer a prod todos los slugs de Greenhouse**, en
`docs/PLAN-SLUGS.md`. Salió de medir por qué había solo 4.046 empresas, con los números
en `docs/MEDICION-SLUGS.md`: el descubrimiento **no pierde nada de lo que lee**, pero
**un solo índice de CommonCrawl ve una fracción**. Dieciséis índices juntan 10.086 slugs,
y la **Wayback Machine** trae 17.730, 8.035 de ellos en ningún índice de CommonCrawl. En
la medición ya se corrieron cuatro índices más en prod, así que **`company` tiene 6.988
filas, 2.942 sin sondear**. De ese plan ya está escrito, con tests en verde, el
**descubrimiento sobre los 10 índices más recientes de CommonCrawl** (y el reordenamiento
que lo precedió: paquete `client` y `util/HttpRetry`), pero **todavía no se probó en prod**.
**El crawling de Wayback no está hecho**: ni cliente, ni endpoint (ver "Puntos abiertos").

## Qué es esto

Backend de `oneprofile`. El objetivo final es producir una buena lista de
vacantes laborales que matcheen con el perfil del usuario.

## El plan general

Para tener vacantes hace falta primero saber **qué empresas usan cada ATS**. Eso
se resuelve con un proceso de descubrimiento sobre **CommonCrawl**: se le pide al
índice todas las URLs que matcheen el patrón de board de un ATS
(`boards.greenhouse.io/*`) y de cada URL se extrae el identificador de la
empresa. Con ese identificador se le pega después a la API del ATS para traer las
vacantes. **Para Greenhouse el descubrimiento y el sondeo de boards ya están hechos
y funcionando** (ver más abajo); lo que falta son las vacantes.

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
- **CommonCrawl es una foto vieja.** Que el índice haya visto un board no quiere
  decir que la empresa siga en Greenhouse hoy: hay slugs guardados que ya dan 404.
  Por eso existe el sondeo, que es lo que separa lo que sigue vivo de lo que no.

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

**Prod corre en un servidor de Elias, no en su máquina.** Se llega por SSH a través de
una VPN de ZeroTier. El servidor no compila nada: baja una imagen ya construida desde
GHCR. Ahí el ciclo es:

```bash
cd ~/oneprofile
docker compose pull           # trae la ultima imagen publicada
docker compose up -d          # recrea solo el container de la app
docker compose logs -f app
docker compose down           # conserva los datos
docker compose down -v        # los borra
```

El servidor tiene **dos archivos y nada más**: `compose.yaml` y `.env`, copiados por
`scp`. No tiene el repo, ni Java, ni Maven. Se descartó clonar el repo ahí justamente
por eso: no usaría el código para nada. El costo asumido es que cuando cambie el
`compose.yaml` hay que volver a copiarlo a mano, que es casi nunca.

En el repo, prod vive en la carpeta `prod/` con el nombre `compose.yaml` a propósito:
así el comando es `docker compose up` pelado, sin `-f`. El nombre `compose.yaml` en la
raíz no se podía usar porque lo ocupa el de dev, que `spring-boot-docker-compose` busca
ahí por convención.

**En la máquina de Elias ya no se levanta prod.** El servicio `app` declara `image:` y
no tiene `build:`, así que `docker compose up --build` en `prod/` no construye nada:
para probar local está dev.

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
  volumen nombrado y servicio `app` con
  `image: ghcr.io/eliasnaires/oneprofile-backend:latest` y
  `SPRING_PROFILES_ACTIVE=prod`. **Ningún puerto publicado**, ni el de la app ni el de
  la base.
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

## El pipeline: de un commit a la imagen

`.github/workflows/publish.yml` se dispara en cada **push a `main`** y tiene dos jobs:

1. **`test`** — `actions/checkout@v5`, `actions/setup-java@v5` (temurin 25, `cache: maven`)
   y `mvn -B test`. Si falla, se corta acá y no se publica nada.
2. **`publish`** — con `needs: test` y `permissions: packages: write`. Loguea a `ghcr.io`
   con `${{ github.actor }}` y el `GITHUB_TOKEN` que Actions ya provee —**no hay ningún
   secret creado a mano**— y publica con `docker/build-push-action`.

La imagen es **`ghcr.io/eliasnaires/oneprofile-backend:latest`**, **pública**, y lleva la
label `org.opencontainers.image.source`. Esa label no es cosmética: es lo que vincula el
paquete al repo, y de esa vinculación sale el permiso del token sobre el paquete. Un
paquete nuevo de GHCR **nace privado**: hay que marcarlo público una vez desde la web,
o el servidor no puede bajarlo sin credenciales.

**Un solo tag, `latest`.** No hay tag por SHA, así que hoy no se puede volver a una
versión anterior; se agrega cuando haga falta.

**El job de tests usa `mvn`, no `./mvnw`, y es a propósito.** La primera corrida del
pipeline falló así, en 0 segundos:

```
wget: Failed to fetch https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.16/apache-maven-3.9.16-bin.zip
```

El wrapper está en `distributionType=only-script`: no hay jar versionado, así que cada
corrida limpia tiene que bajarse Maven entero. En la máquina de Elias no se nota porque
ya está en `~/.m2/wrapper/dists/`. La URL es válida —devuelve 200 y 9,4 MB desde una
conexión normal— y el runner tiene `wget`, así que lo que pasa es que la descarga la
cortan del otro lado desde las IPs de los runners. Como `mvnw` corre `wget` en modo
silencioso, el log no dice el código de error.

La salida fue usar el Maven que la imagen `ubuntu-24.04` **ya trae instalado**, que es
**la misma 3.9.16** que declara `.mvn/wrapper/maven-wrapper.properties`. La contra
asumida: si GitHub actualiza la imagen, CI podría correr una 3.9.x distinta de la del
wrapper. Si algún día importa, la salida es fijar la versión con una acción que instale
Maven, **no** volver al wrapper. Cachear `~/.m2/wrapper` no servía: la caché arranca
vacía y la primera corrida intentaría la misma descarga.

**Los tests corren en CI aunque el `Dockerfile` los saltee**, y no es contradictorio:
adentro del build de la imagen no hay un Docker para que Testcontainers levante Postgres,
y en el runner sí. Por eso van en un job aparte, antes y por fuera de la imagen.

**El despliegue al servidor es a mano** (`docker compose pull && docker compose up -d`).
Que un push a `main` reinicie prod solo es una decisión que no se tomó.

### La mudanza de los datos al servidor (hecha, 2026-09-11)

Las 4.046 empresas se mudaron con un dump, no se rehicieron: el descubrimiento y el
sondeo habían costado horas. El orden importa y quedó verificado: **la base se restaura
con la app apagada**, porque el dump trae las tablas *y* `flyway_schema_history`; si la
app arranca primero, crea el esquema vacío y el restore choca.

```bash
# en la maquina de Elias
docker compose exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB"' > ~/oneprofile.sql
# en el servidor, con la app sin levantar
docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < ~/oneprofile.sql
```

**Un detalle real de esta mudanza:** el prod de la máquina de Elias estaba en **V2** y no
tenía la tabla `vacancy` —las pruebas de vacantes se habían hecho en dev—, así que el dump
llevó las empresas y el historial hasta V2, y al levantar la app en el servidor **Flyway
aplicó `V3` y creó `vacancy` vacía**. Es el comportamiento correcto, pero desmiente la
expectativa de que Flyway no iba a aplicar nada: aplica lo que al dump le falta.

El `.env` tiene que ser el **mismo** de los dos lados: si el usuario de la base no
coincide, el restore se queja de dueños que no existen.

## Qué existe hoy

```
CLAUDE.md
compose.yaml
Dockerfile
.github/workflows/publish.yml                       (el pipeline: tests + imagen a GHCR)
prod/compose.yaml
prod/.env.example
docs/METODOLOGIA.md
docs/CONTEXTO.md
docs/MEDICION-VACANTES.md                           (los números medidos el 2026-09-12)
docs/MEDICION-SLUGS.md                              (cobertura del descubrimiento, medida el 2026-09-13)
docs/PLAN-SLUGS.md                                  (plan para traer todos los slugs; se borra al terminarlo)
mediciones/primera-normalizacion-13-09-2026/*.txt   (salidas crudas historicas; no se leen salvo que un plan apunte a una)
docs/para-humanos/README.md                         (para personas, no para agentes)
docs/para-humanos/descubrimiento.md
docs/para-humanos/sondeo.md
docs/para-humanos/vacantes.md
docs/para-humanos/normalizacion.md
docs/para-humanos/despliegue.md
docs/para-humanos/diagramas/*.puml + *.svg          (8 diagramas PlantUML)
pom.xml
src/main/java/oneprofile/backend/BackendApplication.java
src/main/java/oneprofile/backend/model/Ats.java
src/main/java/oneprofile/backend/model/BoardStatus.java
src/main/java/oneprofile/backend/model/Company.java
src/main/java/oneprofile/backend/model/Vacancy.java
src/main/java/oneprofile/backend/model/Seniority.java
src/main/java/oneprofile/backend/model/WorkMode.java
src/main/java/oneprofile/backend/model/NormalizedVacancy.java
src/main/java/oneprofile/backend/repository/CompanyRepository.java
src/main/java/oneprofile/backend/repository/VacancyRepository.java
src/main/java/oneprofile/backend/repository/NormalizedVacancyRepository.java
src/main/java/oneprofile/backend/client/CommonCrawlIndexClient.java
src/main/java/oneprofile/backend/client/GreenhouseBoardClient.java
src/main/java/oneprofile/backend/service/GreenhouseDiscoveryService.java
src/main/java/oneprofile/backend/service/GreenhouseBoardProbeService.java
src/main/java/oneprofile/backend/service/GreenhouseVacancySyncService.java
src/main/java/oneprofile/backend/service/GreenhouseVacancySweepService.java
src/main/java/oneprofile/backend/service/VacancyNormalizationService.java
src/main/java/oneprofile/backend/controller/DiscoveryController.java
src/main/java/oneprofile/backend/controller/BoardProbeController.java
src/main/java/oneprofile/backend/controller/VacancyController.java
src/main/java/oneprofile/backend/controller/NormalizationController.java
src/main/java/oneprofile/backend/util/GreenhouseBoardUrl.java
src/main/java/oneprofile/backend/util/HtmlToText.java
src/main/java/oneprofile/backend/util/HttpRetry.java
src/main/java/oneprofile/backend/util/TitleCleaner.java
src/main/java/oneprofile/backend/util/SeniorityExtractor.java
src/main/java/oneprofile/backend/util/WorkModeExtractor.java
src/main/resources/application.properties
src/main/resources/application-dev.properties       (vacío)
src/main/resources/application-prod.properties      (vacío)
src/main/resources/db/migration/V1__create_company.sql
src/main/resources/db/migration/V2__add_company_board_status.sql
src/main/resources/db/migration/V3__create_vacancy.sql
src/main/resources/db/migration/V4__create_normalized_vacancy.sql
src/test/java/oneprofile/backend/BackendApplicationTests.java
src/test/java/oneprofile/backend/TestcontainersConfiguration.java
src/test/java/oneprofile/backend/repository/CompanyRepositoryTest.java
src/test/java/oneprofile/backend/repository/VacancyRepositoryTest.java
src/test/java/oneprofile/backend/client/CommonCrawlIndexClientTest.java
src/test/java/oneprofile/backend/client/GreenhouseBoardClientTest.java
src/test/java/oneprofile/backend/service/GreenhouseDiscoveryServiceTest.java
src/test/java/oneprofile/backend/service/GreenhouseBoardProbeServiceTest.java
src/test/java/oneprofile/backend/service/GreenhouseVacancySyncServiceTest.java
src/test/java/oneprofile/backend/service/GreenhouseVacancySweepServiceTest.java
src/test/java/oneprofile/backend/service/VacancyNormalizationServiceTest.java
src/test/java/oneprofile/backend/controller/DiscoveryControllerTest.java
src/test/java/oneprofile/backend/controller/BoardProbeControllerTest.java
src/test/java/oneprofile/backend/controller/VacancyControllerTest.java
src/test/java/oneprofile/backend/controller/NormalizationControllerTest.java
src/test/java/oneprofile/backend/util/GreenhouseBoardUrlTest.java
src/test/java/oneprofile/backend/util/HtmlToTextTest.java
src/test/java/oneprofile/backend/util/HttpRetryTest.java
src/test/java/oneprofile/backend/util/TitleCleanerTest.java
src/test/java/oneprofile/backend/util/SeniorityExtractorTest.java
src/test/java/oneprofile/backend/util/WorkModeExtractorTest.java
src/test/resources/application.properties
```

### Organización de los paquetes

El código se organiza **por capa técnica**, no por feature: `model`, `repository`,
`service`, `controller`, `client` y `util` cuelgan directo de `oneprofile.backend`.
Existen las seis.

**`client`** guarda las clases que hablan con un sistema externo, **una por proveedor**:
hoy `CommonCrawlIndexClient` y `GreenhouseBoardClient`. Se acordó el 2026-09-13 junto con
el criterio de diseño de `METODOLOGIA.md` —se separa por motivo de cambio—: dos
proveedores son dos clients aunque se parezcan, y un caso de uso es un service aunque
tenga varias variantes. Se llama `client` y no `component` porque en Spring todo es un
`@Component` y ese nombre no diría qué hay adentro.

`util` guarda **helpers sin estado y sin Spring** —hoy `GreenhouseBoardUrl`,
`HtmlToText`, `TitleCleaner`, `SeniorityExtractor`, `WorkModeExtractor` y `HttpRetry`—.
Un util existe solo si lo usa más de un lugar: `HttpRetry` entró porque los reintentos
estaban copiados en los dos clients.

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

**`util/HttpRetry`** es lo que los clients hacen igual: `call(what, supplier)` reintenta
5xx y errores de red con backoff exponencial y un `WARN` por reintento, y **no reintenta
ningún 4xx**; `errorFor(status, text)` arma la excepción que hace falta con
`.exchange()`; y `requestFactory(connect, read)` fija los timeouts. Cada client crea su
instancia con **sus** números (CommonCrawl 4 intentos desde 2 s, Greenhouse 3 desde 1 s).
Tiene `HttpRetryTest`, sin Spring.

**`client/CommonCrawlIndexClient`** habla con el índice (CDX). Tiene dos métodos:

- `latestIndexIds(count)` pide `https://index.commoncrawl.org/collinfo.json` y devuelve
  los primeros `count` ids. Verificado el 2026-09-13: ese JSON viene **ordenado del más
  nuevo al más viejo**. Se pide en cada corrida, así que **ningún id queda hardcodeado**:
  cuando CommonCrawl publica un índice nuevo, entra solo.
- `forEachUrl(indexId, pattern, onUrl)`: pide primero `showNumPages=true` para saber
  cuántas páginas hay y después recorre `page=0..N-1`, entregando cada URL a medida
  que la lee.

Detalles que importan:

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

**`service/GreenhouseDiscoveryService.discoverOnRecentCommonCrawl()`** recorre los
**10 índices más recientes** (`RECENT_INDEXES`) de a uno. Por cada índice vuelca los dos
patrones de `GreenhouseBoardUrl.indexPatterns()` en **un único `Set<String>`** (eso
deduplica las capturas repetidas y unifica los dos dominios), y un `saveNew` privado le
resta lo que devuelve `findSlugsByAts(GREENHOUSE)` y hace `saveAll` de los nuevos
**antes de pasar al índice siguiente**. Devuelve
`CommonCrawlResult(List<IndexResult> indexes, List<String> failedIndexes)`, con
`IndexResult(indexId, DiscoveryResult(slugsFound, newCompanies))`. Tres decisiones:

- **10 y no los 127.** Decisión de Elias: no hace falta ser exhaustivo, porque Wayback
  cubre muchísimo, y lo que importaba era dejar de perder la mayoría de las empresas con
  un solo índice.
- **Un índice que falla no corta la corrida**: se loguea `WARN`, va a `failedIndexes` y
  se sigue, igual que el sondeo y la carga. Lo que trajeron los otros queda guardado.
- **Ya no es `@Transactional`.** La lectura es tiempo de red y una transacción
  retendría una conexión durante toda ella. El `saveAll` de `SimpleJpaRepository` abre su
  propia transacción corta, que sigue mandando los INSERTs en lotes de 50.

**`controller/DiscoveryController`** expone
`POST /admin/discovery/greenhouse/commoncrawl`, **sin parámetros**. El endpoint viejo
con `?index=` **se sacó**: el nuevo lo cubre, y descubrir es idempotente. Contesta
**202 al toque** y el trabajo corre en un executor de un solo hilo; un `AtomicBoolean`
da **409** si ya hay una corrida en curso. **El log es el único canal de resultado**:
una línea por índice (`CommonCrawl index <id>: N slugs found, M new companies saved`),
una final con índices leídos, empresas nuevas y fallidos, y la excepción si falla.

### Cómo se corre el descubrimiento

El puerto de la app no se publica, así que se entra al container:

```bash
cd ~/oneprofile
docker compose pull && docker compose up -d
docker compose exec app curl -i -X POST 'localhost:8080/admin/discovery/greenhouse/commoncrawl'
docker compose logs -f app
```

**Varios índices son la forma de tener más empresas**, porque cada uno ve una parte:
trae ~4.000 slugs, pero solo ~3.000 se repiten con el siguiente. Sobre 16 índices, de
agosto 2026 a diciembre 2023, la unión llega a **10.086** y **no se aplanó**: los de 2024
todavía suman 200-300 nuevos cada uno. Detalle en `docs/MEDICION-SLUGS.md`. **No todos los
ids existen** (la numeración salta, `CC-MAIN-2026-26` no está), y por eso salen de
`collinfo.json` y no se inventan.

Con el endpoint viejo, uno por índice, ya se corrieron en prod `CC-MAIN-2026-34` y, el
2026-09-13, `-30`, `-25`, `-21` y `-17`. Dieron 1.269, 637, 533 y 503 empresas nuevas, en
15-50 s cada una. **El endpoint nuevo todavía no se corrió**: al correrlo, esos cinco
tendrían que dar 0 nuevas, los otros cinco cerca de la unión medida (~378, 345, 364, 253
y 286), y `company` quedar cerca de **8.614**.

### La API de Greenhouse, medida

Antes de escribir el sondeo se le pegó a mano a `boards-api.greenhouse.io` para no
codear sobre supuestos. Lo que contesta:

- **Board inexistente → `404`**, con `{"status":404,"error":"Job not found"}`. **No
  es 400.** Verificado con `mercadolibre`, `notion`, `retool`, `benchling` y
  `sourcegraph` — los cuatro últimos están en la base porque CommonCrawl los vio, y
  hoy ya no están en Greenhouse.
- **Board vivo → `200`** con `{"jobs":[...],"meta":{"total":N}}`, **todo en una sola
  respuesta: no hay paginación.** Stripe devuelve 628 vacantes en 392 KB. Es lo
  contrario del índice de CommonCrawl, donde hay que leer en streaming.
- **Cada job trae `company_name` adentro**, así que el nombre legible de la empresa
  sale gratis de la misma respuesta, sin un request extra.
- Existe `GET /v1/boards/<slug>` (sin `/jobs`), que devuelve `{"name":"...",
  "content":""}`. **No se usa**: duplicaría la cantidad de requests para conseguir un
  dato que la respuesta de `/jobs` ya trae en la mayoría de los casos. El precio
  aceptado es que las empresas sin vacantes quedan sin nombre.

Antes de escribir la carga de vacantes se volvió a medir el mismo endpoint, esta vez con los parámetros que
traen todo lo aprovechable. Sondeados 9 boards: splice, discord, airbnb y stripe con
`content=true` (842 vacantes) y figma, ramp, brex, coinbase y reddit (2.410 más).

`GET /v1/boards/<slug>/jobs?content=true&pay_transparency=true`

- **`content=true` multiplica la respuesta por 10-18x** y agrega además `departments`
  y `offices`. Medido: splice 2 KB → 53 KB, discord 35 KB → 383 KB, stripe 4,7 MB.
  Por eso el `READ_TIMEOUT` del cliente subió de 30 s a 120 s.
- **`content` viene HTML-escapeado dentro del JSON.** El valor literal del campo,
  ya parseado el JSON, es `&lt;div class=&quot;…&quot;&gt;` y contiene `&amp;nbsp;`.
  Limpiarlo son **dos desescapes**; de ahí `HtmlToText`.
- **`pay_transparency=true`** —se combina con `content=true`— agrega
  `pay_input_ranges`, con el salario estructurado en **426 de 787** vacantes. Cuando
  hay, es **siempre exactamente 1** rango, así que van columnas planas y no tabla
  hija. Monedas vistas: USD, INR, GBP, CAD, EUR, PHP, SGD, BRL, AED. **25 de esos 426
  son por hora**, y lo único que los distingue de un anual es el `title` del rango
  (`"Hourly Rate:"` vs `"Annual base salary range…"`), que es texto libre.
- **`id` es único dentro del board** (626/626 en stripe) y llega a 8.801.523.002, o
  sea que **no entra en un int**. `internal_job_id` **no** es único (603 sobre 626).
- `departments` viene siempre y siempre con **un solo** elemento (842/842).
- **La ubicación no es parseable.** `location.name` es texto libre:
  `"Remote - U.S."`, `"Atlanta; New York"`, `"AMER"`, `"(San Francisco, Chicago, NYC)"`.
  `offices` sí trae un `location` normalizado con país, pero **solo en 107/842 (13%)**;
  el resto es vocabulario de cada empresa (`"US"`, `"Ireland Locations"`, `"US-PERM"`).
  Por eso no se guarda.
- `metadata` son campos personalizados por empresa (211/842): hay señal útil
  (`"Workplace Type" → "Hybrid"`) pero sin esquema común. Tampoco se guarda.
- Largos máximos medidos: `title` 94, `location.name` 185, `departments[0].name` 60,
  `absolute_url` 76. **La URL no pasa de 255**, al revés de lo que suponía el plan; va
  como `text` igual, porque una URL no tiene largo acotado.
- `absolute_url` **no siempre apunta a Greenhouse**: de 842, 626 van a `stripe.com` y
  166 a `careers.airbnb.com`. Cuando la empresa tiene careers propio, la URL es la
  suya.
- **El endpoint no acepta filtrar por fecha.** `updated_after` se ignora: con un corte de
  hoy devuelve el board entero igual, y con el valor literal `basura` tampoco da error.
  El board viene completo o no viene.

### Vacantes viejas: se evaluó descartarlas y se decidió que no (2026-09-11)

Se pensó en no cargar las vacantes con `updated_at` de más de dos meses, tomándolas por
abandonadas. Se midió antes de escribir nada y **la idea quedó descartada**:

- No hay forma de pedirle al board que no las mande (ver arriba), así que el ahorro de
  descarga —la mitad del motivo— no existe. El filtro solo podría aplicarse al mapear.
- **El recorte es chico y desparejo.** Sobre 8 boards y 1.630 vacantes: splice, figma,
  discord y stripe **0%**; reddit 10%, coinbase 13%, airbnb 14%, brex 17%. Total 117 de
  1.630, **7%**.
- Los ceros lo explican: discord y stripe tienen *todas* sus vacantes tocadas hace uno o
  dos días. Hay empresas que reescriben `updated_at` en bloque y otras no, así que el
  corte mide **hábitos del equipo de RRHH** más que vigencia de la búsqueda.
- Y el error es asimétrico: guardar una vacante muerta cuesta unos KB, borrar una viva
  cuesta justo lo que el proyecto quiere producir, y no se recupera.

**Se guardan todas.** `updated_at` está guardado, así que la recencia se usa **al
buscar** —para filtrar u ordenar—, no como descarte en la carga. Es la misma forma que
tiene tomada el filtro por país. La señal fuerte de abandono es que la vacante
**desaparezca del board**, y eso ya lo captura el borrado.

### El sondeo de boards: cliente, servicio y endpoint

Tres clases que van de un POST a tener clasificada cada empresa.

**`client/GreenhouseBoardClient`** habla con la API. Su método del sondeo es
`BoardProbe probe(String slug)`, con `BoardProbe` un record
`(BoardStatus status, int jobCount, String companyName)`. Sigue el molde de
`CommonCrawlIndexClient` —timeouts y reintentos con backoff vía `HttpRetry`, `.exchange()`
en vez de `.retrieve()`— con dos diferencias que importan:

- **El 404 no es un error, es una respuesta.** Es una de las tres cosas que el
  sondeo busca averiguar, no una falla: se traduce a `NOT_FOUND` y **no se
  reintenta**. Por eso hace falta `.exchange()` y chequear el status a mano;
  `.retrieve()` tiraría excepción.
- **Reintenta 3 veces (1s, 2s), no 4 (2s, 4s, 8s)** como el de CommonCrawl. Allá son
  7 requests enormes y perder uno arruina la corrida; acá son 4.000 chicos y perder
  uno cuesta una empresa, que se reintenta en la corrida siguiente.

**`service/GreenhouseBoardProbeService.probeAll()`** es el recorrido: trae las
empresas con `findByAts(GREENHOUSE)` y, por cada una, espera, sondea, anota y
guarda. Devuelve `ProbeResult(companies, notFound, empty, active, failed)`. Tres
decisiones que no se leen en el código:

- **NO es `@Transactional`.** El sondeo es un UPDATE cada 200 ms
  durante media hora: una transacción así retiene una conexión todo ese tiempo y, si
  el proceso muere, **pierde todo lo ya sondeado**. Con un `save()` por empresa cada
  uno abre su transacción corta y una corrida interrumpida conserva lo que alcanzó.
- **Una empresa que falla no aborta la corrida.** Después de los reintentos se
  loguea `WARN`, se cuenta en `failed` y se sigue; esa empresa conserva su estado
  anterior y se reintenta la próxima vez.
- **Pausa de 200 ms entre boards**, unos 5 por segundo. Es el piso, no el total: hay
  que sumarle lo que tarda cada respuesta, y la corrida real dio **~30 minutos** para
  4.046 empresas, no los ~14 que daría la pausa sola.

Tiene dos constructores: el público `(cliente, repo)` **marcado con `@Autowired`** y
uno de paquete que además recibe la pausa, para que el test la ponga en cero. El
`@Autowired` no es decorativo: con dos constructores y ninguno sin argumentos, Spring
no sabe cuál elegir y **el contexto no levanta**. (`CommonCrawlIndexClient` tiene el
mismo par y no lo necesita porque su constructor público no tiene argumentos.)

**`controller/BoardProbeController`** expone `POST /admin/probe/greenhouse`, **sin
parámetros**: sondea todas las empresas de Greenhouse. Calca a `DiscoveryController`
—202 al toque, executor de un solo hilo, `AtomicBoolean` que da 409 si ya hay una
corrida, resultado al log— a propósito: si el patrón ya está probado a mano, que el
segundo endpoint se lea igual vale más que inventar otra forma.

### Cómo se corre el sondeo

```bash
cd prod
docker compose exec app curl -i -X POST 'localhost:8080/admin/probe/greenhouse'
docker compose logs -f app
```

Y para mirar el resultado en la base:

```sql
select board_status, count(*) from company group by board_status;
```

### Las clases de company

`Ats` es un **enum**, no una entidad. Se decidió así porque es un conjunto cerrado
que define el código y no cargan usuarios: da chequeo del compilador y `switch`
exhaustivo cuando se sumen ATS. La flexibilidad de una tabla sería ilusoria,
porque cada ATS necesita igual su propia clase para parsear su JSON, así que un
INSERT no evitaría recompilar. Hoy tiene **un solo valor, `GREENHOUSE`**, y
ningún campo ni método.

`BoardStatus` es el otro enum, con los tres valores del sondeo: `NOT_FOUND`,
`EMPTY` y `ACTIVE`.

`Company` tiene `id`, `ats` y `slug` —con `@UniqueConstraint` sobre el par
`(ats, slug)`— más los tres campos que deja el sondeo: `name`, `boardStatus` y
`lastProbedAt`. Constructor sin argumentos `protected` para JPA más uno público
`(Ats, String)`, y getters sin setters. Los enums se mapean con
`@Enumerated(EnumType.STRING)`.

En vez de setters sueltos hay un método de dominio,
`recordProbe(BoardStatus status, String name, Instant probedAt)`, con una regla que
importa: **si el nombre viene `null`, no pisa el que ya estaba**. El caso concreto es
una empresa que hoy tiene vacantes —y de ahí se aprende que se llama "Globant"— y en
dos meses cierra las búsquedas: el sondeo la deja `EMPTY` y sin nombre nuevo, y
sobreescribir con `null` perdería un dato ya conseguido por una razón que no tiene
nada que ver. Tiene test.

Los tres campos son **nullable, y eso es semántico**: `board_status is null`
significa exactamente *"esta empresa nunca se sondeó"*. Con `last_probed_at` encima,
el cron futuro puede hacer dos preguntas distintas —"¿a quién no sondeé nunca?" y
"¿a quién no toco hace más de N días?"—, que es para lo que se pidió el timestamp.

`CompanyRepository` es un `JpaRepository<Company, Long>` con dos métodos propios:

```java
@Query("select c.slug from Company c where c.ats = :ats")
List<String> findSlugsByAts(Ats ats);

List<Company> findByAts(Ats ats);
```

El primero devuelve **slugs y no entidades** a propósito: es lo único que el
descubrimiento necesita —restar de un `Set` lo que ya está guardado— y así no hidrata
miles de entidades. El segundo devuelve entidades porque el sondeo **las actualiza**,
y ahí no hay forma de evitarlo.

### Las vacantes: modelo, limpieza, cliente, servicio y endpoint

Las piezas que van de un POST a tener vacantes en la base, sea de una empresa o de
todas. Está todo probado a mano: la carga de una empresa contra la API real, y el
recorrido masivo corrido entero en el servidor.

**`util/HtmlToText`** es una clase final sin instancias con un solo método,
`static String plainText(String escapedHtml)`. Función pura, sin red y sin Spring, al
lado de `GreenhouseBoardUrl`. Hace el desescape doble que el campo `content` necesita:
`Jsoup.parse(Parser.unescapeEntities(raw, false)).text()` — el primero recupera el HTML
real y el segundo lo hace Jsoup solo al pedirle el texto. Devuelve `null` cuando no
queda nada para leer, así una vacante sin descripción queda en `null` y no en `""`.
**Jsoup es dependencia nueva** (`org.jsoup:jsoup:1.21.2`, con `<version>` explícita
porque el parent de Spring Boot no la gestiona).

**`client/GreenhouseBoardClient`** ganó un segundo método público,
`List<BoardJob> jobs(String slug)`, que reusa el mismo `HttpRetry` y el mismo
`ObjectMapper` que ya tenía. `BoardJob` es un record con los 13 campos que se guardan.
Dos detalles: llama a `HtmlToText` **al mapear**, así el record no arrastra HTML crudo,
y los cuatro campos de salario son `null` juntos cuando el board no publica rango. La
URL de `probe` quedó intacta —el sondeo no quiere el contenido— y la nueva es otra
constante con los dos parámetros.

**`model/Vacancy`** sigue el molde de `Company`: constructor `protected` para JPA más
uno público `(Company, Long)`, getters sin setters, y un método de dominio
`describe(...)` que reescribe de una todo lo mutable. La relación a `Company` es
`@ManyToOne(optional = false)` **unidireccional**: la empresa no conoce sus vacantes,
porque una `@OneToMany` de miles de elementos es un problema y no una comodidad.
Identidad: `@UniqueConstraint` sobre `(company_id, external_id)`, porque el id de
Greenhouse es único dentro del board y no globalmente.

Los campos guardados son `externalId`, `title`, `location` (el `location.name` crudo,
sin parsear), `department` (el `departments[0].name`), `description`, `url`,
`language`, `payMinCents`, `payMaxCents`, `payCurrency`, `payTitle`, `firstPublished` y
`updatedAt`. **No** se guardan `offices`, `metadata`, `internal_job_id`,
`requisition_id`, `education`, `employment`, `application_deadline`, `data_compliance`
ni los `ai_*`; `company_name` tampoco, porque ya está en `Company.name`. El `payTitle`
se guarda **crudo**, sin derivar un enum anual/hora: eso sería una heurística sobre
texto libre de cada empresa.

**`service/GreenhouseVacancySyncService`** tiene un solo método,
`Optional<SyncResult> syncCompany(String slug)`, con
`SyncResult(int fetched, int inserted, int updated, int deleted)`. Busca la empresa por
`(GREENHOUSE, slug)` y devuelve **vacío si no la conoce** —eso es lo que el controller
convierte en 404—. Carga en un `Map<Long, Vacancy>` lo que ya tenía guardado de esa
empresa con una sola consulta, y por cada vacante del board hace `stored.remove(...)`
para insertar o actualizar. Ese `remove` es el truco del borrado: **lo que queda en el
mapa al terminar el recorrido es exactamente lo que el board ya no tiene**, y se va con
un `deleteAll`. Es `@Transactional`, al revés del sondeo: una empresa sola tarda
segundos, así que no hay riesgo de tener una transacción abierta media hora.

**`service/GreenhouseVacancySweepService`** es el recorrido masivo, y calca a
`GreenhouseBoardProbeService` porque es el mismo problema. Su método es
`SweepResult syncAllActive()`, con
`SweepResult(companies, fetched, inserted, updated, deleted, failed)`: pide los slugs
`ACTIVE`, y por cada uno espera 500 ms, llama a `syncCompany(slug)` y acumula. Una
empresa que falla —un board que murió desde el sondeo contesta 404, y `jobs()` lo tira
como excepción— se loguea en `WARN`, se cuenta en `failed` y **no aborta la corrida**.
Tiene los dos constructores de siempre, con `@Autowired` en el público, para que el test
ponga la pausa en cero.

Dos cosas de esta clase que no se leen en el código:

- **No es `@Transactional`**, por lo mismo que el sondeo: una corrida de dos horas en una
  sola transacción retiene una conexión todo ese tiempo y pierde todo si el proceso muere.
- **Que sea una clase aparte y no un método más del sync no es cosmético.** Es lo que hace
  que el `@Transactional` de `syncCompany` efectivamente aplique: una llamada entre
  métodos del mismo bean saltea el proxy de Spring, y la corrida entera quedaría sin
  transacción por empresa.

**`controller/VacancyController`** expone los dos endpoints, que contestan distinto a
propósito:

- `POST /admin/vacancies/greenhouse/{slug}` **contesta en línea**, con el `SyncResult` en
  el cuerpo. Todo el método es `ResponseEntity.of(this.syncService.syncCompany(slug))`:
  el `Optional` vacío ya da 404. **No mira `board_status`**: le pega a cualquier empresa
  conocida, que es lo que lo hace útil para probar una empresa puntual.
- `POST /admin/vacancies/greenhouse` (sin slug) usa el molde ya probado dos veces —202 al
  toque, executor de un solo hilo, `AtomicBoolean` que da 409, resultado al log—, porque
  acá sí la corrida dura horas.

**`repository/VacancyRepository`** es un `JpaRepository<Vacancy, Long>` con
`List<Vacancy> findByCompany(Company company)`, que es lo que arma ese mapa. Y
`CompanyRepository` ganó dos métodos más: `Optional<Company> findByAtsAndSlug(Ats, String)`
y `List<String> findSlugsByAtsAndBoardStatus(Ats, BoardStatus)`. El segundo devuelve
**slugs y no entidades**, como `findSlugsByAts`: es lo único que el recorrido necesita
—`syncCompany` recibe un slug— y así no hidrata 3.121 entidades.

### Los extractores del título: `TitleCleaner`, `SeniorityExtractor` y `WorkModeExtractor`

Tres **funciones puras** en `util` —sin red, sin base y sin Spring, como
`GreenhouseBoardUrl` y `HtmlToText`— que se encadenan en este orden:

```java
String clean = TitleCleaner.clean(vacancy.title);
WorkModeExtractor.Extracted mode = WorkModeExtractor.extract(clean, vacancy.location);
SeniorityExtractor.Extracted level = SeniorityExtractor.extract(mode.title());
// level.title() es el titulo normalizado; level.seniority() y mode.mode(), sus campos
```

Las llama **`VacancyNormalizationService`**, que es quien las encadena y guarda el
resultado en `normalized_vacancy` (ver la sección siguiente); no hay una fachada aparte.
Hasta el 2026-09-12 las dos primeras eran una sola clase,
`TitleNormalizer`; se partió porque con la modalidad entrando cada criterio nuevo la iba a
agrandar.

Existen porque el objetivo es **categorizar las vacantes en tech-adyacentes y no
tech-adyacentes** usando el título como señal, y el título viene sucio de dos maneras:
formato (`Sr. Software Engineer - Backend` contra `Senior Software Engineer (Backend)`) y
**atributos metidos adentro del texto**. Todas las reglas de abajo salieron de medir
contra las 128.953 vacantes reales, y el porqué de cada una está abajo.

#### `TitleCleaner.clean(String)` → `String`

**La limpieza es una lista blanca, no una lista negra.** Se saca **todo** lo que no sea
letra, dígito o espacio, salvo cuatro caracteres que se conservan solo donde significan
algo: `.` seguido de alfanumérico (`.net`, `node.js`), `#` precedido de alfanumérico
(`c#`), `+` precedido de alfanumérico o de otro `+` (`c++`) y `&` entre alfanuméricos
(`r&d`). Enumerar lo que molesta siempre deja alguno afuera; enumerar lo que importa es
un conjunto cerrado. Antes se pasa a minúsculas y se sacan los diacríticos, y el
apóstrofo se borra **sin dejar espacio** (`women's` → `womens`). Las clases de caracteres
son unicode, así que un título en coreano no queda vacío, y al final se recompone a
**NFC**: NFD parte cada sílaba del hangul en sus letras, y dejarlo así daría dos
escrituras del mismo título. Devuelve `""` cuando no queda nada legible.

Después, sobre los tokens, dos reglas más:

- **La marca de género se va**: las secuencias `h f`, `f h`, `m f d`, `m w d`, `w m d`,
  `m f x`, `m w x`, `f m x`, en cualquier posición y **solo enteras** —la `e` suelta de
  `assistant(e) de vie h/f` se queda—. Casi no colapsa títulos, pero dejaba letras sueltas
  (`f` en 1.607 títulos) que ensucian cualquier lectura por tokens.
- **La sigla redundante al final se va**, pero **solo si sus letras son las iniciales de
  las palabras anteriores**: `registered behavior technician rbt` pierde el `rbt`, y
  `senior software architect .net`, `account executive us` y
  `dialysis technician … 91359` no pierden nada. La sigla tiene que ser solo letras, de 2
  a 6, y puede saltearse palabras de dos letras o menos (`… engineer in test sdet`). La
  regla obvia —borrar lo que cierra el título— es la equivocada: ahí mismo viven `.NET`,
  `(US)`, `(PRN)` y códigos postales.

#### `WorkModeExtractor.extract(cleanTitle, location)` → `(title, WorkMode mode)`

Lee la modalidad del título **y de `location`**, y saca del título las palabras que la
dicen. Necesita las dos fuentes porque **la señal vive en `location`**: de 16.444 vacantes
remotas, 14.624 lo dicen solo ahí y 1.066 solo en el título. Por eso **`location` manda**
y el título solo contesta cuando `location` no nombra ninguna modalidad.

`WorkMode` es un enum en `model`: `REMOTE`, `FULLY_REMOTE`, `HYBRID`, `ONSITE`.

- **`null` es "no lo declara", no "es presencial"**: son 112.508 vacantes, y pasarlas a
  `ONSITE` sería inventar el dato.
- **Los sinónimos son solo los que aparecen en los datos**: `remote`, `remoto`, `wfh`,
  `home based`, `work from home`, `fully remote`, `100 remote`, `remote only`; `hybrid`,
  `hibrido`; `onsite`, `on site`, `in office`, `in person`, `presencial`, `field based`.
  `telecommute`, `telework`, `teletrabajo` y `a distancia` se midieron y dan **cero**, por
  eso no están. **`virtual` queda afuera por decisión de Elias**: "Virtual Assistant" es
  un puesto.
- Las frases largas se prueban primero (`fully remote` sale entera). Si una misma fuente
  nombra varias, `HYBRID` gana sobre `REMOTE` y `REMOTE` sobre `ONSITE`.
- **`FULLY_REMOTE`** —la idea de Elias: a esas vacantes puede aplicar cualquiera— es
  remoto con una `location` a la que, sacadas las frases de modalidad, **no le queda
  ningún token**: `"Remote"` sí, `"Remote - US"` no. Es conservador a propósito, sin lista
  de palabras de relleno, así que `"Remote, Anywhere"` queda como `REMOTE`.

#### `SeniorityExtractor.extract(cleanTitle)` → `(title, Seniority seniority)`

**El seniority sale a un campo propio**, el enum `Seniority` (`ENTRY`, `JUNIOR`,
`SEMI_SENIOR`, `MID`, `SENIOR`, `STAFF`, `PRINCIPAL`, más `LEVEL_2` y `LEVEL_3` para el
`ii` / `iii` de "Engineer II", que se dejan sin traducir a propósito porque los títulos no
dicen a qué nivel equivalen). No es ruido: estaba adentro de una de cada cuatro vacantes,
y sacarlo colapsa más títulos que toda la limpieza de caracteres junta.

Lo que **no** cuenta como seniority y se queda adentro del título: los roles jerárquicos
(`director`, `lead`, `head`, `vp`, `chief`), porque son la función y no un modificador
—un director de ingeniería no es un ingeniero—; el ambiguo `associate`; `intern`, que es
tipo de contrato; y `experienced`, que se midió y es un adjetivo del rol.

**Tres tokens tienen guarda**, porque se midió que no siempre significan un nivel:
`entry` cuenta solo si le sigue `level` (si no, es `entry door` o `data entry`); `mid`
solo si le sigue `level`, si cierra el título o si forma rango con otro nivel (si no, es
`mid market` o `mid atlantic`); y `staff` no cuenta si lo precede `of` —`chief of staff`,
`member of technical staff`—, si cierra el título, o si le sigue `nurse`, `accountant` o
`attorney`. Un token que no pasa su guarda se queda adentro del título como una palabra
más.

**Cuando el título nombra más de un nivel, gana el más bajo**, porque una vacante publica
**el piso que acepta**: `junior to senior project manager` busca gente desde junior. Por
eso los valores del enum se declaran de menor a mayor y ese orden es significativo. El
tramo `STAFF` < `PRINCIPAL` es convención adoptada, no un hecho medido, y está dicho en
el javadoc del enum. `LEVEL_2` y `LEVEL_3` van declarados después de toda la escala de
palabras, así que el mínimo hace sola la regla de que una palabra le gane a un numeral:
`senior account executive ii` da `SENIOR`.

#### Lo que se midió y se descartó

No todo lo que parece ruido vale una regla. Se midieron once criterios más y los siete
recortes candidatos juntos bajan los títulos distintos apenas **−2,4%**, contra el −5,8%
del seniority solo. Quedaron afuera: la ubicación metida en el
título (un diccionario de tokens toma `west` o `park` por lugares), plata, marketing,
fechas, números de requisición, idioma requerido y contrato/jornada.

#### Dos decisiones de modelo que no se leen en el código

- **El seniority es un enum con nombre, no un nivel numérico.** Se evaluó guardarlo como
  1-5 y se descartó, porque un número no permite reconstruir la vacante:
  - `ii` y `iii` (1.941 + 460 vacantes) no dicen a qué nivel equivalen.
  - `staff` y `principal` son del carril de contribuidor individual, y cada empresa los
    ordena a su manera.
  - `SENIOR` + `software engineer` se recompone a "senior software engineer"; con un `3`
    se pierde qué palabra decía el título.

  El número se puede derivar del enum cuando el matching lo necesite, y al revés no.
- **`Vacancy` no se renombró a `GreenhouseVacancy`.** Las columnas de `vacancy` ya son un
  modelo propio: el formato del ATS lo absorbe `GreenhouseBoardClient.BoardJob`, y un
  segundo ATS caería en las mismas columnas. Se reevalúa cuando ese segundo ATS exista y
  se vea con datos si su forma cruda difiere de verdad.

### La normalización: tabla, servicio y endpoints

Las piezas que corren los extractores sobre las vacantes guardadas y dejan el resultado en
la base.

**`model/NormalizedVacancy`** tiene `title`, `seniority` y `workMode`, con
`@OneToOne(optional = false)` **unidireccional** a `Vacancy` (FK única `vacancy_id`). Sigue
el molde de `Vacancy`: constructor `protected` para JPA, uno público `(Vacancy)`, getters
sin setters y el método de dominio `describe(title, seniority, workMode)`. Los enums van con
`@Enumerated(STRING)`. `title` es nullable porque `SeniorityExtractor` lo deja en `null`
cuando no queda nada, como en `Principal` a secas.

**Es una tabla aparte y no columnas de `vacancy` porque es otro tipo de dato.** `vacancy` es
un **espejo del board**: el sync sobreescribe lo que cambió y borra lo que ya no está. Lo
normalizado es **derivado**: se recalcula cuando cambia una regla, sin volver a pegarle a la
API. Juntarlos obligaría a que `Vacancy.describe(...)` preserve columnas que no vienen del
board.

**La FK lleva `on delete cascade`**, decidido con Elias. Así el sync borra vacantes sin
conocer la tabla derivada: Postgres se lleva la fila normalizada con su vacante.

**`service/VacancyNormalizationService`** tiene dos métodos, que devuelven
`NormalizationResult(int inserted, int updated)`:

- `normalizeAll()` recorre **todas** las vacantes, actualiza las filas que ya existen e
  inserta las que faltan. Es lo que se corre después de cambiar una regla.
- `normalizeMissing()` recorre **solo las vacantes sin fila** y no toca las que ya existen.

Los dos comparten el mismo recorrido y cambia solo la consulta. Cada vacante pasa por
`TitleCleaner.clean` → `WorkModeExtractor.extract(limpio, location)` →
`SeniorityExtractor.extract(mode.title())`. Tres decisiones del recorrido:

- **Páginas de 1.000 por keyset de id** (`id > :after order by id`), no por offset. En
  `normalizeMissing` el conjunto se achica mientras se recorre, y un offset saltearía filas.
  Las consultas son `VacancyRepository.findByIdGreaterThanOrderById` y
  `findNotNormalizedByIdGreaterThan` (un `not exists`), las dos con `Limit`.
- **Cada página va en su propia transacción**, con un `TransactionTemplate`. No es
  `@Transactional` entero, por lo mismo que el sondeo: una corrida cortada conserva lo que
  alcanzó. Usar el template en vez de un segundo bean evita el problema del proxy que obliga
  a separar `GreenhouseVacancySweepService` del sync.
- **Sin pausa**, porque no hay red de por medio. Una página trae sus filas existentes con
  `NormalizedVacancyRepository.findByVacancyIn` y guarda todo con un `saveAll`, que el
  `batch_size=50` agrupa.

Tiene los dos constructores de siempre, con `@Autowired` en el público; el de paquete recibe
el tamaño de página para que el test use páginas de 2.

**`controller/NormalizationController`** usa el molde de los otros controllers: 202 al toque,
executor de un solo hilo y resultado al log. Expone:

- `POST /admin/normalization/vacancies` → `normalizeAll()`
- `POST /admin/normalization/vacancies/missing` → `normalizeMissing()`

Los dos **comparten un solo `AtomicBoolean`**, porque escriben la misma tabla: mientras corre
cualquiera, los dos dan 409.

### Cómo se corre la normalización

```bash
cd ~/oneprofile
docker compose exec app curl -i -X POST 'localhost:8080/admin/normalization/vacancies'
docker compose logs -f app   # "Vacancy normalization (all) finished: N inserted, M updated"
```

### La corrida en prod (2026-09-13)

Terminó con `128953 inserted, 0 updated` en **24 segundos**, sin ninguna vacante sin fila.
Todo coincide con lo medido antes de
escribir las reglas, y cada diferencia tiene explicación:

| | Normalizado | Medido antes | Por qué difiere |
|---|---|---|---|
| `SENIOR` | 24.119 | ~24.230 (senior + sr) | regla del mínimo |
| `STAFF` | 3.281 | 4.562 títulos con la palabra | guardas (`staff accountant` 31, `chief of staff` 28, `staff attorney` 14, `member of technical staff` 8) y la regla del mínimo (`senior staff` → `SENIOR`) |
| `PRINCIPAL` | 1.880 | ~2.140 | regla del mínimo |
| `LEVEL_2` / `LEVEL_3` | 1.587 / 364 | 1.941 / 460 | una palabra le gana al numeral |
| `JUNIOR` / `ENTRY` / `MID` / `SEMI_SENIOR` | 616 / 196 / 159 / 17 | — | — |
| Remoto (`REMOTE` + `FULLY_REMOTE`) | 16.094 (13.166 + 2.928) | ~16.444 | 193 quedan `HYBRID` a propósito; 31 quedan sin modalidad por `remotely` |
| `FULLY_REMOTE` | 2.928 | entre 2.736 y 3.105 | — |
| `HYBRID` | 2.340 | ~2.345 | — |
| `ONSITE` | 821 | ~876 | — |
| Títulos distintos | 77.630 | 87.647 crudos | −11,4% |
| Títulos nulos | 6 | 6 | todos `Principal` a secas |

El top 30 de títulos es el mismo que se había medido con los recortes simulados, con dos
cambios. `registered behavior technician` junta ahora su variante con `rbt` (466), y
`accountant` baja de 182 a 134 porque `staff accountant` queda como título propio. Arriba de
todo: `software engineer` 884, `account executive` 505, `registered behavior technician`
466, `behavior technician` 425, `product manager` 331, `data engineer` 324.

### La carpeta `docs/para-humanos/`

Documentación dirigida a personas, **que este agente no debe leer** (ver la
advertencia del encabezado). Existe porque `METODOLOGIA.md` y este archivo están
escritos para el agente y no sirven para entender el sistema de un vistazo: son
exhaustivos y no tienen un solo diagrama.

Contiene un `README.md` —que cierra con los puntos abiertos—, un archivo por proceso
—`descubrimiento.md`, `sondeo.md`, `vacantes.md` y `normalizacion.md`—, el de
`despliegue.md`, y **ocho** diagramas en `diagramas/`, cada uno con su `.puml` fuente y
su `.svg` versionado al lado: `panorama` (componentes),
`flujo-descubrimiento` y `flujo-sondeo` (secuencia, los dos más importantes),
`url-a-slug` (actividad), `pipeline-normalizacion` (actividad, con un título real
transformándose en cada extractor), `modelo-de-datos` (clases), `entorno` (dónde corre
dev y dónde prod) y `despliegue` (cómo la imagen llega del commit al servidor).
Actualizada el 2026-09-13, con el descubrimiento sobre 10 índices y la normalización.

**PlantUML no está instalado como comando**, pero el jar sí está en la máquina, y con él
alcanza:

```bash
java -jar ~/.vscode/extensions/jebbs.plantuml-2.18.1/plantuml.jar -tsvg <archivo>.puml
```

Java y Graphviz —lo único que PlantUML necesita de fondo— ya están. Para revisar un
diagrama *mirándolo*, que es como se da por bueno, se genera un PNG temporal fuera del
repo (`rsvg-convert -z 2 x.svg -o /tmp/.../x.png`) y se abre, en vez de leer el SVG como
texto.

El criterio de escritura y el de las notas de los diagramas están en
`docs/METODOLOGIA.md`, en "Convenciones del proyecto".

### Migraciones

El esquema vive en `src/main/resources/db/migration/` y lo aplica Flyway, que
anota lo ya corrido en la tabla `flyway_schema_history`. **Cuando cambie el
esquema no se reescribe `V1`: se agrega la siguiente con el `ALTER TABLE`**, y los
datos existentes sobreviven. Hoy hay cuatro migraciones:

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

```sql
-- V2__add_company_board_status.sql
alter table company add column name varchar(255);
alter table company add column board_status varchar(255);
alter table company add column last_probed_at timestamp(6) with time zone;
```

```sql
-- V3__create_vacancy.sql
create sequence vacancy_seq start with 1 increment by 50;

create table vacancy (
    id bigint not null,
    company_id bigint not null references company,
    external_id bigint not null,
    title varchar(255),
    location text,
    department varchar(255),
    description text,
    url text,
    language varchar(255),
    pay_min_cents bigint,
    pay_max_cents bigint,
    pay_currency varchar(255),
    pay_title varchar(255),
    first_published timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    primary key (id),
    unique (company_id, external_id)
);
```

```sql
-- V4__create_normalized_vacancy.sql
create sequence normalized_vacancy_seq start with 1 increment by 50;

create table normalized_vacancy (
    id bigint not null,
    vacancy_id bigint not null unique references vacancy on delete cascade,
    title varchar(255),
    seniority varchar(255),
    work_mode varchar(255),
    primary key (id)
);
```

El `increment by 50` no es decorativo: es el `allocationSize` por defecto que
Hibernate 6/7 espera para `@GeneratedValue`, y si no coincide `validate` falla. Por eso
`vacancy_seq` y `normalized_vacancy_seq` lo repiten. `V4` se aplicó en prod sobre las
128.953 vacantes y creó la tabla vacía.

Las tres columnas `text` de `vacancy` obligan a declarar
`@Column(columnDefinition = "text")` en la entidad: para un `String` pelado Hibernate
espera `varchar(255)` y con `ddl-auto=validate` la app no levantaría.

`V2` se aplicó sobre prod con las 4.046 filas ya cargadas y no las tocó: las tres
columnas entraron en `null`. El tipo `timestamp(6) with time zone` es el que Hibernate
espera para un `Instant`; está verificado porque `ddl-auto=validate` pasa.

## Estado verificado

- `./mvnw test` → **110 tests, 0 fallas** (corrido el 2026-09-13, con el descubrimiento
  sobre 10 índices ya escrito): `BackendApplicationTests.contextLoads`,
  4 de `CompanyRepositoryTest`, 5 de `VacancyRepositoryTest`, 12 casos parametrizados
  de `GreenhouseBoardUrlTest`, 3 de `HtmlToTextTest`, 11 de `TitleCleanerTest`, 13 de
  `SeniorityExtractorTest`, 9 de `WorkModeExtractorTest`, 4 de `HttpRetryTest`, 7 de
  `CommonCrawlIndexClientTest`, 6 de `GreenhouseDiscoveryServiceTest`, 2 de
  `DiscoveryControllerTest`, 9 de `GreenhouseBoardClientTest`, 3 de
  `GreenhouseBoardProbeServiceTest`, 5 de `GreenhouseVacancySyncServiceTest`, 3 de
  `GreenhouseVacancySweepServiceTest`, 2 de `BoardProbeControllerTest`, 4 de
  `VacancyControllerTest`, 3 de `NormalizationControllerTest` y 4 de
  `VacancyNormalizationServiceTest`. Las dos primeras clases importan
  `TestcontainersConfiguration`, que declara un `PostgreSQLContainer` como
  `@Bean @ServiceConnection`; `CompanyRepositoryTest` lleva además
  `@AutoConfigureTestDatabase(replace = NONE)`, porque sin base embebida en el
  classpath `@DataJpaTest` falla al intentar reemplazar el datasource.
- Los tests corren contra **Postgres real y contra el esquema que creó Flyway**, no
  contra uno generado por Hibernate.
- **El despliegue anda de punta a punta.** Probado a mano por Elias el 2026-09-11:
  el pipeline terminó en verde, la imagen quedó publicada en GHCR, el servidor la bajó
  **sin `docker login`** una vez marcado el paquete como público, el dump restauró las
  4.046 empresas, Flyway aplicó `V3` al arrancar la app y la app quedó andando. Antes de
  eso, cuando prod todavía corría en la máquina de Elias, ya se había verificado que el
  volumen nombrado persiste: con un `down` y un `up`, la fila insertada a mano seguía ahí
  y Flyway reportaba el esquema en su versión en vez de reaplicar la migración.
- **La primera corrida del pipeline falló** por la descarga de Maven del wrapper; está
  contado arriba, en "El pipeline". Con `mvn` en vez de `./mvnw` quedó en verde.
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
- **`HttpRetryTest`** no levanta Spring: reintenta un 5xx hasta que anda y un error de
  red, se rinde en el último intento, y un 4xx sale al primer intento.
- **El reordenamiento a `client` + `HttpRetry` no cambió comportamiento**, y está
  verificado así: los tests de los dos clients se movieron **sin tocar sus aserciones**
  (el diff contra lo commiteado es solo `package` e imports) y siguieron en verde.
  Elias corrió `mvn test`: 107 en verde, antes de sumar los 10 índices.
- **`CommonCrawlIndexClientTest`** usa `MockRestServiceServer`, así que verifica
  comportamiento real sin tocar la red: `latestIndexIds(2)` toma los dos primeros ids
  de un `collinfo.json`, recorre las dos páginas que el índice dice
  tener, no pide ninguna si dice cero, saltea líneas vacías o sin `url`, se recupera
  de un 504 y de un 503, se rinde después del cuarto intento, y ante un 404 falla al
  toque **sin reintentar**.
- **`GreenhouseDiscoveryServiceTest`** es `@DataJpaTest` con un doble del cliente
  escrito a mano (una subclase que devuelve índices y URLs de mentira; un índice listado
  pero sin URLs representa uno inalcanzable): guarda una sola empresa por slug por más
  capturas que tenga, trata los dos dominios como la misma empresa, no reinserta las que
  ya estaban, ignora las URLs que no identifican a ninguna, recorre varios índices
  contando en cada uno solo lo que suma, y cuando uno falla conserva lo de los demás y lo
  anota en `failedIndexes`.
- **`DiscoveryControllerTest`** es `@WebMvcTest`: 202 y delegación en el servicio, y
  409 cuando ya hay una corrida en curso, los dos sobre `/commoncrawl`. El doble del servicio se bloquea en un
  `CountDownLatch` para que la segunda request llegue con la primera todavía viva.
- **Ningún test le pega a CommonCrawl de verdad.**
- **El descubrimiento anduvo de punta a punta en prod con el endpoint viejo** (el
  nuevo, sobre 10 índices, falta probarlo). Un POST con
  `index=CC-MAIN-2026-34` devolvió 202 al toque y terminó con
  `4046 slugs found, 4046 new companies saved`. Probado a mano por Elias.
  Ese número **se verificó aparte**, bajando las 7 páginas del índice y corriendo
  las mismas reglas fuera de la app: 76.765 capturas → exactamente 4.046 slugs
  distintos. La app no está perdiendo nada; 4.046 es lo que ese crawl contiene.
  Solo 229 de las 76.765 URLs no dan slug, y son las que corresponde descartar.
- **`GreenhouseBoardClientTest`** usa `MockRestServiceServer`, así que no toca la red:
  un 404 da `NOT_FOUND` **y no se reintenta**, un board sin vacantes da `EMPTY` y sin
  nombre, uno con vacantes da `ACTIVE` con el total y el `company_name`, se recupera
  de un 503 y se rinde al tercer intento.
- **`GreenhouseBoardProbeServiceTest`** es `@DataJpaTest` con un doble del cliente
  escrito a mano —un `Map` de slug a respuesta, donde **un slug ausente significa un
  board inalcanzable**—: deja estado, nombre y fecha en cada empresa; conserva el
  nombre guardado cuando un sondeo posterior no trae uno; y sigue con las demás
  cuando una falla, dejándola sin tocar y contándola en `failed`.
- **`BoardProbeControllerTest`** es `@WebMvcTest`: 202 y delegación, y 409 con una
  corrida en curso, con la misma técnica del `CountDownLatch` que el de descubrimiento.
- **Ningún test le pega a Greenhouse de verdad.**
- **El sondeo anda de punta a punta en prod.** Probado a mano por Elias con un POST
  que devolvió 202 al toque. La corrida **terminó**, y el reparto final de las 4.046
  empresas es **3.121 `ACTIVE`, 708 `NOT_FOUND` y 217 `EMPTY`**. La proporción es el
  hallazgo: el **77%** de lo que descubrió CommonCrawl **sigue vivo y con
  vacantes** — se esperaba bastante más mortandad. La corrida tardó ~30 minutos, más
  de los ~14 que daría la pausa sola.
- **`HtmlToTextTest`** no levanta Spring: el desescape doble sobre un `content` con la
  forma real de Greenhouse deja el texto sin tags, con `&amp;nbsp;` convertido en
  espacio y `&amp;amp;` en `&`; una entidad que solo sobrevive al segundo desescape
  (`&amp;ndash;`) llega bien; y `null`, `""` y un HTML sin texto dan `null`.
- **`VacancyRepositoryTest`** es `@DataJpaTest` contra Postgres real: guarda una
  vacante con los 13 campos y la relee igual; una descripción de 6.600 caracteres
  sobrevive (la columna es `text`); el mismo `(company, external_id)` dos veces salta
  `DataIntegrityViolationException`; el **mismo `external_id` en dos empresas
  distintas se acepta**, que es justamente lo que la unique compuesta tiene que
  permitir; y `findByCompany` trae solo las de esa empresa.
- **`GreenhouseBoardClientTest`** ganó tres casos para `jobs(slug)`: una vacante con
  todo se mapea completa —incluido el `content` ya limpio y el rango en centavos—, una
  sin `pay_input_ranges` deja los cuatro campos de salario en `null`, y un board vacío
  da lista vacía. El `requestTo` exacto verifica que se piden **los dos** parámetros.
- **`GreenhouseVacancySyncServiceTest`** es `@DataJpaTest` con un doble del cliente
  escrito a mano: guarda las vacantes del board, en la segunda corrida **actualiza sin
  duplicar** (`inserted:0`), **borra la vacante que ya no está en el board** y la cuenta
  en `deleted`, y devuelve vacío para un slug que no es una empresa conocida.
- **`GreenhouseVacancySweepServiceTest`** es `@DataJpaTest` con un doble del cliente
  —un `Map` de slug a board, donde un slug ausente significa un board inalcanzable—:
  le pide vacantes **solo a las `ACTIVE`** (una `EMPTY`, una `NOT_FOUND` y una nunca
  sondeada quedan sin nada), suma los contadores de todas las empresas, y sigue con las
  demás cuando una falla, contándola en `failed` y dejándole sus vacantes anteriores.
- **`VacancyControllerTest`** es `@WebMvcTest`: 200 con el JSON de contadores y 404 cuando
  el servicio devuelve vacío para el endpoint por slug; 202 más delegación y 409 con una
  corrida en curso para el masivo, con la misma técnica del `CountDownLatch`.
- **El recorrido masivo corrió entero en el servidor** (2026-09-11/12) y dejó
  **128.953 vacantes sobre 3.118 empresas**. Son 3.118 de las 3.121 `ACTIVE` (verificado en la
  base): **tres empresas quedaron sin ninguna vacante**. No se averiguó por qué —pueden
  haber cerrado las búsquedas entre el sondeo y la carga, o haber fallado— pero en
  cualquiera de los dos casos la corrida siguió, que es lo que `failed` contempla.
- **Para mirar el avance de una corrida** se consulta la base directamente, porque el
  endpoint contesta 202 y el resultado solo se ve en el log:

  ```bash
  docker compose exec -T postgres sh -c \
    'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "select count(*), count(distinct company_id) from vacancy"'
  ```

  Elias lo corre en un bucle cada 60 segundos junto con el delta; el script no está
  versionado, vive en el home del servidor.
- **La carga de una empresa anda de punta a punta contra la API real.** Probado a mano
  por Elias en dev. Salidas textuales: splice `{"fetched":5,"inserted":5,"updated":0}`
  y en el segundo POST `{"fetched":5,"inserted":0,"updated":5}` —idempotente—, discord
  45, figma 153 de las cuales **96 con salario** (por ejemplo `16500000`-`19000000`
  USD con `pay_title` = `"Annual Base Salary Range:"`). Las descripciones quedaron en
  texto plano de 5.008 a 8.102 caracteres, con **cero** `&amp;nbsp;` y cero tags. Un
  slug desconocido da 404.
  Un chequeo tosco de `description like '%<%'` da 10 falsos positivos: son `<`
  legítimos del texto de figma (`"(<5000 FTEs)"`, `"(500< FTEs)"`). Para verificar la
  limpieza hay que buscar **tags y entidades**, no el caracter suelto:
  `description ~ '</[a-zA-Z]'`, los tags que Greenhouse usa, y `&amp;nbsp;` / `&amp;amp;`
  / `&amp;lt;`.
- **Los tests de los extractores** no levantan Spring y usan solo títulos y `location`
  reales sacados de las mediciones. `SeniorityExtractorTest` y `WorkModeExtractorTest`
  pasan la entrada cruda por `TitleCleaner` primero, que es el orden en que corre la
  normalización. Los negativos importan tanto como los positivos: `TitleCleanerTest`
  verifica que `.net`, `us`, `prn` y un código postal **no** se toman por siglas.
- **`VacancyNormalizationServiceTest`** es `@DataJpaTest` contra Postgres real, con páginas
  de 2 y títulos reales. Verifica cuatro cosas:
  - `"Sr. Software Engineer (Remote)"` con location `"Remote"` queda como
    `software engineer` / `SENIOR` / `FULLY_REMOTE`.
  - Cinco vacantes dan cinco filas, o sea que recorre todas las páginas.
  - Volver a correr `normalizeAll` después de cambiar un título **actualiza sin duplicar**.
  - `normalizeMissing` inserta solo la vacante nueva y **deja la fila vieja como estaba**,
    aunque su vacante haya cambiado.
- **`GreenhouseVacancySyncServiceTest`** ganó un caso: el sync borra una vacante que ya
  estaba normalizada **sin error**, y su fila desaparece. Es el test del `on delete cascade`.
- **`NormalizationControllerTest`** es `@WebMvcTest`: 202 y delegación en cada endpoint, y
  409 en los dos mientras corre uno, con la técnica del `CountDownLatch`.
- **La normalización anda de punta a punta en prod**, corrida el 2026-09-13 (ver "La corrida
  en prod"). En dev no se probó a mano: se fue directo al servidor.
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

- **Nada categoriza las vacantes** en tech-adyacentes y no tech-adyacentes, que es para lo
  que se normalizó el título.
- **Nada normaliza el departamento ni la ubicación.** Se decidió normalizar solo el
  título por ahora; los otros dos cuando haya un paso que los use.
- Ningún perfil de usuario, ninguna lógica de matching.
- Ningún endpoint que devuelva datos: todos los que hay disparan procesos. El de
  vacantes contesta con los contadores de lo que cargó, no con las vacantes.
- Ningún ATS además de Greenhouse.
- Ningún cron: los procesos se disparan a mano. `last_probed_at` está puesto para cuando
  exista, pero todavía no lo lee nadie.
- **Ningún tag por SHA ni forma de volver atrás.** La imagen se publica solo como
  `latest`: si una versión rompe, la salida es arreglar y pushear, no revertir el deploy.
- **Ningún despliegue automático.** El `pull` + `up -d` en el servidor es a mano.
- **Ni HTTPS, ni dominio, ni reverse proxy.** No hay nada expuesto todavía.

## Puntos abiertos

- **El crawling de Wayback no se hizo.** La medición mostró que la Wayback Machine trae
  **17.730 slugs**, 8.035 de ellos en ningún índice de CommonCrawl (del orden de ~1.850
  empresas vivas extra), y el plan lo tiene diseñado —`client/WaybackCdxClient`,
  `discoverOnWayback()`, `POST /admin/discovery/greenhouse/wayback` con el mismo lock, 2 s
  entre páginas y el 429 reintentable solo ahí—, pero **no hay nada escrito**. Es el paso 3
  de `docs/PLAN-SLUGS.md`. Hasta que exista, `company` solo crece con CommonCrawl.
- **El descubrimiento sobre 10 índices no se probó en prod.** Está escrito y con tests en
  verde, pero hace falta commitear y pushear para que el pipeline publique la imagen, y
  después correr el POST y comparar con los números esperados (ver "Cómo se corre el
  descubrimiento").

- **Falta ver los falsos positivos de la extracción de modalidad.** Los sinónimos salieron
  de contar cuántas veces aparece cada uno, pero —al revés que el seniority, que tuvo su
  medición de falsos positivos y de ahí sus guardas— **nadie miró en qué contexto
  aparecen**. Casos a revisar: `remote` como parte del puesto (`remote sensing`,
  `remote monitoring`), `field based` y `in person`. Está pendiente antes de dar la
  modalidad por confiable. Ahora que la modalidad está en la base, se puede medir con una
  consulta sobre `normalized_vacancy`.
- **`remotely` no es sinónimo de remoto, y deja falsos negativos.** En la corrida de prod,
  31 vacantes con `location` del tipo `Remotely based` o `Remotely in Germany` quedaron sin
  modalidad. Son pocas y no se tocó el extractor.
- **El sync de vacantes no normaliza.** `GreenhouseVacancySyncService` carga, actualiza y
  borra sin tocar `normalized_vacancy`. Una vacante nueva queda sin fila hasta que se corre
  `/admin/normalization/vacancies/missing`, y una cuyo título cambió conserva la fila vieja
  hasta que se recalcula todo. El borrado sí está cubierto por el `on delete cascade`. Hoy no
  importa porque todo se dispara a mano. **Va a importar cuando exista el cron**, que tendría
  que encadenar sondeo → vacantes → normalización. Elias pidió dejarlo anotado.
- **Hay ~650 filas que no son vacantes**: `talent community/network/pool` (356) y
  `general application / speculative / future opportunities` (296), que son formularios de
  "dejanos tu CV". No se filtran porque nadie lo pidió; se decide cuando el matching exista
  y molesten. Un título que queda vacío es una señal de estas filas que no necesita lista de
  frases, pero hoy casi ninguno se vacía, porque los recortes que los vaciaban no entraron.
- **Contrato y jornada merecen un paso propio.** Fue el recorte más grande de los
  descartados (5.117 vacantes): `part time` 2.128, `intern` 1.939, `contract` 1.429,
  `full time` 914, `seasonal` 750, `per diem / prn` 690, `locum` 644. Pesa en el matching
  tanto como el seniority, y antes de escribirlo hay que medir sus falsos positivos.
- **La ubicación estructurada merece un paso propio, saliendo de `location`.** Es más
  parseable de lo que parecía: el 70,7% lleva coma, tiene 3,28 tokens de promedio y la
  cabeza es `ciudad, región, país`. Ese paso también debería absorber los códigos postales
  metidos en el título (`thousand oaks ca 91359`).
- **`FULLY_REMOTE` es conservador a propósito.** No se sacan palabras de relleno antes de
  preguntar si queda un lugar, así que `"Remote, Anywhere"` o `"Remote - Global"` quedan
  como `REMOTE`. Medido con relleno daba 3.105; la corrida dio 2.928.
- **Los rangos de nivel dejan un conector suelto.** La regla del mínimo los resuelve bien
  (~200 vacantes), pero `controls engineer all levels junior to senior` queda como
  `controls engineer all levels to`. No se limpia porque nadie lo pidió.
- **El falso positivo de `senior` no tiene guarda, por decisión de Elias.** Son 37
  vacantes de cuidado domiciliario (0,18% del token). Si alguna vez molesta, la guarda sería
  descartar `senior` cuando lo precede `a`, `female` o `male`.
- **Quedan adentro del título sin regla:** el nivel `i` (no hay `LEVEL_1`, no se midió),
  `associate` (5.861, ambiguo) y `virtual` como remoto (194 títulos, sin medir). El
  `Principal` a secas (6 vacantes, director de escuela) queda con título nulo.
- **`docs/MEDICION-VACANTES.md` dice 85.050 títulos distintos y la base da 87.647**, con el
  mismo `count(*)`. Ese documento no registró su SQL, así que la diferencia es de cómo se
  contó entonces. Sin resolver.
- **Los endpoints de administración no tienen ninguna protección.** Hoy no importa
  porque el puerto de la app no se publica, pero cuando exista un endpoint que
  **devuelva** vacantes habrá que publicarlo y ahí los de administración quedan
  expuestos. Se decide en ese
  momento.
- **El recorrido de vacantes trabaja sobre la foto que dejó el sondeo.** Le pide vacantes
  solo a las `ACTIVE`, así que una empresa que empezó a publicar después de la última
  corrida de sondeo sigue marcada `EMPTY` y no se le pide nada; y una que cerró todo se
  consulta igual, contesta cero y el borrado le saca lo que tuviera. O sea que el orden
  correcto es **sondeo primero, vacantes después**, y hoy nada lo fuerza porque los dos se
  disparan a mano. Cuando exista el cron habrá que encadenarlos.
- **El sondeo se corre entero cada vez.** No hay forma de pedirle "solo las que nunca
  sondeaste" o "solo las viejas": vuelve a pegarle a las 6.988. Los datos para
  filtrar están (`board_status`, `last_probed_at`), la consulta no. Se agrega cuando
  haya un cron que la necesite, no antes.
- **Una respuesta truncada del índice se aceptaría en silencio.** Bajando páginas a
  mano pasó tres veces que el índice cerró la conexión limpio con un cuerpo corto
  (256 KB o 560 KB en vez de ~9 MB) y HTTP 200. El cliente leería esas líneas, no
  vería ningún error y reportaría menos empresas sin avisar. Los reintentos no
  ayudan, porque el status es 200. En la corrida real no pasó —el conteo dio
  exactamente el mismo número que el cálculo offline completo— pero el agujero está.
- **El dominio EU de Greenhouse no se descubre.** Existe `job-boards.eu.greenhouse.io`,
  con 848 slugs medidos, 93 de ellos ya en prod por el otro dominio, y no está entre
  los patrones. Meterlo toca también el sondeo y la carga, porque su API sería
  `boards-api.eu.greenhouse.io`. Elias decidió dejarlo afuera por ahora.
- **Las reglas de slug tiran algunas empresas reales, a sabiendas.** Existen slugs con
  `&` o `)` (`1pyra)mid_health&care` tiene vacantes) y Wayback trae 77 slugs que solo
  aparecen con `http://`. Por el volumen, se decidió no aflojar las reglas.
- **La base de dev arranca vacía, y ahora además está sola.** Las 6.988 empresas viven
  en el volumen de prod, que está **en el servidor**: ya no hay una base con datos en la
  máquina de Elias. El `compose.yaml` de la raíz no declara volumen, así que para probar
  algo en dev hay que insertar la empresa a mano:
  `insert into company (id, ats, slug) values (nextval('company_seq'), 'GREENHOUSE', 'figma');`
- **El endpoint de vacantes no tiene tope de tamaño.** Una empresa de 700 vacantes con
  `content=true` son varios MB en una sola respuesta que se parsea entera en memoria.
  Con las medidas de hoy entra, pero nadie midió el peor caso.
- `pom.xml` tiene la metadata (`name`, `description`, `url`, `licenses`,
  `developers`, `scm`) vacía, tal como la dejó el Initializr.
- En la máquina de Elias, `docker-rootless-extras` quedó en 29.8.0 y `docker` en
  29.7.2 (actualización parcial). Funciona; se empareja en el próximo `pacman -Syu`.

## Qué sigue

**La normalización de los títulos está hecha y corrida en prod.** Lo que le queda anotado es
revisar los falsos positivos de la modalidad (ver "Puntos abiertos").

Lo que viene ahora es **categorizar las vacantes en
tech-adyacentes y no-tech-adyacentes**, que es para lo que se normaliza. La medición ya
dejó claro que esa categorización **tiene que ser por tokens del título y no por un
diccionario de títulos**: casi la mitad de las vacantes tiene un título que no se repite
nunca.

Más allá de eso, sin priorizar y sin planificar:

- Terminar de traer los slugs descubribles: probar en prod los 10 índices de CommonCrawl,
  escribir el cliente de Wayback, y después sondear y cargar vacantes de lo nuevo. El
  plan, con su estado, está en `docs/PLAN-SLUGS.md`.
- Modelar el perfil del usuario (`profile`).
- Primer algoritmo de matching, simple, con tests sobre casos concretos.
- Endpoint HTTP para consultar las vacantes que matchean.
- Un cron que refresque el sondeo y las vacantes en vez de dispararlos a mano.
