# Base de datos

## Arquitectura

| Responsabilidad | Herramienta |
|---|---|
| Crear y modificar el esquema (tablas, columnas, restricciones, índices) | Flyway, con migraciones SQL en `../../src/main/resources/db/migration` |
| Datos iniciales de sistema | Flyway (`V2__datos_iniciales.sql`) |
| Lectura y escritura de datos de la aplicación (DML) | Hibernate / Spring Data JPA |
| Comprobar que las entidades coinciden con las tablas | Hibernate con `spring.jpa.hibernate.ddl-auto=validate` |

Al arrancar, Flyway aplica las migraciones pendientes y después Hibernate valida el esquema. Si una entidad
declara una tabla o columna que no existe, o con un tipo incompatible, la aplicación no arranca y el error indica
qué elemento falta.

Propiedades relevantes de `application.properties`:

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=false
```

## Entornos

Cada entorno es un perfil de Spring con su propia base de datos. `application.properties` contiene la configuración
común y cada perfil define la URL, el usuario, la clave y si se muestra el SQL.

| Perfil | Uso | Archivo | Base | SQL en consola |
|---|---|---|---|---|
| `dev` (por defecto) | Trabajo diario y pruebas manuales en el navegador | `../../src/main/resources/application-dev.properties` | `maestrocervecero_dev` | Sí |
| `prod` | Versión real y de demostración | `../../src/main/resources/application-prod.properties` | `maestrocervecero` | No |
| `test` | Reservado para los tests de integración | `../../src/test/resources/application-test.properties` | `maestrocervecero_test` | No |

El perfil se elige con la propiedad `spring.profiles.active` o con la variable de entorno `SPRING_PROFILES_ACTIVE`. Sin
perfil activo se usa `dev` (`spring.profiles.default=dev`).

| Qué | Comando |
|---|---|
| Arrancar en desarrollo | `./mvnw spring-boot:run` |
| Arrancar en producción | `./mvnw spring-boot:run "-Dspring-boot.run.profiles=prod"` |
| Arrancar en producción con variable de entorno (PowerShell) | `$env:SPRING_PROFILES_ACTIVE="prod"; ./mvnw spring-boot:run` |

En `prod`, la URL, el usuario y la clave se pueden reemplazar con `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y
`SPRING_DATASOURCE_PASSWORD`; sin ellas apuntan a `jdbc:postgresql://localhost:5432/maestrocervecero` con el usuario
`AdminCerveza`.

Cada base se crea una vez, vacía. Al primer arranque Flyway crea el esquema y los datos iniciales, y `UserDataLoader` crea
el usuario administrador:

```sql
CREATE DATABASE maestrocervecero_dev OWNER "AdminCerveza";
```

## Migraciones

### Convención de nombres

`V<n>__<descripcion_en_minusculas>.sql`, con dos guiones bajos después del número. Ejemplos:
`V1__esquema_inicial.sql`, `V2__datos_iniciales.sql`, `V3__indices_unicos_maestros.sql`.

- El número es entero y creciente, sin repetirse.
- Una migración aplicada no se edita nunca. Flyway guarda su checksum en `flyway_schema_history` y se niega a
  arrancar si el archivo cambia. Toda corrección es una migración nueva.
- Una migración no se elimina ni se renumera una vez que se integró a `main`.

### Convención de nombres de restricciones

| Tipo | Patrón | Ejemplo |
|---|---|---|
| Clave primaria | `pk_<tabla>` | `pk_barril` |
| Clave única | `uk_<tabla>_<columnas>` | `uk_lote_identificador_interno` |
| Restricción de dominio (enumeración) | `ck_<tabla>_<columna>` | `ck_barril_estado` |
| Clave foránea | `fk_<tabla>_<columna>` | `fk_barril_fabricante_id` |
| Índice | `idx_<tabla>_<columnas>` | `idx_lote_estado` |
| Índice único de negocio | `udx_<tabla>_<campo>` | `udx_cliente_email` |

Todos los nombres se declaran de forma explícita. Los límites de PostgreSQL (63 caracteres) aplican a cada nombre.

### Agregar una migración al cambiar una entidad

1. Modificar la entidad JPA (campo, tabla, relación o valor de una enumeración).
2. Crear `V<n+1>__descripcion.sql` con el `ALTER TABLE` / `CREATE TABLE` / `CREATE INDEX` correspondiente, usando la
   convención de nombres anterior.
3. Las restricciones `ck_*` de las enumeraciones deben actualizarse cuando se agrega o quita un valor de la
   enumeración (`ALTER TABLE ... DROP CONSTRAINT ...; ALTER TABLE ... ADD CONSTRAINT ...`).
4. Para una columna `not null` nueva sobre una tabla con datos, agregar la columna con un valor por defecto o
   completarla con un `UPDATE` antes de aplicar `SET NOT NULL`.
5. Arrancar la aplicación contra una base vacía y contra una base ya migrada: Flyway aplica la migración e
   Hibernate valida el resultado.

### Regenerar el esquema de referencia

El esquema que Hibernate generaría a partir de las entidades sirve como referencia para comparar contra las
migraciones. Se genera contra una base descartable vacía sin tocar la de desarrollo:

```bash
./mvnw spring-boot:run \
  -Dspring-boot.run.arguments="--server.port=0 \
    --spring.datasource.url=jdbc:postgresql://localhost:5432/maestrocervecero_ref \
    --spring.flyway.enabled=false \
    --spring.jpa.hibernate.ddl-auto=none \
    --spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create \
    --spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=hib_create.sql"
```

El archivo `hib_create.sql` contiene el DDL completo. La aplicación termina con un error al no encontrar tablas
cuando intenta ejecutar `UserDataLoader`; el script ya está escrito en ese punto. Para comparar, se aplica el script
en una base vacía y se comparan columnas (`information_schema.columns`) y restricciones (`pg_constraint`) con las de
una base migrada por Flyway. Solo difieren los nombres de las restricciones.

