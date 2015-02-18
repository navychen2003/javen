package com.forizon.jimage.viewer.actions;

import com.forizon.jimage.viewer.context.Context;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;

public class RotateRight extends AbstractAction {
	private static final long serialVersionUID = 1L;

	public RotateRight(Context aContext) {
        super(aContext);
    }

    @Override
    public void setValues() {
        putValue(MNEMONIC_KEY, KeyEvent.VK_R);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("shift control RIGHT"));
        putValue(SMALL_ICON, new javax.swing.ImageIcon(getClass().getResource("/images/icons/object-rotate-right.png")));
        super.setValues();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        context.getJImagePane().getJImageHandle().rotate(0.5 * Math.PI);
    }
    
}

