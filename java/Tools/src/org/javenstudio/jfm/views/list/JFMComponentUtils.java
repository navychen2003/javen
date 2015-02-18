package org.javenstudio.jfm.views.list;

import java.awt.Desktop; 
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;

import org.javenstudio.jfm.event.Broadcaster; 
import org.javenstudio.jfm.event.ChangeDirectoryEvent; 
import org.javenstudio.jfm.filesystems.JFMFile;
import org.javenstudio.jfm.main.Options;
import org.javenstudio.jfm.po.ViewFileAction; 

import java.beans.PropertyChangeListener;

import javax.swing.InputMap; 
import javax.swing.KeyStroke; 
import javax.swing.Action;


/**
 * This panel implements a detailed view of the files 
 * @author sergiu
 */
public class JFMComponentUtils {

  public static void initMainKeys(InputMap mainMap) {
    mainMap.put(KeyStroke.getKeyStroke("DOWN"),"selectNextRow");
    mainMap.put(KeyStroke.getKeyStroke("KP_DOWN"),"selectNextRow");
    mainMap.put(KeyStroke.getKeyStroke("UP"),"selectPreviousRow");
    mainMap.put(KeyStroke.getKeyStroke("KP_UP"),"selectPreviousRow");
    mainMap.put(KeyStroke.getKeyStroke("PAGE_UP"),"scrollUpChangeSelection");
    mainMap.put(KeyStroke.getKeyStroke("PAGE_DOWN"),"scrollDownChangeSelection");
    mainMap.put(KeyStroke.getKeyStroke("ctrl PAGE_UP"),"upOneDirectory"); //MUST ADD an action for this
    mainMap.put(KeyStroke.getKeyStroke("ctrl PAGE_DOWN"),"scrollDownChangeSelection");
    mainMap.put(KeyStroke.getKeyStroke("HOME"),"selectFirstRow");
    mainMap.put(KeyStroke.getKeyStroke("END"),"selectLastRow");
    mainMap.put(KeyStroke.getKeyStroke("ctrl A"),"markAllAction");
    mainMap.put(KeyStroke.getKeyStroke("ctrl Z"),"markNoneAction");
    mainMap.put(KeyStroke.getKeyStroke("ctrl R"),"markReverseAction");
    mainMap.put(KeyStroke.getKeyStroke("ctrl F"),"refreshAction");
    mainMap.put(KeyStroke.getKeyStroke("ESCAPE"),"cancel");
    mainMap.put(KeyStroke.getKeyStroke("ENTER"),"fileAction"); //MUST ADD an action for this
    mainMap.put(KeyStroke.getKeyStroke("TAB"),"changePanelAction");
    mainMap.put(KeyStroke.getKeyStroke("INSERT"),"markRowAction");
    mainMap.put(KeyStroke.getKeyStroke("SPACE"),"markRowAction");
  }

  public static void initSecondKeys(InputMap map) {
    map.put(KeyStroke.getKeyStroke("TAB"),"changePanelAction");
  }

  public static void initActionMap(JFMComponent compv) {
    compv.getActionMap().put("fileAction", new FileAction(compv));
    compv.getActionMap().put("upOneDirectory", new UpDirectoryAction(compv));
    compv.getActionMap().put("changePanelAction", new ChangePanelAction(compv));
    compv.getActionMap().put("refreshAction", new RefreshAction(compv));
    compv.getActionMap().put("markRowAction", new MarkRowAction(compv));
    compv.getActionMap().put("markAllAction", new MarkAllRowAction(compv));
    compv.getActionMap().put("markNoneAction", new MarkNoneRowAction(compv));
    compv.getActionMap().put("markReverseAction", new MarkReverseRowAction(compv));
  }

