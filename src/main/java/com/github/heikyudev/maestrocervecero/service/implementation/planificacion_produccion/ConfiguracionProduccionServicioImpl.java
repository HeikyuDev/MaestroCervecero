package com.github.heikyudev.maestrocervecero.service.implementation.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.ConfiguracionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IConfiguracionProduccionRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.ConfiguracionProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion.IConfiguracionProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.ConfiguracionProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.planificacion_produccion.MapperConfiguracionProduccion;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesNumericas;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConfiguracionProduccionServicioImpl implements IConfiguracionProduccionServicio {

    private final IConfiguracionProduccionRepository configuracionProduccionRepository;

    /**
     * Obtiene la configuración general de producción vigente.
     *
     * @return La configuración de producción actual.
     */
    @Override
    @Transactional(readOnly = true)
    public ConfiguracionProduccionResponseDTO buscarConfiguracion() {
        return MapperConfiguracionProduccion.toDTO(buscarEntidadSingleton());
    }

    /**
     * Actualiza la configuración general de producción.
     *
     * @param configuracionProduccionFormDTO Los nuevos valores de configuración.
     * @return La configuración de producción actualizada.
     * @throws ReglaNegocioException Si algún parámetro no fue informado, si la velocidad de envasado o la capacidad no es un número mayor a 0 o supera su máximo técnico, o si el porcentaje mínimo de consumo no es un número entre 0 y 100.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.CONFIGURACION_PRODUCCION)
    public ConfiguracionProduccionResponseDTO actualizarConfiguracion(ConfiguracionProduccionFormDTO configuracionProduccionFormDTO) {
        // 1. Validar que se hayan informado todos los parámetros
        if (configuracionProduccionFormDTO.getVelocidadEstandarEnvasado() == null) {
            throw new ReglaNegocioException("La velocidad de envasado es obligatoria");
        }
        if (configuracionProduccionFormDTO.getCapacidadLoteEstandar() == null) {
            throw new ReglaNegocioException("La capacidad de lote estándar es obligatoria");
        }
        if (configuracionProduccionFormDTO.getPorcentajeMinimoConsumoParaAvanzarEtapa() == null) {
            throw new ReglaNegocioException("El porcentaje mínimo de consumo es obligatorio");
        }
        if (configuracionProduccionFormDTO.getCriterioSeleccionPlanSecuencial() == null) {
            throw new ReglaNegocioException("El criterio de selección secuencial es obligatorio");
        }
        if (configuracionProduccionFormDTO.getCriterioSeleccionPlanConcurrente() == null) {
            throw new ReglaNegocioException("El criterio de selección concurrente es obligatorio");
        }

        // 2. Validar que la velocidad de envasado y la capacidad de lote sean mayores a 0
        if (configuracionProduccionFormDTO.getVelocidadEstandarEnvasado() <= 0) {
            throw new ReglaNegocioException("La velocidad de envasado debe ser mayor a 0");
        }
        if (configuracionProduccionFormDTO.getCapacidadLoteEstandar() <= 0) {
            throw new ReglaNegocioException("La capacidad de lote estándar debe ser mayor a 0");
        }
        ValidacionesNumericas.validarMayorACeroConMaximo(configuracionProduccionFormDTO.getVelocidadEstandarEnvasado(),
                LimitesTecnicos.VELOCIDAD_ENVASADO_MAXIMA_L_POR_HORA, "La velocidad de envasado", "litros por hora");
        ValidacionesNumericas.validarMayorACeroConMaximo(configuracionProduccionFormDTO.getCapacidadLoteEstandar(),
                LimitesTecnicos.CAPACIDAD_LOTE_ESTANDAR_MAXIMA_LITROS, "La capacidad de lote estándar", "litros");

        // 3. Validar que el porcentaje mínimo de consumo para avanzar de etapa esté entre 0 y 100
        if (configuracionProduccionFormDTO.getPorcentajeMinimoConsumoParaAvanzarEtapa() < 0 || configuracionProduccionFormDTO.getPorcentajeMinimoConsumoParaAvanzarEtapa() > 100) {
            throw new ReglaNegocioException("El porcentaje mínimo de consumo debe estar entre 0 y 100");
        }
        ValidacionesNumericas.validarMayorOIgualACero(configuracionProduccionFormDTO.getPorcentajeMinimoConsumoParaAvanzarEtapa(),
                "El porcentaje mínimo de consumo");

        // 4. Localizar la configuración vigente, aplicar los nuevos valores y persistirla
        ConfiguracionProduccionEntity configuracionProduccionEntity = buscarEntidadSingleton();
        configuracionProduccionEntity.setVelocidadEstandarEnvasado(configuracionProduccionFormDTO.getVelocidadEstandarEnvasado());
        configuracionProduccionEntity.setCapacidadLoteEstandar(configuracionProduccionFormDTO.getCapacidadLoteEstandar());
        configuracionProduccionEntity.setPorcentajeMinimoConsumoParaAvanzarEtapa(configuracionProduccionFormDTO.getPorcentajeMinimoConsumoParaAvanzarEtapa());
        configuracionProduccionEntity.setCriterioSeleccionPlanSecuencial(configuracionProduccionFormDTO.getCriterioSeleccionPlanSecuencial());
        configuracionProduccionEntity.setCriterioSeleccionPlanConcurrente(configuracionProduccionFormDTO.getCriterioSeleccionPlanConcurrente());

        return MapperConfiguracionProduccion.toDTO(configuracionProduccionRepository.save(configuracionProduccionEntity));
    }

    /**
     * Busca la única fila de configuración de producción. Existe siempre: la crea la
     * migración de datos iniciales de Flyway (V2__datos_iniciales.sql).
     */
    private ConfiguracionProduccionEntity buscarEntidadSingleton() {
        return configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("No se encontró la configuración de producción: la aplicación no inicializó correctamente"));
    }
}
