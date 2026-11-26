Portions Copyright 2026 3A Systems, LLC.

Initialize a fresh PostgreSQL repository before starting OpenIDM. The
openidm.pgsql script drops an existing openidm schema. For an upgrade,
follow the Installation Guide for the source and target releases.

From the OpenIDM installation directory, edit db/postgresql/scripts/createuser.pgsql
to set a deployment-specific password for the openidm role, then run:

$ psql -U postgres -d postgres -v ON_ERROR_STOP=1 -f db/postgresql/scripts/createuser.pgsql
$ psql -h localhost -U openidm -d openidm -v ON_ERROR_STOP=1 -f db/postgresql/scripts/openidm.pgsql
$ psql -h localhost -U openidm -d openidm -v ON_ERROR_STOP=1 -f db/postgresql/scripts/activiti.postgres.create.engine.sql
$ psql -h localhost -U openidm -d openidm -v ON_ERROR_STOP=1 -f db/postgresql/scripts/activiti.postgres.create.history.sql
$ psql -h localhost -U openidm -d openidm -v ON_ERROR_STOP=1 -f db/postgresql/scripts/activiti.postgres.create.identity.sql

Copy the templates to your project's configuration directory and move
repo.orientdb.json out of that directory. For the default project:

$ cp db/postgresql/conf/repo.jdbc.json conf/
$ cp db/postgresql/conf/datasource.jdbc-default.json conf/

In conf/datasource.jdbc-default.json, set the password you chose for the
openidm role. Supply the database address using openidm.repo.host and
openidm.repo.port in conf/boot/boot.properties, or in the JDBC URL.

If you need the optional indexes and your server provides the pg_trgm
extension, review the comments in the optimization script before running it
with an account authorized to create the extension and indexes:

$ psql -U postgres -d openidm -v ON_ERROR_STOP=1 -f db/postgresql/scripts/default_schema_optimization.pgsql
