package org.javenstudio.jfm.po;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;

import org.javenstudio.common.util.Strings;
import org.javenstudio.jfm.main.Options; 


public class ProgressActionDialog extends JDialog {
  private static final long serialVersionUID = 1L;
  
  private JPanel panel1 = new JPanel();
  private JLabel fileCopyLabel = new JLabel();
  private JLabel totalCopyLabel = new JLabel();
  private JProgressBar fileProgressBar = new JProgressBar();
  private JProgressBar totalProgressBar = new JProgressBar();
  private JLabel copyToLabel = new JLabel();
  private JTextField copyToDirField = new JTextField();
  private JLabel timeStatusLabel = new JLabel();
  private JLabel timeInfoLabel = new JLabel();
  private JLabel fileStatusLabel = new JLabel();
  private JLabel fileInfoLabel = new JLabel();
  private JLabel copyingLabel = new JLabel();
  private JTextField fileField = new JTextField();
  private JPanel buttonsPanel = new JPanel();
  private JButton cancelButton = new JButton();
  private JPanel progressPanel = new JPanel();
  private GridBagLayout gridBagLayout1 = new GridBagLayout();
  private ActionExecuter executer = null;
  private int operation = COPY; 
  private long startTime = System.currentTimeMillis(); 
  @SuppressWarnings("unused")
  private long updateTime = startTime; 
  private long breakTime = 0, breakStart = 0; 
  @SuppressWarnings("unused")
  private long totalFilesSize = 0, totalBytesWritten = 0; 
  private int totalFilesCount = 0, copiedFilesCount = 0; 
  
  public static final int COPY = 1; 
  public static final int MOVE = 2; 
  public static final int EXTRACT = 3; 
  public static final int ARCHIVE = 4; 
  public static final int DELETE = 5; 

  public void setBreakStart() {
    breakStart = System.currentTimeMillis();
  }

  public void setBreakStop() {
    if (breakStart > 0) {
      long breakStop = System.currentTimeMillis(); 
      if (breakStop > breakStart) 
        breakTime += breakStop - breakStart; 
    }
    breakStart = 0; 
  }

  private void setFileProgresssValue(int v) {
    if (v > 100) v = 100; 
    if (v < 0) v = 0; 
    fileProgressBar.setValue(v);
  } 

  private void setTotalProgresssValue(int v) {
    if (v > 100) v = 100; 
    if (v < 0) v = 0; 
    totalProgressBar.setValue(v);
  } 

  public void setProgressValue(long totalFilesSize, long totalBytesWritten, 
                               long fileSize, long bytesWritten, 
                               int totalFilesCount, int copiedFilesCount) {
    setProgressValue(totalFilesSize, totalBytesWritten, totalBytesWritten, fileSize, bytesWritten, 
                     totalFilesCount, copiedFilesCount); 
  }

  public void setProgressValue(long totalFilesSize, long totalFinishedSize, long totalBytesWritten, 
                               long fileSize, long bytesWritten, 
                               int totalFilesCount, int copiedFilesCount) {
    if (totalFilesSize < 0) totalFilesSize = 0; 
    if (fileSize < 0) fileSize = 0; 

    long currentTime = System.currentTimeMillis(); 
    this.totalFilesSize = totalFilesSize; 
    this.totalBytesWritten = totalBytesWritten; 
    this.totalFilesCount = totalFilesCount; 
    this.copiedFilesCount = copiedFilesCount; 

    int f_percent = fileSize > 0 ? (int)((bytesWritten*100)/fileSize) : 100;
    int t_percent = totalFilesSize > 0 ? (int)((totalFinishedSize*100)/totalFilesSize) : 0;

    long timeEscaped = currentTime - startTime - breakTime; 
    long secondsEscaped = timeEscaped / 1000; 

    long bytesPerSecs = timeEscaped > 0 ? (totalBytesWritten * 1000 / timeEscaped) : 0; 
    long finishedPerSecs = timeEscaped > 0 ? (totalFinishedSize * 1000 / timeEscaped) : 0; 
    float filesPerSecs = timeEscaped > 0 && totalFilesCount > 0 ? 
                         (float)((float)(copiedFilesCount * 1000) / (float)timeEscaped) : 0; 

    long secondsLeft = finishedPerSecs > 0 ? ((totalFilesSize - totalFinishedSize) / finishedPerSecs) : 0; 
    if (operation == DELETE) 
      secondsLeft = filesPerSecs > 0 ? (int)((float)(totalFilesCount - copiedFilesCount) / filesPerSecs) : 0; 
    if (secondsLeft < 0) secondsLeft *= -1; 

    if (secondsEscaped >= 0 && bytesPerSecs >= 0) {
      String info1 = Strings.format("%1$s elapsed and %2$s left", 
    		  Options.secondDesc(secondsEscaped), Options.secondDesc(secondsLeft)) 
         + " (" +
         copiedFilesCount + "/" + totalFilesCount + " " + Strings.get("files") + ", " + 
         Options.numberDesc(filesPerSecs) + " " + Strings.get("files") + "/s, " +
         Options.byteDesc(bytesPerSecs) + "/s)"; 

      String info2 = "" + 
         Options.byteDesc(totalBytesWritten) + "(" + totalBytesWritten + " "+ Strings.get("bytes") + ")/" + 
         Options.byteDesc(totalFilesSize) + "(" + totalFilesSize + " " + Strings.get("bytes") +")"; 

      timeInfoLabel.setText(info1); 
      fileInfoLabel.setText(info2); 
    }

    setFileProgresssValue(f_percent);
    setTotalProgresssValue(t_percent);

    updateTime = currentTime; 
  }

