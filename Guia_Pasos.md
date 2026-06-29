# Guía de ejecución — Sistema de Votación Digital Nacional (prototipo)

Guía paso a paso para **levantar el proyecto en Windows** desde cero. Si sigues
el orden, en ~30 minutos lo tienes corriendo en el navegador.

> Stack: Jakarta EE 10 · WildFly · MySQL 8 · JSF/PrimeFaces · JMS · Maven (vía IntelliJ).

---

## 0. Requisitos previos

Instala/descarga esto antes de empezar:

| Componente | Cómo conseguirlo | Verificar |
|---|---|---|
| **JDK 17 o 21** | Eclipse Temurin / Oracle | (lo usa IntelliJ) |
| **IntelliJ IDEA** | jetbrains.com (Community sirve) | — |
| **MySQL 8** | dev.mysql.com | `mysql --version` |
| **WildFly 31+** | wildfly.org → "Jakarta EE Full & Web Distribution" (.zip) | descomprimir |
| **Driver MySQL** | `mysql-connector-j-8.4.0.jar` de Maven Central | — |

Descomprime WildFly en una ruta **corta y sin espacios**, por ejemplo `C:\wildfly`.
Descomprime el proyecto donde quieras, por ejemplo `D:\Proyectos\votacion`.

> **Nota Windows:** los scripts de WildFly terminan en `.bat` (no `.sh`), y las
> variables se escriben `%VAR%` (no `$VAR`).

---

## 1. Base de datos (MySQL)

Abre una terminal (CMD) y crea el esquema. **Ajusta la ruta** a donde tengas el proyecto:

```cmd
mysql -u root -p < RUTA\votacion-parent\votacion-persistence\src\main\resources\db\migration\V1__esquema_inicial.sql
mysql -u root -p < RUTA\votacion-parent\votacion-persistence\src\main\resources\db\migration\V2__credencial_votante.sql
```

Luego entra a MySQL y crea el usuario de la aplicación:

```cmd
mysql -u root -p
```
```sql
CREATE USER 'votacion_app'@'localhost' IDENTIFIED BY 'clave123';
GRANT SELECT, INSERT, UPDATE ON votacion_nacional.* TO 'votacion_app'@'localhost';
FLUSH PRIVILEGES;
exit;
```

---

## 2. Configurar WildFly (driver, datasource y cola JMS)

### 2.1 Define la ruta de WildFly

En **cada ventana nueva** de CMD que abras, lo primero es definir esta variable
(la variable se borra al cerrar la ventana):

```cmd
set WILDFLY_HOME=C:\wildfly
```

Verifica que la ruta es correcta (debe mostrar el archivo):

```cmd
dir "%WILDFLY_HOME%\bin\standalone.bat"
```

### 2.2 Registrar el driver MySQL (WildFly apagado)

Ajusta la ruta al `.jar` que descargaste:

```cmd
"%WILDFLY_HOME%\bin\jboss-cli.bat" --command="module add --name=com.mysql --resources=C:\drivers\mysql-connector-j-8.4.0.jar --dependencies=jakarta.transaction.api,java.sql"
```

### 2.3 Arrancar WildFly (¡con perfil FULL!)

```cmd
"%WILDFLY_HOME%\bin\standalone.bat" -c standalone-full.xml -Dvotacion.rut.pepper=MI_SECRETO_LARGO_FIJO
```

- `-c standalone-full.xml` es **obligatorio**: es el perfil que trae la cola JMS.
- El `pepper` debe ser **el mismo en todos los arranques** (ver Solución de problemas).
- **Deja esta ventana abierta**: WildFly corre aquí. Espera a ver `started in ...ms`.

### 2.4 Registrar datasource y cola (una sola vez)

Abre **otra** ventana de CMD, define de nuevo `WILDFLY_HOME` y conéctate al CLI:

```cmd
set WILDFLY_HOME=C:\wildfly
"%WILDFLY_HOME%\bin\jboss-cli.bat" --connect
```

