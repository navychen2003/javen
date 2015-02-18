package org.javenstudio.raptor.util;

import java.io.File; 
import java.util.Locale;
import java.util.StringTokenizer; 

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.VersionAnnotation;


/**
 * This class finds the package info for Hawk and the VersionAnnotation
 * information.
 * @author Owen O'Malley
 */
public class VersionInfo {
  public static final Logger LOG = Logger.getLogger(VersionInfo.class);

  private static Package myPackage;
  private static VersionAnnotation version;
  private static boolean debugmode = false; 
  
  static {
    myPackage = VersionAnnotation.class.getPackage();
    version = myPackage.getAnnotation(VersionAnnotation.class);
    try {
      String val = System.getProperty("lightning.debug"); 
      if (val == null) val = System.getenv("LIGHTNING_DEBUG"); 
      if (val != null) debugmode = Boolean.valueOf(val); 
    } catch (Exception e) {
      debugmode = false; 
    }
  }

  public static void setVersion(VersionAnnotation v) { 
	if (v != null) version = v;
  }
  
  public static boolean isDebugMode() {
    return debugmode; 
  }

  /**
   * Get the meta-data for the Hawk package.
   * @return
   */
  static Package getPackage() {
    return myPackage;
  }
  
  /**
   * Get the Hawk version.
   * @return the Hawk version string, eg. "0.6.3-dev"
   */
  public static String getVersion() {
    return version != null ? version.version() : "Unknown";
  }
  
  /**
   * Get the subversion revision number for the root directory
   * @return the revision number, eg. "451451"
   */
  public static String getRevision() {
    return version != null ? version.revision() : "Unknown";
  }
  
  /**
   * The date that Hawk was compiled.
   * @return the compilation date in unix date format
   */
  public static String getDate() {
    return version != null ? version.date() : "Unknown";
  }
  
  /**
   * The user that compiled Hawk.
   * @return the username of the user
   */
  public static String getUser() {
    return version != null ? version.user() : "Unknown";
  }
  
  /**
   * Get the subversion URL for the root Hawk directory.
   */
  public static String getUrl() {
    return version != null ? version.url() : "Unknown";
  }

  /**
   * Get the javac version when compile sources.
   */
  public static String getJavacVersion() {
    return version != null ? version.javacversion() : "Unknown";
  }

  public static int[] getJavaVersionNum(String version) {
    if (version == null || version.length() == 0)
      return null; 

    int minVer = -1, maxVer = -1; 

    StringTokenizer st = new StringTokenizer(version, " \t\r\n._-"); 
    while (st.hasMoreTokens()) {
      String token = st.nextToken(); 
      if (maxVer == -1) {
        try { maxVer = Integer.valueOf(token).intValue(); } 
        catch (Exception e) {} 
      } else if (minVer == -1) {
        try { minVer = Integer.valueOf(token).intValue(); } 
        catch (Exception e) {} 
      } else
        break; 
    }

    if (minVer < 0 || maxVer < 0)
      return null; 

    return new int[] {maxVer, minVer}; 
  }

  public static String getVersionMessage() {
    return checkJavaVersion(true); 
  }

  public static String checkJavaVersion() {
    return checkJavaVersion(false); 
  }

  private static String versionMessage = null; 
  private static Object lockVersion = new Object(); 

  public static String checkJavaVersion(boolean warn) {
    if (versionMessage != null) return versionMessage; 

    synchronized (lockVersion) {
      versionMessage = doCheckJavaVersion(warn);
    }

    return versionMessage; 
  }

  private static String doCheckJavaVersion(boolean warn) {
    String javacVer = StringUtils.trim(getJavacVersion()); 
    String javaVer = System.getProperty("java.version"); 
    if (javacVer == null || javacVer.length() == 0 ||
        javaVer == null || javaVer.length() == 0) {
      LOG.warn("Cannot get javac ("+javacVer+") or java ("+javaVer+") version."); 
      return null; 
    }

    int[] javacVerNum = getJavaVersionNum(javacVer); 
    int[] javaVerNum = getJavaVersionNum(javaVer); 

    if (javacVerNum == null || javacVerNum.length != 2 ||
        javaVerNum == null || javaVerNum.length != 2) {
      LOG.warn("Cannot get version from javac "+javacVer+" and java "+javaVer); 
      return null; 
    }

    String message = null; 
    if (javaVerNum[0] < javacVerNum[0] || javaVerNum[1] < javacVerNum[1]) {
      message = "Java version ("+javaVer+") is too older than compile version ("+javacVer+")."; 
    }

    if (message != null) {
      if (warn) {
        LOG.warn(message); 
      } else {
        LOG.fatal(message); throw new RuntimeException(message); 
      }
    }

    return message; 
  }

