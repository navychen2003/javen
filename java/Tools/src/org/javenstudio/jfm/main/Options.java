package org.javenstudio.jfm.main;

import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;
import java.util.Vector;
import java.util.HashSet; 
import java.util.SortedMap; 
import java.util.Iterator; 
import java.util.prefs.Preferences;
import java.util.Date; 
import java.util.Locale; 
import java.text.SimpleDateFormat; 
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.net.URI; 
import java.net.URL; 
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.awt.*; 
import java.awt.event.*; 

import javax.swing.*;
import javax.swing.event.*; 

import org.javenstudio.jfm.event.*;
import org.javenstudio.jfm.views.JFMView;
import org.javenstudio.jfm.filesystems.JFMFile; 
import org.javenstudio.raptor.util.Strings;


public class Options {
  public static final int LEFT_PANEL = 1;
  public static final int RIGHT_PANEL = 2;
  
  public static final String JFM_LEFTVIEWPANEL_PREF = "JFM.leftviewpanel";
  public static final String JFM_RIGHTVIEWPANEL_PREF = "JFM.rightviewpanel";
  public static final String JFM_RIGHTVIEWPANELDIR_PREF = "JFM.rightviewpaneldir";
  public static final String JFM_LEFTVIEWPANELDIR_PREF = "JFM.leftviewpaneldir";
  public static final String JFM_LEFTVIEWPANEL_FILESYSTEM_PREF = "JFM.leftviewpanel.filesystem";
  public static final String JFM_RIGHTVIEWPANEL_FILESYSTEM_PREF = "JFM.rightviewpanel.filesystem";
  public static final String JFM_BARACUDA_URL = "JFM.baracuda.rooturl";
  public static final String JFM_BARACUDA_USERNAME = "JFM.baracuda.username";
  
  public static final String JFM_HELP_URL = "JFM.help.url";

  private static String startDirectory = null;
  private static int[] leftSelections = new int[]{-1,-1};
  private static int[] rightSelections = new int[]{-1,-1};
  private static JFMView activePanel;
  private static JFMView inactivePanel;
  @SuppressWarnings("rawtypes")
  private static Vector leftFiles;
  @SuppressWarnings("rawtypes")
  private static Vector rightFiles;
  private static MainFrame frame;

  private static JPopupMenu opMenu = new JPopupMenu(); 
  private static JMenu edMenu = new JMenu(); 
  private static Action createMenu = null;
  private static Action copyMenu = null;
  private static Action moveMenu = null;
  private static Action deleteMenu = null;
  private static Action renameMenu = null;
  private static Action mkdirMenu = null;
  private static Action archiveMenu = null;
  private static Action extractMenu = null;
  private static Action refreshMenu = null;
  private static Action viewMenu = null;
  private static Action editMenu = null;
  private static Action propsMenu = null;

  public static class JFMPopupMenu extends JPopupMenu {
	private static final long serialVersionUID = 1L;
	public void show(Component invoker, int x, int y) {
        show(invoker, x, y, false, null); 
      }
      public void show(Component invoker, int x, int y, boolean pointOnRow, JFMFile file) {
        updateMenuStatus(pointOnRow, file); 
        super.show(invoker, x, y); 
      }
    };

  public synchronized static void updateMenuStatus(boolean pointOnRow, JFMFile file) {
    copyMenu.setEnabled(false); 
    moveMenu.setEnabled(false); 
    deleteMenu.setEnabled(false); 
    renameMenu.setEnabled(false); 
    viewMenu.setEnabled(false); 
    editMenu.setEnabled(false); 
    propsMenu.setEnabled(false); 
    archiveMenu.setEnabled(false); 
    extractMenu.setEnabled(false); 

    if (pointOnRow || file != null) {
      copyMenu.setEnabled(true); 
      moveMenu.setEnabled(true); 
      deleteMenu.setEnabled(true); 
      archiveMenu.setEnabled(true); 
    }

    if (file != null) {
      renameMenu.setEnabled(true); 
      if (file.isFile()) {
        viewMenu.setEnabled(true); 
        if (file.length() <= 1024 * 1024 * 5)
          editMenu.setEnabled(true); 
        if (isArchiveFile(file))
          extractMenu.setEnabled(true); 
      }
      propsMenu.setEnabled(true); 
    }
  }

  //users preferences for this application
  private static final DecimalFormat decimalFormat;
  private static Preferences preferences = Preferences.userRoot();
  private static ClassLoader classLoader;
  private static HashSet<String> imageTypes = new HashSet<String>(); 
  private static HashSet<String> archiveTypes = new HashSet<String>(); 

