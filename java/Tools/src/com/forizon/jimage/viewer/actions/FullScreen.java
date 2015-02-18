package com.forizon.jimage.viewer.actions;

import com.forizon.jimage.viewer.context.Context;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.Timer;
import javax.swing.KeyStroke;

public class FullScreen extends AbstractToggleableAction {
	private static final long serialVersionUID = 1L;
	
	/** Hide menu in fullscreen timer */
    Timer timer;
    Rectangle bounds;
    MouseMotionAdapter mouseMotionAdapter;

    public FullScreen(Context aContext) {
        super(aContext);
    }

    @Override
    public void setValues() {
        super.setValues();
        putValue(MNEMONIC_KEY, KeyEvent.VK_F);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("F11"));
        putValue(SMALL_ICON, new javax.swing.ImageIcon(getClass().getResource("/images/icons/view-fullscreen.png")));
    }

    @Override
    public void setSelected(boolean selected) {
        JFrame view = context.getWindow();

        if (timer == null) {
            timer = new Timer(3000, new Listener());
        }

        view.dispose(); // Dispose the frame so that the decoration can be changed
        view.setUndecorated(selected);
        if (selected) {
            putValue(SMALL_ICON, new javax.swing.ImageIcon(getClass().getResource("/images/icons/view-normal-screen.png")));
            // "hide" menus (setVisible(false) will also remove the hotkeys)
            view.getJMenuBar().setPreferredSize(new java.awt.Dimension(0, 0));
            bounds = view.getBounds();
            // Set the window bounds to the default screen's bounds
            view.setBounds(view.getGraphicsConfiguration().getBounds());
            timer.start();
            // Attatch the mouse listeners
            mouseMotionAdapter = new MouseMotionAdapter();
            context.getJImagePane().getJImage().addMouseMotionListener(mouseMotionAdapter);
            view.getJMenuBar().addMouseMotionListener(mouseMotionAdapter);
        } else {
            context.getJImagePane().getJImage().removeMouseMotionListener(mouseMotionAdapter);
            view.getJMenuBar().removeMouseMotionListener(mouseMotionAdapter);
            mouseMotionAdapter = null;
            // Undo everything
            putValue(SMALL_ICON, new javax.swing.ImageIcon(getClass().getResource("/images/icons/view-fullscreen.png")));
            view.getJMenuBar().setPreferredSize(null); // see JComponent javadoc
            view.setBounds(bounds);
            timer.stop();
        }
        view.setVisible(true);
        view.repaint();
    }

    @Override
    public void onImageChange() {}

    public Timer getTimer() {
        return timer;
    }

    class MouseMotionAdapter extends java.awt.event.MouseMotionAdapter {
        public MouseMotionAdapter() {
            super();
        }

        @Override
        public void mouseMoved(java.awt.event.MouseEvent evt) {
            if (evt.getYOnScreen() < 50) {
                JMenuBar jMenuBar = context.getWindow().getJMenuBar();
                jMenuBar.setPreferredSize(null);
                jMenuBar.revalidate();
                getTimer().restart();
            }
        }
    }

    class Listener implements ActionListener {
        @Override
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            ((Timer)evt.getSource()).stop();
            JMenuBar jMenuBar = context.getWindow().getJMenuBar();
            jMenuBar.setPreferredSize(new java.awt.Dimension(0,0));
            jMenuBar.revalidate();
        }
    }
}
