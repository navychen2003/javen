package com.forizon.jimage.viewer.actions;

import com.forizon.jimage.viewer.context.Context;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class PageSetup extends AbstractAction {
	private static final long serialVersionUID = 1L;
	
	/** Handles printing */
    final Print printAction;
    public PageSetup(Context aContext, Print printAction) {
        super(aContext);
        this.printAction = printAction;
    }

    @Override
    public void setValues() {
        super.setValues();
        putValue(MNEMONIC_KEY, KeyEvent.VK_S);
        putValue(SMALL_ICON, new javax.swing.ImageIcon(getClass().getResource("/images/icons/document-properties.png")));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            printAction.getPrintUtilities().getPrintJob().printDialog();
        } catch (HeadlessException ex) {    // Printing not supported on system
            context.getReporter().report(ex);
        }
    }
    
}

