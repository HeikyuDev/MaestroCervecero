package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FabricanteBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IFabricanteBarrilRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.BarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.barril.MapperBarril;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del servicio de Barril.
 * <p>
 * Un barril puede repetir identificador entre fabricantes distintos: la unicidad del
 * identificador de un barril es relativa a su fabricante.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class BarrilServicioImpl implements IBarrilServicio {

    private final IBarrilRepository barrilRepository;
    private final IFabricanteBarrilRepository fabricanteBarrilRepository;

    /**
     * Recupera una página de barriles activos, filtrados opcionalmente por identificador,
     * capacidad y/o estado operativo.
     * <p>
     * Los barriles dados de baja son excluidos por la condición {@code estado = 'ACTIVO'}
     * aplicada en el repositorio.
     * </p>
     *
     * @param identificador Texto a buscar dentro del identificador, o {@code null} para no filtrar por él.
     * @param capacidad Capacidad exacta a filtrar, o {@code null} para no filtrar por ella.
     * @param estadoOperativo Estado operativo exacto a filtrar, o {@code null} para no filtrar por él.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link BarrilResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<BarrilResponseDTO> filtrarBarriles(String identificador, Double capacidad, EstadoOperativoBarril estadoOperativo, Pageable pageable) {
        return barrilRepository.filtrarBarriles(identificador, capacidad, estadoOperativo, pageable).map(MapperBarril::toDTO);
    }

    /**
     * Busca y retorna un barril específico mediante su identificador único.
     *
     * @param id Identificador clave primaria del barril buscado.
     * @return Objeto {@link BarrilResponseDTO} con la información del barril encontrado.
     * @throws RecursoNoEncontradoException Si no existe ningún barril activo con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public BarrilResponseDTO buscarPorId(Long id) {
        return MapperBarril.toDTO(barrilRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el barril con ID: " + id)));
    }

    /**
     * Registra un nuevo barril en el sistema.
     * <p>
     * Valida que la capacidad sea mayor a 0, que el fabricante de barril indicado exista y esté
     * activo, y que ningún otro barril activo de ese mismo fabricante tenga ya el identificador
     * indicado (case-insensitive).
     * </p>
     *
     * @param barrilFormDTO Objeto DTO que contiene los datos de creación del barril.
     * @return {@link BarrilResponseDTO} representativo del barril guardado en la base de datos.
     * @throws ReglaNegocioException Si la capacidad es nula o menor o igual a 0.
     * @throws RecursoNoEncontradoException Si no existe un fabricante de barril activo con el ID indicado.
     * @throws RecursoDuplicadoException Si otro barril activo del mismo fabricante ya tiene ese identificador.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.BARRIL)
    public BarrilResponseDTO altaBarril(BarrilFormDTO barrilFormDTO) {
        // 1. Validar que la capacidad sea mayor a 0
        validarCapacidad(barrilFormDTO.getCapacidad());

        // 2. Validar que el fabricante de barril indicado exista y esté activo
        FabricanteBarrilEntity fabricanteBarrilEntity = fabricanteBarrilRepository.findById(barrilFormDTO.getIdFabricanteBarril())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el fabricante de barril con ID: " + barrilFormDTO.getIdFabricanteBarril()));

        // 3. Validar que no haya otro barril activo del mismo fabricante con el mismo identificador
        if (barrilRepository.existsByIdentificadorIgnoreCaseAndFabricanteId(barrilFormDTO.getIdentificador(), barrilFormDTO.getIdFabricanteBarril())) {
            throw new RecursoDuplicadoException("Ya existe un barril activo con el identificador '" + barrilFormDTO.getIdentificador() + "' para el fabricante seleccionado");
        }

        // 4. Creo la entidad que se va a almacenar en la base de datos
        BarrilEntity barrilEntity = BarrilEntity.builder()
                .identificador(barrilFormDTO.getIdentificador())
                .capacidad(barrilFormDTO.getCapacidad())
                .contenidoActual(0.0)
                .estadoOperativo(EstadoOperativoBarril.DISPONIBLE)
                .usosMaximosAntesMantenimiento(barrilFormDTO.getUsosMaximosAntesMantenimiento())
                .estado(Estado.ACTIVO)
                .fabricante(fabricanteBarrilEntity)
                .build();

        // 5. Guardo la entidad en la base de datos y devuelvo el DTO correspondiente
        return MapperBarril.toDTO(barrilRepository.save(barrilEntity));
    }

    /**
     * Actualiza la información de un barril existente en la base de datos.
     * <p>
     * Localiza el barril por su ID, valida que la capacidad sea mayor a 0, que el fabricante de
     * barril indicado exista y esté activo (el fabricante puede reasignarse al modificar), y que
     * ningún otro barril activo de ese fabricante tenga ya el identificador indicado, permitiendo
     * conservar el propio identificador actual.
     * </p>
     *
     * @param id Identificador clave primaria del barril a modificar.
     * @param barrilFormDTO DTO con la información actualizada.
     * @return {@link BarrilResponseDTO} representativo del barril con los cambios aplicados.
     * @throws RecursoNoEncontradoException Si no se localiza un barril activo por el ID proporcionado, o si no existe un fabricante de barril activo con el ID indicado.
     * @throws ReglaNegocioException Si la capacidad es nula o menor o igual a 0.
     * @throws RecursoDuplicadoException Si otro barril activo del mismo fabricante ya tiene ese identificador.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.BARRIL)
    public BarrilResponseDTO modificarBarril(Long id, BarrilFormDTO barrilFormDTO) {
        // 1. Localizar el barril existente. Si no existe, se dispara RecursoNoEncontradoException
        BarrilEntity barrilEntity = barrilRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el barril con ID: " + id));

        // 2. Validar que la capacidad sea mayor a 0
        validarCapacidad(barrilFormDTO.getCapacidad());

        // 3. Validar que el fabricante de barril indicado exista y esté activo
        FabricanteBarrilEntity fabricanteBarrilEntity = fabricanteBarrilRepository.findById(barrilFormDTO.getIdFabricanteBarril())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el fabricante de barril con ID: " + barrilFormDTO.getIdFabricanteBarril()));

        // 4. Validar duplicación excluyendo el propio barril, de modo que conservar el
        //    identificador actual no falle contra el mismo registro
        if (barrilRepository.existsByIdentificadorIgnoreCaseAndFabricanteIdAndIdNot(barrilFormDTO.getIdentificador(), barrilFormDTO.getIdFabricanteBarril(), id)) {
            throw new RecursoDuplicadoException("Ya existe otro barril activo con el identificador '" + barrilFormDTO.getIdentificador() + "' para el fabricante seleccionado");
        }

        // 5. Aplico los cambios sobre la entidad administrada por persistencia
        barrilEntity.setIdentificador(barrilFormDTO.getIdentificador());
        barrilEntity.setCapacidad(barrilFormDTO.getCapacidad());
        barrilEntity.setUsosMaximosAntesMantenimiento(barrilFormDTO.getUsosMaximosAntesMantenimiento());
        barrilEntity.setFabricante(fabricanteBarrilEntity);

        // 6. Persisto la entidad actualizada y devuelvo el DTO correspondiente
        return MapperBarril.toDTO(barrilRepository.save(barrilEntity));
    }

    /**
     * Procesa la baja lógica de un barril existente en el sistema.
     * <p>
     * En lugar de eliminar el registro, marca al barril con {@link Estado#BAJA} y persiste el
     * cambio. No tiene sentido dar de baja un barril que todavía contiene cerveza o que está
     * despachado en lo de un cliente, por eso se valida esa condición antes de aplicar la baja.
     * </p>
     *
     * @param id Identificador clave primaria del barril a dar de baja.
     * @return {@link BarrilResponseDTO} con los datos del barril ya marcado como dado de baja.
     * @throws RecursoNoEncontradoException Si el barril con el ID especificado no existe o ya fue dado de baja.
     * @throws ReglaNegocioException Si el barril se encuentra en estado operativo CON_CERVEZA o DESPACHADO.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.BARRIL)
    public BarrilResponseDTO bajaBarril(Long id) {
        // 1. Buscamos el barril. Si no existe, se dispara RecursoNoEncontradoException
        BarrilEntity barrilEntity = barrilRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el barril con ID: " + id));

        // 2. Validar que el barril no se encuentre CON_CERVEZA ni DESPACHADO
        if (barrilEntity.getEstadoOperativo() == EstadoOperativoBarril.CON_CERVEZA
                || barrilEntity.getEstadoOperativo() == EstadoOperativoBarril.DESPACHADO) {
            throw new ReglaNegocioException("No se puede dar de baja el barril porque se encuentra en estado operativo CON_CERVEZA o DESPACHADO.");
        }

        // 3. Ejecutamos la baja lógica: cambiamos el estado y persistimos el cambio
        barrilEntity.setEstado(Estado.BAJA);

        // 4. Retornamos el DTO del barril dado de baja
        return MapperBarril.toDTO(barrilRepository.save(barrilEntity));
    }

    /**
     * Valida que la capacidad sea mayor a 0.
     *
     * @param capacidad Capacidad a validar.
     * @throws ReglaNegocioException Si la capacidad es nula o menor o igual a 0.
     */
    private static void validarCapacidad(Double capacidad) {
        if (capacidad == null) {
            throw new ReglaNegocioException("La capacidad es obligatoria.");
        }
        if (capacidad <= 0) {
            throw new ReglaNegocioException("La capacidad debe ser mayor a 0.");
        }
    }
}
