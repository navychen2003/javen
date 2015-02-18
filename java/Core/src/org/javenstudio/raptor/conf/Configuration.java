package org.javenstudio.raptor.conf;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.io.FileWriter; 
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.TreeMap; 
import java.util.TreeSet; 
import java.util.Collections; 

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;
import org.javenstudio.raptor.util.InputSource;
import org.javenstudio.raptor.util.StringUtils;


/** 
 * Provides access to configuration parameters.
 *
 * <h4 id="Resources">Resources</h4>
 *
 * <p>Configurations are specified by resources. A resource contains a set of
 * name/value pairs as XML data. Each resource is named by either a 
 * <code>String</code> or by a {@link Path}. If named by a <code>String</code>, 
 * then the classpath is examined for a file with that name.  If named by a 
 * <code>Path</code>, then the local filesystem is examined directly, without 
 * referring to the classpath.
 *
 * <p>Hawk by default specifies two resources, loaded in-order from the
 * classpath: <ol>
 * <li><tt><a href="{@docRoot}/../hawk-default.html">hawk-default.xml</a>
 * </tt>: Read-only defaults for hawk.</li>
 * <li><tt>hawk-site.xml</tt>: Site-specific configuration for a given hawk
 * installation.</li>
 * </ol>
 * Applications may add additional resources, which are loaded
 * subsequent to these resources in the order they are added.
 * 
 * <h4 id="FinalParams">Final Parameters</h4>
 *
 * <p>Configuration parameters may be declared <i>final</i>. 
 * Once a resource declares a value final, no subsequently-loaded 
 * resource can alter that value.  
 * For example, one might define a final parameter with:
 * <tt><pre>
 *  &lt;property&gt;
 *    &lt;name&gt;dfs.client.buffer.dir&lt;/name&gt;
 *    &lt;value&gt;/tmp/hawk/dfs/client&lt;/value&gt;
 *    <b>&lt;final&gt;true&lt;/final&gt;</b>
 *  &lt;/property&gt;</pre></tt>
 *
 * Administrators typically define parameters as final in 
 * <tt>hawk-site.xml</tt> for values that user applications may not alter.
 *
 * <h4 id="VariableExpansion">Variable Expansion</h4>
 *
 * <p>Value strings are first processed for <i>variable expansion</i>. The
 * available properties are:<ol>
 * <li>Other properties defined in this Configuration; and, if a name is
 * undefined here,</li>
 * <li>Properties in {@link System#getProperties()}.</li>
 * </ol>
 *
 * <p>For example, if a configuration resource contains the following property
 * definitions: 
 * <tt><pre>
 *  &lt;property&gt;
 *    &lt;name&gt;basedir&lt;/name&gt;
 *    &lt;value&gt;/user/${<i>user.name</i>}&lt;/value&gt;
 *  &lt;/property&gt;
 *  
 *  &lt;property&gt;
 *    &lt;name&gt;tempdir&lt;/name&gt;
 *    &lt;value&gt;${<i>basedir</i>}/tmp&lt;/value&gt;
 *  &lt;/property&gt;</pre></tt>
 *
 * When <tt>conf.get("tempdir")</tt> is called, then <tt>${<i>basedir</i>}</tt>
 * will be resolved to another property in this Configuration, while
 * <tt>${<i>user.name</i>}</tt> would then ordinarily be resolved to the value
 * of the System property with that name.
 */
public class Configuration implements Iterable<Map.Entry<String,String>> {
  private static final Logger LOG = Logger.getLogger(Configuration.class);

  private boolean quietmode = false;

  public boolean isQuietMode() {
    return quietmode; 
  }

  public static class PropertyElement {
    public String name = null; 
    public String value = null; 
    public String description = null; 
    public String type = null; 
    public String category = null; 
    public Object resource = null; 
    public int resourceType = DEFAULT_RESOURCE_TYPE; 
    public boolean finalParameter = false; 

    public PropertyElement() {} 
    public PropertyElement clone() {
      PropertyElement pe = new PropertyElement(); 
      pe.name = name; 
      pe.value = value; 
      pe.description = description; 
      pe.type = type; 
      pe.category = category; 
      pe.resource = resource; 
      pe.resourceType = resourceType; 
      pe.finalParameter = finalParameter; 
      return pe; 
    }

    public boolean isUserDefined() {
      return resourceType == USER_RESOURCE_TYPE; 
    }
  }

  public static class ResourceInfo {
    public Object name = null; 
    public URL url = null; 
    public long length = 0; 
    public int resourceType = DEFAULT_RESOURCE_TYPE; 
  }

  private Object defaultUserResource = null; 

  public final static int DEFAULT_RESOURCE_TYPE = 0; 
  public final static int USER_RESOURCE_TYPE = 1; 
  
  /**
   * List of configuration properties.
   */
  private ArrayList<PropertyElement> elements = new ArrayList<PropertyElement>();

  private Map<String, PropertyElement> propElements = new TreeMap<String, PropertyElement>(); 

  private Map<String, Set<String> > propCategories = new TreeMap<String, Set<String> >(); 

  private ArrayList<ResourceInfo> resourceInfos = new ArrayList<ResourceInfo>(); 


  /**
   * List of configuration resources.
   */
  private ArrayList<Object> resources = new ArrayList<Object>();

  private ArrayList<Object> userResources = new ArrayList<Object>();


  /**
   * List of configuration parameters marked <b>final</b>. 
   */
  private Set<String> finalParameters = new HashSet<String>();

  
  private Properties properties;
  private Properties overlay;
  private ClassLoader classLoader;
  {
    classLoader = Thread.currentThread().getContextClassLoader();
    if (classLoader == null) {
      classLoader = Configuration.class.getClassLoader();
    }
  }

  
  /** A new configuration. */
  protected Configuration() {
    this(false); 
  }

  /** A new configuration where the behavior of reading from the default 
   * resources can be turned off.
   * 
   * If the parameter {@code loadDefaults} is false, the new instance
   * will not load resources from the default files. 
   * @param quietmode specifies whether run at quietmode
   */
  protected Configuration(boolean quietmode) {
    if (!quietmode && LOG.isDebugEnabled()) LOG.debug("Configuration()");
    this.quietmode = quietmode; 
    initResources(quietmode); 
  }
  
  protected Configuration(InputSource source, boolean quietmode) {
    if (!quietmode && LOG.isDebugEnabled()) LOG.debug("Configuration(InputSource)");
    this.quietmode = quietmode; 
    if (source != null) 
    	initResources(source, quietmode); 
  }

