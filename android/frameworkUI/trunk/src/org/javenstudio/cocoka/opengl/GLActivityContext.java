package org.javenstudio.cocoka.opengl;

import android.content.Context;
import android.content.res.Resources;
import android.os.Looper;

public interface GLActivityContext {

    public Context getActivityContext();
    public Looper getMainLooper();
    public Resources getResources();
	
}
