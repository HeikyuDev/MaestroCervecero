## Casos de prueba de `EquipamientoController` (resumen)

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `EquipamientoControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig` y `@PreAuthorize`), el `ControllerAdvices` y las plantillas Thymeleaf reales; el `IEquipamientoServicio` está simulado (mock) y no hay base de datos. La lógica del resumen se prueba aparte, en `services/equipamiento/CasosPruebaEquipamiento.md`.

**Roles requeridos:** `GERENTE_DE_PRODUCCION` y `OPERARIO_DE_PRODUCCION` (`@PreAuthorize("hasAnyRole('GERENTE_DE_PRODUCCION', 'OPERARIO_DE_PRODUCCION')")`). Es de solo lectura. El gerente ve las cinco pestañas de "Gestión de Equipamiento" (Resumen, molinos, maceradores, ollas de hervor y fermentadores); el operario solo el Resumen.

**Alcance:** el resumen es la pestaña inicial. Muestra una tarjeta por tipo con la cantidad de equipos en cada estado operativo y una tabla con todos los equipos, el lote y la etapa en la que se usan ahora ("En uso en") y los lotes que los tienen reservados ("Reservado para"). Las tarjetas siempre cuentan todo el equipamiento activo; los filtros solo afectan a la tabla.

---

### 1. `GET /equipamiento` — `mostrarResumen(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-REC-01** | Resumen para el gerente con totales, tabla y pestañas | Rol `GERENTE_DE_PRODUCCION`. El service devuelve los totales y 2 equipos (`FER-02` en uso en el lote `L-014`; `MOL-01` disponible con el lote `L-020` reservado) | El rol está autorizado | Responde 200 con la vista `equipamiento/resumen`. Muestra "Gestión de Equipamiento", las tarjetas de los cuatro tipos, los equipos con su estado como "EN USO" y los links de las pestañas `/equipamiento/molinos`, `/equipamiento/maceradores`, `/equipamiento/ollas-hervor` y `/equipamiento/fermentadores`. |
| **CP-REC-02** | Cantidades por tipo y estado | Rol `GERENTE_DE_PRODUCCION`. Totales: molinos 7 disponibles y 2 en mantenimiento, el resto en 0 | Cada tarjeta lista los 4 estados con su cantidad | Cada tarjeta muestra los estados "DISPONIBLE", "EN USO", "EN LIMPIEZA" y "EN MANTENIMIENTO"; la de molinos muestra 7 y 2 en los estados correspondientes. |
| **CP-REC-03** | Lote en curso, reservas y equipos sin participación | Rol `GERENTE_DE_PRODUCCION`. `FER-02` en uso (lote `L-014`, etapa `FERMENTACION`); `MOL-01` con los lotes `L-020` y `L-021` pendientes (`MOLIENDA`); `MAC-01` en mantenimiento sin lotes | La participación depende de la etapa del equipo | "En uso en" muestra el lote y la etapa con su ortografía ("Fermentación"); "Reservado para" lista los lotes pendientes con su etapa ("Molienda"); un equipo sin participación muestra un guion ("—"). |
| **CP-REC-04** | Paginación y orden por defecto | Rol `GERENTE_DE_PRODUCCION`, sin parámetros | Sin criterios. El equipamiento se lista alfabéticamente | Llama al service con `identificador`, `tipo` y `estadoOperativo` en `null` y un `Pageable` de 20 elementos, página 0, ordenado de forma ascendente por `identificadorInterno` (sin distinguir mayúsculas) y por `id`. |
| **CP-REC-05** | Criterios de búsqueda hacia el service | `identificador: " fer "`, `tipo: FERMENTADOR`, `estadoOperativo: DISPONIBLE`, `pagina: 1`; y por separado valores en blanco o vacíos (opciones "Todos") | Un texto en blanco o un select vacío equivale a no filtrar | Llama al service con `"fer"`, `FERMENTADOR` y `DISPONIBLE`; con los valores vacíos lo llama con los tres criterios en `null`. |
| **CP-REC-06** | Sin resultados | Rol `GERENTE_DE_PRODUCCION`, `identificador: "zzz"`. El service devuelve una página vacía | La tabla no tiene elementos | Muestra "No se encontraron equipos con los criterios indicados." y las tarjetas de totales siguen visibles. |
| **CP-REC-07** | El operario ve el resumen sin las pestañas de gestión | Rol `OPERARIO_DE_PRODUCCION` | `sec:authorize` oculta las pestañas de gestión | Muestra el equipo y la pestaña Resumen; no muestra los links `/equipamiento/molinos` ni `/equipamiento/fermentadores`, ni acciones de baja. |
| **CP-REC-08** | Rol sin relación con la producción | Rol `GERENTE_COMERCIAL` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |
| **CP-REC-09** | Visitante sin sesión | Sin autenticación | La URL exige estar autenticado | Redirige al login. No invoca al service. |
