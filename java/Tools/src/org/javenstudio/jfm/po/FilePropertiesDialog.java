package org.javenstudio.jfm.po;

import java.util.Arrays; 
import java.util.Properties; 
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*; 

import org.javenstudio.common.util.Strings;
import org.javenstudio.jfm.filesystems.JFMFile;
import org.javenstudio.jfm.main.Options;


public class FilePropertiesDialog extends JDialog {
  private static final long serialVersionUID = 1L;
  
  private JTabbedPane tabbedPane = new JTabbedPane(); 
  private DefaultTableModel dataModel = new DefaultTableModel(); 
  private JTable table = new JTable(dataModel);
  private JScrollPane scrollPane = new JScrollPane(table);
  private JPanel panel1 = new JPanel();
  private JPanel panel2 = new JPanel();
  private JLabel jLabel1 = new JLabel();
  private JTextField fileNameField = new JTextField();
  private JLabel jLabel2 = new JLabel();
  private JTextField fullPathField = new JTextField();
  private JLabel jLabel3 = new JLabel();
  private JLabel contentTypeLabel = new JLabel();
  private JFMFile file = null;
  private JPanel jPanel1 = new JPanel();
  private TitledBorder titledBorder1;
  private JCheckBox readAttribute = new JCheckBox();
  private JCheckBox writeAttribute = new JCheckBox();
  private JCheckBox hiddenAttribute = new JCheckBox();
  private JButton setAttributesButton = new JButton();
  private JLabel jLabel4 = new JLabel();
  private JLabel fileSizeLabel = new JLabel();
  private JLabel jLabel5 = new JLabel();
  private JLabel lastModifiedLabel = new JLabel();
  private JLabel jLabel6 = new JLabel();
  private JLabel lastAccessedLabel = new JLabel();
  private GridBagLayout gridBagLayout1 = new GridBagLayout();

  public FilePropertiesDialog(Frame frame, String title, boolean modal) {
    super(frame, title, modal);
    try {
      jbInit();
      pack();
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  public FilePropertiesDialog() {
    this(null, "", false);
  }

  void jbInit() throws Exception {
    setIconImage(new ImageIcon(getClass().getResource("/images/icons/document-properties.png")).getImage());
    setSize(new Dimension(400,700));
    setTitle(Strings.get("File Properties"));

    panel1.setLayout(gridBagLayout1);
    jLabel1.setText(Strings.get("File Name:"));
    jLabel2.setText(Strings.get("Full Path:"));
    jLabel3.setText(Strings.get("Content Type:"));
    contentTypeLabel.setText(Strings.get("N/A"));

    table.setFont(Options.getPanelsFont());
    fileNameField.setText(Strings.get("N/A"));
    fileNameField.setFont(Options.getPanelsFont());
    fileNameField.setEditable(false); 
    fullPathField.setText(Strings.get("N/A"));
    fullPathField.setFont(Options.getPanelsFont());
    fullPathField.setEditable(false); 

    titledBorder1 = new TitledBorder(
        BorderFactory.createLineBorder(new Color(153, 153, 153),2),Strings.get("Attributes"));
    jPanel1.setBorder(titledBorder1);
    jPanel1.setLayout(null);
    readAttribute.setText(Strings.get("Can Read"));
    readAttribute.setBounds(new Rectangle(15, 23, 116, 15));
    writeAttribute.setText(Strings.get("Can Write"));
    writeAttribute.setBounds(new Rectangle(15, 55, 116, 15));
    hiddenAttribute.setText(Strings.get("Is Hidden"));
    hiddenAttribute.setBounds(new Rectangle(15, 86, 116, 15));
    setAttributesButton.setBounds(new Rectangle(15, 124, 148, 28));
    setAttributesButton.setText(Strings.get("Set attributes"));
    setAttributesButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setAttributesButton_actionPerformed(e);
      }
    });

    jLabel4.setText(Strings.get("File Size:"));
    fileSizeLabel.setText(Strings.get("N/A"));
    jLabel5.setText(Strings.get("Last Modified:"));
    lastModifiedLabel.setText(Strings.get("N/A"));
    jLabel6.setText(Strings.get("Last Accessed:"));
    lastAccessedLabel.setText(Strings.get("N/A"));
    jPanel1.add(readAttribute, null);
    jPanel1.add(writeAttribute, null);
    jPanel1.add(hiddenAttribute, null);
    //jPanel1.add(setAttributesButton, null);

