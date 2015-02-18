package org.javenstudio.jfm.main;

import org.javenstudio.jfm.po.*;
import org.javenstudio.jfm.views.*;
import org.javenstudio.jfm.event.*;
import org.javenstudio.jfm.filesystems.JFMFileSystem; 

import java.awt.*;
import javax.swing.*;
import java.util.prefs.Preferences;


public class MainPanel extends JPanel {
  private static final long serialVersionUID = 1L;

  private BorderLayout borderLayout1 = new BorderLayout();
  private ButtonsPanel btPanel = new ButtonsPanel();
  private JSplitPane split = new JSplitPane();
  private Preferences prefs = Options.getPreferences();
  private JFMView leftPanel = null;
  private JFMView rightPanel = null;

  public MainPanel() {
    try {
      jbInit();
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  void jbInit() throws Exception {
    removeSplitKeyBindings();
    this.setLayout(borderLayout1);

    this.add(btPanel,BorderLayout.SOUTH);
    this.add(split,  BorderLayout.CENTER);

    leftPanel = createLeftView();
    rightPanel = createRightView();
    Options.setActivePanel(rightPanel);
    Options.setInactivePanel(leftPanel);

    split.add(leftPanel, JSplitPane.LEFT);
    split.add(rightPanel, JSplitPane.RIGHT);
    split.setDividerLocation(MainFrame.MAIN_WIDTH/2 -10);
    setButtonsPanel();

    Broadcaster.addChangeViewListener(new ChangeViewListener(){
      public void viewChanged(ChangeViewEvent ev){           
        boolean changed = false; 
        if (ev.getFilesystemName() != null) {
            changed = changeLeftPanel(ev); 
            changed = changeRightPanel(ev) ? true : changed; 

        } else {
          if (Options.getActivePanel().equals(leftPanel)) 
            changed = changeLeftPanel(ev); 
          else 
            changed = changeRightPanel(ev); 
        }
        if (changed) {
          split.setDividerLocation(split.getWidth()/2);
        }
      }
    });
    
    Broadcaster.addChangeDirectoryListener(new ChangeDirectoryListener(){
      public void changeDirectory(ChangeDirectoryEvent event){
       if(event.getDirectory() == null || event.getDirectory().getAbsolutePath() == null) 
         return;
       if(event.getSource() == leftPanel) {
         Options.getPreferences().put(Options.JFM_LEFTVIEWPANELDIR_PREF, 
                                      event.getDirectory().getAbsolutePath());
       } else {
         Options.getPreferences().put(Options.JFM_RIGHTVIEWPANELDIR_PREF, 
                                      event.getDirectory().getAbsolutePath());
       }
      }
    });
  }

  private boolean changeLeftPanel(ChangeViewEvent ev) {
    boolean changed = false; 
    if (ev.getViewRep() != null) {
      Options.getPreferences().put(Options.JFM_LEFTVIEWPANEL_PREF,
                                   ev.getViewRep().getClassName());
      changed = true;
    }

    @SuppressWarnings("unused")
	String name = ev.getFilesystemName(); 
    String changeClassName = ev.getFilesystemClassName();
    String currentClassName = leftPanel.getFilesystemName();

    if (changeClassName != null) {
      if (ev.isReconnect()) {
        if (changeClassName.equals(currentClassName) || 
            Options.getActivePanel().equals(leftPanel)) {
          Options.getPreferences().put(Options.JFM_LEFTVIEWPANEL_FILESYSTEM_PREF,
                                       ev.getFilesystemClassName());
          changed = true;
        }

      } else if (!changeClassName.equals(currentClassName)) {
        Options.getPreferences().put(Options.JFM_LEFTVIEWPANEL_FILESYSTEM_PREF,
                                     ev.getFilesystemClassName());
        changed = true;
      }
    }

    if (changed) {
      leftPanel = createLeftView();
      split.setLeftComponent(leftPanel);
    }

    return changed; 
  }

  private boolean changeRightPanel(ChangeViewEvent ev) {
    boolean changed = false; 
    if (ev.getViewRep() != null) {
      Options.getPreferences().put(Options.JFM_RIGHTVIEWPANEL_PREF,
                                   ev.getViewRep().getClassName());
      changed = true;
    }

    @SuppressWarnings("unused")
	String name = ev.getFilesystemName(); 
    String changeClassName = ev.getFilesystemClassName();
    String currentClassName = rightPanel.getFilesystemName();

    if (changeClassName != null) {
      if (ev.isReconnect()) {
        if (changeClassName.equals(currentClassName) || 
            Options.getActivePanel().equals(rightPanel)) {
          Options.getPreferences().put(Options.JFM_RIGHTVIEWPANEL_FILESYSTEM_PREF,
                                       ev.getFilesystemClassName());
          changed = true;
        }

      } else if (!changeClassName.equals(currentClassName)) {
        Options.getPreferences().put(Options.JFM_RIGHTVIEWPANEL_FILESYSTEM_PREF,
                                     ev.getFilesystemClassName());
        changed = true;
      }
    }

    if (changed) {
      rightPanel = createRightView();
      split.setRightComponent(rightPanel);
    }

    return changed; 
  }

  public String getActiveViewClassName() {
    JFMView view = Options.getActivePanel(); 
    if (view != null) 
      return view.getClass().getName(); 
    else 
      return getRightViewClassName(); 
  }

  public String getActiveFilesystemClassName() {
    JFMView view = Options.getActivePanel(); 
    if (view != null) 
      return view.getFilesystem().getClass().getName(); 
    else 
      return getRightFilesystemClassName(); 
  }

  private String getRightViewClassName() {
    return prefs.get(Options.JFM_RIGHTVIEWPANEL_PREF, JFMView.DEFAULT_VIEW.getClassName());
  }

  private String getRightFilesystemClassName() {
    return prefs.get(Options.JFM_RIGHTVIEWPANEL_FILESYSTEM_PREF, JFMFileSystem.LOCAL_FILESYSTEM);
  }

  private JFMView createRightView() {
    //create the default view
    JFMView view = JFMView.createView(
        JFMView.getViewRepresentation(getRightViewClassName()), 
                                      getRightFilesystemClassName());
    prefs.put(Options.JFM_RIGHTVIEWPANEL_FILESYSTEM_PREF, view.getFilesystemName());
    view.requestFocus();
    return view;
  }

  private String getLeftViewClassName() {
    return prefs.get(Options.JFM_LEFTVIEWPANEL_PREF, JFMView.DEFAULT_VIEW.getClassName());
  }

  private String getLeftFilesystemClassName() {
    return prefs.get(Options.JFM_LEFTVIEWPANEL_FILESYSTEM_PREF, JFMFileSystem.LOCAL_FILESYSTEM); 
  }

  private JFMView createLeftView() {
    //create the default view
    JFMView view = JFMView.createView(
        JFMView.getViewRepresentation(getLeftViewClassName()), 
                                      getLeftFilesystemClassName());
    prefs.put(Options.JFM_LEFTVIEWPANEL_FILESYSTEM_PREF, view.getFilesystemName());
    return view;
  }

  private void removeSplitKeyBindings(){
    ActionMap newSplitActionMap = new ActionMap();
    split.setActionMap(newSplitActionMap);
  }

  private void setButtonsPanel(){
/*
    Vector buttons=new Vector();

//    JButton f1Button=new JButton("Help (F1)");
//    HelpAction help=new HelpAction();
//    f1Button.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F1"),"helpButton");
//    f1Button.getActionMap().put("helpButton",help);
//    f1Button.addActionListener(help);    

    JButton f1Button=new JButton("Properties (F1)");
    FilePropertiesAction info=new FilePropertiesAction();
    f1Button.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F1"),"infoButton");
    f1Button.getActionMap().put("infoButton",info);
    f1Button.addActionListener(info);    

//    JButton f2Button=new JButton("Menu (F2)");
//    PanelMenuAction menu=new PanelMenuAction();
//    f2Button.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F2"),"menuButton");
//    f2Button.getActionMap().put("menuButton",menu);
//    f2Button.addActionListener(menu);

    JButton f2Button=new JButton("Create (F2)");
    CreateFileAction createFile=new CreateFileAction();
    f2Button.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F2"),"createButton");
    f2Button.getActionMap().put("createButton",createFile);
    f2Button.addActionListener(createFile);

    JButton f3Button=new JButton("View (F3)");
    ViewFileAction view=new ViewFileAction();
    f3Button.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F3"),"viewButton");
    f3Button.getActionMap().put("viewButton",view);
    f3Button.addActionListener(view);

    JButton f4Button=new JButton("Edit (F4)");
    EditFileAction edit=new EditFileAction();
    f4Button.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F4"),"editButton");
    f4Button.getActionMap().put("editButton",edit);
    f4Button.addActionListener(edit);

    JButton f5Button=new JButton("Copy (F5)");
    CopyAction copy=new CopyAction();
    f5Button.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F5"),"copyButton");
    f5Button.getActionMap().put("copyButton",copy);
    f5Button.addActionListener(copy);

    JButton f6Button=new JButton("Move (F6)");
    MoveAction move=new MoveAction();
    f6Button.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F6"),"moveButton");
    f6Button.getActionMap().put("moveButton",move);
    f6Button.addActionListener(move);

    JButton f7Button=new JButton("Mkdir (F7)");
    MkdirAction mkdir=new MkdirAction();
    f7Button.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F7"),"mkdirButton");
    f7Button.getActionMap().put("mkdirButton",mkdir);
    f7Button.addActionListener(mkdir);

    JButton f8Button=new JButton("Delete (F8)");
    DeleteAction del=new DeleteAction();
    f8Button.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F8"),"delButton");
    f8Button.getActionMap().put("delButton",del);
    f8Button.addActionListener(del);

    JButton f9Button=new JButton("Rename (F9)");
    RenameAction ren=new RenameAction();
    f9Button.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F9"),"renButton");
    f9Button.getActionMap().put("renButton",ren);
    f9Button.addActionListener(ren);

//    JButton f10Button=new JButton("Quit");
//    QuitAction quit=new QuitAction();
//    f10Button.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F10"),"quitButton");
//    f10Button.getActionMap().put("quitButton",quit);
//    f10Button.addActionListener(quit);

    buttons.addElement(f1Button);
    buttons.addElement(f2Button);
    buttons.addElement(f3Button);
    buttons.addElement(f4Button);
    buttons.addElement(f5Button);
    buttons.addElement(f6Button);
    buttons.addElement(f7Button);
    buttons.addElement(f8Button);
    buttons.addElement(f9Button);
//    buttons.addElement(f10Button);

    btPanel.setButtons(buttons);
*/
  }

}
