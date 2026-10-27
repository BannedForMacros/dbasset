-- =====================================================================
-- Datos de DEMOSTRACIÓN para "EMPRESA DEMO DOS S.A.C." (cod_empresa = 2)
--
-- Información ficticia para mostrar el sistema con contenido realista:
-- locales, áreas, oficinas, custodios, inventariadores, ~450 activos,
-- cargas en distintos estados, sesiones de inventario RFID,
-- transferencias, bajas y reubicaciones.
--
-- No se ejecuta al arrancar (no está en spring.sql.init.schema-locations).
-- Uso:  psql -U postgres -d dbasset -f seed_demo_dos.sql
-- Solo corre sobre la empresa 2 vacía; si ya tiene datos, se detiene.
-- =====================================================================

BEGIN;
SET search_path TO dbasset;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM dbasset.local WHERE cod_empresa = 2)
     OR EXISTS (SELECT 1 FROM dbasset.m_activos WHERE cod_empresa = 2) THEN
    RAISE EXCEPTION 'La empresa 2 ya tiene datos; este script es solo para una empresa vacía';
  END IF;
END $$;

-- Las secuencias de identidad pueden ir detrás de los ids cargados a mano por otros seeders
DO $$
DECLARE r record; seq text;
BEGIN
  FOR r IN SELECT * FROM (VALUES
      ('local','cod_local'), ('area','cod_area'), ('oficina','cod_oficina'),
      ('responsable','cod_responsable'), ('inventariador','cod_inventariador'),
      ('m_activos','id'), ('cargas','cod_carga'), ('detalle_carga','id'),
      ('inventario_sesion','id'), ('inventario_esperado','id'), ('inventario_lectura','id'),
      ('transferencia','id'), ('baja_activo','id'), ('reubicacion','id'),
      ('marca','cod_marca'), ('color','cod_color')) v(t, c)
  LOOP
    seq := pg_get_serial_sequence('dbasset.' || r.t, r.c);
    EXECUTE format('SELECT setval(%L, GREATEST((SELECT COALESCE(MAX(%I), 0) FROM dbasset.%I), (SELECT last_value FROM %s), 1))',
                   seq, r.c, r.t, seq);
  END LOOP;
END $$;

-- ---------------------------------------------------------------- catálogos globales
INSERT INTO dbasset.marca (nombre_marca) VALUES
  ('LG'), ('EPSON'), ('CANON'), ('CISCO'), ('FORTINET'), ('UBIQUITI'), ('APC'), ('ZEBRA'),
  ('TOYOTA'), ('HYUNDAI'), ('ISUZU'), ('HIKVISION'), ('BOSCH'), ('GRANDSTREAM'), ('YEALINK'),
  ('MIDEA'), ('APPLE'), ('LINCOLN'), ('KARCHER'), ('OFIMUEBLES'), ('ERGOSIT'), ('METALPROTEC'),
  ('CAMPBELL'), ('JUNGHEINRICH'), ('OHAUS')
ON CONFLICT (nombre_marca) DO NOTHING;

INSERT INTO dbasset.color (nombre_color) VALUES
  ('PLATA'), ('ROJO'), ('MADERA'), ('AMARILLO'), ('VERDE'), ('NARANJA')
ON CONFLICT (nombre_color) DO NOTHING;

-- ---------------------------------------------------------------- estructura organizacional
INSERT INTO dbasset.local (cod_empresa, local, direccion, cod_interno) VALUES
  (2, 'Sede Central San Isidro',  'Av. Javier Prado Este 4200, San Isidro, Lima',     'LOC-001'),
  (2, 'Almacén Central Lurín',    'Antigua Panamericana Sur Km 36.5, Lurín, Lima',     'LOC-002'),
  (2, 'Sucursal Arequipa',        'Av. Ejército 710, Yanahuara, Arequipa',             'LOC-003'),
  (2, 'Sucursal Trujillo',        'Av. América Norte 1250, Trujillo, La Libertad',     'LOC-004');

INSERT INTO dbasset.area (cod_empresa, area, observacion, cod_interno, cod_local)
SELECT 2, v.area, v.obs, v.cod, l.cod_local
FROM (VALUES
  ('Gerencia General',              'Dirección y gerencias de línea',        'ARE-001', 'LOC-001'),
  ('Administración y Finanzas',     'Contabilidad, tesorería y compras',     'ARE-002', 'LOC-001'),
  ('Recursos Humanos',              'Gestión del talento y planillas',       'ARE-003', 'LOC-001'),
  ('Tecnología de la Información',  'Infraestructura, soporte y sistemas',   'ARE-004', 'LOC-001'),
  ('Comercial y Ventas',            'Ventas corporativas y marketing',       'ARE-005', 'LOC-001'),
  ('Legal y Cumplimiento',          NULL,                                    'ARE-006', 'LOC-001'),
  ('Operaciones de Almacén',        'Recepción, almacenaje y picking',       'ARE-007', 'LOC-002'),
  ('Despacho y Distribución',       'Flota propia y despachos a provincias', 'ARE-008', 'LOC-002'),
  ('Mantenimiento',                 'Taller y mantenimiento preventivo',     'ARE-009', 'LOC-002'),
  ('Seguridad Patrimonial',         'Vigilancia y control de accesos',       'ARE-010', 'LOC-002'),
  ('Ventas Región Sur',             NULL,                                    'ARE-011', 'LOC-003'),
  ('Administración Sucursal Sur',   NULL,                                    'ARE-012', 'LOC-003'),
  ('Ventas Región Norte',           NULL,                                    'ARE-013', 'LOC-004'),
  ('Atención al Cliente Norte',     'Postventa y reclamos',                  'ARE-014', 'LOC-004')
) v(area, obs, cod, loc)
JOIN dbasset.local l ON l.cod_empresa = 2 AND l.cod_interno = v.loc;

