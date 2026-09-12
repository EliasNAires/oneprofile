# Cómo se categoriza cada empresa

> Para personas, no para agentes. Volvé al [README](README.md); el detalle completo
> está en [`docs/CONTEXTO.md`](../CONTEXTO.md).

## Por qué no alcanza con descubrir

CommonCrawl es **una foto vieja de internet**. Que haya visto el board de una empresa
en agosto no quiere decir que hoy siga ahí: las empresas cambian de ATS, cierran, o se
mudan de slug. Con solo [descubrir](descubrimiento.md), tenés una lista donde no sabés
qué está vivo.

Por eso hay un segundo proceso, el **sondeo**: le pega al board de cada empresa y anota
qué contestó. Hay exactamente tres respuestas posibles:

| Lo que contesta el board | Qué significa |
|---|---|
| `404`, "Job not found" | **No usa Greenhouse.** Estaba en la foto vieja y ya no está. |
| `200` con cero vacantes | **Usa Greenhouse, pero no tiene nada abierto** ahora. |
| `200` con N vacantes | **Tiene búsquedas abiertas.** Es a quien hay que preguntarle. |

Eso es exactamente lo que se necesita para el paso siguiente: **saber a quién vale la
pena pedirle vacantes**, sin desperdiciar miles de requests preguntándole a boards que
no existen.

## Lo que dio la primera corrida

Una sorpresa: **la enorme mayoría sigue viva y con vacantes.** De las 4.046 empresas que
había descubierto CommonCrawl, la corrida completa dejó **3.121 activas**, **708 que ya no
están en Greenhouse** y **217 sin nada publicado**.

O sea que el **77%** de lo que vio una foto vieja de internet sigue vivo y con búsquedas
abiertas. Se esperaba bastante más mortandad de la que hubo.

De paso, el sondeo trae el **nombre legible** de la empresa —"Globant", no `globant`—
porque viene adentro de cada vacante. Gratis, sin un pedido extra. Las empresas que no
tienen vacantes abiertas se quedan sin nombre hasta que publiquen alguna; es el precio
de no duplicar la cantidad de pedidos.

## Qué pasa cuando lo disparás

![Flujo del sondeo](diagramas/flujo-sondeo.svg)

El molde es el mismo que el del descubrimiento —`202` al toque, el trabajo sigue en
otro hilo, el resultado solo se ve en el log— pero adentro el problema es distinto y
hay tres cosas que vale la pena mirar en el dibujo.

**El `404` no es un error, es una respuesta.** Es la que dice "esta empresa no usa
Greenhouse", que es una de las tres cosas que el sondeo salió a averiguar. Por eso no
se reintenta: insistirle a un board que no existe no lo hace aparecer.

**Hay una pausa de 200 ms entre empresa y empresa.** Son miles de pedidos contra la API
de otro, y no hay ninguna urgencia. La corrida completa tarda alrededor de media hora, y
está bien que así sea.

**Cada empresa se guarda por separado, apenas se la sondea.** Podría parecer más prolijo
guardar todo junto al final, pero entonces media hora de trabajo dependería de que nada
falle en el camino: si el proceso se cae a los veinte minutos, se pierde todo. Guardando
de a una, lo que ya se sondeó queda. Y si un board puntual no contesta, se anota la falla
y **se sigue con el siguiente** — no se tira la corrida entera por una empresa.

El log al terminar se lee así:

```
Greenhouse board probe started
Greenhouse board probe finished: 4046 companies, 708 not found, 217 empty, 3121 active, 0 failed
```
