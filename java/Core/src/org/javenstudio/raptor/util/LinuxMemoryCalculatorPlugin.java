package org.javenstudio.raptor.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.javenstudio.common.util.Logger;

/**
 * Plugin to calculate virtual and physical memories on Linux systems.
 */
public class LinuxMemoryCalculatorPlugin extends MemoryCalculatorPlugin {
  private static final Logger LOG = Logger.getLogger(LinuxMemoryCalculatorPlugin.class);

  /**
   * proc's meminfo virtual file has keys-values in the format
   * "key:[ \t]*value[ \t]kB".
   */
  private static final String PROCFS_MEMFILE = "/proc/meminfo";
  private static final Pattern PROCFS_MEMFILE_FORMAT =
      Pattern.compile("^([a-zA-Z]*):[ \t]*([0-9]*)[ \t]kB");

  // We just need the values for the keys MemTotal and SwapTotal
  private static final String MEMTOTAL_STRING = "MemTotal";
  private static final String SWAPTOTAL_STRING = "SwapTotal";

  private long ramSize = 0;
  private long swapSize = 0;

  boolean readMemInfoFile = false;

  private void readProcMemInfoFile() {

    if (readMemInfoFile) {
      return;
    }

    // Read "/proc/memInfo" file
    BufferedReader in = null;
    FileReader fReader = null;
    try {
      fReader = new FileReader(PROCFS_MEMFILE);
      in = new BufferedReader(fReader);
    } catch (FileNotFoundException f) {
      // shouldn't happen....
      return;
    }

    Matcher mat = null;

    try {
      String str = in.readLine();
      while (str != null) {
        mat = PROCFS_MEMFILE_FORMAT.matcher(str);
        if (mat.find()) {
          if (mat.group(1).equals(MEMTOTAL_STRING)) {
            ramSize = Long.parseLong(mat.group(2));
          } else if (mat.group(1).equals(SWAPTOTAL_STRING)) {
            swapSize = Long.parseLong(mat.group(2));
          }
        }
        str = in.readLine();
      }
    } catch (IOException io) {
      LOG.warn("Error reading the stream " + io);
    } finally {
      // Close the streams
      try {
        fReader.close();
        try {
          in.close();
        } catch (IOException i) {
          LOG.warn("Error closing the stream " + in);
        }
      } catch (IOException i) {
        LOG.warn("Error closing the stream " + fReader);
      }
    }

    readMemInfoFile = true;
  }

  /** {@inheritDoc} */
  @Override
  public long getPhysicalMemorySize() {
    readProcMemInfoFile();
    return ramSize * 1024;
  }

  /** {@inheritDoc} */
  @Override
  public long getVirtualMemorySize() {
    readProcMemInfoFile();
    return (ramSize + swapSize) * 1024;
  }

  /**
   * Test the {@link LinuxMemoryCalculatorPlugin}
   * 
   * @param args
   */
  public static void main(String[] args) {
    LinuxMemoryCalculatorPlugin plugin = new LinuxMemoryCalculatorPlugin();
    System.out.println("Physical memory Size(bytes) : "
        + plugin.getPhysicalMemorySize());
    System.out.println("Total Virtual memory Size(bytes) : "
        + plugin.getVirtualMemorySize());
  }
}
