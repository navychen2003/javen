@echo off

REM The Lightning command script
REM
REM Environment Variables
REM
REM   JAVA_HOME        The java implementation to use.  Overrides JAVA_HOME.
REM 
REM   LIGHTNING_CLASSPATH Extra Java CLASSPATH entries.
REM
REM   LIGHTNING_HEAPSIZE  The maximum amount of heap to use, in MB. 
REM                    Default is 200.
REM
REM   LIGHTNING_OPTS      Extra Java runtime options.
REM
REM   LIGHTNING_CONF_DIR  Alternate conf dir. Default is %LIGHTNINGAPP_HOME%\conf.
REM
REM   LIGHTNING_ROOT_LOGGER The root appender. Default is INFO,console
REM

if "%OS%"=="Windows_NT" @setlocal
if "%OS%"=="WINNT" @setlocal
setlocal EnableDelayedExpansion

REM # %~dp0 is expanded pathname of the current script under NT
set DEFAULT_LIGHTNINGAPP_HOME=%~dp0.

if "%LIGHTNINGAPP_HOME%"=="" set LIGHTNINGAPP_HOME=%DEFAULT_LIGHTNINGAPP_HOME%
if "%LIGHTNING_HOME%"=="" set LIGHTNING_HOME=%DEFAULT_LIGHTNINGAPP_HOME%\..
set DEFAULT_LIGHTNINGAPP_HOME=

set LIGHTNING_HTTPPORT=10080
set LIGHTNING_HTTPSPORT=10443

set LIGHTNING_CONF_DIR=%LIGHTNING_HOME%\lightning\conf
set LIGHTNING_LOG_DIR=%LIGHTNING_HOME%\logs

if exist "%LIGHTNING_HOME%\java" set JAVA_HOME=%LIGHTNING_HOME%\java

set jetty.home=%LIGHTNINGAPP_HOME%
set jetty.base=%LIGHTNINGAPP_HOME%
set lightning.home=%LIGHTNING_HOME%\lightning
set jetty.port=%LIGHTNING_HTTPPORT%
set https.port=%LIGHTNING_HTTPSPORT%

REM call "%bin%\lightning-config.bat" 

REM # if no args specified, show usage
if ""%1""=="""" goto doneHelp
set LIGHTNING_COMMAND=%1
shift
set LIGHTNING_CMD_LINE_ARGS=
:nextArg
if ""%1""=="""" goto doneStart
REM if ""%1""==""-noclasspath"" goto clearclasspath
set LIGHTNING_CMD_LINE_ARGS=%LIGHTNING_CMD_LINE_ARGS% %1
shift
goto nextArg

:doneStart
REM # get arguments
set COMMAND=%LIGHTNING_COMMAND%
REM shift

if exist "%LIGHTNING_CONF_DIR%\lightning-env.bat" call "%LIGHTNING_CONF_DIR%\lightning-env.bat"

REM # some Java parameters
if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome

REM if [ "%JAVA_HOME%" != "" ]; then
REM  #echo "run java in %JAVA_HOME%"
REM  JAVA_HOME=%JAVA_HOME%
REM fi

if "%JAVAEXE%"=="" set JAVAEXE=java.exe

set JAVA="%JAVA_HOME%"\bin\%JAVAEXE%
set JAVA_HEAP_MAX=-Xmx768m 

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

REM # CLASSPATH initially contains %LIGHTNING_CONF_DIR%
set LIGHTNING_OPTS=
set CLASSPATH=%LIGHTNING_CONF_DIR%
set CLASSPATH=%CLASSPATH%;%JAVA_HOME%\lib\tools.jar

REM # for developers, add Lightning classes to CLASSPATH
if exist "%LIGHTNINGAPP_HOME%\lib" set CLASSPATH=%CLASSPATH%;%LIGHTNINGAPP_HOME%\lib
if exist "%LIGHTNINGAPP_HOME%\etc" set CLASSPATH=%CLASSPATH%;%LIGHTNINGAPP_HOME%\etc
if exist "%LIGHTNINGAPP_HOME%\conf" set CLASSPATH=%CLASSPATH%;%LIGHTNINGAPP_HOME%\conf

