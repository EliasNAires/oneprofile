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

Este archivo cuenta qué es el sistema y cómo correrlo. Cada uno de los cuatro procesos
tiene el suyo:

- [**Cómo se descubren las empresas**](descubrimiento.md) — de dónde sale la lista de
  empresas y cómo se saca el slug de cada URL.
- [**Cómo se categoriza cada empresa**](sondeo.md) — cómo se separa lo que sigue vivo
  de lo que no, y quién tiene vacantes abiertas.
- [**Cómo se traen las vacantes**](vacantes.md) — qué pide la API, las dos trampas del
  JSON, y qué se guarda de cada vacante.
- [**Cómo se normalizan los títulos**](normalizacion.md) — cómo un título escrito de mil
  maneras queda comparable, con el nivel y la modalidad en campos propios.

Y aparte, cómo se pone esto a correr fuera de tu máquina:

- [**Cómo se despliega**](despliegue.md) — el pipeline que publica la imagen en cada
  commit, y cómo se levanta y se actualiza en el servidor.

---

## Qué es esto

El objetivo final es armar **una buena lista de vacantes laborales que matcheen con el
perfil del usuario**.

Hoy están construidas cuatro piezas:

1. **Descubrir qué empresas usan Greenhouse**, leyendo los 10 índices más recientes de
   CommonCrawl.
2. **Sondear el board de cada una** para saber cuáles siguen vivas y cuáles tienen
   vacantes publicadas hoy.
3. **Traer las vacantes**, de una empresa o de todas las activas de una pasada, y borrar
   las que dejaron de estar publicadas.
4. **Normalizar los títulos**: limpiarlos y sacarles el nivel y la modalidad.

Las cuatro corrieron de verdad en el servidor. Hoy hay **6.988 empresas**, **128.953
vacantes de 3.118 empresas**, y todas esas vacantes con su título normalizado.

El número de vacantes tiene una sorpresa adentro: se esperaba **el doble**. La proyección
se había hecho con el **promedio** de una muestra —80 vacantes por empresa—, y el promedio
estaba inflado por un puñado de empresas enormes: Stripe sola tiene 628. La guía correcta
era la **mediana**, que era 17. En datos así, el promedio miente y la mediana no.

Todavía no hay perfiles ni matching.

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

Son cuatro caminos casi paralelos —uno por proceso— que se juntan abajo, en la misma
base. El código está ordenado por **capa**, y cada capa tiene un trabajo:

- **`controller`** — recibe el pedido por HTTP. No sabe nada del negocio: se ocupa de
  que no haya dos corridas a la vez y de contestar rápido. Hay uno por proceso.
- **`service`** — el que dirige la orquesta de cada proceso: junta, decide y guarda.
- **`client`** — los que salen a internet. **Uno por servicio ajeno**:
  `CommonCrawlIndexClient` y `GreenhouseBoardClient`.
- **`util`** — funciones chicas: entra algo, sale algo, sin red y sin base.
  `GreenhouseBoardUrl` saca el slug de una URL, `HtmlToText` limpia la descripción,
  `HttpRetry` reintenta cuando un servicio ajeno falla, y los tres extractores del título
  hacen la normalización.
- **`repository`** — los que hablan con Postgres, uno por tabla.

Algunas cosas del dibujo que no son obvias:

- **Los dos clientes están separados aunque se parezcan.** Son dos servicios ajenos con
  mañas distintas: el índice de CommonCrawl manda respuestas de 9 MB y se satura seguido,
  la API de Greenhouse manda respuestas chicas y usa el `404` para decirte algo. Lo que sí
  hacen igual —reintentar— está una sola vez, en `HttpRetry`.
- **`GreenhouseBoardClient` es el mismo para el sondeo y para las vacantes.** El sondeo
  pide el board pelado y solo cuenta cuántas vacantes hay; la carga lo pide con la
  descripción y el sueldo. Es el mismo servicio ajeno, así que es la misma clase.
- **Recorrer todas las empresas es una clase aparte** (`GreenhouseVacancySweepService`) y
  no un método más del que carga una. **Spring abre una transacción cuando entrás a un
  objeto desde afuera, no cuando un objeto se llama a sí mismo.** Si el recorrido viviera
  adentro del mismo servicio, las horas de corrida quedarían sin esa red por empresa.

## Qué se guarda

![Modelo de datos](diagramas/modelo-de-datos.svg)

Son **tres tablas**, una por proceso que deja algo.

En **`company`**, la mitad de arriba es lo que deja el descubrimiento —qué ATS y qué
slug—; la de abajo, lo que deja el sondeo: el nombre, en qué estado está el board y
cuándo se lo sondeó por última vez. Esos tres campos **pueden estar vacíos**, y eso
significa algo preciso: una empresa **que nunca se sondeó**.

En **`vacancy`** hay dos cosas que vale la pena mirar. La primera: **la vacante sabe de
qué empresa es, pero la empresa no tiene la lista de sus vacantes.** Es a propósito: una
empresa grande tiene cientos de búsquedas, y una lista así colgada de la empresa se
convierte en una trampa —cada vez que tocás una empresa te arrastra todo lo demás—. La
segunda: cada vacante guarda el **id que le puso Greenhouse**, para reconocerla la
próxima vez. Ese id solo es único dentro de su board, así que lo que no se puede repetir
es el par (empresa, id de Greenhouse).

