package com.github.heikyudev.maestrocervecero.service.implementation.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.PresentacionComercialEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.orden_compra.IOrdenCompraRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.proveedor.ICatalogoProveedorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.proveedor.IPresentacionComercialRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor.PresentacionComercialFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.proveedor.IPresentacionComercialServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.PresentacionComercialResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.proveedor.MapperPresentacionComercial;
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
public class PresentacionComercialServicioImpl implements IPresentacionComercialServicio {

    /**
     * Índice único de la base de datos que garantiza que no haya dos presentaciones comerciales activas con el mismo nombre.
     */
    private static final String INDICE_NOMBRE = "udx_presentacion_comercial_nombre";

    // Inyecto los repositorios gracias a LOMBOK
    private final IPresentacionComercialRepository presentacionComercialRepository;
    private final ICatalogoProveedorRepository catalogoProveedorRepository;
    private final IOrdenCompraRepository ordenCompraRepository;

    /**
     * Filtra las presentaciones comerciales activas, opcionalmente por nombre, cantidad y/o
     * unidad de medida.
     * <p>
     * Las presentaciones comerciales dadas de baja son excluidas por la condición
     * {@code estado = 'ACTIVO'} aplicada en el repositorio.
     * </p>
     *
     * @param nombre Texto a buscar dentro del nombre de la presentación comercial, o {@code null} para no filtrar por nombre.
     * @param cantidad Cantidad exacta a filtrar, o {@code null} para no filtrar por cantidad.
     * @param unidadDeMedida Unidad de medida exacta a filtrar, o {@code null} para no filtrar por ella.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link PresentacionComercialResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<PresentacionComercialResponseDTO> filtrarPresentacionesComerciales(String nombre, Double cantidad, UnidadDeMedida unidadDeMedida, Pageable pageable) {
        // El texto buscado se normaliza igual que el nombre guardado: sin espacios de más
        String nombreNormalizado = NormalizacionTexto.nombrePropio(nombre);
        if (nombreNormalizado != null && nombreNormalizado.isEmpty()) {
            nombreNormalizado = null;
        }
        return presentacionComercialRepository.filtrarPresentacionesComerciales(nombreNormalizado, cantidad, unidadDeMedida, pageable)
                .map(MapperPresentacionComercial::toDTO);
    }

    /**
     * Busca y retorna una presentación comercial específica mediante su identificador único.
     *
     * @param id Identificador clave primaria de la presentación comercial buscada.
     * @return Objeto {@link PresentacionComercialResponseDTO} con la información de la presentación comercial encontrada.
     * @throws RecursoNoEncontradoException Si no existe ninguna presentación comercial activa con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public PresentacionComercialResponseDTO buscarPorId(Long id) {
        // 1. Obtengo la entidad de la base de datos y devuelvo el DTO correspondiente
        return MapperPresentacionComercial.toDTO(presentacionComercialRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La presentación comercial no existe")));
    }

    /**
     * Registra una nueva presentación comercial en el sistema.
     * <p>
     * Normaliza el nombre (sin espacios en los extremos ni repetidos), valida que sea obligatorio y no supere los 100 caracteres,
     * que la cantidad sea mayor a cero y que el nombre no esté duplicado
     * (case-insensitive) entre las presentaciones comerciales activas. Registra el evento en
     * la auditoría.
     * </p>
     *
     * @param presentacionComercialFormDTO Objeto DTO que contiene los datos de creación de la presentación comercial.
     * @return {@link PresentacionComercialResponseDTO} representativo de la presentación comercial guardada en la base de datos.
     * @throws ReglaNegocioException Si el nombre falta o supera los 100 caracteres, o si la cantidad es nula o menor o igual a cero.
     * @throws RecursoDuplicadoException Si el nombre provisto ya pertenece a una presentación comercial activa.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.PRESENTACION_COMERCIAL)
    public PresentacionComercialResponseDTO altaPresentacionComercial(PresentacionComercialFormDTO presentacionComercialFormDTO) {
        // 1. Normalizar el nombre sin modificar el DTO
        String nombre = NormalizacionTexto.nombrePropio(presentacionComercialFormDTO.getNombre());

        // 2. Validar que el nombre sea obligatorio y no supere la longitud máxima
        validarNombre(nombre);

        // 3. Validar la regla de negocio de la cantidad
        validarCantidad(presentacionComercialFormDTO.getCantidad());

        // 4. Validar si el nombre ya está registrado en otra presentación comercial (case-insensitive)
        if (presentacionComercialRepository.existsByNombreIgnoreCase(nombre)) {
            throw nombreDuplicadoEnAlta(nombre);
        }

        // 5. Creo la entidad que se va a almacenar en la base de datos
        PresentacionComercialEntity presentacionComercialEntity = PresentacionComercialEntity.builder()
                .nombre(nombre)
                .cantidad(presentacionComercialFormDTO.getCantidad())
                .unidadDeMedida(presentacionComercialFormDTO.getUnidadDeMedida())
                .estado(Estado.ACTIVO)
                .build();

        // 6. Guardo la entidad en la base de datos (un pedido simultáneo que viole el índice único se traduce al mismo mensaje)
        //    y devuelvo el DTO correspondiente
        return MapperPresentacionComercial.toDTO(GuardadoUnico.guardar(presentacionComercialRepository, presentacionComercialEntity,
                Map.of(INDICE_NOMBRE, () -> nombreDuplicadoEnAlta(nombre))));
    }

    /**
     * Actualiza la información de una presentación comercial existente en la base de datos.
     * <p>
     * Normaliza y valida el nombre (obligatorio y de longitud máxima) y la cantidad antes de consultar la base de datos, luego localiza la presentación
     * comercial por su ID y verifica que el nuevo nombre no colisione con el de otra
     * presentación comercial activa (case-insensitive), permitiendo conservar el propio
     * nombre actual.
     * </p>
     *
     * @param id Identificador clave primaria de la presentación comercial a modificar.
     * @param presentacionComercialFormDTO DTO con la información actualizada.
     * @return {@link PresentacionComercialResponseDTO} representativo de la presentación comercial con los cambios aplicados.
     * @throws ReglaNegocioException Si el nombre falta o supera los 100 caracteres, o si la cantidad es nula o menor o igual a cero.
     * @throws RecursoNoEncontradoException Si no se localiza una presentación comercial activa por el ID proporcionado.
     * @throws RecursoDuplicadoException Si el nuevo nombre ya se encuentra asignado a otra presentación comercial activa.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.PRESENTACION_COMERCIAL)
    public PresentacionComercialResponseDTO modificarPresentacionComercial(Long id, PresentacionComercialFormDTO presentacionComercialFormDTO) {
        // 1. Normalizar el nombre sin modificar el DTO
        String nombre = NormalizacionTexto.nombrePropio(presentacionComercialFormDTO.getNombre());

        // 2. Validar que el nombre sea obligatorio y no supere la longitud máxima antes de consultar la base de datos
        validarNombre(nombre);

        // 3. Validar la regla de negocio de la cantidad antes de consultar la base de datos
        validarCantidad(presentacionComercialFormDTO.getCantidad());

        // 4. Localizar la presentación comercial existente. Si no existe, se dispara RecursoNoEncontradoException
        PresentacionComercialEntity presentacionComercialEntity = presentacionComercialRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La presentación comercial no existe"));

        // 5. Se valida duplicación excluyendo el propio ID, de modo que conservar
        //    el nombre actual (incluso con distinto case) no falla contra el mismo registro
        if (presentacionComercialRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw nombreDuplicadoEnModificacion(nombre);
        }

        // 6. Aplico los cambios sobre la entidad administrada por persistencia
        presentacionComercialEntity.setNombre(nombre);
        presentacionComercialEntity.setCantidad(presentacionComercialFormDTO.getCantidad());
        presentacionComercialEntity.setUnidadDeMedida(presentacionComercialFormDTO.getUnidadDeMedida());

        // 7. Persisto la entidad actualizada (un pedido simultáneo que viole el índice único se traduce al mismo mensaje)
        //    y devuelvo el DTO correspondiente
        return MapperPresentacionComercial.toDTO(GuardadoUnico.guardar(presentacionComercialRepository, presentacionComercialEntity,
                Map.of(INDICE_NOMBRE, () -> nombreDuplicadoEnModificacion(nombre))));
    }

    /**
     * Procesa la baja lógica de una presentación comercial existente en el sistema.
     * <p>
     * En lugar de eliminar el registro, marca a la presentación comercial con
     * {@link Estado#BAJA} y persiste el cambio. A partir de ese momento, todas las consultas
     * del repositorio dejan de encontrarla.
     * </p>
     *
     * @param id Identificador clave primaria de la presentación comercial a dar de baja.
     * @return {@link PresentacionComercialResponseDTO} con los datos de la presentación comercial ya marcada como dada de baja.
     * @throws RecursoNoEncontradoException Si la presentación comercial con el ID especificado no existe o ya fue dada de baja.
     * @throws ReglaNegocioException Si la presentación comercial se encuentra en al menos una orden de compra en estado Pendiente, o asociada al catálogo de al menos un proveedor activo.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.PRESENTACION_COMERCIAL)
    public PresentacionComercialResponseDTO bajaPresentacionComercial(Long id) {
        // 1. Buscamos la presentación comercial. Si no existe, se dispara RecursoNoEncontradoException
        PresentacionComercialEntity presentacionComercialEntity = presentacionComercialRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la presentación comercial con ID: " + id));

        // 2. Validar que la presentación comercial no se encuentre en ninguna orden de compra en estado Pendiente
        //    (cualquier versión del proveedor)
        if (ordenCompraRepository.existsByPresentacionComercialEnOrdenCompraPendiente(id)) {
            throw new ReglaNegocioException("No se puede dar de baja la presentación comercial porque se encuentra en al menos una orden de compra en estado Pendiente");
        }

        // 3. Validar que la presentación comercial no esté asociada a ningún catálogo de proveedor activo
        if (catalogoProveedorRepository.existsByPresentacionComercialId(id)) {
            throw new ReglaNegocioException("No se puede dar de baja la presentación comercial porque se encuentra asociada al catálogo de al menos un proveedor activo");
        }

        // 4. Ejecutamos la baja lógica: cambiamos el estado y persistimos el cambio
        presentacionComercialEntity.setEstado(Estado.BAJA);
        presentacionComercialRepository.save(presentacionComercialEntity);

        // 5. Retornamos el DTO de la presentación comercial dada de baja
        return MapperPresentacionComercial.toDTO(presentacionComercialEntity);
    }

    /**
     * Valida la regla de negocio de la cantidad de la presentación comercial.
     *
     * @param cantidad Cantidad a validar.
     * @throws ReglaNegocioException Si la cantidad es nula o menor o igual a cero.
     */
    private static void validarCantidad(Double cantidad) {
        if (cantidad == null || cantidad <= 0) {
            throw new ReglaNegocioException("La cantidad debe ser mayor a cero");
        }
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
        return new RecursoDuplicadoException("Ya existe una presentación comercial con el nombre '" + nombre + "'");
    }

    private static RecursoDuplicadoException nombreDuplicadoEnModificacion(String nombre) {
        return new RecursoDuplicadoException("El nombre '" + nombre + "' ya está en uso por otra presentación comercial");
    }
}
