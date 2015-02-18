package com.forizon.jimage.viewer.context;

import com.forizon.jimage.viewer.imagelist.ImageIdentityListModelIterator;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

class LoopPropertyChangeListener implements PropertyChangeListener {
    final ImageIdentityListModelIterator imageIterator;

    public LoopPropertyChangeListener(ImageIdentityListModelIterator imageIterator) {
        this.imageIterator = imageIterator;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        Boolean value = Boolean.parseBoolean((String)evt.getNewValue());
        imageIterator.setLooping(value);
    }
}
