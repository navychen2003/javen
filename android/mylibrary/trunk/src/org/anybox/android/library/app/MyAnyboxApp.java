package org.anybox.android.library.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import org.anybox.android.library.AppActivity;
import org.anybox.android.library.MyApp;
import org.anybox.android.library.MyImplements;
import org.anybox.android.library.PhotoShowActivity;
import org.anybox.android.library.R;
import org.anybox.android.library.SettingActivity;
import org.javenstudio.android.AndroidReceiver;
import org.javenstudio.android.AndroidService;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.entitydb.content.AccountData;
import org.javenstudio.android.entitydb.content.ContentHelper;
import org.javenstudio.android.entitydb.content.HostData;
import org.javenstudio.android.information.InformationHelper;
import org.javenstudio.cocoka.android.PluginManager;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.app.ActionItem;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.Provider;
import org.javenstudio.provider.ProviderActionItem;
import org.javenstudio.provider.ProviderController;
import org.javenstudio.provider.ProviderManager;
import org.javenstudio.provider.account.AccountInfoProvider;
import org.javenstudio.provider.account.dashboard.DashboardProvider;
import org.javenstudio.provider.account.dashboard.HistorySectionItem;
import org.javenstudio.provider.account.dashboard.IHistorySectionData;
import org.javenstudio.provider.account.notify.NotifyProvider;
import org.javenstudio.provider.account.space.SpacesProvider;
import org.javenstudio.provider.app.anybox.AnyboxApp;
import org.javenstudio.provider.app.anybox.AnyboxAccount;
import org.javenstudio.provider.app.anybox.AnyboxSectionFile;
import org.javenstudio.provider.app.anybox.AnyboxSectionFolder;
import org.javenstudio.provider.app.anybox.library.AnyboxLibrariesProvider;
import org.javenstudio.provider.app.anybox.library.AnyboxLibraryOptionsMenu;
import org.javenstudio.provider.app.anybox.library.AnyboxLibraryProvider;
import org.javenstudio.provider.app.anybox.library.AnyboxPhotoSet;
import org.javenstudio.provider.app.anybox.library.AnyboxSectionInfoItem;
import org.javenstudio.provider.app.anybox.library.AnyboxSectionInfoProvider;
import org.javenstudio.provider.app.anybox.library.AnyboxSectionOptionsMenu;
import org.javenstudio.provider.app.anybox.library.AnyboxSelectCreate;
import org.javenstudio.provider.app.anybox.user.AnyboxAccountDetails;
import org.javenstudio.provider.app.anybox.user.AnyboxAccountItem;
import org.javenstudio.provider.app.anybox.user.AnyboxAccountOptionsMenu;
import org.javenstudio.provider.app.anybox.user.AnyboxAccountProvider;
import org.javenstudio.provider.app.anybox.user.AnyboxDashboardItem;
import org.javenstudio.provider.app.anybox.user.AnyboxDashboardProvider;
import org.javenstudio.provider.app.anybox.user.AnyboxNotifyProvider;
import org.javenstudio.provider.app.anybox.user.AnyboxOverviewSpaceProvider;
import org.javenstudio.provider.app.anybox.user.AnyboxUsedSpaceProvider;
import org.javenstudio.provider.app.anybox.user.AnyboxSpacesProvider;
import org.javenstudio.provider.app.picasa.PicasaUploader;
import org.javenstudio.provider.library.ILibraryData;
import org.javenstudio.provider.library.ISectionInfoData;
import org.javenstudio.provider.library.ISectionList;
import org.javenstudio.provider.library.details.SectionInfoBinder;
import org.javenstudio.provider.library.details.SectionInfoItem;
import org.javenstudio.provider.library.list.LibrariesProvider;
import org.javenstudio.provider.library.list.LibraryProvider;
import org.javenstudio.provider.library.section.SectionListItem;
import org.javenstudio.provider.task.TaskListBinder;
import org.javenstudio.provider.task.TaskListProvider;
import org.javenstudio.provider.task.download.DownloadHandler;
import org.javenstudio.provider.task.download.DownloadItem;
import org.javenstudio.provider.task.download.DownloadProvider;
import org.javenstudio.provider.task.upload.UploadHandler;
import org.javenstudio.provider.task.upload.UploadItem;
import org.javenstudio.provider.task.upload.UploadNotification;
import org.javenstudio.provider.task.upload.UploadProvider;

