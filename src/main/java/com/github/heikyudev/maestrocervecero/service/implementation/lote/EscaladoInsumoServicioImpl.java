package com.github.heikyudev.maestrocervecero.service.implementation.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleLevaduraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleLupuloEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleMaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.UsoLupulo;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.IEscaladoInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.RequerimientoInsumo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación de {@link IEscaladoInsumoServicio}.
 */
@Service
public class EscaladoInsumoServicioImpl implements IEscaladoInsumoServicio {

    private static final double REFERENCIA_AZUCAR_PURA_PUNTOS_POR_KG_POR_L = 384.0;

    // Constantes fijas de la fórmula de Tinseth (ver docs/Dominio/Escalado/EscaladoDelLupulo.md)
    private static final double TINSETH_CONSTANTE_FACTOR_DENSIDAD = 1.65;
    private static final double TINSETH_BASE_FACTOR_DENSIDAD = 0.000125;
    private static final double TINSETH_CONSTANTE_DECAIMIENTO_TIEMPO = 0.04;
    private static final double TINSETH_DIVISOR_FACTOR_TIEMPO = 4.15;

    // Divisor de conversión de puntos de gravedad a grados Plato (ver EscaladoDeLevadura.md)
    private static final double DIVISOR_GRADOS_PLATO = 4.0;

    @Override
    public List<RequerimientoInsumo> calcularRequerimientosMalta(VersionRecetaEntity versionReceta, double volumenObjetivo, MaceradorEntity macerador) {
        List<DetalleMaltaEntity> detallesMalta = versionReceta.getDetallesMalta();
        double cantidadTotalBase = detallesMalta.stream().mapToDouble(DetalleMaltaEntity::getCantidad).sum();

        // Paso 2: proporción de cada malta sobre el total base de la receta.
        double extractoPotencialPromedio = detallesMalta.stream()
                .mapToDouble(detalle -> (detalle.getCantidad() / cantidadTotalBase) * (detalle.getMalta().getPotencialExtracto() / 100.0))
                .sum();
        // (el mapToDouble de arriba ya combina el Paso 2 con el Paso 3: extracto potencial promedio
        // de la mezcla, ponderado por la proporción de cada malta)

        // Paso 1: puntos de densidad objetivo, a partir del OG objetivo y el volumen del lote.
        double puntosDensidadObjetivo = (versionReceta.getOgObjetivo() - 1) * 1000 * volumenObjetivo;
        // Paso 4: kilos totales de malta necesarios para el lote completo.
        double kgMaltaTotal = puntosDensidadObjetivo / (extractoPotencialPromedio * (macerador.getEficienciaMaceracion() / 100.0) * REFERENCIA_AZUCAR_PURA_PUNTOS_POR_KG_POR_L);

        // Paso 5: reparto del total entre las maltas, manteniendo la proporción de la receta.
        return detallesMalta.stream()
                .map(detalle -> new RequerimientoInsumo(detalle.getMalta(), null, kgMaltaTotal * (detalle.getCantidad() / cantidadTotalBase)))
                .toList();
    }

    @Override
    public List<RequerimientoInsumo> calcularRequerimientosLupulo(VersionRecetaEntity versionReceta, double volumenObjetivo, LoteEntity lote) {
        List<DetalleLupuloEntity> detallesLupulo = versionReceta.getDetallesLupulo();
        List<RequerimientoInsumo> requerimientos = new ArrayList<>();

        List<DetalleLupuloEntity> detallesHervor = detallesLupulo.stream()
                .filter(detalle -> detalle.getUso() == UsoLupulo.HERVOR)
                .toList();

        if (!detallesHervor.isEmpty()) {
            double cantidadTotalBaseHervor = detallesHervor.stream().mapToDouble(DetalleLupuloEntity::getCantidad).sum();
            // Paso 1: Factor de Densidad, común a todos los lúpulos de HERVOR de este lote
            // (depende solo del OG objetivo, no de cada lúpulo en particular).
            double factorDensidad = TINSETH_CONSTANTE_FACTOR_DENSIDAD * Math.pow(TINSETH_BASE_FACTOR_DENSIDAD, versionReceta.getOgObjetivo() - 1);

            // Pasos 2 a 4: para cada lúpulo de HERVOR se calcula su Factor de Tiempo y su
            // Utilización individual (Paso 2), su proporción sobre el total de HERVOR (Paso 3), y
            // se suma todo ponderado por AA% en la constante K de la mezcla (Paso 4).
            double k = detallesHervor.stream()
                    .mapToDouble(detalle -> {
                        double proporcion = detalle.getCantidad() / cantidadTotalBaseHervor;
                        double factorTiempo = (1 - Math.exp(-TINSETH_CONSTANTE_DECAIMIENTO_TIEMPO * detalle.getTiempoDeHervor())) / TINSETH_DIVISOR_FACTOR_TIEMPO;
                        double utilizacion = factorDensidad * factorTiempo * detalle.getLupulo().getFormato().getFactorCorreccionUtilizacion();
                        return proporcion * (detalle.getLupulo().getAa() / 100.0) * utilizacion;
                    })
                    .sum();

            // Paso 5: gramos totales de lúpulo de HERVOR necesarios para alcanzar el IBU objetivo.
            double gramosTotalHervor = (versionReceta.getIbuObjetivo() * volumenObjetivo) / (1000 * k);

            // Paso 6: reparto del total entre los lúpulos de HERVOR, manteniendo su proporción.
            for (DetalleLupuloEntity detalle : detallesHervor) {
                double proporcion = detalle.getCantidad() / cantidadTotalBaseHervor;
                requerimientos.add(new RequerimientoInsumo(detalle.getLupulo(), obtenerEtapaPorTipo(lote, detalle.getEtapaDeUso()), gramosTotalHervor * proporcion));
            }
        }

        // Paso 7: los lúpulos de WHIRLPOOL/DRY_HOP no aportan amargor de forma significativa, así
        // que no usan Tinseth — escalan linealmente respecto al volumen base de la receta.
        detallesLupulo.stream()
                .filter(detalle -> detalle.getUso() != UsoLupulo.HERVOR)
                .forEach(detalle -> requerimientos.add(new RequerimientoInsumo(detalle.getLupulo(), obtenerEtapaPorTipo(lote, detalle.getEtapaDeUso()),
                        detalle.getCantidad() * (volumenObjetivo / versionReceta.getVolumenBase()))));

        return requerimientos;
    }

