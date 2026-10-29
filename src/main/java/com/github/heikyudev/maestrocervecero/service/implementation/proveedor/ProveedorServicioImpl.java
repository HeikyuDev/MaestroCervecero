package com.github.heikyudev.maestrocervecero.service.implementation.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.CatalogoProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.PresentacionComercialEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.ProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.VersionProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion.LocalidadEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.IInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.orden_compra.IOrdenCompraRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.proveedor.IPresentacionComercialRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.proveedor.IProveedorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.proveedor.IVersionProveedorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ubicacion.ILocalidadRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor.CatalogoProveedorFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor.ProveedorFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor.VersionProveedorFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.proveedor.IProveedorServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.ProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.proveedor.MapperProveedor;
import com.github.heikyudev.maestrocervecero.util.method.MetodosVersionado;
import com.github.heikyudev.maestrocervecero.util.persistencia.GuardadoUnico;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import com.github.heikyudev.maestrocervecero.util.validacion.NormalizacionTexto;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesNumericas;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesTexto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProveedorServicioImpl implements IProveedorServicio {

    /**
     * Índice único de la base de datos que garantiza que no haya dos proveedores activos con el mismo CUIT.
     */
    private static final String INDICE_CUIT = "udx_version_proveedor_cuit";

    /**
     * Índice único de la base de datos que garantiza que no haya dos proveedores activos con el mismo correo electrónico.
     */
    private static final String INDICE_EMAIL = "udx_version_proveedor_email";

    /**
     * Índice único de la base de datos que garantiza que un proveedor tenga una sola versión vigente.
     */
    private static final String INDICE_ULTIMA_VERSION = "udx_version_proveedor_ultima_version";

    /**
     * Índice único de la base de datos que garantiza que un catálogo no repita el mismo insumo con la misma presentación comercial.
     */
    private static final String INDICE_ITEM_CATALOGO = "udx_catalogo_proveedor_item";

    /**
     * Campos que indica {@link RecursoDuplicadoException#getCampo()} ante un duplicado del CUIT o del correo electrónico:
     * son los nombres de los campos de {@link VersionProveedorFormDTO}.
     */
    private static final String CAMPO_CUIT = "cuit";
    private static final String CAMPO_EMAIL = "email";

    /**
     * Datos de texto de la versión ya normalizados, listos para validar, consultar la unicidad y guardar.
     */
    private record DatosVersion(String razonSocial, String nombreComercial, String cuit, String telefono,
                                String email, String direccion) {

        DatosVersion conCuit(String cuitFormateado) {
            return new DatosVersion(razonSocial, nombreComercial, cuitFormateado, telefono, email, direccion);
        }
    }

    private final IProveedorRepository proveedorRepository;
    private final IVersionProveedorRepository versionProveedorRepository;
    private final ILocalidadRepository localidadRepository;
    private final IInsumoRepository insumoRepository;
    private final IPresentacionComercialRepository presentacionComercialRepository;
    private final IOrdenCompraRepository ordenCompraRepository;

    /**
     * Filtra los proveedores activos, opcionalmente por razón social, nombre comercial, CUIT y/o
     * localidad de su versión vigente.
     * <p>
     * Los proveedores dados de baja son excluidos por la condición {@code estado = 'ACTIVO'}
     * aplicada en el repositorio.
     * </p>
     *
     * @param razonSocial Texto a buscar dentro de la razón social, o {@code null} para no filtrar por ella.
     * @param nombreComercial Texto a buscar dentro del nombre comercial, o {@code null} para no filtrar por él.
     * @param cuit El CUIT exacto a filtrar (con o sin guiones; se compara con el formato {@code XX-XXXXXXXX-X} en que se guarda), o {@code null} para no filtrar por él.
     * @param idLocalidad El ID de la localidad a filtrar, o {@code null} para no filtrar por ella.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link ProveedorResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ProveedorResponseDTO> filtrarProveedores(String razonSocial, String nombreComercial, String cuit, Long idLocalidad, Pageable pageable) {
        // Los textos buscados se normalizan igual que los guardados: sin espacios de más, y el CUIT con el formato
        // XX-XXXXXXXX-X con el que se guarda (un valor que no tiene 11 dígitos no coincide con ningún proveedor)
        return proveedorRepository.filtrarProveedores(
                        vacioANulo(NormalizacionTexto.nombrePropio(razonSocial)),
                        vacioANulo(NormalizacionTexto.nombrePropio(nombreComercial)),
                        vacioANulo(ValidacionesTexto.formatearCuit(NormalizacionTexto.cuit(cuit))),
                        idLocalidad, pageable)
                .map(MapperProveedor::toDTO);
    }

    /**
     * Busca y retorna un proveedor específico mediante su identificador único.
     *
     * @param id Identificador clave primaria del proveedor buscado.
     * @return Objeto {@link ProveedorResponseDTO} con la información del proveedor encontrado.
     * @throws RecursoNoEncontradoException Si no existe ningún proveedor activo con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public ProveedorResponseDTO buscarPorId(Long id) {
        return MapperProveedor.toDTO(proveedorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el proveedor con ID: " + id)));
    }

    /**
     * Registra un nuevo proveedor en el sistema junto con su versión inicial.
     * <p>
     * {@link ProveedorEntity} no tiene datos propios: toda la información (razón social, CUIT,
     * datos de contacto, localidad y catálogo de productos) se registra en la primera
     * {@link VersionProveedorEntity}, marcada como {@code esUltimaVersion = true}. Gracias al
     * {@code CascadeType.ALL} declarado en las relaciones, persistir el proveedor persiste en
     * cascada toda la versión y su catálogo.
     * </p>
     * <p>
     * Normaliza los datos de texto (el CUIT se guarda con el formato {@code XX-XXXXXXXX-X} y el correo electrónico en minúsculas),
     * valida que sean correctos y que el CUIT y el correo electrónico no estén registrados en otro proveedor activo
     * (sin distinguir mayúsculas). La razón social no es única: el CUIT identifica al proveedor. El teléfono es
     * opcional y no tiene unicidad. La localidad es obligatoria y el catálogo debe tener entre 1 y
     * {@link LimitesTecnicos#CATALOGO_PROVEEDOR_MAXIMO_ITEMS} ítems, cada uno con su insumo y su presentación comercial.
     * </p>
     *
     * @param proveedorFormDTO Objeto DTO que contiene los datos de creación del proveedor y su versión inicial.
     * @return {@link ProveedorResponseDTO} representativo del proveedor guardado en la base de datos.
     * @throws RecursoDuplicadoException Si el CUIT o el correo electrónico ya pertenecen a otro proveedor activo; la excepción indica el campo repetido ({@code "cuit"} o {@code "email"}).
     * @throws ReglaNegocioException Si faltan los datos del proveedor; si la razón social, el nombre comercial, el CUIT, el correo electrónico o la dirección faltan, superan su longitud máxima o tienen un formato inválido; si el teléfono informado no tiene solo dígitos o supera los 15; si falta la localidad; o si el catálogo está vacío, supera el máximo de ítems, tiene un ítem sin insumo o sin presentación comercial, o repite el mismo insumo con la misma presentación comercial.
     * @throws RecursoNoEncontradoException Si la localidad, algún insumo o alguna presentación comercial referenciados no existen o no están activos.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.PROVEEDOR)
    public ProveedorResponseDTO altaProveedor(ProveedorFormDTO proveedorFormDTO) {
        VersionProveedorFormDTO versionFormDTO = proveedorFormDTO.getVersion();

        // 1. Normalizar los datos de texto sin modificar el DTO y validar que sean obligatorios, correctos y no superen la longitud máxima
        DatosVersion datos = normalizarYValidar(versionFormDTO);

        // 2. Validar que el CUIT no esté registrado en otro proveedor activo
        if (versionProveedorRepository.existsByCuitAndEsUltimaVersionTrue(datos.cuit())) {
            throw cuitDuplicado(datos.cuit());
        }

        // 3. Validar que el correo electrónico no esté registrado en otro proveedor activo
        if (versionProveedorRepository.existsByEmailIgnoreCaseAndEsUltimaVersionTrue(datos.email())) {
            throw emailDuplicado(datos.email());
        }

        // 4. Crear el contenedor del proveedor y su versión inicial (cascada hacia el catálogo)
        ProveedorEntity proveedorEntity = ProveedorEntity.builder().estado(Estado.ACTIVO).build();
        VersionProveedorEntity versionProveedorEntity = construirVersion(versionFormDTO, datos, proveedorEntity);
        proveedorEntity.getVersiones().add(versionProveedorEntity);

        // 5. Guardar el proveedor en la base de datos (un pedido simultáneo que viole un índice único se traduce al
        //    mensaje del campo) y retornar el DTO de respuesta correspondiente
        return MapperProveedor.toDTO(GuardadoUnico.guardar(proveedorRepository, proveedorEntity, Map.of(
                INDICE_CUIT, () -> cuitDuplicado(datos.cuit()),
                INDICE_EMAIL, () -> emailDuplicado(datos.email()),
                INDICE_ITEM_CATALOGO, ProveedorServicioImpl::catalogoConItemRepetido)));
    }

    /**
     * Modifica un proveedor existente registrando una nueva versión con los datos actualizados.
     * <p>
     * Por diseño, {@link ProveedorEntity} nunca actualiza una versión existente: "modificar un
     * proveedor" crea una nueva {@link VersionProveedorEntity} con {@code esUltimaVersion = true}
     * y desactiva la anterior, para no afectar retroactivamente las órdenes de compra que ya
     * referencian un ítem del catálogo de la versión previa.
     * </p>
     * <p>
     * Para que un proveedor tenga siempre una sola versión vigente, la fila del proveedor se bloquea
     * ({@code PESSIMISTIC_WRITE}) antes de validar la unicidad, y la versión anterior ya desactivada se guarda y se
     * confirma en la base de datos antes de insertar la nueva (Hibernate ejecuta los {@code INSERT} antes que los
     * {@code UPDATE}, y el índice único de versión vigente lo rechazaría).
     * </p>
     *
     * @param id Identificador clave primaria del proveedor a modificar.
     * @param proveedorFormDTO DTO que contiene los nuevos datos de la versión del proveedor.
     * @return {@link ProveedorResponseDTO} representativo del proveedor con la nueva versión aplicada.
     * @throws RecursoDuplicadoException Si el nuevo CUIT o el nuevo correo electrónico ya pertenecen a otro proveedor activo; la excepción indica el campo repetido ({@code "cuit"} o {@code "email"}).
     * @throws ReglaNegocioException Si los datos de la versión no cumplen las mismas reglas del alta (ver {@link #altaProveedor(ProveedorFormDTO)}), o si otra persona modificó el proveedor al mismo tiempo.
     * @throws RecursoNoEncontradoException Si no se localiza un proveedor activo por el ID proporcionado, o si la localidad, algún insumo o alguna presentación comercial referenciados no existen o no están activos.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.PROVEEDOR)
    public ProveedorResponseDTO modificarProveedor(Long id, ProveedorFormDTO proveedorFormDTO) {
        VersionProveedorFormDTO versionFormDTO = proveedorFormDTO.getVersion();

        // 1. Normalizar los datos de texto sin modificar el DTO y validar que sean obligatorios, correctos y no superen la longitud máxima
        DatosVersion datos = normalizarYValidar(versionFormDTO);

        // 2. Localizar el proveedor existente bloqueando su fila: las modificaciones simultáneas del mismo proveedor se
        //    ejecutan una a una. Si no existe o no está activo, se dispara RecursoNoEncontradoException
        ProveedorEntity proveedorEntity = proveedorRepository.findByIdParaActualizarVersiones(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el proveedor con ID: " + id));

        // 3. Validar el CUIT excluyendo al propio proveedor, de modo que conservar el CUIT actual
        //    no falle contra el mismo registro
        if (versionProveedorRepository.existsByCuitAndEsUltimaVersionTrueAndProveedorIdNot(datos.cuit(), id)) {
            throw cuitDuplicado(datos.cuit());
        }

        // 4. Validar el correo electrónico excluyendo al propio proveedor
        if (versionProveedorRepository.existsByEmailIgnoreCaseAndEsUltimaVersionTrueAndProveedorIdNot(datos.email(), id)) {
            throw emailDuplicado(datos.email());
        }

        // 5. Construir la nueva versión primero: si la localidad, algún insumo o alguna
        //    presentación comercial no existen, esto lanza antes de tocar la versión anterior
        VersionProveedorEntity nuevaVersionProveedorEntity = construirVersion(versionFormDTO, datos, proveedorEntity);

        // 6. Recién si la construcción fue exitosa, desactivar la versión actualmente activa y confirmar el cambio en la
        //    base de datos antes de insertar la nueva
        MetodosVersionado.desactivarVersionAnterior(proveedorEntity.getVersiones());
        proveedorRepository.save(proveedorEntity);
        proveedorRepository.flush();

        // 7. Registrar la nueva versión, persistir el proveedor (un pedido simultáneo que viole un índice único se
        //    traduce al mensaje del campo) y retornar el DTO de respuesta correspondiente
        proveedorEntity.getVersiones().add(nuevaVersionProveedorEntity);
        return MapperProveedor.toDTO(GuardadoUnico.guardar(proveedorRepository, proveedorEntity, Map.of(
                INDICE_CUIT, () -> cuitDuplicado(datos.cuit()),
                INDICE_EMAIL, () -> emailDuplicado(datos.email()),
                INDICE_ITEM_CATALOGO, ProveedorServicioImpl::catalogoConItemRepetido,
                INDICE_ULTIMA_VERSION, ProveedorServicioImpl::proveedorModificadoEnSimultaneo)));
    }

    /**
     * Procesa la baja lógica de un proveedor existente en el sistema.
     * <p>
     * En lugar de eliminar el registro, marca al proveedor con {@link Estado#BAJA} y persiste el
     * cambio. A partir de ese momento, todas las consultas del repositorio dejan de encontrarlo.
     * Todas sus versiones pasan a {@code padreActivo = false}, de modo que su CUIT y su correo electrónico quedan libres
     * para un proveedor nuevo.
     * </p>
     *
     * @param id Identificador clave primaria del proveedor a dar de baja.
     * @return {@link ProveedorResponseDTO} con los datos del proveedor ya marcado como dado de baja.
     * @throws RecursoNoEncontradoException Si el proveedor con el ID especificado no existe o ya fue dado de baja.
     * @throws ReglaNegocioException Si el proveedor tiene una orden de compra en estado {@code PENDIENTE} asociada a alguna de sus versiones.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.PROVEEDOR)
    public ProveedorResponseDTO bajaProveedor(Long id) {
        // 1. Buscar el proveedor bloqueando su fila, para que una modificación simultánea no cree una versión sobre un
        //    proveedor que se está dando de baja. Si no existe, se dispara RecursoNoEncontradoException
        ProveedorEntity proveedorEntity = proveedorRepository.findByIdParaActualizarVersiones(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el proveedor con ID: " + id));

        // 2. Validar que ninguna versión del proveedor (histórica o activa) tenga una orden de
        //    compra en estado PENDIENTE asociada
        if (ordenCompraRepository.existsByVersionProveedor_Proveedor_IdAndEstado(id, EstadoSolicitud.PENDIENTE)) {
            throw new ReglaNegocioException("No se puede dar de baja el proveedor porque tiene al menos una orden de compra en estado Pendiente asociada");
        }

        // 3. Ejecutamos la baja lógica: cambiamos el estado, liberamos los datos únicos de todas sus versiones,
        //    persistimos el cambio y retornamos el DTO del proveedor dado de baja
        proveedorEntity.setEstado(Estado.BAJA);
        MetodosVersionado.marcarPadreInactivo(proveedorEntity.getVersiones());
        return MapperProveedor.toDTO(proveedorRepository.save(proveedorEntity));
    }

    // === NORMALIZACIÓN Y VALIDACIÓN DE LOS DATOS DE TEXTO ===

    /**
     * Normaliza los datos de texto de la versión sin modificar el DTO y valida, antes de consultar la base de datos,
     * que los datos sean obligatorios, correctos y no superen la longitud máxima.
     * <p>
     * La razón social, el nombre comercial y la dirección conservan las mayúsculas y las tildes escritas (la
     * puntuación cuenta: {@code ACME S.A.} y {@code ACME SA} se guardan tal como se escriben); el CUIT se guarda
     * con el formato {@code XX-XXXXXXXX-X} y el correo electrónico en minúsculas.
     * </p>
     * <p>
     * Los datos se validan en el orden de los campos del formulario (razón social, nombre comercial, CUIT, teléfono,
     * correo electrónico, dirección, localidad y catálogo), de modo que se informa el primer dato inválido.
     * </p>
     *
     * @throws ReglaNegocioException Si faltan los datos de la versión o si algún dato no cumple su regla.
     */
    private static DatosVersion normalizarYValidar(VersionProveedorFormDTO versionFormDTO) {
        if (versionFormDTO == null) {
            throw new ReglaNegocioException("Los datos del proveedor son obligatorios");
        }
        DatosVersion datos = new DatosVersion(
                NormalizacionTexto.nombrePropio(versionFormDTO.getRazonSocial()),
                NormalizacionTexto.nombrePropio(versionFormDTO.getNombreComercial()),
                NormalizacionTexto.cuit(versionFormDTO.getCuit()),
                NormalizacionTexto.telefono(versionFormDTO.getTelefono()),
                NormalizacionTexto.correo(versionFormDTO.getEmail()),
                NormalizacionTexto.nombrePropio(versionFormDTO.getDireccion()));

        validarObligatorioYLongitud(datos.razonSocial(), LimitesTecnicos.LONGITUD_MAXIMA_NOMBRE, "La razón social", "obligatoria");
        validarObligatorioYLongitud(datos.nombreComercial(), LimitesTecnicos.LONGITUD_MAXIMA_NOMBRE, "El nombre comercial", "obligatorio");
        validarCuit(datos.cuit());
        // Desde acá el CUIT ya tiene 11 dígitos: se guarda y se compara con el formato XX-XXXXXXXX-X, de modo que
        // "20123456786" y "20-12345678-6" son el mismo CUIT
        datos = datos.conCuit(ValidacionesTexto.formatearCuit(datos.cuit()));
        ValidacionesTexto.validarTelefono(datos.telefono());
        validarObligatorioYLongitud(datos.email(), LimitesTecnicos.LONGITUD_MAXIMA_CORREO, "El correo electrónico", "obligatorio");
        ValidacionesTexto.validarCorreo(datos.email());
        validarObligatorioYLongitud(datos.direccion(), LimitesTecnicos.LONGITUD_MAXIMA_DIRECCION, "La dirección", "obligatoria");
        if (versionFormDTO.getIdLocalidad() == null) {
            throw new ReglaNegocioException("La localidad es obligatoria");
        }
        validarCatalogo(versionFormDTO.getCatalogoProveedor());
        return datos;
    }

    /**
     * Valida el CUIT normalizado: es obligatorio y, si se informa, debe tener 11 dígitos.
     *
     * @throws ReglaNegocioException Si el CUIT es nulo o queda vacío al quitar los separadores, o si no tiene 11 dígitos.
     */
    private static void validarCuit(String cuit) {
        if (cuit == null || cuit.isEmpty()) {
            throw new ReglaNegocioException("El CUIT es obligatorio");
        }
        ValidacionesTexto.validarCuit(cuit);
    }

    /**
     * Valida el catálogo: debe tener entre 1 y {@link LimitesTecnicos#CATALOGO_PROVEEDOR_MAXIMO_ITEMS} ítems, cada ítem
     * debe indicar su insumo y su presentación comercial, y no puede repetir el mismo ítem (un ítem es la combinación
     * de un insumo y una presentación comercial). Un catálogo nulo equivale a un catálogo vacío.
     *
     * @throws ReglaNegocioException Si el catálogo es nulo o está vacío, si supera el máximo de ítems, si un ítem es
     *                               nulo o no indica el insumo o la presentación comercial, o si el mismo insumo con la
     *                               misma presentación comercial figura más de una vez.
     */
    private static void validarCatalogo(List<CatalogoProveedorFormDTO> catalogo) {
        if (catalogo == null || catalogo.isEmpty()) {
            throw new ReglaNegocioException("El catálogo debe tener al menos un ítem");
        }
        if (catalogo.size() > LimitesTecnicos.CATALOGO_PROVEEDOR_MAXIMO_ITEMS) {
            throw new ReglaNegocioException("El catálogo no puede superar "
                    + ValidacionesNumericas.formatearNumero(LimitesTecnicos.CATALOGO_PROVEEDOR_MAXIMO_ITEMS) + " ítems");
        }
        Set<List<Long>> itemsVistos = new HashSet<>();
        for (CatalogoProveedorFormDTO item : catalogo) {
            if (item == null || item.getIdInsumo() == null) {
                throw new ReglaNegocioException("El insumo de cada ítem del catálogo es obligatorio");
            }
            if (item.getIdPresentacionComercial() == null) {
                throw new ReglaNegocioException("La presentación comercial de cada ítem del catálogo es obligatoria");
            }
            if (!itemsVistos.add(Arrays.asList(item.getIdInsumo(), item.getIdPresentacionComercial()))) {
                throw catalogoConItemRepetido();
            }
        }
    }

    private static void validarObligatorioYLongitud(String valor, int maximo, String etiqueta, String obligatorio) {
        if (valor == null || valor.isEmpty()) {
            throw new ReglaNegocioException(etiqueta + " es " + obligatorio);
        }
        ValidacionesNumericas.validarLongitudMaxima(valor, maximo, etiqueta);
    }

    /**
     * Un texto de búsqueda formado solo por espacios equivale a no filtrar por ese criterio.
     */
    private static String vacioANulo(String valor) {
        return (valor == null || valor.isEmpty()) ? null : valor;
    }

    /**
     * Duplicado del CUIT, con el campo {@code "cuit"} para que el Controller marque ese campo del formulario.
     */
    private static RecursoDuplicadoException cuitDuplicado(String cuit) {
        return new RecursoDuplicadoException("Ya existe un proveedor con el CUIT " + cuit, CAMPO_CUIT);
    }

    /**
     * Duplicado del correo electrónico, con el campo {@code "email"} para que el Controller marque ese campo del formulario.
     */
    private static RecursoDuplicadoException emailDuplicado(String email) {
        return new RecursoDuplicadoException("Ya existe un proveedor con el correo electrónico '" + email + "'", CAMPO_EMAIL);
    }

    private static ReglaNegocioException catalogoConItemRepetido() {
        return new ReglaNegocioException("El catálogo no puede repetir el mismo insumo con la misma presentación comercial");
    }

    private static ReglaNegocioException proveedorModificadoEnSimultaneo() {
        return new ReglaNegocioException("El proveedor fue modificado por otra persona al mismo tiempo, intente nuevamente");
    }

    // === CONSTRUCCIÓN DEL ÁRBOL DE ENTIDADES DE LA VERSIÓN ===

    /**
     * Construye la {@link VersionProveedorEntity} completa a partir del DTO de formulario,
     * resolviendo la localidad referenciada e incluyendo el catálogo de productos.
     *
     * @param versionFormDTO Datos de la versión de proveedor a construir (la localidad y el catálogo).
     * @param datos Datos de texto de la versión, ya normalizados y validados.
     * @param proveedorEntity Proveedor contenedor al que pertenece esta versión.
     * @return Entidad de versión de proveedor, marcada como última versión activa, con su catálogo vinculado.
     * @throws RecursoNoEncontradoException Si no existe una localidad activa con el ID indicado.
     */
    private VersionProveedorEntity construirVersion(VersionProveedorFormDTO versionFormDTO, DatosVersion datos, ProveedorEntity proveedorEntity) {
        LocalidadEntity localidadEntity = localidadRepository.findById(versionFormDTO.getIdLocalidad())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la localidad con ID: " + versionFormDTO.getIdLocalidad()));

        VersionProveedorEntity versionProveedorEntity = VersionProveedorEntity.builder()
                .razonSocial(datos.razonSocial())
                .nombreComercial(datos.nombreComercial())
                .cuit(datos.cuit())
                .telefono(datos.telefono())
                .email(datos.email())
                .direccion(datos.direccion())
                .esUltimaVersion(true)
                .padreActivo(true)
                .proveedor(proveedorEntity)
                .localidad(localidadEntity)
                .build();

        versionProveedorEntity.getCatalogoProveedor().addAll(versionFormDTO.getCatalogoProveedor().stream()
                .map(detalle -> construirItemCatalogo(detalle, versionProveedorEntity))
                .toList());

        return versionProveedorEntity;
    }

    /**
     * Construye el ítem de catálogo a partir del DTO de formulario, resolviendo la presentación
     * comercial y el insumo referenciados por sus identificadores.
     *
     * @param catalogoFormDTO Datos del ítem de catálogo.
     * @param versionProveedorEntity Versión de proveedor a la que pertenece este ítem.
     * @return Entidad de ítem de catálogo vinculada a la presentación comercial, al insumo y a la versión de proveedor.
     * @throws RecursoNoEncontradoException Si no existe una presentación comercial activa o un insumo activo con el ID indicado.
     */
    private CatalogoProveedorEntity construirItemCatalogo(CatalogoProveedorFormDTO catalogoFormDTO, VersionProveedorEntity versionProveedorEntity) {
        PresentacionComercialEntity presentacionComercialEntity = presentacionComercialRepository.findById(catalogoFormDTO.getIdPresentacionComercial())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la presentación comercial con ID: " + catalogoFormDTO.getIdPresentacionComercial()));

        InsumoEntity insumoEntity = insumoRepository.findById(catalogoFormDTO.getIdInsumo())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el insumo con ID: " + catalogoFormDTO.getIdInsumo()));

        return CatalogoProveedorEntity.builder()
                .presentacionComercial(presentacionComercialEntity)
                .insumo(insumoEntity)
                .version(versionProveedorEntity)
                .build();
    }
}
