package com.github.heikyudev.maestrocervecero.service.implementation.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FallaEquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FermentadorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.LimpiezaEquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MantenimientoEquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MolinoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.OllaHervorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoOperacionEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IFallaEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.ILimpiezaEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMantenimientoEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IOperacionCicloVidaEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEtapaLoteRepository;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.BloqueLineaTiempoEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoResumenResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.OperacionEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.OperacionesEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.TipoBloqueLineaTiempo;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControlEquipamientoServicioImplTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 1, 15, 10, 0);
    private static final LocalDateTime FIN = LocalDateTime.of(2026, 1, 15, 12, 0);

    @Mock
    private IEquipamientoRepository equipamientoRepository;
    @Mock
    private ILimpiezaEquipamientoRepository limpiezaEquipamientoRepository;
    @Mock
    private IFallaEquipamientoRepository fallaEquipamientoRepository;
    @Mock
    private IMantenimientoEquipamientoRepository mantenimientoEquipamientoRepository;
    @Mock
    private IOperacionCicloVidaEquipamientoRepository operacionCicloVidaEquipamientoRepository;
    @Mock
    private IEtapaLoteRepository etapaLoteRepository;

    @InjectMocks
    private ControlEquipamientoServicioImpl controlEquipamientoServicio;

    // ==================== filtrarEquipamientosParaControl ====================

    @Test
    @DisplayName("CP-CE-01: filtrarEquipamientosParaControl arma los datos de control de la página con una consulta de usos y una de últimas operaciones")
    void filtrar_debeArmarLosDatosDeControlConConsultasPorLista() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        MolinoEntity molino = molino(1L, EstadoOperativo.DISPONIBLE);
        MaceradorEntity macerador = macerador(2L, EstadoOperativo.EN_USO);
        LimpiezaEquipamientoEntity ultimaDelMolino = limpieza(10L, EstadoTransaccion.REGISTRADO, molino, FIN);
        when(equipamientoRepository.filtrarEquipamientos("MOL", MolinoEntity.class, EstadoOperativo.DISPONIBLE, pageable))
                .thenReturn(new PageImpl<>(List.of(molino, macerador)));
        when(limpiezaEquipamientoRepository.contarUsosDesdeUltimoMantenimiento(List.of(1L, 2L)))
                .thenReturn(Collections.singletonList(new Object[]{1L, 7L}));
        when(operacionCicloVidaEquipamientoRepository.buscarUltimasOperacionesRegistradas(List.of(1L, 2L)))
                .thenReturn(List.of(ultimaDelMolino));

        // === EJECUCION ===
        Page<EquipamientoControlResponseDTO> resultado = controlEquipamientoServicio.filtrarEquipamientosParaControl("MOL", TipoEquipamiento.MOLINO, EstadoOperativo.DISPONIBLE, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(2);
        EquipamientoControlResponseDTO dtoMolino = resultado.getContent().get(0);
        assertThat(dtoMolino.getId()).isEqualTo(1L);
        assertThat(dtoMolino.getIdentificadorInterno()).isEqualTo("MOL-1");
        assertThat(dtoMolino.getTipo()).isEqualTo(TipoEquipamiento.MOLINO);
        assertThat(dtoMolino.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(dtoMolino.getUsosDesdeUltimoMantenimiento()).isEqualTo(7L);
        assertThat(dtoMolino.getUsosMaximosAntesMantenimiento()).isEqualTo(100);
        assertThat(dtoMolino.getTipoUltimaOperacion()).isEqualTo(TipoOperacionEquipamiento.LIMPIEZA);
        assertThat(dtoMolino.getFechaUltimaOperacion()).isEqualTo(FIN);
        // El macerador no tiene usos ni operaciones registradas
        EquipamientoControlResponseDTO dtoMacerador = resultado.getContent().get(1);
        assertThat(dtoMacerador.getTipo()).isEqualTo(TipoEquipamiento.MACERADOR);
        assertThat(dtoMacerador.getUsosDesdeUltimoMantenimiento()).isZero();
        assertThat(dtoMacerador.getTipoUltimaOperacion()).isNull();
        assertThat(dtoMacerador.getFechaUltimaOperacion()).isNull();
        verify(limpiezaEquipamientoRepository).contarUsosDesdeUltimoMantenimiento(List.of(1L, 2L));
        verify(operacionCicloVidaEquipamientoRepository).buscarUltimasOperacionesRegistradas(List.of(1L, 2L));
    }

    @Test
    @DisplayName("CP-CE-02: filtrarEquipamientosParaControl no consulta usos ni operaciones cuando la página está vacía")
    void filtrar_noDebeConsultarUsosNiOperacionesConPaginaVacia() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(equipamientoRepository.filtrarEquipamientos(null, null, null, pageable)).thenReturn(new PageImpl<>(List.of()));

        // === EJECUCION ===
        Page<EquipamientoControlResponseDTO> resultado = controlEquipamientoServicio.filtrarEquipamientosParaControl(null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verifyNoInteractions(limpiezaEquipamientoRepository, operacionCicloVidaEquipamientoRepository);
    }

    @Test
    @DisplayName("CP-CE-03: filtrarEquipamientosParaControl normaliza el identificador buscado y un identificador de solo espacios no filtra")
    void filtrar_debeNormalizarElIdentificador() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(equipamientoRepository.filtrarEquipamientos("MOL 01", null, null, pageable)).thenReturn(new PageImpl<>(List.of()));
        when(equipamientoRepository.filtrarEquipamientos(null, null, null, pageable)).thenReturn(new PageImpl<>(List.of()));

        // === EJECUCION ===
        controlEquipamientoServicio.filtrarEquipamientosParaControl("  mol   01  ", null, null, pageable);
        controlEquipamientoServicio.filtrarEquipamientosParaControl("    ", null, null, pageable);

        // === ASSERTS ===
        verify(equipamientoRepository).filtrarEquipamientos("MOL 01", null, null, pageable);
        verify(equipamientoRepository).filtrarEquipamientos(null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-CE-04: filtrarEquipamientosParaControl toma como última operación la de mayor ID ante un empate de fecha de fin")
    void filtrar_debeDesempatarPorElMayorId() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        MolinoEntity molino = molino(1L, EstadoOperativo.EN_MANTENIMIENTO);
        FallaEquipamientoEntity falla = falla(20L, EstadoTransaccion.REGISTRADO, molino, FIN);
        LimpiezaEquipamientoEntity limpieza = limpieza(10L, EstadoTransaccion.REGISTRADO, molino, FIN);
        when(equipamientoRepository.filtrarEquipamientos(null, null, null, pageable)).thenReturn(new PageImpl<>(List.of(molino)));
        when(operacionCicloVidaEquipamientoRepository.buscarUltimasOperacionesRegistradas(List.of(1L))).thenReturn(List.of(falla, limpieza));

        // === EJECUCION ===
        Page<EquipamientoControlResponseDTO> resultado = controlEquipamientoServicio.filtrarEquipamientosParaControl(null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent().get(0).getTipoUltimaOperacion()).isEqualTo(TipoOperacionEquipamiento.FALLA);
    }

    // ==================== buscarResumen ====================

    @Test
    @DisplayName("CP-CE-05: buscarResumen de un molino informa su rendimiento, sus usos y las fechas de su última limpieza y último mantenimiento")
    void buscarResumen_debeResumirUnMolino() {
        // === PREPARACION DE DATOS ===
        MolinoEntity molino = molino(1L, EstadoOperativo.DISPONIBLE);
        LocalDateTime finMantenimiento = INICIO.minusDays(10);
        when(equipamientoRepository.findById(1L)).thenReturn(Optional.of(molino));
        when(mantenimientoEquipamientoRepository.buscarFechaUltimoMantenimientoRegistrado(1L)).thenReturn(Optional.of(finMantenimiento));
        when(limpiezaEquipamientoRepository.buscarFechaUltimaLimpiezaRegistrada(1L)).thenReturn(Optional.of(FIN));
        when(limpiezaEquipamientoRepository.contarLimpiezasRegistradasDesde(1L, finMantenimiento)).thenReturn(4L);

        // === EJECUCION ===
        EquipamientoResumenResponseDTO resultado = controlEquipamientoServicio.buscarResumen(1L);

        // === ASSERTS ===
        assertThat(resultado.getTipo()).isEqualTo(TipoEquipamiento.MOLINO);
        assertThat(resultado.getIdentificadorInterno()).isEqualTo("MOL-1");
        assertThat(resultado.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(resultado.getUsosMaximosAntesMantenimiento()).isEqualTo(100);
        assertThat(resultado.getUsosDesdeUltimoMantenimiento()).isEqualTo(4L);
        assertThat(resultado.getFechaUltimaLimpieza()).isEqualTo(FIN);
        assertThat(resultado.getFechaUltimoMantenimiento()).isEqualTo(finMantenimiento);
        assertThat(resultado.getRendimientoMolienda()).isEqualTo(120.0);
        assertThat(resultado.getCapacidadTotal()).isNull();
        assertThat(resultado.getEvaporacion()).isNull();
    }

    @Test
    @DisplayName("CP-CE-06: buscarResumen de un macerador informa sus capacidades, espacio muerto y eficiencia de maceración")
    void buscarResumen_debeResumirUnMacerador() {
        // === PREPARACION DE DATOS ===
        when(equipamientoRepository.findById(2L)).thenReturn(Optional.of(macerador(2L, EstadoOperativo.EN_LIMPIEZA)));

        // === EJECUCION ===
        EquipamientoResumenResponseDTO resultado = controlEquipamientoServicio.buscarResumen(2L);

        // === ASSERTS ===
        assertThat(resultado.getTipo()).isEqualTo(TipoEquipamiento.MACERADOR);
        assertThat(resultado.getCapacidadTotal()).isEqualTo(500.0);
        assertThat(resultado.getCapacidadUtil()).isEqualTo(450.0);
        assertThat(resultado.getEspacioMuerto()).isEqualTo(10.0);
        assertThat(resultado.getEficienciaMaceracion()).isEqualTo(0.75);
        assertThat(resultado.getRendimientoMolienda()).isNull();
    }

    @Test
    @DisplayName("CP-CE-07: buscarResumen de una olla de hervor informa sus capacidades, evaporación y pérdida por trub")
    void buscarResumen_debeResumirUnaOllaDeHervor() {
        // === PREPARACION DE DATOS ===
        OllaHervorEntity olla = OllaHervorEntity.builder()
                .id(3L).identificadorInterno("OLL-3").estadoOperativo(EstadoOperativo.DISPONIBLE)
                .usosMaximosAntesMantenimiento(100).estado(Estado.ACTIVO)
                .capacidadTotal(300.0).capacidadUtil(250.0).evaporacion(8.0).perdidaPorTrub(3.0)
                .build();
        when(equipamientoRepository.findById(3L)).thenReturn(Optional.of(olla));

        // === EJECUCION ===
        EquipamientoResumenResponseDTO resultado = controlEquipamientoServicio.buscarResumen(3L);

        // === ASSERTS ===
        assertThat(resultado.getTipo()).isEqualTo(TipoEquipamiento.OLLA_HERVOR);
        assertThat(resultado.getCapacidadTotal()).isEqualTo(300.0);
        assertThat(resultado.getCapacidadUtil()).isEqualTo(250.0);
        assertThat(resultado.getEvaporacion()).isEqualTo(8.0);
        assertThat(resultado.getPerdidaPorTrub()).isEqualTo(3.0);
        assertThat(resultado.getEspacioMuerto()).isNull();
    }

    @Test
    @DisplayName("CP-CE-08: buscarResumen de un fermentador sin limpiezas ni mantenimientos informa fechas nulas, sus capacidades y cero usos")
    void buscarResumen_debeResumirUnFermentadorSinHistorial() {
        // === PREPARACION DE DATOS ===
        FermentadorEntity fermentador = FermentadorEntity.builder()
                .id(4L).identificadorInterno("FER-4").estadoOperativo(EstadoOperativo.DISPONIBLE)
                .usosMaximosAntesMantenimiento(100).estado(Estado.ACTIVO)
                .capacidadTotal(1000.0).capacidadUtil(900.0)
                .build();
        when(equipamientoRepository.findById(4L)).thenReturn(Optional.of(fermentador));

        // === EJECUCION ===
        EquipamientoResumenResponseDTO resultado = controlEquipamientoServicio.buscarResumen(4L);

        // === ASSERTS ===
        assertThat(resultado.getTipo()).isEqualTo(TipoEquipamiento.FERMENTADOR);
        assertThat(resultado.getCapacidadTotal()).isEqualTo(1000.0);
        assertThat(resultado.getCapacidadUtil()).isEqualTo(900.0);
        assertThat(resultado.getUsosDesdeUltimoMantenimiento()).isZero();
        assertThat(resultado.getFechaUltimaLimpieza()).isNull();
        assertThat(resultado.getFechaUltimoMantenimiento()).isNull();
        verify(limpiezaEquipamientoRepository).contarLimpiezasRegistradasDesde(4L, null);
    }

    @Test
    @DisplayName("CP-CE-09: buscarResumen lanza RecursoNoEncontradoException cuando el equipamiento no existe")
    void buscarResumen_debeRechazarInexistente() {
        // === PREPARACION DE DATOS ===
        when(equipamientoRepository.findById(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> controlEquipamientoServicio.buscarResumen(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el equipamiento con ID: 99");
    }

    @Test
    @DisplayName("CP-CE-10: buscarResumen lanza RecursoNoEncontradoException cuando el equipamiento está dado de baja")
    void buscarResumen_debeRechazarDadoDeBaja() {
        // === PREPARACION DE DATOS ===
        MolinoEntity molino = molino(1L, EstadoOperativo.DISPONIBLE);
        molino.setEstado(Estado.BAJA);
        when(equipamientoRepository.findById(1L)).thenReturn(Optional.of(molino));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> controlEquipamientoServicio.buscarResumen(1L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el equipamiento con ID: 1");
    }

    // ==================== listarOperaciones ====================

    @Test
    @DisplayName("CP-CE-11: listarOperaciones agrupa por tipo, incluye las anuladas y marca como anulable solo la última operación registrada")
    void listarOperaciones_debeAgruparYMarcarSoloLaUltimaComoAnulable() {
        // === PREPARACION DE DATOS ===
        MolinoEntity molino = molino(1L, EstadoOperativo.DISPONIBLE);
        LimpiezaEquipamientoEntity limpiezaUltima = limpieza(30L, EstadoTransaccion.REGISTRADO, molino, FIN.plusDays(2));
        LimpiezaEquipamientoEntity limpiezaAnulada = limpieza(20L, EstadoTransaccion.ANULADO, molino, FIN.plusDays(2));
        limpiezaAnulada.setFechaAnulacion(FIN.plusDays(3));
        limpiezaAnulada.setMotivoAnulacion("Carga por error");
        FallaEquipamientoEntity falla = falla(10L, EstadoTransaccion.REGISTRADO, molino, FIN);
        MantenimientoEquipamientoEntity mantenimiento = mantenimiento(15L, EstadoTransaccion.REGISTRADO, molino, FIN.plusDays(1));
        when(equipamientoRepository.existsById(1L)).thenReturn(true);
        when(operacionCicloVidaEquipamientoRepository.buscarIdUltimaOperacionRegistrada(1L)).thenReturn(Optional.of(30L));
        when(limpiezaEquipamientoRepository.listarPorEquipamiento(1L)).thenReturn(List.of(limpiezaUltima, limpiezaAnulada));
        when(fallaEquipamientoRepository.listarPorEquipamiento(1L)).thenReturn(List.of(falla));
        when(mantenimientoEquipamientoRepository.listarPorEquipamiento(1L)).thenReturn(List.of(mantenimiento));

        // === EJECUCION ===
        OperacionesEquipamientoResponseDTO resultado = controlEquipamientoServicio.listarOperaciones(1L);

        // === ASSERTS ===
        assertThat(resultado.getLimpiezas()).hasSize(2);
        assertThat(resultado.getFallas()).hasSize(1);
        assertThat(resultado.getMantenimientos()).hasSize(1);

        OperacionEquipamientoResponseDTO ultima = resultado.getLimpiezas().get(0);
        assertThat(ultima.getId()).isEqualTo(30L);
        assertThat(ultima.getTipo()).isEqualTo(TipoOperacionEquipamiento.LIMPIEZA);
        assertThat(ultima.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        assertThat(ultima.getFecha()).isEqualTo(FIN.plusDays(2));
        assertThat(ultima.getEstadoOperativoResultante()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(ultima.isAnulable()).isTrue();

        // Una anulada con el mismo fin no es anulable, y conserva los datos de su anulación
        OperacionEquipamientoResponseDTO anulada = resultado.getLimpiezas().get(1);
        assertThat(anulada.isAnulable()).isFalse();
        assertThat(anulada.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(anulada.getFechaAnulacion()).isEqualTo(FIN.plusDays(3));
        assertThat(anulada.getMotivoAnulacion()).isEqualTo("Carga por error");

        assertThat(resultado.getFallas().get(0).isAnulable()).isFalse();
        assertThat(resultado.getFallas().get(0).getTipo()).isEqualTo(TipoOperacionEquipamiento.FALLA);
        assertThat(resultado.getFallas().get(0).getEstadoOperativoResultante()).isNull();
        assertThat(resultado.getMantenimientos().get(0).isAnulable()).isFalse();
    }

    @Test
    @DisplayName("CP-CE-12: listarOperaciones sin operaciones registradas devuelve listas vacías y ninguna anulable")
    void listarOperaciones_debeDevolverListasVaciasSinOperaciones() {
        // === PREPARACION DE DATOS ===
        when(equipamientoRepository.existsById(1L)).thenReturn(true);
        when(operacionCicloVidaEquipamientoRepository.buscarIdUltimaOperacionRegistrada(1L)).thenReturn(Optional.empty());
        when(limpiezaEquipamientoRepository.listarPorEquipamiento(1L)).thenReturn(List.of());
        when(fallaEquipamientoRepository.listarPorEquipamiento(1L)).thenReturn(List.of());
        when(mantenimientoEquipamientoRepository.listarPorEquipamiento(1L)).thenReturn(List.of());

        // === EJECUCION ===
        OperacionesEquipamientoResponseDTO resultado = controlEquipamientoServicio.listarOperaciones(1L);

        // === ASSERTS ===
        assertThat(resultado.getLimpiezas()).isEmpty();
        assertThat(resultado.getFallas()).isEmpty();
        assertThat(resultado.getMantenimientos()).isEmpty();
    }

    @Test
    @DisplayName("CP-CE-24: listarOperaciones marca como anulable una sola operación ante un empate de fecha de fin: la de mayor ID")
    void listarOperaciones_debeDesempatarPorElMayorId() {
        // === PREPARACION DE DATOS ===
        MolinoEntity molino = molino(1L, EstadoOperativo.DISPONIBLE);
        LimpiezaEquipamientoEntity limpieza = limpieza(30L, EstadoTransaccion.REGISTRADO, molino, FIN);
        FallaEquipamientoEntity falla = falla(31L, EstadoTransaccion.REGISTRADO, molino, FIN);
        when(equipamientoRepository.existsById(1L)).thenReturn(true);
        when(operacionCicloVidaEquipamientoRepository.buscarIdUltimaOperacionRegistrada(1L)).thenReturn(Optional.of(31L));
        when(limpiezaEquipamientoRepository.listarPorEquipamiento(1L)).thenReturn(List.of(limpieza));
        when(fallaEquipamientoRepository.listarPorEquipamiento(1L)).thenReturn(List.of(falla));
        when(mantenimientoEquipamientoRepository.listarPorEquipamiento(1L)).thenReturn(List.of());

        // === EJECUCION ===
        OperacionesEquipamientoResponseDTO resultado = controlEquipamientoServicio.listarOperaciones(1L);

        // === ASSERTS ===
        assertThat(resultado.getLimpiezas().get(0).isAnulable()).isFalse();
        assertThat(resultado.getFallas().get(0).isAnulable()).isTrue();
    }

    @Test
    @DisplayName("CP-CE-13: listarOperaciones lanza RecursoNoEncontradoException cuando el equipamiento no existe")
    void listarOperaciones_debeRechazarInexistente() {
        // === PREPARACION DE DATOS ===
        when(equipamientoRepository.existsById(99L)).thenReturn(false);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> controlEquipamientoServicio.listarOperaciones(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el equipamiento con ID: 99");
        verifyNoInteractions(limpiezaEquipamientoRepository, fallaEquipamientoRepository, mantenimientoEquipamientoRepository);
    }

    // ==================== listarLineaDeTiempo ====================

    @Test
    @DisplayName("CP-CE-14: listarLineaDeTiempo combina los bloques de lote y de operaciones registradas ordenados por inicio")
    void listarLineaDeTiempo_debeCombinarYOrdenarPorInicio() {
        // === PREPARACION DE DATOS ===
        MolinoEntity molino = molino(1L, EstadoOperativo.DISPONIBLE);
        LocalDateTime desde = INICIO.minusDays(30);
        LocalDateTime hasta = INICIO.plusDays(30);
        EtapaLoteEntity etapaFinalizada = etapa(5L, "LOT-001", TipoEtapa.MOLIENDA, EstadoEtapaLote.FINALIZADA, INICIO.plusDays(1), INICIO.plusDays(1).plusHours(3), molino);
        LimpiezaEquipamientoEntity limpieza = limpieza(10L, EstadoTransaccion.REGISTRADO, molino, FIN.plusDays(2));
        when(equipamientoRepository.existsById(1L)).thenReturn(true);
        when(etapaLoteRepository.buscarEtapasConUsoRealEnPeriodo(1L, desde, hasta)).thenReturn(List.of(etapaFinalizada));
        when(operacionCicloVidaEquipamientoRepository.buscarRegistradasEnPeriodo(1L, desde, hasta))
                .thenReturn(List.of(falla(9L, EstadoTransaccion.REGISTRADO, molino, FIN), limpieza));

        // === EJECUCION ===
        List<BloqueLineaTiempoEquipamientoResponseDTO> resultado = controlEquipamientoServicio.listarLineaDeTiempo(1L, desde, hasta);

        // === ASSERTS ===
        assertThat(resultado).extracting(BloqueLineaTiempoEquipamientoResponseDTO::getTipo)
                .containsExactly(TipoBloqueLineaTiempo.FALLA, TipoBloqueLineaTiempo.LOTE, TipoBloqueLineaTiempo.LIMPIEZA);
        BloqueLineaTiempoEquipamientoResponseDTO bloqueLote = resultado.get(1);
        assertThat(bloqueLote.getIdReferencia()).isEqualTo(5L);
        assertThat(bloqueLote.getCodigoLote()).isEqualTo("LOT-001");
        assertThat(bloqueLote.getEtapa()).isEqualTo(TipoEtapa.MOLIENDA);
        assertThat(bloqueLote.getEstadoEtapa()).isEqualTo(EstadoEtapaLote.FINALIZADA);
        assertThat(bloqueLote.getFechaInicio()).isEqualTo(INICIO.plusDays(1));
        assertThat(bloqueLote.getFechaFin()).isEqualTo(INICIO.plusDays(1).plusHours(3));
        assertThat(bloqueLote.isFinProvisorio()).isFalse();
        BloqueLineaTiempoEquipamientoResponseDTO bloqueFalla = resultado.get(0);
        assertThat(bloqueFalla.getIdReferencia()).isEqualTo(9L);
        assertThat(bloqueFalla.getCodigoLote()).isNull();
        assertThat(bloqueFalla.getObservaciones()).isEqualTo("Observaciones de prueba");
        assertThat(bloqueFalla.getFechaInicio()).isEqualTo(FIN);
        assertThat(bloqueFalla.getFechaFin()).isEqualTo(FIN);
    }

    @Test
    @DisplayName("CP-CE-15: listarLineaDeTiempo usa el momento de la consulta como fin provisorio de la etapa en curso")
    void listarLineaDeTiempo_debeUsarAhoraComoFinProvisorioDeLaEtapaEnCurso() {
        // === PREPARACION DE DATOS ===
        MolinoEntity molino = molino(1L, EstadoOperativo.EN_USO);
        EtapaLoteEntity enCurso = etapa(6L, "LOT-002", TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, INICIO, null, molino);
        when(equipamientoRepository.existsById(1L)).thenReturn(true);
        when(etapaLoteRepository.buscarEtapasConUsoRealEnPeriodo(1L, null, null)).thenReturn(List.of(enCurso));
        when(operacionCicloVidaEquipamientoRepository.buscarRegistradasEnPeriodo(1L, null, null)).thenReturn(List.of());
        LocalDateTime antes = LocalDateTime.now();

        // === EJECUCION ===
        List<BloqueLineaTiempoEquipamientoResponseDTO> resultado = controlEquipamientoServicio.listarLineaDeTiempo(1L, null, null);

        // === ASSERTS ===
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).isFinProvisorio()).isTrue();
        assertThat(resultado.get(0).getEstadoEtapa()).isEqualTo(EstadoEtapaLote.EN_CURSO);
    }

    @Test
    @DisplayName("CP-CE-16: listarLineaDeTiempo muestra una etapa cancelada sin fecha de finalización como un bloque sin duración")
    void listarLineaDeTiempo_debeMostrarCanceladaSinFinComoBloqueSinDuracion() {
        // === PREPARACION DE DATOS ===
        MolinoEntity molino = molino(1L, EstadoOperativo.DISPONIBLE);
        EtapaLoteEntity cancelada = etapa(7L, "LOT-003", TipoEtapa.MOLIENDA, EstadoEtapaLote.CANCELADA, INICIO, null, molino);
        when(equipamientoRepository.existsById(1L)).thenReturn(true);
        when(etapaLoteRepository.buscarEtapasConUsoRealEnPeriodo(1L, null, null)).thenReturn(List.of(cancelada));
        when(operacionCicloVidaEquipamientoRepository.buscarRegistradasEnPeriodo(1L, null, null)).thenReturn(List.of());

        // === EJECUCION ===
        List<BloqueLineaTiempoEquipamientoResponseDTO> resultado = controlEquipamientoServicio.listarLineaDeTiempo(1L, null, null);

        // === ASSERTS ===
        assertThat(resultado.get(0).isFinProvisorio()).isFalse();
    }

    @Test
    @DisplayName("CP-CE-17: listarLineaDeTiempo descarta la etapa en curso cuando el período empieza después del momento de la consulta")
    void listarLineaDeTiempo_debeDescartarEtapaEnCursoSiElPeriodoEsFuturo() {
        // === PREPARACION DE DATOS ===
        MolinoEntity molino = molino(1L, EstadoOperativo.EN_USO);
        EtapaLoteEntity enCurso = etapa(6L, "LOT-002", TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, INICIO, null, molino);
        LocalDateTime desde = LocalDateTime.now().plusDays(10);
        LocalDateTime hasta = desde.plusDays(5);
        when(equipamientoRepository.existsById(1L)).thenReturn(true);
        when(etapaLoteRepository.buscarEtapasConUsoRealEnPeriodo(1L, desde, hasta)).thenReturn(List.of(enCurso));
        when(operacionCicloVidaEquipamientoRepository.buscarRegistradasEnPeriodo(1L, desde, hasta)).thenReturn(List.of());

        // === EJECUCION ===
        List<BloqueLineaTiempoEquipamientoResponseDTO> resultado = controlEquipamientoServicio.listarLineaDeTiempo(1L, desde, hasta);

        // === ASSERTS ===
        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("CP-CE-18: listarLineaDeTiempo lanza ReglaNegocioException cuando el período termina antes de empezar")
    void listarLineaDeTiempo_debeRechazarPeriodoInvertido() {
        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> controlEquipamientoServicio.listarLineaDeTiempo(1L, INICIO, INICIO.minusDays(1)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha hasta no puede ser anterior a la fecha desde")
                .extracting("campo").isEqualTo("hasta");
        verify(etapaLoteRepository, never()).buscarEtapasConUsoRealEnPeriodo(any(), any(), any());
    }

    @Test
    @DisplayName("CP-CE-19: listarLineaDeTiempo lanza RecursoNoEncontradoException cuando el equipamiento no existe")
    void listarLineaDeTiempo_debeRechazarInexistente() {
        // === PREPARACION DE DATOS ===
        when(equipamientoRepository.existsById(99L)).thenReturn(false);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> controlEquipamientoServicio.listarLineaDeTiempo(99L, null, null))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el equipamiento con ID: 99");
        verifyNoInteractions(etapaLoteRepository, operacionCicloVidaEquipamientoRepository);
    }

    // ==================== helpers de construcción ====================

    private static MolinoEntity molino(Long id, EstadoOperativo estadoOperativo) {
        return MolinoEntity.builder()
                .id(id)
                .identificadorInterno("MOL-" + id)
                .estadoOperativo(estadoOperativo)
                .usosMaximosAntesMantenimiento(100)
                .estado(Estado.ACTIVO)
                .rendimientoMolienda(120.0)
                .build();
    }

    private static MaceradorEntity macerador(Long id, EstadoOperativo estadoOperativo) {
        return MaceradorEntity.builder()
                .id(id)
                .identificadorInterno("MAC-" + id)
                .estadoOperativo(estadoOperativo)
                .usosMaximosAntesMantenimiento(100)
                .estado(Estado.ACTIVO)
                .capacidadTotal(500.0)
                .capacidadUtil(450.0)
                .espacioMuerto(10.0)
                .eficienciaMaceracion(0.75)
                .build();
    }

    private static LimpiezaEquipamientoEntity limpieza(Long id, EstadoTransaccion estado, EquipamientoEntity equipamiento, LocalDateTime fechaFin) {
        return LimpiezaEquipamientoEntity.builder()
                .id(id)
                .fecha(fechaFin)
                .observaciones("Observaciones de prueba")
                .estado(estado)
                .estadoOperativoResultante(EstadoOperativo.DISPONIBLE)
                .equipamiento(equipamiento)
                .build();
    }

    private static FallaEquipamientoEntity falla(Long id, EstadoTransaccion estado, EquipamientoEntity equipamiento, LocalDateTime fechaFin) {
        return FallaEquipamientoEntity.builder()
                .id(id)
                .fecha(fechaFin)
                .observaciones("Observaciones de prueba")
                .estado(estado)
                .equipamiento(equipamiento)
                .build();
    }

    private static MantenimientoEquipamientoEntity mantenimiento(Long id, EstadoTransaccion estado, EquipamientoEntity equipamiento, LocalDateTime fechaFin) {
        return MantenimientoEquipamientoEntity.builder()
                .id(id)
                .fecha(fechaFin)
                .observaciones("Observaciones de prueba")
                .estado(estado)
                .equipamiento(equipamiento)
                .build();
    }

    private static EtapaLoteEntity etapa(Long id, String codigoLote, TipoEtapa tipoEtapa, EstadoEtapaLote estado, LocalDateTime inicio, LocalDateTime fin, EquipamientoEntity equipamiento) {
        return EtapaLoteEntity.builder()
                .id(id)
                .etapa(tipoEtapa)
                .estado(estado)
                .fechaInicio(inicio)
                .fechaFinalizacion(fin)
                .equipamiento(equipamiento)
                .lote(LoteEntity.builder().identificadorInterno(codigoLote).build())
                .build();
    }
}
