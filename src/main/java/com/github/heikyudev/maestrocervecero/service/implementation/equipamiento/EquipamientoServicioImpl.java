package com.github.heikyudev.maestrocervecero.service.implementation.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEtapaLoteRepository;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IEquipamientoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoResumenResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.ResumenEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.equipamiento.MapperEquipamiento;
import com.github.heikyudev.maestrocervecero.util.mapper.equipamiento.MapperEquipamientoResumen;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementación de {@link IEquipamientoServicio}.
 */
@Service
@RequiredArgsConstructor
public class EquipamientoServicioImpl implements IEquipamientoServicio {

    private final IEquipamientoRepository equipamientoRepository;
    private final IEtapaLoteRepository etapaLoteRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<EquipamientoResponseDTO> filtrarEquipamientos(String identificadorInterno, TipoEquipamiento tipo, EstadoOperativo estadoOperativo, Pageable pageable) {
        Class<? extends EquipamientoEntity> tipoClase = tipo != null ? tipo.getEntityClass() : null;
        return equipamientoRepository.filtrarEquipamientos(identificadorInterno, tipoClase, estadoOperativo, pageable)
                .map(MapperEquipamiento::toDTO);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Parte de un mapa con las 16 combinaciones (tipo x estado) en 0 y suma cada equipamiento
     * activo a la suya, así la vista no tiene que completar las combinaciones sin equipos.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    public ResumenEquipamientoResponseDTO obtenerResumen() {
        Map<TipoEquipamiento, Map<EstadoOperativo, Long>> cantidades = new EnumMap<>(TipoEquipamiento.class);
        for (TipoEquipamiento tipo : TipoEquipamiento.values()) {
            Map<EstadoOperativo, Long> porEstado = new EnumMap<>(EstadoOperativo.class);
            for (EstadoOperativo estado : EstadoOperativo.values()) {
                porEstado.put(estado, 0L);
            }
            cantidades.put(tipo, porEstado);
        }

        for (EquipamientoEntity equipamiento : equipamientoRepository.listarActivos()) {
            cantidades.get(MapperEquipamientoResumen.tipoDe(equipamiento))
                    .merge(equipamiento.getEstadoOperativo(), 1L, Long::sum);
        }

        return ResumenEquipamientoResponseDTO.builder().cantidadesPorTipoYEstado(cantidades).build();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Consulta las etapas de toda la página con una sola query (en lugar de una por equipo) y las
     * reparte: la {@code EN_CURSO} es la etapa en curso y las {@code PENDIENTE} son las reservadas.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    public Page<EquipamientoResumenResponseDTO> filtrarResumenEquipamientos(String identificadorInterno, TipoEquipamiento tipo, EstadoOperativo estadoOperativo, Pageable pageable) {
        Class<? extends EquipamientoEntity> tipoClase = tipo != null ? tipo.getEntityClass() : null;
        Page<EquipamientoEntity> equipamientos =
                equipamientoRepository.filtrarEquipamientos(identificadorInterno, tipoClase, estadoOperativo, pageable);

        List<Long> ids = equipamientos.getContent().stream().map(EquipamientoEntity::getId).toList();
        Map<Long, List<EtapaLoteEntity>> etapasPorEquipamiento = ids.isEmpty()
                ? Map.of()
                : etapaLoteRepository.buscarEtapasEnCursoOPendientes(ids).stream()
                        .collect(Collectors.groupingBy(etapa -> etapa.getEquipamiento().getId()));

        return equipamientos.map(equipamiento -> {
            List<EtapaLoteEntity> etapas = etapasPorEquipamiento.getOrDefault(equipamiento.getId(), List.of());
            EtapaLoteEntity enCurso = etapas.stream()
                    .filter(etapa -> etapa.getEstado() == EstadoEtapaLote.EN_CURSO)
                    .findFirst()
                    .orElse(null);
            List<EtapaLoteEntity> pendientes = etapas.stream()
                    .filter(etapa -> etapa.getEstado() == EstadoEtapaLote.PENDIENTE)
                    .toList();
            return MapperEquipamientoResumen.toDTO(equipamiento, enCurso, pendientes);
        });
    }
}
