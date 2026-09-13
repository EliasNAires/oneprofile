# Tema — vacantes: carga desde Greenhouse

Del board de Greenhouse a filas en `vacancy`, de una empresa o de todas las `ACTIVE`. Lo
que el código no dice: porqués, lo medido y lo descartado. La medición de departamento e
idioma está en `docs/agents/MEDICION-VACANTES.md`; el sondeo que decide qué empresas se
cargan, en `docs/agents/tema/descubrimiento.md`.

## La API de `/jobs`, medida a mano

`GET /v1/boards/<slug>/jobs?content=true&pay_transparency=true`, sondeado sobre 9 boards
(splice, discord, airbnb, stripe con `content`: 842 vacantes; figma, ramp, brex, coinbase,
reddit: 2.410 más).

- **`content=true` multiplica la respuesta 10-18x** y agrega `departments` y `offices`
  (splice 2 KB → 53 KB, discord 35 KB → 383 KB, stripe 4,7 MB). Por eso el read timeout
  del client es 120 s y no 30.
- **`content` viene HTML-escapeado dentro del JSON** (`&lt;div…&gt;`, `&amp;nbsp;`):
  limpiarlo son **dos desescapes**, y de ahí `HtmlToText` (Jsoup, con versión explícita
  porque el parent de Boot no la gestiona). Devuelve `null` si no queda texto.
- **`pay_transparency=true`** trae `pay_input_ranges` en **426 de 787**, siempre
  **exactamente 1** rango → columnas planas, no tabla hija. Monedas: USD, INR, GBP, CAD,
  EUR, PHP, SGD, BRL, AED. **25 son por hora** y lo único que lo dice es el `title` del
  rango, texto libre: `payTitle` se guarda **crudo**, sin derivar un enum anual/hora.
- **`id` es único dentro del board** (626/626 en stripe) y llega a 8.801.523.002: no entra
  en un int. `internal_job_id` **no** es único (603/626). Identidad de `Vacancy`:
  `(company_id, external_id)`.
- `departments` viene siempre con **un solo** elemento (842/842).
- **La ubicación no es parseable**: `location.name` es texto libre (`"Remote - U.S."`,
  `"AMER"`, `"Atlanta; New York"`). `offices` trae país normalizado solo en 107/842 (13%).
  `offices` no se guarda; `metadata` (211/842, campos propios de cada empresa, sin esquema
  común) tampoco.
- Largos máximos: `title` 94, `location.name` 185, departamento 60, `absolute_url` 76. La
  URL va como `text` igual: no tiene largo acotado.
- `absolute_url` **no siempre apunta a Greenhouse** (626 de 842 van a `stripe.com`, 166 a
  `careers.airbnb.com`).
- **No se puede filtrar por fecha**: `updated_after` se ignora (hasta con `basura`). El
  board viene entero o no viene.
- Tampoco se guardan `internal_job_id`, `requisition_id`, `education`, `employment`,
  `application_deadline`, `data_compliance`, los `ai_*` ni `company_name` (ya está en
  `Company.name`).

## Vacantes viejas: se evaluó descartarlas y no (2026-09-11)

Idea: no cargar las de `updated_at` de más de dos meses. Descartada midiendo:

- No hay ahorro de descarga (el endpoint no filtra por fecha).
- Recorte chico y desparejo: sobre 8 boards y 1.630 vacantes, 7% total; splice, figma,
  discord y stripe 0%, brex 17%. Hay empresas que reescriben `updated_at` en bloque: el
  corte mide hábitos de RRHH, no vigencia.
- Error asimétrico: una vacante muerta cuesta KB; borrar una viva no se recupera.

**Se guardan todas**; la recencia se usa al buscar. La señal fuerte de abandono es que
**desaparezca del board**, y eso lo captura el borrado del sync.

## Modelo

- `Vacancy` → `Company` es `@ManyToOne` **unidireccional**: una `@OneToMany` de miles de
  elementos es un problema, no una comodidad.
- `Vacancy` no se renombró a `GreenhouseVacancy`: el formato del ATS lo absorbe
  `GreenhouseBoardClient.BoardJob`, y un segundo ATS caería en las mismas columnas. Se
  reevalúa con ese segundo ATS y sus datos.
