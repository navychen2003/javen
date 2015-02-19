# Set Lightning-specific environment variables here.

# The only required environment variable is JAVA_HOME.  All others are
# optional.  When running a distributed configuration it is best to
# set JAVA_HOME in this file, so that it is correctly defined on
# remote nodes.

# The java implementation to use.  Required.
# export JAVA_HOME=/usr/lib/jvm/jre

# The maximum amount of heap to use, in MB. Default is 1000.
# export LIGHTNING_HEAPSIZE=2000
export LIGHTNING_HEAPSIZE=1024

# The jetty http/https service port
export LIGHTNING_HTTPPORT=10080
export LIGHTNING_HTTPSPORT=10443
