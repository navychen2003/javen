package org.javenstudio.provider.app.anybox.library;

import android.app.Activity;

import org.javenstudio.android.data.IDownloadable;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.account.dashboard.IHistorySectionData;
import org.javenstudio.provider.app.anybox.AnyboxAccount;
import org.javenstudio.provider.app.anybox.AnyboxDownloader;
import org.javenstudio.provider.app.anybox.AnyboxHistory;
import org.javenstudio.provider.app.anybox.AnyboxSectionFile;
import org.javenstudio.provider.library.IPhotoData;
import org.javenstudio.provider.library.ISectionInfoData;
import org.javenstudio.provider.library.ISectionList;
import org.javenstudio.provider.library.SectionPhotoItem;
import org.javenstudio.provider.library.SectionPhotoSet;

public abstract class AnyboxPhotoSet extends SectionPhotoSet {
	private static final Logger LOG = Logger.getLogger(AnyboxPhotoSet.class);

	public AnyboxPhotoSet(AnyboxHistory.SectionData[] list, 
			IHistorySectionData data) {
		super(list, data);
	}
	
	public AnyboxPhotoSet(ISectionList list, AnyboxSectionFile data) {
		super(list, data);
	}
	
	public abstract AnyboxAccount getAccountUser();
	
	@Override
	protected SectionPhotoItem createPhotoItem(IPhotoData data) {
		if (data == null) return null;
		if (data instanceof AnyboxSectionFile) {
			return new AnyboxSectionPhotoItem(this, (AnyboxSectionFile)data);
		} else if (data instanceof AnyboxHistory.SectionData) {
			return new AnyboxDashboardPhotoItem(this, (AnyboxHistory.SectionData)data);
		}
		return null;
	}
	
	@Override
	public boolean onActionDetails(Activity activity, IPhotoData data) {
		if (activity == null || data == null) return false;
		if (LOG.isDebugEnabled()) LOG.debug("onActionDetails: data=" + data);
		if (data instanceof ISectionInfoData) {
			return getAccountUser().getApp().openSectionDetails(activity, 
					getAccountUser(), (ISectionInfoData)data);
		}
		return false;
	}
	
	@Override
	public boolean onActionDownload(Activity activity, IPhotoData data) {
		if (activity == null || data == null) return false;
		if (LOG.isDebugEnabled()) LOG.debug("onActionDownload: data=" + data);
		if (data != null && data instanceof IDownloadable) {
			return AnyboxDownloader.download(activity, getAccountUser(), 
					new IDownloadable[]{(IDownloadable)data});
		}
		return false;
	}
	
}
