package com.github.heikyudev.maestrocervecero.service.implementation.cotizacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.ConfiguracionComprasEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.CriterioPlanCompra;
import com.github.heikyudev.maestrocervecero.persistence.repository.cotizacion.IConfiguracionComprasRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.cotizacion.ConfiguracionComprasFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.ConfiguracionComprasResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfiguracionComprasServicioImplTest {

    @Mock
    private IConfiguracionComprasRepository configuracionComprasRepository;

    @InjectMocks
    private ConfiguracionComprasServicioImpl configuracionComprasServicio;

    private static ConfiguracionComprasEntity configuracion() {
        return ConfiguracionComprasEntity.builder()
                .id(ConfiguracionComprasEntity.SINGLETON_ID)
                .plazoRespuestaHoras(48)
                .criterioPlanCompra(CriterioPlanCompra.PONDERADO)
                .pesoPrecio(50.0)
                .build();
    }

    private static ConfiguracionComprasFormDTO form(Integer plazo, CriterioPlanCompra criterio, Double peso) {
        return ConfiguracionComprasFormDTO.builder()
                .plazoRespuestaHoras(plazo)
                .criterioPlanCompra(criterio)
                .pesoPrecio(peso)
                .build();
    }

    private void conConfiguracionGuardable() {
        lenient().when(configuracionComprasRepository.findById(ConfiguracionComprasEntity.SINGLETON_ID)).thenReturn(Optional.of(configuracion()));
        lenient().when(configuracionComprasRepository.save(any(ConfiguracionComprasEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void verificarRechazo(ConfiguracionComprasFormDTO formDTO, String mensaje) {
        assertThatThrownBy(() -> configuracionComprasServicio.actualizarConfiguracion(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(mensaje);
        verify(configuracionComprasRepository, never()).save(any());
    }

    // ==================== buscarConfiguracion ====================

    @Test
    @DisplayName("CP-CCO-01: buscarConfiguracion retorna la configuración vigente")
    void buscar_retornaConfiguracionVigente() {
        // === PREPARACION DE DATOS ===
        when(configuracionComprasRepository.findById(ConfiguracionComprasEntity.SINGLETON_ID)).thenReturn(Optional.of(configuracion()));

        // === EJECUCION ===
        ConfiguracionComprasResponseDTO resultado = configuracionComprasServicio.buscarConfiguracion();

        // === ASSERTS ===
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getPlazoRespuestaHoras()).isEqualTo(48);
        assertThat(resultado.getCriterioPlanCompra()).isEqualTo(CriterioPlanCompra.PONDERADO);
        assertThat(resultado.getPesoPrecio()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("CP-CCO-02: buscarConfiguracion falla si la fila única no existe")
    void buscar_sinFila_falla() {
        when(configuracionComprasRepository.findById(ConfiguracionComprasEntity.SINGLETON_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> configuracionComprasServicio.buscarConfiguracion())
                .isInstanceOf(IllegalStateException.class);
    }

    // ==================== actualizarConfiguracion ====================

    @Test
    @DisplayName("CP-CCO-03: actualizarConfiguracion rechaza los datos nulos")
    void actualizar_rechazaDatosNulos() {
        verificarRechazo(null, "Los datos de la configuración de compras son obligatorios");
    }

    @Test
    @DisplayName("CP-CCO-04: actualizarConfiguracion rechaza cada parámetro nulo con su propio mensaje")
    void actualizar_rechazaParametrosNulos() {
        verificarRechazo(form(null, CriterioPlanCompra.MENOR_PRECIO, 50.0), "El plazo de respuesta es obligatorio");
        verificarRechazo(form(48, null, 50.0), "El criterio del plan es obligatorio");
        verificarRechazo(form(48, CriterioPlanCompra.MENOR_PRECIO, null), "El peso del precio es obligatorio");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @DisplayName("CP-CCO-05: actualizarConfiguracion rechaza un plazo de respuesta que no es mayor a 0")
    void actualizar_rechazaPlazoNoPositivo(int plazo) {
        verificarRechazo(form(plazo, CriterioPlanCompra.MENOR_PRECIO, 50.0), "El plazo de respuesta debe ser mayor a 0");
    }

    @Test
    @DisplayName("CP-CCO-06: actualizarConfiguracion rechaza un plazo de respuesta mayor a 720 horas")
    void actualizar_rechazaPlazoSobreElTope() {
        verificarRechazo(form(721, CriterioPlanCompra.MENOR_PRECIO, 50.0), "El plazo de respuesta no puede superar 720 horas");
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.01, 100.01, Double.NaN, Double.POSITIVE_INFINITY})
    @DisplayName("CP-CCO-07: actualizarConfiguracion rechaza un peso del precio fuera de 0 a 100 o que no es un número")
    void actualizar_rechazaPesoFueraDeRango(double peso) {
        verificarRechazo(form(48, CriterioPlanCompra.PONDERADO, peso), "El peso del precio debe estar entre 0 y 100");
    }

    @Test
    @DisplayName("CP-CCO-08: actualizarConfiguracion acepta los límites (1 y 720 horas, peso 0 y 100) y guarda los tres valores")
    void actualizar_aceptaLimitesYGuarda() {
        // === PREPARACION DE DATOS ===
        conConfiguracionGuardable();

        // === EJECUCION ===
        ConfiguracionComprasResponseDTO minimo = configuracionComprasServicio.actualizarConfiguracion(form(1, CriterioPlanCompra.MENOR_PRECIO, 0.0));
        ConfiguracionComprasResponseDTO maximo = configuracionComprasServicio.actualizarConfiguracion(form(720, CriterioPlanCompra.MAYOR_CANTIDAD, 100.0));

        // === ASSERTS ===
        assertThat(minimo.getPlazoRespuestaHoras()).isEqualTo(1);
        assertThat(minimo.getCriterioPlanCompra()).isEqualTo(CriterioPlanCompra.MENOR_PRECIO);
        assertThat(minimo.getPesoPrecio()).isEqualTo(0.0);
        assertThat(maximo.getPlazoRespuestaHoras()).isEqualTo(720);
        assertThat(maximo.getCriterioPlanCompra()).isEqualTo(CriterioPlanCompra.MAYOR_CANTIDAD);
        assertThat(maximo.getPesoPrecio()).isEqualTo(100.0);
    }
}
