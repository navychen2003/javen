package org.javenstudio.cocoka.view;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;

import org.javenstudio.cocoka.app.IActionBar;
import org.javenstudio.cocoka.app.IMenu;
import org.javenstudio.cocoka.app.IMenuItem;
import org.javenstudio.cocoka.app.R;
import org.javenstudio.cocoka.data.FilterDeleteSet;
import org.javenstudio.cocoka.data.IMediaItem;
import org.javenstudio.cocoka.data.IMediaObject;
import org.javenstudio.cocoka.data.IMediaSet;
import org.javenstudio.cocoka.data.SnailItem;
import org.javenstudio.cocoka.opengl.AppBridge;
import org.javenstudio.cocoka.opengl.BottomControls;
import org.javenstudio.cocoka.opengl.GLActionBar;
import org.javenstudio.cocoka.opengl.GLActivityState;
import org.javenstudio.cocoka.opengl.GLView;
import org.javenstudio.cocoka.opengl.OrientationManager;
import org.javenstudio.cocoka.opengl.StringTexture;
import org.javenstudio.cocoka.opengl.SynchronizedHandler;
import org.javenstudio.cocoka.opengl.TransitionStore;
import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.common.util.Logger;

public class PhotoPage extends GLActivityState implements PhotoView.Listener, 
		BottomControls.Delegate, AppBridge.Server {
    private static final Logger LOG = Logger.getLogger(PhotoPage.class);

    private static final int MSG_HIDE_BARS = 1;
    private static final int MSG_ON_FULL_SCREEN_CHANGED = 4;
    private static final int MSG_UPDATE_ACTION_BAR = 5;
    private static final int MSG_UNFREEZE_GLROOT = 6;
    private static final int MSG_WANT_BARS = 7;
    private static final int MSG_REFRESH_BOTTOM_CONTROLS = 8;
    private static final int MSG_ON_CAMERA_CENTER = 9;
    private static final int MSG_ON_PICTURE_CENTER = 10;
    private static final int MSG_REFRESH_IMAGE = 11;
    private static final int MSG_UPDATE_PHOTO_UI = 12;
    private static final int MSG_UPDATE_PROGRESS = 13;
    private static final int MSG_UPDATE_DEFERRED = 14;
    private static final int MSG_UPDATE_SHARE_URI = 15;

    private static final int HIDE_BARS_TIMEOUT = 3500;
    private static final int UNFREEZE_GLROOT_TIMEOUT = 250;

    private static final int REQUEST_SLIDESHOW = 1;
    private static final int REQUEST_CROP = 2;
    private static final int REQUEST_CROP_PICASA = 3;
    private static final int REQUEST_EDIT = 4;
    //private static final int REQUEST_PLAY_VIDEO = 5;
    //private static final int REQUEST_TRIM = 6;

    public static final String KEY_MEDIA_SET_PATH = "media-set-path";
    public static final String KEY_MEDIA_ITEM_PATH = "media-item-path"; 
    public static final String KEY_INDEX_HINT = "index-hint";
    public static final String KEY_OPEN_ANIMATION_RECT = "open-animation-rect";
    public static final String KEY_APP_BRIDGE = "app-bridge";
    public static final String KEY_TREAT_BACK_AS_UP = "treat-back-as-up";
    public static final String KEY_START_IN_FILMSTRIP = "start-in-filmstrip";
    public static final String KEY_RETURN_INDEX_HINT = "return-index-hint";
    public static final String KEY_SHOW_WHEN_LOCKED = "show_when_locked";
    public static final String KEY_IN_CAMERA_ROLL = "in_camera_roll";

    public static final String KEY_ALBUMPAGE_TRANSITION = "albumpage-transition";
    public static final int MSG_ALBUMPAGE_NONE = 0;
    public static final int MSG_ALBUMPAGE_STARTED = 1;
    public static final int MSG_ALBUMPAGE_RESUMED = 2;
    public static final int MSG_ALBUMPAGE_PICKED = 4;

    public static final String ACTION_NEXTGEN_EDIT = "action_nextgen_edit";

    private SelectionManager mSelectionManager;

    protected PhotoView mPhotoView;
    private PhotoPage.Model mModel;
    //private DetailsHelper mDetailsHelper;
    private boolean mShowDetails;

    // mMediaSet could be null if there is no KEY_MEDIA_SET_PATH supplied.
    // E.g., viewing a photo in gmail attachment
    private FilterDeleteSet mMediaSet = null;

    private int mCurrentIndex = 0;
    private Handler mHandler;
    private boolean mShowBars = true;
    private volatile boolean mActionBarAllowed = true;
    private GLActionBar mActionBar;
    private boolean mIsMenuVisible = true;
    private boolean mHaveImageEditor;
    private PhotoBottomControls mBottomControls;
    //private PhotoPageProgressBar mProgressBar;
    private IMediaItem mCurrentPhoto = null;
    private MenuExecutor mMenuExecutor;
    private boolean mIsActive;

    private AppBridge mAppBridge;
    private SnailItem mScreenNailItem;
    //private SnailAlbum mScreenNailSet;
    private OrientationManager mOrientationManager;
    private boolean mTreatBackAsUp;
    private boolean mStartInFilmstrip;
    private boolean mHasCameraScreennailOrPlaceholder = false;
    private boolean mRecenterCameraOnResume = true;

    private long mCameraSwitchCutoff = 0;
    private boolean mSkipUpdateCurrentPhoto = false;
    private static final long CAMERA_SWITCH_CUTOFF_THRESHOLD_MS = 300;

    private static final long DEFERRED_UPDATE_MS = 250;
    private boolean mDeferredUpdateWaiting = false;
    private long mDeferUpdateUntil = Long.MAX_VALUE;

    // The item that is deleted (but it can still be undeleted before commiting)
    private IMediaItem mDeleteItem = null;
    private boolean mDeleteIsFocus;  // whether the deleted item was in focus

    private final IActionBar.OnMenuVisibilityListener mMenuVisibilityListener =
    	new IActionBar.OnMenuVisibilityListener() { 
	    	@Override
	        public void onMenuVisibilityChanged(boolean isVisible) {
	            //mIsMenuVisible = isVisible;
	            refreshHidingMessage();
	        }
	    };
    
    public static interface Model extends PhotoView.Model {
        public void resume();
        public void pause();
        public boolean isEmpty();
        public void setCurrentPhoto(IMediaItem item, int indexHint);
    }

    @Override
    protected int getBackgroundColorId() {
        return R.color.photo_background;
    }

    private final GLView mRootPane = new GLView() {
	        @Override
	        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
	            mPhotoView.layout(0, 0, right - left, bottom - top);
	            //if (mShowDetails) 
	            //    mDetailsHelper.layout(left, mActionBar.getHeight(), right, bottom);
	        }
	    };

    protected IMediaObject getMediaObject() { return null; }
    
    protected StringTexture createNoThumbnailText(BitmapHolder holder) {
    	return null;
    	//return StringTexture.newInstance(holder,
    	//		mActivity.getString(R.string.no_thumbnail),
        //        PhotoView.DEFAULT_TEXT_SIZE, Color.LTGRAY);
    }
    
    @Override
    public void onCreate(Bundle data, Bundle restoreState) {
        super.onCreate(data, restoreState);
        mActionBar = mActivity.getGLActionBar();
        mSelectionManager = new SelectionManager(mActivity, false);
        mMenuExecutor = new MenuExecutor(mActivity, this, mSelectionManager);

        mPhotoView = new PhotoView(mActivity, this, createNoThumbnailText(this));
        mPhotoView.setListener(this);
        mRootPane.addComponent(mPhotoView);
        mOrientationManager = mActivity.getOrientationManager();
        mActivity.getGLRoot().setOrientationSource(mOrientationManager);

        mHandler = new SynchronizedHandler(mActivity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
            	if (LOG.isDebugEnabled())
            		LOG.debug("handleMessage: what=" + message.what);
            	
                switch (message.what) {
                    case MSG_HIDE_BARS: {
                        hideBars();
                        break;
                    }
                    case MSG_REFRESH_BOTTOM_CONTROLS: {
                        if (mCurrentPhoto == message.obj && mBottomControls != null) 
                            mBottomControls.refresh(mCurrentPhoto);
                        break;
                    }
                    case MSG_ON_FULL_SCREEN_CHANGED: {
                        if (mAppBridge != null) 
                            mAppBridge.onFullScreenChanged(message.arg1 == 1);
                        break;
                    }
                    case MSG_UPDATE_ACTION_BAR: {
                        updateBars();
                        break;
                    }
                    case MSG_WANT_BARS: {
                        wantBars();
                        break;
                    }
                    case MSG_UNFREEZE_GLROOT: {
                        mActivity.getGLRoot().unfreeze();
                        break;
                    }
                    case MSG_UPDATE_DEFERRED: {
                        long nextUpdate = mDeferUpdateUntil - SystemClock.uptimeMillis();
                        if (nextUpdate <= 0) {
                            mDeferredUpdateWaiting = false;
                            updateUIForCurrentPhoto();
                        } else {
                            mHandler.sendEmptyMessageDelayed(MSG_UPDATE_DEFERRED, nextUpdate);
                        }
                        break;
                    }
                    case MSG_ON_CAMERA_CENTER: {
                        mSkipUpdateCurrentPhoto = false;
                        boolean stayedOnCamera = false;
                        if (!mPhotoView.getFilmMode()) {
                            stayedOnCamera = true;
                        } else if (SystemClock.uptimeMillis() < mCameraSwitchCutoff &&
                        		mMediaSet != null && mMediaSet.getItemCount() > 1) {
                            mPhotoView.switchToImage(1);
                        } else {
                            if (mAppBridge != null) mPhotoView.setFilmMode(false);
                            stayedOnCamera = true;
                        }

                        if (stayedOnCamera) {
                            if (mAppBridge == null) {
                                launchCamera();
                                // We got here by swiping from photo 1 to the
                                //   placeholder, so make it be the thing that
                                //   is in focus when the user presses back from
                                //   the camera app 
                                mPhotoView.switchToImage(1);
                            } else {
                                updateBars();
                                updateCurrentPhoto(mModel.getMediaItem(0));
                            }
                        }
                        break;
                    }
                    case MSG_ON_PICTURE_CENTER: {
                        if (!mPhotoView.getFilmMode() && mCurrentPhoto != null && 
                        		(mCurrentPhoto.getSupportedOperations() & IMediaObject.SUPPORT_ACTION) != 0) {
                            mPhotoView.setFilmMode(true);
                        }
                        break;
                    }
                    case MSG_REFRESH_IMAGE: {
                        final IMediaItem photo = mCurrentPhoto;
                        mCurrentPhoto = null;
                        updateCurrentPhoto(photo);
                        break;
                    }
                    case MSG_UPDATE_PHOTO_UI: {
                        updateUIForCurrentPhoto();
                        break;
                    }
                    case MSG_UPDATE_PROGRESS: {
                        updateProgressBar();
                        break;
                    }
                    case MSG_UPDATE_SHARE_URI: {
                    	if (mActionBar != null && mActionBar instanceof GLPhotoActionBar) {
	                        if (mCurrentPhoto != null && mCurrentPhoto == message.obj) {
	                            Intent shareIntent = mCurrentPhoto.getShareIntent();
	                            ((GLPhotoActionBar)mActionBar).setShareIntent(shareIntent);
	                        } else { 
	                        	((GLPhotoActionBar)mActionBar).setShareIntent(null);
	                        }
                    	}
                        break;
                    }
                    default: 
                    	throw new AssertionError(message.what);
                }
            }
        };

        mTreatBackAsUp = data.getBoolean(KEY_TREAT_BACK_AS_UP, false);
        mStartInFilmstrip = data.getBoolean(KEY_START_IN_FILMSTRIP, false);
        //boolean inCameraRoll = data.getBoolean(KEY_IN_CAMERA_ROLL, false);
        mCurrentIndex = data.getInt(KEY_INDEX_HINT, 0);
        
        IMediaObject mediaObj = getMediaObject();
        if (mediaObj != null && mediaObj instanceof IMediaSet) {
        	IMediaSet mediaSet = (IMediaSet)mediaObj;
            mMediaSet = new FilterDeleteSet(mediaSet);
            mCurrentIndex = mediaSet.getIndexHint();
            
            IMediaItem mediaItem = null;
            if (mediaItem == null) {
                int mediaItemCount = mMediaSet.getItemCount();
                if (mediaItemCount > 0) {
                    if (mCurrentIndex >= mediaItemCount || mCurrentIndex < 0) 
                    	mCurrentIndex = 0;
                    mediaItem = mMediaSet.getItemList(mCurrentIndex, 1).get(0); 
                } else {
                    // Bail out, PhotoPage can't load on an empty album
                	if (LOG.isDebugEnabled())
                		LOG.debug("init: empty mediaSet: " + mediaSet);
                	
                    return;
                }
            }
            
            PhotoAdapter pda = new PhotoAdapter(
                    mActivity, this, mPhotoView, mMediaSet, mediaItem, mCurrentIndex,
                    mAppBridge == null ? -1 : 0,
                    mAppBridge == null ? false : mAppBridge.isStaticCamera());
            
            mModel = pda;
            mPhotoView.setModel(mModel);

            pda.setDataListener(new PhotoAdapter.DataListener() {
                @Override
                public void onPhotoChanged(int index, IMediaItem item) {
                    int oldIndex = mCurrentIndex;
                    mCurrentIndex = index;

                    if (mHasCameraScreennailOrPlaceholder) {
                        if (mCurrentIndex > 0) 
                            mSkipUpdateCurrentPhoto = false;

                        if (oldIndex == 0 && mCurrentIndex > 0 && !mPhotoView.getFilmMode()) {
                            mPhotoView.setFilmMode(true);
                            
                        } else if (oldIndex == 2 && mCurrentIndex == 1) {
                            mCameraSwitchCutoff = SystemClock.uptimeMillis() +
                                    CAMERA_SWITCH_CUTOFF_THRESHOLD_MS;
                            mPhotoView.stopScrolling();
                            
                        } else if (oldIndex >= 1 && mCurrentIndex == 0) {
                            mPhotoView.setWantPictureCenterCallbacks(true);
                            mSkipUpdateCurrentPhoto = true;
                        }
                    }
                    
                    if (!mSkipUpdateCurrentPhoto) {
                        if (item != null) {
                            IMediaItem photo = mModel.getMediaItem(0);
                            if (photo != null) updateCurrentPhoto(photo);
                        }
                        updateBars();
                    }
                    
                    // Reset the timeout for the bars after a swipe
                    refreshHidingMessage();
                }

                @Override
                public void onLoadingFinished(boolean loadingFailed) {
                    if (!mModel.isEmpty()) {
                        IMediaItem photo = mModel.getMediaItem(0);
                        if (photo != null) updateCurrentPhoto(photo);
                        
                    } else if (mIsActive) {
                        // We only want to finish the PhotoPage if there is no
                        // deletion that the user can undo.
                        if (mMediaSet.getNumberOfDeletions() == 0) {
                            mActivity.getGLStateManager().finishState(
                                    PhotoPage.this);
                        }
                    }
                }

                @Override
                public void onLoadingStarted() {
                }
            });
            
        } else if (mediaObj != null && mediaObj instanceof IMediaItem) {
            // Get default media set by the URI
        	IMediaItem mediaItem = (IMediaItem)mediaObj;
            mModel = new SinglePhotoAdapter(mActivity, this, mediaItem);
            mPhotoView.setModel(mModel);
            updateCurrentPhoto(mediaItem);
            
        } else {
        	if (LOG.isDebugEnabled())
        		LOG.debug("init: unknown media: " + mediaObj);
        	
        	return;
        }

        mPhotoView.setFilmMode(mStartInFilmstrip && mMediaSet != null && mMediaSet.getItemCount() > 1);
        
        ViewGroup mainRoot = (ViewGroup)((GLPhotoActivity)mActivity).findGLMainRootView();
        if (mainRoot != null) {
            //if (mSecureAlbum == null) {
                mBottomControls = createBottomControls(mainRoot);
            //}
            //StitchingProgressManager progressManager = mApplication.getStitchingProgressManager();
            //if (progressManager != null) {
            //    mProgressBar = new PhotoPageProgressBar(mActivity, galleryRoot);
            //    mProgressListener = new UpdateProgressListener();
            //    progressManager.addChangeListener(mProgressListener);
            //    if (mSecureAlbum != null) 
            //        progressManager.addChangeListener(mSecureAlbum);
            //}
        }
    }

    protected PhotoBottomControls createBottomControls(ViewGroup layout) { 
    	return new PhotoBottomControls(mActivity, this, layout);
    }
    
    @Override
    public void onPictureCenter(boolean isCamera) {
        isCamera = isCamera || (mHasCameraScreennailOrPlaceholder && mAppBridge == null);
        mPhotoView.setWantPictureCenterCallbacks(false);
        mHandler.removeMessages(MSG_ON_CAMERA_CENTER);
        mHandler.removeMessages(MSG_ON_PICTURE_CENTER);
        mHandler.sendEmptyMessage(isCamera ? MSG_ON_CAMERA_CENTER : MSG_ON_PICTURE_CENTER);
    }

    @Override
    public boolean canDisplayBottomControls() {
    	IMediaItem item = mCurrentPhoto;
        return mIsActive && mShowBars && item != null && 
        		item.getControls().showControls(); 
        		//!mPhotoView.canUndo();
    }

    @Override
    public boolean canDisplayBottomControl(View control) {
        if (mBottomControls == null || mCurrentPhoto == null || control == null) 
            return false;
        
        //if (control.getId() == R.id.photopage_bottom_control_edit) {
        //    return mHaveImageEditor && mShowBars
        //            && !mPhotoView.getFilmMode()
        //            && (mCurrentPhoto.getSupportedOperations() & IMediaItem.SUPPORT_EDIT) != 0
        //            && mCurrentPhoto.getMediaType() == IMediaObject.MEDIA_TYPE_IMAGE;
        //}
        
        //if (control.getId() == R.id.photopage_bottom_control_tiny_planet) {
        //    return mHaveImageEditor && mShowBars && !mPhotoView.getFilmMode();
        //}
        
        if (mBottomControls.isBodyView(control)) 
        	return mShowDetails;
        
        return true;
    }

    @Override
    public boolean isBottomControl(View control) { 
    	return false;
    }
    
    @Override
    public void onBottomControlClicked(View control) {
    	if (mBottomControls == null || control == null) 
    		return;
    	
    	//if (control.getId() == R.id.photopage_bottom_control_edit) {
        //    launchPhotoEditor();
        //    return;
    	//}
    	
    	//if (control.getId() == R.id.photopage_bottom_control_tiny_planet) {
        //    launchTinyPlanet();
        //    return;
    	//}
    	
    	if (mBottomControls.isControlsView(control)) { 
    		toggleDetails();
    		return;
    	}
    }

    public static Intent createShareIntent(IMediaItem mediaObject, 
    		String shareType, String shareText, String shareHtml, Uri shareStream) {
    	if (mediaObject == null) return null;
    	
    	if (LOG.isDebugEnabled())
			LOG.debug("createShareIntent: item=" + mediaObject);
    	
    	//String shareText = mediaObject.getShareText();
    	//String shareHtml = mediaObject.getShareHtml();
    	
    	if (shareText == null || shareText.length() == 0) 
    		shareText = shareHtml;
    	
    	//Uri shareStream = mediaObject.getShareStreamUri();
    	
    	if ((shareText == null || shareText.length() == 0) && shareStream == null) 
			return null;
    	
    	if (shareType == null || shareType.length() == 0)
    		return null;
    	
    	Intent intent = new Intent(Intent.ACTION_SEND)
                .setType(shareType)
                //.putExtra(Intent.EXTRA_TEXT, shareText)
                //.putExtra(Intent.EXTRA_HTML_TEXT, shareHtml)
                //.putExtra(Intent.EXTRA_STREAM, mediaObject.getShareStreamUri())
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    	
		if (shareText != null)
			intent.putExtra(Intent.EXTRA_TEXT, shareText);
		
		if (shareHtml != null)
			intent.putExtra(Intent.EXTRA_HTML_TEXT, shareHtml);
		
		if (shareStream != null)
			intent.putExtra(Intent.EXTRA_STREAM, shareStream);
    	
    	return intent;
    }

    //private void overrideTransitionToEditor() {
    //    ((Activity) mActivity).overridePendingTransition(android.R.anim.slide_in_left,
    //            android.R.anim.fade_out);
    //}

    protected void launchShareTo() { 
    	if (mActionBar != null && mActionBar instanceof GLPhotoActionBar)
    		((GLPhotoActionBar)mActionBar).onActionShareTo(getActivity());
    }
    
    protected void launchSlideshow() {}
    
    @SuppressWarnings("unused")
	private void launchTinyPlanet() {
        // Deep link into tiny planet
        //MediaItem current = mModel.getMediaItem(0);
        //Intent intent = new Intent(FilterShowActivity.TINY_PLANET_ACTION);
        //intent.setClass(mActivity, FilterShowActivity.class);
        //intent.setDataAndType(current.getContentUri(), current.getMimeType())
        //    .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        //intent.putExtra(FilterShowActivity.LAUNCH_FULLSCREEN,
        //        mActivity.isFullscreen());
        //mActivity.startActivityForResult(intent, REQUEST_EDIT);
        //overrideTransitionToEditor();
    }

    private void launchCamera() {
        //Intent intent = new Intent(mActivity, CameraActivity.class)
       //     .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //mRecenterCameraOnResume = false;
        //mActivity.startActivity(intent);
    }

    private void launchPhotoEditor() {
        //MediaItem current = mModel.getMediaItem(0);
        //if (current == null || (current.getSupportedOperations()
        //        & MediaObject.SUPPORT_EDIT) == 0) {
        //    return;
        //}

        Intent intent = new Intent(ACTION_NEXTGEN_EDIT);

        //intent.setDataAndType(current.getContentUri(), current.getMimeType())
        //        .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (mActivity.getPackageManager()
                .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).size() == 0) {
            intent.setAction(Intent.ACTION_EDIT);
        }
        //intent.putExtra(FilterShowActivity.LAUNCH_FULLSCREEN,
        //        mActivity.isFullscreen());
        //((Activity) mActivity).startActivityForResult(Intent.createChooser(intent, null),
         //       REQUEST_EDIT);
        //overrideTransitionToEditor();
    }

    private void requestDeferredUpdate() {
        mDeferUpdateUntil = SystemClock.uptimeMillis() + DEFERRED_UPDATE_MS;
        if (!mDeferredUpdateWaiting) {
            mDeferredUpdateWaiting = true;
            mHandler.sendEmptyMessageDelayed(MSG_UPDATE_DEFERRED, DEFERRED_UPDATE_MS);
        }
    }

    private void updateUIForCurrentPhoto() {
        if (mCurrentPhoto == null) return;

        // If by swiping or deletion the user ends up on an action item
        // and zoomed in, zoom out so that the context of the action is
        // more clear
        if ((mCurrentPhoto.getSupportedOperations() & IMediaObject.SUPPORT_ACTION) != 0 && 
        		!mPhotoView.getFilmMode()) {
            mPhotoView.setWantPictureCenterCallbacks(true);
        }

        updateMenuOperations();
        refreshBottomControlsWhenReady();
        
        //if (mShowDetails) {
        //    //mDetailsHelper.reloadDetails();
        //}
        
        if ((mCurrentPhoto.getSupportedOperations() & IMediaItem.SUPPORT_SHARE) != 0) {
        	mHandler.obtainMessage(MSG_UPDATE_SHARE_URI, 0, 0, mCurrentPhoto)
        			.sendToTarget();
        }
        
        updateProgressBar();
        updateActionBar();
    }

    private void updateCurrentPhoto(IMediaItem photo) {
        if (mCurrentPhoto == photo) return;
        mCurrentPhoto = photo;
        
        if (mPhotoView.getFilmMode()) 
            requestDeferredUpdate();
        else 
            updateUIForCurrentPhoto();
    }

    private void updateActionBar() {
        IMediaItem item = mCurrentPhoto;
        if (item != null) { 
        	String title = item.getControls().getActionTitle();
        	View view = item.getControls().getActionCustomView(mActivity);
        	
        	if (view != null || (title != null && title.length() > 0)) {
        		if (view != null) title = null;
        		
	        	mActionBar.setCustomView(view);
	        	mActionBar.setTitle(title);
	        	mActionBar.setSubtitle(item.getControls().getActionSubTitle());
	        	
	        	if (LOG.isDebugEnabled())
	        		LOG.debug("updateActionBar: title=" + title + " custom=" + view);
        	}
        }
    }
    
    private void updateProgressBar() {
        //if (mProgressBar != null) {
        //    mProgressBar.hideProgress();
        //    StitchingProgressManager progressManager = mApplication.getStitchingProgressManager();
        //    if (progressManager != null && mCurrentPhoto instanceof LocalImage) {
        //        Integer progress = progressManager.getProgress(mCurrentPhoto.getContentUri());
        //        if (progress != null) {
        //            mProgressBar.setProgress(progress);
        //        }
        //    }
        //}
    }

    private void updateMenuOperations() {
        IMenu menu = mActionBar.getMenu();
        if (LOG.isDebugEnabled()) LOG.debug("updateMenuOperations: menu=" + menu);

        // it could be null if onCreateActionBar has not been called yet
        if (menu == null) return;

        IMenuItem item = menu.findItem(R.id.photo_action_slideshow);
        if (item != null) 
            item.setVisible(canDoSlideShow());
        
        if (mCurrentPhoto == null) return;
        int supportedOperations = mCurrentPhoto.getSupportedOperations();
        //if (mSecureAlbum != null) {
        //    supportedOperations &= MediaObject.SUPPORT_DELETE;
        //} else if (!mHaveImageEditor) {
         //   supportedOperations &= ~MediaObject.SUPPORT_EDIT;
        //}
        
        MenuExecutor.updateMenuOperation(menu, supportedOperations);
    }

	private boolean canDoSlideShow() {
        if (mMediaSet == null || mCurrentPhoto == null) 
            return false;
        
        if (mCurrentPhoto.getMediaType() != IMediaObject.MEDIA_TYPE_IMAGE) 
            return false;
        
        return true;
    }

    //////////////////////////////////////////////////////////////////////////
    //  Action Bar show/hide management
    //////////////////////////////////////////////////////////////////////////

    private void showBars() {
        if (mShowBars) return;
        mShowBars = true;
        mActionBar.show();
        mOrientationManager.unlockOrientation();
        mActivity.getGLRoot().setLightsOutMode(false);
        refreshHidingMessage();
        refreshBottomControlsWhenReady();
    }

    private void hideBars() {
        if (!mShowBars) return;
        mShowBars = false;
        mActionBar.hide();
        mActivity.getGLRoot().setLightsOutMode(true);
        mHandler.removeMessages(MSG_HIDE_BARS);
        refreshBottomControlsWhenReady();
    }

    private void refreshHidingMessage() {
        mHandler.removeMessages(MSG_HIDE_BARS);
        if (!mIsMenuVisible && !mPhotoView.getFilmMode()) {
            mHandler.sendEmptyMessageDelayed(MSG_HIDE_BARS, HIDE_BARS_TIMEOUT);
        }
    }

    private boolean canShowBars() {
        // No bars if we are showing camera preview.
        if (mAppBridge != null && mCurrentIndex == 0 && !mPhotoView.getFilmMode()) 
        	return false;

        // No bars if it's not allowed.
        if (!mActionBarAllowed) return false;

        return true;
    }

    private void wantBars() {
        if (canShowBars()) showBars();
    }

    private void toggleBars() {
        if (mShowBars) {
            hideBars();
        } else if (canShowBars()) {
        	showBars();
    	}
    }

    private void updateBars() {
        if (!canShowBars()) hideBars();
    }

    @Override
    protected void onBackPressed() {
        if (mShowDetails) {
            hideDetails();
            return;
        }
        
        if (mAppBridge == null || !switchWithCaptureAnimation(-1)) {
            // We are leaving this page. Set the result now.
            setResult();
            
            if (mStartInFilmstrip && !mPhotoView.getFilmMode()) {
                mPhotoView.setFilmMode(true);
            } else if (mTreatBackAsUp) {
                onUpPressed();
            } else {
                super.onBackPressed();
            }
        }
    }

    private void onUpPressed() {
    	if (mShowDetails) {
            hideDetails();
            return;
        }
    	
        if ((mStartInFilmstrip || mAppBridge != null) && !mPhotoView.getFilmMode()) {
            mPhotoView.setFilmMode(true);
            return;
        }

        if (mActivity.getGLStateManager().getStateCount() > 1) {
            setResult();
            super.onBackPressed();
            return;
        }

        //if (mAppBridge == null) {
        //    // We're in view mode so set up the stacks on our own.
        //    Bundle data = new Bundle(getData());
        //    data.putString(AlbumPage.KEY_MEDIA_PATH, mOriginalSetPathString);
        //    data.putString(AlbumPage.KEY_PARENT_MEDIA_PATH, getTopSetPath(INCLUDE_ALL));
        //    mActivity.getStateManager().switchState(this, AlbumPage.class, data);
        //} else {
        //    GalleryUtils.startGalleryActivity(mActivity);
        //}
        
        super.onBackPressed();
    }

    private void setResult() {
        Intent result = null;
        result = new Intent();
        result.putExtra(KEY_RETURN_INDEX_HINT, mCurrentIndex);
        setStateResult(Activity.RESULT_OK, result);
    }

    //////////////////////////////////////////////////////////////////////////
    //  AppBridge.Server interface
    //////////////////////////////////////////////////////////////////////////

    @Override
    public void setCameraRelativeFrame(Rect frame) {
        mPhotoView.setCameraRelativeFrame(frame);
    }

    @Override
    public boolean switchWithCaptureAnimation(int offset) {
        return mPhotoView.switchWithCaptureAnimation(offset);
    }

    @Override
    public void setSwipingEnabled(boolean enabled) {
        mPhotoView.setSwipingEnabled(enabled);
    }

    @Override
    public void notifyScreenNailChanged() {
        //mScreenNailItem.setScreenNail(mAppBridge.attachScreenNail());
        //mScreenNailSet.notifyChange();
    }

    @Override
    public void addSecureAlbumItem(boolean isVideo, int id) {
        //mSecureAlbum.addMediaItem(isVideo, id);
    }

    @Override
    protected boolean onCreateActionBar(IMenu menu) {
    	mActionBar.createActionBarMenu(R.menu.photo_menu, menu);
    	mActionBar.setProgressMenuItem(R.id.photo_action_progress);
    	
    //    mHaveImageEditor = GalleryUtils.isEditorAvailable(mActivity, "image/*");
        updateMenuOperations();
    //    mActionBar.setTitle(mMediaSet != null ? mMediaSet.getName() : "");
    	
        return true;
    }

    @Override
    protected boolean onItemSelected(IMenuItem item) {
        if (mModel == null) return true;
        
        refreshHidingMessage();
        final IMediaItem current = mModel.getMediaItem(0);

        if (current == null) {
            // item is not ready, ignore
            return true;
        }

        //int currentIndex = mModel.getCurrentIndex();
        int action = item.getItemId();
        String confirmMsg = null;
        
        if (LOG.isDebugEnabled())
        	LOG.debug("onItemSelected: action=" + action);
        
        if (action == android.R.id.home) {
        	if (LOG.isDebugEnabled())
            	LOG.debug("onItemSelected: action=home");
        	
            onUpPressed();
            return true;
            
        } else if (action == R.id.photo_action_share) { 
        	if (LOG.isDebugEnabled())
            	LOG.debug("onItemSelected: action=photo_action_share");
        	
        	if (!current.handleOperation(mActivity, IMediaItem.SUPPORT_SHARE)) 
        		;
        	
            return true;
            
        } else if (action == R.id.photo_action_shareto) { 
        	if (LOG.isDebugEnabled())
            	LOG.debug("onItemSelected: action=photo_action_shareto");
        	
        	if (!current.handleOperation(mActivity, IMediaItem.SUPPORT_SHARETO)) 
        		launchShareTo();
        	
            return true;
        	
        } else if (action == R.id.photo_action_download) { 
        	if (LOG.isDebugEnabled())
            	LOG.debug("onItemSelected: action=photo_action_download");
        	
        	if (!current.handleOperation(mActivity, IMediaItem.SUPPORT_DOWNLOAD)) 
        		;
        	
            return true;
            
        } else if (action == R.id.photo_action_slideshow) {
        	if (LOG.isDebugEnabled())
            	LOG.debug("onItemSelected: action=photo_action_slideshow");
        	
            //Bundle data = new Bundle();
            //data.putString(SlideshowPage.KEY_SET_PATH, mMediaSet.getPath().toString());
            //data.putString(SlideshowPage.KEY_ITEM_PATH, path.toString());
            //data.putInt(SlideshowPage.KEY_PHOTO_INDEX, currentIndex);
            //data.putBoolean(SlideshowPage.KEY_REPEAT, true);
            //mActivity.getGLStateManager().startStateForResult(
            //        SlideshowPage.class, REQUEST_SLIDESHOW, data);
        	
        	//if (!current.handleOperation(mActivity, IMediaItem.SUPPORT_SLIDESHOW)) 
        		launchSlideshow();
        	
            return true;
            
        } else if (action == R.id.photo_action_crop) {
        	if (LOG.isDebugEnabled())
            	LOG.debug("onItemSelected: action=photo_action_crop");
        	
            //Activity activity = mActivity;
            //Intent intent = new Intent(FilterShowActivity.CROP_ACTION);
            //intent.setClass(activity, FilterShowActivity.class);
            //intent.setDataAndType(manager.getContentUri(path), current.getMimeType())
            //    .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            //activity.startActivityForResult(intent, PicasaSource.isPicasaImage(current)
            //        ? REQUEST_CROP_PICASA
            //        : REQUEST_CROP);
        	
        	if (!current.handleOperation(mActivity, IMediaItem.SUPPORT_CROP)) 
        		;
        	
            return true;
            
        } else if (action == R.id.photo_action_trim) {
        	if (LOG.isDebugEnabled())
            	LOG.debug("onItemSelected: action=photo_action_trim");
        	
            //Intent intent = new Intent(mActivity, TrimVideo.class);
            //intent.setData(manager.getContentUri(path));
            //// We need the file path to wrap this into a RandomAccessFile.
            //intent.putExtra(KEY_MEDIA_ITEM_PATH, current.getFilePath());
            //mActivity.startActivityForResult(intent, REQUEST_TRIM);
        	
        	if (!current.handleOperation(mActivity, IMediaItem.SUPPORT_TRIM)) 
        		;
        	
            return true;
            
        } else if (action == R.id.photo_action_edit) {
        	if (LOG.isDebugEnabled())
            	LOG.debug("onItemSelected: action=photo_action_edit");
        	
        	if (!current.handleOperation(mActivity, IMediaItem.SUPPORT_EDIT)) 
        		launchPhotoEditor();
        	
            return true;
            
        } else if (action == R.id.photo_action_details) {
        	if (LOG.isDebugEnabled())
            	LOG.debug("onItemSelected: action=photo_action_details");
        	
        	if (!current.handleOperation(mActivity, IMediaItem.SUPPORT_INFO)) 
        		toggleDetails();
        	
            return true;
            
        } else if (action == R.id.photo_action_delete) {
        	if (LOG.isDebugEnabled())
            	LOG.debug("onItemSelected: action=photo_action_delete item=" + current);
        	
        	if (!current.handleOperation(mActivity, IMediaItem.SUPPORT_DELETE)) {
	            confirmMsg = mActivity.getResources().getQuantityString(
	                    R.plurals.delete_photo_message, 1);
	            
	            mSelectionManager.deSelectAll();
	            mSelectionManager.toggle(current);
	            mMenuExecutor.onMenuClicked(item, confirmMsg, mConfirmDialogListener);
        	}
        	
        	return true;
        	
        } else if (action == R.id.photo_action_setas) {
        	if (LOG.isDebugEnabled())
            	LOG.debug("onItemSelected: action=photo_action_setas item=" + current);
        	
        	if (!current.handleOperation(mActivity, IMediaItem.SUPPORT_SETAS)) {
        		mSelectionManager.deSelectAll();
        		mSelectionManager.toggle(current);
        		mMenuExecutor.onMenuClicked(item, confirmMsg, mConfirmDialogListener);
        	}
        	
        	return true;
        	
        } else if (action == R.id.photo_action_rotate_ccw) {
        	//if (!current.handleOperation(mActivity, IMediaItem.SUPPORT_ROTATE_CCW)) 
        	//	;
        	
        	return true;
        	
        } else if (action == R.id.photo_action_rotate_cw) {
        	//if (!current.handleOperation(mActivity, IMediaItem.SUPPORT_ROTATE_CW)) 
        	//	;
        	
        	return true;
        	
        //} else if (action == R.id.photo_action_show_on_map) {
        //    mSelectionManager.deSelectAll();
        //    mSelectionManager.toggle(path);
        //    mMenuExecutor.onMenuClicked(item, confirmMsg, mConfirmDialogListener);
        //    return true;
            
        //} else if (action == R.id.photo_action_import) {
        //    mSelectionManager.deSelectAll();
        //    mSelectionManager.toggle(path);
        //    mMenuExecutor.onMenuClicked(item, confirmMsg,
        //            new ImportCompleteListener(mActivity));
        //    return true;
            
        }
        
        return false;
    }

    private MenuExecutor.ProgressListener mConfirmDialogListener =
            new MenuExecutor.ProgressListener() {
        @Override
        public void onProgressUpdate(int index) {}

        @Override
        public void onProgressComplete(int result) {}

        @Override
        public void onConfirmDialogShown() {
            mHandler.removeMessages(MSG_HIDE_BARS);
        }

        @Override
        public void onConfirmDialogDismissed(boolean confirmed) {
            refreshHidingMessage();
        }

        @Override
        public void onProgressStart() {}
        
        @Override
        public void onExceptionCatched(Throwable e) { 
        	if (e == null) return;
        	
        	if (LOG.isDebugEnabled())
        		LOG.debug("onExceptionCached: " + e);
        	
        	((GLPhotoActivity)getActivity()).onExceptionCatched(e);
        }
    };
    
    private void toggleDetails() { 
    	if (mShowDetails) 
            hideDetails();
        else 
            showDetails();
    }
    
    private void hideDetails() {
        mShowDetails = false;
        //mDetailsHelper.hide();
        
        if (LOG.isDebugEnabled())
        	LOG.debug("hideDetails");
        
        refreshBottomControlsWhenReady();
    }

	private void showDetails() {
        mShowDetails = true;
        //if (mDetailsHelper == null) {
        //    mDetailsHelper = new DetailsHelper(mActivity, mRootPane, new MyDetailsSource());
        //    mDetailsHelper.setCloseListener(new CloseListener() {
        //        @Override
        //        public void onClose() {
        //            hideDetails();
        //        }
        //    });
        //}
        //mDetailsHelper.show();
        
        if (LOG.isDebugEnabled())
        	LOG.debug("showDetails");
        
        refreshBottomControlsWhenReady();
    }

    ////////////////////////////////////////////////////////////////////////////
    //  Callbacks from PhotoView
    ////////////////////////////////////////////////////////////////////////////
    @Override
    public void onSingleTapUp(int x, int y) {
        if (mAppBridge != null) {
            if (mAppBridge.onSingleTapUp(x, y)) return;
        }

        IMediaItem item = mModel.getMediaItem(0);
        if (item == null || item == mScreenNailItem) {
            // item is not ready or it is camera preview, ignore
            return;
        }

        int supported = item.getSupportedOperations();
        boolean playVideo = ((supported & IMediaItem.SUPPORT_PLAY) != 0);
        boolean unlock = ((supported & IMediaItem.SUPPORT_UNLOCK) != 0);
        boolean goBack = ((supported & IMediaItem.SUPPORT_BACK) != 0);
        boolean launchCamera = ((supported & IMediaItem.SUPPORT_CAMERA_SHORTCUT) != 0);

        if (playVideo) {
            // determine if the point is at center (1/6) of the photo view.
            // (The position of the "play" icon is at center (1/6) of the photo)
            int w = mPhotoView.getWidth();
            int h = mPhotoView.getHeight();
            playVideo = (Math.abs(x - w / 2) * 12 <= w) && (Math.abs(y - h / 2) * 12 <= h);
        }

        if (playVideo) {
            //if (mSecureAlbum == null) {
            //    playVideo(mActivity, item.getPlayUri(), item.getName());
            //} else {
            //    mActivity.getStateManager().finishState(this);
            //}
        	
        } else if (goBack) {
            //onBackPressed();
            
        } else if (unlock) {
            //Intent intent = new Intent(mActivity, Gallery.class);
            //intent.putExtra(Gallery.KEY_DISMISS_KEYGUARD, true);
            //mActivity.startActivity(intent);
        	
        } else if (launchCamera) {
            launchCamera();
            
        } else if (!mShowDetails) {
            toggleBars();
        }
    }

    @Override
    public void onActionBarAllowed(boolean allowed) {
        mActionBarAllowed = allowed;
        mHandler.sendEmptyMessage(MSG_UPDATE_ACTION_BAR);
    }

    @Override
    public void onActionBarWanted() {
        mHandler.sendEmptyMessage(MSG_WANT_BARS);
    }

    @Override
    public void onFullScreenChanged(boolean full) {
        Message m = mHandler.obtainMessage(
                MSG_ON_FULL_SCREEN_CHANGED, full ? 1 : 0, 0);
        m.sendToTarget();
    }

    // How we do delete/undo:
    //
    // When the user choose to delete a media item, we just tell the
    // FilterDeleteSet to hide that item. If the user choose to undo it, we
    // again tell FilterDeleteSet not to hide it. If the user choose to commit
    // the deletion, we then actually delete the media item.
    @Override
    public void onDeleteImage(IMediaItem item, int offset) {
        onCommitDeleteImage();  // commit the previous deletion
        mDeleteItem = item;
        mDeleteIsFocus = (offset == 0);
        if (mMediaSet != null) mMediaSet.addDeletion(item, mCurrentIndex + offset);
    }
    
    void onDeleteItem(IMediaItem item) { 
    	final IMediaItem current = mModel.getMediaItem(0);
    	if (item == current)
    		onDeleteItem(item, 0);
    }
    
    private void onDeleteItem(IMediaItem item, int offset) {
    	if (item == null) return;
    	
    	if (LOG.isDebugEnabled()) {
    		LOG.debug("onDeleteItem: item=" + item + " offset=" + offset 
    				+ " currentIndex=" + mCurrentIndex);
    	}
    	
        mDeleteItem = item;
        mDeleteIsFocus = (offset == 0);
        if (mMediaSet != null) 
        	mMediaSet.addDeletion(item, mCurrentIndex + offset);
        else
        	mActivity.finish();
    }

    @Override
    public void onUndoDeleteImage() {
        if (mDeleteItem == null) return;
        // If the deletion was done on the focused item, we want the model to
        // focus on it when it is undeleted.
        if (mDeleteIsFocus) mModel.setFocusHintItem(mDeleteItem);
        if (mMediaSet != null) mMediaSet.removeDeletion(mDeleteItem);
        mDeleteItem = null;
    }

    @Override
    public void onCommitDeleteImage() {
        if (mDeleteItem == null) return;
        mSelectionManager.deSelectAll();
        mSelectionManager.toggle(mDeleteItem);
        mMenuExecutor.onMenuClicked(R.id.photo_action_delete, null, true, false);
        mDeleteItem = null;
    }

    public void playVideo(Activity activity, Uri uri, String title) {
        try {
            //Intent intent = new Intent(Intent.ACTION_VIEW)
            //        .setDataAndType(uri, "video/*")
            //        .putExtra(Intent.EXTRA_TITLE, title)
            //        .putExtra(MovieActivity.KEY_TREAT_UP_AS_BACK, true);
            //activity.startActivityForResult(intent, REQUEST_PLAY_VIDEO);
        } catch (ActivityNotFoundException e) {
            //Toast.makeText(activity, activity.getString(R.string.video_err),
            //        Toast.LENGTH_SHORT).show();
        }
    }

    private void setCurrentPhotoByIntent(Intent intent) {
        if (intent == null) return;
        //Path path = null;
        //if (path != null) {
        //    Path albumPath = getDefaultSetOf(path);
        //    if (!albumPath.equalsIgnoreCase(mOriginalSetPathString)) {
        //        // If the edited image is stored in a different album, we need
        //        // to start a new activity state to show the new image
        //        Bundle data = new Bundle(getData());
        //        data.putString(KEY_MEDIA_SET_PATH, albumPath.toString());
        //        data.putString(PhotoPage.KEY_MEDIA_ITEM_PATH, path.toString());
        //        mActivity.getStateManager().startState(PhotoPage.class, data);
        //        return;
        //    }
        //    mModel.setCurrentPhoto(path, mCurrentIndex);
        //}
    }

    @Override
    protected void onStateResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_CANCELED) {
            // This is a reset, not a canceled
            return;
        }
        
        //if (resultCode == ProxyLauncher.RESULT_USER_CANCELED) {
        //    // Unmap reset vs. canceled
        //    resultCode = Activity.RESULT_CANCELED;
        //}
        
        mRecenterCameraOnResume = false;
        switch (requestCode) {
            case REQUEST_EDIT:
                setCurrentPhotoByIntent(data);
                break;
            case REQUEST_CROP:
                if (resultCode == Activity.RESULT_OK) {
                    setCurrentPhotoByIntent(data);
                }
                break;
            case REQUEST_CROP_PICASA: {
                if (resultCode == Activity.RESULT_OK) {
                    //Context context = mActivity.getAndroidContext();
                    //String message = context.getString(R.string.crop_saved,
                    //        context.getString(R.string.folder_edited_online_photos));
                    //Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case REQUEST_SLIDESHOW: {
                if (data == null) break;
                //String path = data.getStringExtra(SlideshowPage.KEY_ITEM_PATH);
                //int index = data.getIntExtra(SlideshowPage.KEY_PHOTO_INDEX, 0);
                //if (path != null) {
                //    //mModel.setCurrentPhoto(Path.fromString(path), index);
                //}
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mIsActive = false;

        mActivity.getGLRoot().unfreeze();
        mHandler.removeMessages(MSG_UNFREEZE_GLROOT);

        //DetailsHelper.pause();
        // Hide the detail dialog on exit
        //if (mShowDetails) hideDetails();
        if (mModel != null) 
            mModel.pause();
        
        mPhotoView.pause();
        mHandler.removeMessages(MSG_HIDE_BARS);
        mHandler.removeMessages(MSG_REFRESH_BOTTOM_CONTROLS);
        
        refreshBottomControlsWhenReady();
        mActionBar.removeOnMenuVisibilityListener(mMenuVisibilityListener);
        
        onCommitDeleteImage();
        mMenuExecutor.pause();
        if (mMediaSet != null) mMediaSet.clearDeletion();
    }

    @Override
    public void onCurrentImageUpdated() {
        mActivity.getGLRoot().unfreeze();
    }

    @Override
    public void onFilmModeChanged(boolean enabled) {
        refreshBottomControlsWhenReady();
        
        if (enabled) {
            mHandler.removeMessages(MSG_HIDE_BARS);
        } else {
            refreshHidingMessage();
        }
    }

    private void transitionFromAlbumPageIfNeeded() {
        TransitionStore transitions = mActivity.getTransitionStore();

        int albumPageTransition = transitions.get(
                KEY_ALBUMPAGE_TRANSITION, MSG_ALBUMPAGE_NONE);

        if (albumPageTransition == MSG_ALBUMPAGE_NONE && 
        		mAppBridge != null && mRecenterCameraOnResume) {
            // Generally, resuming the PhotoPage when in Camera should
            // reset to the capture mode to allow quick photo taking
            mCurrentIndex = 0;
            mPhotoView.resetToFirstPicture();
            
        } else {
            int resumeIndex = transitions.get(KEY_INDEX_HINT, -1);
            if (resumeIndex >= 0) {
                if (mHasCameraScreennailOrPlaceholder) {
                    // Account for preview/placeholder being the first item
                    resumeIndex++;
                }
                if (mMediaSet != null && resumeIndex < mMediaSet.getItemCount()) {
                    mCurrentIndex = resumeIndex;
                    mModel.moveTo(mCurrentIndex);
                }
            }
        }

        if (albumPageTransition == MSG_ALBUMPAGE_RESUMED) {
            mPhotoView.setFilmMode(mStartInFilmstrip || mAppBridge != null);
        } else if (albumPageTransition == MSG_ALBUMPAGE_PICKED) {
            mPhotoView.setFilmMode(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mModel == null) {
            mActivity.getGLStateManager().finishState(this);
            return;
        }
        transitionFromAlbumPageIfNeeded();

        mActivity.getGLRoot().freeze();
        mIsActive = true;
        setContentPane(mRootPane);

        mModel.resume();
        mPhotoView.resume();
        
        mActionBar.setDisplayOptions(true, true);
        mActionBar.addOnMenuVisibilityListener(mMenuVisibilityListener);
        
        refreshBottomControlsWhenReady();
        
        if (!mShowBars) {
            mActionBar.hide();
            mActivity.getGLRoot().setLightsOutMode(true);
        }
        
        boolean haveImageEditor = false; //GalleryUtils.isEditorAvailable(mActivity, "image/*");
        if (haveImageEditor != mHaveImageEditor) {
            mHaveImageEditor = haveImageEditor;
            updateMenuOperations();
        }

        mRecenterCameraOnResume = true;
        mHandler.sendEmptyMessageDelayed(MSG_UNFREEZE_GLROOT, UNFREEZE_GLROOT_TIMEOUT);
    }

    @Override
    protected void onDestroy() {
        mActivity.getGLRoot().setOrientationSource(null);
        if (mBottomControls != null) mBottomControls.cleanup();

        // Remove all pending messages.
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public void refreshBottomControlsWhenReady() {
        if (mBottomControls == null) 
            return;
        
        mHandler.obtainMessage(MSG_REFRESH_BOTTOM_CONTROLS, 0, 0, 
        		mCurrentPhoto).sendToTarget();
    }

    @Override
    public void onUndoBarVisibilityChanged(boolean visible) {
        refreshBottomControlsWhenReady();
    }
    
}
