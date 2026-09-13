# Plan — normalizar los títulos de las vacantes

> Documento **autocontenido**: pensado para que otra sesión lo retome leyendo solo
> este archivo, `docs/METODOLOGIA.md` y `docs/CONTEXTO.md`. Se borra cuando el plan
> termina y lo que valga la pena pasa a `CONTEXTO.md`.

## Estado del plan

- **Paso A — medir la distribución del título: HECHO.** Corrido contra prod el
  2026-09-12 y analizado. Los números y su lectura están más abajo; las decisiones que
  dejó, en "Decisiones que cerró el paso A".
- **Paso B — el normalizador (función pura): HECHO, a falta de que Elias corra los
  tests.**
  - **B1 — falsos positivos de los tokens de seniority: MEDIDO Y ANALIZADO.** Salida en
    `medicion-seniority.txt`.
  - **B1b — la cabeza de `senior` a fondo: MEDIDO Y ANALIZADO.** Salió de que el top 15
    de B1 dejaba el caso sospechado fuera de la ventana. Salida y análisis en
    `medicion-senior.txt`.
  - **B2 — el código: ESCRITO.** `Seniority`, `TitleNormalizer` y
    `TitleNormalizerTest`. `./mvnw test` da **80 tests en verde** (eran 62). En B4
    `TitleNormalizer` se partió en dos clases y ya no existe.
- **Paso B3 — más criterios de limpieza, medidos: MEDIDO Y ANALIZADO.** Corrido el
  2026-09-12; Elias leyó las salidas antes del análisis. Salidas en
  `medicion-ubicacion.txt` y `medicion-ruido.txt`, análisis en "El análisis de B3".
  **El resultado es mayormente negativo**: de once hipótesis, entran tres.
- **Paso B4 — el refactor y las tres reglas que B3 sostuvo: ESCRITO.** `TitleCleaner`,
  `SeniorityExtractor`, `WorkModeExtractor` y `WorkMode`, con sus tests. `./mvnw test`
  da **95 tests en verde** (eran 80).
- **Paso C — la tabla y el proceso que la puebla: ESCRITO, falta la corrida contra prod.**
  `V4`, `NormalizedVacancy`, su repositorio, `VacancyNormalizationService` y
  `NormalizationController`. `./mvnw test` da **103 tests en verde** (eran 95). Decidido con
  Elias el 2026-09-13: la FK lleva `on delete cascade` (el sync borra vacantes sin conocer
  la tabla derivada) y hay **dos endpoints**, `POST /admin/normalization/vacancies`
  (recalcula todo) y `POST /admin/normalization/vacancies/missing` (solo las que no tienen
  fila). Recorren por keyset de id en páginas de 1.000, cada una en su transacción.

**Por dónde arrancar la próxima sesión:** el paso C. Las reglas de los tres extractores
están cerradas y probadas; B4 lo dio por probado Elias el 2026-09-13.

**Pendiente: ver los falsos positivos de la extracción de modalidad.** Los sinónimos de
`WorkModeExtractor` salieron de contar apariciones (P3), pero nadie miró en qué contexto
aparecen —como sí se hizo con el seniority en B1—. Casos a revisar: `remote` como parte
del puesto (`remote sensing`, `remote monitoring`), `field based` e `in person`.

**Atajo de lectura:** las secciones "Resultados" y "Segunda ronda" son **salida cruda de
psql, larga y saltéable**. Lo que hay que leer sí o sí es "Decisiones ya tomadas", "El
análisis", "Decisiones que cerró el paso A", los pasos B1 y B2, "El análisis de B3", B4
y C. De B3, las secciones del SQL también se pueden saltear.

Las salidas crudas de las mediciones viven en la raíz del repo, **sin versionar**
(se borran cuando el plan termine):

| Archivo | Qué contiene |
|---|---|
| `medicion-titulos.txt` | Paso A, primera ronda: distribución del título limpio. Analizada. |
| `medicion-titulos-ronda2.txt` | Paso A, segunda ronda: cuantificación del seniority. Analizada. |
| `medicion-seniority.txt` | Paso B1: falsos positivos de cada token de seniority. Analizada. |
| `medicion-senior.txt` | Paso B1b: la cabeza de `senior` a fondo. **Trae el análisis escrito adentro**, arriba de la salida cruda. |
| `medicion-ubicacion.txt` | Paso B3, familia P: modalidad y ubicación. Analizada. |
| `medicion-ruido.txt` | Paso B3, familia Q: ruido y atributos en el título. Analizada. |

## A dónde vamos

El objetivo del paso grande es **categorizar las vacantes en tech-adyacentes y
no-tech-adyacentes**, para después profundizar sobre las primeras. La señal va a ser
el **título**.

Por qué el título y no el departamento: `docs/MEDICION-VACANTES.md` ya lo midió y el
departamento no sirve como señal primaria. El 59,6% de las vacantes cae en un balde
desconocido, el balde está lleno de acrónimos internos de cada empresa (`max ahp` 993
vacantes, `ne team` 523, `lmdiv` 355) y las categorías grandes no son homogéneas:
`sales` contiene Sales Engineer, `operations` contiene DevOps, `marketing` contiene
Growth Engineer. El departamento queda como señal de desempate más adelante; la
descripción, como último recurso.

Antes de categorizar hay que bajarle el ruido al título **sin perder datos**, y antes
de escribir el normalizador hay que **ver la distribución**. De ahí los dos pasos.

## Decisiones ya tomadas

### Dónde van los datos normalizados

Van a una **tabla nueva 1:1**, `normalized_vacancy`, con FK única a `vacancy`.
**No se renombra `Vacancy` a `GreenhouseVacancy`.**

Se evaluó ese rename —crear una `GreenhouseVacancy` con los datos crudos del ATS y una
`Vacancy` de dominio con los normalizados— y se descartó por ahora. El motivo: las
columnas de `vacancy` **ya son un modelo propio y no la forma del JSON de Greenhouse**.
Lo que absorbe el formato del ATS es `GreenhouseBoardClient.BoardJob`, que mapea la
respuesta a 13 campos elegidos a mano y descarta a propósito `offices`, `metadata`,
`internal_job_id`, `requisition_id` y los `ai_*`. Un segundo ATS tendría su propio
cliente y caería en las mismas columnas. El rename se reevalúa cuando ese segundo ATS
exista y se vea **con datos** si su forma cruda difiere de verdad.

La separación de capas que sí es real es otra, y es la que justifica la tabla nueva:
`vacancy` es un **espejo del board** —el sync sobreescribe lo que cambió y borra lo que
el board ya no tiene— mientras que lo normalizado es **derivado**: se recalcula cuando
cambia la regla, sin volver a pegarle a la API. Mezclarlos en la misma tabla obligaría
a que `Vacancy.describe(...)` preserve columnas que no vienen del board.

### Qué se normaliza

**Solo el título**, en este plan. `location` y `department` cuando haya un paso que los
use.

### Cómo se guarda el seniority: enum nombrado, sin nivel numérico

Se evaluó normalizarlo a un **nivel numérico** —1 a 5, o 1 a 3— y se descartó. La duda
que lo motivaba era buena y es la que cerró la decisión: un nivel **no permite
reconstruir la vacante**. Tres razones, todas de los datos ya medidos:

- **`ii` y `iii` no tienen traducción a una escala.** ¿"Engineer II" es mid o junior?
  Los datos no lo dicen, y son 1.941 + 460 vacantes. Con `LEVEL_2` / `LEVEL_3` se
  guarda lo que el título decía; con un número hay que inventar la equivalencia.
- **`staff` y `principal` no son "más que senior" en un eje único.** Son carril de
  contribuidor individual, y el orden entre ellos es convención de cada empresa. Un 1-5
  obliga a decidirlo hoy, sin un caso de uso que lo valide.
- **Con el enum el título se recompone** (`SENIOR` + `software engineer` →
  "senior software engineer"); un `3` pierde qué palabra estaba.

Y lo decisivo: **el nivel numérico es derivable del enum** cuando el matching lo
necesite —un método, o un `order by` en la consulta—, mientras que al revés no: si se
guarda 1-5 ahora, la información original ya se fue. Así que no se agrega hasta que haya
un pedido concreto que lo use.

### Quién lee la salida primero

Las mediciones exploratorias —las que sirven para decidir **cómo modelar**, no para
verificar un número puntual— **las lee Elias antes de que yo las analice**, para
formarse su propio criterio sin que mi lectura se lo anticipe. El entregable de una
medición es entonces: la corro yo, dejo el `.txt` crudo en la raíz del repo, informo si
los controles de sanidad dieron bien, y **me guardo el análisis**. Por eso este
documento describe qué contesta cada consulta y por qué se pide: es lo que le permite
leer la salida sin mí.

### La regla de limpieza

Es una **lista blanca, no una lista negra**: se saca **todo** lo que no sea letra,
dígito o espacio, salvo cuatro caracteres que se conservan a propósito. Enumerar lo que
molesta (`@ - \ | , / ( ) : "`, emojis…) siempre deja alguno afuera; enumerar lo que
importa es un conjunto chico y cerrado que se justifica caracter por caracter.

Los pasos, en orden:

1. **Minúsculas.**
2. **Sin diacríticos** (`á` → `a`), así `Ingeniería` e `Ingenieria` colapsan. El crudo
   queda intacto en `vacancy`.
3. **El apóstrofo se borra sin dejar espacio**: `women's health` → `womens health`, no
   `women s health`. Es el único caracter que se borra en vez de convertirse en espacio.
4. **Todo lo demás que no sea letra, dígito o espacio se convierte en espacio**, no se
   borra: `sr-software-engineer` → `sr software engineer`, no `srsoftwareengineer`.
5. **Colapsar espacios y trimear.**

Los cuatro caracteres que sobreviven al paso 4:

| Char | Por qué | Ejemplos |
|---|---|---|
| `.` | Tecnologías con punto | `.net`, `node.js`, `vue.js`, `asp.net` |
| `+` | `c++` y derivados | `c++`, `notepad++` |
| `#` | Lenguajes con sostenido | `c#`, `f#` |
| `&` | `docs/MEDICION-VACANTES.md` ya documentó el bug de cortar por `&`: convirtió `R&D` en los tokens `r` (597) y `d` (533), que son basura | `r&d`, `at&t`, `p&l` |

Y se conservan **solo donde significan algo**, que es lo que evita tener que elegir
entre romper `.net` y arrastrar el punto de `Engineer.`:

- `.` se conserva solo si **le sigue** un alfanumérico → `.net` y `node.js` sobreviven;
  `engineer.` y `sr.` pierden el punto.
- `#` se conserva solo si le **precede** un alfanumérico → `c#` sobrevive, `#hiring`
  no. `+` igual, **pero también cuenta como precedente otro `+`**: sin eso `c++`
  quedaba en `c+`, que es el bug que encontró la primera corrida.
- `&` se conserva solo si está **entre** alfanuméricos → `r&d` sobrevive;
  `sales & marketing` queda `sales marketing`.

**Se conservan las letras no latinas.** La lista blanca es "letra unicode", no `a-z`,
así que los títulos en coreano, japonés y chino no quedan vacíos. Sale gratis y evita
un agujero; el filtro por idioma se hace después con la columna `language`, que
`docs/MEDICION-VACANTES.md` midió **poblada al 100%** (`en` 123.847, `pt` 1.805,
`fr` 1.314, `de` 562, `es` 446, `ko` 302, `ja` 300 y diez valores más).

## Paso A — medir la distribución del título

**Sin código Java, sin migraciones, sin dependencias.** La regla de limpieza se simula
en SQL para ver la distribución antes de escribir el normalizador.

**Lo corre Elias** y mira los números él antes de que yo los analice.

### Cómo se entra

```bash
ssh elitedesk1
cd ~/oneprofile
```

Y se manda todo el SQL por stdin, guardando la salida:

```bash
docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
  > ~/medicion-titulos.txt <<'SQL'
... (todo el bloque de abajo, la vista y las consultas) ...
SQL

cat ~/medicion-titulos.txt
```

**No escribe nada en la base:** la vista es temporal, vive solo en esa sesión de psql.

### La regla, simulada en SQL

Se lee de adentro hacia afuera, en el orden de los cinco pasos:

```sql
create temp view v as
select id, company_id, language, title,
  btrim(regexp_replace(                                      -- 8. colapsar espacios
    regexp_replace(                                          -- 7. todo lo demas a espacio
      regexp_replace(                                        -- 6. & solo entre alfanumericos
        regexp_replace(                                      -- 5. + precedido por alfanumerico o por otro +
          regexp_replace(                                    -- 4. # solo precedido por alfanumerico
            regexp_replace(                                  -- 3. . solo si le sigue un alfanumerico
              regexp_replace(                                -- 2. apostrofos, sin dejar espacio
                regexp_replace(normalize(lower(title), NFD), -- 1. minusculas y diacriticos
                               '[\u0300-\u036f]', '', 'g'),
                '[''\u2019]', '', 'g'),
              '\.(?![[:alnum:]])', ' ', 'g'),
            '(?<![[:alnum:]])#', ' ', 'g'),
          '(?<![[:alnum:]+])\+', ' ', 'g'),
        '(?<![[:alnum:]])&|&(?![[:alnum:]])', ' ', 'g'),
      '[^[:alnum:] .+#&]', ' ', 'g'),
    '\s+', ' ', 'g')) as clean
from vacancy;
```

