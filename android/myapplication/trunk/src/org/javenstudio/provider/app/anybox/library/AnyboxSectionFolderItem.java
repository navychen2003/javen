package org.javenstudio.provider.app.anybox.library;

import org.javenstudio.android.app.FileOperation;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.app.anybox.AnyboxHelper;
import org.javenstudio.provider.app.anybox.AnyboxSectionFolder;
import org.javenstudio.provider.library.BaseSectionFolderItem;
import org.javenstudio.provider.library.ISectionFolder;

public class AnyboxSectionFolderItem extends BaseSectionFolderItem {
	private static final Logger LOG = Logger.getLogger(AnyboxSectionFolderItem.class);

	public AnyboxSectionFolderItem(AnyboxLibraryProvider p, 
			AnyboxSectionFolder data) {
		super(p, data);
	}
	
	@Override
	public AnyboxSectionFolder getSectionData() {
		return (AnyboxSectionFolder)super.getSectionData();
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
			return fd.getSubFolderOps();
		}
		
		return null;
	}
	
	@Override
	public boolean onItemClick(IActivity activity) {
		if (activity == null || activity.isDestroyed()) return false;
		if (LOG.isDebugEnabled()) LOG.debug("onItemClick: item=" + this);
		
		getProvider().setSectionList(activity, getSectionData(), true);
		return true;
	}
	
	@Override
	public boolean onItemInfoClick(IActivity activity) { 
		if (activity == null || activity.isDestroyed()) return false;
		if (LOG.isDebugEnabled()) LOG.debug("onItemInfoClick: item=" + this);
		
		return getProvider().getAccountApp().openSectionDetails(activity.getActivity(), 
				getProvider().getAccountUser(), getSectionData());
	}
	
}
