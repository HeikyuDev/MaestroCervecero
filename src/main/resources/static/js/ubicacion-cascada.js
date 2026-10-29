/*
 * Ubicación en cascada (fragments/ubicacion-cascada.html): País -> Provincia -> Localidad.
 *
 * Atributos data-* de la página:
 *   data-ubicacion-cascada       bloque con los tres desplegables; lleva data-url-provincias y data-url-localidades.
 *   data-ubicacion-pais          desplegable de país.
 *   data-ubicacion-provincia     desplegable de provincia.
 *   data-ubicacion-localidad     desplegable de localidad (el único con name: es el que viaja en el formulario).
 *
 * Al cambiar el país se piden sus provincias a la URL de provincias (?idPais=) y se vacía la localidad; al cambiar la
 * provincia se piden sus localidades a la URL de localidades (?idProvincia=). Cada respuesta es un JSON
 * [{"id": 1, "nombre": "Córdoba"}, ...] ya en orden alfabético. Solo ayuda a elegir: no valida, eso lo hace el servidor.
 */
(function () {
    'use strict';

    function vaciar(select, textoInicial) {
        select.innerHTML = '';
        var opcion = document.createElement('option');
        opcion.value = '';
        opcion.textContent = textoInicial;
        select.appendChild(opcion);
        select.value = '';
    }

    function cargar(select, opciones) {
        opciones.forEach(function (o) {
            var opcion = document.createElement('option');
            opcion.value = o.id;
            opcion.textContent = o.nombre;
            select.appendChild(opcion);
        });
    }

    function pedir(url, parametro, valor, alTerminar) {
        fetch(url + '?' + parametro + '=' + encodeURIComponent(valor), {headers: {'Accept': 'application/json'}, credentials: 'same-origin'})
            .then(function (respuesta) {
                if (!respuesta.ok) { throw new Error('HTTP ' + respuesta.status); }
                return respuesta.json();
            })
            .then(alTerminar)
            .catch(function () { alTerminar([]); });
    }

    function iniciar(bloque) {
        var pais = bloque.querySelector('[data-ubicacion-pais]');
        var provincia = bloque.querySelector('[data-ubicacion-provincia]');
        var localidad = bloque.querySelector('[data-ubicacion-localidad]');

        pais.addEventListener('change', function () {
            vaciar(localidad, 'Seleccione localidad');
            localidad.disabled = true;
            vaciar(provincia, 'Seleccione provincia');
            if (!pais.value) {
                provincia.disabled = true;
                return;
            }
            pedir(bloque.getAttribute('data-url-provincias'), 'idPais', pais.value, function (opciones) {
                cargar(provincia, opciones);
                provincia.disabled = false;
            });
        });

        provincia.addEventListener('change', function () {
            vaciar(localidad, 'Seleccione localidad');
            if (!provincia.value) {
                localidad.disabled = true;
                return;
            }
            pedir(bloque.getAttribute('data-url-localidades'), 'idProvincia', provincia.value, function (opciones) {
                cargar(localidad, opciones);
                localidad.disabled = false;
            });
        });
    }

    document.querySelectorAll('[data-ubicacion-cascada]').forEach(iniciar);
})();