En el prompt `[standalone@localhost:9990 /]`, pega estas tres líneas, una por una:

```
/subsystem=datasources/jdbc-driver=mysql:add(driver-name=mysql,driver-module-name=com.mysql,driver-class-name=com.mysql.cj.jdbc.Driver)
```
```
data-source add --name=VotacionDS --jndi-name=java:/jdbc/VotacionDS --driver-name=mysql --connection-url="jdbc:mysql://localhost:3306/votacion_nacional?serverTimezone=America/Santiago&rewriteBatchedStatements=true" --user-name=votacion_app --password=clave123 --min-pool-size=10 --max-pool-size=50 --blocking-timeout-wait-millis=5000
```
```
jms-queue add --queue-address=ColaVotos --entries=[java:/jms/cola/votos]
```

Sal con `quit`. Esta configuración **queda guardada**: el paso 2.4 solo se hace una vez.

> Para confirmar que arrancó bien, en el log de WildFly debes ver:
> `Bound data source [java:/jdbc/VotacionDS]` y `Server configuration file in use: standalone-full.xml`.

---

## 3. Compilar el proyecto (en IntelliJ, sin terminal)

Este proyecto **no incluye Maven Wrapper**, así que `mvn` / `mvnw` no funcionan en
la terminal a menos que instales Maven. Lo más simple es compilar **dentro de
IntelliJ**, que trae Maven incorporado.

1. `File > Open` → selecciona la carpeta **`votacion-parent`** (la que tiene `pom.xml`).
2. `File > Project Structure > Project`:
    - **SDK**: elige tu JDK 17 o 21. Si dice `<No SDK>`, usa `Add SDK > JDK...`
      o `Download JDK`.
    - **Language level**: ponlo en `17` o `21` (no dejes un número futuro como 26).
3. Abre el panel **Maven** (borde derecho; o `View > Tool Windows > Maven`).
4. Despliega `votacion-parent > Lifecycle` y haz **doble clic en `package`**.
5. Espera **`BUILD SUCCESS`**. El resultado queda en:
   ```
   votacion-parent\votacion-web\target\votacion.war
   ```

---

## 4. Desplegar en WildFly

Con WildFly corriendo (paso 2.3), copia el WAR a la carpeta de despliegues. Lo más
fácil es con el **Explorador de Windows**:

