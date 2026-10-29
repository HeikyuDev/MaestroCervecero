/*
 * Formularios de un ingreso de insumo (por compra y directo): solo muestran información, no validan nada (el servidor es
 * el único que valida). Cuando un selector del formulario emite 'selector:cambio' (comportamiento de /js/modales.js):
 *
 *   - Selector de la orden de compra (campo idOrdenCompra): quita el ítem elegido, habilita el selector del ítem y apunta
 *     su buscador a los ítems de esa orden (parámetro idOrdenCompra). Si se quita la orden, vuelve a deshabilitarlo.
 *   - Selector del ítem de la orden (campo idDetalleCompra): muestra su insumo, su presentación y lo pedido, recibido y
 *     pendiente en presentaciones.
 *   - Selector del insumo del ingreso directo (campo idInsumo): muestra su tipo y su unidad como sufijo de la cantidad y del precio,
 *     
 *   - Al escribir en un campo, se oculta el mensaje de error que mostraba debajo y se quita su marca roja (solo visual: el
 *     servidor vuelve a validar al enviar).
 *   - La cantidad de unidades recibidas del ingreso por compra informa cuánto insumo ingresa al lote (data-factor
 *     y data-unidad-item del formulario: la cantidad de insumo que trae una presentación y el símbolo de su unidad).
 */
