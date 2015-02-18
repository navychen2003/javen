package com.forizon.jimage.viewer.context;

import com.forizon.jimage.viewer.JImageView;
import com.forizon.util.PersistentProperties;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;

class WindowListener extends WindowAdapter {
    final Context context;

    public WindowListener(Context aContext) {
        context = aContext;
    }

    @Override
    public void windowClosing(WindowEvent e) {
        JFrame window = context.getWindow();
        if (window.getExtendedState() == JFrame.NORMAL) {
            Rectangle bounds = window.getBounds();
            PersistentProperties configuration = context.getConfiguration();
            configuration.setProperty(JImageView.CONFIGURATION_WIDTH, "" + bounds.width);
            configuration.setProperty(JImageView.CONFIGURATION_HEIGHT, "" + bounds.height);
            try {
                context.getConfiguration().save();
            } catch (java.io.IOException ex) {
                context.reporter.report(ex);
            }
        }
    }
}

