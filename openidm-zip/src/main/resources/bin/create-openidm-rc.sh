#!/bin/sh

cd ..
OPENIDM_HOME=`pwd`
OPENIDM_USER=`id -un`
SCRIPT_NAME='openidm'
JAVA_BIN=`which java`

#########################################
cat << EOF > bin/${SCRIPT_NAME}
#!/bin/sh

# chkconfig: 345 95 5
# description: start/stop openidm

# clean up left over pid files if necessary
cleanupPidFile() {
  if [ -f \$OPENIDM_PID_FILE ]; then
    rm -f "\$OPENIDM_PID_FILE"
  fi
  trap - EXIT
  exit
}

JAVA_BIN=${JAVA_BIN}
OPENIDM_HOME=${OPENIDM_HOME}
OPENIDM_USER=${OPENIDM_USER}
OPENIDM_PID_FILE=\$OPENIDM_HOME/.openidm.pid
OPENIDM_OPTS="-Xmx1024m -Dfile.encoding=UTF-8"

cd \${OPENIDM_HOME}

# Set JDK Logger config file if it is present and an override has not been issued
if [ -z "\$LOGGING_CONFIG" ]; then
  if [ -r "\$OPENIDM_HOME"/conf/logging.properties ]; then
    LOGGING_CONFIG="-Djava.util.logging.config.file=\$OPENIDM_HOME/conf/logging.properties"
  else
    LOGGING_CONFIG="-Dnop"
  fi
fi

CLASSPATH="\$OPENIDM_HOME"/bin/*


START_CMD="nohup \$JAVA_BIN \$LOGGING_CONFIG \$JAVA_OPTS \$OPENIDM_OPTS \\
		-Djava.endorsed.dirs=\$JAVA_ENDORSED_DIRS \\
		-classpath \\"\$CLASSPATH\\" \\
		-Djava.awt.headless=true \\
		org.forgerock.commons.launcher.Main -c bin/launcher.json > logs/server.out 2>&1 &"

case "\${1}" in
start)
	su \$OPENIDM_USER -c "\$START_CMD eval echo \\$\! > \$OPENIDM_PID_FILE"
  	exit \${?}
  ;;
stop)
	./shutdown.sh > /dev/null
	exit \${?}
  ;;
restart)
	./shutdown.sh > /dev/null
	su \$OPENIDM_USER -c "\$START_CMD eval echo \\$\! > \$OPENIDM_PID_FILE"
  	exit \${?}
  ;;
*)
  echo "Usage: ${SCRIPT_NAME} { start | stop | restart }"
  exit 1
  ;;
esac
EOF
#########################################

chmod a+x bin/${SCRIPT_NAME}

echo
echo "${SCRIPT_NAME} script has been created in ${OPENIDM_HOME}/bin"
echo "To finish installation, copy the script in the /etc/init.d folder"
echo "and run the following command:"
echo "chkconfig --add openidm"
echo
echo "To remove the service, run the following command:"
echo "chkconfig --del openidm"
echo
