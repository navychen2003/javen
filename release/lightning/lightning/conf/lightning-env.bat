REM Set Lightning-specific environment variables here.

REM The only required environment variable is JAVA_HOME.  All others are
REM optional.  When running a distributed configuration it is best to
REM set JAVA_HOME in this file, so that it is correctly defined on
REM remote nodes.

REM The java implementation to use.  Required.
REM set JAVA_HOME="C:\Program Files\Java\jdk1.8.0"

REM The maximum amount of heap to use, in MB. Default is 1000.
REM export LIGHTNING_HEAPSIZE=2000
set LIGHTNING_HEAPSIZE=1024

REM The jetty http/https service port
set LIGHTNING_HTTPPORT=10080
set LIGHTNING_HTTPSPORT=10443
