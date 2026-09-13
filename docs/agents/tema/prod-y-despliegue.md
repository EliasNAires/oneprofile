# Tema — stack, entorno, prod y despliegue

Lo que el código y la configuración no dicen: trampas, porqués y lo probado en prod.

## Trampas de Spring Boot 4 que ya costaron tiempo

- Paquetes de test modularizados: `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`,
  `org.springframework.boot.jpa.test.autoconfigure.TestEntityManager`,
  `org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase`.
- **`flyway-core` solo no alcanza**: la autoconfiguración vive en `spring-boot-flyway`; sin
  el starter `spring-boot-starter-flyway`, Flyway **no corre y no avisa**.
- **`flyway-database-postgresql` es obligatorio**: sin él, `Unsupported Database:
  PostgreSQL 18.6`. Verificado sacándolo.
- **Testcontainers 2.0.5** renombró: artefacto `testcontainers-postgresql`, clase
  `org.testcontainers.postgresql.PostgreSQLContainer`.
- No hacen falta `testcontainers-junit-jupiter` (el `@Bean @ServiceConnection` maneja el
  ciclo de vida) ni `spring-boot-starter-flyway-test`. Se probaron y se sacaron.
- `@DataJpaTest` necesita `@AutoConfigureTestDatabase(replace = NONE)`: sin base embebida
  en el classpath falla al intentar reemplazar el datasource.

## Entorno de Elias: Docker rootless

Su usuario no está en el grupo `docker` (equivale a root). Arch: `docker` +
`docker-rootless-extras`, que **no trae `dockerd-rootless-setuptool.sh`** sino las unidades
de usuario (`systemctl --user enable --now docker.socket`). Contexto `rootless` en
`unix:///run/user/1000/docker.sock`, linger habilitado, `overlayfs` nativo. Imágenes y
volúmenes en `~/.local/share/docker`. Testcontainers y `spring-boot-docker-compose` lo
detectan solos. `docker-rootless-extras` quedó en 29.8.0 y `docker` en 29.7.2; se empareja
en el próximo `pacman -Syu`.

## Configuración

Mínima: solo lo que el default no cubre.

- `ddl-auto=validate`: el esquema lo hace Flyway; la app **falla al arrancar** si una
  entidad cambió sin migración.
- `batch_size=50`: el descubrimiento inserta miles de filas; coincide con el
  `increment by 50` de las secuencias.
- `application-dev.properties` vacío: `spring-boot-docker-compose` deriva el datasource
  del `compose.yaml` de la raíz (sin volumen; credenciales locales aceptadas por Elias).
- `application-prod.properties` vacío y se queda así: credenciales por
  `SPRING_DATASOURCE_*` desde `prod/compose.yaml`.
- `src/test/resources/application.properties` tapa al de main (los tests no activan
  `dev`) y por eso repite `validate`. Verificado: renombrar una columna hace fallar los tests.
- Al apagar la app, Spring Boot hace `docker compose stop`, **no `down`**: el Postgres de
  dev conserva sus datos hasta un `down` a mano.
- **La base de dev arranca vacía** y no hay otra con datos en la máquina de Elias; para
  probar: `insert into company (id, ats, slug) values (nextval('company_seq'), 'GREENHOUSE', 'figma');`

## Migraciones

Nunca se reescribe una migración aplicada: se agrega la siguiente. Detalles no obvios:

- `increment by 50` en cada secuencia es el `allocationSize` que Hibernate espera; si no
  coincide, `validate` falla.
- Columnas `text` exigen `@Column(columnDefinition = "text")`: para un `String` Hibernate
  espera `varchar(255)`.
- `timestamp(6) with time zone` es lo que Hibernate espera para `Instant`.
- En prod, `V2` entró sobre 4.046 filas (columnas en `null`) y `V4` sobre 128.953 vacantes
  (tabla vacía).

## Prod

Servidor de Elias, por SSH vía ZeroTier (`ssh elitedesk1`, Docker con `sudo`). No compila:
baja la imagen de GHCR. Tiene **solo `compose.yaml` y `.env`** en `~/oneprofile`, copiados
por `scp`; se descartó clonar el repo porque no usaría el código. Si cambia
`prod/compose.yaml`, se vuelve a copiar a mano.

