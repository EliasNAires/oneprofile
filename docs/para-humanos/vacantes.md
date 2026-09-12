# Cómo se traen las vacantes

> Para personas, no para agentes. Volvé al [README](README.md); el detalle completo
> está en [`docs/CONTEXTO.md`](../CONTEXTO.md).

## Dónde estamos

Este es el proceso que produce **lo que el proyecto vino a buscar**. Los dos anteriores
son preparación: [descubrir](descubrimiento.md) dejó la lista de empresas y
[sondear](sondeo.md) dejó marcadas las 3.121 que hoy tienen búsquedas abiertas.

Se construyó en dos tandas, a propósito. Primero **una empresa por vez**, para poder
mirar el ciclo entero de cerca en segundos. Después **las 3.121 de una pasada**, que es
una corrida de varias horas y no se puede mirar de cerca. Los dos están probados: el de
una empresa contra la API real, y el masivo corrió **entero en el servidor** y dejó
**128.953 vacantes de 3.118 empresas**.

Ese 3.118 no es un error de tipeo: son 3.118 de las 3.121 activas, o sea que **tres
empresas quedaron sin ninguna vacante**. No se averiguó por qué —pueden haber cerrado sus
búsquedas entre el sondeo y la carga, o haber fallado— y en los dos casos la corrida
siguió igual, que es exactamente para lo que está el contador de fallas.

Para ver el avance de una corrida hay que preguntarle a la base, porque el endpoint
contesta "ya arranqué" y el resto se ve recién al final, en el log:

```bash
docker compose exec -T postgres sh -c \
  'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "select count(*), count(distinct company_id) from vacancy"'
```

Si ese número sube, está entrando trabajo. Si se queda quieto varios minutos y el log
tampoco se mueve, ahí sí hay algo trabado.

## Lo mismo que el sondeo, pidiendo más

La API es la **misma** que usa el sondeo, con dos parámetros agregados:

```
GET /v1/boards/<slug>/jobs?content=true&pay_transparency=true
```

- **`content=true`** trae la descripción completa de cada vacante. Es el único lugar
  donde están los requisitos de verdad —qué tecnologías, cuánta experiencia, si el
  remoto es remoto—; sin eso, de una vacante solo se tendría el título.
- **`pay_transparency=true`** trae el sueldo **como números**: mínimo, máximo y moneda.
  Aparece en algo más de la mitad de las vacantes que lo publican.

El precio es el tamaño. Pedir la descripción **multiplica la respuesta por diez o
más**: el board de Splice pasa de 2 KB a 53 KB, y el de Stripe llega a 4,7 MB de un
saque. Por eso el cliente tuvo que aguantar esperas más largas —de 30 segundos a dos
minutos— antes de dar una respuesta por perdida.

## Dos trampas que el JSON esconde

**La descripción viene escapada dos veces.** No llega como `<div>` sino como
`&lt;div&gt;`, y un espacio duro del texto original —`&nbsp;`— llega convertido en
`&amp;nbsp;`. O sea que para llegar al texto que lee una persona hay que desarmar el
disfraz dos veces, no una. Si se hace una sola pasada queda un texto lleno de
`&nbsp;` sueltos. Eso lo resuelve una función chiquita, `HtmlToText`, que no hace
nada más que eso y por eso se puede probar sola.

**El sueldo no siempre es anual.** El rango viene en centavos con su moneda, pero el
único lugar que dice si son centavos por año o por hora es el **título** que la empresa
le puso arriba: `"Annual base salary range:"` o `"Hourly Rate:"`. Son textos libres,
distintos en cada empresa. Así que ese título **se guarda tal como vino**, sin
interpretarlo: adivinarlo con reglas sobre texto libre sería inventar, y sin él un
sueldo de 40 dólares por hora quedaría indistinguible de uno de 40 dólares por año.

## Qué se guarda y qué se deja pasar

De cada vacante se guarda el título, la descripción ya limpia, la ubicación, el
departamento, el link para postularse, el idioma, el sueldo cuando está, y dos
fechas: cuándo se publicó y cuándo se modificó por última vez.

Se deja pasar bastante, y es deliberado. Lo más tentador que quedó afuera:

- **La ubicación normalizada.** La API tiene un campo con el país bien escrito
  —"Bengaluru, Karnataka, India"— pero **solo lo completa en el 13% de los casos**. El
  resto trae etiquetas internas de la empresa: `"US"`, `"Ireland Locations"`,
  `"US-PERM"`. Con 13% no se puede basar un filtro por país, así que la ubicación queda
  guardada como el texto libre que es: `"Remote - U.S."`, `"Atlanta; New York"`,
  `"AMER"`, incluso `"(San Francisco, Chicago, NYC)"`. Ordenar eso es un problema
  aparte, más adelante.
- **Los campos personalizados.** Algunas empresas publican cosas jugosas, como
  "Workplace Type: Hybrid". El problema es que cada una los nombra a su manera, así que
  no hay dos empresas comparables. Sin esquema común no sirven para filtrar.

## Cómo lo probás vos

Hay un detalle incómodo: **la base de desarrollo arranca vacía.** Las 4.046 empresas
viven en el volumen de producción, así que en dev hay que crear a mano la empresa que
vas a probar. Con la app corriendo (`./mvnw spring-boot:run`):

```bash
docker compose exec -T postgres psql -U oneprofile -d oneprofile -c \
  "insert into company (id, ats, slug) values
     (nextval('company_seq'), 'GREENHOUSE', 'figma');"

curl -s -X POST localhost:8080/admin/vacancies/greenhouse/figma
```

