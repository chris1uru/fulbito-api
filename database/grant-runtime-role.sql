-- Ejecutar como propietario DESPUES de crear un rol de login llamado
-- fulbito_runtime. La API diaria debe usar ese rol; Flyway debe seguir usando
-- un rol migrador separado mediante un proceso de despliegue controlado.

REVOKE ALL ON SCHEMA public FROM fulbito_runtime;
GRANT USAGE ON SCHEMA public TO fulbito_runtime;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO fulbito_runtime;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO fulbito_runtime;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO fulbito_runtime;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO fulbito_runtime;

REVOKE CREATE ON SCHEMA public FROM fulbito_runtime;
