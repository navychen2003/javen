package org.javenstudio.raptor.metrics.file;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.javenstudio.raptor.metrics.ContextFactory;
import org.javenstudio.raptor.metrics.MetricsException;
import org.javenstudio.raptor.metrics.spi.AbstractMetricsContext;
import org.javenstudio.raptor.metrics.spi.OutputRecord;


/**
 * Metrics context for writing metrics to a file.<p/>
 *
 * This class is configured by setting ContextFactory attributes which in turn
 * are usually configured through a properties file.  All the attributes are
 * prefixed by the contextName. For example, the properties file might contain:
 * <pre>
 * myContextName.fileName=/tmp/metrics.log
 * myContextName.period=5
 * </pre>
 */
public class FileContext extends AbstractMetricsContext {
    
  /* Configuration attribute names */
  protected static final String FILE_NAME_PROPERTY = "fileName";
  protected static final String PERIOD_PROPERTY = "period";
    
  private File file = null;              // file for metrics to be written to
  private PrintWriter writer = null;
    
  /** Creates a new instance of FileContext */
  public FileContext() {}
    
  public void init(String contextName, ContextFactory factory) {
    super.init(contextName, factory);
        
    String fileName = getAttribute(FILE_NAME_PROPERTY);
    if (fileName != null) {
      file = new File(fileName);
    }
        
    String periodStr = getAttribute(PERIOD_PROPERTY);
    if (periodStr != null) {
      int period = 0;
      try {
        period = Integer.parseInt(periodStr);
      } catch (NumberFormatException nfe) {
      }
      if (period <= 0) {
        throw new MetricsException("Invalid period: " + periodStr);
      }
      setPeriod(period);
    }
  }

  /**
   * Returns the configured file name, or null.
   */
  public String getFileName() {
    if (file == null) {
      return null;
    } else {
      return file.getName();
    }
  }
    
  /**
   * Starts or restarts monitoring, by opening in append-mode, the
   * file specified by the <code>fileName</code> attribute,
   * if specified. Otherwise the data will be written to standard
   * output.
   */
  public void startMonitoring()
    throws IOException 
  {
    if (file == null) {
      writer = new PrintWriter(new BufferedOutputStream(System.out));
    } else {
      writer = new PrintWriter(new FileWriter(file, true));
    }
    super.startMonitoring();
  }
    
  /**
   * Stops monitoring, closing the file.
   * @see #close()
   */
  public void stopMonitoring() {
    super.stopMonitoring();
        
    if (writer != null) {
      writer.close();
      writer = null;
    }
  }
    
  /**
   * Emits a metrics record to a file.
   */
  public void emitRecord(String contextName, String recordName, OutputRecord outRec) {
    writer.print(contextName);
    writer.print(".");
    writer.print(recordName);
    String separator = ": ";
    for (String tagName : outRec.getTagNames()) {
      writer.print(separator);
      separator = ", ";
      writer.print(tagName);
      writer.print("=");
      writer.print(outRec.getTag(tagName));
    }
    for (String metricName : outRec.getMetricNames()) {
      writer.print(separator);
      separator = ", ";
      writer.print(metricName);
      writer.print("=");
      writer.print(outRec.getMetric(metricName));
    }
    writer.println();
  }
    
  /**
   * Flushes the output writer, forcing updates to disk.
   */
  public void flush() {
    writer.flush();
  }
}
