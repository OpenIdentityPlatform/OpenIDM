@echo off
setlocal

rem Only set OPENIDM_HOME if not already set
set "CURRENT_DIR=%cd%"
if not "%OPENIDM_HOME%" == "" goto gotHome
set "OPENIDM_HOME=%CURRENT_DIR%"
if exist "%OPENIDM_HOME%\bin\felix.jar" goto okHome
cd ..
set "OPENIDM_HOME=%cd%"
cd "%CURRENT_DIR%"
:gotHome
if exist "%OPENIDM_HOME%\bin\felix.jar" goto okHome
echo The OPENIDM_HOME environment variable is not defined correctly
echo This environment variable is needed to run this program
goto end
:okHome

rem Only set OPENIDM_OPTS if not already set
if not "%OPENIDM_OPTS%" == "" goto noOpenIDMOpts
set OPENIDM_OPTS=${openidm.options} -Dfile.encoding=UTF-8
:noOpenIDMOpts

set "JPDA="

rem Check for a project directory, default to OpenIDM home directory
set PROJECT_HOME=%OPENIDM_HOME%
set CMD_LINE_ARGS=-c bin/launcher.json
:optLoop
if "%1"=="" goto optDone
rem We do not want jpda in CMD_LINE_ARGS
if "%1"=="jpda" goto optJpda
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
if "%1"=="-p" goto optP
shift
goto optLoop
:optJpda
set "JPDA=JPDA"
shift
goto optLoop
:optP
shift
if "%1"=="" goto optDone
set "PROJECT_HOME=%OPENIDM_HOME%\%1"
goto optLoop
:optDone

rem Set JDK Logger config file if it is present and an override has not been issued
if not "%LOGGING_CONFIG%" == "" goto noJuliConfig
set LOGGING_CONFIG=-Dnop
if not exist "%PROJECT_HOME%\conf\logging.properties" goto defaultJuliConfig
set LOGGING_CONFIG=-Djava.util.logging.config.file="%PROJECT_HOME%\conf\logging.properties"
goto noJuliConfig
:defaultJuliConfig
if not exist "%OPENIDM_HOME%\conf\logging.properties" goto noJuliConfig
set LOGGING_CONFIG=-Djava.util.logging.config.file="%OPENIDM_HOME%\conf\logging.properties"
:noJuliConfig
set JAVA_OPTS=%JAVA_OPTS% %LOGGING_CONFIG%

if not "%JPDA_TRANSPORT%" == "" goto gotJpdaTransport
set JPDA_TRANSPORT=dt_socket
:gotJpdaTransport
if not "%JPDA_ADDRESS%" == "" goto gotJpdaAddress
set JPDA_ADDRESS=5005
:gotJpdaAddress
if not "%JPDA_SUSPEND%" == "" goto gotJpdaSuspend
set JPDA_SUSPEND=n
:gotJpdaSuspend
if not "%JPDA_OPTS%" == "" goto gotJpdaOpts
set JPDA_OPTS=-Djava.compiler=NONE -Xnoagent -Xdebug -Xrunjdwp:transport=%JPDA_TRANSPORT%,address=%JPDA_ADDRESS%,server=y,suspend=%JPDA_SUSPEND%
:gotJpdaOpts
shift
:noJpda

rem Ensure that any user defined CLASSPATH variables are not used on startup,
rem but allow them to be specified here, in rare case when it is needed.
set CLASSPATH="bin\*;framework\*"

echo "Using OPENIDM_HOME:   %OPENIDM_HOME%"
echo "Using OPENIDM_OPTS:   %OPENIDM_OPTS%"
echo "Using LOGGING_CONFIG: %LOGGING_CONFIG%"

rem Note the quoting as JAVA_HOME may contain spaces.
set _RUNJAVA="%JAVA_HOME%\bin\java"

if not "%OS%" == "Windows_NT" goto noTitle
if "%TITLE%" == "" set TITLE=OpenIDM
set _EXECJAVA=start "%TITLE%" %_RUNJAVA%
goto gotTitle
:noTitle
set _EXECJAVA=start %_RUNJAVA%
:gotTitle

set MAINCLASS=org.forgerock.commons.launcher.Main

rem Execute Java with the applicable properties
pushd %OPENIDM_HOME%
if not "%JPDA%" == "" goto doJpda
call %_EXECJAVA% %JAVA_OPTS% %OPENIDM_OPTS%  -Djava.endorsed.dirs="%JAVA_ENDORSED_DIRS%" -classpath "%CLASSPATH%" -Dopenidm.system.server.root="%OPENIDM_HOME%" %MAINCLASS% %CMD_LINE_ARGS%
goto end
:doJpda
call %_EXECJAVA% %JAVA_OPTS% %OPENIDM_OPTS% %JPDA_OPTS% -Djava.endorsed.dirs="%JAVA_ENDORSED_DIRS%" -classpath "%CLASSPATH%" -Dopenidm.system.server.root="%OPENIDM_HOME%" %MAINCLASS% %CMD_LINE_ARGS%
popd

:end
