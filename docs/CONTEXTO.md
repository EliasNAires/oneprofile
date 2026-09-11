# Contexto — estado actual del repo

> Se actualiza después de cada milestone alcanzado (ver `docs/METODOLOGIA.md`).
> Describe lo que **hay hoy**, no lo que se planea.

**Última actualización:** 2026-09-11
**Último milestone:** Paso 3 — entidad `Company` con su enum `Ats`, repositorio y
tests de persistencia. `./mvnw test` en verde con 3 tests.

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
  (`"Buenos Aires, Argentina"`, `"Remote - LATAM"`, `"Remote - Americas"`). No hay
  campo `country` confiable, así que filtrar por Argentina es una heurística sobre
  strings.
- **Decisión tomada:** el descubrimiento guarda **todas** las empresas del ATS, no
  solo las que hoy tienen vacantes en Argentina. Una empresa sin vacantes AR hoy
  puede tenerlas mañana, y descartarla obligaría a volver a pegarle a CommonCrawl.
  El filtro por país es una consulta sobre las vacantes, no un descarte en la carga.

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
- **H2** (runtime) — base de desarrollo y de tests, en memoria, autoconfigurada.
- PostgreSQL, driver en scope `runtime` — pensado para prod, todavía sin configurar.
- `spring-boot-devtools` (runtime/optional) — hot reload.
- Tests: `spring-boot-starter-data-jpa-test`, `spring-boot-starter-webmvc-test`.
- Build con el wrapper: `./mvnw`.

**Ojo con los paquetes de test en Spring Boot 4:** se modularizaron y cambiaron de
lugar respecto de Boot 3. Los que van son:

```java
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
```

(en Boot 3 ambos vivían en `org.springframework.boot.test.autoconfigure.orm.jpa`).
Es la misma modularización por la que la consola de H2 pasó a su propio módulo.

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
y `create-drop` dropea el esquema al cerrarlo. Esto va a molestar en cuanto el
script de descubrimiento guarde empresas reales: ahí se decide (H2 en archivo +
`ddl-auto=update`, o pasar a Postgres).

## Qué existe hoy

```
CLAUDE.md
docs/METODOLOGIA.md
docs/CONTEXTO.md
pom.xml
src/main/java/oneprofile/backend/BackendApplication.java
src/main/java/oneprofile/backend/company/Ats.java
src/main/java/oneprofile/backend/company/Company.java
src/main/java/oneprofile/backend/company/CompanyRepository.java
src/main/resources/application.properties
src/main/resources/application-dev.properties      (vacío)
src/main/resources/application-prod.properties     (vacío)
src/test/java/oneprofile/backend/BackendApplicationTests.java
src/test/java/oneprofile/backend/company/CompanyRepositoryTest.java
src/test/resources/application.properties           (vacío)
```

### El paquete `company`

`Ats` es un **enum**, no una entidad. Se decidió así porque es un conjunto cerrado
que define el código y no cargan usuarios: da chequeo del compilador y `switch`
exhaustivo cuando se sumen ATS. La flexibilidad de una tabla sería ilusoria,
porque cada ATS necesita igual su propia clase para parsear su JSON, así que un
INSERT no evitaría recompilar. Hoy tiene **un solo valor, `GREENHOUSE`**, y
ningún campo ni método: el patrón de URL del board se le cuelga cuando el script
lo use.

`Company` tiene `id`, `ats` y `slug`, con `@UniqueConstraint` sobre el par
`(ats, slug)`. Constructor sin argumentos `protected` para JPA más uno público
`(Ats, String)`, y getters sin setters. El enum se mapea con
`@Enumerated(EnumType.STRING)` para que la columna guarde el texto y no una
posición numérica, que se corrompería al reordenar el enum.

**No tiene campo `name` a propósito**: el índice de CommonCrawl da el slug pero no
el nombre legible, así que el nombre se agrega en el paso donde se consulte la API
del ATS, que es cuando el dato existe.

