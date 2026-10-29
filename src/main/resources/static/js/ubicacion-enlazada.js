/*
 * Ubicación con selectores de entidad enlazados (fragments/ubicacion-enlazada.html): País, Provincia y Localidad.
 *
 * Atributos data-* de la página:
 *   data-ubicacion-enlazada          bloque con los tres selectores.
 *   data-ubicacion="pais|provincia|localidad"
 *                                    contenedor de cada selector (fragments/selector-entidad). Lleva data-padre-id: el id
 *                                    del elemento del que depende lo elegido (el país de la provincia, la provincia de
 *                                    la localidad).
 *
 * Reglas (al elegir o quitar en un selector, /js/modales.js emite 'selector:cambio'):
 *   - País elegido: la provincia y la localidad se limpian si no pertenecen a ese país. Quitado: se limpian ambas.
 *   - Provincia elegida: se completa el país (con los datos de la fila: data-id-pais, data-texto-pais) y se limpia la
 *     localidad si no pertenece a ella. Quitada: se limpia la localidad.
 *   - Localidad elegida: se completan la provincia y el país (data-id-provincia, data-texto-provincia, data-id-pais,
 *     data-texto-pais).
 *   - El buscador de provincias se abre filtrado por el país elegido y el de localidades por la provincia elegida (o,
 *     sin provincia, por el país). Esos filtros se pueden quitar o cambiar en el propio buscador.
 * Es solo ayuda de pantalla: no valida. Solo la localidad viaja al servidor, que valida todo.
 */
(function () {
    'use strict';

    function contenedor(bloque, tipo) { return bloque.querySelector('[data-ubicacion="' + tipo + '"]'); }
    function selector(bloque, tipo) { return contenedor(bloque, tipo).querySelector('[data-selector]'); }
    function idDe(bloque, tipo) { return selector(bloque, tipo).querySelector('[data-selector-id]').value; }

    function poner(bloque, tipo, id, texto, padreId) {
        var s = selector(bloque, tipo);
        s.querySelector('[data-selector-id]').value = id;
        s.querySelector('[data-selector-texto]').value = texto;
        if (padreId !== undefined) { contenedor(bloque, tipo).setAttribute('data-padre-id', padreId); }
    }

    function limpiar(bloque, tipo) { poner(bloque, tipo, '', '', ''); }

    /** Deja los buscadores de provincia y de localidad con el filtro de lo ya elegido. */
    function actualizarFiltros(bloque) {
        var pais = idDe(bloque, 'pais');
        var provincia = idDe(bloque, 'provincia');
        var sProvincia = selector(bloque, 'provincia');
        var sLocalidad = selector(bloque, 'localidad');
        var baseProvincia = sProvincia.getAttribute('data-buscador-base');
        var baseLocalidad = sLocalidad.getAttribute('data-buscador-base');
        sProvincia.setAttribute('data-buscador-url', pais ? baseProvincia + '?idPais=' + encodeURIComponent(pais) : baseProvincia);
        var filtroLocalidad = provincia ? '?idProvincia=' + encodeURIComponent(provincia) : (pais ? '?idPais=' + encodeURIComponent(pais) : '');
        sLocalidad.setAttribute('data-buscador-url', baseLocalidad + filtroLocalidad);
    }

    function iniciar(bloque) {
        ['provincia', 'localidad'].forEach(function (tipo) {
            var s = selector(bloque, tipo);
            s.setAttribute('data-buscador-base', s.getAttribute('data-buscador-url'));
        });
        actualizarFiltros(bloque);

        bloque.addEventListener('selector:cambio', function (evento) {
            var origen = evento.target.closest('[data-ubicacion]');
            if (!origen) { return; }
            var tipo = origen.getAttribute('data-ubicacion');
            var id = evento.detail.id;
            var datos = evento.detail.datos || {};

            if (tipo === 'pais') {
                // La provincia y la localidad que no son de este país (o todas, si se quitó el país) se limpian
                if (!id || contenedor(bloque, 'provincia').getAttribute('data-padre-id') !== id) {
                    limpiar(bloque, 'provincia');
                    limpiar(bloque, 'localidad');
                }
            } else if (tipo === 'provincia') {
                if (id) {
                    poner(bloque, 'pais', datos.idPais, datos.textoPais);
                    poner(bloque, 'provincia', id, datos.nombre, datos.idPais);
                    if (contenedor(bloque, 'localidad').getAttribute('data-padre-id') !== id) { limpiar(bloque, 'localidad'); }
                } else {
                    limpiar(bloque, 'localidad');
                }
            } else if (id) {
                poner(bloque, 'pais', datos.idPais, datos.textoPais);
                poner(bloque, 'provincia', datos.idProvincia, datos.textoProvincia, datos.idPais);
                poner(bloque, 'localidad', id, datos.nombre, datos.idProvincia);
            }
            actualizarFiltros(bloque);
        });
    }

    document.querySelectorAll('[data-ubicacion-enlazada]').forEach(iniciar);
})();
