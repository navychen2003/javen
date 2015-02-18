package org.javenstudio.jfm.views.list; 

import javax.swing.ActionMap; 
import javax.swing.ListSelectionModel; 
import java.awt.event.MouseListener; 
import java.awt.Point;

import org.javenstudio.jfm.filesystems.JFMFile; 


public interface JFMComponent {
  public JFMModel getComponentModel();
  public ActionMap getActionMap(); 
  public void addMouseListener(MouseListener l); 
  public int rowAtPoint(Point p);
  public void clearMarkedRows(); 
  public void addMarkedRow(int row);
  public void removeMarkedRow(int row);
  public void setSelectedRow(int row);
  public int getSelectedRow();
  public JFMFile getSelectedFile(); 
  public void requestPanelChange();
  public void ensureIndexIsVisible(int index); 
  public ListSelectionModel getSelectionModel(); 
  public void setPanelChangeRequestListener(PanelChangeRequestListener l); 
  public void repaint();
}
