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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
    @DisplayName("CP-AP-01: altaProveedor lanza RecursoDuplicadoException y no consulta el resto de la BD cuando la razón social ya está registrada por un proveedor activo")
    void altaProveedor_debeRechazarRazonSocialDuplicada() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().build();
        ProveedorFormDTO proveedorFormDTO = proveedorFormDTO(versionFormDTO);
        when(versionProveedorRepository.existsByRazonSocialIgnoreCaseAndEsUltimaVersionTrue(
                versionFormDTO.getRazonSocial())).thenReturn(true);

        assertThatThrownBy(() -> proveedorServicio.altaProveedor(proveedorFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un proveedor activo con la razón social '" + versionFormDTO.getRazonSocial() + "'");

        verifyNoInteractions(proveedorRepository, localidadRepository, insumoRepository, presentacionComercialRepository);
    }

    @Test
    @DisplayName("CP-AP-02: altaProveedor lanza RecursoDuplicadoException cuando el CUIT ya está registrado por un proveedor activo")
    void altaProveedor_debeRechazarCuitDuplicado() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().cuit("30-11111111-1").build();
        ProveedorFormDTO proveedorFormDTO = proveedorFormDTO(versionFormDTO);
        when(versionProveedorRepository.existsByRazonSocialIgnoreCaseAndEsUltimaVersionTrue(
                versionFormDTO.getRazonSocial())).thenReturn(false);
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrue("30-11111111-1")).thenReturn(true);

        assertThatThrownBy(() -> proveedorServicio.altaProveedor(proveedorFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un proveedor activo con el CUIT '30-11111111-1'");

        verify(proveedorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AP-03: altaProveedor lanza RecursoNoEncontradoException y no persiste cuando la localidad referenciada no existe")
    void altaProveedor_debeRechazarLocalidadInexistente() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().idLocalidad(99L).build();
        ProveedorFormDTO proveedorFormDTO = proveedorFormDTO(versionFormDTO);
        when(versionProveedorRepository.existsByRazonSocialIgnoreCaseAndEsUltimaVersionTrue(
                versionFormDTO.getRazonSocial())).thenReturn(false);
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrue(
                versionFormDTO.getCuit())).thenReturn(false);
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
        when(versionProveedorRepository.existsByRazonSocialIgnoreCaseAndEsUltimaVersionTrue(
                versionFormDTO.getRazonSocial())).thenReturn(false);
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrue(
                versionFormDTO.getCuit())).thenReturn(false);
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
        when(versionProveedorRepository.existsByRazonSocialIgnoreCaseAndEsUltimaVersionTrue(
                versionFormDTO.getRazonSocial())).thenReturn(false);
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrue(
                versionFormDTO.getCuit())).thenReturn(false);
        when(localidadRepository.findById(LOCALIDAD_ID)).thenReturn(Optional.of(localidadEntity(LOCALIDAD_ID)));
        when(presentacionComercialRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proveedorServicio.altaProveedor(proveedorFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la presentación comercial con ID: 99");

        verify(proveedorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AP-06: altaProveedor persiste con catálogo vacío y no consulta insumoRepository ni presentacionComercialRepository (opcional)")
    void altaProveedor_debePersistirConCatalogoVacio() {
        // === PREPARACION DE DATOS ===
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().catalogoProveedor(List.of()).build();
        ProveedorFormDTO proveedorFormDTO = proveedorFormDTO(versionFormDTO);
        when(versionProveedorRepository.existsByRazonSocialIgnoreCaseAndEsUltimaVersionTrue(
                versionFormDTO.getRazonSocial())).thenReturn(false);
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrue(
                versionFormDTO.getCuit())).thenReturn(false);
        when(localidadRepository.findById(LOCALIDAD_ID)).thenReturn(Optional.of(localidadEntity(LOCALIDAD_ID)));
        when(proveedorRepository.save(any(ProveedorEntity.class))).thenAnswer(invocation -> {
            ProveedorEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(1L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        ProveedorResponseDTO resultado = proveedorServicio.altaProveedor(proveedorFormDTO);

        // === ASSERTS ===
        assertThat(resultado.getVersion().getCatalogoProveedor()).isEmpty();
        verifyNoInteractions(insumoRepository, presentacionComercialRepository);
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

    // ==================== modificarProveedor ====================

    @Test
    @DisplayName("CP-MP-01: modificarProveedor lanza RecursoDuplicadoException y no busca por ID ni persiste cuando la razón social está en uso por otro proveedor activo")
    void modificarProveedor_debeRechazarRazonSocialEnUsoPorOtroProveedor() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().build();
        ProveedorFormDTO proveedorFormDTO = proveedorFormDTO(versionFormDTO);
        when(versionProveedorRepository.existsByRazonSocialIgnoreCaseAndEsUltimaVersionTrueAndProveedorIdNot(
                versionFormDTO.getRazonSocial(), 1L)).thenReturn(true);

        assertThatThrownBy(() -> proveedorServicio.modificarProveedor(1L, proveedorFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un proveedor activo con la razón social '" + versionFormDTO.getRazonSocial() + "'");

        verifyNoInteractions(proveedorRepository, localidadRepository, insumoRepository, presentacionComercialRepository);
    }

    @Test
    @DisplayName("CP-MP-02: modificarProveedor lanza RecursoDuplicadoException cuando el CUIT está en uso por otro proveedor activo")
    void modificarProveedor_debeRechazarCuitEnUsoPorOtroProveedor() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().build();
        ProveedorFormDTO proveedorFormDTO = proveedorFormDTO(versionFormDTO);
        when(versionProveedorRepository.existsByRazonSocialIgnoreCaseAndEsUltimaVersionTrueAndProveedorIdNot(
                versionFormDTO.getRazonSocial(), 1L)).thenReturn(false);
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrueAndProveedorIdNot(
                versionFormDTO.getCuit(), 1L)).thenReturn(true);

        assertThatThrownBy(() -> proveedorServicio.modificarProveedor(1L, proveedorFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un proveedor activo con el CUIT '" + versionFormDTO.getCuit() + "'");

        verify(proveedorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MP-03: modificarProveedor permite conservar la razón social y el CUIT propios y persiste la nueva versión (camino feliz)")
    void modificarProveedor_debePermitirConservarRazonSocialYCuitPropios() {
        // === PREPARACION DE DATOS ===
        VersionProveedorEntity versionActivaPrevia = crearVersionProveedorEntity(10L, "Maltería del Sur S.A.", "30-11111111-1", true);
        ProveedorEntity proveedorEntityExistente = crearProveedorEntityConVersiones(1L, versionActivaPrevia);
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder()
                .razonSocial("Maltería del Sur S.A.")
                .cuit("30-11111111-1")
                .build();
        ProveedorFormDTO proveedorFormDTO = proveedorFormDTO(versionFormDTO);
        when(versionProveedorRepository.existsByRazonSocialIgnoreCaseAndEsUltimaVersionTrueAndProveedorIdNot(
                "Maltería del Sur S.A.", 1L)).thenReturn(false);
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrueAndProveedorIdNot(
                "30-11111111-1", 1L)).thenReturn(false);
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedorEntityExistente));
        when(localidadRepository.findById(LOCALIDAD_ID)).thenReturn(Optional.of(localidadEntity(LOCALIDAD_ID)));
        mockearCatalogo(versionFormDTO);
        when(proveedorRepository.save(proveedorEntityExistente)).thenReturn(proveedorEntityExistente);

        // === EJECUCION ===
        ProveedorResponseDTO resultado = proveedorServicio.modificarProveedor(1L, proveedorFormDTO);

        // === ASSERTS ===
        assertThat(resultado).isNotNull();
        assertThat(resultado.getVersion().getRazonSocial()).isEqualTo("Maltería del Sur S.A.");
        verify(proveedorRepository).save(proveedorEntityExistente);
    }

    @Test
    @DisplayName("CP-MP-04: modificarProveedor lanza RecursoNoEncontradoException y no persiste cuando el proveedor no existe o está dado de baja")
    void modificarProveedor_debeLanzarExcepcionSiNoExiste() {
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().build();
        ProveedorFormDTO proveedorFormDTO = proveedorFormDTO(versionFormDTO);
        when(versionProveedorRepository.existsByRazonSocialIgnoreCaseAndEsUltimaVersionTrueAndProveedorIdNot(
                versionFormDTO.getRazonSocial(), 99L)).thenReturn(false);
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrueAndProveedorIdNot(
                versionFormDTO.getCuit(), 99L)).thenReturn(false);
        when(proveedorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proveedorServicio.modificarProveedor(99L, proveedorFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el proveedor con ID: 99");

        verify(proveedorRepository).findById(99L);
        verify(proveedorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MP-05: modificarProveedor lanza RecursoNoEncontradoException cuando la localidad referenciada no existe")
    void modificarProveedor_debeRechazarLocalidadInexistente() {
        // === PREPARACION DE DATOS ===
        VersionProveedorEntity versionActivaPrevia = crearVersionProveedorEntity(10L, "Maltería del Sur S.A.", "30-11111111-1", true);
        ProveedorEntity proveedorEntityExistente = crearProveedorEntityConVersiones(1L, versionActivaPrevia);
        VersionProveedorFormDTO versionFormDTO = versionValidaBuilder().idLocalidad(99L).build();
        ProveedorFormDTO proveedorFormDTO = proveedorFormDTO(versionFormDTO);
        when(versionProveedorRepository.existsByRazonSocialIgnoreCaseAndEsUltimaVersionTrueAndProveedorIdNot(
                versionFormDTO.getRazonSocial(), 1L)).thenReturn(false);
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrueAndProveedorIdNot(
                versionFormDTO.getCuit(), 1L)).thenReturn(false);
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedorEntityExistente));
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
        when(versionProveedorRepository.existsByRazonSocialIgnoreCaseAndEsUltimaVersionTrueAndProveedorIdNot(
                versionFormDTO.getRazonSocial(), 1L)).thenReturn(false);
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrueAndProveedorIdNot(
                versionFormDTO.getCuit(), 1L)).thenReturn(false);
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedorEntityExistente));
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
        when(versionProveedorRepository.existsByRazonSocialIgnoreCaseAndEsUltimaVersionTrueAndProveedorIdNot(
                versionFormDTO.getRazonSocial(), 1L)).thenReturn(false);
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrueAndProveedorIdNot(
                versionFormDTO.getCuit(), 1L)).thenReturn(false);
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedorEntityExistente));
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
        when(versionProveedorRepository.existsByRazonSocialIgnoreCaseAndEsUltimaVersionTrueAndProveedorIdNot(
                versionFormDTO.getRazonSocial(), 1L)).thenReturn(false);
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrueAndProveedorIdNot(
                versionFormDTO.getCuit(), 1L)).thenReturn(false);
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedorEntityExistente));
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
        verify(proveedorRepository).save(proveedorEntityExistente);
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
        when(versionProveedorRepository.existsByRazonSocialIgnoreCaseAndEsUltimaVersionTrueAndProveedorIdNot(
                versionFormDTO.getRazonSocial(), 1L)).thenReturn(false);
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrueAndProveedorIdNot(
                versionFormDTO.getCuit(), 1L)).thenReturn(false);
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedorEntityExistente));
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

    // ==================== bajaProveedor ====================

    @Test
    @DisplayName("CP-BP-01: bajaProveedor lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void bajaProveedor_debeLanzarExcepcionYNoPersistirSiNoExiste() {
        when(proveedorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proveedorServicio.bajaProveedor(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el proveedor con ID: 99");

        verify(proveedorRepository).findById(99L);
        verify(proveedorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BP-02: bajaProveedor marca el estado como BAJA, persiste y retorna el DTO cuando no tiene órdenes de compra pendientes")
    void bajaProveedor_debeMarcarBajaYRetornarProveedorExistente() {
        // === PREPARACION DE DATOS ===
        ProveedorEntity proveedorEntity = crearProveedorEntityConVersionActiva(1L, "Maltería del Sur S.A.", "30-11111111-1");
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedorEntity));
        when(ordenCompraRepository.existsByVersionProveedor_Proveedor_IdAndEstado(1L, EstadoSolicitud.PENDIENTE)).thenReturn(false);
        when(proveedorRepository.save(proveedorEntity)).thenReturn(proveedorEntity);

        // === EJECUCION ===
        ProveedorResponseDTO resultado = proveedorServicio.bajaProveedor(1L);

        // === ASSERTS ===
        // La baja es lógica: el estado pasa a BAJA y se persiste con save(), nunca con delete()
        assertThat(proveedorEntity.getEstado()).isEqualTo(Estado.BAJA);
        assertThat(resultado.getId()).isEqualTo(1L);
        verify(proveedorRepository).findById(1L);
        verify(ordenCompraRepository).existsByVersionProveedor_Proveedor_IdAndEstado(1L, EstadoSolicitud.PENDIENTE);
        verify(proveedorRepository).save(proveedorEntity);
        verify(proveedorRepository, never()).delete(any());
    }

    @Test
    @DisplayName("CP-BP-03: bajaProveedor lanza ReglaNegocioException y no persiste cuando hay una orden de compra PENDIENTE asociada")
    void bajaProveedor_debeRechazarConOrdenDeCompraPendienteAsociada() {
        // === PREPARACION DE DATOS ===
        ProveedorEntity proveedorEntity = crearProveedorEntityConVersionActiva(1L, "Maltería del Sur S.A.", "30-11111111-1");
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedorEntity));
        when(ordenCompraRepository.existsByVersionProveedor_Proveedor_IdAndEstado(1L, EstadoSolicitud.PENDIENTE)).thenReturn(true);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> proveedorServicio.bajaProveedor(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja el proveedor porque tiene una orden de compra en estado PENDIENTE asociada");

        verify(proveedorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BP-04: bajaProveedor permite la baja cuando existen órdenes de compra asociadas pero ninguna en estado PENDIENTE")
    void bajaProveedor_debePermitirBajaConOrdenesDeCompraEnOtroEstado() {
        // === PREPARACION DE DATOS ===
        ProveedorEntity proveedorEntity = crearProveedorEntityConVersionActiva(1L, "Maltería del Sur S.A.", "30-11111111-1");
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedorEntity));
        when(ordenCompraRepository.existsByVersionProveedor_Proveedor_IdAndEstado(1L, EstadoSolicitud.PENDIENTE)).thenReturn(false);
        when(proveedorRepository.save(proveedorEntity)).thenReturn(proveedorEntity);

        // === EJECUCION ===
        ProveedorResponseDTO resultado = proveedorServicio.bajaProveedor(1L);

        // === ASSERTS ===
        assertThat(resultado.getId()).isEqualTo(1L);
        verify(proveedorRepository).save(proveedorEntity);
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

    private void mockearAltaExitosa(VersionProveedorFormDTO versionFormDTO) {
        when(versionProveedorRepository.existsByRazonSocialIgnoreCaseAndEsUltimaVersionTrue(
                versionFormDTO.getRazonSocial())).thenReturn(false);
        when(versionProveedorRepository.existsByCuitAndEsUltimaVersionTrue(
                versionFormDTO.getCuit())).thenReturn(false);
        when(localidadRepository.findById(versionFormDTO.getIdLocalidad())).thenReturn(Optional.of(localidadEntity(versionFormDTO.getIdLocalidad())));
        mockearCatalogo(versionFormDTO);
        when(proveedorRepository.save(any(ProveedorEntity.class))).thenAnswer(invocation -> {
            ProveedorEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(1L);
            return entidadGuardada;
        });
    }

    // ==================== helpers de construcción ====================

    private static VersionProveedorFormDTO.VersionProveedorFormDTOBuilder versionValidaBuilder() {
        return VersionProveedorFormDTO.builder()
                .razonSocial("Maltería del Sur S.A.")
                .nombreComercial("Maltería del Sur")
                .cuit("30-12345678-9")
                .telefono("11-2233-4455")
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