Tres cosas de este SQL que no son obvias:

- **`normalize(..., NFD)` es Postgres nativo.** Descompone la letra acentuada en letra
  base + marca combinante, y el primer `regexp_replace` borra las marcas
  (`̀-ͯ` es el bloque de diacríticos combinantes). No hace falta la extensión
  `unaccent`, que habría que crear en prod.
- **`[[:alnum:]]` en Postgres es unicode-aware**, así que los pasos 3 a 6 tratan a una
  letra coreana como alfanumérica y no la borran. Eso es lo que conserva los títulos no
  latinos sin una regla aparte.
- Los *lookahead* `(?!...)` y *lookbehind* `(?<!...)` los soporta el motor de regex de
  Postgres. **Si alguna de esas líneas diera error de sintaxis, pará y avisá**: la regla
  se puede escribir sin ellos, pero sale bastante más larga.

### Las consultas

```sql
\echo === A. control: volumen y titulos que se quedan vacios ===
select count(*) as vacantes,
       count(*) filter (where title is null or btrim(title) = '') as vacio_crudo,
       count(*) filter (where clean = '') as vacio_limpio,
       round(avg(length(title)), 1) as largo_medio_crudo,
       round(avg(length(clean)), 1) as largo_medio_limpio
from v;

\echo === B. cuanto colapsa, y que parte de la regla lo aporta ===
select count(distinct title) as distintos_crudos,
       count(distinct lower(title)) as distintos_solo_minusculas,
       count(distinct clean) as distintos_limpios
from v;

\echo === C. cola larga: crudo vs limpio ===
with c as (select title as t, count(*) as n from v where title is not null group by 1),
     l as (select clean as t, count(*) as n from v where clean <> '' group by 1)
select 'crudo' as version, count(*) as distintos,
       count(*) filter (where n = 1) as aparecen_una_vez,
       round(100.0 * sum(n) filter (where n = 1) / sum(n), 1) as pct_vacantes_en_la_cola
from c
union all
select 'limpio', count(*), count(*) filter (where n = 1),
       round(100.0 * sum(n) filter (where n = 1) / sum(n), 1)
from l;

\echo === D. cobertura acumulada de los top N, LIMPIO ===
with l as (select clean as t, count(*) as n from v where clean <> '' group by 1),
     r as (select t, n, row_number() over (order by n desc) as rn,
                  sum(n) over (order by n desc rows unbounded preceding) as acum,
                  sum(n) over () as total
           from l)
select rn, n as vacantes, round(100.0 * acum / total, 1) as pct_acumulado
from r where rn in (10, 20, 50, 100, 200, 500, 1000, 5000, 10000) order by rn;

\echo === E. cobertura acumulada de los top N, CRUDO ===
with c as (select title as t, count(*) as n from v where title is not null group by 1),
     r as (select t, n, row_number() over (order by n desc) as rn,
                  sum(n) over (order by n desc rows unbounded preceding) as acum,
                  sum(n) over () as total
           from c)
select rn, n as vacantes, round(100.0 * acum / total, 1) as pct_acumulado
from r where rn in (10, 20, 50, 100, 200, 500, 1000, 5000, 10000) order by rn;

\echo === F. top 30 titulos LIMPIOS ===
select clean as titulo, count(*) as vacantes
from v where clean <> '' group by 1 order by 2 desc limit 30;

\echo === G. top 30 titulos CRUDOS ===
select title as titulo, count(*) as vacantes
from v where title is not null group by 1 order by 2 desc limit 30;

\echo === H. que colapso: crudos distintos que caen al mismo limpio ===
select clean as titulo_limpio,
       count(distinct title) as variantes_crudas,
       count(*) as vacantes,
       left(string_agg(distinct title, ' | '), 180) as ejemplos
from v where clean <> '' group by 1 having count(distinct title) > 1
order by 2 desc limit 25;

\echo === I. sobreviven los tokens tecnicos (las dos columnas tienen que dar igual) ===
select 'c++' as token,
       count(*) filter (where title ~* 'c\+\+') as en_crudo,
       count(*) filter (where clean ~ 'c\+\+') as en_limpio from v
union all select 'c#',      count(*) filter (where title ~* 'c#'),       count(*) filter (where clean ~ 'c#') from v
union all select 'f#',      count(*) filter (where title ~* 'f#'),       count(*) filter (where clean ~ 'f#') from v
union all select '.net',    count(*) filter (where title ~* '\.net'),    count(*) filter (where clean ~ '\.net') from v
union all select 'node.js', count(*) filter (where title ~* 'node\.js'), count(*) filter (where clean ~ 'node\.js') from v
union all select '.js',     count(*) filter (where title ~* '\.js'),     count(*) filter (where clean ~ '\.js') from v;

\echo === J. que caracteres ASCII no alfanumericos quedan (solo deberian ser . + # &) ===
with ch as (select regexp_split_to_table(clean, '') as c from v where clean <> '')
select c as caracter, count(*) as apariciones
from ch where c !~ '[a-z0-9 ]' and c ~ '[[:ascii:]]'
group by 1 order by 2 desc limit 40;

\echo === J2. los cuatro guardados, en contexto ===
select 'punto' as caracter, count(*) filter (where clean ~ '\.') as titulos_con_el,
       left(string_agg(distinct clean, ' | ') filter (where clean ~ '\.'), 300) as ejemplos from v
union all select 'mas',       count(*) filter (where clean ~ '\+'), left(string_agg(distinct clean, ' | ') filter (where clean ~ '\+'), 300) from v
union all select 'gato',      count(*) filter (where clean ~ '#'),  left(string_agg(distinct clean, ' | ') filter (where clean ~ '#'), 300) from v
union all select 'ampersand', count(*) filter (where clean ~ '&'),  left(string_agg(distinct clean, ' | ') filter (where clean ~ '&'), 300) from v;

\echo === K. ruido semantico: atributos pegados al titulo ===
select count(*) filter (where title ~* 'm/f/d|f/m/x|w/m/d|m/w/d|\(m/f\)|h/f') as marca_de_genero,
       count(*) filter (where clean ~ '\mremote\M') as dice_remote,
       count(*) filter (where clean ~ '\mhybrid\M|\monsite\M|\mon site\M') as dice_modalidad,
       count(*) filter (where clean ~ '\mintern\M|\minternship\M') as pasantia,
       count(*) filter (where clean ~ '\m(senior|sr|junior|jr|staff|principal|lead|head|director|vp)\M') as trae_seniority,
       count(*) filter (where title ~ '[()]') as crudo_con_parentesis,
       count(*) filter (where title ~ '/') as crudo_con_barra
from v where clean <> '';

\echo === L. titulos que no quedan en alfabeto latino ===
select count(*) filter (where clean ~ '[^[:ascii:]]') as con_algun_no_ascii,
       count(*) filter (where clean !~ '[a-z]') as sin_ninguna_letra_latina
from v where clean <> '';

\echo === M. lo mismo, por idioma declarado ===
select language, count(*) as vacantes,
       count(*) filter (where clean !~ '[a-z]') as sin_letra_latina
from v group by 1 order by 2 desc limit 20;
```

### Qué contesta cada consulta y por qué se pide

| # | Qué responde | Por qué importa |
|---|---|---|
| A | Si la limpieza destruye títulos | **`vacio_limpio` tiene que ser igual a `vacio_crudo`.** Si es mayor, la regla borra datos y hay que cambiarla antes de seguir. El largo medio antes/después dice cuánto se recortó. |
| B | Cuánto colapsa el universo de títulos, y qué parte aportan las minúsculas solas contra los separadores + acentos | Es la medida del ruido de formato. En el departamento fue apenas 2,7%; si acá también es chico, el ruido no está en el formato y hay que atacarlo por otro lado. Se compara contra los **85.050 títulos distintos** ya medidos. |
| C | Cuántos títulos aparecen una sola vez, antes y después | Dice si categorizar con un diccionario de títulos frecuentes es viable o si hace falta matchear por tokens. |
| D / E | **Cuántas vacantes cubren los top 20, 100, 1.000…**, limpio contra crudo | La pregunta central del paso. La diferencia entre las dos tablas es exactamente si la distribución cambió de forma al limpiar. |
| F / G | Los títulos más frecuentes, limpios y crudos | Para mirarlos a ojo: son los que van a definir la primera versión de las reglas de categorización. |
| H | Qué variantes crudas se fusionaron en un mismo limpio | La verificación de "sin perder datos" del otro lado: si algo colapsó mal —dos títulos que significan cosas distintas cayendo en el mismo—, se ve acá. |
| I | Si `c++`, `c#`, `f#`, `.net`, `node.js` y `.js` sobreviven | **Las dos columnas tienen que dar idénticas.** Es la razón por la que `. + #` están en la lista blanca. |
| J | Qué caracteres ASCII raros siguen ahí | Con lista blanca, **lo único que puede aparecer es `. + # &`**. Cualquier otra cosa es un agujero en la regla. |
| J2 | En qué títulos concretos sobrevivió cada uno de los cuatro | La contracara de J: no alcanza con que no haya basura, hay que ver a ojo que `.net`, `c++`, `c#` y `r&d` están, y que no quedaron puntos de `sr.` ni `#` de hashtag. |
| K | Ruido semántico: `(m/f/d)`, `remote`, seniority, pasantías | Estos no son basura, son **atributos pegados al título**. Saber cuántos hay decide si se extraen como campos propios o se ignoran. Paréntesis y barras se cuentan sobre el **crudo**, porque en el limpio ya no existen. |
| L / M | Cuántos títulos quedan sin ninguna letra latina, y de qué idioma son | La lista blanca los conserva en vez de vaciarlos, pero un título en coreano no va a matchear un regex en inglés. Este número dimensiona el filtro por `language` que viene después. |

### Resultados

Corrido el **2026-09-12** contra prod. La salida cruda también quedó en
`medicion-titulos.txt`, en la raíz del repo (**sin versionar**: se borra cuando el
plan termine).

**La primera corrida encontró un bug en la regla y se corrigió antes de dar los
números por buenos.** La versión original conservaba `+` solo si le precedía un
alfanumérico, así que en `c++` el segundo `+` caía y quedaba `c+`: la consulta I dio
**201 en crudo contra 0 en limpio**. La regla corregida —la que está arriba— acepta
como precedente un alfanumérico **o otro `+`**, y los dos números dan 201. Es
exactamente para lo que existía esa consulta.

**Los dos controles duros dan bien:**

- **A** — `vacio_crudo` = `vacio_limpio` = **0**. La limpieza no vacía ningún título.
- **I** — las dos columnas idénticas en los seis tokens (`c++` 201, `.net` 93, `c#` 57,
  `.js` 34, `node.js` 26, `f#` 0).
- **J**, de yapa — los únicos caracteres ASCII no alfanuméricos que sobreviven son los
  cuatro de la lista blanca: `&` 827, `+` 586, `.` 475, `#` 124. No hay agujeros.

