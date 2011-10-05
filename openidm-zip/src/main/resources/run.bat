@ECHO OFF
SETLOCAL
:BEGIN
CLS
REM - THE BELOW LINE GIVES THE USER 3 CHOICES
ECHO Start in debug mode [1] default
ECHO Start in normal mode [2]
ECHO Show help [3]
set USERINP=1
set /p USERINP=choose a number(1-3): %=%
cls
REM - THE NEXT THREE LINES ARE DIRECTING USER DEPENDING UPON INPUT
IF %USERINP% ==3 GOTO SHOW
IF %USERINP% ==2 GOTO RUN
IF %USERINP% ==1 GOTO DEBUG
GOTO END
:SHOW
ECHO To be able to start the OpenIDM with this command the Sun Java 6 MUST be
ECHO installed on this computer and the PATH variable must contain
ECHO this: '%%JAVA_HOME%%/bin' where the JAVA_HOME points to the Java 6
ECHO install directory.
ECHO.
ECHO Current %%JAVA_HOME%% = %JAVA_HOME%
ECHO.
ECHO If you get this message: 
ECHO         'java' is not recognized as an internal or external command,
ECHO         operable program or batch file.
ECHO then install the Sun Java 6 and set the PATH environment variable.
ECHO.
rem ECHO Usage of this command is
rem ECHO run.bat
rem ECHO example "%~dp0run.bat"
GOTO END
:RUN
java -Xmx1024m -Dfile.encoding=UTF-8 -jar bin/felix.jar
GOTO END
:DEBUG
java -Dfile.encoding=UTF-8 -Djava.compiler=NONE -Xnoagent -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 -Xmx1024m -jar bin/felix.jar
:END
ENDLOCAL