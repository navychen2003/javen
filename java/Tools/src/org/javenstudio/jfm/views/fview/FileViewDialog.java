package org.javenstudio.jfm.views.fview;

import java.io.OutputStream; 
import java.util.Arrays; 
import java.util.Enumeration; 
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.beans.PropertyChangeListener;
import java.nio.charset.Charset;

import org.javenstudio.common.util.Strings;
import org.javenstudio.jfm.filesystems.JFMFile;
import org.javenstudio.jfm.views.FontDialog;


public class FileViewDialog extends JDialog {
  private static final long serialVersionUID = 1L;
  
  private JPanel panel1 = new JPanel();
  private BorderLayout borderLayout1 = new BorderLayout();
  private JScrollPane scroll = new JScrollPane();
  private FView view = new FView();
  private JMenuBar menu = new JMenuBar();
  private JMenu fileMenu = new JMenu(Strings.get("File"));
  private JMenuItem quitMenuItem = new JMenuItem(Strings.get("Close"));
  private JMenuItem saveFileMenu = new JMenuItem(Strings.get("Save"));
  private JMenuItem saveAsFileMenu = new JMenuItem(Strings.get("Save As..."));
  private JMenu toolsMenu = new JMenu(Strings.get("Tools"));
  private JMenuItem fontMenuItem = new JMenuItem(Strings.get("Font"));
  private JMenuItem findMenuItem = new JMenuItem(Strings.get("Find text"));
  private JCheckBoxMenuItem wordwrapMenuItem = new JCheckBoxMenuItem(Strings.get("Word wrap"));
  private JCheckBoxMenuItem texthexMenuItem = new JCheckBoxMenuItem(Strings.get("16 Hex"));
  private JMenu charsetMenu = new JMenu(Strings.get("Charset"));
  private ButtonGroup charsetGroup = new ButtonGroup();

  //these are the word separators. If you want to add something, or remove something, this is the place to be.
  private char[] wordSeparatorChars = 
    new char[]{' ','.',',',':','+','-','=','\\','/','?','<','>',';','"','*','(',')','`','\'','\t','\n'};


  public FileViewDialog(Frame frame, String title, boolean modal) {
    super(frame, title, modal);
    try {
      jbInit();
    //  pack();
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  public FileViewDialog() {
    this(null, "", false);
  }

  private void jbInit() throws Exception {
    this.setIconImage(new ImageIcon(getClass().getResource("/images/icons/accessories-text-editor.png")).getImage()); 
    this.setSize(new Dimension(660, 480));
    panel1.setLayout(borderLayout1);

    getContentPane().add(panel1);
    panel1.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"),"escape");
    panel1.getActionMap().put("escape",new EscapeAction());
    panel1.add(scroll,BorderLayout.CENTER);
    menu.add(fileMenu);
    menu.add(toolsMenu);
    fileMenu.add(saveFileMenu);
    fileMenu.add(saveAsFileMenu);
    fileMenu.add(quitMenuItem);
    toolsMenu.add(fontMenuItem);
    toolsMenu.add(findMenuItem);
    toolsMenu.add(wordwrapMenuItem);
    toolsMenu.add(texthexMenuItem);
    toolsMenu.add(charsetMenu); 
    setupCharsetMenu(); 
    this.setJMenuBar(menu);
    scroll.setViewportView(view);
    view.addAdjustmentListener(scroll.getVerticalScrollBar()); 

    wordwrapMenuItem.setMnemonic(KeyEvent.VK_W);
    wordwrapMenuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e){
        boolean wrap=wordwrapMenuItem.getState();
        view.setLineWrap(wrap);
        view.setWrapStyleWord(wrap);
      }
    });

