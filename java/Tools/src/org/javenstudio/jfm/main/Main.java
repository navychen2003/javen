package org.javenstudio.jfm.main;

import java.awt.*;
import javax.swing.UIManager;

import org.javenstudio.jfm.filesystems.JFMFileSystem;
import org.javenstudio.jfm.views.JFMView;


public class Main {
  public static final String VERSION = "0.1.0";
  public static final String WEBSITE = "http://www.anybox.org";
  public static final String HELPURL = "http://www.anybox.org";
  public static final String COPYRIGHT = "Copyright (c) 2009-2014 Javen-Studio";
  public static final String STATUS = "Javen-Studio (c) 2009-2014";
  public static final String AUTHOR = "Naven Chan";
  
  private boolean packFrame = false;

  /**Construct the application*/
  public Main() {
    MainFrame frame = new MainFrame();
    //Validate frames that have preset sizes
    //Pack frames that have useful preferred size info, e.g. from their layout
    if (packFrame) {
      frame.pack();
    }
    else {
      frame.validate();
    }
    //Center the window
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension frameSize = frame.getSize();
    if (frameSize.height > screenSize.height) {
      frameSize.height = screenSize.height;
    }
    if (frameSize.width > screenSize.width) {
      frameSize.width = screenSize.width;
    }
    frame.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
    frame.setVisible(true);
  }

  /**Main method*/
  public static void main(String[] args) { 
	doMain(args);
  }
  
  public static void doMain(String[] args) {
    try {      
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      JFMView.registerViews();      
      JFMFileSystem.registerFilesystems();
    } catch(Exception e) {
      e.printStackTrace();
    } 
    //final SplashWindow splash=new SplashWindow("someimage.gif");
    //splash.setVisible(true);
    new Main();
    EventQueue.invokeLater(new Runnable(){
      public void run() {
      //  splash.dispose();
      }
    });
  }

}
