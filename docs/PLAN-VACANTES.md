# Plan — paso 2: las vacantes

> **Plan en curso.** Se borra cuando 2a y 2b estén terminados y probados a mano, y
> su contenido pasa a `docs/CONTEXTO.md`.
>
> Para retomar esto en una sesión nueva alcanza con leer, en este orden:
> `docs/METODOLOGIA.md` (cómo trabajamos), `docs/CONTEXTO.md` (qué hay hoy) y este
> archivo. **No leas `docs/para-humanos/`.**

## De dónde viene

El repo ya sabe dos cosas de cada empresa de Greenhouse: su **slug** (descubierto
desde CommonCrawl) y **qué contesta su board** (`NOT_FOUND`, `EMPTY` o `ACTIVE`,
averiguado en el paso 1). Falta lo único que realmente importa para el objetivo del
proyecto: **las vacantes**.

El paso 1 ya baja el JSON completo de cada board y **lo tira**: se queda con el total
y con el nombre de la empresa. Este paso lo aprovecha.

## Lo medido (2026-09-11). No hace falta volver a averiguarlo

### La escala

La corrida de sondeo **terminó**, y estos son los números finales:

```
ACTIVE      3121
NOT_FOUND    708
EMPTY        217
             ---- 4046
```

O sea que **el 77% de lo que descubrió CommonCrawl sigue vivo y con vacantes**.

Sobre una muestra de **40 empresas `ACTIVE` al azar** se sumaron sus totales de
vacantes: **3.208 vacantes**, media 80, **mediana 17**, p90 205, máximo 710. La
mediana tan por debajo de la media dice lo esperable: unas pocas empresas enormes y
una larga cola de empresas con menos de 20 búsquedas.

Proyectado sobre las 3.121 activas: **del orden de 250.000 vacantes**. Es bastante
más que las 50.000-100.000 que suponía la versión anterior de este plan.

### La API de Greenhouse con `content=true`

`GET https://boards-api.greenhouse.io/v1/boards/<slug>/jobs?content=true`

- **Sin paginación**: devuelve todas las vacantes de una. Forma:
  `{"jobs":[...], "meta":{"total":N}}`.
- **`content=true` multiplica la respuesta por 10-18x.** Medido: Splice 2 KB → 53 KB,
  Discord 35 KB → 383 KB, Ripple 117 KB → 1,8 MB. Una empresa de 700 vacantes se va a
  la decena de MB.
- Una vacante, con los campos que importan:

```json
{
  "id": 8172510,
  "title": "Abuse Investigator",
  "location": { "name": "Seattle, San Francisco, New York City" },
  "absolute_url": "https://stripe.com/jobs/search?gh_jid=8172510",
  "company_name": "Stripe",
  "updated_at": "2026-09-10T13:11:58-04:00",
  "first_published": "2026-09-09T10:50:29-04:00",
  "content": "&lt;div class=&quot;content-intro&quot;&gt;&lt;p&gt;…"
}
```

- **`content` viene HTML-escapeado dentro del JSON.** No es `<div>`: el valor literal
  del campo es `&lt;div class=&quot;…&quot;&gt;&lt;p&gt;…`. Consecuencia práctica:
  limpiarlo a texto plano son **dos desescapes**, uno para llegar al HTML real y otro
  después de quitar los tags, porque un `&nbsp;` del texto original llega como
  `&amp;nbsp;`.
- **La ubicación es texto libre.** No hay campo `country` ni nada parseable:
  `"Seattle, San Francisco, New York City"`, `"Remote - LATAM"`,
  `"Buenos Aires, Argentina"`. Filtrar por país es una heurística sobre strings y
  **no es parte de este paso**.
- Limpiado a texto plano, el promedio es de **~5.000 caracteres por vacante**. Sobre
  250.000 vacantes son **~1,2 GB de texto**, antes de la compresión que Postgres
  aplica sola a las columnas largas.

## Decisiones tomadas (cerradas, no volver a preguntarlas)

1. **Se guardan todas las vacantes.** Sin filtrar por país en la carga, coherente con
   la decisión ya anotada en `CONTEXTO.md`: el filtro por país es una consulta sobre
   las vacantes, no un descarte al cargarlas.
