@echo off
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

set CP=bin\*;framework\*
set JAVA_OPTS_SERVICE=-Xmx1024m;-Xms1024m;-Dfile.encoding=UTF-8;-Djava.endorsed.dirs="%JAVA_ENDORSED_DIRS%";-Djava.util.logging.config.file=conf\logging.properties;-Djava.security.auth.login.config=security\jaas-repo.conf;
rem Enable debugging uncomment the line below
rem set JAVA_OPTS_SERVICE=%JAVA_OPTS_SERVICE%-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005;

call "%EXECUTABLE%" /install %CMD_LINE_ARGS%
goto :EOF

:end
exit /b 1
