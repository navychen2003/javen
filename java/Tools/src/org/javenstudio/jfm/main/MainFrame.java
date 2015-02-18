package org.javenstudio.jfm.main;

import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;
import java.util.Vector;
import java.util.ArrayList;
import java.net.URI; 
import javax.swing.*;
import javax.swing.event.MenuListener;
import javax.swing.event.MenuEvent;

import org.javenstudio.common.util.Strings;
import org.javenstudio.jfm.event.*;
import org.javenstudio.jfm.filesystems.JFMFile;
import org.javenstudio.jfm.filesystems.JFMFileSystem;
import org.javenstudio.jfm.main.configurationdialog.ConfigurationDialog;
import org.javenstudio.jfm.views.FontDialog;
import org.javenstudio.jfm.views.JFMView;
import org.javenstudio.jfm.views.JFMViewRepresentation;
import org.javenstudio.jfm.views.fview.FileViewDialog;
import org.javenstudio.jfm.help.HelpBrowser;
import com.forizon.jimage.viewer.JImageView;


public class MainFrame extends JFrame {
  private static final long serialVersionUID = 1L;

  private JPanel contentPane;
  private JMenuBar jMenuBar1 = new JMenuBar();
  private JMenu jMenuTools = new JMenu(Strings.get("Tools"));
  private JMenuItem jMenuImageView = new JMenuItem(Strings.get("Image View"));
  private JMenuItem jMenuFileView = new JMenuItem(Strings.get("File View"));
  private JMenuItem jMenuConfiguration = new JMenuItem(Strings.get("Configuration"));
  private JMenuItem jMenuConfigurationBackground = new JMenuItem(Strings.get("Background"));
  private JMenuItem jMenuConfigurationForeground = new JMenuItem(Strings.get("Foreground"));
  private JMenuItem jMenuConfigurationFont = new JMenuItem(Strings.get("Font"));
  
  private JMenu jMenuFile = new JMenu(Strings.get("File"));
  private JMenuItem jMenuFileExit = new JMenuItem(Strings.get("Exit"));
  private JMenuItem jMenuFileSearch = new JMenuItem(Strings.get("Search ..."));
  private JMenu jMenuHelp = new JMenu(Strings.get("Help"));
  private JMenuItem jMenuHelpHelp = new JMenuItem(Strings.get("Help Topics"));
  private JMenuItem jMenuHelpHome = new JMenuItem(Strings.get("Homepage"));
  private JMenuItem jMenuHelpAbout = new JMenuItem(Strings.get("About Me"));
  private JMenu jMenuView = new JMenu(Strings.get("View"));
  private JMenu jMenuFilesystem = new JMenu(Strings.get("Filesystem"));
  private JMenu jMenuViewLookandFeel = new JMenu(Strings.get("Themes"));

  private ButtonGroup viewGroup = new ButtonGroup();
  private ButtonGroup filesystemGroup = new ButtonGroup();
  private ButtonGroup lookandfeelGroup = new ButtonGroup();

  private JLabel statusBar = new JLabel();
  private BorderLayout borderLayout1 = new BorderLayout();
  private MainPanel mainPanel = new MainPanel();


  public static class JHistoryMenuItem extends JMenuItem {
	private static final long serialVersionUID = 1L;
	private String protocol = null;
    private String displayText = null; 
    private URI uri = null; 
    public String getProtocol() {
      return protocol;
    }
    public URI getUri() {
      return uri; 
    }
    public JHistoryMenuItem(String name, URI uri, String displayText) {
      this(name, null, uri, displayText);
    }
    public JHistoryMenuItem(String name, Icon icon, URI uri, String displayText) {
      super(name, icon);
      this.protocol = name;
      this.displayText = displayText; 
    }
    public String getText() {
      if (displayText != null && displayText.length() > 0)
        return displayText;
      else
        return super.getText();
    }
  }

  public static class JConnectMenuItem extends JMenuItem {
	private static final long serialVersionUID = 1L;
	private String protocol = null;
    public String getProtocol() {
      return protocol;
    }
    public JConnectMenuItem(String name) {
      this(name, null);
    }
    public JConnectMenuItem(String name, Icon icon) {
      super(name, icon);
      this.protocol = name;
      if (icon == null) {
        String iconname = JFMFileSystem.getFsIconName(protocol);
        if (iconname != null)
          setIcon(new ImageIcon(getClass().getResource(iconname)));
      }
    }
    public String getText() {
      String text = JFMFileSystem.getFsConnectName(protocol);
      if (text != null && text.length() > 0)
        return text;
      else
        return Strings.format("Connect to %1$s filesystem ...", protocol); 
    }
  }