```
CREATE VIEW
=== A. control: volumen y titulos que se quedan vacios ===
 vacantes | vacio_crudo | vacio_limpio | largo_medio_crudo | largo_medio_limpio 
----------+-------------+--------------+-------------------+--------------------
   128953 |           0 |            0 |              36.0 |               34.4
(1 row)

=== B. cuanto colapsa, y que parte de la regla lo aporta ===
 distintos_crudos | distintos_solo_minusculas | distintos_limpios 
------------------+---------------------------+-------------------
            87647 |                     87513 |             83526
(1 row)

=== C. cola larga: crudo vs limpio ===
 version | distintos | aparecen_una_vez | pct_vacantes_en_la_cola 
---------+-----------+------------------+-------------------------
 crudo   |     87647 |            75160 |                    58.3
 limpio  |     83526 |            70537 |                    54.7
(2 rows)

=== D. cobertura acumulada de los top N, LIMPIO ===
  rn   | vacantes | pct_acumulado 
-------+----------+---------------
    10 |      161 |           2.0
    20 |      117 |           3.1
    50 |       67 |           5.1
   100 |       44 |           7.2
   200 |       29 |           9.9
   500 |       14 |          14.5
  1000 |        9 |          18.7
  5000 |        3 |          32.4
 10000 |        2 |          40.7
(9 rows)

=== E. cobertura acumulada de los top N, CRUDO ===
  rn   | vacantes | pct_acumulado 
-------+----------+---------------
    10 |      141 |           1.7
    20 |      107 |           2.6
    50 |       61 |           4.4
   100 |       41 |           6.3
   200 |       26 |           8.8
   500 |       13 |          12.8
  1000 |        8 |          16.8
  5000 |        3 |          29.8
 10000 |        2 |          37.9
(9 rows)

=== F. top 30 titulos LIMPIOS ===
                  titulo                   | vacantes 
-------------------------------------------+----------
 behavior technician                       |      419
 senior software engineer                  |      351
 registered behavior technician rbt        |      349
 auxiliaire de vie h f                     |      301
 senior account executive                  |      236
 real estate acquisition consultant        |      219
 account executive                         |      218
 sales development representative          |      188
 aide a domicile h f                       |      165
 maintenance technician                    |      161
 software engineer                         |      158
 staff software engineer                   |      156
 project manager                           |      156
 senior data engineer                      |      152
 enterprise account executive              |      149
 senior product manager                    |      141
 leasing consultant                        |      138
 business development representative       |      135
 account manager                           |      123
 registered behavior technician            |      117
 customer success manager                  |      109
 independent sales representative          |      108
 heavy equipment field technician mechanic |      108
 aide aux personnes agees h f              |      105
 seo strategist consultant                 |      105
 sales representative                      |      102
 retail sales associate part time          |      100
 heavy equipment shop technician mechanic  |       98
 product manager                           |       97
 events and ministry coordinator onsite    |       97
(30 rows)

=== G. top 30 titulos CRUDOS ===
                      titulo                       | vacantes 
---------------------------------------------------+----------
 Behavior Technician                               |      354
 Registered Behavior Technician (RBT)              |      337
 Senior Software Engineer                          |      305
 Real Estate Acquisition Consultant                |      219
 Account Executive                                 |      194
 Auxiliaire de vie H/F                             |      184
 Senior Account Executive                          |      175
 Sales Development Representative                  |      162
 Maintenance Technician                            |      146
 Software Engineer                                 |      141
 Staff Software Engineer                           |      137
 Leasing Consultant                                |      132
 Project Manager                                   |      130
 Enterprise Account Executive                      |      123
 Senior Product Manager                            |      121
 Senior Data Engineer                              |      119
 Account Manager                                   |      110
 Aide à domicile H/F                               |      108
 Heavy Equipment Field Technician (Mechanic)       |      108
 Business Development Representative               |      107
 Independent Sales Representative                  |      107
 SEO Strategist (Consultant)                       |      105
 Registered Behavior Technician                    |      101
 Retail Sales Associate - Part Time                |       99
 Heavy Equipment Shop Technician (Mechanic)        |       98
 Customer Success Manager                          |       97
 Events and Ministry Coordinator (Onsite)          |       97
 Center-Based Registered Behavior Technician (RBT) |       87
 Territory Account Manager                         |       86
 Yard Technician                                   |       84
(30 rows)

=== H. que colapso: crudos distintos que caen al mismo limpio ===
               titulo_limpio                | variantes_crudas | vacantes |                                                                                       ejemplos                                                                                       
--------------------------------------------+------------------+----------+--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 auxiliaire de vie h f                      |               23 |      301 |    Auxiliaire de vie H/F |   Auxiliaire de Vie H/F |   Auxiliaire de vie H/F |  AUXILIAIRE DE VIE H/F |  Auxiliaire de vie  (H/F) |  Auxiliaire de vie  H/F |  Auxiliaire de vie (H/
 aide a domicile h f                        |               19 |      165 |   Aide à domicile (H/F)  |   Aide à domicile H/F |  Aide a domicile (H/F) |  Aide à domicile (H/F)  |  Aide à domicile H/F |  Aide à domicile H/F  | AIDE A DOMICILE H/F | AIDE A DO
 aide aux personnes agees h f               |               14 |      105 |   Aide aux personnes âgées (H/F)  |  Aide aux personnes agées H/F |  Aide aux personnes âgées H/F |  Aide aux personnes âgées H/F  | Aide aux personnes agées (H/F) | Aide aux perso
 senior software engineer backend           |                9 |       42 | Senior Software Engineer (Backend) | Senior Software Engineer (Backend)  | Senior Software Engineer - Backend | Senior Software Engineer - Backend  | Senior Software Engineer Backe
 staff software engineer backend            |                8 |       31 | Staff Software Engineer (Backend) | Staff Software Engineer - Backend | Staff Software Engineer - Backend  | Staff Software Engineer - Backend   | Staff Software Engineer Backend  
 join our talent community                  |                8 |       45 | Join Our Talent Community | Join Our Talent Community  | Join Our Talent Community! | Join Our Talent Community!  | Join our Talent Community | Join our Talent Community! | Join ou
 assistant e de vie h f                     |                8 |       30 |   Assistant(e) de vie H/F | ASSISTANT(E) DE VIE H/F | Assistant(e)  de vie H/F | Assistant(e) de Vie H/F | Assistant(e) de vie  H/F | Assistant(e) de vie (H/F) | Assistant(e) de vi
 assistant de vie h f                       |                7 |       61 |  Assistant de vie (H/F) |  Assistant de vie H/F | Assistant de vie  (H/F) | Assistant de vie  H/F  | Assistant de vie (H/F) | Assistant de vie H/F | Assistant de vie H/F 
 sr software engineer backend               |                7 |        7 | Sr Software Engineer - Backend | Sr Software Engineer - Backend  | Sr. Software Engineer (Backend) | Sr. Software Engineer - Backend | Sr. Software Engineer - Backend  | Sr. Softwa
 senior software engineer                   |                7 |      351 |  Senior Software Engineer |  Senior Software Engineer  | Senior Software Engineer | Senior Software Engineer  | Senior Software Engineer || | Senior, Software Engineer  | Senior/So
 senior software engineer frontend          |                7 |       17 | Senior Software Engineer  - Frontend  | Senior Software Engineer (Frontend)  | Senior Software Engineer - Frontend | Senior Software Engineer – Frontend | Senior Software Engineer,
 assistante de vie h f                      |                7 |       29 |  Assistante de vie H/F |  Assistante de vie H/F  | Assistante de vie   H/F | Assistante de vie  (H/F) | Assistante de vie  H/F | Assistante de vie (H/F) | Assistante de vie H/F
 senior software engineer full stack        |                7 |       30 | Senior Software Engineer (Full Stack) | Senior Software Engineer (Full-Stack) | Senior Software Engineer - Full Stack | Senior Software Engineer, Full Stack | Senior Software Engin
 maintenance technician 2 500 sign on bonus |                6 |       11 | Maintenance Technician  $2,500 Sign On Bonus | Maintenance Technician ($2,500 Sign On Bonus!) | Maintenance Technician ($2,500 Sign On Bonus)  | Maintenance Technician ($2,500 Sign
 senior software engineer infrastructure    |                6 |        9 |  Senior Software Engineer, Infrastructure | Senior Software Engineer - Infrastructure | Senior Software Engineer | Infrastructure | Senior Software Engineer,  Infrastructure | Seni
 band 6 locum physiotherapist london        |                6 |       10 | Band 6 - Locum Physiotherapist - London | Band 6 - Locum Physiotherapist - London  | Band 6 Locum Physiotherapist - London | Band 6 Locum Physiotherapist - London  | Band 6 Locum P
 software engineering intern summer 2027    |                6 |        9 | Software Engineering Intern (Summer 2027) | Software Engineering Intern (Summer 2027)  | Software Engineering Intern - Summer 2027 | Software Engineering Intern – Summer 2027 | Sof
 senior product manager                     |                6 |      141 |  Senior Product Manager | Senior  Product Manager | Senior Product Manager | Senior Product Manager  | Senior Product Manager   | Senior Product Manager 
 join our talent network                    |                6 |       10 | Join Our Talent Network | Join Our Talent Network! | Join our Talent Network | Join our Talent Network  | Join our Talent Network!  | Join our talent network!
 senior data engineer                       |                6 |      152 |  Senior Data Engineer |  Senior Data Engineer  | Senior Data Engineer | Senior Data Engineer  | Senior Data Engineer  | Senior, Data Engineer
 spanish speaking behavior technician       |                6 |       72 | Spanish Speaking - Behavior Technician | Spanish Speaking Behavior Technician | Spanish Speaking Behavior Technician  | Spanish Speaking-Behavior Technician | Spanish- Speaking Beh
 behavior technician                        |                6 |      419 |  Behavior Technician |  Behavior Technician  |  Behavior Technician   | Behavior Technician | Behavior Technician  | Behavior Technician  
 mid market account executive               |                5 |       55 |  Mid-Market Account Executive | Mid Market Account Executive | Mid Market Account Executive  | Mid-Market Account Executive | Mid-Market Account Executive 
 project civil engineer project manager     |                5 |       25 | Project Civil Engineer - Project Manager | Project Civil Engineer - Project Manager  | Project Civil Engineer / Project Manager | Project Civil Engineer / Project Manager  | Projec
 licensed practical nurse lpn               |                5 |       12 | Licensed Practical Nurse (LPN) | Licensed Practical Nurse (LPN)  | Licensed Practical Nurse - LPN | Licensed Practical Nurse - LPN  | Licensed Practical Nurse LPN 
(25 rows)

=== I. sobreviven los tokens tecnicos (las dos columnas tienen que dar igual) ===
  token  | en_crudo | en_limpio 
---------+----------+-----------
 c++     |      201 |       201
 .js     |       34 |        34
 f#      |        0 |         0
 .net    |       93 |        93
 c#      |       57 |        57
 node.js |       26 |        26
(6 rows)

=== J. que caracteres ASCII no alfanumericos quedan (solo deberian ser . + # &) ===
 caracter | apariciones 
----------+-------------
 &        |         827
 +        |         586
 .        |         475
 #        |         124
(4 rows)

=== J2. los cuatro guardados, en contexto ===
 caracter  | titulos_con_el |                                                                                                                                                   ejemplos                                                                                                                                                   
-----------+----------------+--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 punto     |            456 | .net application and solutions architect assistant vice president | .net c# developer senior 879 | .net c# developer senior top secret with agreement to obtain ci poly | .net custom developer | .net developer always recruiting | .net developer vulnerability | .net engineer on site | .net react full 
 ampersand |            823 | 1541 microsoft dynamics 365 f&scm functional analyst | 2026 financial analyst i ad&s | 2026 support associate korean&english seoul | a&a engineer | a&e accident and emergency nurse barts | a&e accident emergency nurse east london | a&e accident emergency nurse euston warren street | a&e accident eme
 mas       |            382 | 110k+ bonus tax free opportunity consultant child adolescent psychiatrist doha | account executive dach c1+ german m w x | ai engineer middle middle+ ai team | algorithmic trading c++ engineer equities | analytics engineer middle middle+ dwh risk domain | applied ml cs phd internship 6+ months | arc
 gato      |            124 | .net c# developer senior 879 | .net c# developer senior top secret with agreement to obtain ci poly | 1505 senior fullstack engineer react node.js c# | 1565 senior fullstack c# and react engineer | account executive bailiwick req#1206 | account executive enterprise new england area req#1064 | accoun
(4 rows)

=== K. ruido semantico: atributos pegados al titulo ===
 marca_de_genero | dice_remote | dice_modalidad | pasantia | trae_seniority | crudo_con_parentesis | crudo_con_barra 
-----------------+-------------+----------------+----------+----------------+----------------------+-----------------
            1503 |        1592 |            800 |     1939 |          41837 |                26110 |            9492
(1 row)

=== L. titulos que no quedan en alfabeto latino ===
 con_algun_no_ascii | sin_ninguna_letra_latina 
--------------------+--------------------------
                725 |                      208
(1 row)

=== M. lo mismo, por idioma declarado ===
 language | vacantes | sin_letra_latina 
----------+----------+------------------
 en       |   123847 |               72
 pt       |     1805 |                0
 fr       |     1314 |                0
 de       |      562 |                0
 es       |      446 |                0
 ko       |      302 |               39
 ja       |      300 |               77
 it       |      127 |                0
 nl       |       76 |                0
 iw       |       55 |               14
 zhHant   |       53 |                5
 pl       |       40 |                0
 zh       |       14 |                1
 no       |        6 |                0
 fi       |        3 |                0
 ru       |        2 |                0
 se       |        1 |                0
(17 rows)
```

### Segunda ronda: cuantificar el seniority

Leyendo la primera ronda, Elias marcó que **el seniority pintaba como el mejor
candidato para colapsar títulos**. Se midió en vez de suponerlo, junto con los otros
dos recortes que la consulta H había insinuado (marca de género `h/f` y modalidad).
La salida cruda está en `medicion-titulos-ronda2.txt`.

