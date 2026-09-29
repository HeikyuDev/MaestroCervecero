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
     * @throws ReglaNegocioException Si algún parámetro no fue informado, si alguna velocidad o capacidad no es mayor a cero, o si el porcentaje mínimo de consumo no está entre 0 y 100.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.CONFIGURACION_PRODUCCION)
    public ConfiguracionProduccionResponseDTO actualizarConfiguracion(ConfiguracionProduccionFormDTO configuracionProduccionFormDTO) {
        // 1. Validar que se hayan informado todos los parámetros
        if (configuracionProduccionFormDTO.getVelocidadEstandarMolienda() == null
                || configuracionProduccionFormDTO.getVelocidadEstandarEnvasado() == null
                || configuracionProduccionFormDTO.getCapacidadLoteEstandar() == null
                || configuracionProduccionFormDTO.getPorcentajeMinimoConsumoParaAvanzarEtapa() == null
                || configuracionProduccionFormDTO.getCriterioSeleccionPlanSecuencial() == null
                || configuracionProduccionFormDTO.getCriterioSeleccionPlanConcurrente() == null) {
            throw new ReglaNegocioException("Todos los parámetros de configuración son obligatorios");
        }

        // 2. Validar que las velocidades y la capacidad de lote sean mayores a cero
        if (configuracionProduccionFormDTO.getVelocidadEstandarMolienda() <= 0
                || configuracionProduccionFormDTO.getVelocidadEstandarEnvasado() <= 0
                || configuracionProduccionFormDTO.getCapacidadLoteEstandar() <= 0) {
            throw new ReglaNegocioException("Las velocidades y la capacidad de lote estándar deben ser mayores a cero");
        }

        // 3. Validar que el porcentaje mínimo de consumo para avanzar de etapa esté entre 0 y 100
        if (configuracionProduccionFormDTO.getPorcentajeMinimoConsumoParaAvanzarEtapa() < 0 || configuracionProduccionFormDTO.getPorcentajeMinimoConsumoParaAvanzarEtapa() > 100) {
            throw new ReglaNegocioException("El porcentaje mínimo de consumo para avanzar de etapa debe estar entre 0 y 100");
        }

        // 4. Localizar la configuración vigente, aplicar los nuevos valores y persistirla
        ConfiguracionProduccionEntity configuracionProduccionEntity = buscarEntidadSingleton();
        configuracionProduccionEntity.setVelocidadEstandarMolienda(configuracionProduccionFormDTO.getVelocidadEstandarMolienda());
        configuracionProduccionEntity.setVelocidadEstandarEnvasado(configuracionProduccionFormDTO.getVelocidadEstandarEnvasado());
        configuracionProduccionEntity.setCapacidadLoteEstandar(configuracionProduccionFormDTO.getCapacidadLoteEstandar());
        configuracionProduccionEntity.setPorcentajeMinimoConsumoParaAvanzarEtapa(configuracionProduccionFormDTO.getPorcentajeMinimoConsumoParaAvanzarEtapa());
        configuracionProduccionEntity.setCriterioSeleccionPlanSecuencial(configuracionProduccionFormDTO.getCriterioSeleccionPlanSecuencial());
        configuracionProduccionEntity.setCriterioSeleccionPlanConcurrente(configuracionProduccionFormDTO.getCriterioSeleccionPlanConcurrente());

        return MapperConfiguracionProduccion.toDTO(configuracionProduccionRepository.save(configuracionProduccionEntity));
    }

    /**
     * Busca la única fila de configuración de producción. Existe siempre: la crea
     * {@code ConfiguracionProduccionDataLoader} al arrancar la aplicación.
     */
    private ConfiguracionProduccionEntity buscarEntidadSingleton() {
        return configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("No se encontró la configuración de producción: la aplicación no inicializó correctamente"));
    }
}
