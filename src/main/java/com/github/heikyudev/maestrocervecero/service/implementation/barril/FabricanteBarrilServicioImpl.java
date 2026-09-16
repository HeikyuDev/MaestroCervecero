package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FabricanteBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IFabricanteBarrilRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.FabricanteBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IFabricanteBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.FabricanteBarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.barril.MapperFabricanteBarril;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del servicio de Fabricante de Barril.
 * <p>
 * Un barril puede repetir identificador entre fabricantes distintos (la unicidad del
 * identificador de un barril es relativa a su fabricante), pero un fabricante no puede repetir
 * razón social ni CUIT con otro fabricante activo.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class FabricanteBarrilServicioImpl implements IFabricanteBarrilServicio {

    private final IFabricanteBarrilRepository fabricanteBarrilRepository;
    private final IBarrilRepository barrilRepository;

    /**
     * Recupera una página de fabricantes de barril activos, filtrados opcionalmente por razón
     * social, nombre comercial y/o CUIT.
     * <p>
     * Los fabricantes dados de baja son excluidos por la condición {@code estado = 'ACTIVO'}
     * aplicada en el repositorio.
     * </p>
     *
     * @param razonSocial Texto a buscar dentro de la razón social, o {@code null} para no filtrar por ella.
     * @param nombreComercial Texto a buscar dentro del nombre comercial, o {@code null} para no filtrar por él.
     * @param cuit Texto a buscar dentro del CUIT, o {@code null} para no filtrar por él.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link FabricanteBarrilResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<FabricanteBarrilResponseDTO> filtrarFabricantesBarril(String razonSocial, String nombreComercial, String cuit, Pageable pageable) {
        return fabricanteBarrilRepository.filtrarFabricantesBarril(razonSocial, nombreComercial, cuit, pageable)
                .map(MapperFabricanteBarril::toDTO);
    }

    /**
     * Busca y retorna un fabricante de barril específico mediante su identificador único.
     *
     * @param id Identificador clave primaria del fabricante de barril buscado.
     * @return Objeto {@link FabricanteBarrilResponseDTO} con la información del fabricante encontrado.
     * @throws RecursoNoEncontradoException Si no existe ningún fabricante de barril activo con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public FabricanteBarrilResponseDTO buscarPorId(Long id) {
        return MapperFabricanteBarril.toDTO(fabricanteBarrilRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el fabricante de barril con ID: " + id)));
    }

    /**
     * Registra un nuevo fabricante de barril en el sistema.
     * <p>
     * Valida que la razón social y el CUIT no estén duplicados (case-insensitive) entre los
     * fabricantes activos.
     * </p>
     *
     * @param fabricanteBarrilFormDTO Objeto DTO que contiene los datos de creación del fabricante de barril.
     * @return {@link FabricanteBarrilResponseDTO} representativo del fabricante guardado en la base de datos.
     * @throws RecursoDuplicadoException Si la razón social ya pertenece a otro fabricante de barril activo, o si el CUIT ya pertenece a otro fabricante de barril activo.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.FABRICANTE_BARRIL)
    public FabricanteBarrilResponseDTO altaFabricanteBarril(FabricanteBarrilFormDTO fabricanteBarrilFormDTO) {
        // 1. Validar que la razón social no esté registrada en otro fabricante activo
        if (fabricanteBarrilRepository.existsByRazonSocialIgnoreCase(fabricanteBarrilFormDTO.getRazonSocial())) {
            throw new RecursoDuplicadoException("Ya existe un fabricante de barril activo con la razón social '" + fabricanteBarrilFormDTO.getRazonSocial() + "'");
        }

        // 2. Validar que el CUIT no esté registrado en otro fabricante activo
        if (fabricanteBarrilRepository.existsByCuit(fabricanteBarrilFormDTO.getCuit())) {
            throw new RecursoDuplicadoException("Ya existe un fabricante de barril activo con el CUIT '" + fabricanteBarrilFormDTO.getCuit() + "'");
        }

        // 3. Creo la entidad que se va a almacenar en la base de datos
        FabricanteBarrilEntity fabricanteBarrilEntity = FabricanteBarrilEntity.builder()
                .razonSocial(fabricanteBarrilFormDTO.getRazonSocial())
                .nombreComercial(fabricanteBarrilFormDTO.getNombreComercial())
                .cuit(fabricanteBarrilFormDTO.getCuit())
                .telefono(fabricanteBarrilFormDTO.getTelefono())
                .email(fabricanteBarrilFormDTO.getEmail())
                .estado(Estado.ACTIVO)
                .build();

        // 4. Guardo la entidad en la base de datos y devuelvo el DTO correspondiente
        return MapperFabricanteBarril.toDTO(fabricanteBarrilRepository.save(fabricanteBarrilEntity));
    }

    /**
     * Actualiza la información de un fabricante de barril existente en la base de datos.
     * <p>
     * Localiza el fabricante por su ID y verifica que la nueva razón social o el nuevo CUIT no
     * colisionen con los de otro fabricante activo (case-insensitive), permitiendo conservar los
     * propios valores actuales.
     * </p>
     *
     * @param id Identificador clave primaria del fabricante de barril a modificar.
     * @param fabricanteBarrilFormDTO DTO con la información actualizada.
     * @return {@link FabricanteBarrilResponseDTO} representativo del fabricante con los cambios aplicados.
     * @throws RecursoNoEncontradoException Si no se localiza un fabricante de barril activo por el ID proporcionado.
     * @throws RecursoDuplicadoException Si la nueva razón social ya pertenece a otro fabricante de barril activo, o si el nuevo CUIT ya pertenece a otro fabricante de barril activo.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.FABRICANTE_BARRIL)
    public FabricanteBarrilResponseDTO modificarFabricanteBarril(Long id, FabricanteBarrilFormDTO fabricanteBarrilFormDTO) {
        // 1. Localizar el fabricante existente. Si no existe, se dispara RecursoNoEncontradoException
        FabricanteBarrilEntity fabricanteBarrilEntity = fabricanteBarrilRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el fabricante de barril con ID: " + id));

        // 2. Validar la razón social excluyendo el propio ID, de modo que conservar la razón
        //    social actual no falle contra el mismo registro
        if (fabricanteBarrilRepository.existsByRazonSocialIgnoreCaseAndIdNot(fabricanteBarrilFormDTO.getRazonSocial(), id)) {
            throw new RecursoDuplicadoException("Ya existe otro fabricante de barril activo con la razón social '" + fabricanteBarrilFormDTO.getRazonSocial() + "'");
        }

        // 3. Validar el CUIT excluyendo el propio ID, de modo que conservar el CUIT actual no
        //    falle contra el mismo registro
        if (fabricanteBarrilRepository.existsByCuitAndIdNot(fabricanteBarrilFormDTO.getCuit(), id)) {
            throw new RecursoDuplicadoException("Ya existe otro fabricante de barril activo con el CUIT '" + fabricanteBarrilFormDTO.getCuit() + "'");
        }

        // 4. Aplico los cambios sobre la entidad administrada por persistencia
        fabricanteBarrilEntity.setRazonSocial(fabricanteBarrilFormDTO.getRazonSocial());
        fabricanteBarrilEntity.setNombreComercial(fabricanteBarrilFormDTO.getNombreComercial());
        fabricanteBarrilEntity.setCuit(fabricanteBarrilFormDTO.getCuit());
        fabricanteBarrilEntity.setTelefono(fabricanteBarrilFormDTO.getTelefono());
        fabricanteBarrilEntity.setEmail(fabricanteBarrilFormDTO.getEmail());

        // 5. Persisto la entidad actualizada y devuelvo el DTO correspondiente
        return MapperFabricanteBarril.toDTO(fabricanteBarrilRepository.save(fabricanteBarrilEntity));
    }

    /**
     * Procesa la baja lógica de un fabricante de barril existente en el sistema.
     * <p>
     * En lugar de eliminar el registro, marca al fabricante con {@link Estado#BAJA} y persiste el
     * cambio. No tiene sentido dar de baja un fabricante mientras siga teniendo barriles activos
     * en el sistema, por eso se valida esa condición antes de aplicar la baja.
     * </p>
     *
     * @param id Identificador clave primaria del fabricante de barril a dar de baja.
     * @return {@link FabricanteBarrilResponseDTO} con los datos del fabricante ya marcado como dado de baja.
     * @throws RecursoNoEncontradoException Si el fabricante de barril con el ID especificado no existe o ya fue dado de baja.
     * @throws ReglaNegocioException Si el fabricante tiene al menos un barril activo asociado.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.FABRICANTE_BARRIL)
    public FabricanteBarrilResponseDTO bajaFabricanteBarril(Long id) {
        // 1. Buscamos el fabricante. Si no existe, se dispara RecursoNoEncontradoException
        FabricanteBarrilEntity fabricanteBarrilEntity = fabricanteBarrilRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el fabricante de barril con ID: " + id));

        // 2. Validar que el fabricante no tenga barriles activos asociados
        if (barrilRepository.existsByFabricanteId(id)) {
            throw new ReglaNegocioException("No se puede dar de baja el fabricante de barril porque tiene barriles activos asociados");
        }

        // 3. Ejecutamos la baja lógica: cambiamos el estado y persistimos el cambio
        fabricanteBarrilEntity.setEstado(Estado.BAJA);
        fabricanteBarrilRepository.save(fabricanteBarrilEntity);

        // 4. Retornamos el DTO del fabricante dado de baja
        return MapperFabricanteBarril.toDTO(fabricanteBarrilEntity);
    }
}
