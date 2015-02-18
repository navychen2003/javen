package org.javenstudio.jfm.views.list.detailview;

import javax.swing.table.AbstractTableModel;
import java.util.Vector;
import java.util.Arrays; 

import org.javenstudio.common.util.Strings;
import org.javenstudio.jfm.event.*;
import org.javenstudio.jfm.filesystems.JFMFile;
import org.javenstudio.jfm.filesystems.JFMFileSystem;
import org.javenstudio.jfm.views.JFMView;
import org.javenstudio.jfm.views.list.JFMModel;
import org.javenstudio.jfm.main.Options; 


@SuppressWarnings("unchecked")
public class DetailsTableModel extends AbstractTableModel implements JFMModel {
  private static final long serialVersionUID = 1L;
  
  public static final int COLUMNINDEX_SIZE = 1; 
  private static Object[] columnNames = new Object[]{"Name","Size","Date Modified"};

  @SuppressWarnings("rawtypes")
  private Vector rowData = new Vector();
  private JFMFileSystem filesystem;
  private JFMFile workingDirectory;
    
  /**
   * This is the view that this model belongs to. In order to save the current working directory in the 
   * users' preferences, we have to call view's method setCurrentDirectory 
   */
  @SuppressWarnings("unused")
  private JFMView view;

  public JFMFile getWorkingDirectory() {
    return workingDirectory;
  }

  public DetailsTableModel(JFMFileSystem fs) {
    setFilesystem(fs);
    addListeners();
  }

  private void addListeners() {
   /* Broadcaster.addChangeDirectoryListener(new ChangeDirectoryListener(){
      public void changeDirectory(ChangeDirectoryEvent e){
         if(!(e.getSource() instanceof DetailsTableModel)){
          browseDirectory((JFMFile)((Vector)rowData.elementAt(0)).elementAt(0));
        }
      }
    });*/
  }

  @SuppressWarnings("rawtypes")
  public int getColumnIndex(Class colClass) {
    if (colClass == null) return -1;
    for (int i=0; i<columnNames.length; i++) {        
      if (this.getColumnClass(i) == colClass) return i;
    }

    return -1;
  }

  /**
   * Returns a List of all the files found in the directory.
   * @return Vector
   */
  @SuppressWarnings("rawtypes")
  public Vector getCurrentFiles() {
    Vector files = new Vector();
    for (int i=0; i<rowData.size(); i++) {
      files.addElement(((Vector)rowData.elementAt(i)).elementAt(0));
    }
    return files;
  }

