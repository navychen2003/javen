package org.javenstudio.raptor.metrics;

import java.util.Map; 
import java.util.Properties; 
import java.util.Iterator; 
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.net.DNS; 


/**
 * Utility class to simplify creation and reporting of hawk metrics.
 *
 * For examples of usage, see NameNodeMetrics.
 * @see org.javenstudio.raptor.metrics.MetricsRecord
 * @see org.javenstudio.raptor.metrics.MetricsContext
 * @see org.javenstudio.raptor.metrics.ContextFactory
 */
public class MetricsUtil {
  public static final Logger LOG = Logger.getLogger(MetricsUtil.class);

  /**
   * Don't allow creation of a new instance of Metrics
   */
  private MetricsUtil() {}
    
  public static MetricsContext getContext(String contextName) {
    return getContext(contextName, contextName);
  }

  /**
   * Utility method to return the named context.
   * If the desired context cannot be created for any reason, the exception
   * is logged, and a null context is returned.
   */
  public static MetricsContext getContext(String refName, String contextName) {
    MetricsContext metricsContext;
    try {
      metricsContext =
        ContextFactory.getFactory().getContext(refName, contextName);
      if (!metricsContext.isMonitoring()) {
        metricsContext.startMonitoring();
      }
    } catch (Exception ex) {
      LOG.error("Unable to create metrics context " + contextName, ex);
      metricsContext = ContextFactory.getNullContext(contextName);
    }
    return metricsContext;
  }

  /**
   * Utility method to create and return new metrics record instance within the
   * given context. This record is tagged with the host name.
   *
   * @param context the context
   * @param recordName name of the record
   * @return newly created metrics record
   */
  public static MetricsRecord createRecord(MetricsContext context, 
                                           String recordName) 
  {
    MetricsRecord metricsRecord = context.createRecord(recordName);
    metricsRecord.setTag("hostName", getHostName());
    return metricsRecord;        
  }
    
  /**
   * Returns the host name.  If the host name is unobtainable, logs the
   * exception and returns "unknown".
   */
  private static String getHostName() {
    String hostName = null;
    try {
      hostName = InetAddress.getLocalHost().getHostName();
    } 
    catch (UnknownHostException ex) {
      LOG.info("Unable to obtain hostName", ex);
      hostName = "unknown";
    }
    return hostName;
  }

  @SuppressWarnings("rawtypes")
  public static void printSystemEnv() {
    Map<String,String> envs = System.getenv();
    for (Iterator it = envs.keySet().iterator(); it.hasNext();) {
      String key = (String) it.next();
      String val = (String) envs.get(key);
      LOG.info("Env '" + key + "' is " + val);
    }
    Properties p = System.getProperties();
    for (Iterator it = p.keySet().iterator(); it.hasNext();) {
      String key = (String) it.next();
      String val = (String) p.getProperty(key);
      LOG.info("Property '" + key + "' is " + val);
    }
    try {
      LOG.info("Local HostName: "+DNS.getLocalHost());
    } catch (Exception e) {
      LOG.error(e.toString(), e);
      //e.printStackTrace(LogUtil.getWarnStream(LOG));
    }
  }

}
