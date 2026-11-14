/*
 * Formulario de un ajuste de insumo: el insumo se elige primero y recién entonces se habilita el lote de insumo.
 *
 * No valida nada (el servidor es el único que valida). Cuando un selector del formulario emite 'selector:cambio'
 * (comportamiento de /js/modales.js):
 *
 *   - Selector del insumo (campo idInsumo): habilita el selector del lote de insumo y apunta su buscador a los lotes de
 *     ese insumo (parámetro idInsumo), quita el lote que estuviera elegido y su resumen, y muestra la unidad del insumo
 *     como sufijo de la cantidad (data-unidad del botón "Elegir"). Si se quita el insumo, vuelve a deshabilitar el lote.
 *   - Selector del lote de insumo (campo idLoteInsumo): completa [data-lote-elegido] con el vencimiento y la cantidad
 *     actual que trae el botón "Elegir" del buscador (data-vencimiento y data-cantidad), y la unidad del sufijo de la
 *     cantidad (data-unidad).
 *   - Selector del motivo (campo idMotivoAjuste): muestra su tipo (data-tipo del botón "Elegir").
 *   - Al escribir en un campo, se oculta el mensaje de error que mostraba debajo y se quita su marca roja (solo visual: el
 *     servidor vuelve a validar al enviar).
 *
 * Los campos ocultos data-unidad-insumo, data-vencimiento-lote y data-cantidad-lote guardan lo que se muestra del insumo
 * y del lote elegidos. Al volver del alta de un motivo desde el selector, /js/modales.js recupera lo tipeado en el
 * formulario, incluidos esos campos, y al cargar la página se vuelve a mostrar el estado del insumo y del lote.
 */
