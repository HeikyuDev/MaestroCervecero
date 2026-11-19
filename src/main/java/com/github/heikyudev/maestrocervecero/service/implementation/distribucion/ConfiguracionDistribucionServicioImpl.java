package com.github.heikyudev.maestrocervecero.service.implementation.distribucion;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.distribucion.ConfiguracionDistribucionEntity;
import com.github.heikyudev.maestrocervecero.persistence.repository.distribucion.IConfiguracionDistribucionRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.distribucion.ConfiguracionDistribucionFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.distribucion.IConfiguracionDistribucionServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.distribucion.ConfiguracionDistribucionResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.distribucion.MapperConfiguracionDistribucion;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de {@link IConfiguracionDistribucionServicio}.
 */
@Service
@RequiredArgsConstructor
public class ConfiguracionDistribucionServicioImpl implements IConfiguracionDistribucionServicio {

    static final String MENSAJE_DIAS_FUERA_DE_RANGO = "Los días máximos de anticipación del retiro deben estar entre "
            + LimitesTecnicos.DIAS_ANTICIPACION_RETIRO_MINIMO + " y " + LimitesTecnicos.DIAS_ANTICIPACION_RETIRO_MAXIMO;

    private final IConfiguracionDistribucionRepository configuracionDistribucionRepository;

    @Override
    @Transactional(readOnly = true)
    public ConfiguracionDistribucionResponseDTO buscarConfiguracion() {
        return MapperConfiguracionDistribucion.toDTO(buscarEntidadSingleton());
    }

    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.CONFIGURACION_DISTRIBUCION)
    public ConfiguracionDistribucionResponseDTO actualizarConfiguracion(ConfiguracionDistribucionFormDTO configuracionDistribucionFormDTO) {
        // 1. Validar que se hayan informado los datos y todos los parámetros
        if (configuracionDistribucionFormDTO == null) {
            throw new ReglaNegocioException("Los datos de la configuración de distribución son obligatorios");
        }
        if (configuracionDistribucionFormDTO.getHoraEnvioRecordatorioRetiro() == null) {
            throw new ReglaNegocioException("La hora de envío del recordatorio es obligatoria");
        }
        if (configuracionDistribucionFormDTO.getDiasMaximosAnticipacionRetiro() == null) {
            throw new ReglaNegocioException("Los días máximos de anticipación del retiro son obligatorios");
        }

        // 2. Validar que los días máximos de anticipación estén dentro del rango permitido (ambos inclusive)
        int dias = configuracionDistribucionFormDTO.getDiasMaximosAnticipacionRetiro();
        if (dias < LimitesTecnicos.DIAS_ANTICIPACION_RETIRO_MINIMO || dias > LimitesTecnicos.DIAS_ANTICIPACION_RETIRO_MAXIMO) {
            throw new ReglaNegocioException(MENSAJE_DIAS_FUERA_DE_RANGO);
        }

        // 3. Aplicar los valores sobre la fila única y persistirla. La hora se guarda sin segundos: el formulario la pide
        //    en horas y minutos
        ConfiguracionDistribucionEntity configuracion = buscarEntidadSingleton();
        configuracion.setHoraEnvioRecordatorioRetiro(configuracionDistribucionFormDTO.getHoraEnvioRecordatorioRetiro().withSecond(0).withNano(0));
        configuracion.setDiasMaximosAnticipacionRetiro(dias);
        return MapperConfiguracionDistribucion.toDTO(configuracionDistribucionRepository.save(configuracion));
    }

    /**
     * Busca la única fila de configuración de distribución. Existe siempre: la crea la migración V35.
     */
    private ConfiguracionDistribucionEntity buscarEntidadSingleton() {
        return configuracionDistribucionRepository.findById(ConfiguracionDistribucionEntity.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("No se encontró la configuración de distribución: la aplicación no inicializó correctamente"));
    }
}
