/*
 * Pantalla de envasados del lote: muestra los datos del barril que se elige para registrar el envasado.
 *
 * No valida nada (el servidor es el único que valida). Cuando el selector del barril emite 'selector:cambio'
 * (comportamiento de /js/modales.js), completa [data-barril-elegido] con el fabricante y la capacidad que trae el
 * botón "Elegir" del buscador (data-fabricante y data-capacidad).
 */
(function () {
    'use strict';

    document.addEventListener('selector:cambio', function (evento) {
        var panel = evento.target.closest('[data-registrar-envasado]');
        var resumen = panel ? panel.querySelector('[data-barril-elegido]') : null;
        if (!resumen) { return; }

        var barril = evento.detail || {};
        if (!barril.id) {
            resumen.hidden = true;
            return;
        }

        var datos = barril.datos || {};
        resumen.querySelector('[data-barril-fabricante]').textContent = datos.fabricante || '-';
        resumen.querySelector('[data-barril-capacidad]').textContent = datos.capacidad ? datos.capacidad + ' L' : '-';
        resumen.hidden = false;
    });
})();
