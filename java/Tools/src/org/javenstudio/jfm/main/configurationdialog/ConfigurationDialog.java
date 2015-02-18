package org.javenstudio.jfm.main.configurationdialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.ImageIcon;

import org.javenstudio.common.util.Strings;


/**
 * the dialog that is used to set various configuration options
 * @author sergiu
 */
public class ConfigurationDialog extends JDialog {
  private static final long serialVersionUID = 1L;
  
  private JPanel contentPanel;
  private JPanel buttonsPanel;
  private ConfigurationDialogPanel mainPanel;
  private boolean cancelled=false;
  private JButton okButton=new JButton(Strings.get("OK"));
  private JButton cancelButton=new JButton(Strings.get("Cancel"));
  private JButton applyButton=new JButton(Strings.get("Apply"));
    
  
  /**
   * @param owner
   * @param title
   * @throws java.awt.HeadlessException
   */
  public ConfigurationDialog(Frame owner, String title)
      throws HeadlessException {
    this(owner, title,true);
  }

  /**
   * @param owner
   * @param title
   * @param modal
   * @throws java.awt.HeadlessException
   */
  public ConfigurationDialog(Frame owner, String title, boolean modal)
      throws HeadlessException {
    super(owner, title, modal);
    init();
  }
  
  private void init(){
    setIconImage(new ImageIcon(getClass().getResource("/images/icons/configuration.png")).getImage());
    setSize(500,350);
    contentPanel=(JPanel)getContentPane();
    contentPanel.setLayout(new BorderLayout());    
    setupButtonsPanel();
    mainPanel=new ConfigurationDialogPanel();
    ConfigurationEventsQueue.setConfigurationDialog(this);
    contentPanel.add(buttonsPanel,BorderLayout.SOUTH);
    contentPanel.add(mainPanel,BorderLayout.CENTER);
  }
  
  private void setupButtonsPanel(){
    okButton.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e){
        okButtonActionPeformed();
      }
    });
    cancelButton.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e){
        cancelButtonActionPeformed();
      }
    });
    applyButton.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e){
        applyButtonActionPeformed();
      }
    });
    
    buttonsPanel =new JPanel(new FlowLayout(FlowLayout.RIGHT));    
    buttonsPanel.add(okButton);
    buttonsPanel.add(applyButton);
    buttonsPanel.add(cancelButton);  
    applyButton.setEnabled(false);
  }
  
  /**
   * Called when something changed in the configuration properties
   */
  public void configurationChanged(){
    applyButton.setEnabled(true);
  }
  
  private void okButtonActionPeformed(){
    setCancelled(false);
    ConfigurationEventsQueue.notifyPendingEvents();
    dispose();
  }
  
  private void cancelButtonActionPeformed(){
    setCancelled(true);
    ConfigurationEventsQueue.clearPendingEvents();
    dispose();
  }
  
  private void applyButtonActionPeformed(){
    ConfigurationEventsQueue.notifyPendingEvents();
    applyButton.setEnabled(false);
  }
  
  /**
   * @return Returns the cancelled.
   */
  public boolean isCancelled() {
    return cancelled;
  }

  /**
   * @param cancelled The cancelled to set.
   */
  private void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }

}
