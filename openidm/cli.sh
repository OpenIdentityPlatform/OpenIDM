#!/bin/sh
# 
# Copyright 2013 ForgeRock, Inc.
#
# The contents of this file are subject to the terms of the Common Development and
# Distribution License (the License). You may not use this file except in compliance 
# with the License.
#
# You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for 
# the specific language governing permission and limitations under the License.
#
# When distributing Covered Software, include this CDDL Header Notice in each file 
# and include the License file at legal/CDDLv1.0.txt. If applicable, add the 
# following below the CDDL Header, with the fields enclosed by brackets [] 
# replaced by your own identifying information: 
# "Portions copyright [year] [name of copyright owner]".
#

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

echo "Executing $PRG..."

# Get standard environment variables
PRGDIR=`dirname "$PRG"`

OPENIDM_HOME=${OPENIDM_HOME:-`(cd "$PRGDIR"; pwd)`}
echo "Starting shell in $OPENIDM_HOME"

java $JAVA_OPTS -classpath "$OPENIDM_HOME/bin/*:$OPENIDM_HOME/bundle/*" \
     -Dopenidm.system.server.root="$OPENIDM_HOME" \
     org.forgerock.openidm.shell.impl.Main "$@"
