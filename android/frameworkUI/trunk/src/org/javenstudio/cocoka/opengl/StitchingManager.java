package org.javenstudio.cocoka.opengl;

import android.net.Uri;

public interface StitchingManager {
	
    public void addChangeListener(StitchingListener l);
    public void removeChangeListener(StitchingListener l);

    public Integer getProgress(Uri uri);
    
}
