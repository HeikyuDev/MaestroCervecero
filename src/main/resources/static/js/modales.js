/*
 * Selectores de entidad y confirmación de baja.
 *
 * El servidor responde siempre HTML (fragmentos de Thymeleaf); este archivo solo reacciona a los
 * atributos data-* de las páginas:
 *
 *   data-selector                 campo de selección de una entidad relacionada:
 *     data-selector-buscar        el propio campo: con clic (o Enter / Espacio) abre el buscador (modal con
 *                                 criterios de filtro) y deja elegida la fila.
 *     data-selector-menu          botón de tres puntitos que despliega el menú de opciones.
 *     data-selector-alta          opción del menú: lleva al alta de esa entidad y, al guardar, vuelve a esta
 *                                 página con lo tipeado recuperado y la entidad nueva elegida (alta contextual).
 *     data-selector-limpiar       quita la selección.
 *     data-deshabilitado          atributo de un contenedor del selector: mientras está presente, el selector no abre el buscador.
 *     Al elegir o quitar, el selector emite el evento 'selector:cambio' (con detail = {id, texto, datos}, donde
 *     datos son los data-* del botón "Elegir" elegido) para que la página reaccione a la selección.
 *   data-baja-url / data-baja-nombre
 *                                 botón que abre el diálogo de confirmación de baja (#dialogo-baja).
 *   data-motivo-dialogo / data-motivo-url / data-motivo-nombre
 *                                 botón que abre un diálogo que pide un motivo (fragments/dialogo-motivo): el id
 *                                 del diálogo, a dónde se envía y de qué es la acción.
 *   data-detalle-anulacion / data-detalle-titulo / data-detalle-motivo / data-detalle-etiqueta-usuario / data-detalle-usuario / data-detalle-fecha
 *                                 botón que abre el diálogo de detalle de una anulación o cancelación
 *                                 (fragments/dialogo-detalle-anulacion, incluido en el layout): el título del
 *                                 diálogo, el motivo, el usuario y la fecha y hora.
 *   data-confirmar-titulo / data-confirmar-mensaje
 *                                 botón de envío de un formulario que pide una confirmación antes de enviarlo
 *                                 (fragments/dialogo-confirmacion): el título y el mensaje del diálogo. Sin
 *                                 JavaScript el formulario se envía directamente.
 *   data-un-solo-envio            formulario que se envía una sola vez: al enviarlo se deshabilitan sus botones de
 *                                 envío y un segundo envío se ignora (marca data-enviando). Si el navegador vuelve a
 *                                 la página desde su historial (pageshow), el formulario se habilita de nuevo. Sin
 *                                 JavaScript el formulario se envía igual.
 *
 * Dentro del modal del buscador:
 *   data-cerrar-modal             cierra el modal.
 *   data-modal-buscar (form GET)  vuelve a pedir el fragmento con los criterios de filtro.
 *   data-modal-get (enlace)       paginación: pide el fragmento de otra página.
 *   data-elegir data-id data-texto  fila elegida (cualquier otro data-* de la fila viaja en detail.datos).
 */
