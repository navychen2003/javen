package org.javenstudio.provider.app.picasa;

import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.account.SystemUser;
import org.javenstudio.android.account.AccountUser;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.account.AccountInfoBinder;
import org.javenstudio.provider.app.BaseAccountInfoProvider;
import org.javenstudio.provider.media.album.AlbumSource;

public class PicasaAccountProvider extends BaseAccountInfoProvider {
	private static final Logger LOG = Logger.getLogger(PicasaAccountProvider.class);
	
	private final AccountApp mApplication;
	private final PicasaAccount mAccount;
	private final PicasaAccountBinder mBinder;
	private long mDataVersion = 0;
	
	public PicasaAccountProvider(AccountApp app, SystemUser account, int iconRes, 
			PicasaUserClickListener listener, PicasaAlbumProvider.PicasaAlbumFactory factory) { 
		super(app, account.getAccountName(), iconRes);
		mApplication = app;
		mAccount = new PicasaAccount(this, account, iconRes, listener, factory);
		mBinder = new PicasaAccountBinder(this);
	}
	
	public AccountApp getApplication() { return mApplication; }
	public PicasaAccount getAccountItem() { return mAccount; }
	public AccountUser getAccountUser() { return getAccountItem().getAccountUser(); }
	
	public PicasaCommentProvider getCommentProvider() { return getAccountItem().getCommentProvider(); }
	public PicasaAlbumProvider getAlbumProvider() { return getAccountItem().getAlbumProvider(); }
	public AlbumSource getAlbumSource() { return getAlbumProvider().getSource(); }

	@Override
	public AccountInfoBinder getBinder() {
		return mBinder;
	}
	
	@Override
	public void reloadOnThread(ProviderCallback callback, ReloadType type, long reloadId) { 
		super.reloadOnThread(callback, type, reloadId);
		
		long dataVersion = ((PicasaMediaSet)getAlbumProvider().getAlbumSet()).getVersion();
		if (dataVersion != mDataVersion) { 
			mDataVersion = dataVersion;
			
			if (LOG.isDebugEnabled())
				LOG.debug("reloadOnThread: postUpdateViews, dataVersion=" + dataVersion);
			
			getAccountItem().postUpdateView();
		}
	}
	
}
