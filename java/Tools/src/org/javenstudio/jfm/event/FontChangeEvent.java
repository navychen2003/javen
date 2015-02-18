package org.javenstudio.jfm.event;

import java.awt.Font;


public class FontChangeEvent extends BroadcastEvent {

  private Font font;
  
  /**
   * @return Returns the font.
   */
  public Font getFont() {
    return font;
  }

  /**
   * @param font The font to set.
   */
  public void setFont(Font font) {
    this.font = font;
  }
  
  public int getType() {
    return BroadcastEvent.CHANGE_FONT_TYPE;
  }
 
}
