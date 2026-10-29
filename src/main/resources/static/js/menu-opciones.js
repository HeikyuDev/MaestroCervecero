/*
 * Menú de opciones (fragments/menu-opciones.html) y diálogos informativos que abre.
 *
 *   data-menu-opciones       contenedor del menú.
 *   data-menu-abrir          botón de tres puntos: abre o cierra el panel (aria-expanded).
 *   data-menu-panel          panel con las opciones; oculto mientras el menú está cerrado.
 *   data-menu-opcion         cada opción (button role="menuitem").
 *   data-abrir-dialogo="id"  opción (o botón) que abre el <dialog> con ese id y cierra el menú.
 *   data-dialogo-informativo diálogo de solo lectura: su botón data-cerrar-dialogo lo cierra y un clic en el fondo también.
 *
 * Se cierra con clic afuera o con Escape (el foco vuelve al botón). Teclado: Enter, Espacio o flecha abajo abren el menú y
 * ponen el foco en la primera opción; las flechas recorren las opciones; Tab cierra el menú.
 */
(function () {
    'use strict';

    function panelDe(menu) { return menu.querySelector('[data-menu-panel]'); }
    function botonDe(menu) { return menu.querySelector('[data-menu-abrir]'); }

    function cerrar(menu, devolverFoco) {
        var panel = panelDe(menu);
        if (panel.hidden) { return; }
        panel.hidden = true;
        botonDe(menu).setAttribute('aria-expanded', 'false');
        if (devolverFoco) { botonDe(menu).focus(); }
    }

    function abrir(menu, enfocarPrimera) {
        document.querySelectorAll('[data-menu-opciones]').forEach(function (otro) { if (otro !== menu) { cerrar(otro, false); } });
        panelDe(menu).hidden = false;
        botonDe(menu).setAttribute('aria-expanded', 'true');
        if (enfocarPrimera) {
            var primera = panelDe(menu).querySelector('[data-menu-opcion]');
            if (primera) { primera.focus(); }
        }
    }

    function cerrarTodos(devolverFoco) {
        document.querySelectorAll('[data-menu-opciones]').forEach(function (menu) { cerrar(menu, devolverFoco); });
    }

    document.addEventListener('click', function (evento) {
        var boton = evento.target.closest('[data-menu-abrir]');
        if (boton) {
            var menu = boton.closest('[data-menu-opciones]');
            if (panelDe(menu).hidden) { abrir(menu, false); } else { cerrar(menu, false); }
            return;
        }
        var abrirDialogo = evento.target.closest('[data-abrir-dialogo]');
        if (abrirDialogo) {
            var dialogo = document.getElementById(abrirDialogo.getAttribute('data-abrir-dialogo'));
            cerrarTodos(false);
            if (dialogo && !dialogo.open) { dialogo.showModal(); }
            return;
        }
        var cierre = evento.target.closest('dialog[data-dialogo-informativo] [data-cerrar-dialogo]');
        if (cierre) {
            cierre.closest('dialog').close();
            return;
        }
        // Clic en el fondo de un diálogo informativo (el propio <dialog>, fuera de su contenido)
        if (evento.target.matches && evento.target.matches('dialog[data-dialogo-informativo]')) {
            evento.target.close();
            return;
        }
        if (!evento.target.closest('[data-menu-opciones]')) { cerrarTodos(false); }
    });

    document.addEventListener('keydown', function (evento) {
        var menu = evento.target.closest ? evento.target.closest('[data-menu-opciones]') : null;
        if (evento.key === 'Escape') {
            cerrarTodos(true);
            return;
        }
        if (!menu) { return; }
        var abierto = !panelDe(menu).hidden;
        var opciones = Array.prototype.slice.call(panelDe(menu).querySelectorAll('[data-menu-opcion]'));
        var indice = opciones.indexOf(document.activeElement);
        if (evento.target === botonDe(menu) && evento.key === 'ArrowDown') {
            evento.preventDefault();
            abrir(menu, true);
        } else if (abierto && evento.key === 'ArrowDown' && opciones.length) {
            evento.preventDefault();
            opciones[(indice + 1) % opciones.length].focus();
        } else if (abierto && evento.key === 'ArrowUp' && opciones.length) {
            evento.preventDefault();
            opciones[(indice - 1 + opciones.length) % opciones.length].focus();
        } else if (abierto && evento.key === 'Tab') {
            cerrar(menu, false);
        }
    });
})();