  public static void initMouseActions(final JFMComponent compv) {
    compv.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        int row = compv.rowAtPoint(e.getPoint());
        if (row >= 0 && row < compv.getComponentModel().getRowCount()) {
          if (e.getClickCount() == 2) {
            //double click on row
            processActionOnRow(compv, row);
          } else if (e.getClickCount() == 1 && !e.isMetaDown()) {
            //one click on row, not right click
            mouseClickOnRow(compv, row);
          }
        }
      }
    });
  }

  private static void mouseClickOnRow(JFMComponent compv, int row) {
    JFMModel model = compv.getComponentModel(); 
    if (row >= 0 && row < model.getRowCount()) {
      JFMFile el = model.getFileAt(row);
      if (el == null || "..".equals(el.getDisplayName())) 
        return; 
      el.setMarked(!el.isMarked());
      if (el.isMarked())
        compv.addMarkedRow(row);
      else
        compv.removeMarkedRow(row);
      JFMListView.updateMarkedStatus(compv); 
      compv.repaint();
    }
  }

  private static void processActionOnRow(JFMComponent compv, int row) {
    JFMModel model = compv.getComponentModel(); 
    JFMFile el =  model.getFileAt(row);
    if (el.isDirectory()) {
      ChangeDirectoryEvent ev=new ChangeDirectoryEvent();
      ev.setDirectory(el);
      ev.setSource(compv);
      Broadcaster.notifyChangeDirectoryListeners(ev);
    } else {
      if (el.getLocalFile() != null && Desktop.isDesktopSupported()) {
        try {
          Desktop desktop = Desktop.getDesktop();
          desktop.open(el.getLocalFile()); 
        } catch (Exception e) {
          e.printStackTrace(); 
          Options.showMessage(e); 
        }
      } else {
        ViewFileAction.viewFile(el); 
      }
    }
  }

  private static class MarkAllRowAction implements Action {
    private JFMComponent compv = null; 
    public MarkAllRowAction(JFMComponent compv) {
      this.compv = compv; 
    }
    //requested methods
    public Object getValue(String key){return null;}
    public void putValue(String key, Object value){}
    public void setEnabled(boolean b){}
    public boolean isEnabled(){return true;}
    public void addPropertyChangeListener(PropertyChangeListener listener){}
    public void removePropertyChangeListener(PropertyChangeListener listener){}

    public void actionPerformed(ActionEvent e) {
      for (int row = 0; row < compv.getComponentModel().getRowCount(); row++) {
        JFMFile el = compv.getComponentModel().getFileAt(row);
        if (el == null || "..".equals(el.getDisplayName())) 
          continue; 
        if (el.isFile() || Options.getDirectoriesSelectedOnAsterisk()) {
          if (!el.isMarked())
            compv.addMarkedRow(row);
        }
      }
      JFMListView.updateMarkedStatus(compv); 
      compv.repaint();
    }
  }

  private static class MarkNoneRowAction implements Action {
    private JFMComponent compv = null;
    public MarkNoneRowAction(JFMComponent compv) {
      this.compv = compv;
    }
    //requested methods
    public Object getValue(String key){return null;}
    public void putValue(String key, Object value){}
    public void setEnabled(boolean b){}
    public boolean isEnabled(){return true;}
    public void addPropertyChangeListener(PropertyChangeListener listener){}
    public void removePropertyChangeListener(PropertyChangeListener listener){}

    public void actionPerformed(ActionEvent e) {
      for (int row = 0; row < compv.getComponentModel().getRowCount(); row++) {
        JFMFile el = compv.getComponentModel().getFileAt(row);
        if (el == null || "..".equals(el.getDisplayName())) 
          continue; 
        if (el.isFile() || Options.getDirectoriesSelectedOnAsterisk()) {
          if (el.isMarked())
            compv.removeMarkedRow(row);
        }
      }
      JFMListView.updateMarkedStatus(compv); 
      compv.repaint();
    }
  }

  private static class MarkReverseRowAction implements Action {
    private JFMComponent compv = null;
    public MarkReverseRowAction(JFMComponent compv) {
      this.compv = compv;
    }
    //requested methods
    public Object getValue(String key){return null;}
    public void putValue(String key, Object value){}
    public void setEnabled(boolean b){}
    public boolean isEnabled(){return true;}
    public void addPropertyChangeListener(PropertyChangeListener listener){}
    public void removePropertyChangeListener(PropertyChangeListener listener){}

    public void actionPerformed(ActionEvent e) {
      for (int row = 0; row < compv.getComponentModel().getRowCount(); row++) {
        JFMFile el = compv.getComponentModel().getFileAt(row);
        if (el == null || "..".equals(el.getDisplayName())) 
          continue; 
        if (el.isFile() || Options.getDirectoriesSelectedOnAsterisk()) {
          if (el.isMarked())
            compv.removeMarkedRow(row);
          else
            compv.addMarkedRow(row);
        }
      }
      JFMListView.updateMarkedStatus(compv); 
      compv.repaint();
    }
  }

  private static class MarkRowAction implements Action {
    private JFMComponent compv = null;
    public MarkRowAction(JFMComponent compv) {
      this.compv = compv;
    }
    //requested methods
    public Object getValue(String key){return null;}
    public void putValue(String key, Object value){}
    public void setEnabled(boolean b){}
    public boolean isEnabled(){return true;}
    public void addPropertyChangeListener(PropertyChangeListener listener){}
    public void removePropertyChangeListener(PropertyChangeListener listener){}

    public void actionPerformed(ActionEvent e) {
      int row = compv.getSelectedRow();
      if (row >= 1 && row < compv.getComponentModel().getRowCount()) {
        JFMFile el = compv.getComponentModel().getFileAt(row);
        el.setMarked(!el.isMarked());
        if (el.isMarked())
          compv.addMarkedRow(row);
        else
          compv.removeMarkedRow(row);
      }
      compv.setSelectedRow(row); 
      JFMListView.updateMarkedStatus(compv); 
      compv.repaint();
    }
  }

  private static class RefreshAction implements Action {
    private JFMComponent compv = null;
    public RefreshAction(JFMComponent compv) {
      this.compv = compv;
    }
    //requested methods
    public Object getValue(String key){return null;}
    public void putValue(String key, Object value){}
    public void setEnabled(boolean b){}
    public boolean isEnabled(){return true;}
    public void addPropertyChangeListener(PropertyChangeListener listener){}
    public void removePropertyChangeListener(PropertyChangeListener listener){}

    public void actionPerformed(ActionEvent e){
      compv.getComponentModel().browseDirectory(compv.getComponentModel().getWorkingDirectory());
    }
  }

  private static class ChangePanelAction implements Action {
    private JFMComponent compv = null;
    public ChangePanelAction(JFMComponent compv) {
      this.compv = compv;
    }
    //requested methods
    public Object getValue(String key){return null;}
    public void putValue(String key, Object value){}
    public void setEnabled(boolean b){}
    public boolean isEnabled(){return true;}
    public void addPropertyChangeListener(PropertyChangeListener listener){}
    public void removePropertyChangeListener(PropertyChangeListener listener){}

    public void actionPerformed(ActionEvent e) {
      compv.requestPanelChange(); 
    }
  }

  private static class FileAction implements Action {
    private JFMComponent compv = null;
    public FileAction(JFMComponent compv) {
      this.compv = compv;
    }
    //requested methods
    public Object getValue(String key){return null;}
    public void putValue(String key, Object value){}
    public void setEnabled(boolean b){}
    public boolean isEnabled(){return true;}
    public void addPropertyChangeListener(PropertyChangeListener listener){}
    public void removePropertyChangeListener(PropertyChangeListener listener){}

    public void actionPerformed(ActionEvent e) {
      int row = compv.getSelectedRow();
      if (row >= 0 && row < compv.getComponentModel().getRowCount()) {
        processActionOnRow(compv, row);
      }
    }
  }

  private static class UpDirectoryAction implements Action {
    private JFMComponent compv = null;
    public UpDirectoryAction(JFMComponent compv) {
      this.compv = compv;
    }
    //requested methods
    public Object getValue(String key){return null;}
    public void putValue(String key, Object value){}
    public void setEnabled(boolean b){}
    public boolean isEnabled(){return true;}
    public void addPropertyChangeListener(PropertyChangeListener listener){}
    public void removePropertyChangeListener(PropertyChangeListener listener){}

    public void actionPerformed(ActionEvent e){
      processActionOnRow(compv, 0);
    }
  }

}