  public static String getCompileInfo() {
    StringBuffer sbuf = new StringBuffer(); 
    sbuf.append("Raptor " + getVersion()); 
    sbuf.append(", Subversion -r " + getRevision());
    sbuf.append(", Compiled by " + getUser());
    sbuf.append(" with javac " + getJavacVersion()); 
    sbuf.append(" on " + getDate());
    return sbuf.toString(); 
  }

  public static String getAbsolutePath(String path) {
    try {
      File file = new File(path); 
      return file.getAbsolutePath(); 
    } catch (Exception e) {
      return path; 
    }
  }

  public static String getHomeDir() {
    StringBuffer sbuf = new StringBuffer();
    sbuf.append(getAbsolutePath(System.getProperty("lightning.home.dir")));
    return sbuf.toString();
  }

  public static String getConfDir() {
    StringBuffer sbuf = new StringBuffer();
    sbuf.append(getAbsolutePath(System.getProperty("lightning.conf.dir")));
    return sbuf.toString();
  }

  public static String getLogDir() {
    StringBuffer sbuf = new StringBuffer();
    sbuf.append(getAbsolutePath(System.getProperty("lightning.log.dir")));
    return sbuf.toString();
  }

  public static String getUserName() {
    StringBuffer sbuf = new StringBuffer(); 
    sbuf.append(System.getProperty("user.name")); 
    return sbuf.toString(); 
  }

  public static String getUserInfo() {
    StringBuffer sbuf = new StringBuffer(); 
    sbuf.append(System.getProperty("user.name")); 
    sbuf.append(" at "); 
    sbuf.append(System.getProperty("user.home")); 
    return sbuf.toString(); 
  }

  public static String getOSInfo() {
    StringBuffer sbuf = new StringBuffer(); 
    sbuf.append(System.getProperty("os.name")); 
    sbuf.append(" "); 
    sbuf.append(System.getProperty("os.version")); 
    sbuf.append(" "); 
    sbuf.append(System.getProperty("os.arch")); 
    return sbuf.toString(); 
  }

  public static String getJavaInfo() {
    StringBuffer sbuf = new StringBuffer(); 
    sbuf.append("Java "); 
    sbuf.append(System.getProperty("java.version")); 
    sbuf.append(" at "); 
    sbuf.append(System.getProperty("java.home")); 
    sbuf.append(", "); 
    sbuf.append(System.getProperty("java.runtime.name")); 
    sbuf.append(" "); 
    sbuf.append(System.getProperty("java.runtime.version")); 
    sbuf.append(", "); 
    sbuf.append(System.getProperty("java.vm.name")); 
    sbuf.append(" "); 
    sbuf.append(System.getProperty("java.vm.version")); 
    sbuf.append(", "); 
    sbuf.append(System.getProperty("java.vm.vendor")); 
    return sbuf.toString(); 
  }
  
  public static String getCharacterEncoding() {
    String encoding = System.getProperty("file.encoding"); 
    if (encoding == null || encoding.length() == 0) 
      encoding = System.getProperty("sun.jnu.encoding"); 
    if (encoding == null || encoding.length() == 0) 
      encoding = "UTF-8"; 
    return encoding; 
  }

  public static Locale getLocale() {
    return StringManager.createLocale(getLocaleString()); 
  }

  public static String getLocaleString() {
    String language = System.getProperty("user.language"); 
    String country = System.getProperty("user.country"); 
    if (language == null || language.length() == 0) 
      language = "en"; 
    if (country == null || country.length() == 0) 
      country = "US"; 
    return language + "_" + country; 
  }

  public static void main(String[] args) {
    checkJavaVersion(); 
    System.out.println(getVersionInfo()); 
  }

  /**
   * Returns the buildVersion which includes version, 
   * revision, user and date. 
   */
  public static String getBuildVersion(){
    return VersionInfo.getVersion() + 
           " from " + VersionInfo.getRevision() +
           " by " + VersionInfo.getUser() + 
           " on " + VersionInfo.getDate();
  }

  public static String getVersionInfo() {
    StringBuffer sbuf = new StringBuffer(); 
    sbuf.append("Hawk " + getVersion());
    sbuf.append("\nSubversion " + getUrl() + " -r " + getRevision());
    sbuf.append("\nCompiled by " + getUser() + " with javac " + getJavacVersion() + " on " + getDate());
    return sbuf.toString(); 
  }
}