  /**
   * Returns the JFMFile found at the specific index.
   * @return Vector
   */
  public JFMFile getFileAt(int index) {
    JFMFile file = null;
    try {
      if (index >= 0) 
        file = (JFMFile)getValueAt(index, 0);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return file;
  }

  @SuppressWarnings("rawtypes")
  private static class BytesColumn implements Comparable {
    private String displayName = null; 
    private long bytes = 0; 

    public BytesColumn(String name, long bytes) {
      this.displayName = name; 
      this.bytes = bytes; 
    }

    public int compareTo(Object o) {
      if (o == null || !(o instanceof BytesColumn))
        return 1; 
      BytesColumn other = (BytesColumn)o; 
      return bytes == other.bytes ? 0 : (bytes < other.bytes ? -1 : 1); 
    }

    public String toString() {
      return displayName; 
    }
  }

  @SuppressWarnings("rawtypes")
  private static class TimeColumn implements Comparable {
    private String displayName = null; 
    private long value = 0;

    public TimeColumn(long t) {
      this.value = t;
      this.displayName = Options.formatTime(t);
    }

    public int compareTo(Object o) {
      if (o == null || !(o instanceof TimeColumn))
        return 1;
      TimeColumn other = (TimeColumn)o;
      return value == other.value ? 0 : (value < other.value ? -1 : 1);
    }

    public String toString() {
      return displayName;
    }
  }

  @SuppressWarnings("rawtypes")
  public void browseDirectory(final JFMFile el) {
    clear(); //removing the old rows

    final Vector allFiles = new Vector();
    //setting the working directory
    workingDirectory = el;      

    if (el.getParentFile() != null) {
      Vector firstRow = new Vector();
      JFMFile parent = el.getParentFile();
      firstRow.addElement(parent);
      parent.setDisplayName("..");
      firstRow.addElement(new BytesColumn(Strings.get("up dir"), -1));
      firstRow.addElement(new TimeColumn(el.lastModified()));
      addRow(firstRow);
      allFiles.addElement(parent);        
    }

    JFMFile[] files = null;
    try {
      files = el.listFiles();
    } catch (Exception ex) {
      files = null; 
      Options.showMessage(ex); 
    }

    if (files == null) return;
    Arrays.sort(files);
      
    for (int i=0; i<files.length; i++) {
      JFMFile file = files[i]; 
      Vector v = new Vector();
      v.addElement(file);
      if (file.isDirectory())
        v.addElement(new BytesColumn(Strings.get("dir"), -1));
      else
        v.addElement(new BytesColumn(String.valueOf(file.length())+" "+Strings.get("bytes"), file.length()));
      v.addElement(new TimeColumn(file.lastModified()));
      addRow(v);
      allFiles.addElement(file);
    }          

    BrowseDirectoryEvent dirEvent = new BrowseDirectoryEvent(el);      
    dirEvent.setDirectory(el); 
    dirEvent.setSource(this);
    Broadcaster.notifyBrowseDirectoryListeners(dirEvent);
  }

  @SuppressWarnings("rawtypes")
  public Class getColumnClass(int c) {
    return getValueAt(0, c).getClass();
  }

  /** Removes all rows from the data model. */
  public void clear() {
    int oldSize = rowData.size();
    rowData.clear();
    this.fireTableRowsDeleted(0, oldSize);
  }

  /** Add a row to the data model. */
  @SuppressWarnings("rawtypes")
  public void addRow(Vector newRow) {
    rowData.add(newRow);
    this.fireTableRowsInserted(rowData.size(),rowData.size());
  }

  /** Add a row to the data model. */
  public void addRow(Object[] newRow) {
    addRow(convertToVector(newRow));
  }

  /**Returns the column name*/
  public String getColumnName(int column) { 
    String name = column >= 0 && column < columnNames.length ? columnNames[column].toString() : null; 
    return Strings.get(name);
  }

  /**Returns the number of rows*/
  public int getRowCount() { return this.rowData.size(); }

  /**Returns the number of columns*/
  public int getColumnCount() { return columnNames.length; }

  /**Returns the element at the specified row and column*/
  @SuppressWarnings("rawtypes")
  public Object getValueAt(int row, int column) {
    if (row < 0 || column < 0) return null; 
    if (rowData.size() <= row || !(column >= 0 && column < columnNames.length))
      return null;
    return ((Vector)rowData.elementAt(row)).elementAt(column);
  }

  /**Returns whether the cell is editable. It isn't.*/
  public boolean isCellEditable(int row, int column) {
    return false;
  }

  /**Sets an object at the specified row and column*/
  @SuppressWarnings("rawtypes")
  public void setValueAt(Object value, int row, int column) {
    ((Vector)rowData.elementAt(row)).setElementAt(value, column);
    fireTableCellUpdated(row, column);
  }

  /**
   * Returns a vector that contains the same objects as the array.
   * @param anArray  the array to be converted
   * @return  the new vector; if <code>anArray</code> is <code>null</code>,
   *        returns <code>null</code>
   */
  @SuppressWarnings("rawtypes")
  protected static Vector convertToVector(Object[] anArray) {
    if (anArray == null)
      return null;

    Vector v = new Vector(anArray.length);
    for (int i=0; i < anArray.length; i++) {
      v.addElement(anArray[i]);
    }
    return v;
  }

  /**
   * Returns a vector of vectors that contains the same objects as the array.
   * @param anArray  the double array to be converted
   * @return the new vector of vectors; if <code>anArray</code> is
   *        <code>null</code>, returns <code>null</code>
   */
  @SuppressWarnings("rawtypes")
  protected static Vector convertToVector(Object[][] anArray) {
    if (anArray == null)
      return null;

    Vector v = new Vector(anArray.length);
    for (int i=0; i < anArray.length; i++) {
      v.addElement(convertToVector(anArray[i]));
    }
    return v;
  }
    
  public JFMFileSystem getFilesystem() {
    return filesystem;
  }

  public void setFilesystem(JFMFileSystem filesystem) {
    this.filesystem = filesystem;
  }

  /**
   * @param view The view to set.
   */
  public void setView(JFMView view) {
    this.view = view;
  }

}
