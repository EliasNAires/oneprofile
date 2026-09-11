# Plan — paso 2: las vacantes

> **Plan en curso. 2a está cerrado. 2b está escrito y con tests, pero sin probar a mano.
> Falta 2c.** Lo que ya existe está descrito en `docs/CONTEXTO.md` (sección "Las
> vacantes"), así que **acá queda el guion de prueba de 2b y lo que todavía no existe**,
> más las mediciones y decisiones. Este archivo se borra cuando el paso 2 entero esté
> terminado y probado a mano.
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

### Los campos del endpoint (medido el 2026-09-11)

Sondeados 9 boards: splice, discord, airbnb y stripe con `content=true` (842 vacantes)
y figma, ramp, brex, coinbase y reddit (2.410 vacantes más). Sobre esa muestra:

- `id` es **único dentro del board** (626/626 en stripe) y llega a 8.801.523.002 →
  **bigint, no int**. `internal_job_id` **no** es único (603 sobre 626).
- `departments` viene siempre y siempre con **un solo** elemento (842/842).
- **`pay_transparency=true`** —un parámetro más en la misma request, se combina con
  `content=true`— agrega `pay_input_ranges`, con rango salarial estructurado en
  **426 de 787** vacantes. Cuando hay, es **siempre exactamente 1** rango
  (`min_cents`, `max_cents`, `currency_type`, `title`), así que van columnas planas y
  no tabla hija. Monedas vistas: USD, INR, GBP, CAD, EUR, PHP, SGD, BRL, AED.
  **25 de esos 426 rangos son por hora**, y lo único que los distingue de un anual es
  el `title` (`"Hourly Rate:"` vs `"Annual base salary range…"`), texto libre.
- `offices` da un `location` normalizado con país, pero **solo en 107/842 (13%)**; el
  resto trae `null` y un `name` que es vocabulario de cada empresa (`"US"`,
  `"Ireland Locations"`, `"US-PERM"`).
- `metadata` son campos personalizados por empresa (solo 211/842): hay señal útil
  (`"Workplace Type" → "Hybrid"`) pero sin esquema común entre empresas.
- Largos máximos medidos: `title` 94, `location.name` 185, `departments[0].name` 60,
  `absolute_url` 76, `language` 2, `currency_type` 3, `title` del rango 54.
  **Corrige lo que decía la versión anterior de este plan**: la URL no pasa de 255
  caracteres. Va igual como `text`, porque una URL no tiene largo acotado, pero la
  justificación era falsa.
- `language`: 832 `en`, 9 `fr`, 1 `ja`.
- `application_deadline` existe pero es rarísimo (0 de 842 en los boards con content).

## Decisiones tomadas (cerradas, no volver a preguntarlas)

1. **Se guardan todas las vacantes.** Sin filtrar por país en la carga, coherente con
   la decisión ya anotada en `CONTEXTO.md`: el filtro por país es una consulta sobre
   las vacantes, no un descarte al cargarlas.
2. **Se pide `?content=true&pay_transparency=true`.** La descripción es el único
   lugar donde están los requisitos reales —tecnologías, seniority, si el remoto es de
   verdad—; sin ella el matching futuro tendría solo el título. Y
   `pay_transparency=true` es lo que convierte el salario en números en la mitad de
   las vacantes, en vez de una frase enterrada en la descripción.
3. **La descripción se guarda limpiada a texto plano**, no como HTML crudo.
4. **La limpieza se hace con Jsoup**, dependencia nueva aceptada explícitamente.
5. **Una vacante que desaparece del board se borra.** La tabla es un espejo del
   board: la foto de hoy, no la historia.
6. **Pausa de 500 ms entre empresas**, más conservadora que los 200 ms del sondeo
   porque acá cada respuesta pesa mucho más.
7. **Las columnas de `vacancy`**, decididas con las mediciones de arriba:
   `external_id` (el `id` del ATS), `title`, `location` (el `location.name` crudo, sin
   parsear), `department` (el `departments[0].name`), `description`, `url`, `language`,
   `pay_min_cents`, `pay_max_cents`, `pay_currency`, `pay_title`, `first_published` y
   `updated_at`. **No** se guardan `offices`, `metadata`, `internal_job_id`,
   `requisition_id`, `education`, `employment`, `application_deadline`,
   `data_compliance` ni los `ai_*`; `company_name` tampoco, porque ya está en
   `Company.name` desde el paso 1.
8. **El `title` del rango salarial se guarda crudo**, sin derivar un enum anual/hora:
   eso sería una heurística sobre texto libre de cada empresa y no es parte de esto.
9. **No se descartan vacantes por antigüedad.** Se evaluó no cargar las que tienen
   `updated_at` de más de dos meses y **se decidió que no**: el endpoint ignora
   `updated_after` (probado con un corte de hoy y con el valor `basura`), así que no hay
   ahorro de descarga; y el recorte es chico y desparejo —sobre 8 boards y 1.630
   vacantes da 7%, con splice, figma, discord y stripe en 0% y brex en 17%—, porque mide
   hábitos de cada equipo de RRHH más que vigencia de la búsqueda. La recencia se usa
   **al buscar**, no como descarte en la carga.
10. **El filtro por país sigue sin existir.** Los 107 `offices` con país normalizado no
   alcanzan para basar nada, así que la ubicación queda como el texto libre que es.

## Por qué el paso va partido

Con `content=true` y 3.121 empresas, hacer la carga de una sola vez daría un paso cuya
prueba manual es una corrida de una o dos horas — lo contrario de lo que pide
`METODOLOGIA.md`. De ahí 2a y 2b. El 2c es otra cosa: lo pidió Elias sobre el final,
cuando quedó claro que esto son procesos que tienen que correr seguido y no en su
máquina.

### Paso 2a — el modelo y una empresa — **HECHO** (2026-09-11)

Quedó `POST /admin/vacancies/greenhouse/{slug}`, que carga las vacantes de una empresa
y contesta en línea con `{"fetched":N,"inserted":N,"updated":N}`. Probado a mano por
Elias contra la API real: splice 5, discord 45, figma 153 con 96 salarios. **El detalle
de las clases, los campos y los tests está en `docs/CONTEXTO.md`**, no acá.

Lo que se implementó, contra lo que estaba previsto abajo, con dos desvíos que vale
anotar: se agregó `pay_transparency=true` (decisión 2) y el `READ_TIMEOUT` subió a
**120 s**. Lo previsto era:

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

### Paso 2b — el recorrido completo — **escrito, falta probarlo a mano**

El código está y `./mvnw test` da **62 tests en verde**. Lo que quedó hecho está descrito
en `docs/CONTEXTO.md`: el borrado dentro de `syncCompany`, el
`GreenhouseVacancySweepService`, el `POST /admin/vacancies/greenhouse` y el método
`findSlugsByAtsAndBoardStatus`. Dos decisiones que se tomaron al implementarlo:

- **El borrado vive dentro de `syncCompany`**, así que el endpoint por slug también borra
  y su respuesta ganó un contador `deleted`. Hay un solo camino de sincronización.
- **El POST masivo vive en `VacancyController`**, al lado del que ya existía: mismo
  recurso, dos alcances.

**Lo que falta es la prueba manual.** Guion:

**1. En dev, con pocas empresas** (la base de dev arranca vacía):

```bash
./mvnw spring-boot:run
```

```sql
insert into company (id, ats, slug, board_status) values
  (nextval('company_seq'), 'GREENHOUSE', 'splice',  'ACTIVE'),
  (nextval('company_seq'), 'GREENHOUSE', 'discord', 'ACTIVE'),
  (nextval('company_seq'), 'GREENHOUSE', 'figma',   'ACTIVE'),
  (nextval('company_seq'), 'GREENHOUSE', 'notion',  'NOT_FOUND');
```

```bash
curl -i -X POST 'localhost:8080/admin/vacancies/greenhouse'
```

Tiene que dar **202** al toque, y en el log `Greenhouse vacancy sweep started` y al
terminar algo del orden de `3 companies, 203 fetched, 203 inserted, 0 updated, 0 deleted,
0 failed`. Un segundo POST mientras corre da **409**.

```sql
select c.slug, count(v.id) from company c left join vacancy v on v.company_id = c.id
group by c.slug order by 2 desc;
```

`notion` tiene que quedar en **0**: no es `ACTIVE`, no se le pidió nada.

**2. Idempotencia y borrado.** Repetir el POST: todo pasa a `updated`, `inserted:0`.
Después, meter una vacante que el board no tiene y ver que la corrida siguiente se la
lleva, diciendo `1 deleted`:

```sql
insert into vacancy (id, company_id, external_id, title)
values (nextval('vacancy_seq'), (select id from company where slug='splice'), 999999999, 'Fantasma');
```

**3. La corrida real, en prod** (estimada en 1 a 2 horas; 500 ms × 3.121 son 26 minutos de
pausa sola, más el peso de cada descarga):

```bash
cd prod
docker compose up --build -d
docker compose exec app curl -i -X POST 'localhost:8080/admin/vacancies/greenhouse'
docker compose logs -f app
```

```sql
select count(*) from vacancy;
select count(distinct company_id) from vacancy;
```

Los números de esa corrida son la primera medición real del volumen: hasta ahora las
250.000 vacantes son una proyección de una muestra de 40 empresas.

### Paso 2c — desplegar en un servidor por SSH — **a planificar**

Declaración de Elias, tal como la dio (2026-09-11):

> "Vamos a añadir un paso 2c después de reiniciar el chat. Voy a desplegar esto en un
> servidor por SSH, porque ya estamos hablando de procesos constantes, a futuro en
> paralelo con más ATS y normalización de las vacantes. El paso 2c va a ser una guía de
> despliegue."

**Todavía no está planificado**: no hay servidor elegido, ni decisiones tomadas, ni
alcance definido. Se planifica en la sesión que lo arranque.

## Cómo se cierra cada uno

Como siempre (ver `docs/METODOLOGIA.md`): tests de comportamiento en el mismo paso,
guion de prueba manual con los comandos exactos, y el paso cierra **cuando Elias lo
probó a mano**. Cuando 2c esté cerrado se actualiza `docs/CONTEXTO.md` con los números
reales y se borra este archivo.
