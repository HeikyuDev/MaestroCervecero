package com.github.heikyudev.maestrocervecero.service.implementation.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LevaduraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoLevadura;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.ILoteInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.ILevaduraRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.ILoteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.orden_compra.IOrdenCompraRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IPlanificacionProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.proveedor.ICatalogoProveedorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.receta.IVersionRecetaRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.insumo.LevaduraFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.insumo.ILevaduraServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LevaduraResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.insumo.MapperLevadura;
import com.github.heikyudev.maestrocervecero.util.persistencia.GuardadoUnico;
import com.github.heikyudev.maestrocervecero.util.validacion.BloqueoPorLotesVivos;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import com.github.heikyudev.maestrocervecero.util.validacion.NormalizacionTexto;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesNumericas;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LevaduraServicioImpl implements ILevaduraServicio {

    /**
     * Cantidad mínima de células viables por gramo que se admite para una levadura.
     */
    private static final double CELULAS_VIABLES_MINIMO = 6_000_000_000.0;

    /**
     * Cantidad máxima de células viables por gramo que se admite para una levadura.
     */
    private static final double CELULAS_VIABLES_MAXIMO = 20_000_000_000.0;

    /**
     * Índice único de la base de datos que garantiza que no haya dos levaduras activas con el mismo nombre.
     */
    private static final String INDICE_NOMBRE = "udx_insumo_nombre";

    // Inyecto los repositorios gracias a LOMBOK
    private final ILevaduraRepository levaduraRepository;
    private final IVersionRecetaRepository versionRecetaRepository;
    private final ILoteInsumoRepository loteInsumoRepository;
    private final ILoteRepository loteRepository;
    private final IPlanificacionProduccionRepository planificacionProduccionRepository;
    private final IOrdenCompraRepository ordenCompraRepository;
    private final ICatalogoProveedorRepository catalogoProveedorRepository;

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
        // El texto buscado se normaliza igual que el nombre guardado: sin espacios de más
        String nombreNormalizado = NormalizacionTexto.nombrePropio(nombre);
        if (nombreNormalizado != null && nombreNormalizado.isEmpty()) {
            nombreNormalizado = null;
        }
        return levaduraRepository.filtrarLevaduras(nombreNormalizado, tipo, pageable).map(MapperLevadura::toDTO);
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
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la levadura con ID: " + id)));
    }

    /**
     * Registra una nueva levadura en el sistema.
     * <p>
     * Normaliza el nombre (sin espacios en los extremos ni repetidos), valida que sea obligatorio y no supere los 100 caracteres, que la cantidad de células viables por gramo esté entre 6.000.000.000 y 20.000.000.000, que el nombre no esté
     * duplicado (case-insensitive) entre las levaduras activas, y asigna la unidad de medida fija
     * de negocio ({@code GRAMO}) antes de persistir. Registra el evento en la auditoría.
     * </p>
     *
     * @param levaduraFormDTO Objeto DTO que contiene los datos de creación de la levadura.
     * @return {@link LevaduraResponseDTO} representativo de la levadura guardada en la base de datos.
     * @throws ReglaNegocioException Si el nombre falta o supera los 100 caracteres, si la cantidad de células viables por gramo no es un número o no está entre 6.000.000.000 y 20.000.000.000.
     * @throws RecursoDuplicadoException Si el nombre provisto ya pertenece a una levadura activa.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.LEVADURA)
    public LevaduraResponseDTO altaLevadura(LevaduraFormDTO levaduraFormDTO) {
        // 1. Normalizar el nombre sin modificar el DTO
        String nombre = NormalizacionTexto.nombrePropio(levaduraFormDTO.getNombre());

        // 2. Validar que el nombre sea obligatorio y no supere la longitud máxima
        validarNombre(nombre);

        // 3. Validar la regla de negocio de la cantidad de células por gramo
        validarCantidadCelulasPorGramo(levaduraFormDTO.getCantidadCelulasPorGramo());

        // 4. Validar si el nombre ya está registrado en otra levadura activa (case-insensitive)
        if (levaduraRepository.existsByNombreIgnoreCase(nombre)) {
            throw nombreDuplicadoEnAlta(nombre);
        }

        // 5. Creo la entidad que se va a almacenar en la base de datos.
        //    La unidad de medida es fija por regla de negocio y la asigna el service.
        LevaduraEntity levaduraEntity = LevaduraEntity.builder()
                .nombre(nombre)
                .unidadDeMedida(UnidadDeMedida.GRAMO)
                .tipo(levaduraFormDTO.getTipo())
                .cantidadCelulasPorGramo(levaduraFormDTO.getCantidadCelulasPorGramo())
                .estado(Estado.ACTIVO)
                .build();

        // 6. Guardo la entidad en la base de datos (un pedido simultáneo que viole el índice único se traduce al mismo mensaje)
        //    y devuelvo el DTO correspondiente
        return MapperLevadura.toDTO(GuardadoUnico.guardar(levaduraRepository, levaduraEntity,
                Map.of(INDICE_NOMBRE, () -> nombreDuplicadoEnAlta(nombre))));
    }

    /**
     * Actualiza la información de una levadura existente en la base de datos.
     * <p>
     * Normaliza y valida el nombre (obligatorio y de longitud máxima), y la cantidad de células por gramo antes de consultar la base de datos, luego localiza
     * la levadura por su ID y verifica que el nuevo nombre no colisione con el de otra levadura
     * activa (case-insensitive), permitiendo conservar el propio nombre actual.
     * </p>
     * <p>
     * Si el tipo o la cantidad de células por gramo cambian y algún lote Pendiente o En ejecución usa la levadura
     * (en cualquier versión de la receta de su planificación), la modificación se rechaza: esos datos definen el
     * escalado de esos lotes. Esta verificación va después de localizar la levadura y de validar el nombre
     * duplicado. El resto de los campos siguen siendo editables.
     * </p>
     *
     * @param id Identificador clave primaria de la levadura a modificar.
     * @param levaduraFormDTO DTO con la información actualizada.
     * @return {@link LevaduraResponseDTO} representativo de la levadura con los cambios aplicados.
     * @throws ReglaNegocioException Si el nombre falta o supera los 100 caracteres, si la cantidad de células viables por gramo no es un número o no está entre 6.000.000.000 y 20.000.000.000.
     * @throws RecursoNoEncontradoException Si no se localiza una levadura activa por el ID proporcionado.
     * @throws RecursoDuplicadoException Si el nuevo nombre ya se encuentra asignado a otra levadura activa.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.LEVADURA)
    public LevaduraResponseDTO modificarLevadura(Long id, LevaduraFormDTO levaduraFormDTO) {
        // 1. Normalizar el nombre sin modificar el DTO
        String nombre = NormalizacionTexto.nombrePropio(levaduraFormDTO.getNombre());

        // 2. Validar que el nombre sea obligatorio y no supere la longitud máxima antes de consultar la base de datos
        validarNombre(nombre);

        // 3. Validar la regla de negocio de la cantidad de células por gramo antes de consultar la base de datos
        validarCantidadCelulasPorGramo(levaduraFormDTO.getCantidadCelulasPorGramo());

        // 4. Localizar la levadura existente. Si no existe, se dispara RecursoNoEncontradoException
        LevaduraEntity levaduraEntity = levaduraRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la levadura con ID: " + id));

        // 5. Se valida duplicación excluyendo el propio ID, de modo que conservar
        //    el nombre actual (incluso con distinto case) no falla contra el mismo registro
        if (levaduraRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw nombreDuplicadoEnModificacion(nombre);
        }

        // 6. Si el tipo o la cantidad de células por gramo cambian, ningún lote Pendiente o En ejecución puede estar usando la levadura
        boolean cambiaTipo = !Objects.equals(levaduraEntity.getTipo(), levaduraFormDTO.getTipo());
        boolean cambiaCelulas = !Objects.equals(levaduraEntity.getCantidadCelulasPorGramo(), levaduraFormDTO.getCantidadCelulasPorGramo());
        if (cambiaTipo || cambiaCelulas) {
            String campos = cambiaTipo && cambiaCelulas ? "el tipo y la cantidad de células viables por gramo de la levadura"
                    : cambiaTipo ? "el tipo de la levadura" : "la cantidad de células viables por gramo de la levadura";
            BloqueoPorLotesVivos.validar(loteRepository.buscarCodigosDeLotesVivosPorLevadura(id), campos, "la");
        }

        // 7. Aplico los cambios sobre la entidad administrada por persistencia.
        //    La unidad de medida no se toca: es fija por regla de negocio (GRAMO).
        levaduraEntity.setNombre(nombre);
        levaduraEntity.setTipo(levaduraFormDTO.getTipo());
        levaduraEntity.setCantidadCelulasPorGramo(levaduraFormDTO.getCantidadCelulasPorGramo());

        // 8. Persisto la entidad actualizada (un pedido simultáneo que viole el índice único se traduce al mismo mensaje)
        //    y devuelvo el DTO correspondiente
        return MapperLevadura.toDTO(GuardadoUnico.guardar(levaduraRepository, levaduraEntity,
                Map.of(INDICE_NOMBRE, () -> nombreDuplicadoEnModificacion(nombre))));
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
     * @throws ReglaNegocioException Si la levadura forma parte de al menos una receta activa, de una planificación de producción en estado Pendiente, o de una orden de compra en estado Pendiente, si se encuentra en el catálogo de al menos un proveedor activo, o si tiene stock (al menos un lote de insumo con cantidad actual mayor a cero).
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.LEVADURA)
    public LevaduraResponseDTO bajaLevadura(Long id) {
        // 1. Buscamos la levadura. Si no existe, se dispara RecursoNoEncontradoException
        LevaduraEntity levaduraEntity = levaduraRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la levadura con ID: " + id));

        // 2. Validar que la levadura no forme parte de ninguna receta activa
        if (versionRecetaRepository.existsByDetalleLevaduraEnRecetaActiva(id)) {
            throw new ReglaNegocioException("No se puede dar de baja la levadura porque forma parte de al menos una receta activa");
        }

        // 3. Validar que la levadura no forme parte de la receta de ninguna planificación de producción en estado Pendiente
        //    (cualquier versión de la receta, no solo la última)
        if (planificacionProduccionRepository.existsByDetalleLevaduraEnPlanificacionPendiente(id)) {
            throw new ReglaNegocioException("No se puede dar de baja la levadura porque forma parte de una planificación de producción en estado Pendiente");
        }

        // 4. Validar que la levadura no se encuentre en ninguna orden de compra en estado Pendiente
        //    (cualquier versión del proveedor)
        if (ordenCompraRepository.existsByInsumoEnOrdenCompraPendiente(id)) {
            throw new ReglaNegocioException("No se puede dar de baja la levadura porque se encuentra en al menos una orden de compra en estado Pendiente");
        }

        // 5. Validar que la levadura no esté asociada al catálogo (versión vigente) de ningún proveedor activo
        if (catalogoProveedorRepository.existsByInsumoId(id)) {
            throw new ReglaNegocioException("No se puede dar de baja la levadura porque se encuentra asociada al catálogo de al menos un proveedor activo");
        }

        // 6. Validar que la levadura no tenga stock (al menos un lote de insumo con cantidad actual mayor a cero)
        if (loteInsumoRepository.existsByInsumo_IdAndCantidadActualGreaterThan(id, 0.0)) {
            throw new ReglaNegocioException("No se puede dar de baja la levadura porque tiene stock");
        }

        // 7. Ejecutamos la baja lógica: cambiamos el estado y persistimos el cambio
        levaduraEntity.setEstado(Estado.BAJA);
        levaduraRepository.save(levaduraEntity);

        // 8. Retornamos el DTO de la levadura dada de baja en lugar de null
        return MapperLevadura.toDTO(levaduraEntity);
    }

    /**
     * Valida que el nombre normalizado sea obligatorio y no supere la longitud máxima.
     *
     * @param nombre Nombre normalizado.
     * @throws ReglaNegocioException Si el nombre está vacío o supera los 100 caracteres.
     */
    private static void validarNombre(String nombre) {
        if (nombre == null || nombre.isEmpty()) {
            throw new ReglaNegocioException("El nombre es obligatorio");
        }
        ValidacionesNumericas.validarLongitudMaxima(nombre, LimitesTecnicos.LONGITUD_MAXIMA_NOMBRE, "El nombre");
    }

    private static RecursoDuplicadoException nombreDuplicadoEnAlta(String nombre) {
        return new RecursoDuplicadoException("Ya existe una levadura con el nombre '" + nombre + "'");
    }

    private static RecursoDuplicadoException nombreDuplicadoEnModificacion(String nombre) {
        return new RecursoDuplicadoException("El nombre '" + nombre + "' ya está en uso por otra levadura");
    }

    /**
     * Valida la regla de negocio de la cantidad de células viables por gramo de la levadura: debe
     * ser mayor o igual a 6.000.000.000 y menor o igual a 20.000.000.000. El valor llega en células
     * (no en millones): la conversión desde millones, si el usuario la usa, es de la capa web.
     *
     * @param cantidadCelulasPorGramo Cantidad de células viables por gramo a validar.
     * @throws ReglaNegocioException Si la cantidad es nula, queda fuera del rango permitido o es NaN.
     */
    private static void validarCantidadCelulasPorGramo(Double cantidadCelulasPorGramo) {
        if (cantidadCelulasPorGramo == null) {
            throw new ReglaNegocioException("La cantidad de células viables por gramo es obligatoria");
        }
        if (cantidadCelulasPorGramo < CELULAS_VIABLES_MINIMO
                || cantidadCelulasPorGramo > CELULAS_VIABLES_MAXIMO) {
            throw new ReglaNegocioException("La cantidad de células viables por gramo debe estar entre 6.000 y 20.000 millones");
        }
        ValidacionesNumericas.validarMayorACero(cantidadCelulasPorGramo, "La cantidad de células viables por gramo");
    }
}
