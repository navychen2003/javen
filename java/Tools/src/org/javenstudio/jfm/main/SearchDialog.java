package org.javenstudio.jfm.main;

import java.io.IOException; 
import java.io.InputStream; 
import java.util.Vector; 
import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.event.*;

import org.javenstudio.common.util.Strings;
import org.javenstudio.jfm.views.fview.FileViewDialog;
import org.javenstudio.jfm.po.FilePropertiesDialog; 
import org.javenstudio.jfm.po.ViewFileAction; 
import org.javenstudio.jfm.filesystems.JFMFile;


public class SearchDialog extends JDialog {
  private static final long serialVersionUID = 1L;
  
  private JPanel panel1 = new JPanel();
  private JPanel topPanel = new JPanel();
  private JLabel jLabel1 = new JLabel();
  private JTextField startFromField = new JTextField();
  private JLabel jLabel2 = new JLabel();
  private JTextField filesSearchField = new JTextField();
  private JCheckBox findTextCheckBox = new JCheckBox();
  private JTextField findTextField = new JTextField();
  private JButton searchButton = new JButton();
  private JPanel buttonsPanel = new JPanel();
  private GridBagLayout gridBagLayout1 = new GridBagLayout();
  private JButton viewFileButton = new JButton();
  private JButton editFileButton = new JButton();
  private JButton propsFileButton = new JButton();
  private JButton closeButton = new JButton();
  private JLabel statusLabel = new JLabel();
  private FlowLayout flowLayout1 = new FlowLayout();
  private BorderLayout borderLayout1 = new BorderLayout();
  private boolean isSearching = false;
  private boolean mustStop = false;

  private FoundTableModel dataModel = new FoundTableModel();
  private JTable filesList = new FoundTable(dataModel);
  private JScrollPane scrollPane = new JScrollPane(filesList);
  private static Object lockObject = new Object(); 

  private static class FoundTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 1L;
	private static String[] columnNames = new String[]{Strings.get("Name"),Strings.get("Size"),Strings.get("Date Modified")};
    private Vector<JFMFile> vec = new Vector<JFMFile>(); 
    private int lastUpdatedIndex = 0; 
    private long lastUpdatedTime = 0; 

    public FoundTableModel() {} 

    public int getRowCount() {
      return getSize(); 
    }

    public int getColumnCount() {
      return columnNames.length; 
    }

    public String getColumnName(int column) {
      return column >= 0 && column < columnNames.length ? columnNames[column] : null; 
    }

    public Object getValueAt(int row, int column) {
      if (column < 0 || column >= columnNames.length) 
        return null; 
      JFMFile file = (JFMFile)getElementAt(row); 
      if (file == null) return null; 
      if (column == 0) return file; 
      if (column == 1) return file.isDirectory() ? Strings.get("dir") : ""+file.length()+" "+Strings.get("bytes"); 
      if (column == 2) return Options.formatTime(file.lastModified()); 
      return file.getName(); 
    }

    public JFMFile getElementAt(int i) {
      synchronized (lockObject) {
        if (i >= 0 && i < vec.size()) 
          return vec.elementAt(i); 
        else
          return null; 
      }
    }

    public int getSize() {
      synchronized (lockObject) {
        return vec.size(); 
      }
    }

    public void addElement(JFMFile obj) {
      synchronized (lockObject) {
        vec.add(obj); 
        long current = System.currentTimeMillis(); 
        if (current - lastUpdatedTime > 1000) {
          int index = vec.size()-1; 
          fireTableRowsUpdated(lastUpdatedIndex, index); 
          lastUpdatedTime = current + (index - lastUpdatedIndex) * 100; 
          lastUpdatedIndex = index; 
        }
      }
    }

    public void clear() {
      synchronized (lockObject) {
        @SuppressWarnings("unused")
		int size = vec.size(); 
        vec.clear(); 
        fireTableDataChanged(); 
      }
    }

