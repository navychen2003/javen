package com.forizon.jimage.viewer.actions;

import com.forizon.jimage.viewer.context.Context;
import com.forizon.jimage.viewer.imagelist.AbstractIteratorListener;
import com.forizon.jimage.viewer.imagelist.ImageIdentityListModelIterator;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.NoSuchElementException;
import javax.swing.KeyStroke;

public class Next extends AbstractAction {
	private static final long serialVersionUID = 1L;

	public Next(Context aContext) {
        super(aContext);
        enabled = false;
        context.getImageContext().getIterator().addIteratorListener(new NextPropertyChangeHandler());
        //context.getImageContext().getImageIdentityListModel().addListDataListener(handler);
        //context.getConfiguration().addPropertyChangeListener(Configuration.PROPERTIES_LOOP, handler);
    }

    @Override
    public void setValues() {
        super.setValues();
        putValue(MNEMONIC_KEY, KeyEvent.VK_N);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("K"));
        //putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("RIGHT"));
        putValue(SMALL_ICON, new javax.swing.ImageIcon(getClass().getResource("/images/icons/go-next.png")));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            ImageIdentityListModelIterator iterator = context.getImageContext()
                    .getIterator();
            if (iterator.hasNext(1)) {
                iterator.next();
            }
        } catch (NoSuchElementException ex) {
            context.getReporter().report(ex);
        }
    }

    class NextPropertyChangeHandler
        extends AbstractIteratorListener
    {
        void update() {
            setEnabled(context.getImageContext().getIterator().hasNext());
        }

        @Override
        public void nextChange(ImageIdentityListModelIterator.NextChangeEvent e) {
            update();
        }
    }
}
