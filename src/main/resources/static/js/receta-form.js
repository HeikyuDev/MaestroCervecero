/*
 * Formulario de receta: tablas de maltas, lúpulos y levaduras con ventana emergente para agregar filas.
 *
 * El servidor pinta las filas que la receta ya tiene y deja, por cada tabla, una <template> con una fila
 * vacía (receta/receta-form-filas.html). Este archivo solo reacciona a los atributos data-* de la página:
 *
 *   data-detalle                 sección de una tabla (malta, lupulo o levadura).
 *   data-abrir-agregar           botón "Agregar ..." de la sección: abre su ventana emergente.
 *   data-quitar-fila             botón "Quitar" de una fila.
 *   data-fila / data-prefijo     fila de la tabla y prefijo de sus campos (version.detallesMalta, etc.).
 *   data-fila-vacia              fila con el texto de tabla vacía.
 *   data-campo                   campo oculto de una fila (idMalta, cantidad, ...); su nombre es
 *                                prefijo[indice].campo.
 *   data-celda                   celda visible de una fila.
 *   template[data-plantilla]     fila vacía que se clona al agregar.
 *
 * Dentro de cada ventana emergente (dialog[data-dialogo-agregar]):
 *   data-campo-modal             campo de captura (cantidad, uso, tiempoDeHervor, etapaDeUso).
 *   data-grupo                   bloque que solo aparece para ciertos usos del lúpulo.
 *   data-error                   mensaje de validación (oculto mientras no haya error).
 *   data-confirmar-agregar       "Agregar a la receta".
 *   data-cerrar-agregar          cierra la ventana sin agregar.
 * El insumo se elige con el selector de entidad de /js/modales.js (data-selector).
 *
 * Índices: Spring solo enlaza listas sin huecos (detallesMalta[0], [1], [2]...). Por eso después de
 * cada alta o baja de fila se renumeran los campos de toda la tabla, y se vuelve a hacer al enviar el
 * formulario; así el próximo índice libre es siempre la cantidad de filas.
 */
