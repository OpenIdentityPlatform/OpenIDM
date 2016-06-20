CLASSPATH="bin/*:framework/*"

exec java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 \
    -classpath "$CLASSPATH" \
	org.forgerock.commons.launcher.Main "$@"