2. **Se pide `?content=true`.** La descripción es el único lugar donde están los
   requisitos reales —tecnologías, seniority, si el remoto es de verdad—; sin ella el
   matching futuro tendría solo el título.
3. **La descripción se guarda limpiada a texto plano**, no como HTML crudo.
4. **La limpieza se hace con Jsoup**, dependencia nueva aceptada explícitamente.
5. **Una vacante que desaparece del board se borra.** La tabla es un espejo del
   board: la foto de hoy, no la historia.
6. **Pausa de 500 ms entre empresas**, más conservadora que los 200 ms del sondeo
   porque acá cada respuesta pesa mucho más.

## Por qué el paso va partido en dos

Con `content=true` y 3.121 empresas, hacerlo de una sola vez daría un paso cuya
prueba manual es una corrida de una o dos horas — lo contrario de lo que pide
`METODOLOGIA.md`. Va partido:

### Paso 2a — el modelo y una empresa

Dejar guardadas en la base las vacantes de **una sola empresa** pedida por slug. Sin
recorrido masivo y sin borrado de las que desaparecen.

La forma prevista, sujeta a la conversación de diseño que va antes del código:

- `model/Vacancy` — relación **unidireccional** `Vacancy → Company` (`@ManyToOne`).
  La empresa no conoce sus vacantes: una `@OneToMany` con miles de elementos es un
  problema, no una comodidad. Identidad: **unique sobre `(company_id, external_id)`**,
  porque el id de Greenhouse es único dentro del board y no globalmente.
- `repository/VacancyRepository`.
- `util/` — la limpieza de HTML a texto plano, como **función pura**, al lado de
  `GreenhouseBoardUrl`.
- `service/GreenhouseBoardClient` — se le agrega traer las vacantes parseadas. Hoy
  baja la respuesta entera y la tira; lo único que cambia es que se mapea. **Hay que
  subir el `READ_TIMEOUT`**: los 30 s de hoy se calcularon para respuestas de "unos
  cientos de KB" y con `content=true` eso ya no vale.
- `service/` — la sincronización de **una** empresa: inserta lo nuevo y actualiza lo
  que ya estaba.
- `controller/` — un `POST` que cargue una empresa por slug. **Contesta en línea**,
  no con 202: una empresa sola tarda segundos, así que el molde de executor +
  `AtomicBoolean` de los otros dos endpoints no aplica y copiarlo sería ruido.
- `db/migration/V3__create_vacancy.sql` — **se agrega `V3`, no se tocan `V1` ni `V2`.**
  Necesita su propia secuencia con `increment by 50`, igual que `company_seq`: es el
  `allocationSize` que Hibernate espera, y si no coincide `validate` falla al arrancar.
  La descripción y la URL pasan de 255 caracteres, así que van como `text` y la
  entidad tiene que declararlo, porque con `ddl-auto=validate` Hibernate espera
  `varchar(255)` para un `String` pelado y la app no levanta si no coinciden.

### Paso 2b — el recorrido completo

- `repository/CompanyRepository` — un método para traer las empresas por estado de
  board, y pedirle vacantes solo a las `ACTIVE`.
- `service/` — el recorrido de las 3.121 con la pausa de 500 ms y **el borrado de las
  vacantes que ya no están en el board**. Sin transacción por corrida, igual que el
  sondeo: una corrida interrumpida tiene que conservar lo ya cargado.
- `controller/` — el `POST` masivo, ahí sí con el molde ya usado dos veces: 202 al
  toque, executor de un solo hilo, `AtomicBoolean` para el 409, resultado al log.
- La corrida real, estimada en **1 a 2 horas** (500 ms × 3.121 son 26 minutos de
  pausa sola, más el peso de cada descarga).

## Cómo se cierra cada uno

Como siempre (ver `docs/METODOLOGIA.md`): tests de comportamiento en el mismo paso,
guion de prueba manual con los comandos exactos, y el paso cierra **cuando Elias lo
probó a mano**. Cuando 2b esté cerrado se actualiza `docs/CONTEXTO.md` y se borra
este archivo.
