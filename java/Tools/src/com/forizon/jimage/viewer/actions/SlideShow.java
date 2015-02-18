package com.forizon.jimage.viewer.actions;

import com.forizon.jimage.viewer.JImageView;
import com.forizon.jimage.viewer.context.Context;
import com.forizon.jimage.viewer.imagelist.AbstractIteratorListener;
import com.forizon.jimage.viewer.imagelist.ImageIdentityListModelIterator;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Timer;
import javax.swing.KeyStroke;

public class SlideShow extends AbstractToggleableAction {
	private static final long serialVersionUID = 1L;
	
	/** Slide show delay timer */
    Timer timer;
    MouseListener jImageMouseListener;
    ImageChangeListener jImageFileChangeListener;

    public SlideShow(Context aContext) {
        super(aContext);
    }

    @Override
    public void setValues() {
        super.setValues();
        putValue(MNEMONIC_KEY, KeyEvent.VK_O);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0));
        putValue(SMALL_ICON, new javax.swing.ImageIcon(getClass().getResource("/images/icons/media-playback-start.png")));
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);

        if (timer == null) {
            timer = new Timer(Integer.parseInt(context.getConfiguration().getProperty(JImageView.CONFIGURATION_SLIDESHOWDELAY)) * 1000, new SlideShowListener());
            jImageFileChangeListener = new ImageChangeListener();
            jImageMouseListener = new MouseListener();
        }

        if (selected) {
            putValue(SMALL_ICON, new javax.swing.ImageIcon(getClass().getResource("/images/icons/media-playback-pause.png")));
            context.getJImagePane().getJImage().addMouseListener(jImageMouseListener);
            context.getImageContext().getIterator().addIteratorListener(jImageFileChangeListener);
            timer.setInitialDelay(Integer.parseInt(context.getConfiguration().getProperty(JImageView.CONFIGURATION_SLIDESHOWDELAY)) * 1000);
            timer.start();
        } else {
            context.getJImagePane().getJImage().removeMouseListener(jImageMouseListener);
            putValue(SMALL_ICON, new javax.swing.ImageIcon(getClass().getResource("/images/icons/media-playback-start.png")));
            context.getImageContext().getIterator().removeIteratorListener(jImageFileChangeListener);
            timer.stop();
        }
    }

    @Override
    public void onImageChange() {}

    /**
     * Reset the timer whenever the image changes
     */
    class ImageChangeListener extends AbstractIteratorListener {
        @Override
        public void nextChange(ImageIdentityListModelIterator.NextChangeEvent e) {
            timer.restart();
        }
    }

    /**
     * Listens for JImagePane click events and pauses slideshow
     */
    class MouseListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            timer.stop();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            timer.start();
        }
    }

    /**
     * Changes image when timer hits
     */
    class SlideShowListener implements java.awt.event.ActionListener {
        @Override
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            context.getActions().get(ActionsBuilder.FILE_NAVIGATION_GROUP + "-Next")
                .actionPerformed(null);
        }
    }
}