- Las columnas `text` obligan a `@Column(columnDefinition = "text")`: si no, `validate`
  espera `varchar(255)` y la app no levanta.

## Sync de una empresa y recorrido masivo

- **`GreenhouseVacancySyncService.syncCompany`**: carga lo guardado de la empresa en un
  `Map` y hace `remove` por cada vacante del board; **lo que queda en el mapa es lo que el
  board ya no tiene** y se borra. Es `@Transactional` (una empresa tarda segundos).
- **`GreenhouseVacancySweepService`** es clase aparte **para que el `@Transactional` de
  `syncCompany` aplique**: una llamada dentro del mismo bean saltea el proxy de Spring.
  **No es transaccional** (la corrida dura horas: retendría una conexión y perdería todo).
  500 ms entre empresas; una que falla (p. ej. un board muerto desde el sondeo da 404) va
  a `WARN` y `failed` y no aborta.
- **Solo pide a las `ACTIVE`**: trabaja sobre la foto del último sondeo, así que el orden
  correcto es sondeo → vacantes, y nada lo fuerza.
- `findSlugsByAtsAndBoardStatus` devuelve slugs y no entidades: `syncCompany` recibe un slug.

## Endpoints

- `POST /admin/vacancies/greenhouse/{slug}` **contesta en línea** con los contadores (404
  si la empresa no se conoce) y **no mira `board_status`**: sirve para probar una empresa
  puntual.
- `POST /admin/vacancies/greenhouse` usa el molde de siempre: 202, un hilo, 409 si corre,
  resultado al log.

Avance de una corrida masiva (el endpoint solo da 202), en el servidor:

```bash
sudo docker compose exec -T postgres sh -c \
  'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "select count(*), count(distinct company_id) from vacancy"'
```

Elias lo corre en bucle cada 60 s; el script vive en el home del servidor, sin versionar.

**Dev arranca vacía**: los datos viven en prod. Para probar la carga en dev:
`insert into company (id, ats, slug) values (nextval('company_seq'), 'GREENHOUSE', 'figma');`

## Probado

- **Carga de una empresa en dev contra la API real** (Elias): splice
  `{"fetched":5,"inserted":5,"updated":0}` y al repetir `inserted:0, updated:5`
  (idempotente); discord 45; figma 153, **96 con salario** (`16500000`-`19000000` USD,
  `"Annual Base Salary Range:"`). Descripciones de 5.008 a 8.102 caracteres, sin tags ni
  entidades. Slug desconocido → 404.
  - Para verificar la limpieza se buscan **tags y entidades**
    (`description ~ '</[a-zA-Z]'`, `&amp;nbsp;`, `&amp;amp;`), no `<` suelto: figma tiene
    `"(<5000 FTEs)"` legítimo.
- **Recorrido masivo en prod (2026-09-11/12): 128.953 vacantes sobre 3.118 empresas**, de
  3.121 `ACTIVE`. Tres quedaron sin vacantes; no se averiguó si cerraron o fallaron.
  **Desmintió la proyección** de ~250.000: la media de la muestra (80 por empresa) tiraba
  para arriba; la mediana (17) era la guía.

## Abierto

- **El sync no normaliza**: una vacante nueva queda sin fila en `normalized_vacancy` hasta
  correr `/missing`, y un título cambiado conserva la fila vieja. El borrado sí lo cubre el
  cascade. Importa con el cron (sondeo → vacantes → normalización). Elias pidió anotarlo.
- **Sin tope de tamaño de respuesta**: un board de 700 vacantes con `content` son varios MB
  parseados en memoria. Entra hoy; nadie midió el peor caso.
- **`MEDICION-VACANTES.md` dice 85.050 títulos distintos y la base 87.647**, con el mismo
  `count(*)`. Ese doc no registró su SQL. Sin resolver.
- **Departamento y ubicación no se normalizan**: se decidió solo el título por ahora.
- **Los endpoints de administración no tienen protección**. Hoy el puerto no se publica; se
  decide cuando exista un endpoint que devuelva vacantes.
