package com.forizon.jimage.viewer.actions;

import com.forizon.jimage.viewer.context.Context;
import com.forizon.jimage.viewer.imagelist.AbstractIteratorListener;
import com.forizon.jimage.viewer.imagelist.ImageIdentityListModelIterator.NextChangeEvent;
import com.forizon.jimage.viewer.imagelist.IteratorListener;
import java.awt.event.ActionEvent;

abstract public class AbstractToggleableAction
        extends AbstractAction implements ToggleableAction {
	private static final long serialVersionUID = 1L;
	
	IteratorListener iteratorListener;

    public AbstractToggleableAction(Context aContext) {
        super(aContext);
    }

    @Override
    public void setValues() {
        super.setValues();
        putValue(SELECTED_KEY, Boolean.FALSE);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e == null) {
            putValue(SELECTED_KEY, !getSelected());
        }
        setSelected(getSelected());
    }

    @Override
    public boolean getSelected() {
        return (Boolean)getValue(SELECTED_KEY);
    }

    @Override
    public void setSelected(boolean selected) {
        putValue(SELECTED_KEY, selected);
        if (selected) {
            iteratorListener = new DefaultIteratorListener();
            context.getImageContext().getIterator().addIteratorListener(iteratorListener);
        } else {
            context.getImageContext().getIterator().removeIteratorListener(iteratorListener);
        }
    }

    public void onImageChange() {
        setSelected(false);
    }

    class DefaultIteratorListener extends AbstractIteratorListener {
        @Override
        public void nextChange(NextChangeEvent e) {
            onImageChange();
        }
    }
}

