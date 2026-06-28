# Roadmap criptográfico — por qué separar tablas no basta

Material docente. Explica el salto entre el anonimato "a nivel de base de datos"
del prototipo y el anonimato criptográfico real que exige una elección nacional.

---

## 1. El problema que el prototipo no resuelve

En el prototipo, el anonimato se logra separando dos tablas (`votante` y `voto`)
sin clave foránea entre ellas. Es una buena práctica de diseño, pero tiene un
punto débil que conviene que los estudiantes identifiquen con precisión:

> En el instante en que el servidor autentica a una persona y le emite un token,
> ese servidor "ve" simultáneamente **quién es** (RUT) y **qué token** recibirá.

Aunque no persistamos esa asociación, existe —en memoria, en logs, en un volcado
de RAM— durante una fracción de segundo. Un administrador malicioso, o un atacante
con acceso al proceso, podría capturarla. La separación de tablas protege contra
quien lee la base de datos *después*, no contra quien observa el sistema *durante*.

La pregunta central de la criptografía de votación es:

> ¿Cómo emite el servidor un token válido para una persona habilitada, sin que
> nadie —ni el propio servidor— pueda asociar ese token a esa persona?

La respuesta clásica son las **firmas ciegas**.

---

## 2. Firmas ciegas (David Chaum, 1982)

La idea es contraintuitiva y elegante: el servidor **firma un token que no puede
ver**. Es como meter un documento dentro de un sobre con papel carbón y pedir que
lo firmen por fuera: la firma traspasa al documento, pero quien firma nunca leyó
el contenido.

El protocolo, a alto nivel (sin las matemáticas RSA completas):

1. El votante genera su token aleatorio `t`.
2. Lo **ciega**: lo multiplica por un factor secreto `r` (el "sobre").
   Obtiene `t'` que no revela nada sobre `t`.
3. Se autentica con su RUT y envía `t'` al servidor.
4. El servidor verifica que la persona está habilitada y **no ha pedido firma
   antes**, y firma `t'` a ciegas → obtiene `firma(t')`.
5. El votante **descieg​a**: divide por `r` y obtiene `firma(t)`, una firma válida
   del servidor sobre su token original `t` — que el servidor **nunca vio**.
6. Para votar, el votante presenta `t` + `firma(t)` por un canal anónimo. El
   sistema verifica la firma (es auténtica) y acepta el voto, sin poder ligar `t`
   a la persona, porque jamás conoció `t`.

Resultado: el servidor garantiza que solo personas habilitadas obtienen un token
(una firma por persona), pero le es **matemáticamente imposible** asociar el token
al votante. El anonimato deja de depender de la disciplina del administrador y pasa
a depender de un teorema.

> Ejercicio para estudiantes: implementar firmas ciegas RSA. El cegado es
> `t' = t · rᵉ mod n`; el descegado, `firma(t) = firma(t') · r⁻¹ mod n`.
> Discutir por qué `r` debe ser coprimo con `n` y desechado tras un uso.

---

## 3. El canal anónimo: mix-nets

Las firmas ciegas resuelven el *qué* (el token no se liga a la persona), pero queda
el *cómo llega* el voto: la dirección IP, el orden de llegada y la marca de tiempo
pueden reintroducir la correlación. En el prototipo, mitigamos esto con la cola JMS
(que reordena y desacopla el tiempo). La solución criptográfica fuerte es una
**mix-net** (Chaum, 1981).

Una mix-net es una cadena de servidores ("mezcladores") que reciben un lote de
votos cifrados, los **rebaraja** y los reenvían, cada uno quitando una capa de
cifrado (como pelar una cebolla). Tras pasar por varios mezcladores
independientes, es imposible saber qué voto de salida corresponde a qué voto de
entrada — basta con que **un solo** mezclador sea honesto. Esto rompe la
correlación por orden y por origen.

---

## 4. Conteo sin descifrar: cifrado homomórfico

Una alternativa (o complemento) a las mix-nets es el **cifrado homomórfico**.
Ciertos esquemas (ElGamal exponencial, Paillier) permiten **sumar votos cifrados
sin descifrarlos**: se multiplican los textos cifrados y el resultado, al
descifrarse una sola vez al final, es la suma de los votos.

La consecuencia es notable: nunca se descifra un voto individual. Solo se descifra
el **total**. No existe el momento en que un voto en claro y una identidad
coincidan, porque ningún voto individual se abre jamás.

> Ejercicio: con ElGamal exponencial, mostrar que `E(v₁) · E(v₂) = E(v₁ + v₂)`.
> Discutir por qué se vota por "0" o "1" cifrados y se suman.

---

## 5. Verificabilidad de extremo a extremo (E2E-V)

El último pilar, y el que más confianza pública genera, es que **cualquiera pueda
verificar** que el conteo es correcto, sin confiar en la autoridad. Un sistema
E2E-V ofrece tres garantías:

- **Cast as intended**: el votante puede comprobar que su voto registró su
  intención (sin revelarla a terceros).
- **Recorded as cast**: el votante puede verificar que su voto fue incluido, usando
  un recibo con un código que **no** revela por quién votó.
- **Tallied as recorded**: cualquier observador puede verificar, con **pruebas de
  conocimiento cero**, que el total publicado corresponde a los votos registrados,
  sin abrir ninguno.

El encadenamiento de hash del prototipo (cada voto enlaza al anterior) es una
versión rudimentaria de "tallied as recorded": da evidencia de manipulación. Un
sistema real lo reemplaza por un **bulletin board** público con pruebas
criptográficas verificables por terceros.

---

## 6. Resumen del salto prototipo → producción

| Garantía | Prototipo (este proyecto) | Producción (objetivo) |
|---|---|---|
| Anonimato voto↔persona | Separación de tablas (diseño) | Firmas ciegas (matemático) |
| Canal anónimo | Cola JMS reordena | Mix-net |
| Conteo | SELECT sobre votos en claro | Cifrado homomórfico |
| Evidencia de manipulación | Encadenamiento de hash | Bulletin board + ZKP |
| Verificación por el votante | No | Recibo E2E-V |

---

## 7. Lecturas recomendadas para el curso

- David Chaum, "Blind Signatures for Untraceable Payments" (1982).
- David Chaum, "Untraceable Electronic Mail, Return Addresses, and Digital
  Pseudonyms" (1981) — mix-nets.
- Josh Benaloh, trabajos sobre verifiabilidad y conteo homomórfico.
- Análisis de seguridad del sistema suizo (CHVote) y de Estonia (i-Voting).
- Informe del Tribunal Constitucional alemán (2009) sobre voto electrónico:
  el argumento de por qué la verificabilidad por el ciudadano común es un
  requisito constitucional, no solo técnico.

La conclusión docente más importante: el voto digital seguro **no es** un problema
de "más Java EE", sino de criptografía avanzada combinada con un diseño de sistema
que asuma adversarios internos. Y aun resuelto técnicamente, sigue siendo una
decisión de política pública sobre confianza, no solo de ingeniería.
