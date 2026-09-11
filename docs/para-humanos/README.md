# oneprofile / backend — cómo funciona

> **Esto es para personas, no para agentes.**
>
> Está escrito para entenderse rápido, no para ser exhaustivo. El detalle completo
> —cada decisión, cada trampa, qué está verificado y qué no— vive en
> [`docs/CONTEXTO.md`](../CONTEXTO.md); la forma de trabajar, en
> [`docs/METODOLOGIA.md`](../METODOLOGIA.md).
>
> **Si sos un agente: no leas esta carpeta.** Todo lo que hay acá está duplicado de
> `docs/CONTEXTO.md` en una versión resumida. Leerlo gasta contexto en información
> repetida y te arriesga a trabajar sobre el resumen en vez de sobre la fuente de
> verdad.

---

## Qué es esto

El objetivo final es armar **una buena lista de vacantes laborales que matcheen con
el perfil del usuario**.

Hoy está construida solo la primera pieza, y es esta: **descubrir qué empresas usan
Greenhouse**. Todavía no hay vacantes, ni perfiles, ni matching. De cada empresa se
sabe un identificador corto y nada más.

## La idea de fondo

Las empresas no publican sus vacantes en su propia web: contratan un **ATS**
(Applicant Tracking System) —Greenhouse, Lever, Ashby— y el ATS les da un "board"
público con las búsquedas abiertas.

Eso es bueno, porque cada ATS tiene una API para leer las vacantes de una empresa.
Pero esa API te pide saber **de qué empresa** querés las vacantes, y no hay ninguna
lista pública de empresas por ATS. Ese es el problema que había que resolver
primero.

La salida fue **CommonCrawl**: un archivo público que recorre internet y guarda un
índice de todas las URLs que vio. Si le preguntás por todo lo que empiece con
`boards.greenhouse.io/`, te devuelve decenas de miles de direcciones, y adentro de
cada una está el nombre de la empresa:

```
https://boards.greenhouse.io/mercadolibre/jobs/4567   ← una URL que vio CommonCrawl
                             ^^^^^^^^^^^^
                             esto es el "slug"

https://boards-api.greenhouse.io/v1/boards/mercadolibre/jobs   ← y con eso se piden
                                           ^^^^^^^^^^^^          las vacantes
```

El **slug** es ese identificador corto. Es lo único que el índice te da: el nombre
legible ("Mercado Libre") no aparece ahí, sale recién al preguntarle a la API.

Un detalle que importa: **el slug no es único en el mundo, solo dentro de su ATS.**
Puede haber un `acme` en Greenhouse y otro `acme` en Lever que son empresas
distintas. Por eso acá una empresa se identifica por el **par (ATS, slug)**.

Como preguntarle al índice es caro y lento, esto **no corre solo**: se dispara a
mano cuando hace falta, y el resultado queda guardado en la base.

## Las piezas

![Panorama de componentes](diagramas/panorama.svg)

Cinco clases, cada una con un trabajo bien chico:

- **`DiscoveryController`** — recibe el pedido por HTTP. No sabe nada del negocio:
  solo se ocupa de que no haya dos corridas a la vez y de contestar rápido.
- **`GreenhouseDiscoveryService`** — el que dirige la orquesta. Junta los slugs,
  descarta los que ya tenía y guarda los nuevos.
- **`CommonCrawlIndexClient`** — el único que sale a internet. Le pide páginas al
  índice y las va leyendo.
- **`GreenhouseBoardUrl`** — convierte una URL en un slug. Es texto que entra y
  texto que sale: sin red, sin base, sin nada.
- **`CompanyRepository`** — habla con Postgres.

## Qué pasa cuando disparás el descubrimiento

![Flujo del descubrimiento](diagramas/flujo-descubrimiento.svg)

Lo importante de este dibujo es que **el POST te contesta al toque**, en un
segundo, con un `202 Accepted` que quiere decir "lo tomé, andá tranquilo". El
trabajo de verdad tarda varios minutos y sigue en otro hilo. **La única forma de
ver cómo terminó es mirar el log:**

```
Greenhouse discovery started on CommonCrawl index CC-MAIN-2026-34
Greenhouse discovery finished: 4046 slugs found, 4046 new companies saved
```

Lo otro que se ve ahí: el cliente **nunca junta todas las URLs en una lista**. Va
leyendo la respuesta línea por línea y entregando una URL a la vez, porque una sola
página del índice pesa unos 9 MB y son unas 12.000 líneas.

Y los slugs se acumulan en un conjunto (un `Set`), que es lo que hace que las
repeticiones se resuelvan solas: la misma empresa aparece en cientos de URLs
distintas y termina siendo una sola entrada.

## Cómo se saca el slug de una URL

![De URL a slug](diagramas/url-a-slug.svg)

Parece trivial —"tomá lo que viene después de la barra"— pero no lo es. Entre las
76.765 URLs de una corrida hay `robots.txt`, hay dominios pelados sin nada
después, y hay una porción grande de **boards embebidos** (`/embed/job_board?for=X`),
que son el mismo board metido dentro de la web de carreras de la empresa. Si esos
se tiraran a la basura se perderían muchísimas empresas.

De las 76.765 capturas de la última corrida salieron **4.046 empresas distintas**, y
solo 229 URLs quedaron afuera. Ese número se verificó aparte, calculándolo fuera de
la app: no se está perdiendo nada.

## Qué se guarda

![Modelo de datos](diagramas/modelo-de-datos.svg)

Una sola tabla, con lo mínimo: qué ATS y qué slug.

**No hay nombre de empresa**, y eso es a propósito: CommonCrawl no lo da, así que se
va a agregar cuando exista el dato, no antes.

`Ats` es un enum de Java y no una tabla más. La razón: es una lista cerrada que
define el código, no algo que cargue un usuario. Sumar un ATS obliga igual a
escribir la clase que entiende *su* formato de JSON —porque no hay dos ATS que
devuelvan lo mismo—, así que tenerlo en una tabla no evitaría recompilar nada.

## Dónde corre

![Entorno](diagramas/entorno.svg)

Hay dos formas de levantarlo, y la diferencia que más se nota es que **en
producción no hay ningún puerto abierto**: para hablarle a la app hay que meterse
adentro del container.

En los dos casos pasa lo mismo al arrancar: Flyway aplica las migraciones que
falten y después Hibernate compara las clases contra las tablas reales. Si alguien
cambió una clase y se olvidó la migración, **la app no arranca** — que es
exactamente lo que se quiere.

## Cómo lo probás vos

En desarrollo, para ver que arranca:

```bash
./mvnw spring-boot:run
```

Levanta solo el Postgres en Docker y queda escuchando en el 8080.

En producción, el ciclo completo:

```bash
cd prod
cp .env.example .env          # solo la primera vez, después completalo
docker compose up --build -d

docker compose exec app curl -i -X POST \
  'localhost:8080/admin/discovery/greenhouse?index=CC-MAIN-2026-34'

docker compose logs -f app    # acá se ve cómo termina
```

El `index=` no tiene valor por defecto, y es a propósito: uno fijo quedaría viejo
en silencio. Los que existen salen de
[`collinfo.json`](https://index.commoncrawl.org/collinfo.json) — **ojo que la
numeración salta**, no todos los números existen.

**Repetir el POST cambiando el índice es la forma de tener más empresas**, y no
hace falta tocar código: lo que ya está guardado no se vuelve a insertar. Cada
crawl nuevo suma del orden de un 25-30% más de empresas que los anteriores no
habían visto.
