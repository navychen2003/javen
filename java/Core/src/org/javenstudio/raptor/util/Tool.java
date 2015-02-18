package org.javenstudio.raptor.util;

import org.javenstudio.raptor.conf.Configurable;

/**
 * A tool interface that support generic options handling
 * 
 * @author hairong
 *
 */
public interface Tool extends Configurable {
  /**
   * execute the command with the given arguments
   * @param args command specific arguments
   * @return exit code
   * @throws Exception
   */
  int run(String [] args) throws Exception;
}

