## Casos de prueba de `OperarioSoloConsultaGestionTest`

**Tipo de prueba:** capa web (`@WebMvcTest` con los diez controllers de gestión; services simulados que devuelven páginas vacías). El operario de producción consulta las pantallas de Gestión de Equipamientos, Gestión de Insumos (maltas, lúpulos, levaduras), Gestión de Barriles (barriles y fabricantes) y Gestión de Recetas, y no escribe.

**Autorización:** los GET de lista (y el detalle de la receta, y el buscador de fabricantes que usa el filtro de barriles) llevan `@PreAuthorize("hasAnyRole('GERENTE_DE_PRODUCCION', 'OPERARIO_DE_PRODUCCION')")`, porque esos módulos ya autorizan por rol; el detalle de equipamiento autoriza por `EQUIPAMIENTO_CONSULTAR`, que el operario tiene. Todo GET de formulario de alta o edición y todo POST (alta, modificación, baja) siguen con `hasRole('GERENTE_DE_PRODUCCION')`.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-OSC-01** | Lista de cada módulo de gestión para el operario | Responde 200 con los filtros y sin ningún control de escritura: ni botón de alta, ni enlace de edición, ni botón de baja, ni diálogo de baja. |
| **CP-OSC-02** | Lista de cada módulo para el gerente de producción | Conserva el botón de alta con su enlace `/nuevo`. |
| **CP-OSC-03** | Escritura por URL y por POST del operario | El formulario de alta, el de edición y los POST de alta, modificación y baja redirigen con la alerta `danger` de permisos. |
| **CP-OSC-04** | Los demás roles | El encargado de depósito, el gerente de compras, el encargado de distribución y el administrador siguen redirigidos con la alerta `danger` en todas las listas. |
