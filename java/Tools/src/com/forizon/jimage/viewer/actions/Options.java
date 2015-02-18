package com.forizon.jimage.viewer.actions;

import com.forizon.jimage.viewer.context.Context;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class Options extends AbstractAction {
	private static final long serialVersionUID = 1L;

	public Options(Context aContext) {
        super(aContext);
    }

    @Override
    public void setValues() {
        super.setValues();
        putValue(MNEMONIC_KEY, KeyEvent.VK_R);
        putValue(SMALL_ICON, new javax.swing.ImageIcon(getClass().getResource("/images/icons/window-options.png")));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new com.forizon.jimage.viewer.view.Options(context, true).setVisible(true);
    }
}

