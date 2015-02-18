package org.javenstudio.provider.app.anybox.library;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.app.anybox.AnyboxAccount;
import org.javenstudio.provider.app.anybox.AnyboxApp;
import org.javenstudio.provider.app.anybox.AnyboxProperty;
import org.javenstudio.provider.library.BaseSectionDetailsItem;
import org.javenstudio.provider.library.BaseSectionDetailsTab;
import org.javenstudio.provider.library.INameValue;
import org.javenstudio.provider.library.ISectionInfoData;
import org.javenstudio.provider.library.details.SectionDetailsItem;
import org.javenstudio.provider.library.details.SectionInfoItem;

public class AnyboxSectionMedia extends BaseSectionDetailsTab {

	public static interface MediaFactory {
		public SectionDetailsItem createMediaInfos(SectionInfoItem item, 
				INameValue[] nameValues);
		public SectionDetailsItem[] createMediaTags(SectionInfoItem item, 
				INameValue[] nameValues);
	}
	
	private final AnyboxSectionInfoItem mSection;
	private final MediaFactory mFactory;
	
	public AnyboxSectionMedia(AnyboxSectionInfoItem item, MediaFactory factory) { 
		super(item, ResourceHelper.getResources().getString(R.string.label_action_media));
		mSection = item;
		mFactory = factory;
	}
	
	public AnyboxSectionInfoItem getSectionItem() { return mSection; }
	public MediaFactory getFactory() { return mFactory; }
	
	public AnyboxApp getAccountApp() { return getSectionItem().getAccountApp(); }
	public AnyboxAccount getAccountUser() { return (AnyboxAccount)getSectionItem().getAccountUser(); }
	
	@Override
	public CharSequence getLastUpdatedLabel(IActivity activity) {
		long requestTime = 0;
		AnyboxProperty property = getAccountApp().getSectionProperty(
				getSectionItem().getSectionId());
		if (property != null) requestTime = property.getRequestTime();
		if (requestTime <= 0) {
			ISectionInfoData data = getSectionItem().getSectionData();
			if (data != null) requestTime = data.getRefreshTime();
		}
		return AppResources.getInstance().formatRefreshTime(requestTime);
	}
	
	@Override
	public synchronized void reloadOnThread(ProviderCallback callback, 
			ReloadType type, long reloadId) {
		if (type == ReloadType.FORCE || getDetailsItemCount() == 0) {
			AnyboxProperty property = getAccountApp().getSectionProperty(
					getSectionItem().getSectionId());
			if (type == ReloadType.FORCE || property == null) {
				property = AnyboxProperty.getProperty(getAccountUser(), 
						getSectionItem().getRequestWrapper(), callback, 
						getSectionItem().getSectionId());
			}
			updateDetailsItems(property);
		}
	}
	
	protected void updateDetailsItems(AnyboxProperty property) {
		if (property == null) return;
		clearDetailsItems();
		addDetailsItem(getFactory().createMediaInfos(getSectionItem(), 
				property.getMediaInfos()));
		addDetailsItems(getFactory().createMediaTags(getSectionItem(), 
				property.getMediaTags()));
		postUpdateViews();
	}
	
	public static class AnyboxSectionMediaTags extends BaseSectionDetailsItem { 
		public AnyboxSectionMediaTags(SectionInfoItem item, int nameRes, 
				INameValue[] nameValues) { 
			super(nameRes);
			
			for (int i=0; nameValues != null && i < nameValues.length; i++) {
				INameValue nameValue = nameValues[i];
				if (nameValue == null) continue;
				
				SectionDetailsItem.NameValue nameItem = addNameValue(
						nameValue.getName(), nameValue.getValue());
				if (nameItem != null) nameItem.setEditable(false);
			}
		}
	}
	
}
