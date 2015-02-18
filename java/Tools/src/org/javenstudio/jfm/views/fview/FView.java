package org.javenstudio.jfm.views.fview;

import java.io.InputStream; 
import java.io.IOException;
import java.util.HashSet; 
import java.util.HashMap; 
import java.util.Arrays; 
import java.util.ArrayList; 
import java.awt.event.*; 
import javax.swing.*;
import java.nio.charset.Charset;

import org.javenstudio.common.util.Strings;
import org.javenstudio.jfm.filesystems.JFMFile;
import org.javenstudio.jfm.main.Options; 


public class FView extends JTextArea {
  private static final long serialVersionUID = 1L;
  
  private JScrollBar scrollBar = null; 
  private JFMFile file = null;
  private InputStream fileIn = null; 
  private byte[] fileData = null; 
  private int fileLength = 0; 
  private int dataLength = 0; 
  private int position = 0; 
  private String charsetName = null; 
  private boolean mimeText = false; 
  private boolean editFile = false; 

  public FView() {
    this(null,false);
  }

  public FView(JFMFile file, boolean editable){
    setFile(file, editable);
  }

  public void dispose() {
    if (fileData != null) {
      fileData = null; 
      position = 0; 
    }
    try {
      if (fileIn != null) fileIn.close(); 
      fileIn = null; 
    } catch (Exception ex) { }
  }

  public void addAdjustmentListener(JScrollBar bar) {
    this.scrollBar = bar; 
    this.scrollBar.addAdjustmentListener(getAdjustmentListener());
  }

  public AdjustmentListener getAdjustmentListener() {
    return new AdjustmentListener() {
      public void adjustmentValueChanged(AdjustmentEvent e) {
        int max = scrollBar.getMaximum(); 
        int val = e.getValue(); 
        float percent = (float)val / (float)max; 
        if (percent > 0.98) 
          updateFileEnd(); 
        else if (percent > 0.9) 
          updateFileNext(); 
      }
    };
  }

  public JFMFile getFile() {
    return this.file;
  }

  public void setFile(JFMFile file, boolean editable) {
    this.editFile = editable; 
    this.setEditable(editable); 
    this.file = file;
    if (file == null || file.isDirectory()) {
      this.setText("");
      this.setEditable(false); 
      return;
    }

    if (file.length() <= 0) return;
    try {
      readFile(file); 
      mimeText = isTextFile(file, fileData); 
      updateText(fileData, dataLength); 
    } catch (Exception e) {
      e.printStackTrace(); 
      JOptionPane.showMessageDialog(this, 
        Strings.format("Could not open file: %1$s: %2$s", file.getPath(), e.toString()), 
        Strings.get("Error"), 
        JOptionPane.ERROR_MESSAGE);
    }
  }

  private void updateFileNext() {
    updateFileNext(0); 
  }

  private void updateFileEnd() {
    updateFileNext(fileLength - MAX_READSIZE); 
  }

  private void updateFileNext(int pos) {
    try {
      updateFileNext0(pos); 
    } catch (Exception e) {
      e.printStackTrace(); 
      Options.showMessage(e); 
    }
  }

  private void updateFileNext0(int pos) throws IOException {
    if (fileIn == null || dataLength >= fileLength || position >= fileLength || fileLength <= 0) 
      return; 

    position += dataLength > 0 ? dataLength : 0; 
    dataLength = fileLength - position; 
    if (dataLength <= 0) return; 
    if (dataLength > MAX_READSIZE)
      dataLength = MAX_READSIZE;

    if (fileData == null) 
      fileData = new byte[dataLength];
    else if (dataLength > fileData.length) 
      dataLength = fileData.length; 

    if (pos > position && pos < fileLength) {
      fileIn.skip(pos - position); 
      position = pos; 
    }

    int read = 0, off = 0, len = dataLength;
    while ((read = fileIn.read(fileData, off, len)) >= 0) {
      off += read;
      len = dataLength - off;
      if (off >= dataLength) break;
    }

    try {
      dataLength = off; 
      if ((position+dataLength) >= fileLength) {
        if (fileIn != null) fileIn.close();
        fileIn = null;
      }
    } catch (Exception ex) { }

    updateText(fileData, dataLength); 
  }

  private void updateText(byte[] data, int length) {
    String text = getTextData(data, length); 
    this.setText(text);
    this.setCaretPosition(0);
  }

  private final static int MAX_READSIZE = 1024 * 100; 

  private void readFile(JFMFile file) throws IOException {
    position = 0; 
    fileLength = (int)file.length(); 
    dataLength = fileLength;
    if (dataLength <= 0) return; 
    if (dataLength > MAX_READSIZE)
      dataLength = MAX_READSIZE;

    try {
      if (fileIn != null) fileIn.close(); 
    } catch (Exception ex) { }

    fileIn = file.getInputStream();
    if (fileData == null) 
      fileData = new byte[dataLength];

    int read = 0, off = 0, len = dataLength;
    while ((read = fileIn.read(fileData, off, len)) >= 0) {
      off += read;
      len = dataLength - off; 
      if (off >= dataLength) break;
    }

    try {
      if (dataLength >= fileLength) {
        if (fileIn != null) fileIn.close(); 
        fileIn = null; 
      }
    } catch (Exception ex) { }
  }

