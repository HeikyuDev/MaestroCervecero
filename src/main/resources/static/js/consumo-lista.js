/*
 * Pantalla de consumos de una etapa: muestra los datos del lote de insumo que se elige en el consumo directo.
 *
 * No valida nada (el servidor es el único que valida). Cuando el selector del lote de insumo emite 'selector:cambio'
 * (comportamiento de /js/modales.js), completa [data-lote-elegido] con el vencimiento, el disponible y la marca de
 * vencido que trae el botón "Elegir" del buscador (data-vencimiento, data-disponible y data-vencido).
 */
(function () {
    'use strict';

    document.addEventListener('selector:cambio', function (evento) {
        var panel = evento.target.closest('[data-consumo-directo]');
        var resumen = panel ? panel.querySelector('[data-lote-elegido]') : null;
        if (!resumen) { return; }

        var lote = evento.detail || {};
        if (!lote.id) {
            resumen.hidden = true;
            return;
        }

        var datos = lote.datos || {};
        resumen.querySelector('[data-lote-vencimiento]').textContent = datos.vencimiento || '-';
        resumen.querySelector('[data-lote-disponible]').textContent = datos.disponible || '-';
        resumen.querySelector('[data-lote-vencido]').hidden = datos.vencido !== 'true';
        resumen.hidden = false;
    });
})();