  public static class JFilesystemMenuItem extends JRadioButtonMenuItem {
	private static final long serialVersionUID = 1L;
	private String protocol = null; 
    public String getProtocol() {
      return protocol; 
    }
    public JFilesystemMenuItem(String name) {
      this(name, null, false); 
    }
    public JFilesystemMenuItem(String name, Icon icon) {
      this(name, icon, false); 
    }
    public JFilesystemMenuItem(String name, Icon icon, boolean selected) {
      super(name, icon, selected); 
      this.protocol = name; 
      if (icon == null) {
        String iconname = JFMFileSystem.getFsIconName(protocol); 
        if (iconname != null) 
          setIcon(new ImageIcon(getClass().getResource(iconname))); 
      }
    }
    public String getText() { 
      String text = JFMFileSystem.getFsLongName(protocol); 
      if (text != null && text.length() > 0) 
        return text; 
      else 
        return super.getText(); 
    }
  }

  /**Construct the frame*/
  public MainFrame() {
    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    try {
      jbInit();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }

  //public static final String TITLE = "File System Explorer"; 
  //public static final String DEFAULT_STATUS = "Javen-Studio (c) 2009-2013"; 
  public static final int MAIN_WIDTH = 800; 
  public static final int MAIN_HEIGHT = 570; 

  /**Component initialization*/
  private void jbInit() throws Exception {
    Options.setMainFrame(this);
    Options.setIconImage(this);
    Options.setMessageFont(); 

    contentPane = (JPanel) this.getContentPane();
    contentPane.setLayout(borderLayout1);
    statusBar.setBorder(BorderFactory.createLoweredBevelBorder());
    statusBar.setText(Main.STATUS);
    this.setSize(new Dimension(MAIN_WIDTH, MAIN_HEIGHT));
    this.setTitle(Strings.get("File System Explorer"));

    jMenuFileExit.setIcon(new ImageIcon(getClass().getResource("/images/icons/window-close.png"))); 
    jMenuFileExit.addActionListener(new ActionListener()  {
      public void actionPerformed(ActionEvent e) {
        jMenuFileExit_actionPerformed(e);
      }
    });

    jMenuHelpHelp.setIcon(new ImageIcon(getClass().getResource("/images/icons/help-browser.png"))); 
    jMenuHelpHelp.addActionListener(new ActionListener()  {
      public void actionPerformed(ActionEvent e) {
        jMenuHelpHelp_actionPerformed(e);
      }
    });

    jMenuHelpHome.setIcon(new ImageIcon(getClass().getResource("/images/icons/go-home.png"))); 
    jMenuHelpHome.addActionListener(new ActionListener()  {
      public void actionPerformed(ActionEvent e) {
        jMenuHelpHome_actionPerformed(e);
      }
    });

    jMenuHelpAbout.setIcon(new ImageIcon(getClass().getResource("/images/icons/about.png"))); 
    jMenuHelpAbout.addActionListener(new ActionListener()  {
      public void actionPerformed(ActionEvent e) {
        jMenuHelpAbout_actionPerformed(e);
      }
    });

    jMenuFileSearch.setIcon(new ImageIcon(getClass().getResource("/images/icons/folder-search.png"))); 
    jMenuFileSearch.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        search();
      }
    });

    jMenuConfigurationBackground.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e){
        changeBackgroundColors();
      }
    });
    jMenuConfigurationForeground.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e){
        changeForegroundColors();
      }
    });
    jMenuConfigurationFont.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e){
        changeFont();
      }
    });

    jMenuImageView.setIcon(new ImageIcon(getClass().getResource("/images/icons/jimage.png"))); 
    jMenuImageView.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e){
        showImageViewDialog();
      }
    });  

    jMenuFileView.setIcon(new ImageIcon(getClass().getResource("/images/icons/accessories-text-editor.png"))); 
    jMenuFileView.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e){
        showFileViewDialog();
      }
    });  
    
    jMenuConfiguration.setIcon(new ImageIcon(getClass().getResource("/images/icons/configuration.png"))); 
    jMenuConfiguration.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e){
        showConfDialog();
      }
    });    
    
    jMenuView.setMnemonic(KeyEvent.VK_V);
    jMenuView.add(jMenuConfiguration);
    jMenuView.addSeparator();

    @SuppressWarnings("rawtypes")
	Vector views = JFMView.getRegisteredViews();
    for (int i=0; i<views.size(); i++) {
      final JFMViewRepresentation rep = (JFMViewRepresentation)views.get(i);
      JRadioButtonMenuItem viewItem = new JRadioButtonMenuItem(rep.getName());
      if (rep.getIconName() != null) 
        viewItem.setIcon(new ImageIcon(getClass().getResource(rep.getIconName()))); 
      viewItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {          
           ChangeViewEvent ev=new ChangeViewEvent();
           ev.setSource(MainFrame.this);
           ev.setViewRep(rep);
           Broadcaster.notifyChangeViewListeners(ev);           
        }
      });
      jMenuView.add(viewItem);
      viewGroup.add(viewItem);
    }
    
    String[] filesystemNames = JFMFileSystem.getRegisteredNames(); 
    for (int i=0; filesystemNames != null && i < filesystemNames.length; i++) {
      final String name = filesystemNames[i];
      final String className = JFMFileSystem.getRegisteredFilesystem(name);
      if (name == null || className == null) continue; 
      JFilesystemMenuItem filesystemItem = new JFilesystemMenuItem(name);
      filesystemItem.addActionListener(new ActionListener(){
        public void actionPerformed(ActionEvent e) {
           ChangeViewEvent ev=new ChangeViewEvent();
           ev.setSource(MainFrame.this);
           ev.setFilesystemClassName(className);
           Broadcaster.notifyChangeViewListeners(ev);                             
        }
      });
      jMenuFilesystem.add(filesystemItem);
      filesystemGroup.add(filesystemItem);
    }

    jMenuFilesystem.setMnemonic(KeyEvent.VK_S);
    jMenuFilesystem.addMenuListener(new MenuListener() {
        public void menuCanceled(MenuEvent e) {}
        public void menuDeselected(MenuEvent e) {}
        public void menuSelected(MenuEvent me) {
          updateFilesystemStatus(); 
        }
      });

    jMenuView.addSeparator();
    jMenuView.add(jMenuViewLookandFeel);
    addLookandFeels();
    jMenuView.addMenuListener(new MenuListener() {
        public void menuCanceled(MenuEvent e) {}
        public void menuDeselected(MenuEvent e) {}
        @SuppressWarnings("rawtypes")
		public void menuSelected(MenuEvent me) {
          String selectedName = mainPanel.getActiveViewClassName(); 
          for (Enumeration e = viewGroup.getElements(); e.hasMoreElements();) {
            JRadioButtonMenuItem item = (JRadioButtonMenuItem)e.nextElement();
            if (item == null) continue; 
            item.setSelected(false);
            final JFMViewRepresentation rep = JFMView.getViewByName(item.getText());
            if (rep == null) continue; 
            if (rep.getClassName().equals(selectedName)) 
              item.setSelected(true);
          } 
        }
      }); 

    jMenuFile.setMnemonic(KeyEvent.VK_F);
    jMenuFile.add(jMenuFileSearch);
    addConnectItems(jMenuFile); 
    addHistoryItems(jMenuFile); 
    jMenuFile.addSeparator();
    jMenuFile.add(jMenuFileExit);

    jMenuTools.setMnemonic(KeyEvent.VK_T);
    jMenuTools.add(jMenuFileView);
    jMenuTools.add(jMenuImageView);

    jMenuHelp.setMnemonic(KeyEvent.VK_H);
    jMenuHelp.add(jMenuHelpHelp);
    jMenuHelp.add(jMenuHelpHome);
    jMenuHelp.addSeparator();
    jMenuHelp.add(jMenuHelpAbout);

    JMenu edMenu = Options.getEditMenu();
    edMenu.setText(Strings.get("Edit"));
    
    jMenuBar1.add(jMenuFile);
    jMenuBar1.add(edMenu);
    jMenuBar1.add(jMenuFilesystem);
    jMenuBar1.add(jMenuTools);
    jMenuBar1.add(jMenuView);
    jMenuBar1.add(jMenuHelp);
    
    jMenuFileExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,KeyEvent.CTRL_MASK));
    jMenuFileSearch.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,KeyEvent.CTRL_MASK));
    this.setJMenuBar(jMenuBar1);
    contentPane.add(statusBar,BorderLayout.SOUTH);
    contentPane.add(mainPanel,BorderLayout.CENTER);

    Broadcaster.addChangeStatusListener(new ChangeStatusListener(){
      public void changeStatus(ChangeStatusEvent event){
        if (event.getStatus() == null) 
          statusBar.setText(Main.STATUS); 
        else
          statusBar.setText(event.getStatus()); 
      }
    });
  }
  
  private void addConnectItems(JMenu jMenuFile) {
    jMenuFile.addSeparator();

    String[] filesystemNames = JFMFileSystem.getRegisteredNames();
    for (int i=0; filesystemNames != null && i < filesystemNames.length; i++) {
      final String name = filesystemNames[i];
      if ("local".equals(name)) continue; 
      final String className = JFMFileSystem.getRegisteredFilesystem(name);
      if (name == null || className == null) continue;
      JConnectMenuItem filesystemItem = new JConnectMenuItem(name);
      filesystemItem.addActionListener(new ActionListener(){
        public void actionPerformed(ActionEvent e) {
           String fsClassName = className; 
           int result = JFMFileSystem.tryDisconnectFs(name); 
           if (result < 0 || result == 2) return; 
           ChangeViewEvent ev=new ChangeViewEvent();
           if (result == 1) {
             fsClassName = JFMFileSystem.LOCAL_FILESYSTEM; 
             ev.setFilesystemName(name); 
           } 
           ev.setSource(MainFrame.this);
           ev.setFilesystemClassName(fsClassName);
           Broadcaster.notifyChangeViewListeners(ev);
        }
      });
      jMenuFile.add(filesystemItem);
    }
  }

  private void addHistoryItems(final JMenu jMenuFile) {
    URI[] uris = Options.getHistoryUris(); 
    if (uris == null || uris.length == 0) 
      return; 

    jMenuFile.addSeparator();

    ArrayList<JHistoryMenuItem> items = new ArrayList<JHistoryMenuItem>(); 
    int idx = 0; 
    for (int i=0; i < uris.length; i++) {
      final URI uri = uris[i]; 
      if (uri == null) continue; 
      final String name = uri.getScheme(); 
      JHistoryMenuItem item = new JHistoryMenuItem(name, uri, ""+(++idx)+" "+uri.toString()); 
      item.addActionListener(new ActionListener(){
        public void actionPerformed(ActionEvent e) {
           int result = JFMFileSystem.tryDisconnectFs(name);
           if (result < 0 || result == 2) return;
           if (JFMFileSystem.setConnectUri(name, uri) == false) return; 
           String className = JFMFileSystem.getRegisteredFilesystem(name);
           if (className == null) return; 
           ChangeViewEvent ev=new ChangeViewEvent();
           ev.setSource(MainFrame.this);
           ev.setFilesystemName(name); 
           ev.setFilesystemClassName(className);
           ev.setReconnect(true); 
           Broadcaster.notifyChangeViewListeners(ev);
        }
      });
      jMenuFile.add(item); 
      items.add(item); 
    }

    final JHistoryMenuItem[] historyItems = items.toArray(new JHistoryMenuItem[items.size()]); 

    final JMenuItem cleanItem = new JMenuItem(Strings.get("0 Clean History")); 
    cleanItem.setIcon(new ImageIcon(getClass().getResource("/images/icons/edit-delete.png"))); 
    cleanItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e) {
        for (int i=0; historyItems != null && i < historyItems.length; i++) {
          jMenuFile.remove(historyItems[i]); 
        }
        Options.cleanHistoryUris(); 
        cleanItem.setEnabled(false); 
      }
    });
    jMenuFile.add(cleanItem); 
    if (idx < 1) cleanItem.setEnabled(false); 
  }

  public void updateFilesystemStatus() {
    String selectedName = mainPanel.getActiveFilesystemClassName();
    updateFilesystemStatus(selectedName); 
  }

  @SuppressWarnings("rawtypes")
  public void updateFilesystemStatus(String selectedName) {
    if (selectedName == null) return; 
    for (Enumeration e = filesystemGroup.getElements(); e.hasMoreElements();) {
      JFilesystemMenuItem item = (JFilesystemMenuItem)e.nextElement();
      if (item == null) continue;
      item.setSelected(false);
      String className = JFMFileSystem.getRegisteredFilesystem(item.getProtocol());
      if (className == null) continue;
      if (className.equals(selectedName))
        item.setSelected(true);
    }
  }

  private void showImageViewDialog(){
    JFMFile file = Options.getActivePanel().getSelectedFile(); 
    if (!Options.isImageFile(file)) 
      file = null; 

    JImageView.view(file != null ? file.toURI() : null);
  }

  private void showFileViewDialog(){
    JFMFile file = Options.getActivePanel().getSelectedFile(); 
    //if (!Options.isImageFile(file)) 
    //  file = null; 

    String title = Strings.get("File Viewer");
    if (file != null) title = file.getPath();
    
    FileViewDialog d = new FileViewDialog(Options.getMainFrame(),title,false);
    d.setLocationRelativeTo(Options.getMainFrame());
    if (file != null) d.setContent(file, false);
    d.setVisible(true);
  }
  
  private void showConfDialog(){
    ConfigurationDialog d=new ConfigurationDialog(this,Strings.get("Configuration"));
    d.setLocationRelativeTo(this);
    d.setVisible(true);
  }

  private void addLookandFeels() {
    final UIManager.LookAndFeelInfo[] lfs = UIManager.getInstalledLookAndFeels();
    if (lfs != null) {
      String selectedName = UIManager.getSystemLookAndFeelClassName(); 
      JRadioButtonMenuItem selectedItem = null; 
      for (int i=0; i<lfs.length; i++) {
        JRadioButtonMenuItem look = new JRadioButtonMenuItem(lfs[i].getName());
        final String className = lfs[i].getClassName();
        if (className.equals(selectedName)) 
          selectedItem = look; 
        look.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
              try {
                UIManager.setLookAndFeel(className);
                SwingUtilities.updateComponentTreeUI(MainFrame.this);
              } catch(Exception ex) {
                ex.printStackTrace();
              }
            }
        });
        jMenuViewLookandFeel.add(look);
        lookandfeelGroup.add(look); 
      }
      if (selectedItem != null) 
        selectedItem.setSelected(true); 
    }
  }
  
  private void changeFont() {
    Font f = FontDialog.showDialog(this,Strings.get("Choose font"),true);
    if (f == null) return;
    Options.setPanelsFont(f);
    FontChangeEvent event = new FontChangeEvent();
    event.setSource(this);
    Broadcaster.notifyFontChangeListeners(event);
  }
  
  private void changeForegroundColors() {
    Color c=JColorChooser.showDialog(this,Strings.get("Change foreground"),Options.getForegroundColor());
    if (c == null) return;
    Options.setForegroundColor(c);
    ColorChangeEvent event = new ColorChangeEvent(ColorChangeEvent.FOREGROUND);
    event.setSource(this);
    Broadcaster.notifyColorChangeListeners(event);    
  }
  
  private void changeBackgroundColors(){    
    Color c=JColorChooser.showDialog(this,Strings.get("Change background"),Options.getBackgroundColor());
    if (c == null) return;
    Options.setBackgroundColor(c);
    ColorChangeEvent event = new ColorChangeEvent(ColorChangeEvent.BACKGROUND);
    event.setSource(this);
    Broadcaster.notifyColorChangeListeners(event);
  }

  /**File | Seach action*/
  private void search(){
    SearchDialog dialog=new SearchDialog(this,Strings.get("Search ..."),false);
    dialog.setLocationRelativeTo(this);
    dialog.setVisible(true);
  }

  /**File | Exit action performed*/
  public void jMenuFileExit_actionPerformed(ActionEvent e) {
    Options.savePreferences();
    System.exit(0);
  }

  /**Help | About action performed*/
  public void jMenuHelpAbout_actionPerformed(ActionEvent e) {
    /* MainFrame_AboutBox dlg = new MainFrame_AboutBox(this);
    Dimension dlgSize = dlg.getPreferredSize();
    Dimension frmSize = getSize();
    Point loc = getLocation();
    dlg.setLocation((frmSize.width - dlgSize.width) / 2 + loc.x, (frmSize.height - dlgSize.height) / 2 + loc.y);
    dlg.setModal(true); */

    About dlg = new About(this); 
    dlg.setLocationRelativeTo(this); 
    dlg.setVisible(true);
  }

  public void jMenuHelpHelp_actionPerformed(ActionEvent e) {
    final HelpBrowser browser=new HelpBrowser(null,Strings.get("Help"),false);
    browser.setBaseURL(Options.getPreferences().get(Options.JFM_HELP_URL,Main.HELPURL));
    SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          browser.loadHomePage();
        }
      });
    browser.setVisible(true);
  }

  public void jMenuHelpHome_actionPerformed(ActionEvent e) {
    try {
      Desktop.getDesktop().browse(new URI(Main.HELPURL));
    } catch (Exception ex) {
      ex.printStackTrace(); 
    }
  }

  /**Overridden so we can exit when window is closed*/
  protected void processWindowEvent(WindowEvent e) {
    super.processWindowEvent(e);
    if (e.getID() == WindowEvent.WINDOW_CLOSING) {
      jMenuFileExit_actionPerformed(null);
    }
  }

}