INSERT INTO dbasset.oficina (cod_empresa, oficina, observacion, cod_interno, cod_local, cod_area)
SELECT 2, v.oficina, v.obs, v.cod, a.cod_local, a.cod_area
FROM (VALUES
  ('Despacho de Gerencia General',   'Piso 12', 'OFI-001', 'ARE-001'),
  ('Sala de Directorio',             'Piso 12', 'OFI-002', 'ARE-001'),
  ('Contabilidad',                   'Piso 10', 'OFI-003', 'ARE-002'),
  ('Tesorería',                      'Piso 10', 'OFI-004', 'ARE-002'),
  ('Logística y Compras',            'Piso 9',  'OFI-005', 'ARE-002'),
  ('Selección y Capacitación',       'Piso 8',  'OFI-006', 'ARE-003'),
  ('Compensaciones y Planillas',     'Piso 8',  'OFI-007', 'ARE-003'),
  ('Data Center',                    'Sótano 1, acceso restringido', 'OFI-008', 'ARE-004'),
  ('Soporte Técnico',                'Piso 7',  'OFI-009', 'ARE-004'),
  ('Desarrollo de Sistemas',         'Piso 7',  'OFI-010', 'ARE-004'),
  ('Ventas Corporativas',            'Piso 6',  'OFI-011', 'ARE-005'),
  ('Marketing',                      'Piso 6',  'OFI-012', 'ARE-005'),
  ('Asesoría Legal',                 'Piso 11', 'OFI-013', 'ARE-006'),
  ('Recepción de Mercadería',        'Nave A',  'OFI-014', 'ARE-007'),
  ('Almacén de Producto Terminado',  'Nave B',  'OFI-015', 'ARE-007'),
  ('Control de Inventarios',         'Mezzanine Nave A', 'OFI-016', 'ARE-007'),
  ('Patio de Maniobras',             NULL,      'OFI-017', 'ARE-008'),
  ('Coordinación de Despachos',      NULL,      'OFI-018', 'ARE-008'),
  ('Taller de Mantenimiento',        NULL,      'OFI-019', 'ARE-009'),
  ('Caseta de Control',              'Ingreso principal', 'OFI-020', 'ARE-010'),
  ('Centro de Monitoreo',            NULL,      'OFI-021', 'ARE-010'),
  ('Sala de Ventas Arequipa',        NULL,      'OFI-022', 'ARE-011'),
  ('Oficina Administrativa Arequipa', NULL,     'OFI-023', 'ARE-012'),
  ('Sala de Ventas Trujillo',        NULL,      'OFI-024', 'ARE-013'),
  ('Módulo de Atención Trujillo',    NULL,      'OFI-025', 'ARE-014')
) v(oficina, obs, cod, are)
JOIN dbasset.area a ON a.cod_empresa = 2 AND a.cod_interno = v.are;

INSERT INTO dbasset.responsable (responsable, cargo, cod_interno, cod_empresa) VALUES
  ('Ricardo Alonso Villanueva Prado',  'Gerente General',                    'RES-001', 2),
  ('Ana Lucía Benavides Soto',         'Gerente de Administración y Finanzas','RES-002', 2),
  ('Patricia Elena Rojas Medina',      'Contadora General',                  'RES-003', 2),
  ('Miguel Ángel Torres Castillo',     'Jefe de Tesorería',                  'RES-004', 2),
  ('Carmen Rosa Delgado Ríos',         'Jefa de Logística',                  'RES-005', 2),
  ('Sofía Valeria Mendoza Cruz',       'Jefa de Recursos Humanos',           'RES-006', 2),
  ('Daniel Eduardo Chávez Ortiz',      'Jefe de Infraestructura TI',         'RES-007', 2),
  ('Kevin Andrés Salazar León',        'Coordinador de Soporte',             'RES-008', 2),
  ('Lucía Fernanda Paredes Gamarra',   'Jefa de Desarrollo',                 'RES-009', 2),
  ('Jorge Enrique Vargas Núñez',       'Gerente Comercial',                  'RES-010', 2),
  ('María José Herrera Campos',        'Jefa de Marketing',                  'RES-011', 2),
  ('Fernando José Ugarte Bustamante',  'Asesor Legal',                       'RES-012', 2),
  ('Hugo César Ramos Quispe',          'Jefe de Almacén',                    'RES-013', 2),
  ('Raúl Antonio Espinoza Cárdenas',   'Supervisor de Inventarios',          'RES-014', 2),
  ('Víctor Manuel Gutiérrez Silva',    'Jefe de Distribución',               'RES-015', 2),
  ('Óscar Alberto Pineda Rojas',       'Jefe de Mantenimiento',              'RES-016', 2),
  ('Walter Iván Cabrera Montes',       'Jefe de Seguridad',                  'RES-017', 2),
  ('Gabriela Isabel Zúñiga Apaza',     'Administradora Sucursal Arequipa',   'RES-018', 2),
  ('Renzo Martín Linares Ccori',       'Supervisor de Ventas Sur',           'RES-019', 2),
  ('Claudia Marisol Alvarado Reyes',   'Administradora Sucursal Trujillo',   'RES-020', 2);