## Reiniciar la base de desarrollo

Con Flyway la base conserva su historial entre arranques. Para empezar de cero con la base de desarrollo
(`maestrocervecero_dev`), conectado a ella:

Opción 1, vaciar el esquema (conserva la base y sus permisos):

```sql
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
```

Opción 2, recrear la base:

```sql
DROP DATABASE maestrocervecero_dev;
CREATE DATABASE maestrocervecero_dev OWNER "AdminCerveza";
```

Después se arranca la aplicación: Flyway aplica todas las migraciones y `UserDataLoader` crea el usuario
administrador.

Una base con tablas pero sin tabla `flyway_schema_history` (por ejemplo, creada por Hibernate) no se adopta
automáticamente; Flyway se niega a migrarla. Debe reiniciarse con alguna de las opciones anteriores.

## Tests

Todos los tests de la aplicación son unitarios: cada módulo valida su Service y su Controller con dependencias
simuladas, sin levantar Spring contra una base de datos. Se ejecutan con `./mvnw test` y no necesitan PostgreSQL.

El perfil `test` (`../../src/test/resources/application-test.properties`, base `maestrocervecero_test`) queda preparado
para los tests de integración, que se agregan cuando todos los módulos estén implementados. Ninguna ejecución de tests
usa la base de desarrollo (`maestrocervecero_dev`) ni la de producción (`maestrocervecero`).

| Propiedad del perfil `test` | Valor |
|---|---|
| `spring.datasource.url` | `${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/maestrocervecero_test}` |
| `spring.datasource.username` / `password` | `${SPRING_DATASOURCE_USERNAME:AdminCerveza}` / `${SPRING_DATASOURCE_PASSWORD:admin}` |
| `spring.flyway.enabled` | `true` |
| `spring.jpa.hibernate.ddl-auto` | `validate` |
| `spring.jpa.show-sql` | `false` |

La base de tests se crea una vez, vacía; Flyway crea el esquema y los datos iniciales al primer arranque:

```sql
CREATE DATABASE maestrocervecero_test OWNER "AdminCerveza";
```

En CI (`../../.github/workflows/ci.yml`) el servicio `postgres:16` arranca con una base vacía llamada `maestrocervecero` y el
paso de tests define `SPRING_DATASOURCE_URL` apuntando a ella y `SPRING_PROFILES_ACTIVE=test`.

## Unicidad

### Criterio de normalización antes de guardar

El Service normaliza cada dato con `NormalizacionTexto` antes de validar y de guardar, de modo que la comparación de
unicidad y el índice de la base trabajan sobre el mismo valor.

| Dato | Normalización | Se guarda |
|---|---|---|
| Nombres propios, razón social, nombres de insumos y de recetas y demás textos libres (`nombrePropio`) | Recorte de los extremos, colapso de cualquier espacio en blanco (incluidos los no separables) en uno solo, eliminación de caracteres invisibles de ancho cero y forma Unicode NFC | Con las mayúsculas y las tildes escritas |
| Códigos e identificadores (`codigo`): identificación de lote del proveedor y texto buscado del identificador interno | Igual que `nombrePropio` y pasaje a mayúsculas con `Locale.ROOT` | En mayúsculas; la puntuación cuenta |
| Número de serie del barril (`nombrePropio`) | Igual que los textos libres | Con las mayúsculas escritas |
| CUIT (`cuit`) | Eliminación de espacios, guiones, puntos y barras | Solo los 11 dígitos, sin guiones |
| Correo (`correo`) | Eliminación de caracteres invisibles de ancho cero, recorte, colapso de espacios y minúsculas con `Locale.ROOT` | En minúsculas |
| Teléfono (`telefono`) | Igual que `nombrePropio` (recorte, colapso de espacios, eliminación de caracteres invisibles de ancho cero y forma Unicode NFC); vacío equivale a nulo | Solo dígitos, hasta 15; es opcional y no tiene unicidad |
| Nombre de usuario (`username`) | Eliminación de caracteres invisibles de ancho cero, recorte, colapso de espacios y minúsculas con `Locale.ROOT` | En minúsculas; solo letras, números, `.`, `_` y `-` |

Las tildes y la puntuación son caracteres distintos: `Pale Ale` y `Palé Ale` no son el mismo nombre. Dos nombres son
el mismo si son iguales ignorando mayúsculas y espacios repetidos.

La normalización no valida: un valor con caracteres no permitidos se conserva para que lo rechace la validación
(`ValidacionesTexto.validarCuit`, `validarTelefono`, `validarUsername`). El CUIT se valida solo por largo: 11 dígitos.

### Índices únicos

La unicidad entre registros activos se garantiza en dos niveles:

1. El Service consulta antes de guardar y lanza `RecursoDuplicadoException` con un mensaje por cada campo único (una
   consulta y una excepción por campo).
2. Un índice único de la base de datos es la garantía final frente a pedidos simultáneos.

Los índices únicos de negocio se crean con migraciones SQL, con nombre explícito `udx_<tabla>_<campo>`. La unicidad
solo rige entre registros activos, por lo que el índice es parcial; la comparación ignora las mayúsculas:

```sql
CREATE UNIQUE INDEX udx_cliente_email ON cliente (lower(btrim(email))) WHERE estado = 'ACTIVO';
```

Los códigos en mayúsculas no necesitan `lower(...)` en su índice. El número de serie del barril se guarda tal como se
escribió, por lo que su índice compara ignorando las mayúsculas:

```sql
CREATE UNIQUE INDEX udx_barril_numero_serie ON barril (fabricante_id, lower(btrim(numero_serie))) WHERE estado = 'ACTIVO';
```

