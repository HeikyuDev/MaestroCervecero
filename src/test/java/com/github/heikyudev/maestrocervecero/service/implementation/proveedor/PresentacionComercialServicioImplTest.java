package com.github.heikyudev.maestrocervecero.service.implementation.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.PresentacionComercialEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.proveedor.ICatalogoProveedorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.proveedor.IPresentacionComercialRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor.PresentacionComercialFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.PresentacionComercialResponseDTO;
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
class PresentacionComercialServicioImplTest {

    @Mock
    private IPresentacionComercialRepository presentacionComercialRepository;

    @Mock
    private ICatalogoProveedorRepository catalogoProveedorRepository;

    @InjectMocks
    private PresentacionComercialServicioImpl presentacionComercialServicio;

    // ==================== buscarTodos ====================

    @Test
    @DisplayName("CP-BT-01: buscarTodos retorna una página de presentaciones comerciales correctamente mapeada a DTO")
    void buscarTodos_debeRetornarPaginaMapeada() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        PresentacionComercialEntity presentacionEntity = crearPresentacionComercialEntity(1L, "Bolsa de 25 Kg", 25.0, UnidadDeMedida.KILOGRAMO);
        PresentacionComercialEntity otraPresentacionEntity = crearPresentacionComercialEntity(2L, "Paquete de 100 gm", 100.0, UnidadDeMedida.GRAMO);
        PresentacionComercialEntity terceraPresentacionEntity = crearPresentacionComercialEntity(3L, "Pallet de 1 Tn", 1.0, UnidadDeMedida.TONELADA);

