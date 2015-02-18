package org.javenstudio.jfm.views.list;

import java.awt.BorderLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.javenstudio.common.util.Strings;
import org.javenstudio.jfm.event.*;
import org.javenstudio.jfm.filesystems.JFMFile;
import org.javenstudio.jfm.main.Options;
import org.javenstudio.jfm.po.CopyAction;
import org.javenstudio.jfm.po.MoveAction;
import org.javenstudio.jfm.po.ArchiveAction;
import org.javenstudio.jfm.po.ExtractAction;
import org.javenstudio.jfm.po.ButtonsPanel; 
import org.javenstudio.jfm.views.list.ListView;
import org.javenstudio.jfm.views.list.PanelChangeRequestListener;


/**
 * This panel implements a detailed view of the files 
 * @author sergiu
 */
public abstract class JFMListView extends ListView {
  private static final long serialVersionUID = 1L;

  /**
   * Constructor. 
   */
  public JFMListView(String fs) {
    super(fs);
  }
  
  protected abstract void initModel(); 
  protected abstract JFMComponent getComponent(); 
  protected abstract JFMModel getComponentModel(); 
  protected abstract void processFocusGained(FocusEvent e); 
  protected abstract void processFocusLost(FocusEvent e); 
  protected abstract boolean checkPointAtRow(MouseEvent event, boolean selectRow); 
  