```
CREATE VIEW
CREATE VIEW
=== N1. efecto de cada recorte sobre la cardinalidad ===
       version       | titulos_distintos | vacantes_tocadas 
---------------------+-------------------+------------------
 limpio (linea base) |             83526 |                0
 sin seniority       |             78682 |            31192
 sin marca de genero |             83370 |             1638
 sin modalidad       |             82711 |             6533
 los tres juntos     |             77654 |            38390
(5 rows)

=== N2. cobertura acumulada top N, con los tres recortes ===
  rn   | vacantes | pct_acumulado 
-------+----------+---------------
    10 |      210 |           2.9
    20 |      159 |           4.3
    50 |       93 |           7.0
   100 |       50 |           9.6
   200 |       31 |          12.6
   500 |       16 |          17.6
  1000 |        9 |          22.1
  5000 |        3 |          36.4
 10000 |        2 |          44.8
(9 rows)

=== N3. cola larga con los tres recortes ===
 distintos | aparecen_una_vez | pct_vacantes_en_la_cola 
-----------+------------------+-------------------------
     77653 |            64143 |                    49.7
(1 row)

=== N4. top 30 titulos con los tres recortes ===
               titulo                | vacantes 
-------------------------------------+----------
 software engineer                   |      811
 account executive                   |      489
 behavior technician                 |      424
 registered behavior technician rbt  |      353
 product manager                     |      346
 auxiliaire de vie                   |      312
 data engineer                       |      310
 project manager                     |      289
 real estate acquisition consultant  |      219
 data scientist                      |      210
 sales development representative    |      202
 customer success manager            |      195
 product designer                    |      191
 maintenance technician              |      179
 accountant                          |      176
 enterprise account executive        |      174
 account manager                     |      173
 aide a domicile                     |      171
 site reliability engineer           |      167
 machine learning engineer           |      159
 devops engineer                     |      157
 leasing consultant                  |      151
 mechanical engineer                 |      144
 business development representative |      141
 retail sales associate              |      137
 solutions architect                 |      133
 backend engineer                    |      130
 systems engineer                    |      130
 solutions engineer                  |      128
 electrical engineer                 |      127
(30 rows)

=== N5. cuanto aporta cada token de seniority ===
 tok | vacantes 
-----+----------
(0 rows)

    tok    | vacantes 
-----------+----------
 senior    |    21102
 director  |     6124
 associate |     5861
 lead      |     5153
 staff     |     4562
 sr        |     3133
 principal |     2142
 ii        |     1941
 intern    |     1408
 head      |     1109
 mid       |      565
 junior    |      554
 iii       |      460
 vp        |      415
 chief     |      384
 entry     |      287
 jr        |       73
(17 rows)

=== N6. top 60 tokens del titulo limpio ===
     token      | vacantes | x 
----------------+----------+---
 engineer       |    25322 | 1
 manager        |    21781 | 1
 senior         |    21138 | 1
 software       |     8595 | 1
 sales          |     6262 | 1
 director       |     6229 | 1
 account        |     5978 | 1
 technician     |     5947 | 1
 associate      |     5895 | 1
 specialist     |     5402 | 1
 product        |     5198 | 1
 lead           |     5170 | 1
 operations     |     4893 | 1
 executive      |     4829 | 1
 data           |     4763 | 1
 staff          |     4557 | 1
 analyst        |     3991 | 1
 and            |     3704 | 1
 ai             |     3437 | 1
 engineering    |     3353 | 1
 development    |     3268 | 1
 time           |     3120 | 1
 sr             |     3119 | 1
 assistant      |     3049 | 1
 of             |     2931 | 1
 business       |     2874 | 1
 marketing      |     2871 | 1
 technical      |     2655 | 1
 systems        |     2519 | 1
 project        |     2499 | 1
 consultant     |     2464 | 1
 security       |     2266 | 1
 principal      |     2154 | 1
 platform       |     2141 | 1
 part           |     2134 | 1
 customer       |     2091 | 1
 de             |     2082 | 1
 enterprise     |     2070 | 1
 coordinator    |     2026 | 1
 support        |     1975 | 1
 nurse          |     1959 | 1
 ii             |     1947 | 1
 representative |     1944 | 1
 in             |     1868 | 1
 partner        |     1836 | 1
 management     |     1819 | 1
 program        |     1803 | 1
 behavior       |     1701 | 1
 architect      |     1687 | 1
 full           |     1678 | 1
 solutions      |     1658 | 1
 f              |     1607 | 1
 designer       |     1602 | 1
 remote         |     1594 | 1
 scientist      |     1536 | 1
 field          |     1501 | 1
 service        |     1484 | 1
 health         |     1480 | 1
 developer      |     1435 | 1
 intern         |     1408 | 1
(60 rows)

=== N7. cuantos titulos distintos hay por cantidad de tokens ===
 tokens | vacantes | titulos_distintos 
--------+----------+-------------------
      1 |      949 |               252
      2 |    14190 |              3936
      3 |    29198 |             14558
      4 |    29181 |             20353
      5 |    22920 |             17418
      6 |    14570 |             11895
      7 |     8369 |              7037
      8 |     4480 |              3811
      9 |     2243 |              1942
     10 |     1283 |              1050
     11 |      691 |               546
     12 |      377 |               312
     13 |      247 |               181
     14 |      106 |                97
     15 |       54 |                47
(15 rows)

=== N8. no son vacantes: talent pool y similares ===
 vacantes | talent_pool | aplicacion_general | prueba 
----------+-------------+--------------------+--------
   128953 |         356 |                296 |   1102
(1 row)
```

### El análisis

**1. La limpieza de caracteres colapsó poco en total, pero mucho donde importa.**
87.647 → 83.526 títulos distintos es apenas **-4,7%**, y a primera vista parece flojo.
Pero el colapso no está repartido: cae casi entero sobre la **cabeza** de la
distribución, que es la parte útil. `auxiliaire de vie h f` pasó de 184 a **301** (+64%),
`behavior technician` de 354 a **419**, `senior software engineer` de 305 a **351**. La
cobertura de los top 20 subió de 2,6% a 3,1%, un 19% relativo. La cola no se movió
porque **es única por razones semánticas, no de formato**: son títulos que nadie más
escribió, no variantes de puntuación.

**2. La hipótesis del seniority es correcta, y gana por lejos.** De los tres recortes
medidos:

| Recorte | Títulos distintos | Vacantes tocadas |
|---|---|---|
| línea base (solo limpieza) | 83.526 | — |
| **sin seniority** | **78.682 (−5,8%)** | **31.192 (24%)** |
| sin marca de género | 83.370 (−0,2%) | 1.638 |
| sin modalidad | 82.711 (−1,0%) | 6.533 |
| los tres juntos | 77.654 (−7,0%) | 38.390 (30%) |

**Sacar el seniority colapsa más que toda la limpieza de caracteres** (−5,8% contra
−4,7%), tocando una de cada cuatro vacantes. Y en la cabeza el efecto es mucho más
grande que en la cardinalidad: los top 20 pasan de 3,1% a **4,3%** de cobertura, y la
cola de títulos únicos baja de 54,7% a **49,7%**.

El top 30 lo muestra de una: `software engineer` salta de 158 a **811** (5,1x) al
juntar senior / staff / sr / principal / junior, `account executive` de 218 a 489,
`data engineer` de 152 a 310. Y aparecen arriba títulos tech que antes estaban
desparramados: `data scientist` 210, `product designer` 191, `site reliability
engineer` 167, `machine learning engineer` 159, `devops engineer` 157, `solutions
architect` 133, `backend engineer` 130.

**Conclusión: el seniority no es ruido, es una dimensión ortogonal que está metida
adentro del título.** Corresponde **extraerlo a un campo propio**, no borrarlo — igual
que `language`, que es un dato útil pero no dentro del texto.

**3. Pero "seniority" no es una sola cosa.** Los tokens medidos se parten en tres
grupos, y tratarlos igual sería un error:

- **Modificadores puros**, que no cambian la función: `senior` 21.102, `staff` 4.562,
  `sr` 3.133, `principal` 2.142, `ii` 1.941, `mid` 565, `junior` 554, `iii` 460,
  `entry` 287, `jr` 73. **~34.800 vacantes.** Estos se extraen sin perder nada.
- **Roles jerárquicos**, que **son** la función: `director` 6.124, `lead` 5.153,
  `head` 1.109, `vp` 415, `chief` 384. **~13.200.** Si se borran, "Director of
  Engineering" y "Engineer" colapsan en el mismo título siendo trabajos distintos.
  Estos se quedan.
- **Ambiguos**: `associate` 5.861 —¿"Associate Product Manager" (seniority) o "Sales
  Associate" (función)?— y `intern` 1.408, que en realidad es **tipo de contrato**, no
  seniority. Desambiguar `associate` exige mirar la palabra que le sigue.

**4. La cola larga no la arregla ningún recorte, y hay que aceptarlo.** Aun con los
tres aplicados quedan **64.143 títulos que aparecen una sola vez, el 49,7% de las
vacantes**. La mitad del dataset tiene un título que no se repite nunca. La consulta N7
explica por qué: la masa está en títulos de 3 a 5 tokens (81.299 vacantes, el 63%),
donde la relación vacantes/títulos distintos es de apenas 1,4, y los títulos largos son
casi todos únicos (14 tokens → 106 vacantes en 97 títulos distintos).

**Esta es la conclusión de diseño más importante del paso A: la categorización
tech / no-tech no puede ser un diccionario de títulos.** Un diccionario de los 1.000
títulos más frecuentes cubre el 22% de las vacantes; uno de 10.000, el 45%. Tiene que
trabajar **por tokens**.

**5. Los tokens sí alcanzan.** `engineer` solo aparece en **25.322 vacantes (19,6%)**, y
con `software` 8.595, `data` 4.763, `analyst` 3.991, `ai` 3.437, `technical` 2.655,
`systems` 2.519, `security` 2.266, `platform` 2.141, `architect` 1.687, `scientist`
1.536 y `developer` 1.435 se cubre una porción enorme del universo tech con una lista
corta.

Con dos trampas ya visibles en los datos:

- **`engineer` no implica tech.** En el top 30 de N4 están `mechanical engineer` 144 y
  `electrical engineer` 127. El clasificador va a necesitar **tokens negativos**
  (mechanical, electrical, civil, chemical, structural, industrial) tanto como
  positivos.
- **`manager` es el segundo token más frecuente** (21.781) y por sí solo no dice nada.

**6. Hay filas que no son vacantes.** `talent community / network / pool` son **356** y
`general application / speculative / future opportunities` **296**: unos 650
formularios de "dejanos tu CV", no búsquedas reales. (El contador `prueba` de N8 son
1.102 pero es **falso positivo**: el patrón `test` agarra "Test Engineer" y "QA Test";
ese número hay que descartarlo.)

**7. El dataset no es de tech, y eso valida el orden del plan.** En el top 30 ya limpio
mandan `behavior technician` y `registered behavior technician` (terapia ABA),
`auxiliaire de vie` y `aide a domicile` (cuidado domiciliario francés), `real estate
acquisition consultant`, `leasing consultant`, `maintenance technician` y `heavy
equipment technician`. Solo 5 de los 30 son tech; después de extraer el seniority suben
a unos 12 de 30. Separar tech primero tiene sentido: es una minoría clara adentro de un
mar de salud, cuidados, ventas y oficios.

**8. Dos observaciones menores sobre la lista blanca.** El `#` guarda más ruido que
señal —de los 124 títulos que lo conservan, 57 son `c#` y el resto son números de
requisición tipo `req#1206`—, y el `&`, que defendí con el caso `r&d`, aparece sobre
todo en otros usos (`f&scm`, `a&e`, `a&a`, `korean&english`). Ninguna de las dos
justifica cambiar la regla: las dos conservan información y el ruido está acotado.

**9. Queda una discrepancia sin explicar.** `docs/MEDICION-VACANTES.md` dice 85.050
títulos distintos y acá dan 87.647, con el `count(*)` idéntico (128.953). Ese documento
aclara que el SQL del título no quedó registrado, así que la diferencia es de cómo se
contó entonces, no de los datos.

### Cómo cierra el paso A

Elias corre el guion, pega la salida en la sección de arriba y **mira los números él
primero**. Después analizo yo. Con eso se decide:

1. Si la lista blanca queda como está o le falta algún caracter (según J y J2).
2. Si el normalizado se guarda tal cual o hay que extraerle atributos (según K).
3. Qué forma toma la categorización tech / no-tech —diccionario de títulos frecuentes,
   tokens, o las dos cosas— según C, D y F.

## Decisiones que cerró el paso A

- **La categorización va a ser por tokens, no por diccionario de títulos.** Lo fuerza
  la cola: 64.143 títulos aparecen una sola vez, el 49,7% de las vacantes.
- **El seniority se extrae a un campo propio** y el título normalizado queda **sin él**.
  No se guarda además el título completo: si hace falta, se recompone.
