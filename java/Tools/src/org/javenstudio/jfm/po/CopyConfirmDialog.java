package org.javenstudio.jfm.po;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

import org.javenstudio.common.util.Strings;
import org.javenstudio.jfm.filesystems.JFMFile; 
import org.javenstudio.jfm.main.Options; 


public class CopyConfirmDialog extends JDialog {
  private static final long serialVersionUID = 1L;
  
  private JPanel panel1 = new JPanel();
  private JPanel buttonsPanel = new JPanel();
  private JPanel infPanel = new JPanel();
  private JButton okButton = new JButton();
  private JButton cancelButton = new JButton();
  private JLabel jLabel1 = new JLabel();
  private JTextField copyFromTextField = new JTextField();
  private JLabel jLabel2 = new JLabel();
  private JTextField copyToTextField = new JTextField();
  private GridBagLayout gridBagLayout1 = new GridBagLayout();
  private FlowLayout flowLayout1 = new FlowLayout();
  private BorderLayout borderLayout1 = new BorderLayout();
  @SuppressWarnings("unused")
  private JFMFile[] copyFrom = null;
  private JFMFile copyTo = null;
  private boolean cancelled = true;
  private int operation = ProgressActionDialog.COPY; 

  public CopyConfirmDialog(Frame frame, String title, boolean modal) {
    super(frame, title, modal);
    try {
      jbInit();
      pack();
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  public CopyConfirmDialog() {
    this(null, "", false);
  }
  
  public void setOperation(int op) {
    if (operation == ProgressActionDialog.MOVE) 
      jLabel1.setText(Strings.get("Move file(s):"));
    else if (operation == ProgressActionDialog.ARCHIVE) 
      jLabel1.setText(Strings.get("Archive file(s):"));
    else if (operation == ProgressActionDialog.EXTRACT) 
      jLabel1.setText(Strings.get("Extract file(s):"));
    else
      jLabel1.setText(Strings.get("Copy file(s):"));
    operation = op; 
  }
  
  void jbInit() throws Exception {
    setResizable(false);
    panel1.setLayout(borderLayout1);
    buttonsPanel.setLayout(flowLayout1);
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

    jLabel1.setText(Strings.get("Copy file(s):"));
    jLabel2.setText(Strings.get("To directory:"));
    getContentPane().add(panel1);
    buttonsPanel.add(okButton, null);
    buttonsPanel.add(cancelButton, null);
    panel1.add(buttonsPanel, BorderLayout.SOUTH);
    panel1.add(infPanel, BorderLayout.CENTER);
    panel1.getRootPane().setDefaultButton(okButton);

    infPanel.add(jLabel1,            new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 7, 0, 8), 15, 8));
    infPanel.add(copyFromTextField,  new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 5, 0, 10), 300, 5));
    infPanel.add(jLabel2,            new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(10, 6, 10, 1), 15, 8));
    infPanel.add(copyToTextField,    new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(10, 5, 10, 10), 300, 5));

    copyFromTextField.setFont(Options.getPanelsFont());
    copyFromTextField.setEditable(false); 
    copyToTextField.setFont(Options.getPanelsFont());
    copyToTextField.setEditable(false); 
  }

  void okButton_actionPerformed(ActionEvent e) {
    setCancelled(false);
    this.dispose();
  }

  void cancelButton_actionPerformed(ActionEvent e) {
    setCancelled(true);
    this.dispose();
  }

  public void setCopyFrom(JFMFile[] copyFrom) {
    this.copyFrom = copyFrom;
    copyFromTextField.setText(getFilesText(copyFrom));
    if (operation == ProgressActionDialog.MOVE)
      jLabel1.setText(Strings.format("Move file(s) at %1$s:", getFilesFsName(copyFrom)));
    else if (operation == ProgressActionDialog.ARCHIVE)
      jLabel1.setText(Strings.format("Archive file(s) at %1$s:", getFilesFsName(copyFrom)));
    else if (operation == ProgressActionDialog.EXTRACT)
      jLabel1.setText(Strings.format("Extract file(s) at %1$s:", getFilesFsName(copyFrom)));
    else
      jLabel1.setText(Strings.format("Copy file(s) at %1$s:", getFilesFsName(copyFrom)));
  }

  private String getFilesText(JFMFile[] files) {
    StringBuffer sbuf = new StringBuffer(); 
    for (int i=0; files != null && i < files.length; i++) {
      if (sbuf.length() > 0) sbuf.append(", "); 
      sbuf.append(files[i].getPath()); 
    }
    return sbuf.toString(); 
  }

  private String getFilesFsName(JFMFile[] files) {
    if (files != null && files.length > 0)
      return files[0].getFsName(); 
    else
      return Strings.get("unknown"); 
  }

  public void setCopyTo(JFMFile copyTo) {
    this.copyTo = copyTo;
    copyToTextField.setText(copyTo.getPath());
    jLabel2.setText(Strings.format("To directory at %1$s:", copyTo.getFsName()));
  }

  public JFMFile getCopyTo() {
    copyTo.setPath(copyToTextField.getText());
    return copyTo;
  }

  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }

  public boolean isCancelled() {
    return cancelled;
  }

}