(function () {
    'use strict';

    var CLASES_MODAL = 'm-auto w-full max-w-2xl rounded-xl p-0 shadow-xl backdrop:bg-slate-900/50';
    var PARAM_DESDE_ALTA = 'desdeAlta';
    var PREFIJO_BORRADOR = 'borrador:';

    // ---------- Buscador (modal) ----------

    var MENSAJE_BUSCADOR_NO_DISPONIBLE = 'No se pudo cargar la búsqueda, actualice la página e intente nuevamente';
    var MENSAJE_SESION_FINALIZADA = 'Su sesión finalizó, inicie sesión nuevamente';

    /**
     * Pide el fragmento de {url}. Resuelve {html} con el fragmento o {mensaje} con un texto para mostrar dentro del
     * modal: una respuesta que no es correcta trae el motivo como texto, y una redirigida (la página completa de otro
     * lugar) nunca se inyecta en el modal. Una redirigida al inicio de sesión es una sesión que ya no está vigente (venció
     * o se inició en otro lugar).
     */
    function pedirHtml(url) {
        return fetch(url, {headers: {'X-Requested-With': 'fetch'}, credentials: 'same-origin'})
            .then(function (respuesta) {
                if (respuesta.redirected) {
                    return {mensaje: respuesta.url.indexOf('/login') !== -1 ? MENSAJE_SESION_FINALIZADA : MENSAJE_BUSCADOR_NO_DISPONIBLE};
                }
                return respuesta.text().then(function (texto) {
                    if (respuesta.ok) { return {html: texto}; }
                    var esTexto = (respuesta.headers.get('Content-Type') || '').indexOf('text/plain') === 0;
                    return {mensaje: esTexto && texto ? texto : MENSAJE_BUSCADOR_NO_DISPONIBLE};
                });
            });
    }

    /** Muestra un mensaje dentro del modal, con el botón para cerrarlo. */
    function mostrarMensajeEnModal(dialogo, mensaje) {
        var contenedor = document.createElement('div');
        contenedor.className = 'p-6';
        var texto = document.createElement('p');
        texto.className = 'text-sm text-slate-600';
        texto.setAttribute('data-mensaje-buscador', '');
        texto.textContent = mensaje;
        var acciones = document.createElement('div');
        acciones.className = 'mt-6 flex justify-end';
        var cerrar = document.createElement('button');
        cerrar.type = 'button';
        cerrar.setAttribute('data-cerrar-modal', '');
        cerrar.className = 'px-4 py-2 rounded-lg text-sm font-medium text-slate-600 hover:bg-slate-100 transition-colors';
        cerrar.textContent = 'Cerrar';
        acciones.appendChild(cerrar);
        contenedor.appendChild(texto);
        contenedor.appendChild(acciones);
        dialogo.replaceChildren(contenedor);
    }

    /** Abre el buscador de {url} en un modal. alElegir recibe {id, texto} de la fila elegida. */
    function abrirBuscador(url, alElegir) {
        var dialogo = document.createElement('dialog');
        dialogo.className = CLASES_MODAL;
        document.body.appendChild(dialogo);
        dialogo.addEventListener('close', function () { dialogo.remove(); });

        function mostrar(destino) {
            return pedirHtml(destino).then(function (respuesta) {
                if (respuesta.mensaje) {
                    mostrarMensajeEnModal(dialogo, respuesta.mensaje);
                } else {
                    dialogo.innerHTML = respuesta.html;
                }
            });
        }

        dialogo.addEventListener('click', function (evento) {
            var elegido = evento.target.closest('[data-elegir]');
            if (elegido) {
                dialogo.close();
                alElegir({id: elegido.getAttribute('data-id'), texto: elegido.getAttribute('data-texto'),
                    datos: Object.assign({}, elegido.dataset)});
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

    /** Abre un diálogo de acción (baja o motivo): apunta su formulario a {url} y muestra de qué es {nombre}. */
    function abrirDialogoDeAccion(dialogo, url, nombre) {
        if (!dialogo) { return; }
        dialogo.querySelector('form').setAttribute('action', url);
        var destino = dialogo.querySelector('[data-baja-nombre-destino], [data-dialogo-nombre-destino]');
        if (destino) { destino.textContent = nombre; }
        dialogo.showModal();
    }

    /** Abre el diálogo de detalle de una anulación o cancelación con los datos que trae el botón. */
    function abrirDetalleDeAnulacion(dialogo, boton) {
        if (!dialogo) { return; }
        function asignar(destino, atributo) {
            dialogo.querySelector(destino).textContent = boton.getAttribute(atributo) || '-';
        }
        asignar('[data-detalle-titulo-destino]', 'data-detalle-titulo');
        asignar('[data-detalle-motivo-destino]', 'data-detalle-motivo');
        asignar('[data-detalle-etiqueta-usuario-destino]', 'data-detalle-etiqueta-usuario');
        asignar('[data-detalle-usuario-destino]', 'data-detalle-usuario');
        asignar('[data-detalle-fecha-destino]', 'data-detalle-fecha');
        dialogo.showModal();
    }

    // ---------- Confirmación antes de enviar un formulario ----------

    var formularioPorConfirmar = null;

    /** Abre el diálogo de confirmación con el título y el mensaje del botón; al aceptar, envía su formulario. */
    function abrirDialogoDeConfirmacion(dialogo, boton) {
        formularioPorConfirmar = boton.closest('form');
        dialogo.querySelector('[data-confirmar-titulo-destino]').textContent = boton.getAttribute('data-confirmar-titulo');
        dialogo.querySelector('[data-confirmar-mensaje-destino]').textContent = boton.getAttribute('data-confirmar-mensaje');
        dialogo.showModal();
    }

    function aceptarConfirmacion(dialogo) {
        var formulario = formularioPorConfirmar;
        formularioPorConfirmar = null;
        dialogo.close();
        if (formulario && !formulario.hasAttribute('data-enviando')) {
            marcarEnviando(formulario);
            formulario.submit();
        }
    }

    // ---------- Un solo envío por formulario ----------

    /** Marca el formulario como enviado y deshabilita sus botones de envío. */
    function marcarEnviando(formulario) {
        if (!formulario.hasAttribute('data-un-solo-envio')) { return; }
        formulario.setAttribute('data-enviando', 'true');
        // Se deshabilitan después del evento: un botón deshabilitado antes no viajaría con el formulario
        setTimeout(function () {
            formulario.querySelectorAll('button[type="submit"], input[type="submit"]').forEach(function (boton) { boton.disabled = true; });
        }, 0);
    }

    function permitirOtroEnvio(formulario) {
        formulario.removeAttribute('data-enviando');
        formulario.querySelectorAll('button[type="submit"], input[type="submit"]').forEach(function (boton) { boton.disabled = false; });
    }

    document.addEventListener('submit', function (evento) {
        var formulario = evento.target;
        if (!formulario.hasAttribute || !formulario.hasAttribute('data-un-solo-envio')) { return; }
        if (formulario.hasAttribute('data-enviando')) {
            evento.preventDefault();
            return;
        }
        marcarEnviando(formulario);
    });

    // Al volver a la página desde el historial del navegador (sin recargarla), los formularios se pueden enviar de nuevo
    window.addEventListener('pageshow', function () {
        document.querySelectorAll('form[data-enviando]').forEach(permitirOtroEnvio);
    });

    function accionSelector(boton) {
        var selector = boton.closest('[data-selector]');
        // Un selector deshabilitado (data-deshabilitado en él o en su contenedor) no abre nada hasta que la página lo habilite
        if (selector.closest('[data-deshabilitado]')) { return; }
        function asignar(entidad) {
            selector.querySelector('[data-selector-id]').value = entidad.id;
            selector.querySelector('[data-selector-texto]').value = entidad.texto;
            selector.dispatchEvent(new CustomEvent('selector:cambio', {bubbles: true, detail: entidad}));
        }

        if (boton.hasAttribute('data-selector-buscar')) {
            abrirBuscador(selector.getAttribute('data-buscador-url'), asignar);
        } else if (boton.hasAttribute('data-selector-alta')) {
            irAlAlta(selector);
        } else {
            asignar({id: '', texto: ''});
        }
    }

    // ---------- Menú de tres puntitos del selector ----------

    function cerrarMenus(excepto) {
        document.querySelectorAll('[data-selector-menu-panel]').forEach(function (panel) {
            if (panel !== excepto && !panel.hidden) {
                panel.hidden = true;
                var disparador = panel.closest('[data-selector]').querySelector('[data-selector-menu]');
                if (disparador) { disparador.setAttribute('aria-expanded', 'false'); }
            }
        });
    }

    function alternarMenu(boton) {
        var panel = boton.closest('[data-selector]').querySelector('[data-selector-menu-panel]');
        var abrir = panel.hidden;
        cerrarMenus(panel);
        panel.hidden = !abrir;
        boton.setAttribute('aria-expanded', String(abrir));
    }

    document.addEventListener('click', function (evento) {
        var baja = evento.target.closest('[data-baja-url]');
        if (baja) {
            abrirDialogoDeAccion(document.getElementById('dialogo-baja'),
                baja.getAttribute('data-baja-url'), baja.getAttribute('data-baja-nombre'));
            return;
        }
        var detalle = evento.target.closest('[data-detalle-anulacion]');
        if (detalle) {
            abrirDetalleDeAnulacion(document.getElementById('dialogo-detalle-anulacion'), detalle);
            return;
        }
        var motivo = evento.target.closest('[data-motivo-url]');
        if (motivo) {
            abrirDialogoDeAccion(document.getElementById(motivo.getAttribute('data-motivo-dialogo')),
                motivo.getAttribute('data-motivo-url'), motivo.getAttribute('data-motivo-nombre'));
            return;
        }
        var confirmar = evento.target.closest('[data-confirmar-mensaje]');
        var dialogoConfirmacion = document.getElementById('dialogo-confirmacion');
        if (confirmar && dialogoConfirmacion && confirmar.closest('form')) {
            evento.preventDefault();
            abrirDialogoDeConfirmacion(dialogoConfirmacion, confirmar);
            return;
        }
        if (evento.target.closest('[data-confirmar-aceptar]')) {
            aceptarConfirmacion(dialogoConfirmacion);
            return;
        }
        var cierre = evento.target.closest('#dialogo-baja [data-cerrar-dialogo], dialog[data-dialogo-motivo] [data-cerrar-dialogo], dialog[data-dialogo-detalle] [data-cerrar-dialogo], dialog[data-dialogo-confirmacion] [data-cerrar-dialogo]');
        if (cierre) {
            cierre.closest('dialog').close();
            return;
        }
        var menu = evento.target.closest('[data-selector-menu]');
        if (menu) {
            alternarMenu(menu);
            return;
        }
        // Un clic fuera de un menú abierto lo cierra
        cerrarMenus(null);

        var accion = evento.target.closest('[data-selector-buscar], [data-selector-alta], [data-selector-limpiar]');
        if (accion) {
            accionSelector(accion);
        }
    });

    document.addEventListener('keydown', function (evento) {
        if (evento.key === 'Escape') {
            cerrarMenus(null);
            return;
        }
        // El campo del selector se abre también con el teclado (Enter o Espacio)
        var campo = evento.target.closest ? evento.target.closest('[data-selector-buscar]') : null;
        if (campo && (evento.key === 'Enter' || evento.key === ' ')) {
            evento.preventDefault();
            accionSelector(campo);
        }
    });

    document.addEventListener('DOMContentLoaded', restaurarBorrador);
})();
