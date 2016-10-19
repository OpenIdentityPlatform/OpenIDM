#!/bin/sh
# Copyright 2016 ForgeRock AS All rights reserved.

set -e

# first arg is `-f` or `--some-option`
if [ "${1#-}" != "$1" ] || [ "jpda" = "$1" ]; then
    set -- openidm "$@"
fi

# Need to add more checks around arguments and user being passed
# to the container -- most examples in the docker library didn't
# make sense as they hard-code their user/group ids in this script.

if [ "$1" = 'openidm' ]; then

    #OPENIDM_HOME="/opt/openidm"

    # We set OPENIDM_PID_FILE as we don't want to provide a way
    # to customize it in the Dockerfile.
    OPENIDM_PID_FILE="$OPENIDM_HOME"/.openidm.pid

    # We set JAVA_OPTS as we don't want to provide a way
    # to customize it in the Dockerfile.
    # Note that this is used differently from the startup script
    # as JAVA_OPTS is never used and is believed to be a better match
    # for those configurations
    JAVA_OPTS="-Xmx$JVM_MAX -Xms$JVM_MIN"

    # Let's copy over parts of the startup.sh for now until we refine this
    # a little further.

    # Set JDK Logger config file if it is present and an override has not been issued
    PROJECT_HOME="$OPENIDM_HOME"
    CLOPTS=""
    JPDA=""
    while [ "$1" ]; do
        shift
        if [ "$1" = "jpda" ]; then
            JPDA=$1
        else
            if [ "$1" = "-p" ] && [ "$2" ]; then
                PROJECT_HOME="$OPENIDM_HOME/$2"
            fi
            CLOPTS="$CLOPTS $1"
        fi
    done
    if [ -z "$LOGGING_CONFIG" ]; then
        if [ -r "$PROJECT_HOME"/conf/logging.properties ]; then
            LOGGING_CONFIG="-Djava.util.logging.config.file=$PROJECT_HOME/conf/logging.properties"
        elif [ -r "$OPENIDM_HOME"/conf/logging.properties ]; then
            LOGGING_CONFIG="-Djava.util.logging.config.file=$OPENIDM_HOME/conf/logging.properties"
        else
            LOGGING_CONFIG="-Dnop"
        fi
    fi

    if [ "$JPDA" = "jpda" ]; then
        if [ -z "$JPDA_TRANSPORT" ]; then
            JPDA_TRANSPORT="dt_socket"
        fi
        if [ -z "$JPDA_ADDRESS" ]; then
            JPDA_ADDRESS="5005"
        fi
        if [ -z "$JPDA_SUSPEND" ]; then
            JPDA_SUSPEND="n"
        fi
        if [ -z "$JPDA_OPTS" ]; then
            JPDA_OPTS="-Djava.compiler=NONE -Xnoagent -Xdebug -Xrunjdwp:transport=$JPDA_TRANSPORT,address=$JPDA_ADDRESS,server=y,suspend=$JPDA_SUSPEND"
        fi
        JAVA_OPTS="$JAVA_OPTS $JPDA_OPTS"
    fi

    if [ -z "$NODE_ID" ]; then
        NODE_ID=`uuid`
    fi

    BOOT_OPTS="-Dopenidm.keystore.password=$KST_PASS -Dopenidm.truststore.password=$TST_PASS -Dopenidm.node.id=$NODE_ID -Dopenidm.repo.host=$DB_HOST -Dopenidm.repo.port=$DB_PORT -Dopenidm.repo.user=$DB_USER -Dopenidm.repo.password=$DB_PASSWORD"
    OPENIDM_OPTS="$OPENIDM_OPTS $BOOT_OPTS"

    CLASSPATH="$OPENIDM_HOME/bin/*:$OPENIDM_HOME/framework/*"

    echo "Using OPENIDM_HOME:   $OPENIDM_HOME"
    echo "Using PROJECT_HOME:   $PROJECT_HOME"
    echo "Using OPENIDM_OPTS:   $OPENIDM_OPTS"
    echo "Using LOGGING_CONFIG: $LOGGING_CONFIG"
    echo "Using NODE_ID : $NODE_ID"

    # Keep track of this pid
    echo $$ > "$OPENIDM_PID_FILE"

    # Need to replace the hard-coded user:group with variables
    exec gosu idmuser:openidm java "$LOGGING_CONFIG" $JAVA_OPTS $OPENIDM_OPTS \
        -Djava.endorsed.dirs="$JAVA_ENDORSED_DIRS" \
        -classpath "$CLASSPATH" \
        -Dopenidm.system.server.root="$OPENIDM_HOME" \
        -Djava.awt.headless=true \
        org.forgerock.commons.launcher.Main -c "$OPENIDM_HOME"/bin/launcher.json $CLOPTS
fi

exec "$@"