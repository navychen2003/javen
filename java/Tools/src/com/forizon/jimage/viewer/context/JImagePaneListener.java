package com.forizon.jimage.viewer.context;

import com.forizon.jimage.JImage;
import com.forizon.jimage.JImagePane;
import com.forizon.jimage.viewer.imagelist.AbstractIteratorListener;
import com.forizon.jimage.viewer.imagelist.ImageException;
import com.forizon.jimage.viewer.imagelist.ImageIdentity;
import com.forizon.jimage.viewer.imagelist.ImageIdentityListModelIterator;
import java.awt.Image;

class JImagePaneListener extends AbstractIteratorListener {
    Context context;
    String title; 

    public JImagePaneListener(Context context) {
        this.context = context;
        this.title = context.getWindow().getTitle(); 
    }

    @Override
    public void nextChange(ImageIdentityListModelIterator.NextChangeEvent e) {
        JImagePane jImagePane = context.getJImagePane();
        JImage jImage = jImagePane.getJImage();
        jImage.setImage((Image)null);
        jImagePane.getJImageHandle().reset();
        @SuppressWarnings("rawtypes")
		ImageIdentity imageIdentity = context.getImageContext().getImageIdentity();
        if (imageIdentity != null) {
            try {
                context.getWindow().setTitle(title+" - "+imageIdentity.getName()); 
                jImage.setImage(imageIdentity.getImage());
            } catch (ImageException ex) {
                context.reporter.report(ex);
            }
        }
    }
}

