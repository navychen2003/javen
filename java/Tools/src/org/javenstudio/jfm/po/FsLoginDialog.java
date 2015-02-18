package org.javenstudio.jfm.po;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.text.DecimalFormat; 

import org.javenstudio.common.util.Strings;
import org.javenstudio.jfm.main.Options; 


public class FsLoginDialog extends JDialog {
  private static final long serialVersionUID = 1L;
  
  private JPanel panel1 = new JPanel();
  private JPanel buttonsPanel = new JPanel();
  private JPanel infPanel = new JPanel();
  private JButton okButton = new JButton();
  private JButton cancelButton = new JButton();
  private JLabel jLabel1 = new JLabel();
  private JTextField hostField = new JTextField();
  private JLabel jLabel2 = new JLabel();
  private JFormattedTextField portField = new JFormattedTextField(new DecimalFormat("#"));
  private JLabel jLabel3 = new JLabel();
  private JTextField userField = new JTextField();
  private JLabel jLabel4 = new JLabel();
  private JPasswordField passwordField = new JPasswordField();
  private JLabel jLabel5 = new JLabel();
  private JTextField groupField = new JTextField();
  private JLabel jLabel6 = new JLabel();
  private JTextField privatekeyField = new JTextField();
  private JButton browseButton = new JButton(Strings.get("Browse"));
  private GridBagLayout gridBagLayout1 = new GridBagLayout();
  private FlowLayout flowLayout1 = new FlowLayout();
  private BorderLayout borderLayout1 = new BorderLayout();
  private boolean canceled = true; 


  public FsLoginDialog(Frame frame, String title, boolean modal) {
    this(frame, title, modal, false); 
  }

  public FsLoginDialog(Frame frame, String title, boolean modal, boolean nogroup) {
	this(frame, title, modal, nogroup, false);
  }
  
