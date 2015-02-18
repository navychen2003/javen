package org.javenstudio.jfm.views.list.detailview;

import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;

import org.javenstudio.jfm.views.list.JFMListView;
import org.javenstudio.jfm.views.list.JFMComponent;
import org.javenstudio.jfm.views.list.JFMModel;

import javax.swing.JComponent;


/**
 * This panel implements a detailed view of the files 
 * @author sergiu
 */
public class DetailView extends JFMListView {
  private static final long serialVersionUID = 1L;
  
  private TableSorter model = null; 
  private DetailsTable table = new DetailsTable();
  private int selectedRow = -1;
  
  /**
   * Constructor. 
   */
  public DetailView(String fs) {
    super(fs);
    try {
      init();
    } catch(Exception ex) {
      ex.printStackTrace();
    }
  }
 
  protected JComponent getViewComponent() {
    return table; 
  }

  protected JFMComponent getComponent() {
    return table;
  }
 
  protected void initModel() {
    TableSorter sorter = getTableModel(); 
    sorter.setTableHeader(table.getTableHeader()); 
    table.setModel(sorter); 
  }

  protected JFMModel getComponentModel() {
    return getTableModel(); 
  }

  private TableSorter getTableModel() {
    if (model == null) 
      model = new TableSorter(new DetailsTableModel(filesystem));
    return model; 
  }

  protected void processFocusGained(FocusEvent e) {
    if (selectedRow >= 0 && selectedRow < getTableModel().getRowCount()) {
      try {
        table.getSelectionModel().setSelectionInterval(selectedRow,selectedRow);
        table.repaint();
      } catch (Exception ignored) { }
    }
  }

  protected void processFocusLost(FocusEvent e) {
    selectedRow = table.getSelectedRow();
    //table.getSelectionModel().clearSelection();
    //table.repaint();
  }

  protected boolean checkPointAtRow(MouseEvent event, boolean selectRow) {
    int row = table.rowAtPoint(event.getPoint()); 
    if (row >= 0) {
      if (selectRow) table.setSelectedRow(row); 
      return true; 
    } else
      return false;
  }

}
