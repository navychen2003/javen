package com.forizon.jimage.viewer.actions;

import com.forizon.jimage.viewer.context.Context;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class About extends AbstractAction {
	private static final long serialVersionUID = 1L;

	public About(Context aContext) {
        super(aContext);
     }

    @Override
    public void setValues() {
        super.setValues();
        putValue(MNEMONIC_KEY, KeyEvent.VK_A);
    }

    public void actionPerformed(ActionEvent e) {
        new com.forizon.jimage.viewer.view.About(context).setVisible(true);
    }
}
 