  /** 
   * A new configuration with the same settings cloned from another.
   * 
   * @param other the configuration from which to clone settings.
   */
  @SuppressWarnings("unchecked")
  public Configuration(Configuration other) {
    this.quietmode = other.quietmode; 
    if (!quietmode && LOG.isDebugEnabled()) LOG.debug("Configuration(config)");
    this.resources = (ArrayList<Object>)other.resources.clone();
    if (other.properties != null)
      this.properties = (Properties)other.properties.clone();
    if (other.overlay!=null)
      this.overlay = (Properties)other.overlay.clone();
    this.finalParameters = new HashSet<String>(other.finalParameters);
  }

  /**
   * Add a configuration resource. 
   * 
   * The properties of this resource will override properties of previously 
   * added resources, unless they were marked <a href="#Final">final</a>. 
   * 
   * @param name resource to be added, the classpath is examined for a file 
   *             with that name.
   */
  public void addResource(String name) {
    addResource(resources, name);
  }

  public void addUserResource(String name) {
    addResource(userResources, name);
    defaultUserResource = name; 
  }

  /**
   * Add a configuration resource. 
   * 
   * The properties of this resource will override properties of previously 
   * added resources, unless they were marked <a href="#Final">final</a>. 
   * 
   * @param url url of the resource to be added, the local filesystem is 
   *            examined directly to find the resource, without referring to 
   *            the classpath.
   */
  public void addResource(URL url) {
    addResource(resources, url);
  }

  public void addUserResource(URL url) {
    addResource(userResources, url);
    defaultUserResource = url; 
  }

  /**
   * Add a configuration resource. 
   * 
   * The properties of this resource will override properties of previously 
   * added resources, unless they were marked <a href="#Final">final</a>. 
   * 
   * @param file file-path of resource to be added, the local filesystem is
   *             examined directly to find the resource, without referring to 
   *             the classpath.
   */
  public void addResource(Path file) {
    addResource(resources, file);
  }

  public void addUserResource(Path file) {
    addResource(userResources, file);
    defaultUserResource = file; 
  }

  public void addResource(InputSource source) {
	addResource(resources, source);
  }
  
  private synchronized void addResource(ArrayList<Object> resources,
                                        Object resource) {
    for (int i=0; i<userResources.size(); i++) {
      Object obj = userResources.get(i); 
      if (obj.equals(resource)) 
        return; 
    } 
    for (int i=0; i<this.resources.size(); i++) {
      Object obj = this.resources.get(i); 
      if (obj.equals(resource)) 
        return; 
    } 

    resources.add(resource);                      // add to resources
    properties = null;                            // trigger reload
    finalParameters.clear();                      // clear site-limits

    if (!quietmode)
      LOG.info("add configuration resource: " + resource); 
  }
  
  /**
   * Returns the value of the <code>name</code> property, or null if no such
   * property exists.
   * @deprecated A side map of Configuration to Object should be used instead.
   */
  @Deprecated
  public Object getObject(String name) { return getProps().get(name);}

  /** Sets the value of the <code>name</code> property. 
   * @deprecated
   */
  @Deprecated
  public void setObject(String name, Object value) {
    getProps().put(name, value);
  }

  /** Returns the value of the <code>name</code> property.  If no such property
   * exists, then <code>defaultValue</code> is returned.
   * @deprecated A side map of Configuration to Object should be used instead.
   */
  @Deprecated
  public Object get(String name, Object defaultValue) {
    Object res = getObject(name);
    if (res != null) return res;
    else return defaultValue;
  }
  
  private static Pattern varPat = Pattern.compile("\\$\\{[^\\}\\$\u0020]+\\}");
  private static int MAX_SUBST = 20;

  public String substituteVars(String expr) {
    if (expr == null) {
      return null;
    }
    Matcher match = varPat.matcher("");
    String eval = expr;
    for(int s=0; s<MAX_SUBST; s++) {
      match.reset(eval);
      if (!match.find()) {
        return eval;
      }
      String var = match.group();
      var = var.substring(2, var.length()-1); // remove ${ .. }
      String val = System.getProperty(var);
      if (val == null) val = System.getenv(var); 
      if (val == null) {
        val = getRaw(var);
      }
      if (val == null) {
        return eval; // return literal ${var}: var is unbound
      }
      // substitute
      eval = eval.substring(0, match.start())+val+eval.substring(match.end());
    }
    throw new IllegalStateException("Variable substitution depth too large: " 
                                    + MAX_SUBST + " " + expr);
  }

  public String substituteVarsWin(String expr) {
    if (expr == null || expr.length() == 0)
      return expr;

    StringBuffer sbuf = new StringBuffer(); 
    int pos0 = 0, pos1 = 0, pos2 = 0;

    while (pos0 < expr.length()) {
      pos1 = expr.indexOf('%', pos0);
      if (pos1 < 0) {
        sbuf.append(expr.substring(pos0)); 
        break; 
      }

      pos2 = expr.indexOf('%', pos1 + 1);
      if (pos2 < 0) {
        sbuf.append(expr.substring(pos0));
        break; 
      }

      String var = expr.substring(pos1 + 1, pos2);
      String val = System.getProperty(var);
      if (val == null) val = System.getenv(var); 
      if (val == null) val = getRaw(var);
      if (val == null) {
        sbuf.append(expr.substring(pos0));
        break; 
      }

      sbuf.append(expr.substring(pos0, pos1));
      sbuf.append(val);

      pos0 = pos2 + 1; 
    }

    return sbuf.toString();
  }
  
  /**
   * Get the value of the <code>name</code> property, <code>null</code> if
   * no such property exists.
   * 
   * Values are processed for <a href="#VariableExpansion">variable expansion</a> 
   * before being returned. 
   * 
   * @param name the property name.
   * @return the value of the <code>name</code> property, 
   *         or null if no such property exists.
   */
  public String get(String name) {
    return substituteVars(getProps().getProperty(name));
  }

  /**
   * Get the value of the <code>name</code> property, without doing
   * <a href="#VariableExpansion">variable expansion</a>.
   * 
   * @param name the property name.
   * @return the value of the <code>name</code> property, 
   *         or null if no such property exists.
   */
  public String getRaw(String name) {
    return getProps().getProperty(name);
  }

