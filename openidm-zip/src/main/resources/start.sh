# start in debug mode
java -Djava.compiler=NONE -Xnoagent -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 -Xmx1024mb -jar bin/felix.jar
#java -Xmx1024mb -jar bin/felix.jar

