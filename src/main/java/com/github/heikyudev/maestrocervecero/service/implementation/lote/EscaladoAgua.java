package com.github.heikyudev.maestrocervecero.service.implementation.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.OllaHervorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.VolumenesAgua;
import com.github.heikyudev.maestrocervecero.util.validacion.Redondeo;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesNumericas;

import java.util.ArrayList;
import java.util.List;

/**
 * Motor del escalado de agua (ver docs/Dominio/Escalado/EscaladoDelAgua.md): fórmulas puras, sin acceso a datos.
 * <p>
 * Lo usan el registro del lote, que valida la capacidad de la olla y del macerador con los Pasos 2 y 3, y el
 * cálculo del agua de un lote, que completa los Pasos 4 y 5. Todos los volúmenes están en litros.
 * </p>
 */
public final class EscaladoAgua {

    // Constantes fijas del sistema (ver docs/Dominio/Escalado/EscaladoDelAgua.md)
    private static final double DESPLAZAMIENTO_GRANO_L_POR_KG = 0.7;
    private static final double ABSORCION_GRANO_L_POR_KG = 1.0;
    private static final double MINUTOS_POR_HORA = 60.0;

    private EscaladoAgua() {
    }

    /**
     * Calcula el volumen que debe reunirse en la olla antes de empezar a hervir (V_pre-hervor), sumando al volumen
     * objetivo la pérdida por trub y la evaporación estimada durante el hervor (Paso 2).
     */
    public static double calcularVolumenPreHervor(double volumenObjetivo, OllaHervorEntity ollaHervor, VersionRecetaEntity versionReceta) {
        double horasHervor = versionReceta.getDuracionHervido() / MINUTOS_POR_HORA;
        return volumenObjetivo + ollaHervor.getPerdidaPorTrub() + (ollaHervor.getEvaporacion() * horasHervor);
    }

    /**
     * Calcula el agua de la maceración (V_agua_mac): la masa de malta por la relación de empaste de la receta, más el
     * agua retenida en el espacio muerto del macerador (Paso 3).
     */
    public static double calcularAguaMacerado(double masaMalta, MaceradorEntity macerador, VersionRecetaEntity versionReceta) {
        return (masaMalta * versionReceta.getRelacionDeEmpaste()) + macerador.getEspacioMuerto();
    }

    /**
     * Calcula el volumen que va a ocupar la mezcla de agua y malta dentro del macerador (V_mezcla_mac): el agua de la
     * maceración más el desplazamiento físico del grano (Paso 3).
     */
    public static double calcularVolumenMezclaMacerador(double masaMalta, MaceradorEntity macerador, VersionRecetaEntity versionReceta) {
        return calcularAguaMacerado(masaMalta, macerador, versionReceta) + (masaMalta * DESPLAZAMIENTO_GRANO_L_POR_KG);
    }

    /**
     * Calcula los volúmenes de agua de un lote (Pasos 2 a 5), redondeados a la escala de las cantidades.
     * <p>
     * El mosto extraído del macerador es el agua de la maceración menos el espacio muerto y menos el agua que retiene
     * el grano (masa de malta por la absorción del grano). El agua de lavado es la diferencia entre el volumen
     * pre-hervor y ese mosto; si resulta negativa (el mosto extraído ya supera el volumen pre-hervor) vale 0 y se
     * agrega una advertencia. El agua total es el agua de macerado más el agua de lavado.
     * </p>
     *
     * @param volumenObjetivo El volumen objetivo del lote, en litros.
     * @param masaMalta La masa total de malta escalada, en kilogramos.
     * @param macerador El macerador del lote.
     * @param ollaHervor La olla de hervor del lote.
     * @param versionReceta La versión de receta del lote.
     * @return Los volúmenes de agua del lote.
     * @throws ReglaNegocioException Si algún valor intermedio no es un número finito (por ejemplo, una receta o un
     *                               equipo con datos que producen una masa de malta infinita).
     */
    public static VolumenesAgua calcular(double volumenObjetivo, double masaMalta, MaceradorEntity macerador, OllaHervorEntity ollaHervor, VersionRecetaEntity versionReceta) {
        double volumenPreHervor = calcularVolumenPreHervor(volumenObjetivo, ollaHervor, versionReceta);
        double aguaMacerado = calcularAguaMacerado(masaMalta, macerador, versionReceta);
        double perdidaPorBagazo = masaMalta * ABSORCION_GRANO_L_POR_KG;
        double mostoExtraido = aguaMacerado - macerador.getEspacioMuerto() - perdidaPorBagazo;
        double aguaLavadoSinLimite = volumenPreHervor - mostoExtraido;

        if (!Double.isFinite(volumenPreHervor) || !Double.isFinite(aguaMacerado) || !Double.isFinite(mostoExtraido) || !Double.isFinite(aguaLavadoSinLimite)) {
            throw new ReglaNegocioException("No se puede calcular el agua del lote porque los datos de la receta o de los equipos producen un valor no válido");
        }

        List<String> advertencias = new ArrayList<>();
        double volumenPreHervorRedondeado = Redondeo.redondear(volumenPreHervor);
        double aguaMaceradoRedondeada = Redondeo.redondear(aguaMacerado);
        double aguaLavado = Redondeo.redondear(aguaLavadoSinLimite);
        if (aguaLavado < 0) {
            advertencias.add("El mosto extraído del macerador (" + ValidacionesNumericas.formatearNumero(Redondeo.redondear(mostoExtraido))
                    + " L) supera el volumen pre-hervor (" + ValidacionesNumericas.formatearNumero(volumenPreHervorRedondeado)
                    + " L), por lo que no se requiere agua de lavado");
            aguaLavado = 0.0;
        } else {
            // Evita un -0.0 por redondeo
            aguaLavado = Math.abs(aguaLavado);
        }
        double aguaTotal = Redondeo.redondear(aguaMacerado + Math.max(0.0, aguaLavadoSinLimite));

        return new VolumenesAgua(volumenPreHervorRedondeado, aguaMaceradoRedondeada, aguaLavado, aguaTotal, List.copyOf(advertencias));
    }
}
