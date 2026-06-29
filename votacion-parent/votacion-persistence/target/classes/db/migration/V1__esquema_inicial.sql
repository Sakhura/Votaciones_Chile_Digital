-- =====================================================================
--  Sistema de Votacion Digital Nacional - Esquema inicial
--  Motor: MySQL 8.x  |  Engine: InnoDB  |  Charset: utf8mb4
--  Ruta sugerida en el proyecto:
--     src/main/resources/db/migration/V1__esquema_inicial.sql
--  (convencion de nombres Flyway para migraciones versionadas)
-- =====================================================================
--  PRINCIPIO DE ANONIMATO:
--   - La tabla `votante` sabe QUIEN ya voto, pero NO que voto.
--   - La tabla `voto` sabe QUE se voto, pero NO quien.
--   - NO existe clave foranea entre `votante` y `voto`.
--   - El vinculo entre persona y voto esta cortado por diseno.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS votacion_nacional
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE votacion_nacional;

-- Nivel de aislamiento recomendado para esta carga:
-- READ COMMITTED reduce gap-locks frente al REPEATABLE READ por defecto,
-- bajando la contencion bajo escritura masiva sin sacrificar correctitud.
SET GLOBAL transaction_isolation = 'READ-COMMITTED';


-- ---------------------------------------------------------------------
-- 1) ELECCION  -- evento electoral (presidencial, plebiscito, etc.)
-- ---------------------------------------------------------------------
CREATE TABLE eleccion (
    id            INT UNSIGNED   NOT NULL AUTO_INCREMENT,
    nombre        VARCHAR(200)   NOT NULL,
    tipo          ENUM('PRESIDENCIAL','PARLAMENTARIA','MUNICIPAL','PLEBISCITO')
                                 NOT NULL,
    fecha_inicio  DATETIME       NOT NULL,
    fecha_fin     DATETIME       NOT NULL,
    estado        ENUM('PREPARACION','ABIERTA','CERRADA','ESCRUTADA')
                                 NOT NULL DEFAULT 'PREPARACION',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ---------------------------------------------------------------------
-- 2) CANDIDATO  -- opciones de una eleccion
-- ---------------------------------------------------------------------
CREATE TABLE candidato (
    id              INT UNSIGNED     NOT NULL AUTO_INCREMENT,
    eleccion_id     INT UNSIGNED     NOT NULL,
    nombre          VARCHAR(150)     NOT NULL,
    partido         VARCHAR(150)     NULL,
    numero_papeleta SMALLINT UNSIGNED NOT NULL,
    activo          BOOLEAN          NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_papeleta (eleccion_id, numero_papeleta),
    CONSTRAINT fk_cand_eleccion
        FOREIGN KEY (eleccion_id) REFERENCES eleccion(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ---------------------------------------------------------------------
-- 3) VOTANTE  -- el padron electoral (~15M filas)
--    Guarda el RUT como HASH (SHA-256), nunca en texto plano.
--    En autenticacion se hashea el RUT entrante y se busca por hash.
--
--    Particionado por KEY(rut_hash): distribuye el padron en 16
--    particiones y enruta la consulta de autenticacion (la ruta caliente)
--    a una sola particion. NOTA MySQL: toda clave unica debe contener la
--    columna de particion -> por eso rut_hash es la PK natural.
-- ---------------------------------------------------------------------
CREATE TABLE votante (
    rut_hash            CHAR(64)         NOT NULL,   -- SHA-256 del RUT
    rut_dv              CHAR(1)          NOT NULL,   -- digito verificador
    nombre              VARCHAR(150)     NOT NULL,
    region_id           TINYINT UNSIGNED NOT NULL,
    comuna_id           SMALLINT UNSIGNED NOT NULL,
    mesa_id             INT UNSIGNED     NOT NULL,
    estado_voto         ENUM('HABILITADO','VOTANDO','VOTADO','BLOQUEADO')
                                         NOT NULL DEFAULT 'HABILITADO',
    fecha_habilitacion  DATETIME(3)      NULL,       -- inicio de la sesion de voto
    fecha_marca_voto    DATETIME(3)      NULL,       -- cuando se confirmo que voto
    intentos_fallidos   TINYINT UNSIGNED NOT NULL DEFAULT 0,
    version             INT              NOT NULL DEFAULT 0, -- bloqueo optimista (JPA)
    PRIMARY KEY (rut_hash),
    KEY idx_estado (estado_voto),
    KEY idx_mesa (mesa_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  PARTITION BY KEY(rut_hash) PARTITIONS 16;


-- ---------------------------------------------------------------------
-- 4) VOTO  -- el voto anonimo (escrutinio a nivel de mesa)
--    Sin FK a `votante`. Se identifica por un token de un solo uso.
--    Guarda region_id / comuna_id / mesa_id para escrutinio por mesa,
--    igual que el conteo fisico chileno.
--
--    DOS SALVAGUARDAS DE ANONIMATO (criticas):
--    (A) `fecha_hora` es el instante de PERSISTENCIA (cuando el consumidor
--        JMS escribe el voto), NO el instante en que la persona voto. La
--        cola asincrona (punto 5) reordena los votos, rompiendo la
--        correlacion temporal entre "fulano se autentico a las 10:03:11"
--        y "se inserto un voto a las 10:03:11". Sin esto, en una mesa
--        pequena se podria inferir el voto por coincidencia de tiempos.
--    (B) k-ANONIMATO: el escrutinio por mesa solo se publica si la mesa
--        supero un umbral minimo de votos (ej. k>=30). Mesas con muy
--        pocos votos se agregan a nivel de comuna. Se aplica en reportes.
--
--    Encadenamiento de hash (hash_anterior -> hash_actual) crea un
--    "ledger" a prueba de manipulacion: alterar un voto rompe la cadena.
-- ---------------------------------------------------------------------
CREATE TABLE voto (
    id             BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    token_anonimo  CHAR(64)         NOT NULL,  -- token de un solo uso (hash)
    candidato_id   INT UNSIGNED     NOT NULL,
    eleccion_id    INT UNSIGNED     NOT NULL,
    region_id      TINYINT UNSIGNED NOT NULL,
    comuna_id      SMALLINT UNSIGNED NOT NULL,
    mesa_id        INT UNSIGNED     NOT NULL,
    fecha_hora     DATETIME(3)      NOT NULL,  -- instante de PERSISTENCIA (salvaguarda A)
    secuencia      BIGINT UNSIGNED  NOT NULL,  -- orden en la cadena
    hash_anterior  CHAR(64)         NOT NULL,
    hash_actual    CHAR(64)         NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_token (token_anonimo),  -- defensa extra contra doble voto
    UNIQUE KEY uk_hash (hash_actual),
    KEY idx_conteo (eleccion_id, candidato_id),
    KEY idx_region (eleccion_id, region_id),
    KEY idx_mesa (eleccion_id, mesa_id, candidato_id),
    CONSTRAINT fk_voto_candidato
        FOREIGN KEY (candidato_id) REFERENCES candidato(id),
    CONSTRAINT fk_voto_eleccion
        FOREIGN KEY (eleccion_id) REFERENCES eleccion(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ---------------------------------------------------------------------
-- 5) AUDITORIA_EVENTO  -- trazabilidad del PROCESO (no del voto)
--    Regla de oro de anonimato:
--      - Eventos de persona (AUTENTICACION, TOKEN_EMITIDO) llevan rut_hash
--        pero NO el token emitido.
--      - Eventos de voto (VOTO_PERSISTIDO) llevan token_ref pero NO rut_hash.
--    Asi, ninguna fila permite unir persona <-> candidato.
-- ---------------------------------------------------------------------
CREATE TABLE auditoria_evento (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    tipo_evento ENUM('AUTENTICACION','TOKEN_EMITIDO','VOTO_ENCOLADO',
                     'VOTO_PERSISTIDO','RECHAZO_DOBLE_VOTO','ERROR')
                            NOT NULL,
    rut_hash    CHAR(64)    NULL,   -- solo en eventos de persona
    token_ref   CHAR(64)    NULL,   -- solo en eventos de voto
    ip_origen   VARBINARY(16) NULL, -- IPv4/IPv6 binario
    detalle     VARCHAR(255) NULL,
    fecha_hora  DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_tipo_fecha (tipo_evento, fecha_hora),
    KEY idx_rut (rut_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ---------------------------------------------------------------------
-- 6) Vista de escrutinio (conteo)
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW v_conteo AS
SELECT
    e.id             AS eleccion_id,
    c.id             AS candidato_id,
    c.nombre         AS candidato,
    c.partido        AS partido,
    COUNT(v.id)      AS total_votos
FROM candidato c
JOIN eleccion  e ON e.id = c.eleccion_id
LEFT JOIN voto v ON v.candidato_id = c.id
GROUP BY e.id, c.id, c.nombre, c.partido;


-- ---------------------------------------------------------------------
-- 6b) Vista de escrutinio POR MESA con k-anonimato
--     Solo expone el desglose por mesa si la mesa supero el umbral k.
--     Mesas bajo el umbral devuelven mesa_id = NULL (se agregan aparte
--     a nivel de comuna en la capa de reportes).
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW v_conteo_mesa AS
SELECT
    v.eleccion_id,
    v.mesa_id,
    v.candidato_id,
    COUNT(*) AS total_votos
FROM voto v
WHERE v.mesa_id IN (
        SELECT mesa_id
        FROM voto
        GROUP BY eleccion_id, mesa_id
        HAVING COUNT(*) >= 30      -- umbral k de anonimato
      )
GROUP BY v.eleccion_id, v.mesa_id, v.candidato_id;


-- =====================================================================
--  PATRON ANTI-DOBLE-VOTO (referencia para la capa de aplicacion)
--  Esta es la operacion clave que debe ejecutar el servicio de voto.
--  Es un UPDATE condicional ATOMICO: el WHERE incluye el estado esperado.
--
--    UPDATE votante
--       SET estado_voto = 'VOTANDO',
--           fecha_habilitacion = NOW(3),
--           version = version + 1
--     WHERE rut_hash = ?            -- hash del RUT autenticado
--       AND estado_voto = 'HABILITADO';
--
--  - Si filas_afectadas = 1  -> se gano el "lock logico": proceder a votar.
--  - Si filas_afectadas = 0  -> ya estaba VOTANDO/VOTADO: RECHAZAR.
--
--  Dos peticiones simultaneas del mismo RUT compiten por ese UPDATE;
--  InnoDB serializa el acceso a la fila y solo UNA obtiene el 1.
--  Esto evita el doble voto sin SELECT-then-UPDATE (que tiene condicion
--  de carrera). Al confirmarse el voto, un segundo UPDATE pone 'VOTADO'.
-- =====================================================================
