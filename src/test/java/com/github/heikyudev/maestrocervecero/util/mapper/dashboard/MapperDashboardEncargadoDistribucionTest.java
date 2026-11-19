package com.github.heikyudev.maestrocervecero.util.mapper.dashboard;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.DespachoBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cliente.ClienteEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.ContenidoBarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.BarrilListoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.ClienteConBarrilesResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.DevolucionPendienteResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class MapperDashboardEncargadoDistribucionTest {

    @Test
    @DisplayName("CP-MDI-01: la devolución pendiente lleva el barril, el cliente y la fecha del despacho, y los días de atraso hasta hoy (0 si vence hoy)")
    void toDevolucionPendienteDTO_llevaLosDatosYElAtraso() {
        // Arrange
        LocalDate hoy = LocalDate.of(2027, 7, 10);
        DespachoBarrilEntity vencido = despacho(LocalDate.of(2027, 7, 7));
        DespachoBarrilEntity deHoy = despacho(hoy);

        // Act
        DevolucionPendienteResponseDTO dtoVencido = MapperDashboardEncargadoDistribucion.toDevolucionPendienteDTO(vencido, hoy);
        DevolucionPendienteResponseDTO dtoDeHoy = MapperDashboardEncargadoDistribucion.toDevolucionPendienteDTO(deHoy, hoy);

        // Assert
        assertThat(dtoVencido.getIdBarril()).isEqualTo(21L);
        assertThat(dtoVencido.getIdentificadorInterno()).isEqualTo("BRL-01");
        assertThat(dtoVencido.getNombreCliente()).isEqualTo("Bar Don Jorge");
        assertThat(dtoVencido.getFechaDevolucionEstimada()).isEqualTo(LocalDate.of(2027, 7, 7));
        assertThat(dtoVencido.getDiasDeAtraso()).isEqualTo(3);
        assertThat(dtoDeHoy.getDiasDeAtraso()).isZero();
    }

    @Test
    @DisplayName("CP-MDI-02: el cliente con barriles lleva su ID, su nombre, la cantidad de barriles y su próxima fecha de devolución desde la fila de la consulta")
    void toClienteConBarrilesDTO_llevaLosDatosDeLaFila() {
        // Act
        ClienteConBarrilesResponseDTO dto = MapperDashboardEncargadoDistribucion.toClienteConBarrilesDTO(new Object[]{5L, "Bar Don Jorge", 2L, LocalDate.of(2027, 7, 7)});

        // Assert
        assertThat(dto.getIdCliente()).isEqualTo(5L);
        assertThat(dto.getNombre()).isEqualTo("Bar Don Jorge");
        assertThat(dto.getCantidadBarriles()).isEqualTo(2);
        assertThat(dto.getProximaFechaDevolucion()).isEqualTo(LocalDate.of(2027, 7, 7));
    }

    @Test
    @DisplayName("CP-MDI-03: el barril listo lleva su ID, su identificador, la receta y el lote de su cerveza y su contenido, y deja la receta y el lote nulos si no tiene contenido")
    void toBarrilListoDTO_llevaLosDatosDelBarril() {
        // Arrange
        BarrilControlResponseDTO conContenido = BarrilControlResponseDTO.builder().id(31L).identificadorInterno("BRL-02").contenidoActual(50.0)
                .contenido(new ContenidoBarrilResponseDTO(12L, "PALE_ALE-1", "Pale Ale")).build();
        BarrilControlResponseDTO sinContenido = BarrilControlResponseDTO.builder().id(32L).identificadorInterno("BRL-03").contenidoActual(30.0).build();

        // Act
        BarrilListoResponseDTO dto = MapperDashboardEncargadoDistribucion.toBarrilListoDTO(conContenido);
        BarrilListoResponseDTO dtoSinContenido = MapperDashboardEncargadoDistribucion.toBarrilListoDTO(sinContenido);

        // Assert
        assertThat(dto.getId()).isEqualTo(31L);
        assertThat(dto.getIdentificadorInterno()).isEqualTo("BRL-02");
        assertThat(dto.getNombreReceta()).isEqualTo("Pale Ale");
        assertThat(dto.getCodigoLote()).isEqualTo("PALE_ALE-1");
        assertThat(dto.getContenidoActual()).isEqualTo(50.0);
        assertThat(dtoSinContenido.getNombreReceta()).isNull();
        assertThat(dtoSinContenido.getCodigoLote()).isNull();
    }

    @Test
    @DisplayName("CP-MDI-04: un despacho, una fila o un barril nulo se mapean a nulo")
    void toDTO_conEntradaNulaDevuelveNulo() {
        // Act and Assert
        assertThat(MapperDashboardEncargadoDistribucion.toDevolucionPendienteDTO(null, LocalDate.now())).isNull();
        assertThat(MapperDashboardEncargadoDistribucion.toClienteConBarrilesDTO(null)).isNull();
        assertThat(MapperDashboardEncargadoDistribucion.toBarrilListoDTO(null)).isNull();
        assertThat(MapperDashboardEncargadoDistribucion.toRetiroPendienteDTO(null, java.time.LocalDateTime.now())).isNull();
    }

    @Test
    @DisplayName("CP-MDI-05: el retiro pendiente lleva la solicitud, el barril, el cliente y la fecha propuesta, y está vencido solo si la fecha es anterior al momento de referencia")
    void toRetiroPendienteDTO_llevaLosDatosYSiVencio() {
        // Arrange
        java.time.LocalDateTime ahora = java.time.LocalDateTime.of(2027, 7, 10, 12, 0);
        DespachoBarrilEntity despacho = despacho(LocalDate.of(2027, 7, 7));
        despacho.getBarril().setNumeroSerie("SN-1001");
        com.github.heikyudev.maestrocervecero.persistence.entity.barril.SolicitudBusquedaEntity vencida =
                com.github.heikyudev.maestrocervecero.persistence.entity.barril.SolicitudBusquedaEntity.builder().id(70L)
                        .fechaBusqueda(ahora.minusMinutes(1)).despachoBarril(despacho).build();
        com.github.heikyudev.maestrocervecero.persistence.entity.barril.SolicitudBusquedaEntity justoAhora =
                com.github.heikyudev.maestrocervecero.persistence.entity.barril.SolicitudBusquedaEntity.builder().id(71L)
                        .fechaBusqueda(ahora).despachoBarril(despacho).build();

        // Act
        com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.RetiroPendienteResponseDTO dto =
                MapperDashboardEncargadoDistribucion.toRetiroPendienteDTO(vencida, ahora);

        // Assert
        assertThat(dto.getIdSolicitud()).isEqualTo(70L);
        assertThat(dto.getIdBarril()).isEqualTo(21L);
        assertThat(dto.getIdentificadorInterno()).isEqualTo("BRL-01");
        assertThat(dto.getNumeroSerie()).isEqualTo("SN-1001");
        assertThat(dto.getNombreCliente()).isEqualTo("Bar Don Jorge");
        assertThat(dto.getFechaBusqueda()).isEqualTo(ahora.minusMinutes(1));
        assertThat(dto.isVencida()).isTrue();
        assertThat(MapperDashboardEncargadoDistribucion.toRetiroPendienteDTO(justoAhora, ahora).isVencida()).isFalse();
    }

    private static DespachoBarrilEntity despacho(LocalDate devolucion) {
        return DespachoBarrilEntity.builder().fechaDevolucionEstimada(devolucion)
                .barril(BarrilEntity.builder().id(21L).identificadorInterno("BRL-01").build())
                .cliente(ClienteEntity.builder().nombre("Bar Don Jorge").build()).build();
    }
}
