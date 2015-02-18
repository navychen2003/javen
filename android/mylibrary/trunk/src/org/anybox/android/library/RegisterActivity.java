package org.anybox.android.library;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ImageView;
import android.widget.TextView;

import org.anybox.android.library.app.MyAccountItem;
import org.anybox.android.library.app.MyHostHelper;
import org.javenstudio.android.account.AccountHelper;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.information.InformationHelper;
import org.javenstudio.cocoka.app.RefreshListView;
import org.javenstudio.cocoka.widget.SimpleLinearLayout;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.Provider;
import org.javenstudio.provider.account.RegisterProvider;
import org.javenstudio.provider.activity.AccountAuthActivity;
import org.javenstudio.provider.app.anybox.AnyboxApp;

public class RegisterActivity extends AccountAuthActivity {
	private static final Logger LOG = Logger.getLogger(RegisterActivity.class);

	public static void actionActivity(Context from) { 
		Intent intent = new Intent(from, RegisterActivity.class); 
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
		
		from.startActivity(intent); 
	}
	
	public static void actionLogin(String action, String accountEmail) {
		Application app = MyImplements.getApplication();
		
		Intent intent = new Intent(app, RegisterActivity.class); 
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK); 
		
		if (action != null && action.length() > 0)
			intent.putExtra(EXTRA_ACTION, action);
		
		if (accountEmail != null && accountEmail.length() > 0)
			intent.putExtra(EXTRA_ACCOUNTEMAIL, accountEmail);
		
