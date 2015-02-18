package org.javenstudio.jfm.po;

import org.javenstudio.common.util.Strings;
import org.javenstudio.jfm.help.HelpBrowser;
import org.javenstudio.jfm.main.Main;
import org.javenstudio.jfm.main.Options;

import java.awt.event.ActionEvent;
import javax.swing.SwingUtilities; 


public class HelpAction extends AbstractAction {
  private static final long serialVersionUID = 1L;

  public HelpAction() {
  }
  
  public void actionPerformed(ActionEvent e) {
    final HelpBrowser browser = new HelpBrowser(null, Strings.get("Help"), false);
    browser.setBaseURL(Options.getPreferences().get(Options.JFM_HELP_URL, Main.HELPURL));
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        browser.loadHomePage();
      }
    });
    browser.setVisible(true);
  }

}
