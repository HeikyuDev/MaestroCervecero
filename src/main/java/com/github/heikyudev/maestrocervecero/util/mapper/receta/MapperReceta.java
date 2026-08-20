package com.github.heikyudev.maestrocervecero.util.mapper.receta;

import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleLevaduraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleLupuloEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleMaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleParametroControlEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.PlanMonitoreoEtapaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.RecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.DetalleLevaduraResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.DetalleLupuloResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.DetalleMaltaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.DetalleParametroControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.PlanMonitoreoEtapaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.RecetaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.VersionRecetaResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.etapa_control.MapperEtapaControl;
import com.github.heikyudev.maestrocervecero.util.mapper.insumo.MapperLevadura;
import com.github.heikyudev.maestrocervecero.util.mapper.insumo.MapperLupulo;
import com.github.heikyudev.maestrocervecero.util.mapper.insumo.MapperMalta;
import com.github.heikyudev.maestrocervecero.util.mapper.parametro_control.MapperParametroControl;

/**
 * MapperReceta tiene la responsabilidad de mapear la entidad RecetaEntity a RecetaResponseDTO.
 * <p>
 * {@link RecetaEntity} no guarda datos productivos propios: siempre expone la última
 * {@link VersionRecetaEntity} activa ({@code esUltimaVersion = true}), ya que el usuario no necesita
 * (ni debe) conocer el historial de versiones — esa estrategia es interna del sistema para preservar
 * la trazabilidad de los lotes ya iniciados con versiones anteriores.
 * </p>
 */
public class MapperReceta {

    /**
     * Mapea una instancia de {@link RecetaEntity} a {@link RecetaResponseDTO}, incluyendo la
     * última versión activa de la receta.
     *
     * @param recetaEntity Entidad de receta a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static RecetaResponseDTO toDTO(RecetaEntity recetaEntity) {
        if (recetaEntity == null) {
            return null;
        }

        return RecetaResponseDTO.builder()
                .id(recetaEntity.getId())
                .contadorLotes(recetaEntity.getContadorLotes())
                .version(mapVersionActual(recetaEntity))
                // === AUDITABLE ENTITY ===
                .createdBy(recetaEntity.getCreatedBy())
                .createdDate(recetaEntity.getCreatedDate())
                .lastModifiedBy(recetaEntity.getLastModifiedBy())
                .lastModifiedDate(recetaEntity.getLastModifiedDate())
                .build();
    }

    /**
     * Busca, entre las versiones de la receta, la última versión activa
     * ({@code esUltimaVersion = true}) y la mapea a {@link VersionRecetaResponseDTO}.
     *
     * @param recetaEntity Entidad de receta cuya última versión activa se quiere obtener.
     * @return DTO de la última versión activa, o {@code null} si la receta no tiene ninguna
     * versión marcada como activa.
     */
    private static VersionRecetaResponseDTO mapVersionActual(RecetaEntity recetaEntity) {
        return recetaEntity.getVersiones().stream()
                .filter(VersionRecetaEntity::isEsUltimaVersion)
                .findFirst()
                .map(MapperReceta::toDTO)
                .orElse(null);
    }

    /**
     * Mapea una instancia de {@link VersionRecetaEntity} a {@link VersionRecetaResponseDTO},
     * incluyendo sus detalles de malta, lúpulo, levadura y planes de monitoreo.
     * <p>
     * Público para que otros módulos que referencian directamente una versión de receta
     * (por ejemplo, {@code MapperOrdenProduccion}) puedan reutilizar este mapeo sin duplicarlo.
     * </p>
     *
     * @param versionRecetaEntity Entidad de versión de receta a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static VersionRecetaResponseDTO toDTO(VersionRecetaEntity versionRecetaEntity) {
        if (versionRecetaEntity == null) {
            return null;
        }

        return VersionRecetaResponseDTO.builder()
                .id(versionRecetaEntity.getId())
                .nombre(versionRecetaEntity.getNombre())
                .volumenBase(versionRecetaEntity.getVolumenBase())
                .relacionDeEmpaste(versionRecetaEntity.getRelacionDeEmpaste())
                .ogObjetivo(versionRecetaEntity.getOgObjetivo())
                .fgObjetivo(versionRecetaEntity.getFgObjetivo())
                .ibuObjetivo(versionRecetaEntity.getIbuObjetivo())
                .duracionMaceracion(versionRecetaEntity.getDuracionMaceracion())
                .duracionHervido(versionRecetaEntity.getDuracionHervido())
                .duracionFermentacion(versionRecetaEntity.getDuracionFermentacion())
                .duracionMaduracion(versionRecetaEntity.getDuracionMaduracion())
                .esUltimaVersion(versionRecetaEntity.isEsUltimaVersion())
                .detallesMalta(versionRecetaEntity.getDetallesMalta().stream()
                        .map(MapperReceta::mapDetalleMalta)
                        .toList())
                .detallesLupulo(versionRecetaEntity.getDetallesLupulo().stream()
                        .map(MapperReceta::mapDetalleLupulo)
                        .toList())
                .detallesLevadura(versionRecetaEntity.getDetallesLevadura().stream()
                        .map(MapperReceta::mapDetalleLevadura)
                        .toList())
                .planesMonitoreo(versionRecetaEntity.getPlanesMonitoreo().stream()
                        .map(MapperReceta::mapPlanMonitoreoEtapa)
                        .toList())
                .build();
    }

    /**
     * Mapea una instancia de {@link DetalleMaltaEntity} a {@link DetalleMaltaResponseDTO},
     * incluyendo la información de la malta asociada.
     *
     * @param detalleMaltaEntity Entidad de detalle de malta a convertir.
     * @return Objeto DTO correspondiente.
     */
    private static DetalleMaltaResponseDTO mapDetalleMalta(DetalleMaltaEntity detalleMaltaEntity) {
        return DetalleMaltaResponseDTO.builder()
                .id(detalleMaltaEntity.getId())
                .cantidad(detalleMaltaEntity.getCantidad())
                .malta(MapperMalta.toDTO(detalleMaltaEntity.getMalta()))
                .build();
    }

