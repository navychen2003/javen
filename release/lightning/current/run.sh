#!/bin/sh
#
# The Lightning command script
#
# Environment Variables
#
#   JAVA_HOME        The java implementation to use.  Overrides JAVA_HOME.
# 
#   LIGHTNING_CLASSPATH Extra Java CLASSPATH entries.
#
#   LIGHTNING_HEAPSIZE  The maximum amount of heap to use, in MB. 
#                    Default is 200.
#
#   LIGHTNING_OPTS      Extra Java runtime options.
#
#   LIGHTNING_CONF_DIR  Alternate conf dir. Default is ${LIGHTNINGAPP_HOME}\conf.
#
#   LIGHTNING_ROOT_LOGGER The root appender. Default is INFO,console
#

bin=`dirname "$0"`
bin=`cd "$bin"; pwd`

cygwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
esac

# # the root of the installation
DEFAULT_LIGHTNINGAPP_HOME=${bin}

if [ "${LIGHTNINGAPP_HOME}" = "" ]; then
  export LIGHTNINGAPP_HOME=${DEFAULT_LIGHTNINGAPP_HOME}
fi
if [ "${LIGHTNING_HOME}" = "" ]; then
  export LIGHTNING_HOME=${DEFAULT_LIGHTNINGAPP_HOME}/..
fi

DEFAULT_LIGHTNINGAPP_HOME=

LIGHTNING_HTTPPORT=10080
LIGHTNING_HTTPSPORT=10443

LIGHTNING_CONF_DIR=${LIGHTNING_HOME}/lightning/conf
LIGHTNING_LOG_DIR=${LIGHTNING_HOME}/logs

if [ -d "${LIGHTNING_HOME}/java" ]; then
  export JAVA_HOME=${LIGHTNING_HOME}/java
fi

if [ "${JAVA_HOME}" = "" ]; then
if [ -f "/usr/libexec/java_home" ]; then
  export JAVA_HOME=`/usr/libexec/java_home`
fi
fi

if [ "${JAVA_HOME}" = "" ]; then
if [ -f "/usr/lib/jvm/jre" ]; then
  export JAVA_HOME=/usr/lib/jvm/jre
fi
fi

# export jetty.home=${LIGHTNINGAPP_HOME}
# export jetty.base=${LIGHTNINGAPP_HOME}
# export lightning.home=${LIGHTNING_HOME}/lightning
# export jetty.port=${LIGHTNING_HTTPPORT}
# export https.port=${LIGHTNING_HTTPSPORT}

# . "$bin"/lightning-config.sh

