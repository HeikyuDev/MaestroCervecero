-- =============================================================================
-- V12 - Correo de usuario sin unicidad
-- =============================================================================
-- Una misma persona puede tener varios usuarios (por ejemplo, con distintos
-- roles) y compartir el mismo correo electronico. Se elimina el indice unico
-- del correo de usuarios.
--
-- El nombre de usuario (udx_usuarios_username) sigue siendo unico entre
-- usuarios activos.
-- =============================================================================

DROP INDEX IF EXISTS udx_usuarios_correo;
