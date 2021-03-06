package com.forizon.jimage.viewer.actions;

import com.forizon.jimage.viewer.context.Context;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;

public class ZoomOut extends AbstractAction {
	private static final long serialVersionUID = 1L;
	
	final public static double ZOOM_INCREMENT_RATE = 0.1;

    public ZoomOut(Context aContext) {
        super(aContext);
    }

    @Override
    public void setValues() {
        super.setValues();
        putValue(MNEMONIC_KEY, KeyEvent.VK_O);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("control MINUS"));
        putValue(SMALL_ICON, new javax.swing.ImageIcon(getClass().getResource("/images/icons/zoom-out.png")));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        context.getJImagePane().getJImageHandle().zoom(-ZOOM_INCREMENT_RATE);
    }
}

