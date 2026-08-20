package com.github.heikyudev.maestrocervecero.service.implementation.receta;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.etapa_control.EtapaControlEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LevaduraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LupuloEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.parametro_control.ParametroControlEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleLevaduraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleLupuloEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleMaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleParametroControlEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.PlanMonitoreoEtapaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.RecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.UsoLupulo;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoOrden;
import com.github.heikyudev.maestrocervecero.persistence.repository.etapa_control.IEtapaControlRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.ILevaduraRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.ILupuloRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.IMaltaRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.orden_produccion.IOrdenProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.parametro_control.IParametroControlRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.receta.IRecetaRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.receta.IVersionRecetaRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.receta.DetalleLevaduraFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.receta.DetalleLupuloFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.receta.DetalleMaltaFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.receta.DetalleParametroControlFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.receta.PlanMonitoreoEtapaFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.receta.RecetaFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.receta.VersionRecetaFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.receta.IRecetaServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.RecetaResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.receta.MapperReceta;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecetaServicioImpl implements IRecetaServicio {

    private final IRecetaRepository recetaRepository;
    private final IVersionRecetaRepository versionRecetaRepository;
    private final IMaltaRepository maltaRepository;
    private final ILupuloRepository lupuloRepository;
    private final ILevaduraRepository levaduraRepository;
    private final IEtapaControlRepository etapaControlRepository;
    private final IParametroControlRepository parametroControlRepository;
    private final IOrdenProduccionRepository ordenProduccionRepository;

    /**
     * Recupera una página de recetas activas registradas en el sistema.
     * <p>
     * Las recetas eliminadas lógicamente son excluidas automáticamente por el {@code @SoftDelete}
     * de Hibernate sobre la entidad.
     * </p>
     *
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link RecetaResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<RecetaResponseDTO> buscarTodos(Pageable pageable) {
        return recetaRepository.findAll(pageable).map(MapperReceta::toDTO);
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
     * FG, duraciones, maltas, lúpulos, levaduras y plan de monitoreo opcional) se registra en la
     * primera {@link VersionRecetaEntity}, marcada como {@code esUltimaVersion = true}. Gracias al
     * {@code CascadeType.ALL} declarado en las relaciones, persistir la receta persiste en cascada
     * toda la versión y sus detalles.
     * </p>
     *
     * @param recetaFormDTO Objeto DTO que contiene los datos de creación de la receta y su versión inicial.
     * @return {@link RecetaResponseDTO} representativo de la receta guardada en la base de datos.
     * @throws ReglaNegocioException Si alguno de los datos de la versión, sus detalles o su plan de monitoreo incumple las reglas de negocio.
     * @throws RecursoDuplicadoException Si el nombre provisto ya pertenece a una receta activa.
     * @throws RecursoNoEncontradoException Si alguna malta, lúpulo, levadura, etapa de control o parámetro de control referenciado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.RECETA)
    public RecetaResponseDTO altaReceta(RecetaFormDTO recetaFormDTO) {
        VersionRecetaFormDTO versionFormDTO = recetaFormDTO.getVersion();

        // 1. Validar las reglas de negocio de los datos de la versión y sus detalles
        validarDatosGenerales(versionFormDTO);
        validarDetallesMalta(versionFormDTO.getDetallesMalta());
        validarDetallesLupulo(versionFormDTO.getDetallesLupulo(), versionFormDTO.getDuracionHervido());
        validarDetallesLevadura(versionFormDTO.getDetallesLevadura());

        // 2. Validar si el nombre ya está registrado en otra receta activa (case-insensitive)
        if (versionRecetaRepository.existsByNombreIgnoreCaseAndEsUltimaVersionTrue(versionFormDTO.getNombre())) {
            throw new RecursoDuplicadoException("Ya existe una receta con el nombre '" + versionFormDTO.getNombre() + "'");
        }

        // 3. Crear el contenedor de la receta y su versión inicial (cascada hacia todos los detalles)
        RecetaEntity recetaEntity = RecetaEntity.builder().build();
        VersionRecetaEntity versionRecetaEntity = construirVersion(versionFormDTO, recetaEntity);
        recetaEntity.getVersiones().add(versionRecetaEntity);

        // 4. Guardar la receta en la base de datos y retornar el DTO de respuesta correspondiente
        return MapperReceta.toDTO(recetaRepository.save(recetaEntity));
    }

    /**
     * Modifica una receta existente registrando una nueva versión con los datos actualizados.
     * <p>
     * Por diseño, {@link RecetaEntity} nunca actualiza una versión existente: "modificar una
     * receta" crea una nueva {@link VersionRecetaEntity} con {@code esUltimaVersion = true} y
     * desactiva la anterior, para no afectar retroactivamente lotes de producción que ya
     * arrancaron con la versión previa.
     * </p>
     *
     * @param id Identificador clave primaria de la receta a modificar.
     * @param recetaFormDTO DTO que contiene los nuevos datos de la versión de la receta.
     * @return {@link RecetaResponseDTO} representativo de la receta con la nueva versión aplicada.
     * @throws ReglaNegocioException Si alguno de los datos de la versión, sus detalles o su plan de monitoreo incumple las reglas de negocio.
     * @throws RecursoNoEncontradoException Si no se localiza una receta activa por el ID proporcionado, o si alguna malta, lúpulo, levadura, etapa de control o parámetro de control referenciado no existe.
     * @throws RecursoDuplicadoException Si el nuevo nombre ya se encuentra asignado a otra receta activa.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.RECETA)
    public RecetaResponseDTO modificarReceta(Long id, RecetaFormDTO recetaFormDTO) {
        VersionRecetaFormDTO versionFormDTO = recetaFormDTO.getVersion();

        // 1. Validar las reglas de negocio de los datos de la versión y sus detalles
        validarDatosGenerales(versionFormDTO);
        validarDetallesMalta(versionFormDTO.getDetallesMalta());
        validarDetallesLupulo(versionFormDTO.getDetallesLupulo(), versionFormDTO.getDuracionHervido());
        validarDetallesLevadura(versionFormDTO.getDetallesLevadura());

        // 2. Validar duplicación excluyendo la propia receta, de modo que conservar el nombre
        //    actual no falle contra el mismo registro
        if (versionRecetaRepository.existsByNombreIgnoreCaseAndEsUltimaVersionTrueAndRecetaIdNot(versionFormDTO.getNombre(), id)) {
            throw new RecursoDuplicadoException("Ya existe una receta con el nombre '" + versionFormDTO.getNombre() + "'");
        }

        // 3. Localizar la receta existente. Si no existe, se dispara RecursoNoEncontradoException
        RecetaEntity recetaEntity = recetaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la receta con ID: " + id));

        // 4. Desactivar la versión actualmente activa (si existe) y registrar la nueva versión
        recetaEntity.getVersiones().stream()
                .filter(VersionRecetaEntity::isEsUltimaVersion)
                .forEach(version -> version.setEsUltimaVersion(false));

        VersionRecetaEntity nuevaVersionRecetaEntity = construirVersion(versionFormDTO, recetaEntity);
        recetaEntity.getVersiones().add(nuevaVersionRecetaEntity);

        // 5. Persistir la receta con la nueva versión y retornar el DTO de respuesta correspondiente
        return MapperReceta.toDTO(recetaRepository.save(recetaEntity));
    }

    /**
     * Procesa la baja lógica de una receta existente en el sistema.
     * <p>
     * Invoca el método de eliminación del repositorio. Como {@link RecetaEntity} está anotada
     * con {@code @SoftDelete}, Hibernate ejecuta un UPDATE sobre el flag de borrado en lugar de
     * una eliminación física.
     * </p>
     *
     * @param id Identificador clave primaria de la receta a dar de baja.
     * @return {@link RecetaResponseDTO} con los datos de la receta procesada antes de su inactivación.
     * @throws RecursoNoEncontradoException Si la receta con el ID especificado no existe o ya fue dada de baja.
     * @throws ReglaNegocioException Si la receta tiene una orden de producción en estado {@code PENDIENTE} asociada a alguna de sus versiones.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.RECETA)
    public RecetaResponseDTO bajaReceta(Long id) {
        // 1. Buscar la receta. Si no existe, se dispara RecursoNoEncontradoException
        RecetaEntity recetaEntity = recetaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la receta con ID: " + id));

        // 2. Validar que ninguna versión de la receta (histórica o activa) tenga una orden de
        //    producción en estado PENDIENTE asociada
        if (ordenProduccionRepository.existsByVersionReceta_Receta_IdAndEstado(id, EstadoOrden.PENDIENTE)) {
            throw new ReglaNegocioException("No se puede dar de baja la receta porque tiene una orden de producción en estado PENDIENTE asociada");
        }

        // 3. Ejecutar la baja. Hibernate aplicará automáticamente la anotación de Soft Delete
        recetaRepository.delete(recetaEntity);

        // 4. Retornar el DTO de la receta dada de baja
        return MapperReceta.toDTO(recetaEntity);
    }

    // === VALIDACIONES DE DATOS GENERALES DE LA VERSIÓN ===

    /**
     * Valida los datos generales de la versión de receta: volumen base, relación de empaste,
     * densidades objetivo (OG/FG), IBU objetivo y duraciones estimadas de cada etapa.
     *
     * @param versionFormDTO Datos de la versión de receta a validar.
     * @throws ReglaNegocioException Si alguno de los datos generales incumple su regla de negocio.
     */
    private static void validarDatosGenerales(VersionRecetaFormDTO versionFormDTO) {
        if (versionFormDTO.getVolumenBase() == null || versionFormDTO.getVolumenBase() <= 0) {
            throw new ReglaNegocioException("El volumen base debe ser mayor a 0");
        }
        if (versionFormDTO.getRelacionDeEmpaste() == null || versionFormDTO.getRelacionDeEmpaste() <= 0) {
            throw new ReglaNegocioException("La relación de empaste debe ser mayor a 0");
        }
        if (versionFormDTO.getOgObjetivo() == null || versionFormDTO.getFgObjetivo() == null
                || versionFormDTO.getOgObjetivo() <= versionFormDTO.getFgObjetivo()) {
            throw new ReglaNegocioException("La densidad original objetivo (OG) debe ser mayor a la densidad final objetivo (FG)");
        }
        if (versionFormDTO.getIbuObjetivo() == null || versionFormDTO.getIbuObjetivo() < 0) {
            throw new ReglaNegocioException("El IBU objetivo no puede ser negativo");
        }
        if (versionFormDTO.getDuracionMaceracion() == null || versionFormDTO.getDuracionMaceracion() <= 0) {
            throw new ReglaNegocioException("La duración estimada de la maceración debe ser mayor a 0");
        }
        if (versionFormDTO.getDuracionHervido() == null || versionFormDTO.getDuracionHervido() <= 0) {
            throw new ReglaNegocioException("La duración estimada del hervido debe ser mayor a 0");
        }
        if (versionFormDTO.getDuracionFermentacion() == null || versionFormDTO.getDuracionFermentacion() <= 0) {
            throw new ReglaNegocioException("La duración estimada de la fermentación debe ser mayor a 0");
        }
        if (versionFormDTO.getDuracionMaduracion() == null || versionFormDTO.getDuracionMaduracion() <= 0) {
            throw new ReglaNegocioException("La duración estimada de la maduración debe ser mayor a 0");
        }
    }

    /**
     * Valida que la receta tenga al menos una malta y que todas las cantidades ingresadas
     * sean estrictamente mayores a cero.
     *
     * @param detallesMalta Lista de detalles de malta a validar.
     * @throws ReglaNegocioException Si la lista está vacía o alguna cantidad no es mayor a 0.
     */
    private static void validarDetallesMalta(List<DetalleMaltaFormDTO> detallesMalta) {
        if (detallesMalta == null || detallesMalta.isEmpty()) {
            throw new ReglaNegocioException("La receta debe tener al menos una malta");
        }
        boolean hayCantidadInvalida = detallesMalta.stream()
                .anyMatch(detalle -> detalle.getCantidad() == null || detalle.getCantidad() <= 0);
        if (hayCantidadInvalida) {
            throw new ReglaNegocioException("La cantidad de cada malta debe ser mayor a 0");
        }
    }

    /**
     * Valida que la receta tenga al menos un lúpulo, que todas las cantidades ingresadas sean
     * estrictamente mayores a cero, que exista al menos un lúpulo con uso HERVOR y que el tiempo
     * de hervor de cada lúpulo HERVOR sea mayor a 0 y menor a la duración estimada del hervido.
     *
     * @param detallesLupulo Lista de detalles de lúpulo a validar.
     * @param duracionHervido Duración estimada del hervido de la versión, usada como tope del tiempo de hervor.
     * @throws ReglaNegocioException Si la lista está vacía, alguna cantidad no es mayor a 0, no hay
     * ningún lúpulo HERVOR, o algún tiempo de hervor es inválido.
     */
    private static void validarDetallesLupulo(List<DetalleLupuloFormDTO> detallesLupulo, Integer duracionHervido) {
        if (detallesLupulo == null || detallesLupulo.isEmpty()) {
            throw new ReglaNegocioException("La receta debe tener al menos un lúpulo");
        }
        boolean hayCantidadInvalida = detallesLupulo.stream()
                .anyMatch(detalle -> detalle.getCantidad() == null || detalle.getCantidad() <= 0);
        if (hayCantidadInvalida) {
            throw new ReglaNegocioException("La cantidad de cada lúpulo debe ser mayor a 0");
        }

        List<DetalleLupuloFormDTO> lupulosHervor = detallesLupulo.stream()
                .filter(detalle -> detalle.getUso() == UsoLupulo.HERVOR)
                .toList();
        if (lupulosHervor.isEmpty()) {
            throw new ReglaNegocioException("La receta debe tener al menos un lúpulo con uso HERVOR");
        }

        boolean hayTiempoDeHervorInvalido = lupulosHervor.stream()
                .anyMatch(detalle -> detalle.getTiempoDeHervor() == null
                        || detalle.getTiempoDeHervor() <= 0
                        || detalle.getTiempoDeHervor() >= duracionHervido);
        if (hayTiempoDeHervorInvalido) {
            throw new ReglaNegocioException("El tiempo de hervor de cada lúpulo con uso HERVOR debe ser mayor a 0 y menor a la duración estimada del hervido");
        }
    }

    /**
     * Valida que la receta tenga al menos una levadura y que todas las cantidades ingresadas
     * sean estrictamente mayores a cero.
     *
     * @param detallesLevadura Lista de detalles de levadura a validar.
     * @throws ReglaNegocioException Si la lista está vacía o alguna cantidad no es mayor a 0.
     */
    private static void validarDetallesLevadura(List<DetalleLevaduraFormDTO> detallesLevadura) {
        if (detallesLevadura == null || detallesLevadura.isEmpty()) {
            throw new ReglaNegocioException("La receta debe tener al menos una levadura");
        }
        boolean hayCantidadInvalida = detallesLevadura.stream()
                .anyMatch(detalle -> detalle.getCantidad() == null || detalle.getCantidad() <= 0);
        if (hayCantidadInvalida) {
            throw new ReglaNegocioException("La cantidad de cada levadura debe ser mayor a 0");
        }
    }

    // === CONSTRUCCIÓN DEL ÁRBOL DE ENTIDADES DE LA VERSIÓN ===

    /**
     * Construye la {@link VersionRecetaEntity} completa a partir del DTO de formulario, incluyendo
     * sus detalles de malta, lúpulo, levadura y plan de monitoreo (opcional).
     *
     * @param versionFormDTO Datos de la versión de receta a construir.
     * @param recetaEntity Receta contenedora a la que pertenece esta versión.
     * @return Entidad de versión de receta, marcada como última versión activa, con todo su árbol de detalles vinculado.
     */
    private VersionRecetaEntity construirVersion(VersionRecetaFormDTO versionFormDTO, RecetaEntity recetaEntity) {
        VersionRecetaEntity versionRecetaEntity = VersionRecetaEntity.builder()
                .nombre(versionFormDTO.getNombre())
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

        versionRecetaEntity.getPlanesMonitoreo().addAll(obtenerPlanesMonitoreo(versionFormDTO).stream()
                .map(plan -> construirPlanMonitoreoEtapa(plan, versionRecetaEntity))
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

    /**
     * Construye el plan de monitoreo de etapa a partir del DTO de formulario, resolviendo la
     * etapa de control referenciada por su identificador y sus detalles de parámetros de control.
     *
     * @param planFormDTO Datos del plan de monitoreo de etapa.
     * @param versionRecetaEntity Versión de receta a la que pertenece este plan.
     * @return Entidad de plan de monitoreo de etapa vinculada a la etapa de control y a la versión de receta.
     * @throws RecursoNoEncontradoException Si no existe una etapa de control activa con el ID indicado.
     */
    private PlanMonitoreoEtapaEntity construirPlanMonitoreoEtapa(PlanMonitoreoEtapaFormDTO planFormDTO, VersionRecetaEntity versionRecetaEntity) {
        EtapaControlEntity etapaControlEntity = etapaControlRepository.findById(planFormDTO.getIdEtapaControl())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la etapa de control con ID: " + planFormDTO.getIdEtapaControl()));

        PlanMonitoreoEtapaEntity planMonitoreoEtapaEntity = PlanMonitoreoEtapaEntity.builder()
                .etapaControl(etapaControlEntity)
                .versionReceta(versionRecetaEntity)
                .build();

        List<DetalleParametroControlFormDTO> detallesParametroControl = Optional.ofNullable(planFormDTO.getDetallesParametroControl())
                .orElse(Collections.emptyList());

        planMonitoreoEtapaEntity.getDetallesParametroControl().addAll(detallesParametroControl.stream()
                .map(detalle -> construirDetalleParametroControl(detalle, planMonitoreoEtapaEntity))
                .toList());

        return planMonitoreoEtapaEntity;
    }

    /**
     * Construye el detalle de parámetro de control a partir del DTO de formulario, resolviendo el
     * parámetro de control referenciado por su identificador y validando su rango contra los
     * límites teóricos definidos para ese parámetro.
     *
     * @param detalleFormDTO Datos del detalle de parámetro de control.
     * @param planMonitoreoEtapaEntity Plan de monitoreo de etapa al que pertenece este detalle.
     * @return Entidad de detalle de parámetro de control vinculada al parámetro de control y al plan de monitoreo.
     * @throws RecursoNoEncontradoException Si no existe un parámetro de control activo con el ID indicado.
     * @throws ReglaNegocioException Si el rango ingresado es inválido o excede los límites teóricos del parámetro de control.
     */
    private DetalleParametroControlEntity construirDetalleParametroControl(DetalleParametroControlFormDTO detalleFormDTO, PlanMonitoreoEtapaEntity planMonitoreoEtapaEntity) {
        ParametroControlEntity parametroControlEntity = parametroControlRepository.findById(detalleFormDTO.getIdParametroControl())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el parámetro de control con ID: " + detalleFormDTO.getIdParametroControl()));

        validarRangoDetalleParametroControl(detalleFormDTO, parametroControlEntity);

        return DetalleParametroControlEntity.builder()
                .valorMinimo(detalleFormDTO.getValorMinimo())
                .valorMaximo(detalleFormDTO.getValorMaximo())
                .valorIdeal(detalleFormDTO.getValorIdeal())
                .parametroControl(parametroControlEntity)
                .planMonitoreoEtapa(planMonitoreoEtapaEntity)
                .build();
    }

    /**
     * Valida el rango planificado de un detalle de parámetro de control: que el mínimo no supere
     * al máximo, que el ideal caiga dentro de ese rango, y que el rango planificado no exceda los
     * límites teóricos (mínimo y máximo posibles) definidos para el parámetro de control.
     *
     * @param detalleFormDTO Datos del detalle de parámetro de control a validar.
     * @param parametroControlEntity Parámetro de control referenciado, con sus límites teóricos.
     * @throws ReglaNegocioException Si el rango planificado es inválido o excede los límites teóricos.
     */
    private static void validarRangoDetalleParametroControl(DetalleParametroControlFormDTO detalleFormDTO, ParametroControlEntity parametroControlEntity) {
        Double valorMinimo = detalleFormDTO.getValorMinimo();
        Double valorMaximo = detalleFormDTO.getValorMaximo();
        Double valorIdeal = detalleFormDTO.getValorIdeal();

        if (valorMinimo == null || valorMaximo == null || valorMinimo > valorMaximo) {
            throw new ReglaNegocioException("El valor mínimo del parámetro de control '" + parametroControlEntity.getNombre() + "' no puede ser mayor al valor máximo");
        }
        if (valorIdeal == null || valorIdeal < valorMinimo || valorIdeal > valorMaximo) {
            throw new ReglaNegocioException("El valor ideal del parámetro de control '" + parametroControlEntity.getNombre() + "' debe estar dentro del rango mínimo y máximo definido");
        }
        if (valorMinimo < parametroControlEntity.getValorMinimo()) {
            throw new ReglaNegocioException("El valor mínimo del parámetro de control '" + parametroControlEntity.getNombre()
                    + "' no puede ser inferior al valor mínimo posible (" + parametroControlEntity.getValorMinimo() + ")");
        }
        if (valorMaximo > parametroControlEntity.getValorMaximo()) {
            throw new ReglaNegocioException("El valor máximo del parámetro de control '" + parametroControlEntity.getNombre()
                    + "' no puede ser superior al valor máximo posible (" + parametroControlEntity.getValorMaximo() + ")");
        }
    }

    /**
     * Obtiene la lista de planes de monitoreo de la versión, tratando un valor nulo como una
     * lista vacía. El plan de monitoreo es opcional: el usuario puede optar por no configurar
     * ningún control de etapa para la receta.
     *
     * @param versionFormDTO Datos de la versión de receta.
     * @return Lista de planes de monitoreo de etapa, nunca nula.
     */
    private static List<PlanMonitoreoEtapaFormDTO> obtenerPlanesMonitoreo(VersionRecetaFormDTO versionFormDTO) {
        return Optional.ofNullable(versionFormDTO.getPlanesMonitoreo()).orElse(Collections.emptyList());
    }
}
