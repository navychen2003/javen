package org.javenstudio.jfm.views.list.briefview;

import java.awt.Point; 

import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;

import org.javenstudio.jfm.filesystems.JFMFile;
import org.javenstudio.jfm.views.list.PanelChangeRequestListener;
import org.javenstudio.jfm.views.list.JFMComponentUtils; 
import org.javenstudio.jfm.views.list.JFMComponent; 
import org.javenstudio.jfm.views.list.JFMModel; 

@SuppressWarnings("rawtypes")
public class BriefViewList extends JList implements JFMComponent {
  private static final long serialVersionUID = 1L;
  
  private PanelChangeRequestListener panelChangeListener = null;
  private BriefViewListRenderer renderer = new BriefViewListRenderer();


  public BriefViewList() {
    setFocusTraversalKeysEnabled(false);
    removeDefaultKeys();
    addKeys();
    addMouseActions();
    setOtherProperties();      
  }

  public void setPanelChangeRequestListener(PanelChangeRequestListener l){
    panelChangeListener = l;
  }

  private void setOtherProperties(){
    this.setLayoutOrientation(JList.VERTICAL_WRAP);
    this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    this.setDragEnabled(true);
  }

  /**
   * Returns the preferred number of visible rows.
   *
   * @return an integer indicating the preferred number of rows to display
   *         without using a scroll bar
   * @see #setVisibleRowCount
   */
  public int getVisibleRowCount() {
    //to fill the whole space
    return -1;
  }

  private InputMap getMainInputMap() {
    return this.getInputMap(JComponent.WHEN_FOCUSED);
  }

  private void removeDefaultKeys(){
    getMainInputMap().clear();
  }

  @SuppressWarnings("unused")
  private void addKeys() {
    InputMap mainMap = this.getInputMap();
    JFMComponentUtils.initMainKeys(getMainInputMap());
    JFMComponentUtils.initActionMap(this);
  }

  public ListCellRenderer getCellRenderer() {
    return renderer;
  }
    
  private void addMouseActions(){
    JFMComponentUtils.initMouseActions(this);
  }

  public JFMModel getComponentModel() {
    return (BriefViewListModel)getModel(); 
  }

  public void requestPanelChange() {
    if (panelChangeListener != null) {
      panelChangeListener.requestPanelChange();
    }
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
    if (file != null) file.setMarked(true);
  }

  public void removeMarkedRow(int row) {
    JFMFile file = getComponentModel().getFileAt(row);
    if (file != null) file.setMarked(false);
  }

  public int rowAtPoint(Point p) {
    return locationToIndex(p); 
  }

  public void setSelectedRow(int row) {
    if (row >= 0 && row < getRowCount()) 
      setSelectedIndex(row); 
    else
      clearSelection(); 
  }

  public int getSelectedRow() { 
    return getSelectedIndex(); 
  }

  public JFMFile getSelectedFile() {
    return getComponentModel().getFileAt(getSelectedRow());
  }

}