### Índices únicos de los maestros simples

`V3__indices_unicos_maestros.sql` crea los índices únicos parciales (`WHERE estado = 'ACTIVO'`) de los maestros simples.

| Índice | Tabla | Expresión | Regla |
|---|---|---|---|
| `udx_insumo_nombre` | `insumo` | `(tipo_insumo, lower(btrim(nombre)))` | Nombre único por tipo de insumo: una malta y un lúpulo pueden llamarse igual |
| `udx_equipamiento_identificador_interno` | `equipamiento` | `(identificador_interno)` | Identificador único entre fermentadores, molinos, maceradores y ollas de hervor |
| `udx_pais_nombre` | `pais` | `(lower(btrim(nombre)))` | Nombre único |
| `udx_provincia_nombre` | `provincia` | `(pais_id, lower(btrim(nombre)))` | Nombre único dentro de su país |
| `udx_localidad_nombre` | `localidad` | `(provincia_id, lower(btrim(nombre)))` | Nombre único dentro de su provincia |
| `udx_presentacion_comercial_nombre` | `presentacion_comercial` | `(lower(btrim(nombre)))` | Nombre único |
| `udx_motivo_ajuste_nombre` | `motivo_ajuste` | `(lower(btrim(nombre)))` | Nombre único |
| `udx_costo_directo_adicional_nombre` | `costo_directo_adicional` | `(lower(btrim(nombre)))` | Nombre único |

El código postal de las localidades es un dato informativo y no tiene unicidad. Si las tablas contienen duplicados entre
registros activos, la creación del índice falla y deben resolverse antes de migrar.

`V4__fabricante_barril_solo_nombre.sql` deja al fabricante de barril con `id`, `nombre` y `estado` (más la auditoría) y crea su
índice único parcial:

| Índice | Tabla | Expresión | Regla |
|---|---|---|---|
| `udx_fabricante_barril_nombre` | `fabricante_barril` | `(lower(btrim(nombre)))` | Nombre único entre fabricantes activos |

`V5__indices_unicos_usuario_cliente.sql` crea los índices únicos parciales de usuario y cliente, elimina la restricción
global `uk_usuarios_username` (el nombre de usuario de un usuario dado de baja puede volver a usarse) y deja el teléfono
como dato opcional (la columna `telefono` de `usuarios` y de `cliente` admite nulos). También normaliza los datos
existentes (`lower(btrim(...))` del nombre de usuario y del correo de los usuarios y del correo de los clientes), de modo
que coincidan con lo que guarda el Service y con el nombre de usuario que se normaliza al iniciar sesión.

| Índice | Tabla | Expresión | Regla |
|---|---|---|---|
| `udx_usuarios_username` | `usuarios` | `(lower(btrim(username)))` | Nombre de usuario único entre usuarios activos |
| `udx_cliente_email` | `cliente` | `(lower(btrim(email)))` | Correo electrónico único entre clientes activos |

El teléfono de usuarios y clientes no tiene unicidad, y el nombre de un cliente tampoco. Si las tablas contienen nombres
de usuario (de usuarios) o correos (de clientes) repetidos entre registros activos (ignorando mayúsculas y espacios en los extremos), la creación del
índice falla y deben resolverse antes de migrar.

`V6__indices_unicos_proveedor_receta.sql` crea los índices únicos de los módulos versionados (proveedor y receta), agrega
la columna `padre_activo` a `version_proveedor` y `version_receta`, y deja el teléfono del proveedor como dato opcional (la
columna `telefono` de `version_proveedor` admite nulos). También normaliza los datos existentes (espacios colapsados en la
razón social, el nombre comercial, la dirección y el nombre de la receta; CUIT sin separadores; correo en minúsculas).

Los datos de un proveedor y de una receta viven en su tabla de versiones, y cada modificación inserta una versión nueva
con `es_ultima_version = true` y deja la anterior en `false`. La unicidad rige solo sobre la versión vigente de los
registros activos. El estado (`ACTIVO` o `BAJA`) vive en la tabla padre (`proveedor`, `receta`), y un índice parcial de la
tabla de versiones no puede consultarlo, por eso cada versión guarda `padre_activo`:

- `padre_activo` es `true` mientras el padre está activo (valor por defecto al crear una versión).
- Al dar de baja el padre, el Service pone `padre_activo = false` en todas sus versiones, y su CUIT, correo electrónico o
  nombre quedan libres para un registro nuevo.
- Los índices únicos de negocio llevan `WHERE es_ultima_version = true AND padre_activo = true`.

| Índice | Tabla | Expresión | Regla |
|---|---|---|---|
| `udx_version_proveedor_ultima_version` | `version_proveedor` | `(proveedor_id) WHERE es_ultima_version = true` | Una sola versión vigente por proveedor |
| `udx_version_proveedor_cuit` | `version_proveedor` | `(cuit)` | CUIT único entre proveedores activos (se guarda con sus 11 dígitos) |
| `udx_version_proveedor_email` | `version_proveedor` | `(lower(btrim(email))) WHERE email IS NOT NULL` | Correo electrónico único entre proveedores activos |
| `udx_version_receta_ultima_version` | `version_receta` | `(receta_id) WHERE es_ultima_version = true` | Una sola versión vigente por receta |
| `udx_version_receta_nombre` | `version_receta` | `(lower(btrim(nombre)))` | Nombre único entre recetas activas; los nombres de versiones históricas no cuentan |

Los índices de negocio (los tres con nombre de dato) son parciales por `es_ultima_version = true AND padre_activo = true`;
los de versión vigente solo por `es_ultima_version = true`. La razón social y el teléfono del proveedor no tienen
unicidad, y el código
`R<id>` de la receta tampoco. Si las tablas contienen datos repetidos entre proveedores o recetas activos (sobre su versión
vigente), o un padre con más de una versión vigente, la creación del índice falla y deben resolverse antes de migrar.