En **`normalized_vacancy`** hay una fila por vacante, con el título limpio, el nivel y la
modalidad. Está aparte porque `vacancy` es un espejo del board y esto es un cálculo sobre
él, que se rehace cuando cambia una regla. Si se borra la vacante, su fila se va con ella.

`Ats`, `BoardStatus`, `Seniority` y `WorkMode` son enums de Java y no tablas: son listas
cerradas que define el código, no algo que cargue un usuario.

## Dónde corre

![Entorno](diagramas/entorno.svg)

Hay dos formas de levantarlo. **Desarrollo corre en tu máquina y producción en un
servidor**, con la imagen bajada del registry: cómo llega hasta ahí está en
[cómo se despliega](despliegue.md). La diferencia que más se nota es que **en
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

Levanta solo el Postgres en Docker y queda escuchando en el 8080. Ojo: esa base arranca
vacía, los datos están en el servidor.

En producción —o sea, en el servidor— el ciclo completo:

```bash
cd ~/oneprofile
docker compose pull
docker compose up -d

# 1. descubrir empresas en los 10 índices más nuevos (minutos)
docker compose exec app curl -i -X POST \
  'localhost:8080/admin/discovery/greenhouse/commoncrawl'

# 2. sondear sus boards (media hora o más)
docker compose exec app curl -i -X POST \
  'localhost:8080/admin/probe/greenhouse'

# 3a. traer las vacantes de una empresa (un segundo)
docker compose exec app curl -i -X POST \
  'localhost:8080/admin/vacancies/greenhouse/figma'

# 3b. o las de todas las empresas activas (horas)
docker compose exec app curl -i -X POST \
  'localhost:8080/admin/vacancies/greenhouse'

# 4. normalizar los títulos (segundos)
docker compose exec app curl -i -X POST \
  'localhost:8080/admin/normalization/vacancies'

docker compose logs -f app    # acá se ve cómo terminan
```

**Todos te contestan `202` al instante y siguen trabajando por atrás**, menos el de una
empresa sola, que contesta en la misma respuesta con la cuenta de lo que cargó
(`{"fetched":153,"inserted":153,"updated":0,"deleted":0}`). Si disparás uno que ya está
corriendo, te contesta `409` y no hace nada.

**El orden de arriba es el orden real**, no una sugerencia: el recorrido de vacantes solo
le pregunta a las empresas que el sondeo dejó activas, y la normalización trabaja sobre
las vacantes que ya están.

Para ver el resultado del sondeo, en la base:

```sql
select board_status, count(*) from company group by board_status;
```

## Qué sigue

Lo próximo es **separar las vacantes tech-adyacentes de las que no**, que es para lo que
se normalizaron los títulos. Ya se sabe que tiene que ser **por palabras del título y no
con una lista de títulos**: casi la mitad de las vacantes tiene un título que no se
repite nunca.

Más adelante, sin orden fijo: el perfil del usuario, un primer matching, un endpoint que
devuelva las vacantes que matchean, y un cron que corra todo solo.

## Puntos abiertos

Lo que se sabe que falta o que no está resuelto:

- **Faltan muchas empresas por descubrir.** La **Wayback Machine** trae 17.730 slugs, y
  8.035 no aparecen en ningún índice de CommonCrawl. Está medido y diseñado, pero no hay
  nada escrito.
- **El descubrimiento sobre 10 índices todavía no corrió en prod.** Está escrito y con
  tests. Si da lo esperado, `company` queda cerca de 8.614 empresas.
- **Hay 2.942 empresas sin sondear**, las que trajeron los últimos índices. Sin sondeo no
  se les piden vacantes.
- **La modalidad puede tener falsos positivos.** Nadie miró todavía en qué contexto
  aparece `remote`: "remote sensing" o "remote monitoring" son puestos, no modalidades.
  Hasta revisarlo, la modalidad no es del todo confiable.
- **Traer vacantes no normaliza.** Una vacante nueva queda sin título normalizado hasta
  que se corre la normalización. Hoy da igual porque todo es a mano; con un cron habrá que
  encadenar sondeo → vacantes → normalización.
- **Hay ~650 filas que no son vacantes**: "talent community", "general application" y
  parecidos, que son formularios para dejar el CV. No se filtran todavía.
- **Contrato, jornada y ubicación siguen adentro del texto.** "Part time", "intern" o
  "contract" pesan en un matching tanto como el nivel. La ubicación es más ordenable de lo
  que parecía (ciudad, región, país). Cada uno merece su propio paso.
- **Una respuesta cortada de CommonCrawl pasaría sin aviso.** A mano se vio que el índice
  a veces corta la página con un `200`. El cliente no tiene cómo notarlo, y reportaría
  menos empresas sin avisar.
- **Hay empresas que se dejan afuera a sabiendas.** Las del dominio europeo de Greenhouse
  (unas 850) y los slugs con símbolos raros, por volumen.
- **Nada protege los endpoints de administración.** Hoy no hace falta porque no hay
  ningún puerto abierto. Cuando exista un endpoint que devuelva vacantes, habrá que
  resolverlo.
- **No hay cómo volver a una versión anterior.** La imagen se publica solo como `latest`,
  y el despliegue al servidor es a mano.
