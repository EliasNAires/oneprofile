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

Este archivo cuenta qué es el sistema y cómo correrlo. Cada uno de los dos procesos
tiene el suyo:

- [**Cómo se descubren las empresas**](descubrimiento.md) — de dónde sale la lista de
  empresas y cómo se saca el slug de cada URL.
- [**Cómo se categoriza cada empresa**](sondeo.md) — cómo se separa lo que sigue vivo
  de lo que no, y quién tiene vacantes abiertas.

---

## Qué es esto

El objetivo final es armar **una buena lista de vacantes laborales que matcheen con
el perfil del usuario**.

Hoy están construidas las dos primeras piezas:

1. **Descubrir qué empresas usan Greenhouse.**
2. **Sondear el board de cada una** para saber cuáles siguen vivas y cuáles tienen
   vacantes publicadas hoy.

Todavía **no hay vacantes guardadas**, ni perfiles, ni matching. De cada empresa se
sabe su identificador corto, si su board existe, cuántas búsquedas tiene abiertas y
cómo se llama.

## La idea de fondo

Las empresas no publican sus vacantes en su propia web: contratan un **ATS**
(Applicant Tracking System) —Greenhouse, Lever, Ashby— y el ATS les da un "board"
público con las búsquedas abiertas.

Eso es bueno, porque cada ATS tiene una API para leer las vacantes de una empresa.
Pero esa API te pide saber **de qué empresa** querés las vacantes, y para eso hace
falta su **slug**: el identificador corto que aparece en la URL de su board.

```
https://boards.greenhouse.io/mercadolibre/jobs/4567   ← el board público
                             ^^^^^^^^^^^^
                             esto es el "slug"

https://boards-api.greenhouse.io/v1/boards/mercadolibre/jobs   ← y con eso se piden
                                           ^^^^^^^^^^^^          las vacantes
```

El slug es lo único que se consigue de arranque: el nombre legible ("Mercado Libre")
sale recién al preguntarle a la API. De dónde salen esos slugs es toda una historia,
y está en [cómo se descubren las empresas](descubrimiento.md).

Un detalle que importa: **el slug no es único en el mundo, solo dentro de su ATS.**
Puede haber un `acme` en Greenhouse y otro `acme` en Lever que son empresas
distintas. Por eso acá una empresa se identifica por el **par (ATS, slug)**.

## Las piezas

![Panorama de componentes](diagramas/panorama.svg)

Son dos columnas casi paralelas —un proceso cada una— que se juntan abajo, en la
misma tabla. Cada clase tiene un trabajo bien chico:

Del **descubrimiento**:

- **`DiscoveryController`** — recibe el pedido por HTTP. No sabe nada del negocio:
  solo se ocupa de que no haya dos corridas a la vez y de contestar rápido.
- **`GreenhouseDiscoveryService`** — el que dirige la orquesta. Junta los slugs,
  descarta los que ya tenía y guarda los nuevos.
- **`CommonCrawlIndexClient`** — le pide páginas al índice y las va leyendo.
- **`GreenhouseBoardUrl`** — convierte una URL en un slug. Es texto que entra y
  texto que sale: sin red, sin base, sin nada.

Del **sondeo**:

- **`BoardProbeController`** — el gemelo del otro, con el mismo molde. Que se lean
  igual es a propósito: el patrón ya estaba probado.
- **`GreenhouseBoardProbeService`** — recorre las empresas de a una y anota qué
  contestó cada board.
- **`GreenhouseBoardClient`** — el que le habla a la API de Greenhouse.

Y **`CompanyRepository`**, que es de los dos: habla con Postgres.

Que los dos clientes que salen a internet estén en clases separadas no es capricho.
Son dos servicios ajenos con mañas distintas: el índice de CommonCrawl manda
respuestas de 9 MB y se satura seguido, la API de Greenhouse manda respuestas chicas
y usa el `404` para decirte algo. Mezclarlos sería meter dos problemas en una caja.

## Qué se guarda

![Modelo de datos](diagramas/modelo-de-datos.svg)

Sigue siendo **una sola tabla**. La mitad de arriba es lo que deja el
descubrimiento —qué ATS y qué slug—; la de abajo, lo que deja el sondeo: el nombre,
en qué estado está el board y cuándo se lo sondeó por última vez.

Esos tres campos **pueden estar vacíos**, y eso significa algo preciso: una empresa
sin estado de board es una empresa **que nunca se sondeó**. Las 4.000 que cargó el
descubrimiento arrancaron así.

La fecha del último sondeo todavía no la usa nadie. Está para cuando esto corra solo:
con ella se puede pedir "volvé a sondear lo que no se toca hace una semana" en vez de
repasar las 4.000 cada vez.

`Ats` y `BoardStatus` son enums de Java y no tablas. La razón: son listas cerradas
que define el código, no algo que cargue un usuario. Sumar un ATS obliga igual a
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

# 1. descubrir empresas (unos minutos)
docker compose exec app curl -i -X POST \
  'localhost:8080/admin/discovery/greenhouse?index=CC-MAIN-2026-34'

# 2. sondear sus boards (alrededor de media hora)
docker compose exec app curl -i -X POST \
  'localhost:8080/admin/probe/greenhouse'

docker compose logs -f app    # acá se ve cómo terminan
```

Los dos te contestan `202` al instante y siguen trabajando por atrás. Si disparás uno
que ya está corriendo, te contesta `409` y no hace nada: dos corridas a la vez se
pelearían por las mismas empresas.

Para ver el resultado del sondeo, en la base:

```sql
select board_status, count(*) from company group by board_status;
```

El `index=` no tiene valor por defecto, y es a propósito: uno fijo quedaría viejo
en silencio. Los que existen salen de
[`collinfo.json`](https://index.commoncrawl.org/collinfo.json) — **ojo que la
numeración salta**, no todos los números existen.

**Repetir el POST cambiando el índice es la forma de tener más empresas**, y no
hace falta tocar código: lo que ya está guardado no se vuelve a insertar. Cada
crawl nuevo suma empresas que los anteriores no habían visto.

## Qué sigue

Lo próximo es **traer las vacantes de verdad** de las ~3.000 empresas activas y
guardarlas. Ahí aparece el problema interesante: los títulos son texto libre, escrito
por cada empresa a su manera, y para que el matching sirva hay que hacer que
"Sr. Backend Engineer", "Backend Developer Senior" y "SWE II - Backend" se reconozcan
como el mismo tipo de puesto. Eso todavía no está resuelto ni decidido.
