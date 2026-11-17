# Diagrama de secuencia de diseño: registrar envasado de lote

El diagrama muestra cómo `EnvasadoLoteController.registrarEnvasado` y `EnvasadoLoteServicioImpl.registrarEnvasadoLote` registran una cantidad de cerveza en un barril disponible durante la etapa de envasado. Parte del formulario ya abierto, con un usuario autorizado mediante `ENVASADO_REGISTRAR`.

```mermaid
    
```

## Participantes y abstracción

- **Interfaz web y EnvasadoLoteController:** representan `lote/envasado-form.html` y `lote/envasado-lista.html`, el envío HTTP y la navegación Post-Redirect-Get. El controlador resuelve la etapa de envasado a partir del lote de la ruta. Sus consultas de contexto y de presentación quedan resumidas.
- **EnvasadoLoteServicioImpl, IEtapaLoteRepository e ILoteRepository:** coordinan la transacción y bloquean el lote antes de recuperar y validar la etapa. Esto serializa registros, anulaciones, finalización del envasado y cancelación del lote.
- **IBarrilRepository:** recupera el barril activo bajo bloqueo y guarda su estado `EN_USO` y contenido actual. La transición a `CON_CERVEZA` pertenece a la finalización del envasado del lote, no a este registro.
- **IEnvasadoLoteRepository:** aporta el acumulado registrado para controlar la capacidad del fermentador y persiste el envasado `REGISTRADO`, asociado a etapa y barril.
- **ControllerAdvices:** maneja los recursos inexistentes que el controlador deja propagar. También interviene si falla la resolución del contexto previa al servicio, resumida en la nota inicial.

La alternativa permitida resume el recorrido satisfactorio; cada rechazo interrumpe el primer paso que falle. Las reglas de negocio se capturan en el controlador: si la excepción indica un campo admitido (`idBarril`, `cantidad` o `fechaEnvasado`), el error se asocia a ese campo; en otro caso aparece en el resumen global. Tanto esas reglas como los errores de validación MVC vuelven al formulario mediante redirección con datos y errores en flash. Si al reconstruir el formulario el lote o la etapa dejaron de estar operables, el GET devuelve a la lista con un aviso. Las escrituras del servicio se revierten al fallar la transacción.

La fecha puede coincidir con el inicio de la etapa, pero debe ser estrictamente posterior a la última operación registrada del barril y no puede ser futura. Las consultas a `IFallaBarrilRepository`, `IMantenimientoBarrilRepository`, `ILimpiezaBarrilRepository`, `IDespachoBarrilRepository`, `IDevolucionBarrilRepository`, `IFraccionamientoBarrilRepository` e `IEnvasadoLoteRepository` se agrupan en una nota; `MetodosCicloVida` concentra la validación temporal. No se inventa un repositorio común de historial.

La cantidad debe caber en el barril y el total registrado de la etapa no debe superar la capacidad útil del fermentador. Puede superar el volumen objetivo del lote, porque ese volumen es una previsión y no el límite de producción real. El método obtiene la capacidad desde la instantánea de escalado del lote o, si falta, desde el fermentador asociado. No se inicia ni se finaliza una etapa al registrar un envasado.

Se omiten getters, setters, construcción de entidades, conversiones a DTO, redondeos, buscadores y consultas de presentación. Las entidades se representan como datos porque sus cambios relevantes quedan en las notas. La infraestructura transversal de seguridad y auditoría y los errores técnicos quedan fuera del diagrama.

## Código de referencia

- [EnvasadoLoteController.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/presentation/controller/lote/EnvasadoLoteController.java): `registrarEnvasado`, `resolverContexto`, `mostrarFormulario` y redirecciones.
- [EnvasadoLoteServicioImpl.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/service/implementation/lote/EnvasadoLoteServicioImpl.java): `registrarEnvasadoLote`, bloqueo y validación de etapa y capacidad.
- [IEtapaLoteRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/lote/IEtapaLoteRepository.java), [ILoteRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/lote/ILoteRepository.java), [IBarrilRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/barril/IBarrilRepository.java) e [IEnvasadoLoteRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/lote/IEnvasadoLoteRepository.java): búsquedas, bloqueos y persistencia.
- [MetodosCicloVida.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/util/method/MetodosCicloVida.java): validación de fechas no futuras y posteriores a las operaciones del barril.
- [ControllerAdvices.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/presentation/advice/ControllerAdvices.java): manejo global de recursos no encontrados y redirección segura.