- **Solo cuentan como seniority los modificadores puros**: `senior`, `sr`, `staff`,
  `principal`, `junior`, `jr`, `mid`, `entry` y los niveles `ii` / `iii`. Los roles
  jerárquicos (`director`, `lead`, `head`, `vp`, `chief`), el ambiguo `associate` y el
  tipo de contrato `intern` **se quedan adentro del título**.
- **La lista blanca de caracteres queda como está.** El `#` que arrastra números de
  requisición y el `&` que casi nunca es `r&d` conservan información y su ruido está
  acotado; no justifican cambiar la regla.

## Paso B1 — medir los falsos positivos de los tokens de seniority

**Corrido el 2026-09-12. La salida está en `medicion-seniority.txt` y todavía no se
analizó.**

### Por qué existe este paso

El paso A cerró *qué* tokens cuentan como seniority, pero no verificó que esos tokens
**signifiquen** seniority cada vez que aparecen. Varios tienen usos donde la palabra
**es la función, no el nivel**, y este dataset está dominado por salud, cuidados y
oficios —justo donde caen—:

| Token | Falso positivo sospechado | Por qué es creíble en este dataset |
|---|---|---|
| `senior` | `senior care assistant`, `senior living` | el top 30 del paso A lo dominan cuidado domiciliario y terapia ABA |
| `staff` | `staff nurse` (UK), `staff accountant` | `nurse` aparece en 1.959 vacantes; `accountant` 176 en el top 30 de N4 |
| `entry` | `data entry clerk` | son solo 287 en total: podría ser casi todo esto |
| `mid` | `mid market account executive` | la consulta H del paso A ya lo mostró: 55 vacantes |
| `principal` | `principal` de escuela | menos probable, pero existe |

La diferencia con el ruido de formato es que acá el normalizador no ensucia el dato:
**produce un dato equivocado**. "Senior Care Assistant" quedaría como `care assistant` +
`SENIOR`, que es falso. Y se mide con una sola consulta, así que se mide en vez de
suponerse — es el mismo camino que encontró el bug de `c++` en el paso A.

### Cómo se corrió

El SQL —las dos vistas de abajo más las siete consultas, y quedó en `~/seniority.sql`
en el servidor— se copió con `scp` y se mandó por stdin. **Dos detalles del servidor
que cuestan una corrida perdida si no se saben:** el usuario no está en el grupo
`docker` y el Docker de esa máquina es rootful, así que todo comando va con `sudo`; y el
ticket de `sudo` **no sobrevive entre invocaciones de SSH** (`tty_tickets`), así que hay
que cachearlo y usarlo en la misma invocación.

```bash
scp seniority.sql elitedesk1:~/seniority.sql
ssh elitedesk1 'echo <password> | sudo -S -p "" true 2>/dev/null; \
  cd ~/oneprofile && sudo -n docker compose exec -T postgres \
  sh -c "psql -U \$POSTGRES_USER -d \$POSTGRES_DB" \
  < ~/seniority.sql > ~/medicion-seniority.txt'
scp elitedesk1:~/medicion-seniority.txt ./medicion-seniority.txt
```

**No escribe nada en la base:** las tres vistas son `temp` y mueren con la sesión de
psql.

### Las vistas

La primera es **la misma vista `v` del paso A**, transcripta arriba en "La regla,
simulada en SQL", así que la medición corre sobre el título ya limpio. Encima van dos
que lo parten en tokens:

```sql
create temp view toks as
select id, clean, string_to_array(clean, ' ') as t
from v where clean <> '';

create temp view sen as
select x.id, u.tok, u.ord,
       x.t[u.ord - 1] as prev,
       x.t[u.ord + 1] as next,
       cardinality(x.t) as n_toks,
       x.clean
from toks x, unnest(x.t) with ordinality as u(tok, ord)
where u.tok in ('senior','sr','staff','principal','junior','jr','mid','entry','ii','iii');
```

`unnest(...) with ordinality` es lo que da la posición del token, y con ella las columnas
`prev` y `next`: una fila por **aparición** de un token de seniority, con la palabra que
lo rodea a cada lado.

### Qué contesta cada consulta y por qué se pide

| # | Qué responde | Por qué importa |
|---|---|---|
| S1 | Vacantes por token y **en qué posición del título cae** (primera, última, y el `pct_primera`) | Si el seniority está casi siempre al principio, la posición sirve como guarda barata y **general**, sin enumerar palabras. |
| S2 | **Top 15 de la palabra que le sigue** a cada token, con títulos de ejemplo | La consulta central: es donde se separa `senior → software` de `senior → care`, y `entry → level` de `entry → clerk`. |
| S3 | Top 10 de la palabra que le **precede** | Atrapa el caso inverso que S2 no ve (`data entry`, `nurse staff`). |
| S4a / S4b | Cuántos títulos traen **más de un** token de seniority, y qué combinaciones son | Dimensiona la regla de precedencia, que hoy se supone a partir del único caso "Senior Staff Engineer". |
| S5 | Los cuatro tokens **chicos** (`entry` 287, `mid` 566, `jr` 73, `iii` 462) **título por título** | Son pocos: se deciden con certeza mirándolos enteros, no con estadística. |
| S6 | `ii` / `iii`: con qué palabras los rodean | Verifica que son niveles de puesto y no otra cosa (`phase ii`, `part ii`). |

### Controles de sanidad de la corrida

- `psql` salió con **código 0** y la salida no tiene ningún `ERROR`, `does not exist` ni
  error de sintaxis. Las tres vistas se crearon y las siete consultas corrieron.
- Los **10 tokens aparecen en S1**, con volúmenes que coinciden con la segunda ronda del
  paso A (`senior` 21.138 contra 21.102, `staff` 4.557 contra 4.562, `sr` 3.119 contra
  3.133, `ii` 1.947 contra 1.941). Las diferencias chicas son esperables: la ronda 2
  contó con un regex sobre el título entero y B1 cuenta por **token exacto**.

### Lo que cerró B1, y el agujero que dejó

**Siete de los diez tokens no necesitan guarda:** `sr` 3.119 (92,8% en primera posición),
`junior` 543 y `jr` 73 —S5 los muestra título por título y son todos legítimos—,
`principal` 2.154 —ni un caso de director de escuela—, e `ii` 1.947 / `iii` 462, que
según S6 siempre siguen a un sustantivo de rol (`engineer ii` 351), nunca `phase ii`.
`senior` entra también acá, pero por lo que midió B1b.

**Tres sí**, y las guardas están en el paso B2, cada una con su número.

El agujero: S2 traía solo el **top 15** de la palabra siguiente a cada token, y para
`senior` —21.138 apariciones— ese top 15 corta en 209 vacantes, así que los usos
sospechados quedaban fuera de la ventana. De ahí salió B1b.

## Paso B1b — la cabeza de `senior` a fondo

**Corrido y analizado el 2026-09-12. `medicion-senior.txt` trae el análisis escrito
adentro, arriba de la salida cruda.**

Cuatro consultas sobre la misma vista `sen` de B1, filtrando `tok = 'senior'`: el top
**50** de la palabra siguiente y de la anterior con porcentaje acumulado, los 50 títulos
completos más frecuentes de la **cola** que queda fuera de ese top 50, y un conteo
directo de los usos donde `senior` podría ser sustantivo.

Lo que encontró:

- **El falso positivo existe y son 37 vacantes sobre 21.100: el 0,18%.** No tiene la
  forma sospechada —`senior care` son 2 y `senior living` 1—, sino `senior` como
  **paciente**, en dos plantillas de una empresa de cuidado domiciliario:
  `caregiver needed support for a senior client <ciudad>` 21 y
  `hiring caregiver for a female|male senior in <ciudad>` 16.
- **`senior client` (114) no era el problema**: 93 son seniority legítima
  (`senior client success manager`, `senior client partner`).
- **La cola no esconde nada.** El top 50 cubre el 67,3%, y los 50 títulos más frecuentes
  del 32,7% restante son todos puestos reales (`senior analytics engineer` 31,
  `senior recruiter` 25).
- **Hallazgo nuevo: `semi senior`**, el nivel intermedio del mercado hispanoamericano.

**Decisión de Elias: `senior` no lleva guarda**, porque el 0,18% no daña los títulos de
los que sí se extrae.

## Paso B2 — el normalizador (función pura)

**HECHO.** Tres archivos, sin base y sin Spring:

- `src/main/java/oneprofile/backend/model/Seniority.java` — enum al lado de `Ats` y
  `BoardStatus`. Los valores **se declaran de menor a mayor** (`ENTRY`, `JUNIOR`,
  `SEMI_SENIOR`, `MID`, `SENIOR`, `STAFF`, `PRINCIPAL`) y después `LEVEL_2` y `LEVEL_3`,
  que quedan fuera de esa escala. **El orden de declaración es significativo**: lo usa la
  regla del mínimo.
- `src/main/java/oneprofile/backend/util/TitleNormalizer.java` — clase final sin
  instancias, al lado de `HtmlToText` y `GreenhouseBoardUrl`. Un método
  `static NormalizedTitle normalize(String)` que devuelve el record
  `(String title, Seniority seniority)`.
- `src/test/java/oneprofile/backend/util/TitleNormalizerTest.java` — 18 tests, todos con
  títulos reales de las mediciones.

### Las reglas que implementa

**Limpieza: lista blanca.** Minúsculas → sin diacríticos → el apóstrofo se borra sin
dejar espacio (`women's` → `womens`) → todo lo que no sea letra, dígito o espacio pasa a
espacio, salvo `.` seguido de alfanumérico (`.net`), `#` precedido de alfanumérico (`c#`),
`+` precedido de alfanumérico o de otro `+` (`c++`) y `&` entre alfanuméricos (`r&d`) →
colapsar espacios. Las clases son unicode, así que un título en coreano no queda vacío.

**Un detalle que el SQL de las mediciones no tenía:** al final se recompone a **NFC**,
porque NFD parte cada sílaba del hangul en sus letras y dejarlo así daría dos escrituras
del mismo título coreano. Para el alfabeto latino no cambia nada.

**Los tokens de seniority y sus guardas:**

| Token | Guarda | Falso positivo que evita |
|---|---|---|
| `senior`, `sr`, `junior`, `jr`, `principal`, `ii`, `iii`, `ssr`, `semisenior` | ninguna | — |
| `semi senior` / `semi sr` | se reconoce como **secuencia de dos tokens**, antes que los sueltos | que `semi senior` dé `SENIOR` |
| `entry` 287 | solo si le sigue `level` (196) | `entry door` 72, `data entry` |
| `mid` 566 | solo si le sigue `level`, si cierra el título, o si forma rango con otro nivel (~125) | `mid market` 300, `mid atlantic` 25, `mid enterprise` 15 |
| `staff` 4.557 | no cuenta si lo precede `of` —directo o con una palabra en el medio—, si cierra el título, o si le sigue `nurse`, `accountant` o `attorney` | `chief of staff` 119, `member of technical staff` 117, `staff nurse` 66, `staff accountant` 53 |

Un token que no pasa su guarda **se queda adentro del título** como una palabra más.

**Cuando el título nombra más de un nivel (1.584 títulos), gana el más bajo**, porque una
vacante publica **el piso que acepta**: `junior to senior project manager` busca gente
desde junior. Quedarse con el máximo subiría la barra de entrada y escondería vacantes
para las que el usuario califica; el error es asimétrico. Así: `senior staff` 609 →
`SENIOR`, `staff principal` 32 → `STAFF`, `mid senior` 39 → `MID`, `ii iii` 47 →
`LEVEL_2`. El orden `STAFF < PRINCIPAL` es **convención adoptada**, no un hecho medido, y
está escrito en el javadoc del enum.

`LEVEL_2` y `LEVEL_3` están declarados después de toda la escala de palabras, así que el
mínimo hace sola la regla de que **una palabra le gane a un numeral**: `senior ii` 244 →
`SENIOR`.

### El semi senior, medido antes de escribirlo

Elias lo pidió con esta razón: *"quiero darle importancia al español, ahora tengo pocas
vacantes, pero cuando sume más ATS van a ser más"*. El dataset de hoy sale de un solo ATS
sesgado a Estados Unidos, así que su volumen actual subestima el caso; y hoy se
clasificaría como `SENIOR`, que es un dato **equivocado**, no solo incompleto.

`semi senior` son **13** vacantes, `semisenior` en una palabra **1**, y `ssr` **3**, las
tres con sentido de semi senior (`ssr data scientist`, `accounting analyst ssr`). **`ssr`
no colisiona con *server-side rendering***: ningún título lo usa así, y "server side
rendering" no aparece escrito entero en ninguno. `junior senior` 6 **no** es semi senior:
son rangos.

`SEMI_SENIOR` es un valor propio y **no se mapea a `MID`**: son escalas de culturas
distintas y equipararlas sería inventar la equivalencia que el paso A ya descartó para
`ii` / `iii`.

### Cómo cierra B2