public class MyAnyboxApp extends AnyboxApp {
	private static final Logger LOG = Logger.getLogger(MyAnyboxApp.class);

	private static final String ANYBOX_ADDRESS = "www.anybox.org";
	private final MyApp mApp;
	
	private UploadHandler mUploadHandler = null;
	private DownloadHandler mDownloadHandler = null;
	
	public MyAnyboxApp(MyApp app) {
		this(app, ANYBOX_ADDRESS);
	}
	
	public MyAnyboxApp(MyApp app, String hostAddress) { 
		super(hostAddress);
		if (app == null) throw new NullPointerException();
		mApp = app;
	}
	
	public MyApp getDataApp() { return mApp; }
	public Context getContext() { return mApp.getContext(); }
	
	@Override
	public String getAnyboxSiteUrl() {
		return "http://" + ANYBOX_ADDRESS; 
	}
	
	public void onInitialized(Context context) { 
		AndroidService.registerHandler(getUploadHandler()); 
		AndroidReceiver.registerHandler(getUploadHandler());
		
		HostData host = queryLastHost();
		if (host != null) {
			setHostDisplayName(host.getDisplayName());
			setHostAddressPort(host.getRequestAddressPort());
		}
	}
	
	private HostData queryLastHost() {
		HostData[] hosts = ContentHelper.getInstance().queryHosts();
		HostData hostLast = null;
		if (hosts != null) {
			for (HostData host : hosts) {
				if (host == null || host.getFailedCode() != 0) continue;
				if (hostLast == null || hostLast.getUpdateTime() < host.getUpdateTime())
					hostLast = host;
			}
		}
		return hostLast;
	}
	
	@Override
	public PluginManager.PackageInfo getPluginInfo(Context context) {
		final Resources res = context.getResources();
		
		PluginManager.PackageInfo packageInfo = new PluginManager.PackageInfo(
				new ComponentName(MyImplements.PACKAGE_NAME, ""),
				res.getString(R.string.plugin_anybox_name),
				res.getString(R.string.app_versionname),
				res.getDrawable(R.drawable.ic_home_anybox)
			);
		
		String summary = res.getString(R.string.about_message);
		summary = String.format(summary, res.getString(R.string.app_versionname));
		packageInfo.setSummary(InformationHelper.formatContentSpanned(summary));
		packageInfo.setSmallIcon(context.getResources().getDrawable(R.drawable.nav_anybox_selector));
		
		return packageInfo;
	}
	
	public synchronized UploadHandler getUploadHandler() { 
		if (mUploadHandler == null) { 
			UploadNotification notifier = new UploadNotification(this, 
					new ComponentName(MyImplements.PACKAGE_NAME, AndroidReceiver.class.getName()),
					MyApp.NOTIFICATIONID_UPLOAD, 
					getContext().getString(R.string.app_name),
					R.drawable.ic_nav_anybox_dark);
			
			UploadHandler handler = new UploadHandler(this, notifier) { 
					@Override
					protected void launchUploadList(Context context) { 
						AnyboxAccount account = MyAnyboxApp.this.getAccount();
						if (account != null) { 
							AppActivity.actionActivity(context, 
									account.getTaskListProvider());
						}
					}
				};
			
			handler.addUploader(new PicasaUploader(getDataApp(), handler, R.drawable.ic_nav_anybox));
			
			mUploadHandler = handler;
		}
		return mUploadHandler;
	}
	
	public synchronized DownloadHandler getDownloadHandler() {
		if (mDownloadHandler == null) {
			DownloadHandler handler = new DownloadHandler();
			mDownloadHandler = handler;
		}
		return mDownloadHandler;
	}
	
