package com.github.heikyudev.maestrocervecero.service.implementation.orden_compra;

import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.OrdenCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.ProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.VersionProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion.LocalidadEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.repository.orden_compra.IOrdenCompraRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IPlanificacionProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.proveedor.ICatalogoProveedorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.proveedor.IProveedorRepository;
import com.github.heikyudev.maestrocervecero.service.response_dto.orden_compra.OrdenCompraResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdenCompraServicioImplTest {

    @Mock
    private IOrdenCompraRepository ordenCompraRepository;
    @Mock
    private IPlanificacionProduccionRepository planificacionProduccionRepository;
    @Mock
    private IProveedorRepository proveedorRepository;
    @Mock
    private ICatalogoProveedorRepository catalogoProveedorRepository;

    @InjectMocks
    private OrdenCompraServicioImpl ordenCompraServicio;

    // ==================== filtrarOrdenesCompra ====================

    @Test
    @DisplayName("CP-FOC-01: filtrarOrdenesCompra filtra por los 4 criterios informados")
    void filtrar_debeFiltrarPorLosCuatroCriterios() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate fechaEntregaEstimada = LocalDate.of(2026, 1, 15);
        VersionProveedorEntity versionProveedor = crearVersionProveedorEntity(1L, 2L);
        PlanificacionProduccionEntity planificacion = crearPlanificacionProduccionEntity(1L, EstadoSolicitud.PENDIENTE);
        OrdenCompraEntity orden = crearOrdenCompraEntity(1L, EstadoSolicitud.PENDIENTE, fechaEntregaEstimada, planificacion, versionProveedor);
        when(ordenCompraRepository.filtrarOrdenesCompra(1L, 2L, EstadoSolicitud.PENDIENTE, fechaEntregaEstimada, pageable))
                .thenReturn(new PageImpl<>(List.of(orden)));

        // === EJECUCION ===
        Page<OrdenCompraResponseDTO> resultado = ordenCompraServicio.filtrarOrdenesCompra(1L, 2L, EstadoSolicitud.PENDIENTE, fechaEntregaEstimada, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(1);
        verify(ordenCompraRepository).filtrarOrdenesCompra(1L, 2L, EstadoSolicitud.PENDIENTE, fechaEntregaEstimada, pageable);
    }

    @Test
    @DisplayName("CP-FOC-02: filtrarOrdenesCompra con los 4 parámetros nulos no restringe la búsqueda")
    void filtrar_debePropagarCuatroParametrosNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        VersionProveedorEntity versionProveedor = crearVersionProveedorEntity(1L, 2L);
        PlanificacionProduccionEntity planificacion = crearPlanificacionProduccionEntity(1L, EstadoSolicitud.PENDIENTE);
        OrdenCompraEntity ordenPendiente = crearOrdenCompraEntity(1L, EstadoSolicitud.PENDIENTE, LocalDate.now().plusDays(10), planificacion, versionProveedor);
        OrdenCompraEntity ordenFinalizada = crearOrdenCompraEntity(2L, EstadoSolicitud.FINALIZADA, LocalDate.now().plusDays(20), planificacion, versionProveedor);
        OrdenCompraEntity ordenAnulada = crearOrdenCompraEntity(3L, EstadoSolicitud.ANULADA, LocalDate.now().plusDays(30), planificacion, versionProveedor);
        when(ordenCompraRepository.filtrarOrdenesCompra(null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(ordenPendiente, ordenFinalizada, ordenAnulada)));

        // === EJECUCION ===
        Page<OrdenCompraResponseDTO> resultado = ordenCompraServicio.filtrarOrdenesCompra(null, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(3);
        verify(ordenCompraRepository).filtrarOrdenesCompra(null, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FOC-03: filtrarOrdenesCompra retorna una página vacía cuando no hay coincidencias")
    void filtrar_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(ordenCompraRepository.filtrarOrdenesCompra(99L, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        // === EJECUCION ===
        Page<OrdenCompraResponseDTO> resultado = ordenCompraServicio.filtrarOrdenesCompra(99L, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(ordenCompraRepository).filtrarOrdenesCompra(99L, null, null, null, pageable);
    }

    // ==================== helpers de construcción ====================

    private static OrdenCompraEntity crearOrdenCompraEntity(Long id, EstadoSolicitud estado, LocalDate fechaEntregaEstimada,
                                                              PlanificacionProduccionEntity planificacionProduccion, VersionProveedorEntity versionProveedor) {
        return OrdenCompraEntity.builder()
                .id(id)
                .fechaEntregaEstimada(fechaEntregaEstimada)
                .estado(estado)
                .planificacionProduccion(planificacionProduccion)
                .versionProveedor(versionProveedor)
                .build();
    }

    private static PlanificacionProduccionEntity crearPlanificacionProduccionEntity(Long id, EstadoSolicitud estado) {
        return PlanificacionProduccionEntity.builder()
                .id(id)
                .fechaInicioEstimada(LocalDate.now())
                .fechaFinalizacionEstimada(LocalDate.now().plusMonths(1))
                .cantidadAProducir(100.0)
                .estado(estado)
                .versionReceta(crearVersionRecetaEntity(1L))
                .build();
    }

    private static VersionRecetaEntity crearVersionRecetaEntity(Long id) {
        return VersionRecetaEntity.builder()
                .id(id)
                .nombre("Receta Test")
                .esUltimaVersion(true)
                .build();
    }

    private static VersionProveedorEntity crearVersionProveedorEntity(Long id, Long idProveedor) {
        return VersionProveedorEntity.builder()
                .id(id)
                .razonSocial("Proveedor Test")
                .nombreComercial("Proveedor Test")
                .cuit("30-12345678-9")
                .telefono("11-2233-4455")
                .email("contacto@proveedor.com")
                .direccion("Ruta 5 Km 120")
                .esUltimaVersion(true)
                .localidad(crearLocalidadEntity(1L))
                .proveedor(ProveedorEntity.builder().id(idProveedor).estado(Estado.ACTIVO).build())
                .build();
    }

    private static LocalidadEntity crearLocalidadEntity(Long id) {
        return LocalidadEntity.builder().id(id).nombre("San Nicolás").estado(Estado.ACTIVO).build();
    }
}