for /r %LIGHTNINGAPP_HOME%\lib %%f in ( *.jar ) do set CLASSPATH=!CLASSPATH!;%%f
for /r %LIGHTNINGAPP_HOME%\webapps\lightning\WEB-INF\lib %%f in ( *.jar ) do set CLASSPATH=!CLASSPATH!;%%f

REM # for releases, add lightning jars & webapps to CLASSPATH
if exist "%LIGHTNINGAPP_HOME%\resources" set CLASSPATH=%CLASSPATH%;%LIGHTNINGAPP_HOME%\resources

REM # add Lightning release jar to CLASSPATH
for %%f in ( "%LIGHTNINGAPP_HOME%\lightning-*.jar" ) do set CLASSPATH=!CLASSPATH!;%%f

REM # add libs to CLASSPATH
for /r %LIGHTNINGAPP_HOME%\lib\ %%f in ( *.jar ) do set CLASSPATH=!CLASSPATH!;%%f


REM # default log directory & file
if "%LIGHTNING_LOG_DIR%" == "" set LIGHTNING_LOG_DIR=%LIGHTNING_HOME%\logs
if "%LIGHTNING_LOGFILE%" == "" set LIGHTNING_LOGFILE=lightning.log


REM # figure out which class to run
if "%COMMAND%"=="bdb" goto setBdb
goto nextNameNode
:setBdb
set LIGHTNING_OPTS=%LIGHTNING_OPTS% -Dbigdb.ruby.sources=%LIGHTNINGAPP_HOME%\lib\ruby
set CLASS=org.jruby.Main -X+O %JRUBY_OPTS% %LIGHTNINGAPP_HOME%\lib\ruby\start.rb
goto endClass

:nextNameNode
if "%COMMAND%"=="namenode" goto setNameNode
goto nextDataNode
:setNameNode
set CLASS=org.javenstudio.lightning.util.SimpleNamenode
goto endClass

:nextDataNode
if "%COMMAND%" == "datanode" goto setDataNode 
goto nextFS
:setDataNode
set CLASS=org.javenstudio.lightning.util.SimpleDatanode
goto endClass

:nextFS
if "%COMMAND%" == "fs" goto setFS
goto nextDFS
:setFS
set CLASS=org.javenstudio.lightning.util.SimpleFsShell
goto endClass

:nextDFS
if "%COMMAND%" == "dfs" goto setDFS
goto nextDfsAdmin
:setDFS
set CLASS=org.javenstudio.lightning.util.SimpleFsShell
goto endClass

:nextDfsAdmin
if "%COMMAND%" == "dfsadmin" goto setDfsAdmin
goto nextJFM
:setDfsAdmin
set CLASS=org.javenstudio.lightning.util.SimpleFsAdmin
goto endClass

:nextJFM
if "%COMMAND%" == "jfm" goto setJFM
goto nextBigdb
:setJFM
set CLASS=org.javenstudio.lightning.util.SimpleJfm
goto endClass

:nextBigdb
if "%COMMAND%" == "bigdb" goto setBigdb
goto nextVersion
:setBigdb
set CLASS=org.javenstudio.lightning.util.SimpleBigdb
goto endClass

:nextVersion
if "%COMMAND%" == "version" goto setVersion
goto nextFsck
:setVersion
set CLASS=org.javenstudio.lightning.util.SimpleVersion
goto endClass

:nextFsck
if "%COMMAND%" == "fsck" goto setFsck
goto nextPaxos
:setFsck
set CLASS=org.javenstudio.lightning.util.SimpleFsck
goto endClass

:nextPaxos
if "%COMMAND%" == "paxos" goto setPaxos
goto nextPaxosCli
:setPaxos
set CLASS=org.javenstudio.lightning.util.SimplePaxos
goto endClass

:nextPaxosCli
if "%COMMAND%" == "pcli" goto setPaxosCli
goto nextJar
:setPaxosCli
set CLASS=org.javenstudio.lightning.util.SimplePaxosCli
goto endClass

