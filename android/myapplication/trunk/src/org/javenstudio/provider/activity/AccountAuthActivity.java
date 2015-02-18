package org.javenstudio.provider.activity;

import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.OvershootInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.javenstudio.android.ActionError;
import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.account.AccountAuth;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.DataApp;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.image.ImageEvent;
import org.javenstudio.android.data.image.ImageListener;
import org.javenstudio.android.data.image.http.HttpEvent;
import org.javenstudio.android.data.image.http.HttpImage;
import org.javenstudio.android.data.image.http.HttpResource;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.graphics.TransformDrawable;
import org.javenstudio.cocoka.widget.SimpleLinearLayout;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.Provider;
import org.javenstudio.provider.ProviderController;
import org.javenstudio.util.StringUtils;

public abstract class AccountAuthActivity extends ProviderActivity 
		implements AccountApp.AppListener {
	private static final Logger LOG = Logger.getLogger(AccountAuthActivity.class);

	public static final String EXTRA_ACTION = "org.javenstudio.account.action";
	public static final String EXTRA_ACCOUNTEMAIL = "org.javenstudio.account.email";
	
	public static final String ACTION_ADD_ACCOUNT = "addaccount";
	public static final String ACTION_SELECT_ACCOUNT = "selectaccount";
	public static final String ACTION_SWITCH_ACCOUNT = "switchaccount";
	public static final String ACTION_AUTO_AUTH = "autoauth";
	
	private static final String STATE_EMAIL = "AccountAuthActivity.email";
	private static final String STATE_USERNAME = "AccountAuthActivity.username";
	//private static final String STATE_PASSWORD = "AccountAuthActivity.password";
	
    /** The Intent flag to confirm credentials. */
    public static final String PARAM_CONFIRM_CREDENTIALS = "confirmCredentials";

    /** The Intent extra to store password. */
    public static final String PARAM_PASSWORD = "password";

    /** The Intent extra to store username. */
    public static final String PARAM_USERNAME = "username";

    /** The Intent extra to store username. */
    public static final String PARAM_AUTHTOKEN_TYPE = "authtokenType";
	
	private ProviderController mController = null;
	
	private AccountAuthenticatorResponse mAccountAuthenticatorResponse = null;
    private Bundle mResultBundle = null;

    /**
     * Set the result that is to be sent as the result of the request that caused this
     * Activity to be launched. If result is null or this method is never called then
     * the request will be canceled.
     * @param result this is returned as the result of the AbstractAccountAuthenticator request
     */
    public final void setAccountAuthenticatorResult(Bundle result) {
        mResultBundle = result;
    }
	
	public abstract DataApp getDataApp();
	public abstract AccountApp getAccountApp();
	
	protected abstract Drawable getBackgroundDrawable();
	protected abstract TouchHandler newTouchHandler();
	protected abstract Provider newStartupProvider();

	@Override
	public synchronized ProviderController getController() {
		if (mController == null) { 
			mController = new ProviderController(ResourceHelper.getApplication(), 
					ResourceHelper.getContext());
		}
		return mController;
	}

	private Drawable loadWelcomeDrawable(String imageUrl) {
		ImageItem imageItem = mImageItem;
		if (imageItem != null) { 
			if (imageItem.getImage().existBitmap())
				return imageItem.getImageDrawable();
			
		} else if (imageUrl != null) {
			if (imageUrl != null && imageUrl.length() > 0) {
				if (LOG.isDebugEnabled())
					LOG.debug("loadWelcomeDrawable: create ImageItem: " + imageUrl);
				
				mImageItem = imageItem = new ImageItem(imageUrl);
				if (imageItem.getImage().existBitmap())
					return imageItem.getImageDrawable();
			}
		}
		
		return null;
	}
	
	private ImageItem mImageItem = null;
	
	private class ImageItem implements ImageListener {
		private final String mImageUrl;
		private final HttpImage mImage;
		
		public ImageItem(String imageUrl) {
			mImageUrl = imageUrl;
			mImage = HttpResource.getInstance().getImage(imageUrl);
			mImage.addListener(this);
			mImage.checkDownload(HttpImage.RequestType.DOWNLOAD, true);
		}
		
		public String getImageUrl() { return mImageUrl; }
		public HttpImage getImage() { return mImage; }
		
		public Drawable getImageDrawable() {
			if (mImage != null) return mImage.getThumbnailDrawable(1080, 1240);
			return null;
		}
		
		@Override
		public void onImageEvent(Image image, ImageEvent event) {
			if (image == null || event == null) 
				return;
			
			final String location = image.getLocation();
			synchronized (this) { 
				if (!location.equals(getImageUrl())) 
					return;
			}
			
			if (event instanceof HttpEvent) { 
				HttpEvent e = (HttpEvent)event;
				onHttpImageEvent(image, e);
			}
		}
		
		private void onHttpImageEvent(Image image, HttpEvent event) { 
			if (AccountAuthActivity.this.isDestroyed()) return;
			
			if (event.getEventType() == HttpEvent.EventType.FETCH_FINISH) {
				if (LOG.isDebugEnabled())
					LOG.debug("onHttpImageEvent: " + mImageUrl + " downloaded");
				
				ResourceHelper.getHandler().postDelayed(new Runnable() {
						@Override
						public void run() { 
							if (getImage().existBitmap()) {
								AccountAuthActivity.this.setContentBackground(getCurrentProvider());
								AccountAuthActivity.this.startBackgroundTransforming();
							}
						}
					}, 1000);
			} else if (event.getEventType() == HttpEvent.EventType.NOT_FOUND) {
				if (LOG.isDebugEnabled())
					LOG.debug("onHttpImageEvent: " + mImageUrl + " not found");
				
				mImageItem = null;
			}
		}
	}
	
	public void onAppInited(final AccountApp app) {
		if (isDestroyed()) return;
		if (/*!app.isInited() || */app != getAccountApp()) return;
		
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					if (LOG.isDebugEnabled()) LOG.debug("onAppInited: init views");
					setActivityHostTitle(null);
					setActivityLogoTitle(app.getWelcomeTitle());
					setWelcomeBackground(app.getWelcomeImageUrl());
				}
			});
	}
	
	public void setActivityHostTitle(CharSequence title) {
		mTouchHandler.setHostTitle(title);
	}
	
	public void setActivityLogoTitle(CharSequence title) {
		mTouchHandler.setLogoTitle(title);
	}
	
	public void setWelcomeBackground(String imageUrl) {
		if (imageUrl != null && imageUrl.length() > 0) {
			Drawable d = loadWelcomeDrawable(imageUrl);
			if (d != null) initTransformBackground(d);
		}
	}
	
	private TransformDrawable mBackground = null;
	
	@Override
	public void setContentBackground(Provider p) {
		Drawable d = loadWelcomeDrawable(getAccountApp().getWelcomeImageUrl());
		if (d == null) d = getBackgroundDrawable();
		initTransformBackground(d);
	}
	
	protected void initTransformBackground(Drawable image) {
		if (image == null) return;
		if (LOG.isDebugEnabled())
			LOG.debug("initTransformBackground: image=" + image);
		
		TransformDrawable d = new TransformDrawable(image);
		setContentBackground(d);
		
		TransformDrawable old = mBackground;
		mBackground = d;
		if (old != null)
			old.stopTransforming();
	}
	
	protected void startBackgroundTransforming() {
		TransformDrawable d = mBackground;
		if (d != null) 
			d.startTransforming();
	}
	
	protected void stopBackgroundTransforming() {
		TransformDrawable d = mBackground;
		if (d != null) 
			d.stopTransforming();
	}
	
	protected void postStartBackgroundTransforming(long delayMillis) {
		ResourceHelper.getHandler().postDelayed(new Runnable() {
				@Override
				public void run() {
					startBackgroundTransforming();
				}
		    }, delayMillis);
	}
	
	protected String getIntentAccountAction() {
		return getIntent().getStringExtra(EXTRA_ACTION);
	}
	
	protected String getIntentAccountEmail() {
		return getIntent().getStringExtra(EXTRA_ACCOUNTEMAIL);
	}
	
	@Override
	protected void doOnCreate(Bundle savedInstanceState) { 
		if (getAccountApp().getAccount() != null) {
        	startupMain(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			finish();
        }
		
		getController().setProvider(newStartupProvider());
		super.doOnCreate(savedInstanceState);
		//getActivityHelper().lockOrientation();
		//getSupportActionBar().hide();
		
		//initContentBackground();
		getAccountApp().addListener(this);
		
        mAccountAuthenticatorResponse = getIntent().getParcelableExtra(
        		AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);

        if (mAccountAuthenticatorResponse != null) 
            mAccountAuthenticatorResponse.onRequestContinued();
        
        String accountAction = getIntentAccountAction();
        if (accountAction == null || accountAction.length() == 0 || 
        	accountAction.equals(ACTION_AUTO_AUTH) || accountAction.equals(ACTION_SWITCH_ACCOUNT) ||
        	accountAction.equals(ACTION_SELECT_ACCOUNT)) {
        	AccountAuth.startInitWork(getAccountApp(), newInitCallback(false), 0);
        	AccountAuth.startAuthWork(getAccountApp(), newCheckCallback(), getIntentAccountEmail(), 500);
        } else {
        	if (AccountAuth.startInitWork(getAccountApp(), newInitCallback(true), 0) == false)
        		postShowStartView(null);
        }
        
        mActivityStopped = false;
	}
	
	private boolean mActivityStopped = false;
	
	//@Override
	//protected void onStart() {
	//	super.onStart();
	//}
	
	//@Override
	//protected void setContentProviderOnResume() {
	//	setContentProvider(getController().getProvider(), false);
	//}
	
	@Override
	protected void onResume() {
		super.onResume();
	    postStartBackgroundTransforming(1500);
	}
	
	@Override
	protected void onPause() {
	    super.onPause();
	    stopBackgroundTransforming();
	}
	
	@Override
	protected void onStop() {
	    super.onStop();
	    stopBackgroundTransforming();
	    mActivityStopped = true;
	}
	
	@Override
	protected void onDestroy() {
		getAccountApp().removeListener(this);
		super.onDestroy();
	}
	
    /**
     * Sends the result or a Constants.ERROR_CODE_CANCELED error 
     * if a result isn't present.
     */
	@Override
    public void finish() {
        if (mAccountAuthenticatorResponse != null) {
            // send the result bundle back if set, otherwise send an error.
            if (mResultBundle != null) 
                mAccountAuthenticatorResponse.onResult(mResultBundle);
            else 
                mAccountAuthenticatorResponse.onError(AccountManager.ERROR_CODE_CANCELED, "canceled");
            
            mAccountAuthenticatorResponse = null;
        }
        super.finish();
    }
	
	@Override
	protected void onRequestFeatures(Bundle savedInstanceState) {
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}
	
	@Override
	protected GestureDetector onCreateGestureDetector() {
		return new TouchDetector(this, newTouchListener());
	}
	
	@Override
	public boolean onActionHome() { return false; }
	
	@Override
	protected boolean isLockOrientationDisabled(int orientation) { 
		return true; 
	}
	
	@Override
	protected boolean isUnlockOrientationDisabled() { 
		return true; 
	}
	
	@Override
	protected void overridePendingTransition() {}
	
	protected void overridePendingTransitionAlphaExit() {
		overridePendingTransition(R.anim.activity_fade_enter, R.anim.activity_fade_exit); 
	}
	
	@Override
	public void onBackPressed() {
		if (mTouchHandler.handleBack()) return;
		super.onBackPressed();
	}
	
	@Override
	public void refreshContent(boolean force) { 
		if (LOG.isDebugEnabled())
			LOG.debug("refreshContent: disable refresh, force=" + force);
	}
	
	public static enum ViewState {
		START, REGISTER, LOGIN, SELECTACCOUNT
	}
	
	public abstract class TouchHandler implements SimpleLinearLayout.OnDrawListener, 
			SimpleLinearLayout.CanvasTransformer {
		private static final int DISTANCE_MAX = 180;
		private static final int DISTANCE_MIN = (-1) * DISTANCE_MAX;
		
		protected SimpleLinearLayout mLogoView = null;
		protected SimpleLinearLayout mButtonView = null;
		protected SimpleLinearLayout mInputView = null;
		protected SimpleLinearLayout mAccountView = null;
		
		protected TextView mTitleView = null;
		protected ListView mAccountList = null;
		protected ListAdapter mAccountAdapter = null;
		
		protected EditText mEmailEdit = null;
		protected EditText mUsernameEdit = null;
		protected EditText mPasswordEdit = null;
		
		private int mTouchAction = MotionEvent.ACTION_UP;
		private int mLogoDistanceY = 0;
		private int mButtonDistanceY = 0;
		private float mDistanceDelta = 0;
		private float mDistance = 0;
		private float mPercentOpen = 0.0f;
		//private boolean mRegisterMode = false;
		private boolean mScrollEnable = false;
		
		private ViewState mState = null;
		private boolean mRegisterMode = false;
		
		public abstract void bindView(LayoutInflater inflater, View view, 
				Bundle savedInstanceState);
		
		private void hideSoftInput() {
			final InputMethodManager imm = (InputMethodManager)
					getSystemService(INPUT_METHOD_SERVICE);
			
			final EditText emailEdit = mEmailEdit;
			final EditText usernameEdit = mUsernameEdit;
			final EditText passwordEdit = mPasswordEdit;
			
			if (imm != null) {
				if (emailEdit != null) 
					imm.hideSoftInputFromWindow(emailEdit.getWindowToken(), 0);
				if (usernameEdit != null) 
					imm.hideSoftInputFromWindow(usernameEdit.getWindowToken(), 0);
				if (passwordEdit != null) 
					imm.hideSoftInputFromWindow(passwordEdit.getWindowToken(), 0);
			}
		}
		
		private void onScroll(float distanceX, float distanceY) {
			if (mScrollEnable == false) return;
			//if (DEBUG && LOG.isDebugEnabled()) {
			//	LOG.debug("onScroll: distanceX=" + distanceX 
			//			+ " distanceY=" + distanceY + " delta=" + mDistanceDelta 
			//			+ " distance=" + mDistance + " percent=" + mPercentOpen 
			//			+ " action=" + mTouchAction);
			//}
			
			final View logoview = mLogoView;
			final View buttonview = mButtonView;
			final View inputview = mInputView;
			final View accountview = mAccountView;
			final TextView titleview = mTitleView;
			
			float alpha = 0;
			
			mDistanceDelta = distanceY;
			distanceY = distanceY / 2.0f;
			
			if (logoview != null) {
				int x = 0; //(int)distanceX;
				int y = (int)distanceY;
				
				int distance = mLogoDistanceY + y;
				if (distance < 0) y -= distance;
				else if (distance > DISTANCE_MAX) 
					y -= distance - DISTANCE_MAX;
				
				distance = (mLogoDistanceY += y);
				mDistance = distance;
				
				logoview.scrollBy(x, y);
				logoview.invalidate();
			}
			
			if (buttonview != null) {
				int x = 0; //(int)distanceX;
				int y = (int)(distanceY * (-1));
				
				int distance = mButtonDistanceY + y;
				if (distance > 0) y -= distance;
				else if (distance < DISTANCE_MIN)
					y -= distance - DISTANCE_MIN;
				
				distance = (mButtonDistanceY += y);
				
				alpha = 1 - ((float)distance / DISTANCE_MIN);
				if (alpha < 0) alpha = 0;
				else if (alpha > 1) alpha = 1;
				
				if (alpha <= 0.05f) {
					buttonview.setVisibility(View.INVISIBLE);
					if (titleview != null) titleview.setVisibility(View.INVISIBLE);
				} else {
					buttonview.setVisibility(View.VISIBLE);
					if (titleview != null) titleview.setVisibility(View.VISIBLE);
					setViewAlpha(buttonview, alpha);
					setViewAlpha(titleview, alpha);
				}
				
				buttonview.scrollBy(x, y);
				buttonview.invalidate();
			}
			
			if (inputview != null) {
				alpha = 1 - alpha;
				if (alpha < 0) alpha = 0;
				else if (alpha > 1) alpha = 1;
				mPercentOpen = alpha;
				
				if (alpha <= 0.1f) {
					inputview.setVisibility(View.INVISIBLE);
					hideSoftInput();
				} else {
					inputview.setVisibility(View.VISIBLE);
					setViewAlpha(inputview, alpha);
				}
				
				if (accountview != null) {
					if (alpha < 1.0f) {
						accountview.setVisibility(View.GONE);
					}
				}
				
				inputview.invalidate();
			}
		}
		
		protected boolean isScrollEnabled() {
			return getPercentOpen() < 1.0f;
		}
		
		public void handleScroll(MotionEvent e1, MotionEvent e2, 
				float distanceX, float distanceY) { 
			if (e1 == null || e2 == null) return;
			mTouchAction = e2.getAction();
			
			if (isScrollEnabled())
				onScroll(distanceX, distanceY);
		}
		
		public void handleFling(MotionEvent e1, MotionEvent e2, 
				float velocityX, float velocityY) {
		}
		
		public void handleDown(MotionEvent e) {
			if (e == null) return;
			mTouchAction = e.getAction();
			
			//if (DEBUG && LOG.isDebugEnabled()) {
			//	LOG.debug("handleDown: delta=" + mDistanceDelta + " distance=" + mDistance 
			//			+ " action=" + mTouchAction);
			//}
		}
		
		public void handleUp(MotionEvent e) {
			if (e == null) return;
			mTouchAction = e.getAction();
			
			//if (DEBUG && LOG.isDebugEnabled()) {
			//	LOG.debug("handleUp: delta=" + mDistanceDelta + " distance=" + mDistance 
			//			+ " action=" + mTouchAction);
			//}
			
			scrollAuto();
		}
		
		@Override
		public void onDraw(Canvas canvas) {
			//if (DEBUG && LOG.isDebugEnabled()) {
			//	LOG.debug("onDraw: delta=" + mDistanceDelta + " distance=" + mDistance 
			//			+ " action=" + mTouchAction);
			//}
			
			scrollAuto();
		}
		
		@Override
		public void transformCanvas(Canvas canvas, View view) {
			if (view != mInputView) return;
			if (mDistance <= 0 || mDistance >= DISTANCE_MAX) return;
			
			float percent = mPercentOpen;
			if (percent < 0) percent = 0;
			else if (percent > 1) percent = 1;
			
			float dx = (1 - percent) * 20.0f;
			float dy = (1 - percent) * 40.0f;
			float sx = 1.0f - (0.1f * (1 - percent));
			float sy = 1.0f - (0.1f * (1 - percent));
			
			canvas.translate(dx, dy);
			canvas.scale(sx, sy);
			
			//if (DEBUG && LOG.isDebugEnabled()) {
			//	LOG.debug("transformCanvas: dx=" + dx + " dy=" + dy + " sx=" + sx + " sy=" + sy 
			//			+ " percent=" + percent);
			//}
		}
		
		private void scrollAuto() {
			if (mTouchAction != MotionEvent.ACTION_UP) return;
			if (mDistance <= 0 || mDistance >= DISTANCE_MAX) return;
			
			//if (DEBUG && LOG.isDebugEnabled()) {
			//	LOG.debug("scrollAuto: delta=" + mDistanceDelta + " distance=" + mDistance 
			//			+ " action=" + mTouchAction);
			//}
			
			float delta = mDistanceDelta;
			if (delta >= 0) {
				delta = delta * 0.98f + 1.0f;
				if (delta < 25.0f) delta = 25.0f;
			} else { 
				delta = delta * 0.98f - 1.0f;
				if (delta > -25.0f) delta = -25.0f;
			}
			
			onScroll(0, delta);
		}
		
		public float getPercentOpen() {
			return mPercentOpen;
		}
		
		public void scrollClose() {
			onScroll(0, -35.0f);
		}
		
		public void scrollOpen() {
			onScroll(0, 35.0f);
		}
		
		public void initScroll(boolean enable) {
			mScrollEnable = true;
			onScroll(0, 0);
			mScrollEnable = enable;
			mState = ViewState.START;
		}
		
		public int getAccountCount() {
			return getAccountApp().getAccountCount();
		}
		
		protected void initAccountView() {
			final View inputview = mInputView;
			final View accountview = mAccountView;
			
			if (inputview != null) {
				inputview.setVisibility(View.GONE);
			}
			
			if (accountview != null) {
				accountview.setVisibility(View.VISIBLE);
			}
			
			setHostTitle(null);
			setAccountList();
			
			mState = ViewState.SELECTACCOUNT;
			mRegisterMode = false;
		}
		
		protected void initRegisterView() {
			final View inputview = mInputView;
			final View accountview = mAccountView;
			
			if (inputview != null) {
				//inputview.setVisibility(View.VISIBLE);
			}
			
			if (accountview != null) {
				accountview.setVisibility(View.GONE);
			}
			
			EditText emailEdit = mEmailEdit;
			EditText usernameEdit = mUsernameEdit;
			
			if (emailEdit != null) {
				//emailEdit.setHint(R.string.label_email_hint);
				emailEdit.setVisibility(View.VISIBLE);
			}
			
			if (usernameEdit != null) {
				//usernameEdit.setHint(R.string.label_username_hint);
				usernameEdit.setVisibility(View.VISIBLE);
			}
			
			setHostTitle(null);
			
			mState = ViewState.REGISTER;
			mRegisterMode = true;
		}
		
		protected void setUsernameText(String username) {
			if (username == null) username = "";
			EditText usernameEdit = mUsernameEdit;
			if (usernameEdit != null)
				usernameEdit.setText(username);
		}
		
		public void handleRegisterClick() {
			initRegisterView();
			scrollOpen();
		}
		
		protected void initLoginView() {
			final View inputview = mInputView;
			final View accountview = mAccountView;
			
			if (inputview != null) {
				//inputview.setVisibility(View.VISIBLE);
			}
			
			if (accountview != null) {
				accountview.setVisibility(View.GONE);
			}
			
			EditText emailEdit = mEmailEdit;
			EditText usernameEdit = mUsernameEdit;
			
			if (emailEdit != null) {
				//emailEdit.setHint(R.string.label_email_hint);
				emailEdit.setVisibility(View.GONE);
			}
			
			if (usernameEdit != null) {
				//usernameEdit.setHint(R.string.label_username_or_email_hint);
				usernameEdit.setVisibility(View.VISIBLE);
			}
			
			setHostTitle(null);
			
			mState = ViewState.LOGIN;
			mRegisterMode = false;
		}
		
		public void handleLoginClick() {
			initLoginView();
			scrollOpen();
		}
		
		public boolean handleBack() {
			if (mDistance > 0) {
				scrollClose();
				return true;
			}
			return false;
		}
		
		public void handleResetPassword() {}
		public void handleSelectAccount() { initAccountView(); }
		public void handleSelectHost() {}
		public void handleSignin() { handleLoginClick(); }
		public void handleSignup() { handleRegisterClick(); }
		
		public void setHostTitle(CharSequence title) {}
		public void setLogoTitle(CharSequence title) {}
		public void setAccountList() {}
		
		public ViewState getViewState() { return mState; }
		public boolean isRegisterMode() { return mRegisterMode; }
		
		public void postSetHostTitle(final CharSequence title) {
			ResourceHelper.getHandler().post(new Runnable() {
					@Override
					public void run() {
						setHostTitle(title);
					}
				});
		}
		
		public void postSetAccountList() {
			ResourceHelper.getHandler().post(new Runnable() {
					@Override
					public void run() {
						setAccountList();
					}
				});
		}
		
		public void onBindedView(Bundle savedInstanceState) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("onBindedView: stopped=" + mActivityStopped 
						+ " savedInstanceState=" + savedInstanceState);
			}
			
			EditText emailEdit = mEmailEdit;
			EditText usernameEdit = mUsernameEdit;
			EditText passwordEdit = mPasswordEdit;
			
			if (savedInstanceState != null) {
				if (emailEdit != null) {
					CharSequence text = emailEdit.getText();
					if (text == null || text.length() == 0) {
						text = savedInstanceState.getString(STATE_EMAIL);
						if (text != null && text.length() > 0)
							emailEdit.setText(text);
					}
				}
				if (usernameEdit != null) {
					CharSequence text = usernameEdit.getText();
					if (text == null || text.length() == 0) {
						text = savedInstanceState.getString(STATE_USERNAME);
						if (text != null && text.length() > 0)
							usernameEdit.setText(text);
					}
				}
				if (passwordEdit != null) {
					//CharSequence text = passwordEdit.getText();
					//if (text == null || text.length() == 0) {
					//	text = savedInstanceState.getString(STATE_PASSWORD);
					//	if (text != null && text.length() > 0)
					//		passwordEdit.setText(text);
					//}
				}
			}
			
			if (mActivityStopped) {
				mActivityStopped = false;
				
				//if (AccountAuthActivity.this.isProgressRunning() == false)
				//	postShowStartView(null);
			}
		}
		
		public void onRestoreInstanceState(Bundle savedInstanceState) {
			if (LOG.isDebugEnabled())
				LOG.debug("onRestoreInstanceState: savedInstanceState=" + savedInstanceState);
		}
		
		public void onSaveInstanceState(Bundle savedInstanceState) {
			EditText emailEdit = mEmailEdit;
			EditText usernameEdit = mUsernameEdit;
			EditText passwordEdit = mPasswordEdit;
			
			if (savedInstanceState != null) {
				if (emailEdit != null) {
					CharSequence text = emailEdit.getText();
					if (text != null && text.length() > 0)
						savedInstanceState.putString(STATE_EMAIL, text.toString());
				}
				if (usernameEdit != null) {
					CharSequence text = usernameEdit.getText();
					if (text != null && text.length() > 0)
						savedInstanceState.putString(STATE_USERNAME, text.toString());
				}
				if (passwordEdit != null) {
					//CharSequence text = passwordEdit.getText();
					//if (text != null && text.length() > 0)
					//	savedInstanceState.putString(STATE_PASSWORD, text.toString());
				}
			}
			
			if (LOG.isDebugEnabled()) 
				LOG.debug("onSaveInstanceState: savedInstanceState=" + savedInstanceState);
		}
		
		public void showStartView(boolean showAnimation) {
			if (LOG.isDebugEnabled()) 
				LOG.debug("showStartView: showAnimation=" + showAnimation);
			
			handleBack();
			initRegisterView();
			initScroll(true);
			
			final View buttonview = mButtonView;
			if (buttonview != null) {
				if (showAnimation == false) {
					buttonview.setVisibility(View.VISIBLE);
					return;
				}
				
				final TranslateAnimation ani = new TranslateAnimation(0, 0, 400, 0);
				ani.setInterpolator(new OvershootInterpolator());
				ani.setDuration(1000);
				ani.setAnimationListener(
					new Animation.AnimationListener() {
						@Override
						public void onAnimationStart(Animation animation) {
						}
						@Override
						public void onAnimationEnd(Animation animation) {
							buttonview.clearAnimation();
						}
						@Override
						public void onAnimationRepeat(Animation animation) {
						}
					});
				
				buttonview.setVisibility(View.INVISIBLE);
				ResourceHelper.getHandler().postDelayed(
					new Runnable() {
						@Override
						public void run() {
							buttonview.startAnimation(ani);
							buttonview.setVisibility(View.VISIBLE);
						}
					}, 400);
			}
		}
	}
	
	private static void setViewAlpha(View view, float alpha) {
		if (view == null) return;
		view.setAlpha(alpha);
		
		if (view instanceof ViewGroup) {
			ViewGroup group = (ViewGroup)view;
			for (int i=0; i < group.getChildCount(); i++) {
				View child = group.getChildAt(i);
				setViewAlpha(child, alpha);
			}
		}
	}
	
	protected final TouchHandler mTouchHandler = newTouchHandler();
	
	//public TouchHandler getTouchHandler() { return mTouchHandler; }
	
	public static interface OnTouchListener extends GestureDetector.OnGestureListener {
		public boolean onTouchDown(MotionEvent e);
		public boolean onTouchUp(MotionEvent e);
	}
	
	private static class TouchDetector extends GestureDetector {
		private final OnTouchListener mListener;
		
		public TouchDetector(Context context, OnTouchListener listener) {
			super(context, listener);
			mListener = listener;
		}
		
		@Override
		public boolean onTouchEvent(MotionEvent ev) {
			if (mListener != null && ev != null) {
				switch (ev.getAction() & MotionEvent.ACTION_MASK) {
				case MotionEvent.ACTION_DOWN:
					mListener.onTouchDown(ev);
					break;
				case MotionEvent.ACTION_UP:
					mListener.onTouchUp(ev);
					break;
				}
			}
			return super.onTouchEvent(ev);
		}
	}
	
	private OnTouchListener newTouchListener() {
		return new OnTouchListener() { 
			public boolean onDown(MotionEvent e) { return false; }
			public void onShowPress(MotionEvent e) {}
			public boolean onSingleTapUp(MotionEvent e) { return false; }
			public void onLongPress(MotionEvent e) {}
	
			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, 
					float velocityX, float velocityY) { 
				if (e1 == null || e2 == null) return false;
				mTouchHandler.handleFling(e1, e2, velocityX, velocityY);
				return false; 
			}
			
			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, 
					float distanceX, float distanceY) { 
				if (e1 == null || e2 == null) return false;
				mTouchHandler.handleScroll(e1, e2, distanceX, distanceY);
				return false; 
			}
			
			@Override
			public boolean onTouchDown(MotionEvent e) {
				mTouchHandler.handleDown(e);
				return false;
			}
			
			@Override
			public boolean onTouchUp(MotionEvent e) {
				mTouchHandler.handleUp(e);
				return false;
			}
		};
	}
	
	protected void handleRegisterLogin(EditText emailTxt, 
			EditText usernameTxt, EditText passwordTxt) {
		if (emailTxt == null || usernameTxt == null || passwordTxt == null) 
			return;
		
		emailTxt.clearFocus();
		usernameTxt.clearFocus();
		passwordTxt.clearFocus();
		
		InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(emailTxt.getWindowToken(), 0);
		imm.hideSoftInputFromWindow(usernameTxt.getWindowToken(), 0);
		imm.hideSoftInputFromWindow(passwordTxt.getWindowToken(), 0);
		
		String email = StringUtils.trim(emailTxt.getText().toString());
		String username = StringUtils.trim(usernameTxt.getText().toString());
		String password = StringUtils.trim(passwordTxt.getText().toString());
		
		boolean registerMode = mTouchHandler.isRegisterMode();
		
		if (registerMode && checkEmail(email, registerMode) == false) 
			return;
		
		if (checkUsername(username, registerMode) == false)
			return;
		
		if (checkPassword(password, registerMode) == false)
			return;
		
        AccountAuth.startLoginWork(getAccountApp(), 
        	username, password, email, registerMode, 
        	new AccountAuth.Callback() {
				@Override
				public void onWorkStart(AccountApp app, AccountAuth.Action action) {
					if (LOG.isDebugEnabled()) LOG.debug("onWorkStart: login action=" + action);
					getActivityHelper().postShowProgressAlert(
							AppResources.getInstance().getStringText(AppResources.string.login_signingin_message));
				}
				@Override
				public void onWorkError(AccountApp app, AccountAuth result) {
					if (LOG.isDebugEnabled()) LOG.debug("onWorkError: login");
					getActivityHelper().postHideProgressAlert();
					handleAuthResult(app, result);
				}
				@Override
				public void onWorkDone(AccountApp app, AccountAuth result) {
					if (LOG.isDebugEnabled()) LOG.debug("onWorkDone: login");
					getActivityHelper().postHideProgressAlert();
					handleAuthResult(app, result);
				}
				@Override
				public void onRequestStart(AccountApp app, AccountAuth.Action action) {
					if (LOG.isDebugEnabled()) LOG.debug("onRequestStart: login");
					mTouchHandler.postSetHostTitle(null);
				}
			});
	}
	
	protected AccountAuth.Callback newCheckCallback() {
		return new AccountAuth.Callback() {
				@Override
				public void onWorkStart(AccountApp app, AccountAuth.Action action) {
					if (LOG.isDebugEnabled()) LOG.debug("onWorkStart: check action=" + action);
					postShowProgress(false);
				}
				@Override
				public void onWorkError(AccountApp app, AccountAuth result) {
					if (LOG.isDebugEnabled()) LOG.debug("onWorkError: check");
					postHideProgress(false);
					handleAuthResult(app, result);
				}
				@Override
				public void onWorkDone(AccountApp app, AccountAuth result) {
					if (LOG.isDebugEnabled()) LOG.debug("onWorkDone: check");
					postHideProgress(false);
					handleAuthResult(app, result);
				}
				@Override
				public void onRequestStart(AccountApp app, AccountAuth.Action action) {
					if (LOG.isDebugEnabled()) LOG.debug("onRequestStart: check");
					mTouchHandler.postSetHostTitle(null);
				}
			};
	}
	
	protected AccountAuth.Callback newInitCallback(final boolean gotoLogin) {
		return new AccountAuth.Callback() {
				@Override
				public void onWorkStart(AccountApp app, AccountAuth.Action action) {
					if (LOG.isDebugEnabled()) LOG.debug("onWorkStart: init action=" + action);
					postShowProgress(false);
				}
				@Override
				public void onWorkError(AccountApp app, AccountAuth result) {
					if (LOG.isDebugEnabled()) LOG.debug("onWorkError: init");
					postHideProgress(false);
					if (gotoLogin) postShowStartView(null);
				}
				@Override
				public void onWorkDone(AccountApp app, AccountAuth result) {
					if (LOG.isDebugEnabled()) LOG.debug("onWorkDone: init");
					postHideProgress(false);
					onAppInited(app);
					if (gotoLogin) postShowStartView(null);
				}
				@Override
				public void onRequestStart(AccountApp app, AccountAuth.Action action) {
					if (LOG.isDebugEnabled()) LOG.debug("onRequestStart: init");
					mTouchHandler.postSetHostTitle(getAppInitTitle());
				}
			};
	}
	
	protected CharSequence getAppInitTitle() {
		return getString(R.string.refreshing_title);
	}
	
	@Override
	public boolean isContentProgressEnabled() { return true; }
	
	protected void handleAuthResult(AccountApp app, AccountAuth result) {
		if (app == null) return;
		if (LOG.isDebugEnabled()) 
			LOG.debug("handleAuthResult: result=" + result);
		
		String accountName = null;
		if (result != null) {
			AccountAuth.Result res = result.getResult();
			if (res == AccountAuth.Result.REGISTER_SUCCESS || 
				res == AccountAuth.Result.LOGIN_SUCCESS || 
				res == AccountAuth.Result.AUTH_SUCCESS || 
				res == AccountAuth.Result.AUTHENTICATED) {
				gotoMainActivity(500);
				return;
				
			} else {
				ActionError error = result.getError();
				if (error != null) { 
					getActivityHelper().onActionError(error, result);
					if (error.getAction() == ActionError.Action.ACCOUNT_REGISTER || 
						error.getAction() == ActionError.Action.ACCOUNT_LOGIN) {
						return;
					}
				}
			}
			accountName = app.getAccountName(result.getAccountId());
		}
		
		postShowStartView(accountName);
	}
	
	protected void postShowStartView(final String accountName) {
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					showStartView(accountName, true);
				}
			});
	}
	
	private void gotoMainActivity(long delayedMillis) {
		ResourceHelper.getHandler().postDelayed(
			new Runnable() {
				@Override
				public void run() {
					startupMain(Intent.FLAG_ACTIVITY_SINGLE_TOP);
					finish();
					overridePendingTransitionAlphaExit();
				}
			}, delayedMillis);
	}
	
	private void showStartView(final String accountName, 
			final boolean showAnimation) {
		ResourceHelper.getHandler().post(
			new Runnable() {
				@Override
				public void run() {
					mTouchHandler.setUsernameText(accountName);
					mTouchHandler.showStartView(showAnimation);
				}
			});
	}
	
	protected abstract void startupMain(int flag);
	protected abstract boolean checkEmail(String email, boolean registerMode);
	protected abstract boolean checkUsername(String username, boolean registerMode);
	protected abstract boolean checkPassword(String password, boolean registerMode);
	
}
