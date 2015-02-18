package com.forizon.jimage.viewer.actions;

import com.forizon.jimage.viewer.context.Context;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;

public class ZoomNormal extends AbstractAction {
	private static final long serialVersionUID = 1L;

	public ZoomNormal(Context aContext) {
        super(aContext);
    }

    @Override
    public void setValues() {
        super.setValues();
        putValue(MNEMONIC_KEY, KeyEvent.VK_N);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("control 0"));
        putValue(SMALL_ICON, new javax.swing.ImageIcon(getClass().getResource("/images/icons/zoom-original.png")));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        context.getJImagePane().getJImageHandle().setZoom(1);
    }
    
}