    public void finished() {
      synchronized (lockObject) {
        fireTableDataChanged(); 
      }
    }
  }

  public class FoundTable extends JTable {
	private static final long serialVersionUID = 1L;
	private FoundCellRenderer renderer = new FoundCellRenderer(); 

    public FoundTable() { 
      super(); 
      setAttributes(); 
    }

    public FoundTable(TableModel dm) {
      super(dm); 
      setAttributes(); 
    }

    public void setAttributes() {
      getTableHeader().setReorderingAllowed(false);
      setShowGrid(false);
      setColumnSelectionAllowed(false);
      setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      setDragEnabled(false);
    }

    public TableCellRenderer getCellRenderer(int row, int column) {
      return renderer;
    }
  }

  public class FoundCellRenderer extends DefaultTableCellRenderer {
	private static final long serialVersionUID = 1L;

	public FoundCellRenderer() {
      super();
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
                         boolean isSelected, boolean hasFocus, int row, int column) {
      setFont(table.getFont());
      setValue(value);

      if (column == 0 && table.isRowSelected(row)) {
        setForeground(Options.getMarkedColor());
        setBackground(Options.getMarkedBackground());
      } else {
        setForeground(table.getForeground());
        setBackground(table.getBackground());
      }

      if (column == 1) // Size
        setHorizontalAlignment(JLabel.RIGHT);
      else
        setHorizontalAlignment(JLabel.LEFT);

      if (!(value instanceof JFMFile))
        setIcon(null);
      else
        setIcon(((JFMFile)value).getDisplayIcon());

      return this;
    }
  }

  public SearchDialog(Frame frame, String title, boolean modal) {
    super(frame, title, modal);
    try {
      jbInit();
      pack();
      initStartPath(); 
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  public SearchDialog() {
    this(null, "", false);
  }

  void jbInit() throws Exception {
    setIconImage(new ImageIcon(getClass().getResource("/images/icons/folder-search.png")).getImage());
    setSize(new Dimension(500, 300)); 
    panel1.setLayout(borderLayout1);
    panel1.add(topPanel, BorderLayout.NORTH);
    panel1.add(scrollPane, BorderLayout.CENTER);
    panel1.add(buttonsPanel, BorderLayout.SOUTH);
    getContentPane().add(panel1);
    flowLayout1.setAlignment(FlowLayout.LEFT);

    scrollPane.getViewport().setBackground(Options.getBackgroundColor());
    filesList.setPreferredScrollableViewportSize(new Dimension(500, 150)); 
    filesList.setBackground(Options.getBackgroundColor());
    filesList.setForeground(Options.getForegroundColor());
    filesList.setFont(Options.getPanelsFont());

    startFromField.setFont(Options.getPanelsFont());
    filesSearchField.setFont(Options.getPanelsFont());
    findTextField.setFont(Options.getPanelsFont());

    jLabel1.setText(Strings.get("Start from:"));
    jLabel2.setText(Strings.get("Search for:"));
    findTextCheckBox.setText(Strings.get("Find text"));
    findTextCheckBox.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        findTextCheckBox_itemStateChanged(e);
      }
    });
    findTextField.setEnabled(false);
    searchButton.setText(Strings.get("Search"));
    searchButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        searchButton_actionPerformed(e);
      }
    });
    buttonsPanel.setLayout(flowLayout1);
    viewFileButton.setText(Strings.get("View"));
    viewFileButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        viewFileButton_actionPerformed(e);
      }
    });
    editFileButton.setText(Strings.get("Edit"));
    editFileButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        editFileButton_actionPerformed(e);
      }
    });
    propsFileButton.setText(Strings.get("Properties"));
    propsFileButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        propsFileButton_actionPerformed(e);
      }
    });
    closeButton.setText(Strings.get("Close"));
    closeButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        closeButton_actionPerformed(e);
      }
    });

    topPanel.setLayout(gridBagLayout1);

    topPanel.add(jLabel1,           new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 10, 0, 0), 11, 0));
    topPanel.add(startFromField,    new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 0, 0), 200, 2));
    topPanel.add(searchButton,      new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 25, 0, 33), 11, -2));
    topPanel.add(jLabel2,           new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 10, 0, 0), 11, 4));
    topPanel.add(filesSearchField,  new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 0, 0), 200, 2));
    topPanel.add(findTextCheckBox,  new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 10, 5, 0), 8, -5));
    topPanel.add(findTextField,     new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 5, 0), 200, 2));

    topPanel.getRootPane().setDefaultButton(searchButton);

    buttonsPanel.add(viewFileButton, null);
    buttonsPanel.add(editFileButton, null);
    buttonsPanel.add(propsFileButton, null);
    buttonsPanel.add(closeButton, null);
    buttonsPanel.add(statusLabel, null); 
  }

  private void initStartPath() {
    JFMFile dir = Options.getActivePanel().getCurrentWorkingDirectory();
    if (dir != null) {
      startFromField.setText(dir.getAbsolutePath());
      setTitle(Strings.format("Search at %1$s",dir.getFsName())); 
    }
  }

  private void setDefaultTitle() {
    JFMFile dir = Options.getActivePanel().getCurrentWorkingDirectory();
    if (dir != null) {
      setTitle(Strings.format("Search at %1$s",dir.getFsName()));
    }
  }

  void closeButton_actionPerformed(ActionEvent e) {
    this.dispose();
  }

  void findTextCheckBox_itemStateChanged(ItemEvent e) {
    findTextField.setEnabled(e.getStateChange()==ItemEvent.SELECTED);
  }

  JFMFile getSelectedFile() {
    if (filesList.getSelectedRow() < 0)
      return null;

    return (JFMFile)dataModel.getElementAt(filesList.getSelectedRow());
  }

  void viewFileButton_actionPerformed(ActionEvent e) {
    JFMFile selFile = getSelectedFile();
    if (selFile == null || selFile.isDirectory()) 
      return; 

    ViewFileAction.viewFile(selFile); 
  }

  void editFileButton_actionPerformed(ActionEvent e) {
    JFMFile selFile = getSelectedFile();
    if (selFile == null || selFile.isDirectory()) 
      return; 

    FileViewDialog d = new FileViewDialog(JOptionPane.getFrameForComponent(this),selFile.getPath(),false);
    d.setLocationRelativeTo(this);
    d.setContent(selFile,true);
    d.setVisible(true);
  }

  void propsFileButton_actionPerformed(ActionEvent e) {
    JFMFile selFile = getSelectedFile();
    if (selFile == null)
      return;

    FilePropertiesDialog.showDialog(selFile);
  }

  /**
   * The search action. Begin searching files.
   * @param e
   */
  void searchButton_actionPerformed(ActionEvent e) {
    if (isSearching) {
      mustStop = true;
      return;
    }

    synchronized (lockObject) {
      foundDirs = 0; foundFiles = 0; 
      statusLabel.setText(""); 
      dataModel.clear();
    }

    String startDir = startFromField.getText(); 
    if (startDir == null || startDir.length() <= 0 || startDir.equals(".")) 
      startDir = System.getProperty("user.dir");

    final JFMFile startFileDir = Options.getActivePanel().getFile(startDir);
    if (startFileDir == null || !startFileDir.isDirectory() || !startFileDir.exists()) {
      JOptionPane.showMessageDialog(this, 
    		  Strings.format("Invalid start directory specified: %1$s",startDir), 
    		  Strings.get("Error"), 
    		  JOptionPane.ERROR_MESSAGE);
      return;
    }

    String searchText = filesSearchField.getText(); 

    if(searchText == null || searchText.length() <= 0) {
      JOptionPane.showMessageDialog(this, 
          Strings.get("Invalid search mask specified. Use *,? to specify more files."), 
          Strings.get("Error"), 
          JOptionPane.ERROR_MESSAGE);
      return;
    }

    if (searchText.indexOf('*') < 0 && searchText.indexOf('?') < 0) 
      searchText = "*" + searchText + "*"; 

    final String finalSearchText = searchText.toLowerCase(); 
    final String finalFindText = findTextCheckBox.isSelected() ? findTextField.getText() : null; 

    //start the search
    new Thread(new Runnable() {

      public void run() {
        synchronized (lockObject) {
          searchButton.setText(Strings.get("Stop"));
          isSearching = true;
        }

        try {
          search(startFileDir, finalSearchText, finalFindText);
        } catch(Exception ex) {}

        synchronized (lockObject) {
          dataModel.finished(); 
          isSearching = false;
          mustStop = false;
          searchButton.setText(Strings.get("Search"));
          if (foundDirs <= 0 && foundFiles <= 0) 
            statusLabel.setText(Strings.get("No directory or file found")); 
          setDefaultTitle(); 
        }
      }
    }).start();
  }

  public void dispose() {
    mustStop = true; 
    super.dispose(); 
  }

  /**
   * Searches a directory for files. Throws an exception if it has to stop, 
   * just to terminate the thread.
   *
   * @param dir
   * @throws Exception
   */
  private void search(JFMFile dir, String searchText, String findText) throws Exception {
    if (dir == null) return; 
    setTitle(Strings.format("Searching in %1$s ...", dir.getAbsolutePath())); 

    JFMFile[] files = dir.listFiles();
    for (int i=0; i<files.length; i++) {
      if (mustStop) throw new Exception("I have to stop");

      JFMFile file = files[i]; 
      if (file.isDirectory()) {
        if (new MaskMatch().doesMatch(searchText, file.getName().toLowerCase())) {
          synchronized (lockObject) {
            dataModel.addElement(file);
            updateStatus(true); 
          }
        }
        search(file, searchText, findText);
      } else {
        if (isEligible(file, searchText, findText)) {
          synchronized (lockObject) {
            dataModel.addElement(file);
            updateStatus(false); 
          }
        }
      }
    }
  }

  private int foundDirs = 0; 
  private int foundFiles = 0; 

  private void updateStatus(boolean isDir) {
    if (isDir) foundDirs ++; 
    else foundFiles ++; 
    statusLabel.setText(Strings.format("Found %1$s directories and %2$s files", ""+foundDirs, ""+foundFiles)); 
  }

  /**
   * Returns true if the name specified is matching the mask.
   * @param name
   * @return
   */
  private boolean isEligible(JFMFile f, String searchText, String findText) {
    boolean match = false;
    String name = f.getName().toLowerCase(); 
    match = new MaskMatch().doesMatch(searchText, name);

    if (match && findText != null && findText.length() > 0) {  //search within the file
      match = false;
      try {
        InputStream buf = f.getInputStream();
        byte[] wholeFile = null;
        if (f.length() >= (long)Integer.MAX_VALUE) {
          wholeFile = new byte[Integer.MAX_VALUE];
        } else {
          wholeFile = new byte[(int)f.length()];
        }
        int bytesRead = -1;
        while ((bytesRead = buf.read(wholeFile)) >= 0) {
          String stringRead = new String(wholeFile,0,bytesRead);
          if (stringRead.indexOf(findText) >= 0) {
            match = true;
            break;
          }
        }
        buf.close();
      } catch(IOException ignored) {
      }
    }

    return match;
  }

  /**
   * These classes are used for pattern match. These are not mine, 
   * are stolen from a friend of mine Arthur Vitui.
   * I use them, so that i won't be forced to use jdk's 1.4 regexp, 
   * to try to maintain 1.3 compatibility.
   */
  private class AuxPoz{
    public int pozw;
    public int pozt;
  }

  private class MaskMatch {
    private AuxPoz aux;

    public MaskMatch() {
      aux = new AuxPoz();
    }

    private int asterix(char[] wildcard, char[] test, AuxPoz aux) {
      int fit = 1;
      (aux.pozw)++;
      while ((test[aux.pozt] != '\0') && ((wildcard[aux.pozw] == '?') || (wildcard[aux.pozw] == '*'))) {
        if (wildcard[aux.pozw] == '?')
          (aux.pozt)++;
        (aux.pozw)++;
      }
      while (wildcard[aux.pozw] == '*')
        (aux.pozw)++;
      if ((test[aux.pozt] == '\0') && (wildcard[aux.pozw] != '\0')) {
        return fit = 0;
      }
      if ((test[aux.pozt] == '\0') && (wildcard[aux.pozw] == '\0')) {
        return fit = 1;
      } else {
        if (wildcardfit(wildcard, test, aux.pozw, aux.pozt) == 0) {
          do {
            (aux.pozt)++;
            while ((wildcard[aux.pozw] != test[aux.pozt]) && (test[aux.pozt] != '\0'))
              (aux.pozt)++;
          } while(test[aux.pozt] != '\0' ? (wildcardfit(wildcard, test, aux.pozw,aux.pozt) == 0) : (0 != (fit=0)));
        }
        if ((test[aux.pozt] == '\0') && (wildcard[aux.pozw] == '\0'))
          fit = 1;
        return fit;
      }
    }

    private int wildcardfit(char[] wildcard, char[] test, int pozw, int pozt) {
      int fit = 1;
      for (;(wildcard[pozw] != '\0') && (fit == 1) && (test[pozt] != '\0'); (pozw)++) {
        if (wildcard[pozw] == '?')
          (pozt)++;
        else if (wildcard[pozw] == '*') {
          aux.pozw = pozw;
          aux.pozt = pozt;
          fit = asterix(wildcard, test, aux);
          (aux.pozw) --;
          pozw = aux.pozw;
          pozt = aux.pozt;
        } else {
          if (wildcard[pozw] == test[pozt]) fit = 1;
          else fit = 0;
          (pozt)++;
        }
      }
      while ((wildcard[pozw] == '*') && (fit == 1))
        (pozw)++;
      if ((fit == 1) && (test[pozt] == '\0') && (wildcard[pozw] == '\0'))
        fit = 1;
      else fit = 0;
      return fit;
    }

    public boolean doesMatch(String wStr, String tStr) {
      boolean match = false;
      char[] wildcard = new char[wStr.length()+2];
      char[] test = new char[tStr.length()+2];
      int i=0, pozw=0, pozt=0;
      wStr.getChars(0,wStr.length(),wildcard,0);
      tStr.getChars(0,tStr.length(),test,0);
      wildcard[wildcard.length-1] = '\0';
      wildcard[wildcard.length-2] = '\0';
      test[test.length-1] = '\0';
      test[test.length-2] = '\0';
      aux.pozt = 0;
      aux.pozw = 0;
      i = wildcardfit(wildcard,test,pozw,pozt);
      if (i == 1)
        match = true;
      else
        match = false;
      return match;
    }
  }

}