	public synchronized TaskListProvider createTaskListProvider(AnyboxAccount user) {
		TaskListProvider group = new TaskListProvider(this, user, 
				R.string.tasks_name, R.drawable.ic_home_task_dark, 
				R.drawable.ic_ab_back_holo_dark) {
				@Override
				public boolean overridePendingTransitionOnFinish(IActivity activity) { 
					activity.overridePendingTransition(R.anim.setting_left_enter, R.anim.activity_right_exit);
					return true; 
				}
				@Override
				protected void onActionTitleBinded(ProviderActionItem item, 
						View view, View subview, boolean dropdown) {
					MyAnyboxApp.this.onActionTitleBinded(item, view, subview, dropdown);
				}
				@Override
				public void setContentBackground(IActivity activity) {
					MyBinderHelper.setBackgroundDefault(activity);
				}
				@Override
				protected TaskListBinder createTaskListBinder() {
					return new MyTaskListBinder(this);
				}
			};
		
		group.addProvider(new UploadProvider(group, getUploadHandler(), 
				ResourceHelper.getResources().getString(R.string.upload_tasks_title), 
				R.drawable.ic_home_task_dark) {
				@Override
				public UploadItem createEmptyItem() {
					return new MyTaskListBinder.UploadEmptyItem(this);
				}
			});
		
		group.addProvider(new DownloadProvider(group, getDownloadHandler(), 
				ResourceHelper.getResources().getString(R.string.download_tasks_title), 
				R.drawable.ic_home_task_dark) {
				@Override
				public DownloadItem createEmptyItem() {
					return new MyTaskListBinder.DownloadEmptyItem(this);
				}
			});
		
		return group;
	}
	
	@Override
	public AnyboxAccount createAccount(AccountData account, 
			HostData host, long authTime) {
		return new MyAnyboxAccount(this, account, host, authTime);
	}
	
	@Override
	public AccountInfoProvider createAccountProvider(AnyboxAccount user) {
		if (user == null) return null;
		
		AnyboxAccountProvider p = new AnyboxAccountProvider(this, user, 
					R.drawable.ic_home_anybox_dark, R.drawable.ic_ab_menu_holo_light) {
				@Override
				protected MyAccountBinder createAccountBinder() {
					return new MyAccountBinder(this);
				}
				@Override
				protected MyAccountMenuBinder createAccountMenuBinder() {
					return new MyAccountMenuBinder(this);
				}
				@Override
				protected MySecondaryMenuBinder createAccountSecondaryMenuBinder() {
					return new MySecondaryMenuBinder(this);
				}
				@Override
				public Drawable getActionBarBackground(IActivity activity) { 
					return activity.getResources().getDrawable(R.drawable.bg_account_actionbar); 
				}
				@Override
				public int getActionBarTitleColorRes(IActivity activity) { 
					return R.color.white; 
				}
				@Override
				public int getHomeAsUpIndicatorRes() {
					return getBinder().getHomeAsUpIndicatorMenuRes();
				}
				@Override
				protected AnyboxAccountItem createAccountItem(AnyboxAccount account) {
					return new AnyboxAccountItem(this, account, new AnyboxAccountItem.AccountFactory() {
							@Override
							public AnyboxAccountDetails.AnyboxDetailsBasic createDetailsBasic(AnyboxAccountItem item) {
								return new MyAccountDetailsBasic(item);
							}
							@Override
							public AnyboxAccountDetails.AnyboxDetailsBrief createDetailsBrief(AnyboxAccountItem item) {
								return new MyAccountDetailsBrief(item);
							}
						});
				}
			};
		
		p.setOptionsMenu(createAccountOptionsMenu(this));
		return p;
	}
	
	@Override
	public SpacesProvider createSpacesProvider(AnyboxAccount user) {
		if (user == null) return null;
		
		AnyboxSpacesProvider group = new AnyboxSpacesProvider(this, user, 
				R.string.storagespace_title, R.drawable.ic_home_anybox_dark,
				R.drawable.ic_ab_menu_holo_light, null) { 
				@Override
				protected void onActionTitleBinded(ProviderActionItem item, 
						View view, View subview, boolean dropdown) {
					MyAnyboxApp.this.onActionTitleBinded(item, view, subview, dropdown);
				}
				@Override
				public void setContentBackground(IActivity activity) {
					MyBinderHelper.setBackgroundDefault(activity);
				}
			};
		
		group.setOptionsMenu(createAccountOptionsMenu(this));
		group.addProvider(new AnyboxOverviewSpaceProvider(group, 
				R.string.overviewspace_title, R.drawable.ic_home_anybox_dark, 
				R.drawable.ic_ab_menu_holo_light));
		group.addProvider(new AnyboxUsedSpaceProvider(group, 
				R.string.usedspace_title, R.drawable.ic_home_anybox_dark, 
				R.drawable.ic_ab_menu_holo_light));
		
		return group;
	}

