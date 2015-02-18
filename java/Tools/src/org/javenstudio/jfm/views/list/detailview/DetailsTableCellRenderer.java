package org.javenstudio.jfm.views.list.detailview;

import java.awt.Component;
import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.javenstudio.jfm.filesystems.JFMFile;
import org.javenstudio.jfm.main.Options;
import org.javenstudio.jfm.views.CellBorder;


public class DetailsTableCellRenderer extends DefaultTableCellRenderer {
  private static final long serialVersionUID = 1L;
  
  private CellBorder border = new CellBorder(Options.getForegroundColor(),1,true);

  public DetailsTableCellRenderer() {
    super();
  }

  public Component getTableCellRendererComponent(JTable table, Object value, 
                       boolean isSelected, boolean hasFocus, int row, int column) {
    setFont(table.getFont());
    setValue(value);

    TableModel model = table.getModel();
    JFMFile selectedFile = (JFMFile)model.getValueAt(row, 0);

    if(selectedFile != null && selectedFile.isMarked()) {
      setForeground(Options.getMarkedColor());      
      setBackground(Options.getMarkedBackground());    
    } else {
      setForeground(table.getForeground()); 
      setBackground(table.getBackground());    
    }

    if (column == 0 && table.isRowSelected(row)) {
      border.setLineColor(Options.getForegroundColor());
      setBorder(border);
    } else {
      setBorder(noFocusBorder);
    }      

    if (column == DetailsTableModel.COLUMNINDEX_SIZE) 
      setHorizontalAlignment(JLabel.RIGHT);
    else
      setHorizontalAlignment(JLabel.LEFT);
    
    if (!(value instanceof JFMFile)) 
      setIcon(null);
    else 
      setIcon(((JFMFile)value).getDisplayIcon());

    return this;
  }

}