  protected void init() throws Exception{    
    final JComponent compv = getViewComponent(); 
    initModel(); 

    this.setLayout(new BorderLayout());
    scroll = new JScrollPane(compv);
    scroll.getViewport().setBackground(Options.getBackgroundColor());

    compv.setBackground(Options.getBackgroundColor());
    compv.setForeground(Options.getForegroundColor());
    compv.setFont(Options.getPanelsFont());
    
    add(scroll, BorderLayout.CENTER);
    add(topPanel, BorderLayout.NORTH);

    Broadcaster.addFontChangeListener(new FontChangeListener() {
      public void fontChanged(FontChangeEvent ev){
        compv.setFont(Options.getPanelsFont());
      }
    });  
    
    Broadcaster.addColorChangeListener(new ColorChangeListener() {
      public void colorChanged(ColorChangeEvent event) {
        if (event.getColorType() == ColorChangeEvent.BACKGROUND) {
          scroll.getViewport().setBackground(Options.getBackgroundColor());
          compv.setBackground(Options.getBackgroundColor());
        }
        if (event.getColorType() == ColorChangeEvent.FOREGROUND) {
          compv.setForeground(Options.getForegroundColor());
        }
      }
    });

    Broadcaster.addChangeDirectoryListener(new ChangeDirectoryListener() {
      public void changeDirectory(ChangeDirectoryEvent e) {
        if (e.getSource().equals(getComponent())) {
          JFMListView.this.changeDirectory(e.getDirectory());
          return;
        }
        if ((e.getSource() instanceof ButtonsPanel) && isActive) {
          JFMListView.this.changeDirectory(e.getDirectory());
          return;
        }
        if ((e.getSource() instanceof javax.swing.Action) && isActive) { 
          JFMListView.this.changeDirectory(getCurrentWorkingDirectory());
          return;
        }
        //if we were the target of a Copyaction or Move action, 
        //then we're not active, but we still should update ourselves
        if (((e.getSource() instanceof CopyAction) || 
             (e.getSource() instanceof MoveAction) ||
             (e.getSource() instanceof ExtractAction) || 
             (e.getSource() instanceof ArchiveAction)) && !isActive){  
          JFMListView.this.changeDirectory(getCurrentWorkingDirectory());
          return;
        }
      }
    }); 

    Broadcaster.addBrowseDirectoryListener(new BrowseDirectoryListener() {
      public void browseDirectory(BrowseDirectoryEvent e) {
        if ((e.getSource() instanceof JFMModel) &&  isActive) {
          statusLabel.setText(getCurrentWorkingDirectory().getAbsolutePath());
          return;
        }
      }
    });

    compv.addFocusListener(new FocusListener(){
      public void focusGained(FocusEvent e) {
        titleLabel.setForeground(UIManager.getColor("Label.foreground"));
        statusLabel.setForeground(UIManager.getColor("Label.foreground"));
        if (!JFMListView.this.isActive())
          Options.setActivePanel(JFMListView.this);
        processFocusGained(e); 
      } 
      public void focusLost(FocusEvent e){
        titleLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        statusLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        processFocusLost(e); 
      }
    });    

    getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;
        if (getSelectionModel().isSelectionEmpty()) return;        

        int firstIndex = getSelectionModel().getMinSelectionIndex();
        int lastIndex = getSelectionModel().getMaxSelectionIndex();        

        JFMFile file = getSelectedFile(); 
        if (file != null) {
          ChangeSelectionEvent event = new ChangeSelectionEvent();
          event.setFile(file);
          event.setFirstIndex(firstIndex); 
          event.setLastIndex(lastIndex); 
          event.setSource(JFMListView.this);
          Broadcaster.notifyChangeSelectionListeners(event);
        }
      }
    }); 

    setPanelChangeRequestListener(new PanelChangeRequestListener() {
      public void requestPanelChange() {
        ChangePanelEvent ev = new ChangePanelEvent();
        ev.setSource(JFMListView.this);
        Broadcaster.notifyChangePanelListeners(ev);    
      }
    });

    compv.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent event) {
        compv.requestFocus(); 
        if (SwingUtilities.isRightMouseButton(event)) {
          boolean pointAtRow = checkPointAtRow(event, true); 
          JFMFile file = ((JFMComponent)compv).getSelectedFile(); 
          Options.updateMenuStatus(pointAtRow, file); 
          Options.getPopupMenu().show(compv, event.getX(), event.getY()); 
        }
      }
    });

    scroll.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent event) {
        compv.requestFocus(); 
        if (SwingUtilities.isRightMouseButton(event)) {
          Options.updateMenuStatus(false, null); 
          Options.getPopupMenu().show(scroll, event.getX(), event.getY());
        }
      }
    });

    topPanel.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent event) {
        compv.requestFocus();
      }
    });

    changeDirectory(filesystem.getStartDirectory());
  }

  protected void setPanelChangeRequestListener(PanelChangeRequestListener l) {
    getComponent().setPanelChangeRequestListener(l);
  }

  protected ListSelectionModel getSelectionModel() {
    return getComponent().getSelectionModel();
  }

  protected void browseDirectory(JFMFile dir) {
    getComponentModel().browseDirectory(dir);
  }

  protected void clearMarkedRows() {
    getComponent().clearMarkedRows();
    getComponent().setSelectedRow(-1);
    updateSelectedStatus(); 
  }

  protected void changeDirectory(JFMFile file) {
    browseDirectory(file);
    clearMarkedRows();
  }

  public JFMFile getCurrentWorkingDirectory(){
    return getComponentModel().getWorkingDirectory();
  }

  public JFMFile getSelectedFile() {
    return getComponent().getSelectedFile();
  }

  public JFMFile[] getSelectedFiles() {
    return getMarkedFiles(getComponent()); 
  }

  protected void updateSelectedStatus() {
    updateMarkedStatus(getComponent()); 
  }

  public static JFMFile[] getMarkedFiles(JFMComponent compv) {
    ArrayList<JFMFile> files = new ArrayList<JFMFile>();
    for (int i = 0; i < compv.getComponentModel().getRowCount(); i++) {
      JFMFile file = compv.getComponentModel().getFileAt(i);
      if (file != null && file.isMarked()) 
        files.add(file); 
    }
    if (files.size() == 0) {
      JFMFile file = compv.getSelectedFile();
      if (file != null && file.isMarked()) 
        files.add(file); 
    }
    if (files.size() > 0) 
      return files.toArray(new JFMFile[files.size()]);
    return null; 
  }

  public static void updateMarkedStatus(JFMComponent compv) {
    int fileCount = 0, dirCount = 0; 
    long totalSize = 0; 
    JFMFile[] files = getMarkedFiles(compv); 
    for (int i=0; files != null && i < files.length; i++) {
      JFMFile file = files[i]; 
      if (file == null) continue; 
      if (file.isFile()) {
        fileCount ++; 
        totalSize += file.length(); 
      } else
        dirCount ++; 
    }
    if (fileCount > 0 || dirCount > 0) {
      updateMainStatus(compv, 
          Strings.format("selected %1$s files (%2$s/%3$s bytes) and %4$s directories", 
          ""+fileCount, Options.byteDesc(totalSize), ""+totalSize, ""+dirCount)); 
    } else
      updateMainStatus(compv, null); 
  }

  public static void updateMainStatus(Object source, String status) {
    ChangeStatusEvent event = new ChangeStatusEvent();
    event.setStatus(status);
    event.setSource(source);
    Broadcaster.notifyChangeStatusListeners(event);
  }

}
