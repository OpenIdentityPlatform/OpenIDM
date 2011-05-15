# start in debug mode
java -Djava.compiler=NONE -Xnoagent -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 -Xmx1024m -jar bin/felix.jar

