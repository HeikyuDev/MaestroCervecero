/*
 * Formulario de receta: tablas de maltas, lúpulos y levaduras con ventana emergente para agregar filas.
 *
 * El servidor pinta las filas que la receta ya tiene y deja, por cada tabla, una <template> con una fila
 * vacía (receta/receta-form-filas.html). Este archivo solo reacciona a los atributos data-* de la página:
 *
 *   data-detalle                 sección de una tabla (malta, lupulo o levadura).
 *   data-abrir-agregar           botón "Agregar ..." de la sección: abre su ventana emergente.
 *   data-quitar-fila             botón "Quitar" de una fila.
 *   data-fila / data-prefijo     fila de la tabla y prefijo de sus campos (version.detallesMalta, etc.).
 *   data-fila-vacia              fila con el texto de tabla vacía.
 *   data-campo                   campo oculto de una fila (idMalta, cantidad, ...); su nombre es
 *                                prefijo[indice].campo.
 *   data-celda                   celda visible de una fila.
 *   template[data-plantilla]     fila vacía que se clona al agregar.
 *
 * Dentro de cada ventana emergente (dialog[data-dialogo-agregar]):
 *   data-campo-modal             campo de captura (cantidad, uso, tiempoDeHervor, etapaDeUso).
 *   data-grupo                   bloque que solo aparece para ciertos usos del lúpulo.
 *   data-error="<clave>"         aviso del campo (insumo, duplicado, cantidad, uso, tiempoDeHervor, etapaDeUso),
 *                                oculto mientras no haga falta; el texto lo trae la página.
 *   data-confirmar-agregar       "Agregar a la receta".
 *   data-cerrar-agregar          cierra la ventana sin agregar.
 * El insumo se elige con el selector de entidad de /js/modales.js (data-selector).
 *
 * Validación: es una excepción acordada a la regla de "validar solo en el servidor". Estas ventanas arman
 * filas de tablas que todavía no se enviaron, así que acá se frena la fila ANTES de que entre a la tabla
 * (insumo elegido, cantidad mayor a 0, sin repetidos, y uso / tiempo de hervor / etapa del lúpulo). Los
 * avisos se muestran dentro de la ventana, que queda abierta. Nada de esto reemplaza al servidor: al
 * enviar el formulario valida todo de nuevo (Bean Validation y reglas del service).
 *
 * Índices: Spring solo enlaza listas sin huecos (detallesMalta[0], [1], [2]...). Por eso después de
 * cada alta o baja de fila se renumeran los campos de toda la tabla, y se vuelve a hacer al enviar el
 * formulario; así el próximo índice libre es siempre la cantidad de filas.
 */
