/*
 * Pantalla de planes de producción (planificacion_produccion/planes.html).
 *
 * El servidor responde siempre HTML; este archivo no valida ni calcula nada:
 *
 *   [data-pestana-secuencial] / [data-pestana-concurrente]
 *                                 pestañas: muestran el panel [data-panel-plan] de su plan sin recargar la página,
 *                                 para no perder las modificaciones (sin JavaScript, cada pestaña es su URL).
 *   [data-modificar-lote]         abre el modal #dialogo-lote-plan con el volumen y los equipos del lote de su fila.
 *   [data-aplicar-lote-plan]      copia los valores del modal a los campos ocultos de la fila y envía el formulario
 *                                 del plan a la URL de recálculo (data-url-recalcular del modal). Si el servidor
 *                                 devuelve el plan, reemplaza el plan mostrado; si devuelve [data-error-recalculo],
 *                                 restaura los valores anteriores y muestra los motivos en el modal. Al
 *                                 reemplazar el plan quita el aviso [data-errores-plan] de una confirmación rechazada.
 *   [data-cerrar-lote-plan]       cierra el modal sin cambiar nada.
 */
(function () {
    'use strict';

    var MENSAJE_NO_DISPONIBLE = 'No se pudo recalcular el plan, actualice la página e intente nuevamente';
    var MENSAJE_SESION_FINALIZADA = 'Su sesión finalizó: inicie sesión nuevamente';
    var CLASES_ACTIVA = ['border-amber-500', 'bg-white', 'text-amber-600'];
    var CLASES_INACTIVA = ['border-slate-200', 'bg-slate-50', 'text-slate-600', 'hover:border-amber-300', 'hover:bg-amber-50', 'hover:text-amber-700'];
    var CAMPOS_EQUIPO = {
        idFermentador: 'data-texto-id-fermentador',
        idMolino: 'data-texto-id-molino',
        idMacerador: 'data-texto-id-macerador',
        idOllaHervor: 'data-texto-id-olla-hervor'
    };

    var filaEnEdicion = null;

    // ---------- Pestañas ----------

    function mostrarPestana(modalidad) {
        document.querySelectorAll('[data-panel-plan]').forEach(function (panel) {
            panel.hidden = panel.getAttribute('data-panel-plan') !== modalidad;
        });
        [['SECUENCIAL', '[data-pestana-secuencial]'], ['CONCURRENTE', '[data-pestana-concurrente]']].forEach(function (par) {
            var pestana = document.querySelector(par[1]);
            if (!pestana) { return; }
            var activa = par[0] === modalidad;
            CLASES_ACTIVA.forEach(function (clase) { pestana.classList.toggle(clase, activa); });
            CLASES_INACTIVA.forEach(function (clase) { pestana.classList.toggle(clase, !activa); });
            if (activa) {
                pestana.setAttribute('aria-current', 'page');
                history.replaceState(null, '', pestana.getAttribute('href'));
            } else {
                pestana.removeAttribute('aria-current');
            }
        });
    }

    // ---------- Modal del lote ----------

    function dialogo() {
        return document.getElementById('dialogo-lote-plan');
    }

    function campoDeFila(fila, nombre) {
        return fila.querySelector('[data-campo-lote="' + nombre + '"]');
    }

    function mostrarErrores(html) {
        var destino = dialogo().querySelector('[data-errores-lote-plan]');
        if (html) {
            destino.innerHTML = html;
            destino.hidden = false;
        } else {
            destino.replaceChildren();
            destino.hidden = true;
        }
    }

    function mostrarMensaje(mensaje) {
        var parrafo = document.createElement('p');
        parrafo.textContent = mensaje;
        var destino = dialogo().querySelector('[data-errores-lote-plan]');
        destino.replaceChildren(parrafo);
        destino.hidden = false;
    }

    function abrirModal(fila) {
        var modal = dialogo();
        filaEnEdicion = fila;
        mostrarErrores(null);
        modal.querySelector('[data-numero-lote]').textContent = fila.getAttribute('data-orden');
        modal.querySelector('[data-campo-modal="volumenObjetivo"]').value = campoDeFila(fila, 'volumenObjetivo').value;
        Object.keys(CAMPOS_EQUIPO).forEach(function (nombre) {
            var selector = modal.querySelector('[data-campo-equipo="' + nombre + '"] [data-selector]');
            selector.querySelector('[data-selector-id]').value = campoDeFila(fila, nombre).value;
            selector.querySelector('[data-selector-texto]').value = fila.getAttribute(CAMPOS_EQUIPO[nombre]) || '';
        });
        modal.showModal();
    }

    function aplicarCambios() {
        var modal = dialogo();
        var fila = filaEnEdicion;
        if (!fila) { return; }
        var formulario = fila.closest('form');
        var anteriores = {};

        // Copia los valores del modal a los campos ocultos de la fila, guardando los anteriores para restaurarlos
        anteriores.volumenObjetivo = campoDeFila(fila, 'volumenObjetivo').value;
        campoDeFila(fila, 'volumenObjetivo').value = modal.querySelector('[data-campo-modal="volumenObjetivo"]').value;
        Object.keys(CAMPOS_EQUIPO).forEach(function (nombre) {
            anteriores[nombre] = campoDeFila(fila, nombre).value;
            campoDeFila(fila, nombre).value = modal.querySelector('[data-campo-equipo="' + nombre + '"] [data-selector-id]').value;
        });

        function restaurar() {
            Object.keys(anteriores).forEach(function (nombre) { campoDeFila(fila, nombre).value = anteriores[nombre]; });
        }

        var boton = modal.querySelector('[data-aplicar-lote-plan]');
        boton.disabled = true;
        fetch(modal.getAttribute('data-url-recalcular'), {
            method: 'POST',
            body: new URLSearchParams(new FormData(formulario)),
            headers: {'X-Requested-With': 'fetch'},
            credentials: 'same-origin'
        }).then(function (respuesta) {
            if (respuesta.redirected) {
                restaurar();
                mostrarMensaje(respuesta.url.indexOf('/login') !== -1 ? MENSAJE_SESION_FINALIZADA : MENSAJE_NO_DISPONIBLE);
                return;
            }
            return respuesta.text().then(function (html) {
                var plantilla = document.createElement('template');
                plantilla.innerHTML = html;
                var error = plantilla.content.querySelector('[data-error-recalculo]');
                var plan = plantilla.content.querySelector('[data-plan]');
                if (error && error.getAttribute('data-redirigir')) {
                    window.location.href = error.getAttribute('data-redirigir');
                    return;
                }
                if (!respuesta.ok || error || !plan) {
                    restaurar();
                    if (error) {
                        mostrarErrores(error.innerHTML);
                    } else {
                        mostrarMensaje(MENSAJE_NO_DISPONIBLE);
                    }
                    return;
                }
                fila.closest('[data-plan]').replaceWith(plan);
                // Los motivos de una confirmación rechazada se refieren al plan anterior: dejan de valer al recalcularlo
                var erroresAnteriores = document.querySelector('[data-errores-plan]');
                if (erroresAnteriores) { erroresAnteriores.remove(); }
                filaEnEdicion = null;
                modal.close();
            });
        }).catch(function () {
            restaurar();
            mostrarMensaje(MENSAJE_NO_DISPONIBLE);
        }).finally(function () {
            boton.disabled = false;
        });
    }

    // ---------- Eventos ----------

    document.addEventListener('click', function (evento) {
        var pestana = evento.target.closest('[data-pestana-secuencial], [data-pestana-concurrente]');
        if (pestana) {
            evento.preventDefault();
            mostrarPestana(pestana.hasAttribute('data-pestana-secuencial') ? 'SECUENCIAL' : 'CONCURRENTE');
            return;
        }
        var modificar = evento.target.closest('[data-modificar-lote]');
        if (modificar) {
            abrirModal(modificar.closest('[data-lote-plan]'));
            return;
        }
        if (evento.target.closest('[data-aplicar-lote-plan]')) {
            aplicarCambios();
            return;
        }
        if (evento.target.closest('[data-cerrar-lote-plan]')) {
            filaEnEdicion = null;
            dialogo().close();
        }
    });
})();