INSERT INTO dbasset.responsable_oficina (cod_responsable, cod_oficina)
SELECT r.cod_responsable, o.cod_oficina
FROM (VALUES
  ('RES-001','OFI-001'), ('RES-001','OFI-002'), ('RES-003','OFI-003'), ('RES-004','OFI-004'),
  ('RES-005','OFI-005'), ('RES-006','OFI-006'), ('RES-006','OFI-007'), ('RES-007','OFI-008'),
  ('RES-008','OFI-009'), ('RES-009','OFI-010'), ('RES-010','OFI-011'), ('RES-011','OFI-012'),
  ('RES-012','OFI-013'), ('RES-013','OFI-014'), ('RES-013','OFI-015'), ('RES-014','OFI-016'),
  ('RES-015','OFI-017'), ('RES-015','OFI-018'), ('RES-016','OFI-019'), ('RES-017','OFI-020'),
  ('RES-017','OFI-021'), ('RES-019','OFI-022'), ('RES-018','OFI-023'), ('RES-020','OFI-024'),
  ('RES-020','OFI-025'), ('RES-002','OFI-003'), ('RES-002','OFI-004')
) v(res, ofi)
JOIN dbasset.responsable r ON r.cod_empresa = 2 AND r.cod_interno = v.res
JOIN dbasset.oficina o ON o.cod_empresa = 2 AND o.cod_interno = v.ofi;

INSERT INTO dbasset.inventariador (nombre_inventariador, dni, telefono, email, cod_interno, usuario, clave, cod_empresa) VALUES
  ('Luis Alberto Quispe Mamani',   '45872136', '987654321', 'lquispe@demodos.pe', 'INV-001', 'lquispe', '123456', 2),
  ('Rosa Elena Huamán Torres',     '47129385', '976512348', 'rhuaman@demodos.pe', 'INV-002', 'rhuaman', '123456', 2),
  ('Jorge Luis Ramírez Paredes',   '43918274', '965874123', 'jramirez@demodos.pe', 'INV-003', 'jramirez', '123456', 2),
  ('Karen Milagros Flores Chávez', '72451896', '954123789', 'kflores@demodos.pe', 'INV-004', 'kflores', '123456', 2);

-- ---------------------------------------------------------------- plantillas de activos
CREATE TEMP TABLE tmp_grupo_area (area text, grupo text) ON COMMIT DROP;
INSERT INTO tmp_grupo_area VALUES
  ('ARE-001','GERENCIA'), ('ARE-004','TI'), ('ARE-005','GERENCIA'),
  ('ARE-007','ALMACEN'), ('ARE-008','FLOTA'), ('ARE-009','TALLER'), ('ARE-010','SEGURIDAD');

-- modelos: 'MARCA|Modelo'; cantidad por oficina entre cant_min y cant_max
CREATE TEMP TABLE tmp_plantilla (
  grupo text, tipo text, descripcion text, categoria text, color text,
  dimensiones text, cant_min int, cant_max int, modelos text[]
) ON COMMIT DROP;

