# OpenDJ Repository Support

OpenDJ repo support is currently expiremental in OpenIDM with several
key features not yet implemented. To compensate for this OrientDB is
still being used to support any not-yet-implemented in DJ features.

# Preparing OpenDJ

In order to use OpenDJ as our back-end repository we must first
load the openidm schema and populate the base structure.

## Installing the Schema

OpenIDM adds several new attributes and objectClasses to the DJ schema.
In order to add this to DJ they must be copied to the `config/schema`
directory.

    cp openidm/db/opendj/schema/openidm.ldif /path/to/opendj/config/schema/99-openidm.ldif

## Create OpenIDM Backend and BaseDN

It is recommended to create a new backend dedicated to OpenIDM

    ./bin/dsconfig create-backend \
               --backend-name openidm \
               --set base-dn:dc=openidm,dc=forgerock,dc=com \
               --set enabled:true \
               --type local-db \
               --hostName localhost \
               --port 4444 \
               --bindDN cn=Directory\ Manager \
               --bindPassword password \
               --trustAll \
               --noPropertiesFile \
               --no-prompt

You can then populate the directory structure

    ./bin/ldapmodify \
               --hostname localhost \
               --port 1389 \
               --bindDN cn=Directory\ Manager \
               --bindPassword password \
               --continueOnError \
               --filename openidm/db/opendj/scripts/populate_users.ldif

# Preparing OpenIDM

## Copy Repo Configuration

OpenDJ repository support is currently not complete. As such, we must use the base
repo.orientdb.json and repo.opendj.json. Copy the `db/opendj/conf/repo.opendj.json`
to the `conf/` directory alongside repo.orientdb.json.

