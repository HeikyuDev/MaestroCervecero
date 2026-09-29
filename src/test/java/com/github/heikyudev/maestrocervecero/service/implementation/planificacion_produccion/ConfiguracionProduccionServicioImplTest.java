package com.github.heikyudev.maestrocervecero.service.implementation.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.ConfiguracionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.CriterioSeleccionPlanConcurrente;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.CriterioSeleccionPlanSecuencial;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IConfiguracionProduccionRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.ConfiguracionProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.ConfiguracionProduccionResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfiguracionProduccionServicioImplTest {

    @Mock
    private IConfiguracionProduccionRepository configuracionProduccionRepository;

    @InjectMocks
    private ConfiguracionProduccionServicioImpl configuracionProduccionServicio;

    // ==================== buscarConfiguracion ====================

    @Test
    @DisplayName("CP-BC-01: buscarConfiguracion retorna la configuración vigente (camino feliz)")
    void buscar_debeRetornarConfiguracionVigente() {
        // === PREPARACION DE DATOS ===
        ConfiguracionProduccionEntity configuracion = crearConfiguracionEntity(6.0, 12.0, 25.0, 90.0,
                CriterioSeleccionPlanSecuencial.FERMENTADOR_LIBERACION_MAS_TEMPRANA, CriterioSeleccionPlanConcurrente.MENOR_CANTIDAD_DE_LOTES);
        when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.of(configuracion));

        // === EJECUCION ===
        ConfiguracionProduccionResponseDTO resultado = configuracionProduccionServicio.buscarConfiguracion();

        // === ASSERTS ===
        assertThat(resultado.getId()).isEqualTo(ConfiguracionProduccionEntity.SINGLETON_ID);
        assertThat(resultado.getVelocidadEstandarMolienda()).isEqualTo(6.0);
        assertThat(resultado.getCriterioSeleccionPlanSecuencial()).isEqualTo(CriterioSeleccionPlanSecuencial.FERMENTADOR_LIBERACION_MAS_TEMPRANA);
    }

    // ==================== actualizarConfiguracion ====================

    @Test
    @DisplayName("CP-AC-01: actualizarConfiguracion lanza ReglaNegocioException cuando la velocidad estándar de molienda es nula")
    void actualizar_debeRechazarVelocidadMoliendaNula() {
        ConfiguracionProduccionFormDTO formDTO = configuracionFormDTO(null, 12.0, 25.0, 90.0,
                CriterioSeleccionPlanSecuencial.FERMENTADOR_LIBERACION_MAS_TEMPRANA, CriterioSeleccionPlanConcurrente.MENOR_CANTIDAD_DE_LOTES);

        assertThatThrownBy(() -> configuracionProduccionServicio.actualizarConfiguracion(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Todos los parámetros de configuración son obligatorios");

        verify(configuracionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AC-02: actualizarConfiguracion lanza ReglaNegocioException cuando el criterio secuencial es nulo")
    void actualizar_debeRechazarCriterioSecuencialNulo() {
        ConfiguracionProduccionFormDTO formDTO = configuracionFormDTO(6.0, 12.0, 25.0, 90.0,
                null, CriterioSeleccionPlanConcurrente.MENOR_CANTIDAD_DE_LOTES);

        assertThatThrownBy(() -> configuracionProduccionServicio.actualizarConfiguracion(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Todos los parámetros de configuración son obligatorios");

        verify(configuracionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AC-03: actualizarConfiguracion lanza ReglaNegocioException cuando la velocidad estándar de envasado es cero (límite)")
    void actualizar_debeRechazarVelocidadEnvasadoEnCero() {
        ConfiguracionProduccionFormDTO formDTO = configuracionFormDTO(6.0, 0.0, 25.0, 90.0,
                CriterioSeleccionPlanSecuencial.FERMENTADOR_LIBERACION_MAS_TEMPRANA, CriterioSeleccionPlanConcurrente.MENOR_CANTIDAD_DE_LOTES);

        assertThatThrownBy(() -> configuracionProduccionServicio.actualizarConfiguracion(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las velocidades y la capacidad de lote estándar deben ser mayores a cero");

        verify(configuracionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AC-04: actualizarConfiguracion lanza ReglaNegocioException cuando la capacidad de lote estándar es negativa")
    void actualizar_debeRechazarCapacidadLoteNegativa() {
        ConfiguracionProduccionFormDTO formDTO = configuracionFormDTO(6.0, 12.0, -5.0, 90.0,
                CriterioSeleccionPlanSecuencial.FERMENTADOR_LIBERACION_MAS_TEMPRANA, CriterioSeleccionPlanConcurrente.MENOR_CANTIDAD_DE_LOTES);

        assertThatThrownBy(() -> configuracionProduccionServicio.actualizarConfiguracion(formDTO))
                .isInstanceOf(ReglaNegocioException.class);

        verify(configuracionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AC-05: actualizarConfiguracion lanza ReglaNegocioException cuando el porcentaje mínimo de consumo es negativo")
    void actualizar_debeRechazarPorcentajeNegativo() {
        ConfiguracionProduccionFormDTO formDTO = configuracionFormDTO(6.0, 12.0, 25.0, -1.0,
                CriterioSeleccionPlanSecuencial.FERMENTADOR_LIBERACION_MAS_TEMPRANA, CriterioSeleccionPlanConcurrente.MENOR_CANTIDAD_DE_LOTES);

        assertThatThrownBy(() -> configuracionProduccionServicio.actualizarConfiguracion(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El porcentaje mínimo de consumo para avanzar de etapa debe estar entre 0 y 100");

        verify(configuracionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AC-06: actualizarConfiguracion lanza ReglaNegocioException cuando el porcentaje mínimo de consumo supera 100")
    void actualizar_debeRechazarPorcentajeMayorACien() {
        ConfiguracionProduccionFormDTO formDTO = configuracionFormDTO(6.0, 12.0, 25.0, 101.0,
                CriterioSeleccionPlanSecuencial.FERMENTADOR_LIBERACION_MAS_TEMPRANA, CriterioSeleccionPlanConcurrente.MENOR_CANTIDAD_DE_LOTES);

        assertThatThrownBy(() -> configuracionProduccionServicio.actualizarConfiguracion(formDTO))
                .isInstanceOf(ReglaNegocioException.class);

        verify(configuracionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AC-07: actualizarConfiguracion acepta el porcentaje mínimo de consumo en los límites 0 y 100 (camino feliz)")
    void actualizar_debeAceptarPorcentajeEnLosLimites() {
        // === PREPARACION DE DATOS ===
        ConfiguracionProduccionEntity configuracion = crearConfiguracionEntity(6.0, 12.0, 25.0, 50.0,
                CriterioSeleccionPlanSecuencial.FERMENTADOR_LIBERACION_MAS_TEMPRANA, CriterioSeleccionPlanConcurrente.MENOR_CANTIDAD_DE_LOTES);
        when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.of(configuracion));
        when(configuracionProduccionRepository.save(any(ConfiguracionProduccionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION Y ASSERTS ===
        ConfiguracionProduccionFormDTO conCero = configuracionFormDTO(6.0, 12.0, 25.0, 0.0,
                CriterioSeleccionPlanSecuencial.FERMENTADOR_LIBERACION_MAS_TEMPRANA, CriterioSeleccionPlanConcurrente.MENOR_CANTIDAD_DE_LOTES);
        assertThat(configuracionProduccionServicio.actualizarConfiguracion(conCero).getPorcentajeMinimoConsumoParaAvanzarEtapa()).isEqualTo(0.0);

        ConfiguracionProduccionFormDTO conCien = configuracionFormDTO(6.0, 12.0, 25.0, 100.0,
                CriterioSeleccionPlanSecuencial.FERMENTADOR_LIBERACION_MAS_TEMPRANA, CriterioSeleccionPlanConcurrente.MENOR_CANTIDAD_DE_LOTES);
        assertThat(configuracionProduccionServicio.actualizarConfiguracion(conCien).getPorcentajeMinimoConsumoParaAvanzarEtapa()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("CP-AC-08: actualizarConfiguracion actualiza correctamente los 6 parámetros (camino feliz)")
    void actualizar_debeActualizarCorrectamente() {
        // === PREPARACION DE DATOS ===
        ConfiguracionProduccionEntity configuracion = crearConfiguracionEntity(5.0, 10.0, 20.0, 80.0,
                CriterioSeleccionPlanSecuencial.FERMENTADOR_MAYOR_CAPACIDAD, CriterioSeleccionPlanConcurrente.EQUIPOS_LIBERACION_MAS_TEMPRANA);
        when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.of(configuracion));
        when(configuracionProduccionRepository.save(any(ConfiguracionProduccionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConfiguracionProduccionFormDTO formDTO = configuracionFormDTO(6.0, 12.0, 25.0, 90.0,
                CriterioSeleccionPlanSecuencial.FERMENTADOR_LIBERACION_MAS_TEMPRANA, CriterioSeleccionPlanConcurrente.MENOR_CANTIDAD_DE_LOTES);

        // === EJECUCION ===
        ConfiguracionProduccionResponseDTO resultado = configuracionProduccionServicio.actualizarConfiguracion(formDTO);

        // === ASSERTS ===
        assertThat(resultado.getVelocidadEstandarMolienda()).isEqualTo(6.0);
        assertThat(resultado.getVelocidadEstandarEnvasado()).isEqualTo(12.0);
        assertThat(resultado.getCapacidadLoteEstandar()).isEqualTo(25.0);
        assertThat(resultado.getPorcentajeMinimoConsumoParaAvanzarEtapa()).isEqualTo(90.0);
        assertThat(resultado.getCriterioSeleccionPlanSecuencial()).isEqualTo(CriterioSeleccionPlanSecuencial.FERMENTADOR_LIBERACION_MAS_TEMPRANA);
        assertThat(resultado.getCriterioSeleccionPlanConcurrente()).isEqualTo(CriterioSeleccionPlanConcurrente.MENOR_CANTIDAD_DE_LOTES);
        verify(configuracionProduccionRepository).save(configuracion);
    }

    // ==================== Helpers ====================

    private static ConfiguracionProduccionFormDTO configuracionFormDTO(Double velocidadMolienda, Double velocidadEnvasado, Double capacidadLote, Double porcentajeMinimo,
                                                                        CriterioSeleccionPlanSecuencial criterioSecuencial, CriterioSeleccionPlanConcurrente criterioConcurrente) {
        return ConfiguracionProduccionFormDTO.builder()
                .velocidadEstandarMolienda(velocidadMolienda)
                .velocidadEstandarEnvasado(velocidadEnvasado)
                .capacidadLoteEstandar(capacidadLote)
                .porcentajeMinimoConsumoParaAvanzarEtapa(porcentajeMinimo)
                .criterioSeleccionPlanSecuencial(criterioSecuencial)
                .criterioSeleccionPlanConcurrente(criterioConcurrente)
                .build();
    }

    private static ConfiguracionProduccionEntity crearConfiguracionEntity(double velocidadMolienda, double velocidadEnvasado, double capacidadLote, double porcentajeMinimo,
                                                                           CriterioSeleccionPlanSecuencial criterioSecuencial, CriterioSeleccionPlanConcurrente criterioConcurrente) {
        return ConfiguracionProduccionEntity.builder()
                .id(ConfiguracionProduccionEntity.SINGLETON_ID)
                .velocidadEstandarMolienda(velocidadMolienda)
                .velocidadEstandarEnvasado(velocidadEnvasado)
                .capacidadLoteEstandar(capacidadLote)
                .porcentajeMinimoConsumoParaAvanzarEtapa(porcentajeMinimo)
                .criterioSeleccionPlanSecuencial(criterioSecuencial)
                .criterioSeleccionPlanConcurrente(criterioConcurrente)
                .build();
    }
}
