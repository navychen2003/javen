package com.forizon.jimage.viewer.actions;

import com.forizon.jimage.viewer.context.Context;
import javax.swing.SwingUtilities;

/** Parent class for all JImageView actions */
abstract public class AbstractAction extends javax.swing.AbstractAction {
	private static final long serialVersionUID = 1L;
	final protected Context context;

    public AbstractAction(Context aContext) {
        context = aContext;
        SwingUtilities.invokeLater(new Runnable());
    }

    public void setValues() {
        putValue(NAME, context.getLanguageSupport().localizeString(this.getClass().getName() + ".text"));
    }

    public Context getContext() {
        return context;
    }

    class Runnable implements java.lang.Runnable {
        @Override
        public void run() {
            setValues();
        }
    }
}

