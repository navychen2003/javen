package org.javenstudio.jfm.po;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

import org.javenstudio.common.util.Strings;
import org.javenstudio.jfm.main.Options; 


public class InputDialog extends JDialog {
  private static final long serialVersionUID = 1L;
  
  private JPanel panel1 = new JPanel();
  private JPanel msgPanel = new JPanel();
  private JPanel buttonsPanel = new JPanel();
  private JPanel infPanel = new JPanel();
  private JButton okButton = new JButton();
  private JButton cancelButton = new JButton();
  private JLabel jLabelMessage = new JLabel();
  private JLabel jLabel1 = new JLabel();
  private JTextField pathTextField = new JTextField();
  private JLabel jLabel2 = new JLabel();
  private JTextField inputTextField = new JTextField();
  private GridBagLayout gridBagLayout1 = new GridBagLayout();
  private FlowLayout flowLayout1 = new FlowLayout();
  private BorderLayout borderLayout1 = new BorderLayout();
  private boolean cancelled = true;

  public InputDialog(Frame frame, String title, boolean modal) {
    super(frame, title, modal);
    try {
      jbInit();
      pack();
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  public InputDialog() {
    this(null, "", false);
  }
  
  void jbInit() throws Exception {
    setResizable(false);
    panel1.setLayout(borderLayout1);
    buttonsPanel.setLayout(flowLayout1);
    msgPanel.setLayout(flowLayout1);
    infPanel.setLayout(gridBagLayout1);
    okButton.setText(Strings.get("OK"));
    okButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        okButton_actionPerformed(e);
      }
    });
    cancelButton.setText(Strings.get("Cancel"));
    cancelButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        cancelButton_actionPerformed(e);
      }
    });

    jLabel1.setText(Strings.get("Directory:"));
    jLabel2.setText(Strings.get("Input Name:"));

    getContentPane().add(panel1);
    buttonsPanel.add(okButton, null);
    buttonsPanel.add(cancelButton, null);
    msgPanel.add(jLabelMessage, null); 
    panel1.add(buttonsPanel, BorderLayout.SOUTH);
    panel1.add(infPanel, BorderLayout.CENTER);
    panel1.add(msgPanel, BorderLayout.NORTH);
    panel1.getRootPane().setDefaultButton(okButton);

    infPanel.add(jLabel1,       new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 5, 0, 10), 15, 8));
    infPanel.add(pathTextField, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 5, 0, 10), 300, 5));
    infPanel.add(jLabel2,       new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(10, 5, 10, 10), 15, 8));
    infPanel.add(inputTextField,new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(10, 5, 10, 10), 300, 5));

    pathTextField.setFont(Options.getPanelsFont());
    pathTextField.setEditable(false); 
    inputTextField.setFont(Options.getPanelsFont());
    inputTextField.setEditable(true); 
  }

  void okButton_actionPerformed(ActionEvent e) {
    setCancelled(false);
    this.dispose();
  }

  void cancelButton_actionPerformed(ActionEvent e) {
    setCancelled(true);
    this.dispose();
  }

  public void setMessage(String msg, String pathName, String inputName) {
    setMessage(msg); 
    setInputPathName(pathName); 
    setInputTextName(inputName); 
    pack(); 
  }

  private void setMessage(String msg) {
    jLabelMessage.setText(msg); 
  }

  private void setInputPathName(String name) {
    jLabel1.setText(name); 
  }

  public void setInputPath(String dir) {
    pathTextField.setText(dir);
    pathTextField.repaint();
  }

  private void setInputTextName(String name) {
    jLabel2.setText(name); 
  }

  public void setInputText(String name) {
    inputTextField.setText(name);
    inputTextField.repaint();
  }

  public String getInputText() {
    return Options.trim(inputTextField.getText()); 
  }

  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }

  public boolean isCancelled() {
    return cancelled;
  }

  public void setVisible(boolean b) {
    //pack(); 
    setLocationRelativeTo(Options.getMainFrame());
    super.setVisible(b); 
  }
}