`./mvnw test` en verde. Da **80 tests** (eran 62; los 18 nuevos son del normalizador).
**No hay prueba manual contra prod**: el normalizador no tiene red ni base, así que los
tests son la verificación. La corrida real llega en el paso C.


## Paso B3 — medir otros criterios para limpiar el título

**Corrido el 2026-09-12 y analizado. Las salidas están en `medicion-ubicacion.txt` y
`medicion-ruido.txt`; el análisis, en "El análisis de B3".**

### Por qué existe este paso

Hasta acá el único criterio medido y aplicado es el **seniority**. Elias pidió anidar un
paso antes del C para ver **por qué otro criterio se puede limpiar el título**, y el
orden no es capricho: el paso C crea `normalized_vacancy`, y una dimensión que aparezca
después obliga a migrar la tabla. Va partido en dos, como los anteriores: **B3 mide** y
**B4 escribe el código**.

### Las hipótesis

Las de Elias:

| # | Hipótesis | Qué se espera de ella |
|---|---|---|
| H1 | **Modalidad**: `remote` y sus equivalentes, `on-site` y los suyos | se **extrae** a un campo propio, no se limpia |
| H2 | **Ubicación** metida en el título | se extrae a **otro** campo; y si dice remote y no hay ubicación, es **fully remote** |
| H3 | **Ruido puro**: salario, bonos y "oportunidad" pegados al título | se limpia |
| H4 | **`experienced`** | ¿mapea a algún valor de `Seniority`? |

H2 es la que Elias marcó como más clara, y su regla del *fully remote* interesa porque
**a esas vacantes puede aplicar cualquiera**: es un subconjunto útil para el matching, no
un detalle de limpieza.

Las mías, todas sacadas de datos que las mediciones anteriores ya habían mostrado:

| # | Hipótesis | Evidencia que ya estaba a la vista |
|---|---|---|
| H5 | **Marca de género** (`h/f`, `m/f/d`, `w/m/x`) | 1.503 títulos en la consulta K. En el limpio quedan como tokens sueltos `h f`, y `auxiliaire de vie h f` (301) es el mismo puesto que `auxiliaire de vie`. **Ruido puro.** |
| H6 | **Sigla de certificación entre paréntesis** | `registered behavior technician rbt` 353 contra `registered behavior technician` 117, y `licensed practical nurse lpn`. El mismo puesto partido en dos por la sigla, justo en la cabeza de la distribución. |
| H7 | **Número de requisición pegado** | `account executive bailiwick req#1206`, `1541 microsoft dynamics 365 f&scm…`, `.net c# developer senior 879`. Ids internos de cada empresa. **Ruido puro.** |
| H8 | **Contrato y jornada** | `part time` 2.134, `intern` 1.408, más `full time`, `contract`, `seasonal`, `per diem`. Es una dimensión ortogonal: **campo propio, no borrado**, por el mismo argumento que ganó el seniority. |
| H9 | **Idioma requerido** | `spanish speaking behavior technician` 72, `korean&english`, `c1+ german`. Atributo real del puesto. |
| H10 | **Cohorte o fecha** | `software engineering intern summer 2027`, `2026 financial analyst i ad&s`, `class of`. |
| H11 | **Marketing y urgencia** | `join our talent community!`, `hiring now`, `apply now`, `immediate start`. Se toca con las ~650 filas que no son vacantes. |

### Las dos fuentes

**Esta medición mira `title` y `location`, no solo el título.** Son dos razones
distintas y las dos importan:

- La **modalidad vive más en `location` que en el título**: el board escribe
  `"Remote - U.S."`, `"Remote - LATAM"`. Medir solo el título la subestimaría.
- Los **tokens de `location` son un diccionario de lugares real**, sacado de estos mismos
  datos. Es lo que permite reconocer la ciudad dentro del título sin inventar una lista
  de ciudades del mundo, que era el problema de H2.

`docs/CONTEXTO.md` ya dice que `location` **no es parseable** como campo estructurado
—es texto libre, y `offices` trae país en solo el 13% de los casos—. Eso sigue siendo
cierto y no es lo que se busca acá: no se quiere el país normalizado, se quiere saber si
el título repite algo que `location` ya dice.

### Cómo se corrió

Las consultas **las corro yo** por `ssh elitedesk1`. El SQL va copiado con `scp` y
redirigido desde el archivo —el heredoc pelea con las regex—, y el `sudo` se cachea y se
usa **dentro de la misma invocación de SSH**, porque el ticket no sobrevive entre
conexiones:

```bash
scp medicion-ubicacion.sql medicion-ruido.sql elitedesk1:~/
ssh elitedesk1 'echo <password> | sudo -S -p "" true 2>/dev/null; \
  cd ~/oneprofile && sudo -n docker compose exec -T postgres \
  sh -c "psql -U \$POSTGRES_USER -d \$POSTGRES_DB" < ~/medicion-ubicacion.sql \
  > ~/medicion-ubicacion.txt'
```

**No escribe nada en la base:** funciones y vistas se crean en `pg_temp`, así que viven
solo en esa sesión de psql.

### La limpieza, ahora como función

Los dos archivos empiezan con la **misma** regla del paso A, pero envuelta en una función
temporal en vez de repetida dentro de una vista. El motivo es concreto: hay que aplicarla
**al título y a `location`**, y copiar los ocho `regexp_replace` anidados dos veces es
justamente cómo se cuela un error.

```sql
create function pg_temp.clean(t text) returns text language sql immutable as $fn$
select btrim(regexp_replace(                                      -- 8. colapsar espacios
  regexp_replace(                                                 -- 7. todo lo demas a espacio
    regexp_replace(                                               -- 6. & solo entre alfanumericos
      regexp_replace(                                             -- 5. + precedido por alfanumerico o por otro +
        regexp_replace(                                           -- 4. # solo precedido por alfanumerico
          regexp_replace(                                         -- 3. . solo si le sigue un alfanumerico
            regexp_replace(                                       -- 2. apostrofos, sin dejar espacio
              regexp_replace(normalize(lower(t), NFD),            -- 1. minusculas y diacriticos
                             '[̀-ͯ]', '', 'g'),
              '[''’]', '', 'g'),
            '\.(?![[:alnum:]])', ' ', 'g'),
          '(?<![[:alnum:]])#', ' ', 'g'),
        '(?<![[:alnum:]+])\+', ' ', 'g'),
      '(?<![[:alnum:]])&|&(?![[:alnum:]])', ' ', 'g'),
    '[^[:alnum:] .+#&]', ' ', 'g'),
  '\s+', ' ', 'g'))
$fn$;
```

**Y pasó exactamente eso**: la primera corrida de `medicion-ruido.sql` falló entera —28
errores en cascada— porque a esa copia de la función le faltaba un nivel de anidamiento.
`psql` no para en el primer error, así que la vista no se creó y las 20 consultas
siguientes dieron `relation "v" does not exist`. Se arregló copiando la función que ya
había funcionado en el otro archivo, y se volvió a correr.

La familia P agrega una segunda función, que es la que hace medible la regla del *fully
remote*: **lo que queda de un texto de ubicación después de sacarle las palabras de
modalidad y el relleno**. Si queda vacío, esa vacante no nombra ningún lugar.

```sql
create function pg_temp.place_left(t text) returns text language sql immutable as $fn$
select btrim(regexp_replace(
  regexp_replace(
    regexp_replace(coalesce(t, ''),
      '\m(remote|remoto|telecommute|telecommuting|telework|teletrabajo|wfh|virtual|hybrid|hibrido|onsite|presencial)\M|work from home|home based|a distancia|on site|in office|in person',
      ' ', 'g'),
    '\m(only|anywhere|flexible|optional|friendly|first|work|from|home|based|or|and|the|of|position|role|job|other|location|locations|office|offices|area|areas|any|various|multiple|global|worldwide|distributed|field|travel|tbd|n a)\M',
    ' ', 'g'),
  '\s+', ' ', 'g'))
$fn$;
```

Esa lista de relleno es **lista negra, no blanca**, al revés de la regla de limpieza, y
es una debilidad conocida de esta medición: si falta una palabra de relleno, una vacante
que sí es fully remote va a contarse como "remote con lugar". Se acepta porque acá no se
está guardando nada, se está dimensionando; la regla definitiva se escribe en B4 con la
salida a la vista.

La familia Q agrega una línea base propia: **el título limpio y además sin seniority**,
que es lo que el normalizador produce hoy. La aproximación del seniority es la misma
regex de la ronda 2 del paso A —no las guardas finas de `TitleNormalizer`— justamente
para que los números sean comparables con N1 y N2.

```sql
create temp table v as
select id, title, location,
       pg_temp.clean(title) as clean,
       pg_temp.squeeze(regexp_replace(pg_temp.clean(title),
         '\m(senior|sr|junior|jr|staff|principal|mid level|entry level|ii|iii|ssr|semi senior)\M', ' ', 'g')) as base
from vacancy;

create index on v (clean);
create index on v (base);
analyze v;
```

**Acá es tabla y no vista, al revés que en el paso A, y no es cosmético.** Una vista
recalcula `pg_temp.clean(title)` cada vez que alguien la toca, y esta familia la toca
veintitrés veces. Con vista, la consulta Q2c —que busca para cada título con sigla si
existe el mismo título sin ella— **se colgó**: hubo que cancelarla a mano con
`pg_cancel_backend` después de varios minutos. Materializada, con un índice sobre
`clean` y con los títulos distintos en su propia tabla, la corrida entera termina en
menos de un minuto. Lo mismo vale para `w`, la tabla de los recortes, que se recorre
cuatro veces.

Dos trampas más de esta corrida, las dos de sintaxis y las dos encontradas corriendo:

- **`full` es palabra reservada** (por `full outer join`). Postgres acepta
  `... as full` en la lista de un `select`, así que la tabla se creó igual, pero después
  `create index on w (full)` y `select count(distinct full)` fallan. La columna se llama
  `recortado`.
- En la última consulta, `w` y `v` **comparten la columna `base`**, así que el `join` la
  deja ambigua y hay que calificarla.

### Familia P — modalidad y ubicación (`medicion-ubicacion.txt`)

| # | Qué contesta | Por qué se pide |
|---|---|---|
| P1 | **Control.** Cuántas vacantes tienen `location`, su largo medio, cuántos valores distintos, y cuántos quedan vacíos al limpiar | dice si `location` es un campo confiable —poblado como `language`— o opcional. **`location_vacia_limpia` no puede ser mayor que `location_vacia_cruda`**: si lo es, la limpieza destruye ubicaciones |
| P2 | **Matriz título × location** para cada modalidad: dice remote solo en el título, solo en location, en las dos, en ninguna | **la consulta central de H1.** Decide de qué fuente se lee la modalidad, y si hace falta leer las dos |
| P3 | **Vocabulario**: cada variante por separado (`remote`, `fully remote`, `100 remote`, `work from home`, `wfh`, `telecommute`, `virtual`, `home based`, `remoto`, `teletrabajo`, `a distancia`, `onsite`, `on site`, `in office`, `in person`, `presencial`, `hybrid`, `hibrido`, `field based`), en título y en location | qué equivalentes existen **de verdad**. Sin esto, la lista de sinónimos del extractor sería inventada |
| P4 | La **forma** de `location`: top 50 valores crudos, y cuántos llevan coma, guion suelto, punto y coma, barra o paréntesis, cuántos empiezan con "Remote", y cuántos tokens tiene en promedio | si `"Remote - U.S."` es el patrón dominante, partir el campo es trivial; si no, hay que ir por tokens |
| P5 | **La regla de Elias**: cuántas dicen remote **sin** ningún lugar nombrado (→ fully remote) contra cuántas lo dicen **con** un lugar (→ remoto restringido) | dimensiona el subconjunto "puede aplicar cualquiera", que es el que motivó la hipótesis |
| P5b | Qué queda concretamente en `location` cuando dice remote (top 40), con `(nada: fully remote)` como una fila más | es la contracara de P5: permite ver a ojo si `place_left` está sacando lo que debe y no de más |
| P6 | Cuántas vacantes tienen en el título un token que **también está en su propia `location`** | **mide si el diccionario funciona**, que es lo que hace viable H2 sin una lista de ciudades |
| P6b | Cuáles son esos tokens (top 60) | para mirarlos a ojo: separa las ciudades de las coincidencias casuales |
| P7 | El **diccionario candidato**: top 100 tokens de `location` con cuántas veces aparecen ahí y cuántas en títulos | el insumo directo del extractor de B4 |

### Familia Q — ruido y atributos en el título (`medicion-ruido.txt`)