    tabbedPane.addTab(Strings.get("Common"), panel1); 
    tabbedPane.addTab(Strings.get("More ..."), panel2); 
    getContentPane().add(tabbedPane);

    dataModel.addColumn(Strings.get("Property")); 
    dataModel.addColumn(Strings.get("Value")); 
    panel2.setLayout(new BorderLayout());
    panel2.add(scrollPane, BorderLayout.CENTER); 

    panel1.add(jPanel1,            new GridBagConstraints(0, 6, 2, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(10, 13, 7, 8), 318, 163));
    panel1.add(jLabel6,            new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(12, 13, 0, 0), 5, 2));
    panel1.add(lastAccessedLabel,  new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(12, 9, 0, 15), 184, 2));
    panel1.add(jLabel5,            new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(12, 13, 0, 0), 5, 2));
    panel1.add(lastModifiedLabel,  new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(12, 9, 0, 15), 184, 2));
    panel1.add(jLabel4,            new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(12, 13, 0, 0), 5, 2));
    panel1.add(fileSizeLabel,      new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(12, 9, 0, 15), 184, 2));
    panel1.add(jLabel3,            new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(12, 13, 0, 0), 5, 2));
    panel1.add(contentTypeLabel,   new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(12, 9, 0, 15), 184, 2));
    panel1.add(jLabel2,            new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(12, 13, 0, 0), 5, 2));
    panel1.add(fullPathField,      new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(12, 9, 0, 15), 10, 2));
    panel1.add(jLabel1,            new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(12, 13, 0, 0), 5, 2));
    panel1.add(fileNameField,      new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(12, 9, 0, 15), 10, 2));

    readAttribute.setEnabled(false);
    writeAttribute.setEnabled(false);
    hiddenAttribute.setEnabled(false);

    Action actionListener = new AbstractAction() { 
      private static final long serialVersionUID = 1L;
      @Override
	  public void actionPerformed(ActionEvent actionEvent) { 
        FilePropertiesDialog.this.dispose(); 
      } 
    };
    InputMap inputMap = panel1.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), "cancel");
    panel1.getActionMap().put("cancel", actionListener);
  }

  public void setFile(JFMFile file) {
    this.file = file;
    fillFields();
  }

  private void fillFields(){
    if (file == null) return; 

    int rows = dataModel.getRowCount(); 
    for (int row = rows-1; row >= 0; row --) 
      dataModel.removeRow(row); 
    Properties props = file.getProperties(); 
    if (props != null && props.size() > 0) {
      Object[] keys = props.keySet().toArray(); 
      Arrays.sort(keys); 
      for (int i=0; keys != null && i < keys.length; i++) {
        String key = keys[i].toString(); 
        String[] rowData = new String[] {Strings.get(key), props.getProperty(key)}; 
        dataModel.addRow(rowData); 
      }
    }

    fileNameField.setText(file.getName());
    fullPathField.setText(file.getAbsolutePath());
    if (!file.isDirectory()) {
      String mimeType = file.getMimeType(); 
      if (mimeType == null) mimeType = "unknown"; 
      contentTypeLabel.setText(mimeType);
    } else
      contentTypeLabel.setText(Strings.get("dir"));

    lastAccessedLabel.setText(Options.timeDesc(file.lastAccessed()));
    lastModifiedLabel.setText(Options.timeDesc(file.lastModified()));
    fileSizeLabel.setText(Options.byteDesc(file.length())+" ("+file.length()+" "+Strings.get("bytes")+")");
    readAttribute.setSelected(file.canRead());
    writeAttribute.setSelected(file.canWrite());
    hiddenAttribute.setSelected(file.isHidden());
  }

  void setAttributesButton_actionPerformed(ActionEvent e) {
    ///todo find a cool way to set the attributes of a file.
  }

  public static void showDialog(JFMFile file) {
    FilePropertiesDialog d = new FilePropertiesDialog(Options.getMainFrame(),"File Properties",false);
    d.setLocationRelativeTo(Options.getMainFrame());
    d.setFile(file);
    d.setVisible(true);
  }

}