    @Override
    public List<RequerimientoInsumo> calcularRequerimientosLevadura(VersionRecetaEntity versionReceta, double volumenObjetivo) {
        List<DetalleLevaduraEntity> detallesLevadura = versionReceta.getDetallesLevadura();
        double cantidadTotalBase = detallesLevadura.stream().mapToDouble(DetalleLevaduraEntity::getCantidad).sum();

        // Paso 1: conversión del OG objetivo a grados Plato.
        double gradosPlato = ((versionReceta.getOgObjetivo() - 1) * 1000) / DIVISOR_GRADOS_PLATO;
        // Pasos 2 y 3: proporción de cada levadura sobre el total base, y Tasa de Inoculación
        // promedio ponderada de la mezcla.
        double tasaPromedio = detallesLevadura.stream()
                .mapToDouble(detalle -> (detalle.getCantidad() / cantidadTotalBase) * detalle.getLevadura().getTipo().getTasaInoculacion())
                .sum();

        // Paso 4: cantidad total de células viables necesarias, en millones.
        double volumenObjetivoMl = volumenObjetivo * 1000;
        double celulasTotalesMillones = volumenObjetivoMl * gradosPlato * tasaPromedio;

        return detallesLevadura.stream()
                .map(detalle -> {
                    // Paso 5: reparto de las células totales entre las levaduras de la receta.
                    double proporcion = detalle.getCantidad() / cantidadTotalBase;
                    double celulasMillones = celulasTotalesMillones * proporcion;
                    // Paso 6: conversión a gramos según la concentración celular propia de cada levadura.
                    double gramos = (celulasMillones * 1_000_000) / detalle.getLevadura().getCantidadCelulasPorGramo();
                    return new RequerimientoInsumo(detalle.getLevadura(), null, gramos);
                })
                .toList();
    }

    @Override
    public List<RequerimientoInsumo> asociarEtapa(List<RequerimientoInsumo> requerimientos, EtapaLoteEntity etapa) {
        return requerimientos.stream()
                .map(requerimiento -> new RequerimientoInsumo(requerimiento.insumo(), etapa, requerimiento.cantidadRequerida()))
                .toList();
    }

