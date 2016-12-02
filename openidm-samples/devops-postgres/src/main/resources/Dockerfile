# Extend Postgres to be used as an OpenIDM repo
# See https://hub.docker.com/_/postgres/


FROM postgres:9.6

ENV TMP_PG /tmp/scripts
COPY scripts $TMP_PG

RUN buildDeps=' \
       	unzip \
    	' \
    && set -x \
	&& apt-get update && apt-get install -y --no-install-recommends $buildDeps \
	&& rm -rf /var/lib/apt/lists/* \
	&& cp $TMP_PG/openidm.pgsql /docker-entrypoint-initdb.d/01_init.sql \
	&& cp $TMP_PG/default_schema_optimization.pgsql /docker-entrypoint-initdb.d/02_optimize.sql \
	&& cp $TMP_PG/activiti.postgres.create.*.sql /docker-entrypoint-initdb.d/ \
	&& rm -r $TMP_PG