:nextJar
if "%COMMAND%" == "jar" goto setJar
goto nextDistcp
:setJar
set CLASS=org.javenstudio.raptor.util.RunJar
goto endClass

:nextDistcp
if "%COMMAND%" == "distcp" goto setDistcp
goto nextDefault
:setDistcp
set CLASS=org.javenstudio.raptor.util.CopyFiles
goto endClass

:nextDefault
set CLASS=%COMMAND%
:endClass

REM # cygwin path translation
REM if expr `uname` : 'CYGWIN*' > \dev\null; then
REM   CLASSPATH=`cygpath -p -w "%CLASSPATH%"`
REM   LIGHTNINGAPP_HOME=`cygpath -d "%LIGHTNINGAPP_HOME%"`
REM   LIGHTNING_LOG_DIR=`cygpath -d "%LIGHTNING_LOG_DIR%"`
REM fi

if "%LIGHTNING_ROOT_LOGGER%" == "" set LIGHTNING_ROOT_LOGGER=INFO,console
if "%LIGHTNING_CHARACTERENCODING%" == "" set LIGHTNING_CHARACTERENCODING=UTF-8

REM set LIGHTNING_OPTS=
set LIGHTNING_OPTS=%LIGHTNING_OPTS% -Dfile.encoding=%LIGHTNING_CHARACTERENCODING%
set LIGHTNING_OPTS=%LIGHTNING_OPTS% -Dsun.jnu.encoding=%LIGHTNING_CHARACTERENCODING%
set LIGHTNING_OPTS=%LIGHTNING_OPTS% -Djetty.home=%LIGHTNINGAPP_HOME%
set LIGHTNING_OPTS=%LIGHTNING_OPTS% -Djetty.base=%LIGHTNINGAPP_HOME%
set LIGHTNING_OPTS=%LIGHTNING_OPTS% -Dlightning.home=%LIGHTNING_HOME%\lightning
set LIGHTNING_OPTS=%LIGHTNING_OPTS% -Djetty.port=%LIGHTNING_HTTPPORT%
set LIGHTNING_OPTS=%LIGHTNING_OPTS% -Dhttps.port=%LIGHTNING_HTTPSPORT%
set LIGHTNING_OPTS=%LIGHTNING_OPTS% -Djava.util.logging.config.file=%LIGHTNINGAPP_HOME%/etc/logging.properties
set LIGHTNING_OPTS=%LIGHTNING_OPTS% -Dorg.javenstudio.common.util.logger.debug=%LIGHTNING_DEBUG%

if "%JAVA_LIBRARY_PATH%" == "" goto nextStepAfterSetJavaLibraryPath
:setJavaLibraryPath
set LIGHTNING_OPTS=%LIGHTNING_OPTS% -Djava.library.path=%JAVA_LIBRARY_PATH%
:nextStepAfterSetJavaLibraryPath

REM run it
:runAgain
%JAVA% %JAVA_HEAP_MAX% %LIGHTNING_OPTS% -classpath "%CLASSPATH%" %CLASS% %LIGHTNING_CMD_LINE_ARGS%

REM if "%COMMAND%" == "start" goto runAgain
goto end

REM if no args specified, show usage
:doneHelp
echo Usage: run [--config confdir] COMMAND
echo where COMMAND is one of:
echo   namenode -format  format the DFS filesystem
echo   namenode          run the DFS namenode
echo   datanode          run a DFS datanode
echo   bigdb             run the BigDB server
echo   paxos             run the Paxos server
echo   dfsadmin          run a DFS admin client
echo   fsck              run a DFS filesystem checking utility
echo   fs                run a generic filesystem user client
echo   jfm               run a gui filesystem user client
echo   bdb               run a BigDB client shell
echo   pcli              run a Paxos client shell
echo   version           print the version
echo   jar jarfile       run a jar file
echo  or
echo   CLASSNAME         run the class named CLASSNAME
echo Most commands print help when invoked w\o parameters.
goto end

:noJavaHome
echo "Error: JAVA_HOME is not set."
goto end

:end
if "%OS%"=="Windows_NT" @endlocal
if "%OS%"=="WINNT" @endlocal