```bash
cd ~/oneprofile
sudo docker compose pull && sudo docker compose up -d   # up -d mata cualquier corrida en curso
sudo docker compose logs -f app
```

- **Ningún puerto publicado**: a los endpoints se entra con
  `docker compose exec app curl ...`; por eso la imagen de runtime instala `curl`.
- `prod/` se llama `compose.yaml` para usar `docker compose` sin `-f`; el de la raíz lo
  ocupa dev por convención de `spring-boot-docker-compose`. En la máquina de Elias ya no
  se levanta prod (`app` tiene `image:` y no `build:`).
- `Dockerfile` multi-stage que copia solo `.mvn`, `mvnw`, `pom.xml` y `src`: así
  `compose.yaml` no entra y el soporte de docker-compose queda inerte sin configurarlo.
  Saltea tests porque Testcontainers no tiene Docker adentro del build.
- `prod/.env` no va al repo; hay `prod/.env.example`.

Tres cosas que costó descubrir:

- **El volumen va en `/var/lib/postgresql`**, no en `.../data`: Postgres 18 movió
  `PGDATA`. Con el path viejo no persiste.
- **`name: oneprofile-prod` no es cosmético**: sin él, un `up` de prod recreó el
  Postgres de dev.
- **`healthcheck` + `depends_on: condition: service_healthy`**: si no, Flyway muere
  porque la app arranca antes que Postgres.

Para mirar el avance de una corrida (los endpoints solo contestan 202 y loguean):

```bash
sudo docker compose exec -T postgres sh -c \
  'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "select count(*), count(distinct company_id) from vacancy"'
```

Elias tiene un bucle de 60 s con el delta, sin versionar, en el home del servidor.

## Pipeline

`.github/workflows/publish.yml`, en cada push a `main`: job `test` (`mvn -B test`) y, si
pasa, `publish` a `ghcr.io/eliasnaires/oneprofile-backend:latest` con el `GITHUB_TOKEN`
(ningún secret a mano).

- **`mvn` y no `./mvnw`, a propósito.** El wrapper (`only-script`) baja Maven en cada
  corrida limpia y la descarga se corta desde las IPs de los runners (`wget: Failed to
  fetch ...apache-maven-3.9.16-bin.zip`). La imagen `ubuntu-24.04` trae la misma 3.9.16.
  Si alguna vez difiere, se fija la versión con una acción que instale Maven, no se vuelve
  al wrapper; cachear `~/.m2/wrapper` no sirve (arranca vacía).
- La label `org.opencontainers.image.source` vincula el paquete al repo, y de ahí sale el
  permiso del token. Un paquete nuevo de GHCR **nace privado**: se marca público una vez
  desde la web o el servidor no lo baja.
- **Un solo tag, `latest`**: no hay vuelta atrás; si algo rompe, se arregla y se pushea.
- **El despliegue es a mano**: que un push reinicie prod no se decidió.

## Mudanza de datos (hecha, 2026-09-11)

Con dump, porque descubrir y sondear costaba horas. **Se restaura con la app apagada**: el
dump trae `flyway_schema_history` y si la app arranca antes crea el esquema y el restore
choca. El `.env` tiene que ser el mismo de los dos lados (dueños de tablas).

```bash
docker compose exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB"' > ~/oneprofile.sql
docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < ~/oneprofile.sql
```

El dump venía en V2, así que Flyway aplicó `V3` al arrancar: aplica lo que al dump le falta.

## Probado en prod

- **Despliegue de punta a punta** (Elias, 2026-09-11): pipeline verde, imagen pública
  bajada sin `docker login`, restore de las 4.046 empresas, `V3` aplicada, app andando. El
  volumen nombrado persiste entre `down` y `up`.

## Abierto

- **No siempre está claro qué código tiene la imagen de prod.** Elias corre varias sesiones
  en paralelo sobre el mismo working tree y ninguna ve lo que otra no commiteó. Antes de
  sacar conclusiones de una prueba en prod, confirmar qué commit se publicó.
- **Endpoints de administración sin protección.** Hoy no importa (sin puertos); se decide
  cuando haya que publicar un endpoint que devuelva datos.
- Sin tag por SHA, sin despliegue automático, sin HTTPS, dominio ni reverse proxy.
- `pom.xml` con la metadata del Initializr vacía.