  public FsLoginDialog(Frame frame, String title, boolean modal, boolean nogroup, boolean noprivate) {
    super(frame, title, modal);
    try {
      jbInit(nogroup, noprivate);
      pack();
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  void jbInit(boolean nogroup, boolean noprivate) throws Exception {
    setResizable(false);
    panel1.setLayout(borderLayout1);
    buttonsPanel.setLayout(flowLayout1);
    infPanel.setLayout(gridBagLayout1);
    okButton.setText(Strings.get("Login"));
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

    getContentPane().add(panel1);
    buttonsPanel.add(okButton, null);
    buttonsPanel.add(cancelButton, null);
    panel1.add(buttonsPanel, BorderLayout.SOUTH);
    panel1.add(infPanel, BorderLayout.CENTER);
    panel1.getRootPane().setDefaultButton(okButton);

    jLabel1.setText(Strings.get("Server Host:"));
    jLabel2.setText(Strings.get("Server Port:"));
    jLabel3.setText(Strings.get("Login User:"));
    jLabel4.setText(Strings.get("Password:"));
    jLabel5.setText(Strings.get("User Group:"));
    jLabel6.setText(Strings.get("Private Key:"));

    hostField.setText(""); 
    portField.setText(""); 
    userField.setText(""); 
    passwordField.setText(""); 
    groupField.setText(""); 
    privatekeyField.setText(""); 

    infPanel.add(jLabel1,      new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 7, 0, 8), 10, 8));
    infPanel.add(hostField,    new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 5, 0, 5), 236, 5));
    infPanel.add(jLabel2,      new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(10, 6, 0, 8), 12, 8));
    infPanel.add(portField,    new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(10, 5, 0, 0), 60, 5));
    infPanel.add(jLabel3,      new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(10, 6, 0, 8), 12, 8));
    infPanel.add(userField,    new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(10, 5, 0, 0), 120, 5));
    infPanel.add(jLabel4,      new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(10, 6, 0, 8), 12, 8));
    infPanel.add(passwordField,new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(10, 5, 0, 0), 120, 5));

    if (!nogroup) {
      infPanel.add(jLabel5,      new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(10, 6, 0, 8), 12, 8));
      infPanel.add(groupField,   new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(10, 5, 0, 0), 120, 5));
    } else if (!noprivate) {
      infPanel.add(jLabel6,      new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(10, 6, 0, 8), 12, 8));
      infPanel.add(privatekeyField,   new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(10, 5, 0, 5), 120, 5));
      infPanel.add(browseButton, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(10, 0, 0, 10), 0, 5));
    }

    browseButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        browseButton_actionPerformed(e);
      }
    });

    if (nogroup) { 
      groupField.setEditable(false); 
    } else if (noprivate) {
      privatekeyField.setEditable(false); 
    }
  }

  void browseButton_actionPerformed(ActionEvent e) {
    JFileChooser chooser = new JFileChooser(getPrivateKey());
    chooser.setDialogTitle(Strings.get("Choose your private key(ex. ~/.ssh/id_dsa)"));
    chooser.setFileHidingEnabled(false);
    int returnVal = chooser.showOpenDialog(null);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      setPrivateKey(chooser.getSelectedFile().getAbsolutePath()); 
    }
  }

  void okButton_actionPerformed(ActionEvent e) {
    setCanceled(false); 
    this.dispose();
  }

  void cancelButton_actionPerformed(ActionEvent e) {
    setCanceled(true); 
    this.dispose();
  }

  void setCanceled(boolean canceled) {
    this.canceled = canceled; 
  }

  public boolean isCanceled() {
    return canceled; 
  }

  public void setHost(String host) {
    hostField.setText(host); 
  }

  public String getHost() {
    return Options.trim(hostField.getText()); 
  }

  public void setPort(int port) {
    if (port > 0) portField.setValue(new Integer(port)); 
  }

  public int getPort() {
    try {
      int port = Integer.valueOf(Options.trim(portField.getValue().toString())).intValue(); 
      if (port > 0) return port; 
    } catch (Exception e) {
    }
    return 0; 
  }

  public void setUser(String user) {
    userField.setText(user); 
  }

  public String getUser() {
    return Options.trim(userField.getText()); 
  }

  public void setPassword(String password) {
    passwordField.setText(password); 
  }

  public char[] getPasswd() {
    return passwordField.getPassword(); 
  }
  
  @SuppressWarnings("deprecation")
  public String getPassword() {
    return passwordField.getText(); 
  }

  public void setGroup(String group) {
    groupField.setText(group); 
  }

  public String getGroup() {
    return Options.trim(groupField.getText()); 
  }

  public void setPrivateKey(String privatekey) {
    privatekeyField.setText(privatekey);
  }

  public String getPrivateKey() {
    return Options.trim(privatekeyField.getText());
  }

  public static FsLoginDialog showDialog2(String protocol, String host, int port, String user, String privatekey) {
    FsLoginDialog d = new FsLoginDialog(Options.getMainFrame(), Strings.format("Login %1$s file system", protocol), true, true); 
    d.setLocationRelativeTo(Options.getMainFrame());
    d.setHost(host); 
    d.setPort(port); 
    d.setUser(user); 
    d.setPrivateKey(privatekey); 
    d.setVisible(true);
    return d; 
  }

  public static FsLoginDialog showDialog(String protocol, String host, int port, String user, String group) {
    FsLoginDialog d = new FsLoginDialog(Options.getMainFrame(), Strings.format("Login %1$s file system", protocol), true); 
    d.setLocationRelativeTo(Options.getMainFrame());
    d.setHost(host); 
    d.setPort(port); 
    d.setUser(user); 
    d.setGroup(group); 
    d.setVisible(true);
    return d; 
  }

  public static FsLoginDialog showDialog(String protocol, String host, int port) {
    FsLoginDialog d = new FsLoginDialog(Options.getMainFrame(), Strings.format("Login %1$s database", protocol), true, true, true); 
    d.setLocationRelativeTo(Options.getMainFrame());
    d.setHost(host); 
    d.setPort(port); 
    d.setVisible(true);
    return d; 
  }
  
}