    @Override
    public EtapaLoteEntity obtenerEtapaPorTipo(LoteEntity lote, TipoEtapa tipo) {
        return lote.getEtapas().stream()
                .filter(etapa -> etapa.getEtapa() == tipo)
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("El lote no tiene una etapa de " + tipo));
    }

    @Override
    public List<RequerimientoInsumo> calcularRequerimientosTotales(LoteEntity lote) {
        VersionRecetaEntity versionReceta = lote.getPlanificacionProduccion().getVersionReceta();
        double volumenObjetivo = lote.getVolumenObjetivo();
        EtapaLoteEntity etapaMaceracion = obtenerEtapaPorTipo(lote, TipoEtapa.MACERACION);
        EtapaLoteEntity etapaFermentacion = obtenerEtapaPorTipo(lote, TipoEtapa.FERMENTACION);

        List<RequerimientoInsumo> todosLosRequerimientos = new ArrayList<>();
        todosLosRequerimientos.addAll(asociarEtapa(calcularRequerimientosMalta(versionReceta, volumenObjetivo, resolverMacerador(etapaMaceracion)), etapaMaceracion));
        todosLosRequerimientos.addAll(calcularRequerimientosLupulo(versionReceta, volumenObjetivo, lote));
        todosLosRequerimientos.addAll(asociarEtapa(calcularRequerimientosLevadura(versionReceta, volumenObjetivo), etapaFermentacion));

        return fusionarPorInsumoYEtapa(todosLosRequerimientos);
    }

    @Override
    public List<RequerimientoInsumo> calcularRequerimientosEtapa(EtapaLoteEntity etapaLote) {
        LoteEntity lote = etapaLote.getLote();
        VersionRecetaEntity versionReceta = lote.getPlanificacionProduccion().getVersionReceta();
        double volumenObjetivo = lote.getVolumenObjetivo();
        TipoEtapa tipoEtapa = etapaLote.getEtapa();

        List<RequerimientoInsumo> requerimientos = new ArrayList<>();

        // La malta solo se requiere en Maceración: si la etapa pedida es otra, ni siquiera se
        // resuelve el macerador ni se calcula el escalado de malta.
        if (tipoEtapa == TipoEtapa.MACERACION) {
            requerimientos.addAll(asociarEtapa(calcularRequerimientosMalta(versionReceta, volumenObjetivo, resolverMacerador(etapaLote)), etapaLote));
        }

        // La levadura solo se requiere en Fermentación.
        if (tipoEtapa == TipoEtapa.FERMENTACION) {
            requerimientos.addAll(asociarEtapa(calcularRequerimientosLevadura(versionReceta, volumenObjetivo), etapaLote));
        }

        // El lúpulo puede requerirse en cualquier etapa (HERVOR típicamente en Hervido, WHIRLPOOL/
        // DRY_HOP según lo configurado en cada detalle): solo se dispara el cálculo completo (que
        // internamente agrupa TODOS los lúpulos de HERVOR de la receta para la fórmula de Tinseth)
        // si algún detalle de la receta efectivamente apunta a esta etapa; de lo contrario se evita
        // por completo.
        boolean requiereLupuloEnEstaEtapa = versionReceta.getDetallesLupulo().stream()
                .anyMatch(detalle -> detalle.getEtapaDeUso() == tipoEtapa);
        if (requiereLupuloEnEstaEtapa) {
            requerimientos.addAll(calcularRequerimientosLupulo(versionReceta, volumenObjetivo, lote).stream()
                    .filter(requerimiento -> requerimiento.etapa().getId().equals(etapaLote.getId()))
                    .toList());
        }

        return fusionarPorInsumoYEtapa(requerimientos);
    }

    /**
     * Resuelve el {@link MaceradorEntity} asociado a una etapa de Maceración, navegando la
     * relación directa de la etapa (join de herencia {@code EquipamientoEntity} → {@code
     * MaceradorEntity}). Este cálculo es de solo lectura, así que no corresponde bloquearlo con el
     * mismo lock pesimista que usa el flujo transaccional de inicio de lote.
     *
     * @param etapaMaceracion La etapa de Maceración del lote.
     * @return El macerador asociado a esa etapa.
     * @throws ReglaNegocioException Si el equipamiento asociado a la etapa no es un macerador (dato
     *                               corrupto: no debería ocurrir bajo el invariante del dominio).
     */
    private MaceradorEntity resolverMacerador(EtapaLoteEntity etapaMaceracion) {
        if (etapaMaceracion.getEquipamiento() instanceof MaceradorEntity macerador) {
            return macerador;
        }
        throw new ReglaNegocioException("La etapa de Maceración del lote " + etapaMaceracion.getLote().getId()
                + " no tiene un macerador asociado como equipamiento");
    }

    /**
     * Unifica los requerimientos POR INSUMO Y POR ETAPA: un mismo insumo (típicamente un lúpulo)
     * puede usarse en más de una etapa dentro de la misma receta (por ejemplo, HERVOR en Hervido y
     * DRY_HOP en Maduración), y cada uso se mantiene atado a su propia etapa — nunca se fusionan
     * entre etapas distintas, solo dentro de la misma.
     */
    private List<RequerimientoInsumo> fusionarPorInsumoYEtapa(List<RequerimientoInsumo> requerimientos) {
        Map<String, RequerimientoInsumo> requerimientosPorInsumoYEtapa = new LinkedHashMap<>();
        for (RequerimientoInsumo requerimiento : requerimientos) {
            String clave = requerimiento.insumo().getId() + "-" + requerimiento.etapa().getId();
            requerimientosPorInsumoYEtapa.merge(clave, requerimiento,
                    (existente, nuevo) -> new RequerimientoInsumo(existente.insumo(), existente.etapa(), existente.cantidadRequerida() + nuevo.cantidadRequerida()));
        }
        return List.copyOf(requerimientosPorInsumoYEtapa.values());
    }
}