    texthexMenuItem.setMnemonic(KeyEvent.VK_H);
    texthexMenuItem.setState(view.isTextHexMode()); 
    texthexMenuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e){
        boolean state = texthexMenuItem.getState();
        view.setTextHexMode(state);
        charsetMenu.setEnabled(!state); 
        wordwrapMenuItem.setEnabled(!state); 
        boolean editable = view.isEditable(); 
        saveAsFileMenu.setEnabled(editable);
        saveFileMenu.setEnabled(editable);
      }
    });

    quitMenuItem.setIcon(new ImageIcon(getClass().getResource("/images/icons/window-close.png")));
    quitMenuItem.addActionListener(new EscapeAction());

    findMenuItem.setIcon(new ImageIcon(getClass().getResource("/images/icons/edit-find-replace.png")));
    findMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        findMenuItem_actionPerformed(e);
      }
    });

    fontMenuItem.setIcon(new ImageIcon(getClass().getResource("/images/icons/fonts.png")));
    fontMenuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e){
        FontDialog f=new FontDialog(JOptionPane.getFrameForComponent(FileViewDialog.this),Strings.get("Fonts"),true);
        f.setLocationRelativeTo(FileViewDialog.this);
        f.setVisible(true);
        if(!f.isCancelled())
          view.setFont(f.getSelectedFont());
      }
    });

    saveAsFileMenu.setIcon(new ImageIcon(getClass().getResource("/images/icons/document-save-as.png")));
    saveAsFileMenu.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        JFileChooser choose=new JFileChooser(view.getFile().getAbsolutePath());
        int result=choose.showSaveDialog(FileViewDialog.this);
        if(result!=JFileChooser.APPROVE_OPTION) return;
        @SuppressWarnings("unused")
        java.io.File f=choose.getSelectedFile();
        //@todo Save the file on the requested filesystem
        //saveFile(f);
      }
    });

    saveFileMenu.setIcon(new ImageIcon(getClass().getResource("/images/icons/document-save.png")));
    saveFileMenu.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        saveFile(view.getFile());
      }
    });

    addWindowListener(new WindowAdapter() { 
      public void windowClosing(WindowEvent e) {   
        closeWindow(); 
      }   
    });   

    saveFileMenu.setAccelerator(KeyStroke.getKeyStroke("ctrl S"));
    quitMenuItem.setAccelerator(KeyStroke.getKeyStroke("ctrl Q"));
    wordwrapMenuItem.setAccelerator(KeyStroke.getKeyStroke("ctrl W"));
    texthexMenuItem.setAccelerator(KeyStroke.getKeyStroke("ctrl H"));
    fontMenuItem.setAccelerator(KeyStroke.getKeyStroke("ctrl O"));
    findMenuItem.setAccelerator(KeyStroke.getKeyStroke("ctrl F"));
  }

  private void setupCharsetMenu() {
    Charset[] charsets = view.getAvaliableCharsets(); 
    JRadioButtonMenuItem selectedItem = null; 
    for (int i=0; charsets != null && i < charsets.length; i++) {
      Charset charset = charsets[i]; 
      if (charset == null) continue; 
      final String name = charset.name(); 
      final String selectedName = view.getCharsetName();
      JRadioButtonMenuItem charsetItem = new JRadioButtonMenuItem(name);
      if (name.equalsIgnoreCase(selectedName)) 
        selectedItem = charsetItem; 
      charsetItem.addActionListener(new ActionListener(){
        public void actionPerformed(ActionEvent e) {
          view.setCharset(name); 
        }
      });
      charsetMenu.add(charsetItem); 
      charsetGroup.add(charsetItem); 
    }

    if (selectedItem != null) 
      selectedItem.setSelected(true); 

    charsetMenu.addMenuListener(new MenuListener() {
      public void menuCanceled(MenuEvent e) {}
      public void menuDeselected(MenuEvent e) {}
      @SuppressWarnings("rawtypes")
	  public void menuSelected(MenuEvent me) {
        String selectedName = view.getCharsetName();
        for (Enumeration e = charsetGroup.getElements(); e.hasMoreElements();) {
          JRadioButtonMenuItem item = (JRadioButtonMenuItem)e.nextElement();
          if (item == null) continue;
          if (selectedName.equalsIgnoreCase(item.getName()))
            item.setSelected(true);
          else 
            item.setSelected(false);
        }
      }
    });
  }

  private void saveFile(JFMFile f) {
    try{
      OutputStream out = f.getOutputStream();
      byte[] data = view.getText().getBytes();
      out.write(data);
      out.close();
    } catch(Exception ex){
      ex.printStackTrace(); 
      JOptionPane.showMessageDialog(this, 
    	Strings.get("Couldn't write the file..."), Strings.get("Error"), 
    	JOptionPane.ERROR_MESSAGE);
    }
  }

  public void setContent(JFMFile el, boolean editable){
    view.setFile(el, editable);
    editable = view.isEditable(); 
    saveAsFileMenu.setEnabled(editable);
    if(!el.isDirectory())
      saveFileMenu.setEnabled(editable);
    else
      saveFileMenu.setEnabled(false);
    texthexMenuItem.setState(view.isTextHexMode()); 
    charsetMenu.setEnabled(!view.isTextHexMode()); 
    wordwrapMenuItem.setEnabled(!view.isTextHexMode()); 
  }

  public void closeWindow() {
    view.dispose(); 
  }

  private class EscapeAction implements Action {
    private boolean enabled = true;
    public Object getValue(String key) {
      return null;
    }
    public void putValue(String key, Object value) {}
    public void setEnabled(boolean b) {
      enabled = b;
    }
    public boolean isEnabled() {
      return enabled;
    }
    public void addPropertyChangeListener(PropertyChangeListener listener) {}
    public void removePropertyChangeListener(PropertyChangeListener listener) {}
    public void actionPerformed(java.awt.event.ActionEvent e) {
      FileViewDialog.this.dispose();
    }
  }

  private boolean findRegularText(String findText, int position, boolean caseSensitive, boolean wholeWord) {
    int foundTextIndex = -1;
    if(caseSensitive) 
      foundTextIndex=view.getText().indexOf(findText,position);
    else
      foundTextIndex=view.getText().toLowerCase().indexOf(findText.toLowerCase(),position);

    if(foundTextIndex<0)
      return false;

    if(findText.equals(view.getText()))
      return true;

    if(wholeWord) {
      if((foundTextIndex-1) < 0 || (foundTextIndex+findText.length()+1) > view.getText().length()) 
        return false; //return false if we don't have at least a character before or after the word

      Arrays.sort(wordSeparatorChars);
      if (Arrays.binarySearch(wordSeparatorChars, view.getText().charAt(foundTextIndex-1)) < 0)
        return false;
      if (Arrays.binarySearch(wordSeparatorChars, view.getText().charAt(foundTextIndex+findText.length()+1)) < 0)
        return false;
    }

    view.setCaretPosition(foundTextIndex);
    view.moveCaretPosition(foundTextIndex+findText.length());
    //view.select(foundTextIndex,foundTextIndex+findText.length());
    //view.setCaretPosition(foundTextIndex+findText.length());
    return true;
  }

  private boolean replaceRegularText(String findText, String replaceText, 
                                     int position, boolean caseSensitive, boolean wholeWord) {
      int foundTextIndex = -1;
      if(caseSensitive)
        foundTextIndex = view.getText().indexOf(findText,position);
      else
        foundTextIndex=view.getText().toLowerCase().indexOf(findText.toLowerCase(),position);

      if(foundTextIndex < 0)
        return false;

      if(findText.equals(view.getText()))
        return true;

      if(wholeWord) {
        if((foundTextIndex-1) < 0 || (foundTextIndex+findText.length()+1) > view.getText().length())
          return false; //return false if we don't have at least a character before or after the word

        Arrays.sort(wordSeparatorChars);
        if(Arrays.binarySearch(wordSeparatorChars, view.getText().charAt(foundTextIndex-1)) < 0)
          return false;
        if(Arrays.binarySearch(wordSeparatorChars, view.getText().charAt(foundTextIndex+findText.length()+1)) < 0)
          return false;
      }

      view.setCaretPosition(foundTextIndex);
      view.moveCaretPosition(foundTextIndex+findText.length());

      int option = JOptionPane.showConfirmDialog(this, 
    	Strings.get("Replace this text?"), Strings.get("Confirmation"), JOptionPane.YES_NO_OPTION);

      if(option == JOptionPane.YES_OPTION) {
        StringBuffer buf = new StringBuffer(view.getText());
        buf.replace(foundTextIndex, foundTextIndex+findText.length(), replaceText);
        view.setText(buf.toString());
        view.setCaretPosition(foundTextIndex);
      }

      return true;
  }

  private boolean findRegexpText(String findText, int position, boolean caseSensitive, boolean wholeWord) {
    //this is not good, it has to be replaced
    return true;// java.util.regex.Pattern.matches(findText,view.getText());
  }

  void findMenuItem_actionPerformed(ActionEvent e) {
    final FindTextDialog find = new FindTextDialog(JOptionPane.getFrameForComponent(this), Strings.get("Find"), false);
    find.setFindListener(new FindListener() {
      public void find(String findText, String replaceText, 
                       boolean caseSensitive, boolean fileStart, boolean wholeWords, 
                       boolean regexp, int count) {
        if (count == 0 && fileStart) 
          view.setCaretPosition(0);

        int startFrom = view.getCaretPosition();
        if (!regexp && (replaceText == null)) {
          if(!findRegularText(findText,startFrom,caseSensitive,wholeWords))
            JOptionPane.showMessageDialog(find, Strings.get("Text not found"));
        }

        if (regexp && (replaceText == null || replaceText.length() <= 0)) 
          findRegexpText(findText,startFrom,caseSensitive,wholeWords);

        if (replaceText != null) {
          if (!replaceRegularText(findText, replaceText, startFrom, caseSensitive, wholeWords))
            JOptionPane.showMessageDialog(find, Strings.get("Text not found"));
        }
      }

      public void all(String findText, String replaceText, 
                      boolean caseSensitive, boolean fileStart, boolean wholeWords, boolean regexp) {
        if (fileStart)
          view.setCaretPosition(0);
        int startFrom=view.getCaretPosition();
        while (replaceRegularText(findText,replaceText,startFrom,caseSensitive,wholeWords));
      }
    });
    find.setIconImage(new ImageIcon(getClass().getResource("/images/icons/edit-find-replace.png")).getImage()); 
    find.setLocationRelativeTo(this);
    find.setVisible(true);
  }

}