  static {
    classLoader = Thread.currentThread().getContextClassLoader();
    if (classLoader == null) {
      classLoader = Options.class.getClassLoader();
    }
    NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.ENGLISH);
    decimalFormat = (DecimalFormat) numberFormat;
    decimalFormat.applyPattern("#.##");
    setUpMenuStuff();
    setupMimeTypes(); 
  }

  public static Preferences getPreferences() {
    try {
      preferences.sync();  
    } catch (Exception e) {
      e.printStackTrace();
    }
    return preferences;
  }
  
  public static void savePreferences(){
    try {
      getPreferences().flush();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void cleanHistoryUris() {
    getPreferences().put("JFM.history.uris", ""); 
  }

  public static URI[] getHistoryUris() {
    List<URI> uris = parseHistoryUris(getPreferences().get("JFM.history.uris", null)); 
    if (uris != null) 
      return uris.toArray(new URI[uris.size()]); 
    else 
      return null; 
  }

  public static void addHistoryUri(String scheme, String host, int port, String user) {
    if (scheme == null || host == null || port < 0 || user == null) 
      return; 

    URI newuri = null; 
    try { 
      newuri = new URI(scheme, user, host, port, null, null, null); 
    } catch (Exception e) {
      newuri = null; 
    }
    if (newuri == null) return; 

    List<URI> uris = parseHistoryUris(getPreferences().get("JFM.history.uris", null)); 

    StringBuffer sbuf = new StringBuffer(); 
    if (uris != null) {
      int i = uris.size() > 9 ? uris.size() - 9 : 0; 
      for (; i < uris.size(); i++) {
        URI uri = uris.get(i); 
        if (uri == null) continue; 
        if (uri.equals(newuri)) return; 
        if (sbuf.length() > 0) sbuf.append("|"); 
        sbuf.append(uri.toString()); 
      }
    }

    if (sbuf.length() > 0) sbuf.append("|"); 
    sbuf.append(newuri.toString()); 
    getPreferences().put("JFM.history.uris", sbuf.toString()); 
  }

  private static List<URI> parseHistoryUris(String str) {
    if (str == null || str.length() == 0) 
      return null; 

    ArrayList<URI> uris = new ArrayList<URI>(); 
    StringTokenizer st = new StringTokenizer(str, "| \t\r\n"); 
    while (st.hasMoreTokens()) {
      String ustr = st.nextToken();
      if (ustr == null || ustr.length() == 0) 
        continue; 
      try {
        URI uri = new URI(ustr); 
        uris.add(uri); 
      } catch (Exception e) {
      }
    }

    return uris; //.toArray(new URI[uris.size()]); 
  }

  public static String get(String name) {
    return name; 
  }

  public static String get(String name, String def) {
    return def; 
  }
  
  private static String encodingDefault = null; 

  private static String getCharacterEncoding0() {
    String encoding = System.getProperty("file.encoding");
    if (encoding == null || encoding.length() == 0)
      encoding = System.getProperty("sun.jnu.encoding");
    if (encoding == null || encoding.length() == 0)
      encoding = "UTF-8";
    return encoding;
  }

  public static String getCharacterEncoding() {
    if (encodingDefault == null) {
      synchronized (preferences) {
        encodingDefault = getCharacterEncoding(getCharacterEncoding0());
      }
    }
    return encodingDefault;
  }

  public static String getCharacterEncoding(String encoding) {
    try {
      Charset.forName(encoding);
      return encoding;
    } catch (Exception e) {
      return "UTF-8";
    }
  }

  public static String URLEncode(String str) {
    return URLEncode(str, getCharacterEncoding());
  }

  public static String URLEncode(String str, String encoding) {
    try {
      return URLEncoder.encode(str, encoding);
    } catch (Exception e) {
      return str;
    }
  }

  public static void showMessage(Exception e) {
    if (e == null) return; 
    e.printStackTrace(); 

    String message = e.toString(); 
    showMessage(message); 
  }

  public static void showMessage(String message) {
    JOptionPane.showMessageDialog(Options.getMainFrame(),
        message, Strings.get("Warning"), JOptionPane.WARNING_MESSAGE);
  }

  public static void showStatus(String status) {
    showStatus(getActivePanel(), status); 
  }

  public static void showStatus(Object source, String status) {
    ChangeStatusEvent event = new ChangeStatusEvent();
    event.setStatus(status);
    event.setSource(source);
    Broadcaster.notifyChangeStatusListeners(event);
  }

  public static boolean startsWithRoot(String path) {
    if (path.startsWith("/") || hasWindowsDrive(path, false)) 
      return true; 
    return false; 
  }

  public static boolean hasWindowsDrive(String path, boolean slashed) {
    int start = slashed ? 1 : 0;
    return
      path != null && path.length() >= start+2 &&
      (slashed ? path.charAt(0) == '/' : true) &&
      path.charAt(start+1) == ':' &&
      ((path.charAt(start) >= 'A' && path.charAt(start) <= 'Z') ||
       (path.charAt(start) >= 'a' && path.charAt(start) <= 'z'));
  }

  public static String normalizePath(String path) {
    if (path == null) return null; 

    // remove double slashes & backslashes
    path = path.replace("//", "/");
    path = path.replace("\\", "/");

    // trim trailing slash from non-root path (ignoring windows drive)
    int minLength = hasWindowsDrive(path, true) ? 4 : 1;
    if (path.length() > minLength && path.endsWith("/")) {
      path = path.substring(0, path.length()-1);
    }

    return path;
  }

  public static URL getResource(String name) {
    if (name != null) {
      if (startsWithRoot(name))
        name = "file:" + name;

      if (name.indexOf(':') > 0) {
        try {
          URL url = new URL(name);
          return url;
        } catch (Exception e) {
        }
      }
    }
    return classLoader.getResource(name);
  }

  public static void setIconImage(JLabel label) {
    try {
      label.setIcon(new ImageIcon(getResource("logo.png")));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void setIconImage(Window win) {
    try {
      win.setIconImage(new ImageIcon(getResource("logo.png")).getImage());
    } catch (Exception e) {
      e.printStackTrace(); 
    }
  }

  public static TrayIcon getTrayIcon() {
	TrayIcon icon = new TrayIcon(new ImageIcon(getResource("logo.png")).getImage());
	icon.setImageAutoSize(true);
	return icon;
  }
  
  static SimpleDateFormat FORMATER = new SimpleDateFormat("EEE, MMM d, yyyy 'at' hh:mm:ss");
  static SimpleDateFormat formater = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

  public static String formatTime(long tm) {
    return formater.format(new Date(tm)); 
  }

  public static String timeDesc(long tm) {
    return formater.format(new Date(tm)); 
  }

  public static String byteDesc(long bytes) {
    return byteDesc0(bytes);
  }

  /**
   * Return an abbreviated English-language desc of the byte length
   */
  private static String byteDesc0(long len) {
    double val = 0.0;
    String ending = "";
    if (len < 1024 * 1024) {
      val = (1.0 * len) / 1024;
      ending = " KB";
    } else if (len < 1024 * 1024 * 1024) {
      val = (1.0 * len) / (1024 * 1024);
      ending = " MB";
    } else if (len < 1024L * 1024 * 1024 * 1024) {
      val = (1.0 * len) / (1024 * 1024 * 1024);
      ending = " GB";
    } else if (len < 1024L * 1024 * 1024 * 1024 * 1024) {
      val = (1.0 * len) / (1024L * 1024 * 1024 * 1024);
      ending = " TB";
    } else {
      val = (1.0 * len) / (1024L * 1024 * 1024 * 1024 * 1024);
      ending = " PB";
    }
    return limitDecimalTo2(val) + ending;
  }

  public static String numberDesc(float num) {
    return limitDecimalTo2((double)num); 
  }

  public static synchronized String limitDecimalTo2(double d) {
    return decimalFormat.format(d);
  }

  public static String secondDesc(long secs) {
    long mins = secs / 60; 
    long seconds = secs - mins * 60; 
    long hours = mins / 60; 
    long minutes = mins - hours * 60; 

    StringBuffer sbuf = new StringBuffer(); 
    if (hours < 10)
      sbuf.append("0"+hours); 
    else
      sbuf.append(hours); 
    if (minutes < 10)
      sbuf.append(":0"+minutes); 
    else
      sbuf.append(":"+minutes); 
    if (seconds < 10) 
      sbuf.append(":0"+seconds); 
    else
      sbuf.append(":"+seconds); 
    return sbuf.toString(); 
  }

  public static Font getDefaultPanelsFont() {
    return Font.decode("Arial-PLAIN-10");
  }
  
  public static Font getPanelsFont() {
    String font = getPreferences().get("JFM.panels.font","Arial-PLAIN-11"); 
    return Font.decode(font);
  }

  public static void setPanelsFont(Font f) {
    if(f == null) return;

    String  strStyle;
    if (f.isBold()) 
      strStyle = f.isItalic() ? "BOLDITALIC" : "BOLD";
    else 
      strStyle = f.isItalic() ? "ITALIC" : "PLAIN";
    
    getPreferences().put("JFM.panels.font",f.getFamily()+"-"+strStyle+"-"+f.getSize());        
    UIManager.put("OptionPane.messageFont", f); 
  }

  public static void setMessageFont() {
    UIManager.put("OptionPane.messageFont", getPanelsFont()); 
  }
  
  public static Color getDefaultBackgroundColor() {
    return new Color(255,255,255,255);
  }
  
  public static Color getBackgroundColor() {
    return getNamedColor("JFM.backgroundcolor", "255,255,255,255"); 
  }
  
  public static void setForegroundColor(Color c) {
    setNamedColor("JFM.foregroundcolor", c); 
  }
 
  public static Color getDefaultForegroundColor() {
    return new Color(0,0,0,255);
  }
  
  public static Color getForegroundColor() {
    return getNamedColor("JFM.foregroundcolor", "0,0,0,255"); 
  }
  
  public static void setBackgroundColor(Color c) {
    setNamedColor("JFM.backgroundcolor", c); 
  }

  public static void setMarkedColor(Color c) {
    setNamedColor("JFM.markedcolor", c); 
  }
  
  public static Color getDefaultMarkedColor() {
    return new Color(255,0,0,255);
  }
  
  public static Color getMarkedColor() {
    return getNamedColor("JFM.markedcolor", "255,255,255,255"); 
  }

  public static void setMarkedBackground(Color c) {
    setNamedColor("JFM.markedbackground", c); 
  }

  public static Color getDefaultMarkedBackground() {
    return new Color(0,0,0,255);
  }

  public static Color getMarkedBackground() {
    return getNamedColor("JFM.markedbackground", "0,0,0,255"); 
  }

  private static void setNamedColor(String name, Color c) {
    getPreferences().put(name, c.getRed()+","+c.getGreen()+","+c.getBlue()+","+c.getAlpha());
  }

  private static Color getNamedColor(String name, String def) {
    int red = 255, green = 255, blue = 255;
    int alpha = 255;
    String colorSpecs = getPreferences().get(name, def);
    StringTokenizer tokenizer = new StringTokenizer(colorSpecs, ",");
    try {
      red = Integer.parseInt(tokenizer.nextToken());
      green = Integer.parseInt(tokenizer.nextToken());
      blue = Integer.parseInt(tokenizer.nextToken());
      alpha = Integer.parseInt(tokenizer.nextToken());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return new Color(red,green,blue,alpha);
  }
  
  public static void setDirectoriesSelectedOnAsterisk(boolean flag) {
    getPreferences().put("JFM.dirsselectedonasterisk",Boolean.toString(flag));
  }  
  public static boolean getDirectoriesSelectedOnAsterisk() {
    return Boolean.valueOf(getPreferences().get("JFM.dirsselectedonasterisk","true")).booleanValue();
  }

  public static void setDirectoriesSelectedOnPlus(boolean flag) {
    getPreferences().put("JFM.dirsselectedonplus",Boolean.toString(flag));
  }  
  public static boolean getDirectoriesSelectedOnPlus() {
    return Boolean.valueOf(getPreferences().get("JFM.dirsselectedonplus","true")).booleanValue();
  }
  
  private static void setUpMenuStuff() {
    createMenu = new org.javenstudio.jfm.po.CreateFileAction();
    copyMenu = new org.javenstudio.jfm.po.CopyAction();
    moveMenu = new org.javenstudio.jfm.po.MoveAction();
    deleteMenu = new org.javenstudio.jfm.po.DeleteAction();
    renameMenu = new org.javenstudio.jfm.po.RenameAction();
    mkdirMenu = new org.javenstudio.jfm.po.MkdirAction();
    refreshMenu = new org.javenstudio.jfm.po.RefreshAction();
    archiveMenu = new org.javenstudio.jfm.po.ArchiveAction();
    extractMenu = new org.javenstudio.jfm.po.ExtractAction();
    viewMenu = new org.javenstudio.jfm.po.ViewFileAction();
    editMenu = new org.javenstudio.jfm.po.EditFileAction();
    propsMenu = new org.javenstudio.jfm.po.FilePropertiesAction();

    opMenu.add(createMenu);   edMenu.add(createMenu); 
    opMenu.add(copyMenu);     edMenu.add(copyMenu); 
    opMenu.add(moveMenu);     edMenu.add(moveMenu); 
    opMenu.add(deleteMenu);   edMenu.add(deleteMenu); 
    opMenu.add(renameMenu);   edMenu.add(renameMenu); 
    opMenu.add(mkdirMenu);    edMenu.add(mkdirMenu); 
    opMenu.addSeparator();    edMenu.addSeparator(); 
    opMenu.add(archiveMenu);  edMenu.add(archiveMenu); 
    opMenu.add(extractMenu);  edMenu.add(extractMenu); 
    opMenu.addSeparator();    edMenu.addSeparator(); 
    opMenu.add(viewMenu);     edMenu.add(viewMenu); 
    opMenu.add(editMenu);     edMenu.add(editMenu); 
    opMenu.add(refreshMenu);  edMenu.add(refreshMenu); 
    opMenu.addSeparator();    edMenu.addSeparator(); 
    opMenu.add(propsMenu);    edMenu.add(propsMenu); 

    edMenu.setText(Strings.get("Edit"));
    edMenu.setMnemonic(KeyEvent.VK_E);
    edMenu.addMenuListener(new MenuListener() {
        public void menuCanceled(MenuEvent e) {}
        public void menuDeselected(MenuEvent e) {}
        public void menuSelected(MenuEvent me) {
          updateMenuStatus(false, Options.getActivePanel().getSelectedFile()); 
        }
      });
  }

  public static JPopupMenu getPopupMenu() {
    return opMenu;
  }

  public static JMenu getEditMenu() {
    return edMenu; 
  }

  public static void setMainFrame(MainFrame f) {
    frame = f;
  }

  public static MainFrame getMainFrame() {
    return frame;
  }

  @SuppressWarnings("rawtypes")
  public static void setRightFiles(Vector v) {
    rightFiles=v;
  }

  @SuppressWarnings("rawtypes")
  public static Vector getRightFiles() {
    return rightFiles;
  }

  @SuppressWarnings("rawtypes")
  public static void setLeftFiles(Vector v) {
    leftFiles=v;
  }

  @SuppressWarnings("rawtypes")
  public static Vector getLeftFiles() {
    return leftFiles;
  }

  public static void setStartDirectory(String path) {
    startDirectory=path;
  }

  public static String getStartDirectory() {
    if(startDirectory == null){
      setStartDirectory(System.getProperty("user.dir"));
    }
    return startDirectory;
  }

  public static void setRightPanelSelections(int[] sel) {
    rightSelections=sel;
    FileListSelectionEvent ev=new FileListSelectionEvent();
    ev.setSource(null);
    ev.setFilesIndexes(sel);
    ev.setPanelLocation(RIGHT_PANEL);
    Broadcaster.notifyFileListSelectionListener(ev);
  }

  public static int[] getRightPanelSelections() {
    return rightSelections;
  }

  public static void setLeftPanelSelections(int[] sel) {
    leftSelections=sel;
    FileListSelectionEvent ev=new FileListSelectionEvent();
    ev.setSource(null);
    ev.setFilesIndexes(sel);
    ev.setPanelLocation(LEFT_PANEL);
    Broadcaster.notifyFileListSelectionListener(ev);
  }

  public static int[] getLeftPanelSelections() {
    return leftSelections;
  }

  public static void setActivePanel(JFMView panel) {
    if (activePanel != null) {
      activePanel.setActive(false);
    }
    if (inactivePanel == null || inactivePanel != activePanel) {
      if (activePanel != panel) 
        inactivePanel = activePanel;
    }
    activePanel = panel;
    activePanel.setActive(true);
  }

  public static void setInactivePanel(JFMView panel) {
    inactivePanel = panel; 
    inactivePanel.setActive(false); 
  }

  public static JFMView getActivePanel() {
    return activePanel;
  }

  public static JFMView getInactivePanel() {
    return inactivePanel;
  }

  private static void setupMimeTypes() {
    synchronized (imageTypes) {
      imageTypes.add("jpg"); 
      imageTypes.add("jpeg"); 
      imageTypes.add("png"); 
      imageTypes.add("gif"); 
      imageTypes.add("bmp"); 
      imageTypes.add("tif"); 
      imageTypes.add("tiff"); 

      archiveTypes.add("zip"); 
      archiveTypes.add("jar"); 
      archiveTypes.add("rar"); 
      archiveTypes.add("gz"); 
      archiveTypes.add("tgz"); 
      archiveTypes.add("tar"); 
      archiveTypes.add("bz2"); 
    }
  }

  public static String[] getArchiveTypesSupported() {
    return new String[] {".zip", ".tar.gz", ".tar.bz2", ".tgz", ".tar"}; 
  }

  public static String getExtension(JFMFile file) {
    String name = file.getName();
    int pos = name.lastIndexOf('.');
    if (pos > 0) name = name.substring(pos+1);
    if (name != null) name.toLowerCase();
    return name; 
  }

  public static boolean isImageFile(JFMFile file) {
    if (file == null || file.isDirectory()) 
      return false; 

    String name = getExtension(file);
    if (imageTypes.contains(name)) 
      return true; 

    String mime = file.getMimeType(); 
    if (mime != null) {
      mime = mime.toLowerCase(); 
      if (mime.startsWith("image")) 
        return true; 
    }

    return false; 
  }

  public static boolean isArchiveFile(JFMFile file) {
    if (file == null || file.isDirectory())
      return false;

    String name = getExtension(file);
    if (archiveTypes.contains(name))
      return true;

    return false;
  }

  public static boolean isRarFile(JFMFile file) {
    if (file == null || file.isDirectory())
      return false;

    String name = getExtension(file);
    if (!archiveTypes.contains(name))
      return false;

    if ("rar".equals(name))
      return true;

    return false;
  }

  public static boolean isZipFile(JFMFile file) {
    if (file == null || file.isDirectory())
      return false;

    String name = getExtension(file);
    if (!archiveTypes.contains(name))
      return false;

    if ("zip".equals(name) || "jar".equals(name) || "gz".equals(name)) 
      return true; 

    return false;
  }

  public static boolean isTarFile(JFMFile file) {
    if (file == null || file.isDirectory())
      return false;

    String name = getExtension(file);
    if (!archiveTypes.contains(name))
      return false;

    name = file.getName().toLowerCase(); 

    if (name.endsWith(".tar") || name.endsWith(".tgz") || 
        name.endsWith(".tar.gz") || name.endsWith(".tar.bz2"))
      return true;

    return false;
  }

  public static String[] getTextTypes() {
    HashSet<String> textTypes = new HashSet<String>(); 
    textTypes.add("txt");
    textTypes.add("xml");
    textTypes.add("htm");
    textTypes.add("html");
    textTypes.add("mht");
    textTypes.add("bat");
    textTypes.add("sh");
    textTypes.add("java");
    textTypes.add("c");
    textTypes.add("cpp");
    textTypes.add("h");
    textTypes.add("hpp");
    textTypes.add("php");
    textTypes.add("php");
    textTypes.add("css");
    textTypes.add("ini");
    textTypes.add("log");
    textTypes.add("properties");
    textTypes.add("xsl");
    textTypes.add("dtd");
    textTypes.add("asp");
    textTypes.add("js");
    textTypes.add("srt");
    return textTypes.toArray(new String[textTypes.size()]); 
  }

  public static String[] getCharsetNames() {
    HashSet<String> names = new HashSet<String>(); 
    SortedMap<String, Charset> charsets = Charset.availableCharsets();
    Iterator<String> it = charsets.keySet().iterator();
    while (it.hasNext()) {
      String name = it.next();
      if (name.startsWith("IBM") || name.startsWith("x-") || 
          name.startsWith("JIS") || name.startsWith("KOI") || 
          name.startsWith("Shift") || name.startsWith("TIS"))
        continue; 
      names.add(name); 
    }
    return names.toArray(new String[names.size()]); 
  }

  public static String trim(String str) {
    if (str != null) {
      int pos1 = 0;
      int pos2 = str.length();

      while (pos1 < str.length()) {
        char chr = str.charAt(pos1++);
        if (chr == ' ' || chr == '\"' || chr == '\t' || chr == '\r' || chr == '\n')
          continue;
        pos1 --;
        break;
      }

      while (pos2 > 0) {
        char chr = str.charAt(--pos2);
        if (chr == ' ' || chr == '\"' || chr == '\t' || chr == '\r' || chr == '\n')
          continue;
        pos2 ++;
        break;
      }

      if (pos1 < 0 || pos2 < 0)
        str = "";
      else if (pos1 > pos2)
        str = "";
      else if ((pos1 != 0 || pos2 != str.length()))
        str = str.substring(pos1, pos2);
    }

    return str;
  }

}
