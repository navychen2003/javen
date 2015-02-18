package org.javenstudio.raptor.util;


/**
 * A helper class for getting build-info of the java-vm. 
 * 
 * @author Arun C Murthy
 */
public class PlatformName {
  /**
   * The complete platform 'name' to identify the platform as 
   * per the java-vm.
   */
  private static final String platformName = System.getProperty("os.name") + "-" + 
    System.getProperty("os.arch") + "-" +
    System.getProperty("sun.arch.data.model");
  
  /**
   * Get the complete platform as per the java-vm.
   * @return returns the complete platform as per the java-vm.
   */
  public static String getPlatformName() {
    return platformName;
  }
  
  public static void main(String[] args) {
    System.out.println(platformName);
  }
}
