package org.javenstudio.raptor.util;

import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.conf.Configured;

/**
 * Plugin to calculate virtual and physical memories on the system.
 * 
 */
public abstract class MemoryCalculatorPlugin extends Configured {

  /**
   * Obtain the total size of the virtual memory present in the system.
   * 
   * @return virtual memory size in bytes.
   */
  public abstract long getVirtualMemorySize();

  /**
   * Obtain the total size of the physical memory present in the system.
   * 
   * @return physical memory size bytes.
   */
  public abstract long getPhysicalMemorySize();

  /**
   * Get the MemoryCalculatorPlugin from the class name and configure it. If
   * class name is null, this method will try and return a memory calculator
   * plugin available for this system.
   * 
   * @param clazz class-name
   * @param conf configure the plugin with this.
   * @return MemoryCalculatorPlugin
   */
  public static MemoryCalculatorPlugin getMemoryCalculatorPlugin(
      Class<? extends MemoryCalculatorPlugin> clazz, Configuration conf) {

    if (clazz != null) {
      return ReflectionUtils.newInstance(clazz, conf);
    }

    // No class given, try a os specific class
    try {
      String osName = System.getProperty("os.name");
      if (osName.startsWith("Linux")) {
        return new LinuxMemoryCalculatorPlugin();
      }
    } catch (SecurityException se) {
      // Failed to get Operating System name.
      return null;
    }

    // Not supported on this system.
    return null;
  }
}
