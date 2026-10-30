/*
 * Formulario de orden de compra: tabla de ítems con modal para agregarlos (ítem del catálogo del proveedor + cantidad +
 * costo unitario). Usa el mismo mecanismo de filas, plantilla e índices que /js/proveedor-form.js: ahí está el contrato
 * de los atributos data-* compartidos (data-detalle, data-abrir-agregar, data-quitar-fila, data-fila, data-prefijo,
 * data-fila-vacia, data-campo, data-celda, template[data-plantilla], data-selector-de, data-error, data-confirmar-agregar
 * y data-cerrar-agregar). Los propios de este formulario son:
 *
 *   data-seccion-datos           sección de la planificación, el proveedor y la fecha.
 *   data-aviso-bloqueo           aviso "quite primero todos los ítems", visible mientras la orden tiene filas.
 *   data-ayuda-agregar           explica por qué "Agregar ítem" está deshabilitado (hasta elegir planificación y proveedor).
 *   data-total                   total de la orden bajo la tabla.
 *   data-campo-modal             campo del modal que se lee al confirmar (cantidad, costoUnitario).
 *
 * El selector de ítem del modal recibe, justo antes de abrirse, la planificación, el proveedor y los ítems ya agregados
 * (en su data-buscador-url). Al elegir, emite 'selector:cambio' con data-insumo, data-presentacion y data-medida del
 * botón "Elegir" del buscador, que se guardan para escribir la fila.
 *
 * Bloqueo: mientras la tabla tiene al menos una fila, la planificación y el proveedor no se pueden cambiar, porque los
 * ítems ofrecidos y los ya cargados dependen de los dos. Los selectores se muestran deshabilitados (data-deshabilitado
 * hace que /js/modales.js no abra su buscador) y el aviso lo explica; al quitar la última fila se habilitan de nuevo.
 * Sus ids viajan igual en campos ocultos, que no se deshabilitan.
 *
 * Validación: es la excepción acordada a la regla de "validar solo en el servidor", igual que en la receta y en el
 * catálogo del proveedor. El modal frena la fila ANTES de que entre a la tabla; al enviar el formulario el servidor
 * valida todo de nuevo (Bean Validation y reglas del service).
 *
 * Importes: el subtotal y el total solo se muestran (el servidor los vuelve a calcular). Se calculan con enteros (BigInt)
 * en la escala del costo unitario, 4 decimales, para no arrastrar errores de coma flotante, y se escriben igual que
 * FormatoImporte: punto de miles, coma decimal, mínimo 2 y máximo 4 decimales.
 *
 * Índices: Spring solo enlaza listas sin huecos (detallesCompra[0], [1], [2]...). Por eso después de cada alta o baja de
 * fila se renumeran los campos de toda la tabla, y se vuelve a hacer al enviar el formulario.
 */
