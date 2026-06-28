# Sistema de Votación Digital Nacional — Prototipo

Prototipo funcional (maqueta académica) que demuestra una arquitectura Jakarta EE 10
de alta concurrencia para votación digital. **No es apto para una elección real**:
ver la sección "Límites y deuda técnica".

Stack: Jakarta EE 10 · WildFly · MySQL 8 · JSF/PrimeFaces · JMS (Artemis) · Maven · IntelliJ IDEA.

---

## 1. Requisitos

- JDK 17
- Maven 3.9+
- MySQL 8.x en `localhost:3306`
- WildFly 31+ (full platform)
- IntelliJ IDEA (Community o Ultimate)

## 2. Base de datos

```sql
-- Como root en MySQL:
SOURCE votacion-persistence/src/main/resources/db/migration/V1__esquema_inicial.sql;
SOURCE votacion-persistence/src/main/resources/db/migration/V2__credencial_votante.sql;

-- Usuario de la aplicacion (minimo privilegio recomendado en produccion)
CREATE USER 'votacion_app'@'localhost' IDENTIFIED BY 'CAMBIAR_EN_DESPLIEGUE';
GRANT SELECT, INSERT, UPDATE ON votacion_nacional.* TO 'votacion_app'@'localhost';
FLUSH PRIVILEGES;
```

> Las migraciones usan la convención de nombres de Flyway (`V1__`, `V2__`).
> Si usas el plugin de Flyway, se aplican automáticamente; aquí se cargan a mano
> para simplificar el piloto.

## 3. Driver MySQL en WildFly

1. Descarga `mysql-connector-j-8.4.0.jar` de Maven Central.
2. Cópialo a `wildfly-config/mysql-module/com/mysql/main/`.
3. Copia la carpeta `com/` a `$WILDFLY_HOME/modules/`.
4. Registra el `<driver name="mysql">` en `standalone-full.xml`
   (ver `wildfly-config/mysql-module/LEEME.txt`).

## 4. DataSource y cola JMS

- Integra el bloque `<datasource>` de `wildfly-config/votacion-ds.xml` en el
  subsistema `datasources` de `standalone-full.xml`, o despliega el archivo en
  `$WILDFLY_HOME/standalone/deployments/`.
- Integra el `<jms-queue>` de `wildfly-config/jms-cola-votos.xml` en el subsistema
  `messaging-activemq` de `standalone-full.xml`.

## 5. El pepper criptográfico

El `HashService` usa un *pepper* para el HMAC del RUT. Para el piloto, defínelo
como propiedad de sistema al arrancar WildFly:

```bash
./standalone.sh -c standalone-full.xml -Dvotacion.rut.pepper=UN_SECRETO_LARGO
```

> En producción el pepper debe venir de un HSM, nunca de la línea de comandos.

## 6. Compilar y desplegar

```bash
# Desde votacion-parent/
mvn clean package
# Genera votacion-web/target/votacion.war
```

Despliega `votacion.war` en WildFly (cópialo a `standalone/deployments/` o usa el
plugin de WildFly). Al desplegar, `CargaDatosPrueba` siembra una elección,
candidatos y votantes de prueba **si la base está vacía**.

## 7. Probar el flujo completo

1. Abre `http://localhost:8080/votacion/login.xhtml`
2. Ingresa con un votante de prueba:
   - RUT `11111111-1` · clave `1234`
3. Selecciona una opción y presiona **Emitir voto**.
4. Verifica:
   - En `voto` aparece una fila anónima con `token_anonimo` y `hash_actual`.
   - En `auditoria_evento` hay eventos `AUTENTICACION`, `TOKEN_EMITIDO`,
     `VOTO_ENCOLADO`, `VOTO_PERSISTIDO` — ninguno cruza `rut_hash` con `token_ref`.
   - Si reingresas con el mismo RUT, el sistema rechaza por doble voto
     (estado `VOTANDO`/`VOTADO`).

## 8. Prueba de carga (opcional)

Usa JMeter para simular un pico de autenticaciones+votos concurrentes y observa:
- el pool de conexiones no se desborda (cola JMS absorbe el pico),
- el `blocking-timeout` rechaza rápido en saturación en vez de colgar hilos.

---

## Estructura del proyecto

```
votacion-parent/
├── votacion-domain/        entidades JPA, DTO, excepciones
├── votacion-persistence/   repositorios, persistence.xml, migraciones
├── votacion-service/       EJB, seguridad, JMS (productor + MDB), auditoría, seeder
├── votacion-web/           JSF/PrimeFaces, beans, CSS/JS accesibles  -> WAR
├── wildfly-config/         datasource, cola JMS, módulo del driver MySQL
└── docs/                   roadmap criptográfico
```

## Límites y deuda técnica (LEER)

Este prototipo demuestra **disponibilidad, escalabilidad y consistencia**, y un
anonimato a nivel de diseño de base de datos. NO implementa el anonimato
criptográfico fuerte que exige una elección nacional real. Antes de cualquier uso
productivo se requiere, como mínimo:

- Anonimato criptográfico **end-to-end verificable** (firmas ciegas, mix-nets):
  ver `docs/roadmap-criptografico.md`.
- HSM físico certificado (FIPS 140-2) para las claves.
- Réplicas de BD, redundancia geográfica e infraestructura en nube soberana.
- Auditoría de seguridad independiente y pruebas de penetración.
- Marco legal habilitante y contingencia en papel.

El voto digital es uno de los problemas más difíciles de la ingeniería de software,
no por la tecnología sino por la tensión entre secreto del voto y verificabilidad.
Varios países técnicamente capaces decidieron **no** desplegarlo. Trata este
prototipo como herramienta de aprendizaje, no como un sistema listo para producción.
