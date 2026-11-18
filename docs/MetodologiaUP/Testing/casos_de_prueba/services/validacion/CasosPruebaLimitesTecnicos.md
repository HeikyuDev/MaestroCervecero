## Casos de prueba de `LimitesTecnicos`

**Tipo de prueba:** unitaria (clase `LimitesTecnicosTest`), sin Spring ni mocks. Verifica los valores de los techos técnicos compartidos por las validaciones de los módulos.

**Regla:** son límites de sentido común que protegen la base de datos, los cálculos y la interfaz de valores absurdos; no son reglas de negocio ni parámetros configurables. Las constantes enteras son constantes de compilación y se usan también en anotaciones de Bean Validation.

---

### Constantes de `LimitesTecnicos`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-LT-01** | Límites de texto | `LONGITUD_MAXIMA_NOMBRE`, `LONGITUD_MAXIMA_MOTIVO` | Valor de cada constante | 100 y 255 caracteres. |
| **CP-LT-02** | Límites de equipamiento | `CAPACIDAD_EQUIPO_MAXIMA_LITROS`, `RENDIMIENTO_MOLINO_MAXIMO_KG_H`, `USOS_MAXIMOS_MANTENIMIENTO_MAXIMO` | Valor de cada constante | 100.000 L, 10.000 kg/h y 100.000 usos. |
| **CP-LT-03** | Límites de recetas | `VOLUMEN_BASE_RECETA_MAXIMO_LITROS`, `RELACION_EMPASTE_MAXIMA_L_POR_KG`, `IBU_MAXIMO`, `CANTIDAD_INSUMO_RECETA_MAXIMA`, `DURACION_MACERACION_HERVIDO_MAXIMA_MINUTOS`, `DURACION_FERMENTACION_MADURACION_MAXIMA_DIAS` | Valor de cada constante | 100.000 L, 20 L/kg, 500 IBU, 1.000.000, 10.080 minutos y 3.650 días. |
| **CP-LT-04** | Límites de planificación y configuración | `CANTIDAD_A_PRODUCIR_MAXIMA_LITROS`, `RANGO_MAXIMO_PLANIFICACION_DIAS`, `CAPACIDAD_LOTE_ESTANDAR_MAXIMA_LITROS`, `VELOCIDAD_ENVASADO_MAXIMA_L_POR_HORA` | Valor de cada constante | 10.000.000 L, 1.825 días, 100.000 L y 100.000 L/h. |
| **CP-LT-05** | Límites de consumos y envasados y escala de las cantidades | `CANTIDAD_CONSUMIDA_MAXIMA`, `CANTIDAD_ENVASADA_MAXIMA_LITROS`, `ESCALA_CANTIDADES` | Valor de cada constante | 1.000.000 (en kg o g según el insumo), 100.000 L y 4 decimales. |
| **CP-LT-06** | Límite de ingresos de insumo | `CANTIDAD_INGRESO_MAXIMA` | Valor de la constante | 1.000.000 (en la unidad de medida del insumo). |
