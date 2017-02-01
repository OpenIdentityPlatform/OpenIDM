# OpenDJ Repository Support

OpenDJ repo support is currently expiremental in OpenIDM with several
key features not yet implemented. To compensate for this OrientDB is
still being used to support any not-yet-implemented in DJ features.

# Preparing OpenDJ

In order to use OpenDJ as our back-end repository we must first
load the openidm schema and populate the base structure.

## Option 1. Setting up a new OpenDJ

If setting up a fresh instance of DJ dedicated to IDM the following commands can be run to setup and start the server

    ./setup --cli --doNotStart -a --noPropertiesFile --no-prompt --skipPortCheck --baseDN dc=openidm,dc=forgerock,dc=com  --hostname localhost --rootUserPassword password --ldapPort 1389 --adminConnectorPort 4444 --backendType pdb 
    ln -s /path/to/openidm/db/opendj/schema/openidm.ldif config/schema/99-openidm.ldif 
    ./bin/import-ldif -n userRoot -l /path/to/openidm/openidm-zip/src/main/resources/db/opendj/scripts/populate_users.ldif 
    ./bin/start-ds
    
## Option 2. Configuring an existing OpenDJ server

Alternatively an existing DJ instance can be configured to support IDM as follows.

### Installing the Schema

OpenIDM adds several new attributes and objectClasses to the DJ schema.
In order to add this to DJ they must be copied to the `config/schema`
directory.

    cp openidm/db/opendj/schema/openidm.ldif /path/to/opendj/config/schema/99-openidm.ldif

### Create OpenIDM Backend and BaseDN

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

    ./bin/import-ldif -n openidm -l openidm/db/opendj/scripts/populate_users.ldif

# Preparing OpenIDM

## Copy Repo Configuration

OpenDJ repository support is currently not complete. As such, we must use the base
repo.orientdb.json and repo.opendj.json. Copy the `db/opendj/conf/repo.opendj.json`
to the `conf/` directory alongside repo.orientdb.json.

