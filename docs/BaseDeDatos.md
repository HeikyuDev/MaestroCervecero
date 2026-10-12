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
`V1__esquema_inicial.sql`, `V2__datos_iniciales.sql`, `V3__indices_unicos_de_negocio.sql`.

- El número es entero y creciente, sin repetirse.
- Una migración aplicada no se edita nunca. Flyway guarda su checksum en `flyway_schema_history` y se niega a
  arrancar si el archivo cambia. Toda corrección es una migración nueva.
- Una migración no se elimina ni se renumera una vez que se integró a `main`.

### Convención de nombres de restricciones

| Tipo | Patrón | Ejemplo |
|---|---|---|
| Clave primaria | `pk_<tabla>` | `pk_barril` |
| Clave única | `uk_<tabla>_<columnas>` | `uk_usuarios_username` |
| Restricción de dominio (enumeración) | `ck_<tabla>_<columna>` | `ck_barril_estado` |
| Clave foránea | `fk_<tabla>_<columna>` | `fk_barril_fabricante_id` |
| Índice | `idx_<tabla>_<columnas>` | `idx_lote_estado` |

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

## Ejecutar los tests contra una base descartable

`MaestroCerveceroApplicationTests.contextLoads` levanta el contexto completo y usa la base configurada en
`spring.datasource.url`. Para no tocar la base de desarrollo se apunta la ejecución a una base propia:

```bash
# Crear la base (una vez)
psql -U AdminCerveza -h localhost -d postgres -c "CREATE DATABASE maestrocervecero_flyway_check"

# Ejecutar la suite completa contra ella
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/maestrocervecero_flyway_check ./mvnw test

# Eliminarla al terminar
psql -U AdminCerveza -h localhost -d postgres -c "DROP DATABASE maestrocervecero_flyway_check"
```

En PowerShell: `$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/maestrocervecero_flyway_check"; ./mvnw test`.

Una base descartable vacía comprueba además que todas las migraciones se aplican de principio a fin.

En CI (`.github/workflows/ci.yml`) el servicio `postgres:16` arranca con una base vacía llamada `maestrocervecero`:
Flyway crea el esquema y los datos iniciales en cada ejecución.

## Política de datos iniciales

| Tipo de dato | Dónde vive | Motivo |
|---|---|---|
| Datos de sistema determinísticos, iguales en todo ambiente (configuración singleton de producción) | Migración Flyway (`V2__datos_iniciales.sql`) | Se versionan junto al esquema y se aplican una sola vez, en orden |
| Datos que requieren lógica de la aplicación (usuario administrador, cuya contraseña se cifra con `PasswordEncoder`) | `DataLoader` idempotente (`UserDataLoader`) | El hash no puede calcularse de forma fiable en SQL; el `DataLoader` verifica la existencia antes de insertar |
| Datos de prueba o de demostración | Fuera de las migraciones | Las migraciones se ejecutan en todos los ambientes, incluida producción |

Cambiar un valor por defecto de un dato inicial ya aplicado se hace con una migración nueva (`UPDATE`), nunca
editando `V2`.
