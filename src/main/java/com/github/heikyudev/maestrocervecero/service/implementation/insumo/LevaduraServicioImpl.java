package com.github.heikyudev.maestrocervecero.service.implementation.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LevaduraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoLevadura;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.ILevaduraRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.insumo.LevaduraFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.insumo.ILevaduraServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LevaduraResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.insumo.MapperLevadura;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LevaduraServicioImpl implements ILevaduraServicio {

    // Inyecto el repositorio gracias a LOMBOK
    private final ILevaduraRepository levaduraRepository;

    /**
     * Recupera una página de levaduras activas registradas en el sistema, filtradas
     * opcionalmente por nombre (coincidencia parcial, sin distinguir mayúsculas/minúsculas)
     * y/o tipo (coincidencia exacta).
     * <p>
     * Las levaduras dadas de baja son excluidas por la condición {@code estado = 'ACTIVO'}
     * aplicada en el repositorio.
     * </p>
     *
     * @param nombre Texto a buscar dentro del nombre de la levadura, o {@code null} para no filtrar por nombre.
     * @param tipo Tipo de levadura exacto a filtrar, o {@code null} para no filtrar por tipo.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link LevaduraResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<LevaduraResponseDTO> filtrarLevaduras(String nombre, TipoLevadura tipo, Pageable pageable) {
        // Obtengo las entidades de la base de datos y devuelvo el DTO correspondiente
        return levaduraRepository.filtrarLevaduras(nombre, tipo, pageable).map(MapperLevadura::toDTO);
    }

    /**
     * Busca y retorna una levadura específica mediante su identificador único.
     *
     * @param id Identificador clave primaria de la levadura buscada.
     * @return Objeto {@link LevaduraResponseDTO} con la información de la levadura encontrada.
     * @throws RecursoNoEncontradoException Si no existe ninguna levadura activa con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public LevaduraResponseDTO buscarPorId(Long id) {
        // 1. Obtengo la entidad de la base de datos y devuelvo el DTO correspondiente
        return MapperLevadura.toDTO(levaduraRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La levadura no existe")));
    }

    /**
     * Registra una nueva levadura en el sistema.
     * <p>
     * Valida que la cantidad de células por gramo sea positiva (mayor a 0), que el nombre no esté
     * duplicado (case-insensitive) entre las levaduras activas, y asigna la unidad de medida fija
     * de negocio ({@code GRAMO}) antes de persistir. Registra el evento en la auditoría.
     * </p>
     *
     * @param levaduraFormDTO Objeto DTO que contiene los datos de creación de la levadura.
     * @return {@link LevaduraResponseDTO} representativo de la levadura guardada en la base de datos.
     * @throws ReglaNegocioException Si la cantidad de células por gramo no es positiva (mayor a 0).
     * @throws RecursoDuplicadoException Si el nombre provisto ya pertenece a una levadura activa.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.LEVADURA)
    public LevaduraResponseDTO altaLevadura(LevaduraFormDTO levaduraFormDTO) {
        // 1. Validar la regla de negocio de la cantidad de células por gramo
        validarCantidadCelulasPorGramo(levaduraFormDTO.getCantidadCelulasPorGramo());

        // 2. Validar si el nombre ya está registrado en otra levadura (case-insensitive)
        if (levaduraRepository.existsByNombreIgnoreCase(levaduraFormDTO.getNombre())) {
            throw new RecursoDuplicadoException("Ya existe una levadura con el nombre '" + levaduraFormDTO.getNombre() + "'");
        }

        // 3. Creo la entidad que se va a almacenar en la base de datos.
        //    La unidad de medida es fija por regla de negocio y la asigna el service.
        LevaduraEntity levaduraEntity = LevaduraEntity.builder()
                .nombre(levaduraFormDTO.getNombre())
                .unidadDeMedida(UnidadDeMedida.GRAMO)
                .tipo(levaduraFormDTO.getTipo())
                .cantidadCelulasPorGramo(levaduraFormDTO.getCantidadCelulasPorGramo())
                .estado(Estado.ACTIVO)
                .build();

        // 4. Guardo la entidad en la base de datos y devuelvo el DTO correspondiente
        return MapperLevadura.toDTO(levaduraRepository.save(levaduraEntity));
    }

    /**
     * Actualiza la información de una levadura existente en la base de datos.
     * <p>
     * Valida la cantidad de células por gramo antes de consultar la base de datos, luego localiza
     * la levadura por su ID y verifica que el nuevo nombre no colisione con el de otra levadura
     * activa (case-insensitive), permitiendo conservar el propio nombre actual.
     * </p>
     *
     * @param id Identificador clave primaria de la levadura a modificar.
     * @param levaduraFormDTO DTO con la información actualizada.
     * @return {@link LevaduraResponseDTO} representativo de la levadura con los cambios aplicados.
     * @throws ReglaNegocioException Si la cantidad de células por gramo no es positiva (mayor a 0).
     * @throws RecursoNoEncontradoException Si no se localiza una levadura activa por el ID proporcionado.
     * @throws RecursoDuplicadoException Si el nuevo nombre ya se encuentra asignado a otra levadura activa.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.LEVADURA)
    public LevaduraResponseDTO modificarLevadura(Long id, LevaduraFormDTO levaduraFormDTO) {
        // 1. Validar la regla de negocio de la cantidad de células por gramo antes de consultar la base de datos
        validarCantidadCelulasPorGramo(levaduraFormDTO.getCantidadCelulasPorGramo());

        // 2. Localizar la levadura existente. Si no existe, se dispara RecursoNoEncontradoException
        LevaduraEntity levaduraEntity = levaduraRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La levadura no existe"));

        // 3. Se valida duplicación excluyendo el propio ID, de modo que conservar
        //    el nombre actual (incluso con distinto case) no falla contra el mismo registro
        if (levaduraRepository.existsByNombreIgnoreCaseAndIdNot(levaduraFormDTO.getNombre(), id)) {
            throw new RecursoDuplicadoException("El nombre '" + levaduraFormDTO.getNombre() + "' ya está en uso por otra levadura");
        }

        // 4. Aplico los cambios sobre la entidad administrada por persistencia.
        //    La unidad de medida no se toca: es fija por regla de negocio (GRAMO).
        levaduraEntity.setNombre(levaduraFormDTO.getNombre());
        levaduraEntity.setTipo(levaduraFormDTO.getTipo());
        levaduraEntity.setCantidadCelulasPorGramo(levaduraFormDTO.getCantidadCelulasPorGramo());

        // 5. Persisto la entidad actualizada y devuelvo el DTO correspondiente
        return MapperLevadura.toDTO(levaduraRepository.save(levaduraEntity));
    }

    /**
     * Procesa la baja lógica de una levadura existente en el sistema.
     * <p>
     * En lugar de eliminar el registro, marca a la levadura con {@link Estado#BAJA} y persiste
     * el cambio. A partir de ese momento, todas las consultas del repositorio dejan de encontrarla.
     * </p>
     *
     * @param id Identificador clave primaria de la levadura a dar de baja.
     * @return {@link LevaduraResponseDTO} con los datos de la levadura ya marcada como dada de baja.
     * @throws RecursoNoEncontradoException Si la levadura con el ID especificado no existe o ya fue dada de baja.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.LEVADURA)
    public LevaduraResponseDTO bajaLevadura(Long id) {
        // 1. Buscamos la levadura. Si no existe, se dispara RecursoNoEncontradoException
        LevaduraEntity levaduraEntity = levaduraRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la levadura con ID: " + id));

        // 2. Ejecutamos la baja lógica: cambiamos el estado y persistimos el cambio
        // TODO: validar dependencias de Stock/Recetas cuando esos módulos existan
        levaduraEntity.setEstado(Estado.BAJA);
        levaduraRepository.save(levaduraEntity);

        // 3. Retornamos el DTO de la levadura dada de baja en lugar de null
        return MapperLevadura.toDTO(levaduraEntity);
    }

    /**
     * Valida la regla de negocio de la cantidad de células por gramo de la levadura.
     *
     * @param cantidadCelulasPorGramo Cantidad de células por gramo a validar.
     * @throws ReglaNegocioException Si la cantidad de células por gramo es nula o no es positiva (mayor a 0).
     */
    private static void validarCantidadCelulasPorGramo(Double cantidadCelulasPorGramo) {
        if (cantidadCelulasPorGramo == null || cantidadCelulasPorGramo <= 0) {
            throw new ReglaNegocioException("La cantidad de células por gramo debe ser mayor a 0");
        }
    }
}
