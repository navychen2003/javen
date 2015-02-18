package com.forizon.jimage.viewer.actions;

import com.forizon.jimage.viewer.context.Context;
import java.awt.event.ActionEvent;
import javax.swing.KeyStroke;

public class RotateNormal extends AbstractAction {
	private static final long serialVersionUID = 1L;

	public RotateNormal(Context aContext) {
        super(aContext);
    }

    @Override
    public void setValues() {
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("alt R"));
        super.setValues();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        context.getJImagePane().getJImageHandle().setRotation(0);
    }
}

