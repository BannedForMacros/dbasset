-- =====================================================================
--  DBAsset - Superadministrador (administración de empresas y usuarios)
--  Idempotente y solo ADITIVA. Se ejecuta al arrancar junto con migracion_rfid.sql.
--  El usuario superadmin NO se crea aquí (no se guardan claves en el repositorio):
--      UPDATE dbasset.usuario SET es_superadmin = TRUE WHERE nombre_usuario = '...';
-- =====================================================================

ALTER TABLE dbasset.usuario ADD COLUMN IF NOT EXISTS es_superadmin BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE dbasset.usuario ADD COLUMN IF NOT EXISTS activo        BOOLEAN NOT NULL DEFAULT TRUE;
