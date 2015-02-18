package org.javenstudio.jfm.views.list;

import java.awt.Component;

import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.DefaultListCellRenderer;


/**
 * This renderer displays an icon along with the text, 
 * if the value is of type JFMFile
 * @author sergiu
 */
@SuppressWarnings({ "rawtypes", "serial" })
public class ComboBoxCellRenderer extends DefaultListCellRenderer {

  public Component getListCellRendererComponent(JList list, Object value,
                       int index, boolean isSelected, boolean cellHasFocus) {
    JLabel component = (JLabel)super.getListCellRendererComponent(list,value,index,isSelected,cellHasFocus);
    
    if (value instanceof ComboBoxCellObject) {
      ComboBoxCellObject obj = (ComboBoxCellObject)value;
      if (obj.getFile() != null) {
        component.setIcon(obj.getFile().getIcon());
      }
    }
 
    return component;
  }

}
