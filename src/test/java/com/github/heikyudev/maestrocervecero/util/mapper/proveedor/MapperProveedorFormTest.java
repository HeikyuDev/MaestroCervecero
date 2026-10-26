package com.github.heikyudev.maestrocervecero.util.mapper.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor.ProveedorFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor.VersionProveedorFormDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LupuloResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.MaltaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.CatalogoProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.PresentacionComercialResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.ProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.VersionProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.LocalidadResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MapperProveedorFormTest {

    private static ProveedorResponseDTO proveedor() {
        return ProveedorResponseDTO.builder().id(4L).version(versionVigente().build()).build();
    }

    private static ProveedorResponseDTO proveedor(VersionProveedorResponseDTO version) {
        return ProveedorResponseDTO.builder().id(4L).version(version).build();
    }

    private static VersionProveedorResponseDTO.VersionProveedorResponseDTOBuilder versionVigente() {
        PresentacionComercialResponseDTO bolsa = PresentacionComercialResponseDTO.builder()
                .id(5L).nombre("Bolsa de 25 Kg").cantidad(25.0).unidadDeMedida(UnidadDeMedida.KILOGRAMO).build();
        PresentacionComercialResponseDTO sobre = PresentacionComercialResponseDTO.builder()
                .id(6L).nombre("Sobre de 100 g").cantidad(100.0).unidadDeMedida(UnidadDeMedida.GRAMO).build();
        return VersionProveedorResponseDTO.builder()
                .id(40L).razonSocial("Malterías del Sur S.A.").nombreComercial("Malterías del Sur")
                .cuit("30712345678").telefono("3584123456").email("ventas@malteriasdelsur.com")
                .direccion("Av. Italia 1234").esUltimaVersion(true)
                .localidad(LocalidadResponseDTO.builder().id(9L).nombre("Río Cuarto").build())
                .catalogoProveedor(List.of(
                        CatalogoProveedorResponseDTO.builder().id(100L).presentacionComercial(bolsa)
                                .insumo(MaltaResponseDTO.builder().id(11L).nombre("Pilsen").build()).build(),
                        CatalogoProveedorResponseDTO.builder().id(101L).presentacionComercial(sobre)
                                .insumo(LupuloResponseDTO.builder().id(21L).nombre("Cascade").build()).build()));
    }

    @Test
    @DisplayName("CP-MPF-01: los datos de la versión vigente pasan al formulario, con el CUIT con guiones y la localidad por su id")
    void toFormDTO_debeCopiarLosDatosDeLaVersionVigente() {
        // Act
        VersionProveedorFormDTO version = MapperProveedorForm.toFormDTO(proveedor()).getVersion();

        // Assert
        assertThat(version.getRazonSocial()).isEqualTo("Malterías del Sur S.A.");
        assertThat(version.getNombreComercial()).isEqualTo("Malterías del Sur");
        assertThat(version.getCuit()).isEqualTo("30-71234567-8");
        assertThat(version.getTelefono()).isEqualTo("3584123456");
        assertThat(version.getEmail()).isEqualTo("ventas@malteriasdelsur.com");
        assertThat(version.getDireccion()).isEqualTo("Av. Italia 1234");
        assertThat(version.getIdLocalidad()).isEqualTo(9L);
    }

    @Test
    @DisplayName("CP-MPF-02: cada ítem del catálogo pasa como una fila con el id del insumo y el de la presentación comercial")
    void toFormDTO_debeConvertirCadaItemDelCatalogoEnUnaFila() {
        // Act
        VersionProveedorFormDTO version = MapperProveedorForm.toFormDTO(proveedor()).getVersion();

        // Assert
        assertThat(version.getCatalogoProveedor()).hasSize(2);
        assertThat(version.getCatalogoProveedor().get(0).getIdInsumo()).isEqualTo(11L);
        assertThat(version.getCatalogoProveedor().get(0).getIdPresentacionComercial()).isEqualTo(5L);
        assertThat(version.getCatalogoProveedor().get(1).getIdInsumo()).isEqualTo(21L);
        assertThat(version.getCatalogoProveedor().get(1).getIdPresentacionComercial()).isEqualTo(6L);
    }

    @Test
    @DisplayName("CP-MPF-03: un proveedor nulo devuelve null y uno sin versión vigente devuelve un formulario vacío")
    void toFormDTO_debeManejarProveedorNuloOSinVersion() {
        // Act
        ProveedorFormDTO nulo = MapperProveedorForm.toFormDTO(null);
        ProveedorFormDTO sinVersion = MapperProveedorForm.toFormDTO(ProveedorResponseDTO.builder().id(4L).build());

        // Assert
        assertThat(nulo).isNull();
        assertThat(sinVersion.getVersion()).isNotNull();
        assertThat(sinVersion.getVersion().getRazonSocial()).isNull();
        assertThat(sinVersion.getVersion().getCatalogoProveedor()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("CP-MPF-04: un ítem del catálogo sin insumo o sin presentación comercial pasa como una fila con ese id en null")
    void toFormDTO_debeDejarEnNullElIdDelInsumoOLaPresentacionFaltante() {
        // Arrange
        ProveedorResponseDTO proveedor = proveedor(versionVigente().catalogoProveedor(List.of(
                CatalogoProveedorResponseDTO.builder().id(100L).insumo(null)
                        .presentacionComercial(PresentacionComercialResponseDTO.builder().id(5L).build()).build(),
                CatalogoProveedorResponseDTO.builder().id(101L).presentacionComercial(null)
                        .insumo(MaltaResponseDTO.builder().id(11L).build()).build())).build());

        // Act
        VersionProveedorFormDTO version = MapperProveedorForm.toFormDTO(proveedor).getVersion();

        // Assert
        assertThat(version.getCatalogoProveedor()).hasSize(2);
        assertThat(version.getCatalogoProveedor().get(0).getIdInsumo()).isNull();
        assertThat(version.getCatalogoProveedor().get(0).getIdPresentacionComercial()).isEqualTo(5L);
        assertThat(version.getCatalogoProveedor().get(1).getIdInsumo()).isEqualTo(11L);
        assertThat(version.getCatalogoProveedor().get(1).getIdPresentacionComercial()).isNull();
    }

    @Test
    @DisplayName("CP-MPF-05: una versión con el catálogo nulo pasa con el catálogo vacío (no nulo)")
    void toFormDTO_debeConvertirElCatalogoNuloEnUnaListaVacia() {
        // Arrange
        ProveedorResponseDTO proveedor = proveedor(versionVigente().catalogoProveedor(null).build());

        // Act
        VersionProveedorFormDTO version = MapperProveedorForm.toFormDTO(proveedor).getVersion();

        // Assert
        assertThat(version.getCatalogoProveedor()).isNotNull().isEmpty();
        assertThat(version.getRazonSocial()).isEqualTo("Malterías del Sur S.A.");
    }

    @Test
    @DisplayName("CP-MPF-06: un CUIT que ya viene con guiones pasa sin cambios, sin duplicar los guiones")
    void toFormDTO_debeConservarElCuitYaFormateado() {
        // Arrange
        ProveedorResponseDTO proveedor = proveedor(versionVigente().cuit("30-71234567-8").build());

        // Act
        VersionProveedorFormDTO version = MapperProveedorForm.toFormDTO(proveedor).getVersion();

        // Assert
        assertThat(version.getCuit()).isEqualTo("30-71234567-8");
    }
}
