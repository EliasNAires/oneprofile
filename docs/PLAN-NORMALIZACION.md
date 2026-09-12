# Plan — normalizar los títulos de las vacantes

> Documento **autocontenido**: pensado para que otra sesión lo retome leyendo solo
> este archivo, `docs/METODOLOGIA.md` y `docs/CONTEXTO.md`. Se borra cuando el plan
> termina y lo que valga la pena pasa a `CONTEXTO.md`.

## Estado del plan

- **Paso A — medir la distribución del título: HECHO.** Corrido contra prod el
  2026-09-12 y analizado. Los números y su lectura están más abajo; las decisiones que
  dejó, en "Decisiones que cerró el paso A".
- **Paso B — el normalizador (función pura): EN CURSO, partido en dos.**
  - **B1 — medir los falsos positivos de los tokens de seniority: CORRIDO** contra prod
    el 2026-09-12. La salida cruda está en `medicion-seniority.txt`, en la raíz del
    repo. **Elias todavía no la leyó**, así que el análisis está sin hacer **a
    propósito** (ver "Quién lee la salida primero" más abajo). Es lo primero que hay que
    hacer al retomar.
  - **B2 — el código (`Seniority`, `TitleNormalizer` y su test): PENDIENTE.** Depende de
    las guardas que salgan de leer B1.
- **Paso C — la tabla y el proceso que la puebla: PENDIENTE.**

**Por dónde arrancar la próxima sesión:** Elias lee `medicion-seniority.txt`, y con eso
se define la lista de guardas de B2 —cada una justificada con su número— y se escribe el
código. No hace falta correr ninguna medición más.

**Atajo de lectura:** las secciones "Resultados" y "Segunda ronda" son **salida cruda de
psql, larga y saltéable**. Lo que hay que leer sí o sí es "Decisiones ya tomadas", "El
análisis", "Decisiones que cerró el paso A" y los pasos B1, B2 y C.

Las salidas crudas de las tres mediciones viven en la raíz del repo, **sin versionar**
(se borran cuando el plan termine):

| Archivo | Qué contiene |
|---|---|
| `medicion-titulos.txt` | Paso A, primera ronda: distribución del título limpio. Ya analizada. |
| `medicion-titulos-ronda2.txt` | Paso A, segunda ronda: cuantificación del seniority. Ya analizada. |
| `medicion-seniority.txt` | **Paso B1: falsos positivos de cada token de seniority. SIN ANALIZAR.** |

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

### Cómo cierra B1

Elias lee `medicion-seniority.txt`. Con eso se define **la lista de guardas**, cada una
justificada con su número, y recién entonces se escribe B2. **El análisis está
deliberadamente sin hacer**: no hay que reconstruirlo ni buscarlo en otro lado, hay que
leer el `.txt`.

## Paso B2 — el normalizador (función pura)

Chico y cerrado: **no toca la base**. Se prueba entero con `./mvnw test`.

- `src/main/java/oneprofile/backend/model/Seniority.java` — enum, al lado de `Ats` y
  `BoardStatus`. Valores: `ENTRY`, `JUNIOR`, `MID`, `SENIOR`, `STAFF`, `PRINCIPAL`,
  `LEVEL_2`, `LEVEL_3`. Los dos últimos son `ii` y `iii` **sin traducir a una
  seniority nombrada**: mapear "Engineer II" a `MID` sería inventar una equivalencia
  que los datos no dicen.
- `src/main/java/oneprofile/backend/util/TitleNormalizer.java` — clase final sin
  instancias, un método `static NormalizedTitle normalize(String title)` que devuelve
  un record `(String title, Seniority seniority)`. Función pura, sin red y sin Spring,
  al lado de `GreenhouseBoardUrl` y `HtmlToText`. Implementa la regla de la lista
  blanca y después saca el seniority. Devuelve `title` en `null` cuando no queda nada,
  igual que `HtmlToText.plainText`.

  **Precedencia cuando hay más de uno** ("Senior Staff Engineer" existe):
  `PRINCIPAL` > `STAFF` > `SENIOR` > `MID` > `JUNIOR` > `ENTRY`, y los niveles
  `ii`/`iii` solo se miran si no apareció ninguna palabra. **S4b es la que dice si esa
  precedencia alcanza** o si hay combinaciones que no se previeron.

  **Las guardas de los falsos positivos salen de B1** y hay que leerlas de ahí: un token
  cuenta como seniority solo si pasa su guarda, y si no la pasa **se queda adentro del
  título** como cualquier otra palabra. La forma que tome cada guarda —posición en el
  título, palabra siguiente, palabra anterior— la decide la salida de B1; lo que ya se
  sabe es que **la posición es la guarda preferible cuando alcanza**, porque es general,
  y que enumerar palabras es el último recurso, por la misma razón por la que la
  limpieza de caracteres es lista blanca y no lista negra.
