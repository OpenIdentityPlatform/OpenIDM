#!/bin/sh

basedir=$(dirname $0)
cd $basedir/..

OPENIDM_HOME=`pwd`
SCRIPT_NAME='openidmlog'

#########################################
cat << EOF > bin/${SCRIPT_NAME}
${OPENIDM_HOME}/audit/*.csv {
daily
missingok
rotate 54
compress
}
EOF
#########################################

echo
echo "${SCRIPT_NAME} logrotate has been created in ${OPENIDM_HOME}/bin"
echo "To finish installation, copy the file in the /etc/logrotate.d/ folder"
echo
