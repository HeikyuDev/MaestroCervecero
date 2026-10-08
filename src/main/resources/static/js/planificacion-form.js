/*
 * Formulario de planificación de producción: mecánica de pantalla del botón "Sugerir fecha".
 *
 * No valida nada (el servidor es el único que valida). Al apretar el botón [data-sugerir-fecha] pide al
 * servidor el fragmento HTML de la sugerencia con la receta, la cantidad y el inicio que hay en ese momento
 * y lo muestra en [data-sugerencia-destino]. Nunca corre solo.
 *
 * La fecha de fin solo cambia porque el usuario apretó el botón:
 *   - si el campo está vacío, o su valor lo puso antes este botón, se completa con la fecha sugerida;
 *   - si el usuario ya escribió una fecha, se muestra la sugerencia con un enlace "Usar esta fecha" y el
 *     campo no se toca hasta que lo use.
 */
(function () {
    'use strict';

    var MENSAJE_FALLO = 'No se pudo obtener la sugerencia. Intentá nuevamente.';

    function valorDe(selector) {
        var campo = document.querySelector(selector);
        return campo ? campo.value : '';
    }

    function mostrarFallo(destino) {
        var aviso = document.createElement('p');
        aviso.className = 'text-xs text-rose-600';
        aviso.textContent = MENSAJE_FALLO;
        destino.replaceChildren(aviso);
    }

    function aplicarSugerencia(destino, campoFin) {
        var sugerencia = destino.querySelector('[data-fecha-sugerida]');
        if (!sugerencia) { return; }

        var fecha = sugerencia.getAttribute('data-fecha-sugerida');
        var completarlo = campoFin.value === '' || campoFin.dataset.autocompletada === 'true';
        if (completarlo) {
            campoFin.value = fecha;
            campoFin.dataset.autocompletada = 'true';
            return;
        }

        var enlace = document.createElement('button');
        enlace.type = 'button';
        enlace.className = 'ml-1 font-semibold text-amber-600 underline hover:text-amber-700';
        enlace.textContent = 'Usar esta fecha';
        enlace.addEventListener('click', function () {
            campoFin.value = fecha;
            campoFin.dataset.autocompletada = 'true';
            enlace.remove();
        });
        sugerencia.appendChild(enlace);
    }

    function sugerir(boton) {
        var destino = document.querySelector('[data-sugerencia-destino]');
        var campoFin = document.getElementById('fechaFinalizacionEstimada');
        if (!destino || !campoFin) { return; }

        var parametros = new URLSearchParams({
            idReceta: valorDe('input[name="idReceta"]'),
            cantidadAProducir: valorDe('#cantidadAProducir'),
            fechaInicioEstimada: valorDe('#fechaInicioEstimada')
        });

        fetch(boton.getAttribute('data-sugerencia-url') + '?' + parametros,
            {headers: {'X-Requested-With': 'fetch'}, credentials: 'same-origin'})
            .then(function (respuesta) { return respuesta.text(); })
            .then(function (html) {
                destino.innerHTML = html;
                aplicarSugerencia(destino, campoFin);
            })
            .catch(function () { mostrarFallo(destino); });
    }

    document.addEventListener('click', function (evento) {
        var boton = evento.target.closest('[data-sugerir-fecha]');
        if (boton) { sugerir(boton); }
    });

    // Lo que el usuario escribe a mano deja de ser una fecha puesta por el botón
    document.addEventListener('input', function (evento) {
        if (evento.target.id === 'fechaFinalizacionEstimada') {
            delete evento.target.dataset.autocompletada;
        }
    });
})();
