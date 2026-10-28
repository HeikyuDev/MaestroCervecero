/*
 * Formularios de un ingreso de insumo (por compra y directo): solo muestran información, no validan nada (el servidor es
 * el único que valida). Cuando un selector del formulario emite 'selector:cambio' (comportamiento de /js/modales.js):
 *
 *   - Selector de la orden de compra (campo idOrdenCompra): quita el ítem elegido, habilita el selector del ítem y apunta
 *     su buscador a los ítems de esa orden (parámetro idOrdenCompra). Si se quita la orden, vuelve a deshabilitarlo.
 *   - Selector del ítem de la orden (campo idDetalleCompra): muestra su insumo, su presentación y lo pedido, recibido y
 *     pendiente en presentaciones, y habilita "Ver lotes existentes" para ese insumo.
 *   - Selector del insumo del ingreso directo (campo idInsumo): muestra su tipo y la unidad en la cantidad y en el precio,
 *     y habilita "Ver lotes existentes" para ese insumo.
 *   - "Ver lotes existentes" ([data-lote-existente]): completa la identificación y el vencimiento del lote elegido y deja
 *     el vencimiento de solo lectura. "Limpiar" ([data-lote-limpiar]) desbloquea el vencimiento y vacía los dos campos
 *     para escribir un lote nuevo. Cambiar el insumo, o escribir otra identificación, también desbloquea el vencimiento.
 *   - Al escribir en un campo, se oculta el mensaje de error que mostraba debajo y se quita su marca roja (solo visual: el
 *     servidor vuelve a validar al enviar).
 *   - La cantidad de presentaciones recibidas del ingreso por compra informa cuánto insumo ingresa al lote (data-factor
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

    function fijarUnidad(formulario, unidad) {
        formulario.querySelectorAll('[data-unidad]').forEach(function (destino) {
            if (destino.getAttribute('data-unidad-inicial') === null) {
                destino.setAttribute('data-unidad-inicial', destino.textContent);
            }
            destino.textContent = unidad || destino.getAttribute('data-unidad-inicial');
        });
    }

    // ---------- Lote existente ----------

    function entradas(formulario) {
        return {
            identificacion: formulario.querySelector('#identificacionLoteProveedor'),
            vencimiento: formulario.querySelector('#fechaVencimiento'),
            aviso: formulario.querySelector('[data-lote-bloqueado]')
        };
    }

    function bloqueado(formulario) {
        return formulario.hasAttribute('data-lote-elegido');
    }

    /** Desbloquea el vencimiento; con {vaciar} también vacía la identificación y el vencimiento. */
    function desbloquearLote(formulario, vaciar) {
        var campos = entradas(formulario);
        formulario.removeAttribute('data-lote-elegido');
        campos.vencimiento.readOnly = false;
        campos.vencimiento.classList.remove('bg-slate-100');
        if (campos.aviso) { campos.aviso.hidden = true; }
        if (vaciar) {
            campos.identificacion.value = '';
            campos.vencimiento.value = '';
        }
    }

    function elegirLote(formulario, lote) {
        var datos = lote.datos || {};
        if (!lote.id) { return; }
        var campos = entradas(formulario);
        campos.identificacion.value = lote.texto || '';
        campos.vencimiento.value = datos.vencimientoIso || '';
        campos.vencimiento.readOnly = true;
        campos.vencimiento.classList.add('bg-slate-100');
        if (campos.aviso) { campos.aviso.hidden = false; }
        formulario.setAttribute('data-lote-elegido', '');
        // El selector del lote es solo un disparador: no conserva la elección
        var selector = formulario.querySelector('[data-lote-existente]');
        if (selector) { vaciarSelector(selector); }
    }

    /** El insumo del ingreso cambió (o se quitó): el botón de lotes existentes apunta a ese insumo. */
    function alCambiarInsumo(formulario, idInsumo) {
        var lote = formulario.querySelector('[data-lote-existente]');
        if (lote) {
            var base = lote.getAttribute('data-buscador-base');
            configurarBuscador(lote, lote, idInsumo ? base + '?idInsumo=' + encodeURIComponent(idInsumo) : base, !!idInsumo);
            var ayuda = formulario.querySelector('[data-ayuda-lote]');
            if (ayuda) { ayuda.hidden = !!idInsumo; }
        }
        // Un lote elegido pertenecía al insumo anterior
        if (bloqueado(formulario)) { desbloquearLote(formulario, true); }
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
        alCambiarInsumo(formulario, null);
    }

    function alCambiarItem(formulario, item) {
        var elegido = !!item.id;
        mostrarItem(formulario, elegido ? (item.datos || {}) : null);
        alCambiarInsumo(formulario, elegido ? (item.datos || {}).insumoId : null);
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
        alCambiarInsumo(formulario, elegido ? insumo.id : null);
    }

    // ---------- Insumo que ingresa al lote ----------

    /** Muestra "Ingresan N kg al lote" con las presentaciones cargadas por lo que trae cada una; solo informa. */
    function actualizarIngresaAlLote(formulario) {
        var salida = formulario.querySelector('[data-ingresa-al-lote]');
        var entrada = formulario.querySelector('#cantidadPresentaciones');
        if (!salida || !entrada) { return; }
        var factor = parseFloat(formulario.getAttribute('data-factor'));
        var presentaciones = parseFloat(entrada.value);
        if (!(factor > 0) || !(presentaciones > 0)) {
            salida.hidden = true;
            return;
        }
        salida.querySelector('[data-ingresa-valor]').textContent =
            formatearNumero(presentaciones * factor) + ' ' + (formulario.getAttribute('data-unidad-item') || '');
        salida.hidden = false;
    }

    // ---------- Error del campo ----------

    /** Oculta el error de {entrada} (el párrafo rojo de su contenedor) y quita la marca roja del campo. */
    function limpiarErrorDelCampo(entrada) {
        var contenedor = entrada.parentElement;
        if (!contenedor) { return; }
        contenedor.querySelectorAll('p.text-rose-600').forEach(function (error) { error.hidden = true; });
        if (entrada.classList.contains('border-rose-400')) {
            entrada.classList.remove('border-rose-400', 'bg-rose-50');
            entrada.classList.add('border-slate-300');
        }
    }

    // ---------- Eventos ----------

    document.addEventListener('selector:cambio', function (evento) {
        var formulario = evento.target.closest('[data-formulario-ingreso]');
        if (!formulario) { return; }
        var detalle = evento.detail || {};

        if (evento.target.hasAttribute('data-lote-existente')) {
            elegirLote(formulario, detalle);
            return;
        }
        var campo = campoDe(evento.target);
        if (campo === 'idOrdenCompra') {
            alCambiarOrden(formulario, detalle);
        } else if (campo === 'idDetalleCompra') {
            alCambiarItem(formulario, detalle);
        } else if (campo === 'idInsumo') {
            alCambiarInsumoDirecto(formulario, detalle);
        }
    });

    document.addEventListener('click', function (evento) {
        if (!evento.target.closest('[data-lote-limpiar]')) { return; }
        var formulario = evento.target.closest('[data-formulario-ingreso]');
        if (formulario) { desbloquearLote(formulario, true); }
    });

    document.addEventListener('input', function (evento) {
        var formulario = evento.target.closest ? evento.target.closest('[data-formulario-ingreso]') : null;
        if (!formulario) { return; }
        if (evento.target.matches && evento.target.matches('input:not([data-selector-texto]), select, textarea')) {
            limpiarErrorDelCampo(evento.target);
        }
        if (evento.target.id === 'cantidadPresentaciones') {
            actualizarIngresaAlLote(formulario);
        } else if (evento.target.id === 'identificacionLoteProveedor' && bloqueado(formulario)) {
            // Otra identificación ya no es el lote elegido: el vencimiento vuelve a ser editable
            desbloquearLote(formulario, false);
            entradas(formulario).vencimiento.value = '';
        }
    });

    document.querySelectorAll('[data-formulario-ingreso]').forEach(actualizarIngresaAlLote);
})();
