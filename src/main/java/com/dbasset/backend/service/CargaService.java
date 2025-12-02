package com.dbasset.backend.service;

import com.dbasset.backend.entity.*;
import com.dbasset.backend.repository.*;
import com.dbasset.backend.dto.RangoDistribucionRequest; // ✅ ASEGÚRATE DE IMPORTAR TU DTO
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CargaService {

    @Autowired private CargaRepository cargaRepository;
    @Autowired private DetalleCargaRepository detalleCargaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ActivoRepository activoRepository;
    @Autowired private ConfiguracionCampoRepository configRepository;
    @Autowired private ResponsableRepository responsableRepository;

    // --- MÉTODOS BÁSICOS ---

    public List<Carga> listarTodas(Integer codEmpresa) {
        return cargaRepository.findByCodEmpresaAndActivoTrue(codEmpresa);
    }

    @Transactional
    public Carga crearCarga(String descripcion, Integer codEmpresa) {
        Carga carga = new Carga();
        carga.setDescripcion(descripcion);
        carga.setEstado("C");
        carga.setFecha(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        carga.setActivo(true);
        carga.setCodEmpresa(codEmpresa);
        return cargaRepository.save(carga);
    }

    @Transactional
    public void asignarCargaAUsuario(Integer codCarga, Integer codUsuario, Integer codEmpresa) {
        Carga carga = cargaRepository.findByCodCargaAndCodEmpresa(codCarga, codEmpresa)
                .orElseThrow(() -> new RuntimeException("Carga no encontrada o sin permiso"));
        Usuario usuario = usuarioRepository.findById(codUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<DetalleCarga> detalles = detalleCargaRepository.findByCarga_CodCarga(codCarga);
        for (DetalleCarga detalle : detalles) {
            detalle.setUsuario(usuario);
        }
        detalleCargaRepository.saveAll(detalles);

        carga.setEstado("A");
        cargaRepository.save(carga);
    }

    // --- MÉTODOS AVANZADOS (CARGA MASIVA) ---

    @Transactional
    public Map<String, Object> importarMasivo(
            Integer codCarga,
            MultipartFile file,
            String jsonMapeo,
            String jsonConfiguracion,
            Integer codEmpresa
    ) throws Exception {

        // 1. Validar Carga
        Carga carga = cargaRepository.findByCodCargaAndCodEmpresa(codCarga, codEmpresa)
                .orElseThrow(() -> new RuntimeException("Carga no encontrada"));

        ObjectMapper mapper = new ObjectMapper();

        // 2. ACTUALIZAR CONFIGURACIÓN EN BD
        if (jsonConfiguracion != null && !jsonConfiguracion.isEmpty()) {
            ConfiguracionCampo[] configsArray = mapper.readValue(jsonConfiguracion, ConfiguracionCampo[].class);
            for (ConfiguracionCampo nuevaConfig : configsArray) {
                configRepository.findById(nuevaConfig.getId()).ifPresent(existente -> {
                    if (existente.getCodEmpresa().equals(codEmpresa)) {
                        existente.setEsVisible(nuevaConfig.getEsVisible());
                        existente.setEsObligatorio(nuevaConfig.getEsObligatorio());
                        configRepository.save(existente);
                    }
                });
            }
        }

        // 3. Leer Mapeo (JSON -> Map)
        Map<String, String> mapeoUsuario = mapper.readValue(jsonMapeo, Map.class);

        // 4. Procesar Excel
        Workbook workbook = WorkbookFactory.create(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);
        Row headerRow = sheet.getRow(0);

        Map<String, Integer> colIndices = new HashMap<>();
        for (Cell cell : headerRow) {
            colIndices.put(cell.getStringCellValue().trim(), cell.getColumnIndex());
        }

        List<Activo> activosNuevos = new ArrayList<>();
        List<DetalleCarga> detallesNuevos = new ArrayList<>();
        List<String> errores = new ArrayList<>();
        int procesados = 0;

        // 5. Iterar Filas
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            Activo activo = new Activo();
            activo.setCodEmpresa(codEmpresa);
            activo.setActivo(true);

            boolean filaValida = true;

            // Llenar campos
            for (Map.Entry<String, String> entry : mapeoUsuario.entrySet()) {
                String campoBD = entry.getKey();
                String columnaExcel = entry.getValue();

                Integer colIndex = colIndices.get(columnaExcel);
                String valor = "";

                if (colIndex != null) {
                    valor = getCellValueAsString(row.getCell(colIndex));
                }
                asignarValorAlActivo(activo, campoBD, valor);
            }

            if (filaValida) {
                // Validar duplicados de código
                if (activo.getCodActivo() != null && !activoRepository.existsByCodActivoAndCodEmpresa(activo.getCodActivo(), codEmpresa)) {
                    activosNuevos.add(activo);
                    procesados++;
                } else {
                    errores.add("Fila " + (i + 1) + ": Código '" + activo.getCodActivo() + "' duplicado o vacío.");
                }
            }
        }

        // 6. Guardar en Lote
        if (!activosNuevos.isEmpty()) {
            List<Activo> guardados = activoRepository.saveAll(activosNuevos);

            for (Activo act : guardados) {
                DetalleCarga det = new DetalleCarga();
                det.setCarga(carga);
                det.setActivo(act);
                det.setCodActivo(act.getCodActivo());
                det.setInventariado("0");
                det.setCodEstado(1);
                detallesNuevos.add(det);
            }
            detalleCargaRepository.saveAll(detallesNuevos);
        }

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", "Proceso completado");
        respuesta.put("totalProcesados", procesados);
        respuesta.put("errores", errores);
        return respuesta;
    }

    // --- ✅ NUEVOS MÉTODOS DE DISTRIBUCIÓN POR RANGOS (USUARIOS INVENTARIADORES) ---

    // 1. Obtener total para el frontend
    public Integer obtenerTotalItems(Integer codCarga) {
        return detalleCargaRepository.countByCarga_CodCarga(codCarga);
    }

    // 2. Lógica de distribución matemática
    @Transactional
    public void distribuirCarga(Integer codCarga, List<RangoDistribucionRequest> distribuciones, Integer codEmpresa) {

        // 1. Validar Carga
        Carga carga = cargaRepository.findByCodCargaAndCodEmpresa(codCarga, codEmpresa)
                .orElseThrow(() -> new RuntimeException("Carga no encontrada"));

        // 2. Obtener items ordenados (Orden del Excel)
        List<DetalleCarga> detalles = detalleCargaRepository.findByCarga_CodCargaOrderByIdDetalleAsc(codCarga);

        if (detalles.isEmpty()) {
            throw new RuntimeException("La carga está vacía.");
        }

        // Lista para guardar los activos modificados en lote
        List<Activo> activosParaActualizar = new ArrayList<>();

        // 3. Procesar Rangos
        for (RangoDistribucionRequest rango : distribuciones) {

            // ✅ Buscar al RESPONSABLE (No al usuario)
            Responsable resp = responsableRepository.findById(rango.getCodResponsable())
                    .orElseThrow(() -> new RuntimeException("Responsable ID " + rango.getCodResponsable() + " no encontrado"));

            int indexInicio = rango.getInicio() - 1;
            int indexFin = rango.getFin() - 1;

            if (indexInicio < 0) indexInicio = 0;
            if (indexFin >= detalles.size()) indexFin = detalles.size() - 1;
            if (indexInicio > indexFin) continue;

            // 4. Actualizar los ACTIVOS asociados a esas filas
            for (int i = indexInicio; i <= indexFin; i++) {
                DetalleCarga detalle = detalles.get(i);

                // Obtenemos el activo real asociado a esta fila
                Activo activo = detalle.getActivo();

                if (activo != null) {
                    // 1. ✅ Asignamos el OBJETO Responsable completo (no el ID entero)
                    activo.setResponsable(resp);

                    // 2. ✅ Asignamos el OBJETO Oficina (que ya viene dentro de 'resp')
                    if (resp.getOficina() != null) {
                        activo.setOficina(resp.getOficina());
                    }

                    // 3. ✅ Asignamos Area
                    // Como 'Responsable' solo tiene el ID entero (codArea), creamos una
                    // instancia temporal de Area solo con el ID para que Hibernate sepa hacer la relación.
                    if (resp.getCodArea() != null) {
                        Area areaRef = new Area();
                        areaRef.setCodArea(resp.getCodArea()); // Asegúrate que el ID en tu entidad Area se llame 'codArea' o usa 'setId'
                        activo.setArea(areaRef);
                    }

                    // 4. ✅ Asignamos Local
                    // Mismo caso: creamos una referencia temporal con el ID.
                    if (resp.getCodLocal() != null) {
                        Local localRef = new Local();
                        localRef.setCodLocal(resp.getCodLocal()); // Asegúrate que el ID en tu entidad Local se llame 'codLocal' o usa 'setId'
                        activo.setLocal(localRef);
                    }

                    activosParaActualizar.add(activo);
                }
            }
        }

        // 5. Guardar cambios en la tabla de ACTIVOS
        if (!activosParaActualizar.isEmpty()) {
            activoRepository.saveAll(activosParaActualizar);
        }

        // Opcional: Marcar carga como 'Asignada' si esa es la lógica de negocio
        // carga.setEstado("A");
        // cargaRepository.save(carga);
    }


    // --- MÉTODOS LEGACY (RESPONSABLES DE ACTIVOS / OFICINA) ---
    // Este método asigna "Responsables" (quien tiene el activo), NO "Usuarios" (quien inventaria).

    @Transactional
    public void asignarRangoResponsable(Integer codCarga, Integer codResponsable, int filaInicio, int filaFin, Integer codEmpresa) {
        Carga carga = cargaRepository.findByCodCargaAndCodEmpresa(codCarga, codEmpresa)
                .orElseThrow(() -> new RuntimeException("Carga no encontrada"));

        Responsable resp = responsableRepository.findById(codResponsable)
                .orElseThrow(() -> new RuntimeException("Responsable no encontrado"));

        int limite = filaFin - filaInicio + 1;
        int desplazamiento = filaInicio - 1;
        if (limite <= 0) throw new RuntimeException("Rango inválido");

        activoRepository.asignarResponsablePorRango(
                codCarga,
                resp.getCodResponsable(),
                resp.getOficina().getCodOficina(),
                resp.getCodArea(),
                resp.getCodLocal(),
                limite,
                desplazamiento
        );
    }

    // --- HELPERS ---

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) return cell.getDateCellValue().toString();
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            default: return "";
        }
    }

    private void asignarValorAlActivo(Activo activo, String campoBD, String valor) {
        if (valor != null) valor = valor.trim();

        switch (campoBD) {
            case "cod_activo": activo.setCodActivo(valor); break;
            case "cod_interno": activo.setCodInterno(valor); break;
            case "descripcion": activo.setDescripcion(valor); break;
            case "marca": activo.setMarca(valor); break;
            case "modelo": activo.setModelo(valor); break;
            case "serie": activo.setSerie(valor); break;
            case "color": activo.setColor(valor); break;
            case "anio": activo.setAnio(valor); break;
            case "fecha_compra": activo.setFechaCompra(valor); break;
            case "tipo": activo.setTipo(valor); break;
            case "dimensiones": activo.setDimensiones(valor); break;
            case "n_motor": activo.setNMotor(valor); break;
            case "n_chasis": activo.setNChasis(valor); break;
            case "placa": activo.setPlaca(valor); break;
            case "cod_color": activo.setCodColor(valor); break;
            case "obs": activo.setObs(valor); break;
            default: break;
        }
    }
}