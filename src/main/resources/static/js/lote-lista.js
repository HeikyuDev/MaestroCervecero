/*
 * Lista de lotes: la fila completa abre el detalle del lote.
 *
 * Cada fila lleva en data-href la dirección del detalle. El código del lote es un enlace real (<a>), que es lo que
 * usan el teclado y los lectores de pantalla; este script solo suma el clic sobre el resto de la fila. Un clic sobre
 * un enlace o un botón, o al terminar de seleccionar texto, no navega. Con Ctrl o Cmd se abre en otra pestaña.
 */
(function () {
    'use strict';

    document.addEventListener('click', function (evento) {
        var fila = evento.target.closest('[data-fila-lote]');
        if (!fila || evento.target.closest('a, button, input, select, label')) { return; }

        var seleccion = window.getSelection ? window.getSelection().toString() : '';
        if (seleccion) { return; }

        var destino = fila.getAttribute('data-href');
        if (!destino) { return; }

        if (evento.ctrlKey || evento.metaKey) {
            window.open(destino, '_blank', 'noopener');
        } else {
            window.location.href = destino;
        }
    });
})();
