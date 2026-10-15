## Estilos y recursos locales

La aplicación no depende de internet para verse: todos los estilos, íconos y fuentes se sirven desde `src/main/resources/static`.

| Recurso | Ubicación |
| --- | --- |
| Tailwind CSS 3.4 (CSS compilado y minificado) | `static/css/tailwind.css` |
| Bootstrap Icons 1.11.3 (CSS, fuentes `woff2` y `woff`, licencia MIT) | `static/vendor/bootstrap-icons/` |

Ambos se cargan con `<link rel="stylesheet" th:href="@{...}">` en `layout/base.html` y `login.html`. `SecurityConfig` permite sin sesión `/css/**`, `/js/**`, `/img/**` y `/vendor/**`, de modo que el login también se ve con los estilos.

### Reconstruir `tailwind.css`

Tailwind genera solamente las clases que encuentra escritas en el código. Cada vez que se usa una clase nueva (en una plantilla, un script o una cadena de Java) hay que reconstruir el CSS:

```
cd frontend/tailwind
npm install      # solo la primera vez (instala tailwindcss 3.4.17 en node_modules, que no se versiona)
npm run build
```

- `frontend/tailwind/tailwind.config.js` define los archivos que se escanean: `templates/**/*.html`, `static/js/**/*.js` y `src/main/java/**/*.java`.
- Las clases deben aparecer completas en el texto (por ejemplo `bg-amber-100`); una clase armada por partes (`'bg-' + color`) no se detecta. Si hace falta, se agrega a `safelist` en `tailwind.config.js`.
- El archivo generado `static/css/tailwind.css` se versiona junto con el cambio que lo motivó.
