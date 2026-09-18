package com.github.heikyudev.maestrocervecero.service.implementation.costo_adicional;

import com.github.heikyudev.maestrocervecero.persistence.entity.costo_adicional.CostoDirectoAdicionalEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.costo_adicional.ICostoDirectoAdicionalRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.costo_adicional.CostoDirectoAdicionalFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.costo_adicional.CostoDirectoAdicionalResponseDTO;
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

import java.math.BigDecimal;
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
class CostoDirectoAdicionalServicioImplTest {

    @Mock
    private ICostoDirectoAdicionalRepository costoDirectoAdicionalRepository;

    @InjectMocks
    private CostoDirectoAdicionalServicioImpl costoDirectoAdicionalServicio;

    // ==================== filtrarCostosDirectosAdicionales ====================

    @Test
    @DisplayName("CP-FCDA-01: filtrarCostosDirectosAdicionales retorna una página de costos correctamente mapeada a DTO cuando se filtra por nombre")
    void filtrarCostosDirectosAdicionales_debeRetornarPaginaMapeadaFiltrandoPorNombre() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        CostoDirectoAdicionalEntity costoEntity = crearCostoDirectoAdicionalEntity(1L, "Gas natural", new BigDecimal("3.20"));
        when(costoDirectoAdicionalRepository.filtrarCostosDirectosAdicionales("Gas", pageable))
                .thenReturn(new PageImpl<>(List.of(costoEntity), pageable, 1));

        // === EJECUCION ===
        Page<CostoDirectoAdicionalResponseDTO> resultado = costoDirectoAdicionalServicio.filtrarCostosDirectosAdicionales("Gas", pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertCostoDirectoAdicionalDTO(costoEntity, resultado.getContent().get(0));
        verify(costoDirectoAdicionalRepository).filtrarCostosDirectosAdicionales("Gas", pageable);
    }

    @Test
    @DisplayName("CP-FCDA-02: filtrarCostosDirectosAdicionales propaga el nombre nulo sin restringir la búsqueda")
    void filtrarCostosDirectosAdicionales_debePropagarNombreNulo() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        CostoDirectoAdicionalEntity costoEntity = crearCostoDirectoAdicionalEntity(1L, "Energía eléctrica", new BigDecimal("5.00"));
        CostoDirectoAdicionalEntity otroCostoEntity = crearCostoDirectoAdicionalEntity(2L, "Gas natural", new BigDecimal("3.20"));
        CostoDirectoAdicionalEntity tercerCostoEntity = crearCostoDirectoAdicionalEntity(3L, "Mantenimiento de equipos", new BigDecimal("1.75"));
        when(costoDirectoAdicionalRepository.filtrarCostosDirectosAdicionales(null, pageable))
                .thenReturn(new PageImpl<>(List.of(costoEntity, otroCostoEntity, tercerCostoEntity), pageable, 3));

        // === EJECUCION ===
        Page<CostoDirectoAdicionalResponseDTO> resultado = costoDirectoAdicionalServicio.filtrarCostosDirectosAdicionales(null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(3);
        verify(costoDirectoAdicionalRepository).filtrarCostosDirectosAdicionales(null, pageable);
    }

