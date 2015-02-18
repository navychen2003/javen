package com.forizon.jimage.viewer.actions;

import com.forizon.jimage.viewer.context.Context;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;

public class Close extends AbstractAction {
	private static final long serialVersionUID = 1L;

	public Close(Context aContext) {
        super(aContext);
     }

    @Override
    public void setValues() {
        super.setValues();
        putValue(MNEMONIC_KEY, KeyEvent.VK_C);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("control W"));
        putValue(SMALL_ICON, new javax.swing.ImageIcon(getClass().getResource("/images/icons/window-close.png")));
    }

    public void actionPerformed(ActionEvent e) {
        JFrame view = context.getWindow();
        view.dispatchEvent(new WindowEvent(view, WindowEvent.WINDOW_CLOSING));
    }    
}

