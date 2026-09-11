# Cómo se descubren las empresas

> Para personas, no para agentes. Volvé al [README](README.md); el detalle completo
> está en [`docs/CONTEXTO.md`](../CONTEXTO.md).

## De dónde sale la lista

Para pedirle las vacantes a la API de Greenhouse hay que saber **de qué empresa** las
querés, y no existe ninguna lista pública de empresas por ATS. Ese fue el primer
problema a resolver.

La salida fue **CommonCrawl**: un archivo público que recorre internet y guarda un
índice de todas las URLs que vio. Si le preguntás por todo lo que empiece con
`boards.greenhouse.io/`, te devuelve decenas de miles de direcciones, y adentro de
cada una está el slug de la empresa.

Preguntarle al índice es caro y lento, así que esto **no corre solo**: se dispara a
mano cuando hace falta y el resultado queda guardado en la base.

## Qué pasa cuando lo disparás

![Flujo del descubrimiento](diagramas/flujo-descubrimiento.svg)

Lo importante de este dibujo es que **el POST te contesta al toque**, en un segundo,
con un `202 Accepted` que quiere decir "lo tomé, andá tranquilo". El trabajo de verdad
tarda varios minutos y sigue en otro hilo. **La única forma de ver cómo terminó es
mirar el log:**

```
Greenhouse discovery started on CommonCrawl index CC-MAIN-2026-34
Greenhouse discovery finished: 4046 slugs found, 4046 new companies saved
```

Lo otro que se ve ahí: el cliente **nunca junta todas las URLs en una lista**. Va
leyendo la respuesta línea por línea y entregando una URL a la vez, porque una sola
página del índice pesa unos 9 MB.

Y los slugs se acumulan en un conjunto (un `Set`), que es lo que hace que las
repeticiones se resuelvan solas: la misma empresa aparece en cientos de URLs distintas
y termina siendo una sola entrada.

## Cómo se saca el slug de una URL

![De URL a slug](diagramas/url-a-slug.svg)

Parece trivial —"tomá lo que viene después de la barra"— pero no lo es. Entre las URLs
que devuelve el índice hay `robots.txt`, hay dominios pelados sin nada después, y hay
una porción grande de **boards embebidos** (`/embed/job_board?for=X`), que son el mismo
board metido dentro de la web de carreras de la empresa. Si esos se tiraran a la basura
se perderían muchísimas empresas.

De una corrida salen unas **4.046 empresas distintas**. Lo que queda afuera se verificó
aparte, calculándolo fuera de la app: no se está perdiendo nada.
