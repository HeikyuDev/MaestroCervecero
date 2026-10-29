/*
 * Formulario de proveedor: tabla del catálogo con ventana emergente para agregar ítems (insumo + presentación
 * comercial). Sigue el mismo mecanismo de filas que /js/receta-form.js.
 *
 * El servidor pinta las filas que el catálogo ya tiene y deja una <template> con una fila vacía
 * (proveedor/proveedor-form-filas.html). Este archivo solo reacciona a los atributos data-* de la página:
 *
 *   data-detalle="catalogo"      sección de la tabla del catálogo.
 *   data-abrir-agregar           botón "Agregar ítem": abre la ventana emergente.
 *   data-quitar-fila             botón "Quitar" de una fila.
 *   data-fila / data-prefijo     fila de la tabla y prefijo de sus campos (version.catalogoProveedor).
 *   data-fila-vacia              fila con el texto de tabla vacía.
 *   data-campo                   campo oculto de una fila (idInsumo, idPresentacionComercial); su nombre es
 *                                prefijo[indice].campo.
 *   data-celda                   texto visible de una fila (insumo, tipoInsumo, presentacion, medidaPresentacion).
 *   template[data-plantilla]     fila vacía que se clona al agregar.
 *
 * Dentro de la ventana emergente (dialog[data-dialogo-agregar]):
 *   data-selector-de             bloque del selector de insumo o de presentación comercial (/js/modales.js). Al elegir,
 *                                el selector emite 'selector:cambio' con los data-* del botón "Elegir" del buscador:
 *                                el tipo del insumo (data-tipo) y la cantidad (ya formateada como
 *                                receta/formato :: numero) y el símbolo de la unidad de la presentación
 *                                (data-cantidad, data-unidad), que se guardan para escribir la fila.
 *   data-error="<clave>"         aviso (insumo, presentacion, duplicado), oculto mientras no haga falta.
 *   data-confirmar-agregar       "Agregar al catálogo".
 *   data-cerrar-agregar          cierra la ventana sin agregar.
 *
 * Validación: es la excepción acordada a la regla de "validar solo en el servidor", igual que en la receta. La ventana
 * frena la fila ANTES de que entre a la tabla: exige el insumo y la presentación comercial elegidos y no repite el
 * mismo insumo con la misma presentación (el mismo insumo con otra presentación sí es válido). Al enviar el formulario
 * el servidor valida todo de nuevo (Bean Validation y reglas del service).
 *
 * CUIT: el usuario escribe solo números y el campo [data-cuit] inserta los guiones mientras escribe (XX-XXXXXXXX-X) y
 * limita la entrada a 11 dígitos. Es solo ayuda de formato: no valida. El servidor acepta el CUIT con o sin guiones, exige
 * 11 dígitos y lo guarda con ese formato.
 *
 * Índices: Spring solo enlaza listas sin huecos (catalogoProveedor[0], [1], [2]...). Por eso después de cada alta o
 * baja de fila se renumeran los campos de toda la tabla, y se vuelve a hacer al enviar el formulario.
 */
