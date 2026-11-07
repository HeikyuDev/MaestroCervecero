package com.github.heikyudev.maestrocervecero.presentation.controller.control_barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.TipoOperacionBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Permiso;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.DespachoBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.DevolucionBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.FallaBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.FraccionamientoBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.LimpiezaBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.MantenimientoBarrilFormDTO;
import lombok.Getter;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Cada una de las seis pestañas del panel de un barril, que es también cada tipo de operación de su ciclo de vida.
 * Reúne lo que las diferencia: la clave de la URL, el estado operativo en el que se puede registrar, los permisos, los
 * textos y el formulario.
 */
@Getter
public enum PestanaOperacionBarril {

    LIMPIEZAS("limpiezas", "limpieza", TipoOperacionBarril.LIMPIEZA, "una limpieza", "Limpiezas", "bi-stars", EstadoOperativoBarril.EN_LIMPIEZA,
            Permiso.LIMPIEZA_BARRIL_CONSULTAR, Permiso.LIMPIEZA_BARRIL_REGISTRAR, Permiso.LIMPIEZA_BARRIL_ANULAR,
            "Limpieza registrada correctamente", "Limpieza anulada correctamente",
            "Ej: Limpieza con soda cáustica y enjuague", "No hay limpiezas registradas", "Se registró la limpieza en el barril equivocado", "Observaciones de la limpieza"),
    FALLAS("fallas", "falla", TipoOperacionBarril.FALLA, "una falla", "Fallas", "bi-exclamation-triangle", EstadoOperativoBarril.DISPONIBLE,
            Permiso.FALLA_BARRIL_CONSULTAR, Permiso.FALLA_BARRIL_REGISTRAR, Permiso.FALLA_BARRIL_ANULAR,
            "Falla registrada correctamente", "Falla anulada correctamente",
            "Ej: Pérdida en la válvula del barril", "No hay fallas registradas", "Se registró la falla en el barril equivocado", "Observaciones de la falla"),
    FRACCIONAMIENTOS("fraccionamientos", "fraccionamiento", TipoOperacionBarril.FRACCIONAMIENTO, "un fraccionamiento", "Fraccionamientos", "bi-droplet-half", EstadoOperativoBarril.CON_CERVEZA,
            Permiso.FRACCIONAMIENTO_BARRIL_CONSULTAR, Permiso.FRACCIONAMIENTO_BARRIL_REGISTRAR, Permiso.FRACCIONAMIENTO_BARRIL_ANULAR,
            "Fraccionamiento registrado correctamente", "Fraccionamiento anulado correctamente",
            "Ej: Muestra para control de calidad", "No hay fraccionamientos registrados", "Se registró el fraccionamiento en el barril equivocado", "Observaciones del fraccionamiento"),
    MANTENIMIENTOS("mantenimientos", "mantenimiento", TipoOperacionBarril.MANTENIMIENTO, "un mantenimiento", "Mantenimientos", "bi-wrench", EstadoOperativoBarril.EN_MANTENIMIENTO,
            Permiso.MANTENIMIENTO_BARRIL_CONSULTAR, Permiso.MANTENIMIENTO_BARRIL_REGISTRAR, Permiso.MANTENIMIENTO_BARRIL_ANULAR,
            "Mantenimiento registrado correctamente", "Mantenimiento anulado correctamente",
            "Ej: Cambio de juntas y revisión de la válvula", "No hay mantenimientos registrados", "Se registró el mantenimiento en el barril equivocado", "Observaciones del mantenimiento"),
    DESPACHOS("despachos", "despacho", TipoOperacionBarril.DESPACHO, "un despacho", "Despachos", "bi-truck", EstadoOperativoBarril.CON_CERVEZA,
            Permiso.DESPACHO_BARRIL_CONSULTAR, Permiso.DESPACHO_BARRIL_REGISTRAR, Permiso.DESPACHO_BARRIL_ANULAR,
            "Despacho registrado correctamente", "Despacho anulado correctamente",
            "Ej: Entrega para el evento del sábado", "No hay despachos registrados", "Se registró el despacho en el barril equivocado", "Observaciones del despacho"),
    DEVOLUCIONES("devoluciones", "devolucion", TipoOperacionBarril.DEVOLUCION, "una devolución", "Devoluciones", "bi-arrow-return-left", EstadoOperativoBarril.DESPACHADO,
            Permiso.DEVOLUCION_BARRIL_CONSULTAR, Permiso.DEVOLUCION_BARRIL_REGISTRAR, Permiso.DEVOLUCION_BARRIL_ANULAR,
            "Devolución registrada correctamente", "Devolución anulada correctamente",
            "Ej: El cliente devolvió el barril con restos de cerveza", "No hay devoluciones registradas", "Se registró la devolución en el barril equivocado", "Observaciones de la devolución");

