package org.javenstudio.jfm.po;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.Vector;

import org.javenstudio.jfm.event.*;
import org.javenstudio.jfm.main.Options; 
import org.javenstudio.jfm.filesystems.JFMFile;


public class ButtonsPanel extends JPanel {
  private static final long serialVersionUID = 1L;
  
  @SuppressWarnings("rawtypes")
  private Vector buttons = null;
  private JPanel commandPanel = new JPanel(new BorderLayout());
  private JPanel btPanel = new JPanel();
  private JLabel currentPath = new JLabel();
  private JTextField commandField = new JTextField();
  @SuppressWarnings("unused")
  private JFMFile workingDir = null;

  @SuppressWarnings("rawtypes")
  public ButtonsPanel(Vector buttons) {
    try {
      jbInit();
      setButtons(buttons);
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  public ButtonsPanel(){
    this(null);
  }

  @SuppressWarnings("rawtypes")
  public void setButtons(Vector newButtons) {
    buttons = newButtons;
    btPanel.removeAll();
    if (buttons != null) {
      addButtons();
    }
  }

  private void addButtons() {
    btPanel.setLayout(new BoxLayout(btPanel,BoxLayout.X_AXIS));
    for (int i=0;i<buttons.size();i++) {
      if (buttons.elementAt(i) instanceof JButton) {
        btPanel.add((JButton)buttons.elementAt(i));
      }
    }
    //this.revalidate();
  }

  @SuppressWarnings("rawtypes")
  public Vector getButtons() {
    return buttons;
  }

  private void jbInit() throws Exception {
    this.setLayout(new BorderLayout());
    this.add(commandPanel,BorderLayout.NORTH);
    //this.add(btPanel,BorderLayout.CENTER);
    addCommandPanelStuff();
  }

  private void addCommandPanelStuff() {
   commandField.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String command = commandField.getText(); 
        if (command == null || command.length() <= 0)
          return;

        /* try {
          @SuppressWarnings("unused")
          Process p = Runtime.getRuntime().exec(command,null,null);
          CommandOutputViewDialog d=new CommandOutputViewDialog(
              JOptionPane.getFrameForComponent(commandField), "Output", false);
          d.setLocationRelativeTo(commandField);
          d.setMonitoringProcess(p);
          d.show();
          commandField.setText("");
        } catch(java.io.IOException ex) {
          ex.printStackTrace();
        } */

        JFMFile dir = Options.getActivePanel().getFile(command); 
        if (dir != null && dir.isFile()) dir = dir.getParentFile(); 
        if (dir == null || !dir.exists()) return; 

        ChangeDirectoryEvent ev=new ChangeDirectoryEvent();
        ev.setDirectory(dir);
        ev.setSource(ButtonsPanel.this);
        Broadcaster.notifyChangeDirectoryListeners(ev);
      }
    });

    Broadcaster.addChangeDirectoryListener(new ChangeDirectoryListener() {
      public void changeDirectory(ChangeDirectoryEvent e) {
        if (e.getSource() instanceof ButtonsPanel) return; 
        if (e.getDirectory() != null) {
          JFMFile dir = e.getDirectory(); 
          currentPath.setText(dir.getFsSchemeName()); 
          workingDir = e.getDirectory();
        }
      }
    });

    Broadcaster.addChangeSelectionListener(new ChangeSelectionListener() {
      public void changeSelection(ChangeSelectionEvent e) {
        if (e.getSource() instanceof ButtonsPanel) return;
        if (e.getFile() != null) {
          JFMFile file = e.getFile();
          currentPath.setText(file.getFsSchemeName());
          commandField.setText(file.getAbsolutePath());
        }
      }
    });

    commandField.setFont(Options.getPanelsFont());
    Broadcaster.addFontChangeListener(new FontChangeListener() {
      public void fontChanged(FontChangeEvent ev){
        commandField.setFont(Options.getPanelsFont());
      }
    });

    commandPanel.add(currentPath, BorderLayout.WEST);
    commandPanel.add(commandField,BorderLayout.CENTER);
  }

}
