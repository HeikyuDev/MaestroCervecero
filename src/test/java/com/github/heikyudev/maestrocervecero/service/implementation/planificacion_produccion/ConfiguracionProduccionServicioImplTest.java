package com.github.heikyudev.maestrocervecero.service.implementation.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.ConfiguracionProduccionEntity;
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
        ConfiguracionProduccionEntity configuracion = crearConfiguracionEntity(12.0, 90.0);
        when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.of(configuracion));

        // === EJECUCION ===
        ConfiguracionProduccionResponseDTO resultado = configuracionProduccionServicio.buscarConfiguracion();

        // === ASSERTS ===
        assertThat(resultado.getId()).isEqualTo(ConfiguracionProduccionEntity.SINGLETON_ID);
        assertThat(resultado.getVelocidadEstandarEnvasado()).isEqualTo(12.0);
    }

    // ==================== actualizarConfiguracion ====================

    @Test
    @DisplayName("CP-AC-03: actualizarConfiguracion lanza ReglaNegocioException cuando la velocidad estándar de envasado es cero (límite)")
    void actualizar_debeRechazarVelocidadEnvasadoEnCero() {
        ConfiguracionProduccionFormDTO formDTO = configuracionFormDTO(0.0, 90.0);

        assertThatThrownBy(() -> configuracionProduccionServicio.actualizarConfiguracion(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La velocidad de envasado debe ser mayor a 0");

        verify(configuracionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AC-05: actualizarConfiguracion lanza ReglaNegocioException cuando el porcentaje mínimo de consumo es negativo")
    void actualizar_debeRechazarPorcentajeNegativo() {
        ConfiguracionProduccionFormDTO formDTO = configuracionFormDTO(12.0, -1.0);

        assertThatThrownBy(() -> configuracionProduccionServicio.actualizarConfiguracion(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El porcentaje mínimo de consumo debe estar entre 0 y 100");

        verify(configuracionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AC-06: actualizarConfiguracion lanza ReglaNegocioException cuando el porcentaje mínimo de consumo supera 100")
    void actualizar_debeRechazarPorcentajeMayorACien() {
        ConfiguracionProduccionFormDTO formDTO = configuracionFormDTO(12.0, 101.0);

        assertThatThrownBy(() -> configuracionProduccionServicio.actualizarConfiguracion(formDTO))
                .isInstanceOf(ReglaNegocioException.class);

        verify(configuracionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AC-07: actualizarConfiguracion acepta el porcentaje mínimo de consumo en los límites 0 y 100 (camino feliz)")
    void actualizar_debeAceptarPorcentajeEnLosLimites() {
        // === PREPARACION DE DATOS ===
        ConfiguracionProduccionEntity configuracion = crearConfiguracionEntity(12.0, 50.0);
        when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.of(configuracion));
        when(configuracionProduccionRepository.save(any(ConfiguracionProduccionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION Y ASSERTS ===
        ConfiguracionProduccionFormDTO conCero = configuracionFormDTO(12.0, 0.0);
        assertThat(configuracionProduccionServicio.actualizarConfiguracion(conCero).getPorcentajeMinimoConsumoParaAvanzarEtapa()).isEqualTo(0.0);

        ConfiguracionProduccionFormDTO conCien = configuracionFormDTO(12.0, 100.0);
        assertThat(configuracionProduccionServicio.actualizarConfiguracion(conCien).getPorcentajeMinimoConsumoParaAvanzarEtapa()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("CP-AC-08: actualizarConfiguracion actualiza correctamente los 3 parámetros (camino feliz)")
    void actualizar_debeActualizarCorrectamente() {
        // === PREPARACION DE DATOS ===
        ConfiguracionProduccionEntity configuracion = crearConfiguracionEntity(10.0, 80.0);
        when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.of(configuracion));
        when(configuracionProduccionRepository.save(any(ConfiguracionProduccionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConfiguracionProduccionFormDTO formDTO = configuracionFormDTO(12.0, 90.0);

        // === EJECUCION ===
        ConfiguracionProduccionResponseDTO resultado = configuracionProduccionServicio.actualizarConfiguracion(formDTO);

        // === ASSERTS ===
        assertThat(resultado.getVelocidadEstandarEnvasado()).isEqualTo(12.0);
        assertThat(resultado.getPorcentajeMinimoConsumoParaAvanzarEtapa()).isEqualTo(90.0);
        assertThat(resultado.getExcedenteMaximoLitros()).isEqualTo(300.0);
        verify(configuracionProduccionRepository).save(configuracion);
    }

    @Test
    @DisplayName("CP-AC-09: actualizarConfiguracion lanza ReglaNegocioException cuando la velocidad de envasado es NaN o +Infinity")
    void actualizar_debeRechazarVelocidadEnvasadoNoFinita() {
        for (double valor : new double[]{Double.NaN, Double.POSITIVE_INFINITY}) {
            ConfiguracionProduccionFormDTO formDTO = configuracionFormDTO(valor, 90.0);

            assertThatThrownBy(() -> configuracionProduccionServicio.actualizarConfiguracion(formDTO))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("La velocidad de envasado debe ser mayor a 0");
        }

        verify(configuracionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AC-11: actualizarConfiguracion lanza ReglaNegocioException cuando el porcentaje mínimo de consumo es NaN")
    void actualizar_debeRechazarPorcentajeNaN() {
        ConfiguracionProduccionFormDTO formDTO = configuracionFormDTO(12.0, Double.NaN);

        assertThatThrownBy(() -> configuracionProduccionServicio.actualizarConfiguracion(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El porcentaje mínimo de consumo debe ser mayor o igual a 0");

        verify(configuracionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AC-12: actualizarConfiguracion acepta la velocidad de envasado igual a su techo de 100.000 L/h (límite)")
    void actualizar_debeAceptarVelocidadEnSuTecho() {
        ConfiguracionProduccionEntity configuracion = crearConfiguracionEntity(12.0, 50.0);
        when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.of(configuracion));
        when(configuracionProduccionRepository.save(any(ConfiguracionProduccionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConfiguracionProduccionFormDTO formDTO = configuracionFormDTO(100_000.0, 90.0);

        ConfiguracionProduccionResponseDTO resultado = configuracionProduccionServicio.actualizarConfiguracion(formDTO);

        assertThat(resultado.getVelocidadEstandarEnvasado()).isEqualTo(100_000.0);
        verify(configuracionProduccionRepository).save(configuracion);
    }

    @Test
    @DisplayName("CP-AC-13: actualizarConfiguracion lanza ReglaNegocioException cuando la velocidad de envasado supera el techo de 100.000 L/h")
    void actualizar_debeRechazarVelocidadEnvasadoSobreElTecho() {
        ConfiguracionProduccionFormDTO formDTO = configuracionFormDTO(100_000.01, 90.0);

        assertThatThrownBy(() -> configuracionProduccionServicio.actualizarConfiguracion(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La velocidad de envasado no puede superar 100.000 litros por hora");

        verify(configuracionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AC-15: actualizarConfiguracion lanza ReglaNegocioException cuando la velocidad estándar de envasado es nula")
    void actualizar_debeRechazarVelocidadEnvasadoNula() {
        ConfiguracionProduccionFormDTO formDTO = configuracionFormDTO(null, 90.0);

        assertThatThrownBy(() -> configuracionProduccionServicio.actualizarConfiguracion(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La velocidad de envasado es obligatoria");

        verify(configuracionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AC-17: actualizarConfiguracion lanza ReglaNegocioException cuando el porcentaje mínimo de consumo es nulo")
    void actualizar_debeRechazarPorcentajeNulo() {
        ConfiguracionProduccionFormDTO formDTO = configuracionFormDTO(12.0, null);

        assertThatThrownBy(() -> configuracionProduccionServicio.actualizarConfiguracion(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El porcentaje mínimo de consumo es obligatorio");

        verify(configuracionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AC-20: actualizarConfiguracion lanza ReglaNegocioException cuando el excedente máximo de un plan es nulo")
    void actualizar_debeRechazarExcedenteMaximoNulo() {
        ConfiguracionProduccionFormDTO formDTO = configuracionFormDTO(12.0, 90.0);
        formDTO.setExcedenteMaximoLitros(null);

        assertThatThrownBy(() -> configuracionProduccionServicio.actualizarConfiguracion(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El excedente máximo de un plan es obligatorio");

        verify(configuracionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AC-21: actualizarConfiguracion lanza ReglaNegocioException cuando el excedente máximo de un plan es negativo o NaN")
    void actualizar_debeRechazarExcedenteMaximoNegativoONaN() {
        for (Double excedente : new Double[]{-1.0, Double.NaN}) {
            ConfiguracionProduccionFormDTO formDTO = configuracionFormDTO(12.0, 90.0);
            formDTO.setExcedenteMaximoLitros(excedente);

            assertThatThrownBy(() -> configuracionProduccionServicio.actualizarConfiguracion(formDTO))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("El excedente máximo de un plan debe ser mayor o igual a 0");
        }

        verify(configuracionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AC-22: actualizarConfiguracion lanza ReglaNegocioException cuando el excedente máximo de un plan supera su techo")
    void actualizar_debeRechazarExcedenteMaximoSobreElTecho() {
        ConfiguracionProduccionFormDTO formDTO = configuracionFormDTO(12.0, 90.0);
        formDTO.setExcedenteMaximoLitros(100000.01);

        assertThatThrownBy(() -> configuracionProduccionServicio.actualizarConfiguracion(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageStartingWith("El excedente máximo de un plan no puede superar");

        verify(configuracionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AC-23: actualizarConfiguracion persiste un excedente máximo de 0 y uno igual a su techo (límites válidos)")
    void actualizar_debePersistirExcedenteMaximoEnSusLimites() {
        for (Double excedente : new Double[]{0.0, 100000.0}) {
            ConfiguracionProduccionEntity configuracion = crearConfiguracionEntity(10.0, 80.0);
            when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.of(configuracion));
            when(configuracionProduccionRepository.save(any(ConfiguracionProduccionEntity.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
            ConfiguracionProduccionFormDTO formDTO = configuracionFormDTO(12.0, 90.0);
            formDTO.setExcedenteMaximoLitros(excedente);

            ConfiguracionProduccionResponseDTO resultado = configuracionProduccionServicio.actualizarConfiguracion(formDTO);

            assertThat(configuracion.getExcedenteMaximoLitros()).isEqualTo(excedente);
            assertThat(resultado.getExcedenteMaximoLitros()).isEqualTo(excedente);
        }
    }

    // ==================== Helpers ====================

    private static ConfiguracionProduccionFormDTO configuracionFormDTO(Double velocidadEnvasado, Double porcentajeMinimo) {
        return ConfiguracionProduccionFormDTO.builder()
                .velocidadEstandarEnvasado(velocidadEnvasado)
                .porcentajeMinimoConsumoParaAvanzarEtapa(porcentajeMinimo)
                .excedenteMaximoLitros(300.0)
                .build();
    }

    private static ConfiguracionProduccionEntity crearConfiguracionEntity(double velocidadEnvasado, double porcentajeMinimo) {
        return ConfiguracionProduccionEntity.builder()
                .id(ConfiguracionProduccionEntity.SINGLETON_ID)
                .velocidadEstandarEnvasado(velocidadEnvasado)
                .porcentajeMinimoConsumoParaAvanzarEtapa(porcentajeMinimo)
                .excedenteMaximoLitros(300.0)
                .build();
    }
}
