package com.forizon.jimage.viewer.imagelist;

public abstract class AbstractIteratorListener implements IteratorListener {
    @Override
    public void positionChange(ImageIdentityListModelIterator.PositionChangeEvent e){}
    @Override
    public void nextChange(ImageIdentityListModelIterator.NextChangeEvent e){}
    @Override
    public void previousChange(ImageIdentityListModelIterator.PreviousChangeEvent e){}
}
