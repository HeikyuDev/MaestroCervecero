/*
 * Conserva el scroll entre navegaciones completas.
 *
 *   Sidebar (aside.sidebar-scroll): guarda su scrollTop en sessionStorage ("scroll-sidebar") al hacer clic en sus
 *   enlaces y al salir de la página, y lo restaura apenas se carga, para que no vuelva al inicio.
 *
 *   Pestañas (nav.border-b[aria-label] de fragments/pestanas y nav[data-pestanas] de las etapas del lote): al hacer
 *   clic en una pestaña guarda el scrollTop del <main> (el contenedor con scroll de la página) junto con el pathname
 *   de destino ("scroll-pestana"); la página cargada lo restaura solo si su pathname es ese. El resto de las
 *   navegaciones abre arriba, como siempre.
 *
 * Todo acceso a sessionStorage va en try/catch: sin almacenamiento la página funciona igual.
 */
(function () {
    'use strict';

    var CLAVE_SIDEBAR = 'scroll-sidebar';
    var CLAVE_PESTANA = 'scroll-pestana';

    function leer(clave) {
        try { return window.sessionStorage.getItem(clave); } catch (e) { return null; }
    }

    function guardar(clave, valor) {
        try { window.sessionStorage.setItem(clave, valor); } catch (e) { /* sin almacenamiento */ }
    }

    function borrar(clave) {
        try { window.sessionStorage.removeItem(clave); } catch (e) { /* sin almacenamiento */ }
    }

    var sidebar = document.querySelector('aside.sidebar-scroll');
    var principal = document.querySelector('main');

    if (sidebar) {
        var guardado = parseInt(leer(CLAVE_SIDEBAR), 10);
        if (!isNaN(guardado)) {
            // Se asigna ya y otra vez al terminar de cargar (fuentes e íconos pueden cambiar el alto del sidebar)
            sidebar.scrollTop = guardado;
            window.addEventListener('load', function () { sidebar.scrollTop = guardado; });
        }
        var guardarSidebar = function () { guardar(CLAVE_SIDEBAR, String(sidebar.scrollTop)); };
        sidebar.addEventListener('click', function (evento) {
            if (evento.target.closest && evento.target.closest('a')) { guardarSidebar(); }
        });
        window.addEventListener('pagehide', guardarSidebar);
    }

    if (principal) {
        var pendiente = leer(CLAVE_PESTANA);
        if (pendiente) {
            borrar(CLAVE_PESTANA);
            try {
                var dato = JSON.parse(pendiente);
                if (dato && dato.ruta === window.location.pathname && typeof dato.y === 'number') {
                    principal.scrollTop = dato.y;
                }
            } catch (e) { /* dato inválido: se ignora */ }
        }
        document.addEventListener('click', function (evento) {
            var enlace = evento.target.closest ? evento.target.closest('nav.border-b[aria-label] a, nav[data-pestanas] a') : null;
            if (!enlace || !enlace.href) { return; }
            var destino;
            try { destino = new URL(enlace.href, window.location.href); } catch (e) { return; }
            guardar(CLAVE_PESTANA, JSON.stringify({ ruta: destino.pathname, y: principal.scrollTop }));
        });
    }
})();
