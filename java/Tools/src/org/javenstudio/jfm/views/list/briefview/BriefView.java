package org.javenstudio.jfm.views.list.briefview;

import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;

import org.javenstudio.jfm.views.list.JFMListView;
import org.javenstudio.jfm.views.list.JFMComponent;
import org.javenstudio.jfm.views.list.JFMModel;


/**
 * This is the view that shows the files as a list 
 * @author sergiu
 */
public class BriefView extends JFMListView {
  private static final long serialVersionUID = 1L;
  
  private BriefViewListModel model = null;
  private BriefViewList list = new BriefViewList();
  private int selectedRow = -1;
  
  public BriefView(String fs){
    super(fs);
    try {
      init();
    } catch(Exception ex) {
      ex.printStackTrace();
    }      
  }
  
  protected JComponent getViewComponent(){
    return list;
  }

  protected JFMComponent getComponent(){
    return list;
  }
  
  @SuppressWarnings("unchecked")
  protected void initModel() {
    list.setModel(getListModel());
  }

  protected JFMModel getComponentModel() {
    return getListModel();
  }

  private BriefViewListModel getListModel() {
    if (model == null)
      model = new BriefViewListModel(filesystem);
    return model;
  }

  protected void processFocusGained(FocusEvent e) {
    if (selectedRow >= 0) {
      list.setSelectedIndex(selectedRow);
    }
  }

  protected void processFocusLost(FocusEvent e) {
    selectedRow = list.getSelectedIndex();
  }

  protected boolean checkPointAtRow(MouseEvent event, boolean selectRow) {
    int row = list.locationToIndex(event.getPoint()); 
    if (row >= 0) {
      if (selectRow) list.setSelectedRow(row); 
      return true; 
    } else 
      return false;
  }

}
