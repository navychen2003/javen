package com.forizon.jimage.viewer.context;

import com.forizon.jimage.URIDropEvent;
import com.forizon.jimage.viewer.imagelist.wrapspi.WrapException;

class URIDropListener implements com.forizon.jimage.URIDropListener {
    final Context context;

    public URIDropListener(Context context) {
        this.context = context;
    }

    public void drop(URIDropEvent dropTargetDropEvent) {
        try {
            context.getImageContext().setImage(dropTargetDropEvent.getURI());
        } catch (WrapException ex) {
            context.reporter.report(ex);
        }
    }
}
