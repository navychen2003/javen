package com.forizon.jimage.viewer.actions;

import com.forizon.jimage.viewer.context.Context;
import com.forizon.jimage.viewer.imagelist.AbstractIteratorListener;
import com.forizon.jimage.viewer.imagelist.ImageIdentityListModelIterator;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.NoSuchElementException;
import javax.swing.KeyStroke;

public class Previous extends AbstractAction {
	private static final long serialVersionUID = 1L;

	public Previous(Context aContext) {
        super(aContext);
        enabled = false;
        context.getImageContext().getIterator().addIteratorListener(new PreviousPropertyChangeHandler());
    }

    @Override
    public void setValues() {
        putValue(MNEMONIC_KEY, KeyEvent.VK_R);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("J"));
        //putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("LEFT"));
        putValue(SMALL_ICON, new javax.swing.ImageIcon(getClass().getResource("/images/icons/go-previous.png")));
        super.setValues();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            ImageIdentityListModelIterator iterator = context.getImageContext().getIterator();
            if (iterator.hasPrevious(1)) {
                iterator.previous();
            }
        } catch (NoSuchElementException ex) {
            context.getReporter().report(ex);
        }
    }

    class PreviousPropertyChangeHandler
        extends AbstractIteratorListener
    {
        void update() {
            setEnabled(context.getImageContext().getIterator().hasPrevious());
        }

        @Override
        public void previousChange(ImageIdentityListModelIterator.PreviousChangeEvent e) {
            update();
        }
    }
}
