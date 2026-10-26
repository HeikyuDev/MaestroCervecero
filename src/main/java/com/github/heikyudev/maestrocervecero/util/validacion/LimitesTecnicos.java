package com.github.heikyudev.maestrocervecero.util.validacion;

/**
 * Techos técnicos compartidos por las validaciones de los módulos.
 * <p>
 * Son límites de sentido común que protegen la base de datos, los cálculos y la interfaz de valores
 * absurdos (por ejemplo una capacidad de mil millones de litros); no son reglas de negocio ni
 * parámetros configurables del cervecero. Las constantes enteras son constantes de compilación, por lo
 * que se pueden usar en anotaciones como {@code @Size(max = LimitesTecnicos.LONGITUD_MAXIMA_NOMBRE)}.
 * </p>
 */
public final class LimitesTecnicos {

    /**
     * Longitud máxima, en caracteres, del nombre de un insumo, de una receta, de un país, de una provincia o de una
     * localidad, del nombre y el apellido de un usuario y del identificador de un equipo.
     */
    public static final int LONGITUD_MAXIMA_NOMBRE = 100;

    /** Longitud máxima, en caracteres, del nombre de usuario con el que se inicia sesión. */
    public static final int LONGITUD_MAXIMA_USERNAME = 50;

    /** Longitud máxima, en caracteres, de un correo electrónico (el máximo de la columna de la base de datos). */
    public static final int LONGITUD_MAXIMA_CORREO = 255;

    /** Cantidad máxima de dígitos de un número de teléfono (el máximo del plan internacional E.164). */
    public static final int LONGITUD_MAXIMA_TELEFONO = 15;

    /** Longitud máxima, en caracteres, de una dirección (el máximo de la columna de la base de datos). */
    public static final int LONGITUD_MAXIMA_DIRECCION = 255;

    /** Longitud máxima, en caracteres, de un código postal. */
    public static final int LONGITUD_MAXIMA_CODIGO_POSTAL = 10;

    /** Longitud máxima, en caracteres, de un motivo u observación. */
    public static final int LONGITUD_MAXIMA_MOTIVO = 255;

    /** Longitud máxima, en caracteres, de la identificación del lote asignada por el proveedor (el máximo de la columna de la base de datos). */
    public static final int LONGITUD_MAXIMA_IDENTIFICACION_LOTE = 255;

    /** Capacidad máxima, en litros, de un equipo (capacidad total, útil y espacio muerto). */
    public static final double CAPACIDAD_EQUIPO_MAXIMA_LITROS = 100_000.0;

    /** Capacidad máxima, en litros, de un barril. */
    public static final double CAPACIDAD_BARRIL_MAXIMA_LITROS = 1_000.0;

    /** Rendimiento máximo de un molino, en kilogramos por hora. */
    public static final double RENDIMIENTO_MOLINO_MAXIMO_KG_H = 10_000.0;

    /** Cantidad máxima de usos entre mantenimientos preventivos de un equipo. */
    public static final int USOS_MAXIMOS_MANTENIMIENTO_MAXIMO = 100_000;

    /** Volumen base máximo de una receta, en litros. */
    public static final double VOLUMEN_BASE_RECETA_MAXIMO_LITROS = 100_000.0;

    /** Relación de empaste máxima, en litros de agua por kilogramo de malta. */
    public static final double RELACION_EMPASTE_MAXIMA_L_POR_KG = 20.0;

    /** Amargor máximo de una receta, en IBU. */
    public static final int IBU_MAXIMO = 500;

    /** Cantidad máxima de un insumo dentro de una receta, en kilogramos o gramos según el insumo. */
    public static final double CANTIDAD_INSUMO_RECETA_MAXIMA = 1_000_000.0;

    /** Duración máxima de una maceración o un hervido, en minutos (7 días). */
    public static final int DURACION_MACERACION_HERVIDO_MAXIMA_MINUTOS = 10_080;

    /** Duración máxima de una fermentación o una maduración, en días (10 años). */
    public static final int DURACION_FERMENTACION_MADURACION_MAXIMA_DIAS = 3_650;

    /** Cantidad máxima a producir en una planificación, en litros. */
    public static final double CANTIDAD_A_PRODUCIR_MAXIMA_LITROS = 10_000_000.0;

    /** Rango máximo, en días, entre el inicio y el fin de una planificación (5 años). */
    public static final int RANGO_MAXIMO_PLANIFICACION_DIAS = 1_825;

    /**
     * Rango máximo, en días, entre el primer y el último día del período del cronograma de producción (5 años): un
     * período de {@code RANGO_MAXIMO_CRONOGRAMA_DIAS + 1} días contando ambos extremos, de modo que entra completa una
     * planificación del rango máximo de {@link #RANGO_MAXIMO_PLANIFICACION_DIAS}.
     */
    public static final int RANGO_MAXIMO_CRONOGRAMA_DIAS = 1_825;

    /** Capacidad máxima del lote estándar de la configuración de producción, en litros. */
    public static final double CAPACIDAD_LOTE_ESTANDAR_MAXIMA_LITROS = 100_000.0;

    /** Velocidad máxima de envasado de la configuración de producción, en litros por hora. */
    public static final double VELOCIDAD_ENVASADO_MAXIMA_L_POR_HORA = 100_000.0;

    /** Cantidad máxima que se puede consumir de un insumo en un solo consumo, en kilogramos o gramos según el insumo (igual que la cantidad máxima de un insumo en una receta). */
    public static final double CANTIDAD_CONSUMIDA_MAXIMA = CANTIDAD_INSUMO_RECETA_MAXIMA;

    /**
     * Cantidad máxima de una presentación comercial, en kilogramos (10 toneladas): la cantidad informada en gramos,
     * kilogramos o toneladas se convierte a esta unidad antes de compararla.
     */
    public static final double CANTIDAD_PRESENTACION_COMERCIAL_MAXIMA_KG = 10_000.0;

    /** Cantidad máxima de ítems (combinaciones de insumo y presentación comercial) del catálogo de un proveedor. */
    public static final int CATALOGO_PROVEEDOR_MAXIMO_ITEMS = 100;

    /** Cantidad máxima que se puede envasar en un solo envasado, en litros (igual que la capacidad máxima de un equipo). */
    public static final double CANTIDAD_ENVASADA_MAXIMA_LITROS = CAPACIDAD_EQUIPO_MAXIMA_LITROS;

    /**
     * Cantidad de decimales con la que se guardan y se comparan las cantidades de stock, reservas, consumos y envasados:
     * absorbe el error de representación de los números de punto flotante (por ejemplo, {@code 0,1 + 0,2}).
     */
    public static final int ESCALA_CANTIDADES = 4;

    private LimitesTecnicos() {
    }
}
