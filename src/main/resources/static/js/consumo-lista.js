/*
 * Formulario de un consumo: el insumo se elige primero y recién entonces se habilita el lote de insumo.
 *
 * No valida nada (el servidor es el único que valida). Cuando un selector del formulario emite 'selector:cambio'
 * (comportamiento de /js/modales.js):
 *
 *   - Selector del insumo (campo idInsumo): habilita el selector del lote de insumo y apunta su buscador a los lotes de
 *     ese insumo (parámetro idInsumo), quita el lote que estuviera elegido y su resumen, y muestra la unidad del insumo
 *     en la cantidad a consumir (data-unidad del botón "Elegir"). Si se quita el insumo, vuelve a deshabilitar el lote.
 *   - Selector del lote de insumo (campo idLoteInsumo): completa [data-lote-elegido] con el vencimiento, la cantidad
 *     (reservada o disponible, según el tipo de consumo) y la marca de vencido que trae el botón "Elegir" del buscador
 *     (data-vencimiento, data-cantidad y data-vencido), y la unidad de la cantidad a consumir (data-unidad).
 */
(function () {
    'use strict';

    var UNIDAD_SIN_ELEGIR = 'g o kg, según el insumo';

    function campoDe(selector) {
        return selector.querySelector('[data-selector-id]').name;
    }

    function ocultarLote(formulario, unidad, textoUnidad) {
        var resumen = formulario.querySelector('[data-lote-elegido]');
        if (resumen) { resumen.hidden = true; }
        if (unidad) { unidad.textContent = textoUnidad; }
    }

    function alCambiarInsumo(formulario, selector, insumo) {
        var contenedor = formulario.querySelector('[data-selector-lote]');
        var unidad = formulario.querySelector('[data-unidad]');
        if (!contenedor) { return; }

        var lote = contenedor.querySelector('[data-selector]');
        lote.querySelector('[data-selector-id]').value = '';
        lote.querySelector('[data-selector-texto]').value = '';

        var elegido = !!insumo.id;
        var base = contenedor.getAttribute('data-buscador-base');
        lote.setAttribute('data-buscador-url', elegido ? base + '?idInsumo=' + encodeURIComponent(insumo.id) : base);
        if (elegido) { contenedor.removeAttribute('data-deshabilitado'); } else { contenedor.setAttribute('data-deshabilitado', ''); }
        contenedor.classList.toggle('opacity-50', !elegido);
        lote.querySelector('[data-selector-texto]').setAttribute('aria-disabled', elegido ? 'false' : 'true');

        var ayuda = formulario.querySelector('[data-ayuda-lote]');
        if (ayuda) { ayuda.hidden = elegido; }

        var datos = insumo.datos || {};
        ocultarLote(formulario, unidad, elegido && datos.unidad ? datos.unidad : UNIDAD_SIN_ELEGIR);
    }

    function alCambiarLote(formulario, lote) {
        var resumen = formulario.querySelector('[data-lote-elegido]');
        if (!resumen) { return; }

        var unidad = formulario.querySelector('[data-unidad]');
        if (!lote.id) {
            resumen.hidden = true;
            return;
        }

        var datos = lote.datos || {};
        resumen.querySelector('[data-lote-vencimiento]').textContent = datos.vencimiento || '-';
        resumen.querySelector('[data-lote-cantidad]').textContent = datos.cantidad || '-';
        resumen.querySelector('[data-lote-cantidad-bloque]').hidden = false;
        resumen.querySelector('[data-lote-vencido]').hidden = datos.vencido !== 'true';
        resumen.hidden = false;
        if (unidad && datos.unidad) { unidad.textContent = datos.unidad; }
    }

    document.addEventListener('selector:cambio', function (evento) {
        var formulario = evento.target.closest('[data-formulario-consumo]');
        if (!formulario) { return; }

        if (campoDe(evento.target) === 'idInsumo') {
            alCambiarInsumo(formulario, evento.target, evento.detail || {});
        } else {
            alCambiarLote(formulario, evento.detail || {});
        }
    });
})();
