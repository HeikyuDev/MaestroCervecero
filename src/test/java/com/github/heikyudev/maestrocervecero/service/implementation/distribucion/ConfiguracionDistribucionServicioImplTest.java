package com.github.heikyudev.maestrocervecero.service.implementation.distribucion;

import com.github.heikyudev.maestrocervecero.persistence.entity.distribucion.ConfiguracionDistribucionEntity;
import com.github.heikyudev.maestrocervecero.persistence.repository.distribucion.IConfiguracionDistribucionRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.distribucion.ConfiguracionDistribucionFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.distribucion.ConfiguracionDistribucionResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfiguracionDistribucionServicioImplTest {

    @Mock
    private IConfiguracionDistribucionRepository configuracionDistribucionRepository;

    @InjectMocks
    private ConfiguracionDistribucionServicioImpl configuracionDistribucionServicio;

    private static ConfiguracionDistribucionEntity configuracion() {
        return ConfiguracionDistribucionEntity.builder()
                .id(ConfiguracionDistribucionEntity.SINGLETON_ID)
                .horaEnvioRecordatorioRetiro(LocalTime.of(9, 0))
                .diasMaximosAnticipacionRetiro(7)
                .build();
    }

    private static ConfiguracionDistribucionFormDTO form(LocalTime hora, Integer dias) {
        return ConfiguracionDistribucionFormDTO.builder().horaEnvioRecordatorioRetiro(hora).diasMaximosAnticipacionRetiro(dias).build();
    }

    private void conConfiguracionGuardable() {
        lenient().when(configuracionDistribucionRepository.findById(ConfiguracionDistribucionEntity.SINGLETON_ID)).thenReturn(Optional.of(configuracion()));
        lenient().when(configuracionDistribucionRepository.save(any(ConfiguracionDistribucionEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void verificarRechazo(ConfiguracionDistribucionFormDTO formDTO, String mensaje) {
        assertThatThrownBy(() -> configuracionDistribucionServicio.actualizarConfiguracion(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(mensaje);
        verify(configuracionDistribucionRepository, never()).save(any());
    }

    // ==================== buscarConfiguracion ====================

    @Test
    @DisplayName("CP-CDI-01: buscarConfiguracion retorna la configuración vigente")
    void buscar_retornaConfiguracionVigente() {
        // === PREPARACION DE DATOS ===
        when(configuracionDistribucionRepository.findById(ConfiguracionDistribucionEntity.SINGLETON_ID)).thenReturn(Optional.of(configuracion()));

        // === EJECUCION ===
        ConfiguracionDistribucionResponseDTO resultado = configuracionDistribucionServicio.buscarConfiguracion();

        // === ASSERTS ===
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getHoraEnvioRecordatorioRetiro()).isEqualTo(LocalTime.of(9, 0));
        assertThat(resultado.getDiasMaximosAnticipacionRetiro()).isEqualTo(7);
    }

    @Test
    @DisplayName("CP-CDI-02: buscarConfiguracion lanza IllegalStateException si la fila única no existe")
    void buscar_sinFilaUnica_lanzaIllegalState() {
        when(configuracionDistribucionRepository.findById(ConfiguracionDistribucionEntity.SINGLETON_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> configuracionDistribucionServicio.buscarConfiguracion())
                .isInstanceOf(IllegalStateException.class);
    }

    // ==================== actualizarConfiguracion ====================

    @Test
    @DisplayName("CP-CDI-03: actualizarConfiguracion rechaza los datos nulos")
    void actualizar_datosNulos_rechaza() {
        verificarRechazo(null, "Los datos de la configuración de distribución son obligatorios");
    }

    @Test
    @DisplayName("CP-CDI-04: actualizarConfiguracion rechaza cada parámetro nulo con su mensaje")
    void actualizar_parametroNulo_rechaza() {
        verificarRechazo(form(null, 7), "La hora de envío del recordatorio es obligatoria");
        verificarRechazo(form(LocalTime.of(9, 0), null), "Los días máximos de anticipación del retiro son obligatorios");
    }

    @Test
    @DisplayName("CP-CDI-05: actualizarConfiguracion rechaza los días máximos de anticipación fuera de 1 a 30")
    void actualizar_diasFueraDeRango_rechaza() {
        conConfiguracionGuardable();

        for (int dias : new int[]{0, -1, 31}) {
            verificarRechazo(form(LocalTime.of(9, 0), dias), "Los días máximos de anticipación del retiro deben estar entre 1 y 30");
        }
    }

    @Test
    @DisplayName("CP-CDI-06: actualizarConfiguracion acepta los límites 1 y 30 días y persiste los dos valores")
    void actualizar_limitesValidos_persiste() {
        conConfiguracionGuardable();

        ConfiguracionDistribucionResponseDTO minimo = configuracionDistribucionServicio.actualizarConfiguracion(form(LocalTime.of(0, 0), 1));
        ConfiguracionDistribucionResponseDTO maximo = configuracionDistribucionServicio.actualizarConfiguracion(form(LocalTime.of(23, 59), 30));

        assertThat(minimo.getHoraEnvioRecordatorioRetiro()).isEqualTo(LocalTime.of(0, 0));
        assertThat(minimo.getDiasMaximosAnticipacionRetiro()).isEqualTo(1);
        assertThat(maximo.getHoraEnvioRecordatorioRetiro()).isEqualTo(LocalTime.of(23, 59));
        assertThat(maximo.getDiasMaximosAnticipacionRetiro()).isEqualTo(30);
    }

    @Test
    @DisplayName("CP-CDI-07: actualizarConfiguracion guarda la hora sin segundos")
    void actualizar_horaConSegundos_seGuardaSinSegundos() {
        conConfiguracionGuardable();

        ConfiguracionDistribucionResponseDTO resultado = configuracionDistribucionServicio.actualizarConfiguracion(form(LocalTime.of(10, 15, 42, 5), 7));

        assertThat(resultado.getHoraEnvioRecordatorioRetiro()).isEqualTo(LocalTime.of(10, 15));
    }
}
