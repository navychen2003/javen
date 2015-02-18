package org.javenstudio.android.data.media;

import java.util.concurrent.atomic.AtomicBoolean;

import android.net.Uri;

import org.javenstudio.android.data.DataApp;
import org.javenstudio.cocoka.data.ChangeNotifier;

public class MediaNotifier implements ChangeNotifier {

    @SuppressWarnings("unused")
	private MediaSet mMediaSet;
    private AtomicBoolean mContentDirty = new AtomicBoolean(true);

    public MediaNotifier(DataApp app, MediaSet set, Uri uri) {
        mMediaSet = set;
        app.getDataManager().registerChangeNotifier(uri, this);
    }

    public MediaNotifier(DataApp app, MediaSet set, Uri[] uris) {
        mMediaSet = set;
        for (int i = 0; i < uris.length; i++) {
            app.getDataManager().registerChangeNotifier(uris[i], this);
        }
    }

    // Returns the dirty flag and clear it.
    public boolean isDirty() {
        return mContentDirty.compareAndSet(true, false);
    }

    public void fakeChange() {
        onChange(false);
    }

    @Override
    public void onChange(boolean selfChange) {
        if (mContentDirty.compareAndSet(false, true)) {
            //mMediaSet.notifyContentChanged();
        }
    }
    
}
