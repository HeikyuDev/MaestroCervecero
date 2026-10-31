## Casos de prueba de `ControlEquipamientoController`

**Tipo de prueba:** capa web (`@WebMvcTest`): lista, cronograma, panel operativo y detalle de una operación, con Spring MVC, Security, `ControllerAdvices` y plantillas reales; el service está simulado. Clase `ControlEquipamientoControllerTest`.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-CCE-01** | el operario ve la lista con el botón Abrir y sin el ojo del cronograma | el operario ve la lista con el botón Abrir y sin el ojo del cronograma |
| **CP-CCE-02** | el gerente ve la lista con el ojo del cronograma y el botón Abrir | el gerente ve la lista con el ojo del cronograma y el botón Abrir |
| **CP-CCE-03** | los demás roles no abren la lista, el cronograma ni el panel: redirigen con alerta y no se invoca el service | los demás roles no abren la lista, el cronograma ni el panel: redirigen con alerta y no se invoca el service |
| **CP-CCE-04** | los criterios de la lista se pasan al service (identificador sin espacios, tipo y estado operativo) | los criterios de la lista se pasan al service (identificador sin espacios, tipo y estado operativo) |
| **CP-CCE-05** | un tipo y un estado operativo inválidos no filtran y se avisa con una alerta warning | un tipo y un estado operativo inválidos no filtran y se avisa con una alerta warning |
| **CP-CCE-06** | el cronograma del gerente pide los últimos 30 días hasta hoy y dibuja un bloque por actividad con enlaces y leyenda | el cronograma del gerente pide los últimos 30 días hasta hoy y dibuja un bloque por actividad con enlaces y leyenda |
| **CP-CCE-07** | el operario no abre el cronograma del equipo | el operario no abre el cronograma del equipo |
| **CP-CCE-08** | sin actividad en el período el cronograma muestra el mensaje de vacío y no el diagrama | sin actividad en el período el cronograma muestra el mensaje de vacío y no el diagrama |
| **CP-CCE-09** | Anterior y Siguiente desplazan el período su misma longitud y Hoy vuelve al período por defecto | Anterior y Siguiente desplazan el período su misma longitud y Hoy vuelve al período por defecto |
| **CP-CCE-10** | un período inválido se avisa y se muestra el período por defecto | un período inválido se avisa y se muestra el período por defecto |
| **CP-CCE-11** | el operario con el equipo EN_LIMPIEZA ve el formulario de registro de limpieza con fechas, observaciones y placeholder, y Anular solo en la operación anulable | el operario con el equipo EN_LIMPIEZA ve el formulario de registro de limpieza con fechas, observaciones y placeholder, y Anular solo en la operación anulable |
| **CP-CCE-12** | con un estado que no lo permite el panel no tiene formulario y explica el motivo con el texto del service | con un estado que no lo permite el panel no tiene formulario y explica el motivo con el texto del service |
| **CP-CCE-13** | el gerente ve el panel en modo consulta, sin formulario ni botones de anular | el gerente ve el panel en modo consulta, sin formulario ni botones de anular |
| **CP-CCE-14** | una pestaña inexistente muestra la primera, y la pestaña pedida se marca como activa | una pestaña inexistente muestra la primera, y la pestaña pedida se marca como activa |
| **CP-CCE-15** | ambos roles ven el detalle de una operación con la información del equipo y los datos por tema | ambos roles ven el detalle de una operación con la información del equipo y los datos por tema |
| **CP-CCE-16** | el detalle de una operación anulada muestra cuándo y por qué, sin el usuario | el detalle de una operación anulada muestra cuándo y por qué, sin el usuario |
| **CP-CCE-17** | una operación o un equipamiento inexistente vuelve al origen con alerta de error | una operación o un equipamiento inexistente vuelve al origen con alerta de error |
| **CP-CCE-18** | Registrado por aparece solo en el detalle de la operación, no en el panel ni en el cronograma, y Responsable no aparece en ninguna | Registrado por aparece solo en el detalle de la operación, no en el panel ni en el cronograma, y Responsable no aparece en ninguna |
| **CP-CCE-19** | un período en el borde del rango de fechas no ofrece el enlace que se saldría del rango y una fecha fuera de rango se avisa como inválida | un período en el borde del rango de fechas no ofrece el enlace que se saldría del rango y una fecha fuera de rango se avisa como inválida |
