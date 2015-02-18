package com.forizon.jimage.viewer.actions;

import com.forizon.jimage.viewer.context.Context;
import com.forizon.util.PrintUtilities;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.print.PrinterException;
import javax.swing.KeyStroke;

public class Print extends AbstractAction {
	private static final long serialVersionUID = 1L;
	
	/** Handles printing */
    PrintUtilities printUtilities;
    
    public Print(Context aContext) {
        super(aContext);
     }

    @Override
    public void setValues() {
        super.setValues();
        putValue(MNEMONIC_KEY, KeyEvent.VK_P);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("control P"));
        putValue(SMALL_ICON, new javax.swing.ImageIcon(getClass().getResource("/images/icons/document-print.png")));
    }

    public PrintUtilities getPrintUtilities () {
        if (printUtilities == null) {
            printUtilities = new PrintUtilities(context.getJImagePane().getJImage());
        }

        return printUtilities;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            getPrintUtilities().print();
        } catch (PrinterException ex) {
            context.getReporter().report(ex);
        }
    }
}