`V9__eliminar_unicidad_razon_social_proveedor.sql` elimina el índice `udx_version_proveedor_razon_social` que crea V6.
El CUIT es el identificador único del proveedor: dos proveedores activos pueden tener la misma razón social si su CUIT
es distinto. El CUIT y el correo electrónico son únicos entre las versiones vigentes de los proveedores activos.

### Identificador interno generado y número de serie del barril

`V11__identificador_interno_generado_y_numero_serie_barril.sql` crea una secuencia por tipo para el identificador
interno que genera el sistema al dar de alta, y separa el identificador del barril en número de serie e identificador
interno.

| Secuencia | Prefijo | Identificador generado |
|---|---|---|
| `seq_identificador_mol` | `MOL` | Molino: `MOL-01`, `MOL-02`... |
| `seq_identificador_mac` | `MAC` | Macerador |
| `seq_identificador_oll` | `OLL` | Olla de hervor |
| `seq_identificador_fer` | `FER` | Fermentador |
| `seq_identificador_brl` | `BRL` | Barril |

- El Service pide el número con una consulta nativa (`SELECT nextval(...)`) como último paso del alta, después de todas
  las validaciones y justo antes de guardar. `GeneradorIdentificador` arma el texto: prefijo, guion y el número con dos
  dígitos como mínimo (`MOL-99`, `MOL-100`).
- Las secuencias no son transaccionales: un número nunca se reutiliza, ni siquiera tras una baja o un alta revertida, y
  una alta revertida deja un hueco en la numeración. El identificador interno no se edita.
- Cada secuencia de equipamiento arranca después del mayor sufijo numérico de los identificadores existentes con su
  prefijo (sin distinguir mayúsculas, incluidos los dados de baja), o en 1 si no hay ninguno. Los equipos existentes
  conservan su identificador.
- Barril: la columna `identificador` pasa a `identificador_interno` (`BRL-01`... según el `id` de los barriles
  existentes) y se agrega `numero_serie`, obligatorio, con el valor que tenía el identificador. La secuencia arranca en
  la cantidad de barriles más uno.

| Índice | Tabla | Expresión | Regla |
|---|---|---|---|
| `udx_barril_numero_serie` | `barril` | `(fabricante_id, lower(btrim(numero_serie))) WHERE estado = 'ACTIVO'` | Número de serie único dentro de su fabricante entre los barriles activos, sin distinguir mayúsculas |
| `udx_barril_identificador_interno` | `barril` | `(identificador_interno)` | Identificador interno único entre todos los barriles |

`udx_equipamiento_identificador_interno` (V3) se mantiene sin cambios.

### Correo de usuario sin unicidad

`V12__eliminar_unicidad_correo_usuario.sql` elimina el índice `udx_usuarios_correo` que crea V5. Una misma persona puede
tener varios usuarios (por ejemplo, con distintos roles) y compartir el correo electrónico: el correo de un usuario solo
se valida por formato y longitud. El nombre de usuario (`udx_usuarios_username`) sigue siendo único entre usuarios activos.

**Orden de los listados.** Las consultas de listado de equipos y de barriles ordenan por identificador interno en orden
natural, con `regexp_replace` de PostgreSQL dentro del `ORDER BY` de la consulta: primero el prefijo (sin distinguir
mayúsculas), luego la cantidad de dígitos del número final, luego esos dígitos y, como desempate, el texto completo y el `id`.
Así `MOL-2` va antes que `MOL-11` y `MOL-11` antes que `MOL-100`. Un identificador heredado que no termina en número
(por ejemplo `Olla Grande`) se ordena por su texto entre los que comparten ese prefijo. El orden del `Pageable` que
pasa el Controller solo desempata.

**Una sola versión vigente.** Dos protecciones se combinan al modificar un proveedor o una receta:

1. El Service bloquea la fila del padre (`PESSIMISTIC_WRITE`) antes de validar y de guardar, de modo que las
   modificaciones simultáneas del mismo padre se ejecutan una a una. La baja bloquea la misma fila.
2. Hibernate ejecuta los `INSERT` antes que los `UPDATE` al confirmar los cambios, y el índice de versión vigente se
   evalúa en cada sentencia. Por eso el Service guarda y confirma (`save` y `flush`) la versión anterior ya desactivada
   antes de insertar la nueva.

`V7__indices_unicos_reglas_nuevas.sql` crea los índices únicos que garantizan, en la base de datos, reglas que el Service
ya valida antes de guardar. Se aplican a todas las filas.

| Índice | Tabla | Expresión | Regla |
|---|---|---|---|
| `udx_catalogo_proveedor_item` | `catalogo_proveedor` | `(version_proveedor_id, insumo_id, presentacion_comercial_id)` | Un catálogo no repite el mismo insumo con la misma presentación comercial |
| `udx_detalle_compra_item` | `detalle_compra` | `(orden_compra_id, catalogo_proveedor_id)` | Una orden de compra no repite el mismo ítem del catálogo |
| `udx_detalle_malta_version_malta` | `detalle_malta` | `(version_receta_id, malta_id)` | Una versión de receta no repite la misma malta |
| `udx_detalle_lupulo_version_lupulo_uso_tiempo` | `detalle_lupulo` | `(version_receta_id, lupulo_id, uso, tiempo_de_hervor)` | Una versión de receta no repite un lúpulo con el mismo uso y el mismo tiempo de hervor (se guarda en 0 si el uso no es `HERVOR`) |
| `udx_detalle_levadura_version_levadura` | `detalle_levadura` | `(version_receta_id, levadura_id)` | Una versión de receta no repite la misma levadura |

