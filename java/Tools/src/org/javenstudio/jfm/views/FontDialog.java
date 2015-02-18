package org.javenstudio.jfm.views;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;

import java.awt.event.*;
import javax.swing.border.*;

import org.javenstudio.common.util.Strings;


public class FontDialog extends JDialog {
  private static final long serialVersionUID = 1L;
  
  private JPanel panel1 = new JPanel();
  private JPanel buttonsPanel = new JPanel();
  private JButton okButton = new JButton();
  private JButton cancelButton = new JButton();
  private JPanel panel = new JPanel();
  private JScrollPane scroll = new JScrollPane();
  @SuppressWarnings("rawtypes")
  private JList fontList = new JList();
  private JScrollPane sampleScroll = new JScrollPane();
  private JTextArea sample = new JTextArea();
  private JCheckBox boldCheckBox = new JCheckBox();
  private JCheckBox italicCheckBox = new JCheckBox();
  private JLabel jLabel2 = new JLabel();
  private JSpinner sizeField = new JSpinner();
  private boolean cancelled=false;
  private TitledBorder titledBorder1;
  private JPanel jPanel1 = new JPanel();
  private TitledBorder titledBorder2;

  public boolean isCancelled() {
    return this.cancelled;
  }

  public void setCancelled(boolean b) {
    this.cancelled = b;
  }

  public FontDialog(Frame frame, String title, boolean modal) {
    super(frame, title, modal);
    try {
      jbInit();
//      pack();
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  public FontDialog() {
    this(null, "", false);
  }

  public static Font showDialog(Frame parent,String title,boolean modal) {
    FontDialog d = new FontDialog(parent,title,modal);
    d.setLocationRelativeTo(parent);
    d.setVisible(true);
    if (d.isCancelled())
      return null;
    else
      return d.getSelectedFont();
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  void jbInit() throws Exception {
    titledBorder1 = new TitledBorder(BorderFactory.createLineBorder(new Color(153, 153, 153),2),"Fonts");
    titledBorder2 = new TitledBorder(BorderFactory.createLineBorder(new Color(153, 153, 153),2),"Attributes");
    this.setIconImage(new ImageIcon(getClass().getResource("/images/icons/fonts.png")).getImage());
    this.setSize(new Dimension(338, 400));
    this.setResizable(false);
    panel1.setLayout(null);
    okButton.setText(Strings.get("OK"));
    sizeField.setValue(new Integer(10));
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
    panel.setLayout(null);   
    sample.setText(Strings.get("The quick brown fox jumps over the lazy dog.\n1234567890\n~!@#$%^&*()_-+=\\|{}[];:\"',.<>/?"));
    buttonsPanel.setBounds(new Rectangle(0, 325, 330, 40));
    boldCheckBox.setText(Strings.get("Bold"));
    boldCheckBox.setBounds(new Rectangle(8, 18, 74, 17));
    italicCheckBox.setText(Strings.get("Italic"));
    italicCheckBox.setBounds(new Rectangle(8, 37, 74, 17));
    jLabel2.setText(Strings.get("Size:"));
    jLabel2.setBounds(new Rectangle(12, 71, 63, 21));
    sizeField.setBounds(new Rectangle(13, 98, 55, 22));
    scroll.setBorder(titledBorder1);
    scroll.setBounds(new Rectangle(3, 10, 216, 228));
    sampleScroll.setBounds(new Rectangle(3, 246, 326, 74));
    sampleScroll.setViewportView(sample);
    jPanel1.setBorder(titledBorder2);
    jPanel1.setBounds(new Rectangle(222, 10, 109, 228));
    jPanel1.setLayout(null);
    panel.setBounds(new Rectangle(0, 0, 338, 355));
    getContentPane().add(panel1);
    panel1.add(buttonsPanel, null);
    buttonsPanel.add(okButton, null);
    buttonsPanel.add(cancelButton, null);
    panel1.add(panel, null);
    panel.add(sampleScroll, null);
    jPanel1.add(boldCheckBox, null);
    jPanel1.add(italicCheckBox, null);
    jPanel1.add(sizeField, null);
    jPanel1.add(jLabel2, null);
    panel.add(scroll, null);
    panel.add(jPanel1, null);
    scroll.getViewport().add(fontList, null);
    buttonsPanel.getRootPane().setDefaultButton(okButton);
    fontList.setModel(new DefaultListModel());
    fontList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    this.getRootPane().setDefaultButton(okButton);
    
    fontList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        if(e.getValueIsAdjusting()) return;
        setSampleFont();
      }
    });

    boldCheckBox.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
         setSampleFont();
      }
    });

    italicCheckBox.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
         setSampleFont();
      }
    });
    sizeField.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e){
        setSampleFont();
      }
    });

    sample.setVisible(true);
    loadFonts();   
  }
  
  protected void processWindowEvent(WindowEvent e){
    if(e.getID()==WindowEvent.WINDOW_CLOSING){
      cancelButton_actionPerformed(null);
    }
  }

  private void setSampleFont(){
    int style=Font.PLAIN;
    if(boldCheckBox.isSelected()) style=style|Font.BOLD;
    if(italicCheckBox.isSelected()) style=style|Font.ITALIC;
    int size=10;
    size=((Integer)sizeField.getValue()).intValue();
    sample.setFont(new Font((String)fontList.getSelectedValue(),style,size));
    sample.revalidate();
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void loadFonts(){
    String[] sfonts=GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
    for(int i=0;i<sfonts.length;i++){
      ((DefaultListModel)fontList.getModel()).addElement(sfonts[i]);
    }
  }

  public Font getSelectedFont(){
    return sample.getFont();
  }

  void okButton_actionPerformed(ActionEvent e) {
    setCancelled(false);
    this.dispose();
  }

  void cancelButton_actionPerformed(ActionEvent e) {
    setCancelled(true);
    this.dispose();
  }

}
