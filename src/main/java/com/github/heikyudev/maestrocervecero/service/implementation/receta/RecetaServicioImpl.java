package com.github.heikyudev.maestrocervecero.service.implementation.receta;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LevaduraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LupuloEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleLevaduraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleLupuloEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleMaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.RecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.UsoLupulo;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.ILevaduraRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.ILupuloRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.IMaltaRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IPlanificacionProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.receta.IRecetaRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.receta.IVersionRecetaRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.receta.DetalleLevaduraFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.receta.DetalleLupuloFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.receta.DetalleMaltaFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.receta.RecetaFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.receta.VersionRecetaFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.receta.IRecetaServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.RecetaResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.receta.MapperReceta;
import com.github.heikyudev.maestrocervecero.util.method.MetodosVersionado;
import com.github.heikyudev.maestrocervecero.util.persistencia.GuardadoUnico;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import com.github.heikyudev.maestrocervecero.util.validacion.NormalizacionTexto;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesNumericas;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RecetaServicioImpl implements IRecetaServicio {

    /**
     * Índice único de la base de datos que garantiza que no haya dos recetas activas con el mismo nombre.
     */
    private static final String INDICE_NOMBRE = "udx_version_receta_nombre";

    /**
     * Índice único de la base de datos que garantiza que una receta tenga una sola versión vigente.
     */
    private static final String INDICE_ULTIMA_VERSION = "udx_version_receta_ultima_version";

    private static final BigDecimal OG_MINIMA = new BigDecimal("1.020");
    private static final BigDecimal OG_MAXIMA = new BigDecimal("1.120");
    private static final BigDecimal FG_MINIMA = new BigDecimal("1.000");
    private static final BigDecimal FG_MAXIMA = new BigDecimal("1.040");
    private static final int GRAVEDAD_MAX_DECIMALES = 3;

    private final IRecetaRepository recetaRepository;
    private final IVersionRecetaRepository versionRecetaRepository;
    private final IMaltaRepository maltaRepository;
    private final ILupuloRepository lupuloRepository;
    private final ILevaduraRepository levaduraRepository;
    private final IPlanificacionProduccionRepository planificacionProduccionRepository;

    /**
     * Recupera una página de recetas activas registradas en el sistema, filtradas opcionalmente
     * por el nombre de su versión vigente (coincidencia parcial, sin distinguir
     * mayúsculas/minúsculas).
     * <p>
     * Las recetas dadas de baja son excluidas por la condición {@code estado = 'ACTIVO'} aplicada
     * en el repositorio.
     * </p>
     *
     * @param nombre Texto a buscar dentro del nombre de la versión vigente, o {@code null} para no filtrar por él.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link RecetaResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<RecetaResponseDTO> filtrarRecetas(String nombre, Pageable pageable) {
        // El texto buscado se normaliza igual que el nombre guardado: sin espacios de más
        return recetaRepository.filtrarRecetas(vacioANulo(NormalizacionTexto.nombrePropio(nombre)), pageable).map(MapperReceta::toDTO);
    }

    /**
     * Busca y retorna una receta específica mediante su identificador único.
     *
     * @param id Identificador clave primaria de la receta buscada.
     * @return Objeto {@link RecetaResponseDTO} con la información de la receta encontrada.
     * @throws RecursoNoEncontradoException Si no existe ninguna receta activa con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public RecetaResponseDTO buscarPorId(Long id) {
        return MapperReceta.toDTO(recetaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la receta con ID: " + id)));
    }

    /**
     * Registra una nueva receta en el sistema junto con su versión inicial.
     * <p>
     * {@link RecetaEntity} no tiene datos propios: toda la información productiva (volumen, OG,
     * FG, duraciones, maltas, lúpulos, levaduras) se registra en la
     * primera {@link VersionRecetaEntity}, marcada como {@code esUltimaVersion = true}. Gracias al
     * {@code CascadeType.ALL} declarado en las relaciones, persistir la receta persiste en cascada
     * toda la versión y sus detalles.
     * </p>
     *
     * @param recetaFormDTO Objeto DTO que contiene los datos de creación de la receta y su versión inicial.
     * @return {@link RecetaResponseDTO} representativo de la receta guardada en la base de datos.
     * @throws ReglaNegocioException Si el nombre falta o supera su longitud máxima, o si alguno de los demás datos de la versión o sus detalles incumple las reglas de negocio.
     * @throws RecursoDuplicadoException Si el nombre provisto ya pertenece a una receta activa (sin distinguir mayúsculas).
     * @throws RecursoNoEncontradoException Si alguna malta, lúpulo o levadura referenciada no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.RECETA)
    public RecetaResponseDTO altaReceta(RecetaFormDTO recetaFormDTO) {
        VersionRecetaFormDTO versionFormDTO = recetaFormDTO.getVersion();

        // 1. Normalizar el nombre sin modificar el DTO y validar las reglas de negocio de los datos de la versión y sus detalles
        String nombre = NormalizacionTexto.nombrePropio(versionFormDTO.getNombre());
        validarDatosGenerales(nombre, versionFormDTO);
        validarDetallesMalta(versionFormDTO.getDetallesMalta());
        validarDetallesLupulo(versionFormDTO.getDetallesLupulo(), versionFormDTO.getDuracionHervido());
        validarDetallesLevadura(versionFormDTO.getDetallesLevadura());

        // 2. Validar si el nombre ya está registrado en otra receta activa (sin distinguir mayúsculas)
        if (versionRecetaRepository.existsByNombreIgnoreCaseAndEsUltimaVersionTrue(nombre)) {
            throw nombreDuplicadoEnAlta(nombre);
        }

        // 3. Crear el contenedor de la receta y su versión inicial (cascada hacia todos los detalles)
        RecetaEntity recetaEntity = RecetaEntity.builder().estado(Estado.ACTIVO).build();
        VersionRecetaEntity versionRecetaEntity = construirVersion(nombre, versionFormDTO, recetaEntity);
        recetaEntity.getVersiones().add(versionRecetaEntity);

        // 4. Guardar la receta en la base de datos (un pedido simultáneo que viole el índice único se traduce al mensaje
        //    del nombre) y retornar el DTO de respuesta correspondiente
        return MapperReceta.toDTO(GuardadoUnico.guardar(recetaRepository, recetaEntity, Map.of(
                INDICE_NOMBRE, () -> nombreDuplicadoEnAlta(nombre))));
    }

    /**
     * Modifica una receta existente registrando una nueva versión con los datos actualizados.
     * <p>
     * Por diseño, {@link RecetaEntity} nunca actualiza una versión existente: "modificar una
     * receta" crea una nueva {@link VersionRecetaEntity} con {@code esUltimaVersion = true} y
     * desactiva la anterior, para no afectar retroactivamente lotes de producción que ya
     * arrancaron con la versión previa.
     * </p>
     * <p>
     * Para que una receta tenga siempre una sola versión vigente, la fila de la receta se bloquea
     * ({@code PESSIMISTIC_WRITE}) antes de validar la unicidad, y la versión anterior ya desactivada se guarda y se
     * confirma en la base de datos antes de insertar la nueva (Hibernate ejecuta los {@code INSERT} antes que los
     * {@code UPDATE}, y el índice único de versión vigente lo rechazaría).
     * </p>
     *
     * @param id Identificador clave primaria de la receta a modificar.
     * @param recetaFormDTO DTO que contiene los nuevos datos de la versión de la receta.
     * @return {@link RecetaResponseDTO} representativo de la receta con la nueva versión aplicada.
     * @throws ReglaNegocioException Si el nombre falta o supera su longitud máxima, si alguno de los demás datos de la versión o sus detalles incumple las reglas de negocio, o si otra persona modificó la receta al mismo tiempo.
     * @throws RecursoNoEncontradoException Si no se localiza una receta activa por el ID proporcionado, o si alguna malta, lúpulo o levadura referenciada no existe.
     * @throws RecursoDuplicadoException Si el nuevo nombre ya se encuentra asignado a otra receta activa.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.RECETA)
    public RecetaResponseDTO modificarReceta(Long id, RecetaFormDTO recetaFormDTO) {
        VersionRecetaFormDTO versionFormDTO = recetaFormDTO.getVersion();

        // 1. Normalizar el nombre sin modificar el DTO y validar las reglas de negocio de los datos de la versión y sus detalles
        String nombre = NormalizacionTexto.nombrePropio(versionFormDTO.getNombre());
        validarDatosGenerales(nombre, versionFormDTO);
        validarDetallesMalta(versionFormDTO.getDetallesMalta());
        validarDetallesLupulo(versionFormDTO.getDetallesLupulo(), versionFormDTO.getDuracionHervido());
        validarDetallesLevadura(versionFormDTO.getDetallesLevadura());

        // 2. Localizar la receta existente bloqueando su fila: las modificaciones simultáneas de la misma receta se
        //    ejecutan una a una. Si no existe, se dispara RecursoNoEncontradoException
        RecetaEntity recetaEntity = recetaRepository.findByIdParaActualizarVersiones(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la receta con ID: " + id));

        // 3. Validar duplicación excluyendo la propia receta, de modo que conservar el nombre
        //    actual no falle contra el mismo registro
        if (versionRecetaRepository.existsByNombreIgnoreCaseAndEsUltimaVersionTrueAndRecetaIdNot(nombre, id)) {
            throw nombreDuplicadoEnModificacion(nombre);
        }

        // 4. Construir la nueva versión primero: si alguna malta, lúpulo o levadura
        //    no existe, esto lanza antes de tocar la versión anterior
        VersionRecetaEntity nuevaVersionRecetaEntity = construirVersion(nombre, versionFormDTO, recetaEntity);

        // 5. Recién si la construcción fue exitosa, desactivar la versión actualmente activa (si existe) y confirmar el
        //    cambio en la base de datos antes de insertar la nueva
        MetodosVersionado.desactivarVersionAnterior(recetaEntity.getVersiones());
        recetaRepository.save(recetaEntity);
        recetaRepository.flush();

        // 6. Registrar la nueva versión, persistir la receta (un pedido simultáneo que viole un índice único se traduce
        //    al mensaje correspondiente) y retornar el DTO de respuesta correspondiente
        recetaEntity.getVersiones().add(nuevaVersionRecetaEntity);
        return MapperReceta.toDTO(GuardadoUnico.guardar(recetaRepository, recetaEntity, Map.of(
                INDICE_NOMBRE, () -> nombreDuplicadoEnModificacion(nombre),
                INDICE_ULTIMA_VERSION, RecetaServicioImpl::recetaModificadaEnSimultaneo)));
    }

    /**
     * Procesa la baja lógica de una receta existente en el sistema.
     * <p>
     * En lugar de eliminar el registro, marca a la receta con {@link Estado#BAJA} y persiste el
     * cambio. A partir de ese momento, todas las consultas del repositorio dejan de encontrarla.
     * Todas sus versiones pasan a {@code padreActivo = false}, de modo que su nombre queda libre para una receta nueva.
     * </p>
     *
     * @param id Identificador clave primaria de la receta a dar de baja.
     * @return {@link RecetaResponseDTO} con los datos de la receta ya marcada como dada de baja.
     * @throws RecursoNoEncontradoException Si la receta con el ID especificado no existe o ya fue dada de baja.
     * @throws ReglaNegocioException Si la receta tiene una planificación de producción en estado {@code PENDIENTE} asociada a alguna de sus versiones.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.RECETA)
    public RecetaResponseDTO bajaReceta(Long id) {
        // 1. Buscar la receta bloqueando su fila, para que una modificación simultánea no cree una versión sobre una
        //    receta que se está dando de baja. Si no existe, se dispara RecursoNoEncontradoException
        RecetaEntity recetaEntity = recetaRepository.findByIdParaActualizarVersiones(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la receta con ID: " + id));

        // 2. Validar que ninguna versión de la receta (histórica o activa) tenga una planificación de
        //    producción en estado PENDIENTE asociada
        if (planificacionProduccionRepository.existsByVersionReceta_Receta_IdAndEstado(id, EstadoSolicitud.PENDIENTE)) {
            throw new ReglaNegocioException("No se puede dar de baja la receta porque tiene una planificación de producción en estado Pendiente asociada");
        }

        // 3. Ejecutamos la baja lógica: cambiamos el estado, liberamos el nombre de todas sus versiones y persistimos el cambio
        recetaEntity.setEstado(Estado.BAJA);
        MetodosVersionado.marcarPadreInactivo(recetaEntity.getVersiones());
        recetaRepository.save(recetaEntity);

        // 4. Retornar el DTO de la receta dada de baja
        return MapperReceta.toDTO(recetaEntity);
    }

    // === VALIDACIONES DE DATOS GENERALES DE LA VERSIÓN ===

    /**
     * Indica si una densidad objetivo está dentro del rango permitido (ambos extremos incluidos)
     * y tiene como máximo tres decimales. La comparación se hace con {@link BigDecimal} para
     * evitar errores de representación de punto flotante.
     *
     * @param gravedad Densidad a evaluar (puede ser {@code null}).
     * @param minima   Valor mínimo permitido.
     * @param maxima   Valor máximo permitido.
     * @return {@code true} si la densidad no es nula, está en rango y tiene a lo sumo tres decimales.
     */
    private static boolean esGravedadValida(Double gravedad, BigDecimal minima, BigDecimal maxima) {
        if (gravedad == null || gravedad.isNaN() || gravedad.isInfinite()) {
            return false;
        }
        BigDecimal valor = BigDecimal.valueOf(gravedad).stripTrailingZeros();
        return valor.scale() <= GRAVEDAD_MAX_DECIMALES
                && valor.compareTo(minima) >= 0
                && valor.compareTo(maxima) <= 0;
    }

    /**
     * Valida los datos generales de la versión de receta: nombre, volumen base, relación de empaste,
     * densidades objetivo (OG/FG), IBU objetivo y duraciones estimadas de cada etapa.
     *
     * @param nombre Nombre de la receta, ya normalizado.
     * @param versionFormDTO Datos de la versión de receta a validar.
     * @throws ReglaNegocioException Si alguno de los datos generales incumple su regla de negocio.
     */
    private static void validarDatosGenerales(String nombre, VersionRecetaFormDTO versionFormDTO) {
        if (nombre == null || nombre.isEmpty()) {
            throw new ReglaNegocioException("El nombre es obligatorio");
        }
        ValidacionesNumericas.validarLongitudMaxima(nombre, LimitesTecnicos.LONGITUD_MAXIMA_NOMBRE, "El nombre");
        if (versionFormDTO.getVolumenBase() == null) {
            throw new ReglaNegocioException("El volumen base es obligatorio");
        }
        if (versionFormDTO.getVolumenBase() <= 0) {
            throw new ReglaNegocioException("El volumen base debe ser mayor a 0");
        }
        ValidacionesNumericas.validarMayorACeroConMaximo(versionFormDTO.getVolumenBase(),
                LimitesTecnicos.VOLUMEN_BASE_RECETA_MAXIMO_LITROS, "El volumen base", "litros");
        if (versionFormDTO.getRelacionDeEmpaste() == null) {
            throw new ReglaNegocioException("La relación de empaste es obligatoria");
        }
        if (versionFormDTO.getRelacionDeEmpaste() <= 0) {
            throw new ReglaNegocioException("La relación de empaste debe ser mayor a 0");
        }
        ValidacionesNumericas.validarMayorACeroConMaximo(versionFormDTO.getRelacionDeEmpaste(),
                LimitesTecnicos.RELACION_EMPASTE_MAXIMA_L_POR_KG, "La relación de empaste", "L/kg");
        if (versionFormDTO.getOgObjetivo() == null) {
            throw new ReglaNegocioException("La OG objetivo es obligatoria");
        }
        if (!esGravedadValida(versionFormDTO.getOgObjetivo(), OG_MINIMA, OG_MAXIMA)) {
            throw new ReglaNegocioException("La densidad inicial (OG) debe estar entre 1,020 y 1,120 y tener como máximo tres decimales (por ejemplo 1,050)");
        }
        if (versionFormDTO.getFgObjetivo() == null) {
            throw new ReglaNegocioException("La FG objetivo es obligatoria");
        }
        if (!esGravedadValida(versionFormDTO.getFgObjetivo(), FG_MINIMA, FG_MAXIMA)) {
            throw new ReglaNegocioException("La densidad final (FG) debe estar entre 1,000 y 1,040 y tener como máximo tres decimales (por ejemplo 1,010)");
        }
        if (versionFormDTO.getOgObjetivo() <= versionFormDTO.getFgObjetivo()) {
            throw new ReglaNegocioException("La densidad inicial (OG) debe ser mayor a la densidad final (FG)");
        }
        if (versionFormDTO.getIbuObjetivo() == null) {
            throw new ReglaNegocioException("El IBU objetivo es obligatorio");
        }
        if (versionFormDTO.getIbuObjetivo() <= 0) {
            throw new ReglaNegocioException("El IBU objetivo debe ser mayor a 0");
        }
        ValidacionesNumericas.validarEnteroMayorACeroConMaximo(versionFormDTO.getIbuObjetivo(),
                LimitesTecnicos.IBU_MAXIMO, "El IBU objetivo", "IBU");
        if (versionFormDTO.getDuracionMaceracion() == null) {
            throw new ReglaNegocioException("La duración estimada de la maceración es obligatoria");
        }
        if (versionFormDTO.getDuracionMaceracion() <= 0) {
            throw new ReglaNegocioException("La duración estimada de la maceración debe ser mayor a 0");
        }
        ValidacionesNumericas.validarEnteroMayorACeroConMaximo(versionFormDTO.getDuracionMaceracion(),
                LimitesTecnicos.DURACION_MACERACION_HERVIDO_MAXIMA_MINUTOS, "La duración estimada de la maceración", "minutos");
        if (versionFormDTO.getDuracionHervido() == null) {
            throw new ReglaNegocioException("La duración estimada del hervido es obligatoria");
        }
        if (versionFormDTO.getDuracionHervido() <= 0) {
            throw new ReglaNegocioException("La duración estimada del hervido debe ser mayor a 0");
        }
        ValidacionesNumericas.validarEnteroMayorACeroConMaximo(versionFormDTO.getDuracionHervido(),
                LimitesTecnicos.DURACION_MACERACION_HERVIDO_MAXIMA_MINUTOS, "La duración estimada del hervido", "minutos");
        if (versionFormDTO.getDuracionFermentacion() == null) {
            throw new ReglaNegocioException("La duración estimada de la fermentación es obligatoria");
        }
        if (versionFormDTO.getDuracionFermentacion() <= 0) {
            throw new ReglaNegocioException("La duración estimada de la fermentación debe ser mayor a 0");
        }
        ValidacionesNumericas.validarEnteroMayorACeroConMaximo(versionFormDTO.getDuracionFermentacion(),
                LimitesTecnicos.DURACION_FERMENTACION_MADURACION_MAXIMA_DIAS, "La duración estimada de la fermentación", "días");
        if (versionFormDTO.getDuracionMaduracion() == null) {
            throw new ReglaNegocioException("La duración estimada de la maduración es obligatoria");
        }
        if (versionFormDTO.getDuracionMaduracion() <= 0) {
            throw new ReglaNegocioException("La duración estimada de la maduración debe ser mayor a 0");
        }
        ValidacionesNumericas.validarEnteroMayorACeroConMaximo(versionFormDTO.getDuracionMaduracion(),
                LimitesTecnicos.DURACION_FERMENTACION_MADURACION_MAXIMA_DIAS, "La duración estimada de la maduración", "días");
    }

    /**
     * Valida que la receta tenga al menos una malta, que todas las cantidades ingresadas
     * sean estrictamente mayores a 0 y que la misma malta no se repita.
     *
     * @param detallesMalta Lista de detalles de malta a validar.
     * @throws ReglaNegocioException Si la lista está vacía, alguna cantidad no es mayor a 0 o la misma malta se repite.
     */
    private static void validarDetallesMalta(List<DetalleMaltaFormDTO> detallesMalta) {
        if (detallesMalta == null || detallesMalta.isEmpty()) {
            throw new ReglaNegocioException("La receta debe tener al menos una malta");
        }
        if (detallesMalta.stream().anyMatch(detalle -> detalle.getCantidad() == null)) {
            throw new ReglaNegocioException("La cantidad de cada malta es obligatoria");
        }
        if (detallesMalta.stream().anyMatch(detalle -> detalle.getCantidad() <= 0)) {
            throw new ReglaNegocioException("La cantidad de cada malta debe ser mayor a 0");
        }
        detallesMalta.forEach(detalle -> ValidacionesNumericas.validarMayorACeroConMaximo(detalle.getCantidad(),
                LimitesTecnicos.CANTIDAD_INSUMO_RECETA_MAXIMA, "La cantidad de cada malta", "kg"));
        if (hayRepetidos(detallesMalta.stream().map(DetalleMaltaFormDTO::getIdMalta).toList())) {
            throw new ReglaNegocioException("La receta no puede tener la misma malta más de una vez");
        }
    }

    /**
     * Valida que la receta tenga al menos un lúpulo, que todas las cantidades ingresadas sean
     * estrictamente mayores a 0, que la etapa de uso de cada lúpulo sea coherente con su uso
     * (HERVOR y WHIRLPOOL en HERVIDO; DRY_HOP en FERMENTACION o MADURACION), que exista al menos un
     * lúpulo con uso HERVOR, que el tiempo de hervor de cada lúpulo HERVOR sea mayor a 0 y no supere la
     * duración estimada del hervido (puede ser igual), y que no se repita un mismo lúpulo con el mismo uso y tiempo de hervor.
     *
     * @param detallesLupulo Lista de detalles de lúpulo a validar.
     * @param duracionHervido Duración estimada del hervido de la versión, usada como tope del tiempo de hervor.
     * @throws ReglaNegocioException Si la lista está vacía, alguna cantidad no es mayor a 0, alguna etapa de uso
     * no corresponde al uso, no hay ningún lúpulo HERVOR, algún tiempo de hervor es inválido o se repite
     * la combinación de lúpulo, uso y tiempo de hervor.
     */
    private static void validarDetallesLupulo(List<DetalleLupuloFormDTO> detallesLupulo, Integer duracionHervido) {
        if (detallesLupulo == null || detallesLupulo.isEmpty()) {
            throw new ReglaNegocioException("La receta debe tener al menos un lúpulo");
        }
        if (detallesLupulo.stream().anyMatch(detalle -> detalle.getCantidad() == null)) {
            throw new ReglaNegocioException("La cantidad de cada lúpulo es obligatoria");
        }
        if (detallesLupulo.stream().anyMatch(detalle -> detalle.getCantidad() <= 0)) {
            throw new ReglaNegocioException("La cantidad de cada lúpulo debe ser mayor a 0");
        }
        detallesLupulo.forEach(detalle -> ValidacionesNumericas.validarMayorACeroConMaximo(detalle.getCantidad(),
                LimitesTecnicos.CANTIDAD_INSUMO_RECETA_MAXIMA, "La cantidad de cada lúpulo", "g"));

        if (detallesLupulo.stream().anyMatch(detalle -> detalle.getUso() != null
                && detalle.getUso() != UsoLupulo.DRY_HOP && detalle.getEtapaDeUso() != TipoEtapa.HERVIDO)) {
            throw new ReglaNegocioException("La etapa de uso de un lúpulo con uso Hervor o Whirlpool debe ser Hervido");
        }
        if (detallesLupulo.stream().anyMatch(detalle -> detalle.getUso() == UsoLupulo.DRY_HOP
                && detalle.getEtapaDeUso() != TipoEtapa.FERMENTACION && detalle.getEtapaDeUso() != TipoEtapa.MADURACION)) {
            throw new ReglaNegocioException("La etapa de uso de un lúpulo con uso Dry hop debe ser Fermentación o Maduración");
        }

        List<DetalleLupuloFormDTO> lupulosHervor = detallesLupulo.stream()
                .filter(detalle -> detalle.getUso() == UsoLupulo.HERVOR)
                .toList();
        if (lupulosHervor.isEmpty()) {
            throw new ReglaNegocioException("La receta debe tener al menos un lúpulo con uso Hervor");
        }

        if (lupulosHervor.stream().anyMatch(detalle -> detalle.getTiempoDeHervor() == null)) {
            throw new ReglaNegocioException("El tiempo de hervor de cada lúpulo con uso Hervor es obligatorio");
        }
        if (lupulosHervor.stream().anyMatch(detalle -> detalle.getTiempoDeHervor() <= 0)) {
            throw new ReglaNegocioException("El tiempo de hervor de cada lúpulo debe ser mayor a 0");
        }
        if (lupulosHervor.stream().anyMatch(detalle -> detalle.getTiempoDeHervor() > duracionHervido)) {
            throw new ReglaNegocioException("El tiempo de hervor del lúpulo no puede superar la duración del hervido ("
                    + ValidacionesNumericas.formatearNumero(duracionHervido) + " minutos)");
        }
        lupulosHervor.forEach(detalle -> ValidacionesNumericas.validarMayorACero(detalle.getTiempoDeHervor(), "El tiempo de hervor"));

        // El tiempo de hervor solo aplica a HERVOR: para cualquier otro uso cuenta como 0, igual que al persistir
        boolean hayLupuloRepetido = hayRepetidos(detallesLupulo.stream()
                .map(detalle -> Arrays.asList(detalle.getIdLupulo(), detalle.getUso(),
                        detalle.getUso() == UsoLupulo.HERVOR ? detalle.getTiempoDeHervor() : Double.valueOf(0.0)))
                .toList());
        if (hayLupuloRepetido) {
            throw new ReglaNegocioException("La receta no puede repetir un lúpulo con el mismo uso y el mismo tiempo de hervor");
        }
    }

    /**
     * Valida que la receta tenga al menos una levadura, que todas las cantidades ingresadas
     * sean estrictamente mayores a 0 y que la misma levadura no se repita.
     *
     * @param detallesLevadura Lista de detalles de levadura a validar.
     * @throws ReglaNegocioException Si la lista está vacía, alguna cantidad no es mayor a 0 o la misma levadura se repite.
     */
    private static void validarDetallesLevadura(List<DetalleLevaduraFormDTO> detallesLevadura) {
        if (detallesLevadura == null || detallesLevadura.isEmpty()) {
            throw new ReglaNegocioException("La receta debe tener al menos una levadura");
        }
        if (detallesLevadura.stream().anyMatch(detalle -> detalle.getCantidad() == null)) {
            throw new ReglaNegocioException("La cantidad de cada levadura es obligatoria");
        }
        if (detallesLevadura.stream().anyMatch(detalle -> detalle.getCantidad() <= 0)) {
            throw new ReglaNegocioException("La cantidad de cada levadura debe ser mayor a 0");
        }
        detallesLevadura.forEach(detalle -> ValidacionesNumericas.validarMayorACeroConMaximo(detalle.getCantidad(),
                LimitesTecnicos.CANTIDAD_INSUMO_RECETA_MAXIMA, "La cantidad de cada levadura", "g"));
        if (hayRepetidos(detallesLevadura.stream().map(DetalleLevaduraFormDTO::getIdLevadura).toList())) {
            throw new ReglaNegocioException("La receta no puede tener la misma levadura más de una vez");
        }
    }

    /**
     * Indica si la lista de claves contiene algún elemento repetido (admite claves nulas).
     *
     * @param claves Claves a comparar entre sí.
     * @return {@code true} si al menos una clave aparece más de una vez.
     */
    private static boolean hayRepetidos(List<?> claves) {
        Set<Object> vistas = new HashSet<>();
        return claves.stream().anyMatch(clave -> !vistas.add(clave));
    }

    /**
     * Un texto de búsqueda formado solo por espacios equivale a no filtrar por ese criterio.
     */
    private static String vacioANulo(String valor) {
        return (valor == null || valor.isEmpty()) ? null : valor;
    }

    private static RecursoDuplicadoException nombreDuplicadoEnAlta(String nombre) {
        return new RecursoDuplicadoException("Ya existe una receta con el nombre '" + nombre + "'");
    }

    private static RecursoDuplicadoException nombreDuplicadoEnModificacion(String nombre) {
        return new RecursoDuplicadoException("El nombre '" + nombre + "' ya está en uso por otra receta");
    }

    private static ReglaNegocioException recetaModificadaEnSimultaneo() {
        return new ReglaNegocioException("La receta fue modificada por otra persona al mismo tiempo, intente nuevamente");
    }

    // === CONSTRUCCIÓN DEL ÁRBOL DE ENTIDADES DE LA VERSIÓN ===

    /**
     * Construye la {@link VersionRecetaEntity} completa a partir del DTO de formulario, incluyendo
     * sus detalles de malta, lúpulo y levadura.
     *
     * @param nombre Nombre de la receta, ya normalizado.
     * @param versionFormDTO Datos de la versión de receta a construir.
     * @param recetaEntity Receta contenedora a la que pertenece esta versión.
     * @return Entidad de versión de receta, marcada como última versión activa, con todo su árbol de detalles vinculado.
     */
    private VersionRecetaEntity construirVersion(String nombre, VersionRecetaFormDTO versionFormDTO, RecetaEntity recetaEntity) {
        VersionRecetaEntity versionRecetaEntity = VersionRecetaEntity.builder()
                .nombre(nombre)
                .volumenBase(versionFormDTO.getVolumenBase())
                .relacionDeEmpaste(versionFormDTO.getRelacionDeEmpaste())
                .ogObjetivo(versionFormDTO.getOgObjetivo())
                .fgObjetivo(versionFormDTO.getFgObjetivo())
                .ibuObjetivo(versionFormDTO.getIbuObjetivo())
                .duracionMaceracion(versionFormDTO.getDuracionMaceracion())
                .duracionHervido(versionFormDTO.getDuracionHervido())
                .duracionFermentacion(versionFormDTO.getDuracionFermentacion())
                .duracionMaduracion(versionFormDTO.getDuracionMaduracion())
                .esUltimaVersion(true)
                .padreActivo(true)
                .receta(recetaEntity)
                .build();

        versionRecetaEntity.getDetallesMalta().addAll(versionFormDTO.getDetallesMalta().stream()
                .map(detalle -> construirDetalleMalta(detalle, versionRecetaEntity))
                .toList());

        versionRecetaEntity.getDetallesLupulo().addAll(versionFormDTO.getDetallesLupulo().stream()
                .map(detalle -> construirDetalleLupulo(detalle, versionRecetaEntity))
                .toList());

        versionRecetaEntity.getDetallesLevadura().addAll(versionFormDTO.getDetallesLevadura().stream()
                .map(detalle -> construirDetalleLevadura(detalle, versionRecetaEntity))
                .toList());

        return versionRecetaEntity;
    }

    /**
     * Construye el detalle de malta a partir del DTO de formulario, resolviendo la malta
     * referenciada por su identificador.
     *
     * @param detalleFormDTO Datos del detalle de malta.
     * @param versionRecetaEntity Versión de receta a la que pertenece este detalle.
     * @return Entidad de detalle de malta vinculada a la malta y a la versión de receta.
     * @throws RecursoNoEncontradoException Si no existe una malta activa con el ID indicado.
     */
    private DetalleMaltaEntity construirDetalleMalta(DetalleMaltaFormDTO detalleFormDTO, VersionRecetaEntity versionRecetaEntity) {
        MaltaEntity maltaEntity = maltaRepository.findById(detalleFormDTO.getIdMalta())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la malta con ID: " + detalleFormDTO.getIdMalta()));

        return DetalleMaltaEntity.builder()
                .cantidad(detalleFormDTO.getCantidad())
                .malta(maltaEntity)
                .versionReceta(versionRecetaEntity)
                .build();
    }

    /**
     * Construye el detalle de lúpulo a partir del DTO de formulario, resolviendo el lúpulo
     * referenciado por su identificador.
     * <p>
     * El tiempo de hervor se normaliza a 0 cuando el uso no es HERVOR, ya que solo aplica en
     * ese caso y la columna no admite nulos.
     * </p>
     *
     * @param detalleFormDTO Datos del detalle de lúpulo.
     * @param versionRecetaEntity Versión de receta a la que pertenece este detalle.
     * @return Entidad de detalle de lúpulo vinculada al lúpulo y a la versión de receta.
     * @throws RecursoNoEncontradoException Si no existe un lúpulo activo con el ID indicado.
     */
    private DetalleLupuloEntity construirDetalleLupulo(DetalleLupuloFormDTO detalleFormDTO, VersionRecetaEntity versionRecetaEntity) {
        LupuloEntity lupuloEntity = lupuloRepository.findById(detalleFormDTO.getIdLupulo())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lúpulo con ID: " + detalleFormDTO.getIdLupulo()));

        return DetalleLupuloEntity.builder()
                .cantidad(detalleFormDTO.getCantidad())
                .uso(detalleFormDTO.getUso())
                .etapaDeUso(detalleFormDTO.getEtapaDeUso())
                .tiempoDeHervor(detalleFormDTO.getUso() == UsoLupulo.HERVOR ? detalleFormDTO.getTiempoDeHervor() : 0.0)
                .lupulo(lupuloEntity)
                .versionReceta(versionRecetaEntity)
                .build();
    }

    /**
     * Construye el detalle de levadura a partir del DTO de formulario, resolviendo la levadura
     * referenciada por su identificador.
     *
     * @param detalleFormDTO Datos del detalle de levadura.
     * @param versionRecetaEntity Versión de receta a la que pertenece este detalle.
     * @return Entidad de detalle de levadura vinculada a la levadura y a la versión de receta.
     * @throws RecursoNoEncontradoException Si no existe una levadura activa con el ID indicado.
     */
    private DetalleLevaduraEntity construirDetalleLevadura(DetalleLevaduraFormDTO detalleFormDTO, VersionRecetaEntity versionRecetaEntity) {
        LevaduraEntity levaduraEntity = levaduraRepository.findById(detalleFormDTO.getIdLevadura())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la levadura con ID: " + detalleFormDTO.getIdLevadura()));

        return DetalleLevaduraEntity.builder()
                .cantidad(detalleFormDTO.getCantidad())
                .levadura(levaduraEntity)
                .versionReceta(versionRecetaEntity)
                .build();
    }
}
