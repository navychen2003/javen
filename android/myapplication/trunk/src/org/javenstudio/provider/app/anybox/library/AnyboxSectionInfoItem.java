package org.javenstudio.provider.app.anybox.library;

import java.util.ArrayList;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.data.IDownloadable;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.app.anybox.AnyboxApp;
import org.javenstudio.provider.app.anybox.AnyboxDownloader;
import org.javenstudio.provider.app.anybox.AnyboxHelper;
import org.javenstudio.provider.app.anybox.AnyboxLibrary;
import org.javenstudio.provider.app.anybox.AnyboxSection;
import org.javenstudio.provider.library.BaseSectionInfoItem;
import org.javenstudio.provider.library.ISectionInfoData;
import org.javenstudio.provider.library.details.SectionActionTab;

public class AnyboxSectionInfoItem extends BaseSectionInfoItem {
	private static final Logger LOG = Logger.getLogger(AnyboxSectionInfoItem.class);

	public static interface SectionInfoFactory 
			extends AnyboxSectionProperty.PropertyFactory, 
			AnyboxSectionMedia.MediaFactory, 
			AnyboxSectionMetadata.MetadataFactory {
	}
	
	private final SectionInfoFactory mFactory;
	private SectionActionTab[] mActions = null;
	
	public AnyboxSectionInfoItem(AnyboxSectionInfoProvider p, 
			ISectionInfoData data, SectionInfoFactory factory) { 
		super(p, data);
		mFactory = factory;
	}
	
	@Override
	public AnyboxApp getAccountApp() {
		return (AnyboxApp)super.getAccountApp();
	}

	public AnyboxHelper.IRequestWrapper getRequestWrapper() {
		ISectionInfoData data = getSectionData();
		if (data != null) {
			if (data instanceof AnyboxLibrary) {
				return ((AnyboxLibrary)data).getRequestWrapper();
			} else if (data instanceof AnyboxSection) {
				return ((AnyboxSection)data).getRequestWrapper();
			}
		}
		return null;
	}
	
	protected AnyboxSectionProperty createSectionProperty(IActivity activity) {
		return new AnyboxSectionProperty(this, mFactory);
	}
	
	protected AnyboxSectionMedia createSectionMedia(IActivity activity) {
		return new AnyboxSectionMedia(this, mFactory);
	}
	
	protected AnyboxSectionMetadata createSectionMetadata(IActivity activity) {
		return new AnyboxSectionMetadata(this, mFactory);
	}
	
	@Override
	public synchronized SectionActionTab[] getActionItems(IActivity activity) { 
		if (mActions == null) { 
			ArrayList<SectionActionTab> tabs = new ArrayList<SectionActionTab>();
			if (hasPropertyDetails()) {
				AnyboxSectionProperty tab = createSectionProperty(activity);
				if (tab != null) tabs.add(tab);
			}
			if (hasMediaDetails()) {
				AnyboxSectionMedia tab = createSectionMedia(activity);
				if (tab != null) tabs.add(tab);
			}
			if (hasMetadataDetails()) {
				AnyboxSectionMetadata tab = createSectionMetadata(activity);
				if (tab != null) tabs.add(tab);
			}
			mActions = tabs.toArray(new SectionActionTab[tabs.size()]);
		}
		return mActions; 
	}
	
	@Override
	public SectionActionTab getSelectedAction() { 
		SectionActionTab[] actions = mActions;
		if (actions != null) { 
			for (SectionActionTab action : actions) { 
				if (action.isSelected())
					return action;
			}
		}
		return null; 
	}
	
	public boolean isMediaData() {
		if (getSectionData().isFolder()) return false;
		String type = getSectionData().getType();
		if (type != null) {
			if (type.startsWith("image/") || type.startsWith("audio") || 
				type.startsWith("video")) {
				return true;
			}
		}
		return false;
	}
	
	public boolean hasMediaDetails() { return isMediaData(); }
	public boolean hasMetadataDetails() { return !getSectionData().isFolder() && !isMediaData(); }
	public boolean hasPropertyDetails() { return true; }
	
	@Override
	public boolean onItemDownload(IActivity activity) { 
		if (activity == null || activity.isDestroyed()) return false;
		if (LOG.isDebugEnabled()) LOG.debug("onItemDownload: item=" + this);
		ISectionInfoData data = getSectionData();
		if (data != null && data instanceof IDownloadable) {
			return AnyboxDownloader.download(activity.getActivity(), 
					getAccountApp().getAccount(), 
					new IDownloadable[]{(IDownloadable)data});
		}
		return false; 
	}
	
}