    @Test
    @DisplayName("CP-FCDA-03: filtrarCostosDirectosAdicionales retorna una página vacía cuando ningún registro cumple el criterio")
    void filtrarCostosDirectosAdicionales_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(costoDirectoAdicionalRepository.filtrarCostosDirectosAdicionales("Inexistente", pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<CostoDirectoAdicionalResponseDTO> resultado = costoDirectoAdicionalServicio.filtrarCostosDirectosAdicionales("Inexistente", pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(costoDirectoAdicionalRepository).filtrarCostosDirectosAdicionales("Inexistente", pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO del costo directo adicional cuando el ID existe")
    void buscarPorId_debeRetornarCostoDirectoAdicionalExistente() {
        // === PREPARACION DE DATOS ===
        CostoDirectoAdicionalEntity costoEntity = crearCostoDirectoAdicionalEntity(1L, "Energía eléctrica", new BigDecimal("5.00"));
        when(costoDirectoAdicionalRepository.findById(1L)).thenReturn(Optional.of(costoEntity));

        // === EJECUCION ===
        CostoDirectoAdicionalResponseDTO resultado = costoDirectoAdicionalServicio.buscarPorId(1L);

        // === ASSERTS ===
        assertCostoDirectoAdicionalDTO(costoEntity, resultado);
        verify(costoDirectoAdicionalRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe o está dado de baja")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        // findById filtra por estado = ACTIVO, por lo que devuelve Optional.empty() también para costos dados de baja
        when(costoDirectoAdicionalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> costoDirectoAdicionalServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el costo directo adicional con ID: 99");
        verify(costoDirectoAdicionalRepository).findById(99L);
    }

    // ==================== altaCostoDirectoAdicional ====================

    @Test
    @DisplayName("CP-ACD-01: altaCostoDirectoAdicional lanza RecursoDuplicadoException y no persiste cuando el nombre ya existe")
    void altaCostoDirectoAdicional_debeRechazarNombreDuplicado() {
        CostoDirectoAdicionalFormDTO formDTO = costoDirectoAdicionalFormDTO("Energía eléctrica", new BigDecimal("5.00"));
        when(costoDirectoAdicionalRepository.existsByNombreIgnoreCase("Energía eléctrica")).thenReturn(true);

        assertThatThrownBy(() -> costoDirectoAdicionalServicio.altaCostoDirectoAdicional(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un costo directo adicional con el nombre 'Energía eléctrica'");

        verify(costoDirectoAdicionalRepository).existsByNombreIgnoreCase("Energía eléctrica");
        verify(costoDirectoAdicionalRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ACD-02: altaCostoDirectoAdicional lanza RecursoDuplicadoException cuando el nombre ya existe con distinto case (case-insensitive)")
    void altaCostoDirectoAdicional_debeRechazarNombreDuplicadoCaseInsensitive() {
        CostoDirectoAdicionalFormDTO formDTO = costoDirectoAdicionalFormDTO("energía eléctrica", new BigDecimal("5.00"));
        when(costoDirectoAdicionalRepository.existsByNombreIgnoreCase("energía eléctrica")).thenReturn(true);

        assertThatThrownBy(() -> costoDirectoAdicionalServicio.altaCostoDirectoAdicional(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un costo directo adicional con el nombre 'energía eléctrica'");

        verify(costoDirectoAdicionalRepository).existsByNombreIgnoreCase("energía eléctrica");
        verify(costoDirectoAdicionalRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ACD-03: altaCostoDirectoAdicional lanza ReglaNegocioException cuando el costo por litro es nulo")
    void altaCostoDirectoAdicional_debeRechazarCostoPorLitroNulo() {
        CostoDirectoAdicionalFormDTO formDTO = costoDirectoAdicionalFormDTO("Gas natural", null);
        when(costoDirectoAdicionalRepository.existsByNombreIgnoreCase("Gas natural")).thenReturn(false);

        assertThatThrownBy(() -> costoDirectoAdicionalServicio.altaCostoDirectoAdicional(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El costo por litro debe ser mayor a cero");

        verify(costoDirectoAdicionalRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ACD-04: altaCostoDirectoAdicional lanza ReglaNegocioException cuando el costo por litro es cero (límite)")
    void altaCostoDirectoAdicional_debeRechazarCostoPorLitroEnCero() {
        CostoDirectoAdicionalFormDTO formDTO = costoDirectoAdicionalFormDTO("Gas natural", BigDecimal.ZERO);
        when(costoDirectoAdicionalRepository.existsByNombreIgnoreCase("Gas natural")).thenReturn(false);

        assertThatThrownBy(() -> costoDirectoAdicionalServicio.altaCostoDirectoAdicional(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El costo por litro debe ser mayor a cero");

        verify(costoDirectoAdicionalRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ACD-05: altaCostoDirectoAdicional lanza ReglaNegocioException cuando el costo por litro es negativo")
    void altaCostoDirectoAdicional_debeRechazarCostoPorLitroNegativo() {
        CostoDirectoAdicionalFormDTO formDTO = costoDirectoAdicionalFormDTO("Gas natural", new BigDecimal("-0.01"));
        when(costoDirectoAdicionalRepository.existsByNombreIgnoreCase("Gas natural")).thenReturn(false);

        assertThatThrownBy(() -> costoDirectoAdicionalServicio.altaCostoDirectoAdicional(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El costo por litro debe ser mayor a cero");

        verify(costoDirectoAdicionalRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ACD-06: altaCostoDirectoAdicional valida el nombre duplicado antes que el costo por litro")
    void altaCostoDirectoAdicional_debeValidarDuplicadoAntesQueCosto() {
        CostoDirectoAdicionalFormDTO formDTO = costoDirectoAdicionalFormDTO("Energía eléctrica", BigDecimal.ZERO);
        when(costoDirectoAdicionalRepository.existsByNombreIgnoreCase("Energía eléctrica")).thenReturn(true);

        assertThatThrownBy(() -> costoDirectoAdicionalServicio.altaCostoDirectoAdicional(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un costo directo adicional con el nombre 'Energía eléctrica'");

        verify(costoDirectoAdicionalRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ACD-07: altaCostoDirectoAdicional persiste con éxito cuando el costo por litro está en el límite inferior válido")
    void altaCostoDirectoAdicional_debePersistirConCostoPorLitroEnLimiteInferiorValido() {
        // === PREPARACION DE DATOS ===
        CostoDirectoAdicionalFormDTO formDTO = costoDirectoAdicionalFormDTO("Mantenimiento de equipos", new BigDecimal("0.0001"));
        when(costoDirectoAdicionalRepository.existsByNombreIgnoreCase("Mantenimiento de equipos")).thenReturn(false);
        when(costoDirectoAdicionalRepository.save(any(CostoDirectoAdicionalEntity.class))).thenAnswer(invocation -> {
            CostoDirectoAdicionalEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(4L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        CostoDirectoAdicionalResponseDTO resultado = costoDirectoAdicionalServicio.altaCostoDirectoAdicional(formDTO);

        // === ASSERTS ===
        ArgumentCaptor<CostoDirectoAdicionalEntity> captor = ArgumentCaptor.forClass(CostoDirectoAdicionalEntity.class);
        verify(costoDirectoAdicionalRepository).save(captor.capture());
        assertThat(captor.getValue().getCostoPorLitro()).isEqualByComparingTo("0.0001");

        assertThat(resultado.getCostoPorLitro()).isEqualByComparingTo("0.0001");
        assertThat(resultado.getNombre()).isEqualTo("Mantenimiento de equipos");
    }

    @Test
    @DisplayName("CP-ACD-08: altaCostoDirectoAdicional persiste y retorna el DTO cuando los datos son válidos (camino feliz)")
    void altaCostoDirectoAdicional_debePersistirYRetornarDTOCuandoDatosSonValidos() {
        // === PREPARACION DE DATOS ===
        CostoDirectoAdicionalFormDTO formDTO = costoDirectoAdicionalFormDTO("Energía eléctrica", new BigDecimal("5.00"));
        when(costoDirectoAdicionalRepository.existsByNombreIgnoreCase("Energía eléctrica")).thenReturn(false);
        when(costoDirectoAdicionalRepository.save(any(CostoDirectoAdicionalEntity.class))).thenAnswer(invocation -> {
            CostoDirectoAdicionalEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(1L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        CostoDirectoAdicionalResponseDTO resultado = costoDirectoAdicionalServicio.altaCostoDirectoAdicional(formDTO);

        // === ASSERTS ===
        ArgumentCaptor<CostoDirectoAdicionalEntity> captor = ArgumentCaptor.forClass(CostoDirectoAdicionalEntity.class);
        verify(costoDirectoAdicionalRepository).save(captor.capture());
        CostoDirectoAdicionalEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getNombre()).isEqualTo("Energía eléctrica");
        assertThat(entidadCapturada.getCostoPorLitro()).isEqualByComparingTo("5.00");
        // El alta siempre debe registrar al costo directo adicional como ACTIVO, sin importar lo que traiga el FormDTO
        assertThat(entidadCapturada.getEstado()).isEqualTo(Estado.ACTIVO);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Energía eléctrica");
        assertThat(resultado.getCostoPorLitro()).isEqualByComparingTo("5.00");
        verify(costoDirectoAdicionalRepository).existsByNombreIgnoreCase("Energía eléctrica");
    }

    // ==================== modificarCostoDirectoAdicional ====================

    @Test
    @DisplayName("CP-MCD-01: modificarCostoDirectoAdicional lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void modificarCostoDirectoAdicional_debeLanzarExcepcionSiNoExiste() {
        CostoDirectoAdicionalFormDTO formDTO = costoDirectoAdicionalFormDTO("Energía eléctrica", new BigDecimal("5.00"));
        when(costoDirectoAdicionalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> costoDirectoAdicionalServicio.modificarCostoDirectoAdicional(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el costo directo adicional con ID: 99");

        verify(costoDirectoAdicionalRepository).findById(99L);
        // Al no existir la entidad, no debe llegarse a validar duplicidad de nombre ni costo
        verify(costoDirectoAdicionalRepository, never()).existsByNombreIgnoreCaseAndIdNot(any(), any());
        verify(costoDirectoAdicionalRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MCD-02: modificarCostoDirectoAdicional lanza RecursoDuplicadoException y no persiste cuando el nombre está en uso por otro costo directo adicional")
    void modificarCostoDirectoAdicional_debeRechazarNombreEnUsoPorOtroCostoDirectoAdicional() {
        CostoDirectoAdicionalEntity costoEntity = crearCostoDirectoAdicionalEntity(1L, "Energía eléctrica", new BigDecimal("5.00"));
        CostoDirectoAdicionalFormDTO formDTO = costoDirectoAdicionalFormDTO("Gas natural", new BigDecimal("5.00"));
        when(costoDirectoAdicionalRepository.findById(1L)).thenReturn(Optional.of(costoEntity));
        when(costoDirectoAdicionalRepository.existsByNombreIgnoreCaseAndIdNot("Gas natural", 1L)).thenReturn(true);

        assertThatThrownBy(() -> costoDirectoAdicionalServicio.modificarCostoDirectoAdicional(1L, formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("El nombre 'Gas natural' ya está en uso por otro costo directo adicional");

        verify(costoDirectoAdicionalRepository).existsByNombreIgnoreCaseAndIdNot("Gas natural", 1L);
        verify(costoDirectoAdicionalRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MCD-03: modificarCostoDirectoAdicional permite conservar el propio nombre actual al actualizar")
    void modificarCostoDirectoAdicional_debePermitirConservarNombrePropio() {
        CostoDirectoAdicionalEntity costoEntity = crearCostoDirectoAdicionalEntity(1L, "Energía eléctrica", new BigDecimal("5.00"));
        // Mismo nombre (distinto case): el AndIdNot excluye el propio ID y no debe fallar
        CostoDirectoAdicionalFormDTO formDTO = costoDirectoAdicionalFormDTO("energía eléctrica", new BigDecimal("5.00"));
        when(costoDirectoAdicionalRepository.findById(1L)).thenReturn(Optional.of(costoEntity));
        when(costoDirectoAdicionalRepository.existsByNombreIgnoreCaseAndIdNot("energía eléctrica", 1L)).thenReturn(false);
        when(costoDirectoAdicionalRepository.save(costoEntity)).thenReturn(costoEntity);

        CostoDirectoAdicionalResponseDTO resultado = costoDirectoAdicionalServicio.modificarCostoDirectoAdicional(1L, formDTO);

        assertThat(resultado.getNombre()).isEqualTo("energía eléctrica");
        verify(costoDirectoAdicionalRepository).save(costoEntity);
    }

    @Test
    @DisplayName("CP-MCD-04: modificarCostoDirectoAdicional lanza ReglaNegocioException cuando el costo por litro es nulo")
    void modificarCostoDirectoAdicional_debeRechazarCostoPorLitroNulo() {
        CostoDirectoAdicionalEntity costoEntity = crearCostoDirectoAdicionalEntity(1L, "Energía eléctrica", new BigDecimal("5.00"));
        CostoDirectoAdicionalFormDTO formDTO = costoDirectoAdicionalFormDTO("Energía eléctrica", null);
        when(costoDirectoAdicionalRepository.findById(1L)).thenReturn(Optional.of(costoEntity));
        when(costoDirectoAdicionalRepository.existsByNombreIgnoreCaseAndIdNot("Energía eléctrica", 1L)).thenReturn(false);

        assertThatThrownBy(() -> costoDirectoAdicionalServicio.modificarCostoDirectoAdicional(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El costo por litro debe ser mayor a cero");

        verify(costoDirectoAdicionalRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MCD-05: modificarCostoDirectoAdicional lanza ReglaNegocioException cuando el costo por litro es cero (límite)")
    void modificarCostoDirectoAdicional_debeRechazarCostoPorLitroEnCero() {
        CostoDirectoAdicionalEntity costoEntity = crearCostoDirectoAdicionalEntity(1L, "Energía eléctrica", new BigDecimal("5.00"));
        CostoDirectoAdicionalFormDTO formDTO = costoDirectoAdicionalFormDTO("Energía eléctrica", BigDecimal.ZERO);
        when(costoDirectoAdicionalRepository.findById(1L)).thenReturn(Optional.of(costoEntity));
        when(costoDirectoAdicionalRepository.existsByNombreIgnoreCaseAndIdNot("Energía eléctrica", 1L)).thenReturn(false);

        assertThatThrownBy(() -> costoDirectoAdicionalServicio.modificarCostoDirectoAdicional(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El costo por litro debe ser mayor a cero");

        verify(costoDirectoAdicionalRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MCD-06: modificarCostoDirectoAdicional lanza ReglaNegocioException cuando el costo por litro es negativo")
    void modificarCostoDirectoAdicional_debeRechazarCostoPorLitroNegativo() {
        CostoDirectoAdicionalEntity costoEntity = crearCostoDirectoAdicionalEntity(1L, "Energía eléctrica", new BigDecimal("5.00"));
        CostoDirectoAdicionalFormDTO formDTO = costoDirectoAdicionalFormDTO("Energía eléctrica", new BigDecimal("-0.01"));
        when(costoDirectoAdicionalRepository.findById(1L)).thenReturn(Optional.of(costoEntity));
        when(costoDirectoAdicionalRepository.existsByNombreIgnoreCaseAndIdNot("Energía eléctrica", 1L)).thenReturn(false);

        assertThatThrownBy(() -> costoDirectoAdicionalServicio.modificarCostoDirectoAdicional(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El costo por litro debe ser mayor a cero");

        verify(costoDirectoAdicionalRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MCD-07: modificarCostoDirectoAdicional actualiza los datos y persiste cuando el ID existe y el nombre está libre (camino feliz)")
    void modificarCostoDirectoAdicional_debeActualizarCostoDirectoAdicionalExistente() {
        // === PREPARACION DE DATOS ===
        CostoDirectoAdicionalEntity costoEntity = crearCostoDirectoAdicionalEntity(1L, "Energía eléctrica", new BigDecimal("5.00"));
        CostoDirectoAdicionalFormDTO formDTO = costoDirectoAdicionalFormDTO("Energía eléctrica (tarifa 2026)", new BigDecimal("6.50"));
        when(costoDirectoAdicionalRepository.findById(1L)).thenReturn(Optional.of(costoEntity));
        when(costoDirectoAdicionalRepository.existsByNombreIgnoreCaseAndIdNot("Energía eléctrica (tarifa 2026)", 1L)).thenReturn(false);
        when(costoDirectoAdicionalRepository.save(costoEntity)).thenReturn(costoEntity);

        // === EJECUCION ===
        CostoDirectoAdicionalResponseDTO resultado = costoDirectoAdicionalServicio.modificarCostoDirectoAdicional(1L, formDTO);

        // === ASSERTS ===
        ArgumentCaptor<CostoDirectoAdicionalEntity> captor = ArgumentCaptor.forClass(CostoDirectoAdicionalEntity.class);
        verify(costoDirectoAdicionalRepository).save(captor.capture());
        CostoDirectoAdicionalEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getNombre()).isEqualTo("Energía eléctrica (tarifa 2026)");
        assertThat(entidadCapturada.getCostoPorLitro()).isEqualByComparingTo("6.50");

        assertThat(resultado.getNombre()).isEqualTo("Energía eléctrica (tarifa 2026)");
        assertThat(resultado.getCostoPorLitro()).isEqualByComparingTo("6.50");
        verify(costoDirectoAdicionalRepository).findById(1L);
        verify(costoDirectoAdicionalRepository).existsByNombreIgnoreCaseAndIdNot("Energía eléctrica (tarifa 2026)", 1L);
    }

    // ==================== bajaCostoDirectoAdicional ====================

    @Test
    @DisplayName("CP-BCD-01: bajaCostoDirectoAdicional lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void bajaCostoDirectoAdicional_debeLanzarExcepcionYNoPersistirSiNoExiste() {
        when(costoDirectoAdicionalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> costoDirectoAdicionalServicio.bajaCostoDirectoAdicional(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el costo directo adicional con ID: 99");

        verify(costoDirectoAdicionalRepository).findById(99L);
        verify(costoDirectoAdicionalRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BCD-02: bajaCostoDirectoAdicional marca el estado como BAJA, persiste y retorna el DTO")
    void bajaCostoDirectoAdicional_debeMarcarBajaYRetornarCostoDirectoAdicionalExistente() {
        CostoDirectoAdicionalEntity costoEntity = crearCostoDirectoAdicionalEntity(1L, "Energía eléctrica", new BigDecimal("5.00"));
        when(costoDirectoAdicionalRepository.findById(1L)).thenReturn(Optional.of(costoEntity));
        when(costoDirectoAdicionalRepository.save(costoEntity)).thenReturn(costoEntity);

        CostoDirectoAdicionalResponseDTO resultado = costoDirectoAdicionalServicio.bajaCostoDirectoAdicional(1L);

        // La baja es lógica: el estado pasa a BAJA y se persiste con save(), nunca con delete()
        assertThat(costoEntity.getEstado()).isEqualTo(Estado.BAJA);
        assertCostoDirectoAdicionalDTO(costoEntity, resultado);
        verify(costoDirectoAdicionalRepository).findById(1L);
        verify(costoDirectoAdicionalRepository).save(costoEntity);
        verify(costoDirectoAdicionalRepository, never()).delete(any());
    }

    // ==================== helpers ====================

    private static CostoDirectoAdicionalEntity crearCostoDirectoAdicionalEntity(Long id, String nombre, BigDecimal costoPorLitro) {
        return CostoDirectoAdicionalEntity.builder()
                .id(id)
                .nombre(nombre)
                .costoPorLitro(costoPorLitro)
                .estado(Estado.ACTIVO)
                .build();
    }

    private static CostoDirectoAdicionalFormDTO costoDirectoAdicionalFormDTO(String nombre, BigDecimal costoPorLitro) {
        return CostoDirectoAdicionalFormDTO.builder()
                .nombre(nombre)
                .costoPorLitro(costoPorLitro)
                .build();
    }

    private static void assertCostoDirectoAdicionalDTO(CostoDirectoAdicionalEntity entidad, CostoDirectoAdicionalResponseDTO dto) {
        assertThat(dto.getId()).isEqualTo(entidad.getId());
        assertThat(dto.getNombre()).isEqualTo(entidad.getNombre());
        assertThat(dto.getCostoPorLitro()).isEqualByComparingTo(entidad.getCostoPorLitro());
        assertThat(dto.getEstado()).isEqualTo(entidad.getEstado());
    }
}
