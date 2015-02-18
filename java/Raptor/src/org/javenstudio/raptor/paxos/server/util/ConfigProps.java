package org.javenstudio.raptor.paxos.server.util;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigProps {

  private static Pattern varPat = Pattern.compile("\\$\\{[^\\}\\$\u0020]+\\}");
  private static int MAX_SUBST = 20;

  private final Properties mProps;
  
  public ConfigProps(Properties props) { 
	if (props == null) throw new NullPointerException();
	mProps = props;
  }
  
  private Properties getProps() { return mProps; }
  
  public final String[] getNames() { 
	  return getProps().keySet().toArray(new String[0]);
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

  /** 
   * Set the <code>value</code> of the <code>name</code> property.
   * 
   * @param name property name.
   * @param value property value.
   */
  public void set(String name, String value) {
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
	
}