  /** Sets the value of the <code>name</code> property. 
   * @deprecated
   */
  @Deprecated
  public void set(String name, Object value) {
    getOverlay().setProperty(name, value.toString());
    getProps().setProperty(name, value.toString());
  }
  
  /** 
   * Set the <code>value</code> of the <code>name</code> property.
   * 
   * @param name property name.
   * @param value property value.
   */
  public void set(String name, String value) {
    getOverlay().setProperty(name, value);
    getProps().setProperty(name, value);
  }
  
  /**
   * Sets a property if it is currently unset.
   * @param name the property name
   * @param value the new value
   */
  public void setIfUnset(String name, String value) {
    if (get(name) == null) {
      set(name, value);
    }
  }

  private synchronized Properties getOverlay() {
    if (overlay==null){
      overlay=new Properties();
    }
    return overlay;
  }

  /** 
   * Get the value of the <code>name</code> property. If no such property 
   * exists, then <code>defaultValue</code> is returned.
   * 
   * @param name property name.
   * @param defaultValue default value.
   * @return property value, or <code>defaultValue</code> if the property 
   *         doesn't exist.                    
   */
  public String get(String name, String defaultValue) {
    return substituteVars(getProps().getProperty(name, defaultValue));
  }
    
  /** 
   * Get the value of the <code>name</code> property as an <code>int</code>.
   *   
   * If no such property exists, or if the specified value is not a valid
   * <code>int</code>, then <code>defaultValue</code> is returned.
   * 
   * @param name property name.
   * @param defaultValue default value.
   * @return property value as an <code>int</code>, 
   *         or <code>defaultValue</code>. 
   */
  public int getInt(String name, int defaultValue) {
    String valueString = get(name);
    if (valueString == null)
      return defaultValue;
    try {
      return Integer.parseInt(valueString);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /** 
   * Set the value of the <code>name</code> property to an <code>int</code>.
   * 
   * @param name property name.
   * @param value <code>int</code> value of the property.
   */
  public void setInt(String name, int value) {
    set(name, Integer.toString(value));
  }


  /** 
   * Get the value of the <code>name</code> property as a <code>long</code>.  
   * If no such property is specified, or if the specified value is not a valid
   * <code>long</code>, then <code>defaultValue</code> is returned.
   * 
   * @param name property name.
   * @param defaultValue default value.
   * @return property value as a <code>long</code>, 
   *         or <code>defaultValue</code>. 
   */
  public long getLong(String name, long defaultValue) {
    String valueString = get(name);
    if (valueString == null)
      return defaultValue;
    try {
      return Long.parseLong(valueString);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /** 
   * Set the value of the <code>name</code> property to a <code>long</code>.
   * 
   * @param name property name.
   * @param value <code>long</code> value of the property.
   */
  public void setLong(String name, long value) {
    set(name, Long.toString(value));
  }

  /** 
   * Get the value of the <code>name</code> property as a <code>float</code>.  
   * If no such property is specified, or if the specified value is not a valid
   * <code>float</code>, then <code>defaultValue</code> is returned.
   * 
   * @param name property name.
   * @param defaultValue default value.
   * @return property value as a <code>float</code>, 
   *         or <code>defaultValue</code>. 
   */
  public float getFloat(String name, float defaultValue) {
    String valueString = get(name);
    if (valueString == null)
      return defaultValue;
    try {
      return Float.parseFloat(valueString);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /** 
   * Get the value of the <code>name</code> property as a <code>boolean</code>.  
   * If no such property is specified, or if the specified value is not a valid
   * <code>boolean</code>, then <code>defaultValue</code> is returned.
   * 
   * @param name property name.
   * @param defaultValue default value.
   * @return property value as a <code>boolean</code>, 
   *         or <code>defaultValue</code>. 
   */
  public boolean getBoolean(String name, boolean defaultValue) {
    String valueString = get(name);
    if ("true".equals(valueString))
      return true;
    else if ("false".equals(valueString))
      return false;
    else return defaultValue;
  }

  /** 
   * Set the value of the <code>name</code> property to a <code>boolean</code>.
   * 
   * @param name property name.
   * @param value <code>boolean</code> value of the property.
   */
  public void setBoolean(String name, boolean value) {
    set(name, Boolean.toString(value));
  }

  /**
   * Set the given property, if it is currently unset.
   * @param name property name
   * @param value new value
   */
  public void setBooleanIfUnset(String name, boolean value) {
    setIfUnset(name, Boolean.toString(value));
  }

  /**
   * A class that represents a set of positive integer ranges. It parses 
   * strings of the form: "2-3,5,7-" where ranges are separated by comma and 
   * the lower/upper bounds are separated by dash. Either the lower or upper 
   * bound may be omitted meaning all values up to or over. So the string 
   * above means 2, 3, 5, and 7, 8, 9, ...
   */
  public static class IntegerRanges {
    private static class Range {
      int start;
      int end;
    }

    List<Range> ranges = new ArrayList<Range>();
    
    public IntegerRanges() {
    }
    
    public IntegerRanges(String newValue) {
      StringTokenizer itr = new StringTokenizer(newValue, ",");
      while (itr.hasMoreTokens()) {
        String rng = itr.nextToken().trim();
        String[] parts = rng.split("-", 3);
        if (parts.length < 1 || parts.length > 2) {
          throw new IllegalArgumentException("integer range badly formed: " + 
                                             rng);
        }
        Range r = new Range();
        r.start = convertToInt(parts[0], 0);
        if (parts.length == 2) {
          r.end = convertToInt(parts[1], Integer.MAX_VALUE);
        } else {
          r.end = r.start;
        }
        if (r.start > r.end) {
          throw new IllegalArgumentException("IntegerRange from " + r.start + 
                                             " to " + r.end + " is invalid");
        }
        ranges.add(r);
      }
    }

    /**
     * Convert a string to an int treating empty strings as the default value.
     * @param value the string value
     * @param defaultValue the value for if the string is empty
     * @return the desired integer
     */
    private static int convertToInt(String value, int defaultValue) {
      String trim = value.trim();
      if (trim.length() == 0) {
        return defaultValue;
      }
      return Integer.parseInt(trim);
    }

    /**
     * Is the given value in the set of ranges
     * @param value the value to check
     * @return is the value in the ranges?
     */
    public boolean isIncluded(int value) {
      for(Range r: ranges) {
        if (r.start <= value && value <= r.end) {
          return true;
        }
      }
      return false;
    }
    
    public String toString() {
      StringBuffer result = new StringBuffer();
      boolean first = true;
      for(Range r: ranges) {
        if (first) {
          first = false;
        } else {
          result.append(',');
        }
        result.append(r.start);
        result.append('-');
        result.append(r.end);
      }
      return result.toString();
    }
  }

  /**
   * Parse the given attribute as a set of integer ranges
   * @param name the attribute name
   * @param defaultValue the default value if it is not set
   * @return a new set of ranges from the configured value
   */
  public IntegerRanges getRange(String name, String defaultValue) {
    return new IntegerRanges(get(name, defaultValue));
  }

  /** 
   * Get the comma delimited values of the <code>name</code> property as 
   * a collection of <code>String</code>s.  
   * If no such property is specified then empty collection is returned.
   * <p>
   * This is an optimized version of {@link #getStrings(String)}
   * 
   * @param name property name.
   * @return property value as a collection of <code>String</code>s. 
   */
  public Collection<String> getStringCollection(String name) {
    String valueString = get(name);
    return StringUtils.getStringCollection(valueString);
  }

  /** 
   * Get the comma delimited values of the <code>name</code> property as 
   * an array of <code>String</code>s.  
   * If no such property is specified then <code>null</code> is returned.
   * 
   * @param name property name.
   * @return property value as an array of <code>String</code>s, 
   *         or <code>null</code>. 
   */
  public String[] getStrings(String name) {
    String valueString = get(name);
    return StringUtils.getStrings(valueString);
  }

  /** 
   * Get the comma delimited values of the <code>name</code> property as 
   * an array of <code>String</code>s.  
   * If no such property is specified then default value is returned.
   * 
   * @param name property name.
   * @param defaultValue The default value
   * @return property value as an array of <code>String</code>s, 
   *         or default value. 
   */
  public String[] getStrings(String name, String... defaultValue) {
    String valueString = get(name);
    if (valueString == null) {
      return defaultValue;
    } else {
      return StringUtils.getStrings(valueString);
    }
  }

  /** 
   * Set the array of string values for the <code>name</code> property as 
   * as comma delimited values.  
   * 
   * @param name property name.
   * @param values The values
   */
  public void setStrings(String name, String... values) {
    set(name, StringUtils.arrayToString(values));
  }

  /**
   * Load a class by name.
   * 
   * @param name the class name.
   * @return the class object.
   * @throws ClassNotFoundException if the class is not found.
   */
  public Class<?> getClassByName(String name) throws ClassNotFoundException {
    return Class.forName(name, true, classLoader);
  }

  /** 
   * Get the value of the <code>name</code> property
   * as an array of <code>Class</code>.
   * The value of the property specifies a list of comma separated class names.  
   * If no such property is specified, then <code>defaultValue</code> is 
   * returned.
   * 
   * @param name the property name.
   * @param defaultValue default value.
   * @return property value as a <code>Class[]</code>, 
   *         or <code>defaultValue</code>. 
   */
  public Class<?>[] getClasses(String name, Class<?> ... defaultValue) {
    String[] classnames = getStrings(name);
    if (classnames == null)
      return defaultValue;
    try {
      Class<?>[] classes = new Class<?>[classnames.length];
      for(int i = 0; i < classnames.length; i++) {
        classes[i] = getClassByName(classnames[i]);
      }
      return classes;
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  /** 
   * Get the value of the <code>name</code> property as a <code>Class</code>.  
   * If no such property is specified, then <code>defaultValue</code> is 
   * returned.
   * 
   * @param name the class name.
   * @param defaultValue default value.
   * @return property value as a <code>Class</code>, 
   *         or <code>defaultValue</code>. 
   */
  public Class<?> getClass(String name, Class<?> defaultValue) {
    String valueString = get(name);
    if (valueString == null)
      return defaultValue;
    try {
      return getClassByName(valueString);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  /** 
   * Get the value of the <code>name</code> property as a <code>Class</code>
   * implementing the interface specified by <code>xface</code>.
   *   
   * If no such property is specified, then <code>defaultValue</code> is 
   * returned.
   * 
   * An exception is thrown if the returned class does not implement the named
   * interface. 
   * 
   * @param name the class name.
   * @param defaultValue default value.
   * @param xface the interface implemented by the named class.
   * @return property value as a <code>Class</code>, 
   *         or <code>defaultValue</code>.
   */
  public <U> Class<? extends U> getClass(String name, 
                                         Class<? extends U> defaultValue, 
                                         Class<U> xface) {
    try {
      Class<?> theClass = getClass(name, defaultValue);
      if (theClass != null && !xface.isAssignableFrom(theClass))
        throw new RuntimeException(theClass+" not "+xface.getName());
      else if (theClass != null)
        return theClass.asSubclass(xface);
      else
        return null;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** 
   * Set the value of the <code>name</code> property to the name of a 
   * <code>theClass</code> implementing the given interface <code>xface</code>.
   * 
   * An exception is thrown if <code>theClass</code> does not implement the 
   * interface <code>xface</code>. 
   * 
   * @param name property name.
   * @param theClass property value.
   * @param xface the interface implemented by the named class.
   */
  public void setClass(String name, Class<?> theClass, Class<?> xface) {
    if (!xface.isAssignableFrom(theClass))
      throw new RuntimeException(theClass+" not "+xface.getName());
    set(name, theClass.getName());
  }

  /** 
   * Get a local file under a directory named by <i>dirsProp</i> with
   * the given <i>path</i>.  If <i>dirsProp</i> contains multiple directories,
   * then one is chosen based on <i>path</i>'s hash code.  If the selected
   * directory does not exist, an attempt is made to create it.
   * 
   * @param dirsProp directory in which to locate the file.
   * @param path file-path.
   * @return local file under the directory with the given path.
   */
  public Path getLocalPath(String dirsProp, String path)
    throws IOException {
    String[] dirs = getStrings(dirsProp);
    int hashCode = path.hashCode();
    FileSystem fs = FileSystem.getLocal(this);
    for (int i = 0; i < dirs.length; i++) {  // try each local dir
      int index = (hashCode+i & Integer.MAX_VALUE) % dirs.length;
      Path file = new Path(dirs[index], path);
      Path dir = file.getParent();
      if (fs.mkdirs(dir) || fs.exists(dir)) {
        return file;
      }
    }
    if (!quietmode) LOG.warn("Could not make " + path + " in local directories from " + dirsProp);
    for(int i=0; i < dirs.length; i++) {
      int index = (hashCode+i & Integer.MAX_VALUE) % dirs.length;
      if (!quietmode) LOG.warn(dirsProp + "[" + index + "]=" + dirs[index]);
    }
    throw new IOException("No valid local directories in property: "+dirsProp);
  }

  /** 
   * Get a local file name under a directory named in <i>dirsProp</i> with
   * the given <i>path</i>.  If <i>dirsProp</i> contains multiple directories,
   * then one is chosen based on <i>path</i>'s hash code.  If the selected
   * directory does not exist, an attempt is made to create it.
   * 
   * @param dirsProp directory in which to locate the file.
   * @param path file-path.
   * @return local file under the directory with the given path.
   */
  public File getFile(String dirsProp, String path)
    throws IOException {
    String[] dirs = getStrings(dirsProp);
    int hashCode = path.hashCode();
    for (int i = 0; i < dirs.length; i++) {  // try each local dir
      int index = (hashCode+i & Integer.MAX_VALUE) % dirs.length;
      File file = new File(dirs[index], path);
      File dir = file.getParentFile();
      if (dir.exists() || dir.mkdirs()) {
        return file;
      }
    }
    throw new IOException("No valid local directories in property: "+dirsProp);
  }

  /** 
   * Get the {@link URL} for the named resource.
   * 
   * @param name resource name.
   * @return the url for the named resource.
   */
  public URL getResource(String name) {
    if (name != null) {
      if (name.startsWith(Path.SEPARATOR))
        name = "file:" + name; 

      if (name.indexOf(':') > 0) {
        try {
          URL url = new URL(name); 
          return url; 
        } catch (Exception e) {
        }
      }
    }
    return classLoader.getResource(name);
  }
  
  /** 
   * Get an input stream attached to the configuration resource with the
   * given <code>name</code>.
   * 
   * @param name configuration resource name.
   * @return an input stream attached to the resource.
   */
  public InputStream getConfResourceAsInputStream(String name) {
    try {
      URL url= getResource(name);

      if (url == null) {
        if (!quietmode && LOG.isDebugEnabled()) LOG.debug(name + " not found");
        return null;
      } else {
        if (!quietmode) LOG.info("found resource " + name + " at " + url);
      }

      return url.openStream();
    } catch (Exception e) {
      if (!quietmode) LOG.warn("open resource: "+name+" error: "+e); 
      return null;
    }
  }

  public OutputStream getConfResourceAsOutputStream(String name) {
    try {
      URL url= getResource(name);

      if (url == null) {
        if (!quietmode && LOG.isDebugEnabled()) LOG.debug(name + " not found");
        return null;
      } else {
        if (!quietmode) LOG.info("found resource " + name + " at " + url);
      }

      return new FileOutputStream(new File(url.toURI()));
    } catch (Exception e) {
      if (!quietmode) LOG.warn("open resource: "+name+" error: "+e); 
      return null;
    }
  }

  /** 
   * Get a {@link Reader} attached to the configuration resource with the
   * given <code>name</code>.
   * 
   * @param name configuration resource name.
   * @return a reader attached to the resource.
   */
  public Reader getConfResourceAsReader(String name) {
    try {
      URL url= getResource(name);

      if (url == null) {
        if (!quietmode && LOG.isDebugEnabled()) LOG.debug(name + " not found");
        return null;
      } else {
        if (!quietmode) LOG.info("found resource " + name + " at " + url);
      }

      return new InputStreamReader(url.openStream());
    } catch (Exception e) {
      if (!quietmode) LOG.warn("open resource: "+name+" error: "+e); 
      return null;
    }
  }

  public Writer getConfResourceAsWriter(String name) {
    try {
      URL url= getResource(name);

      if (url == null) {
        if (!quietmode && LOG.isDebugEnabled()) LOG.debug(name + " not found");
        return null;
      } else {
        if (!quietmode) LOG.info("found resource " + name + " at " + url);
      }

      return new FileWriter(new File(url.toURI()));
    } catch (Exception e) {
      if (!quietmode) LOG.warn("open resource: "+name+" error: "+e); 
      return null;
    }
  }

  private synchronized Properties getProps() {
    if (properties == null) {
      properties = new Properties();
      loadResources(properties, resources, quietmode);
      if (overlay != null)
        properties.putAll(overlay);
    }
    return properties;
  }

  /** @return Iterator&lt; Map.Entry&lt;String,String> >  
   * @deprecated Use {@link #iterator()} instead. 
   */
  @Deprecated
  public Iterator<?> entries() {
    return iterator();
  }

  /**
   * Get an {@link Iterator} to go through the list of <code>String</code> 
   * key-value pairs in the configuration.
   * 
   * @return an iterator over the entries.
   */
  public Iterator<Map.Entry<String, String>> iterator() {
    // Get a copy of just the string to string pairs. After the old object
    // methods that allow non-strings to be put into configurations are removed,
    // we could replace properties with a Map<String,String> and get rid of this
    // code.
    Map<String,String> result = new HashMap<String,String>();
    for(Map.Entry<Object,Object> item: getProps().entrySet()) {
      if (item.getKey() instanceof String && 
          item.getValue() instanceof String) {
        result.put((String) item.getKey(), (String) item.getValue());
      }
    }
    return result.entrySet().iterator();
  }

  private void initResources(boolean quiet) {
	  initResources("configuration.xml", quiet);
  }
  
  private void initResources(Object input, boolean quiet) {
    try {
      //clear resources first
      resources.clear(); 
      userResources.clear(); 

      DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
      //ignore all comments inside the xml file
      docBuilderFactory.setIgnoringComments(true);
      DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
      Document doc = null;

      if (input instanceof InputSource) { 
    	  InputSource source = (InputSource)input;
    	  if (!quiet) {
    		LOG.info("parsing InputSource: " + source);
    	  }
    	  doc = builder.parse(source.openStream());
    	  
      } else {
	      String name = (String)input; 
	
	      URL url = getResource((String)name);
	      if (url != null) {
	        if (!quiet) {
	          LOG.info("parsing CLASSPATH: " + url);
	        }
	        ResourceInfo ri = new ResourceInfo();
	        ri.name = name;
	        ri.url = url;
	        resourceInfos.add(ri);
	
	        File file = new File(url.toURI());
	        if (file == null || !file.exists() || file.length() == 0) {
	          if (!quiet) LOG.warn("resource: " + url + " not exists or empty.");
	          return;
	        }
	
	        doc = builder.parse(url.toString());
	      }
      }
      if (doc == null) {
        throw new RuntimeException(input + " not found");
      }

      Element root = doc.getDocumentElement();
      if (!"configuration".equals(root.getTagName()))
        LOG.error("bad conf file: top-level element not <configuration>");

      NodeList props = root.getChildNodes();
      for (int i = 0; i < props.getLength(); i++) {
        Node propNode = props.item(i);
        if (!(propNode instanceof Element))
          continue;
        Element prop = (Element)propNode;

        String tagName = prop.getTagName().toLowerCase(); 
        if ("addresource".equals(tagName))
          loadResourceInclude(properties, prop); 
        else 
          if (!quiet) LOG.warn("bad conf file: element not <addResource>");
      }

    } catch (Exception e) {
      LOG.error("error parsing conf file: " + e);
      //e.printStackTrace(LogUtil.getErrorStream(LOG));
      throw new RuntimeException(e);
    }

  }

  private void loadResources(Properties properties, ArrayList<Object> resources, boolean quiet) {
    elements.clear(); 
    propElements.clear(); 
    propCategories.clear(); 
    resourceInfos.clear(); 

    for (Object resource : resources) {
      loadResource(properties, resource, DEFAULT_RESOURCE_TYPE, quiet);
    }

    for (Object resource : userResources) {
      loadResource(properties, resource, USER_RESOURCE_TYPE, quiet);
    }
  }

  private void loadResource(Properties properties, Object name, int resourceType, boolean quiet) {
    try {
      DocumentBuilderFactory docBuilderFactory 
        = DocumentBuilderFactory.newInstance();
      //ignore all comments inside the xml file
      docBuilderFactory.setIgnoringComments(true);
      DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
      Document doc = null;

      if (name instanceof InputSource) {   				 // an InputSource resource
    	  InputSource source = (InputSource)name;
    	  if (!quiet) {
    		  LOG.info("parsing InputSource: " + source);
    	  }
    	  doc = builder.parse(source.openStream());
    	  
      } else if (name instanceof URL) {                  // an URL resource
        URL url = (URL)name;
        if (url != null) {
          if (!quiet) LOG.info("parsing URL: " + url);

          ResourceInfo ri = new ResourceInfo(); 
          ri.name = name; 
          ri.url = url; 
          ri.resourceType = resourceType; 
          resourceInfos.add(ri); 

          File file = new File(url.toURI()); 
          if (file == null || !file.exists() || file.length() == 0) {
            if (!quiet) LOG.warn("resource: " + url + " not exists or empty."); 
            return; 
          }

          doc = builder.parse(url.toString());
        }
      } else if (name instanceof String) {        // a CLASSPATH resource
        URL url = getResource((String)name);
        if (url != null) {
          if (!quiet) LOG.info("parsing CLASSPATH: " + url);

          ResourceInfo ri = new ResourceInfo(); 
          ri.name = name; 
          ri.url = url; 
          ri.resourceType = resourceType; 
          resourceInfos.add(ri); 

          File file = new File(url.toURI()); 
          if (file == null || !file.exists() || file.length() == 0) {
            if (!quiet) LOG.warn("resource: " + url + " not exists or empty."); 
            return; 
          }

          doc = builder.parse(url.toString());
        }
      } else if (name instanceof Path) {          // a file resource
        // Can't use FileSystem API or we get an infinite loop
        // since FileSystem uses Configuration API.  Use java.io.File instead.
        File file = new File(((Path)name).toUri().getPath()).getAbsoluteFile();
        if (file.exists()) {
          if (!quiet) LOG.info("parsing FILE: " + file);

          ResourceInfo ri = new ResourceInfo(); 
          ri.name = name; 
          ri.url = new URL("file:"+file.getAbsolutePath()); 
          ri.length = file.length(); 
          ri.resourceType = resourceType; 
          resourceInfos.add(ri); 

          if (file == null || !file.exists() || file.length() == 0) {
            if (!quiet) LOG.warn("resource: " + name + " not exists or empty."); 
            return; 
          }

          InputStream in = new BufferedInputStream(new FileInputStream(file));
          try {
            doc = builder.parse(in);
          } finally {
            in.close();
          }
        }
      }

      if (doc == null) {
        if (quiet)
          return;
        throw new RuntimeException(name + " not found");
      }

      Element root = doc.getDocumentElement();
      if (!"configuration".equals(root.getTagName()))
        LOG.error("bad conf file: top-level element not <configuration>");

      NodeList props = root.getChildNodes();
      for (int i = 0; i < props.getLength(); i++) {
        Node propNode = props.item(i);
        if (!(propNode instanceof Element))
          continue;
        Element prop = (Element)propNode;

        if ("category".equals(prop.getTagName()))
          loadResourceCategory(properties, name, prop, resourceType); 
        else if ("property".equals(prop.getTagName()))
          loadResourceProperty(properties, name, prop, resourceType); 
        else 
          if (!quiet) LOG.warn("bad conf file: element not <property> or <category>");
      }
        
    } catch (Exception e) {
      LOG.error("error parsing conf file: " + e);
      if (quiet) 
        ;//e.printStackTrace(LogUtil.getErrorStream(LOG)); 
      else
        throw new RuntimeException(e);
    }
    
  }

  private void loadResourceInclude(Properties properties, Element prop) throws Exception {
    String tagName = prop.getTagName().toLowerCase(); 
    if (!"addresource".equals(tagName))
      if (!quietmode) LOG.warn("bad conf file: element not <addResource>");

    String filename = StringUtils.trim(prop.getAttribute("name")); 
    String type = StringUtils.trim(prop.getAttribute("type")); 

    if (filename == null || filename.length() == 0) {
      if (!quietmode) LOG.warn("addResource has no \"name\" attribute"); 
      return; 
    }

    if (type == null || type.length() == 0) 
      type = "default"; 

    if ("user".equals(type)) 
      addUserResource(filename); 
    else
      addResource(filename); 
  }

  private void loadResourceCategory(Properties properties, Object name, Element cateProp, 
                                    int resourceType) throws Exception {
    if (!"category".equals(cateProp.getTagName()))
      if (!quietmode) LOG.warn("bad conf file: element not <category>");

    String category = StringUtils.trim(cateProp.getAttribute("name")); 

    NodeList props = cateProp.getChildNodes();
    for (int i = 0; i < props.getLength(); i++) {
      Node propNode = props.item(i);
      if (!(propNode instanceof Element))
        continue;
      Element prop = (Element)propNode;

      loadResourceProperty(properties, name, prop, category, resourceType); 
    }
  }

  private void loadResourceProperty(Properties properties, Object name, Element prop, 
                                    int resourceType) throws Exception {
    loadResourceProperty(properties, name, prop, null, resourceType); 
  }

  private void loadResourceProperty(Properties properties, Object name, Element prop, 
                                    String category, int resourceType) throws Exception {
    if (!"property".equals(prop.getTagName()))
      if (!quietmode) LOG.warn("bad conf file: element not <property>");

    if (category == null || category.length() == 0) 
      category = "general"; 
    else 
      category = category.toLowerCase(); 

    NodeList fields = prop.getChildNodes();
    String attr = null;
    String value = null;
    String description = null; 
    String type = null; 
    boolean finalParameter = false;

    for (int j = 0; j < fields.getLength(); j++) {
      Node fieldNode = fields.item(j);
      if (!(fieldNode instanceof Element))
        continue;
      Element field = (Element)fieldNode;
      if ("name".equals(field.getTagName()) && field.hasChildNodes())
        attr = ((Text)field.getFirstChild()).getData();
      if ("value".equals(field.getTagName()) && field.hasChildNodes())
        value = ((Text)field.getFirstChild()).getData();
      if ("description".equals(field.getTagName()) && field.hasChildNodes())
        description = ((Text)field.getFirstChild()).getData();
      if ("type".equals(field.getTagName()) && field.hasChildNodes())
        type = ((Text)field.getFirstChild()).getData();
      if ("final".equals(field.getTagName()) && field.hasChildNodes())
        finalParameter = "true".equals(((Text)field.getFirstChild()).getData());
    }

    if ((description == null || description.length() == 0) && attr != null) {
      PropertyElement pe2 = propElements.get(attr); 
      if (pe2 != null) {
          description = pe2.description; 
      }
    }

    if (type != null && type.length() > 0) 
      type = StringUtils.trim(type.toLowerCase()); 
    if (type == null || type.length() == 0) 
      type = "text"; 

    // Ignore this parameter if it has already been marked as 'final'
    if (attr != null && value != null) {
      PropertyElement pe = new PropertyElement(); 
      pe.name = attr; 
      pe.value = value; 
      pe.description = description; 
      pe.category = category; 
      pe.type = type; 
      pe.resource = name; 
      pe.resourceType = resourceType; 
      pe.finalParameter = finalParameter; 
      elements.add(pe); 

      Set<String> cateNames = propCategories.get(category); 
      if (cateNames == null) {
        cateNames = new TreeSet<String>(); 
        propCategories.put(category, cateNames); 
      }
      cateNames.add(attr); 

      if (!finalParameters.contains(attr)) {
        properties.setProperty(attr, value);
        propElements.put(attr, pe); 
        if (finalParameter)
          finalParameters.add(attr);
      } else {
        if (!quietmode) LOG.warn(name+":a attempt to override final parameter: "+attr+";  Ignoring.");
      }
    }
  }

  /** 
   * Write out the non-default properties in this configuration to the give
   * {@link OutputStream}.
   * 
   * @param out the output stream to write to.
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void writeXml(OutputStream out) throws IOException {
    Properties properties = getProps();
    try {
      Document doc =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      Element conf = doc.createElement("configuration");
      doc.appendChild(conf);
      conf.appendChild(doc.createTextNode("\n"));
      Enumeration en = properties.propertyNames(); 
      ArrayList al = Collections.list(en); 
      Collections.sort(al); 
      for (Enumeration e = Collections.enumeration(al); e.hasMoreElements();) {
        String name = (String)e.nextElement();
        Object object = properties.get(name);
        String value = null;
        if (object instanceof String) {
          value = (String) object;
        }else {
          continue;
        }
        Element propNode = doc.createElement("property");
        conf.appendChild(propNode);
      
        Element nameNode = doc.createElement("name");
        nameNode.appendChild(doc.createTextNode(name));
        propNode.appendChild(nameNode);
      
        Element valueNode = doc.createElement("value");
        valueNode.appendChild(doc.createTextNode(value));
        propNode.appendChild(valueNode);

        conf.appendChild(doc.createTextNode("\n"));
      }
    
      DOMSource source = new DOMSource(doc);
      StreamResult result = new StreamResult(out);
      TransformerFactory transFactory = TransformerFactory.newInstance();
      Transformer transformer = transFactory.newTransformer();
      transformer.transform(source, result);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Enumeration<Object> names() {
    return getProps().keys(); 
  }

  public ArrayList<ResourceInfo> getResourceInfos() {
    return resourceInfos; 
  }

  public PropertyElement getPropertyElement(String name) {
    getProps(); 
    return propElements.get(name); 
  }

  public Iterator<String> getCategoryIterator() {
    getProps(); 
    return propCategories.keySet().iterator(); 
  }

  public Iterator<String> getCategoryPropertyIterator(String category) {
    getProps(); 

    Set<String> keys = propCategories.get(category); 
    if (keys != null) 
      return keys.iterator(); 
    else
      return null; 
  }

  public PropertyElement[] getPropertyElements(String name) {
    if (name == null) return null; 
    getProps(); 

    ArrayList<PropertyElement> els = new ArrayList<PropertyElement>(); 
    for (int i=0; i<elements.size(); i++) {
      PropertyElement e = elements.get(i); 
      if (e == null) continue; 
      if (!name.equals(e.name)) continue; 
      els.add(e); 
    }

    return els.toArray(new PropertyElement[els.size()]); 
  }

  public void setUserProperty(String name, String value) { 
    if (name == null || value == null) 
      return; 

    getProps(); 
    set(name, value); 

    boolean shouldAppend = false; 
    PropertyElement pe = getPropertyElement(name); 
    if (pe == null) {
      pe = new PropertyElement(); 
      pe.name = name; 
      pe.finalParameter = false; 
      pe.resource = defaultUserResource; 
      pe.resourceType = USER_RESOURCE_TYPE; 
      shouldAppend = true; 
    } else {
      if (!pe.isUserDefined()) {
        pe = pe.clone(); 
        pe.resource = defaultUserResource; 
        pe.resourceType = USER_RESOURCE_TYPE; 
        shouldAppend = true; 
      }
    }

    if (defaultUserResource == null) 
      shouldAppend = false; 

    pe.value = value; 
    propElements.put(name, pe); 

    if (shouldAppend) 
      elements.add(pe); 
  }

  public void removeUserProperty(String name) { 
    if (name == null) 
      return; 

    getProps(); 

    for (int i=elements.size()-1; i>=0; i--) {
      PropertyElement pe = elements.get(i); 
      if (pe == null) continue; 
      if (!name.equals(pe.name)) continue; 
      if (pe.isUserDefined()) {
        elements.remove(i);
      } else {
        set(name, pe.value); 
        propElements.put(name, pe); 
        break; 
      }
    }
  }

  public void saveUserProperties() {
    getProps(); 

    for (int i=0; i < resourceInfos.size(); i++) {
      ResourceInfo ri = resourceInfos.get(i); 
      if (ri.resourceType == USER_RESOURCE_TYPE) {
        try {
          if (ri.url != null) {
            File file = new File(ri.url.toURI()); 
            saveProperties(ri, file); 
          }
        } catch (Exception e) {
          LOG.error("open user properties file error: "+e); 
        }
      }
    }
  }

  private void saveProperties(ResourceInfo resourceInfo, File file) {
    if (resourceInfo == null || file == null) 
      return; 

    Map<String, Set<String> > cates = new TreeMap<String, Set<String> >();
    Map<String, PropertyElement> props = new TreeMap<String, PropertyElement>();

    for (int i=0; i<elements.size(); i++) {
      PropertyElement pe = elements.get(i); 
      if (pe == null) continue; 
      if (pe.resource.equals(resourceInfo.name)) {
        String name = pe.name; 
        String category = pe.category; 
        if (category == null) category = "general"; 
        if (name == null) continue; 

        Set<String> set = cates.get(category); 
        if (set == null) {
          set = new TreeSet<String>();
          cates.put(category, set); 
        }
        set.add(name); 
        props.put(name, pe); 
      }
    }

    if (props.size() >= 0) {
      try {
        FileWriter writer = new FileWriter(file); 
        writer.write(toXMLString(cates, props)); 
        writer.flush(); 
        writer.close(); 
        if (!quietmode) LOG.info("saved properties to "+file.getAbsolutePath()); 
      } catch (Exception e) {
        LOG.error("save properties error: "+e); 
      }
    }
  }

  /**
   * Get the {@link ClassLoader} for this job.
   * 
   * @return the correct class loader.
   */
  public ClassLoader getClassLoader() {
    return classLoader;
  }
  
  /**
   * Set the class loader that will be used to load the various objects.
   * 
   * @param classLoader the new class loader.
   */
  public void setClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }
  
  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("Configuration: ");
    toString(resources, sb);
    return sb.toString();
  }

  private void toString(ArrayList<?> resources, StringBuffer sb) {
    ListIterator<?> i = resources.listIterator();
    while (i.hasNext()) {
      if (i.nextIndex() != 0) {
        sb.append(", ");
      }
      sb.append(i.next());
    }
  }

  /** 
   * Set the quiteness-mode. 
   * 
   * In the the quite-mode error and informational messages might not be logged.
   * 
   * @param quietmode <code>true</code> to set quiet-mode on, <code>false</code>
   *              to turn it off.
   */
  public synchronized void setQuietMode(boolean quietmode) {
    this.quietmode = quietmode;
  }

  public String toXMLString() {
    getProps(); 
    return toXMLString(propCategories, propElements); 
  }

  public static String toXMLString(Map<String, Set<String> > categories, 
                                   Map<String, PropertyElement> properties) {
    if (categories == null || properties == null) 
      return null; 

    StringBuffer sbuf = new StringBuffer(); 
    sbuf.append("<?xml version=\"1.0\"?>\n"); 
    sbuf.append("<?xml-stylesheet type=\"text/xsl\" href=\"configuration.xsl\"?>\n"); 
    //sbuf.append("<!-- saved time: " + StringUtils.formatDate(System.currentTimeMillis()) + " -->\n\n"); 
    sbuf.append("<configuration>\n\n"); 

    Iterator<String> cit = categories.keySet().iterator();
    while (cit != null && cit.hasNext()) {
      String category = cit.next(); 
      sbuf.append("  <category name=\"" + category + "\">\n\n"); 

      Iterator<String> pit = categories.get(category).iterator(); 
      while (pit != null && pit.hasNext()) {
        String name = pit.next(); 
        PropertyElement pe = properties.get(name); 
        sbuf.append("    <property>\n"); 
        //sbuf.append("      <name>" + StringUtils.HTMLEncode(getStr(pe.name)) + "</name>\n"); 
        //sbuf.append("      <value>" + StringUtils.HTMLEncode(getStr(pe.value)) + "</value>\n"); 
        //sbuf.append("      <type>" + StringUtils.HTMLEncode(getStr(pe.type)) + "</type>\n"); 
        //sbuf.append("      <description>" + StringUtils.HTMLEncode(getStr(pe.description)) + "</description>\n"); 
        //sbuf.append("      <resource>" + pe.resource + "</resource>\n"); 
        if (pe.finalParameter == true) 
          sbuf.append("      <final>" + pe.finalParameter + "</final>\n"); 
        sbuf.append("    </property>\n\n"); 
      }

      sbuf.append("  </category>\n\n"); 
    }

    sbuf.append("</configuration>\n"); 
    return sbuf.toString(); 
  }

  //private static String getStr(String str) {
  //  return str == null ? "" : str; 
  //}

  /**
   * Returns the hash code value for this Configuration. The hash code of a
   * Configuration is defined by the xor of the hash codes of its entries.
   *
   * @see Configuration#iterator() How the entries are obtained.
   */
  @Override
  @Deprecated
  public int hashCode() {
    return hashCode(this);
  }

  /**
   * Returns the hash code value for this Configuration. The hash code of a
   * Configuration is defined by the xor of the hash codes of its entries.
   *
   * @see Configuration#iterator() How the entries are obtained.
   */
  public static int hashCode(Configuration conf) {
    int hash = 0;

    Iterator<Map.Entry<String, String>> propertyIterator = conf.iterator();
    while (propertyIterator.hasNext()) {
      hash ^= propertyIterator.next().hashCode();
    }
    return hash;
  }

  /** For debugging.  List non-default properties to the terminal and exit. */
  public static void main(String[] args) throws Exception {
    new Configuration().writeXml(System.out);
  }

}

