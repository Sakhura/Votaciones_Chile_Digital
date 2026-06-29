# 🗳️ Sistema de Votación Digital Nacional

### Prototipo académico de arquitectura empresarial con Jakarta EE

> **Proyecto educativo · Prototipo de ejecución local**
> Instituto Profesional **IPCHILE** — Ingeniería en Informática
> Docente: **Sabina Romero Rodríguez** · Año **2026**

---

## ¿Qué es este proyecto?

Este repositorio contiene un **prototipo funcional** que demuestra cómo se diseña
y construye un sistema crítico de alta concurrencia usando **Jakarta EE 10**,
tomando como caso de estudio una **votación digital nacional** (escenario
hipotético de ~15 millones de votantes habilitados).

Es, ante todo, una **pieza de aprendizaje**. Su objetivo no es ser un sistema
electoral real, sino servir como ejemplo concreto y completo de patrones de
arquitectura empresarial: separación de capas, transacciones, concurrencia,
mensajería asíncrona, seguridad y persistencia, todos integrados en un mismo
proyecto que se puede compilar, desplegar y ejecutar en una máquina local.

---

## 🎯 Propósito educativo

El proyecto fue concebido para ilustrar, con código real y ejecutable, conceptos
que suelen verse por separado en clase:

- **Arquitectura en capas** físicamente separadas en módulos Maven, donde las
  dependencias fluyen en una sola dirección y la propia estructura impide
  mezclar responsabilidades.
- **Concurrencia y consistencia (ACID)**: cómo prevenir el "doble voto" con un
  `UPDATE` condicional atómico en lugar de un patrón inseguro.
- **Mensajería asíncrona (JMS)**: por qué encolar los votos evita que el sistema
  se caiga bajo un pico masivo de tráfico.
- **Seguridad aplicada**: hashing de credenciales (HMAC y PBKDF2), tokens
  anónimos y un diseño de base de datos que protege el secreto del voto.
- **Persistencia (JPA/Hibernate)** y configuración de un *pool* de conexiones
  afinado para no convertirse en cuello de botella.

También es un buen disparador para discutir un tema profundo de ingeniería: la
**tensión entre el secreto del voto y la verificabilidad**, que hace del voto
electrónico uno de los problemas más difíciles del software (ver
[`docs/roadmap-criptografico.md`](docs/roadmap-criptografico.md)).

---

## 🧩 ¿Qué demuestra el prototipo?

| Capacidad | Cómo se demuestra |
|---|---|
| Autenticación segura | RUT con HMAC + clave con PBKDF2 en tiempo constante |
| Anti-doble-voto | `UPDATE` atómico `HABILITADO → VOTANDO` (gana una sola petición) |
| Anonimato del voto | Tablas `votante` y `voto` separadas, sin relación trazable |
| Resistencia al pico | Registro asíncrono del voto vía cola **JMS** durable |
| Integridad/auditoría | Encadenamiento de hash entre votos (estilo *ledger*) |
| Accesibilidad | Formulario JSF con etiquetas, foco visible y alto contraste |

---

## 🛠️ Tecnologías

`Java 17/21` · `Jakarta EE 10` · `WildFly` · `MySQL 8` · `JPA / Hibernate` ·
`EJB / CDI` · `JMS (Apache Artemis)` · `JSF / PrimeFaces` · `Maven` · `IntelliJ IDEA`

---

## 🚀 Cómo ejecutarlo

Es un **prototipo de ejecución local**: se levanta en tu propia máquina con WildFly
y MySQL. La guía completa, paso a paso y con solución de problemas, está en:

➡️ **[`GUIA-EJECUCION.md`](GUIA-EJECUCION.md)**

Resumen del flujo: crear la base de datos → configurar WildFly (driver,
*datasource* y cola JMS) → compilar el WAR en IntelliJ → desplegar → probar en
`http://localhost:8080/votacion/login.xhtml` con un votante de prueba
(`11111111-1` / `1234`).

---

## 📁 Estructura

```
votacion-parent/
├── votacion-domain/        entidades JPA, DTO, excepciones
├── votacion-persistence/   repositorios, persistence.xml, migraciones SQL
├── votacion-service/       EJB, seguridad, JMS, auditoría, datos de prueba
├── votacion-web/           JSF/PrimeFaces, beans, interfaz accesible → WAR
├── wildfly-config/         datasource, cola JMS y módulo del driver MySQL
└── docs/                   roadmap criptográfico (material docente)
```

---

## ⚠️ Alcance y advertencia

Este es un **prototipo académico de ejecución local**, no un sistema apto para una
elección real. Demuestra disponibilidad, escalabilidad y consistencia, con un
anonimato a nivel de diseño de base de datos. Un sistema electoral productivo
exigiría además: anonimato criptográfico verificable de extremo a extremo (firmas
ciegas, *mix-nets*), HSM físico certificado, redundancia geográfica, auditoría de
seguridad independiente y un marco legal habilitante. Esa frontera entre "lo que
el prototipo logra" y "lo que producción exige" es, justamente, una de las
lecciones centrales del proyecto.

---

## 👩‍🏫 Créditos

Trabajo desarrollado en el marco de **Ingeniería en Informática** del
**Instituto Profesional IPCHILE**.

- **Docente guía:** Sabina Romero Rodríguez
- **Año:** 2026
- **Naturaleza:** prototipo educativo de ejecución local

---

<p align="center"><em>Proyecto con fines exclusivamente educativos.</em></p>
