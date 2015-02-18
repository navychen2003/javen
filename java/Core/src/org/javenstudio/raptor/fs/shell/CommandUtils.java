package org.javenstudio.raptor.fs.shell;

final class CommandUtils {
  static String formatDescription(String usage, String... desciptions) {
    StringBuilder b = new StringBuilder(usage + ": " + desciptions[0]);
    for(int i = 1; i < desciptions.length; i++) {
      b.append("\n\t\t" + desciptions[i]);
    }
    return b.toString();
  }
}

