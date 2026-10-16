# Módulo RFID – Fase 1 (rama `rfid`)

## Puesta en marcha
- **Base de datos:** `src/main/resources/db/migracion_rfid.sql` se ejecuta solo al arrancar
  (`spring.sql.init.mode=always`). Es idempotente y solo agrega columnas y tablas; no modifica datos.
  Para desactivarlo: `SPRING_SQL_INIT_MODE=never` (y correr el script a mano con psql).
- **Archivos (fotos y sustentos de bajas):** se guardan en el disco del servidor, en `DBASSET_ARCHIVOS_DIR`
  (por defecto `./archivos`). Hay que incluir esa carpeta en el respaldo del servidor.
- **Cabeceras:** `X-Tenant-ID` (obligatoria). Para auditoría: `X-User-ID` (web) o `X-Inventariador-ID` (app).

## Contrato para la app móvil (RFD8500)
| Acción | Endpoint |
|---|---|
| Sesiones abiertas | `GET /api/rfid/inventarios?estado=ABIERTA` |
| Lista esperada (trabajo sin conexión) | `GET /api/rfid/inventarios/{id}/esperados` |
| Enviar barrido | `POST /api/rfid/inventarios/{id}/lecturas` |
| Resultado / avance | `GET /api/rfid/inventarios/{id}/resultado` |
| Activo por EPC (alta, búsqueda) | `GET /api/activos/epc/{epc}` |
| Vincular EPC a un activo | `PUT /api/activos/{id}/epc` `{"epc": "..."}` |
| Foto de evidencia | `POST /api/activos/{id}/fotos` (multipart `file`) |
| Transferir | `POST /api/transferencias` `{"epc": "...", "codAreaDestino": 2, "codOficinaDestino": 5, "codRespDestino": 7, "motivo": "..."}` |
| Confirmar recepción | `POST /api/transferencias/{id}/confirmar` `{"recibidoPor": "...", "observacion": "..."}` |
| Dar de baja | `POST /api/bajas` `{"epc": "...", "motivo": "PERDIDA", "autorizadoPor": "...", "detalle": "..."}` + `POST /api/bajas/{id}/adjuntos` (multipart `files`) |

Cuerpo de `lecturas`:
```json
{
  "idLote": "uuid-generado-en-el-celular",
  "dispositivo": "RFD8500-01",
  "codInventariador": 3,
  "codAreaLectura": 12,
  "lecturas": [{ "epc": "E200001A2B3C4D5E6F708001", "conteo": 5, "rssi": -52, "fecha": "2026-10-16T10:15:00-05:00" }]
}
```
- **Sin conexión:** la app guarda los lotes y los reenvía con el mismo `idLote`. Un lote repetido responde
  `{"duplicado": true}` y no se vuelve a contar.
- Los EPC se normalizan: se quitan espacios, guiones y `:` y se pasan a mayúsculas (hexadecimal de 4 a 128 caracteres).
- La respuesta incluye `lecturas[]` con la clasificación de cada EPC del lote, para el aviso en pantalla.

## Clasificación
| Resultado | Detalle |
|---|---|
| `ENCONTRADO` | Esperado en la sesión y leído |
| `NO_ENCONTRADO` | `NO_LEIDO`, o `SIN_EPC` si el activo aún no tiene tag |
| `INESPERADO` | `OTRA_UBICACION`, `DADO_DE_BAJA` o `NO_REGISTRADO` (EPC que no está en el catálogo) |

El cruce se calcula cada vez que se consulta. Si después se vincula un EPC desconocido a un activo,
el resultado se corrige solo.

## Pendiente (requiere la app o el SDK del lector)
- Lectura física de tags y barrido masivo (app móvil con el SDK de Zebra).
- Modo «Localizar activo» por proximidad (en el panel web el botón está desactivado).