Si las tablas ya tuvieran filas repetidas, la creación de un índice falla y deben resolverse antes de migrar.

La operación de «buscar o crear» se serializa con un bloqueo `PESSIMISTIC_WRITE` sobre la fila
que la ordena, antes de validar:

- Ingreso de insumo: bloquea la fila del insumo y luego busca el lote por `(insumo, identificación de lote de proveedor)`.
  La identificación se normaliza como código (mayúsculas, sin espacios de más; la puntuación cuenta). La restricción
  `uk_lote_insumo_insumo_id_identificacion_lote_proveedor` es la garantía final.

El identificador interno del lote de producción (por ejemplo `LOT-R7-03`, con el número de lote de la receta en dos dígitos como mínimo) es único por la restricción
`uk_lote_identificador_interno`.

### Escalado del lote

`V8__escalado_lote.sql` crea las tablas que guardan la instantánea del escalado de un lote, tomada al registrarlo:

| Tabla | Contenido |
|---|---|
| `escalado_lote` | Los parámetros de los cuatro equipos del lote: rendimiento del molino; eficiencia de maceración, espacio muerto y capacidad útil del macerador; pérdida por trub, evaporación y capacidad útil de la olla de hervor; capacidad útil del fermentador |
| `escalado_lote_insumo` | Una fila por insumo de la receta del lote, con el extracto potencial (malta), los ácidos alfa y el formato (lúpulo), o el tipo y las células por gramo (levadura). Es única por `(escalado_lote_id, insumo_id)` |

`lote.escalado_lote_id` (única, nula solo en los lotes registrados sin instantánea) referencia la fila de `escalado_lote`. Un lote sin
instantánea calcula con los valores vigentes de los equipos y los insumos.

### Estado En uso del barril, fecha del envasado y litros descartados

`V18__estado_en_uso_barril_fecha_envasado_y_litros_descartados.sql`:

| Tabla y columna | Cambio |
|---|---|
| `barril.estado_operativo` | La restricción `ck_barril_estado_operativo` admite `CON_CERVEZA`, `DESPACHADO`, `DISPONIBLE`, `EN_LIMPIEZA`, `EN_MANTENIMIENTO` y `EN_USO` (el barril está siendo llenado por un lote en ejecución) |
| `envasado_lote.fecha_envasado` | `timestamp(6)` obligatoria: la fecha y hora del envasado, que es la fecha de participación del barril en el lote |
| `devolucion_barril.litros_descartados` | `double precision` obligatoria, con valor por defecto 0: los litros que tenía el barril al volver y que se descartaron al registrar la devolución |

Con los datos existentes, `fecha_envasado` toma la fecha de registro del envasado (`created_date`) y, si falta, el inicio de su etapa o la última modificación del registro; `litros_descartados` queda en 0; y los barriles `CON_CERVEZA` que conservan el contenido de un envasado registrado de un lote en ejecución, sin operaciones posteriores, pasan a `EN_USO`.

### Índice por motivo de ajuste

`V19__indice_ajuste_insumo_motivo_ajuste.sql`:

| Tabla y columna | Cambio |
|---|---|
| `ajuste_insumo.motivo_ajuste_id` | Índice `idx_ajuste_insumo_motivo_ajuste_id`: acelera saber si un motivo de ajuste tiene ajustes de insumo asociados, consulta que fija su tipo y impide su baja |

### Traducción de una violación de unicidad

Los Services guardan con `GuardadoUnico.guardar(repositorio, entidad, excepcionesPorIndice)`, que ejecuta
`save` y `flush` dentro del método y traduce la violación del índice a la excepción de negocio asociada a su nombre
(el mismo mensaje por campo que da la consulta previa):

```java
GuardadoUnico.guardar(clienteRepository, cliente, Map.of(
        "udx_cliente_email", () -> new RecursoDuplicadoException("Ya existe un cliente con el correo electrónico '" + email + "'")));
```

- Si el índice violado no figura en el mapa, lanza `RecursoDuplicadoException("Ya existe un registro con los mismos datos")`.
- Una violación que no es de unicidad (clave foránea, columna obligatoria nula) se propaga como
  `DataIntegrityViolationException`; `ControllerAdvices` la muestra con el mensaje "No se pudo guardar porque los datos
  entran en conflicto con otro registro. Revise la información e intente nuevamente".

**Nombre de la restricción con PostgreSQL.** Hibernate 7.4 informa el SQLSTATE `23505` y la clase
`ConstraintViolationException` con `getKind() == UNIQUE`, pero `getConstraintName()` devuelve `null`. El nombre del
índice está en el campo `constraint` del mensaje de error del servidor
(`PSQLException.getServerErrorMessage().getConstraint()`), que no depende del idioma de la base de datos como sí
depende el texto del mensaje (con un servidor en español el texto dice `llave duplicada viola restricción de
unicidad «udx_usuarios_username»`). La misma lectura sirve para restricciones (`uk_lote_identificador_interno`) y para índices
únicos parciales o por expresión (`udx_...`). Por eso el driver `postgresql` se declara con alcance de compilación en
el `../../pom.xml`.

### Enlaces para restablecer la contraseña

`V27__token_restablecimiento_contrasena.sql` crea la tabla de los enlaces de un solo uso que recibe por correo el usuario que olvidó su contraseña (`TokenRestablecimientoContrasenaEntity`):