| # | Qué contesta | Hipótesis |
|---|---|---|
| Q1 / Q1b / Q1c | Marca de género por patrón, cómo queda en el limpio (los tokens sueltos `h`, `f`, `m`, `d`, `w`, `x`) y ejemplos | H5 |
| Q2a-Q2d | Sigla entre paréntesis: volumen, top 40 siglas, **cuántos títulos con sigla tienen un gemelo sin sigla en el dataset**, y los pares concretos | H6. El gemelo es la prueba: si existe, la sigla está partiendo un mismo puesto en dos |
| Q3 / Q3b | `req#`, `#` con número, números de 3 y 4+ dígitos al principio, al final y sueltos, más ejemplos | H7 |
| Q4 / Q4b | `$`, `\d+k`, `sign on bonus`, `bonus`, `hourly`/`per hour`, `salary`/`pay`, `up to`, `opportunity`, urgencia, más ejemplos | H3 |
| Q5 | `!` en el crudo, `join our`, talent pool, `general application`, `we are hiring`, `apply now` | H11 |
| Q6 | `part time`, `full time`, `intern`, `contract`, `temporary`, `seasonal`, `per diem`/`prn`, `apprentice`, `freelance`, `w2`/`1099`, `locum`, `volunteer`, `casual` | H8 |
| Q7 | `20xx`, estación (`summer`/`winter`/`fall`/`spring`), `class of`, `new grad`, `campus`, `cohort` | H10 |
| Q8 | `speaking`/`speaker`, `bilingual`, los idiomas nombrados, y el nivel del marco europeo (`b2`, `c1+`) | H9 |
| Q9a-Q9c | **`experienced`**: volumen, `experience`, `N+ years`, `experienced hire`, sinónimos (`seasoned`, `veteran`, `expert`, `advanced`), y **con qué palabra sigue y qué palabra lo precede** (top 25 y top 15) | H4. Los vecinos son lo que separa "nivel" de "categoría de reclutamiento": `experienced hire` es lo segundo |
| Q10 | **El efecto agregado**: títulos distintos y vacantes tocadas **por cada recorte separado**, contra la línea base | **la consulta que decide qué vale la pena.** Un recorte que mueve la cardinalidad menos que la marca de género (−0,2% en la ronda 2) no justifica escribir una regla |
| Q10b / Q10c | Los siete recortes **juntos**: cardinalidad, cola larga, cobertura top N y top 30 títulos | la comparación directa contra N1, N2, N3 y N4 de la ronda 2 |
| Q11 / Q11b | **Control de sanidad**: cuántos títulos quedan vacíos con solo la limpieza, sin seniority, y con todos los recortes; y cuáles son | mismo control que la consulta A del paso A. Un recorte que vacía un título borró la vacante entera |

### Cómo cierra B3

Dejo los dos `.txt` en la raíz del repo, informo el resultado de los controles duros (P1
y Q11) y **no escribo el análisis hasta que Elias los haya leído**: es una medición
exploratoria, de las que sirven para decidir cómo modelar, y la decisión tomada es que él
se forme su criterio primero. Después analizo, y de ahí salen las reglas que implementa
B4.

**Los controles, tal como dieron (2026-09-12):**

- **P1 pasa.** 128.953 vacantes, `location` nula en **1** sola, y la limpieza no vacía
  ninguna: `location_vacia_limpia` da 0 contra `location_vacia_cruda` 1. (El 0 es porque
  `clean(null)` devuelve `null` y no `''`; la fila sin `location` está contada en
  `location_null`.) El campo tiene **20.113 valores crudos distintos** y 17.699 limpios.
- **Q11 NO pasa, y es un hallazgo, no un error de la regla.** Con solo la limpieza no se
  vacía ningún título —igual que en el paso A—, pero **al sacar el seniority se vacían 6**
  y **con los siete recortes juntos, 87**. Q11b dice cuáles: los 87 son casi todos
  `Join Our Talent Community`, `Talent Network`, `Talent Pool` y variantes, o sea las
  filas que el paso A ya había marcado como *no son vacantes*; y las 6 del seniority son
  el título `Principal` solo. **Ninguna regla se da por buena hasta que Elias lea la
  salida**, pero el control queda registrado como no limpio.

### El análisis de B3

**1. Casi ningún recorte de esta ronda vale la pena, y ese es el resultado principal.**
Q10, contra la línea base de 78.142 títulos distintos:

| Recorte | Títulos distintos | Vacantes tocadas |
|---|---|---|
| línea base (limpio, sin seniority) | 78.142 | — |
| sin contrato | 77.264 (**−1,1%**) | 5.117 |
| sin números | 77.914 (−0,3%) | 2.787 |
| sin modalidad | 77.881 (−0,3%) | 2.671 |
| sin fecha | 77.915 (−0,3%) | 1.494 |
| sin marca de género | 77.980 (−0,2%) | 1.524 |
| sin marketing | 78.092 (−0,1%) | 832 |
| sin plata | 78.124 (−0,02%) | 371 |
| **los siete juntos** | **76.303 (−2,4%)** | 12.751 (9,9%) |

El seniority solo valía **−5,8% y 31.192 vacantes**: los siete recortes juntos valen
menos de la mitad. La cola baja de 49,7% a 48,5% y los top 20 suben de 4,3% a 4,5%.
**La forma de la distribución no se mueve.** La conclusión que deja es tan útil como
incómoda: el ruido de formato ya está exprimido, y lo que queda por ganar está en la
categorización por tokens, no en seguir limpiando el título.

Por eso B4 implementa tres reglas y no once. Lo que entra, entra por una razón distinta
de la cardinalidad.

**2. La modalidad: la hipótesis se confirma, pero la fuente no es el título.** P2 es
tajante: de las **16.444** vacantes que declaran trabajo remoto, **14.624 lo dicen solo
en `location`** y apenas **1.066 solo en el título** (754 en las dos). Leer solo el
título perdería el 89% de la señal. Lo mismo con híbrido: 2.345 en total, 1.829 solo en
`location`. Presencial es marginal —876— y además poco informativo, porque el default
implícito de una vacante es que sea presencial.

P3 recorta la lista de sinónimos a lo que existe: `remote` (16.416 apariciones),
`hybrid`, `home based` (280, casi todas en `location`), `on site`, `in office`, `remoto`,
`hibrido`, `presencial`, `in person`, `field based` (89). Y **`telecommute`, `telework`,
`teletrabajo`, `a distancia` y `site based` dan cero en las dos fuentes**: son
exactamente las palabras que una lista escrita de memoria habría incluido, y habrían sido
código muerto. Esa es la medición pagándose sola.

**3. La regla del fully remote funciona: 3.105 vacantes** (2,4% del total), de las cuales
3.099 salen de `location` y 6 solo del título. P5b la valida a ojo: lo que queda cuando
*no* es fully remote es casi todo país limpio —`united states` 2.075, `us` 1.916, `usa`
921, `canada` 364, `india` 221, `united kingdom` 196—. O sea el remoto casi siempre trae
un ámbito geográfico (13.339 de 16.444, el 81%), y el "puede aplicar cualquiera" es el
19% restante.

**4. La sigla redundante es el mejor hallazgo de la familia Q.** 3.033 títulos terminan
en una sigla, y **1.393 tienen un gemelo idéntico sin ella**. Q2d son pares perfectos:
`registered behavior technician` / `…rbt` (349 vacantes), `center based registered
behavior technician` / `…rbt` (87), `board certified behavior analyst` / `…bcba`,
`infusion registered nurse` / `…rn`, `licensed practical nurse` / `…lpn`,
`chief information security officer` / `…ciso`, `sdr`, `bdr`, `cna`, `tam`.

Pero la regla obvia —borrar lo que cierra el título entre paréntesis— es **la
equivocada**, y Q2b lo muestra: en ese mismo lugar viven `.NET` (24), `H/F` (212), el
código postal `91359` (46), `2027`, `US`, `UK`, `LATAM`, `EMEA`, `PRN`, `CONTRACT`.
Ninguno es redundante y borrarlos perdería información. Lo que los separa es pedir que
**las letras sean las iniciales de las palabras anteriores**, que además es una función
pura del título: no hace falta ver el resto del dataset.

**5. La marca de género entra pese a colapsar poco.** Son 1.524 vacantes y −0,2% de
cardinalidad, apenas la vara. Lo que la justifica es Q1b: deja **tokens de una sola letra
sueltos** dentro del título —`f` 1.607, `h` 970, `m` 953, `d` 760, `w` 329, `x` 259—, y
el paso A ya decidió que la categorización va a ser **por tokens**. Para un clasificador
por tokens eso es basura pura. El criterio acá es calidad de tokens, no cardinalidad.

**6. La ubicación dentro del título se descarta, y es el resultado que más contradice lo
esperado.** P6 parece prometer: 15.365 vacantes (11,9%) repiten en el título un token que
está en su propia `location`. Pero P6b muestra de qué están hechos esos tokens: `new`
287, `united` 260, `city` 234, `san` 229, `west` 177, `south` 117, más `park`, `hills`,
`mall`, `beach`, `county`, `thousand`. Y P7 da la contraprueba: `west` aparece en 500
títulos, `based` en 867, `office` en 362, `park` en 167. Un diccionario de tokens sueltos
convierte "West Coast Sales Manager" en una ubicación. **No alcanza.**

Lo que sí quedó claro es lo contrario de lo que suponíamos: **`location` es mucho más
parseable de lo que dice `docs/CONTEXTO.md`.** De 128.952 valores, **91.223 llevan coma**
(70,7%), el promedio es de 3,28 tokens y P4a es `"London, United Kingdom"`,
`"New York, NY"`, `"Remote - US"`, `"Costa Mesa, California, United States"`. Ese juicio
viejo se había hecho sobre el campo `offices` y sobre 842 vacantes de 9 boards; con
128.953 no se sostiene igual. **La ubicación estructurada merece un paso propio, saliendo
de `location` y no del título.**

**7. `experienced` no mapea a ningún nivel.** Son 253 títulos, y Q9b dice qué palabra le
sigue: `sales` 26, `event` 18, `aesthetic` 11, `clinician` 11, `trader` 9, `dermatology`
8, `teacher` 8. Es un **adjetivo del rol** —"Experienced Sales Professional"—, no un
escalón de la escala; `experienced hire`, que sí sería categoría de reclutamiento, son 3
casos. Q9c lo confirma: en 170 de 253 abre el título. Mapearlo a `SENIOR` sería inventar
la equivalencia que el paso A ya descartó para `ii` / `iii`. Lo mismo vale para
`seasoned`, `veteran`, `expert` y `advanced` (518 juntos).

**8. La hipótesis de los números de requisición estaba mal formulada.** Q3 la desmiente:
`req#` son **68** casos y `#` con número **53**. Los números que sí abundan son de otra
cosa: 2.136 títulos con un número de 4+ dígitos, y Q3b muestra que son **códigos
postales** —`thousand oaks ca 91359`, `norcross ga 30071`, `poteet texas 78065`— y
**años** —`summer 2027`—. El código postal es ubicación metida en el título, así que se
va con el paso de ubicación, no con una regla de ids.

**9. Plata y marketing son reales pero chicos.** La plata existe —`home inspector salary
50 85k` 16, `maintenance technician 2 500 sign on bonus` 11— pero toca 371 vacantes, el
0,3%. El marketing toca 832. Los dos están debajo de la vara y no justifican una regla.
Lo que sí confirma Q5 es el punto abierto que ya venía: `talent pool` 357 y
`general application` 318, **675 filas que no son vacantes**.

**10. Contrato y jornada es el recorte más grande, y por eso mismo no entra acá.** Q6:
`part time` 2.128, `intern` 1.939, `contract` 1.429, `full time` 914, `seasonal` 750,
`per diem / prn` 690, `locum` 644, `volunteer` 290, `temporary` 288 — unas 9.000
vacantes, y −1,1% de cardinalidad, el único recorte que pasa la vara con claridad. Pero
es un **atributo, no ruido**: para el matching, part time contra full time pesa tanto
como el seniority. Le corresponde un paso propio con su propia medición de falsos
positivos, igual que el seniority tuvo B1 —sin esa medición no existirían las guardas que
salvaron a `staff nurse` y `mid market`—.

**11. El control Q11 no dio limpio, y dice algo.** Con solo la limpieza no se vacía
ningún título. Al sacar el seniority se vacían **6**: son el título `Principal` a secas,
que en este dataset es el director de una escuela y no un nivel. Con los siete recortes
juntos se vacían **87**, y Q11b muestra que son casi todos `Join Our Talent Community`,
`Talent Network` y `Talent Pool`. O sea que **un título que queda vacío es señal de que
la fila no es una vacante** — una forma de detectarlas que no requiere lista de frases.
No se implementa acá porque nadie lo pidió; queda como punto abierto.

## Paso B4 — el refactor y las tres reglas que B3 sostuvo

**ESCRITO.** `./mvnw test` da **95 tests en verde** (eran 80). Sin prueba manual contra
prod: las tres clases son funciones puras, sin red y sin base, así que los tests son la
verificación. La corrida real llega en el paso C.

