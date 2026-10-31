package com.github.heikyudev.maestrocervecero.presentation.controller.control_equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoOperacionEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Permiso;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.FallaEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.LimpiezaEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.MantenimientoEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.OperacionEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.OperacionesEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.method.equipamiento.MetodosCicloVidaEquipamiento;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Cada una de las tres pestañas del panel operativo de un equipamiento, que es también cada tipo de operación de su
 * ciclo de vida. Reúne lo que las diferencia: la clave de la URL, el estado operativo en el que se puede registrar, los
 * permisos, los textos y el formulario.
 */
@Getter
public enum PestanaOperacion {

    LIMPIEZAS("limpiezas", "limpieza", TipoOperacionEquipamiento.LIMPIEZA, "una limpieza", "Limpiezas", "bi-stars",
            EstadoOperativo.EN_LIMPIEZA, Permiso.LIMPIEZA_EQUIPAMIENTO_CONSULTAR, Permiso.LIMPIEZA_EQUIPAMIENTO_REGISTRAR,
            Permiso.LIMPIEZA_EQUIPAMIENTO_ANULAR, "Limpieza registrada correctamente", "Limpieza anulada correctamente",
            "Ej: Limpieza con soda cáustica y enjuague", "No hay limpiezas registradas", "Se registró la limpieza en el equipamiento equivocado"),
    FALLAS("fallas", "falla", TipoOperacionEquipamiento.FALLA, "una falla", "Fallas", "bi-exclamation-triangle",
            EstadoOperativo.DISPONIBLE, Permiso.FALLA_EQUIPAMIENTO_CONSULTAR, Permiso.FALLA_EQUIPAMIENTO_REGISTRAR,
            Permiso.FALLA_EQUIPAMIENTO_ANULAR, "Falla registrada correctamente", "Falla anulada correctamente",
            "Ej: Falla en la resistencia de calentamiento", "No hay fallas registradas", "Se registró la falla en el equipamiento equivocado"),
    MANTENIMIENTOS("mantenimientos", "mantenimiento", TipoOperacionEquipamiento.MANTENIMIENTO, "un mantenimiento", "Mantenimientos", "bi-wrench",
            EstadoOperativo.EN_MANTENIMIENTO, Permiso.MANTENIMIENTO_EQUIPAMIENTO_CONSULTAR, Permiso.MANTENIMIENTO_EQUIPAMIENTO_REGISTRAR,
            Permiso.MANTENIMIENTO_EQUIPAMIENTO_ANULAR, "Mantenimiento registrado correctamente", "Mantenimiento anulado correctamente",
            "Ej: Cambio de juntas y revisión de válvulas", "No hay mantenimientos registrados", "Se registró el mantenimiento en el equipamiento equivocado");

    /** Clave de la pestaña en la URL del panel ({@code ?pestana=}) y en las rutas de registro y anulación. */
    private final String clave;
    /** Clave del tipo de operación en la URL del detalle de una operación. */
    private final String claveOperacion;
    private final TipoOperacionEquipamiento tipo;
    /** Nombre de la operación con su artículo, tal como lo usan los mensajes del service. */
    private final String articulo;
    private final String etiqueta;
    private final String icono;
    /** Estado operativo del equipamiento en el que se puede registrar la operación. */
    private final EstadoOperativo estadoRequerido;
    private final Permiso permisoConsultar;
    private final Permiso permisoRegistrar;
    private final Permiso permisoAnular;
    private final String mensajeRegistrada;
    private final String mensajeAnulada;
    private final String ejemploObservaciones;
    private final String textoSinRegistros;
    /** Texto de ejemplo del motivo de anulación, sin el prefijo "Ej: " (lo agrega el diálogo). */
    private final String ejemploMotivo;

    PestanaOperacion(String clave, String claveOperacion, TipoOperacionEquipamiento tipo, String articulo, String etiqueta, String icono,
                     EstadoOperativo estadoRequerido, Permiso permisoConsultar, Permiso permisoRegistrar, Permiso permisoAnular,
                     String mensajeRegistrada, String mensajeAnulada, String ejemploObservaciones, String textoSinRegistros, String ejemploMotivo) {
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
    }

    /**
     * Resuelve la pestaña a partir de su clave en la URL.
     *
     * @param clave La clave pedida, sin distinguir mayúsculas.
     * @return La pestaña, o vacío si la clave es nula o no existe.
     */
    public static Optional<PestanaOperacion> deClave(String clave) {
        if (clave == null) {
            return Optional.empty();
        }
        String buscada = clave.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(p -> p.clave.equals(buscada)).findFirst();
    }

    /** Un formulario de registro vacío del tipo de la pestaña. */
    public Object nuevoFormulario() {
        return switch (this) {
            case LIMPIEZAS -> new LimpiezaEquipamientoFormDTO();
            case FALLAS -> new FallaEquipamientoFormDTO();
            case MANTENIMIENTOS -> new MantenimientoEquipamientoFormDTO();
        };
    }

    /** Las operaciones de este tipo dentro de las de un equipamiento. */
    public List<OperacionEquipamientoResponseDTO> operacionesDe(OperacionesEquipamientoResponseDTO operaciones) {
        return switch (this) {
            case LIMPIEZAS -> operaciones.getLimpiezas();
            case FALLAS -> operaciones.getFallas();
            case MANTENIMIENTOS -> operaciones.getMantenimientos();
        };
    }

    /**
     * El motivo por el que no se puede registrar la operación con el equipamiento en el estado dado: el mismo texto que
     * daría el service al rechazar el registro.
     *
     * @param estadoActual Estado operativo actual del equipamiento.
     * @return El motivo, o vacío si el estado permite registrar la operación.
     */
    public Optional<String> motivoNoRegistrable(EstadoOperativo estadoActual) {
        try {
            MetodosCicloVidaEquipamiento.validarEstadoOperativoParaRegistrar(estadoActual, estadoRequerido, articulo);
            return Optional.empty();
        } catch (ReglaNegocioException e) {
            return Optional.of(e.getMessage());
        }
    }
}