| Tabla y columna | Contenido |
|---|---|
| `token_restablecimiento_contrasena.usuario_id` | Usuario cuya contraseña se restablece (`fk_token_restablecimiento_contrasena_usuario_id`) |
| `token_restablecimiento_contrasena.token_hash` | Hash SHA-256 del token, en hexadecimal (`varchar(64)`, único por `uk_token_restablecimiento_contrasena_token_hash`). El token en sí viaja solo en el enlace del correo y nunca se guarda |
| `token_restablecimiento_contrasena.fecha_expiracion` | Momento a partir del cual el enlace deja de servir (30 minutos después de enviarlo). Al enviar un enlace nuevo o restablecer la contraseña, los enlaces sin usar del usuario pasan a vencer en ese momento |
| `token_restablecimiento_contrasena.fecha_uso` | Momento en que se restableció la contraseña con el enlace; `NULL` mientras no se usó |
| `token_restablecimiento_contrasena.created_date` | Momento en que se envió el enlace. El índice `idx_token_restablecimiento_contrasena_usuario_id_created_date` acelera saber si al usuario se le envió un enlace en los últimos 2 minutos, en cuyo caso no se le envía otro |

La restricción `ck_audit_log_accion` admite además `SOLICITUD_RESTABLECIMIENTO_CONTRASENA` y `RESTABLECIMIENTO_CONTRASENA`, que la bitácora registra con el concepto `SESION`.

### Excedente máximo de un plan de producción

`V29__excedente_maximo_plan_produccion.sql` agrega a la configuración de producción el excedente que admite un plan de producción al confirmarlo (`ConfiguracionProduccionEntity.excedenteMaximoLitros`):

| Tabla y columna | Contenido |
|---|---|
| `configuracion_produccion.excedente_maximo_litros` | Litros que la suma de los lotes de un plan de producción puede superar a la cantidad a producir de su planificación (`double precision not null`, por defecto `300`). La restricción `ck_configuracion_produccion_excedente_maximo_litros` exige un valor mayor o igual a 0; el service además lo limita a 100.000 litros |

Los planes de producción se calculan al momento y no tienen tabla propia: confirmar un plan registra sus lotes en `lote` como cualquier otro registro de lote.

### Generación automática de planes de órdenes de compra

`V30__generacion_automatica_planes_compra.sql` crea las tablas del proceso que, al registrar una planificación de producción con insumos faltantes, genera una solicitud de cotización, pide al gerente de compras que confirme el envío a los proveedores y, al vencer el plazo, genera su plan de órdenes de compra:

| Tabla y columna | Contenido |
|---|---|
| `configuracion_compras` | Fila única (`id = 1`, `ck_configuracion_compras_id`) con el plazo de respuesta en horas (por defecto 48), el criterio con el que se arma el plan (`MENOR_PRECIO`, `MAYOR_CANTIDAD` o `PONDERADO`, por defecto `PONDERADO`) y el peso del precio del criterio ponderado (0 a 100, por defecto 50). La edita el gerente de compras (ver `V34`) |
| `solicitud_cotizacion` | Solicitud de una planificación (`fk_solicitud_cotizacion_planificacion_produccion_id`) con su estado (`PENDIENTE_DE_CONFIRMACION`, `ABIERTA`, `CERRADA`, `PLAN_GENERADO` o `CANCELADA`, ver `V34`); la fecha de confirmación del envío y la fecha límite de respuesta (las dos nulas mientras está pendiente de confirmación: el plazo corre desde la confirmación); las fechas de cierre, de generación del plan (`fecha_generacion_plan`) y de cancelación con su motivo; y los intentos fallidos del cierre programado (`intentos_cierre`, `falla_cierre` y `motivo_falla_cierre`: al tercer intento se marca la falla y se deja de reintentar). El índice parcial `udx_solicitud_cotizacion_planificacion_vigente` admite una sola solicitud no cancelada por planificación, y `idx_solicitud_cotizacion_estado_fecha_limite_respuesta` acelera el cierre de las abiertas vencidas |
| `linea_solicitud_cotizacion` | Insumo faltante con el cálculo que lo originó: requerido, disponible sin reservar, comprometido en lotes registrados sin iniciar (`cantidad_comprometida_lotes`), pendiente de recibir y faltante (mayor a 0), en la unidad del insumo, y si ningún proveedor lo ofrecía. Un insumo por solicitud (`uk_linea_solicitud_cotizacion_solicitud_insumo`) |
| `invitacion_cotizacion` | Proveedor candidato de una solicitud (uno por proveedor, `uk_invitacion_cotizacion_solicitud_proveedor`) y su enlace: hash SHA-256 del token (`varchar(64)`, único por `uk_invitacion_cotizacion_token_hash`, nulo hasta el primer envío; el token viaja solo en el correo y cada intento de envío genera uno nuevo), vencimiento (la fecha límite, nulo mientras la solicitud está pendiente de confirmación), resultado del envío del correo (`estado_envio`, ver `V31`), intentos de envío (hasta 3) y fecha de la última respuesta. El proveedor no indica fecha de entrega: la indica el gerente de compras en la orden de compra (ver `V36`) |
| `oferta_cotizacion` | Oferta del proveedor para una línea en una presentación: ítem de su catálogo (insumo y presentación), precio de cada unidad de la presentación (`numeric(14,4)`, mayor a 0; nulo si no tiene stock, ver `V33`) y unidades disponibles (mayor a 0, o 0 si no tiene stock). Un proveedor puede ofrecer un mismo insumo en varias presentaciones: una oferta por invitación, línea y presentación (`uk_oferta_cotizacion_invitacion_linea_catalogo`, ver `V32`) |
| `plan_compra` | Plan generado: uno por solicitud (`uk_plan_compra_solicitud_cotizacion_id`), con su estado (`PROPUESTO`, `ELEGIDO` o `CANCELADO`, ver `V34`) |
| `orden_compra_propuesta` | Orden que un plan propone a un proveedor, con su estado (`PROPUESTA`, `CONFIRMADA` o `DESCARTADA`) y, una vez confirmada, la orden de compra definitiva (`orden_compra_id`, única) |
| `item_orden_compra_propuesta` | Unidades de un ítem del catálogo al precio ofrecido, para una línea de la solicitud |

