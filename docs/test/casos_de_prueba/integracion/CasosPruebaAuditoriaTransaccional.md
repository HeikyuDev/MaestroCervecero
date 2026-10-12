## Casos de prueba de integración: auditoría y transacciones (`AuditAspect`)

**Tipo de prueba:** integración (clase `AuditoriaTransaccionalIntegracionTest`, etiqueta `integracion`) contra la base `maestrocervecero_test`. Usa un service de prueba, registrado solo para este test, con métodos `@Transactional` y `@AuditableAction` sobre `UsuarioEntity`.

**Regla:** la bitácora se registra solo después de que la operación se confirma. El aspecto está por fuera del interceptor transaccional (`AuditAspect.ORDEN`) y, si el método corre dentro de una transacción externa abierta, difiere el registro a `afterCommit`. Como el guardado de la bitácora es asíncrono, las comprobaciones de "no se registra" esperan un segundo con la condición sostenida.

---

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-ATX-01** | Orden del aspecto | Advisors del proxy del service de prueba | El aspecto de auditoría está antes (más externo) que el interceptor transaccional | El advice de auditoría precede al de la transacción en la cadena del proxy. |
| **CP-ATX-02** | Operación confirmada | Se cambia el nombre de un usuario con `@AuditableAction(MODIFICAR, USUARIO)` | La operación se confirma | Se registra una única entrada `MODIFICAR:USUARIO:<id>`. |
| **CP-ATX-03** | Falla al confirmar | Se asigna a un usuario el username de otro sin llamar a `save`; el `UPDATE` lo envía Hibernate al confirmar y el índice único lo rechaza | La transacción falla en el commit | Lanza `DataIntegrityViolationException`; no queda ningún registro de auditoría y el username del usuario no cambió. |
| **CP-ATX-04** | Transacción externa revertida | Una operación auditada se ejecuta dentro de una transacción externa que luego lanza una excepción | La transacción externa se revierte | No queda ningún registro de auditoría. |
| **CP-ATX-05** | Transacción externa confirmada | Una operación auditada se ejecuta dentro de una transacción externa que termina bien | El registro se difiere hasta el commit externo | Dentro de la transacción todavía no hay registros; después del commit hay una entrada. |
