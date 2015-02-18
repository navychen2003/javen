package org.javenstudio.provider.app.anybox.library;

import org.javenstudio.android.app.FileOperation;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.data.IDownloadable;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.app.anybox.AnyboxDownloader;
import org.javenstudio.provider.app.anybox.AnyboxHelper;
import org.javenstudio.provider.app.anybox.AnyboxSection;
import org.javenstudio.provider.app.anybox.AnyboxSectionFile;
import org.javenstudio.provider.library.BaseSectionFileItem;
import org.javenstudio.provider.library.ISectionFolder;

public class AnyboxSectionFileItem extends BaseSectionFileItem {
	private static final Logger LOG = Logger.getLogger(AnyboxSectionFileItem.class);

	public AnyboxSectionFileItem(AnyboxLibraryProvider p, 
			AnyboxSectionFile data) {
		super(p, data);
	}
	
	@Override
	public AnyboxSection getSectionData() {
		return (AnyboxSection)super.getSectionData();
	}
	
	@Override
	public AnyboxLibraryProvider getProvider() {
		return (AnyboxLibraryProvider)super.getProvider();
	}
	
	@Override
	public FileOperation getOperation() {
		FileOperation ops = getSectionData().getOperation();
		if (ops != null) return ops;
		
		ISectionFolder folder = getSectionData().getParent();
		if (folder != null && folder instanceof AnyboxHelper.IAnyboxFolder) {
			AnyboxHelper.IAnyboxFolder fd = (AnyboxHelper.IAnyboxFolder)folder;
			return fd.getSubFileOps();
		}
		
		return null;
	}
	
	@Override
	public boolean onItemInfoClick(IActivity activity) { 
		if (activity == null || activity.isDestroyed()) return false;
		if (LOG.isDebugEnabled()) LOG.debug("onItemInfoClick: item=" + this);
		
		return getProvider().getAccountApp().openSectionDetails(activity.getActivity(), 
				getProvider().getAccountUser(), getSectionData());
	}
	
	@Override
	public boolean onItemClick(IActivity activity) {
		if (activity == null || activity.isDestroyed()) return false;
		if (LOG.isDebugEnabled()) LOG.debug("onItemClick: item=" + this);
		
		String type = getSectionData().getType();
		if (type != null) {
			if (type.startsWith("image/")) {
				return onImageItemClick(activity);
			} else if (type.startsWith("audio/")) {
				return onAudioItemClick(activity);
			} else if (type.startsWith("video/")) {
				return onVideoItemClick(activity);
			}
		}
		return true;
	}
	
	@Override
	public boolean onItemDownload(IActivity activity) { 
		if (activity == null || activity.isDestroyed()) return false;
		if (LOG.isDebugEnabled()) LOG.debug("onItemDownload: item=" + this);
		AnyboxSection data = getSectionData();
		if (data != null && data instanceof IDownloadable) {
			return AnyboxDownloader.download(activity.getActivity(), 
					getProvider().getAccountUser(), 
					new IDownloadable[]{(IDownloadable)data});
		}
		return false; 
	}
	
	protected boolean onImageItemClick(IActivity activity) { return true; }
	protected boolean onAudioItemClick(IActivity activity) { return true; }
	protected boolean onVideoItemClick(IActivity activity) { return true; }
	
}