- `src/test/java/oneprofile/backend/util/TitleNormalizerTest.java` — sin contexto de
  Spring. Casos que salen de los datos reales medidos: `C++`, `C#`, `.NET`, `Node.js`,
  `R&D`, `Women's`; los negativos de la regla posicional (`Engineer.`, `#hiring`,
  `Sales & Marketing`); las variantes que la consulta H mostró colapsando
  (`Sr. Software Engineer - Backend`, `Senior Software Engineer (Backend)`,
  `Aide à domicile (H/F)`); la precedencia de `Senior Staff Engineer`; y que
  `Director of Engineering` **conserva** el `director`.

  Se suman **un test por cada falso positivo que B1 confirme** —el caso tiene que ser un
  título real de la salida, no inventado—, que el título en coreano no quede vacío, y que
  `null`, `""` y un título que es solo puntuación devuelvan `title` en `null`.

### Cómo cierra B2

`./mvnw test` en verde. Hoy son 62 tests; los nuevos se suman a esa cuenta. **No hay
prueba manual contra prod en este paso**: el normalizador no tiene red ni base, así que
los tests *son* la verificación. La corrida real llega en el paso C, cuando exista la
tabla.

## Paso C — la tabla y el proceso que la puebla

- `src/main/resources/db/migration/V4__create_normalized_vacancy.sql` — la tabla y
  `normalized_vacancy_seq` con `increment by 50`. El 50 no es decorativo: es el
  `allocationSize` que espera Hibernate, y con `ddl-auto=validate` la app no levanta
  si no coincide.
- `src/main/java/oneprofile/backend/model/NormalizedVacancy.java` — entidad 1:1 con
  `Vacancy` (FK única `vacancy_id`), con el molde de `Company` y `Vacancy`:
  constructor `protected` para JPA, uno público, getters sin setters y un método de
  dominio en vez de setters sueltos. Campos: `title` y `seniority`.
- `src/main/java/oneprofile/backend/repository/NormalizedVacancyRepository.java`.
- `src/main/java/oneprofile/backend/service/VacancyNormalizationService.java` — recorre
  `vacancy` **de a páginas**, con cada página en su propia transacción. No hay red de
  por medio, así que no hace falta la pausa del sondeo ni del recorrido de vacantes;
  lo que sí hace falta es no traer 128.953 entidades a memoria de una.
- `src/main/java/oneprofile/backend/controller/NormalizationController.java` — el molde
  ya probado tres veces: 202 al toque, executor de un solo hilo, `AtomicBoolean` que da
  409 si ya hay una corrida, resultado al log.

Va en un paso aparte del B **porque se prueba distinto**: el B cierra con `./mvnw test`
y el C con una corrida real contra prod.

### Cómo cierra el paso C

`./mvnw test` en verde, y una corrida contra prod donde `count(*)` de
`normalized_vacancy` dé 128.953, el reparto de `seniority` se parezca a lo medido
(`senior` ~21.100, `staff` ~4.560, `sr` ~3.130, `principal` ~2.140) y los top 20
`title` se parezcan al top 30 de la consulta N4.

## Puntos abiertos

- **Hay ~650 filas que no son vacantes**: `talent community/network/pool` 356 y
  `general application / speculative / future opportunities` 296. Son formularios de
  "dejanos tu CV". No se filtran en estos pasos porque nadie lo pidió; se decide cuando
  el matching exista y moleste.
- **`associate` (5.861) quedó adentro del título** por ambiguo. Desambiguarlo exige
  mirar la palabra siguiente y no hay pedido de hacerlo.
- **`docs/MEDICION-VACANTES.md` dice 85.050 títulos distintos y acá dan 87.647**, con el
  `count(*)` idéntico. Ese documento aclara que el SQL del título no quedó registrado,
  así que la diferencia es de cómo se contó entonces. Sin resolver.
