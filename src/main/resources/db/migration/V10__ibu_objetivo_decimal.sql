-- =============================================================================
-- V10 - IBU objetivo con decimales
-- =============================================================================
-- El IBU objetivo de una version de receta admite un decimal (por ejemplo
-- 35,5), como el porcentaje de acidos alfa del lupulo. La columna
-- version_receta.ibu_objetivo pasa de integer a double precision y sigue siendo
-- obligatoria. Los valores existentes se conservan.
-- =============================================================================

alter table version_receta
    alter column ibu_objetivo type double precision;
