# Medición — la tabla `vacancy` antes de normalizar

> **Esto no es un plan y no propone ningún enfoque.** Son los números de la tabla
> `vacancy` ya cargada, medidos el **2026-09-12** contra la base de prod por SSH.
> Está acá para que quien planifique la normalización no tenga que volver a medir:
> la corrida cuesta una sesión contra el servidor. Las lecturas y decisiones que se
> habían escrito sobre estos datos se sacaron a propósito, para discutirlas en limpio.

## Las consultas

Sin código, sin migraciones, sin dependencias. Se corre en el servidor:

```bash
cd ~/oneprofile
docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"' > ~/medicion.txt <<'SQL'
\echo === 1. volumen ===
select count(*) as vacantes, count(distinct company_id) as empresas from vacancy;

\echo === 2. la columna language de greenhouse ===
select coalesce(language, '(null)') as language, count(*) as vacantes
from vacancy group by 1 order by 2 desc limit 30;

\echo === 3. departamentos: nulos, crudos y normalizados ===
select count(*) filter (where department is null) as sin_departamento,
       count(distinct department) as distintos_crudos,
       count(distinct regexp_replace(lower(btrim(department)), '\s+', ' ', 'g')) as distintos_normalizados
from vacancy;

\echo === 4. cuanta cola larga hay ===
with freq as (
  select regexp_replace(lower(btrim(department)), '\s+', ' ', 'g') as d, count(*) as c
  from vacancy where department is not null group by 1)
select count(*) as distintos,
       count(*) filter (where c = 1) as aparecen_una_sola_vez,
       sum(c) filter (where c = 1) as vacantes_en_esa_cola
from freq;

\echo === 5. cobertura acumulada de los top N departamentos ===
with freq as (
  select regexp_replace(lower(btrim(department)), '\s+', ' ', 'g') as d, count(*) as c
  from vacancy where department is not null group by 1),
ranked as (
  select d, c,
         row_number() over (order by c desc) as rn,
         sum(c) over (order by c desc rows unbounded preceding) as acum,
         sum(c) over () as total
  from freq)
select rn, c as vacantes, round(100.0 * acum / total, 1) as pct_acumulado
from ranked where rn in (10, 25, 50, 100, 200, 500, 1000) order by rn;

\echo === 6. top 200 departamentos normalizados ===
select regexp_replace(lower(btrim(department)), '\s+', ' ', 'g') as departamento, count(*) as vacantes
from vacancy where department is not null group by 1 order by 2 desc limit 200;

\echo === 7. top 100 tokens de departamento ===
with tokens as (
  select btrim(tok) as token
  from vacancy, lateral regexp_split_to_table(lower(department), '[&/,()|]|\s-\s|\sand\s') as tok
  where department is not null)
select token, count(*) as vacantes
from tokens where token <> '' group by 1 order by 2 desc limit 100;

\echo === 8. proxy de idioma sobre la descripcion (heuristica tosca) ===
select count(*) as con_descripcion,
       count(*) filter (where description ~* '\m(que|para|con|los|las|una)\M') as pinta_espanol
from vacancy where description is not null;

\echo === 9. cruce: lo que dice greenhouse vs el proxy ===
select coalesce(language, '(null)') as language,
       count(*) as vacantes,
       count(*) filter (where description ~* '\m(que|para|con|los|las|una)\M') as pinta_espanol
from vacancy group by 1 order by 2 desc limit 15;
SQL
```

Después, `cat ~/medicion.txt`.

### Qué mide cada una

- **1** la escala real de la tabla.
- **2** cuánto viene poblada la columna `language`, que llega tal cual del campo
  `language` de la API de Greenhouse.
- **3** cuánto ruido saca normalizar el departamento: la diferencia entre distintos
  crudos y normalizados es exactamente lo que aportan mayúsculas y espacios de más.
- **4** cuánta cola larga hay, o sea cuántos departamentos aparecen una sola vez.
- **5** qué porcentaje de las vacantes cubren los top N departamentos.
- **6** el listado de los departamentos más frecuentes, en crudo.
- **7** los mismos departamentos tokenizados, cortando por `&`, `/`, `,`, paréntesis,
  `|`, guion suelto y `and`. Es lo que hace que `"Product & Engineering"` y
  `"R&D - Engineering"` aporten los dos el token `engineering` en vez de ser dos
  valores nuevos.
