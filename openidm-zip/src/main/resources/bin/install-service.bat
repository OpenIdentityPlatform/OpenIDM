rem @echo off
set "EXECUTABLE=%cd%\launcher.bat"

rem Check that target executable exists
if exist "%EXECUTABLE%" goto execOK
echo Cannot find "%EXECUTABLE%"
echo This file is needed to run this program
goto end
:execOK

rem Get remaining unshifted command line arguments and save them in the
set CMD_LINE_ARGS=
:setArgs
if ""%1""=="""" goto doneSetArgs
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto setArgs
:doneSetArgs

rem set SERVER_START_PARAMS="-c;bin/launcher.json"
set CP=bin/launcher.jar;bin/felix.jar
set JAVA_OPTS_SERVICE=-Xmx1024m;-Djava.util.logging.config.file=conf\logging.properties;-Dlogback.configurationFile=conf\logging-config.xml;
rem Enable debugging uncomment the line below
set JAVA_OPTS_SERVICE=%JAVA_OPTS_SERVICE%-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005;

call "%EXECUTABLE%" /install %CMD_LINE_ARGS%
goto :EOF

:end
exit /b 1