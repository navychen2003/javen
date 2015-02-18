package org.javenstudio.jfm.po;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

import org.javenstudio.common.util.Strings;
import org.javenstudio.jfm.filesystems.JFMFile; 
import org.javenstudio.jfm.main.Options; 


public class ArchiveConfirmDialog extends JDialog {
  private static final long serialVersionUID = 1L;
  
  private JPanel panel1 = new JPanel();
  private JPanel buttonsPanel = new JPanel();
  private JPanel infPanel = new JPanel();
  private JButton okButton = new JButton();
  private JButton cancelButton = new JButton();
  private JLabel jLabel1 = new JLabel();
  private JTextField archiveFromTextField = new JTextField();
  private JLabel jLabel2 = new JLabel();
  private JTextField archiveToTextField = new JTextField();
  private JLabel jLabel3 = new JLabel();
  private JTextField archiveNameTextField = new JTextField();
  private JLabel jLabel4 = new JLabel();
  @SuppressWarnings("rawtypes")
  private JComboBox archiveTypeBox = new JComboBox(); 
  private GridBagLayout gridBagLayout1 = new GridBagLayout();
  private FlowLayout flowLayout1 = new FlowLayout();
  private BorderLayout borderLayout1 = new BorderLayout();
  @SuppressWarnings("unused")
  private JFMFile[] archiveFrom = null;
  private JFMFile archiveTo = null;
  private boolean cancelled = true;

  public ArchiveConfirmDialog(Frame frame, String title, boolean modal) {
    super(frame, title, modal);
    try {
      jbInit();
      pack();
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  public ArchiveConfirmDialog() {
    this(null, "", false);
  }
  
  @SuppressWarnings("unchecked")
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

    jLabel1.setText("Archive file(s):");
    jLabel2.setText("To directory:");
    jLabel3.setText("Archive name:");
    jLabel4.setText("Archive type:");

    getContentPane().add(panel1);
    buttonsPanel.add(okButton, null);
    buttonsPanel.add(cancelButton, null);
    panel1.add(buttonsPanel, BorderLayout.SOUTH);
    panel1.add(infPanel, BorderLayout.CENTER);
    panel1.getRootPane().setDefaultButton(okButton);

    infPanel.add(jLabel1,              new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 5, 0, 10), 25, 8));
    infPanel.add(archiveFromTextField, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 5, 0, 10), 336, 5));
    infPanel.add(jLabel2,              new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(10, 5, 0, 10), 12, 8));
    infPanel.add(archiveToTextField,   new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(10, 5, 0, 10), 336, 5));
    infPanel.add(jLabel3,              new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(10, 5, 0, 10), 12, 8));
    infPanel.add(archiveNameTextField, new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(10, 5, 0, 10), 336, 5));
    infPanel.add(jLabel4,              new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(10, 5, 10, 10), 12, 8));
    infPanel.add(archiveTypeBox,       new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(10, 5, 10, 10), 6, 5));

    archiveFromTextField.setFont(Options.getPanelsFont());
    archiveFromTextField.setEditable(false); 
    archiveToTextField.setFont(Options.getPanelsFont());
    archiveToTextField.setEditable(false); 
    archiveNameTextField.setFont(Options.getPanelsFont());

    String[] types = Options.getArchiveTypesSupported(); 
    for (int i=0; types != null && i < types.length; i++) {
      archiveTypeBox.addItem(types[i]); 
      if (i == 0) archiveTypeBox.setSelectedIndex(i); 
    }
    archiveTypeBox.setMaximumRowCount(types!=null?types.length:5); 
  }

  void okButton_actionPerformed(ActionEvent e) {
    setCancelled(false);
    this.dispose();
  }

  void cancelButton_actionPerformed(ActionEvent e) {
    setCancelled(true);
    this.dispose();
  }

  public void setArchiveFrom(JFMFile[] archiveFrom) {
    this.archiveFrom = archiveFrom;
    archiveFromTextField.setText(getFilesText(archiveFrom));
    //jLabel1.setText("Archive file(s) at "+getFilesFsName(archiveFrom)+":");
  }

  private String getFilesText(JFMFile[] files) {
    StringBuffer sbuf = new StringBuffer(); 
    for (int i=0; files != null && i < files.length; i++) {
      if (sbuf.length() > 0) sbuf.append(", "); 
      sbuf.append(files[i].getPath()); 
    }
    return sbuf.toString(); 
  }

  @SuppressWarnings("unused")
  private String getFilesFsName(JFMFile[] files) {
    if (files != null && files.length > 0)
      return files[0].getFsName(); 
    else
      return Strings.get("unknown"); 
  }

  public void setArchiveTo(JFMFile archiveTo) {
    this.archiveTo = archiveTo;
    archiveToTextField.setText(archiveTo.getPath());
    //jLabel2.setText("To directory at "+archiveTo.getFsName()+":");
  }

  public JFMFile getArchiveTo() {
    archiveTo.setPath(archiveToTextField.getText());
    return archiveTo;
  }

  public String getArchiveName() {
    return Options.trim(archiveNameTextField.getText()); 
  }

  public String getArchiveType() {
    return (String)archiveTypeBox.getSelectedItem(); 
  }

  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }

  public boolean isCancelled() {
    return cancelled;
  }

}
