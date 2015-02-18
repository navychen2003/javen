package org.javenstudio.jfm.views.fview;


public interface FindListener {

  public void find(String findText, String replaceText, 
                   boolean caseSensitive, boolean fileStart, boolean wholeWords, boolean regexp, int count);

  public void all(String findText, String replaceText, 
                  boolean caseSensitive, boolean fileStart, boolean wholeWords, boolean regexp);

}
