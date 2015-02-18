package org.javenstudio.cocoka.opengl;

import android.os.Handler;
import android.os.Message;

import org.javenstudio.cocoka.util.Utils;

public class SynchronizedHandler extends Handler {

    private final GLRoot mRoot;

    public SynchronizedHandler(GLRoot root) {
        mRoot = Utils.checkNotNull(root);
    }

    @Override
    public void dispatchMessage(Message message) {
        mRoot.lockRenderThread();
        try {
            super.dispatchMessage(message);
        } finally {
            mRoot.unlockRenderThread();
        }
    }
}
