# Plan — paso 2: modelar y persistir las vacantes

> **Plan en curso.** Se borra cuando esté terminado y probado a mano, igual que se
> hizo con `PLAN-DESCUBRIMIENTO.md`.
>
> Para retomar esto en una sesión nueva alcanza con leer, en este orden:
> `docs/METODOLOGIA.md` (cómo trabajamos), `docs/CONTEXTO.md` (qué hay hoy) y este
> archivo. **No leas `docs/para-humanos/`.**

## De dónde viene

El repo ya sabe dos cosas de cada empresa de Greenhouse: su **slug** (descubierto
desde CommonCrawl) y **qué contesta su board** (`NOT_FOUND`, `EMPTY` o `ACTIVE`,
averiguado en el paso 1). Lo que falta es lo único que realmente importa para el
objetivo del proyecto: **las vacantes**.

El paso 1 ya baja el JSON completo de cada board y **lo tira**: se queda con el
total y con el nombre de la empresa. Este paso lo aprovecha.

### El número que cambia la escala

La corrida de sondeo dio, de ~4.000 empresas, aproximadamente **~3.000 `ACTIVE`,
~300 `NOT_FOUND` y ~80 `EMPTY`** (parcial, leído con la corrida en curso). Eso
significa que **casi todo lo que descubrió CommonCrawl sigue vivo**, bastante más de
lo que se esperaba.

Con ~3.000 empresas activas, esto ya no es un juguete: **el orden de magnitud de las
vacantes a guardar son decenas de miles, probablemente entre 50.000 y 100.000**. La
muestra medida a mano da una idea de la dispersión: Stripe 628, GitLab 223, Figma
155, Vercel 84, Discord 44, CockroachDB 21, Airtable 16 — y son todas empresas
grandes y conocidas, así que la mediana real va a ser bastante más baja que esos
números. **Confirmar el total real es lo primero que hay que hacer en este paso**,
antes de decidir nada.

## Lo que ya está medido de la API (no hace falta volver a probarlo)

`GET https://boards-api.greenhouse.io/v1/boards/<slug>/jobs`

- **Sin paginación**: devuelve todas las vacantes de una. Stripe, 628 vacantes,
  392 KB.
- Forma: `{"jobs":[...], "meta":{"total":N}}`.
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
  "requisition_id": "See Opening ID",
  "internal_job_id": 3537063,
  "metadata": null,
  "language": "en",
  "education": "education_required",
  "data_compliance": [ ... ],
  "application_deadline": null
}
```

- **La ubicación es texto libre.** No hay campo `country` ni nada parseable:
  `"Seattle, San Francisco, New York City"`, `"Remote - LATAM"`,
  `"Buenos Aires, Argentina"`. Filtrar por país va a ser una heurística sobre
  strings, y **no es parte de este paso**.
- Existe `?content=true`, que agrega la descripción completa en HTML. **Engorda
  mucho la respuesta** y no se sabe todavía para qué haría falta.

## Forma prevista

**Nuevos**

- `model/Vacancy` — relación **unidireccional** `Vacancy → Company` (`@ManyToOne`).
  La empresa no conoce sus vacantes; no hace falta y una colección `@OneToMany` con
  miles de elementos es un problema, no una comodidad.
  Campos: `externalId` (el `id` de Greenhouse), `title`, `location` (texto libre),
  `absoluteUrl`, `updatedAt`, `firstPublished`.
  Identidad: **unique sobre `(company_id, external_id)`**, porque el id de Greenhouse
  es único dentro del board, no globalmente.
- `repository/VacancyRepository`.
- `service/GreenhouseVacancyService` — el recorrido: para cada empresa `ACTIVE`, pide
  las vacantes y las persiste.
- `controller/` — un `POST` que dispare la carga, con el mismo molde ya usado dos
  veces (202 + executor de un solo hilo + `AtomicBoolean` para el 409 + resultado al
  log).
- `db/migration/V3__create_vacancy.sql` — **se agrega `V3`, no se toca `V1` ni `V2`.**
  Va a necesitar su propia secuencia con `increment by 50`, igual que `company_seq`:
  es el `allocationSize` que Hibernate espera y si no coincide `validate` falla al
  arrancar.

**Modificados**

- `service/GreenhouseBoardClient` — hoy cuenta las vacantes y descarta el resto. Se
  extiende para devolverlas parseadas. La respuesta ya se baja entera: lo único que
  cambia es que se mapea en vez de tirarse.
- `repository/CompanyRepository` — un método para traer las empresas por estado de
  board, para pedirle vacantes solo a las `ACTIVE`.

## Decisiones a tomar ANTES de escribir código (preguntarle a Elias)

Ninguna de estas tiene respuesta obvia y todas cambian el diseño. **No asumir:
preguntar.**

1. **Qué pasa con una vacante que desaparece del board.** Las dos salidas son
   borrarla (el board es la fuente de verdad, la tabla es un espejo) o marcarla
   cerrada con un `closed_at` (se conserva la historia y se puede responder "cuánto
   duró abierta esta búsqueda", pero la tabla crece para siempre). Depende de si a
   Elias le interesa la historia o solo la foto de hoy.
2. **Si se pide `?content=true`.** La descripción en HTML es el único lugar donde
   están los requisitos reales (tecnologías, seniority, si es remoto de verdad). Sin
   ella, el matching futuro solo tiene el título. Con ella, las respuestas se
   multiplican en tamaño y hay que decidir dónde guardar decenas de miles de
   documentos HTML.
3. **Si se guardan todas las vacantes o solo algunas.** Con ~3.000 empresas activas
   pueden ser ~100.000 filas. Guardar todo es lo más simple y lo más flexible;
   filtrar por país en la carga sería ir en contra de una decisión ya tomada y
   anotada en `CONTEXTO.md` (el filtro por país es una consulta, no un descarte en la
   carga), pero con este volumen vale la pena volver a preguntarlo.
4. **El ritmo.** ~3.000 requests, esta vez de respuestas grandes. La pausa de 200 ms
   del sondeo dio ~30 minutos para 4.046 boards; acá el peso de cada respuesta es
   mayor y conviene confirmar si se mantiene ese ritmo.

## Cómo arrancar

Antes de cualquier decisión, **medir el volumen real**, que es barato y cierra la
pregunta 3:

```sql
select count(*) from company where board_status = 'ACTIVE';
```

Y sumar los totales de vacantes: el paso 1 no guardó el `jobCount` de cada board (no
se pidió), así que ese número hay que sacarlo aparte, con un script fuera de la app
sobre una muestra de slugs `ACTIVE`.

## Cómo se cierra

Como siempre (ver `docs/METODOLOGIA.md`): tests de comportamiento en el mismo paso,
guion de prueba manual con los comandos exactos, y el paso cierra **cuando Elias lo
probó a mano**. Después se actualiza `docs/CONTEXTO.md` y se borra este archivo.
