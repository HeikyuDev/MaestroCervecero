package com.github.heikyudev.maestrocervecero.service.implementation.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoMalta;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.ILoteInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.IMaltaRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.orden_compra.IOrdenCompraRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IPlanificacionProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.proveedor.ICatalogoProveedorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.receta.IVersionRecetaRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.insumo.MaltaFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.insumo.IMaltaServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.MaltaResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.insumo.MapperMalta;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesNumericas;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MaltaServicioImpl implements IMaltaServicio {

    /**
     * Extracto potencial máximo (en porcentaje) que se admite para una malta.
     */
    private static final int POTENCIAL_EXTRACTO_MAXIMO = 90;

    // Inyecto los repositorios gracias a LOMBOK
    private final IMaltaRepository maltaRepository;
    private final IVersionRecetaRepository versionRecetaRepository;
    private final ILoteInsumoRepository loteInsumoRepository;
    private final IPlanificacionProduccionRepository planificacionProduccionRepository;
    private final IOrdenCompraRepository ordenCompraRepository;
    private final ICatalogoProveedorRepository catalogoProveedorRepository;

    /**
     * Recupera una página de maltas activas registradas en el sistema, filtradas opcionalmente
     * por nombre (coincidencia parcial, sin distinguir mayúsculas/minúsculas) y/o tipo
     * (coincidencia exacta).
     * <p>
     * Las maltas dadas de baja son excluidas por la condición {@code estado = 'ACTIVO'}
     * aplicada en el repositorio.
     * </p>
     *
     * @param nombre Texto a buscar dentro del nombre de la malta, o {@code null} para no filtrar por nombre.
     * @param tipo Tipo de malta exacto a filtrar, o {@code null} para no filtrar por tipo.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link MaltaResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<MaltaResponseDTO> filtrarMaltas(String nombre, TipoMalta tipo, Pageable pageable) {
        // Obtengo las entidades de la base de datos y devuelvo el DTO correspondiente
        return maltaRepository.filtrarMaltas(nombre, tipo, pageable).map(MapperMalta::toDTO);
    }

    /**
     * Busca y retorna una malta específica mediante su identificador único.
     *
     * @param id Identificador clave primaria de la malta buscada.
     * @return Objeto {@link MaltaResponseDTO} con la información de la malta encontrada.
     * @throws RecursoNoEncontradoException Si no existe ninguna malta activa con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public MaltaResponseDTO buscarPorId(Long id) {
        // 1. Obtengo la entidad de la base de datos y devuelvo el DTO correspondiente
        return MapperMalta.toDTO(maltaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la malta con ID: " + id)));
    }

    /**
     * Registra una nueva malta en el sistema.
     * <p>
     * Valida que el nombre no supere los 100 caracteres, que el extracto potencial sea mayor a 0% y menor o igual a 90%, que el nombre no esté
     * duplicado (case-insensitive) entre las maltas activas, y asigna la unidad de medida fija
     * de negocio ({@code KILOGRAMO}) antes de persistir. Registra el evento en la auditoría.
     * </p>
     *
     * @param maltaFormDTO Objeto DTO que contiene los datos de creación de la malta.
     * @return {@link MaltaResponseDTO} representativo de la malta guardada en la base de datos.
     * @throws ReglaNegocioException Si el nombre supera los 100 caracteres o si el extracto potencial no es mayor a 0% y menor o igual a 90%.
     * @throws RecursoDuplicadoException Si el nombre provisto ya pertenece a una malta activa.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.MALTA)
    public MaltaResponseDTO altaMalta(MaltaFormDTO maltaFormDTO) {
        // 1. Validar la longitud máxima del nombre
        ValidacionesNumericas.validarLongitudMaxima(maltaFormDTO.getNombre(), LimitesTecnicos.LONGITUD_MAXIMA_NOMBRE, "El nombre");

        // 2. Validar la regla de negocio del extracto potencial
        validarPotencialExtracto(maltaFormDTO.getPotencialExtracto());

        // 3. Validar si el nombre ya está registrado en otra malta (case-insensitive)
        if (maltaRepository.existsByNombreIgnoreCase(maltaFormDTO.getNombre())) {
            throw new RecursoDuplicadoException("Ya existe una malta con el nombre '" + maltaFormDTO.getNombre() + "'");
        }

        // 4. Creo la entidad que se va a almacenar en la base de datos.
        //    La unidad de medida es fija por regla de negocio y la asigna el service.
        MaltaEntity maltaEntity = MaltaEntity.builder()
                .nombre(maltaFormDTO.getNombre())
                .unidadDeMedida(UnidadDeMedida.KILOGRAMO)
                .tipo(maltaFormDTO.getTipo())
                .potencialExtracto(maltaFormDTO.getPotencialExtracto())
                .estado(Estado.ACTIVO)
                .build();

        // 5. Guardo la entidad en la base de datos y devuelvo el DTO correspondiente
        return MapperMalta.toDTO(maltaRepository.save(maltaEntity));
    }

    /**
     * Actualiza la información de una malta existente en la base de datos.
     * <p>
     * Valida la longitud del nombre y el extracto potencial antes de consultar la base de datos, luego localiza la malta
     * por su ID y verifica que el nuevo nombre no colisione con el de otra malta activa
     * (case-insensitive), permitiendo conservar el propio nombre actual.
     * </p>
     *
     * @param id Identificador clave primaria de la malta a modificar.
     * @param maltaFormDTO DTO con la información actualizada.
     * @return {@link MaltaResponseDTO} representativo de la malta con los cambios aplicados.
     * @throws ReglaNegocioException Si el nombre supera los 100 caracteres o si el extracto potencial no es mayor a 0% y menor o igual a 90%.
     * @throws RecursoNoEncontradoException Si no se localiza una malta activa por el ID proporcionado.
     * @throws RecursoDuplicadoException Si el nuevo nombre ya se encuentra asignado a otra malta activa.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.MALTA)
    public MaltaResponseDTO modificarMalta(Long id, MaltaFormDTO maltaFormDTO) {
        // 1. Validar la longitud máxima del nombre antes de consultar la base de datos
        ValidacionesNumericas.validarLongitudMaxima(maltaFormDTO.getNombre(), LimitesTecnicos.LONGITUD_MAXIMA_NOMBRE, "El nombre");

        // 2. Validar la regla de negocio del extracto potencial antes de consultar la base de datos
        validarPotencialExtracto(maltaFormDTO.getPotencialExtracto());

        // 3. Localizar la malta existente. Si no existe, se dispara RecursoNoEncontradoException
        MaltaEntity maltaEntity = maltaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la malta con ID: " + id));

        // 4. Se valida duplicación excluyendo el propio ID, de modo que conservar
        //    el nombre actual (incluso con distinto case) no falla contra el mismo registro
        if (maltaRepository.existsByNombreIgnoreCaseAndIdNot(maltaFormDTO.getNombre(), id)) {
            throw new RecursoDuplicadoException("El nombre '" + maltaFormDTO.getNombre() + "' ya está en uso por otra malta");
        }

        // 5. Aplico los cambios sobre la entidad administrada por persistencia.
        //    La unidad de medida no se toca: es fija por regla de negocio (KILOGRAMO).
        maltaEntity.setNombre(maltaFormDTO.getNombre());
        maltaEntity.setTipo(maltaFormDTO.getTipo());
        maltaEntity.setPotencialExtracto(maltaFormDTO.getPotencialExtracto());

        // 6. Persisto la entidad actualizada y devuelvo el DTO correspondiente
        return MapperMalta.toDTO(maltaRepository.save(maltaEntity));
    }

    /**
     * Procesa la baja lógica de una malta existente en el sistema.
     * <p>
     * En lugar de eliminar el registro, marca a la malta con {@link Estado#BAJA} y persiste el
     * cambio. A partir de ese momento, todas las consultas del repositorio dejan de encontrarla.
     * </p>
     *
     * @param id Identificador clave primaria de la malta a dar de baja.
     * @return {@link MaltaResponseDTO} con los datos de la malta ya marcada como dada de baja.
     * @throws RecursoNoEncontradoException Si la malta con el ID especificado no existe o ya fue dada de baja.
     * @throws ReglaNegocioException Si la malta forma parte de al menos una receta activa, de una planificación de producción en estado Pendiente, o de una orden de compra en estado Pendiente, si se encuentra en el catálogo de al menos un proveedor activo, o si tiene stock (al menos un lote de insumo con cantidad actual mayor a cero).
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.MALTA)
    public MaltaResponseDTO bajaMalta(Long id) {
        // 1. Buscamos la malta. Si no existe, se dispara RecursoNoEncontradoException
        MaltaEntity maltaEntity = maltaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la malta con ID: " + id));

        // 2. Validar que la malta no forme parte de ninguna receta activa
        if (versionRecetaRepository.existsByDetalleMaltaEnRecetaActiva(id)) {
            throw new ReglaNegocioException("No se puede dar de baja la malta porque forma parte de al menos una receta activa");
        }

        // 3. Validar que la malta no forme parte de la receta de ninguna planificación de producción en estado Pendiente
        //    (cualquier versión de la receta, no solo la última)
        if (planificacionProduccionRepository.existsByDetalleMaltaEnPlanificacionPendiente(id)) {
            throw new ReglaNegocioException("No se puede dar de baja la malta porque forma parte de una planificación de producción en estado Pendiente");
        }

        // 4. Validar que la malta no se encuentre en ninguna orden de compra en estado Pendiente
        //    (cualquier versión del proveedor)
        if (ordenCompraRepository.existsByInsumoEnOrdenCompraPendiente(id)) {
            throw new ReglaNegocioException("No se puede dar de baja la malta porque se encuentra en al menos una orden de compra en estado Pendiente");
        }

        // 5. Validar que la malta no esté asociada al catálogo (versión vigente) de ningún proveedor activo
        if (catalogoProveedorRepository.existsByInsumoId(id)) {
            throw new ReglaNegocioException("No se puede dar de baja la malta porque se encuentra asociada al catálogo de al menos un proveedor activo");
        }

        // 6. Validar que la malta no tenga stock (al menos un lote de insumo con cantidad actual mayor a cero)
        if (loteInsumoRepository.existsByInsumo_IdAndCantidadActualGreaterThan(id, 0.0)) {
            throw new ReglaNegocioException("No se puede dar de baja la malta porque tiene stock");
        }

        // 7. Ejecutamos la baja lógica: cambiamos el estado y persistimos el cambio
        maltaEntity.setEstado(Estado.BAJA);
        maltaRepository.save(maltaEntity);

        // 8. Retornamos el DTO de la malta dada de baja en lugar de null
        return MapperMalta.toDTO(maltaEntity);
    }

    /**
     * Valida la regla de negocio del extracto potencial de la malta: debe ser mayor a 0% y menor o
     * igual a {@value #POTENCIAL_EXTRACTO_MAXIMO}%.
     *
     * @param potencialExtracto Extracto potencial en porcentaje a validar.
     * @throws ReglaNegocioException Si el extracto potencial es nulo, no es mayor a 0% o supera el 90%.
     */
    private static void validarPotencialExtracto(Integer potencialExtracto) {
        if (potencialExtracto == null) {
            throw new ReglaNegocioException("El extracto potencial es obligatorio");
        }
        if (potencialExtracto <= 0) {
            throw new ReglaNegocioException("El extracto potencial debe ser mayor a 0");
        }
        if (potencialExtracto > POTENCIAL_EXTRACTO_MAXIMO) {
            throw new ReglaNegocioException("El extracto potencial debe ser mayor a 0% y menor o igual a "
                    + POTENCIAL_EXTRACTO_MAXIMO + "%");
        }
    }
}