(function () {
    'use strict';

    var USOS = {HERVOR: 'Hervor', WHIRLPOOL: 'Whirlpool', DRY_HOP: 'Dry hop'};
    var ETAPAS = {MACERACION: 'Maceración', HERVIDO: 'Hervido', FERMENTACION: 'Fermentación', MADURACION: 'Maduración'};

    // Lo que cambia entre una tabla y otra: el campo con el id del insumo y la unidad de la cantidad
    var TIPOS = {
        malta: {campoId: 'idMalta', unidad: 'kg'},
        lupulo: {campoId: 'idLupulo', unidad: 'g'},
        levadura: {campoId: 'idLevadura', unidad: 'g'}
    };

    // ---------- Utilidades ----------

    /**
     * Cantidad con su unidad tal como la muestra el servidor (receta/formato :: medida): sin ceros
     * sobrantes, con coma decimal y punto como separador de miles en los enteros ("4,5 kg", "1.500 g").
     * Si el campo quedó vacío la celda queda vacía.
     */
    function formatearNumero(valor) {
        var numero = Number(valor);
        return Number.isInteger(numero)
            ? String(numero).replace(/\B(?=(\d{3})+(?!\d))/g, '.')
            : String(numero).replace('.', ',');
    }

    function formatear(valor, unidad) {
        if (valor === '') { return ''; }
        return formatearNumero(valor) + ' ' + unidad;
    }

    function campoModal(dialogo, nombre) {
        return dialogo.querySelector('[data-campo-modal="' + nombre + '"]');
    }

    // ---------- Filas e índices ----------

    /**
     * Renumera los campos de todas las filas de la sección (sin huecos y en el orden en que se ven) y
     * muestra u oculta el texto de tabla vacía.
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

    /** Clona la fila vacía de la sección, la completa con los valores y la agrega al final de la tabla. */
    function agregarFila(tipo, seccion, datos) {
        var plantilla = seccion.querySelector('template[data-plantilla]');
        var fila = plantilla.content.firstElementChild.cloneNode(true);

        fila.querySelectorAll('[data-campo]').forEach(function (campo) {
            var valor = datos.campos[campo.getAttribute('data-campo')];
            if (valor === undefined) {
                campo.remove(); // campo que no aplica (el tiempo de hervor fuera del uso Hervor)
            } else {
                campo.value = valor;
            }
        });
        fila.querySelectorAll('[data-celda]').forEach(function (celda) {
            var texto = datos.celdas[celda.getAttribute('data-celda')];
            if (texto !== undefined) { celda.textContent = texto; }
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

    /** Muestra solo los bloques que corresponden al uso elegido: tiempo de hervor (Hervor) o etapa (Dry hop). */
    function actualizarGrupos(dialogo) {
        var uso = campoModal(dialogo, 'uso');
        if (!uso) { return; }
        var tiempo = dialogo.querySelector('[data-grupo="tiempoDeHervor"]');
        var etapa = dialogo.querySelector('[data-grupo="etapaDeUso"]');
        tiempo.hidden = uso.value !== 'HERVOR';
        etapa.hidden = uso.value !== 'DRY_HOP';
    }

    /** Deja la ventana vacía, sin aviso y con el estado inicial del uso del lúpulo. */
    function reiniciar(dialogo) {
        dialogo.querySelector('[data-selector-id]').value = '';
        dialogo.querySelector('[data-selector-texto]').value = '';
        dialogo.querySelectorAll('[data-campo-modal]').forEach(function (campo) { campo.value = ''; });
        limpiarErrores(dialogo);
        actualizarGrupos(dialogo);
    }

    function abrirAgregar(tipo) {
        var dialogo = document.getElementById('dialogo-agregar-' + tipo);
        reiniciar(dialogo);
        dialogo.showModal();
        dialogo.querySelector('[data-selector-texto]').focus();
    }

    /**
     * Lee lo ingresado en la ventana, sin validarlo, y arma los datos de la fila ({campos: ocultos,
     * celdas: visibles}). La etapa de uso del Hervor y del Whirlpool la define el sistema (Hervido).
     */
    function leer(tipo, dialogo) {
        var cfg = TIPOS[tipo];
        var cantidad = campoModal(dialogo, 'cantidad').value;

        var campos = {};
        campos[cfg.campoId] = dialogo.querySelector('[data-selector-id]').value;
        campos.cantidad = cantidad;
        var celdas = {
            nombre: dialogo.querySelector('[data-selector-texto]').value,
            cantidad: formatear(cantidad, cfg.unidad)
        };

        if (tipo === 'lupulo') {
            var uso = campoModal(dialogo, 'uso').value;
            var etapa = uso === 'DRY_HOP' ? campoModal(dialogo, 'etapaDeUso').value : (uso ? 'HERVIDO' : '');
            campos.uso = uso;
            campos.etapaDeUso = etapa;
            celdas.uso = USOS[uso];
            celdas.etapaDeUso = ETAPAS[etapa];
            if (uso === 'HERVOR') {
                campos.tiempoDeHervor = campoModal(dialogo, 'tiempoDeHervor').value;
                celdas.tiempoDeHervor = formatear(campos.tiempoDeHervor, 'min');
            } else {
                celdas.tiempoDeHervor = '-';
            }
        }

        return {campos: campos, celdas: celdas};
    }

    // ---------- Validación previa a agregar la fila ----------

    function esMayorACero(valor) {
        return valor !== '' && Number(valor) > 0;
    }

    /**
     * Clave de repetición de una fila, la misma que usa el servidor (RecetaServicioImpl): la malta y la
     * levadura por su id; el lúpulo por id + uso + tiempo de hervor (0 cuando el uso no es Hervor).
     */
    function claveDeRepetido(tipo, campos) {
        if (tipo !== 'lupulo') { return String(campos[TIPOS[tipo].campoId]); }
        var tiempo = campos.uso === 'HERVOR' ? Number(campos.tiempoDeHervor) : 0;
        return [campos.idLupulo, campos.uso, tiempo].join('|');
    }

    /** Claves de las filas que ya están en la tabla, leídas de sus campos ocultos. */
    function clavesExistentes(tipo, seccion) {
        return Array.prototype.map.call(seccion.querySelectorAll('tbody [data-fila]'), function (fila) {
            var campos = {};
            fila.querySelectorAll('[data-campo]').forEach(function (campo) {
                campos[campo.getAttribute('data-campo')] = campo.value;
            });
            return claveDeRepetido(tipo, campos);
        });
    }

    /**
     * Devuelve los problemas de la fila como [{clave, foco}]: la clave del aviso a mostrar y el elemento
     * que recibe el foco. Vacía si la fila puede entrar a la tabla.
     */
    function validar(tipo, dialogo, seccion, campos) {
        var problemas = [];
        function falla(clave, nombreCampo) {
            problemas.push({clave: clave, foco: campoModal(dialogo, nombreCampo)});
        }

        if (!esMayorACero(campos.cantidad)) { falla('cantidad', 'cantidad'); }

        if (tipo === 'lupulo') {
            if (!campos.uso) {
                falla('uso', 'uso');
            } else if (campos.uso === 'HERVOR' && !esMayorACero(campos.tiempoDeHervor)) {
                falla('tiempoDeHervor', 'tiempoDeHervor');
            } else if (campos.uso === 'DRY_HOP' && !campos.etapaDeUso) {
                falla('etapaDeUso', 'etapaDeUso');
            }
        }

        // El repetido solo se evalúa con el resto de los datos bien: la clave del lúpulo depende de ellos
        if (!problemas.length && clavesExistentes(tipo, seccion).indexOf(claveDeRepetido(tipo, campos)) !== -1) {
            problemas.push({clave: 'duplicado', foco: dialogo.querySelector('[data-selector-texto]')});
        }
        return problemas;
    }

    function confirmarAgregar(dialogo) {
        var tipo = dialogo.getAttribute('data-dialogo-agregar');
        var seccion = document.querySelector('[data-detalle="' + tipo + '"]');
        var datos = leer(tipo, dialogo);

        limpiarErrores(dialogo);

        // Sin insumo no hay nada que mostrar en la fila
        if (!datos.campos[TIPOS[tipo].campoId]) {
            mostrarError(dialogo, 'insumo', true);
            dialogo.querySelector('[data-selector-texto]').focus();
            return;
        }

        var problemas = validar(tipo, dialogo, seccion, datos.campos);
        if (problemas.length) {
            problemas.forEach(function (problema) { mostrarError(dialogo, problema.clave, true); });
            if (problemas[0].foco) { problemas[0].foco.focus(); }
            return;
        }

        agregarFila(tipo, seccion, datos);
        dialogo.close();
    }

    // Al elegir algo en un selector de una ventana, los avisos del insumo dejan de tener sentido
    document.addEventListener('selector:cambio', function (evento) {
        var ventana = evento.target.closest('dialog');
        if (!ventana || !evento.detail || !evento.detail.id) { return; }
        mostrarError(ventana, 'insumo', false);
        mostrarError(ventana, 'duplicado', false);
    });

    /** Al corregir un campo de la ventana se oculta su aviso (y el de repetido, que depende de varios campos). */
    function corregido(campo) {
        var ventana = campo.closest('dialog');
        mostrarError(ventana, campo.getAttribute('data-campo-modal'), false);
        mostrarError(ventana, 'duplicado', false);
    }

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

    document.addEventListener('change', function (evento) {
        var campo = evento.target.closest('[data-campo-modal]');
        if (!campo) { return; }
        if (campo.getAttribute('data-campo-modal') === 'uso') { actualizarGrupos(campo.closest('dialog')); }
        corregido(campo);
    });

    document.addEventListener('input', function (evento) {
        var campo = evento.target.closest ? evento.target.closest('[data-campo-modal]') : null;
        if (campo) { corregido(campo); }
    });

    // Enter en un campo de la ventana equivale a "Agregar a la receta"
    document.addEventListener('keydown', function (evento) {
        var campo = evento.target.closest ? evento.target.closest('input[data-campo-modal]') : null;
        if (campo && evento.key === 'Enter') {
            evento.preventDefault();
            confirmarAgregar(campo.closest('dialog'));
        }
    });

    document.addEventListener('DOMContentLoaded', function () {
        var formulario = document.getElementById('receta-form');
        var secciones = document.querySelectorAll('[data-detalle]');
        secciones.forEach(sincronizar);
        // Última garantía antes de enviar: índices sin huecos en las tres tablas
        formulario.addEventListener('submit', function () {
            secciones.forEach(sincronizar);
        });
    });
})();
