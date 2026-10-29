## Casos de prueba de `UbicacionOpcionesController`

**Tipo de prueba:** unitaria de Controller (`@WebMvcTest`, clase `UbicacionOpcionesControllerTest`), con los services simulados.

**Objetivo:** las opciones de los desplegables en cascada País, Provincia y Localidad. Devuelven JSON `[{"id", "nombre"}]` en el orden que entrega el service (alfabético) y las usan los roles que eligen una localidad en sus formularios: `ADMINISTRADOR`, `GERENTE_COMERCIAL` y `GERENTE_DE_COMPRAS`.

---

### 1. `GET /ubicaciones/opciones/provincias` y `GET /ubicaciones/opciones/localidades`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-UOC-01** | Provincias de un país | Rol `GERENTE_DE_COMPRAS`, `idPais=3`; el service devuelve "Córdoba" (`2`) y "Misiones" (`4`) | El rol está autorizado | Responde 200 con un JSON de dos elementos con `id` y `nombre`, en ese orden, y llama a `listarProvinciasActivasDePais(3)`. |
| **CP-UOC-02** | Localidades de una provincia | Rol `GERENTE_COMERCIAL`, `idProvincia=2`; el service devuelve "Río Cuarto" (`9`) | El rol está autorizado | Responde 200 con un JSON de un elemento con `id: 9` y `nombre: "Río Cuarto"` y llama a `listarLocalidadesActivasDeProvincia(2)`. |
| **CP-UOC-03** | El administrador accede; país sin provincias | Rol `ADMINISTRADOR`, `idPais=7`; el service devuelve una lista vacía | El rol está autorizado | Responde 200 con `[]`. |
| **CP-UOC-04** | Rol sin permiso | Rol `GERENTE_DE_PRODUCCION`, ambas rutas | Solo los tres roles indicados acceden | Redirige con la alerta `danger` y no invoca ningún service. |
| **CP-UOC-05** | Falta el parámetro obligatorio | Rol `GERENTE_DE_COMPRAS`, `GET /ubicaciones/opciones/provincias` sin `idPais` | El parámetro es obligatorio | Redirige con la alerta `danger` del `ControllerAdvices` y no invoca el service. |
