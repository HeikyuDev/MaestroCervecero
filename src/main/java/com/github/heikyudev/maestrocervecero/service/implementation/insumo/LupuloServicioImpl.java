package com.github.heikyudev.maestrocervecero.service.implementation.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.FormatoLupulo;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LupuloEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.ILoteInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.ILupuloRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.orden_compra.IOrdenCompraRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IPlanificacionProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.proveedor.ICatalogoProveedorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.receta.IVersionRecetaRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.insumo.LupuloFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.insumo.ILupuloServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LupuloResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.insumo.MapperLupulo;
import com.github.heikyudev.maestrocervecero.util.persistencia.GuardadoUnico;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import com.github.heikyudev.maestrocervecero.util.validacion.NormalizacionTexto;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesNumericas;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class LupuloServicioImpl implements ILupuloServicio {

    /**
     * Porcentaje máximo de ácidos alfa que se admite para un lúpulo.
     */
    private static final double ALFA_ACIDOS_MAXIMO = 25.0;

    /**
     * Índice único de la base de datos que garantiza que no haya dos lúpulos activos con el mismo nombre.
     */
    private static final String INDICE_NOMBRE = "udx_insumo_nombre";

    // Inyecto los repositorios gracias a LOMBOK
    private final ILupuloRepository lupuloRepository;
    private final IVersionRecetaRepository versionRecetaRepository;
    private final ILoteInsumoRepository loteInsumoRepository;
    private final IPlanificacionProduccionRepository planificacionProduccionRepository;
    private final IOrdenCompraRepository ordenCompraRepository;
    private final ICatalogoProveedorRepository catalogoProveedorRepository;

    /**
     * Recupera una página de lúpulos activos registrados en el sistema, filtrados opcionalmente
     * por nombre (coincidencia parcial, sin distinguir mayúsculas/minúsculas) y/o formato
     * (coincidencia exacta).
     * <p>
     * Los lúpulos dados de baja son excluidos por la condición {@code estado = 'ACTIVO'}
     * aplicada en el repositorio.
     * </p>
     *
     * @param nombre Texto a buscar dentro del nombre del lúpulo, o {@code null} para no filtrar por nombre.
     * @param formato Formato exacto a filtrar, o {@code null} para no filtrar por formato.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link LupuloResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<LupuloResponseDTO> filtrarLupulos(String nombre, FormatoLupulo formato, Pageable pageable) {
        return lupuloRepository.filtrarLupulos(nombre, formato, pageable).map(MapperLupulo::toDTO);
    }

    /**
     * Busca y retorna un lúpulo específico mediante su identificador único.
     *
     * @param id Identificador clave primaria del lúpulo buscado.
     * @return Objeto {@link LupuloResponseDTO} con la información del lúpulo encontrado.
     * @throws RecursoNoEncontradoException Si no existe ningún lúpulo activo con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public LupuloResponseDTO buscarPorId(Long id) {
        // 1. Obtengo la entidad de la base de datos y devuelvo el DTO correspondiente
        return MapperLupulo.toDTO(lupuloRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lúpulo con ID: " + id)));
    }

    /**
     * Registra un nuevo lúpulo en el sistema.
     * <p>
     * Normaliza el nombre (sin espacios en los extremos ni repetidos), valida que sea obligatorio y no supere los 100 caracteres, que los ácidos alfa sean mayores a 0% y menores o iguales a 25%, que el nombre no esté
     * duplicado (case-insensitive) entre los lúpulos activos, y asigna la unidad de medida fija
     * de negocio ({@code GRAMO}) antes de persistir. Registra el evento en la auditoría.
     * </p>
     *
     * @param lupuloFormDTO Objeto DTO que contiene los datos de creación del lúpulo.
     * @return {@link LupuloResponseDTO} representativo del lúpulo guardado en la base de datos.
     * @throws ReglaNegocioException Si el nombre falta o supera los 100 caracteres, si los ácidos alfa no son un número o si no son mayores a 0% y menores o iguales a 25%.
     * @throws RecursoDuplicadoException Si el nombre provisto ya pertenece a un lúpulo activo.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.LUPULO)
    public LupuloResponseDTO altaLupulo(LupuloFormDTO lupuloFormDTO) {
        // 1. Normalizar el nombre sin modificar el DTO
        String nombre = NormalizacionTexto.nombrePropio(lupuloFormDTO.getNombre());

        // 2. Validar que el nombre sea obligatorio y no supere la longitud máxima
        validarNombre(nombre);

        // 3. Validar la regla de negocio de los alfa ácidos
        validarAlfaAcidos(lupuloFormDTO.getAa());

        // 4. Validar si el nombre ya está registrado en otro lúpulo activo (case-insensitive)
        if (lupuloRepository.existsByNombreIgnoreCase(nombre)) {
            throw nombreDuplicadoEnAlta(nombre);
        }

        // 5. Creo la entidad que se va a almacenar en la base de datos.
        //    La unidad de medida es fija por regla de negocio y la asigna el service.
        LupuloEntity lupuloEntity = LupuloEntity.builder()
                .nombre(nombre)
                .unidadDeMedida(UnidadDeMedida.GRAMO)
                .formato(lupuloFormDTO.getFormato())
                .aa(lupuloFormDTO.getAa())
                .estado(Estado.ACTIVO)
                .build();

        // 6. Guardo la entidad en la base de datos (un pedido simultáneo que viole el índice único se traduce al mismo mensaje)
        //    y devuelvo el DTO correspondiente
        return MapperLupulo.toDTO(GuardadoUnico.guardar(lupuloRepository, lupuloEntity,
                Map.of(INDICE_NOMBRE, () -> nombreDuplicadoEnAlta(nombre))));
    }

    /**
     * Actualiza la información de un lúpulo existente en la base de datos.
     * <p>
     * Normaliza y valida el nombre (obligatorio y de longitud máxima), y el porcentaje de alfa ácidos antes de consultar la base de datos, luego localiza
     * el lúpulo por su ID y verifica que el nuevo nombre no colisione con el de otro lúpulo
     * activo (case-insensitive), permitiendo conservar el propio nombre actual.
     * </p>
     *
     * @param id Identificador clave primaria del lúpulo a modificar.
     * @param lupuloFormDTO DTO con la información actualizada.
     * @return {@link LupuloResponseDTO} representativo del lúpulo con los cambios aplicados.
     * @throws ReglaNegocioException Si el nombre falta o supera los 100 caracteres, si los ácidos alfa no son un número o si no son mayores a 0% y menores o iguales a 25%.
     * @throws RecursoNoEncontradoException Si no se localiza un lúpulo activo por el ID proporcionado.
     * @throws RecursoDuplicadoException Si el nuevo nombre ya se encuentra asignado a otro lúpulo activo.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.LUPULO)
    public LupuloResponseDTO modificarLupulo(Long id, LupuloFormDTO lupuloFormDTO) {
        // 1. Normalizar el nombre sin modificar el DTO
        String nombre = NormalizacionTexto.nombrePropio(lupuloFormDTO.getNombre());

        // 2. Validar que el nombre sea obligatorio y no supere la longitud máxima antes de consultar la base de datos
        validarNombre(nombre);

        // 3. Validar la regla de negocio de los alfa ácidos antes de consultar la base de datos
        validarAlfaAcidos(lupuloFormDTO.getAa());

        // 4. Localizar el lúpulo existente. Si no existe, se dispara RecursoNoEncontradoException
        LupuloEntity lupuloEntity = lupuloRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lúpulo con ID: " + id));

        // 5. Se valida duplicación excluyendo el propio ID, de modo que conservar
        //    el nombre actual (incluso con distinto case) no falla contra el mismo registro
        if (lupuloRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw nombreDuplicadoEnModificacion(nombre);
        }

        // 6. Aplico los cambios sobre la entidad administrada por persistencia.
        //    La unidad de medida no se toca: es fija por regla de negocio (GRAMO).
        lupuloEntity.setNombre(nombre);
        lupuloEntity.setFormato(lupuloFormDTO.getFormato());
        lupuloEntity.setAa(lupuloFormDTO.getAa());

        // 7. Persisto la entidad actualizada (un pedido simultáneo que viole el índice único se traduce al mismo mensaje)
        //    y devuelvo el DTO correspondiente
        return MapperLupulo.toDTO(GuardadoUnico.guardar(lupuloRepository, lupuloEntity,
                Map.of(INDICE_NOMBRE, () -> nombreDuplicadoEnModificacion(nombre))));
    }

    /**
     * Procesa la baja lógica de un lúpulo existente en el sistema.
     * <p>
     * En lugar de eliminar el registro, marca al lúpulo con {@link Estado#BAJA} y persiste el
     * cambio. A partir de ese momento, todas las consultas del repositorio dejan de encontrarlo.
     * </p>
     *
     * @param id Identificador clave primaria del lúpulo a dar de baja.
     * @return {@link LupuloResponseDTO} con los datos del lúpulo ya marcado como dado de baja.
     * @throws RecursoNoEncontradoException Si el lúpulo con el ID especificado no existe o ya fue dado de baja.
     * @throws ReglaNegocioException Si el lúpulo forma parte de al menos una receta activa, de una planificación de producción en estado Pendiente, o de una orden de compra en estado Pendiente, si se encuentra en el catálogo de al menos un proveedor activo, o si tiene stock (al menos un lote de insumo con cantidad actual mayor a cero).
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.LUPULO)
    public LupuloResponseDTO bajaLupulo(Long id) {
        // 1. Buscamos el lúpulo. Si no existe, se dispara RecursoNoEncontradoException
        LupuloEntity lupuloEntity = lupuloRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lúpulo con ID: " + id));

        // 2. Validar que el lúpulo no forme parte de ninguna receta activa
        if (versionRecetaRepository.existsByDetalleLupuloEnRecetaActiva(id)) {
            throw new ReglaNegocioException("No se puede dar de baja el lúpulo porque forma parte de al menos una receta activa");
        }

        // 3. Validar que el lúpulo no forme parte de la receta de ninguna planificación de producción en estado Pendiente
        //    (cualquier versión de la receta, no solo la última)
        if (planificacionProduccionRepository.existsByDetalleLupuloEnPlanificacionPendiente(id)) {
            throw new ReglaNegocioException("No se puede dar de baja el lúpulo porque forma parte de una planificación de producción en estado Pendiente");
        }

        // 4. Validar que el lúpulo no se encuentre en ninguna orden de compra en estado Pendiente
        //    (cualquier versión del proveedor)
        if (ordenCompraRepository.existsByInsumoEnOrdenCompraPendiente(id)) {
            throw new ReglaNegocioException("No se puede dar de baja el lúpulo porque se encuentra en al menos una orden de compra en estado Pendiente");
        }

        // 5. Validar que el lúpulo no esté asociado al catálogo (versión vigente) de ningún proveedor activo
        if (catalogoProveedorRepository.existsByInsumoId(id)) {
            throw new ReglaNegocioException("No se puede dar de baja el lúpulo porque se encuentra asociado al catálogo de al menos un proveedor activo");
        }

        // 6. Validar que el lúpulo no tenga stock (al menos un lote de insumo con cantidad actual mayor a cero)
        if (loteInsumoRepository.existsByInsumo_IdAndCantidadActualGreaterThan(id, 0.0)) {
            throw new ReglaNegocioException("No se puede dar de baja el lúpulo porque tiene stock");
        }

        // 7. Ejecutamos la baja lógica: cambiamos el estado y persistimos el cambio
        lupuloEntity.setEstado(Estado.BAJA);
        lupuloRepository.save(lupuloEntity);

        // 8. Retornamos el DTO del lúpulo dado de baja en lugar de null
        return MapperLupulo.toDTO(lupuloEntity);
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
        return new RecursoDuplicadoException("Ya existe un lúpulo con el nombre '" + nombre + "'");
    }

    private static RecursoDuplicadoException nombreDuplicadoEnModificacion(String nombre) {
        return new RecursoDuplicadoException("El nombre '" + nombre + "' ya está en uso por otro lúpulo");
    }

    /**
     * Valida la regla de negocio de los ácidos alfa del lúpulo: deben ser mayores a 0% y menores o
     * iguales a {@value #ALFA_ACIDOS_MAXIMO}%.
     *
     * @param aa Porcentaje de ácidos alfa a validar.
     * @throws ReglaNegocioException Si los ácidos alfa son nulos, no son mayores a 0%, superan el 25% o son NaN.
     */
    private static void validarAlfaAcidos(Double aa) {
        if (aa == null) {
            throw new ReglaNegocioException("Los ácidos alfa son obligatorios");
        }
        if (aa <= 0) {
            throw new ReglaNegocioException("Los ácidos alfa deben ser mayores a 0");
        }
        if (aa > ALFA_ACIDOS_MAXIMO) {
            throw new ReglaNegocioException("Los ácidos alfa deben ser mayores a 0% y menores o iguales a 25%");
        }
        ValidacionesNumericas.validarMayorACero(aa, "El valor de los ácidos alfa");
    }
}