  private void setProgressDone() {
    if (operation == MOVE) 
      copyingLabel.setText(Strings.get("Moved file:"));
    else if (operation == EXTRACT) 
      copyingLabel.setText(Strings.get("Extracted file:"));
    else if (operation == ARCHIVE) 
      copyingLabel.setText(Strings.get("Archived file:"));
    else if (operation == DELETE) 
      copyingLabel.setText(Strings.get("Deleted file:"));
    else 
      copyingLabel.setText(Strings.get("Copied file:"));
    cancelButton.setText(Strings.get("Done")); 
  }

  public void setCopyingFile(String name) {
    fileField.setText(name);
  }

  public void setCopyingTo(String name) {
    copyToDirField.setText(name); 
  }

  public void setScaningStart() {
    copyingLabel.setText(Strings.get("Scaning file:"));
  }

  public void setScaningFile(String name) {
    fileField.setText(name); 
  }

  public void setScaningDone() {
    updateDoingLabel(); 
  }

  public void updateDoingLabel() {
    if (operation == MOVE) 
      copyingLabel.setText(Strings.get("Moving file:"));
    else if (operation == EXTRACT) 
      copyingLabel.setText(Strings.get("Extracting file:"));
    else if (operation == ARCHIVE) 
      copyingLabel.setText(Strings.get("Archiving file:"));
    else if (operation == DELETE) {
      copyingLabel.setText(Strings.get("Deleting file:"));
      copyToLabel.setText(Strings.get("At directory:"));
    } else 
      copyingLabel.setText(Strings.get("Copying file:"));
  }

  public void setOperation(int op) {
    operation = op; 
    updateDoingLabel(); 
  }

  public void startAction(final ActionExecuter ex) {
    this.executer = ex;
    Thread actionThread = new Thread(new Runnable(){
      public void run(){
        executer.start();
        setProgressDone(); 
        long currentTime = System.currentTimeMillis();
        long timeEscaped = currentTime - startTime;
        if (timeEscaped >= 0 && timeEscaped < 5000)
          closeDialog(); 
      }
    });
    actionThread.start();
    this.setVisible(true);
  }

  public ProgressActionDialog(Frame frame, String title, boolean modal) {
    super(frame, title, modal);
    try {
      jbInit();
      pack();
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  public ProgressActionDialog() {
    this(null, "", false);
  }
 
  void jbInit() throws Exception {
    setResizable(false);
    panel1.setLayout(new BorderLayout());
    panel1.add(progressPanel,BorderLayout.NORTH);
    panel1.add(buttonsPanel, BorderLayout.SOUTH);
    getContentPane().add(panel1);

    copyToDirField.setText(""); 
    copyToDirField.setFont(Options.getPanelsFont());
    copyToDirField.setEditable(false); 
    fileField.setText(""); 
    fileField.setFont(Options.getPanelsFont());
    fileField.setEditable(false); 

    progressPanel.setLayout(gridBagLayout1);
    copyingLabel.setText(Strings.get("Copying file:"));
    copyToLabel.setText(Strings.get("To directory:"));
    timeStatusLabel.setText(Strings.get("Time status:"));
    timeInfoLabel.setText(""); 
    fileStatusLabel.setText(Strings.get("Data status:"));
    fileInfoLabel.setText(""); 
    fileCopyLabel.setText(Strings.get("File progress:"));
    totalCopyLabel.setText(Strings.get("Total progress:"));
    totalProgressBar.setStringPainted(true);
    totalProgressBar.setMaximum(100); 
    totalProgressBar.setMinimum(0); 
    fileProgressBar.setStringPainted(true);
    fileProgressBar.setMaximum(100); 
    fileProgressBar.setMinimum(0); 

    progressPanel.add(totalProgressBar,  new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 10, 5, 10), 3, 6));
    progressPanel.add(totalCopyLabel,    new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 10, 5, 10), 10, 5));
    progressPanel.add(fileProgressBar,   new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 10, 5, 10), 3, 6));
    progressPanel.add(fileCopyLabel,     new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 10, 5, 10), 10, 5));
    progressPanel.add(fileInfoLabel,     new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 10, 5, 10), 20, 5));
    progressPanel.add(fileStatusLabel,   new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 10, 5, 10), 10, 5));
    progressPanel.add(timeInfoLabel,     new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 10, 5, 10), 20, 5));
    progressPanel.add(timeStatusLabel,   new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 10, 5, 10), 10, 5));
    progressPanel.add(copyToDirField,    new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 10, 5, 10), 500, 2));
    progressPanel.add(copyToLabel,       new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 10, 5, 10), 10, 5));
    progressPanel.add(fileField,         new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 10, 5, 10), 500, 2));
    progressPanel.add(copyingLabel,      new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 10, 5, 10), 10, 5));
    
    totalProgressBar.addChangeListener(new ChangeListener(){
      public void stateChanged(ChangeEvent e) {
        if (totalProgressBar.getValue() >= 100 && copiedFilesCount >= totalFilesCount) {
          setProgressDone(); 
        }
      }
    });

    buttonsPanel.add(cancelButton, null);
    cancelButton.setText(Strings.get("Cancel"));
    cancelButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        cancelButton_actionPerformed(e);
      }
    });

    setFileProgresssValue(0);
    setTotalProgresssValue(0);
  }
  
  void cancelButton_actionPerformed(ActionEvent e) {    
    executer.cancel();    
    closeDialog(); 
  }
  
  void closeDialog() {
    this.dispose();
  }
}