    /**
     * Mapea una instancia de {@link DetalleLupuloEntity} a {@link DetalleLupuloResponseDTO},
     * incluyendo la información del lúpulo asociado.
     *
     * @param detalleLupuloEntity Entidad de detalle de lúpulo a convertir.
     * @return Objeto DTO correspondiente.
     */
    private static DetalleLupuloResponseDTO mapDetalleLupulo(DetalleLupuloEntity detalleLupuloEntity) {
        return DetalleLupuloResponseDTO.builder()
                .id(detalleLupuloEntity.getId())
                .cantidad(detalleLupuloEntity.getCantidad())
                .uso(detalleLupuloEntity.getUso())
                .etapaDeUso(detalleLupuloEntity.getEtapaDeUso())
                .tiempoDeHervor(detalleLupuloEntity.getTiempoDeHervor())
                .lupulo(MapperLupulo.toDTO(detalleLupuloEntity.getLupulo()))
                .build();
    }

    /**
     * Mapea una instancia de {@link DetalleLevaduraEntity} a {@link DetalleLevaduraResponseDTO},
     * incluyendo la información de la levadura asociada.
     *
     * @param detalleLevaduraEntity Entidad de detalle de levadura a convertir.
     * @return Objeto DTO correspondiente.
     */
    private static DetalleLevaduraResponseDTO mapDetalleLevadura(DetalleLevaduraEntity detalleLevaduraEntity) {
        return DetalleLevaduraResponseDTO.builder()
                .id(detalleLevaduraEntity.getId())
                .cantidad(detalleLevaduraEntity.getCantidad())
                .levadura(MapperLevadura.toDTO(detalleLevaduraEntity.getLevadura()))
                .build();
    }

    /**
     * Mapea una instancia de {@link PlanMonitoreoEtapaEntity} a {@link PlanMonitoreoEtapaResponseDTO},
     * incluyendo la etapa de control y los detalles de parámetros de control asociados.
     *
     * @param planMonitoreoEtapaEntity Entidad de plan de monitoreo de etapa a convertir.
     * @return Objeto DTO correspondiente.
     */
    private static PlanMonitoreoEtapaResponseDTO mapPlanMonitoreoEtapa(PlanMonitoreoEtapaEntity planMonitoreoEtapaEntity) {
        return PlanMonitoreoEtapaResponseDTO.builder()
                .id(planMonitoreoEtapaEntity.getId())
                .etapaControl(MapperEtapaControl.toDTO(planMonitoreoEtapaEntity.getEtapaControl()))
                .detallesParametroControl(planMonitoreoEtapaEntity.getDetallesParametroControl().stream()
                        .map(MapperReceta::mapDetalleParametroControl)
                        .toList())
                .build();
    }

    /**
     * Mapea una instancia de {@link DetalleParametroControlEntity} a {@link DetalleParametroControlResponseDTO},
     * incluyendo la información del parámetro de control asociado.
     *
     * @param detalleParametroControlEntity Entidad de detalle de parámetro de control a convertir.
     * @return Objeto DTO correspondiente.
     */
    private static DetalleParametroControlResponseDTO mapDetalleParametroControl(DetalleParametroControlEntity detalleParametroControlEntity) {
        return DetalleParametroControlResponseDTO.builder()
                .id(detalleParametroControlEntity.getId())
                .valorMinimo(detalleParametroControlEntity.getValorMinimo())
                .valorMaximo(detalleParametroControlEntity.getValorMaximo())
                .valorIdeal(detalleParametroControlEntity.getValorIdeal())
                .parametroControl(MapperParametroControl.toDTO(detalleParametroControlEntity.getParametroControl()))
                .build();
    }
}
