package com.forizon.jimage.viewer.imagelist;

import java.util.EventListener;

public interface IteratorListener extends EventListener {
    public void positionChange(ImageIdentityListModelIterator.PositionChangeEvent e);
    public void nextChange(ImageIdentityListModelIterator.NextChangeEvent e);
    public void previousChange(ImageIdentityListModelIterator.PreviousChangeEvent e);
}