(function () {
    'use strict';

    function campoDe(selector) {
        return selector.querySelector('[data-selector-id]').name;
    }

    /** Muestra {unidad} en el sufijo de la cantidad ([data-unidad-sufijo]); sin unidad queda "-". */
    function fijarUnidad(formulario, unidad) {
        formulario.querySelectorAll('[data-unidad-sufijo]').forEach(function (sufijo) {
            sufijo.textContent = unidad ? (sufijo.getAttribute('data-prefijo') || '') + unidad : (sufijo.getAttribute('data-vacio') || '-');
        });
        var oculto = formulario.querySelector('[data-unidad-insumo]');
        if (oculto) { oculto.value = unidad || ''; }
    }

    /** Muestra el vencimiento y la cantidad actual del lote elegido; sin lote oculta el resumen. */
    function mostrarLote(formulario, vencimiento, cantidad) {
        var resumen = formulario.querySelector('[data-lote-elegido]');
        var elegido = !!(vencimiento || cantidad);
        if (resumen) {
            resumen.hidden = !elegido;
            resumen.querySelector('[data-lote-vencimiento]').textContent = vencimiento || '-';
            resumen.querySelector('[data-lote-cantidad]').textContent = cantidad || '-';
        }
        var ocultoVencimiento = formulario.querySelector('[data-vencimiento-lote]');
        if (ocultoVencimiento) { ocultoVencimiento.value = elegido ? (vencimiento || '') : ''; }
        var ocultoCantidad = formulario.querySelector('[data-cantidad-lote]');
        if (ocultoCantidad) { ocultoCantidad.value = elegido ? (cantidad || '') : ''; }
    }

    /** Habilita o deshabilita el selector del lote y apunta su buscador a los lotes del insumo {idInsumo}. */
    function configurarLote(formulario, idInsumo) {
        var contenedor = formulario.querySelector('[data-selector-lote]');
        if (!contenedor) { return null; }
        var lote = contenedor.querySelector('[data-selector]');
        var elegido = !!idInsumo;
        var base = contenedor.getAttribute('data-buscador-base');
        lote.setAttribute('data-buscador-url', elegido ? base + '?idInsumo=' + encodeURIComponent(idInsumo) : base);
        if (elegido) { contenedor.removeAttribute('data-deshabilitado'); } else { contenedor.setAttribute('data-deshabilitado', ''); }
        contenedor.classList.toggle('opacity-50', !elegido);
        lote.querySelector('[data-selector-texto]').setAttribute('aria-disabled', elegido ? 'false' : 'true');
        var ayuda = formulario.querySelector('[data-ayuda-lote]');
        if (ayuda) { ayuda.hidden = elegido; }
        return lote;
    }

    function alCambiarInsumo(formulario, insumo) {
        var lote = configurarLote(formulario, insumo.id);
        if (lote) {
            lote.querySelector('[data-selector-id]').value = '';
            lote.querySelector('[data-selector-texto]').value = '';
        }
        mostrarLote(formulario, '', '');
        var datos = insumo.datos || {};
        fijarUnidad(formulario, insumo.id && datos.unidad ? datos.unidad : '');
    }

    function alCambiarLote(formulario, lote) {
        if (!lote.id) {
            mostrarLote(formulario, '', '');
            return;
        }
        var datos = lote.datos || {};
        mostrarLote(formulario, datos.vencimiento || '-', datos.cantidad || '-');
        if (datos.unidad) { fijarUnidad(formulario, datos.unidad); }
    }

    function alCambiarMotivo(formulario, motivo) {
        var resumen = formulario.querySelector('[data-motivo-elegido]');
        if (!resumen) { return; }
        var datos = motivo.datos || {};
        resumen.hidden = !motivo.id;
        resumen.querySelector('[data-motivo-tipo]').textContent = motivo.id ? (datos.tipo || '') : '';
    }

    /** Oculta el error de {entrada} (el párrafo rojo de su contenedor) y quita la marca roja del campo. */
    function limpiarErrorDelCampo(entrada) {
        var grupo = entrada.closest('[data-grupo-unidad]');
        var contenedor = grupo ? grupo.parentElement : entrada.parentElement;
        if (!contenedor) { return; }
        contenedor.querySelectorAll('p.text-rose-600').forEach(function (error) { error.hidden = true; });
        if (entrada.classList.contains('border-rose-400')) {
            entrada.classList.remove('border-rose-400', 'bg-rose-50');
            entrada.classList.add('border-slate-300');
            var sufijo = grupo ? grupo.querySelector('[data-unidad-sufijo]') : null;
            if (sufijo) {
                sufijo.classList.remove('border-rose-400');
                sufijo.classList.add('border-slate-300');
            }
        }
    }

    /** Vuelve a mostrar el estado del insumo y del lote según los campos del formulario (por ejemplo, tras recuperar lo tipeado). */
    function sincronizar(formulario) {
        var insumo = formulario.querySelector('[data-selector] [data-selector-id][name="idInsumo"]');
        if (!insumo) { return; }
        configurarLote(formulario, insumo.value);
        var unidad = formulario.querySelector('[data-unidad-insumo]');
        if (insumo.value && unidad && unidad.value) { fijarUnidad(formulario, unidad.value); }
        var lote = formulario.querySelector('[data-selector] [data-selector-id][name="idLoteInsumo"]');
        if (lote && lote.value) {
            var vencimiento = formulario.querySelector('[data-vencimiento-lote]');
            var cantidad = formulario.querySelector('[data-cantidad-lote]');
            mostrarLote(formulario, vencimiento ? vencimiento.value : '', cantidad ? cantidad.value : '');
        }
    }

    document.addEventListener('selector:cambio', function (evento) {
        var formulario = evento.target.closest('[data-formulario-ajuste]');
        if (!formulario) { return; }
        var detalle = evento.detail || {};

        var campo = campoDe(evento.target);
        if (campo === 'idInsumo') {
            alCambiarInsumo(formulario, detalle);
        } else if (campo === 'idLoteInsumo') {
            alCambiarLote(formulario, detalle);
        } else if (campo === 'idMotivoAjuste') {
            alCambiarMotivo(formulario, detalle);
        }
    });

    document.addEventListener('input', function (evento) {
        var formulario = evento.target.closest ? evento.target.closest('[data-formulario-ajuste]') : null;
        if (formulario && evento.target.matches && evento.target.matches('input:not([data-selector-texto]), select, textarea')) {
            limpiarErrorDelCampo(evento.target);
        }
    });

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('[data-formulario-ajuste]').forEach(sincronizar);
    });
})();