La restricción `ck_audit_log_concepto_auditoria` admite además `CONFIGURACION_COMPRAS`, con el que la bitácora registra los cambios de la configuración de compras.

La pantalla pública de cotización (`/cotizacion/{token}`) guarda el token CSRF en una cookie propia con el atributo `Secure` cuando el pedido llega por HTTPS. Detrás de un proxy que termina el HTTPS, la aplicación debe confiar en sus cabeceras `X-Forwarded-*` (`server.forward-headers-strategy`) para reconocerlo.

### Estados del envío de las invitaciones a cotizar

`V31__estados_envio_invitacion_cotizacion.sql` redefine `ck_invitacion_cotizacion_estado_envio` con los estados del envío del correo de cada invitación:

| Estado | Significado |
|---|---|
| `PENDIENTE` | Todavía no se intentó enviar |
| `EN_ENVIO` | El token del intento ya está guardado y el correo se está enviando fuera de toda transacción; no se reintenta, porque el correo pudo haber salido con ese enlace |
| `ENVIADA` | El servidor de correo aceptó el correo |
| `FALLIDA` | Falló el último intento; el proceso programado lo reintenta hasta 3 intentos |
| `OMITIDA` | El proveedor se dio de baja o no tiene una versión vigente: no corresponde enviarlo (estado final) |

### Ofertas por presentación y fecha estimada de entrega

`V32__ofertas_por_presentacion_y_fecha_entrega_cotizacion.sql`:

- Reemplaza `uk_oferta_cotizacion_invitacion_linea` por `uk_oferta_cotizacion_invitacion_linea_catalogo` (`invitacion_cotizacion_id`, `linea_solicitud_cotizacion_id`, `catalogo_proveedor_id`): un proveedor puede ofrecer un mismo insumo en varias presentaciones de su catálogo a la vez (por ejemplo, 2 bolsas de 25 kg y 3 bolsas de 10 kg). Cada presentación es una oferta aparte, el plan las compara por precio por unidad de medida del insumo y la orden propuesta al proveedor lleva un ítem por presentación.
- Agrega `invitacion_cotizacion.fecha_entrega_estimada` (`date`, nula), que `V36` elimina.

### Ofertas sin stock

`V33__ofertas_sin_stock_cotizacion.sql` permite que el proveedor indique cantidad disponible 0 en una presentación para decir explícitamente que no tiene stock: `ck_oferta_cotizacion_cantidad_disponible` pasa a admitir 0, `precio_unitario` admite nulos y `ck_oferta_cotizacion_precio_unitario` exige precio mayor a 0 cuando la cantidad es mayor a 0 y precio nulo cuando la cantidad es 0. El plan no usa las ofertas sin stock y una respuesta solo con presentaciones sin stock cuenta como respuesta sin ofertas.

### Un solo plan de órdenes de compra por solicitud

`V34__plan_de_compra_unico_por_solicitud.sql` deja un único plan de órdenes de compra por solicitud de cotización: el mejor según el criterio y el peso del precio de la configuración de compras.

| Tabla y columna | Contenido |
|---|---|
| `configuracion_compras` | Plazo de respuesta, criterio y peso del precio; sin `cantidad_planes` ni `ck_configuracion_compras_cantidad_planes` |
| `plan_compra` | Sin `numero`; `uk_plan_compra_solicitud_cotizacion_id` admite un solo plan por solicitud y `ck_plan_compra_estado` admite `PROPUESTO` (al generarse), `ELEGIDO` (al confirmar la primera orden del plan; desde ese momento la solicitud no se puede cancelar) y `CANCELADO` (solicitud cancelada o planificación anulada) |
| `solicitud_cotizacion.estado` | `ck_solicitud_cotizacion_estado` admite `PENDIENTE_DE_CONFIRMACION`, `ABIERTA`, `CERRADA`, `PLAN_GENERADO` y `CANCELADA`; las solicitudes con estado `PLANES_GENERADOS` pasan a `PLAN_GENERADO` |
| `solicitud_cotizacion.fecha_generacion_plan` | Momento en que se generó el plan; la migración renombra `fecha_generacion_planes` a `fecha_generacion_plan` |

Datos existentes: de cada solicitud se conserva un solo plan, el `ELEGIDO` si lo hay; si no, el que tiene alguna orden propuesta vinculada a una orden de compra; y si no, el de menor número (el mejor según el criterio). Los demás planes de la solicitud (los `DESCARTADO` y los alternativos sin elegir) se borran junto con sus órdenes propuestas y sus ítems. Ninguno de ellos tiene órdenes de compra confirmadas, porque confirmar una orden propuesta elige su plan, así que no se pierde ningún vínculo con `orden_compra`.

### Fecha estimada de entrega en la orden de compra

`V36__quitar_fecha_entrega_estimada_cotizacion.sql` elimina `invitacion_cotizacion.fecha_entrega_estimada`: el proveedor cotiza solo el precio y la cantidad de cada presentación. La fecha estimada de entrega la indica el gerente de compras en el formulario de la orden de compra que se abre prearmado desde la orden propuesta del plan; llega vacía y es obligatoria, con las reglas del alta de órdenes de compra.

## Auditoría y transacciones

`AuditAspect` registra la bitácora solo después de que la operación se confirma:

- Tiene el orden explícito `AuditAspect.ORDEN` (`Ordered.LOWEST_PRECEDENCE - 100`), que lo ubica por fuera del
  interceptor transaccional del Service. El commit ocurre dentro de la ejecución del método auditado, y una falla al
  confirmar (por ejemplo, un índice único) se propaga antes de auditar.
- Si el método auditado se ejecuta dentro de una transacción externa que sigue abierta, el registro se difiere a
  `TransactionSynchronization.afterCommit`; si esa transacción se revierte, no se registra.

