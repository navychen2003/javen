package com.forizon.jimage;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.JScrollPane;

/**
 * <code>{@link javax.swing.JPanel}</code> which contains a
 * <code>{@link javax.swing.JScrollPane}</code> and a
 * <code>{@link JImage}</code>. Also wraps a <code>{@link JImageHandle}</code>
 */
public class JImagePane extends JScrollPane {
	private static final long serialVersionUID = 1L;
	/** The key for the file property */
    final static public String FILE_PROPERTY = "file";
    /** Handle for image transformations */
    final protected JImageHandle handle;
    /** JImage component */
    protected JImage jImage;

    /** Component constructor */
    public JImagePane() {
        super(new JImage());
        jImage = (JImage)getViewport().getComponent(0);
        jImage.setOpaque(true);
        jImage.setDoubleBuffered(true);
        jImage.setBackground(java.awt.Color.WHITE);

        MouseHandler handler = new MouseHandler();
        jImage.addMouseListener(handler);
        jImage.addMouseMotionListener(handler);
        jImage.addMouseWheelListener(handler);

        handle = new JImageHandle(jImage);
    }

    /**
     * Returns the JImage
     * @return the JImage
     */
    public JImage getJImage() {
        return jImage;
    }

    /**
     * Returns the JImageHandle which handles the JImage's transformation
     * @return the JImageHandle which handles the JImage's transformation
     */
    public JImageHandle getJImageHandle() {
        return handle;
    }

    /**
     * Handles mouse events for <code>{@link JImagePane}<code>
     */
    class MouseHandler
            implements MouseWheelListener, MouseListener, MouseMotionListener {
        /** The starting point of a drag-drop */
        Point start;
        /** The scroll rate per mousewheel scroll "click" */
        double scrollRate;

        public MouseHandler() {
            scrollRate = 75.0;
        }

        /**
         * Sets the scroll rate per mousewheel scroll "click"
         * @param aScrollRate how much the image should be scaled per mousewheel
         * scroll "click"
         */
        public void setScrollRate(double aScrollRate) {
            scrollRate = aScrollRate;
        }

        /**
         * Returns the scroll rate per mousewheel scroll "click"
         * @return the scroll rate per mousewheel scroll "click"
         */
        public double getScrollRate() {
            return scrollRate;
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent evt) {
            handle.zoom(evt.getWheelRotation() / scrollRate);
        }

        @Override
        public void mouseEntered(MouseEvent evt) {
            jImage.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }

        @Override
        public void mouseExited(MouseEvent evt) {
            jImage.setCursor(Cursor.getDefaultCursor());
        }

        @Override
        public void mousePressed(MouseEvent evt) {
            start = evt.getLocationOnScreen();
        }

        @Override
        public void mouseReleased(MouseEvent evt) {
            start = null;
        }

        @Override
        public void mouseDragged(MouseEvent evt) {
            if (start != null && JImagePane.this != null) {
                Point current = evt.getLocationOnScreen();
                JImagePane.this.getVerticalScrollBar().setValue(start.y - current.y
                                   + JImagePane.this.getVerticalScrollBar().getValue());
                JImagePane.this.getHorizontalScrollBar().setValue(start.x - current.x
                                 + JImagePane.this.getHorizontalScrollBar().getValue());
                start = current;
            }
        }

        @Override
        public void mouseMoved(MouseEvent evt) {}
        @Override
        public void mouseClicked(MouseEvent evt) {}
    }
}

