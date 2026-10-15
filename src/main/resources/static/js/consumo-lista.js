/*
 * Formulario de un consumo: muestra los datos del lote de insumo que se elige y la unidad de la cantidad.
 *
 * No valida nada (el servidor es el único que valida). Cuando el selector del lote de insumo emite 'selector:cambio'
 * (comportamiento de /js/modales.js), completa [data-lote-elegido] con el vencimiento, la cantidad (reservada o
 * disponible, según el tipo de consumo) y la marca de vencido que trae el botón "Elegir" del buscador (data-vencimiento,
 * data-cantidad y data-vencido), y la unidad de la cantidad a consumir (data-unidad).
 */
(function () {
    'use strict';

    document.addEventListener('selector:cambio', function (evento) {
        var formulario = evento.target.closest('[data-formulario-consumo]');
        var resumen = formulario ? formulario.querySelector('[data-lote-elegido]') : null;
        if (!resumen) { return; }

        var unidad = formulario.querySelector('[data-unidad]');
        var lote = evento.detail || {};
        if (!lote.id) {
            resumen.hidden = true;
            if (unidad) { unidad.textContent = 'g o kg, según el insumo'; }
            return;
        }

        var datos = lote.datos || {};
        resumen.querySelector('[data-lote-vencimiento]').textContent = datos.vencimiento || '-';
        resumen.querySelector('[data-lote-cantidad]').textContent = datos.cantidad || '-';
        resumen.querySelector('[data-lote-cantidad-bloque]').hidden = false;
        resumen.querySelector('[data-lote-vencido]').hidden = datos.vencido !== 'true';
        resumen.hidden = false;
        if (unidad) { unidad.textContent = datos.unidad || 'g o kg, según el insumo'; }
    });
})();
