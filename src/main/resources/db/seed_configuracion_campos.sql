-- =====================================================================
--  Seed de configuracion_campo (campos del activo para el wizard de cargas)
--  El paso "Campos del Activo" del wizard se llena desde esta tabla.
--  nombre_campo_bd DEBE coincidir EXACTO (snake_case) con los 'case' de
--  CargaService.asignarValorAlActivo(), o el valor del Excel se ignora.
--
--  Idempotente: se puede ejecutar varias veces sin duplicar.
--  Aplica a TODAS las empresas existentes.
-- =====================================================================

SET search_path TO dbasset;

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

-- Reajustar la secuencia de identidad por si se insertaron filas
SELECT setval(pg_get_serial_sequence('dbasset.configuracion_campo', 'id'),
              (SELECT COALESCE(MAX(id), 1) FROM dbasset.configuracion_campo));
