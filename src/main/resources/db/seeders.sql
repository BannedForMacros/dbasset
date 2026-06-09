-- =====================================================================
--  DBAsset - Datos iniciales (SEEDERS) para empezar a probar
--  Ejecutar DESPUES de schema_postgres17.sql
--      \c dbasset
--      \i seeders.sql
--
--  IMPORTANTE: las contraseñas van en TEXTO PLANO (NoOpPasswordEncoder).
--  Credenciales de prueba:
--      Admin web      -> usuario: admin   clave: 12345
--      Inventariador  -> usuario: inv1    clave: 123456
-- =====================================================================

SET search_path TO dbasset;

-- ---------------------------------------------------------------------
-- 1. EMPRESAS (2 para probar el selector multi-tenant del frontend)
-- ---------------------------------------------------------------------
INSERT INTO dbasset.empresa (cod_empresa, ruc, razon_social, activo) VALUES
    (1, '20100000001', 'EMPRESA DEMO UNO S.A.C.', TRUE),
    (2, '20200000002', 'EMPRESA DEMO DOS S.A.C.', TRUE)
ON CONFLICT (cod_empresa) DO NOTHING;

-- ---------------------------------------------------------------------
-- 2. USUARIO ADMIN (login web)
-- ---------------------------------------------------------------------
INSERT INTO dbasset.usuario (cod_usuario, nombre_usuario, clave, nombre_completo, descripcion, tipo_usu) VALUES
    (1, 'admin', '12345', 'Administrador General', 'Usuario administrador de pruebas', 1)
ON CONFLICT (cod_usuario) DO NOTHING;

-- ---------------------------------------------------------------------
-- 3. ASIGNAR EL ADMIN A LAS 2 EMPRESAS (esto alimenta el login -> "empresas")
-- ---------------------------------------------------------------------
INSERT INTO dbasset.usuario_empresa (cod_usuario, cod_empresa, rol_en_empresa, activo) VALUES
    (1, 1, 'ADMIN', TRUE),
    (1, 2, 'ADMIN', TRUE)
ON CONFLICT (cod_usuario, cod_empresa) DO NOTHING;

-- ---------------------------------------------------------------------
-- 4. INVENTARIADOR DE PRUEBA (login app móvil, pertenece a empresa 1)
-- ---------------------------------------------------------------------
INSERT INTO dbasset.inventariador
    (cod_inventariador, nombre_inventariador, dni, telefono, email, cod_interno, usuario, clave, cod_empresa, activo) VALUES
    (1, 'Juan Pérez (Inventariador)', '40000000', '999111222', 'inv1@demo.com', 'INV-001', 'inv1', '123456', 1, TRUE)
ON CONFLICT (cod_inventariador) DO NOTHING;

-- ---------------------------------------------------------------------
-- 5. CATALOGOS BASICOS (colores y marcas para pruebas)
--    (los estados ya se sembraron en schema_postgres17.sql)
-- ---------------------------------------------------------------------
INSERT INTO dbasset.color (cod_color, nombre_color) VALUES
    (1, 'NEGRO'),
    (2, 'BLANCO'),
    (3, 'GRIS'),
    (4, 'AZUL')
ON CONFLICT (cod_color) DO NOTHING;

INSERT INTO dbasset.marca (cod_marca, nombre_marca) VALUES
    (1, 'HP'),
    (2, 'DELL'),
    (3, 'LENOVO'),
    (4, 'SAMSUNG')
ON CONFLICT (cod_marca) DO NOTHING;

-- ---------------------------------------------------------------------
-- 6. UBICACIONES DE EJEMPLO PARA LA EMPRESA 1 (Local -> Área -> Oficina)
--    Útiles para la "ubicación única" del wizard de cargas (Escenario 2).
-- ---------------------------------------------------------------------
INSERT INTO dbasset.local (cod_local, cod_empresa, local, direccion, cod_interno, activo) VALUES
    (1, 1, 'Sede Central', 'Av. Principal 123', 'LOC-001', TRUE)
ON CONFLICT (cod_local) DO NOTHING;

INSERT INTO dbasset.area (cod_area, cod_empresa, area, observacion, cod_interno, cod_local, activo) VALUES
    (1, 1, 'Administración', 'Área administrativa', 'ARE-001', 1, TRUE)
ON CONFLICT (cod_area) DO NOTHING;

INSERT INTO dbasset.oficina (cod_oficina, cod_empresa, oficina, observacion, cod_interno, cod_local, cod_area, activo) VALUES
    (1, 1, 'Oficina de Contabilidad', 'Piso 2', 'OFI-001', 1, 1, TRUE)
ON CONFLICT (cod_oficina) DO NOTHING;

-- ---------------------------------------------------------------------
-- 7. RESPONSABLE DE EJEMPLO (empresa 1) y su vínculo con la oficina
-- ---------------------------------------------------------------------
INSERT INTO dbasset.responsable (cod_responsable, responsable, cargo, cod_interno, cod_empresa, activo) VALUES
    (1, 'María Gómez', 'Contadora', 'RES-001', 1, TRUE)