1. Copia `votacion-parent\votacion-web\target\votacion.war`
2. Pégalo en `C:\wildfly\standalone\deployments\`

Mira la ventana de WildFly. Debe aparecer:

```
Deployed "votacion.war"
```

Se crea el marcador `votacion.war.deployed` (éxito) o `votacion.war.failed` (error).
El **seeder** carga automáticamente una elección, candidatos y votantes de prueba
si la base está vacía.

---

## 5. Probar

Abre en el navegador:

```
http://localhost:8080/votacion/login.xhtml
```

Ingresa con un votante de prueba y vota:

| RUT | Clave |
|---|---|
| `11111111-1` | `1234` |
| `22222222-2` | `1234` |
| `33333333-3` | `1234` |

Para verificar en MySQL:

```sql
USE votacion_nacional;
SELECT id, candidato_id, mesa_id, hash_actual FROM voto;        -- voto anónimo
SELECT tipo_evento, rut_hash, token_ref FROM auditoria_evento;  -- ningún registro cruza ambos
SELECT rut_dv, estado_voto FROM votante;                        -- quedó VOTANDO/VOTADO
```

Si reingresas con el mismo RUT, el sistema rechaza por doble voto.

---

## Arranque rápido (sesiones siguientes)

Una vez configurado todo (pasos 1, 2.2 y 2.4 ya hechos), para volver a usarlo:

```cmd
set WILDFLY_HOME=C:\wildfly
"%WILDFLY_HOME%\bin\standalone.bat" -c standalone-full.xml -Dvotacion.rut.pepper=MI_SECRETO_LARGO_FIJO
```

El WAR ya desplegado se carga solo al arrancar.

---

## Solución de problemas

Errores reales que aparecen con frecuencia y su causa:

**`'$WILDFLY_HOME' no se reconoce` / `El sistema no puede encontrar la ruta`**
Estás en Windows: usa `%WILDFLY_HOME%` (no `$WILDFLY_HOME`) y `.bat` (no `.sh`).
Y recuerda ejecutar `set WILDFLY_HOME=...` en **cada ventana nueva**.

**`echo %WILDFLY_HOME%` imprime `%WILDFLY_HOME%` literal**
No definiste la variable en esa ventana. Ejecuta `set WILDFLY_HOME=C:\wildfly`.

**`dir "%WILDFLY_HOME%\bin\standalone.bat"` no encuentra el archivo**
La ruta de WildFly está mal. Revisa el nombre exacto de la carpeta (la versión
puede diferir, p. ej. `wildfly-41.0.0.Beta1`) o si quedó descomprimida "doble"
(una carpeta dentro de otra con el mismo nombre).

**`'mvn' no se reconoce`**
No tienes Maven instalado en la terminal. Compila desde IntelliJ (paso 3); no
necesitas instalar Maven aparte.

**`mvnw` no se reconoce**
Este proyecto no trae Maven Wrapper. Usa el panel Maven de IntelliJ.

**`<No SDK>` (en rojo) en Project Structure**
Asigna tu JDK 17/21 en `File > Project Structure > Project > SDK`. Sin SDK no compila.

**`PersistenceException: Unable to build Hibernate SessionFactory` +
`Schema validation: wrong column type ... found [tinyint/smallint unsigned] but expecting [integer]`**
Hibernate en modo `validate` exige tipos exactos y el esquema usa `UNSIGNED`.
Solución: en `persistence.xml` deja
`<property name="hibernate.hbm2ddl.auto" value="none"/>` (en vez de `validate`),
**recompila** (paso 3) y **redespliega** (borra `votacion.war` y `votacion.war.failed`
de `deployments\` y copia el WAR nuevo). Si el error persiste, es que el WAR
desplegado aún trae el `persistence.xml` viejo: confirma que recompilaste.

**`ClassNotFound` / entidades no encontradas en el persistence unit**
Las clases en `persistence.xml` deben apuntar a
`cl.gob.votacion.domain.entity.*` (no `persistence.entity`).

**El login falla aunque el RUT y la clave sean correctos**
Cambiaste el `pepper` entre el sembrado de datos y el arranque actual. Los
`rut_hash` se calculan con el pepper; si cambia, dejan de coincidir. Usa siempre
el mismo `-Dvotacion.rut.pepper=...`, o borra los datos y deja que el seeder los
recree con el pepper nuevo (`DELETE FROM votante; DELETE FROM candidato; DELETE FROM eleccion;`
y reinicia WildFly).

**No existe la cola JMS / errores de mensajería al votar**
Arrancaste con `standalone.xml` en vez de `standalone-full.xml`. La mensajería
JMS solo está en el perfil **full**.

**`404` al abrir la app**
Verifica que el log diga `Deployed "votacion.war"` y usa la URL exacta:
`http://localhost:8080/votacion/login.xhtml`.

---

## Notas para producción (no para el piloto)

Este prototipo demuestra disponibilidad, escalabilidad y consistencia, con
anonimato a nivel de diseño de base de datos. Para uso real se requiere, además:
anonimato criptográfico E2E (firmas ciegas, mix-nets), HSM físico, réplicas de BD
y nube soberana, auditoría de seguridad independiente, y marco legal habilitante.
Ver `docs/roadmap-criptografico.md`. El modo `hbm2ddl=none` se usa por comodidad
en el piloto; en producción se mantiene `validate` con un esquema perfectamente
alineado.