`CompanyRepository` es un `JpaRepository<Company, Long>` pelado, sin métodos
propios. El `findByAtsAndSlug` que el script va a querer para no duplicar se
agrega en ese paso, con su test.

Esquema que Hibernate genera a partir de las anotaciones:

```sql
create table company (
  id bigint not null,
  slug varchar(255),
  ats enum ('GREENHOUSE'),
  primary key (id),
  unique (ats, slug)
)
```

H2 mapea el enum a su tipo nativo `enum('GREENHOUSE')` en vez de a `varchar`. Es
equivalente a nivel comportamiento; Postgres lo va a resolver distinto.

## Estado verificado

- `./mvnw test` → **3 tests, 0 fallas**: `BackendApplicationTests.contextLoads`
  más los dos de `CompanyRepositoryTest`.
- `CompanyRepositoryTest` (`@DataJpaTest`) prueba comportamiento, no anotaciones:
  1. `savesAndReadsBackAts` — guarda, hace flush y clear, relee por id y verifica
     que vuelven el mismo `ats` y el mismo `slug` (o sea que el mapeo del enum
     funciona en las dos direcciones).
  2. `rejectsSameSlugTwiceWithinAnAts` — al guardar dos veces el mismo par
     `(ats, slug)` salta `DataIntegrityViolationException`. Si la
     `@UniqueConstraint` no llegara al esquema real, el duplicado entraría sin
     quejarse y el test fallaría.
- `./mvnw spring-boot:run` → levanta en perfil `dev`, Tomcat en 8080, H2 en
  memoria, sin escribir nada en disco. Hot reload de devtools confirmado (~0,4 s).

## Qué NO existe todavía

- **El script de descubrimiento sobre CommonCrawl.** Es el próximo paso.
- Ningún cliente de API de ATS, ninguna vacante, ninguna lógica de matching.
- Ningún servicio ni endpoint HTTP propio.
- Ninguna forma de cargar empresas: la tabla `company` existe pero arranca vacía.
- Sin consola web de H2. Si hace falta inspeccionar la base desde el navegador,
  requiere la dependencia `org.springframework.boot:spring-boot-h2console` (en
  Spring Boot 4 la consola vive en su propio módulo) más
  `spring.h2.console.enabled=true`; sin la dependencia, `/h2-console` da 404.
- Sin migraciones de esquema (Flyway/Liquibase).
- El perfil prod está vacío: hoy la app solo corre en dev.

## Puntos abiertos

- `pom.xml` tiene la metadata (`name`, `description`, `url`, `licenses`,
  `developers`, `scm`) vacía, tal como la dejó el Initializr.
- El perfil prod está vacío y nunca se probó contra una base Postgres real.
- Con `create-drop` en memoria, todo lo que guarde el script se pierde al apagar
  la app. Hay que resolverlo antes o durante el paso del descubrimiento.
- Falta decidir **cómo se dispara** el script: un `CommandLineRunner` condicionado
  por un argumento o perfil, o una clase con `main` propia.

## Próximos pasos (candidatos, sin priorizar — decide Elias)

- **Paso siguiente acordado:** el script de descubrimiento contra el índice de
  CommonCrawl para Greenhouse — consultar el índice CDX
  (`index.commoncrawl.org/CC-MAIN-*-index?url=boards.greenhouse.io/*&matchType=prefix&output=json`)
  o el índice columnar en Parquet, extraer los slugs de las URLs y guardarlos como
  `Company`.
- Agregar `name` a `Company` al implementar el cliente de la API de Greenhouse.
- Modelar la vacante (`vacancy`), con relación **unidireccional `Vacancy → Company`**
  (decidido: la consulta real es "vacantes que matchean el perfil", no "vacantes de
  esta empresa", así que una colección en `Company` sería peso muerto y fuente de
  `LazyInitializationException`).
- Modelar el perfil del usuario (`profile`).
- Primer algoritmo de matching, simple, con tests sobre casos concretos.
- Endpoint HTTP para consultar las vacantes que matchean.
- Completar el perfil prod y elegir herramienta de migraciones cuando prod sea real.