INSERT INTO tmp_plantilla VALUES
  ('OFICINA','ESCRITORIO','Escritorio de melamina en L','MOBILIARIO','MADERA','1.40 x 1.20 x 0.75 m',1,4,
     ARRAY['OFIMUEBLES|Línea Ejecutiva L-140','OFIMUEBLES|Operativo OP-120']),
  ('OFICINA','SILLA','Silla ergonómica giratoria','MOBILIARIO','NEGRO',NULL,2,5,
     ARRAY['ERGOSIT|Malla Pro 300','ERGOSIT|Ejecutiva E-500']),
  ('OFICINA','LAPTOP','Laptop','EQUIPOS DE CÓMPUTO','GRIS',NULL,1,4,
     ARRAY['HP|ProBook 450 G9','LENOVO|ThinkPad E14 Gen 4','DELL|Latitude 5440','HP|EliteBook 840 G8']),
  ('OFICINA','MONITOR','Monitor LED 24 pulgadas','EQUIPOS DE CÓMPUTO','NEGRO',NULL,1,3,
     ARRAY['LG|24MK430H','SAMSUNG|S24C310','DELL|P2422H']),
  ('OFICINA','CPU','Computadora de escritorio','EQUIPOS DE CÓMPUTO','NEGRO',NULL,0,2,
     ARRAY['DELL|OptiPlex 7010','HP|ProDesk 400 G9','LENOVO|ThinkCentre M70s']),
  ('OFICINA','IMPRESORA','Impresora multifuncional','EQUIPOS DE CÓMPUTO','NEGRO',NULL,0,1,
     ARRAY['EPSON|EcoTank L6270','HP|LaserJet Pro M428fdw','CANON|imageCLASS MF445dw']),
  ('OFICINA','TELEFONO','Teléfono IP','COMUNICACIONES','NEGRO',NULL,1,2,
     ARRAY['GRANDSTREAM|GRP2612','YEALINK|T31P']),
  ('OFICINA','ARCHIVADOR','Archivador metálico de 4 gavetas','MOBILIARIO','GRIS','0.47 x 0.62 x 1.32 m',0,2,
     ARRAY['METALPROTEC|AR-4G']),
  ('OFICINA','AIRE','Aire acondicionado split 18000 BTU','EQUIPOS ELECTRÓNICOS','BLANCO',NULL,0,1,
     ARRAY['LG|DualCool S4-Q18','MIDEA|Xtreme Save 18K','SAMSUNG|WindFree AR18']),

  ('GERENCIA','TV','Televisor Smart 65 pulgadas','EQUIPOS ELECTRÓNICOS','NEGRO',NULL,1,1,
     ARRAY['SAMSUNG|Crystal UHD CU8000','LG|OLED C3']),
  ('GERENCIA','PROYECTOR','Proyector multimedia','EQUIPOS ELECTRÓNICOS','BLANCO',NULL,1,1,
     ARRAY['EPSON|PowerLite E20','EPSON|PowerLite 119W']),
  ('GERENCIA','MESA','Mesa de reuniones para 10 personas','MOBILIARIO','MADERA','3.00 x 1.20 x 0.75 m',1,1,
     ARRAY['OFIMUEBLES|Directorio DR-300']),
  ('GERENCIA','LAPTOP','Laptop de alta gama','EQUIPOS DE CÓMPUTO','PLATA',NULL,1,2,
     ARRAY['APPLE|MacBook Pro 14 M3','DELL|XPS 13 Plus']),

  ('TI','SERVIDOR','Servidor de rack 1U','EQUIPOS DE CÓMPUTO','NEGRO',NULL,1,3,
     ARRAY['DELL|PowerEdge R650','HP|ProLiant DL360 Gen10']),
  ('TI','SWITCH','Switch administrable 48 puertos PoE','COMUNICACIONES','AZUL',NULL,1,3,
     ARRAY['CISCO|Catalyst 9200L-48P','CISCO|CBS350-48P']),
  ('TI','FIREWALL','Firewall de nueva generación','COMUNICACIONES','NEGRO',NULL,0,1,
     ARRAY['FORTINET|FortiGate 60F']),
  ('TI','UPS','UPS en línea 3000 VA','EQUIPOS ELECTRÓNICOS','NEGRO',NULL,1,2,
     ARRAY['APC|Smart-UPS SRT3000']),
  ('TI','ACCESS POINT','Punto de acceso WiFi 6','COMUNICACIONES','BLANCO',NULL,1,3,
     ARRAY['UBIQUITI|UniFi U6-Pro','UBIQUITI|UniFi U6-Lite']),

  ('ALMACEN','MONTACARGAS','Montacargas a gas 2.5 t','MAQUINARIA','NARANJA',NULL,0,2,
     ARRAY['TOYOTA|8FGU25','JUNGHEINRICH|TFG 425s']),
  ('ALMACEN','TRANSPALETA','Transpaleta manual 2.5 t','MAQUINARIA','ROJO',NULL,1,3,
     ARRAY['JUNGHEINRICH|AM 22']),
  ('ALMACEN','ESTANTERIA','Estantería industrial selectiva','MOBILIARIO','AZUL','2.70 x 1.00 x 4.50 m',2,5,
     ARRAY['METALPROTEC|Rack Selectivo RS-450']),
  ('ALMACEN','BALANZA','Balanza de plataforma 500 kg','EQUIPOS ELECTRÓNICOS','GRIS',NULL,0,1,
     ARRAY['OHAUS|Defender 3000']),
  ('ALMACEN','LECTOR RFID','Lector RFID portátil','EQUIPOS ELECTRÓNICOS','NEGRO',NULL,1,2,
     ARRAY['ZEBRA|RFD8500']),
  ('ALMACEN','TERMINAL','Terminal móvil Android','EQUIPOS ELECTRÓNICOS','NEGRO',NULL,1,3,
     ARRAY['ZEBRA|TC52','ZEBRA|TC21']),
  ('ALMACEN','IMPRESORA ETIQUETAS','Impresora industrial de etiquetas','EQUIPOS ELECTRÓNICOS','GRIS',NULL,0,1,
     ARRAY['ZEBRA|ZT411']),

  ('FLOTA','CAMIONETA','Camioneta pick-up 4x4','VEHÍCULOS','BLANCO',NULL,1,2,
     ARRAY['TOYOTA|Hilux 2.4 4x4','TOYOTA|Hilux 2.8 SRV']),
  ('FLOTA','CAMION','Camión furgón 5 t','VEHÍCULOS','BLANCO',NULL,1,2,
     ARRAY['HYUNDAI|HD78','ISUZU|NQR 75L']),
  ('FLOTA','TERMINAL','Terminal móvil Android','EQUIPOS ELECTRÓNICOS','NEGRO',NULL,1,2,
     ARRAY['ZEBRA|TC21']),

  ('TALLER','TALADRO','Taladro percutor inalámbrico','HERRAMIENTAS','AZUL',NULL,1,2,
     ARRAY['BOSCH|GSB 18V-50']),
  ('TALLER','COMPRESORA','Compresora de aire 100 L','MAQUINARIA','ROJO',NULL,1,1,
     ARRAY['CAMPBELL|HL5500']),
  ('TALLER','SOLDADORA','Máquina de soldar inverter','MAQUINARIA','ROJO',NULL,0,1,
     ARRAY['LINCOLN|Invertec 205']),
  ('TALLER','HIDROLAVADORA','Hidrolavadora industrial','MAQUINARIA','AMARILLO',NULL,0,1,
     ARRAY['KARCHER|HD 6/15 C']),

  ('SEGURIDAD','CAMARA','Cámara IP domo 4 MP','EQUIPOS ELECTRÓNICOS','BLANCO',NULL,2,6,
     ARRAY['HIKVISION|DS-2CD2143G2-I']),
  ('SEGURIDAD','NVR','Grabador NVR 16 canales','EQUIPOS ELECTRÓNICOS','NEGRO',NULL,0,1,
     ARRAY['HIKVISION|DS-7616NI-K2']),
  ('SEGURIDAD','EXTINTOR','Extintor PQS 12 kg','SEGURIDAD','ROJO',NULL,1,3,
     ARRAY['METALPROTEC|PQS-12']);

-- ---------------------------------------------------------------- activos
DO $$
DECLARE
  o record; t record;
  n int; i int; num int := 0;
  m text; v_marca text; v_modelo text;
  v_anio int; v_fecha date; v_estado int; v_r float8;
  v_placa text; v_motor text; v_chasis text; v_obs text;
