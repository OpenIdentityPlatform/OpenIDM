#!/bin/sh
DEBUG_MODE="n" # Default is no debug mode

#echo "Do you want to start in debug mode: [y]n"
#read DEBUG_MODE

if [ "$DEBUG_MODE" == "n" ]; then
	# start in normal mode
    java -Xmx1024m -jar bin/felix.jar
else
	# start in debug mode
    java -Djava.compiler=NONE -Xnoagent -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 -Xmx1024m -jar bin/felix.jar
fi



