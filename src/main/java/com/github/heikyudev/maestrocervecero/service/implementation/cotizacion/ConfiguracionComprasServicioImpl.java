package com.github.heikyudev.maestrocervecero.service.implementation.cotizacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.ConfiguracionComprasEntity;
import com.github.heikyudev.maestrocervecero.persistence.repository.cotizacion.IConfiguracionComprasRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.cotizacion.ConfiguracionComprasFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.cotizacion.IConfiguracionComprasServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.ConfiguracionComprasResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.cotizacion.MapperConfiguracionCompras;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesNumericas;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de {@link IConfiguracionComprasServicio}.
 */
@Service
@RequiredArgsConstructor
public class ConfiguracionComprasServicioImpl implements IConfiguracionComprasServicio {

    static final String MENSAJE_PESO_FUERA_DE_RANGO = "El peso del precio debe estar entre 0 y 100";

    private final IConfiguracionComprasRepository configuracionComprasRepository;

    @Override
    @Transactional(readOnly = true)
    public ConfiguracionComprasResponseDTO buscarConfiguracion() {
        return MapperConfiguracionCompras.toDTO(buscarEntidadSingleton());
    }

    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.CONFIGURACION_COMPRAS)
    public ConfiguracionComprasResponseDTO actualizarConfiguracion(ConfiguracionComprasFormDTO configuracionComprasFormDTO) {
        // 1. Validar que se hayan informado los datos y todos los parámetros
        if (configuracionComprasFormDTO == null) {
            throw new ReglaNegocioException("Los datos de la configuración de compras son obligatorios");
        }
        if (configuracionComprasFormDTO.getPlazoRespuestaHoras() == null) {
            throw new ReglaNegocioException("El plazo de respuesta es obligatorio");
        }
        if (configuracionComprasFormDTO.getCantidadPlanes() == null) {
            throw new ReglaNegocioException("La cantidad de planes es obligatoria");
        }
        if (configuracionComprasFormDTO.getCriterioPlanCompra() == null) {
            throw new ReglaNegocioException("El criterio de los planes es obligatorio");
        }
        if (configuracionComprasFormDTO.getPesoPrecio() == null) {
            throw new ReglaNegocioException("El peso del precio es obligatorio");
        }

        // 2. Validar el plazo de respuesta y la cantidad de planes: enteros mayores a 0 que no superan su máximo
        ValidacionesNumericas.validarEnteroMayorACeroConMaximo(configuracionComprasFormDTO.getPlazoRespuestaHoras(),
                LimitesTecnicos.PLAZO_RESPUESTA_COTIZACION_MAXIMO_HORAS, "El plazo de respuesta", "horas");
        ValidacionesNumericas.validarEnteroMayorACeroConMaximo(configuracionComprasFormDTO.getCantidadPlanes(),
                LimitesTecnicos.CANTIDAD_PLANES_COMPRA_MAXIMA, "La cantidad de planes", "planes");

        // 3. Validar que el peso del precio sea un número entre 0 y 100 (ambos inclusive)
        double peso = configuracionComprasFormDTO.getPesoPrecio();
        if (!Double.isFinite(peso) || peso < 0 || peso > LimitesTecnicos.PESO_PRECIO_PLAN_COMPRA_MAXIMO) {
            throw new ReglaNegocioException(MENSAJE_PESO_FUERA_DE_RANGO);
        }

        // 4. Aplicar los valores sobre la fila única y persistirla
        ConfiguracionComprasEntity configuracion = buscarEntidadSingleton();
        configuracion.setPlazoRespuestaHoras(configuracionComprasFormDTO.getPlazoRespuestaHoras());
        configuracion.setCantidadPlanes(configuracionComprasFormDTO.getCantidadPlanes());
        configuracion.setCriterioPlanCompra(configuracionComprasFormDTO.getCriterioPlanCompra());
        configuracion.setPesoPrecio(peso);
        return MapperConfiguracionCompras.toDTO(configuracionComprasRepository.save(configuracion));
    }

    /**
     * Busca la única fila de configuración de compras. Existe siempre: la crea la migración V30.
     */
    private ConfiguracionComprasEntity buscarEntidadSingleton() {
        return configuracionComprasRepository.findById(ConfiguracionComprasEntity.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("No se encontró la configuración de compras: la aplicación no inicializó correctamente"));
    }
}
