package org.javenstudio.raptor.fs.shell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parse the args of a command and check the format of args.
 */
public class CommandFormat {
  final String name;
  final int minPar, maxPar;
  final Map<String, Boolean> options = new HashMap<String, Boolean>();

  /** constructor */
  public CommandFormat(String n, int min, int max, String ... possibleOpt) {
    name = n;
    minPar = min;
    maxPar = max;
    for(String opt : possibleOpt)
      options.put(opt, Boolean.FALSE);
  }

  /** Parse parameters starting from the given position
   * 
   * @param args an array of input arguments
   * @param pos the position at which starts to parse
   * @return a list of parameters
   */
  public List<String> parse(String[] args, int pos) {
    List<String> parameters = new ArrayList<String>();
    for(; pos < args.length; pos++) {
      if (args[pos].charAt(0) == '-' && args[pos].length() > 1) {
        String opt = args[pos].substring(1);
        if (options.containsKey(opt))
          options.put(opt, Boolean.TRUE);
        else
          throw new IllegalArgumentException("Illegal option " + args[pos]);
      }
      else
        parameters.add(args[pos]);
    }
    int psize = parameters.size();
    if (psize < minPar || psize > maxPar)
      throw new IllegalArgumentException("Illegal number of arguments");
    return parameters;
  }
  
  /** Return if the option is set or not
   * 
   * @param option String representation of an option
   * @return true is the option is set; false otherwise
   */
  public boolean getOpt(String option) {
    return options.get(option);
  }
}

