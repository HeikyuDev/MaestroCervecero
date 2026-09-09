package com.github.heikyudev.maestrocervecero.configuration.app;

import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.ConfiguracionPlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IConfiguracionPlanificacionProduccionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConfiguracionPlanificacionProduccionDataLoader implements CommandLineRunner {

    private final IConfiguracionPlanificacionProduccionRepository configuracionPlanificacionProduccionRepository;

    @Override
    @Transactional
    public void run(String... args) {
        // Idempotente: si la configuración singleton ya existe (id fijo), no hace nada.
        // Sin este chequeo, cada restart de la app (ej. con Spring DevTools) insertaría
        // una fila nueva.
        if (configuracionPlanificacionProduccionRepository.existsById(ConfiguracionPlanificacionProduccionEntity.SINGLETON_ID)) {
            log.info(">>> Configuracion de planificacion de produccion ya existe, no se recrea.");
            return;
        }

        configuracionPlanificacionProduccionRepository.save(ConfiguracionPlanificacionProduccionEntity.builder()
                .id(ConfiguracionPlanificacionProduccionEntity.SINGLETON_ID)
                .capacidadLoteEstandar(300.0)      // Lotes de 300 Litros
                .tiempoEstandarCip(6.0)            // 6 horas entre lote y lote
                .velocidadEstandarEnvasado(100.0)  // 100 Litros por hora
                .velocidadEstandarMolienda(200.0)  // 200 Kilogramos por hora
                .build());

        log.info(">>> Configuracion de planificacion de produccion creada exitosamente por defecto.");
    }
}