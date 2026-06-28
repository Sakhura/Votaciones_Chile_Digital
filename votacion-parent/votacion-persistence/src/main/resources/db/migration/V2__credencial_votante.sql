-- =====================================================================
--  V2 - Credencial de autenticacion del votante
--  Ruta: src/main/resources/db/migration/V2__credencial_votante.sql
--
--  Guarda la credencial como hash PBKDF2 (formato iter:salt:hash),
--  NUNCA en texto plano. Nullable porque el padron se carga primero y
--  las credenciales se asignan/activan despues.
-- =====================================================================
USE votacion_nacional;

ALTER TABLE votante
    ADD COLUMN credencial_hash VARCHAR(255) NULL AFTER mesa_id;