  private static HashSet<String> textTypes = new HashSet<String>(); 
  private static HashMap<String, Charset> charsets = new HashMap<String, Charset>(); 
  static {
    String[] names = Options.getTextTypes(); 
    for (int i=0; names != null && i < names.length; i++) {
      addTextType(names[i]); 
    }

    names = Options.getCharsetNames(); 
    for (int i=0; names != null && i < names.length; i++) {
      addCharset(names[i]); 
    }
    addCharset("utf-8"); 
    addCharset("gbk"); 
  }

  private static void addTextType(String name) {
    synchronized (textTypes) {
      if (name != null) 
        textTypes.add(name.toLowerCase()); 
    }
  }

  private static void addCharset(String name) {
    synchronized (charsets) {
      try {
        if (name != null) 
          charsets.put(name.toLowerCase(), Charset.forName(name)); 
      } catch (Exception e) { }
    }
  }

  public Charset[] getAvaliableCharsets() {
    synchronized (charsets) {
      String[] names = charsets.keySet().toArray(new String[0]); 
      if (names == null) return null; 
      Arrays.sort(names); 
      ArrayList<Charset> results = new ArrayList<Charset>(); 
      for (int i=0; names != null && i < names.length; i++) {
        Charset charset = getCharset(names[i]); 
        if (charset != null) 
          results.add(charset); 
      }
      return results.toArray(new Charset[0]); 
    }
  }

  public void setCharset(String name) {
    if (name == null) return; 
    name = name.toLowerCase(); 
    if (charsets.containsKey(name) && !name.equals(charsetName)) {
      charsetName = name; 
      updateText(this.fileData, this.dataLength); 
    }
  }

  public String getCharsetName() {
    if (charsetName == null || charsetName.length() == 0) 
      return "utf-8"; 
    else
      return charsetName; 
  }

  private Charset getCharset() {
    return getCharset(getCharsetName()); 
  }

  private Charset getCharset(String name) {
    synchronized (charsets) {
      if (name != null) 
        return charsets.get(name.toLowerCase()); 
      else
        return null; 
    }
  }

  public boolean isTextHexMode() { return !mimeText; } 

  public void setTextHexMode(boolean hexMode) { 
    if (mimeText != !hexMode) {
      mimeText = !hexMode; 
      updateText(this.fileData, this.dataLength); 
    }
  }

  private boolean isTextFile(JFMFile file, byte[] data) throws IOException {
    if (file == null || data == null) return false; 

    String name = file.getName(); 
    int pos = name.lastIndexOf('.'); 
    if (pos > 0) name = name.substring(pos+1); 
    if (name != null) name.toLowerCase(); 

    boolean isMimeText = false; 
    if (name != null && textTypes.contains(name)) {
      isMimeText = true; 
    } else {
      String mime = JFMFile.guessMimeType(file, data); 
      if (mime == null) mime = "application/*"; 
      else mime = mime.toLowerCase(); 
      if (mime.startsWith("text") || mime.indexOf("xml") >= 0 || 
          mime.indexOf("htm") >= 0) {
        isMimeText = true; 
      }
    }

    return isMimeText; 
  }

  private String getTextData(byte[] data, int length) {
    if (data == null) return null; 
    if (length <= 0) return ""; 
    if (length > data.length) length = data.length; 
    if (mimeText) {
      if (fileLength != dataLength) 
        setEditable(false); 
      else
        setEditable(editFile); 
      return new String(data, 0, length, getCharset()); 
    } else {
      setEditable(false); 
      return getBinaryText(data, 0, length); 
    }
  }

  private String getBinaryText(byte[] data, int offset, int length) {
    if (data == null || data.length == 0)
      return null; 

    StringBuffer sbuf = new StringBuffer(length*5); 
    StringBuffer hexs = new StringBuffer(); 
    StringBuffer chars = new StringBuffer(); 
    StringBuffer tmps = new StringBuffer(); 
    int pos = offset, end = offset + length; 
    while (pos < data.length && pos < end) {
      sbuf.append(getHexString(tmps, pos+position, 8)); 
      sbuf.append("h: "); 
      hexs.setLength(0); chars.setLength(0); 
      for (int i=pos; i < data.length && i < pos+16; i++) {
        int n = (int)data[i]; char c = (char)data[i]; 
        if (hexs.length() > 0) hexs.append(' '); 
        hexs.append(getHexString(tmps, n, 2)); 
        if (Character.isLetter(c)) 
          chars.append(c); 
        else
          chars.append('.'); 
      }
      if (hexs.length() < 47) {
        int left = 47 - hexs.length(); 
        for (int i=0; i<left; i++)
          hexs.append(' '); 
      }
      sbuf.append(hexs.toString()); 
      sbuf.append(" ; "); 
      sbuf.append(chars.toString()); 
      sbuf.append('\n'); 
      pos += 16; 
    }

    return sbuf.toString(); 
  }

  private String getHexString(StringBuffer sbuf, int code, int len) {
    sbuf.setLength(0); 
    String str = Integer.toHexString(code); 
    if (str.length() > len) 
      str = str.substring(str.length()-len); 
    for (int i=0; i < len - str.length(); i++) 
      sbuf.append('0'); 
    sbuf.append(str.toUpperCase()); 
    return sbuf.toString(); 
  }

}
