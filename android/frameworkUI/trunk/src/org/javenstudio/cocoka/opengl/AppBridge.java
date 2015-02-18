package org.javenstudio.cocoka.opengl;

import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;

// This is the bridge to connect a PhotoPage to the external environment.
public abstract class AppBridge implements Parcelable {
	
    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {}

    //////////////////////////////////////////////////////////////////////////
    //  These are requests sent from PhotoPage to the app
    //////////////////////////////////////////////////////////////////////////

    public abstract boolean isStaticCamera();
    public abstract ScreenNail attachScreenNail();
    public abstract void detachScreenNail();

    // Return true if the tap is consumed.
    public abstract boolean onSingleTapUp(int x, int y);

    // This is used to notify that the screen nail will be drawn in full screen
    // or not in next draw() call.
    public abstract void onFullScreenChanged(boolean full);

    //////////////////////////////////////////////////////////////////////////
    //  These are requests send from app to PhotoPage
    //////////////////////////////////////////////////////////////////////////

    public interface Server {
        // Set the camera frame relative to GLRootView.
        public void setCameraRelativeFrame(Rect frame);
        // Switch to the previous or next picture using the capture animation.
        // The offset is -1 to switch to the previous picture, 1 to switch to
        // the next picture.
        public boolean switchWithCaptureAnimation(int offset);
        // Enable or disable the swiping gestures (the default is enabled).
        public void setSwipingEnabled(boolean enabled);
        // Notify that the ScreenNail is changed.
        public void notifyScreenNailChanged();
        // Add a new media item to the secure album.
        public void addSecureAlbumItem(boolean isVideo, int id);
    }

    // If server is null, the services are not available.
    public abstract void setServer(Server server);
    
}
