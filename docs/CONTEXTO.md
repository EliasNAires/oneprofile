# Contexto — estado actual del repo

> Se actualiza después de cada milestone alcanzado (ver `docs/METODOLOGIA.md`).
> Describe lo que **hay hoy**, no lo que se planea.

> **`docs/para-humanos/` no la leas.** Es documentación para personas: una versión
> resumida y con diagramas de lo que este archivo ya cuenta en detalle. Está
> duplicada, así que leerla gasta contexto en información repetida y te arriesga a
> trabajar sobre el resumen en vez de sobre la fuente de verdad, que es este
> documento. Se abre solo si Elias pide explícitamente escribir o actualizar esa
> carpeta.

**Última actualización:** 2026-09-12
**Último milestone probado:** el **despliegue**. Prod salió de la máquina de
Elias y corre en un **servidor propio**, con la imagen publicada en GHCR por un pipeline
de GitHub Actions que se dispara en cada push a `main`. Probado a mano por Elias de punta
a punta: pipeline en verde, imagen bajada del registry, datos mudados y app andando.

**Milestone anterior:** la carga de vacantes de una empresa por slug.

**El recorrido masivo de vacantes terminó.** Corrió entero en el servidor y dejó
**128.953 vacantes sobre 3.118 empresas**. El número importa porque **desmiente la
proyección**: se esperaban ~250.000, o sea el doble. La media de la muestra (80 vacantes
por empresa) tiraba para arriba; la mediana (17) era la guía correcta.