BEGIN
  PERFORM setseed(0.42);

  FOR o IN
    SELECT ofi.cod_oficina, ofi.cod_area, ofi.cod_local, a.cod_interno AS area_cod,
           (SELECT ro.cod_responsable FROM dbasset.responsable_oficina ro
              JOIN dbasset.responsable r ON r.cod_responsable = ro.cod_responsable
             WHERE ro.cod_oficina = ofi.cod_oficina ORDER BY r.cod_interno DESC LIMIT 1) AS cod_resp
    FROM dbasset.oficina ofi
    JOIN dbasset.area a ON a.cod_area = ofi.cod_area
    WHERE ofi.cod_empresa = 2
    ORDER BY ofi.cod_interno
  LOOP
    FOR t IN
      SELECT p.* FROM tmp_plantilla p
      WHERE p.grupo = 'OFICINA'
         OR p.grupo IN (SELECT g.grupo FROM tmp_grupo_area g WHERE g.area = o.area_cod)
      ORDER BY p.grupo, p.tipo
    LOOP
      n := t.cant_min + floor(random() * (t.cant_max - t.cant_min + 1))::int;
      FOR i IN 1..n LOOP
        num := num + 1;
        m := t.modelos[1 + floor(random() * array_length(t.modelos, 1))::int];
        v_marca := split_part(m, '|', 1);
        v_modelo := split_part(m, '|', 2);
        v_anio := 2017 + floor(random() * 9)::int;
        v_fecha := make_date(v_anio, 1 + floor(random() * 12)::int, 1 + floor(random() * 28)::int);

        v_r := random();
        v_estado := CASE WHEN v_r < 0.70 THEN 1 WHEN v_r < 0.92 THEN 2 ELSE 3 END;
        v_obs := CASE v_estado
                   WHEN 3 THEN (ARRAY['Requiere mantenimiento correctivo','Presenta fallas intermitentes','Pendiente de evaluación técnica'])[1 + floor(random() * 3)::int]
                   WHEN 2 THEN (ARRAY['Desgaste normal por uso','Rayaduras leves en la carcasa',NULL])[1 + floor(random() * 3)::int]
                   ELSE NULL END;

        v_placa := NULL; v_motor := NULL; v_chasis := NULL;
        IF t.categoria = 'VEHÍCULOS' OR t.tipo = 'MONTACARGAS' THEN
          v_motor := upper(substr(md5(random()::text), 1, 3)) || lpad((floor(random() * 9999999))::int::text, 7, '0');
          v_chasis := 'MR0' || upper(substr(md5(random()::text), 1, 14));
        END IF;
        IF t.categoria = 'VEHÍCULOS' THEN
          v_placa := chr(65 + floor(random() * 26)::int) || chr(65 + floor(random() * 26)::int)
                     || chr(65 + floor(random() * 26)::int) || '-' || lpad((100 + floor(random() * 900))::int::text, 3, '0');
        END IF;

        INSERT INTO dbasset.m_activos (
          cod_empresa, cod_activo, cod_interno, descripcion, marca, modelo, tipo, color, cod_color,
          dimensiones, serie, n_motor, n_chasis, placa, anio, fecha_compra, obs,
          cod_local, cod_area, cod_oficina, cod_responsable, cod_estado, categoria, situacion)
        VALUES (
          2, 'AF-' || lpad(num::text, 5, '0'), 'PAT-' || lpad((1000 + num)::text, 6, '0'),
          t.descripcion || CASE WHEN t.categoria IN ('MOBILIARIO','SEGURIDAD') THEN '' ELSE ' ' || v_marca || ' ' || v_modelo END,
          v_marca, v_modelo, t.tipo, t.color,
          (SELECT c.cod_color::text FROM dbasset.color c WHERE c.nombre_color = t.color),
          t.dimensiones,
          CASE WHEN t.categoria = 'MOBILIARIO' THEN NULL
               ELSE upper(substr(v_marca, 1, 2)) || upper(substr(md5(random()::text), 1, 8)) END,
          v_motor, v_chasis, v_placa, v_anio::text, to_char(v_fecha, 'DD/MM/YYYY'), v_obs,
          o.cod_local, o.cod_area, o.cod_oficina, o.cod_resp, v_estado, t.categoria, 'ACTIVO');
      END LOOP;
    END LOOP;
  END LOOP;

  -- Tags RFID en ~75% de los activos (EPC-96: 24 caracteres hex)
  UPDATE dbasset.m_activos
     SET epc = 'E28011700000' || lpad(upper(to_hex(id)), 12, '0')
   WHERE cod_empresa = 2 AND random() < 0.75;
END $$;

-- ---------------------------------------------------------------- cargas de inventario
INSERT INTO dbasset.cargas (cod_empresa, descripcion, estado, fecha, created_at) VALUES
  (2, 'Inventario anual 2025 - Sede Central San Isidro',          'T', '15/12/2025', '2025-12-15 08:00'),
  (2, 'Inventario semestral 2026 - Almacén Central Lurín',        'A', '06/10/2026', '2026-10-06 07:30'),
  (2, 'Toma de inventario 2026 - Sucursales Arequipa y Trujillo', 'A', '20/10/2026', '2026-10-20 09:00'),
  (2, 'Carga de activos nuevos - Octubre 2026',                   'C', '26/10/2026', '2026-10-26 16:10');

DO $$
DECLARE
  c record; a record;
  invs int[];
  k int; total int; pct float8; dia date;
