/*
 * Formulario de receta: tablas de maltas, lúpulos y levaduras con ventana emergente para agregar filas, y
 * plan de monitoreo con ventana emergente para agregar planes (más abajo, "Plan de monitoreo").
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
 *   data-error="insumo"          aviso de que falta elegir el insumo (oculto mientras no haga falta).
 *   data-confirmar-agregar       "Agregar a la receta".
 *   data-cerrar-agregar          cierra la ventana sin agregar.
 * El insumo se elige con el selector de entidad de /js/modales.js (data-selector).
 *
 * Este archivo no valida datos: lo que se escribe en la ventana se agrega a la fila tal cual y el servidor
 * lo valida al enviar el formulario (Bean Validation y reglas del service), igual que el resto de los
 * formularios. La única comprobación es que haya un insumo elegido, porque sin él no hay nada que mostrar.
 *
 * Índices: Spring solo enlaza listas sin huecos (detallesMalta[0], [1], [2]...). Por eso después de
 * cada alta o baja de fila se renumeran los campos de toda la tabla, y se vuelve a hacer al enviar el
 * formulario; así el próximo índice libre es siempre la cantidad de filas.
 *
 * Plan de monitoreo (sección [data-plan-monitoreo], fuera de las tablas anteriores):
 *   data-abrir-plan              botón "Agregar plan de monitoreo": abre #dialogo-agregar-plan.
 *   data-plan                    bloque de un plan; data-plan-campo es su campo oculto idEtapaControl.
 *   data-parametro-plan          fila de un parámetro dentro del bloque; sus campos ocultos son data-campo
 *                                (idParametroControl, valorMinimo, valorIdeal, valorMaximo).
 *   data-quitar-plan             botón "Quitar plan" del bloque.
 *   template[data-plantilla-plan] y template[data-plantilla-parametro-plan]
 *                                bloque vacío y fila de parámetro que se clonan al confirmar la ventana.
 * Dentro de la ventana (#dialogo-agregar-plan):
 *   data-bloque="etapa|parametro"  contenedor de cada selector de entidad.
 *   data-campo-plan              campos del mini formulario (minimo, ideal, maximo).
 *   data-agregar-parametro       agrega el parámetro elegido (con sus valores) a la tabla del plan.
 *   data-parametro-dialogo       fila de esa tabla (los datos viajan en sus data-*);
 *                                data-quitar-parametro-dialogo la quita.
 *   data-confirmar-plan          "Agregar a la receta": agrega el bloque completo; data-cerrar-plan cierra.
 *   data-error="etapa|parametro" avisos de que falta elegir la entidad (lo único que se comprueba acá).
 * El servidor valida el plan (etapa, parámetros, valores y rangos) igual que el resto del formulario. Los índices
 * son de dos niveles (version.planesMonitoreo[i].detallesParametroControl[j].campo) y se renumeran ambos al
 * agregar o quitar un plan y al enviar el formulario.
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

    function mostrarAvisoInsumo(dialogo, mostrar) {
        var aviso = dialogo.querySelector('[data-error="insumo"]');
        if (aviso) { aviso.hidden = !mostrar; }
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
        mostrarAvisoInsumo(dialogo, false);
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

    function confirmarAgregar(dialogo) {
        var tipo = dialogo.getAttribute('data-dialogo-agregar');
        var seccion = document.querySelector('[data-detalle="' + tipo + '"]');
        var datos = leer(tipo, dialogo);

        // Sin insumo no hay nada que mostrar en la fila: es lo único que se comprueba acá
        if (!datos.campos[TIPOS[tipo].campoId]) {
            mostrarAvisoInsumo(dialogo, true);
            return;
        }

        agregarFila(tipo, seccion, datos);
        dialogo.close();
    }

    // ---------- Plan de monitoreo ----------

    function dialogoPlan() { return document.getElementById('dialogo-agregar-plan'); }

    function seccionPlanes() { return document.querySelector('[data-plan-monitoreo]'); }

    function selectorDe(dialogo, bloque) { return dialogo.querySelector('[data-bloque="' + bloque + '"] [data-selector]'); }

    function idDe(selector) { return selector.querySelector('[data-selector-id]').value; }

    function textoDe(selector) { return selector.querySelector('[data-selector-texto]').value; }

    function mostrarAvisoPlan(dialogo, nombre, mostrar) {
        dialogo.querySelector('[data-error="' + nombre + '"]').hidden = !mostrar;
    }

    function vaciarSelector(selector) {
        selector.querySelector('[data-selector-id]').value = '';
        selector.querySelector('[data-selector-texto]').value = '';
        ['etapa', 'unidad', 'minimo', 'maximo'].forEach(function (dato) { selector.removeAttribute('data-' + dato); });
    }

    /** Escribe la unidad del parámetro elegido en las etiquetas de los valores y muestra su rango admitido. */
    function actualizarUnidadParametro(dialogo) {
        var selector = selectorDe(dialogo, 'parametro');
        var elegido = idDe(selector) !== '' && selector.hasAttribute('data-unidad');
        var unidad = elegido ? selector.getAttribute('data-unidad') : '';
        dialogo.querySelectorAll('[data-unidad-parametro]').forEach(function (etiqueta) {
            etiqueta.textContent = unidad || 'unidad';
        });
        dialogo.querySelector('[data-rango-parametro]').textContent = elegido
            ? 'Rango admitido: ' + formatearNumero(selector.getAttribute('data-minimo')) + '–'
                + formatear(selector.getAttribute('data-maximo'), unidad)
            : 'Elegí un parámetro para ver su unidad y su rango admitido.';
    }

    /** Deja la ventana vacía: sin etapa, sin parámetros en la tabla, con el mini formulario limpio y sin avisos. */
    function reiniciarPlan(dialogo) {
        vaciarSelector(selectorDe(dialogo, 'etapa'));
        vaciarSelector(selectorDe(dialogo, 'parametro'));
        dialogo.querySelectorAll('[data-campo-plan]').forEach(function (campo) { campo.value = ''; });
        dialogo.querySelectorAll('[data-parametro-dialogo]').forEach(function (fila) { fila.remove(); });
        dialogo.querySelector('[data-parametros-dialogo-vacio]').hidden = false;
        mostrarAvisoPlan(dialogo, 'etapa', false);
        mostrarAvisoPlan(dialogo, 'parametro', false);
        actualizarUnidadParametro(dialogo);
    }

    function abrirPlan() {
        var dialogo = dialogoPlan();
        reiniciarPlan(dialogo);
        dialogo.showModal();
        selectorDe(dialogo, 'etapa').querySelector('[data-selector-texto]').focus();
    }

    function valorPlan(dialogo, campo) {
        return dialogo.querySelector('[data-campo-plan="' + campo + '"]').value;
    }

    /**
     * Agrega a la tabla de la ventana el parámetro elegido con los valores tal como se escribieron (sin validarlos).
     * Sin parámetro elegido no hay nada que mostrar: solo se avisa.
     */
    function agregarParametroAlPlan(dialogo) {
        var selector = selectorDe(dialogo, 'parametro');
        if (idDe(selector) === '') {
            mostrarAvisoPlan(dialogo, 'parametro', true);
            return;
        }
        mostrarAvisoPlan(dialogo, 'parametro', false);

        var unidad = selector.getAttribute('data-unidad') || '';
        var datos = {
            id: idDe(selector), nombre: textoDe(selector), unidad: unidad,
            minimo: valorPlan(dialogo, 'minimo'), ideal: valorPlan(dialogo, 'ideal'), maximo: valorPlan(dialogo, 'maximo')
        };

        var fila = dialogo.querySelector('template[data-plantilla-parametro-dialogo]').content.firstElementChild.cloneNode(true);
        Object.keys(datos).forEach(function (dato) { fila.setAttribute('data-' + dato, datos[dato]); });
        fila.querySelector('[data-celda="nombre"]').textContent = datos.nombre;
        ['minimo', 'ideal', 'maximo'].forEach(function (valor) {
            fila.querySelector('[data-celda="' + valor + '"]').textContent = formatear(datos[valor], unidad);
        });

        var vacia = dialogo.querySelector('[data-parametros-dialogo-vacio]');
        vacia.parentNode.insertBefore(fila, vacia);
        vacia.hidden = true;

        vaciarSelector(selector);
        dialogo.querySelectorAll('[data-campo-plan]').forEach(function (campo) { campo.value = ''; });
        actualizarUnidadParametro(dialogo);
    }

    /**
     * Renumera los dos niveles de índices: cada plan (version.planesMonitoreo[i]) y, dentro de él, cada parámetro
     * (detallesParametroControl[j]), sin huecos y en el orden en que se ven. Muestra u oculta el texto de sin planes.
     */
    function sincronizarPlanes() {
        var seccion = seccionPlanes();
        if (!seccion) { return; }
        var planes = seccion.querySelectorAll('[data-planes] > [data-plan]');
        planes.forEach(function (plan, i) {
            plan.querySelector('[data-plan-campo]').name = 'version.planesMonitoreo[' + i + '].idEtapaControl';
            plan.querySelectorAll('[data-parametro-plan]').forEach(function (fila, j) {
                fila.querySelectorAll('[data-campo]').forEach(function (campo) {
                    campo.name = 'version.planesMonitoreo[' + i + '].detallesParametroControl[' + j + '].' + campo.getAttribute('data-campo');
                });
            });
        });
        seccion.querySelector('[data-planes-vacio]').hidden = planes.length > 0;
    }

    /** Arma el bloque completo del plan con lo cargado en la ventana y lo agrega a la receta. */
    function confirmarPlan(dialogo) {
        var selector = selectorDe(dialogo, 'etapa');
        // Sin etapa de control no hay nada que mostrar en el bloque: es lo único que se comprueba acá
        if (idDe(selector) === '') {
            mostrarAvisoPlan(dialogo, 'etapa', true);
            return;
        }

        var seccion = seccionPlanes();
        var plan = seccion.querySelector('template[data-plantilla-plan]').content.firstElementChild.cloneNode(true);
        plan.querySelector('[data-plan-campo]').value = idDe(selector);
        plan.querySelector('[data-celda="nombre"]').textContent = textoDe(selector);
        plan.querySelector('[data-celda="etapa"]').textContent = ETAPAS[selector.getAttribute('data-etapa')] || '';
        plan.querySelector('[data-etapa-bloque]').hidden = false;

        var plantilla = seccion.querySelector('template[data-plantilla-parametro-plan]');
        var cuerpo = plan.querySelector('[data-parametros-plan]');
        dialogo.querySelectorAll('[data-parametro-dialogo]').forEach(function (origen) {
            var fila = plantilla.content.firstElementChild.cloneNode(true);
            var unidad = origen.getAttribute('data-unidad');
            var campos = {
                idParametroControl: origen.getAttribute('data-id'), valorMinimo: origen.getAttribute('data-minimo'),
                valorIdeal: origen.getAttribute('data-ideal'), valorMaximo: origen.getAttribute('data-maximo')
            };
            fila.querySelectorAll('[data-campo]').forEach(function (campo) {
                campo.value = campos[campo.getAttribute('data-campo')];
            });
            fila.querySelector('[data-celda="nombre"]').textContent = origen.getAttribute('data-nombre');
            fila.querySelector('[data-celda="valorMinimo"]').textContent = formatear(campos.valorMinimo, unidad);
            fila.querySelector('[data-celda="valorIdeal"]').textContent = formatear(campos.valorIdeal, unidad);
            fila.querySelector('[data-celda="valorMaximo"]').textContent = formatear(campos.valorMaximo, unidad);
            cuerpo.appendChild(fila);
        });

        seccion.querySelector('[data-planes]').appendChild(plan);
        sincronizarPlanes();
        dialogo.close();
    }

    document.addEventListener('click', function (evento) {
        if (evento.target.closest('[data-abrir-plan]')) {
            abrirPlan();
            return;
        }
        var quitarPlan = evento.target.closest('[data-quitar-plan]');
        if (quitarPlan) {
            quitarPlan.closest('[data-plan]').remove();
            sincronizarPlanes();
            return;
        }
        var quitarParametro = evento.target.closest('[data-quitar-parametro-dialogo]');
        if (quitarParametro) {
            var dialogo = quitarParametro.closest('dialog');
            quitarParametro.closest('[data-parametro-dialogo]').remove();
            dialogo.querySelector('[data-parametros-dialogo-vacio]').hidden =
                dialogo.querySelectorAll('[data-parametro-dialogo]').length > 0;
            return;
        }
        if (evento.target.closest('[data-agregar-parametro]')) {
            agregarParametroAlPlan(dialogoPlan());
            return;
        }
        if (evento.target.closest('[data-confirmar-plan]')) {
            confirmarPlan(dialogoPlan());
            return;
        }
        if (evento.target.closest('[data-cerrar-plan]')) {
            dialogoPlan().close();
        }
    });

    // Al elegir (o quitar) la etapa de control o el parámetro, el selector guarda los datos que necesita el plan
    document.addEventListener('selector:cambio', function (evento) {
        var selector = evento.target;
        var dialogo = dialogoPlan();
        if (!dialogo || !dialogo.contains(selector)) { return; }
        var datos = evento.detail.datos || {};
        ['etapa', 'unidad', 'minimo', 'maximo'].forEach(function (dato) {
            if (datos[dato] === undefined) {
                selector.removeAttribute('data-' + dato);
            } else {
                selector.setAttribute('data-' + dato, datos[dato]);
            }
        });
        if (selector === selectorDe(dialogo, 'parametro')) { actualizarUnidadParametro(dialogo); }
    });

    // Enter en un valor del mini formulario equivale a "Agregar parámetro"
    document.addEventListener('keydown', function (evento) {
        var campo = evento.target.closest ? evento.target.closest('input[data-campo-plan]') : null;
        if (campo && evento.key === 'Enter') {
            evento.preventDefault();
            agregarParametroAlPlan(campo.closest('dialog'));
        }
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

    document.addEventListener('change', function (evento) {
        var campo = evento.target.closest('[data-campo-modal]');
        if (!campo) { return; }
        if (campo.getAttribute('data-campo-modal') === 'uso') { actualizarGrupos(campo.closest('dialog')); }
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
        sincronizarPlanes();
        // Última garantía antes de enviar: índices sin huecos en las tres tablas y en los dos niveles del plan
        formulario.addEventListener('submit', function () {
            secciones.forEach(sincronizar);
            sincronizarPlanes();
        });
    });
})();