(function () {
    'use strict';

    // ---------- Utilidades ----------

    /**
     * Cantidad con su unidad tal como la muestra el servidor (receta/formato :: medida). La cantidad llega del buscador
     * de presentaciones (data-cantidad) ya escrita con el formato de receta/formato :: numero ("25", "2,5", "1.000",
     * "1234,5"): solo se le agrega la unidad, así la fila agregada queda igual que la que pinta el servidor.
     */
    function formatearMedida(cantidad, unidad) {
        if (cantidad === undefined || cantidad === null || cantidad === '' || !unidad) { return ''; }
        return cantidad + ' ' + unidad;
    }

    function selectorDe(dialogo, clave) {
        return dialogo.querySelector('[data-selector-de="' + clave + '"] [data-selector]');
    }

    // ---------- CUIT ----------

    /** Deja solo los dígitos (máximo 11) y les inserta los guiones: 20123456786 queda 20-12345678-6. */
    function formatearCuit(texto) {
        var digitos = texto.replace(/\D/g, '').slice(0, 11);
        if (digitos.length <= 2) { return digitos; }
        if (digitos.length <= 10) { return digitos.slice(0, 2) + '-' + digitos.slice(2); }
        return digitos.slice(0, 2) + '-' + digitos.slice(2, 10) + '-' + digitos.slice(10);
    }

    document.addEventListener('input', function (evento) {
        var campo = evento.target;
        if (!campo.matches || !campo.matches('[data-cuit]')) { return; }
        campo.value = formatearCuit(campo.value);
    });

    // ---------- Filas e índices ----------

    /**
     * Renumera los campos de todas las filas de la sección (sin huecos y en el orden en que se ven) y muestra u oculta
     * el texto de tabla vacía.
     */
    function sincronizar(seccion) {
        var filas = seccion.querySelectorAll('tbody [data-fila]');
        filas.forEach(function (fila, indice) {
            var prefijo = fila.getAttribute('data-prefijo');
            fila.querySelectorAll('[data-campo]').forEach(function (campo) {
                campo.name = prefijo + '[' + indice + '].' + campo.getAttribute('data-campo');
            });
        });
        var vacia = seccion.querySelector('[data-fila-vacia]');
        if (vacia) { vacia.hidden = filas.length > 0; }
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

    /** Único lugar que muestra u oculta un aviso de la ventana, por su clave (data-error). */
    function mostrarError(dialogo, clave, mostrar) {
        var aviso = dialogo.querySelector('[data-error="' + clave + '"]');
        if (aviso) { aviso.hidden = !mostrar; }
    }

    function limpiarErrores(dialogo) {
        dialogo.querySelectorAll('[data-error]').forEach(function (aviso) { aviso.hidden = true; });
    }

    /** Deja los DOS selectores de la ventana sin elección y sin avisos. */
    function reiniciar(dialogo) {
        dialogo.querySelectorAll('[data-selector]').forEach(function (selector) {
            selector.querySelector('[data-selector-id]').value = '';
            selector.querySelector('[data-selector-texto]').value = '';
            delete selector.datosElegidos;
        });
        limpiarErrores(dialogo);
    }

    function abrirAgregar(tipo) {
        var dialogo = document.getElementById('dialogo-agregar-' + tipo);
        reiniciar(dialogo);
        dialogo.showModal();
        selectorDe(dialogo, 'insumo').querySelector('[data-selector-texto]').focus();
    }

    /** Lee lo elegido en la ventana, sin validarlo, y arma los datos de la fila ({campos: ocultos, celdas: visibles}). */
    function leer(dialogo) {
        var insumo = selectorDe(dialogo, 'insumo');
        var presentacion = selectorDe(dialogo, 'presentacion');
        var datosInsumo = insumo.datosElegidos || {};
        var datosPresentacion = presentacion.datosElegidos || {};
        return {
            campos: {
                idInsumo: insumo.querySelector('[data-selector-id]').value,
                idPresentacionComercial: presentacion.querySelector('[data-selector-id]').value
            },
            celdas: {
                insumo: insumo.querySelector('[data-selector-texto]').value,
                tipoInsumo: datosInsumo.tipo || '',
                presentacion: presentacion.querySelector('[data-selector-texto]').value,
                medidaPresentacion: formatearMedida(datosPresentacion.cantidad, datosPresentacion.unidad)
            }
        };
    }

    // ---------- Validación previa a agregar la fila ----------

    /** Clave de repetición de un ítem, la misma que usa el servidor (ProveedorServicioImpl): insumo + presentación. */
    function claveDeRepetido(campos) {
        return campos.idInsumo + '|' + campos.idPresentacionComercial;
    }

    /** Claves de las filas que ya están en la tabla, leídas de sus campos ocultos. */
    function clavesExistentes(seccion) {
        return Array.prototype.map.call(seccion.querySelectorAll('tbody [data-fila]'), function (fila) {
            var campos = {};
            fila.querySelectorAll('[data-campo]').forEach(function (campo) {
                campos[campo.getAttribute('data-campo')] = campo.value;
            });
            return claveDeRepetido(campos);
        });
    }

    function confirmarAgregar(dialogo) {
        var seccion = document.querySelector('[data-detalle="' + dialogo.getAttribute('data-dialogo-agregar') + '"]');
        var datos = leer(dialogo);
        var foco = null;

        limpiarErrores(dialogo);

        if (!datos.campos.idInsumo) {
            mostrarError(dialogo, 'insumo', true);
            foco = foco || selectorDe(dialogo, 'insumo').querySelector('[data-selector-texto]');
        }
        if (!datos.campos.idPresentacionComercial) {
            mostrarError(dialogo, 'presentacion', true);
            foco = foco || selectorDe(dialogo, 'presentacion').querySelector('[data-selector-texto]');
        }
        // El repetido solo se evalúa con los dos datos elegidos: la clave depende de ambos
        if (!foco && clavesExistentes(seccion).indexOf(claveDeRepetido(datos.campos)) !== -1) {
            mostrarError(dialogo, 'duplicado', true);
            foco = selectorDe(dialogo, 'insumo').querySelector('[data-selector-texto]');
        }
        if (foco) {
            foco.focus();
            return;
        }

        agregarFila(seccion, datos);
        dialogo.close();
    }

    // Al elegir algo en un selector de la ventana se guardan sus datos extra y su aviso deja de tener sentido
    document.addEventListener('selector:cambio', function (evento) {
        var ventana = evento.target.closest('dialog[data-dialogo-agregar]');
        if (!ventana) { return; }
        var bloque = evento.target.closest('[data-selector-de]');
        evento.target.datosElegidos = evento.detail && evento.detail.id ? evento.detail.datos : undefined;
        if (bloque && evento.detail && evento.detail.id) {
            mostrarError(ventana, bloque.getAttribute('data-selector-de'), false);
        }
        mostrarError(ventana, 'duplicado', false);
    });

    // ---------- Eventos ----------

    document.addEventListener('click', function (evento) {
        var abrir = evento.target.closest('[data-abrir-agregar]');
        if (abrir) {
            abrirAgregar(abrir.getAttribute('data-abrir-agregar'));
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
        var formulario = document.getElementById('proveedor-form');
        var secciones = document.querySelectorAll('[data-detalle]');
        secciones.forEach(sincronizar);
        // Un CUIT cargado por el servidor (por ejemplo, tras un error) también se muestra con su formato
        var cuit = document.querySelector('[data-cuit]');
        if (cuit) { cuit.value = formatearCuit(cuit.value); }
        // Última garantía antes de enviar: índices sin huecos en la tabla del catálogo
        formulario.addEventListener('submit', function () {
            secciones.forEach(sincronizar);
        });
    });
})();