BEGIN
  SELECT array_agg(cod_inventariador ORDER BY cod_interno) INTO invs
  FROM dbasset.inventariador WHERE cod_empresa = 2;

  FOR c IN
    SELECT cg.cod_carga, cg.descripcion,
           CASE WHEN cg.descripcion LIKE '%Sede Central%' THEN ARRAY['LOC-001']
                WHEN cg.descripcion LIKE '%Lurín%'        THEN ARRAY['LOC-002']
                ELSE ARRAY['LOC-003','LOC-004'] END AS locales,
           CASE WHEN cg.estado = 'T' THEN 0.97
                WHEN cg.descripcion LIKE '%Lurín%' THEN 0.58 ELSE 0.18 END AS avance,
           CASE WHEN cg.estado = 'T' THEN DATE '2025-12-15'
                WHEN cg.descripcion LIKE '%Lurín%' THEN DATE '2026-10-07' ELSE DATE '2026-10-21' END AS inicio
    FROM dbasset.cargas cg
    WHERE cg.cod_empresa = 2 AND cg.estado <> 'C'
  LOOP
    SELECT count(*) INTO total
    FROM dbasset.m_activos m JOIN dbasset.local l ON l.cod_local = m.cod_local
    WHERE m.cod_empresa = 2 AND l.cod_interno = ANY (c.locales);

    k := 0;
    FOR a IN
      SELECT m.cod_activo, m.cod_estado, m.cod_responsable
      FROM dbasset.m_activos m JOIN dbasset.local l ON l.cod_local = m.cod_local
      WHERE m.cod_empresa = 2 AND l.cod_interno = ANY (c.locales)
      ORDER BY m.cod_activo
    LOOP
      k := k + 1;
      dia := c.inicio + floor(random() * 4)::int;
      INSERT INTO dbasset.detalle_carga (cod_carga, cod_activo, inventariado, cod_estado, fechainventario,
                                         nuevo, impreso, modificado, cod_responsable, cod_inventariador)
      VALUES (c.cod_carga, a.cod_activo,
              CASE WHEN random() < c.avance THEN '1' ELSE '0' END,
              a.cod_estado,
              NULL, 0, 0, 0, a.cod_responsable,
              invs[1 + ((k - 1) * array_length(invs, 1)) / GREATEST(total, 1)]);
      UPDATE dbasset.detalle_carga
         SET fechainventario = to_char(dia + time '08:30' + (floor(random() * 540) || ' minutes')::interval, 'DD/MM/YYYY HH24:MI'),
             impreso = 1
       WHERE id = currval(pg_get_serial_sequence('dbasset.detalle_carga', 'id')) AND inventariado = '1';
    END LOOP;
  END LOOP;

  -- El activo guarda el último inventariador asignado
  UPDATE dbasset.m_activos m
     SET cod_inventariador = d.cod_inventariador
    FROM dbasset.detalle_carga d
    JOIN dbasset.cargas cg ON cg.cod_carga = d.cod_carga AND cg.cod_empresa = 2
   WHERE m.cod_empresa = 2 AND m.cod_activo = d.cod_activo;
END $$;

-- ---------------------------------------------------------------- reubicaciones detectadas en el inventario anual
INSERT INTO dbasset.reubicacion (cod_carga, cod_activo, cod_inventariador,
       cod_local_anterior, cod_area_anterior, cod_oficina_anterior, cod_resp_anterior,
       cod_local_nuevo, cod_area_nuevo, cod_oficina_nuevo, cod_resp_nuevo,
       estado, observacion, fecha_reubicacion, fecha_registro)
SELECT cg.cod_carga, m.cod_activo, m.cod_inventariador,
       o2.cod_local, o2.cod_area, o2.cod_oficina, ro2.cod_responsable,
       m.cod_local, m.cod_area, m.cod_oficina, m.cod_responsable,
       'REUBICADO', v.obs, '16/12/2025 ' || v.hora, TIMESTAMP '2025-12-16 00:00' + v.hora::interval
FROM (VALUES
  ('AF-00012', 'OFI-005', 'Encontrado en Logística, pertenecía a Gerencia', '10:15'),
  ('AF-00058', 'OFI-012', 'Laptop prestada a Marketing, se regulariza',      '11:40'),
  ('AF-00091', 'OFI-003', 'Monitor trasladado desde Contabilidad',          '15:05'),
  ('AF-00120', 'OFI-011', 'Silla reubicada por remodelación del piso 6',     '16:30')
) v(cod, ofi_ant, obs, hora)
JOIN dbasset.m_activos m ON m.cod_empresa = 2 AND m.cod_activo = v.cod
JOIN dbasset.oficina o2 ON o2.cod_empresa = 2 AND o2.cod_interno = v.ofi_ant
LEFT JOIN LATERAL (SELECT cod_responsable FROM dbasset.responsable_oficina WHERE cod_oficina = o2.cod_oficina LIMIT 1) ro2 ON TRUE
JOIN dbasset.cargas cg ON cg.cod_empresa = 2 AND cg.estado = 'T';

-- ---------------------------------------------------------------- bajas
DO $$
DECLARE a record; i int := 0;
  motivos text[] := ARRAY['OBSOLESCENCIA','DESTRUCCION','PERDIDA','VENTA','OBSOLESCENCIA','OTRO','OBSOLESCENCIA'];
  detalles text[] := ARRAY[
    'Equipo sin soporte del fabricante y con fallas recurrentes; informe técnico TI-2026-031',
    'Daño irreparable por cortocircuito; informe de mantenimiento MT-2026-014',
    'No ubicado en dos inventarios consecutivos; denuncia policial N.° 2026-118',
    'Vendido a trabajador mediante subasta interna, acta N.° 07-2026',
    'Reemplazado por renovación tecnológica 2026',
    'Donado a la I.E. N.° 7041 de Villa El Salvador, resolución GG-045-2026',
    'Superó su vida útil contable y técnica'];
BEGIN
  FOR a IN
    SELECT * FROM dbasset.m_activos
    WHERE cod_empresa = 2 AND cod_estado = 3 AND categoria <> 'VEHÍCULOS'
    ORDER BY cod_activo
    LIMIT 7
  LOOP
    i := i + 1;
    UPDATE dbasset.m_activos SET situacion = 'INACTIVO', updated_at = now() WHERE id = a.id;
    INSERT INTO dbasset.baja_activo (cod_empresa, id_activo, cod_activo, motivo, detalle, autorizado_por,
           registrado_por, fecha_baja, fecha_registro, cod_local, cod_area, cod_oficina, cod_responsable)
    VALUES (2, a.id, a.cod_activo, motivos[i], detalles[i], 'Ana Lucía Benavides Soto',
            'Administrador General', DATE '2026-03-10' + (i * 27), TIMESTAMP '2026-03-10 10:00' + (i * 27 || ' days')::interval,
            a.cod_local, a.cod_area, a.cod_oficina, a.cod_responsable);
  END LOOP;
