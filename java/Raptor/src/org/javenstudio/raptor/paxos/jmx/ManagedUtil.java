package org.javenstudio.raptor.paxos.jmx;

import javax.management.JMException;

/**
 * Shared utilities
 */
public class ManagedUtil {
    /**
     * Register the log4j JMX mbeans. Set environment variable
     * "raptor.jmx.log4j.disable" to true to disable registration.
     * @see http://logging.apache.org/log4j/1.2/apidocs/index.html?org/apache/log4j/jmx/package-summary.html
     * @throws JMException if registration fails
     */
    public static void registerLog4jMBeans() throws JMException {
        if (Boolean.getBoolean("raptor.jmx.log4j.disable") == true) {
            return;
        }
        
        //MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        // Create and Register the top level Log4J MBean
        //HierarchyDynamicMBean hdm = new HierarchyDynamicMBean();

        //ObjectName mbo = new ObjectName("log4j:hiearchy=default");
        //mbs.registerMBean(hdm, mbo);

        // Add the root logger to the Hierarchy MBean
        //Logger rootLogger = Logger.getRootLogger();
        //hdm.addLoggerMBean(rootLogger.getName());

        // Get each logger from the Log4J Repository and add it to
        // the Hierarchy MBean created above.
        //LoggerRepository r = LogManager.getLoggerRepository();
        //Enumeration enumer = r.getCurrentLoggers();
        //Logger logger = null;

        //while (enumer.hasMoreElements()) {
        //   logger = (Logger) enumer.nextElement();
        //   hdm.addLoggerMBean(logger.getName());
        //}
    }

}

