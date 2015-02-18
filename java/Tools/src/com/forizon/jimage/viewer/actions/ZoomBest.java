package com.forizon.jimage.viewer.actions;

import com.forizon.jimage.JImagePane;
import com.forizon.jimage.viewer.JImageView;
import com.forizon.jimage.viewer.context.Context;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.KeyStroke;

public class ZoomBest extends AbstractToggleableAction {
	private static final long serialVersionUID = 1L;
	
	ZoomBestComponentListener componentListener;

    public ZoomBest(Context aContext) {
        super(aContext);
     }

    @Override
    public void setValues() {
        super.setValues();
        putValue(MNEMONIC_KEY, KeyEvent.VK_B);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("control DECIMAL"));
        putValue(SMALL_ICON, new javax.swing.ImageIcon(getClass().getResource("/images/icons/zoom-best-fit.png")));

        // Handle whether action should be checked or not checked
        boolean state = Boolean.valueOf(
                  context.getConfiguration().getProperty(JImageView.CONFIGURATION_ZOOMTOFIT));

        context.getConfiguration().addPropertyChangeListener(
            JImageView.CONFIGURATION_ZOOMTOFIT,
            new ZoomToBestFilePropertyChangeListener());

        setSelected(state, false);
    }

    @Override
    public void setSelected(boolean selected) {
        setSelected(selected, true);
    }

    void performAction() {
        JImagePane jImagePane = context.getJImagePane();
        java.awt.Dimension size = jImagePane.getViewport().getSize();
        // Remove scrollbar width and height if they are visible.
        if (jImagePane.getHorizontalScrollBar().isVisible()) {
            size.height += jImagePane.getHorizontalScrollBar().getHeight();
        }
        if (jImagePane.getVerticalScrollBar().isVisible()) {
            size.width += jImagePane.getVerticalScrollBar().getWidth();
        }
        context.getJImagePane().getJImageHandle().fit(size);
    }

    void setSelected(boolean selected, boolean notify) {
        super.setSelected(selected);
        if (notify) {
            context.getConfiguration().setProperty(JImageView.CONFIGURATION_ZOOMTOFIT,
                    Boolean.toString(selected));
        }

        // Make sure that componentListener is not active
        if (componentListener != null) {
            context.getJImagePane().removeComponentListener(componentListener);
            componentListener = null;
        }

        if (selected) {
            putValue(NAME, context.getLanguageSupport().localizeString(
                this.getClass().getName() + ".On.text"));
            performAction();
            componentListener = new ZoomBestComponentListener();
            context.getJImagePane().addComponentListener(componentListener);
        } else {
            putValue(NAME, context.getLanguageSupport().localizeString(
                this.getClass().getName() + ".text"));
            AbstractAction action = context.getActions().get(ActionsBuilder.VIEW_ZOOM_GROUP
                                                + "-ZoomNormal");
            if (action != null) {
                action.actionPerformed(null);
            }
        }
    }

    @Override
    public void onImageChange() {
        performAction();
    }

    class ZoomToBestFilePropertyChangeListener
            implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            boolean state = Boolean.valueOf((String)evt.getNewValue());
            setSelected(state, false);
        }
    }

    class ZoomBestComponentListener extends ComponentAdapter {
        @Override
        public void componentResized(ComponentEvent e) {
            performAction();
        }
    }
}

