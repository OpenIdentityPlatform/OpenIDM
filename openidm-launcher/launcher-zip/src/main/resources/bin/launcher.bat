@echo off
SETLOCAL ENABLEDELAYEDEXPANSION

rem Set Launcher Home
set CURRENT_DIR=%cd%
cd /d %0\..
set SCRIPT_DIR=%cd%
cd ..
if not "%LAUNCHER_SERVER_HOME%" == "" goto homeSet
set LAUNCHER_SERVER_HOME=%cd%
:homeSet
cd "%CURRENT_DIR%""
if exist "%LAUNCHER_SERVER_HOME%\bin\launcher.bat" goto homeOk
echo Invalid LAUNCHER_SERVER_HOME environment variable
echo Please set it to correct Launcher Home
goto exit
:homeOk

rem Check Java availability
if not "%JAVA_HOME%" == "" goto checkJavaHome
if not "%JRE_HOME%" == "" goto checkJavaHome
echo JAVA_HOME or JRE_HOME not available, Java is needed to run the Connector Server
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

rem Check and Set CLASSPATH for starting Launcher
if not "%CP%" == "" goto classpathOK
set CP="%SCRIPT_DIR%\*"
:classpathOK

rem SET MISC PROPERTIES
rem Architecture, can be i386 or amd64 or ia64 (it is basically the directory name
rem where the binaries are stored, if not set this script will try to 
rem find the value automatically based on environment variables)
set ARCH=
rem find out the architecture
if ""%ARCH%"" == """" (
  set ARCH=i386
  if ""%PROCESSOR_ARCHITECTURE%"" == ""AMD64"" set ARCH=amd64
  if ""%PROCESSOR_ARCHITECTURE%"" == ""IA64""  set ARCH=ia64
)


rem Set Launcher start params, needs to be separated by ;
if not "%LAUNCHER_START_PARAMS%" == "" goto launcerStartOK
set LAUNCHER_START_PARAMS=-c;bin/launcher.json
:launcerStartOK
rem -------------------------------------------------------------


if ""%1"" == ""/install"" goto srvInstall
if ""%1"" == ""/uninstall"" goto srvUninstall

echo Usage: launcher.bat ^<command^> ^[option^]
echo command:
echo    /install ^[^<serviceName^>^] - Installs the service.
echo    /uninstall ^[^<serviceName^>^] - Uninstalls the service.
echo.
goto :EOF


:srvInstall
rem Install the ForgeRock Launcher as Windows service
shift
set SERVICE_NAME=ForgeRockLauncherJavaService
if not ""%1"" == """" (
    set SERVICE_NAME=%1
)
shift
:noServiceName
set MAIN_CLASS=org.forgerock.commons.launcher.Main
"%LAUNCHER_SERVER_HOME%\bin\%ARCH%\prunsrv.exe" //IS//%SERVICE_NAME% --Install="%LAUNCHER_SERVER_HOME%\bin\%ARCH%\prunsrv.exe" --Description="ForgeRock OSGi Java Server" --Jvm=%JAVA_DLL% --Classpath=%CP% --JvmOptions=%JAVA_OPTS_SERVICE%%JAVA_OPTS_PARAM% --StartPath="%LAUNCHER_SERVER_HOME%" --StartMode=jvm --StartClass=%MAIN_CLASS% --StartMethod=start --StartParams="%LAUNCHER_START_PARAMS%" --StopMode=jvm --StopClass=%MAIN_CLASS% --StopMethod=stop --LogPath="%LAUNCHER_SERVER_HOME%\logs" --LogPrefix=launcher --StdOutput=auto --StdError=auto --LogLevel=INFO
echo ForgeRock Launcher Java Service successfully installed as "%SERVICE_NAME%" service
goto :EOF

:srvUninstall
shift
if not ""%1"" == """" ( 
    set SERVICE_NAME=%1
) else (
    set SERVICE_NAME=ForgeRockLauncherJavaService
)
"%LAUNCHER_SERVER_HOME%\bin\%ARCH%\prunsrv.exe" //DS//%SERVICE_NAME%
echo Service "%SERVICE_NAME%" removed successfully
goto :EOF

:exit
exit /b 1
