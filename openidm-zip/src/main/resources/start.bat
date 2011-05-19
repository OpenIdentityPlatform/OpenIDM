@echo off
SETLOCAL
set START_MODE=%1

if not "%START_MODE%" == "" goto  start_debug

if not "%2" == "" goto  start
goto usage

:start_debug
# start in debug mode
java -Djava.compiler=NONE -Xnoagent -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 -Xmx1024m -jar bin/felix.jar
goto endprg

:start
# start in debug mode
java -Xmx1024m -jar bin/felix.jar
goto endprg

:usage
echo Usage of this command is
echo start.bat [debug]
echo example "%~dp0start"

:endprg
ENDLOCAL