**No hay ningún plan en curso.** Lo que sigue es normalizar, y todavía no hay un plan
escrito para eso. Lo único que existe es la **medición** de la tabla `vacancy` corrida
el 2026-09-12 contra prod, que vive en `docs/MEDICION-VACANTES.md`: números, sin
enfoque propuesto.

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
docs/para-humanos/README.md                         (para personas, no para agentes)
docs/para-humanos/descubrimiento.md
docs/para-humanos/sondeo.md
docs/para-humanos/vacantes.md
docs/para-humanos/despliegue.md
docs/para-humanos/diagramas/*.puml + *.svg          (7 diagramas PlantUML)
pom.xml
src/main/java/oneprofile/backend/BackendApplication.java
src/main/java/oneprofile/backend/model/Ats.java
src/main/java/oneprofile/backend/model/BoardStatus.java
src/main/java/oneprofile/backend/model/Company.java
src/main/java/oneprofile/backend/model/Vacancy.java
src/main/java/oneprofile/backend/repository/CompanyRepository.java
src/main/java/oneprofile/backend/repository/VacancyRepository.java
src/main/java/oneprofile/backend/service/CommonCrawlIndexClient.java
src/main/java/oneprofile/backend/service/GreenhouseDiscoveryService.java
src/main/java/oneprofile/backend/service/GreenhouseBoardClient.java
src/main/java/oneprofile/backend/service/GreenhouseBoardProbeService.java
src/main/java/oneprofile/backend/service/GreenhouseVacancySyncService.java
src/main/java/oneprofile/backend/service/GreenhouseVacancySweepService.java
src/main/java/oneprofile/backend/controller/DiscoveryController.java
src/main/java/oneprofile/backend/controller/BoardProbeController.java
src/main/java/oneprofile/backend/controller/VacancyController.java
src/main/java/oneprofile/backend/util/GreenhouseBoardUrl.java
src/main/java/oneprofile/backend/util/HtmlToText.java
src/main/resources/application.properties
src/main/resources/application-dev.properties       (vacío)
src/main/resources/application-prod.properties      (vacío)
src/main/resources/db/migration/V1__create_company.sql
src/main/resources/db/migration/V2__add_company_board_status.sql
src/main/resources/db/migration/V3__create_vacancy.sql
src/test/java/oneprofile/backend/BackendApplicationTests.java
src/test/java/oneprofile/backend/TestcontainersConfiguration.java
src/test/java/oneprofile/backend/repository/CompanyRepositoryTest.java
src/test/java/oneprofile/backend/repository/VacancyRepositoryTest.java
src/test/java/oneprofile/backend/service/CommonCrawlIndexClientTest.java
src/test/java/oneprofile/backend/service/GreenhouseDiscoveryServiceTest.java
src/test/java/oneprofile/backend/service/GreenhouseBoardClientTest.java
src/test/java/oneprofile/backend/service/GreenhouseBoardProbeServiceTest.java
src/test/java/oneprofile/backend/service/GreenhouseVacancySyncServiceTest.java
src/test/java/oneprofile/backend/service/GreenhouseVacancySweepServiceTest.java
src/test/java/oneprofile/backend/controller/DiscoveryControllerTest.java
src/test/java/oneprofile/backend/controller/BoardProbeControllerTest.java
src/test/java/oneprofile/backend/controller/VacancyControllerTest.java
src/test/java/oneprofile/backend/util/GreenhouseBoardUrlTest.java
src/test/java/oneprofile/backend/util/HtmlToTextTest.java
src/test/resources/application.properties
```

### Organización de los paquetes

El código se organiza **por capa técnica**, no por feature: `model`, `repository`,
`service`, `controller` y `util` cuelgan directo de `oneprofile.backend`. Ya
existen las cinco.

`util` quedó reservado para **funciones puras sin dependencias** —hoy
`GreenhouseBoardUrl` y `HtmlToText`—. Un componente que hace I/O va a la capa que le
corresponde,
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

**`service/GreenhouseBoardClient`** habla con la API. Su único método es
`BoardProbe probe(String slug)`, con `BoardProbe` un record
`(BoardStatus status, int jobCount, String companyName)`. Sigue el molde de
`CommonCrawlIndexClient` —timeouts explícitos, reintentos con backoff, `.exchange()`
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

- **NO es `@Transactional`, al revés que `GreenhouseDiscoveryService`.** Son
  problemas distintos: el descubrimiento es un montón de INSERTs juntos al final, y
  la transacción los agrupa en lotes de 50. El sondeo es un UPDATE cada 200 ms
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

**`service/GreenhouseBoardClient`** ganó un segundo método público,
`List<BoardJob> jobs(String slug)`, que reusa el mismo `withRetries(...)` y el mismo
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

### La carpeta `docs/para-humanos/`

Documentación dirigida a personas, **que este agente no debe leer** (ver la
advertencia del encabezado). Existe porque `METODOLOGIA.md` y este archivo están
escritos para el agente y no sirven para entender el sistema de un vistazo: son
exhaustivos y no tienen un solo diagrama.

Contiene un `README.md`, un archivo por proceso —`descubrimiento.md`, `sondeo.md` y
`vacantes.md`—, el de `despliegue.md`, y **siete** diagramas en `diagramas/`, cada uno
con su `.puml` fuente y su `.svg` versionado al lado: `panorama` (componentes),
`flujo-descubrimiento` y `flujo-sondeo` (secuencia, los dos más importantes),
`url-a-slug` (actividad), `modelo-de-datos` (clases), `entorno` (dónde corre dev y dónde
prod) y `despliegue` (cómo la imagen llega del commit al servidor).

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
datos existentes sobreviven. Hoy hay tres migraciones:

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

El `increment by 50` no es decorativo: es el `allocationSize` por defecto que
Hibernate 6/7 espera para `@GeneratedValue`, y si no coincide `validate` falla. Por eso
`vacancy_seq` lo repite.

Las tres columnas `text` de `vacancy` obligan a declarar
`@Column(columnDefinition = "text")` en la entidad: para un `String` pelado Hibernate
espera `varchar(255)` y con `ddl-auto=validate` la app no levantaría.

`V2` se aplicó sobre prod con las 4.046 filas ya cargadas y no las tocó: las tres
columnas entraron en `null`. El tipo `timestamp(6) with time zone` es el que Hibernate
espera para un `Instant`; está verificado porque `ddl-auto=validate` pasa.

## Estado verificado

- `./mvnw test` → **62 tests, 0 fallas**: `BackendApplicationTests.contextLoads`,
  4 de `CompanyRepositoryTest`, 5 de `VacancyRepositoryTest`, 12 casos parametrizados
  de `GreenhouseBoardUrlTest`, 3 de `HtmlToTextTest`, 6 de `CommonCrawlIndexClientTest`,
  4 de `GreenhouseDiscoveryServiceTest`, 2 de `DiscoveryControllerTest`, 9 de
  `GreenhouseBoardClientTest`, 3 de `GreenhouseBoardProbeServiceTest`, 4 de
  `GreenhouseVacancySyncServiceTest`, 3 de `GreenhouseVacancySweepServiceTest`, 2 de
  `BoardProbeControllerTest` y 4 de `VacancyControllerTest`. Las dos primeras clases importan
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

- **Nada normaliza los títulos, el departamento ni la ubicación.** Están guardados como
  los escribió cada empresa. **La normalización todavía no empezó**: lo único que hay
  es la medición de `docs/MEDICION-VACANTES.md`.
- Ningún perfil de usuario, ninguna lógica de matching.
- Ningún endpoint que devuelva datos: los tres que hay disparan procesos. El de
  vacantes contesta con los contadores de lo que cargó, no con las vacantes.
- Ningún ATS además de Greenhouse.
- Ningún cron: los procesos se disparan a mano. `last_probed_at` está puesto para cuando
  exista, pero todavía no lo lee nadie.
- **Ningún tag por SHA ni forma de volver atrás.** La imagen se publica solo como
  `latest`: si una versión rompe, la salida es arreglar y pushear, no revertir el deploy.
- **Ningún despliegue automático.** El `pull` + `up -d` en el servidor es a mano.
- **Ni HTTPS, ni dominio, ni reverse proxy.** No hay nada expuesto todavía.

## Puntos abiertos

- **Los endpoints de administración no tienen ninguna protección.** Hoy no importa
  porque el puerto de la app no se publica, pero cuando exista un endpoint que
  **devuelva** vacantes habrá que publicarlo y ahí los tres de administración quedan
  expuestos. Se decide en ese
  momento.
- **El recorrido de vacantes trabaja sobre la foto que dejó el sondeo.** Le pide vacantes
  solo a las `ACTIVE`, así que una empresa que empezó a publicar después de la última
  corrida de sondeo sigue marcada `EMPTY` y no se le pide nada; y una que cerró todo se
  consulta igual, contesta cero y el borrado le saca lo que tuviera. O sea que el orden
  correcto es **sondeo primero, vacantes después**, y hoy nada lo fuerza porque los dos se
  disparan a mano. Cuando exista el cron habrá que encadenarlos.
- **El sondeo se corre entero cada vez.** No hay forma de pedirle "solo las que nunca
  sondeaste" o "solo las viejas": vuelve a pegarle a las 4.046. Los datos para
  filtrar están (`board_status`, `last_probed_at`), la consulta no. Se agrega cuando
  haya un cron que la necesite, no antes.
- **Una respuesta truncada del índice se aceptaría en silencio.** Bajando páginas a
  mano pasó tres veces que el índice cerró la conexión limpio con un cuerpo corto
  (256 KB o 560 KB en vez de ~9 MB) y HTTP 200. El cliente leería esas líneas, no
  vería ningún error y reportaría menos empresas sin avisar. Los reintentos no
  ayudan, porque el status es 200. En la corrida real no pasó —el conteo dio
  exactamente el mismo número que el cálculo offline completo— pero el agujero está.
- **La base de dev arranca vacía, y ahora además está sola.** Las 4.046 empresas viven
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

Lo inmediato es la **normalización**, y **todavía no hay plan escrito**: se va a
discutir de cero. Lo que sí está es la medición previa, en `docs/MEDICION-VACANTES.md`
(volumen real, la columna `language`, la cardinalidad de los departamentos y una
primera mirada a los títulos); esos números no se duplican acá.

Más allá de eso, sin priorizar y sin planificar:

- Correr el descubrimiento con más índices de CommonCrawl para engordar la tabla.
  No necesita código: es repetir el POST cambiando el `index`.
- Modelar el perfil del usuario (`profile`).
- Primer algoritmo de matching, simple, con tests sobre casos concretos.
- Endpoint HTTP para consultar las vacantes que matchean.
- Un cron que refresque el sondeo y las vacantes en vez de dispararlos a mano.
