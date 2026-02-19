package com.dbasset.backend.service;

import com.dbasset.backend.entity.*;
import com.dbasset.backend.repository.*;
import com.dbasset.backend.dto.RangoDistribucionRequest;
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
    @Autowired private InventariadorRepository inventariadorRepository;
    @Autowired private LocalRepository localRepository;
    @Autowired private AreaRepository areaRepository;
    @Autowired private OficinaRepository oficinaRepository;

    // --- MÉTODOS BÁSICOS ---

    public List<Carga> listarTodas(Integer codEmpresa) {
        List<Carga> lista = cargaRepository.findByCodEmpresaAndActivoTrue(codEmpresa);
        lista.sort((a, b) -> b.getCodCarga().compareTo(a.getCodCarga()));
        return lista;
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
                .orElseThrow(() -> new RuntimeException("Carga no encontrada"));
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

    // --- CARGA MASIVA CORREGIDA ---

    @Transactional
    public Map<String, Object> importarMasivo(
            Integer codCarga,
            MultipartFile file,
            String jsonMapeo,
            String jsonConfiguracion,
            Integer codEmpresa,
            Integer codLocalUnico,
            Integer codAreaUnica,
            Integer codOficinaUnica
    ) throws Exception {

        Carga carga = cargaRepository.findByCodCargaAndCodEmpresa(codCarga, codEmpresa)
                .orElseThrow(() -> new RuntimeException("Carga no encontrada"));

        ObjectMapper mapper = new ObjectMapper();

        // Actualizar configuración
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

        Map<String, String> mapeoUsuario = mapper.readValue(jsonMapeo, Map.class);

        // Detectar escenario
        boolean tieneColumnasUbicacion = mapeoUsuario.containsKey("cod_local")
                || mapeoUsuario.containsKey("cod_area")
                || mapeoUsuario.containsKey("cod_oficina");

        // Validar Escenario 2
        if (!tieneColumnasUbicacion && (codLocalUnico == null || codAreaUnica == null || codOficinaUnica == null)) {
            throw new RuntimeException("Debe especificar la ubicación única (Local/Área/Oficina) para esta carga");
        }

        // Procesar Excel
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

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            Activo activo = new Activo();
            activo.setCodEmpresa(codEmpresa);
            activo.setActivo(true);

            // Mapear campos del Excel
            for (Map.Entry<String, String> entry : mapeoUsuario.entrySet()) {
                String campoBD = entry.getKey();
                String columnaExcel = entry.getValue();
                Integer colIndex = colIndices.get(columnaExcel);
                String valor = colIndex != null ? getCellValueAsString(row.getCell(colIndex)) : "";
                asignarValorAlActivo(activo, campoBD, valor, codEmpresa);
            }

            // Escenario 2: Asignar ubicación única
            if (!tieneColumnasUbicacion) {
                activo.setLocal(localRepository.findById(codLocalUnico).orElse(null));
                activo.setArea(areaRepository.findById(codAreaUnica).orElse(null));
                activo.setOficina(oficinaRepository.findById(codOficinaUnica).orElse(null));
            }

            // Validar código único
            if (activo.getCodActivo() != null && !activoRepository.existsByCodActivoAndCodEmpresa(activo.getCodActivo(), codEmpresa)) {
                activosNuevos.add(activo);
                procesados++;
            } else {
                errores.add("Fila " + (i + 1) + ": Código '" + activo.getCodActivo() + "' duplicado o vacío");
            }
        }

        if (!activosNuevos.isEmpty()) {

            // 1. EL FRENO DE MANO: saveAllAndFlush
            // Esto obliga a Spring a detenerse, enviar el paquete por la red a Postgres en Render,
            // y esperar la confirmación física antes de pasar a la siguiente línea.
            activoRepository.saveAllAndFlush(activosNuevos);

            for (Activo act : activosNuevos) {
                DetalleCarga det = new DetalleCarga();
                det.setCarga(carga);

                // 2. Usamos tu lógica original intacta. Sin @JoinColumn raros.
                det.setCodActivo(act.getCodActivo());

                det.setInventariado("0");
                det.setCodEstado(1);
                detallesNuevos.add(det);
            }

            // 3. Guardamos los detalles. Ahora Postgres ya sabe que los activos existen.
            detalleCargaRepository.saveAll(detallesNuevos);

            carga.setEstado("A");
            cargaRepository.save(carga);

            // 4. Otro freno por seguridad para el estado de la carga
            cargaRepository.flush();
        }

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", "Proceso completado");
        respuesta.put("totalProcesados", procesados);
        respuesta.put("errores", errores);
        return respuesta;
    }

    // --- DISTRIBUCIÓN CORREGIDA: SOLO ASIGNA RESPONSABLE/INVENTARIADOR ---

    public Integer obtenerTotalItems(Integer codCarga) {
        return detalleCargaRepository.countByCarga_CodCarga(codCarga);
    }

    @Transactional
    public void distribuirCarga(Integer codCarga, List<RangoDistribucionRequest> rangos, Integer codEmpresa) {
        Carga carga = cargaRepository.findByCodCargaAndCodEmpresa(codCarga, codEmpresa)
                .orElseThrow(() -> new RuntimeException("Carga no encontrada"));

        List<DetalleCarga> detalles = detalleCargaRepository.findByCarga_CodCargaOrderByIdAsc(codCarga);

        for (RangoDistribucionRequest rango : rangos) {
            // Validar rango
            if (rango.getInicio() < 1 || rango.getFin() > detalles.size() || rango.getInicio() > rango.getFin()) {
                throw new RuntimeException("Rango inválido: " + rango.getInicio() + "-" + rango.getFin());
            }

            // Obtener entidades (si aplica)
            Responsable responsable = null;
            if (rango.getCodResponsable() != null) {
                responsable = responsableRepository.findById(rango.getCodResponsable())
                        .orElseThrow(() -> new RuntimeException("Responsable no encontrado"));
            }

            Inventariador inventariador = null;
            if (rango.getCodInventariador() != null) {
                inventariador = inventariadorRepository.findById(rango.getCodInventariador())
                        .orElseThrow(() -> new RuntimeException("Inventariador no encontrado"));
            }

            // Asignar al rango
            for (int i = rango.getInicio() - 1; i < rango.getFin(); i++) {
                DetalleCarga detalle = detalles.get(i);

                // ✅ Buscar el activo usando cod_activo
                if (detalle.getCodActivo() != null) {
                    Optional<Activo> activoOpt = activoRepository.findByCodActivoAndCodEmpresa(detalle.getCodActivo(), codEmpresa);

                    if (activoOpt.isPresent()) {
                        Activo activo = activoOpt.get();

                        // ✅ Asignar en m_activos
                        if (responsable != null) {
                            activo.setResponsable(responsable);
                        }
                        if (inventariador != null) {
                            activo.setInventariador(inventariador);
                        }
                        activoRepository.save(activo);
                    }
                }

                // ✅ Asignar en detalle_carga
                if (responsable != null) {
                    detalle.setResponsable(responsable);
                }
                if (inventariador != null) {
                    detalle.setInventariador(inventariador);
                }
                detalleCargaRepository.save(detalle);
            }
        }
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

    private void asignarValorAlActivo(Activo activo, String campoBD, String valor, Integer codEmpresa) {
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

            // ✅ MAPEO DE UBICACIÓN (Escenario 1)
            case "cod_local":
                if (valor != null && !valor.isEmpty()) {
                    try {
                        activo.setLocal(localRepository.findById(Integer.parseInt(valor)).orElse(null));
                    } catch (NumberFormatException e) {}
                }
                break;
            case "cod_area":
                if (valor != null && !valor.isEmpty()) {
                    try {
                        activo.setArea(areaRepository.findById(Integer.parseInt(valor)).orElse(null));
                    } catch (NumberFormatException e) {}
                }
                break;
            case "cod_oficina":
                if (valor != null && !valor.isEmpty()) {
                    try {
                        activo.setOficina(oficinaRepository.findById(Integer.parseInt(valor)).orElse(null));
                    } catch (NumberFormatException e) {}
                }
                break;

            default: break;
        }
    }

    public Carga obtenerPorId(Integer codCarga, Integer codEmpresa) {
        return cargaRepository.findByCodCargaAndCodEmpresa(codCarga, codEmpresa)
                .orElseThrow(() -> new RuntimeException("Carga no encontrada"));
    }

    // ✅ AGREGAR ESTE MÉTODO EN CargaService.java (si no existe)

    public List<DetalleCarga> obtenerDetalleCarga(Integer codCarga, Integer codEmpresa) {
        // Verificar que la carga pertenece a la empresa
        cargaRepository.findByCodCargaAndCodEmpresa(codCarga, codEmpresa)
                .orElseThrow(() -> new RuntimeException("Carga no encontrada"));

        List<DetalleCarga> detalles = detalleCargaRepository.findByCarga_CodCargaOrderByIdAsc(codCarga);

        // ✅ Cargar el objeto Activo manualmente para cada detalle
        for (DetalleCarga detalle : detalles) {
            if (detalle.getCodActivo() != null) {
                activoRepository.findByCodActivoAndCodEmpresa(detalle.getCodActivo(), codEmpresa)
                        .ifPresent(detalle::setActivo);
            }
        }

        return detalles;
    }
}