# Tema — normalización de títulos

Los tres extractores del título y la tabla `normalized_vacancy`. Lo que el código no dice:
porqués, lo medido y lo descartado. Las salidas crudas están en
`mediciones/primera-normalizacion-13-09-2026/` y
`mediciones/falsos-positivos-modalidad-13-09-2026/` (no se leen salvo que se pida).

## Para qué

El objetivo es **categorizar las vacantes en tech-adyacentes y no tech-adyacentes** con el
título como señal, y el título viene sucio de formato (`Sr. Software Engineer - Backend`)
y con **atributos metidos adentro**. Todas las reglas salieron de medir contra las 128.953
vacantes reales. La medición dejó claro que la categorización **tiene que ser por tokens y
no por diccionario de títulos**: casi la mitad de las vacantes tiene un título que no se
repite nunca.

Orden de la cadena (lo encadena `VacancyNormalizationService`, sin fachada):
`TitleCleaner.clean` → `WorkModeExtractor.extract(limpio, location)` →
`SeniorityExtractor.extract(mode.title())`. Hasta el 2026-09-12 las dos primeras eran
`TitleNormalizer`; se partió porque cada criterio nuevo la iba a agrandar.

## `TitleCleaner`

- **Lista blanca, no negra**: se saca todo lo que no sea letra, dígito o espacio, salvo
  `.` seguido de alfanumérico (`.net`, `node.js`), `#` precedido de alfanumérico (`c#`),
  `+` tras alfanumérico u otro `+` (`c++`) y `&` entre alfanuméricos (`r&d`). Enumerar lo
  que molesta siempre deja algo afuera.
- Minúsculas, sin diacríticos, apóstrofo borrado sin espacio (`womens`). Clases unicode
  (un título coreano no queda vacío) y se recompone a **NFC**: NFD parte el hangul y daría
  dos escrituras del mismo título.
- **Marca de género** (`h f`, `m w d`, `m f x`…) se va **solo entera** — la `e` de
  `assistant(e)` se queda —. Casi no colapsa títulos pero dejaba letras sueltas (`f` en
  1.607).
- **Sigla final redundante** se va **solo si son las iniciales de las palabras
  anteriores** (`registered behavior technician rbt`), 2-6 letras, salteando palabras de
  ≤2 letras (`engineer in test sdet`). Borrar lo que cierra el título a secas es la regla
  equivocada: ahí viven `.NET`, `(US)`, `(PRN)` y códigos postales.

## `WorkModeExtractor`

- **La señal vive en `location`**: de 16.444 remotas, 14.624 lo dicen solo ahí y 1.066 solo
  en el título. **`location` manda**; el título contesta solo si `location` no nombra
  modalidad.
- **`null` es "no lo declara"**, no `ONSITE` (112.508 vacantes; sería inventar el dato).
- **Sinónimos solo los que aparecen en los datos.** `telecommute`, `telework`,
  `teletrabajo`, `a distancia` dieron cero. **`virtual` afuera por decisión de Elias**
  ("Virtual Assistant" es un puesto).
- Frases largas primero; en una misma fuente `HYBRID` > `REMOTE` > `ONSITE`.
- **`FULLY_REMOTE`** (idea de Elias: puede aplicar cualquiera) = remoto con una `location`
  a la que, sin las frases de modalidad, **no le queda ningún token**. Conservador a
  propósito, sin lista de relleno: `"Remote, Anywhere"` queda `REMOTE`. Con relleno daba
  3.105; la corrida dio 2.928.

## `SeniorityExtractor`

- **Sale a un campo propio**: estaba en una de cada cuatro vacantes y sacarlo colapsa más
  títulos que toda la limpieza de caracteres.
- **No son seniority**: `director`, `lead`, `head`, `vp`, `chief` (son la función), el
  ambiguo `associate`, `intern` (contrato), `experienced` (medido: adjetivo del rol).
- **Guardas medidas**: `entry` solo con `level` (si no, `data entry`); `mid` solo con
  `level`, al final o en rango (si no, `mid market`); `staff` no cuenta tras `of`
  (`chief of staff`, `member of technical staff`), al final, ni antes de `nurse`,
  `accountant`, `attorney`.
- **Varios niveles → gana el más bajo**: la vacante publica el piso que acepta
  (`junior to senior`). El orden del enum es significativo; `STAFF` < `PRINCIPAL` es
  convención, no medido. `LEVEL_2`/`LEVEL_3` van al final del enum, así una palabra le gana
  al numeral (`senior … ii` → `SENIOR`).
- **Enum con nombre, no nivel 1-5**: `ii`/`iii` (1.941 + 460) no dicen a qué equivalen,
  `staff`/`principal` cada empresa los ordena distinto, y con un número no se reconstruye
  la vacante. El número se deriva del enum si hace falta; al revés no.

## Medido y descartado

Once criterios más; los siete recortes candidatos juntos bajan los títulos distintos apenas
**−2,4%**, contra −5,8% del seniority solo. Afuera: ubicación en el título (un diccionario
toma `west` o `park` por lugares), plata, marketing, fechas, números de requisición, idioma
requerido, contrato/jornada.

## Tabla `normalized_vacancy` y servicio

