package com.github.heikyudev.maestrocervecero.service.implementation.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.OllaHervorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.VolumenesAgua;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EscaladoAguaTest {

    private static final String MENSAJE_VALOR_NO_VALIDO = "No se puede calcular el agua del lote porque los datos de la receta o de los equipos producen un valor no válido";

    // ==================== calcular ====================

    @Test
    @DisplayName("CP-EA-01: calcular reproduce el ejemplo del documento (EscaladoDelAgua.md): 20 L, 4,3952 kg de malta, relación de empaste 3, espacio muerto 5 L, evaporación 3 L/h, trub 2 L y hervor de 60 minutos")
    void calcular_debeReproducirElEjemploDelDocumento() {
        // === PREPARACION DE DATOS ===
        double masaMalta = 1000.0 / (0.79 * 0.75 * 384); // masa del ejemplo de EscaladoDeMalta.md
        VersionRecetaEntity versionReceta = crearVersionReceta(3.0, 60);

        // === EJECUCION ===
        VolumenesAgua resultado = EscaladoAgua.calcular(20.0, masaMalta, crearMacerador(5.0), crearOllaHervor(3.0, 2.0), versionReceta);

        // === ASSERTS ===
        // Vpre = 20 + 2 + 3*60/60 = 25; Vagua_mac = 4,3952*3 + 5 = 18,1857; Pbagazo = 4,3952; Vmosto = 18,1857 - 5 - 4,3952 = 8,7904;
        // Vlavado = 25 - 8,7904 = 16,2096; Vtotal = 18,1857 + 16,2096 = 34,3952 (= Vpre + espacio muerto + Pbagazo)
        assertThat(resultado.volumenPreHervor()).isEqualTo(25.0);
        assertThat(resultado.aguaMacerado()).isEqualTo(18.1857);
        assertThat(resultado.aguaLavado()).isEqualTo(16.2096);
        assertThat(resultado.aguaTotal()).isEqualTo(34.3952);
        assertThat(resultado.advertencias()).isEmpty();
        assertThat(EscaladoAgua.calcularVolumenMezclaMacerador(masaMalta, crearMacerador(5.0), versionReceta)).isCloseTo(21.2623, org.assertj.core.api.Assertions.within(0.0001));
    }

    @Test
    @DisplayName("CP-EA-02: calcular con valores redondos: 5 kg, relación de empaste 3, espacio muerto 1,5 L, evaporación 4 L/h, trub 1 L y 60 minutos de hervor")
    void calcular_debeCalcularConValoresRedondos() {
        // === EJECUCION ===
        VolumenesAgua resultado = EscaladoAgua.calcular(20.0, 5.0, crearMacerador(1.5), crearOllaHervor(4.0, 1.0), crearVersionReceta(3.0, 60));

        // === ASSERTS ===
        // Vpre = 25; Vagua_mac = 15 + 1,5 = 16,5; Pbagazo = 5; Vmosto = 16,5 - 1,5 - 5 = 10; Vlavado = 15; Vtotal = 31,5
        assertThat(resultado.volumenPreHervor()).isEqualTo(25.0);
        assertThat(resultado.aguaMacerado()).isEqualTo(16.5);
        assertThat(resultado.aguaLavado()).isEqualTo(15.0);
        assertThat(resultado.aguaTotal()).isEqualTo(31.5);
        assertThat(resultado.advertencias()).isEmpty();
    }

    @Test
    @DisplayName("CP-EA-03: calcular con el mosto extraído mayor al volumen pre-hervor deja el agua de lavado en 0, suma solo el agua de macerado y advierte")
    void calcular_debeDejarElLavadoEnCeroYAdvertirSiSeraNegativo() {
        // === EJECUCION ===
        VolumenesAgua resultado = EscaladoAgua.calcular(20.0, 10.0, crearMacerador(1.0), crearOllaHervor(3.0, 1.0), crearVersionReceta(5.0, 60));

        // === ASSERTS ===
        // Vpre = 24; Vagua_mac = 51; Vmosto = 51 - 1 - 10 = 40; Vlavado = 24 - 40 = -16 → 0
        assertThat(resultado.volumenPreHervor()).isEqualTo(24.0);
        assertThat(resultado.aguaMacerado()).isEqualTo(51.0);
        assertThat(resultado.aguaLavado()).isEqualTo(0.0);
        assertThat(resultado.aguaTotal()).isEqualTo(51.0);
        assertThat(resultado.advertencias()).containsExactly(
                "El mosto extraído del macerador (40 L) supera el volumen pre-hervor (24 L), por lo que no se requiere agua de lavado");
    }

    @Test
    @DisplayName("CP-EA-04: calcular con el mosto extraído igual al volumen pre-hervor (límite) deja el agua de lavado en 0 sin advertencia")
    void calcular_noDebeAdvertirSiElMostoExtraidoIgualaElVolumenPreHervor() {
        // === EJECUCION ===
        VolumenesAgua resultado = EscaladoAgua.calcular(20.0, 4.0, crearMacerador(1.0), crearOllaHervor(3.0, 1.0), crearVersionReceta(7.0, 60));

        // === ASSERTS ===
        // Vpre = 24; Vagua_mac = 28 + 1 = 29; Vmosto = 29 - 1 - 4 = 24; Vlavado = 0
        assertThat(resultado.aguaMacerado()).isEqualTo(29.0);
        assertThat(resultado.aguaLavado()).isEqualTo(0.0);
        assertThat(resultado.aguaTotal()).isEqualTo(29.0);
        assertThat(resultado.advertencias()).isEmpty();
    }

    @Test
    @DisplayName("CP-EA-05: calcular con un agua de lavado negativa que se redondea a cero (-0,00001) la deja en 0 positivo y no advierte")
    void calcular_noDebeAdvertirSiElLavadoNegativoSeRedondeaACero() {
        // === EJECUCION ===
        VolumenesAgua resultado = EscaladoAgua.calcular(19.99999, 4.0, crearMacerador(1.0), crearOllaHervor(3.0, 1.0), crearVersionReceta(7.0, 60));

        // === ASSERTS ===
        assertThat(resultado.aguaLavado()).isEqualTo(0.0);
        assertThat(resultado.advertencias()).isEmpty();
    }

    @Test
    @DisplayName("CP-EA-06: calcular con masa de malta 0 (límite): el agua de macerado es el espacio muerto y el agua de lavado completa todo el volumen pre-hervor")
    void calcular_debeCalcularConMasaDeMaltaCero() {
        // === EJECUCION ===
        VolumenesAgua resultado = EscaladoAgua.calcular(20.0, 0.0, crearMacerador(1.0), crearOllaHervor(3.0, 1.0), crearVersionReceta(3.0, 60));

        // === ASSERTS ===
        assertThat(resultado.aguaMacerado()).isEqualTo(1.0);
        assertThat(resultado.aguaLavado()).isEqualTo(24.0);
        assertThat(resultado.aguaTotal()).isEqualTo(25.0);
        assertThat(resultado.advertencias()).isEmpty();
    }

    @Test
    @DisplayName("CP-EA-07: calcular con hervor de 0 minutos (límite): el volumen pre-hervor no suma evaporación")
    void calcular_noDebeSumarEvaporacionSinHervor() {
        // === EJECUCION ===
        VolumenesAgua resultado = EscaladoAgua.calcular(20.0, 5.0, crearMacerador(1.5), crearOllaHervor(4.0, 1.0), crearVersionReceta(3.0, 0));

        // === ASSERTS ===
        assertThat(resultado.volumenPreHervor()).isEqualTo(21.0);
    }

    @Test
    @DisplayName("CP-EA-08: calcular con una masa de malta NaN o infinita lanza ReglaNegocioException")
    void calcular_debeRechazarUnaMasaDeMaltaNoFinita() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMacerador(1.0);
        OllaHervorEntity ollaHervor = crearOllaHervor(3.0, 1.0);
        VersionRecetaEntity versionReceta = crearVersionReceta(3.0, 60);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> EscaladoAgua.calcular(20.0, Double.NaN, macerador, ollaHervor, versionReceta))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(MENSAJE_VALOR_NO_VALIDO);
        assertThatThrownBy(() -> EscaladoAgua.calcular(20.0, Double.POSITIVE_INFINITY, macerador, ollaHervor, versionReceta))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(MENSAJE_VALOR_NO_VALIDO);
    }

    @Test
    @DisplayName("CP-EA-09: calcular con un parámetro de la olla de hervor infinito lanza ReglaNegocioException")
    void calcular_debeRechazarUnParametroDeLaOllaNoFinito() {
        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> EscaladoAgua.calcular(20.0, 5.0, crearMacerador(1.0), crearOllaHervor(Double.POSITIVE_INFINITY, 1.0), crearVersionReceta(3.0, 60)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(MENSAJE_VALOR_NO_VALIDO);
    }

    // ==================== pasos 2 y 3 ====================

    @Test
    @DisplayName("CP-EA-10: calcularVolumenPreHervor y calcularVolumenMezclaMacerador resuelven los Pasos 2 y 3 del documento")
    void debeCalcularLosPasosDosYTres() {
        // === PREPARACION DE DATOS ===
        VersionRecetaEntity versionReceta = crearVersionReceta(3.0, 90);

        // === EJECUCION ===
        double volumenPreHervor = EscaladoAgua.calcularVolumenPreHervor(20.0, crearOllaHervor(4.0, 1.0), versionReceta);
        double volumenMezcla = EscaladoAgua.calcularVolumenMezclaMacerador(4.0, crearMacerador(5.0), versionReceta);

        // === ASSERTS ===
        // Vpre = 20 + 1 + 4*90/60 = 27; Vmezcla = (4*3 + 5) + 4*0,7 = 19,8
        assertThat(volumenPreHervor).isEqualTo(27.0);
        assertThat(volumenMezcla).isEqualTo(19.8);
    }

    // ==================== helpers de construcción ====================

    private static VersionRecetaEntity crearVersionReceta(double relacionDeEmpaste, int duracionHervido) {
        return VersionRecetaEntity.builder().relacionDeEmpaste(relacionDeEmpaste).duracionHervido(duracionHervido).build();
    }

    @Test
    @DisplayName("CP-EA-11: calcular informa el desglose del agua de macerado: agua por relación de empaste, espacio muerto, absorción, pérdida por bagazo y mosto extraído")
    void calcular_debeInformarElDesgloseDelAguaDeMacerado() {
        // === EJECUCION ===
        VolumenesAgua resultado = EscaladoAgua.calcular(20.0, 5.0, crearMacerador(1.5), crearOllaHervor(4.0, 1.0), crearVersionReceta(3.0, 60));

        // === ASSERTS ===
        // 5 kg x 3 L/kg = 15 L; + 1,5 L de espacio muerto = 16,5 L; bagazo = 5 kg x 1,0 L/kg = 5 L; mosto = 16,5 - 1,5 - 5 = 10 L
        assertThat(resultado.aguaPorRelacionDeEmpaste()).isEqualTo(15.0);
        assertThat(resultado.espacioMuerto()).isEqualTo(1.5);
        assertThat(resultado.absorcionPorKg()).isEqualTo(1.0);
        assertThat(resultado.perdidaPorBagazo()).isEqualTo(5.0);
        assertThat(resultado.mostoExtraido()).isEqualTo(10.0);
        assertThat(resultado.aguaPorRelacionDeEmpaste() + resultado.espacioMuerto()).isEqualTo(resultado.aguaMacerado());
    }

    @Test
    @DisplayName("CP-EA-12: calcular con el espacio muerto, la evaporación y la pérdida por trub como valores (los de la instantánea del lote) da el mismo resultado que con las entidades")
    void calcular_conValoresDeLaInstantaneaDebeCoincidirConLasEntidades() {
        // === PREPARACION DE DATOS ===
        VersionRecetaEntity versionReceta = crearVersionReceta(3.0, 60);

        // === EJECUCION ===
        VolumenesAgua conEntidades = EscaladoAgua.calcular(20.0, 5.0, crearMacerador(1.5), crearOllaHervor(4.0, 1.0), versionReceta);
        VolumenesAgua conValores = EscaladoAgua.calcular(20.0, 5.0, 1.5, 1.0, 4.0, versionReceta);

        // === ASSERTS ===
        assertThat(conValores).isEqualTo(conEntidades);
        assertThat(EscaladoAgua.calcularVolumenPreHervor(20.0, 1.0, 4.0, versionReceta)).isEqualTo(25.0);
        assertThat(EscaladoAgua.calcularAguaMacerado(5.0, 1.5, versionReceta)).isEqualTo(16.5);
    }

    private static MaceradorEntity crearMacerador(double espacioMuerto) {
        return MaceradorEntity.builder()
                .id(1L)
                .identificadorInterno("Macerador 1")
                .estado(Estado.ACTIVO)
                .estadoOperativo(EstadoOperativo.DISPONIBLE)
                .usosMaximosAntesMantenimiento(100)
                .capacidadTotal(100.0)
                .capacidadUtil(90.0)
                .espacioMuerto(espacioMuerto)
                .eficienciaMaceracion(75.0)
                .build();
    }

    private static OllaHervorEntity crearOllaHervor(double evaporacion, double perdidaPorTrub) {
        return OllaHervorEntity.builder()
                .id(2L)
                .identificadorInterno("Olla 1")
                .estado(Estado.ACTIVO)
                .estadoOperativo(EstadoOperativo.DISPONIBLE)
                .usosMaximosAntesMantenimiento(100)
                .capacidadTotal(100.0)
                .capacidadUtil(90.0)
                .evaporacion(evaporacion)
                .perdidaPorTrub(perdidaPorTrub)
                .build();
    }
}
