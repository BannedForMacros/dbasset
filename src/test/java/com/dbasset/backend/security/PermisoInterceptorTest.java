package com.dbasset.backend.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifica el mapeo ruta -> permiso que aplica PermisoInterceptor.
 * No necesita base de datos ni contexto de Spring.
 */
class PermisoInterceptorTest {

    @Test
    void consultarActivosExigeVer() {
        assertEquals("activos.ver", PermisoInterceptor.permisoRequerido("/api/activos", "GET"));
        assertEquals("activos.ver", PermisoInterceptor.permisoRequerido("/api/activos/15", "GET"));
    }

    @Test
    void crearYEditarActivosExigenSuAccion() {
        assertEquals("activos.crear", PermisoInterceptor.permisoRequerido("/api/activos", "POST"));
        assertEquals("activos.editar", PermisoInterceptor.permisoRequerido("/api/activos/15", "PUT"));
        assertEquals("activos.eliminar", PermisoInterceptor.permisoRequerido("/api/activos/15", "DELETE"));
    }

    @Test
    void subirFotoDeActivoExigeEditar() {
        assertEquals("activos.editar", PermisoInterceptor.permisoRequerido("/api/activos/7/fotos", "POST"));
    }

    @Test
    void busquedaTienePermisoPropio() {
        assertEquals("busqueda.ver", PermisoInterceptor.permisoRequerido("/api/activos/busqueda", "GET"));
    }

    @Test
    void vincularEpcUsaPermisosRfid() {
        assertEquals("rfid.ver", PermisoInterceptor.permisoRequerido("/api/activos/sin-epc", "GET"));
        assertEquals("rfid.editar", PermisoInterceptor.permisoRequerido("/api/activos/9/epc", "PUT"));
        assertEquals("rfid.eliminar", PermisoInterceptor.permisoRequerido("/api/activos/9/epc", "DELETE"));
    }

    @Test
    void transferenciasDistinguenCrearDeConfirmar() {
        assertEquals("transferencias.crear", PermisoInterceptor.permisoRequerido("/api/transferencias", "POST"));
        assertEquals("transferencias.editar", PermisoInterceptor.permisoRequerido("/api/transferencias/4/confirmar", "POST"));
        assertEquals("transferencias.editar", PermisoInterceptor.permisoRequerido("/api/transferencias/4/anular", "POST"));
        assertEquals("transferencias.ver", PermisoInterceptor.permisoRequerido("/api/transferencias", "GET"));
    }

    @Test
    void cerrarInventarioRfidExigeEditar() {
        assertEquals("rfid.editar", PermisoInterceptor.permisoRequerido("/api/rfid/inventarios/3/cerrar", "POST"));
        assertEquals("rfid.crear", PermisoInterceptor.permisoRequerido("/api/rfid/inventarios", "POST"));
    }

    @Test
    void mantenedoresYConfiguracionMapeanSuModulo() {
        assertEquals("mantenedores.eliminar", PermisoInterceptor.permisoRequerido("/api/marcas/2", "DELETE"));
        assertEquals("configuracion.crear", PermisoInterceptor.permisoRequerido("/api/oficinas", "POST"));
        assertEquals("inventariadores.ver", PermisoInterceptor.permisoRequerido("/api/inventariadores", "GET"));
        assertEquals("reportes.ver", PermisoInterceptor.permisoRequerido("/api/reportes/altas-bajas", "GET"));
    }

    @Test
    void rutasSinPermisoOMetodosNoRestringidos() {
        assertNull(PermisoInterceptor.permisoRequerido("/api/superadmin/roles", "GET"));
        assertNull(PermisoInterceptor.permisoRequerido("/api/inventariador-v2/sincronizar", "POST"));
        assertNull(PermisoInterceptor.permisoRequerido("/api/auth/login", "POST"));
        assertNull(PermisoInterceptor.permisoRequerido("/api/activos", "OPTIONS"));
    }
}
