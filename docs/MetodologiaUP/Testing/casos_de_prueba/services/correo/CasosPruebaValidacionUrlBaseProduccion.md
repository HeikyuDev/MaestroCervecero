## Casos de prueba de `ValidacionUrlBaseProduccion`

**Tipo de prueba:** unitarias (clase `ValidacionUrlBaseProduccionTest`), sin contexto de Spring.

**Reglas:** en el perfil `prod`, la dirección pública con la que se arman los enlaces de los correos (`app.url-base`, variable de entorno `APP_URL_BASE`, sin valor por defecto) debe empezar con `https://`, sin distinguir mayúsculas. Si no, la aplicación no arranca: el componente lanza `IllegalStateException` al crearse.

---

### 1. `ValidacionUrlBaseProduccion(String urlBase)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-VUB-01** | Dirección HTTPS | `urlBase: "https://maestrocervecero.com"` y `"HTTPS://maestrocervecero.com"` | Empieza con `https://` | No lanza excepción. |
| **CP-VUB-02** | Dirección HTTP | `urlBase: "http://localhost:7777"` | No usa HTTPS | Lanza `IllegalStateException` con "La propiedad app.url-base (variable de entorno APP_URL_BASE) debe empezar con https:// en producción. Valor actual: 'http://localhost:7777'". |
| **CP-VUB-03** | Dirección nula o en blanco | `urlBase: null` y `"   "` | No hay dirección | Lanza `IllegalStateException`. |
| **CP-VUB-04** | Solo en producción | Anotación `@Profile` de la clase | El componente se registra solo en `prod` | `@Profile` tiene el único valor `"prod"`: dev y test usan `http://localhost:7777`. |
