## Casos de prueba de `BloqueDashboardResponseDTO`

**Tipo de prueba:** unitaria de la fábrica `BloqueDashboardResponseDTO.de`, que arma un bloque de un panel a partir de una página. Clase `BloqueDashboardResponseDTOTest`.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-BDD-01** | Página con filas y total mayor | El total del bloque es el total real de la página y las filas son su contenido mapeado, en el mismo orden. |
| **CP-BDD-02** | Página vacía | El bloque tiene total 0 y una lista de filas vacía, nunca nula. |