(function () {
    'use strict';

    // Mismos valores que LimitesTecnicos (CANTIDAD_COMPRA_MAXIMA, COSTO_UNITARIO_COMPRA_MAXIMO, ESCALA_COSTO_UNITARIO)
    var CANTIDAD_MAXIMA = 1000000;
    var COSTO_MAXIMO = 1000000000;
    var ESCALA = 4;

    // ---------- Importes ----------

    /** El texto como entero en la escala de 4 decimales (BigInt), o null si no es un número decimal con hasta 4 decimales. */
    function aEscala(texto) {
        var partes = /^(\d*)(?:\.(\d{1,4}))?$/.exec(String(texto === undefined || texto === null ? '' : texto).trim());
        if (!partes || (partes[1] === '' && partes[2] === undefined)) { return null; }
        var decimales = partes[2] || '';
        while (decimales.length < ESCALA) { decimales += '0'; }
        return BigInt((partes[1] || '0') + decimales);
    }

    /** El importe (entero en escala de 4 decimales) escrito como FormatoImporte.conSimbolo: $ 1.250,50 o $ 0,1234. */
    function formatear(importe) {
        var digitos = importe.toString();
        while (digitos.length <= ESCALA) { digitos = '0' + digitos; }
        var entera = digitos.slice(0, digitos.length - ESCALA);
        var decimales = digitos.slice(digitos.length - ESCALA).replace(/0+$/, '');
        while (decimales.length < 2) { decimales += '0'; }
        return '$ ' + entera.replace(/\B(?=(\d{3})+(?!\d))/g, '.') + ',' + decimales;
    }

    function esEntero(texto) {
        return /^\d+$/.test(String(texto).trim());
    }

    // ---------- Filas, índices, total y bloqueo ----------

    function campoDe(fila, nombre) {
        return fila.querySelector('[data-campo="' + nombre + '"]');
    }

    /** Subtotal de una fila (entero en escala de 4 decimales), o null si la fila no tiene cantidad y costo válidos. */
    function subtotalDe(fila) {
        var cantidad = campoDe(fila, 'cantidad').value;
        var costoTexto = campoDe(fila, 'costoUnitario').value;
        if (!cantidadValida(cantidad) || !costoValido(costoTexto)) { return null; }
        var costo = aEscala(costoTexto);
        return BigInt(cantidad.trim()) * costo;
    }

    /** Habilita o deshabilita un selector de la sección de datos, y muestra u oculta el aviso del bloqueo. */
    function bloquearSelectores(bloquear) {
        document.querySelectorAll('[data-seccion-datos] [data-selector-de]').forEach(function (bloque) {
            if (bloquear) { bloque.setAttribute('data-deshabilitado', 'true'); } else { bloque.removeAttribute('data-deshabilitado'); }
            bloque.classList.toggle('opacity-50', bloquear);
            var texto = bloque.querySelector('[data-selector-texto]');
            if (texto) { texto.setAttribute('aria-disabled', bloquear ? 'true' : 'false'); }
        });
        var aviso = document.querySelector('[data-aviso-bloqueo]');
        if (aviso) { aviso.hidden = !bloquear; }
    }

    /** "Agregar ítem" solo se puede usar con la planificación y el proveedor elegidos. */
    function actualizarBotonAgregar() {
        var elegidos = ['idPlanificacionProduccion', 'idProveedor'].every(function (nombre) {
            var campo = document.querySelector('[data-seccion-datos] [data-selector-id][name="' + nombre + '"]');
            return campo && campo.value !== '';
        });
        var boton = document.querySelector('[data-abrir-agregar]');
        if (boton) { boton.disabled = !elegidos; }
        var ayuda = document.querySelector('[data-ayuda-agregar]');
        if (ayuda) { ayuda.hidden = elegidos; }
    }

    /**
     * Renumera los campos de todas las filas de la sección (sin huecos y en el orden en que se ven), muestra u oculta el
     * texto de tabla vacía, recalcula el total y aplica el bloqueo de la planificación y el proveedor.
     */
    function sincronizar(seccion) {
        var filas = seccion.querySelectorAll('tbody [data-fila]');
        var total = BigInt(0);
        filas.forEach(function (fila, indice) {
            var prefijo = fila.getAttribute('data-prefijo');
            fila.querySelectorAll('[data-campo]').forEach(function (campo) {
                campo.name = prefijo + '[' + indice + '].' + campo.getAttribute('data-campo');
            });
            var subtotal = subtotalDe(fila);
            if (subtotal !== null) { total += subtotal; }
        });
        var vacia = seccion.querySelector('[data-fila-vacia]');
        if (vacia) { vacia.hidden = filas.length > 0; }
        var destinoTotal = seccion.querySelector('[data-total]');
        if (destinoTotal) { destinoTotal.textContent = formatear(total); }

        bloquearSelectores(filas.length > 0);
        actualizarBotonAgregar();
    }

    /** Clona la fila vacía de la sección, la completa con los datos y la agrega al final de la tabla. */
    function agregarFila(seccion, datos) {
        var plantilla = seccion.querySelector('template[data-plantilla]');
        var fila = plantilla.content.firstElementChild.cloneNode(true);

        fila.querySelectorAll('[data-campo]').forEach(function (campo) {
            campo.value = datos.campos[campo.getAttribute('data-campo')];
        });
        fila.querySelectorAll('[data-celda]').forEach(function (celda) {
            var texto = datos.celdas[celda.getAttribute('data-celda')];
            celda.textContent = texto !== undefined ? texto : '';
        });

        var tbody = seccion.querySelector('tbody');
        tbody.insertBefore(fila, tbody.querySelector('[data-fila-vacia]'));
        sincronizar(seccion);
    }

    // ---------- Ventana emergente ----------

    function selectorDe(dialogo, clave) {
        return dialogo.querySelector('[data-selector-de="' + clave + '"] [data-selector]');
    }

    /** Único lugar que muestra u oculta un aviso del modal, por su clave (data-error). */
    function mostrarError(dialogo, clave, mostrar) {
        var aviso = dialogo.querySelector('[data-error="' + clave + '"]');
        if (aviso) { aviso.hidden = !mostrar; }
    }

    function limpiarErrores(dialogo) {
        dialogo.querySelectorAll('[data-error]').forEach(function (aviso) { aviso.hidden = true; });
    }

    function campoModal(dialogo, nombre) {
        return dialogo.querySelector('[data-campo-modal="' + nombre + '"]');
    }

    /** Deja el selector y los campos del modal sin datos y sin avisos. */
    function reiniciar(dialogo) {
        dialogo.querySelectorAll('[data-selector]').forEach(function (selector) {
            selector.querySelector('[data-selector-id]').value = '';
            selector.querySelector('[data-selector-texto]').value = '';
            delete selector.datosElegidos;
        });
        dialogo.querySelectorAll('[data-campo-modal]').forEach(function (campo) { campo.value = ''; });
        limpiarErrores(dialogo);
    }

    /** Ids de los ítems del catálogo que la orden ya tiene, leídos de los campos ocultos de sus filas. */
    function idsAgregados(seccion) {
        return Array.prototype.map.call(seccion.querySelectorAll('tbody [data-fila]'), function (fila) {
            return campoDe(fila, 'idCatalogoProveedor').value;
        });
    }

    function valorDeSeccionDatos(nombre) {
        return document.querySelector('[data-seccion-datos] [data-selector-id][name="' + nombre + '"]').value;
    }

    /** Le indica al buscador de ítems la planificación, el proveedor y los ítems ya agregados de la orden. */
    function prepararBuscadorDeItems(dialogo, seccion) {
        var selector = selectorDe(dialogo, 'item');
        if (!selector.hasAttribute('data-buscador-base')) {
            selector.setAttribute('data-buscador-base', selector.getAttribute('data-buscador-url'));
        }
        var parametros = new URLSearchParams();
        parametros.set('idProveedor', valorDeSeccionDatos('idProveedor'));
        parametros.set('idPlanificacion', valorDeSeccionDatos('idPlanificacionProduccion'));
        var agregados = idsAgregados(seccion);
        if (agregados.length > 0) { parametros.set('idsAgregados', agregados.join(',')); }
        selector.setAttribute('data-buscador-url', selector.getAttribute('data-buscador-base') + '?' + parametros.toString());
    }

    function abrirAgregar(tipo) {
        var dialogo = document.getElementById('dialogo-agregar-' + tipo);
        var seccion = document.querySelector('[data-detalle="' + tipo + '"]');
        reiniciar(dialogo);
        prepararBuscadorDeItems(dialogo, seccion);
        dialogo.showModal();
        selectorDe(dialogo, 'item').querySelector('[data-selector-texto]').focus();
    }

    /** Lee lo cargado en el modal, sin validarlo, y arma los datos de la fila ({campos: ocultos, celdas: visibles}). */
    function leer(dialogo) {
        var item = selectorDe(dialogo, 'item');
        var datosItem = item.datosElegidos || {};
        var cantidad = campoModal(dialogo, 'cantidad').value.trim();
        if (esEntero(cantidad)) { cantidad = BigInt(cantidad).toString(); } // "0007" -> "7"
        var costo = campoModal(dialogo, 'costoUnitario').value.trim();
        var costoEscala = aEscala(costo);
        var subtotal = esEntero(cantidad) && costoEscala !== null ? BigInt(cantidad) * costoEscala : null;
        return {
            campos: {
                idCatalogoProveedor: item.querySelector('[data-selector-id]').value,
                cantidad: cantidad,
                costoUnitario: costo
            },
            celdas: {
                insumo: datosItem.insumo || '',
                presentacion: datosItem.presentacion || '',
                medida: datosItem.medida || '',
                cantidad: cantidad,
                costoUnitario: costoEscala !== null ? formatear(costoEscala) : costo,
                subtotal: subtotal !== null ? formatear(subtotal) : ''
            }
        };
    }

    // ---------- Validación previa a agregar la fila ----------

    function cantidadValida(texto) {
        return esEntero(texto) && Number(texto) >= 1 && Number(texto) <= CANTIDAD_MAXIMA;
    }

    function costoValido(texto) {
        var escala = aEscala(texto);
        return escala !== null && escala > BigInt(0) && escala <= BigInt(COSTO_MAXIMO) * BigInt(Math.pow(10, ESCALA));
    }

    function confirmarAgregar(dialogo) {
        var seccion = document.querySelector('[data-detalle="' + dialogo.getAttribute('data-dialogo-agregar') + '"]');
        var datos = leer(dialogo);
        var foco = null;

        limpiarErrores(dialogo);

        if (!datos.campos.idCatalogoProveedor) {
            mostrarError(dialogo, 'item', true);
            foco = foco || selectorDe(dialogo, 'item').querySelector('[data-selector-texto]');
        } else if (idsAgregados(seccion).indexOf(datos.campos.idCatalogoProveedor) !== -1) {
            mostrarError(dialogo, 'duplicado', true);
            foco = foco || selectorDe(dialogo, 'item').querySelector('[data-selector-texto]');
        }
        if (!cantidadValida(datos.campos.cantidad)) {
            mostrarError(dialogo, 'cantidad', true);
            foco = foco || campoModal(dialogo, 'cantidad');
        }
        if (!costoValido(datos.campos.costoUnitario)) {
            mostrarError(dialogo, 'costo', true);
            foco = foco || campoModal(dialogo, 'costoUnitario');
        }
        if (foco) {
            foco.focus();
            return;
        }

        agregarFila(seccion, datos);
        dialogo.close();
    }

    // Al elegir algo en un selector del modal se guardan sus datos extra y su aviso deja de tener sentido
    document.addEventListener('selector:cambio', function (evento) {
        var ventana = evento.target.closest('dialog[data-dialogo-agregar]');
        if (ventana) {
            evento.target.datosElegidos = evento.detail && evento.detail.id ? evento.detail.datos : undefined;
            if (evento.detail && evento.detail.id) { mostrarError(ventana, 'item', false); }
            mostrarError(ventana, 'duplicado', false);
            return;
        }
        // Elegir o quitar la planificación o el proveedor habilita o no "Agregar ítem"
        if (evento.target.closest('[data-seccion-datos]')) { actualizarBotonAgregar(); }
    });

    // ---------- Eventos ----------

    document.addEventListener('click', function (evento) {
        var abrir = evento.target.closest('[data-abrir-agregar]');
        if (abrir) {
            if (!abrir.disabled) { abrirAgregar(abrir.getAttribute('data-abrir-agregar')); }
            return;
        }
        var quitar = evento.target.closest('[data-quitar-fila]');
        if (quitar) {
            var fila = quitar.closest('[data-fila]');
            var seccion = fila.closest('[data-detalle]');
            fila.remove();
            sincronizar(seccion);
            return;
        }
        var confirmar = evento.target.closest('[data-confirmar-agregar]');
        if (confirmar) {
            confirmarAgregar(confirmar.closest('dialog'));
            return;
        }
        var cerrar = evento.target.closest('[data-cerrar-agregar]');
        if (cerrar) {
            cerrar.closest('dialog').close();
        }
    });

    document.addEventListener('DOMContentLoaded', function () {
        var formulario = document.getElementById('orden-form');
        var secciones = document.querySelectorAll('[data-detalle]');
        secciones.forEach(sincronizar);
        // Última garantía antes de enviar: índices sin huecos en la tabla de ítems
        formulario.addEventListener('submit', function () {
            secciones.forEach(sincronizar);
        });
    });
})();
