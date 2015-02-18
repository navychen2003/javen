package org.javenstudio.jfm.views.list.detailview;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.Rectangle;

import org.javenstudio.jfm.filesystems.JFMFile;
import org.javenstudio.jfm.views.list.PanelChangeRequestListener;
import org.javenstudio.jfm.views.list.JFMComponentUtils; 
import org.javenstudio.jfm.views.list.JFMComponent; 
import org.javenstudio.jfm.views.list.JFMModel; 


public class DetailsTable extends JTable implements JFMComponent {
  private static final long serialVersionUID = 1L;
  
  private DetailsTableCellRenderer renderer = new DetailsTableCellRenderer();
  private PanelChangeRequestListener panelChangeListener = null;
  
  public DetailsTable() {
    super();
    removeDefaultKeys();
    addKeys();
    addMouseActions();
    setOtherProperties();
  }

  public void setPanelChangeRequestListener(PanelChangeRequestListener l) {
    panelChangeListener = l;
  }

  private void setOtherProperties(){
    this.getTableHeader().setReorderingAllowed(false);
    this.setShowGrid(false);
    this.setColumnSelectionAllowed(false);
    this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    this.setDragEnabled(true);
  }

  private InputMap getMainInputMap() {
    return this.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
  }

  private InputMap getSecondInputMap() {
    return this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
  }

  private void removeDefaultKeys(){
    getMainInputMap().clear();
    getSecondInputMap().clear();
  }

  @SuppressWarnings("unused")
  private void addKeys(){
    InputMap mainMap = this.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    InputMap secondMap = this.getInputMap(JTable.WHEN_IN_FOCUSED_WINDOW);
    JFMComponentUtils.initMainKeys(getMainInputMap()); 
    JFMComponentUtils.initSecondKeys(getSecondInputMap()); 
    JFMComponentUtils.initActionMap(this); 
  }

  public TableCellRenderer getCellRenderer(int row, int column) {
    return renderer;
  }

  private void addMouseActions() {
    JFMComponentUtils.initMouseActions(this); 
  }

  /**
   * Scrolls the viewport to the row at the specified index.
   * Stolen from javax.swing.JList.
   * @param index the index of the row.
   */
  public void ensureIndexIsVisible(int index) {      
    Rectangle cellBounds = this.getCellRect(index, 0,true);
    if (cellBounds != null) {
      scrollRectToVisible(cellBounds);
    }
  }

  public void requestPanelChange() {
    if (panelChangeListener != null) {
      panelChangeListener.requestPanelChange();
    }
  }

  public JFMModel getComponentModel() {
    return (JFMModel)getModel(); 
  }

  public int getRowCount() {
    return getComponentModel().getRowCount(); 
  }

  public void clearMarkedRows() {
    for (int i=0; i < getRowCount(); i++) {
      removeMarkedRow(i); 
    }
  }

  public void addMarkedRow(int row) {
    JFMFile file = getComponentModel().getFileAt(row); 
    if (file != null) { 
      file.setMarked(true); 
    }
  }

  public void removeMarkedRow(int row) {
    JFMFile file = getComponentModel().getFileAt(row); 
    if (file != null) {
      file.setMarked(false); 
    }
  }

  public void setSelectedRow(int row) {
    if (row >= 0 && row < getRowCount())
      setRowSelectionInterval(row, row);
    else
      clearSelection(); 
  }

  public JFMFile getSelectedFile() {
    return getComponentModel().getFileAt(getSelectedRow());
  }

}