**Alcance, decidido con Elias después del análisis:** el refactor, la modalidad, la sigla
redundante y la marca de género. **Nada más.** Contrato y jornada, y la ubicación
estructurada, quedan para pasos propios (ver "Puntos abiertos").

### El refactor: `TitleNormalizer` se parte en dos

La clase hacía dos cosas —limpiar y extraer el seniority— y con una tercera extracción
entrando cada criterio nuevo la iba a agrandar. Elias pidió separarlas:

- **`util/TitleCleaner`** — `static String clean(String)`. Se llevó la lista blanca
  entera, tal cual estaba, y suma las dos reglas nuevas de abajo. Devuelve `""` cuando no
  queda nada legible.
- **`util/SeniorityExtractor`** — `static Extracted extract(String cleanTitle)`, con el
  record `(String title, Seniority seniority)`. Se llevó `LEVELS`, las guardas de `entry`,
  `mid` y `staff`, el `semi senior` y la regla del mínimo, **sin tocar una línea**.
- `TitleNormalizer` y su test **se borraron**. **No quedó una fachada**: quien encadena
  limpieza → seniority → modalidad es el servicio del paso C, y una clase que nadie usa
  no va.

**Los 18 tests de B2 no quedaron idénticos, y conviene decirlo.** Los de
`SeniorityExtractorTest` conservan entradas y valores esperados y encadenan las dos
clases desde un helper, que es lo que prueba que el refactor no cambió comportamiento. Los
de `TitleCleanerTest` conservan las entradas, pero el esperado ahora trae el seniority
adentro —`"senior software engineer c++"` y no `"software engineer c++"`—, porque esa
clase ya no lo saca. Y **un caso cambió de verdad**: `"Aide à domicile (H/F)"` daba
`"aide a domicile h f"` y ahora da `"aide a domicile"`. No es un error del refactor: es
la regla nueva de género haciendo su trabajo.

### Regla nueva 1: la sigla redundante al final

Se borra el último token **solo si sus letras son las iniciales de las palabras que lo
preceden**. La regla obvia —borrar la sigla final— perdía `.NET`, `(US)`, `(PRN)` y los
códigos postales, que viven en ese mismo lugar y no repiten nada.

- La sigla tiene que ser **solo letras** (eso descarta `.net`, `c#` y `91359`) y tener
  **de 2 a 6**.
- Se compara de atrás hacia adelante: la última letra contra la inicial de la palabra
  anterior, y así. **Una palabra de dos letras o menos se puede saltear** si no aporta
  letra —es lo que hace entrar `senior software development engineer in test sdet`, que
  saltea el `in`—; **una más larga que no aporta letra rompe la sigla**.
- Positivos en los tests: `rbt`, `bcba`, `lpn`, `ciso`, `sdet`. Negativos, que son la
  razón de ser de la regla: `senior software architect .net`,
  `dialysis technician thousand oaks ca 91359`, `account executive us`,
  `certified nurse midwife cnm prn`.

Se revisó a mano contra los 18 títulos de B2 que terminan en palabra corta —`ii`, `iii`,
`mid`, `staff`, `senior`, `ai`, `care`, `level`— y ninguno dispara la regla: siempre hay
una palabra larga en el medio que no aporta la letra.

### Regla nueva 2: la marca de género

Se sacan las secuencias `h f`, `f h`, `m f d`, `m w d`, `w m d`, `m f x`, `m w x` y
`f m x` **en cualquier posición** —Q1c mostró que también aparece en el medio:
`solution architect m w d financial services`— y **solo como secuencia completa**. El
test que lo cuida es real: `"Assistant(e) de vie H/F"` da `"assistant e de vie"`; la `e`
suelta de "Assistant(e)" se queda porque no es parte de la marca.

Corre **antes** que la de la sigla, así una sigla que quedaba tapada por la marca
(`… rbt h f`) queda al final y también se va.

### Regla nueva 3: la modalidad

**`model/WorkMode`** — `REMOTE`, `FULLY_REMOTE`, `HYBRID`, `ONSITE`.

- `FULLY_REMOTE` es un valor propio y no un booleano aparte porque es como Elias lo
  describió, y el javadoc aclara que es un caso de `REMOTE`, no otra modalidad.
- **`null` significa "no lo declara", no "es presencial"**: son 112.508 vacantes, y
  pasarlas a `ONSITE` sería inventar el dato. Misma semántica que `boardStatus` nulo.

**`util/WorkModeExtractor`** — `static Extracted extract(String cleanTitle, String
location)`, con el record `(String title, WorkMode mode)`. Recibe el título ya limpio y la
`location` **cruda**, que normaliza con `TitleCleaner.clean`.

- **Los sinónimos son solo los que P3 encontró.** Remoto: `remote`, `remoto`, `wfh`,
  `home based`, `work from home`, `fully remote`, `100 remote`, `remote only`. Híbrido:
  `hybrid`, `hibrido`. Presencial: `onsite`, `on site`, `in office`, `in person`,
  `presencial`, `field based`. **No están** `telecommute`, `telework`, `teletrabajo`,
  `a distancia` ni `site based`, que dieron cero. **`virtual` quedó afuera por decisión
  de Elias**: 194 títulos, pero "Virtual Assistant" es un puesto.
- **Las frases largas se prueban primero**, así `fully remote` sale entera y no deja un
  `fully` colgando en el título.
- **`location` manda sobre el título**, porque ahí vive el 89% de la señal. El título
  solo contesta cuando `location` no nombra ninguna modalidad.
- **Dentro de una misma fuente, `HYBRID` gana sobre `REMOTE` y `REMOTE` sobre `ONSITE`**,
  de más específico a menos: `"Hybrid Remote - London"` es híbrido.
- **`FULLY_REMOTE` cuando la modalidad es remota y a la `location` limpia, sacadas las
  frases de modalidad, no le queda ningún token.** `"Remote"` da fully remote;
  `"Remote - US"`, `"US Remote"` y `"Remote, United States"` dan `REMOTE`. Una `location`
  nula o vacía con remoto en el título también da fully remote (P5 midió 6 casos).
- **Las frases de modalidad salen del título siempre**, haya decidido la fuente que haya
  decidido.

**Una diferencia deliberada con la medición.** P5 contó 3.105 fully remote usando una
lista de palabras de relleno (`anywhere`, `only`, `flexible`, `global`…) que se sacaban
antes de preguntar si quedaba un lugar. **El código no la usa**: es una lista negra, que
es justo lo que este proyecto evita. Sin ella la regla es más conservadora —`"Remote"` a
secas son 2.736 vacantes— y el precio es algún falso negativo del tipo
`"Remote, Anywhere"`, que queda como `REMOTE`. Anotado en "Puntos abiertos".

## Paso C — la tabla y el proceso que la puebla

- `src/main/resources/db/migration/V4__create_normalized_vacancy.sql` — la tabla y
  `normalized_vacancy_seq` con `increment by 50`. El 50 no es decorativo: es el
  `allocationSize` que espera Hibernate, y con `ddl-auto=validate` la app no levanta
  si no coincide.
- `src/main/java/oneprofile/backend/model/NormalizedVacancy.java` — entidad 1:1 con
  `Vacancy` (FK única `vacancy_id`), con el molde de `Company` y `Vacancy`:
  constructor `protected` para JPA, uno público, getters sin setters y un método de
  dominio en vez de setters sueltos. Campos: `title`, `seniority` y **`work_mode`**, este
  último salido de B4. `place` **no** va: B3 descartó sacar la ubicación del título, y la
  estructurada tiene su propio paso. Esperar a B4 fue todo el motivo de anidarlo: así la
  tabla nace completa en vez de migrarse al poco tiempo de creada.
- `src/main/java/oneprofile/backend/repository/NormalizedVacancyRepository.java`.
- `src/main/java/oneprofile/backend/service/VacancyNormalizationService.java` — recorre
  `vacancy` **de a páginas**, con cada página en su propia transacción. Por cada vacante
  encadena `TitleCleaner.clean(title)` → `WorkModeExtractor.extract(limpio, location)` →
  `SeniorityExtractor.extract(título sin modalidad)`. No hay red de
  por medio, así que no hace falta la pausa del sondeo ni del recorrido de vacantes;
  lo que sí hace falta es no traer 128.953 entidades a memoria de una.
- `src/main/java/oneprofile/backend/controller/NormalizationController.java` — el molde
  ya probado tres veces: 202 al toque, executor de un solo hilo, `AtomicBoolean` que da
  409 si ya hay una corrida, resultado al log.

Va en un paso aparte de los B **porque se prueba distinto**: los B cierran con
`./mvnw test` y el C con una corrida real contra prod.

### Cómo cierra el paso C

`./mvnw test` en verde, y una corrida contra prod donde `count(*)` de
`normalized_vacancy` dé 128.953, el reparto de `seniority` se parezca a lo medido
(`senior` ~21.100, `staff` ~4.560, `sr` ~3.130, `principal` ~2.140), el de `work_mode`
se parezca a P2 y P5 (remoto ~16.400 entre `REMOTE` y `FULLY_REMOTE`, con fully remote
entre 2.736 y 3.105; híbrido ~2.300) y los top 20 `title` se parezcan al top 30 de Q10c.

## Puntos abiertos

- **Hay ~650 filas que no son vacantes**: `talent community/network/pool` 356 y
  `general application / speculative / future opportunities` 296. Son formularios de
  "dejanos tu CV". No se filtran en estos pasos porque nadie lo pidió; se decide cuando
  el matching exista y moleste.
- **Los rangos de nivel** (`junior to senior` 13, `junior senior` 6, `i ii iii` 94, ~200
  vacantes en total) los resuelve bien la regla del mínimo, que es justamente el caso que
  la motiva. Lo que queda feo es el conector suelto: `controls engineer all levels junior
  to senior` deja el título en `controls engineer all levels to`. No se limpia porque
  nadie lo pidió y son 200 sobre 128.953.
- **El falso positivo de `senior`** —37 vacantes de cuidado domiciliario, el 0,18%— está
  medido y **sin guarda por decisión de Elias**. Si alguna vez molesta, la guarda sería
  descartar `senior` cuando lo precede `a`, `female` o `male`.
- **El nivel `i` no está en la lista de tokens** y no hay `LEVEL_1`: no se midió y no se
  pidió, así que `engineer i` conserva el `i` adentro del título.
- **`associate` (5.861) quedó adentro del título** por ambiguo. Desambiguarlo exige
  mirar la palabra siguiente y no hay pedido de hacerlo.
- **`docs/MEDICION-VACANTES.md` dice 85.050 títulos distintos y acá dan 87.647**, con el
  `count(*)` idéntico. Ese documento aclara que el SQL del título no quedó registrado,
  así que la diferencia es de cómo se contó entonces. Sin resolver.
- **Contrato y jornada merece un paso propio.** Es el recorte más grande de B3 (5.117
  vacantes, −1,1%): `part time` 2.128, `intern` 1.939, `contract` 1.429, `full time` 914,
  `seasonal` 750, `per diem / prn` 690, `locum` 644. Es un atributo que pesa en el
  matching tanto como el seniority, y antes de escribirlo hay que medir sus falsos
  positivos, como B1 hizo con el seniority.
- **La ubicación estructurada merece un paso propio, saliendo de `location`.** B3 descartó
  sacarla del título, pero mostró que `location` es mucho más parseable de lo que dice
  `docs/CONTEXTO.md`: 70,7% lleva coma, 3,28 tokens de promedio, y la cabeza es
  `ciudad, región, país`. Ese paso también debería absorber los códigos postales metidos
  en el título (`thousand oaks ca 91359`).
- **`virtual` no está entre los sinónimos de remoto**, por decisión de Elias y sin medir:
  194 títulos y 59 `location`, pero "Virtual Assistant" es un puesto.
- **`FULLY_REMOTE` es conservador a propósito.** No se sacan palabras de relleno antes de
  preguntar si queda un lugar, así que `"Remote, Anywhere"` o `"Remote - Global"` quedan
  como `REMOTE`. La medición con relleno dio 3.105 contra 2.736 sin él: el hueco es de
  unas 370 vacantes como máximo.
- **Un título que queda vacío es señal de que la fila no es una vacante.** Q11b: los 87
  títulos que se vacían con todos los recortes de B3 son casi todos `Talent Community`,
  `Talent Network` y `Talent Pool`. Es una forma de detectar las ~650 filas que no son
  vacantes sin escribir una lista de frases; no se implementa porque nadie lo pidió.
- **`Principal` a secas se queda sin título.** Son 6 vacantes donde es el puesto (director
  de escuela) y no el nivel; `SeniorityExtractor` lo toma como `PRINCIPAL` y deja `title`
  nulo. No se agrega guarda porque son 6.
- **`experienced` y sinónimos (`seasoned`, `veteran`, `expert`, `advanced`) quedan adentro
  del título.** Q9 mostró que son adjetivos del rol y no niveles de la escala.
