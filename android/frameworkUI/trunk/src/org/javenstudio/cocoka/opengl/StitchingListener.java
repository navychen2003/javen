package org.javenstudio.cocoka.opengl;

import android.net.Uri;

public interface StitchingListener {
    public void onStitchingQueued(Uri uri);
    public void onStitchingResult(Uri uri);
    public void onStitchingProgress(Uri uri, int progress);
}