	private void onActionTitleBinded(ProviderActionItem item, 
			View view, View subview, boolean dropdown) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("onActionTitleBinded: item=" + item + " view=" + view 
					+ " dropdown=" + dropdown);
		}
		if (view != null && view instanceof TextView) {
			TextView textView = (TextView)view;
			if (dropdown == false) {
				textView.setTextColor(ResourceHelper.getResources().getColor(
						R.color.white));
			}
		}
		if (subview != null && subview instanceof TextView) {
			TextView textView = (TextView)subview;
			if (dropdown == false) {
				textView.setTextColor(ResourceHelper.getResources().getColor(
						R.color.white75));
			}
		}
	}
	
	@Override
	public NotifyProvider createNotifyProvider(AnyboxAccount user) {
		if (user == null) return null;
		
		return new AnyboxNotifyProvider(user, 
				R.string.notification_title, R.drawable.ic_ab_back_holo_light,
				MyNotifyBinder.FACTORY);
	}
	
	@Override
	public DashboardProvider createDashboardProvider(AnyboxAccount user) {
		AnyboxDashboardProvider p = new AnyboxDashboardProvider(user, 
				R.string.dashboard_title, R.drawable.ic_home_anybox_dark, 
				R.drawable.ic_ab_menu_holo_light, MyDashboardFactory.FACTORY) {
				@Override
				public int getHomeAsUpIndicatorRes() {
					return getBinder().getHomeAsUpIndicatorMenuRes();
				}
				@Override
				protected HistorySectionItem createSectionItem(IHistorySectionData data) {
					if (data == null) return null;
					return new AnyboxDashboardItem(this, data) {
							@Override
							protected void onImageClick(IActivity activity) {
								if (activity == null || activity.isDestroyed()) return;
								AnyboxPhotoSet photoSet = new AnyboxPhotoSet(getDashboardSections(), getData()) { 
										@Override
										public AnyboxAccount getAccountUser() {
											return (AnyboxAccount)getProvider().getAccountUser();
										}
										@Override
										public Drawable getProviderIcon() { 
											return ResourceHelper.getResources().getDrawable(R.drawable.ic_nav_anybox);
										}
									};
								PhotoShowActivity.actionShow(activity.getActivity(), photoSet);
							}
						};
				}
			};
		
		p.setNavigationIconRes(R.drawable.nav_home_selector);
		p.setNavigationTitle(ResourceHelper.getResources().getString(R.string.dashboard_navigation_title));
		p.setOptionsMenu(createAccountOptionsMenu(this));
		
		return p;
	}

	@Override
	public LibrariesProvider createLibrariesProvider(AnyboxAccount user) {
		AnyboxLibrariesProvider group = new AnyboxLibrariesProvider(user, 
				R.string.library_title, R.drawable.ic_home_anybox_dark, 
				R.drawable.ic_ab_menu_holo_light) {
				@Override
				protected void onActionTitleBinded(ProviderActionItem item, 
						View view, View subview, boolean dropdown) {
					MyAnyboxApp.this.onActionTitleBinded(item, view, subview, dropdown);
				}
			};
		
		group.setNavigationIconRes(R.drawable.nav_library_selector);
		group.setNavigationTitle(ResourceHelper.getResources().getString(R.string.library_navigation_title));
		group.setOptionsMenu(createLibraryOptionsMenu(this));
		
		return group;
	}
	
	@Override
	public LibraryProvider createLibraryProvider(AnyboxAccount user, ILibraryData data) {
		AnyboxLibraryProvider p = new AnyboxLibraryProvider(user, data,
				data.getName(), R.drawable.ic_home_anybox_dark, MyLibraryFactory.FACTORY) { 
				@Override
				public int getHomeAsUpIndicatorRes() {
					ISectionList list = getSectionList();
					if (list == null || list instanceof ILibraryData) 
						return getBinder().getHomeAsUpIndicatorMenuRes(); //R.drawable.ic_ab_menu_holo_dark;
					return getBinder().getHomeAsUpIndicatorBackRes(); //R.drawable.ic_ab_back_holo_dark;
				}
				@Override
				public void onSearchViewOpen(IActivity activity, View view) { 
					super.onSearchViewOpen(activity, view);
					activity.getActionHelper().setHomeAsUpIndicator(getBinder().getHomeAsUpIndicatorBackRes()); //R.drawable.ic_ab_back_holo_dark);
				}
				@Override
				public void onSearchViewClose(IActivity activity, View view) { 
					super.onSearchViewClose(activity, view);
					activity.getActionHelper().setHomeAsUpIndicator(getHomeAsUpIndicatorRes());
				}
				@Override
				protected AnyboxSelectCreate getSelectCreate() {
					return new MySelectCreate(getAccountUser());
				}
				@Override
				protected SectionListItem createFileItem(AnyboxSectionFile data) {
					return new MySectionFileItem(this, data, getSectionList());
				}
				@Override
				protected SectionListItem createFolderItem(AnyboxSectionFolder data) {
					return new MySectionFolderItem(this, data);
				}
			};
		
		//p.setOptionsMenu(createLibraryOptionsMenu(this));
		return p;
	}
	
	@Override
	public boolean openSectionDetails(Activity activity, 
			AnyboxAccount user, ISectionInfoData data) {
		if (activity == null || user == null || data == null)
			return false;
		
		AnyboxSectionInfoProvider p = new AnyboxSectionInfoProvider(user, 
			data.getId(), R.drawable.ic_home_anybox_dark, R.drawable.ic_ab_back_holo_dark, data) { 
				@Override
				protected SectionInfoBinder createDetailsBinder() {
					return new MySectionInfoBinder(this);
				}
				@Override
				protected SectionInfoItem createSectionItem(ISectionInfoData data) {
					return new AnyboxSectionInfoItem(this, data, MySectionInfoBinder.FACTORY);
				}
			};
		
		p.setOptionsMenu(createSectionOptionsMenu(this));
		
		AppActivity.actionActivity(activity, p);
		activity.overridePendingTransition(R.anim.activity_right_enter, R.anim.activity_fade_exit);
		
		return true;
	}
	
	@Override
	public ProviderController getNavigationController(Activity activity) {
		return ProviderManager.getListController();
	}

	@Override
	public ActionItem[] getNavigationItems(Activity activity) {
		AnyboxAccount account = getAccount();
		if (account != null) return account.getNavigationItems(activity);
		return null;
	}
	
	@Override
	public Provider getCurrentProvider(Activity activity) {
		synchronized (mProviderLock) {
			return mCurrentProvider;
		}
	}

	@Override
	public Provider setCurrentProvider(Activity activity, Provider provider) {
		if (LOG.isDebugEnabled()) 
			LOG.debug("setCurrentProvider: provider=" + provider);
		
		synchronized (mProviderLock) {
			Provider old = mCurrentProvider;
			mCurrentProvider = provider;
			return old;
		}
	}
	
	@Override
	protected void onAccountChanged(AnyboxAccount user, AnyboxAccount old) {
		super.onAccountChanged(user, old);
		
		synchronized (mProviderLock) {
			ProviderManager.getListController().setProvider(null);
			ProviderManager.clearProviders();
			mCurrentProvider = null;
		}
	}
	
	private final Object mProviderLock = new Object();
	private Provider mCurrentProvider = null;
	
	private AnyboxAccountOptionsMenu createAccountOptionsMenu(AnyboxApp app) {
		return new AnyboxAccountOptionsMenu(app) {
				@Override
				protected boolean onSettingItemSelected(Activity activity) {
					if (activity != null) 
						SettingActivity.actionSettings(activity);
					return true;
				}
			};
	}
	
	private AnyboxLibraryOptionsMenu createLibraryOptionsMenu(AnyboxApp app) {
		return new AnyboxLibraryOptionsMenu(app);
	}
	
	private AnyboxSectionOptionsMenu createSectionOptionsMenu(AnyboxApp app) {
		return new AnyboxSectionOptionsMenu(app);
	}
	
}
