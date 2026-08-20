package com.github.heikyudev.maestrocervecero.service.implementation.etapa_control;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.etapa_control.EtapaControlEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.persistence.repository.etapa_control.IEtapaControlRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.etapa_control.EtapaControlFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.etapa_control.IEtapaControlServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.etapa_control.EtapaControlResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.etapa_control.MapperEtapaControl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;

@Service
@RequiredArgsConstructor
public class EtapaControlServicioImpl implements IEtapaControlServicio {

    /**
     * Etapas de receta sobre las que se puede definir una etapa de control. El resto de los
     * valores de {@link TipoEtapa} (MOLIENDA y ENVASADO) no admiten planes de monitoreo.
     */
    private static final EnumSet<TipoEtapa> ETAPAS_CONTROLABLES = EnumSet.of(
            TipoEtapa.MACERACION, TipoEtapa.FERMENTACION, TipoEtapa.HERVIDO, TipoEtapa.MADURACION);

    private final IEtapaControlRepository etapaControlRepository;

    /**
     * Recupera una página de etapas de control activas registradas en el sistema.
     * <p>
     * Las etapas de control eliminadas lógicamente son excluidas automáticamente por el
     * {@code @SoftDelete} de Hibernate sobre la entidad.
     * </p>
     *
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link EtapaControlResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<EtapaControlResponseDTO> buscarTodos(Pageable pageable) {
        return etapaControlRepository.findAll(pageable).map(MapperEtapaControl::toDTO);
    }

    /**
     * Busca y retorna una etapa de control específica mediante su identificador único.
     *
     * @param id Identificador clave primaria de la etapa de control buscada.
     * @return Objeto {@link EtapaControlResponseDTO} con la información de la etapa de control encontrada.
     * @throws RecursoNoEncontradoException Si no existe ninguna etapa de control activa con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public EtapaControlResponseDTO buscarPorId(Long id) {
        return MapperEtapaControl.toDTO(etapaControlRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la etapa de control con ID: " + id)));
    }

    /**
     * Registra una nueva etapa de control en el sistema.
     * <p>
     * Valida que la etapa de receta a controlar sea una de las admitidas para monitoreo y que no
     * exista otra etapa de control activa con el mismo nombre (case-insensitive) para esa misma
     * etapa de receta. Dos etapas de control pueden compartir nombre siempre que controlen etapas
     * de receta distintas.
     * </p>
     *
     * @param etapaControlFormDTO Objeto DTO que contiene los datos de creación de la etapa de control.
     * @return {@link EtapaControlResponseDTO} representativo de la etapa de control guardada en la base de datos.
     * @throws ReglaNegocioException Si la etapa a controlar no es MACERACION, FERMENTACION, HERVIDO o MADURACION.
     * @throws RecursoDuplicadoException Si ya existe una etapa de control activa con el mismo nombre para la misma etapa a controlar.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.ETAPA_CONTROL)
    public EtapaControlResponseDTO altaEtapaControl(EtapaControlFormDTO etapaControlFormDTO) {
        // 1. Validar que la etapa a controlar sea una de las admitidas para monitoreo
        validarEtapaAControlar(etapaControlFormDTO.getEtapaAControlar());

        // 2. Validar que no exista otra etapa de control activa con el mismo nombre para la misma etapa a controlar
        if (etapaControlRepository.existsByNombreIgnoreCaseAndEtapaAControlar(
                etapaControlFormDTO.getNombre(), etapaControlFormDTO.getEtapaAControlar())) {
            throw new RecursoDuplicadoException("Ya existe una etapa de control con el nombre '" + etapaControlFormDTO.getNombre()
                    + "' para la etapa " + etapaControlFormDTO.getEtapaAControlar());
        }

        // 3. Crear la entidad de la etapa de control a partir del DTO de formulario
        EtapaControlEntity etapaControlEntity = EtapaControlEntity.builder()
                .nombre(etapaControlFormDTO.getNombre())
                .descripcion(etapaControlFormDTO.getDescripcion())
                .etapaAControlar(etapaControlFormDTO.getEtapaAControlar())
                .build();

        // 4. Guardar la entidad en la base de datos y retornar el DTO de respuesta correspondiente
        return MapperEtapaControl.toDTO(etapaControlRepository.save(etapaControlEntity));
    }

    /**
     * Modifica una etapa de control existente en el sistema.
     * <p>
     * Valida que la etapa de receta a controlar sea una de las admitidas para monitoreo y que no
     * exista otra etapa de control activa con el mismo nombre (case-insensitive) para esa misma
     * etapa de receta, excluyendo de la verificación a la propia etapa de control que se está modificando.
     * </p>
     *
     * @param id Identificador clave primaria de la etapa de control a modificar.
     * @param etapaControlFormDTO DTO que contiene los nuevos datos de la etapa de control.
     * @return {@link EtapaControlResponseDTO} representativo de la etapa de control con los cambios aplicados.
     * @throws ReglaNegocioException Si la etapa a controlar no es MACERACION, FERMENTACION, HERVIDO o MADURACION.
     * @throws RecursoNoEncontradoException Si no se localiza una etapa de control activa por el ID proporcionado.
     * @throws RecursoDuplicadoException Si otra etapa de control activa ya posee el mismo nombre para la misma etapa a controlar.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.ETAPA_CONTROL)
    public EtapaControlResponseDTO modificarEtapaControl(Long id, EtapaControlFormDTO etapaControlFormDTO) {
        // 1. Validar que la etapa a controlar sea una de las admitidas para monitoreo
        validarEtapaAControlar(etapaControlFormDTO.getEtapaAControlar());

        // 2. Validar duplicación excluyendo el propio ID, de modo que conservar el nombre y la
        //    etapa a controlar actuales no falle contra el mismo registro
        if (etapaControlRepository.existsByNombreIgnoreCaseAndEtapaAControlarAndIdNot(
                etapaControlFormDTO.getNombre(), etapaControlFormDTO.getEtapaAControlar(), id)) {
            throw new RecursoDuplicadoException("Ya existe una etapa de control con el nombre '" + etapaControlFormDTO.getNombre()
                    + "' para la etapa " + etapaControlFormDTO.getEtapaAControlar());
        }

        // 3. Localizar la etapa de control existente. Si no existe, se dispara RecursoNoEncontradoException
        EtapaControlEntity etapaControlEntity = etapaControlRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la etapa de control con ID: " + id));

        // 4. Aplicar los cambios sobre la entidad administrada por persistencia
        etapaControlEntity.setNombre(etapaControlFormDTO.getNombre());
        etapaControlEntity.setDescripcion(etapaControlFormDTO.getDescripcion());
        etapaControlEntity.setEtapaAControlar(etapaControlFormDTO.getEtapaAControlar());

        // 5. Persistir la entidad actualizada y retornar el DTO de respuesta correspondiente
        return MapperEtapaControl.toDTO(etapaControlRepository.save(etapaControlEntity));
    }

    /**
     * Procesa la baja lógica de una etapa de control existente en el sistema.
     * <p>
     * Invoca el método de eliminación del repositorio. Como {@link EtapaControlEntity} está
     * anotada con {@code @SoftDelete}, Hibernate ejecuta un UPDATE sobre el flag de borrado en
     * lugar de una eliminación física.
     * </p>
     *
     * @param id Identificador clave primaria de la etapa de control a dar de baja.
     * @return {@link EtapaControlResponseDTO} con los datos de la etapa de control procesada antes de su inactivación.
     * @throws RecursoNoEncontradoException Si la etapa de control con el ID especificado no existe o ya fue dada de baja.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.ETAPA_CONTROL)
    public EtapaControlResponseDTO bajaEtapaControl(Long id) {
        // 1. Buscar la etapa de control. Si no existe, se dispara RecursoNoEncontradoException
        EtapaControlEntity etapaControlEntity = etapaControlRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la etapa de control con ID: " + id));

        // TODO: Implementar validaciones para verificar que la etapa de control no se encuentre asociada a planes de monitoreo de recetas activas

        // 2. Ejecutar la baja. Hibernate aplicará automáticamente la anotación de Soft Delete
        etapaControlRepository.delete(etapaControlEntity);

        // 3. Retornar el DTO de la etapa de control dada de baja
        return MapperEtapaControl.toDTO(etapaControlEntity);
    }

    /**
     * Valida la regla de negocio que restringe las etapas de receta sobre las que se puede
     * definir una etapa de control.
     *
     * @param etapaAControlar Etapa de receta a validar.
     * @throws ReglaNegocioException Si la etapa es nula o distinta de MACERACION, FERMENTACION, HERVIDO o MADURACION.
     */
    private static void validarEtapaAControlar(TipoEtapa etapaAControlar) {
        if (etapaAControlar == null || !ETAPAS_CONTROLABLES.contains(etapaAControlar)) {
            throw new ReglaNegocioException("La etapa a controlar debe ser MACERACION, FERMENTACION, HERVIDO o MADURACION");
        }
    }
}