END $$;

-- ---------------------------------------------------------------- transferencias
-- RECIBIDA: el activo ya está en el destino (su ubicación actual); el origen es otra oficina
INSERT INTO dbasset.transferencia (cod_empresa, id_activo, cod_activo,
       cod_local_origen, cod_area_origen, cod_oficina_origen, cod_resp_origen,
       cod_local_destino, cod_area_destino, cod_oficina_destino, cod_resp_destino,
       motivo, estado, registrado_por, origen_registro, fecha_envio, recibido_por, fecha_recepcion, observacion_recepcion)
SELECT 2, m.id, m.cod_activo,
       o2.cod_local, o2.cod_area, o2.cod_oficina, (SELECT cod_responsable FROM dbasset.responsable_oficina WHERE cod_oficina = o2.cod_oficina LIMIT 1),
       m.cod_local, m.cod_area, m.cod_oficina, m.cod_responsable,
       v.motivo, 'RECIBIDA', 'Administrador General', v.canal, v.envio::timestamp,
       r.responsable, v.envio::timestamp + interval '1 day 3 hours', 'Recibido conforme'
FROM (VALUES
  ('AF-00030', 'OFI-009', 'Asignación de laptop por ingreso de personal', 'WEB', '2026-08-04 09:20'),
  ('AF-00150', 'OFI-010', 'Monitor adicional para puesto de diseño',      'WEB', '2026-08-19 11:05'),
  ('AF-00210', 'OFI-018', 'Terminal móvil reasignado a recepción',        'APP', '2026-09-02 15:40'),
  ('AF-00305', 'OFI-023', 'Equipo enviado a la sucursal por apertura de puesto', 'WEB', '2026-09-23 10:10')
) v(cod, ofi_ori, motivo, canal, envio)
JOIN dbasset.m_activos m ON m.cod_empresa = 2 AND m.cod_activo = v.cod AND m.situacion = 'ACTIVO'
JOIN dbasset.oficina o2 ON o2.cod_empresa = 2 AND o2.cod_interno = v.ofi_ori AND o2.cod_oficina <> m.cod_oficina
LEFT JOIN dbasset.responsable r ON r.cod_responsable = m.cod_responsable;

-- PENDIENTE / RECHAZADA: el activo sigue en su ubicación (origen) y se propone otro destino
INSERT INTO dbasset.transferencia (cod_empresa, id_activo, cod_activo,
       cod_local_origen, cod_area_origen, cod_oficina_origen, cod_resp_origen,
       cod_local_destino, cod_area_destino, cod_oficina_destino, cod_resp_destino,
       motivo, estado, registrado_por, origen_registro, fecha_envio, recibido_por, fecha_recepcion, observacion_recepcion)
SELECT 2, m.id, m.cod_activo,
       m.cod_local, m.cod_area, m.cod_oficina, m.cod_responsable,
       o2.cod_local, o2.cod_area, o2.cod_oficina, (SELECT cod_responsable FROM dbasset.responsable_oficina WHERE cod_oficina = o2.cod_oficina LIMIT 1),
       v.motivo, v.estado, 'Administrador General', v.canal, v.envio::timestamp,
       CASE WHEN v.estado = 'RECHAZADA' THEN 'Hugo César Ramos Quispe' END,
       CASE WHEN v.estado = 'RECHAZADA' THEN v.envio::timestamp + interval '2 days' END,
       CASE WHEN v.estado = 'RECHAZADA' THEN 'No hay espacio disponible en la nave; reprogramar para noviembre' END
FROM (VALUES
  ('AF-00045', 'OFI-022', 'Refuerzo de equipos para campaña de ventas en Arequipa', 'PENDIENTE', 'WEB', '2026-10-22 09:45'),
  ('AF-00180', 'OFI-016', 'Préstamo temporal para conteo cíclico',                  'PENDIENTE', 'APP', '2026-10-24 14:10'),
  ('AF-00260', 'OFI-024', 'Traslado a sucursal Trujillo',                            'PENDIENTE', 'WEB', '2026-10-26 08:55'),
  ('AF-00095', 'OFI-015', 'Archivador para zona de despacho',                       'RECHAZADA', 'WEB', '2026-10-01 11:30')
) v(cod, ofi_dest, motivo, estado, canal, envio)
JOIN dbasset.m_activos m ON m.cod_empresa = 2 AND m.cod_activo = v.cod AND m.situacion = 'ACTIVO'
JOIN dbasset.oficina o2 ON o2.cod_empresa = 2 AND o2.cod_interno = v.ofi_dest AND o2.cod_oficina <> m.cod_oficina;

-- ---------------------------------------------------------------- inventarios RFID
INSERT INTO dbasset.inventario_sesion (cod_empresa, descripcion, alcance, cod_local, cod_area, cod_oficina,
       estado, total_esperados, creado_por, fecha_creacion, cerrado_por, fecha_cierre)
SELECT 2, 'Inventario RFID - Tecnología de la Información (3er trimestre)', 'AREA', a.cod_local, a.cod_area, NULL,
       'CERRADA', 0, 'Administrador General', '2026-09-18 09:10', 'Administrador General', '2026-09-18 17:40'
FROM dbasset.area a WHERE a.cod_empresa = 2 AND a.cod_interno = 'ARE-004';

INSERT INTO dbasset.inventario_sesion (cod_empresa, descripcion, alcance, cod_local, cod_area, cod_oficina,
       estado, total_esperados, creado_por, fecha_creacion)
SELECT 2, 'Inventario RFID - Almacén Central Lurín (octubre)', 'LOCAL', l.cod_local, NULL, NULL,
       'ABIERTA', 0, 'Administrador General', '2026-10-26 08:30'
FROM dbasset.local l WHERE l.cod_empresa = 2 AND l.cod_interno = 'LOC-002';

