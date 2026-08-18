package com.github.heikyudev.maestrocervecero.configuration.app;

import com.github.heikyudev.maestrocervecero.persistence.entity.orden_produccion.ConfiguracionOrdenProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.repository.orden_produccion.IConfiguracionOrdenProduccionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConfiguracionOrdenProduccionDataLoader implements CommandLineRunner {

    private final IConfiguracionOrdenProduccionRepository configuracionOrdenProduccionRepository;

    @Override
    @Transactional
    public void run(String... args) {
        // Idempotente: si la configuración singleton ya existe (id fijo), no hace nada.
        // Sin este chequeo, cada restart de la app (ej. con Spring DevTools) insertaría
        // una fila nueva.
        if (configuracionOrdenProduccionRepository.existsById(ConfiguracionOrdenProduccionEntity.SINGLETON_ID)) {
            log.info(">>> Configuracion de orden de produccion ya existe, no se recrea.");
            return;
        }

        configuracionOrdenProduccionRepository.save(ConfiguracionOrdenProduccionEntity.builder()
                .id(ConfiguracionOrdenProduccionEntity.SINGLETON_ID)
                .capacidadLoteEstandar(300.0)      // Lotes de 300 Litros
                .tiempoEstandarCip(6.0)            // 6 horas entre lote y lote
                .velocidadEstandarEnvasado(100.0)  // 100 Litros por hora
                .velocidadEstandarMolienda(200.0)  // 200 Kilogramos por hora
                .build());

        log.info(">>> Configuracion de orden de produccion creada exitosamente por defecto.");
    }
}