- **8 y 9** son un **proxy tosco, no detección de idioma**: buscan palabras
  funcionales del español en la descripción, y cruzan ese proxy contra lo que dice
  `language`.

## Los resultados (2026-09-12, contra prod)

### 1. Volumen

```
vacantes | empresas
  128953 |     3118
```

**128.953 vacantes sobre 3.118 empresas.** La proyección que había en
`docs/CONTEXTO.md` era de ~250.000, o sea el doble: la mediana de la muestra
(17 vacantes por empresa) resultó mejor guía que la media (80).

### 2. `language` viene poblado al 100%

Cero `null`. Los 17 valores suman exactamente las 128.953 filas:

```
en 123847 | pt 1805 | fr 1314 | de 562 | es 446 | ko 302 | ja 300 | it 127
nl 76 | iw 55 | zhHant 53 | pl 40 | zh 14 | no 6 | fi 3 | ru 2 | se 1
```

El cruce con el proxy da **415 de 446 en `es` (93%)**, y cero falsos positivos en
`ko`, `ja`, `zh`, `iw` y `pl`. Donde el proxy se ensucia es con `pt`, `fr` e `it`,
que comparten la palabra "que" — era esperable, el proxy es tosco a propósito.

Dos números que salen de acá:

- `en` + `es` son **124.293 de 128.953, el 96,4%**.
- El español son **446 vacantes, el 0,35%**. Greenhouse es un ATS de empresas de
  EE.UU. y se nota.

### 3. Departamentos: cardinalidad y cola

```
sin_departamento | distintos_crudos | distintos_normalizados
             510 |            14106 |                  13729
```

Normalizar (minúsculas + trim + espacios) saca apenas el **2,7%**: el ruido no está
en el formato, está en que cada empresa usa su propio vocabulario.

```
distintos | aparecen_una_vez | vacantes_en_esa_cola
    13729 |             5086 |                 5086
```

Cobertura acumulada de los top N:

```
 top 10 → 17,2%      top 100 → 39,2%      top 500 → 57,7%
 top 25 → 24,8%      top 200 → 47,0%      top 1000 → 66,8%
 top 50 → 31,8%
```

### 4. Un corte en cuatro baldes por departamento

Con un borrador generoso de patrones sobre el departamento normalizado, el reparto
fue:

```
balde        | departamentos | vacantes |   pct
DESCONOCIDO  |          8906 |    76570 | 59,6%
TECH         |          2689 |    26911 | 21,0%
DESCARTA     |          1332 |    17627 | 13,7%
QUIZAS       |           802 |     7335 |  5,7%
```

Los `DESCONOCIDO` más grandes son áreas de negocio genéricas:

```
sales 4270 | operations 2606 | marketing 2253 | finance 1550 | field sales 881
customer success 658 | people 577 | comercial 573 | commercial 494
business development 426 | human resources 381 | revenue 324 | supply chain 301
business operations 276 | compliance 246 | account management 219
```

Ninguna de esas es homogénea: `sales` contiene Sales Engineer, `operations`
contiene DevOps, `marketing` contiene Growth Engineer y `finance` contiene Data
Analyst.

El resto del balde son **acrónimos internos de cada empresa**, sin significado
fuera de ella: `max ahp` (993), `ne team` (523), `hss` (486),
`statistician network` (413), `lmdiv` (355), `gn` (361),
`prop business ops support` (302).

### 5. El título

```
vacantes | titulos_distintos | titulo_pinta_tech
  128953 |             85050 |             36179
```

**85.050 títulos distintos**, y un regex tosco de doce palabras sobre el título
matchea **36.179 vacantes (28%)**.

### 6. Un bug del tokenizador

La consulta 7 corta `"R&D"` por el `&` y genera los tokens `r` (597) y `d` (533).
Si se usa tokenización, hay que no cortar por `&` cuando queda un token de una sola
letra.

## Qué de esto no es reproducible tal cual

Las consultas 1 a 9 están arriba completas y se pueden volver a correr. En cambio
**el SQL del corte en baldes (resultado 4) y el del título (resultado 5) no quedó
registrado**: se escribieron a mano en esa sesión y no se guardó el texto. Los
números están, pero para reproducirlos hay que volver a escribir los patrones de
los baldes y el regex de doce palabras, y con otros patrones los porcentajes van a
dar distinto.
