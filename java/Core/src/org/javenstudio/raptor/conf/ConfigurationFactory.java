package org.javenstudio.raptor.conf;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.javenstudio.raptor.util.InputSource;


/** Utility to create Hawk {@link Configuration}s that include Hawk-specific
 * resources.  */
public class ConfigurationFactory {
  //private final static String KEY = ConfigurationFactory.class.getName();
  
  protected ConfigurationFactory() {}                 // singleton

  private static boolean quietmode = false;
  private static Configuration conf = null; 

  /** Create a {@link Configuration} for Hawk. */
  public static Configuration create() {
    return create(quietmode); 
  }

  public synchronized static Configuration create(boolean quiet) {
    if (conf == null) 
      conf = new Configuration(quiet);
    setQuietMode(quiet); 
    return conf;
  }

  public static Configuration create(String source) {
	  return create(source, quietmode);
  }
  
  public static Configuration create(final String source, boolean quiet) {
	  return create(new InputSource() {
			@Override
			public InputStream openStream() throws IOException {
				return new ByteArrayInputStream(source.getBytes("UTF-8"));
			}
		  }, quiet);
  }
  
  public synchronized static Configuration create(InputSource source, boolean quiet) {
    if (conf == null) {
	  conf = new Configuration((InputSource)null, quiet);
      conf.addResource(source);
    }
    setQuietMode(quiet); 
    return conf;
  }
  
  public synchronized static Configuration create(Configuration conf) {
    if (conf == null) return create(); 
    return new Configuration(conf); 
  }

  public static Configuration get() {
    return get(false); 
  }

  public synchronized static Configuration get(boolean create) {
    if (conf == null && create) create(); 
	if (conf == null)
	  throw new NullPointerException("Configuration not initailized");
    return conf; 
  }

  public synchronized static void setQuietMode(boolean quiet) {
    quietmode = quiet; 
  }

  public static boolean isQuietMode() {
    return quietmode; 
  }

  /**
   * Create a {@link Configuration} for Hawk front-end.
   *
   * If a {@link Configuration} is found in the
   * {@link javax.servlet.ServletContext} it is simply returned, otherwise,
   * a new {@link Configuration} is created using the {@link #create()} method,
   * and then all the init parameters found in the
   * {@link javax.servlet.ServletContext} are added to the {@link Configuration}
   * (the created {@link Configuration} is then saved into the
   * {@link javax.servlet.ServletContext}).
   *
   * @param application is the ServletContext whose init parameters
   *        must override those of Hawk.
   */
  //public static Configuration get(ServletContext application) {
  //  Configuration conf = (Configuration) application.getAttribute(KEY);
  //  if (conf == null) {
  //    conf = create();
  //    Enumeration e = application.getInitParameterNames();
  //    while (e.hasMoreElements()) {
  //      String name = (String) e.nextElement();
  //      conf.set(name, application.getInitParameter(name));
  //    }
  //    application.setAttribute(KEY, conf);
  //  }
  //  return conf;
  //}

  public static Configuration getSimple() { 
	  return create(SIMPLE_CONFIGURATION_XML);
  }
  
  public static final String SIMPLE_CONFIGURATION_XML = "<?xml version=\"1.0\"?>" + 
			"<configuration>" + 
			"  <category name=\"filesystem\">" +
			"    <property>" + 
			"      <name>fs.default.name</name>" +
			"      <value>file:///</value>" + 
			"    </property>" +
			"    <property>" + 
			"      <name>fs.file.impl</name>" + 
			"      <value>org.javenstudio.raptor.fs.LocalFileSystem</value>" + 
			"    </property>" +
			"  </category>" +
			"</configuration>";
  
}
