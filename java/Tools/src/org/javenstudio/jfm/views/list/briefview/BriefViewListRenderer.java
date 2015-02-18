package org.javenstudio.jfm.views.list.briefview;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JLabel;

import org.javenstudio.jfm.filesystems.JFMFile;
import org.javenstudio.jfm.main.Options;
import org.javenstudio.jfm.views.CellBorder;


/**
 * The cell renderer for the brief list view component. 
 * @author sergiu
 */
public class BriefViewListRenderer extends DefaultListCellRenderer {
  private static final long serialVersionUID = 1L;
  
  private CellBorder border = new CellBorder(Options.getForegroundColor(),1,true);
  
  /**
   * Simple constructor
   */
  public BriefViewListRenderer() {
    super();    
  }

  @SuppressWarnings("rawtypes")
  public Component getListCellRendererComponent(JList list, Object value,
                       int index, boolean isSelected, boolean cellHasFocus) {
    JLabel c = (JLabel)super.getListCellRendererComponent(list,value,index,isSelected,cellHasFocus);
    if (isSelected) { 
      border.setLineColor(Options.getForegroundColor());
      setBorder(border);
    }

    JFMFile f = (JFMFile)value;
    if (f != null && f.isMarked()) {
      setForeground(Options.getMarkedColor());
      setBackground(Options.getMarkedBackground());
    } else {
      setForeground(list.getForeground());  
      setBackground(list.getBackground());
    }

    if (f != null) 
      c.setIcon(f.getDisplayIcon());

    return c;
  }
 
}
