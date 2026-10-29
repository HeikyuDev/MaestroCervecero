package com.github.heikyudev.maestrocervecero.service.implementation.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.PresentacionComercialEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.ProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.VersionProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion.LocalidadEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.IInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.orden_compra.IOrdenCompraRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.proveedor.IPresentacionComercialRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.proveedor.IProveedorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.proveedor.IVersionProveedorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ubicacion.ILocalidadRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor.CatalogoProveedorFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor.ProveedorFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor.VersionProveedorFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.ProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.util.persistencia.GuardadoUnico;
import com.github.heikyudev.maestrocervecero.util.persistencia.GuardadoUnicoTest;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesTexto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProveedorServicioImplTest {

    private static final Long LOCALIDAD_ID = 1L;
    private static final Long PRESENTACION_ID = 1L;
    private static final Long INSUMO_ID = 1L;

    @Mock
    private IProveedorRepository proveedorRepository;
    @Mock
    private IVersionProveedorRepository versionProveedorRepository;
    @Mock
    private ILocalidadRepository localidadRepository;
    @Mock
    private IInsumoRepository insumoRepository;
    @Mock
    private IPresentacionComercialRepository presentacionComercialRepository;
    @Mock
    private IOrdenCompraRepository ordenCompraRepository;

    @InjectMocks
    private ProveedorServicioImpl proveedorServicio;

    // ==================== filtrarProveedores ====================

    @Test
    @DisplayName("CP-FP-01: filtrarProveedores retorna una página de proveedores correctamente mapeada a DTO cuando se filtra por razón social, nombre comercial, CUIT e idLocalidad")
    void filtrarProveedores_debeRetornarPaginaMapeadaFiltrandoPorLos4Criterios() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        ProveedorEntity proveedor1 = crearProveedorEntityConVersionActiva(1L, "Maltería del Sur S.A.", "30-11111111-1");
        ProveedorEntity proveedor2 = crearProveedorEntityConVersionActiva(2L, "Maltería del Norte S.A.", "30-22222222-2");
        when(proveedorRepository.filtrarProveedores("Maltería", "Sur", "30-11111111-1", LOCALIDAD_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(proveedor1, proveedor2), pageable, 2));

        // === EJECUCION ===
        Page<ProveedorResponseDTO> resultado = proveedorServicio.filtrarProveedores("Maltería", "Sur", "30-11111111-1", LOCALIDAD_ID, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertThat(resultado.getContent()).hasSize(2);
        assertThat(resultado.getContent().get(0).getVersion().getRazonSocial()).isEqualTo("Maltería del Sur S.A.");
        verify(proveedorRepository).filtrarProveedores("Maltería", "Sur", "30-11111111-1", LOCALIDAD_ID, pageable);
    }

    @Test
    @DisplayName("CP-FP-02: filtrarProveedores propaga razón social, nombre comercial, CUIT e idLocalidad nulos sin restringir esos criterios")
    void filtrarProveedores_debePropagarCriteriosNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        ProveedorEntity proveedor1 = crearProveedorEntityConVersionActiva(1L, "Maltería del Sur S.A.", "30-11111111-1");
        ProveedorEntity proveedor2 = crearProveedorEntityConVersionActiva(2L, "Lupulera Patagónica S.A.", "30-33333333-3");
        when(proveedorRepository.filtrarProveedores(null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(proveedor1, proveedor2), pageable, 2));

        // === EJECUCION ===
        Page<ProveedorResponseDTO> resultado = proveedorServicio.filtrarProveedores(null, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        verify(proveedorRepository).filtrarProveedores(null, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FP-03: filtrarProveedores retorna una página vacía cuando ningún registro cumple los criterios")
    void filtrarProveedores_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(proveedorRepository.filtrarProveedores("Inexistente", null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<ProveedorResponseDTO> resultado = proveedorServicio.filtrarProveedores("Inexistente", null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(proveedorRepository).filtrarProveedores("Inexistente", null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FP-04: filtrarProveedores normaliza los textos buscados antes de consultar")
    void filtrarProveedores_debeNormalizarLosTextosBuscados() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(proveedorRepository.filtrarProveedores("Maltería del Sur", "Sur", "30-11111111-1", null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        proveedorServicio.filtrarProveedores("   Maltería   del  Sur ", "  Sur ", " 30-11111111-1 ", null, pageable);

        // === ASSERTS ===
        verify(proveedorRepository).filtrarProveedores("Maltería del Sur", "Sur", "30-11111111-1", null, pageable);
    }

    @Test
    @DisplayName("CP-FP-05: filtrarProveedores trata los textos de solo espacios como criterios nulos")
    void filtrarProveedores_debeTratarTextosEnBlancoComoNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(proveedorRepository.filtrarProveedores(null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        proveedorServicio.filtrarProveedores("   ", "  ", " - ", null, pageable);

        // === ASSERTS ===
        verify(proveedorRepository).filtrarProveedores(null, null, null, null, pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO del proveedor cuando el ID existe")
    void buscarPorId_debeRetornarProveedorExistente() {
        // === PREPARACION DE DATOS ===
        ProveedorEntity proveedorEntity = crearProveedorEntityConVersionActiva(1L, "Maltería del Sur S.A.", "30-11111111-1");
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedorEntity));

        // === EJECUCION ===
        ProveedorResponseDTO resultado = proveedorServicio.buscarPorId(1L);

        // === ASSERTS ===
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getVersion().getRazonSocial()).isEqualTo("Maltería del Sur S.A.");
        assertThat(resultado.getEstado()).isEqualTo(Estado.ACTIVO);
        verify(proveedorRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe o está dado de baja")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        // findById filtra por estado = ACTIVO, por lo que devuelve Optional.empty() también para proveedores dados de baja
        when(proveedorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proveedorServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el proveedor con ID: 99");
        verify(proveedorRepository).findById(99L);
    }

    // ==================== altaProveedor ====================

    @Test
    @DisplayName("CP-AP-02: altaProveedor lanza RecursoDuplicadoException cuando el CUIT ya está registrado por un proveedor activo")
    void altaProveedor_debeRechazarCuitDuplicado() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().cuit("30111111111").build();
        ProveedorFormDTO proveedorFormDTO = proveedorFormDTO(versionFormDTO);
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrue("30-11111111-1")).thenReturn(true);

        assertThatThrownBy(() -> proveedorServicio.altaProveedor(proveedorFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un proveedor con el CUIT 30-11111111-1");

        verify(proveedorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AP-03: altaProveedor lanza RecursoNoEncontradoException y no persiste cuando la localidad referenciada no existe")
    void altaProveedor_debeRechazarLocalidadInexistente() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().idLocalidad(99L).build();
        ProveedorFormDTO proveedorFormDTO = proveedorFormDTO(versionFormDTO);
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrue(
                ValidacionesTexto.formatearCuit(versionFormDTO.getCuit()))).thenReturn(false);
        when(localidadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proveedorServicio.altaProveedor(proveedorFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la localidad con ID: 99");

        verify(proveedorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AP-04: altaProveedor lanza RecursoNoEncontradoException y no persiste cuando el insumo de un ítem del catálogo no existe")
    void altaProveedor_debeRechazarInsumoInexistente() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder()
                .catalogoProveedor(List.of(catalogoProveedorFormDTO(PRESENTACION_ID, 99L)))
                .build();
        ProveedorFormDTO proveedorFormDTO = proveedorFormDTO(versionFormDTO);
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrue(
                ValidacionesTexto.formatearCuit(versionFormDTO.getCuit()))).thenReturn(false);
        when(localidadRepository.findById(LOCALIDAD_ID)).thenReturn(Optional.of(localidadEntity(LOCALIDAD_ID)));
        when(presentacionComercialRepository.findById(PRESENTACION_ID)).thenReturn(Optional.of(presentacionComercialEntity(PRESENTACION_ID)));
        when(insumoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proveedorServicio.altaProveedor(proveedorFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el insumo con ID: 99");

        verify(proveedorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AP-05: altaProveedor lanza RecursoNoEncontradoException y no persiste cuando la presentación comercial de un ítem del catálogo no existe")
    void altaProveedor_debeRechazarPresentacionComercialInexistente() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder()
                .catalogoProveedor(List.of(catalogoProveedorFormDTO(99L, INSUMO_ID)))
                .build();
        ProveedorFormDTO proveedorFormDTO = proveedorFormDTO(versionFormDTO);
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrue(
                ValidacionesTexto.formatearCuit(versionFormDTO.getCuit()))).thenReturn(false);
        when(localidadRepository.findById(LOCALIDAD_ID)).thenReturn(Optional.of(localidadEntity(LOCALIDAD_ID)));
        when(presentacionComercialRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proveedorServicio.altaProveedor(proveedorFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la presentación comercial con ID: 99");

        verify(proveedorRepository, never()).save(any());
    }

    @ParameterizedTest(name = "CP-AP-06: catálogo {0}")
    @NullAndEmptySource
    @DisplayName("CP-AP-06: altaProveedor lanza ReglaNegocioException y no consulta la BD cuando el catálogo es nulo o está vacío")
    void altaProveedor_debeRechazarCatalogoNuloOVacio(List<CatalogoProveedorFormDTO> catalogo) {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().catalogoProveedor(catalogo).build();

        assertThatThrownBy(() -> proveedorServicio.altaProveedor(proveedorFormDTO(versionFormDTO)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El catálogo debe tener al menos un ítem");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-AP-07: altaProveedor persiste el proveedor completo con estado ACTIVO, primera versión activa y catálogo (camino feliz)")
    void altaProveedor_debePersistirProveedorCompleto() {
        // === PREPARACION DE DATOS ===
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder()
                .catalogoProveedor(List.of(
                        catalogoProveedorFormDTO(PRESENTACION_ID, INSUMO_ID),
                        catalogoProveedorFormDTO(2L, 2L)))
                .build();
        ProveedorFormDTO proveedorFormDTO = proveedorFormDTO(versionFormDTO);
        mockearAltaExitosa(versionFormDTO);

        // === EJECUCION ===
        ProveedorResponseDTO resultado = proveedorServicio.altaProveedor(proveedorFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<ProveedorEntity> captor = ArgumentCaptor.forClass(ProveedorEntity.class);
        verify(proveedorRepository).save(captor.capture());
        ProveedorEntity entidadGuardada = captor.getValue();
        // El alta siempre debe registrar al proveedor como ACTIVO, sin importar lo que traiga el FormDTO
        assertThat(entidadGuardada.getEstado()).isEqualTo(Estado.ACTIVO);
        VersionProveedorEntity versionGuardada = entidadGuardada.getVersiones().get(0);
        assertThat(versionGuardada.isEsUltimaVersion()).isTrue();
        assertThat(versionGuardada.getCatalogoProveedor()).hasSize(2);

        assertThat(resultado.getEstado()).isEqualTo(Estado.ACTIVO);
        assertThat(resultado.getVersion().isEsUltimaVersion()).isTrue();
        assertThat(resultado.getVersion().getCatalogoProveedor()).hasSize(2);
    }

    @Test
    @DisplayName("CP-AP-08: altaProveedor normaliza lo que guarda y consulta la unicidad con los valores normalizados")
    void altaProveedor_debeNormalizarLoQueGuarda() {
        // === PREPARACION DE DATOS ===
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder()
                .razonSocial("  Maltería   del  Sur S.A. ")
                .nombreComercial("  Maltería  del Sur ")
                .cuit(" 30-12345678-9 ")
                .telefono(" 1122334455 ")
                .email("  Contacto@MalteriaDelSur.COM ")
                .direccion("  Ruta 5   Km 120 ")
                .build();
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrue("30-12345678-9")).thenReturn(false);
        when(versionProveedorRepository.existsByEmailIgnoreCaseAndEsUltimaVersionTrue("contacto@malteriadelsur.com")).thenReturn(false);
        when(localidadRepository.findById(LOCALIDAD_ID)).thenReturn(Optional.of(localidadEntity(LOCALIDAD_ID)));
        mockearCatalogo(versionFormDTO);
        when(proveedorRepository.save(any(ProveedorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        proveedorServicio.altaProveedor(proveedorFormDTO(versionFormDTO));

        // === ASSERTS ===
        ArgumentCaptor<ProveedorEntity> captor = ArgumentCaptor.forClass(ProveedorEntity.class);
        verify(proveedorRepository).save(captor.capture());
        VersionProveedorEntity versionGuardada = captor.getValue().getVersiones().get(0);
        assertThat(versionGuardada.getRazonSocial()).isEqualTo("Maltería del Sur S.A.");
        assertThat(versionGuardada.getNombreComercial()).isEqualTo("Maltería del Sur");
        assertThat(versionGuardada.getCuit()).isEqualTo("30-12345678-9");
        assertThat(versionGuardada.getTelefono()).isEqualTo("1122334455");
        assertThat(versionGuardada.getEmail()).isEqualTo("contacto@malteriadelsur.com");
        assertThat(versionGuardada.getDireccion()).isEqualTo("Ruta 5 Km 120");
        // El DTO no se modifica
        assertThat(versionFormDTO.getRazonSocial()).isEqualTo("  Maltería   del  Sur S.A. ");
        assertThat(versionFormDTO.getEmail()).isEqualTo("  Contacto@MalteriaDelSur.COM ");
    }

    @ParameterizedTest(name = "CP-AP-09: {0}")
    @MethodSource("versionesConDatoObligatorioVacio")
    @DisplayName("CP-AP-09: altaProveedor lanza ReglaNegocioException y no consulta la BD cuando falta un dato obligatorio")
    void altaProveedor_debeRechazarDatosObligatoriosVacios(String descripcion, VersionProveedorFormDTO version, String mensaje) {
        assertThatThrownBy(() -> proveedorServicio.altaProveedor(proveedorFormDTO(version)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(mensaje);

        verifyNoRepositoryInteractions();
    }

    @ParameterizedTest(name = "CP-AP-10: {0}")
    @MethodSource("versionesConDatoSobreElMaximo")
    @DisplayName("CP-AP-10: altaProveedor lanza ReglaNegocioException y no consulta la BD cuando un dato supera su longitud máxima")
    void altaProveedor_debeRechazarDatosSobreElMaximo(String descripcion, VersionProveedorFormDTO version, String mensaje) {
        assertThatThrownBy(() -> proveedorServicio.altaProveedor(proveedorFormDTO(version)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(mensaje);

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-AP-11: altaProveedor persiste cuando los datos tienen exactamente su longitud máxima")
    void altaProveedor_debePersistirConDatosEnElMaximo() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder()
                .razonSocial("a".repeat(100))
                .nombreComercial("b".repeat(100))
                .email("c".repeat(243) + "@example.com")
                .direccion("d".repeat(255))
                .build();
        mockearAltaExitosa(versionFormDTO);

        proveedorServicio.altaProveedor(proveedorFormDTO(versionFormDTO));

        verify(proveedorRepository).save(any(ProveedorEntity.class));
    }

    @ParameterizedTest(name = "CP-AP-12: {0}")
    @MethodSource("versionesConCuitInvalido")
    @DisplayName("CP-AP-12: altaProveedor lanza ReglaNegocioException y no consulta la BD cuando el CUIT no tiene 11 dígitos")
    void altaProveedor_debeRechazarCuitInvalido(String descripcion, VersionProveedorFormDTO version) {
        assertThatThrownBy(() -> proveedorServicio.altaProveedor(proveedorFormDTO(version)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El CUIT debe tener exactamente 11 dígitos");

        verifyNoRepositoryInteractions();
    }

    @ParameterizedTest(name = "CP-AP-13: CUIT {0}")
    @ValueSource(strings = {"30-12345678-9", "30 12345678 9", "30.12345678.9", " 30123456789 "})
    @DisplayName("CP-AP-13: altaProveedor acepta un CUIT con o sin separadores y lo guarda con el formato XX-XXXXXXXX-X")
    void altaProveedor_debeGuardarElCuitSoloConDigitos(String cuit) {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().cuit(cuit).build();
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrue("30-12345678-9")).thenReturn(false);
        when(localidadRepository.findById(LOCALIDAD_ID)).thenReturn(Optional.of(localidadEntity(LOCALIDAD_ID)));
        mockearCatalogo(versionFormDTO);
        when(proveedorRepository.save(any(ProveedorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        proveedorServicio.altaProveedor(proveedorFormDTO(versionFormDTO));

        ArgumentCaptor<ProveedorEntity> captor = ArgumentCaptor.forClass(ProveedorEntity.class);
        verify(proveedorRepository).save(captor.capture());
        assertThat(captor.getValue().getVersiones().get(0).getCuit()).isEqualTo("30-12345678-9");
    }

    @ParameterizedTest(name = "CP-AP-14: {0}")
    @MethodSource("versionesConTelefonoInvalido")
    @DisplayName("CP-AP-14: altaProveedor lanza ReglaNegocioException y no consulta la BD cuando el teléfono no tiene solo dígitos o supera los 15")
    void altaProveedor_debeRechazarTelefonoInvalido(String descripcion, VersionProveedorFormDTO version, String mensaje) {
        assertThatThrownBy(() -> proveedorServicio.altaProveedor(proveedorFormDTO(version)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(mensaje);

        verifyNoRepositoryInteractions();
    }

    @ParameterizedTest(name = "CP-AP-40: correo {0}")
    @MethodSource("versionesConCorreoInvalido")
    @DisplayName("CP-AP-40: altaProveedor lanza ReglaNegocioException y no consulta la BD cuando el correo electrónico no tiene un formato válido")
    void altaProveedor_debeRechazarCorreoConFormatoInvalido(String descripcion, VersionProveedorFormDTO version) {
        // Act + Assert
        assertThatThrownBy(() -> proveedorServicio.altaProveedor(proveedorFormDTO(version)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El correo electrónico no tiene un formato válido");

        verifyNoRepositoryInteractions();
    }

    @ParameterizedTest(name = "CP-AP-15: teléfono [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("CP-AP-15: altaProveedor persiste con teléfono nulo cuando el teléfono no se informa (opcional)")
    void altaProveedor_debePersistirSinTelefono(String telefono) {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().telefono(telefono).build();
        mockearAltaExitosa(versionFormDTO);

        proveedorServicio.altaProveedor(proveedorFormDTO(versionFormDTO));

        ArgumentCaptor<ProveedorEntity> captor = ArgumentCaptor.forClass(ProveedorEntity.class);
        verify(proveedorRepository).save(captor.capture());
        assertThat(captor.getValue().getVersiones().get(0).getTelefono()).isNull();
    }

    @Test
    @DisplayName("CP-AP-16: altaProveedor no aplica unicidad al teléfono: dos proveedores pueden tener el mismo")
    void altaProveedor_debePermitirTelefonoRepetido() {
        VersionProveedorFormDTO primera = versionValidaBuilder().telefono("1122334455").build();
        VersionProveedorFormDTO segunda = versionValidaBuilder().razonSocial("Lupulera del Norte S.A.").cuit("30987654321")
                .email("ventas@lupuleradelnorte.com").telefono("1122334455").build();

        mockearAltaExitosa(primera);
        proveedorServicio.altaProveedor(proveedorFormDTO(primera));
        mockearAltaExitosa(segunda);
        proveedorServicio.altaProveedor(proveedorFormDTO(segunda));

        ArgumentCaptor<ProveedorEntity> captor = ArgumentCaptor.forClass(ProveedorEntity.class);
        verify(proveedorRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(proveedor -> proveedor.getVersiones().get(0).getTelefono())
                .containsExactly("1122334455", "1122334455");
    }

    @Test
    @DisplayName("CP-AP-17: altaProveedor lanza RecursoDuplicadoException cuando el correo electrónico ya está registrado por un proveedor activo")
    void altaProveedor_debeRechazarCorreoDuplicado() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().email("  CONTACTO@malteriadelsur.com ").build();
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrue(ValidacionesTexto.formatearCuit(versionFormDTO.getCuit()))).thenReturn(false);
        when(versionProveedorRepository.existsByEmailIgnoreCaseAndEsUltimaVersionTrue("contacto@malteriadelsur.com")).thenReturn(true);

        assertThatThrownBy(() -> proveedorServicio.altaProveedor(proveedorFormDTO(versionFormDTO)))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un proveedor con el correo electrónico 'contacto@malteriadelsur.com'");

        verifyNoInteractions(proveedorRepository, localidadRepository, insumoRepository, presentacionComercialRepository);
    }

    @Test
    @DisplayName("CP-AP-19: altaProveedor detecta el CUIT duplicado aunque se escriba con guiones")
    void altaProveedor_debeDetectarCuitDuplicadoConSeparadores() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().cuit("30-11111111-1").build();
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrue("30-11111111-1")).thenReturn(true);

        assertThatThrownBy(() -> proveedorServicio.altaProveedor(proveedorFormDTO(versionFormDTO)))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un proveedor con el CUIT 30-11111111-1");

        verify(proveedorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AP-41: altaProveedor detecta el CUIT duplicado aunque se escriba solo con dígitos, porque se compara con el formato XX-XXXXXXXX-X")
    void altaProveedor_debeDetectarCuitDuplicadoEscritoSoloConDigitos() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().cuit("20123456786").build();
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrue("20-12345678-6")).thenReturn(true);

        assertThatThrownBy(() -> proveedorServicio.altaProveedor(proveedorFormDTO(versionFormDTO)))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un proveedor con el CUIT 20-12345678-6");

        verify(proveedorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AP-20: altaProveedor conserva la puntuación de la razón social al guardarla (ACME S.A. y ACME SA son distintas)")
    void altaProveedor_debeConservarLaPuntuacionDeLaRazonSocial() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().razonSocial("ACME S.A.").build();
        mockearAltaExitosa(versionFormDTO);

        proveedorServicio.altaProveedor(proveedorFormDTO(versionFormDTO));

        ArgumentCaptor<ProveedorEntity> captor = ArgumentCaptor.forClass(ProveedorEntity.class);
        verify(proveedorRepository).save(captor.capture());
        assertThat(captor.getValue().getVersiones().get(0).getRazonSocial()).isEqualTo("ACME S.A.");
    }

    @Test
    @DisplayName("CP-AP-22: altaProveedor traduce la violación del índice único del CUIT al mensaje del campo (pedido simultáneo)")
    void altaProveedor_debeTraducirViolacionDelIndiceDeCuit() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().build();
        mockearAltaSinGuardado(versionFormDTO);
        when(proveedorRepository.save(any(ProveedorEntity.class)))
                .thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_version_proveedor_cuit"));

        assertThatThrownBy(() -> proveedorServicio.altaProveedor(proveedorFormDTO(versionFormDTO)))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un proveedor con el CUIT 30-12345678-9");
    }

    @Test
    @DisplayName("CP-AP-23: altaProveedor traduce la violación del índice único del correo electrónico al mensaje del campo (pedido simultáneo)")
    void altaProveedor_debeTraducirViolacionDelIndiceDeCorreo() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().build();
        mockearAltaSinGuardado(versionFormDTO);
        when(proveedorRepository.save(any(ProveedorEntity.class)))
                .thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_version_proveedor_email"));

        assertThatThrownBy(() -> proveedorServicio.altaProveedor(proveedorFormDTO(versionFormDTO)))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un proveedor con el correo electrónico 'contacto@malteriadelsur.com'");
    }

    @Test
    @DisplayName("CP-AP-24: altaProveedor reutiliza el CUIT y el correo de un proveedor dado de baja (las consultas solo ven proveedores activos)")
    void altaProveedor_debeReutilizarDatosDeUnProveedorDadoDeBaja() {
        // === PREPARACION DE DATOS ===
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().build();
        // Solo un proveedor con estado BAJA tiene estos datos: las consultas de unicidad consideran padreActivo = true
        mockearAltaExitosa(versionFormDTO);

        // === EJECUCION ===
        ProveedorResponseDTO resultado = proveedorServicio.altaProveedor(proveedorFormDTO(versionFormDTO));

        // === ASSERTS ===
        ArgumentCaptor<ProveedorEntity> captor = ArgumentCaptor.forClass(ProveedorEntity.class);
        verify(proveedorRepository).save(captor.capture());
        VersionProveedorEntity versionGuardada = captor.getValue().getVersiones().get(0);
        assertThat(captor.getValue().getEstado()).isEqualTo(Estado.ACTIVO);
        assertThat(versionGuardada.isEsUltimaVersion()).isTrue();
        assertThat(versionGuardada.isPadreActivo()).isTrue();
        assertThat(resultado.getEstado()).isEqualTo(Estado.ACTIVO);
    }

    @Test
    @DisplayName("CP-AP-25: altaProveedor guarda y confirma en la base de datos dentro del método (save y luego flush)")
    void altaProveedor_debeGuardarYConfirmarEnOrden() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().build();
        mockearAltaExitosa(versionFormDTO);

        proveedorServicio.altaProveedor(proveedorFormDTO(versionFormDTO));

        InOrder orden = inOrder(proveedorRepository);
        orden.verify(proveedorRepository).save(any(ProveedorEntity.class));
        orden.verify(proveedorRepository).flush();
    }

    @Test
    @DisplayName("CP-AP-26: altaProveedor lanza ReglaNegocioException y no consulta la BD cuando el catálogo repite el mismo insumo con la misma presentación comercial")
    void altaProveedor_debeRechazarCatalogoConItemRepetido() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder()
                .catalogoProveedor(List.of(catalogoProveedorFormDTO(PRESENTACION_ID, INSUMO_ID), catalogoProveedorFormDTO(PRESENTACION_ID, INSUMO_ID)))
                .build();

        assertThatThrownBy(() -> proveedorServicio.altaProveedor(proveedorFormDTO(versionFormDTO)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El catálogo no puede repetir el mismo insumo con la misma presentación comercial");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-AP-27: altaProveedor acepta el mismo insumo con distinta presentación comercial y la misma presentación con distinto insumo")
    void altaProveedor_debeAceptarItemsQueDifierenEnInsumoOEnPresentacion() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder()
                .catalogoProveedor(List.of(
                        catalogoProveedorFormDTO(PRESENTACION_ID, INSUMO_ID),
                        catalogoProveedorFormDTO(2L, INSUMO_ID),
                        catalogoProveedorFormDTO(PRESENTACION_ID, 2L)))
                .build();
        mockearAltaExitosa(versionFormDTO);

        proveedorServicio.altaProveedor(proveedorFormDTO(versionFormDTO));

        ArgumentCaptor<ProveedorEntity> captor = ArgumentCaptor.forClass(ProveedorEntity.class);
        verify(proveedorRepository).save(captor.capture());
        assertThat(captor.getValue().getVersiones().get(0).getCatalogoProveedor()).hasSize(3);
    }

    @Test
    @DisplayName("CP-AP-28: altaProveedor traduce la violación del índice único del ítem de catálogo al mensaje del catálogo")
    void altaProveedor_debeTraducirViolacionDelIndiceDelItemDeCatalogo() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().build();
        mockearAltaSinGuardado(versionFormDTO);
        when(proveedorRepository.save(any(ProveedorEntity.class)))
                .thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_catalogo_proveedor_item"));

        assertThatThrownBy(() -> proveedorServicio.altaProveedor(proveedorFormDTO(versionFormDTO)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El catálogo no puede repetir el mismo insumo con la misma presentación comercial");
    }

    @Test
    @DisplayName("CP-AP-29: altaProveedor persiste dos proveedores con la misma razón social y distinto CUIT (la razón social no es única)")
    void altaProveedor_debePermitirRazonSocialRepetida() {
        // === PREPARACION DE DATOS ===
        VersionProveedorFormDTO primera = versionValidaBuilder().build();
        VersionProveedorFormDTO segunda = versionValidaBuilder().cuit("30987654321").email("ventas@malteriadelsur.com").build();

        // === EJECUCION ===
        mockearAltaExitosa(primera);
        proveedorServicio.altaProveedor(proveedorFormDTO(primera));
        mockearAltaExitosa(segunda);
        proveedorServicio.altaProveedor(proveedorFormDTO(segunda));

        // === ASSERTS ===
        ArgumentCaptor<ProveedorEntity> captor = ArgumentCaptor.forClass(ProveedorEntity.class);
        verify(proveedorRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(proveedor -> proveedor.getVersiones().get(0).getRazonSocial())
                .containsExactly("Maltería del Sur S.A.", "Maltería del Sur S.A.");
    }

    @ParameterizedTest(name = "CP-AP-30: {0}")
    @MethodSource("versionesConCuitVacio")
    @DisplayName("CP-AP-30: altaProveedor lanza ReglaNegocioException y no consulta la BD cuando el CUIT no se informa")
    void altaProveedor_debeRechazarCuitVacio(String descripcion, VersionProveedorFormDTO version) {
        assertThatThrownBy(() -> proveedorServicio.altaProveedor(proveedorFormDTO(version)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El CUIT es obligatorio");

        verifyNoRepositoryInteractions();
    }

    @ParameterizedTest(name = "CP-AP-31: {0}")
    @MethodSource("versionesConItemDeCatalogoIncompleto")
    @DisplayName("CP-AP-31: altaProveedor lanza ReglaNegocioException y no consulta la BD cuando un ítem del catálogo es nulo o no indica el insumo o la presentación comercial")
    void altaProveedor_debeRechazarItemDeCatalogoIncompleto(String descripcion, VersionProveedorFormDTO version, String mensaje) {
        assertThatThrownBy(() -> proveedorServicio.altaProveedor(proveedorFormDTO(version)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(mensaje);

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-AP-32: altaProveedor lanza ReglaNegocioException y no consulta la BD cuando la localidad no se informa")
    void altaProveedor_debeRechazarLocalidadNula() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().idLocalidad(null).build();

        assertThatThrownBy(() -> proveedorServicio.altaProveedor(proveedorFormDTO(versionFormDTO)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La localidad es obligatoria");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-AP-33: altaProveedor lanza ReglaNegocioException y no consulta la BD cuando no se informan los datos del proveedor")
    void altaProveedor_debeRechazarVersionNula() {
        ProveedorFormDTO proveedorFormDTO = proveedorFormDTO(null);

        assertThatThrownBy(() -> proveedorServicio.altaProveedor(proveedorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los datos del proveedor son obligatorios");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-AP-34: altaProveedor persiste un catálogo con exactamente 100 ítems (máximo permitido)")
    void altaProveedor_debePersistirCatalogoEnElMaximo() {
        // === PREPARACION DE DATOS ===
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().catalogoProveedor(catalogoConItemsDistintos(100)).build();
        mockearAltaExitosa(versionFormDTO);

        // === EJECUCION ===
        proveedorServicio.altaProveedor(proveedorFormDTO(versionFormDTO));

        // === ASSERTS ===
        ArgumentCaptor<ProveedorEntity> captor = ArgumentCaptor.forClass(ProveedorEntity.class);
        verify(proveedorRepository).save(captor.capture());
        assertThat(captor.getValue().getVersiones().get(0).getCatalogoProveedor()).hasSize(100);
    }

    @Test
    @DisplayName("CP-AP-35: altaProveedor lanza ReglaNegocioException y no consulta la BD cuando el catálogo supera los 100 ítems")
    void altaProveedor_debeRechazarCatalogoSobreElMaximo() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().catalogoProveedor(catalogoConItemsDistintos(101)).build();

        assertThatThrownBy(() -> proveedorServicio.altaProveedor(proveedorFormDTO(versionFormDTO)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El catálogo no puede superar 100 ítems");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-AP-36: altaProveedor traduce la violación de un índice único de la razón social al mensaje genérico (la razón social no tiene mensaje propio)")
    void altaProveedor_debeTraducirIndiceDeRazonSocialAlMensajeGenerico() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().build();
        mockearAltaSinGuardado(versionFormDTO);
        when(proveedorRepository.save(any(ProveedorEntity.class)))
                .thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_version_proveedor_razon_social"));

        assertThatThrownBy(() -> proveedorServicio.altaProveedor(proveedorFormDTO(versionFormDTO)))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage(GuardadoUnico.MENSAJE_DUPLICADO_GENERICO);
    }

    @ParameterizedTest(name = "CP-AP-37: {0}")
    @MethodSource("versionesConVariosDatosInvalidos")
    @DisplayName("CP-AP-37: altaProveedor informa el primer dato inválido en el orden de los campos del formulario")
    void altaProveedor_debeValidarEnElOrdenDelFormulario(String descripcion, VersionProveedorFormDTO version, String mensaje) {
        assertThatThrownBy(() -> proveedorServicio.altaProveedor(proveedorFormDTO(version)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(mensaje);

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-AP-38: altaProveedor indica en la excepción de duplicado el campo repetido (cuit o email) al detectarlo con la consulta")
    void altaProveedor_debeIndicarElCampoDuplicadoDetectadoPorLaConsulta() {
        // Arrange
        VersionProveedorFormDTO conCuitRepetido = versionValidaBuilder().cuit("30111111111").build();
        VersionProveedorFormDTO conCorreoRepetido = versionValidaBuilder().cuit("30222222222").email("contacto@malteriadelsur.com").build();
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrue("30-11111111-1")).thenReturn(true);
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrue("30-22222222-2")).thenReturn(false);
        when(versionProveedorRepository.existsByEmailIgnoreCaseAndEsUltimaVersionTrue("contacto@malteriadelsur.com")).thenReturn(true);

        // Act
        Throwable cuit = catchThrowable(() -> proveedorServicio.altaProveedor(proveedorFormDTO(conCuitRepetido)));
        Throwable correo = catchThrowable(() -> proveedorServicio.altaProveedor(proveedorFormDTO(conCorreoRepetido)));

        // Assert
        assertThat(cuit).isInstanceOf(RecursoDuplicadoException.class);
        assertThat(((RecursoDuplicadoException) cuit).getCampo()).isEqualTo("cuit");
        assertThat(correo).isInstanceOf(RecursoDuplicadoException.class);
        assertThat(((RecursoDuplicadoException) correo).getCampo()).isEqualTo("email");
        verify(proveedorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AP-39: altaProveedor indica en la excepción de duplicado el campo repetido (cuit o email) al traducir la violación de su índice único")
    void altaProveedor_debeIndicarElCampoDuplicadoAlTraducirLaViolacionDelIndice() {
        // Arrange
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().build();
        mockearAltaSinGuardado(versionFormDTO);
        when(proveedorRepository.save(any(ProveedorEntity.class)))
                .thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_version_proveedor_cuit"))
                .thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_version_proveedor_email"));

        // Act
        Throwable cuit = catchThrowable(() -> proveedorServicio.altaProveedor(proveedorFormDTO(versionFormDTO)));
        Throwable correo = catchThrowable(() -> proveedorServicio.altaProveedor(proveedorFormDTO(versionFormDTO)));

        // Assert
        assertThat(cuit).isInstanceOf(RecursoDuplicadoException.class);
        assertThat(((RecursoDuplicadoException) cuit).getCampo()).isEqualTo("cuit");
        assertThat(correo).isInstanceOf(RecursoDuplicadoException.class);
        assertThat(((RecursoDuplicadoException) correo).getCampo()).isEqualTo("email");
    }

    // ==================== modificarProveedor ====================

    @Test
    @DisplayName("CP-MP-02: modificarProveedor lanza RecursoDuplicadoException cuando el CUIT está en uso por otro proveedor activo")
    void modificarProveedor_debeRechazarCuitEnUsoPorOtroProveedor() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().build();
        ProveedorFormDTO proveedorFormDTO = proveedorFormDTO(versionFormDTO);
        when(proveedorRepository.findByIdParaActualizarVersiones(1L))
                .thenReturn(Optional.of(crearProveedorEntityConVersionActiva(1L, "Maltería del Norte S.A.", "30987654321")));
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrueAndProveedorIdNot(
                ValidacionesTexto.formatearCuit(versionFormDTO.getCuit()), 1L)).thenReturn(true);

        assertThatThrownBy(() -> proveedorServicio.modificarProveedor(1L, proveedorFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un proveedor con el CUIT " + ValidacionesTexto.formatearCuit(versionFormDTO.getCuit()));

        verify(proveedorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MP-03: modificarProveedor permite conservar el CUIT y el correo propios y persiste la nueva versión (camino feliz)")
    void modificarProveedor_debePermitirConservarCuitYCorreoPropios() {
        // === PREPARACION DE DATOS ===
        VersionProveedorEntity versionActivaPrevia = crearVersionProveedorEntity(10L, "Maltería del Sur S.A.", "30111111111", true);
        ProveedorEntity proveedorEntityExistente = crearProveedorEntityConVersiones(1L, versionActivaPrevia);
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder()
                .razonSocial("Maltería del Sur S.A.")
                .cuit("30111111111")
                .build();
        ProveedorFormDTO proveedorFormDTO = proveedorFormDTO(versionFormDTO);
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrueAndProveedorIdNot(
                "30-11111111-1", 1L)).thenReturn(false);
        when(versionProveedorRepository.existsByEmailIgnoreCaseAndEsUltimaVersionTrueAndProveedorIdNot(
                versionFormDTO.getEmail(), 1L)).thenReturn(false);
        when(proveedorRepository.findByIdParaActualizarVersiones(1L)).thenReturn(Optional.of(proveedorEntityExistente));
        when(localidadRepository.findById(LOCALIDAD_ID)).thenReturn(Optional.of(localidadEntity(LOCALIDAD_ID)));
        mockearCatalogo(versionFormDTO);
        when(proveedorRepository.save(proveedorEntityExistente)).thenReturn(proveedorEntityExistente);

        // === EJECUCION ===
        ProveedorResponseDTO resultado = proveedorServicio.modificarProveedor(1L, proveedorFormDTO);

        // === ASSERTS ===
        assertThat(resultado).isNotNull();
        assertThat(resultado.getVersion().getRazonSocial()).isEqualTo("Maltería del Sur S.A.");
        verify(proveedorRepository, times(2)).save(proveedorEntityExistente);
    }

    @Test
    @DisplayName("CP-MP-04: modificarProveedor lanza RecursoNoEncontradoException y no persiste cuando el proveedor no existe o está dado de baja")
    void modificarProveedor_debeLanzarExcepcionSiNoExiste() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().build();
        ProveedorFormDTO proveedorFormDTO = proveedorFormDTO(versionFormDTO);
        when(proveedorRepository.findByIdParaActualizarVersiones(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proveedorServicio.modificarProveedor(99L, proveedorFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el proveedor con ID: 99");

        verify(proveedorRepository).findByIdParaActualizarVersiones(99L);
        verify(proveedorRepository, never()).save(any());
        verifyNoInteractions(versionProveedorRepository);
    }

    @Test
    @DisplayName("CP-MP-05: modificarProveedor lanza RecursoNoEncontradoException cuando la localidad referenciada no existe")
    void modificarProveedor_debeRechazarLocalidadInexistente() {
        // === PREPARACION DE DATOS ===
        VersionProveedorEntity versionActivaPrevia = crearVersionProveedorEntity(10L, "Maltería del Sur S.A.", "30-11111111-1", true);
        ProveedorEntity proveedorEntityExistente = crearProveedorEntityConVersiones(1L, versionActivaPrevia);
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().idLocalidad(99L).build();
        ProveedorFormDTO proveedorFormDTO = proveedorFormDTO(versionFormDTO);
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrueAndProveedorIdNot(
                ValidacionesTexto.formatearCuit(versionFormDTO.getCuit()), 1L)).thenReturn(false);
        when(proveedorRepository.findByIdParaActualizarVersiones(1L)).thenReturn(Optional.of(proveedorEntityExistente));
        when(localidadRepository.findById(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> proveedorServicio.modificarProveedor(1L, proveedorFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la localidad con ID: 99");

        // La versión anterior se construye (y valida) antes de desactivar la actual, así que si la
        // localidad no existe, la versión previa nunca se toca.
        assertThat(versionActivaPrevia.isEsUltimaVersion()).isTrue();
        verify(proveedorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MP-06: modificarProveedor lanza RecursoNoEncontradoException y no persiste cuando el insumo de un ítem del catálogo no existe")
    void modificarProveedor_debeRechazarInsumoInexistente() {
        // === PREPARACION DE DATOS ===
        VersionProveedorEntity versionActivaPrevia = crearVersionProveedorEntity(10L, "Maltería del Sur S.A.", "30-11111111-1", true);
        ProveedorEntity proveedorEntityExistente = crearProveedorEntityConVersiones(1L, versionActivaPrevia);
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder()
                .catalogoProveedor(List.of(catalogoProveedorFormDTO(PRESENTACION_ID, 99L)))
                .build();
        ProveedorFormDTO proveedorFormDTO = proveedorFormDTO(versionFormDTO);
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrueAndProveedorIdNot(
                ValidacionesTexto.formatearCuit(versionFormDTO.getCuit()), 1L)).thenReturn(false);
        when(proveedorRepository.findByIdParaActualizarVersiones(1L)).thenReturn(Optional.of(proveedorEntityExistente));
        when(localidadRepository.findById(LOCALIDAD_ID)).thenReturn(Optional.of(localidadEntity(LOCALIDAD_ID)));
        when(presentacionComercialRepository.findById(PRESENTACION_ID)).thenReturn(Optional.of(presentacionComercialEntity(PRESENTACION_ID)));
        when(insumoRepository.findById(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> proveedorServicio.modificarProveedor(1L, proveedorFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el insumo con ID: 99");

        verify(proveedorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MP-07: modificarProveedor lanza RecursoNoEncontradoException y no persiste cuando la presentación comercial de un ítem del catálogo no existe")
    void modificarProveedor_debeRechazarPresentacionComercialInexistente() {
        // === PREPARACION DE DATOS ===
        VersionProveedorEntity versionActivaPrevia = crearVersionProveedorEntity(10L, "Maltería del Sur S.A.", "30-11111111-1", true);
        ProveedorEntity proveedorEntityExistente = crearProveedorEntityConVersiones(1L, versionActivaPrevia);
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder()
                .catalogoProveedor(List.of(catalogoProveedorFormDTO(99L, INSUMO_ID)))
                .build();
        ProveedorFormDTO proveedorFormDTO = proveedorFormDTO(versionFormDTO);
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrueAndProveedorIdNot(
                ValidacionesTexto.formatearCuit(versionFormDTO.getCuit()), 1L)).thenReturn(false);
        when(proveedorRepository.findByIdParaActualizarVersiones(1L)).thenReturn(Optional.of(proveedorEntityExistente));
        when(localidadRepository.findById(LOCALIDAD_ID)).thenReturn(Optional.of(localidadEntity(LOCALIDAD_ID)));
        when(presentacionComercialRepository.findById(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> proveedorServicio.modificarProveedor(1L, proveedorFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la presentación comercial con ID: 99");

        verify(proveedorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MP-08: modificarProveedor desactiva la versión previa, agrega la nueva y persiste (camino feliz)")
    void modificarProveedor_debeVersionarExitosamente() {
        // === PREPARACION DE DATOS ===
        VersionProveedorEntity versionActivaPrevia = crearVersionProveedorEntity(10L, "Maltería del Sur S.A.", "30-11111111-1", true);
        ProveedorEntity proveedorEntityExistente = crearProveedorEntityConVersiones(1L, versionActivaPrevia);
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder()
                .razonSocial("Maltería del Sur S.A. - Sucursal Norte")
                .build();
        ProveedorFormDTO proveedorFormDTO = proveedorFormDTO(versionFormDTO);
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrueAndProveedorIdNot(
                ValidacionesTexto.formatearCuit(versionFormDTO.getCuit()), 1L)).thenReturn(false);
        when(proveedorRepository.findByIdParaActualizarVersiones(1L)).thenReturn(Optional.of(proveedorEntityExistente));
        when(localidadRepository.findById(LOCALIDAD_ID)).thenReturn(Optional.of(localidadEntity(LOCALIDAD_ID)));
        mockearCatalogo(versionFormDTO);
        when(proveedorRepository.save(proveedorEntityExistente)).thenReturn(proveedorEntityExistente);

        // === EJECUCION ===
        ProveedorResponseDTO resultado = proveedorServicio.modificarProveedor(1L, proveedorFormDTO);

        // === ASSERTS ===
        assertThat(resultado).isNotNull();
        assertThat(versionActivaPrevia.isEsUltimaVersion()).isFalse();
        assertThat(proveedorEntityExistente.getVersiones()).hasSize(2);
        long cantidadActivas = proveedorEntityExistente.getVersiones().stream().filter(VersionProveedorEntity::isEsUltimaVersion).count();
        assertThat(cantidadActivas).isEqualTo(1);
        assertThat(resultado.getVersion().getRazonSocial()).isEqualTo("Maltería del Sur S.A. - Sucursal Norte");
        verify(proveedorRepository, times(2)).save(proveedorEntityExistente);
    }

    @Test
    @DisplayName("CP-MP-09: modificarProveedor versiona correctamente sobre un historial existente de múltiples versiones")
    void modificarProveedor_debeVersionarSobreHistorialExistente() {
        // === PREPARACION DE DATOS ===
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().build();
        ProveedorFormDTO proveedorFormDTO = proveedorFormDTO(versionFormDTO);
        VersionProveedorEntity version1 = crearVersionProveedorEntity(10L, "Maltería del Sur v1", "30-11111111-1", false);
        VersionProveedorEntity version2 = crearVersionProveedorEntity(11L, "Maltería del Sur v2", "30-11111111-1", false);
        VersionProveedorEntity version3 = crearVersionProveedorEntity(12L, "Maltería del Sur S.A.", "30-11111111-1", true);
        ProveedorEntity proveedorEntityExistente = crearProveedorEntityConVersiones(1L, version1, version2, version3);
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrueAndProveedorIdNot(
                ValidacionesTexto.formatearCuit(versionFormDTO.getCuit()), 1L)).thenReturn(false);
        when(proveedorRepository.findByIdParaActualizarVersiones(1L)).thenReturn(Optional.of(proveedorEntityExistente));
        when(localidadRepository.findById(LOCALIDAD_ID)).thenReturn(Optional.of(localidadEntity(LOCALIDAD_ID)));
        mockearCatalogo(versionFormDTO);
        when(proveedorRepository.save(proveedorEntityExistente)).thenReturn(proveedorEntityExistente);

        // === EJECUCION ===
        proveedorServicio.modificarProveedor(1L, proveedorFormDTO);

        // === ASSERTS ===
        assertThat(proveedorEntityExistente.getVersiones()).hasSize(4);
        long cantidadActivas = proveedorEntityExistente.getVersiones().stream().filter(VersionProveedorEntity::isEsUltimaVersion).count();
        assertThat(cantidadActivas).isEqualTo(1);
    }

    @Test
    @DisplayName("CP-MP-10: modificarProveedor normaliza lo que guarda y consulta la unicidad con los valores normalizados")
    void modificarProveedor_debeNormalizarLoQueGuarda() {
        // === PREPARACION DE DATOS ===
        VersionProveedorEntity versionActivaPrevia = crearVersionProveedorEntity(10L, "Maltería del Sur S.A.", "30111111111", true);
        ProveedorEntity proveedorEntityExistente = crearProveedorEntityConVersiones(1L, versionActivaPrevia);
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder()
                .razonSocial("  Maltería   del  Sur S.A. ")
                .nombreComercial("  Maltería  del Sur ")
                .cuit(" 30-12345678-9 ")
                .telefono(" 1122334455 ")
                .email("  Contacto@MalteriaDelSur.COM ")
                .direccion("  Ruta 5   Km 120 ")
                .build();
        when(proveedorRepository.findByIdParaActualizarVersiones(1L)).thenReturn(Optional.of(proveedorEntityExistente));
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrueAndProveedorIdNot("30-12345678-9", 1L)).thenReturn(false);
        when(versionProveedorRepository.existsByEmailIgnoreCaseAndEsUltimaVersionTrueAndProveedorIdNot("contacto@malteriadelsur.com", 1L)).thenReturn(false);
        when(localidadRepository.findById(LOCALIDAD_ID)).thenReturn(Optional.of(localidadEntity(LOCALIDAD_ID)));
        mockearCatalogo(versionFormDTO);
        when(proveedorRepository.save(proveedorEntityExistente)).thenReturn(proveedorEntityExistente);

        // === EJECUCION ===
        proveedorServicio.modificarProveedor(1L, proveedorFormDTO(versionFormDTO));

        // === ASSERTS ===
        VersionProveedorEntity nuevaVersion = proveedorEntityExistente.getVersiones().get(1);
        assertThat(nuevaVersion.getRazonSocial()).isEqualTo("Maltería del Sur S.A.");
        assertThat(nuevaVersion.getNombreComercial()).isEqualTo("Maltería del Sur");
        assertThat(nuevaVersion.getCuit()).isEqualTo("30-12345678-9");
        assertThat(nuevaVersion.getTelefono()).isEqualTo("1122334455");
        assertThat(nuevaVersion.getEmail()).isEqualTo("contacto@malteriadelsur.com");
        assertThat(nuevaVersion.getDireccion()).isEqualTo("Ruta 5 Km 120");
        assertThat(versionFormDTO.getRazonSocial()).isEqualTo("  Maltería   del  Sur S.A. ");
    }

    @ParameterizedTest(name = "CP-MP-11: {0}")
    @MethodSource("versionesConDatoObligatorioVacio")
    @DisplayName("CP-MP-11: modificarProveedor lanza ReglaNegocioException y no consulta la BD cuando falta un dato obligatorio")
    void modificarProveedor_debeRechazarDatosObligatoriosVacios(String descripcion, VersionProveedorFormDTO version, String mensaje) {
        assertThatThrownBy(() -> proveedorServicio.modificarProveedor(1L, proveedorFormDTO(version)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(mensaje);

        verifyNoRepositoryInteractions();
    }

    @ParameterizedTest(name = "CP-MP-12: {0}")
    @MethodSource("versionesConDatoSobreElMaximo")
    @DisplayName("CP-MP-12: modificarProveedor lanza ReglaNegocioException y no consulta la BD cuando un dato supera su longitud máxima")
    void modificarProveedor_debeRechazarDatosSobreElMaximo(String descripcion, VersionProveedorFormDTO version, String mensaje) {
        assertThatThrownBy(() -> proveedorServicio.modificarProveedor(1L, proveedorFormDTO(version)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(mensaje);

        verifyNoRepositoryInteractions();
    }

    @ParameterizedTest(name = "CP-MP-13: {0}")
    @MethodSource("versionesConCuitInvalido")
    @DisplayName("CP-MP-13: modificarProveedor lanza ReglaNegocioException y no consulta la BD cuando el CUIT no tiene 11 dígitos")
    void modificarProveedor_debeRechazarCuitInvalido(String descripcion, VersionProveedorFormDTO version) {
        assertThatThrownBy(() -> proveedorServicio.modificarProveedor(1L, proveedorFormDTO(version)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El CUIT debe tener exactamente 11 dígitos");

        verifyNoRepositoryInteractions();
    }

    @ParameterizedTest(name = "CP-MP-14: {0}")
    @MethodSource("versionesConTelefonoInvalido")
    @DisplayName("CP-MP-14: modificarProveedor lanza ReglaNegocioException y no consulta la BD cuando el teléfono no tiene solo dígitos o supera los 15")
    void modificarProveedor_debeRechazarTelefonoInvalido(String descripcion, VersionProveedorFormDTO version, String mensaje) {
        assertThatThrownBy(() -> proveedorServicio.modificarProveedor(1L, proveedorFormDTO(version)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(mensaje);

        verifyNoRepositoryInteractions();
    }

    @ParameterizedTest(name = "CP-MP-36: correo {0}")
    @MethodSource("versionesConCorreoInvalido")
    @DisplayName("CP-MP-36: modificarProveedor lanza ReglaNegocioException y no consulta la BD cuando el correo electrónico no tiene un formato válido")
    void modificarProveedor_debeRechazarCorreoConFormatoInvalido(String descripcion, VersionProveedorFormDTO version) {
        // Act + Assert
        assertThatThrownBy(() -> proveedorServicio.modificarProveedor(1L, proveedorFormDTO(version)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El correo electrónico no tiene un formato válido");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-MP-15: modificarProveedor lanza RecursoDuplicadoException y no persiste cuando el correo electrónico está en uso por otro proveedor activo")
    void modificarProveedor_debeRechazarCorreoEnUsoPorOtroProveedor() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().email("  CONTACTO@malteriadelsur.com ").build();
        when(proveedorRepository.findByIdParaActualizarVersiones(1L))
                .thenReturn(Optional.of(crearProveedorEntityConVersionActiva(1L, "Maltería del Norte S.A.", "30987654321")));
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrueAndProveedorIdNot(ValidacionesTexto.formatearCuit(versionFormDTO.getCuit()), 1L)).thenReturn(false);
        when(versionProveedorRepository.existsByEmailIgnoreCaseAndEsUltimaVersionTrueAndProveedorIdNot("contacto@malteriadelsur.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> proveedorServicio.modificarProveedor(1L, proveedorFormDTO(versionFormDTO)))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un proveedor con el correo electrónico 'contacto@malteriadelsur.com'");

        verify(proveedorRepository, never()).save(any());
    }

    @ParameterizedTest(name = "CP-MP-17: teléfono [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("CP-MP-17: modificarProveedor persiste con teléfono nulo cuando el teléfono no se informa (opcional)")
    void modificarProveedor_debePersistirSinTelefono(String telefono) {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().telefono(telefono).build();
        ProveedorEntity proveedorEntityExistente = crearProveedorEntityConVersiones(1L, crearVersionProveedorEntity(10L, "Maltería del Sur S.A.", "30111111111", true));
        mockearModificacionSinGuardado(versionFormDTO, proveedorEntityExistente);
        when(proveedorRepository.save(proveedorEntityExistente)).thenReturn(proveedorEntityExistente);

        proveedorServicio.modificarProveedor(1L, proveedorFormDTO(versionFormDTO));

        assertThat(proveedorEntityExistente.getVersiones().get(1).getTelefono()).isNull();
    }

    @Test
    @DisplayName("CP-MP-19: modificarProveedor traduce la violación del índice único del CUIT al mensaje del campo (pedido simultáneo)")
    void modificarProveedor_debeTraducirViolacionDelIndiceDeCuit() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().build();
        ProveedorEntity proveedorEntityExistente = crearProveedorEntityConVersiones(1L, crearVersionProveedorEntity(10L, "Maltería del Sur S.A.", "30111111111", true));
        mockearModificacionSinGuardado(versionFormDTO, proveedorEntityExistente);
        when(proveedorRepository.save(proveedorEntityExistente)).thenReturn(proveedorEntityExistente)
                .thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_version_proveedor_cuit"));

        assertThatThrownBy(() -> proveedorServicio.modificarProveedor(1L, proveedorFormDTO(versionFormDTO)))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un proveedor con el CUIT 30-12345678-9");
    }

    @Test
    @DisplayName("CP-MP-20: modificarProveedor traduce la violación del índice único del correo electrónico al mensaje del campo (pedido simultáneo)")
    void modificarProveedor_debeTraducirViolacionDelIndiceDeCorreo() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().build();
        ProveedorEntity proveedorEntityExistente = crearProveedorEntityConVersiones(1L, crearVersionProveedorEntity(10L, "Maltería del Sur S.A.", "30111111111", true));
        mockearModificacionSinGuardado(versionFormDTO, proveedorEntityExistente);
        when(proveedorRepository.save(proveedorEntityExistente)).thenReturn(proveedorEntityExistente)
                .thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_version_proveedor_email"));

        assertThatThrownBy(() -> proveedorServicio.modificarProveedor(1L, proveedorFormDTO(versionFormDTO)))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un proveedor con el correo electrónico 'contacto@malteriadelsur.com'");
    }

    @Test
    @DisplayName("CP-MP-21: modificarProveedor traduce la violación del índice de versión vigente a ReglaNegocioException (modificación simultánea)")
    void modificarProveedor_debeTraducirViolacionDelIndiceDeVersionVigente() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().build();
        ProveedorEntity proveedorEntityExistente = crearProveedorEntityConVersiones(1L, crearVersionProveedorEntity(10L, "Maltería del Sur S.A.", "30111111111", true));
        mockearModificacionSinGuardado(versionFormDTO, proveedorEntityExistente);
        when(proveedorRepository.save(proveedorEntityExistente)).thenReturn(proveedorEntityExistente)
                .thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_version_proveedor_ultima_version"));

        assertThatThrownBy(() -> proveedorServicio.modificarProveedor(1L, proveedorFormDTO(versionFormDTO)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El proveedor fue modificado por otra persona al mismo tiempo, intente nuevamente");
    }

    @Test
    @DisplayName("CP-MP-22: modificarProveedor guarda y confirma la versión anterior desactivada antes de insertar la nueva (save, flush, save, flush)")
    void modificarProveedor_debeConfirmarLaVersionAnteriorAntesDeInsertarLaNueva() {
        // === PREPARACION DE DATOS ===
        VersionProveedorEntity versionActivaPrevia = crearVersionProveedorEntity(10L, "Maltería del Sur S.A.", "30111111111", true);
        ProveedorEntity proveedorEntityExistente = crearProveedorEntityConVersiones(1L, versionActivaPrevia);
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().build();
        mockearModificacionSinGuardado(versionFormDTO, proveedorEntityExistente);
        // En cada save se registra cuántas versiones tiene el proveedor y si la anterior sigue vigente
        List<String> estadoEnCadaSave = new ArrayList<>();
        when(proveedorRepository.save(proveedorEntityExistente)).thenAnswer(invocation -> {
            estadoEnCadaSave.add(proveedorEntityExistente.getVersiones().size() + " versiones, anterior vigente: " + versionActivaPrevia.isEsUltimaVersion());
            return proveedorEntityExistente;
        });

        // === EJECUCION ===
        proveedorServicio.modificarProveedor(1L, proveedorFormDTO(versionFormDTO));

        // === ASSERTS ===
        assertThat(estadoEnCadaSave).containsExactly("1 versiones, anterior vigente: false", "2 versiones, anterior vigente: false");
        InOrder orden = inOrder(proveedorRepository);
        orden.verify(proveedorRepository).findByIdParaActualizarVersiones(1L);
        orden.verify(proveedorRepository).save(proveedorEntityExistente);
        orden.verify(proveedorRepository).flush();
        orden.verify(proveedorRepository).save(proveedorEntityExistente);
        orden.verify(proveedorRepository).flush();
        VersionProveedorEntity nuevaVersion = proveedorEntityExistente.getVersiones().get(1);
        assertThat(nuevaVersion.isEsUltimaVersion()).isTrue();
        assertThat(nuevaVersion.isPadreActivo()).isTrue();
    }

    @Test
    @DisplayName("CP-MP-23: modificarProveedor bloquea la fila del proveedor antes de validar la unicidad del CUIT y no usa la búsqueda sin bloqueo")
    void modificarProveedor_debeBloquearElProveedorAntesDeValidarLaUnicidad() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().build();
        ProveedorEntity proveedorEntityExistente = crearProveedorEntityConVersiones(1L, crearVersionProveedorEntity(10L, "Maltería del Sur S.A.", "30111111111", true));
        mockearModificacionSinGuardado(versionFormDTO, proveedorEntityExistente);
        when(proveedorRepository.save(proveedorEntityExistente)).thenReturn(proveedorEntityExistente);

        proveedorServicio.modificarProveedor(1L, proveedorFormDTO(versionFormDTO));

        InOrder orden = inOrder(proveedorRepository, versionProveedorRepository);
        orden.verify(proveedorRepository).findByIdParaActualizarVersiones(1L);
        orden.verify(versionProveedorRepository).existsByCuitAndEsUltimaVersionTrueAndProveedorIdNot(ValidacionesTexto.formatearCuit(versionFormDTO.getCuit()), 1L);
        verify(proveedorRepository, never()).findById(any());
    }

    @Test
    @DisplayName("CP-MP-24: modificarProveedor lanza ReglaNegocioException y no consulta la BD cuando el catálogo repite el mismo insumo con la misma presentación comercial")
    void modificarProveedor_debeRechazarCatalogoConItemRepetido() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder()
                .catalogoProveedor(List.of(catalogoProveedorFormDTO(PRESENTACION_ID, INSUMO_ID), catalogoProveedorFormDTO(PRESENTACION_ID, INSUMO_ID)))
                .build();

        assertThatThrownBy(() -> proveedorServicio.modificarProveedor(1L, proveedorFormDTO(versionFormDTO)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El catálogo no puede repetir el mismo insumo con la misma presentación comercial");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-MP-25: modificarProveedor traduce la violación del índice único del ítem de catálogo al mensaje del catálogo")
    void modificarProveedor_debeTraducirViolacionDelIndiceDelItemDeCatalogo() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().build();
        ProveedorEntity proveedorEntityExistente = crearProveedorEntityConVersiones(1L, crearVersionProveedorEntity(10L, "Maltería del Sur S.A.", "30111111111", true));
        mockearModificacionSinGuardado(versionFormDTO, proveedorEntityExistente);
        when(proveedorRepository.save(proveedorEntityExistente)).thenReturn(proveedorEntityExistente)
                .thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_catalogo_proveedor_item"));

        assertThatThrownBy(() -> proveedorServicio.modificarProveedor(1L, proveedorFormDTO(versionFormDTO)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El catálogo no puede repetir el mismo insumo con la misma presentación comercial");
    }

    @Test
    @DisplayName("CP-MP-26: modificarProveedor persiste cuando la razón social es la de otro proveedor activo (la razón social no es única)")
    void modificarProveedor_debePermitirRazonSocialDeOtroProveedor() {
        // === PREPARACION DE DATOS ===
        // "Lupulera del Norte S.A." es la razón social de otro proveedor activo
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().razonSocial("Lupulera del Norte S.A.").build();
        ProveedorEntity proveedorEntityExistente = crearProveedorEntityConVersiones(1L, crearVersionProveedorEntity(10L, "Maltería del Sur S.A.", "30123456789", true));
        mockearModificacionSinGuardado(versionFormDTO, proveedorEntityExistente);
        when(proveedorRepository.save(proveedorEntityExistente)).thenReturn(proveedorEntityExistente);

        // === EJECUCION ===
        ProveedorResponseDTO resultado = proveedorServicio.modificarProveedor(1L, proveedorFormDTO(versionFormDTO));

        // === ASSERTS ===
        assertThat(resultado.getVersion().getRazonSocial()).isEqualTo("Lupulera del Norte S.A.");
        verify(proveedorRepository, times(2)).save(proveedorEntityExistente);
    }

    @ParameterizedTest(name = "CP-MP-27: {0}")
    @MethodSource("versionesConCuitVacio")
    @DisplayName("CP-MP-27: modificarProveedor lanza ReglaNegocioException y no consulta la BD cuando el CUIT no se informa")
    void modificarProveedor_debeRechazarCuitVacio(String descripcion, VersionProveedorFormDTO version) {
        assertThatThrownBy(() -> proveedorServicio.modificarProveedor(1L, proveedorFormDTO(version)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El CUIT es obligatorio");

        verifyNoRepositoryInteractions();
    }

    @ParameterizedTest(name = "CP-MP-28: catálogo {0}")
    @NullAndEmptySource
    @DisplayName("CP-MP-28: modificarProveedor lanza ReglaNegocioException y no consulta la BD cuando el catálogo es nulo o está vacío")
    void modificarProveedor_debeRechazarCatalogoNuloOVacio(List<CatalogoProveedorFormDTO> catalogo) {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().catalogoProveedor(catalogo).build();

        assertThatThrownBy(() -> proveedorServicio.modificarProveedor(1L, proveedorFormDTO(versionFormDTO)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El catálogo debe tener al menos un ítem");

        verifyNoRepositoryInteractions();
    }

    @ParameterizedTest(name = "CP-MP-29: {0}")
    @MethodSource("versionesConItemDeCatalogoIncompleto")
    @DisplayName("CP-MP-29: modificarProveedor lanza ReglaNegocioException y no consulta la BD cuando un ítem del catálogo es nulo o no indica el insumo o la presentación comercial")
    void modificarProveedor_debeRechazarItemDeCatalogoIncompleto(String descripcion, VersionProveedorFormDTO version, String mensaje) {
        assertThatThrownBy(() -> proveedorServicio.modificarProveedor(1L, proveedorFormDTO(version)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(mensaje);

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-MP-30: modificarProveedor lanza ReglaNegocioException y no consulta la BD cuando la localidad no se informa")
    void modificarProveedor_debeRechazarLocalidadNula() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().idLocalidad(null).build();

        assertThatThrownBy(() -> proveedorServicio.modificarProveedor(1L, proveedorFormDTO(versionFormDTO)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La localidad es obligatoria");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-MP-31: modificarProveedor lanza ReglaNegocioException y no consulta la BD cuando no se informan los datos del proveedor")
    void modificarProveedor_debeRechazarVersionNula() {
        ProveedorFormDTO proveedorFormDTO = proveedorFormDTO(null);

        assertThatThrownBy(() -> proveedorServicio.modificarProveedor(1L, proveedorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los datos del proveedor son obligatorios");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-MP-32: modificarProveedor lanza ReglaNegocioException y no consulta la BD cuando el catálogo supera los 100 ítems")
    void modificarProveedor_debeRechazarCatalogoSobreElMaximo() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().catalogoProveedor(catalogoConItemsDistintos(101)).build();

        assertThatThrownBy(() -> proveedorServicio.modificarProveedor(1L, proveedorFormDTO(versionFormDTO)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El catálogo no puede superar 100 ítems");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-MP-33: modificarProveedor traduce la violación de un índice único de la razón social al mensaje genérico (la razón social no tiene mensaje propio)")
    void modificarProveedor_debeTraducirIndiceDeRazonSocialAlMensajeGenerico() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().build();
        ProveedorEntity proveedorEntityExistente = crearProveedorEntityConVersiones(1L, crearVersionProveedorEntity(10L, "Maltería del Sur S.A.", "30111111111", true));
        mockearModificacionSinGuardado(versionFormDTO, proveedorEntityExistente);
        when(proveedorRepository.save(proveedorEntityExistente)).thenReturn(proveedorEntityExistente)
                .thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_version_proveedor_razon_social"));

        assertThatThrownBy(() -> proveedorServicio.modificarProveedor(1L, proveedorFormDTO(versionFormDTO)))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage(GuardadoUnico.MENSAJE_DUPLICADO_GENERICO);
    }

    @Test
    @DisplayName("CP-MP-34: modificarProveedor indica en la excepción de duplicado el campo repetido (cuit o email) al detectarlo con la consulta")
    void modificarProveedor_debeIndicarElCampoDuplicadoDetectadoPorLaConsulta() {
        // Arrange
        VersionProveedorFormDTO conCuitRepetido = versionValidaBuilder().cuit("30111111111").build();
        VersionProveedorFormDTO conCorreoRepetido = versionValidaBuilder().cuit("30222222222").email("contacto@malteriadelsur.com").build();
        when(proveedorRepository.findByIdParaActualizarVersiones(1L))
                .thenReturn(Optional.of(crearProveedorEntityConVersionActiva(1L, "Maltería del Norte S.A.", "30987654321")));
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrueAndProveedorIdNot("30-11111111-1", 1L)).thenReturn(true);
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrueAndProveedorIdNot("30-22222222-2", 1L)).thenReturn(false);
        when(versionProveedorRepository.existsByEmailIgnoreCaseAndEsUltimaVersionTrueAndProveedorIdNot("contacto@malteriadelsur.com", 1L)).thenReturn(true);

        // Act
        Throwable cuit = catchThrowable(() -> proveedorServicio.modificarProveedor(1L, proveedorFormDTO(conCuitRepetido)));
        Throwable correo = catchThrowable(() -> proveedorServicio.modificarProveedor(1L, proveedorFormDTO(conCorreoRepetido)));

        // Assert
        assertThat(cuit).isInstanceOf(RecursoDuplicadoException.class);
        assertThat(((RecursoDuplicadoException) cuit).getCampo()).isEqualTo("cuit");
        assertThat(correo).isInstanceOf(RecursoDuplicadoException.class);
        assertThat(((RecursoDuplicadoException) correo).getCampo()).isEqualTo("email");
        verify(proveedorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MP-35: modificarProveedor indica el campo repetido (cuit o email) al traducir la violación de su índice único, y ninguno ante un índice sin mensaje propio")
    void modificarProveedor_debeIndicarElCampoDuplicadoAlTraducirLaViolacionDelIndice() {
        // Arrange
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().build();
        ProveedorEntity proveedorEntityExistente = crearProveedorEntityConVersiones(1L, crearVersionProveedorEntity(10L, "Maltería del Sur S.A.", "30111111111", true));
        mockearModificacionSinGuardado(versionFormDTO, proveedorEntityExistente);
        when(proveedorRepository.save(proveedorEntityExistente)).thenReturn(proveedorEntityExistente)
                .thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_version_proveedor_cuit"))
                .thenReturn(proveedorEntityExistente)
                .thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_version_proveedor_email"))
                .thenReturn(proveedorEntityExistente)
                .thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_version_proveedor_razon_social"));

        // Act
        Throwable cuit = catchThrowable(() -> proveedorServicio.modificarProveedor(1L, proveedorFormDTO(versionFormDTO)));
        Throwable correo = catchThrowable(() -> proveedorServicio.modificarProveedor(1L, proveedorFormDTO(versionFormDTO)));
        Throwable generico = catchThrowable(() -> proveedorServicio.modificarProveedor(1L, proveedorFormDTO(versionFormDTO)));

        // Assert
        assertThat(cuit).isInstanceOf(RecursoDuplicadoException.class);
        assertThat(((RecursoDuplicadoException) cuit).getCampo()).isEqualTo("cuit");
        assertThat(correo).isInstanceOf(RecursoDuplicadoException.class);
        assertThat(((RecursoDuplicadoException) correo).getCampo()).isEqualTo("email");
        assertThat(generico).isInstanceOf(RecursoDuplicadoException.class).hasMessage(GuardadoUnico.MENSAJE_DUPLICADO_GENERICO);
        assertThat(((RecursoDuplicadoException) generico).getCampo()).isNull();
    }

    // ==================== bajaProveedor ====================

    @Test
    @DisplayName("CP-BP-01: bajaProveedor lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void bajaProveedor_debeLanzarExcepcionYNoPersistirSiNoExiste() {
        when(proveedorRepository.findByIdParaActualizarVersiones(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proveedorServicio.bajaProveedor(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el proveedor con ID: 99");

        verify(proveedorRepository).findByIdParaActualizarVersiones(99L);
        verify(proveedorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BP-02: bajaProveedor marca el estado como BAJA, persiste y retorna el DTO cuando no tiene órdenes de compra en estado Pendiente")
    void bajaProveedor_debeMarcarBajaYRetornarProveedorExistente() {
        // === PREPARACION DE DATOS ===
        ProveedorEntity proveedorEntity = crearProveedorEntityConVersionActiva(1L, "Maltería del Sur S.A.", "30-11111111-1");
        when(proveedorRepository.findByIdParaActualizarVersiones(1L)).thenReturn(Optional.of(proveedorEntity));
        when(ordenCompraRepository.existsByVersionProveedor_Proveedor_IdAndEstado(1L, EstadoSolicitud.PENDIENTE)).thenReturn(false);
        when(proveedorRepository.save(proveedorEntity)).thenReturn(proveedorEntity);

        // === EJECUCION ===
        ProveedorResponseDTO resultado = proveedorServicio.bajaProveedor(1L);

        // === ASSERTS ===
        // La baja es lógica: el estado pasa a BAJA y se persiste con save(), nunca con delete()
        assertThat(proveedorEntity.getEstado()).isEqualTo(Estado.BAJA);
        assertThat(resultado.getId()).isEqualTo(1L);
        verify(proveedorRepository).findByIdParaActualizarVersiones(1L);
        verify(ordenCompraRepository).existsByVersionProveedor_Proveedor_IdAndEstado(1L, EstadoSolicitud.PENDIENTE);
        verify(proveedorRepository).save(proveedorEntity);
        verify(proveedorRepository, never()).delete(any());
    }

    @Test
    @DisplayName("CP-BP-03: bajaProveedor lanza ReglaNegocioException y no persiste cuando hay una orden de compra PENDIENTE asociada")
    void bajaProveedor_debeRechazarConOrdenDeCompraPendienteAsociada() {
        // === PREPARACION DE DATOS ===
        ProveedorEntity proveedorEntity = crearProveedorEntityConVersionActiva(1L, "Maltería del Sur S.A.", "30-11111111-1");
        when(proveedorRepository.findByIdParaActualizarVersiones(1L)).thenReturn(Optional.of(proveedorEntity));
        when(ordenCompraRepository.existsByVersionProveedor_Proveedor_IdAndEstado(1L, EstadoSolicitud.PENDIENTE)).thenReturn(true);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> proveedorServicio.bajaProveedor(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja el proveedor porque tiene al menos una orden de compra en estado Pendiente asociada");

        verify(proveedorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BP-04: bajaProveedor permite la baja cuando existen órdenes de compra asociadas pero ninguna en estado PENDIENTE")
    void bajaProveedor_debePermitirBajaConOrdenesDeCompraEnOtroEstado() {
        // === PREPARACION DE DATOS ===
        ProveedorEntity proveedorEntity = crearProveedorEntityConVersionActiva(1L, "Maltería del Sur S.A.", "30-11111111-1");
        when(proveedorRepository.findByIdParaActualizarVersiones(1L)).thenReturn(Optional.of(proveedorEntity));
        when(ordenCompraRepository.existsByVersionProveedor_Proveedor_IdAndEstado(1L, EstadoSolicitud.PENDIENTE)).thenReturn(false);
        when(proveedorRepository.save(proveedorEntity)).thenReturn(proveedorEntity);

        // === EJECUCION ===
        ProveedorResponseDTO resultado = proveedorServicio.bajaProveedor(1L);

        // === ASSERTS ===
        assertThat(resultado.getId()).isEqualTo(1L);
        verify(proveedorRepository).save(proveedorEntity);
    }

    @Test
    @DisplayName("CP-BP-05: bajaProveedor marca padreActivo = false en todas las versiones, históricas y vigente, para liberar los datos únicos")
    void bajaProveedor_debeMarcarPadreInactivoEnTodasLasVersiones() {
        // === PREPARACION DE DATOS ===
        VersionProveedorEntity version1 = crearVersionProveedorEntity(10L, "Maltería del Sur v1", "30111111111", false);
        VersionProveedorEntity version2 = crearVersionProveedorEntity(11L, "Maltería del Sur v2", "30111111111", false);
        VersionProveedorEntity version3 = crearVersionProveedorEntity(12L, "Maltería del Sur S.A.", "30111111111", true);
        ProveedorEntity proveedorEntity = crearProveedorEntityConVersiones(1L, version1, version2, version3);
        when(proveedorRepository.findByIdParaActualizarVersiones(1L)).thenReturn(Optional.of(proveedorEntity));
        when(ordenCompraRepository.existsByVersionProveedor_Proveedor_IdAndEstado(1L, EstadoSolicitud.PENDIENTE)).thenReturn(false);
        when(proveedorRepository.save(proveedorEntity)).thenReturn(proveedorEntity);

        // === EJECUCION ===
        proveedorServicio.bajaProveedor(1L);

        // === ASSERTS ===
        assertThat(proveedorEntity.getEstado()).isEqualTo(Estado.BAJA);
        assertThat(proveedorEntity.getVersiones()).hasSize(3).allMatch(version -> !version.isPadreActivo());
    }

    @Test
    @DisplayName("CP-BP-06: bajaProveedor bloquea la fila del proveedor y no usa la búsqueda sin bloqueo")
    void bajaProveedor_debeBloquearElProveedor() {
        ProveedorEntity proveedorEntity = crearProveedorEntityConVersionActiva(1L, "Maltería del Sur S.A.", "30111111111");
        when(proveedorRepository.findByIdParaActualizarVersiones(1L)).thenReturn(Optional.of(proveedorEntity));
        when(ordenCompraRepository.existsByVersionProveedor_Proveedor_IdAndEstado(1L, EstadoSolicitud.PENDIENTE)).thenReturn(false);
        when(proveedorRepository.save(proveedorEntity)).thenReturn(proveedorEntity);

        proveedorServicio.bajaProveedor(1L);

        verify(proveedorRepository).findByIdParaActualizarVersiones(1L);
        verify(proveedorRepository, never()).findById(any());
    }

    @Test
    @DisplayName("CP-BP-07: bajaProveedor rechazada por orden de compra Pendiente no modifica el estado ni padreActivo")
    void bajaProveedor_rechazadaNoDebeTocarElProveedor() {
        ProveedorEntity proveedorEntity = crearProveedorEntityConVersionActiva(1L, "Maltería del Sur S.A.", "30111111111");
        when(proveedorRepository.findByIdParaActualizarVersiones(1L)).thenReturn(Optional.of(proveedorEntity));
        when(ordenCompraRepository.existsByVersionProveedor_Proveedor_IdAndEstado(1L, EstadoSolicitud.PENDIENTE)).thenReturn(true);

        assertThatThrownBy(() -> proveedorServicio.bajaProveedor(1L)).isInstanceOf(ReglaNegocioException.class);

        assertThat(proveedorEntity.getEstado()).isEqualTo(Estado.ACTIVO);
        assertThat(proveedorEntity.getVersiones()).allMatch(VersionProveedorEntity::isPadreActivo);
        verify(proveedorRepository, never()).save(any());
    }

    // ==================== helpers de mockeo ====================

    private void mockearCatalogo(VersionProveedorFormDTO versionFormDTO) {
        versionFormDTO.getCatalogoProveedor().forEach(detalle -> {
            when(presentacionComercialRepository.findById(detalle.getIdPresentacionComercial()))
                    .thenReturn(Optional.of(presentacionComercialEntity(detalle.getIdPresentacionComercial())));
            when(insumoRepository.findById(detalle.getIdInsumo()))
                    .thenReturn(Optional.of(insumoEntity(detalle.getIdInsumo())));
        });
    }

    private void verifyNoRepositoryInteractions() {
        verifyNoInteractions(proveedorRepository, versionProveedorRepository, localidadRepository, insumoRepository,
                presentacionComercialRepository, ordenCompraRepository);
    }

    /**
     * Prepara un alta cuyos datos ya están normalizados en el DTO: sin duplicados, localidad y catálogo existentes. No
     * prepara el guardado.
     */
    private void mockearAltaSinGuardado(VersionProveedorFormDTO versionFormDTO) {
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrue(
                ValidacionesTexto.formatearCuit(versionFormDTO.getCuit()))).thenReturn(false);
        when(versionProveedorRepository.existsByEmailIgnoreCaseAndEsUltimaVersionTrue(
                versionFormDTO.getEmail())).thenReturn(false);
        when(localidadRepository.findById(versionFormDTO.getIdLocalidad())).thenReturn(Optional.of(localidadEntity(versionFormDTO.getIdLocalidad())));
        mockearCatalogo(versionFormDTO);
    }

    /**
     * Prepara una modificación del proveedor 1 cuyos datos ya están normalizados en el DTO: el proveedor se encuentra
     * con bloqueo, no hay duplicados, y la localidad y el catálogo existen. No prepara el guardado.
     */
    private void mockearModificacionSinGuardado(VersionProveedorFormDTO versionFormDTO, ProveedorEntity proveedorEntityExistente) {
        when(proveedorRepository.findByIdParaActualizarVersiones(1L)).thenReturn(Optional.of(proveedorEntityExistente));
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrueAndProveedorIdNot(
                ValidacionesTexto.formatearCuit(versionFormDTO.getCuit()), 1L)).thenReturn(false);
        when(versionProveedorRepository.existsByEmailIgnoreCaseAndEsUltimaVersionTrueAndProveedorIdNot(
                versionFormDTO.getEmail(), 1L)).thenReturn(false);
        when(localidadRepository.findById(versionFormDTO.getIdLocalidad())).thenReturn(Optional.of(localidadEntity(versionFormDTO.getIdLocalidad())));
        mockearCatalogo(versionFormDTO);
    }

    private void mockearAltaExitosa(VersionProveedorFormDTO versionFormDTO) {
        mockearAltaSinGuardado(versionFormDTO);
        when(proveedorRepository.save(any(ProveedorEntity.class))).thenAnswer(invocation -> {
            ProveedorEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(1L);
            return entidadGuardada;
        });
    }

    // ==================== fuentes de casos parametrizados ====================

    static Stream<Arguments> versionesConDatoObligatorioVacio() {
        return Stream.of(
                Arguments.of("razón social null", versionValidaBuilder().razonSocial(null).build(), "La razón social es obligatoria"),
                Arguments.of("razón social de solo espacios", versionValidaBuilder().razonSocial("   ").build(), "La razón social es obligatoria"),
                Arguments.of("nombre comercial de solo espacios", versionValidaBuilder().nombreComercial("   ").build(), "El nombre comercial es obligatorio"),
                Arguments.of("correo electrónico de solo espacios", versionValidaBuilder().email("   ").build(), "El correo electrónico es obligatorio"),
                Arguments.of("dirección de solo espacios", versionValidaBuilder().direccion("   ").build(), "La dirección es obligatoria"));
    }

    static Stream<Arguments> versionesConDatoSobreElMaximo() {
        return Stream.of(
                Arguments.of("razón social de 101 caracteres", versionValidaBuilder().razonSocial("a".repeat(101)).build(), "La razón social no puede superar 100 caracteres"),
                Arguments.of("nombre comercial de 101 caracteres", versionValidaBuilder().nombreComercial("a".repeat(101)).build(), "El nombre comercial no puede superar 100 caracteres"),
                Arguments.of("correo electrónico de 256 caracteres", versionValidaBuilder().email("a".repeat(244) + "@example.com").build(), "El correo electrónico no puede superar 255 caracteres"),
                Arguments.of("dirección de 256 caracteres", versionValidaBuilder().direccion("a".repeat(256)).build(), "La dirección no puede superar 255 caracteres"));
    }

    static Stream<Arguments> versionesConCuitInvalido() {
        return Stream.of(
                Arguments.of("CUIT de 10 dígitos", versionValidaBuilder().cuit("3012345678").build()),
                Arguments.of("CUIT de 12 dígitos", versionValidaBuilder().cuit("301234567890").build()),
                Arguments.of("CUIT con una letra", versionValidaBuilder().cuit("3012345678a").build()));
    }

    static Stream<Arguments> versionesConCuitVacio() {
        return Stream.of(
                Arguments.of("CUIT null", versionValidaBuilder().cuit(null).build()),
                Arguments.of("CUIT vacío", versionValidaBuilder().cuit("").build()),
                Arguments.of("CUIT de solo espacios", versionValidaBuilder().cuit("   ").build()),
                Arguments.of("CUIT de solo separadores", versionValidaBuilder().cuit(" -.- / ").build()));
    }

    static Stream<Arguments> versionesConItemDeCatalogoIncompleto() {
        return Stream.of(
                Arguments.of("ítem null", versionValidaBuilder().catalogoProveedor(Arrays.asList(catalogoProveedorFormDTO(PRESENTACION_ID, INSUMO_ID), null)).build(),
                        "El insumo de cada ítem del catálogo es obligatorio"),
                Arguments.of("ítem sin insumo", versionValidaBuilder().catalogoProveedor(List.of(catalogoProveedorFormDTO(PRESENTACION_ID, null))).build(),
                        "El insumo de cada ítem del catálogo es obligatorio"),
                Arguments.of("ítem sin presentación comercial", versionValidaBuilder().catalogoProveedor(List.of(catalogoProveedorFormDTO(null, INSUMO_ID))).build(),
                        "La presentación comercial de cada ítem del catálogo es obligatoria"));
    }

    static Stream<Arguments> versionesConVariosDatosInvalidos() {
        return Stream.of(
                Arguments.of("razón social, CUIT, localidad y catálogo vacíos", versionValidaBuilder().razonSocial(null).cuit(null).idLocalidad(null).catalogoProveedor(null).build(),
                        "La razón social es obligatoria"),
                Arguments.of("CUIT, correo electrónico y localidad vacíos", versionValidaBuilder().cuit("").email(null).idLocalidad(null).build(),
                        "El CUIT es obligatorio"),
                Arguments.of("dirección, localidad y catálogo vacíos", versionValidaBuilder().direccion(null).idLocalidad(null).catalogoProveedor(null).build(),
                        "La dirección es obligatoria"),
                Arguments.of("localidad y catálogo vacíos", versionValidaBuilder().idLocalidad(null).catalogoProveedor(List.of()).build(),
                        "La localidad es obligatoria"));
    }

    static Stream<Arguments> versionesConCorreoInvalido() {
        return Stream.of(
                Arguments.of("sin @", versionValidaBuilder().email("no-es-correo").build()),
                Arguments.of("dominio sin punto", versionValidaBuilder().email("a@b").build()),
                Arguments.of("dos @", versionValidaBuilder().email("a@@b.com").build()),
                Arguments.of("espacio interno", versionValidaBuilder().email("a b@c.com").build()),
                Arguments.of("sin parte local", versionValidaBuilder().email("@x.com").build()),
                Arguments.of("dominio que empieza con punto", versionValidaBuilder().email("x@.com").build()),
                Arguments.of("dominio que termina en punto", versionValidaBuilder().email("x@y.").build()));
    }

    static Stream<Arguments> versionesConTelefonoInvalido() {
        return Stream.of(
                Arguments.of("teléfono con guiones", versionValidaBuilder().telefono("11-2233-4455").build(), "El teléfono solo puede contener dígitos"),
                Arguments.of("teléfono con signo más y espacios", versionValidaBuilder().telefono("+54 11 2233 4455").build(), "El teléfono solo puede contener dígitos"),
                Arguments.of("teléfono con letras", versionValidaBuilder().telefono("abc123").build(), "El teléfono solo puede contener dígitos"),
                Arguments.of("teléfono de 16 dígitos", versionValidaBuilder().telefono("1".repeat(16)).build(), "El teléfono no puede superar 15 dígitos"));
    }

    // ==================== helpers de construcción ====================

    private static VersionProveedorFormDTO.VersionProveedorFormDTOBuilder versionValidaBuilder() {
        return VersionProveedorFormDTO.builder()
                .razonSocial("Maltería del Sur S.A.")
                .nombreComercial("Maltería del Sur")
                .cuit("30123456789")
                .telefono("1122334455")
                .email("contacto@malteriadelsur.com")
                .direccion("Ruta 5 Km 120")
                .idLocalidad(LOCALIDAD_ID)
                .catalogoProveedor(List.of(catalogoProveedorFormDTO(PRESENTACION_ID, INSUMO_ID)));
    }

    private static ProveedorFormDTO proveedorFormDTO(VersionProveedorFormDTO version) {
        return ProveedorFormDTO.builder().version(version).build();
    }

    private static CatalogoProveedorFormDTO catalogoProveedorFormDTO(Long idPresentacionComercial, Long idInsumo) {
        return CatalogoProveedorFormDTO.builder()
                .idPresentacionComercial(idPresentacionComercial)
                .idInsumo(idInsumo)
                .build();
    }

    /**
     * Catálogo con la cantidad de ítems indicada, todos distintos: el ítem {@code i} combina la presentación comercial
     * {@code i} con el insumo {@code i}.
     */
    private static List<CatalogoProveedorFormDTO> catalogoConItemsDistintos(int cantidad) {
        return LongStream.rangeClosed(1, cantidad)
                .mapToObj(id -> catalogoProveedorFormDTO(id, id))
                .toList();
    }

    private static LocalidadEntity localidadEntity(Long id) {
        return LocalidadEntity.builder().id(id).nombre("San Nicolás").estado(Estado.ACTIVO).build();
    }

    private static PresentacionComercialEntity presentacionComercialEntity(Long id) {
        return PresentacionComercialEntity.builder()
                .id(id)
                .nombre("Bolsa de 25 Kg")
                .cantidad(25.0)
                .unidadDeMedida(UnidadDeMedida.KILOGRAMO)
                .estado(Estado.ACTIVO)
                .build();
    }

    private static InsumoEntity insumoEntity(Long id) {
        return MaltaEntity.builder().id(id).nombre("Malta Pilsen").estado(Estado.ACTIVO).build();
    }

    private static VersionProveedorEntity crearVersionProveedorEntity(Long id, String razonSocial, String cuit, boolean esUltimaVersion) {
        return VersionProveedorEntity.builder()
                .id(id)
                .razonSocial(razonSocial)
                .nombreComercial(razonSocial)
                .cuit(cuit)
                .telefono("11-2233-4455")
                .email("contacto@proveedor.com")
                .direccion("Ruta 5 Km 120")
                .esUltimaVersion(esUltimaVersion)
                .localidad(localidadEntity(LOCALIDAD_ID))
                .build();
    }

    private static ProveedorEntity crearProveedorEntityConVersiones(Long id, VersionProveedorEntity... versiones) {
        ProveedorEntity proveedorEntity = ProveedorEntity.builder().id(id).estado(Estado.ACTIVO).build();
        for (VersionProveedorEntity version : versiones) {
            version.setProveedor(proveedorEntity);
            proveedorEntity.getVersiones().add(version);
        }
        return proveedorEntity;
    }

    private static ProveedorEntity crearProveedorEntityConVersionActiva(Long id, String razonSocial, String cuit) {
        return crearProveedorEntityConVersiones(id, crearVersionProveedorEntity(id, razonSocial, cuit, true));
    }
}