		app.startActivity(intent);
	}
	
	private final Object mProviderLock = new Object();
	private Provider mCurrentProvider = null;
	
	@Override
	public Provider getCurrentProvider() {
		synchronized (mProviderLock) {
			return mCurrentProvider;
		}
	}

	@Override
	public Provider setCurrentProvider(Provider provider) {
		if (LOG.isDebugEnabled()) 
			LOG.debug("setCurrentProvider: provider=" + provider);
		
		synchronized (mProviderLock) {
			Provider old = mCurrentProvider;
			mCurrentProvider = provider;
			return old;
		}
	}
	
	@Override
	protected void doOnCreateDone(Bundle savedInstanceState) { 
		super.doOnCreateDone(savedInstanceState);
		setContentProviderOnStart();
	}
	
	@Override
	protected void onStart() {
        super.onStart();
        setContentProviderOnStart();
	}
	
	@Override
	protected void onResume() {
        super.onResume();
        setContentProviderOnStart();
        Provider provider = getCurrentProvider();
        if (provider != null) {
        	setContentBackground(provider);
        	provider.setContentBackground(this);
        }
	}
	
	private void setContentProviderOnStart() {
		setContentProvider(getController().getProvider(), false);
	}
	
	@Override
	public MyApp getDataApp() {
		return MyApp.getInstance();
	}

	@Override
	public AnyboxApp getAccountApp() {
		return MyApp.getInstance().getAccountApp();
	}
	
	@SuppressWarnings("unused")
	private static int[] sBackgroundIds = new int[]{ 
			R.drawable.background_01
		};
	
	private int getBackgroundResourceId() {
		//int idx = (int)(System.currentTimeMillis() % sBackgroundIds.length);
		//if (idx >= 0 && idx < sBackgroundIds.length)
		//	return sBackgroundIds[idx];
		return R.drawable.background_01;
	}
	
	@Override
	protected Drawable getBackgroundDrawable() {
		return getResources().getDrawable(getBackgroundResourceId());
	}
	
	@Override
	public void setWelcomeBackground(String imageUrl) {
		super.setWelcomeBackground(imageUrl);
	}
	
	@Override
	protected TouchHandler newTouchHandler() {
		return new TouchHandler() {
			private View mAnyboxView = null;
			private View mPromptView = null;
			
			private TextView mHostView = null;
			
			private TextView mAccountText = null;
			private TextView mExplanationText = null;
			private View mResetPasswordView = null;
			private View mSelectAccountView = null;
			private View mSelectHostView = null;
			private View mSelectAccountHostView = null;
			private View mSigninAccountView = null;
			private View mSignupAccountView = null;
			
			@Override
			public void bindView(LayoutInflater inflater, View view, 
					Bundle savedInstanceState) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("bindView: handler=" + this + " view=" + view 
							+ " savedInstanceState=" + savedInstanceState);
				}
				
				//final View googleBtn = view.findViewById(R.id.login_button_google);
				final View anyboxBtn = view.findViewById(R.id.login_button_anybox);
				final View submitBtn = view.findViewById(R.id.login_submit_button);
				final View loginBtn = view.findViewById(R.id.login_prompt_button);
				final View prompt = view.findViewById(R.id.login_prompt);
				
				mLogoView = (SimpleLinearLayout)view.findViewById(R.id.login_logo);
				mButtonView = (SimpleLinearLayout)view.findViewById(R.id.login_button);
				mInputView = (SimpleLinearLayout)view.findViewById(R.id.login_input);
				mAccountView = (SimpleLinearLayout)view.findViewById(R.id.login_account);
				mAccountList = (ListView)view.findViewById(R.id.login_account_list);
				
				mTitleView = (TextView)view.findViewById(R.id.login_text_title);
				mHostView = (TextView)view.findViewById(R.id.login_text_hosttitle);
				mAccountText = (TextView)view.findViewById(R.id.login_account_title);
				mExplanationText = (TextView)view.findViewById(R.id.login_explanation);
				
				mResetPasswordView = view.findViewById(R.id.login_resetpassword);
				mSelectAccountView = view.findViewById(R.id.login_selectaccount);
				mSelectHostView = view.findViewById(R.id.login_selecthost);
				mSelectAccountHostView = view.findViewById(R.id.login_account_selecthost);
				mSigninAccountView = view.findViewById(R.id.login_account_signin);
				mSignupAccountView = view.findViewById(R.id.login_account_signup);
				
				mEmailEdit = (EditText)view.findViewById(R.id.login_email_field);
				mUsernameEdit = (EditText)view.findViewById(R.id.login_username_field);
				mPasswordEdit = (EditText)view.findViewById(R.id.login_password_field);
				
				mLogoView.setOnDrawListener(this);
				mInputView.setCanvasTransformer(this);
				mAnyboxView = anyboxBtn;
				mPromptView = prompt;
				
				//mAccountAdapter = MyAccountItem.getListAdapter(RegisterActivity.this, getAccountApp());
				//mAccountList.setAdapter(mAccountAdapter);
				RefreshListView.disableOverscrollGlowEdge(mAccountList);
				
				EditText emailEdit = mEmailEdit;
				if (emailEdit != null) {
					emailEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
						@Override
						public void onFocusChange(View v, boolean hasFocus) {
							String email = mEmailEdit.getText().toString();
							String username = mUsernameEdit.getText().toString();
							if ((email != null && email.length() > 0) && 
								(username == null || username.length() == 0)) { 
								int pos = email.indexOf('@');
								if (pos > 0) {
									String name = email.substring(0, pos);
									if (name != null && name.length() > 0) {
										mUsernameEdit.setText(name);
									}
								}
							}
						}
					});
				}
				
				TextView hostView = mHostView;
				if (hostView != null) {
					hostView.setText(getAccountApp().getHostDisplayName());
					hostView.setVisibility(View.VISIBLE);
					hostView.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								handleSelectHost();
							}
						});
				}
				
				View selecthostView = mSelectHostView;
				if (selecthostView != null) {
					selecthostView.setVisibility(View.VISIBLE);
					selecthostView.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								handleSelectHost();
							}
						});
				}
				
				View selectaccounthostView = mSelectAccountHostView;
				if (selectaccounthostView != null) {
					selectaccounthostView.setVisibility(View.VISIBLE);
					selectaccounthostView.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								handleSelectHost();
							}
						});
				}
				
				View resetpasswordView = mResetPasswordView;
				if (resetpasswordView != null) {
					resetpasswordView.setVisibility(View.VISIBLE);
					resetpasswordView.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								handleResetPassword();
							}
						});
				}
				
				View selectaccountView = mSelectAccountView;
				if (selectaccountView != null) {
					selectaccountView.setVisibility(getAccountCount() > 0 ? View.VISIBLE: View.GONE);
					selectaccountView.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								handleSelectAccount();
							}
						});
				}
				
				View signinaccountView = mSigninAccountView;
				if (signinaccountView != null) {
					signinaccountView.setVisibility(View.VISIBLE);
					signinaccountView.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								handleSignin();
							}
						});
				}
				
				View singupaccountView = mSignupAccountView;
				if (singupaccountView != null) {
					singupaccountView.setVisibility(View.VISIBLE);
					singupaccountView.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								handleSignup();
							}
						});
				}
				
				if (anyboxBtn != null) {
					anyboxBtn.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								handleRegisterClick();
							}
						});
				}
				
				if (loginBtn != null) {
					loginBtn.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								handleLoginClick();
							}
						});
				}
				
				if (submitBtn != null) {
					submitBtn.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								handleRegisterLogin(mEmailEdit, mUsernameEdit, mPasswordEdit);
							}
						});
				}
				
				setLogoTitle(getAccountApp().getWelcomeTitle());
				//initRegisterView();
				initScroll(false);
				
				onBindedView(savedInstanceState);
			}
			
			@Override
			protected void initAccountView() {
				TextView accountText = mAccountText;
				
				if (accountText != null) {
					accountText.setText(InformationHelper.formatContentSpanned(
							getString(R.string.login_explanation_selectaccount)));
					accountText.setAutoLinkMask(Linkify.ALL);
				}
				
				super.initAccountView();
			}
			
			@Override
			protected void initRegisterView() {
				View anyboxView = mAnyboxView;
				View promptView = mPromptView;
				//View inputView = mInputView;
				
				if (anyboxView != null) anyboxView.setVisibility(View.VISIBLE);
				if (promptView != null) promptView.setVisibility(View.VISIBLE);
				//if (inputView != null) inputView.setVisibility(View.INVISIBLE);
				
				View resetpasswordView = mResetPasswordView;
				View selectaccountView = mSelectAccountView;
				View selecthostView = mSelectHostView;
				
				TextView explanationText = mExplanationText;
				EditText emailEdit = mEmailEdit;
				EditText usernameEdit = mUsernameEdit;
				
				if (resetpasswordView != null) {
					resetpasswordView.setVisibility(View.GONE);
				}
				
				if (selectaccountView != null) {
					selectaccountView.setVisibility(getAccountCount() > 0 ? View.VISIBLE: View.GONE);
				}
				
				if (selecthostView != null) {
					selecthostView.setVisibility(View.VISIBLE);
				}
				
				if (explanationText != null) {
					explanationText.setText(InformationHelper.formatContentSpanned(
							getString(R.string.login_explanation_signup)));
					explanationText.setAutoLinkMask(Linkify.ALL);
					explanationText.setVisibility(View.VISIBLE);
				}
				
				if (emailEdit != null) {
					emailEdit.setHint(R.string.label_email_hint);
					emailEdit.setVisibility(View.VISIBLE);
				}
				
				if (usernameEdit != null) {
					usernameEdit.setHint(R.string.label_username_hint);
					usernameEdit.setVisibility(View.VISIBLE);
				}
				
				super.initRegisterView();
			}
			
			@Override
			protected void initLoginView() {
				View anyboxView = mAnyboxView;
				View promptView = mPromptView;
				//View inputView = mInputView;
				
				if (anyboxView != null) anyboxView.setVisibility(View.VISIBLE);
				if (promptView != null) promptView.setVisibility(View.VISIBLE);
				//if (inputView != null) inputView.setVisibility(View.VISIBLE);
				
				View resetpasswordView = mResetPasswordView;
				View selectaccountView = mSelectAccountView;
				View selecthostView = mSelectHostView;
				
				TextView explanationText = mExplanationText;
				EditText emailEdit = mEmailEdit;
				EditText usernameEdit = mUsernameEdit;
				
				if (resetpasswordView != null) {
					resetpasswordView.setVisibility(View.VISIBLE);
				}
				
				if (selectaccountView != null) {
					selectaccountView.setVisibility(getAccountCount() > 0 ? View.VISIBLE: View.GONE);
				}
				
				if (selecthostView != null) {
					selecthostView.setVisibility(View.VISIBLE);
				}
				
				if (explanationText != null) {
					explanationText.setText(InformationHelper.formatContentSpanned(
							getString(R.string.login_explanation_signin)));
					explanationText.setAutoLinkMask(Linkify.ALL);
					explanationText.setVisibility(View.VISIBLE);
				}
				
				if (emailEdit != null) {
					//emailEdit.setHint(R.string.label_email_hint);
					emailEdit.setVisibility(View.GONE);
				}
				
				if (usernameEdit != null) {
					usernameEdit.setHint(R.string.label_username_or_email_hint);
					usernameEdit.setVisibility(View.VISIBLE);
				}
				
				super.initLoginView();
			}
			
			@Override
			public void setHostTitle(CharSequence title) {
				TextView hostView = mHostView;
				if (hostView != null) {
					if (title == null) title = getAccountApp().getHostDisplayName();
					hostView.setText(title);
					hostView.setVisibility(View.VISIBLE);
				}
			}
			
			@Override
			public void setLogoTitle(CharSequence title) {
				TextView textView = mTitleView;
				if (textView != null && title != null && title.length() > 0)
					textView.setText(title);
			}
			
			@Override
			public void handleSelectHost() {
				new MyHostHelper(getAccountApp()).showHostDialog(getActivity());
			}
			
			@Override
			public void setAccountList() {
				if (mAccountList == null) return;
				mAccountAdapter = MyAccountItem.getListAdapter(RegisterActivity.this, getAccountApp());
				mAccountList.setAdapter(mAccountAdapter);
			}
		};
	}
	
	@Override
	protected RegisterProvider newStartupProvider() {
		return new RegisterProvider(getString(R.string.app_name), 0) { 
			@Override
			public View bindView(IActivity activity, LayoutInflater inflater,
					ViewGroup container, Bundle savedInstanceState) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("bindView: activity=" + activity + " provider=" + this 
							+ " savedInstanceState=" + savedInstanceState);
				}
				
				final View view = inflater.inflate(R.layout.register_content, null);
				final View googleBtn = view.findViewById(R.id.login_button_google);
				final ImageView logoImg = (ImageView)view.findViewById(R.id.login_image_logo);
				
				logoImg.setImageAlpha(200);
				logoImg.setScaleX(0.8f);
				logoImg.setScaleY(0.8f);
				
				googleBtn.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if (addGoogleAccount(RegisterActivity.this))
								RegisterActivity.this.finish();
						}
					});
				
				mTouchHandler.bindView(inflater, view, savedInstanceState);
				return view;
			}
			
			@Override
			public void onSaveActivityState(Bundle savedInstanceState) {
				super.onSaveActivityState(savedInstanceState);
				mTouchHandler.onSaveInstanceState(savedInstanceState);
			}
			
			@Override
			public void onRestoreActivityState(Bundle savedInstanceState) {
				super.onRestoreActivityState(savedInstanceState);
				mTouchHandler.onRestoreInstanceState(savedInstanceState);
			}
		};
	}
	
	@Override
	protected void startupMain(int flag) {
		MainActivity.actionActivity(this, flag);
	}
	
	@Override
	protected boolean checkEmail(String email, boolean registerMode) {
		if (email == null || email.length() == 0) {
			getActivityHelper().showWarningMessage(R.string.login_email_missing_message);
			return false;
		}
		
		if (registerMode) {
			if (AccountHelper.checkEmailAddress(email) == false) {
				getActivityHelper().showWarningMessage(R.string.login_email_illegal_message);
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	protected boolean checkUsername(String username, boolean registerMode) {
		if (username == null || username.length() == 0) {
			getActivityHelper().showWarningMessage(R.string.login_username_missing_message);
			return false;
		}
		
		if (registerMode) {
			if (AccountHelper.checkUserName(username) == false) {
				getActivityHelper().showWarningMessage(R.string.login_username_illegal_message);
				return false;
			}
		} else {
			//if (AnyboxHelper.checkEmailAddress(username) == false) {
			//	getActivityHelper().showWarningMessage(R.string.login_usernameemail_illegal_message);
			//	return false;
			//}
		}
		
		return true;
	}
	
	@Override
	protected boolean checkPassword(String password, boolean registerMode) {
		if (password == null || password.length() == 0) {
			getActivityHelper().showWarningMessage(R.string.login_password_missing_message);
			return false;
		}
		
		if (registerMode) {
			if (AccountHelper.checkPassword(password) == false) {
				getActivityHelper().showWarningMessage(R.string.login_password_illegal_message);
				return false;
			}
		}
		
		return true;
	}
	
}