DO $$
DECLARE s record; a record; inv int; leidos float8; base timestamp; t1 timestamp; t2 timestamp;
BEGIN
  SELECT cod_inventariador INTO inv FROM dbasset.inventariador WHERE cod_empresa = 2 AND cod_interno = 'INV-001';

  FOR s IN SELECT * FROM dbasset.inventario_sesion WHERE cod_empresa = 2 LOOP
    INSERT INTO dbasset.inventario_esperado (id_sesion, id_activo, cod_activo, epc, cod_local, cod_area, cod_oficina, cod_responsable)
    SELECT s.id, m.id, m.cod_activo, m.epc, m.cod_local, m.cod_area, m.cod_oficina, m.cod_responsable
    FROM dbasset.m_activos m
    WHERE m.cod_empresa = 2 AND m.activo = TRUE AND m.situacion = 'ACTIVO'
      AND (s.alcance <> 'AREA'  OR m.cod_area  = s.cod_area)
      AND (s.alcance <> 'LOCAL' OR m.cod_local = s.cod_local);

    UPDATE dbasset.inventario_sesion
       SET total_esperados = (SELECT count(*) FROM dbasset.inventario_esperado WHERE id_sesion = s.id)
     WHERE id = s.id;

    leidos := CASE WHEN s.estado = 'CERRADA' THEN 0.92 ELSE 0.6 END;
    base := s.fecha_creacion + interval '20 minutes';

    FOR a IN SELECT e.epc, e.cod_area FROM dbasset.inventario_esperado e WHERE e.id_sesion = s.id AND e.epc IS NOT NULL LOOP
      IF random() < leidos THEN
        t1 := base + (floor(random() * 360) || ' minutes')::interval;
        t2 := t1 + (floor(random() * 25) || ' minutes')::interval;
        INSERT INTO dbasset.inventario_lectura (id_sesion, epc, conteo, rssi, primera_lectura, ultima_lectura,
               cod_area_lectura, dispositivo, cod_inventariador, origen)
        VALUES (s.id, a.epc, 1 + floor(random() * 14)::int, -38 - floor(random() * 38)::int, t1, t2,
                a.cod_area, 'RFD8500-0' || (1 + floor(random() * 2))::int, inv, 'APP');
      END IF;
    END LOOP;

    -- Un activo de otra ubicación leído en la zona y un tag que no está en el catálogo
    INSERT INTO dbasset.inventario_lectura (id_sesion, epc, conteo, rssi, primera_lectura, ultima_lectura,
           cod_area_lectura, dispositivo, cod_inventariador, origen)
    SELECT s.id, m.epc, 3, -61, base + interval '2 hours', base + interval '2 hours 4 minutes',
           COALESCE(s.cod_area, (SELECT e.cod_area FROM dbasset.inventario_esperado e WHERE e.id_sesion = s.id LIMIT 1)),
           'RFD8500-01', inv, 'APP'
    FROM dbasset.m_activos m
    WHERE m.cod_empresa = 2 AND m.epc IS NOT NULL AND m.situacion = 'ACTIVO'
      AND m.id NOT IN (SELECT id_activo FROM dbasset.inventario_esperado WHERE id_sesion = s.id)
    ORDER BY m.id LIMIT 1;

    INSERT INTO dbasset.inventario_lectura (id_sesion, epc, conteo, rssi, primera_lectura, ultima_lectura,
           cod_area_lectura, dispositivo, cod_inventariador, origen)
    VALUES (s.id, 'E28068940000500B1C3D9A' || lpad(s.id::text, 2, '0'), 2, -70,
            base + interval '3 hours', base + interval '3 hours 1 minute',
            COALESCE(s.cod_area, (SELECT e.cod_area FROM dbasset.inventario_esperado e WHERE e.id_sesion = s.id LIMIT 1)),
            'RFD8500-02', inv, 'APP');
  END LOOP;
END $$;

COMMIT;

-- Resumen
SELECT 'locales' AS tabla, count(*) FROM dbasset.local WHERE cod_empresa = 2
UNION ALL SELECT 'areas', count(*) FROM dbasset.area WHERE cod_empresa = 2
UNION ALL SELECT 'oficinas', count(*) FROM dbasset.oficina WHERE cod_empresa = 2
UNION ALL SELECT 'custodios', count(*) FROM dbasset.responsable WHERE cod_empresa = 2
UNION ALL SELECT 'inventariadores', count(*) FROM dbasset.inventariador WHERE cod_empresa = 2
UNION ALL SELECT 'activos', count(*) FROM dbasset.m_activos WHERE cod_empresa = 2
UNION ALL SELECT 'activos con EPC', count(*) FROM dbasset.m_activos WHERE cod_empresa = 2 AND epc IS NOT NULL
UNION ALL SELECT 'cargas', count(*) FROM dbasset.cargas WHERE cod_empresa = 2
UNION ALL SELECT 'detalle de cargas', count(*) FROM dbasset.detalle_carga d JOIN dbasset.cargas c ON c.cod_carga = d.cod_carga WHERE c.cod_empresa = 2
UNION ALL SELECT 'sesiones RFID', count(*) FROM dbasset.inventario_sesion WHERE cod_empresa = 2
UNION ALL SELECT 'lecturas RFID', count(*) FROM dbasset.inventario_lectura l JOIN dbasset.inventario_sesion s ON s.id = l.id_sesion WHERE s.cod_empresa = 2
UNION ALL SELECT 'transferencias', count(*) FROM dbasset.transferencia WHERE cod_empresa = 2
UNION ALL SELECT 'bajas', count(*) FROM dbasset.baja_activo WHERE cod_empresa = 2
UNION ALL SELECT 'reubicaciones', count(*) FROM dbasset.reubicacion r JOIN dbasset.cargas c ON c.cod_carga = r.cod_carga WHERE c.cod_empresa = 2;
