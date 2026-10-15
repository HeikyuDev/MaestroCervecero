/*
 * Tooltips flotantes, compartidos por todas las pantallas.
 *
 * Un elemento con data-tooltip (texto simple) o data-tooltip-contenido (id de un elemento oculto cuyo contenido se
 * muestra) abre una tarjeta flotante con flecha. La tarjeta se posiciona sobre el contenido (position: fixed), por lo
 * que nunca empuja ni modifica el diseño de la página ni la recortan los contenedores con scroll.
 *
 *   Se abre con el mouse encima, con el foco del teclado y con un clic o toque (el clic la deja fija).
 *   Se cierra al sacar el mouse, al perder el foco, con Escape y con un clic fuera.
 *
 * No valida nada ni toca el servidor.
 */
(function () {
    'use strict';

    var MARGEN = 8;
    var SEPARACION = 10;
    var tarjeta = null;
    var disparadorActual = null;
    var fijadaPorClic = false;

    function crearTarjeta() {
        tarjeta = document.createElement('div');
        tarjeta.setAttribute('role', 'tooltip');
        tarjeta.id = 'tooltip-flotante';
        tarjeta.hidden = true;
        tarjeta.className = 'fixed left-0 top-0 z-[100] max-w-xs rounded-lg bg-slate-800 px-3 py-2 text-xs leading-relaxed text-slate-100 shadow-xl';
        // Estilos de posición en línea: la tarjeta se mide apenas se muestra, antes de que Tailwind genere sus clases
        tarjeta.style.cssText = 'position:fixed;left:0;top:0;z-index:100;max-width:20rem;width:max-content';

        var contenido = document.createElement('div');
        contenido.setAttribute('data-tooltip-texto', '');
        var flecha = document.createElement('span');
        flecha.setAttribute('data-tooltip-flecha', '');
        flecha.className = 'absolute h-2 w-2 rotate-45 bg-slate-800';

        tarjeta.appendChild(contenido);
        tarjeta.appendChild(flecha);
        document.body.appendChild(tarjeta);
    }

    function posicionar(disparador) {
        var flecha = tarjeta.querySelector('[data-tooltip-flecha]');
        var caja = disparador.getBoundingClientRect();
        var ancho = tarjeta.offsetWidth;
        var alto = tarjeta.offsetHeight;

        var abajo = caja.bottom + SEPARACION + alto <= window.innerHeight - MARGEN || caja.top - SEPARACION - alto < MARGEN;
        var arriba = abajo ? caja.bottom + SEPARACION : caja.top - SEPARACION - alto;
        var centro = caja.left + caja.width / 2;
        var izquierda = Math.min(Math.max(centro - ancho / 2, MARGEN), Math.max(window.innerWidth - ancho - MARGEN, MARGEN));

        tarjeta.style.transform = 'translate(' + Math.round(izquierda) + 'px, ' + Math.round(arriba) + 'px)';
        flecha.style.left = Math.round(Math.min(Math.max(centro - izquierda, 12), ancho - 12) - 4) + 'px';
        flecha.style.top = abajo ? '-4px' : '';
        flecha.style.bottom = abajo ? '' : '-4px';
    }

    function mostrar(disparador, porClic) {
        if (!tarjeta) { crearTarjeta(); }
        var contenido = tarjeta.querySelector('[data-tooltip-texto]');
        var origen = disparador.getAttribute('data-tooltip-contenido');
        if (origen) {
            var elemento = document.getElementById(origen);
            if (!elemento) { return; }
            contenido.innerHTML = elemento.innerHTML;
        } else {
            contenido.textContent = disparador.getAttribute('data-tooltip');
        }

        if (disparadorActual && disparadorActual !== disparador) { disparadorActual.removeAttribute('aria-describedby'); }
        disparadorActual = disparador;
        fijadaPorClic = !!porClic;
        disparador.setAttribute('aria-describedby', tarjeta.id);
        tarjeta.hidden = false;
        posicionar(disparador);
        requestAnimationFrame(function () { if (disparadorActual === disparador) { posicionar(disparador); } });
    }

    function ocultar() {
        if (!tarjeta || tarjeta.hidden) { return; }
        tarjeta.hidden = true;
        if (disparadorActual) { disparadorActual.removeAttribute('aria-describedby'); }
        disparadorActual = null;
        fijadaPorClic = false;
    }

    function disparadorDe(destino) {
        return destino && destino.closest ? destino.closest('[data-tooltip], [data-tooltip-contenido]') : null;
    }

    document.addEventListener('mouseover', function (evento) {
        var disparador = disparadorDe(evento.target);
        if (disparador && !fijadaPorClic) { mostrar(disparador, false); }
    });

    document.addEventListener('mouseout', function (evento) {
        var disparador = disparadorDe(evento.target);
        if (disparador && !fijadaPorClic && !disparador.contains(evento.relatedTarget)) { ocultar(); }
    });

    document.addEventListener('focusin', function (evento) {
        var disparador = disparadorDe(evento.target);
        if (disparador && !fijadaPorClic) { mostrar(disparador, false); }
    });

    document.addEventListener('focusout', function (evento) {
        if (disparadorDe(evento.target) && !fijadaPorClic) { ocultar(); }
    });

    document.addEventListener('click', function (evento) {
        var disparador = disparadorDe(evento.target);
        if (disparador) {
            if (disparador === disparadorActual && fijadaPorClic) { ocultar(); } else { mostrar(disparador, true); }
        } else if (!(tarjeta && tarjeta.contains(evento.target))) {
            ocultar();
        }
    });

    document.addEventListener('keydown', function (evento) {
        if (evento.key === 'Escape') { ocultar(); }
    });

    window.addEventListener('scroll', ocultar, true);
    window.addEventListener('resize', ocultar);
})();