Contesta **al instante y en la misma respuesta**, no como los otros dos:

```json
{"fetched":153,"inserted":153,"updated":0,"deleted":0}
```

Que este endpoint conteste en línea y los otros dos con un `202` no es inconsistencia:
una empresa sola tarda un segundo, y ahí esperar es lo más cómodo. Media hora de
sondeo, en cambio, no se puede esperar con el navegador abierto.

**Volvé a correr el mismo comando.** Tiene que contestar `inserted:0` y
`updated:153`: el board es el espejo, así que lo que ya estaba se actualiza en lugar de
duplicarse. Un slug que no esté en la base contesta `404` — pero si la empresa existe, no
importa en qué estado la haya dejado el sondeo: este endpoint le pega igual, y por eso
sirve para probar una empresa puntual.

Para mirar lo que quedó:

```sql
select count(*) total, count(pay_min_cents) con_salario from vacancy;

select title, location, pay_min_cents, pay_max_cents, pay_currency, pay_title
  from vacancy where pay_min_cents is not null limit 5;
```

Y para confirmar que la limpieza del HTML funcionó, contar lo que **no** tendría que
existir:

```sql
select count(*) from vacancy
 where description ~ '</[a-zA-Z]'
    or description ~ '<(div|p|br|li|ul|strong|span|em|h[1-6])[ />]'
    or description like '%&nbsp;%' or description like '%&amp;%';
```

Tiene que dar **0**. Ojo con la versión tosca de este chequeo —buscar un `<` suelto—:
da falsos positivos, porque hay descripciones que dicen cosas como `"(<5000 FTEs)"`.

## Las 3.121 de una pasada

El mismo trabajo, repetido, con **medio segundo de pausa entre empresa y empresa** para no
maltratar una API ajena. Es el doble de la pausa del sondeo, porque acá cada respuesta
pesa muchísimo más.

```bash
curl -i -X POST localhost:8080/admin/vacancies/greenhouse    # sin slug
```

Este sí contesta `202` y sigue por atrás, como los otros dos procesos, porque son varias
horas. El resultado aparece al final, en el log, y en la corrida real dio 128.953 vacantes
sobre 3.118 de las 3.121 empresas activas.

Tres cosas que vale la pena saber:

- **Solo se le piden vacantes a las empresas marcadas como activas.** Las que el sondeo
  dejó vacías o inexistentes se saltean enteras: ya dijeron que no tienen nada.
- **Eso significa que el orden importa**: primero sondear, después traer vacantes. Si una
  empresa empezó a publicar ayer y el sondeo es de la semana pasada, esa empresa sigue
  marcada como vacía y no se la va a consultar hasta el próximo sondeo.
- **Una empresa que falla no arruina la corrida.** Si un board murió desde el último
  sondeo, contesta un error, queda anotado en el log, se cuenta aparte y la corrida sigue
  con la siguiente. Esa empresa conserva las vacantes que ya tenía.

## El borrado

Una vacante que deja de estar en el board **se borra**, y esto vale para los dos
endpoints. La tabla es una foto de hoy, no un archivo histórico: que una búsqueda
desaparezca del board es la señal más confiable de que se cerró.

Cómo funciona es casi un truco de manos. Antes de pedirle nada a la API, el proceso se
arma un mapa con todo lo que ya tenía guardado de esa empresa. Después, por cada vacante
que llega del board, **la saca del mapa** —y de paso decide si es nueva o si hay que
actualizarla—. Cuando terminó de recorrer el board, lo que quedó en el mapa es
exactamente lo que el board ya no tiene, y eso es lo que se borra.

Hubo una tentación relacionada que **se descartó a propósito**: no cargar las vacantes que
nadie tocó en más de dos meses, dándolas por abandonadas. Se midió antes de escribirlo y
no daba: la API no deja pedir "solo las recientes" (así que igual hay que bajarlas todas),
y el corte recortaba apenas un 7%, muy desparejo — 0% en Stripe y Discord, 17% en Brex.
Eso no dice que Brex abandone más búsquedas: dice que **algunas empresas repasan todo su
board periódicamente y otras no**, o sea que el criterio termina midiendo los hábitos del
equipo de RRHH. Y el error es de un solo lado: guardar una vacante muerta cuesta unos KB,
borrar una viva cuesta justo lo que el proyecto quiere producir. Así que se guardan todas,
y la fecha queda ahí para filtrar u ordenar **al buscar**.

## La proyección que quedó desmentida

Antes de correrlo se esperaban **del orden de 250.000 vacantes**, casi el doble de las
128.953 que salieron. La proyección se había hecho sobre una muestra de 40 empresas al
azar, usando el **promedio**: 80 vacantes por empresa. El problema es que un promedio así
lo levantan unas pocas empresas gigantes —Stripe sola publica 628—, mientras que la
empresa típica tiene muchísimas menos. La **mediana** de esa misma muestra era 17, y era
la que había que mirar.

No cambia nada de lo construido, pero sí la forma de proyectar: con datos tan desparejos,
el promedio miente.

## Qué falta

Los títulos son texto libre, y para que un matching sirva hay que lograr que
"Sr. Backend Engineer", "Backend Developer Senior" y "SWE II - Backend" se reconozcan como
el mismo puesto. Es lo que está en curso ahora, y no se puede contar todavía como
funcionando.
