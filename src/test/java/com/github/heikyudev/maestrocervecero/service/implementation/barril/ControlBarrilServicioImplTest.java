package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.DespachoBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.DevolucionBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FabricanteBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FallaBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FraccionamientoBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.LimpiezaBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.MantenimientoBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.TipoOperacionBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.cliente.ClienteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EnvasadoLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IDespachoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IDevolucionBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IFallaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IFraccionamientoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.ILimpiezaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IMantenimientoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IOperacionCicloVidaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEnvasadoLoteRepository;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.ActividadBarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilResumenResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.OperacionBarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.TipoActividadBarril;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControlBarrilServicioImplTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 10, 20, 12, 0);

    @Mock
    private IBarrilRepository barrilRepository;
    @Mock
    private ILimpiezaBarrilRepository limpiezaBarrilRepository;
    @Mock
    private IFallaBarrilRepository fallaBarrilRepository;
    @Mock
    private IFraccionamientoBarrilRepository fraccionamientoBarrilRepository;
    @Mock
    private IMantenimientoBarrilRepository mantenimientoBarrilRepository;
    @Mock
    private IDespachoBarrilRepository despachoBarrilRepository;
    @Mock
    private IDevolucionBarrilRepository devolucionBarrilRepository;
    @Mock
    private IOperacionCicloVidaBarrilRepository operacionCicloVidaBarrilRepository;
    @Mock
    private IEnvasadoLoteRepository envasadoLoteRepository;

    @InjectMocks
    private ControlBarrilServicioImpl servicio;

    private static BarrilEntity barril(Long id, String identificador, EstadoOperativoBarril estado) {
        return BarrilEntity.builder().id(id).identificadorInterno(identificador).numeroSerie("SN-" + id).capacidad(50.0).contenidoActual(20.0)
                .estadoOperativo(estado).usosMaximosAntesMantenimiento(10).estado(Estado.ACTIVO)
                .fabricante(FabricanteBarrilEntity.builder().id(1L).nombre("Barrilitos SA").estado(Estado.ACTIVO).build()).build();
    }

    private static LimpiezaBarrilEntity limpieza(Long id, EstadoTransaccion estado, LocalDateTime fecha) {
        return LimpiezaBarrilEntity.builder().id(id).fecha(fecha).observaciones("Limpieza").estado(estado)
                .estadoOperativoResultante(EstadoOperativoBarril.DISPONIBLE).build();
    }

    // ==================== filtrarBarrilesParaControl ====================

    @Test
    @DisplayName("CP-CBS-01: arma los datos de control con una sola consulta de usos para toda la página")
    void filtrar_armaLosDatosConUnaConsultaDeUsos() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        when(barrilRepository.filtrarBarrilesParaControl("BRL", 1L, EstadoOperativoBarril.EN_LIMPIEZA, pageable)).thenReturn(new PageImpl<>(List.of(
                barril(1L, "BRL-01", EstadoOperativoBarril.EN_LIMPIEZA), barril(2L, "BRL-02", EstadoOperativoBarril.EN_LIMPIEZA))));
        when(limpiezaBarrilRepository.contarUsosDesdeUltimoMantenimiento(List.of(1L, 2L))).thenReturn(Collections.singletonList(new Object[]{1L, 7L}));

        // Act
        Page<BarrilControlResponseDTO> pagina = servicio.filtrarBarrilesParaControl("BRL", 1L, EstadoOperativoBarril.EN_LIMPIEZA, pageable);

        // Assert
        assertThat(pagina.getContent()).hasSize(2);
        assertThat(pagina.getContent().get(0).getUsosDesdeUltimoMantenimiento()).isEqualTo(7L);
        assertThat(pagina.getContent().get(0).getUsosMaximosAntesMantenimiento()).isEqualTo(10);
        assertThat(pagina.getContent().get(0).getFabricante()).isEqualTo("Barrilitos SA");
        assertThat(pagina.getContent().get(1).getUsosDesdeUltimoMantenimiento()).isZero();
        verify(limpiezaBarrilRepository, times(1)).contarUsosDesdeUltimoMantenimiento(any());
    }

    @Test
    @DisplayName("CP-CBS-02: una página vacía no consulta los usos")
    void filtrar_paginaVacia() {
        when(barrilRepository.filtrarBarrilesParaControl(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        Page<BarrilControlResponseDTO> pagina = servicio.filtrarBarrilesParaControl(null, null, null, PageRequest.of(0, 20));

        assertThat(pagina.getContent()).isEmpty();
        verify(limpiezaBarrilRepository, never()).contarUsosDesdeUltimoMantenimiento(any());
    }

    @Test
    @DisplayName("CP-CBS-03: el identificador buscado se normaliza igual que el guardado, y uno en blanco no filtra")
    void filtrar_normalizaElIdentificador() {
        when(barrilRepository.filtrarBarrilesParaControl(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        servicio.filtrarBarrilesParaControl("  brl   01  ", null, null, PageRequest.of(0, 20));
        servicio.filtrarBarrilesParaControl("    ", null, null, PageRequest.of(0, 20));

        verify(barrilRepository).filtrarBarrilesParaControl(eq("BRL 01"), isNull(), isNull(), any());
        verify(barrilRepository).filtrarBarrilesParaControl(isNull(), isNull(), isNull(), any());
    }

    @Test
    @DisplayName("CP-CBS-22: los comodines del identificador buscado se escapan para compararse de forma literal")
    void filtrar_escapaLosComodinesDelIdentificador() {
        // Arrange
        when(barrilRepository.filtrarBarrilesParaControl(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        // Act
        servicio.filtrarBarrilesParaControl("_", null, null, PageRequest.of(0, 20));

        // Assert
        verify(barrilRepository).filtrarBarrilesParaControl(eq("!_"), isNull(), isNull(), any());
    }

    // ==================== buscarResumen ====================

    @Test
    @DisplayName("CP-CBS-04: el resumen informa los datos del barril, sus usos y las fechas de su última limpieza y su último mantenimiento")
    void resumen() {
        LocalDateTime mantenimiento = FECHA.minusDays(10);
        when(barrilRepository.findById(1L)).thenReturn(Optional.of(barril(1L, "BRL-01", EstadoOperativoBarril.CON_CERVEZA)));
        when(mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(1L)).thenReturn(Optional.of(mantenimiento));
        when(limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(1L)).thenReturn(Optional.of(FECHA));
        when(limpiezaBarrilRepository.contarLimpiezasRegistradasDesde(1L, mantenimiento)).thenReturn(4L);

        BarrilResumenResponseDTO resumen = servicio.buscarResumen(1L);

        assertThat(resumen.getIdentificadorInterno()).isEqualTo("BRL-01");
        assertThat(resumen.getNumeroSerie()).isEqualTo("SN-1");
        assertThat(resumen.getFabricante()).isEqualTo("Barrilitos SA");
        assertThat(resumen.getCapacidad()).isEqualTo(50.0);
        assertThat(resumen.getContenidoActual()).isEqualTo(20.0);
        assertThat(resumen.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.CON_CERVEZA);
        assertThat(resumen.getUsosDesdeUltimoMantenimiento()).isEqualTo(4L);
        assertThat(resumen.getUsosMaximosAntesMantenimiento()).isEqualTo(10);
        assertThat(resumen.getFechaUltimaLimpieza()).isEqualTo(FECHA);
        assertThat(resumen.getFechaUltimoMantenimiento()).isEqualTo(mantenimiento);
    }

    @Test
    @DisplayName("CP-CBS-05: un barril sin limpiezas ni mantenimientos informa 0 usos y ambas fechas nulas, y cuenta las limpiezas sin fecha de corte")
    void resumen_sinOperaciones() {
        when(barrilRepository.findById(1L)).thenReturn(Optional.of(barril(1L, "BRL-01", EstadoOperativoBarril.DISPONIBLE)));
        when(mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(1L)).thenReturn(Optional.empty());
        when(limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(1L)).thenReturn(Optional.empty());
        when(limpiezaBarrilRepository.contarLimpiezasRegistradasDesde(1L, null)).thenReturn(0L);

        BarrilResumenResponseDTO resumen = servicio.buscarResumen(1L);

        assertThat(resumen.getUsosDesdeUltimoMantenimiento()).isZero();
        assertThat(resumen.getFechaUltimaLimpieza()).isNull();
        assertThat(resumen.getFechaUltimoMantenimiento()).isNull();
    }

    @Test
    @DisplayName("CP-CBS-06: un barril inexistente o dado de baja lanza RecursoNoEncontradoException")
    void resumen_inexistente() {
        when(barrilRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.buscarResumen(99L)).isInstanceOf(RecursoNoEncontradoException.class).hasMessage("No se encontró el barril con ID: 99");
    }

    // ==================== listarOperaciones ====================

    @Test
    @DisplayName("CP-CBS-07: solo la última operación registrada del barril es anulable; la anulada y las anteriores no")
    void operaciones_soloLaUltimaEsAnulable() {
        when(barrilRepository.findById(1L)).thenReturn(Optional.of(barril(1L, "BRL-01", EstadoOperativoBarril.DISPONIBLE)));
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(1L)).thenReturn(Optional.of(9L));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.empty());
        LimpiezaBarrilEntity ultima = limpieza(9L, EstadoTransaccion.REGISTRADO, FECHA);
        ultima.setCreatedBy("operario1");
        when(limpiezaBarrilRepository.listarPorBarril(1L)).thenReturn(List.of(ultima, limpieza(8L, EstadoTransaccion.ANULADO, FECHA.minusDays(1)), limpieza(7L, EstadoTransaccion.REGISTRADO, FECHA.minusDays(2))));

        List<OperacionBarrilResponseDTO> operaciones = servicio.listarOperaciones(1L, TipoOperacionBarril.LIMPIEZA);

        assertThat(operaciones).extracting(OperacionBarrilResponseDTO::getId).containsExactly(9L, 8L, 7L);
        assertThat(operaciones).extracting(OperacionBarrilResponseDTO::isAnulable).containsExactly(true, false, false);
        assertThat(operaciones.get(0).getRegistradoPor()).isEqualTo("operario1");
        assertThat(operaciones.get(0).getTipo()).isEqualTo(TipoOperacionBarril.LIMPIEZA);
        assertThat(operaciones.get(0).getEstadoOperativoResultante()).isEqualTo(EstadoOperativoBarril.DISPONIBLE);
    }

    @Test
    @DisplayName("CP-CBS-08: con el barril EN_USO ninguna operación es anulable")
    void operaciones_enUsoNoAnulable() {
        when(barrilRepository.findById(1L)).thenReturn(Optional.of(barril(1L, "BRL-01", EstadoOperativoBarril.EN_USO)));
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(1L)).thenReturn(Optional.of(9L));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.empty());
        when(limpiezaBarrilRepository.listarPorBarril(1L)).thenReturn(List.of(limpieza(9L, EstadoTransaccion.REGISTRADO, FECHA)));

        assertThat(servicio.listarOperaciones(1L, TipoOperacionBarril.LIMPIEZA)).extracting(OperacionBarrilResponseDTO::isAnulable).containsExactly(false);
    }

    @Test
    @DisplayName("CP-CBS-09: una operación que no es posterior al último envasado no es anulable, aunque sea la última")
    void operaciones_noPosteriorAlEnvasado() {
        when(barrilRepository.findById(1L)).thenReturn(Optional.of(barril(1L, "BRL-01", EstadoOperativoBarril.DISPONIBLE)));
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(1L)).thenReturn(Optional.of(9L));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.of(FECHA));
        when(limpiezaBarrilRepository.listarPorBarril(1L)).thenReturn(List.of(limpieza(9L, EstadoTransaccion.REGISTRADO, FECHA)));

        assertThat(servicio.listarOperaciones(1L, TipoOperacionBarril.LIMPIEZA)).extracting(OperacionBarrilResponseDTO::isAnulable).containsExactly(false);
    }

    @Test
    @DisplayName("CP-CBS-10: lista el tipo pedido y mapea sus datos propios: cantidad extraída, cliente con fecha estimada y litros descartados")
    void operaciones_datosPropiosDeCadaTipo() {
        when(barrilRepository.findById(1L)).thenReturn(Optional.of(barril(1L, "BRL-01", EstadoOperativoBarril.DISPONIBLE)));
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(1L)).thenReturn(Optional.empty());
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.empty());
        when(fraccionamientoBarrilRepository.listarPorBarril(1L)).thenReturn(List.of(FraccionamientoBarrilEntity.builder().id(1L).fecha(FECHA).observaciones("Muestra")
                .estado(EstadoTransaccion.REGISTRADO).cantidadFraccionada(5.5).estadoOperativoResultante(EstadoOperativoBarril.CON_CERVEZA).build()));
        when(despachoBarrilRepository.listarPorBarril(1L)).thenReturn(List.of(DespachoBarrilEntity.builder().id(2L).fecha(FECHA).observaciones("Entrega")
                .estado(EstadoTransaccion.REGISTRADO).fechaDevolucionEstimada(LocalDate.of(2026, 11, 30)).cliente(ClienteEntity.builder().id(3L).nombre("Bar El Puerto").build()).build()));
        when(devolucionBarrilRepository.listarPorBarril(1L)).thenReturn(List.of(DevolucionBarrilEntity.builder().id(3L).fecha(FECHA).observaciones("Devuelto")
                .estado(EstadoTransaccion.REGISTRADO).litrosDescartados(12.0).build()));

        OperacionBarrilResponseDTO fraccionamiento = servicio.listarOperaciones(1L, TipoOperacionBarril.FRACCIONAMIENTO).get(0);
        OperacionBarrilResponseDTO despacho = servicio.listarOperaciones(1L, TipoOperacionBarril.DESPACHO).get(0);
        OperacionBarrilResponseDTO devolucion = servicio.listarOperaciones(1L, TipoOperacionBarril.DEVOLUCION).get(0);

        assertThat(fraccionamiento.getCantidadExtraida()).isEqualTo(5.5);
        assertThat(fraccionamiento.getEstadoOperativoResultante()).isEqualTo(EstadoOperativoBarril.CON_CERVEZA);
        assertThat(despacho.getCliente()).isEqualTo("Bar El Puerto");
        assertThat(despacho.getFechaDevolucionEstimada()).isEqualTo(LocalDate.of(2026, 11, 30));
        assertThat(devolucion.getLitrosDescartados()).isEqualTo(12.0);
        verifyNoInteractions(fallaBarrilRepository, mantenimientoBarrilRepository, limpiezaBarrilRepository);
    }

    @Test
    @DisplayName("CP-CBS-11: las fallas y los mantenimientos se listan con sus propios repositorios")
    void operaciones_fallasYMantenimientos() {
        when(barrilRepository.findById(1L)).thenReturn(Optional.of(barril(1L, "BRL-01", EstadoOperativoBarril.DISPONIBLE)));
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(1L)).thenReturn(Optional.empty());
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.empty());
        when(fallaBarrilRepository.listarPorBarril(1L)).thenReturn(List.of(FallaBarrilEntity.builder().id(4L).fecha(FECHA).observaciones("Falla").estado(EstadoTransaccion.REGISTRADO).build()));
        when(mantenimientoBarrilRepository.listarPorBarril(1L)).thenReturn(List.of(MantenimientoBarrilEntity.builder().id(5L).fecha(FECHA).observaciones("Mant").estado(EstadoTransaccion.REGISTRADO).build()));

        assertThat(servicio.listarOperaciones(1L, TipoOperacionBarril.FALLA)).extracting(OperacionBarrilResponseDTO::getTipo).containsExactly(TipoOperacionBarril.FALLA);
        assertThat(servicio.listarOperaciones(1L, TipoOperacionBarril.MANTENIMIENTO)).extracting(OperacionBarrilResponseDTO::getTipo).containsExactly(TipoOperacionBarril.MANTENIMIENTO);
    }

    @Test
    @DisplayName("CP-CBS-12: listar las operaciones de un barril inexistente lanza RecursoNoEncontradoException")
    void operaciones_barrilInexistente() {
        when(barrilRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.listarOperaciones(99L, TipoOperacionBarril.LIMPIEZA)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ==================== listarHistorial ====================

    private static EnvasadoLoteEntity envasado(Long id, EstadoLote estadoLote, LocalDateTime fecha) {
        LoteEntity lote = LoteEntity.builder().id(12L).identificadorInterno("LOT-R1-01").estado(estadoLote).build();
        return EnvasadoLoteEntity.builder().id(id).fechaEnvasado(fecha).estado(EstadoTransaccion.REGISTRADO).etapaLote(EtapaLoteEntity.builder().id(1L).lote(lote).build()).build();
    }

    @Test
    @DisplayName("CP-CBS-13: el historial une envasados y operaciones, de la más reciente a la más antigua, con el estado resultante de cada una")
    void historial_unionOrdenadaConEstadoResultante() {
        when(barrilRepository.findById(1L)).thenReturn(Optional.of(barril(1L, "BRL-01", EstadoOperativoBarril.DISPONIBLE)));
        when(envasadoLoteRepository.buscarRegistradosDelBarrilEnPeriodo(1L, null, null)).thenReturn(List.of(envasado(1L, EstadoLote.FINALIZADO, FECHA.minusDays(5)), envasado(2L, EstadoLote.EN_EJECUCION, FECHA.minusDays(9))));
        when(operacionCicloVidaBarrilRepository.buscarRegistradasEnPeriodo(1L, null, null)).thenReturn(List.of(
                DevolucionBarrilEntity.builder().id(30L).fecha(FECHA).observaciones("Devuelto").estado(EstadoTransaccion.REGISTRADO).build(),
                DespachoBarrilEntity.builder().id(29L).fecha(FECHA.minusDays(1)).observaciones("Entrega").estado(EstadoTransaccion.REGISTRADO).build(),
                FraccionamientoBarrilEntity.builder().id(28L).fecha(FECHA.minusDays(2)).observaciones("Muestra").estado(EstadoTransaccion.REGISTRADO)
                        .estadoOperativoResultante(EstadoOperativoBarril.EN_LIMPIEZA).build(),
                FallaBarrilEntity.builder().id(27L).fecha(FECHA.minusDays(3)).observaciones("Falla").estado(EstadoTransaccion.REGISTRADO).build(),
                MantenimientoBarrilEntity.builder().id(26L).fecha(FECHA.minusDays(4)).observaciones("Mant").estado(EstadoTransaccion.REGISTRADO).build(),
                LimpiezaBarrilEntity.builder().id(25L).fecha(FECHA.minusDays(6)).observaciones("Limp").estado(EstadoTransaccion.REGISTRADO)
                        .estadoOperativoResultante(EstadoOperativoBarril.EN_MANTENIMIENTO).build()));

        Page<ActividadBarrilResponseDTO> pagina = servicio.listarHistorial(1L, null, null, null, PageRequest.of(0, 20));

        assertThat(pagina.getContent()).extracting(ActividadBarrilResponseDTO::getTipo).containsExactly(TipoActividadBarril.DEVOLUCION, TipoActividadBarril.DESPACHO,
                TipoActividadBarril.FRACCIONAMIENTO, TipoActividadBarril.FALLA, TipoActividadBarril.MANTENIMIENTO, TipoActividadBarril.ENVASADO,
                TipoActividadBarril.LIMPIEZA, TipoActividadBarril.ENVASADO);
        assertThat(pagina.getContent()).extracting(ActividadBarrilResponseDTO::getEstadoOperativoResultante).containsExactly(
                EstadoOperativoBarril.EN_LIMPIEZA, EstadoOperativoBarril.DESPACHADO, EstadoOperativoBarril.EN_LIMPIEZA, EstadoOperativoBarril.EN_MANTENIMIENTO,
                EstadoOperativoBarril.DISPONIBLE, EstadoOperativoBarril.CON_CERVEZA, EstadoOperativoBarril.EN_MANTENIMIENTO, EstadoOperativoBarril.EN_USO);
        ActividadBarrilResponseDTO envasadoFinalizado = pagina.getContent().get(5);
        assertThat(envasadoFinalizado.getCodigoLote()).isEqualTo("LOT-R1-01");
        assertThat(envasadoFinalizado.getIdLote()).isEqualTo(12L);
        assertThat(envasadoFinalizado.getFecha()).isEqualTo(FECHA.minusDays(5));
        verify(envasadoLoteRepository, times(1)).buscarRegistradosDelBarrilEnPeriodo(any(), any(), any());
        verify(operacionCicloVidaBarrilRepository, times(1)).buscarRegistradasEnPeriodo(any(), any(), any());
    }

    @Test
    @DisplayName("CP-CBS-14: el filtro por Envasado no consulta las operaciones y el filtro por otro tipo no consulta los envasados")
    void historial_filtroPorTipo() {
        when(barrilRepository.findById(1L)).thenReturn(Optional.of(barril(1L, "BRL-01", EstadoOperativoBarril.DISPONIBLE)));
        when(envasadoLoteRepository.buscarRegistradosDelBarrilEnPeriodo(any(), any(), any())).thenReturn(List.of(envasado(1L, EstadoLote.EN_EJECUCION, FECHA)));
        when(operacionCicloVidaBarrilRepository.buscarRegistradasEnPeriodo(any(), any(), any())).thenReturn(List.of(
                FallaBarrilEntity.builder().id(27L).fecha(FECHA).observaciones("Falla").estado(EstadoTransaccion.REGISTRADO).build(),
                DevolucionBarrilEntity.builder().id(30L).fecha(FECHA).observaciones("Devuelto").estado(EstadoTransaccion.REGISTRADO).build()));

        Page<ActividadBarrilResponseDTO> soloEnvasados = servicio.listarHistorial(1L, TipoActividadBarril.ENVASADO, null, null, PageRequest.of(0, 20));
        verify(operacionCicloVidaBarrilRepository, never()).buscarRegistradasEnPeriodo(any(), any(), any());
        Page<ActividadBarrilResponseDTO> soloFallas = servicio.listarHistorial(1L, TipoActividadBarril.FALLA, null, null, PageRequest.of(0, 20));

        assertThat(soloEnvasados.getContent()).extracting(ActividadBarrilResponseDTO::getTipo).containsExactly(TipoActividadBarril.ENVASADO);
        assertThat(soloFallas.getContent()).extracting(ActividadBarrilResponseDTO::getTipo).containsExactly(TipoActividadBarril.FALLA);
        verify(envasadoLoteRepository, times(1)).buscarRegistradosDelBarrilEnPeriodo(any(), any(), any());
    }

    @Test
    @DisplayName("CP-CBS-15: el historial se pagina y conserva el total")
    void historial_paginacion() {
        when(barrilRepository.findById(1L)).thenReturn(Optional.of(barril(1L, "BRL-01", EstadoOperativoBarril.DISPONIBLE)));
        when(envasadoLoteRepository.buscarRegistradosDelBarrilEnPeriodo(any(), any(), any())).thenReturn(List.of());
        List<com.github.heikyudev.maestrocervecero.persistence.entity.barril.OperacionCicloVidaBarril> muchas = new ArrayList<>();
        for (long i = 1; i <= 25; i++) {
            muchas.add(FallaBarrilEntity.builder().id(i).fecha(FECHA.minusHours(i)).observaciones("F").estado(EstadoTransaccion.REGISTRADO).build());
        }
        when(operacionCicloVidaBarrilRepository.buscarRegistradasEnPeriodo(any(), any(), any())).thenReturn(muchas);

        Page<ActividadBarrilResponseDTO> segunda = servicio.listarHistorial(1L, null, null, null, PageRequest.of(1, 20));

        assertThat(segunda.getTotalElements()).isEqualTo(25);
        assertThat(segunda.getContent()).hasSize(5);
    }

    @Test
    @DisplayName("CP-CBS-16: un rango invertido lanza ReglaNegocioException sobre el campo hasta, y un barril inexistente RecursoNoEncontradoException")
    void historial_validaciones() {
        assertThatThrownBy(() -> servicio.listarHistorial(1L, null, FECHA, FECHA.minusDays(1), PageRequest.of(0, 20)))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("La fecha hasta no puede ser anterior a la fecha desde");
        when(barrilRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.listarHistorial(99L, null, null, null, PageRequest.of(0, 20))).isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ==================== Barril dado de baja ====================

    private static BarrilEntity barrilDeBaja() {
        BarrilEntity barril = barril(5L, "BRL-05", EstadoOperativoBarril.DISPONIBLE);
        barril.setEstado(Estado.BAJA);
        return barril;
    }

    @Test
    @DisplayName("CP-CBS-17: el resumen de un barril dado de baja lanza RecursoNoEncontradoException")
    void resumen_barrilDeBaja() {
        when(barrilRepository.findById(5L)).thenReturn(Optional.of(barrilDeBaja()));

        assertThatThrownBy(() -> servicio.buscarResumen(5L)).isInstanceOf(RecursoNoEncontradoException.class).hasMessage("No se encontró el barril con ID: 5");
    }

    @Test
    @DisplayName("CP-CBS-18: listar las operaciones de un barril dado de baja lanza RecursoNoEncontradoException")
    void operaciones_barrilDeBaja() {
        when(barrilRepository.findById(5L)).thenReturn(Optional.of(barrilDeBaja()));

        assertThatThrownBy(() -> servicio.listarOperaciones(5L, TipoOperacionBarril.LIMPIEZA)).isInstanceOf(RecursoNoEncontradoException.class).hasMessage("No se encontró el barril con ID: 5");
    }

    @Test
    @DisplayName("CP-CBS-19: el historial de un barril dado de baja lanza RecursoNoEncontradoException")
    void historial_barrilDeBaja() {
        when(barrilRepository.findById(5L)).thenReturn(Optional.of(barrilDeBaja()));

        assertThatThrownBy(() -> servicio.listarHistorial(5L, null, null, null, PageRequest.of(0, 20))).isInstanceOf(RecursoNoEncontradoException.class).hasMessage("No se encontró el barril con ID: 5");
    }

    // ==================== Estado operativo del barril y anulable ====================

    @Test
    @DisplayName("CP-CBS-20: una operación no es anulable si el barril no está en el estado operativo que ella dejó, aunque sea la última")
    void operaciones_estadoDelBarrilNoCoincide() {
        when(barrilRepository.findById(1L)).thenReturn(Optional.of(barril(1L, "BRL-01", EstadoOperativoBarril.EN_LIMPIEZA)));
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(1L)).thenReturn(Optional.of(9L));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.empty());
        when(limpiezaBarrilRepository.listarPorBarril(1L)).thenReturn(List.of(limpieza(9L, EstadoTransaccion.REGISTRADO, FECHA)));

        assertThat(servicio.listarOperaciones(1L, TipoOperacionBarril.LIMPIEZA)).extracting(OperacionBarrilResponseDTO::isAnulable).containsExactly(false);
    }

    @Test
    @DisplayName("CP-CBS-21: cada tipo es anulable solo con el barril en su estado resultante (falla En mantenimiento, mantenimiento Disponible, despacho Despachado, devolución En limpieza, fraccionamiento el que registró) y no con otro")
    void operaciones_estadoResultanteDeCadaTipo() {
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(1L)).thenReturn(Optional.of(4L));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.empty());
        when(fallaBarrilRepository.listarPorBarril(1L)).thenReturn(List.of(FallaBarrilEntity.builder().id(4L).fecha(FECHA).observaciones("F").estado(EstadoTransaccion.REGISTRADO).build()));
        when(mantenimientoBarrilRepository.listarPorBarril(1L)).thenReturn(List.of(MantenimientoBarrilEntity.builder().id(4L).fecha(FECHA).observaciones("M").estado(EstadoTransaccion.REGISTRADO).build()));
        when(despachoBarrilRepository.listarPorBarril(1L)).thenReturn(List.of(DespachoBarrilEntity.builder().id(4L).fecha(FECHA).observaciones("D").estado(EstadoTransaccion.REGISTRADO)
                .fechaDevolucionEstimada(LocalDate.of(2026, 11, 30)).cliente(ClienteEntity.builder().id(3L).nombre("Bar").build()).build()));
        when(devolucionBarrilRepository.listarPorBarril(1L)).thenReturn(List.of(DevolucionBarrilEntity.builder().id(4L).fecha(FECHA).observaciones("V").estado(EstadoTransaccion.REGISTRADO).build()));
        when(fraccionamientoBarrilRepository.listarPorBarril(1L)).thenReturn(List.of(FraccionamientoBarrilEntity.builder().id(4L).fecha(FECHA).observaciones("X").estado(EstadoTransaccion.REGISTRADO)
                .cantidadFraccionada(1.0).estadoOperativoResultante(EstadoOperativoBarril.EN_LIMPIEZA).build()));

        Object[][] casos = {{TipoOperacionBarril.FALLA, EstadoOperativoBarril.EN_MANTENIMIENTO}, {TipoOperacionBarril.MANTENIMIENTO, EstadoOperativoBarril.DISPONIBLE},
                {TipoOperacionBarril.DESPACHO, EstadoOperativoBarril.DESPACHADO}, {TipoOperacionBarril.DEVOLUCION, EstadoOperativoBarril.EN_LIMPIEZA},
                {TipoOperacionBarril.FRACCIONAMIENTO, EstadoOperativoBarril.EN_LIMPIEZA}};
        for (Object[] caso : casos) {
            when(barrilRepository.findById(1L)).thenReturn(Optional.of(barril(1L, "BRL-01", (EstadoOperativoBarril) caso[1])));
            assertThat(servicio.listarOperaciones(1L, (TipoOperacionBarril) caso[0])).as(caso[0] + " con su estado").extracting(OperacionBarrilResponseDTO::isAnulable).containsExactly(true);
            when(barrilRepository.findById(1L)).thenReturn(Optional.of(barril(1L, "BRL-01", EstadoOperativoBarril.CON_CERVEZA)));
            assertThat(servicio.listarOperaciones(1L, (TipoOperacionBarril) caso[0])).as(caso[0] + " con otro estado").extracting(OperacionBarrilResponseDTO::isAnulable).containsExactly(false);
        }
    }
}