        // Cuando presentacionComercialRepository.findAll(pageable) sea llamado, retorna una página con las presentaciones activas
        // (el filtrado de soft-deleted es automático por @SoftDelete de Hibernate sobre la entidad)
        when(presentacionComercialRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(presentacionEntity, otraPresentacionEntity, terceraPresentacionEntity), pageable, 3));

        // === EJECUCION ===
        Page<PresentacionComercialResponseDTO> resultado = presentacionComercialServicio.buscarTodos(pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(3);
        assertPresentacionComercialDTO(presentacionEntity, resultado.getContent().get(0));
        assertPresentacionComercialDTO(otraPresentacionEntity, resultado.getContent().get(1));
        assertPresentacionComercialDTO(terceraPresentacionEntity, resultado.getContent().get(2));
        verify(presentacionComercialRepository).findAll(pageable);
    }

    @Test
    @DisplayName("CP-BT-02: buscarTodos retorna una página vacía cuando no hay presentaciones comerciales registradas")
    void buscarTodos_debeRetornarPaginaVaciaSinRegistros() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(presentacionComercialRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<PresentacionComercialResponseDTO> resultado = presentacionComercialServicio.buscarTodos(pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(presentacionComercialRepository).findAll(pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO de la presentación comercial cuando el ID existe")
    void buscarPorId_debeRetornarPresentacionComercialExistente() {
        // === PREPARACION DE DATOS ===
        PresentacionComercialEntity presentacionEntity = crearPresentacionComercialEntity(1L, "Bolsa de 25 Kg", 25.0, UnidadDeMedida.KILOGRAMO);
        when(presentacionComercialRepository.findById(1L)).thenReturn(Optional.of(presentacionEntity));

        // === EJECUCION ===
        PresentacionComercialResponseDTO resultado = presentacionComercialServicio.buscarPorId(1L);

        // === ASSERTS ===
        assertPresentacionComercialDTO(presentacionEntity, resultado);
        verify(presentacionComercialRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe o está soft-deleted")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        // @SoftDelete hace que findById devuelva Optional.empty() también para registros eliminados lógicamente
        when(presentacionComercialRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> presentacionComercialServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("La presentación comercial no existe");
        verify(presentacionComercialRepository).findById(99L);
    }

    // ==================== altaPresentacionComercial ====================

    @Test
    @DisplayName("CP-APC-01: altaPresentacionComercial lanza ReglaNegocioException y no consulta el repositorio cuando la cantidad es nula")
    void altaPresentacionComercial_debeRechazarCantidadNula() {
        PresentacionComercialFormDTO formDTO = presentacionComercialFormDTO("Bolsa de 25 Kg", null, UnidadDeMedida.KILOGRAMO);

        assertThatThrownBy(() -> presentacionComercialServicio.altaPresentacionComercial(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad debe ser mayor a cero");

        // La validación de negocio se ejecuta antes de cualquier acceso a la base de datos
        verifyNoInteractions(presentacionComercialRepository);
    }

    @Test
    @DisplayName("CP-APC-02: altaPresentacionComercial lanza ReglaNegocioException cuando la cantidad es cero (límite)")
    void altaPresentacionComercial_debeRechazarCantidadEnCero() {
        PresentacionComercialFormDTO formDTO = presentacionComercialFormDTO("Bolsa de 25 Kg", 0.0, UnidadDeMedida.KILOGRAMO);

        assertThatThrownBy(() -> presentacionComercialServicio.altaPresentacionComercial(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad debe ser mayor a cero");

        verifyNoInteractions(presentacionComercialRepository);
    }

    @Test
    @DisplayName("CP-APC-03: altaPresentacionComercial lanza ReglaNegocioException cuando la cantidad es negativa")
    void altaPresentacionComercial_debeRechazarCantidadNegativa() {
        PresentacionComercialFormDTO formDTO = presentacionComercialFormDTO("Bolsa de 25 Kg", -0.1, UnidadDeMedida.KILOGRAMO);

        assertThatThrownBy(() -> presentacionComercialServicio.altaPresentacionComercial(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad debe ser mayor a cero");

        verifyNoInteractions(presentacionComercialRepository);
    }

    @Test
    @DisplayName("CP-APC-04: altaPresentacionComercial persiste con éxito cuando la cantidad está en el límite inferior válido")
    void altaPresentacionComercial_debePersistirConCantidadEnLimiteInferiorValido() {
        // === PREPARACION DE DATOS ===
        PresentacionComercialFormDTO formDTO = presentacionComercialFormDTO("Sobre de 0,1 gm", 0.1, UnidadDeMedida.GRAMO);
        when(presentacionComercialRepository.existsByNombreIgnoreCase("Sobre de 0,1 gm")).thenReturn(false);
        when(presentacionComercialRepository.save(any(PresentacionComercialEntity.class))).thenAnswer(invocation -> {
            PresentacionComercialEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(4L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        PresentacionComercialResponseDTO resultado = presentacionComercialServicio.altaPresentacionComercial(formDTO);

        // === ASSERTS ===
        ArgumentCaptor<PresentacionComercialEntity> captor = ArgumentCaptor.forClass(PresentacionComercialEntity.class);
        verify(presentacionComercialRepository).save(captor.capture());
        assertThat(captor.getValue().getCantidad()).isEqualTo(0.1);

        assertThat(resultado.getCantidad()).isEqualTo(0.1);
        assertThat(resultado.getNombre()).isEqualTo("Sobre de 0,1 gm");
    }

    @Test
    @DisplayName("CP-APC-05: altaPresentacionComercial lanza RecursoDuplicadoException y no persiste cuando el nombre ya existe")
    void altaPresentacionComercial_debeRechazarNombreDuplicado() {
        PresentacionComercialFormDTO formDTO = presentacionComercialFormDTO("Bolsa de 25 Kg", 25.0, UnidadDeMedida.KILOGRAMO);
        when(presentacionComercialRepository.existsByNombreIgnoreCase("Bolsa de 25 Kg")).thenReturn(true);

        assertThatThrownBy(() -> presentacionComercialServicio.altaPresentacionComercial(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe una presentación comercial con el nombre 'Bolsa de 25 Kg'");

        verify(presentacionComercialRepository).existsByNombreIgnoreCase("Bolsa de 25 Kg");
        verify(presentacionComercialRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-APC-06: altaPresentacionComercial lanza RecursoDuplicadoException cuando el nombre ya existe con distinto case (case-insensitive)")
    void altaPresentacionComercial_debeRechazarNombreDuplicadoCaseInsensitive() {
        PresentacionComercialFormDTO formDTO = presentacionComercialFormDTO("bolsa de 25 kg", 25.0, UnidadDeMedida.KILOGRAMO);
        when(presentacionComercialRepository.existsByNombreIgnoreCase("bolsa de 25 kg")).thenReturn(true);

        assertThatThrownBy(() -> presentacionComercialServicio.altaPresentacionComercial(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe una presentación comercial con el nombre 'bolsa de 25 kg'");

        verify(presentacionComercialRepository).existsByNombreIgnoreCase("bolsa de 25 kg");
        verify(presentacionComercialRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-APC-07: altaPresentacionComercial valida la cantidad antes que la duplicación de nombre")
    void altaPresentacionComercial_debeValidarCantidadAntesQueNombre() {
        PresentacionComercialFormDTO formDTO = presentacionComercialFormDTO("Bolsa de 25 Kg", 0.0, UnidadDeMedida.KILOGRAMO);

        assertThatThrownBy(() -> presentacionComercialServicio.altaPresentacionComercial(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad debe ser mayor a cero");

        verify(presentacionComercialRepository, never()).existsByNombreIgnoreCase(any());
        verifyNoInteractions(presentacionComercialRepository);
    }

    @Test
    @DisplayName("CP-APC-08: altaPresentacionComercial persiste y retorna el DTO cuando los datos son válidos (camino feliz)")
    void altaPresentacionComercial_debePersistirYRetornarDTOCuandoDatosSonValidos() {
        // === PREPARACION DE DATOS ===
        PresentacionComercialFormDTO formDTO = presentacionComercialFormDTO("Paquete de 100 gm", 100.0, UnidadDeMedida.GRAMO);
        when(presentacionComercialRepository.existsByNombreIgnoreCase("Paquete de 100 gm")).thenReturn(false);
        when(presentacionComercialRepository.save(any(PresentacionComercialEntity.class))).thenAnswer(invocation -> {
            PresentacionComercialEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(1L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        PresentacionComercialResponseDTO resultado = presentacionComercialServicio.altaPresentacionComercial(formDTO);

        // === ASSERTS ===
        ArgumentCaptor<PresentacionComercialEntity> captor = ArgumentCaptor.forClass(PresentacionComercialEntity.class);
        verify(presentacionComercialRepository).save(captor.capture());
        PresentacionComercialEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getNombre()).isEqualTo("Paquete de 100 gm");
        assertThat(entidadCapturada.getCantidad()).isEqualTo(100.0);
        assertThat(entidadCapturada.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.GRAMO);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Paquete de 100 gm");
        assertThat(resultado.getCantidad()).isEqualTo(100.0);
        assertThat(resultado.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.GRAMO);
        verify(presentacionComercialRepository).existsByNombreIgnoreCase("Paquete de 100 gm");
    }

    @Test
    @DisplayName("CP-APC-09: altaPresentacionComercial persiste y retorna el DTO con unidad de medida TONELADA")
    void altaPresentacionComercial_debePersistirConUnidadDeMedidaTonelada() {
        // === PREPARACION DE DATOS ===
        PresentacionComercialFormDTO formDTO = presentacionComercialFormDTO("Pallet de 1 Tn", 1.0, UnidadDeMedida.TONELADA);
        when(presentacionComercialRepository.existsByNombreIgnoreCase("Pallet de 1 Tn")).thenReturn(false);
        when(presentacionComercialRepository.save(any(PresentacionComercialEntity.class))).thenAnswer(invocation -> {
            PresentacionComercialEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(2L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        PresentacionComercialResponseDTO resultado = presentacionComercialServicio.altaPresentacionComercial(formDTO);

        // === ASSERTS ===
        assertThat(resultado.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.TONELADA);
        ArgumentCaptor<PresentacionComercialEntity> captor = ArgumentCaptor.forClass(PresentacionComercialEntity.class);
        verify(presentacionComercialRepository).save(captor.capture());
        assertThat(captor.getValue().getUnidadDeMedida()).isEqualTo(UnidadDeMedida.TONELADA);
    }

    @Test
    @DisplayName("CP-APC-10: altaPresentacionComercial lanza RecursoDuplicadoException por nombre repetido sin importar la unidad de medida distinta")
    void altaPresentacionComercial_debeRechazarNombreDuplicadoConDistintaUnidadDeMedida() {
        PresentacionComercialFormDTO formDTO = presentacionComercialFormDTO("Bolsa de 25 Kg", 25000.0, UnidadDeMedida.GRAMO);
        when(presentacionComercialRepository.existsByNombreIgnoreCase("Bolsa de 25 Kg")).thenReturn(true);

        assertThatThrownBy(() -> presentacionComercialServicio.altaPresentacionComercial(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe una presentación comercial con el nombre 'Bolsa de 25 Kg'");

        // La unicidad es solo por nombre: no se compara la unidad de medida
        verify(presentacionComercialRepository, never()).save(any());
    }

    // ==================== modificarPresentacionComercial ====================

    @Test
    @DisplayName("CP-MPC-01: modificarPresentacionComercial lanza ReglaNegocioException y no consulta el repositorio cuando la cantidad es nula")
    void modificarPresentacionComercial_debeRechazarCantidadNula() {
        PresentacionComercialFormDTO formDTO = presentacionComercialFormDTO("Bolsa de 25 Kg", null, UnidadDeMedida.KILOGRAMO);

        assertThatThrownBy(() -> presentacionComercialServicio.modificarPresentacionComercial(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad debe ser mayor a cero");

        verifyNoInteractions(presentacionComercialRepository);
    }

    @Test
    @DisplayName("CP-MPC-02: modificarPresentacionComercial lanza ReglaNegocioException cuando la cantidad es cero (límite)")
    void modificarPresentacionComercial_debeRechazarCantidadEnCero() {
        PresentacionComercialFormDTO formDTO = presentacionComercialFormDTO("Bolsa de 25 Kg", 0.0, UnidadDeMedida.KILOGRAMO);

        assertThatThrownBy(() -> presentacionComercialServicio.modificarPresentacionComercial(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad debe ser mayor a cero");

        verifyNoInteractions(presentacionComercialRepository);
    }

    @Test
    @DisplayName("CP-MPC-03: modificarPresentacionComercial lanza ReglaNegocioException cuando la cantidad es negativa")
    void modificarPresentacionComercial_debeRechazarCantidadNegativa() {
        PresentacionComercialFormDTO formDTO = presentacionComercialFormDTO("Bolsa de 25 Kg", -0.1, UnidadDeMedida.KILOGRAMO);

        assertThatThrownBy(() -> presentacionComercialServicio.modificarPresentacionComercial(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad debe ser mayor a cero");

        verifyNoInteractions(presentacionComercialRepository);
    }

    @Test
    @DisplayName("CP-MPC-04: modificarPresentacionComercial lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void modificarPresentacionComercial_debeLanzarExcepcionSiNoExiste() {
        PresentacionComercialFormDTO formDTO = presentacionComercialFormDTO("Bolsa de 25 Kg", 25.0, UnidadDeMedida.KILOGRAMO);
        when(presentacionComercialRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> presentacionComercialServicio.modificarPresentacionComercial(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("La presentación comercial no existe");

        verify(presentacionComercialRepository).findById(99L);
        // Al no existir la entidad, no debe llegarse a validar la duplicidad de nombre
        verify(presentacionComercialRepository, never()).existsByNombreIgnoreCaseAndIdNot(any(), any());
        verify(presentacionComercialRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MPC-05: modificarPresentacionComercial lanza RecursoDuplicadoException y no persiste cuando el nombre está en uso por otra presentación comercial")
    void modificarPresentacionComercial_debeRechazarNombreEnUsoPorOtraPresentacionComercial() {
        PresentacionComercialEntity presentacionEntity = crearPresentacionComercialEntity(1L, "Bolsa de 25 Kg", 25.0, UnidadDeMedida.KILOGRAMO);
        PresentacionComercialFormDTO formDTO = presentacionComercialFormDTO("Paquete de 100 gm", 25.0, UnidadDeMedida.KILOGRAMO);
        when(presentacionComercialRepository.findById(1L)).thenReturn(Optional.of(presentacionEntity));
        when(presentacionComercialRepository.existsByNombreIgnoreCaseAndIdNot("Paquete de 100 gm", 1L)).thenReturn(true);

        assertThatThrownBy(() -> presentacionComercialServicio.modificarPresentacionComercial(1L, formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("El nombre 'Paquete de 100 gm' ya está en uso por otra presentación comercial");

        verify(presentacionComercialRepository).existsByNombreIgnoreCaseAndIdNot("Paquete de 100 gm", 1L);
        verify(presentacionComercialRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MPC-06: modificarPresentacionComercial permite conservar el propio nombre actual al actualizar")
    void modificarPresentacionComercial_debePermitirConservarNombrePropio() {
        PresentacionComercialEntity presentacionEntity = crearPresentacionComercialEntity(1L, "Bolsa de 25 Kg", 25.0, UnidadDeMedida.KILOGRAMO);
        // Mismo nombre (distinto case): el AndIdNot excluye el propio ID y no debe fallar
        PresentacionComercialFormDTO formDTO = presentacionComercialFormDTO("bolsa de 25 kg", 25.0, UnidadDeMedida.KILOGRAMO);
        when(presentacionComercialRepository.findById(1L)).thenReturn(Optional.of(presentacionEntity));
        when(presentacionComercialRepository.existsByNombreIgnoreCaseAndIdNot("bolsa de 25 kg", 1L)).thenReturn(false);
        when(presentacionComercialRepository.save(presentacionEntity)).thenReturn(presentacionEntity);

        PresentacionComercialResponseDTO resultado = presentacionComercialServicio.modificarPresentacionComercial(1L, formDTO);

        assertThat(resultado.getNombre()).isEqualTo("bolsa de 25 kg");
        verify(presentacionComercialRepository).save(presentacionEntity);
    }

    @Test
    @DisplayName("CP-MPC-07: modificarPresentacionComercial actualiza cantidad y unidad de medida")
    void modificarPresentacionComercial_debeActualizarCantidadYUnidadDeMedida() {
        // === PREPARACION DE DATOS ===
        PresentacionComercialEntity presentacionEntity = crearPresentacionComercialEntity(1L, "Bolsa de 25 Kg", 25.0, UnidadDeMedida.KILOGRAMO);
        PresentacionComercialFormDTO formDTO = presentacionComercialFormDTO("Bolsa de 25 Kg", 25000.0, UnidadDeMedida.GRAMO);
        when(presentacionComercialRepository.findById(1L)).thenReturn(Optional.of(presentacionEntity));
        when(presentacionComercialRepository.existsByNombreIgnoreCaseAndIdNot("Bolsa de 25 Kg", 1L)).thenReturn(false);
        when(presentacionComercialRepository.save(presentacionEntity)).thenReturn(presentacionEntity);

        // === EJECUCION ===
        PresentacionComercialResponseDTO resultado = presentacionComercialServicio.modificarPresentacionComercial(1L, formDTO);

        // === ASSERTS ===
        ArgumentCaptor<PresentacionComercialEntity> captor = ArgumentCaptor.forClass(PresentacionComercialEntity.class);
        verify(presentacionComercialRepository).save(captor.capture());
        PresentacionComercialEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getCantidad()).isEqualTo(25000.0);
        assertThat(entidadCapturada.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.GRAMO);

        assertThat(resultado.getCantidad()).isEqualTo(25000.0);
        assertThat(resultado.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.GRAMO);
    }

    @Test
    @DisplayName("CP-MPC-08: modificarPresentacionComercial actualiza los datos y persiste cuando el ID existe y el nombre está libre (camino feliz)")
    void modificarPresentacionComercial_debeActualizarPresentacionComercialExistente() {
        // === PREPARACION DE DATOS ===
        PresentacionComercialEntity presentacionEntity = crearPresentacionComercialEntity(1L, "Bolsa de 25 Kg", 25.0, UnidadDeMedida.KILOGRAMO);
        PresentacionComercialFormDTO formDTO = presentacionComercialFormDTO("Bolsa de 50 Kg", 50.0, UnidadDeMedida.KILOGRAMO);
        when(presentacionComercialRepository.findById(1L)).thenReturn(Optional.of(presentacionEntity));
        when(presentacionComercialRepository.existsByNombreIgnoreCaseAndIdNot("Bolsa de 50 Kg", 1L)).thenReturn(false);
        when(presentacionComercialRepository.save(presentacionEntity)).thenReturn(presentacionEntity);

        // === EJECUCION ===
        PresentacionComercialResponseDTO resultado = presentacionComercialServicio.modificarPresentacionComercial(1L, formDTO);

        // === ASSERTS ===
        ArgumentCaptor<PresentacionComercialEntity> captor = ArgumentCaptor.forClass(PresentacionComercialEntity.class);
        verify(presentacionComercialRepository).save(captor.capture());
        PresentacionComercialEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getNombre()).isEqualTo("Bolsa de 50 Kg");
        assertThat(entidadCapturada.getCantidad()).isEqualTo(50.0);
        assertThat(entidadCapturada.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.KILOGRAMO);

        assertThat(resultado.getNombre()).isEqualTo("Bolsa de 50 Kg");
        verify(presentacionComercialRepository).findById(1L);
        verify(presentacionComercialRepository).existsByNombreIgnoreCaseAndIdNot("Bolsa de 50 Kg", 1L);
    }

    // ==================== bajaPresentacionComercial ====================

    @Test
    @DisplayName("CP-BPC-01: bajaPresentacionComercial lanza RecursoNoEncontradoException y no consulta catálogos ni elimina cuando el ID no existe")
    void bajaPresentacionComercial_debeLanzarExcepcionYNoEliminarSiNoExiste() {
        when(presentacionComercialRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> presentacionComercialServicio.bajaPresentacionComercial(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la presentación comercial con ID: 99");

        verify(presentacionComercialRepository).findById(99L);
        verifyNoInteractions(catalogoProveedorRepository);
        verify(presentacionComercialRepository, never()).delete(any());
    }

    @Test
    @DisplayName("CP-BPC-02: bajaPresentacionComercial lanza ReglaNegocioException y no elimina cuando está asociada a un catálogo de proveedor activo")
    void bajaPresentacionComercial_debeRechazarConCatalogoDeProveedorAsociado() {
        PresentacionComercialEntity presentacionEntity = crearPresentacionComercialEntity(1L, "Bolsa de 25 Kg", 25.0, UnidadDeMedida.KILOGRAMO);
        when(presentacionComercialRepository.findById(1L)).thenReturn(Optional.of(presentacionEntity));
        when(catalogoProveedorRepository.existsByPresentacionComercialId(1L)).thenReturn(true);

        assertThatThrownBy(() -> presentacionComercialServicio.bajaPresentacionComercial(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja la presentación comercial porque se encuentra asociada al catálogo de al menos un proveedor activo");

        verify(catalogoProveedorRepository).existsByPresentacionComercialId(1L);
        verify(presentacionComercialRepository, never()).delete(any());
    }

    @Test
    @DisplayName("CP-BPC-03: bajaPresentacionComercial elimina lógicamente (soft-delete) y retorna el DTO cuando no tiene catálogos asociados")
    void bajaPresentacionComercial_debeEliminarYRetornarPresentacionComercialExistente() {
        PresentacionComercialEntity presentacionEntity = crearPresentacionComercialEntity(1L, "Bolsa de 25 Kg", 25.0, UnidadDeMedida.KILOGRAMO);
        when(presentacionComercialRepository.findById(1L)).thenReturn(Optional.of(presentacionEntity));
        when(catalogoProveedorRepository.existsByPresentacionComercialId(1L)).thenReturn(false);

        PresentacionComercialResponseDTO resultado = presentacionComercialServicio.bajaPresentacionComercial(1L);

        // El borrado físico a nivel repositorio es convertido a UPDATE por el @SoftDelete de Hibernate
        assertPresentacionComercialDTO(presentacionEntity, resultado);
        verify(presentacionComercialRepository).findById(1L);
        verify(catalogoProveedorRepository).existsByPresentacionComercialId(1L);
        verify(presentacionComercialRepository).delete(presentacionEntity);
    }

    // ==================== helpers ====================

    private static PresentacionComercialEntity crearPresentacionComercialEntity(Long id, String nombre, Double cantidad, UnidadDeMedida unidadDeMedida) {
        return PresentacionComercialEntity.builder()
                .id(id)
                .nombre(nombre)
                .cantidad(cantidad)
                .unidadDeMedida(unidadDeMedida)
                .build();
    }

    private static PresentacionComercialFormDTO presentacionComercialFormDTO(String nombre, Double cantidad, UnidadDeMedida unidadDeMedida) {
        return PresentacionComercialFormDTO.builder()
                .nombre(nombre)
                .cantidad(cantidad)
                .unidadDeMedida(unidadDeMedida)
                .build();
    }

    private static void assertPresentacionComercialDTO(PresentacionComercialEntity entidad, PresentacionComercialResponseDTO dto) {
        assertThat(dto.getId()).isEqualTo(entidad.getId());
        assertThat(dto.getNombre()).isEqualTo(entidad.getNombre());
        assertThat(dto.getCantidad()).isEqualTo(entidad.getCantidad());
        assertThat(dto.getUnidadDeMedida()).isEqualTo(entidad.getUnidadDeMedida());
    }
}