(function () {
    'use strict';

    var USOS = {HERVOR: 'Hervor', WHIRLPOOL: 'Whirlpool', DRY_HOP: 'Dry hop'};
    var ETAPAS = {HERVIDO: 'Hervido', FERMENTACION: 'Fermentación', MADURACION: 'Maduración'};

    // Lo que cambia entre una tabla y otra: el campo con el id del insumo y los mensajes
    var TIPOS = {
        malta: {
            campoId: 'idMalta',
            mensajeInsumo: 'Elegí una malta.',
            mensajeDuplicado: 'Esa malta ya está en la receta.'
        },
        lupulo: {
            campoId: 'idLupulo',
            mensajeInsumo: 'Elegí un lúpulo.',
            mensajeDuplicado: 'Ese lúpulo ya está en la receta con el mismo uso y el mismo tiempo de hervor.'
        },
        levadura: {
            campoId: 'idLevadura',
            mensajeInsumo: 'Elegí una levadura.',
            mensajeDuplicado: 'Esa levadura ya está en la receta.'
        }
    };

    // ---------- Utilidades ----------

    /** Número que contiene un campo, o NaN si está vacío o no es un número. */
    function numeroDe(campo) {
        return campo.value === '' ? NaN : Number(campo.value);
    }

    /** Número tal como lo muestra el servidor: sin ceros sobrantes y con coma decimal. */
    function formatear(numero) {
        return String(numero).replace('.', ',');
    }

    function campoModal(dialogo, nombre) {
        return dialogo.querySelector('[data-campo-modal="' + nombre + '"]');
    }

    // ---------- Filas e índices ----------

    /**
     * Renumera los campos de todas las filas de la sección (sin huecos y en el orden en que se ven) y
     * muestra u oculta el texto de tabla vacía.
     */
    function sincronizar(seccion) {
        var filas = seccion.querySelectorAll('tbody [data-fila]');
        filas.forEach(function (fila, indice) {
            var prefijo = fila.getAttribute('data-prefijo');
            fila.querySelectorAll('[data-campo]').forEach(function (campo) {
                campo.name = prefijo + '[' + indice + '].' + campo.getAttribute('data-campo');
            });
        });
        var vacia = seccion.querySelector('[data-fila-vacia]');
        if (vacia) { vacia.hidden = filas.length > 0; }
    }

    /** Valores de los campos ocultos de una fila existente, por nombre de campo. */
    function valoresDeFila(fila) {
        var valores = {};
        fila.querySelectorAll('[data-campo]').forEach(function (campo) {
            valores[campo.getAttribute('data-campo')] = campo.value;
        });
        return valores;
    }

    /**
     * Clave que identifica una fila para detectar repetidos, con las mismas reglas del service: una
     * malta o una levadura no se repite; un lúpulo no se repite con el mismo uso y tiempo de hervor (el
     * tiempo solo cuenta en el uso Hervor).
     */
    function claveDe(tipo, valores) {
        var id = valores[TIPOS[tipo].campoId];
        if (tipo !== 'lupulo') { return id; }
        var tiempo = valores.uso === 'HERVOR' ? Number(valores.tiempoDeHervor) : 0;
        return [id, valores.uso, tiempo].join('|');
    }

    function estaRepetido(tipo, seccion, valores) {
        var clave = claveDe(tipo, valores);
        return Array.prototype.some.call(seccion.querySelectorAll('tbody [data-fila]'), function (fila) {
            return claveDe(tipo, valoresDeFila(fila)) === clave;
        });
    }

    /** Clona la fila vacía de la sección, la completa con los valores y la agrega al final de la tabla. */
    function agregarFila(tipo, seccion, datos) {
        var plantilla = seccion.querySelector('template[data-plantilla]');
        var fila = plantilla.content.firstElementChild.cloneNode(true);

        fila.querySelectorAll('[data-campo]').forEach(function (campo) {
            var valor = datos.campos[campo.getAttribute('data-campo')];
            if (valor === undefined) {
                campo.remove(); // campo que no aplica (el tiempo de hervor fuera del uso Hervor)
            } else {
                campo.value = valor;
            }
        });
        fila.querySelectorAll('[data-celda]').forEach(function (celda) {
            var texto = datos.celdas[celda.getAttribute('data-celda')];
            if (texto !== undefined) { celda.textContent = texto; }
        });

        var tbody = seccion.querySelector('tbody');
        tbody.insertBefore(fila, tbody.querySelector('[data-fila-vacia]'));
        sincronizar(seccion);
    }

    // ---------- Ventana emergente ----------

    function mostrarError(dialogo, clave, mensaje) {
        var etiqueta = dialogo.querySelector('[data-error="' + clave + '"]');
        if (!etiqueta) { return; }
        etiqueta.textContent = mensaje || '';
        etiqueta.hidden = !mensaje;
    }

    /** Muestra solo los bloques que corresponden al uso elegido: tiempo de hervor (Hervor) o etapa (Dry hop). */
    function actualizarGrupos(dialogo) {
        var uso = campoModal(dialogo, 'uso');
        if (!uso) { return; }
        var tiempo = dialogo.querySelector('[data-grupo="tiempoDeHervor"]');
        var etapa = dialogo.querySelector('[data-grupo="etapaDeUso"]');
        tiempo.hidden = uso.value !== 'HERVOR';
        etapa.hidden = uso.value !== 'DRY_HOP';
    }

    /** Deja la ventana vacía, sin errores y con el estado inicial del uso del lúpulo. */
    function reiniciar(dialogo) {
        dialogo.querySelector('[data-selector-id]').value = '';
        dialogo.querySelector('[data-selector-texto]').value = '';
        dialogo.querySelectorAll('[data-campo-modal]').forEach(function (campo) { campo.value = ''; });
        dialogo.querySelectorAll('[data-error]').forEach(function (etiqueta) {
            etiqueta.textContent = '';
            etiqueta.hidden = true;
        });
        actualizarGrupos(dialogo);
    }

    function abrirAgregar(tipo) {
        var dialogo = document.getElementById('dialogo-agregar-' + tipo);
        reiniciar(dialogo);
        dialogo.showModal();
        dialogo.querySelector('[data-selector-texto]').focus();
    }

    /**
     * Valida lo ingresado en la ventana. Devuelve {errores, datos}: errores por clave de campo (vacío si
     * todo está bien) y, si es válido, los datos de la fila ({campos: ocultos, celdas: visibles}).
     */
    function leer(tipo, dialogo) {
        var cfg = TIPOS[tipo];
        var errores = {};
        var id = dialogo.querySelector('[data-selector-id]').value;
        var nombre = dialogo.querySelector('[data-selector-texto]').value;
        var cantidad = numeroDe(campoModal(dialogo, 'cantidad'));

        if (!id) { errores.insumo = cfg.mensajeInsumo; }
        if (!(cantidad > 0)) { errores.cantidad = 'La cantidad debe ser mayor a 0.'; }

        var campos = {};
        campos[cfg.campoId] = id;
        campos.cantidad = String(cantidad);
        var celdas = {nombre: nombre, cantidad: formatear(cantidad)};

        if (tipo === 'lupulo') {
            var uso = campoModal(dialogo, 'uso').value;
            var etapa = null;
            if (!uso) {
                errores.uso = 'Elegí el uso del lúpulo.';
            } else if (uso === 'HERVOR') {
                var tiempo = numeroDe(campoModal(dialogo, 'tiempoDeHervor'));
                if (!(tiempo > 0)) { errores.tiempoDeHervor = 'El tiempo de hervor debe ser mayor a 0.'; }
                etapa = 'HERVIDO'; // la etapa del Hervor la define el sistema, nunca se pregunta
                campos.tiempoDeHervor = String(tiempo);
            } else if (uso === 'WHIRLPOOL') {
                etapa = 'HERVIDO'; // idem para el Whirlpool
            } else {
                etapa = campoModal(dialogo, 'etapaDeUso').value;
                if (!etapa) { errores.etapaDeUso = 'Elegí la etapa de uso del dry hop.'; }
            }
            campos.uso = uso;
            campos.etapaDeUso = etapa;
            celdas.uso = USOS[uso];
            celdas.etapaDeUso = ETAPAS[etapa];
            celdas.tiempoDeHervor = uso === 'HERVOR' ? formatear(campos.tiempoDeHervor) : '-';
        }

        return {errores: errores, datos: {campos: campos, celdas: celdas}};
    }

    function confirmarAgregar(dialogo) {
        var tipo = dialogo.getAttribute('data-dialogo-agregar');
        var seccion = document.querySelector('[data-detalle="' + tipo + '"]');
        var resultado = leer(tipo, dialogo);

        // Un repetido solo se informa si lo demás está bien (así los datos que lo definen son válidos)
        if (Object.keys(resultado.errores).length === 0 && estaRepetido(tipo, seccion, resultado.datos.campos)) {
            resultado.errores.duplicado = TIPOS[tipo].mensajeDuplicado;
        }

        ['insumo', 'cantidad', 'uso', 'tiempoDeHervor', 'etapaDeUso', 'duplicado'].forEach(function (clave) {
            mostrarError(dialogo, clave, resultado.errores[clave]);
        });
        if (Object.keys(resultado.errores).length > 0) { return; }

        agregarFila(tipo, seccion, resultado.datos);
        dialogo.close();
    }

    // ---------- Eventos ----------

    document.addEventListener('click', function (evento) {
        var abrir = evento.target.closest('[data-abrir-agregar]');
        if (abrir) {
            abrirAgregar(abrir.getAttribute('data-abrir-agregar'));
            return;
        }
        var quitar = evento.target.closest('[data-quitar-fila]');
        if (quitar) {
            var fila = quitar.closest('[data-fila]');
            var seccion = fila.closest('[data-detalle]');
            fila.remove();
            sincronizar(seccion);
            return;
        }
        var confirmar = evento.target.closest('[data-confirmar-agregar]');
        if (confirmar) {
            confirmarAgregar(confirmar.closest('dialog'));
            return;
        }
        var cerrar = evento.target.closest('[data-cerrar-agregar]');
        if (cerrar) {
            cerrar.closest('dialog').close();
        }
    });

    document.addEventListener('change', function (evento) {
        var campo = evento.target.closest('[data-campo-modal]');
        if (!campo) { return; }
        var dialogo = campo.closest('dialog');
        if (campo.getAttribute('data-campo-modal') === 'uso') { actualizarGrupos(dialogo); }
        mostrarError(dialogo, campo.getAttribute('data-campo-modal'), null);
        mostrarError(dialogo, 'duplicado', null);
    });

    // Enter en un campo de la ventana equivale a "Agregar a la receta"
    document.addEventListener('keydown', function (evento) {
        var campo = evento.target.closest ? evento.target.closest('input[data-campo-modal]') : null;
        if (campo && evento.key === 'Enter') {
            evento.preventDefault();
            confirmarAgregar(campo.closest('dialog'));
        }
    });

    document.addEventListener('DOMContentLoaded', function () {
        var formulario = document.getElementById('receta-form');
        var secciones = document.querySelectorAll('[data-detalle]');
        secciones.forEach(sincronizar);
        // Última garantía antes de enviar: índices sin huecos en las tres tablas
        formulario.addEventListener('submit', function () { secciones.forEach(sincronizar); });
    });
})();
