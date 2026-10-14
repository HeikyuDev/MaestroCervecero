/*
 * Detalle del lote: botones de información que despliegan una explicación.
 *
 * Cada botón [data-info-toggle] controla el panel cuyo id indica en aria-controls. Al hacer clic alterna el atributo
 * hidden del panel y mantiene aria-expanded en sincronía, de modo que el estado también lo anuncian los lectores de
 * pantalla. Con Escape se cierra el panel abierto y el foco vuelve al botón. No valida nada ni toca el servidor.
 */
(function () {
    'use strict';

    function alternar(boton, mostrar) {
        var panel = document.getElementById(boton.getAttribute('aria-controls'));
        if (!panel) { return; }
        panel.hidden = !mostrar;
        boton.setAttribute('aria-expanded', mostrar ? 'true' : 'false');
    }

    document.addEventListener('click', function (evento) {
        var boton = evento.target.closest('[data-info-toggle]');
        if (!boton) { return; }
        alternar(boton, boton.getAttribute('aria-expanded') !== 'true');
    });

    document.addEventListener('keydown', function (evento) {
        if (evento.key !== 'Escape') { return; }
        document.querySelectorAll('[data-info-toggle][aria-expanded="true"]').forEach(function (boton) {
            alternar(boton, false);
            boton.focus();
        });
    });
})();