ON CONFLICT (cod_responsable) DO NOTHING;

INSERT INTO dbasset.responsable_oficina (cod_responsable, cod_oficina) VALUES
    (1, 1)
ON CONFLICT (cod_responsable, cod_oficina) DO NOTHING;

-- =====================================================================
--  REAJUSTE DE SECUENCIAS DE IDENTIDAD
--  (necesario porque insertamos IDs explícitos; evita choques al crear
--   nuevos registros desde la app)
-- =====================================================================
SELECT setval(pg_get_serial_sequence('dbasset.empresa',       'cod_empresa'),       (SELECT COALESCE(MAX(cod_empresa),       1) FROM dbasset.empresa));
SELECT setval(pg_get_serial_sequence('dbasset.usuario',       'cod_usuario'),       (SELECT COALESCE(MAX(cod_usuario),       1) FROM dbasset.usuario));
SELECT setval(pg_get_serial_sequence('dbasset.inventariador', 'cod_inventariador'), (SELECT COALESCE(MAX(cod_inventariador), 1) FROM dbasset.inventariador));
SELECT setval(pg_get_serial_sequence('dbasset.color',         'cod_color'),         (SELECT COALESCE(MAX(cod_color),         1) FROM dbasset.color));
SELECT setval(pg_get_serial_sequence('dbasset.marca',         'cod_marca'),         (SELECT COALESCE(MAX(cod_marca),         1) FROM dbasset.marca));
SELECT setval(pg_get_serial_sequence('dbasset.local',         'cod_local'),         (SELECT COALESCE(MAX(cod_local),         1) FROM dbasset.local));
SELECT setval(pg_get_serial_sequence('dbasset.area',          'cod_area'),          (SELECT COALESCE(MAX(cod_area),          1) FROM dbasset.area));
SELECT setval(pg_get_serial_sequence('dbasset.oficina',       'cod_oficina'),       (SELECT COALESCE(MAX(cod_oficina),       1) FROM dbasset.oficina));
SELECT setval(pg_get_serial_sequence('dbasset.responsable',   'cod_responsable'),   (SELECT COALESCE(MAX(cod_responsable),   1) FROM dbasset.responsable));

-- ---------------------------------------------------------------------
-- 8. CONFIGURACION DE CAMPOS DEL ACTIVO (para el paso "match" del wizard)
--    Sin esto, el wizard NO muestra campos del activo para mapear.
--    nombre_campo_bd debe coincidir EXACTO (snake_case) con los 'case'
--    de CargaService.asignarValorAlActivo(). Aplica a todas las empresas.
-- ---------------------------------------------------------------------
INSERT INTO dbasset.configuracion_campo
    (cod_empresa, nombre_campo_bd, etiqueta_usuario, es_visible, es_obligatorio, orden)
SELECT e.cod_empresa, c.nombre_campo_bd, c.etiqueta_usuario, c.es_visible, c.es_obligatorio, c.orden
FROM (VALUES
    ('cod_activo',   'Código del Activo',  TRUE, TRUE,  1),
    ('cod_interno',  'Código Interno',     TRUE, FALSE, 2),
    ('descripcion',  'Descripción',        TRUE, FALSE, 3),
    ('marca',        'Marca',              TRUE, FALSE, 4),
    ('modelo',       'Modelo',             TRUE, FALSE, 5),
    ('serie',        'Número de Serie',    TRUE, FALSE, 6),
    ('color',        'Color',              TRUE, FALSE, 7),
    ('anio',         'Año',                TRUE, FALSE, 8),
    ('fecha_compra', 'Fecha de Compra',    TRUE, FALSE, 9),
    ('tipo',         'Tipo',               TRUE, FALSE, 10),
    ('dimensiones',  'Dimensiones',        TRUE, FALSE, 11),
    ('n_motor',      'Número de Motor',    TRUE, FALSE, 12),
    ('n_chasis',     'Número de Chasis',   TRUE, FALSE, 13),
    ('placa',        'Placa',              TRUE, FALSE, 14),
    ('obs',          'Observaciones',      TRUE, FALSE, 15)
) AS c(nombre_campo_bd, etiqueta_usuario, es_visible, es_obligatorio, orden)
CROSS JOIN (SELECT cod_empresa FROM dbasset.empresa) e
WHERE NOT EXISTS (
    SELECT 1 FROM dbasset.configuracion_campo cc
    WHERE cc.cod_empresa = e.cod_empresa
      AND cc.nombre_campo_bd = c.nombre_campo_bd
);

SELECT setval(pg_get_serial_sequence('dbasset.configuracion_campo', 'id'),
              (SELECT COALESCE(MAX(id), 1) FROM dbasset.configuracion_campo));
