// Botón del ojo de los campos de contraseña: muestra u oculta el texto del campo que indica
// data-alternar-contrasena. Solo cambia la visibilidad: no valida nada.
(function () {
    document.querySelectorAll('[data-alternar-contrasena]').forEach(function (boton) {
        var campo = document.getElementById(boton.getAttribute('data-alternar-contrasena'));
        var icono = boton.querySelector('i');
        if (!campo) {
            return;
        }
        boton.addEventListener('click', function () {
            var mostrar = campo.type === 'password';
            campo.type = mostrar ? 'text' : 'password';
            boton.setAttribute('aria-pressed', String(mostrar));
            boton.setAttribute('aria-label', mostrar ? 'Ocultar contraseña' : 'Mostrar contraseña');
            if (icono) {
                icono.classList.toggle('bi-eye', !mostrar);
                icono.classList.toggle('bi-eye-slash', mostrar);
            }
        });
    });
})();
