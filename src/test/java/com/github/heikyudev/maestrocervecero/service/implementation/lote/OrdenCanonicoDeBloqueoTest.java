package com.github.heikyudev.maestrocervecero.service.implementation.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FermentadorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IFermentadorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.ILoteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IPlanificacionProduccionRepository;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdenCanonicoDeBloqueoTest {

    private static final Long ID_LOTE = 7L;
    private static final Long ID_FERMENTADOR = 5L;
    private static final Long ID_PLANIFICACION = 3L;

    @Mock
    private ILoteRepository loteRepository;
    @Mock
    private IPlanificacionProduccionRepository planificacionProduccionRepository;
    @Mock
    private IFermentadorRepository fermentadorRepository;

    @InjectMocks
    private OrdenCanonicoDeBloqueo ordenCanonicoDeBloqueo;

    @Test
    @DisplayName("CP-OCB-01: bloquearLote con planificación bloquea en orden planificación, fermentador, cola pendiente y lote, y devuelve el lote")
    void bloquearLote_conPlanificacionDebeRespetarElOrdenCanonico() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = new LoteEntity();
        lote.setId(ID_LOTE);
        when(loteRepository.buscarIdFermentadorDelLote(ID_LOTE)).thenReturn(Optional.of(ID_FERMENTADOR));
        when(loteRepository.buscarIdPlanificacionDelLote(ID_LOTE)).thenReturn(Optional.of(ID_PLANIFICACION));
        when(planificacionProduccionRepository.buscarPorIdParaActualizar(ID_PLANIFICACION)).thenReturn(Optional.of(new PlanificacionProduccionEntity()));
        when(fermentadorRepository.buscarPorIdParaCambiarEstadoOperativo(ID_FERMENTADOR)).thenReturn(Optional.of(new FermentadorEntity()));
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(ID_FERMENTADOR)).thenReturn(List.of(lote));
        when(loteRepository.buscarPorIdParaActualizar(ID_LOTE)).thenReturn(Optional.of(lote));

        // === EJECUCION ===
        LoteEntity resultado = ordenCanonicoDeBloqueo.bloquearLote(ID_LOTE, true);

        // === ASSERTS ===
        assertThat(resultado).isSameAs(lote);
        InOrder orden = inOrder(planificacionProduccionRepository, fermentadorRepository, loteRepository);
        orden.verify(planificacionProduccionRepository).buscarPorIdParaActualizar(ID_PLANIFICACION);
        orden.verify(fermentadorRepository).buscarPorIdParaCambiarEstadoOperativo(ID_FERMENTADOR);
        orden.verify(loteRepository).buscarPendientesDeFermentadorParaReordenar(ID_FERMENTADOR);
        orden.verify(loteRepository).buscarPorIdParaActualizar(ID_LOTE);
    }

    @Test
    @DisplayName("CP-OCB-02: bloquearLote sin planificación no bloquea ninguna planificación")
    void bloquearLote_sinPlanificacionNoDebeBloquearPlanificaciones() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = new LoteEntity();
        when(loteRepository.buscarIdFermentadorDelLote(ID_LOTE)).thenReturn(Optional.of(ID_FERMENTADOR));
        when(fermentadorRepository.buscarPorIdParaCambiarEstadoOperativo(ID_FERMENTADOR)).thenReturn(Optional.of(new FermentadorEntity()));
        when(loteRepository.buscarPorIdParaActualizar(ID_LOTE)).thenReturn(Optional.of(lote));

        // === EJECUCION ===
        LoteEntity resultado = ordenCanonicoDeBloqueo.bloquearLote(ID_LOTE, false);

        // === ASSERTS ===
        assertThat(resultado).isSameAs(lote);
        verifyNoInteractions(planificacionProduccionRepository);
        InOrder orden = inOrder(fermentadorRepository, loteRepository);
        orden.verify(fermentadorRepository).buscarPorIdParaCambiarEstadoOperativo(ID_FERMENTADOR);
        orden.verify(loteRepository).buscarPendientesDeFermentadorParaReordenar(ID_FERMENTADOR);
        orden.verify(loteRepository).buscarPorIdParaActualizar(ID_LOTE);
    }

    @Test
    @DisplayName("CP-OCB-03: bloquearLote lanza RecursoNoEncontradoException sin bloquear nada cuando el lote no existe")
    void bloquearLote_loteInexistenteDebeLanzarExcepcion() {
        // === PREPARACION DE DATOS ===
        when(loteRepository.buscarIdFermentadorDelLote(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> ordenCanonicoDeBloqueo.bloquearLote(99L, true))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el lote con ID: 99");
        verifyNoInteractions(planificacionProduccionRepository, fermentadorRepository);
    }

    @Test
    @DisplayName("CP-OCB-04: bloquearLote lanza RecursoNoEncontradoException cuando la planificación o el fermentador del lote no existen")
    void bloquearLote_planificacionOFermentadorInexistenteDebeLanzarExcepcion() {
        // === PREPARACION DE DATOS ===
        when(loteRepository.buscarIdFermentadorDelLote(ID_LOTE)).thenReturn(Optional.of(ID_FERMENTADOR));
        when(loteRepository.buscarIdPlanificacionDelLote(ID_LOTE)).thenReturn(Optional.of(ID_PLANIFICACION));
        when(planificacionProduccionRepository.buscarPorIdParaActualizar(ID_PLANIFICACION)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> ordenCanonicoDeBloqueo.bloquearLote(ID_LOTE, true))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la planificación de producción con ID: 3");
        verifyNoInteractions(fermentadorRepository);

        when(fermentadorRepository.buscarPorIdParaCambiarEstadoOperativo(ID_FERMENTADOR)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> ordenCanonicoDeBloqueo.bloquearLote(ID_LOTE, false))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el fermentador con ID: 5");
    }
}
