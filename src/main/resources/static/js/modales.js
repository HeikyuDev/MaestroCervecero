/*
 * Selectores de entidad y confirmación de baja.
 *
 * El servidor responde siempre HTML (fragmentos de Thymeleaf); este archivo solo reacciona a los
 * atributos data-* de las páginas:
 *
 *   data-selector                 campo de selección de una entidad relacionada. Sus botones:
 *     data-selector-buscar        abre el buscador (modal con criterios de filtro) y deja elegida la fila.
 *     data-selector-alta          lleva al alta de esa entidad y, al guardar, vuelve a esta página con
 *                                 lo tipeado recuperado y la entidad nueva elegida (alta contextual).
 *     data-selector-limpiar       quita la selección.
 *   data-baja-url / data-baja-nombre
 *                                 botón que abre el diálogo de confirmación de baja (#dialogo-baja).
 *
 * Dentro del modal del buscador:
 *   data-cerrar-modal             cierra el modal.
 *   data-modal-buscar (form GET)  vuelve a pedir el fragmento con los criterios de filtro.
 *   data-modal-get (enlace)       paginación: pide el fragmento de otra página.
 *   data-elegir data-id data-texto  fila elegida.
 */
(function () {
    'use strict';

    var CLASES_MODAL = 'm-auto w-full max-w-2xl rounded-xl p-0 shadow-xl backdrop:bg-slate-900/50';
    var PARAM_DESDE_ALTA = 'desdeAlta';
    var PREFIJO_BORRADOR = 'borrador:';

    // ---------- Buscador (modal) ----------

    function pedirHtml(url) {
        return fetch(url, {headers: {'X-Requested-With': 'fetch'}, credentials: 'same-origin'})
            .then(function (respuesta) { return respuesta.text(); });
    }

    /** Abre el buscador de {url} en un modal. alElegir recibe {id, texto} de la fila elegida. */
    function abrirBuscador(url, alElegir) {
        var dialogo = document.createElement('dialog');
        dialogo.className = CLASES_MODAL;
        document.body.appendChild(dialogo);
        dialogo.addEventListener('close', function () { dialogo.remove(); });

        function mostrar(destino) {
            return pedirHtml(destino).then(function (html) { dialogo.innerHTML = html; });
        }

        dialogo.addEventListener('click', function (evento) {
            var elegido = evento.target.closest('[data-elegir]');
            if (elegido) {
                dialogo.close();
                alElegir({id: elegido.getAttribute('data-id'), texto: elegido.getAttribute('data-texto')});
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
            }
        });

        mostrar(url).then(function () { dialogo.showModal(); }, function () { dialogo.remove(); });
    }

    // ---------- Alta contextual: guardar y recuperar lo tipeado ----------

    function formularioDelSelector(selector) {
        return selector.closest('form');
    }

    function guardarBorrador(formulario) {
        var campos = [];
        new FormData(formulario).forEach(function (valor, nombre) {
            if (typeof valor === 'string' && nombre !== '_csrf' && nombre !== 'retorno' && nombre !== 'campo') {
                campos.push([nombre, valor]);
            }
        });
        var textos = {};
        formulario.querySelectorAll('[data-selector]').forEach(function (selector) {
            textos[selector.querySelector('[data-selector-id]').name] = selector.querySelector('[data-selector-texto]').value;
        });
        try {
            sessionStorage.setItem(PREFIJO_BORRADOR + location.pathname, JSON.stringify({campos: campos, textos: textos}));
        } catch (e) { /* sin almacenamiento: se vuelve igual, pero sin recuperar lo tipeado */ }
    }

    function restaurarBorrador() {
        var parametros = new URLSearchParams(location.search);
        if (!parametros.has(PARAM_DESDE_ALTA)) { return; }

        var clave = PREFIJO_BORRADOR + location.pathname;
        var borrador = null;
        try {
            borrador = JSON.parse(sessionStorage.getItem(clave));
            sessionStorage.removeItem(clave);
        } catch (e) { /* sin almacenamiento */ }

        if (borrador) {
            borrador.campos.forEach(function (par) {
                // Los campos que vuelven en la URL (la entidad recién creada) ya los dejó el servidor
                if (parametros.has(par[0])) { return; }
                document.querySelectorAll('form [name="' + par[0] + '"]').forEach(function (elemento) {
                    if (elemento.type === 'checkbox' || elemento.type === 'radio') {
                        elemento.checked = elemento.value === par[1];
                    } else {
                        elemento.value = par[1];
                    }
                });
            });
            document.querySelectorAll('form [data-selector]').forEach(function (selector) {
                var nombre = selector.querySelector('[data-selector-id]').name;
                if (!parametros.has(nombre) && borrador.textos[nombre] !== undefined) {
                    selector.querySelector('[data-selector-texto]').value = borrador.textos[nombre];
                }
            });
        }

        parametros.delete(PARAM_DESDE_ALTA);
        var resto = parametros.toString();
        history.replaceState(null, '', location.pathname + (resto ? '?' + resto : ''));
    }

    function irAlAlta(selector) {
        var formulario = formularioDelSelector(selector);
        if (formulario) { guardarBorrador(formulario); }

        var destino = selector.getAttribute('data-alta-url');
        var separador = destino.indexOf('?') === -1 ? '?' : '&';
        window.location.href = destino + separador
            + 'retorno=' + encodeURIComponent(location.pathname + location.search)
            + '&campo=' + encodeURIComponent(selector.querySelector('[data-selector-id]').name);
    }

    // ---------- Eventos ----------

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
            abrirBuscador(selector.getAttribute('data-buscador-url'), asignar);
        } else if (boton.hasAttribute('data-selector-alta')) {
            irAlAlta(selector);
        } else {
            asignar({id: '', texto: ''});
        }
    }

    document.addEventListener('click', function (evento) {
        var baja = evento.target.closest('[data-baja-url]');
        if (baja) {
            abrirConfirmacionBaja(baja);
            return;
        }
        if (evento.target.closest('#dialogo-baja [data-cerrar-dialogo]')) {
            document.getElementById('dialogo-baja').close();
            return;
        }
        var accion = evento.target.closest('[data-selector-buscar], [data-selector-alta], [data-selector-limpiar]');
        if (accion) {
            accionSelector(accion);
        }
    });

    document.addEventListener('DOMContentLoaded', restaurarBorrador);
})();
