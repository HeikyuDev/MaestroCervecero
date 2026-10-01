/*
 * Modales y selectores de entidad.
 *
 * El servidor responde siempre HTML (fragmentos de Thymeleaf); este archivo solo los muestra en
 * un <dialog> y reacciona a los atributos data-* que traen esos fragmentos:
 *
 *   data-modal-url="/x/nuevo"      botón/enlace que abre ese fragmento en un modal.
 *   data-baja-url / data-baja-nombre
 *                                  botón que abre el diálogo de confirmación de baja (#dialogo-baja).
 *   data-selector                  campo de selección de una entidad relacionada (buscar / crear / limpiar).
 *
 * Dentro de un modal:
 *   data-cerrar-modal              cierra el modal.
 *   data-modal-buscar (form GET)   vuelve a pedir el fragmento con los criterios de filtro.
 *   data-modal-get (enlace)        paginación: pide el fragmento de otra página.
 *   data-elegir data-id data-texto fila elegida de un buscador.
 *   data-modal-form (form POST)    se envía con fetch: éxito normal -> recarga la página (el flash del
 *                                  Post-Redirect-Get se muestra al recargar); 201 -> alta al vuelo, devuelve la
 *                                  entidad creada (data-resultado); otro estado -> muestra el formulario con errores.
 */
(function () {
    'use strict';

    var CLASES_MODAL = 'm-auto w-full max-w-2xl rounded-xl p-0 shadow-xl backdrop:bg-slate-900/50';
    var CABECERAS = {'X-Requested-With': 'fetch'};

    function pedirHtml(url) {
        return fetch(url, {headers: CABECERAS, credentials: 'same-origin'}).then(function (respuesta) {
            return respuesta.text();
        });
    }

    /** Abre {url} en un modal. alElegir recibe {id, texto} cuando el usuario elige o crea una entidad. */
    function abrirModal(url, alElegir) {
        var dialogo = document.createElement('dialog');
        dialogo.className = CLASES_MODAL;
        document.body.appendChild(dialogo);
        dialogo.addEventListener('close', function () { dialogo.remove(); });

        function mostrar(url) {
            return pedirHtml(url).then(function (html) { dialogo.innerHTML = html; });
        }

        function devolver(resultado) {
            dialogo.close();
            if (alElegir) { alElegir(resultado); }
        }

        function enviar(formulario) {
            fetch(formulario.action, {
                method: 'POST',
                headers: CABECERAS,
                credentials: 'same-origin',
                redirect: 'manual',
                body: new URLSearchParams(new FormData(formulario))
            }).then(function (respuesta) {
                if (respuesta.type === 'opaqueredirect') {
                    window.location.reload();
                    return null;
                }
                return respuesta.text().then(function (html) {
                    var plantilla = document.createElement('template');
                    plantilla.innerHTML = html;
                    var resultado = respuesta.status === 201 ? plantilla.content.querySelector('[data-resultado]') : null;
                    if (resultado) {
                        devolver({id: resultado.getAttribute('data-id'), texto: resultado.getAttribute('data-texto')});
                    } else {
                        dialogo.innerHTML = html;
                    }
                });
            });
        }

        dialogo.addEventListener('click', function (evento) {
            var elegido = evento.target.closest('[data-elegir]');
            if (elegido) {
                devolver({id: elegido.getAttribute('data-id'), texto: elegido.getAttribute('data-texto')});
            } else if (evento.target.closest('[data-cerrar-modal]')) {
                dialogo.close();
            } else {
                var enlace = evento.target.closest('a[data-modal-get]');
                if (enlace) {
                    evento.preventDefault();
                    mostrar(enlace.href);
                }
            }
        });

        dialogo.addEventListener('submit', function (evento) {
            var formulario = evento.target;
            if (formulario.hasAttribute('data-modal-buscar')) {
                evento.preventDefault();
                mostrar(formulario.action + '?' + new URLSearchParams(new FormData(formulario)));
            } else if (formulario.hasAttribute('data-modal-form')) {
                evento.preventDefault();
                enviar(formulario);
            }
        });

        mostrar(url).then(function () { dialogo.showModal(); }, function () { dialogo.remove(); });
    }

    function abrirConfirmacionBaja(boton) {
        var dialogo = document.getElementById('dialogo-baja');
        if (!dialogo) { return; }
        dialogo.querySelector('form').setAttribute('action', boton.getAttribute('data-baja-url'));
        dialogo.querySelector('[data-baja-nombre-destino]').textContent = boton.getAttribute('data-baja-nombre');
        dialogo.showModal();
    }

    function accionSelector(boton) {
        var selector = boton.closest('[data-selector]');
        function asignar(entidad) {
            selector.querySelector('[data-selector-id]').value = entidad.id;
            selector.querySelector('[data-selector-texto]').value = entidad.texto;
        }

        if (boton.hasAttribute('data-selector-buscar')) {
            abrirModal(selector.getAttribute('data-buscador-url'), asignar);
        } else if (boton.hasAttribute('data-selector-alta')) {
            abrirModal(selector.getAttribute('data-alta-url'), asignar);
        } else {
            asignar({id: '', texto: ''});
        }
    }

    document.addEventListener('click', function (evento) {
        var abrir = evento.target.closest('[data-modal-url]');
        if (abrir) {
            abrirModal(abrir.getAttribute('data-modal-url'), null);
            return;
        }
        var baja = evento.target.closest('[data-baja-url]');
        if (baja) {
            abrirConfirmacionBaja(baja);
            return;
        }
        var cerrar = evento.target.closest('#dialogo-baja [data-cerrar-dialogo]');
        if (cerrar) {
            document.getElementById('dialogo-baja').close();
            return;
        }
        var accion = evento.target.closest('[data-selector-buscar], [data-selector-alta], [data-selector-limpiar]');
        if (accion) {
            accionSelector(accion);
        }
    });
})();
