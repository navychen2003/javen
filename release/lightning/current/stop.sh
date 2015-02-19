#!/bin/sh
#
# The Lightning command script
#
# Environment Variables
#
#   JAVA_HOME        The java implementation to use.  Overrides JAVA_HOME.
# 

bin=`dirname "$0"`
bin=`cd "$bin"; pwd`

cygwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
esac

# # the root of the installation
DEFAULT_LIGHTNINGAPP_HOME=${bin}

LIGHTNINGAPP_HOME=${DEFAULT_LIGHTNINGAPP_HOME}
LIGHTNING_HOME=${DEFAULT_LIGHTNINGAPP_HOME}/..

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

if [ -f "${LIGHTNING_CONF_DIR}/lightning-env.sh" ]; then
  . "${LIGHTNING_CONF_DIR}/lightning-env.sh"
fi

# export jetty.home=${LIGHTNINGAPP_HOME}
# export jetty.base=${LIGHTNINGAPP_HOME}
# export lightning.home=${LIGHTNING_HOME}/lightning
# export jetty.port=${LIGHTNING_HTTPPORT}
# export https.port=${LIGHTNING_HTTPSPORT}

if [ "${JAVA_HOME}" = "" ]; then
  echo "Error: JAVA_HOME is not set."
  exit 1
fi

# WRITELOCK_FILE=${LIGHTNING_HOME}/lightning/index/data/index/write.lock
# if exist "%WRITELOCK_FILE%" del "%WRITELOCK_FILE%"

# # for developers, add Lightning classes to CLASSPATH
if [ -d "${LIGHTNINGAPP_HOME}/lib" ]; then
  CLASSPATH="${CLASSPATH};${LIGHTNINGAPP_HOME}/lib"
fi
if [ -d "${LIGHTNINGAPP_HOME}/conf" ]; then
  CLASSPATH="${CLASSPATH};${LIGHTNINGAPP_HOME}/conf"
fi

if [ "${LIGHTNING_ROOT_LOGGER}" = "" ]; then
  LIGHTNING_ROOT_LOGGER="INFO,console"
fi
if [ "${LIGHTNING_CHARACTERENCODING}" = "" ]; then
  LIGHTNING_CHARACTERENCODING="UTF-8"
fi

# export PATH=${JAVA_HOME}/bin;${PATH}

STOP_OPTS="-DSTOP.PORT=10089 -DSTOP.KEY=secret"
START_OPTS="-jar ${LIGHTNINGAPP_HOME}/start.jar ${LIGHTNINGAPP_HOME}/etc/jetty.xml --stop"

JAVA_OPTS=""
JAVA_OPTS="${JAVA_OPTS} -Dfile.encoding=${LIGHTNING_CHARACTERENCODING}"
JAVA_OPTS="${JAVA_OPTS} -Dsun.jnu.encoding=${LIGHTNING_CHARACTERENCODING}"
JAVA_OPTS="${JAVA_OPTS} -Djetty.home=${LIGHTNINGAPP_HOME}"
JAVA_OPTS="${JAVA_OPTS} -Djetty.base=${LIGHTNINGAPP_HOME}"
JAVA_OPTS="${JAVA_OPTS} -Dlightning.home=${LIGHTNING_HOME}/lightning"
JAVA_OPTS="${JAVA_OPTS} -Djetty.port=${LIGHTNING_HTTPPORT}"
JAVA_OPTS="${JAVA_OPTS} -Dhttps.port=${LIGHTNING_HTTPSPORT}"

JAVA=${JAVA_HOME}/bin/java
JAVA_HEAP_MAX=-Xmx768m 

# # check envvars which might override default args
if [ "${LIGHTNING_HEAPSIZE}" != "" ]; then
  #echo "run with heapsize ${LIGHTNING_HEAPSIZE}"
  JAVA_HEAP_MAX="-Xmx""${LIGHTNING_HEAPSIZE}""m"
fi

if [ "$1" = "-debug" ]; then
  JAVA_OPTS="${JAVA_OPTS} -Djava.util.logging.config.file=${LIGHTNINGAPP_HOME}/etc/loggingdebug.properties"
  JAVA_OPTS="${JAVA_OPTS} -Dorg.javenstudio.common.util.logger.debug=true"
  
  echo ${JAVA} ${JAVA_HEAP_MAX} ${JAVA_OPTS} ${STOP_OPTS} ${START_OPTS} "$@"
  exec ${JAVA} ${JAVA_HEAP_MAX} ${JAVA_OPTS} ${STOP_OPTS} ${START_OPTS} "$@"
else
  JAVA_OPTS="${JAVA_OPTS} -Djava.util.logging.config.file=${LIGHTNINGAPP_HOME}/etc/logging.properties"
  JAVA_OPTS="${JAVA_OPTS} -Dorg.javenstudio.common.util.logger.debug=false"
  
  exec ${JAVA} ${JAVA_HEAP_MAX} ${JAVA_OPTS} ${STOP_OPTS} ${START_OPTS} "$@"
fi

WRITELOCK_FILE=${LIGHTNING_HOME}/lightning/index/data/index/write.lock
if [ -d "${WRITELOCK_FILE}" ]; then
  rm -f "${WRITELOCK_FILE}"
fi
