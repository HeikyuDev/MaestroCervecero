## Casos de prueba de la zona horaria de la aplicación

**Tipo de prueba:** unitaria (clase `MaestroCerveceroApplicationZonaHorariaTest`), sin Spring.

**Regla:** la aplicación usa la zona `America/Argentina/Buenos_Aires` para todas las decisiones de "hoy" y "ahora" (`LocalDate.now()`, `LocalDateTime.now()`), sin depender de la zona del servidor. La zona predeterminada de la JVM se fija al cargar `MaestroCerveceroApplication`, antes de crear el pool de conexiones, de modo que la sesión de PostgreSQL y `current_timestamp` usan la misma zona; Hibernate escribe y lee las fechas con hora en esa zona (`spring.jpa.properties.hibernate.jdbc.time_zone`).

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-ZHA-01** | Zona del negocio | `MaestroCerveceroApplication.ZONA_HORARIA` | Zona configurada | Es `America/Argentina/Buenos_Aires`. |
| **CP-ZHA-02** | Zona predeterminada de la JVM | La zona de la JVM es `Pacific/Auckland`; se inicializa de nuevo la clase de la aplicación | El bloque estático fija la zona | `TimeZone.getDefault()` queda en `America/Argentina/Buenos_Aires`. |
