# Base de datos

## Arquitectura

| Responsabilidad | Herramienta |
|---|---|
| Crear y modificar el esquema (tablas, columnas, restricciones, índices) | Flyway, con migraciones SQL en `src/main/resources/db/migration` |
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

Con Flyway la base conserva su historial entre arranques. Para empezar de cero:

Opción 1, vaciar el esquema (conserva la base y sus permisos):

```sql
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
```

Opción 2, recrear la base:

```sql
DROP DATABASE maestrocervecero;
CREATE DATABASE maestrocervecero OWNER "AdminCerveza";
```

Después se arranca la aplicación: Flyway aplica todas las migraciones y `UserDataLoader` crea el usuario
administrador.

Una base con tablas pero sin tabla `flyway_schema_history` (por ejemplo, creada por Hibernate) no se adopta
automáticamente; Flyway se niega a migrarla. Debe reiniciarse con alguna de las opciones anteriores.

## Tests de integración

Los tests que levantan Spring contra una base real usan el perfil `test`
(`src/test/resources/application-test.properties`) y una base propia, `maestrocervecero_test`. Ningún test usa la
base de desarrollo `maestrocervecero`.

| Propiedad | Valor |
|---|---|
| `spring.datasource.url` | `${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/maestrocervecero_test}` |
| `spring.datasource.username` / `password` | `${SPRING_DATASOURCE_USERNAME:AdminCerveza}` / `${SPRING_DATASOURCE_PASSWORD:admin}` |
| `spring.flyway.enabled` | `true` |
| `spring.jpa.hibernate.ddl-auto` | `validate` |
| `spring.jpa.show-sql` | `false` |

### Preparar la base de tests

Se crea una vez, vacía. Flyway crea el esquema y los datos iniciales al primer arranque:

```sql
CREATE DATABASE maestrocervecero_test OWNER "AdminCerveza";
```

### Ejecutar

| Qué | Comando |
|---|---|
| Toda la suite (unitarios e integración) | `./mvnw test` |
| Solo los de integración | `./mvnw test -Dgroups=integracion` |
| Solo los unitarios (no necesitan base de datos) | `./mvnw test -DexcludedGroups=integracion` |

Los tests de integración llevan la etiqueta `@Tag("integracion")`, heredada de `IntegracionBaseTest`. Para apuntar la
ejecución a otra base se define la URL en la variable de entorno `SPRING_DATASOURCE_URL`. En PowerShell:
`$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/otra_base"; ./mvnw test`. Una base vacía comprueba además
que todas las migraciones se aplican de principio a fin.

En CI (`.github/workflows/ci.yml`) el servicio `postgres:16` arranca con una base vacía llamada `maestrocervecero` y el
paso de tests define `SPRING_DATASOURCE_URL` apuntando a ella: Flyway crea el esquema y los datos iniciales en cada
ejecución.

### Clase base y limpieza de datos

Todo test de integración extiende `IntegracionBaseTest` (`@SpringBootTest`, `@ActiveProfiles("test")`,
`@Tag("integracion")`). Antes de cada test la clase base vacía las tablas de negocio con
`TRUNCATE ... RESTART IDENTITY CASCADE` y elimina los usuarios distintos de `admin`. No se vacían:

- `flyway_schema_history`: historial de migraciones.
- `configuracion_produccion`: fila singleton cargada por `V2`.
- `usuarios`: se conserva el administrador que crea `UserDataLoader` al arrancar el contexto.

Se usa `TRUNCATE` y no una transacción con rollback por test porque estos tests necesitan que las transacciones se
confirmen: verifican restricciones que PostgreSQL evalúa al confirmar y el registro de auditoría, que se guarda de
forma asíncrona en otra transacción y no vería datos sin confirmar.

## Unicidad

### Criterio de normalización antes de guardar

El Service normaliza cada dato con `NormalizacionTexto` antes de validar y de guardar, de modo que la comparación de
unicidad y el índice de la base trabajan sobre el mismo valor.

| Dato | Normalización | Se guarda |
|---|---|---|
| Nombres propios, razón social, nombres de insumos y demás textos libres (`nombrePropio`) | Recorte de los extremos, colapso de cualquier espacio en blanco (incluidos los no separables) en uno solo, eliminación de caracteres invisibles de ancho cero y forma Unicode NFC | Con las mayúsculas y las tildes escritas |
| Códigos e identificadores (`codigo`): identificador interno de equipos, identificador de barril, identificación de lote del proveedor | Igual que `nombrePropio` y pasaje a mayúsculas con `Locale.ROOT` | En mayúsculas; la puntuación cuenta |
| CUIT (`cuit`) | Eliminación de espacios, guiones, puntos y barras | Solo los 11 dígitos, sin guiones |
| Correo (`correo`) | Recorte y minúsculas | En minúsculas |
| Teléfono (`telefono`) | Recorte; vacío equivale a nulo | Solo dígitos, hasta 15; es opcional y no tiene unicidad |
| Nombre de usuario (`username`) | Recorte y minúsculas | En minúsculas; solo letras, números, `.`, `_` y `-` |

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

Los códigos e identificadores se guardan en mayúsculas, por lo que su índice no necesita `lower(...)`:

```sql
CREATE UNIQUE INDEX udx_barril_identificador ON barril (identificador) WHERE estado = 'ACTIVO';
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
| `udx_barril_identificador` | `barril` | `(fabricante_id, identificador)` | Identificador único dentro de su fabricante |

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
| `udx_usuarios_correo` | `usuarios` | `(lower(btrim(correo)))` | Correo electrónico único entre usuarios activos |
| `udx_cliente_email` | `cliente` | `(lower(btrim(email)))` | Correo electrónico único entre clientes activos |

El teléfono de usuarios y clientes no tiene unicidad, y el nombre de un cliente tampoco. Si las tablas contienen nombres
de usuario o correos repetidos entre registros activos (ignorando mayúsculas y espacios en los extremos), la creación del
índice falla y deben resolverse antes de migrar.

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
el `pom.xml`.

## Auditoría y transacciones

`AuditAspect` registra la bitácora solo después de que la operación se confirma:

- Tiene el orden explícito `AuditAspect.ORDEN` (`Ordered.LOWEST_PRECEDENCE - 100`), que lo ubica por fuera del
  interceptor transaccional del Service. El commit ocurre dentro de la ejecución del método auditado, y una falla al
  confirmar (por ejemplo, un índice único) se propaga antes de auditar.
- Si el método auditado se ejecuta dentro de una transacción externa que sigue abierta, el registro se difiere a
  `TransactionSynchronization.afterCommit`; si esa transacción se revierte, no se registra.

## Política de datos iniciales

| Tipo de dato | Dónde vive | Motivo |
|---|---|---|
| Datos de sistema determinísticos, iguales en todo ambiente (configuración singleton de producción) | Migración Flyway (`V2__datos_iniciales.sql`) | Se versionan junto al esquema y se aplican una sola vez, en orden |
| Datos que requieren lógica de la aplicación (usuario administrador, cuya contraseña se cifra con `PasswordEncoder`) | `DataLoader` idempotente (`UserDataLoader`) | El hash no puede calcularse de forma fiable en SQL; el `DataLoader` verifica la existencia antes de insertar |
| Datos de prueba o de demostración | Fuera de las migraciones | Las migraciones se ejecutan en todos los ambientes, incluida producción |

Cambiar un valor por defecto de un dato inicial ya aplicado se hace con una migración nueva (`UPDATE`), nunca
editando `V2`.
