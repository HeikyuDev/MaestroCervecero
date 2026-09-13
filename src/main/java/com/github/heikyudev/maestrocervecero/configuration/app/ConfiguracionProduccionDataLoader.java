package com.github.heikyudev.maestrocervecero.configuration.app;

import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.ConfiguracionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.CriterioSeleccionPlanConcurrente;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.CriterioSeleccionPlanSecuencial;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IConfiguracionProduccionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConfiguracionProduccionDataLoader implements CommandLineRunner {

    private final IConfiguracionProduccionRepository configuracionProduccionRepository;

    @Override
    @Transactional
    public void run(String... args) {
        // Idempotente: si la configuración singleton ya existe (id fijo), no hace nada.
        // Sin este chequeo, cada restart de la app (ej. con Spring DevTools) insertaría
        // una fila nueva.
        if (configuracionProduccionRepository.existsById(ConfiguracionProduccionEntity.SINGLETON_ID)) {
            log.info(">>> Configuracion de produccion ya existe, no se recrea.");
            return;
        }

        configuracionProduccionRepository.save(ConfiguracionProduccionEntity.builder()
                .id(ConfiguracionProduccionEntity.SINGLETON_ID)
                // Seccion 1: estimacion de fecha de finalizacion
                .capacidadLoteEstandar(300.0)      // Lotes de 300 Litros
                .tiempoEstandarCip(6.0)            // 6 horas entre lote y lote
                .velocidadEstandarEnvasado(100.0)  // 100 Litros por hora
                .velocidadEstandarMolienda(200.0)  // 200 Kilogramos por hora
                // Seccion 2: lotes
                .porcentajeMinimoConsumoParaAvanzarEtapa(80.0) // 80% de lo requerido, por defecto
                // Seccion 3: generacion automatica de planes de produccion
                .criterioSeleccionPlanSecuencial(CriterioSeleccionPlanSecuencial.FERMENTADOR_LIBERACION_MAS_TEMPRANA)
                .criterioSeleccionPlanConcurrente(CriterioSeleccionPlanConcurrente.EQUIPOS_LIBERACION_MAS_TEMPRANA)
                .build());

        log.info(">>> Configuracion de produccion creada exitosamente por defecto.");
    }
}