    /** Clave de la pestaña en la URL del panel ({@code ?pestana=}) y en las rutas de registro y anulación. */
    private final String clave;
    /** Clave del tipo de operación en la URL del detalle de una operación. */
    private final String claveOperacion;
    private final TipoOperacionBarril tipo;
    /** Nombre de la operación con su artículo, tal como lo usan los mensajes del service. */
    private final String articulo;
    private final String etiqueta;
    private final String icono;
    /** Estado operativo del barril en el que se puede registrar la operación. */
    private final EstadoOperativoBarril estadoRequerido;
    private final Permiso permisoConsultar;
    private final Permiso permisoRegistrar;
    private final Permiso permisoAnular;
    private final String mensajeRegistrada;
    private final String mensajeAnulada;
    private final String ejemploObservaciones;
    private final String textoSinRegistros;
    /** Texto de ejemplo del motivo de anulación, sin el prefijo "Ej: " (lo agrega el diálogo). */
    private final String ejemploMotivo;
    /** Título del diálogo con las observaciones de una operación de este tipo. */
    private final String tituloObservaciones;

    PestanaOperacionBarril(String clave, String claveOperacion, TipoOperacionBarril tipo, String articulo, String etiqueta, String icono,
                           EstadoOperativoBarril estadoRequerido, Permiso permisoConsultar, Permiso permisoRegistrar, Permiso permisoAnular,
                           String mensajeRegistrada, String mensajeAnulada, String ejemploObservaciones, String textoSinRegistros,
                           String ejemploMotivo, String tituloObservaciones) {
        this.clave = clave;
        this.claveOperacion = claveOperacion;
        this.tipo = tipo;
        this.articulo = articulo;
        this.etiqueta = etiqueta;
        this.icono = icono;
        this.estadoRequerido = estadoRequerido;
        this.permisoConsultar = permisoConsultar;
        this.permisoRegistrar = permisoRegistrar;
        this.permisoAnular = permisoAnular;
        this.mensajeRegistrada = mensajeRegistrada;
        this.mensajeAnulada = mensajeAnulada;
        this.ejemploObservaciones = ejemploObservaciones;
        this.textoSinRegistros = textoSinRegistros;
        this.ejemploMotivo = ejemploMotivo;
        this.tituloObservaciones = tituloObservaciones;
    }

    private boolean esMasculina() {
        return this == FRACCIONAMIENTOS || this == MANTENIMIENTOS || this == DESPACHOS;
    }

    /** Nombre de la operación sin artículo ("limpieza", "fraccionamiento", "devolución"). */
    public String getNombreOperacion() {
        return articulo.substring(articulo.indexOf(' ') + 1);
    }

    /** Texto del botón de alta de la tabla ("Nueva limpieza", "Nuevo despacho"). */
    public String getTextoNueva() {
        return (esMasculina() ? "Nuevo " : "Nueva ") + getNombreOperacion();
    }

    /** Mensaje de error del formulario de registro ("No se pudo registrar la limpieza", ...). */
    public String getTituloError() {
        return "No se pudo registrar " + (esMasculina() ? "el " : "la ") + getNombreOperacion();
    }

    /** Título del formulario de registro ("Registrar limpieza", ...). */
    public String getTituloRegistrar() {
        return "Registrar " + getNombreOperacion();
    }

    /** Ruta, relativa al barril, del formulario de registro ("limpiezas/nueva", "fraccionamientos/nuevo", ...). */
    public String getRutaFormulario() {
        return clave + (esMasculina() ? "/nuevo" : "/nueva");
    }

    /** Título de la tabla de operaciones de este tipo ("Limpiezas registradas", "Mantenimientos registrados"). */
    public String getTituloRegistradas() {
        return etiqueta + (esMasculina() ? " registrados" : " registradas");
    }

    /**
     * Resuelve la pestaña a partir de su clave en la URL.
     *
     * @param clave La clave pedida, sin distinguir mayúsculas.
     * @return La pestaña, o vacío si la clave es nula o no existe.
     */
    public static Optional<PestanaOperacionBarril> deClave(String clave) {
        if (clave == null) {
            return Optional.empty();
        }
        String buscada = clave.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(p -> p.clave.equals(buscada)).findFirst();
    }

    /** Un formulario de registro vacío del tipo de la pestaña. */
    public Object nuevoFormulario() {
        return switch (this) {
            case LIMPIEZAS -> new LimpiezaBarrilFormDTO();
            case FALLAS -> new FallaBarrilFormDTO();
            case FRACCIONAMIENTOS -> new FraccionamientoBarrilFormDTO();
            case MANTENIMIENTOS -> new MantenimientoBarrilFormDTO();
            case DESPACHOS -> new DespachoBarrilFormDTO();
            case DEVOLUCIONES -> new DevolucionBarrilFormDTO();
        };
    }

    /**
     * El motivo por el que no se puede registrar la operación con el barril en el estado dado: el mismo texto que da el
     * service al rechazar el registro y, con el barril En uso, un aviso propio de que lo ocupa un lote.
     *
     * @param estadoActual Estado operativo actual del barril.
     * @return El motivo, o vacío si el estado permite registrar la operación.
     */
    public Optional<String> motivoNoRegistrable(EstadoOperativoBarril estadoActual) {
        if (estadoActual == estadoRequerido) {
            return Optional.empty();
        }
        if (estadoActual == EstadoOperativoBarril.EN_USO) {
            return Optional.of("No se puede registrar " + articulo + " porque el barril está en uso por un lote");
        }
        return Optional.of("Solo se puede registrar " + articulo + " sobre un barril en estado operativo " + estadoRequerido.getEtiqueta());
    }
}
