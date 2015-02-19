@echo off

if "%OS%"=="Windows_NT" @setlocal
if "%OS%"=="WINNT" @setlocal
setlocal EnableDelayedExpansion

set LIGHTNINGAPP_HOME=%~dp0.
set LIGHTNING_HOME=%LIGHTNINGAPP_HOME%\..

set LIGHTNING_HTTPPORT=10080
set LIGHTNING_HTTPSPORT=10443

set LIGHTNING_CONF_DIR=%LIGHTNING_HOME%\lightning\conf
set LIGHTNING_LOG_DIR=%LIGHTNING_HOME%\logs

if exist "%LIGHTNING_CONF_DIR%\lightning-env.bat" call "%LIGHTNING_CONF_DIR%\lightning-env.bat"

set jetty.home=%LIGHTNINGAPP_HOME%
set jetty.base=%LIGHTNINGAPP_HOME%
set lightning.home=%LIGHTNING_HOME%\lightning
set jetty.port=%LIGHTNING_HTTPPORT%
set https.port=%LIGHTNING_HTTPSPORT%

if exist "%LIGHTNING_HOME%\java" set JAVA_HOME=%LIGHTNING_HOME%\java
if "%JAVA_HOME%" == "" goto noJavaHome

REM set WRITELOCK_FILE=%LIGHTNING_HOME%\lightning\index\data\index\write.lock
REM if exist "%WRITELOCK_FILE%" del "%WRITELOCK_FILE%"

REM # for developers, add Lightning classes to CLASSPATH
if exist "%LIGHTNING_HOME%\lightning\conf" set CLASSPATH=%CLASSPATH%;%LIGHTNING_HOME%\lightning\conf

if "%LIGHTNING_ROOT_LOGGER%" == "" set LIGHTNING_ROOT_LOGGER=INFO,console
if "%LIGHTNING_CHARACTERENCODING%" == "" set LIGHTNING_CHARACTERENCODING=UTF-8

set STOP_OPTS=-DSTOP.PORT=10089 -DSTOP.KEY=secret
set START_OPTS=-jar %LIGHTNINGAPP_HOME%\start.jar %LIGHTNINGAPP_HOME%\etc\jetty.xml --stop

set JAVA_OPTS=
set JAVA_OPTS=%JAVA_OPTS% -Dfile.encoding=%LIGHTNING_CHARACTERENCODING%
set JAVA_OPTS=%JAVA_OPTS% -Dsun.jnu.encoding=%LIGHTNING_CHARACTERENCODING%
set JAVA_OPTS=%JAVA_OPTS% -Djetty.home=%LIGHTNINGAPP_HOME%
set JAVA_OPTS=%JAVA_OPTS% -Djetty.base=%LIGHTNINGAPP_HOME%
set JAVA_OPTS=%JAVA_OPTS% -Dlightning.home=%LIGHTNING_HOME%\lightning
set JAVA_OPTS=%JAVA_OPTS% -Djetty.port=%LIGHTNING_HTTPPORT%
set JAVA_OPTS=%JAVA_OPTS% -Dhttps.port=%LIGHTNING_HTTPSPORT%

set JAVA_HEAP_MAX=-Xmx1024m 

REM # check envvars which might override default args
if "%LIGHTNING_HEAPSIZE%"=="" goto endHeapSize
:setHeapSize
REM # echo "run with heapsize %LIGHTNING_HEAPSIZE%"
set JAVA_HEAP_MAX="-Xmx%LIGHTNING_HEAPSIZE%m"
:endHeapSize
if "%JAVA_HEAPSIZE%"=="" goto endJavaHeapSize
:setJavaHeapSize
echo "run with heapsize %JAVA_HEAPSIZE%"
set JAVA_HEAP_MAX="-Xmx%JAVA_HEAPSIZE%m"
:endJavaHeapSize

if ""%1""==""-debug"" goto startDebug
goto startApp

:startDebug
set JAVA="%JAVA_HOME%"\bin\java.exe

set JAVA_OPTS=%JAVA_OPTS% -Djava.util.logging.config.file=%LIGHTNINGAPP_HOME%/etc/loggingdebug.properties
set JAVA_OPTS=%JAVA_OPTS% -Dorg.javenstudio.common.util.logger.debug=true

echo %JAVA% %JAVA_HEAP_MAX% %JAVA_OPTS% %STOP_OPTS% %START_OPTS%
%JAVA% %JAVA_HEAP_MAX% %JAVA_OPTS% %STOP_OPTS% %START_OPTS%
goto end

:startApp
set JAVA="%JAVA_HOME%"\bin\java.exe

set JAVA_OPTS=%JAVA_OPTS% -Djava.util.logging.config.file=%LIGHTNINGAPP_HOME%/etc/logging.properties
set JAVA_OPTS=%JAVA_OPTS% -Dorg.javenstudio.common.util.logger.debug=false

%JAVA% %JAVA_HEAP_MAX% %JAVA_OPTS% %STOP_OPTS% %START_OPTS%

set WRITELOCK_FILE=%LIGHTNING_HOME%\lightning\index\data\index\write.lock
if exist "%WRITELOCK_FILE%" del "%WRITELOCK_FILE%"

goto end

:noJavaHome
echo "Error: JAVA_HOME is not set."
pause
goto end

:end
if "%OS%"=="Windows_NT" @endlocal
if "%OS%"=="WINNT" @endlocal
