package com.forizon.jimage.viewer.actions;

import com.forizon.jimage.viewer.context.Context;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;

public class FlipHorizontally extends AbstractToggleableAction {
	private static final long serialVersionUID = 1L;

	public FlipHorizontally(Context aContext) {
        super(aContext);
     }

    @Override
    public void setValues() {
        super.setValues();
        putValue(MNEMONIC_KEY, KeyEvent.VK_H);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("control shift H"));
        putValue(SMALL_ICON, new javax.swing.ImageIcon(getClass().getResource("/images/icons/object-flip-horizontal.png")));
    }

    @Override
    public void setSelected(boolean selected) {
        context.getJImagePane().getJImageHandle().setFlipHorizontally(selected);
    }
    
}

