package com.forizon.jimage.viewer.actions;

import com.forizon.jimage.viewer.context.Context;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;

public class FlipVertically extends AbstractToggleableAction {
	private static final long serialVersionUID = 1L;

	public FlipVertically(Context aContext) {
        super(aContext);
     }

    @Override
    public void setValues() {
        super.setValues();
        putValue(MNEMONIC_KEY, KeyEvent.VK_V);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("control shift V"));
        putValue(SMALL_ICON, new javax.swing.ImageIcon(getClass().getResource("/images/icons/object-flip-vertical.png")));
    }

    @Override
    public void setSelected(boolean selected) {
        context.getJImagePane().getJImageHandle().setFlipVertically(selected);
    }
}