## Detalle de estados de la bitácora

`V22__detalle_estados_auditoria.sql` agrega a `audit_log` las columnas `estado_anterior` y `estado_posterior` (`text`, aceptan `NULL`). Guardan, en JSON, el estado del registro afectado antes y después de la operación. Las entradas de sesión y las operaciones que no modifican el registro quedan con las dos columnas en `NULL`.

**Captura.** `AuditAspect` abre una captura (`RegistroEstadosEntidad`, por hilo) antes de ejecutar el método auditado. `CapturaEstadoInterceptor`, el interceptor de Hibernate que registra `AuditConfig`, entrega a esa captura el estado anterior y el posterior de cada entidad que se inserta, actualiza o elimina en el flush del commit; Hibernate conoce ambos en `onFlushDirty` (estado actual y estado cargado de la base). Con varios cambios de la misma entidad en una operación se conserva el anterior del primero y el posterior del último. Fuera de un método auditado no se captura nada.

**Qué entrada recibe cada estado.** La entrada toma los estados de la entidad cuyo tipo corresponde al concepto de la entrada (el nombre de la clase sin `Entity` es igual al del concepto sin guiones bajos ni la palabra `DE`; `OLLA_DE_HERVOR` corresponde a `OllaHervorEntity`) y cuyo id es el `entidadId` de la entrada. Los cambios de otras entidades de la misma operación (por ejemplo, el barril que modifica un despacho) no se atribuyen a la entrada.

| Operación | Estado anterior | Estado posterior |
| --- | --- | --- |
| Alta | `NULL` | Registro creado |
| Modificación, finalización, anulación y baja lógica (que es una modificación del estado) | Registro antes del cambio | Registro después del cambio |
| Eliminación física | Registro eliminado | `NULL` |

**Forma del JSON.** Un objeto plano con el `id` primero y luego los campos persistentes de la entidad, en el orden de sus propiedades. Los enteros y decimales (incluido `BigDecimal`) son números JSON, los booleanos son booleanos, los enumerados llevan su nombre, las fechas y horas van en ISO-8601 y los valores sin dato van como `null`. Una asociación se reemplaza por el id de la entidad asociada bajo el nombre del campo seguido de `Id` (`fabricanteId`). No se recorren colecciones ni grafos, y nunca se inicializa un proxy perezoso.

```json
{"id":12,"createdBy":"admin","createdDate":"2026-11-13T10:30:15","estado":"ACTIVO","fabricanteId":3,"capacidad":50.0,"nombre":"Barril A"}
```

**Datos secretos y personales.** La bitácora no copia credenciales ni datos personales de contacto. Se omiten los campos marcados con `@ExcluidoDeAuditoria` y todo campo cuyo nombre contenga `password`, `contrasena`, `contraseña`, `secret`, `token`, `hash`, `clave`, `pin`, `apikey`, `credencial` u `otp`, sin distinguir mayúsculas (`SerializadorEstadoEntidad.FRAGMENTOS_SENSIBLES`). Una prueba recorre todas las entidades y falla si alguna tiene un campo de nombre sensible sin la anotación.

| Entidad | Campos excluidos | Motivo |
| --- | --- | --- |
| `UsuarioEntity` | `password` | Credencial (hash BCrypt) |
| `UsuarioEntity` | `correo`, `telefono` | Datos personales de contacto |
| `ClienteEntity` | `telefono`, `email`, `direccion` | Datos personales de contacto |
| `VersionProveedorEntity` | `telefono`, `email`, `direccion` | Datos de contacto |

Los datos de negocio (CUIT, razón social, nombres, localidad) sí se guardan: identifican qué registro cambió. Un cambio en un dato de contacto genera igualmente la entrada de la bitácora, pero su detalle no muestra el campo excluido. Un campo personal nuevo de una entidad se anota con `@ExcluidoDeAuditoria`.

**Acotación de la captura.** La captura retiene solo las entidades cuyo tipo corresponde a un concepto de los métodos auditados en curso, de modo que una operación que modifica muchas entidades de otros tipos no acumula sus estados en memoria.

**Tolerancia a fallas.** La bitácora nunca interrumpe la operación de negocio: una falla al despachar la entrada (por ejemplo, la cola asíncrona llena) se registra en el log sin propagarse; si no se puede persistir una entrada con estados, se reintenta una vez sin ellos para conservar la entrada. Los logs de estas fallas informan solo el tipo de la excepción, nunca su mensaje, porque puede citar valores de los estados.

**Alcance.** Se capturan los cambios que pasan por la sesión de Hibernate; las actualizaciones masivas por consulta (`@Modifying`) no pasan por el interceptor y no generan estados. El estado se guarda como se leyó de la entidad: los cambios posteriores del registro no reescriben entradas viejas.

## Política de datos iniciales

| Tipo de dato | Dónde vive | Motivo |
|---|---|---|
| Datos de sistema determinísticos, iguales en todo ambiente (configuración singleton de producción) | Migración Flyway (`V2__datos_iniciales.sql`) | Se versionan junto al esquema y se aplican una sola vez, en orden |
| Datos que requieren lógica de la aplicación (usuario administrador, cuya contraseña se cifra con `PasswordEncoder`) | `DataLoader` (`UserDataLoader`) | El hash no puede calcularse de forma fiable en SQL; el `DataLoader` crea el usuario `admin` con la contraseña `admin` solo si no existe un usuario activo con ese nombre de usuario. La contraseña inicial debe cambiarse en el primer ingreso |
| Datos de prueba o de demostración | Fuera de las migraciones | Las migraciones se ejecutan en todos los ambientes, incluida producción |

Cambiar un valor por defecto de un dato inicial ya aplicado se hace con una migración nueva (`UPDATE`), nunca
editando `V2`.