- **Tabla aparte y no columnas de `vacancy`**: `vacancy` es **espejo del board** (el sync
  pisa y borra); lo normalizado es **derivado** y se recalcula al cambiar una regla sin
  pegarle a la API. Juntarlos obligaría a `Vacancy.describe` a preservar columnas ajenas.
- **FK con `on delete cascade`** (con Elias): el sync borra sin conocer la tabla derivada.
- `title` nullable: `Principal` a secas queda sin título.
- `normalizeAll` (tras cambiar una regla) y `normalizeMissing` (solo vacantes sin fila).
- **Keyset por id, páginas de 1.000**, no offset: en `normalizeMissing` el conjunto se
  achica mientras se recorre y un offset saltearía filas.
- **Una transacción por página con `TransactionTemplate`**: una corrida cortada conserva lo
  hecho, y el template evita el problema del proxy que obligó a separar el sweep del sync.
- Sin pausa: no hay red.
- `POST /admin/normalization/vacancies` y `/vacancies/missing`: molde de siempre, **un solo
  `AtomicBoolean` compartido** porque escriben la misma tabla.

```bash
docker compose exec app curl -i -X POST 'localhost:8080/admin/normalization/vacancies'
docker compose logs -f app   # "Vacancy normalization (all) finished: N inserted, M updated"
```

## Probado en prod (2026-09-13)

`128953 inserted, 0 updated` en **24 s**, ninguna vacante sin fila. En dev no se probó a
mano. Coincide con lo medido antes de escribir las reglas:

| | Normalizado | Medido antes | Por qué difiere |
|---|---|---|---|
| `SENIOR` | 24.119 | ~24.230 | regla del mínimo |
| `STAFF` | 3.281 | 4.562 con la palabra | guardas (`staff accountant` 31, `chief of staff` 28, `staff attorney` 14, `member of technical staff` 8) y mínimo |
| `PRINCIPAL` | 1.880 | ~2.140 | mínimo |
| `LEVEL_2` / `LEVEL_3` | 1.587 / 364 | 1.941 / 460 | la palabra le gana al numeral |
| `JUNIOR` / `ENTRY` / `MID` / `SEMI_SENIOR` | 616 / 196 / 159 / 17 | — | — |
| Remoto (`REMOTE` + `FULLY_REMOTE`) | 16.094 (13.166 + 2.928) | ~16.444 | 193 quedan `HYBRID` a propósito; 31 sin modalidad por `remotely` |
| `HYBRID` | 2.340 | ~2.345 | — |
| `ONSITE` | 821 | ~876 | — |
| Títulos distintos | 77.630 | 87.647 crudos | −11,4% |
| Títulos nulos | 6 | 6 | `Principal` a secas |

Top: `software engineer` 884, `account executive` 505, `registered behavior technician`
466 (junta la variante con `rbt`), `behavior technician` 425, `product manager` 331,
`data engineer` 324. `accountant` baja de 182 a 134 porque `staff accountant` queda aparte.

## Abierto

- **Falsos positivos de modalidad sin guarda (Elias)**: 37 sobre 19.255 (0,19%), todos por
  el título: `hybrid casual` 13, `remote sensing` 7, `hybrid cloud` 4, `remote assist` 3,
  `home based primary care` 3, `onsite` como equipo 3, `hybrid electric`/`behavior
  planning` 2, `remote handling`/`care` como producto 2. Otros 9 pierden la palabra sin
  cambiar modalidad (`in-person giving` 8). ~5 con `location` `Remote` mal cargada
  (`Plumber - On-site` → `FULLY_REMOTE`). Guarda posible: palabra siguiente (`sensing`,
  `handling`, `assist`, `casual`, `cloud`, `electric`); no `remote care`, porque `Remote Care
  Coordinator` sí es remoto.
- **`remotely` no es sinónimo**: 31 falsos negativos (`Remotely based`). No se tocó.
- **Falso positivo de `senior` sin guarda (Elias)**: 37 de cuidado domiciliario (0,18%).
  Guarda posible: no contar tras `a`, `female`, `male`.
- **Rangos de nivel dejan conector suelto**: `controls engineer all levels to`. Nadie lo pidió.
- **Sin regla**: nivel `i` (no hay `LEVEL_1`, sin medir), `associate` (5.861, ambiguo),
  `virtual` como remoto (194 títulos, sin medir).
- **~650 filas que no son vacantes**: `talent community/network/pool` 356 y `general
  application / speculative / future opportunities` 296. No se filtran; se decide cuando el
  matching exista. Un título vacío sería señal, pero hoy casi ninguno se vacía.
- **Contrato y jornada merecen paso propio**: el mayor recorte descartado (5.117): `part
  time` 2.128, `intern` 1.939, `contract` 1.429, `full time` 914, `seasonal` 750, `per diem
  / prn` 690, `locum` 644. Antes, medir falsos positivos.
- **Ubicación estructurada merece paso propio desde `location`**: 70,7% lleva coma, 3,28
  tokens de promedio, cabeza `ciudad, región, país`. Absorbería los códigos postales del
  título (`thousand oaks ca 91359`).
- **Nada categoriza todavía** en tech-adyacente / no tech-adyacente.
