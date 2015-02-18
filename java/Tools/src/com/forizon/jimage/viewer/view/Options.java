package com.forizon.jimage.viewer.view;

import com.forizon.jimage.viewer.JImageView;
import com.forizon.jimage.viewer.context.Context;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;

import org.javenstudio.common.util.Strings;

/**
 * JImageView's Option dialog. Changes to the form will update the
 * associated <code>Properties</code> object.
 */
public class Options extends javax.swing.JDialog {
	private static final long serialVersionUID = 1L;
	final Context context;

    /**
     * Creates a new Option dialog
     * @param context The 
     * @param modal
     */
    public Options(Context context, boolean modal) {
        super(context.getWindow(), modal);
        this.context = context;
        initComponents();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane = new javax.swing.JTabbedPane();
        jPanelViewer = new javax.swing.JPanel();
        jCheckBoxZoomBestFit = new javax.swing.JCheckBox();
        jCheckBoxLoop = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jPanelSlideShow = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jSpinnerDelay = new javax.swing.JSpinner();
        jButton1 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jCheckBoxZoomBestFit.setSelected(context.getConfiguration().getProperty(JImageView.CONFIGURATION_ZOOMTOFIT).equals("true"));
        jCheckBoxZoomBestFit.setText(Strings.get("Zoom to best fit")); // NOI18N
        jCheckBoxZoomBestFit.setToolTipText(Strings.get("If this is set to true, then when an image is displayed, it will automatically be zoomed so that everything is visiable without scrollbars")); // NOI18N
        jCheckBoxZoomBestFit.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jCheckBoxZoomBestFitItemStateChanged(evt);
            }
        });

        jCheckBoxLoop.setSelected(context.getConfiguration().getProperty(JImageView.CONFIGURATION_LOOP).equals("true"));
        jCheckBoxLoop.setText(Strings.get("Continous Loop")); // NOI18N
        jCheckBoxLoop.setToolTipText(Strings.get("If this is set to true, then the \"next\" image of a directory is considered the first image of the directory, and the \"previous\" image of a directory is considered the first image of the directory.")); // NOI18N
        jCheckBoxLoop.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jCheckBoxLoopItemStateChanged(evt);
            }
        });

        jLabel1.setFont(jLabel1.getFont().deriveFont(jLabel1.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel1.setText(Strings.get("When Displaying")); // NOI18N

        jLabel5.setFont(jLabel5.getFont().deriveFont(jLabel5.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel5.setText(Strings.get("Image Order")); // NOI18N

        javax.swing.GroupLayout jPanelViewerLayout = new javax.swing.GroupLayout(jPanelViewer);
        jPanelViewer.setLayout(jPanelViewerLayout);
        jPanelViewerLayout.setHorizontalGroup(
            jPanelViewerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelViewerLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelViewerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelViewerLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(jCheckBoxLoop))
                    .addComponent(jLabel1)
                    .addGroup(jPanelViewerLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(jCheckBoxZoomBestFit))
                    .addComponent(jLabel5))
                .addContainerGap(82, Short.MAX_VALUE))
        );
        jPanelViewerLayout.setVerticalGroup(
            jPanelViewerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelViewerLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxZoomBestFit)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxLoop)
                .addContainerGap(38, Short.MAX_VALUE))
        );

        jTabbedPane.addTab(Strings.get("Display"), jPanelViewer);

        jLabel2.setFont(jLabel2.getFont().deriveFont(jLabel2.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel2.setText(Strings.get("Transitions")); // NOI18N

        jLabel3.setText(Strings.get("Switch after:")); // NOI18N

        jLabel4.setText("(s)"); // NOI18N

        jSpinnerDelay.setValue(Integer.parseInt(context.getConfiguration().getProperty(JImageView.CONFIGURATION_SLIDESHOWDELAY)));
        jSpinnerDelay.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinnerDelayStateChanged(evt);
            }
        });

        javax.swing.GroupLayout jPanelSlideShowLayout = new javax.swing.GroupLayout(jPanelSlideShow);
        jPanelSlideShow.setLayout(jPanelSlideShowLayout);
        jPanelSlideShowLayout.setHorizontalGroup(
            jPanelSlideShowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSlideShowLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelSlideShowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addGroup(jPanelSlideShowLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(jLabel3)
                        .addGap(6, 6, 6)
                        .addComponent(jSpinnerDelay, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(3, 3, 3)
                        .addComponent(jLabel4)))
                .addContainerGap(40, Short.MAX_VALUE))
        );
        jPanelSlideShowLayout.setVerticalGroup(
            jPanelSlideShowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSlideShowLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelSlideShowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4)
                    .addComponent(jSpinnerDelay, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(78, Short.MAX_VALUE))
        );

        jTabbedPane.addTab(Strings.get("Slide Show"), jPanelSlideShow);

        jButton1.setText(Strings.get("OK")); // NOI18N
        jButton1.setPreferredSize(new java.awt.Dimension(50, 29));
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(150, Short.MAX_VALUE)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addComponent(jTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 210, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 157, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
        setLocationRelativeTo(context.getWindow()); 
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        Toolkit.getDefaultToolkit()
                .getSystemEventQueue()
                .postEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jCheckBoxZoomBestFitItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jCheckBoxZoomBestFitItemStateChanged
        context.getConfiguration().setProperty(JImageView.CONFIGURATION_ZOOMTOFIT,
                Boolean.toString(jCheckBoxZoomBestFit.isSelected()));
    }//GEN-LAST:event_jCheckBoxZoomBestFitItemStateChanged

    private void jCheckBoxLoopItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jCheckBoxLoopItemStateChanged
        context.getConfiguration().setProperty(JImageView.CONFIGURATION_LOOP,
                Boolean.toString(jCheckBoxLoop.isSelected()));
    }//GEN-LAST:event_jCheckBoxLoopItemStateChanged

    private void jSpinnerDelayStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSpinnerDelayStateChanged
        context.getConfiguration().setProperty(JImageView.CONFIGURATION_SLIDESHOWDELAY,
                jSpinnerDelay.getValue().toString());
    }//GEN-LAST:event_jSpinnerDelayStateChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JCheckBox jCheckBoxLoop;
    private javax.swing.JCheckBox jCheckBoxZoomBestFit;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanelSlideShow;
    private javax.swing.JPanel jPanelViewer;
    private javax.swing.JSpinner jSpinnerDelay;
    private javax.swing.JTabbedPane jTabbedPane;
    // End of variables declaration//GEN-END:variables
    
}

