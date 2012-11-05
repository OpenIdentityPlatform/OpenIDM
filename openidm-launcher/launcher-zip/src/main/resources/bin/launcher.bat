@echo off
SETLOCAL ENABLEDELAYEDEXPANSION

rem Guess FORGEROCK_HOME if not defined
set "CURRENT_DIR=%cd%"
if not "%FORGEROCK_HOME%" == "" goto homeSet
set FORGEROCK_HOME=%CURRENT_DIR%
if exist "%FORGEROCK_HOME%\bin\launcher.bat" goto homeOk
cd ..
set FORGEROCK_HOME=%cd%
:homeSet

if exist "%FORGEROCK_HOME%\bin\launcher.bat" goto homeOk
echo Invalid FORGEROCK_HOME environment variable
echo Please set it to correct ForgeRock Launcher Home
:homeOk

rem Check Java availability
if not "%JAVA_HOME%" == "" goto checkJavaHome
if not "%JRE_HOME%" == "" goto checkJavaHome
echo JAVA_HOME or JRE_HOME not available, Java is needed to run the ForgeRock Launcher
echo Please install Java and set the JAVA_HOME accordingly
goto exit
:checkJavaHome
if exist "%JAVA_HOME%\bin\java.exe" goto javaHomeOk
if exist "%JRE_HOME%\bin\java.exe" goto jreHomeOk
echo Incorrect JAVA_HOME or JRE_HOME
goto exit
:jreHomeOk
set JAVA="%JRE_HOME%\bin\java.exe"
set JAVA_DLL="%JRE_HOME%\bin\server\jvm.dll"
goto homeOk
:javaHomeOk
set JAVA="%JAVA_HOME%\bin\java.exe"
set JAVA_DLL="%JAVA_HOME%\jre\bin\server\jvm.dll"
:homeOk

rem Set CLASSPATH for starting ForgeRock Launcher
set CP="bin/*;framework/*"


rem SET MISC PROPERTIES
rem Architecture, can be i386 or amd64 or ia64 (it is basically the directory name
rem   where the binaries are stored, if not set this script will try to 
rem   find the value automatically based on environment variables)
set ARCH=
rem find out the architecture
if ""%ARCH%"" == """" (
  set ARCH=i386
  if ""%PROCESSOR_ARCHITECTURE%"" == ""AMD64"" set ARCH=amd64
  if ""%PROCESSOR_ARCHITECTURE%"" == ""IA64""  set ARCH=ia64
)

rem Run java options, needs to be separated by space
set JAVA_OPTS=-Xmx1024m

rem Service java options, needs to be separated by ; or #
set JAVA_OPTS_SERVICE=-Xmx1024m
set MAIN_CLASS=org.forgerock.commons.launcher.Main
set JVM_OPTION_IDENTIFIER=-J


if ""%1"" == ""/run"" goto srvRun
if ""%1"" == ""/install"" goto srvInstall
if ""%1"" == ""/uninstall"" goto srvUninstall

echo Usage: launcher ^<command^> ^[option^]
echo command:
echo    /install ^[^<serviceName^>^] ^["-J<java option>"^] - Installs the service.
echo    /uninstall ^[^<serviceName^>^] - Uninstalls the service.
echo    /run ^["-J<java option>"^] - Runs the server from the console.
echo.
echo example:
echo     launcher.bat /install
echo        - this will install launcher as service
echo.    
echo     launcher.bat /run "-J-Xdebug" "-J-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
echo        - this will run launcher in debug mode
goto :EOF

:srvRun
rem Run the server main class
shift
set JAVA_OPTS_PARAM=
set JAVA_OPTS_DELIM=
for %%P in (%*) do (
    set T=%%P
    if "!T:~1,2!" == "%JVM_OPTION_IDENTIFIER%" (
      set JAVA_OPTS_PARAM=!JAVA_OPTS_PARAM!!JAVA_OPTS_DELIM!!T:~3,-1!
      set JAVA_OPTS_DELIM= 
    )
)
cd "%FORGEROCK_HOME%"

%JAVA% %JAVA_OPTS% %JAVA_OPTS_PARAM% -server -classpath %CP% %MAIN_CLASS%
cd "%CURRENT_DIR%"
goto :EOF


:srvInstall
rem Install the Connector Server as Windows service
shift
set SERVICE_NAME=ForgeRockLauncherJavaService
if not ""%1"" == """" (
    set T=%1
    if "!T:~1,2!" == "%JVM_OPTION_IDENTIFIER%" goto :noServiceName
    set SERVICE_NAME=%1
)
shift
:noServiceName
set JAVA_OPTS_PARAM=
set JAVA_OPTS_DELIM=
for %%P in (%*) do (
    set T=%%P
    if "!T:~1,2!" == "%JVM_OPTION_IDENTIFIER%" (
      set JAVA_OPTS_PARAM=!JAVA_OPTS_PARAM!!JAVA_OPTS_DELIM!!T:~3,-1!
      set JAVA_OPTS_DELIM=;
    )
)
"%FORGEROCK_HOME%\bin\%ARCH%\prunsrv.exe" //IS//%SERVICE_NAME% --Install="%FORGEROCK_HOME%\bin\%ARCH%\prunsrv.exe" --Description="ForgeRock OSGi Java Server" --Jvm=%JAVA_DLL% --JvmOptions=%JAVA_OPTS_SERVICE%%JAVA_OPTS_PARAM% --Classpath=%CP% --StartMode=jvm --StartPath="%FORGEROCK_HOME%" --StartClass=%MAIN_CLASS% --StartMethod=start --StartParams=dummy --StopMode=jvm --StopClass=%MAIN_CLASS% --StopMethod=stop --StopParams=dummy --LogPath="%FORGEROCK_HOME%\logs" --LogPrefix=service --LogLevel=INFO --StdOutput=auto --StdError=auto


echo ForgeRock Launcher Java Service successfully installed as "%SERVICE_NAME%" service
goto :EOF

:srvUninstall
shift
if not ""%1"" == """" ( 
    set SERVICE_NAME=%1
) else (
    set SERVICE_NAME=ForgeRockLauncherJavaService
)
"%FORGEROCK_HOME%\bin\%ARCH%\prunsrv.exe" //DS//%SERVICE_NAME%
echo ForgeRock Launcher Java Service "%SERVICE_NAME%" removed successfully
goto :EOF

:exit
exit /b 1
