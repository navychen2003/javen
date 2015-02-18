package org.javenstudio.raptor.util;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.conf.Configuration;

/**
 * A helper to load the native raptor code i.e. libraptor.so.
 * This handles the fallback to either the bundled libraptor-Linux-i386-32.so
 * or the default java implementations where appropriate.
 *  
 */
public class NativeCodeLoader {
  private static final Logger LOG = Logger.getLogger(NativeCodeLoader.class);
  
  private static boolean nativeCodeLoaded = false;
  
  static {
    // Try to load native raptor library and set fallback flag appropriately
    LOG.debug("Trying to load the custom-built native-raptor library...");
    try {
      System.loadLibrary("raptor");
      LOG.info("Loaded the native-raptor library");
      nativeCodeLoaded = true;
    } catch (Throwable t) {
      // Ignore failure to load
      LOG.debug("Failed to load native-raptor with error: " + t);
      LOG.debug("java.library.path=" + System.getProperty("java.library.path"));
    }

    if (!nativeCodeLoaded) {
      LOG.warn("Unable to load native-raptor library for your platform... " +
               "using builtin-java classes where applicable");
    }
  }

  /**
   * Check if native-raptor code is loaded for this platform.
   * 
   * @return <code>true</code> if native-raptor is loaded, 
   *         else <code>false</code>
   */
  public static boolean isNativeCodeLoaded() {
    return nativeCodeLoaded;
  }

  /**
   * Return if native raptor libraries, if present, can be used for this job.
   * @param conf configuration
   * 
   * @return <code>true</code> if native raptor libraries, if present, can be 
   *         used for this job; <code>false</code> otherwise.
   */
  public boolean getLoadNativeLibraries(Configuration conf) {
    return conf.getBoolean("raptor.native.lib", true);
  }
  
  /**
   * Set if native raptor libraries, if present, can be used for this job.
   * 
   * @param conf configuration
   * @param loadNativeLibraries can native raptor libraries be loaded
   */
  public void setLoadNativeLibraries(Configuration conf, 
                                     boolean loadNativeLibraries) {
    conf.setBoolean("raptor.native.lib", loadNativeLibraries);
  }

}