# # if no args specified, show usage
if [ $# = 0 ]; then
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
  exit 1
fi

# # get arguments
COMMAND=$1
shift

if [ -f "${LIGHTNING_CONF_DIR}/lightning-env.sh" ]; then
  . "${LIGHTNING_CONF_DIR}/lightning-env.sh"
fi

# # some Java parameters
if [ "${JAVA_HOME}" != "" ]; then
  #echo "run java in ${JAVA_HOME}"
  JAVA_HOME=${JAVA_HOME}
fi
  
if [ "${JAVA_HOME}" = "" ]; then
  echo "Error: JAVA_HOME is not set."
  exit 1
fi

JAVA=${JAVA_HOME}/bin/java
JAVA_HEAP_MAX=-Xmx768m 

# # check envvars which might override default args
if [ "${LIGHTNING_HEAPSIZE}" != "" ]; then
  #echo "run with heapsize ${LIGHTNING_HEAPSIZE}"
  JAVA_HEAP_MAX="-Xmx""${LIGHTNING_HEAPSIZE}""m"
fi
if [ "${JAVA_HEAPSIZE}" != "" ]; then
  #echo "run with heapsize ${JAVA_HEAPSIZE}"
  JAVA_HEAP_MAX="-Xmx""${JAVA_HEAPSIZE}""m"
fi

# # CLASSPATH initially contains $LIGHTNING_CONF_DIR
LIGHTNING_OPTS=
CLASSPATH="${LIGHTNING_CONF_DIR}"
CLASSPATH="${CLASSPATH};${JAVA_HOME}/lib/tools.jar"

# # for developers, add Lightning classes to CLASSPATH
if [ -d "${LIGHTNINGAPP_HOME}/lib" ]; then
  CLASSPATH="${CLASSPATH};${LIGHTNINGAPP_HOME}/lib"
fi
if [ -d "${LIGHTNINGAPP_HOME}/etc" ]; then
  CLASSPATH="${CLASSPATH};${LIGHTNINGAPP_HOME}/etc"
fi
if [ -d "${LIGHTNINGAPP_HOME}/conf" ]; then
  CLASSPATH="${CLASSPATH};${LIGHTNINGAPP_HOME}/conf"
fi

# # add libs to CLASSPATH
for f in ${LIGHTNINGAPP_HOME}/lib/*.jar; do
  CLASSPATH=${CLASSPATH}:$f;
done
for f in ${LIGHTNINGAPP_HOME}/webapps/lightning/WEB-INF/lib/*.jar; do
  CLASSPATH=${CLASSPATH}:$f;
done

# # for releases, add lightning jars & webapps to CLASSPATH
if [ -d "${LIGHTNINGAPP_HOME}/resources" ]; then
  CLASSPATH="${CLASSPATH}:${LIGHTNINGAPP_HOME}/resources"
fi

# # add Lightning release jar to CLASSPATH
for f in ${LIGHTNINGAPP_HOME}/lightning-*.jar; do
  CLASSPATH=${CLASSPATH}:$f;
done


# # default log directory & file
if [ "${LIGHTNING_LOG_DIR}" = "" ]; then
  LIGHTNING_LOG_DIR="${LIGHTNING_HOME}/logs"
fi
if [ "${LIGHTNING_LOGFILE}" = "" ]; then
  LIGHTNING_LOGFILE='lightning.log'
fi

# # cygwin path translation
if $cygwin; then
  CLASSPATH=`cygpath -p -w "${CLASSPATH}"`
  LIGHTNINGAPP_HOME=`cygpath -d "${LIGHTNINGAPP_HOME}"`
  LIGHTNING_LOG_DIR=`cygpath -d "${LIGHTNING_LOG_DIR}"`
fi

# # setup 'java.library.path' for native-hawk code if necessary
JAVA_LIBRARY_PATH=''
if [ -d "${LIGHTNINGAPP_HOME}/build/native" -o -d "${LIGHTNINGAPP_HOME}/lib/native" ]; then
  JAVA_PLATFORM=`CLASSPATH=${CLASSPATH} ${JAVA} org.lightning.util.PlatformName | sed -e "s/ /_/g"`
  
  if [ -d "${LIGHTNINGAPP_HOME}/build/native" ]; then
    JAVA_LIBRARY_PATH="${LIGHTNINGAPP_HOME}/build/native/${JAVA_PLATFORM}/lib"
  fi
  
  if [ -d "${LIGHTNINGAPP_HOME}/lib/native" ]; then
    if [ "x$JAVA_LIBRARY_PATH" != "x" ]; then
      JAVA_LIBRARY_PATH="${JAVA_LIBRARY_PATH}:${LIGHTNINGAPP_HOME}/lib/native/${JAVA_PLATFORM}"
    else
      JAVA_LIBRARY_PATH="${LIGHTNINGAPP_HOME}/lib/native/${JAVA_PLATFORM}"
    fi
  fi
fi

# # cygwin path translation
if $cygwin; then
  JAVA_LIBRARY_PATH=`cygpath -p "${JAVA_LIBRARY_PATH}"`
fi
 
# # restore ordinary behaviour
unset IFS

# # figure out which class to run
if [ "$COMMAND" = "bdb" ] ; then
  LIGHTNING_OPTS="${LIGHTNING_OPTS} -Dbigdb.ruby.sources=${LIGHTNINGAPP_HOME}/lib/ruby"
  CLASS_OPTS="-X+O ${JRUBY_OPTS} ${LIGHTNINGAPP_HOME}/lib/ruby/start.rb"
  CLASS='org.jruby.Main'
elif [ "$COMMAND" = "namenode" ] ; then
  CLASS='org.javenstudio.lightning.util.SimpleNamenode'
elif [ "$COMMAND" = "datanode" ] ; then
  CLASS='org.javenstudio.lightning.util.SimpleDatanode'
elif [ "$COMMAND" = "fs" ] ; then
  CLASS='org.javenstudio.lightning.util.SimpleFsShell'
elif [ "$COMMAND" = "dfs" ] ; then
  CLASS='org.javenstudio.lightning.util.SimpleFsShell'
elif [ "$COMMAND" = "dfsadmin" ] ; then
  CLASS='org.javenstudio.lightning.util.SimpleFsAdmin'
elif [ "$COMMAND" = "jfm" ] ; then
  CLASS='org.javenstudio.lightning.util.SimpleJfm'
elif [ "$COMMAND" = "bigdb" ] ; then
  CLASS='org.javenstudio.lightning.util.SimpleBigdb'
elif [ "$COMMAND" = "version" ] ; then
  CLASS='org.javenstudio.lightning.util.SimpleVersion'
elif [ "$COMMAND" = "fsck" ] ; then
  CLASS='org.javenstudio.lightning.util.SimpleFsck'
elif [ "$COMMAND" = "paxos" ] ; then
  CLASS='org.javenstudio.lightning.util.SimplePaxos'
elif [ "$COMMAND" = "pcli" ] ; then
  CLASS='org.javenstudio.lightning.util.SimplePaxosCli'
elif [ "$COMMAND" = "jar" ] ; then
  CLASS='org.javenstudio.raptor.util.RunJar'
elif [ "$COMMAND" = "distcp" ] ; then
  CLASS='org.javenstudio.raptor.util.CopyFiles'
else
  CLASS=${COMMAND}
fi

if [ "${LIGHTNING_ROOT_LOGGER}" = "" ]; then
  LIGHTNING_ROOT_LOGGER="INFO,console"
fi
if [ "${LIGHTNING_CHARACTERENCODING}" = "" ]; then
  LIGHTNING_CHARACTERENCODING="UTF-8"
fi

# export LIGHTNING_OPTS=
LIGHTNING_OPTS="${LIGHTNING_OPTS} -Dfile.encoding=${LIGHTNING_CHARACTERENCODING}"
LIGHTNING_OPTS="${LIGHTNING_OPTS} -Dsun.jnu.encoding=${LIGHTNING_CHARACTERENCODING}"
LIGHTNING_OPTS="${LIGHTNING_OPTS} -Djetty.home=${LIGHTNINGAPP_HOME}"
LIGHTNING_OPTS="${LIGHTNING_OPTS} -Djetty.base=${LIGHTNINGAPP_HOME}"
LIGHTNING_OPTS="${LIGHTNING_OPTS} -Dlightning.home=${LIGHTNING_HOME}/lightning"
LIGHTNING_OPTS="${LIGHTNING_OPTS} -Djetty.port=${LIGHTNING_HTTPPORT}"
LIGHTNING_OPTS="${LIGHTNING_OPTS} -Dhttps.port=${LIGHTNING_HTTPSPORT}"
LIGHTNING_OPTS="${LIGHTNING_OPTS} -Djava.util.logging.config.file=${LIGHTNINGAPP_HOME}/etc/logging.properties"
LIGHTNING_OPTS="${LIGHTNING_OPTS} -Dorg.javenstudio.common.util.logger.debug=${LIGHTNING_DEBUG}"

if [ "x$JAVA_LIBRARY_PATH" != "x" ]; then
  LIGHTNING_OPTS="${LIGHTNING_OPTS} -Djava.library.path=${JAVA_LIBRARY_PATH}"
fi

# # run it
exec ${JAVA} ${JAVA_HEAP_MAX} ${LIGHTNING_OPTS} -classpath "${CLASSPATH}" ${CLASS} ${CLASS_OPTS} "$@"