(function () {
    'use strict';

    /** El número con el formato de la aplicación: miles con punto, coma decimal y hasta 4 decimales ("1.250,5"). */
    function formatearNumero(numero) {
        var partes = (Math.round(numero * 10000) / 10000).toFixed(4).replace(/0+$/, '').replace(/\.$/, '').split('.');
        partes[0] = partes[0].replace(/\B(?=(\d{3})+(?!\d))/g, '.');
        return partes.join(',');
    }

    function campoDe(selector) {
        var id = selector.querySelector('[data-selector-id]');
        return id ? id.name : '';
    }

    // ---------- Selector con buscador dependiente de otro dato ----------

    /** Apunta el buscador del selector a {url} y lo habilita o deshabilita según haya con qué buscar. */
    function configurarBuscador(contenedor, selector, url, habilitado) {
        selector.setAttribute('data-buscador-url', url);
        if (habilitado) { contenedor.removeAttribute('data-deshabilitado'); } else { contenedor.setAttribute('data-deshabilitado', ''); }
        contenedor.classList.toggle('opacity-50', !habilitado);
        var texto = selector.querySelector('[data-selector-texto]');
        if (texto) { texto.setAttribute('aria-disabled', habilitado ? 'false' : 'true'); }
        var disparador = selector.querySelector('[data-selector-buscar]');
        if (disparador) { disparador.setAttribute('aria-disabled', habilitado ? 'false' : 'true'); }
    }

    function vaciarSelector(selector) {
        selector.querySelector('[data-selector-id]').value = '';
        selector.querySelector('[data-selector-texto]').value = '';
    }

    // ---------- Unidad ----------

    /** Muestra {unidad} en el sufijo de cada campo con unidad ([data-unidad-sufijo]); sin unidad queda "-". */
    function fijarUnidad(formulario, unidad) {
        formulario.querySelectorAll('[data-unidad-sufijo]').forEach(function (sufijo) {
            sufijo.textContent = unidad ? (sufijo.getAttribute('data-prefijo') || '') + unidad : '-';
        });
    }

    // ---------- Ingreso por compra ----------

    function mostrarItem(formulario, datos) {
        var resumen = formulario.querySelector('[data-item-elegido]');
        if (!resumen) { return; }
        var elegido = !!datos;
        resumen.hidden = !elegido;
        if (elegido) {
            resumen.querySelector('[data-item-insumo]').textContent = datos.insumo || '';
            resumen.querySelector('[data-item-presentacion]').textContent = datos.presentacion || '';
            resumen.querySelector('[data-item-pedido]').textContent = datos.pedido || '';
            resumen.querySelector('[data-item-recibido]').textContent = datos.recibido || '';
            resumen.querySelector('[data-item-pendiente]').textContent = datos.pendiente || '';
        }
        formulario.setAttribute('data-factor', elegido && datos.factor ? datos.factor : '');
        formulario.setAttribute('data-unidad-item', elegido && datos.unidad ? datos.unidad : '');
        actualizarIngresaAlLote(formulario);
    }

    function alCambiarOrden(formulario, orden) {
        var contenedor = formulario.querySelector('[data-selector-item]');
        if (!contenedor) { return; }
        var item = contenedor.querySelector('[data-selector]');
        vaciarSelector(item);
        var base = contenedor.getAttribute('data-buscador-base');
        var elegida = !!orden.id;
        configurarBuscador(contenedor, item, elegida ? base + '?idOrdenCompra=' + encodeURIComponent(orden.id) : base, elegida);
        var ayuda = formulario.querySelector('[data-ayuda-item]');
        if (ayuda) { ayuda.hidden = elegida; }
        mostrarItem(formulario, null);
    }

    function alCambiarItem(formulario, item) {
        var elegido = !!item.id;
        mostrarItem(formulario, elegido ? (item.datos || {}) : null);
    }

    // ---------- Ingreso directo ----------

    function alCambiarInsumoDirecto(formulario, insumo) {
        var elegido = !!insumo.id;
        var datos = insumo.datos || {};
        var resumen = formulario.querySelector('[data-insumo-elegido]');
        if (resumen) {
            resumen.hidden = !elegido;
            var tipo = resumen.querySelector('[data-insumo-tipo]');
            if (tipo) { tipo.textContent = datos.tipo || ''; }
        }
        fijarUnidad(formulario, elegido ? datos.unidad : '');
    }

    // ---------- Insumo que ingresa al lote ----------

    /** Muestra "Ingresan N kg al lote" con las unidades cargadas por lo que trae cada una; solo informa. */
    function actualizarIngresaAlLote(formulario) {
        var salida = formulario.querySelector('[data-ingresa-al-lote]');
        var entrada = formulario.querySelector('#cantidadPresentaciones');
        if (!salida || !entrada) { return; }
        var factor = parseFloat(formulario.getAttribute('data-factor'));
        var unidades = parseFloat(entrada.value);
        if (!(factor > 0) || !(unidades > 0)) {
            salida.hidden = true;
            return;
        }
        salida.querySelector('[data-ingresa-valor]').textContent =
            formatearNumero(unidades * factor) + ' ' + (formulario.getAttribute('data-unidad-item') || '');
        salida.hidden = false;
    }

    // ---------- Error del campo ----------

    /** Oculta el error de {entrada} (el párrafo rojo de su contenedor) y quita la marca roja del campo. */
    function limpiarErrorDelCampo(entrada) {
        var grupo = entrada.closest('[data-grupo-unidad]');
        var contenedor = grupo ? grupo.parentElement : entrada.parentElement;
        if (!contenedor) { return; }
        contenedor.querySelectorAll('p.text-rose-600').forEach(function (error) { error.hidden = true; });
        if (entrada.classList.contains('border-rose-400')) {
            entrada.classList.remove('border-rose-400', 'bg-rose-50');
            entrada.classList.add('border-slate-300');
            var sufijo = grupo ? grupo.querySelector('[data-unidad-sufijo]') : null;
            if (sufijo) {
                sufijo.classList.remove('border-rose-400');
                sufijo.classList.add('border-slate-300');
            }
        }
    }

    // ---------- Eventos ----------

    document.addEventListener('selector:cambio', function (evento) {
        var formulario = evento.target.closest('[data-formulario-ingreso]');
        if (!formulario) { return; }
        var detalle = evento.detail || {};

        var campo = campoDe(evento.target);
        if (campo === 'idOrdenCompra') {
            alCambiarOrden(formulario, detalle);
        } else if (campo === 'idDetalleCompra') {
            alCambiarItem(formulario, detalle);
        } else if (campo === 'idInsumo') {
            alCambiarInsumoDirecto(formulario, detalle);
        }
    });

    document.addEventListener('input', function (evento) {
        var formulario = evento.target.closest ? evento.target.closest('[data-formulario-ingreso]') : null;
        if (!formulario) { return; }
        if (evento.target.matches && evento.target.matches('input:not([data-selector-texto]), select, textarea')) {
            limpiarErrorDelCampo(evento.target);
        }
        if (evento.target.id === 'cantidadPresentaciones') {
            actualizarIngresaAlLote(formulario);
        }
    });

    document.querySelectorAll('[data-formulario-ingreso]').forEach(actualizarIngresaAlLote);
})();
