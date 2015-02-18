package org.javenstudio.provider.app.anybox.library;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.FileOperation;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.provider.library.BaseSectionDetailsTab;
import org.javenstudio.provider.library.BaseSectionDetailsItem;
import org.javenstudio.provider.library.ISectionInfoData;
import org.javenstudio.provider.library.details.SectionDetailsItem;
import org.javenstudio.provider.library.details.SectionInfoItem;

public class AnyboxSectionProperty extends BaseSectionDetailsTab {

	public static interface PropertyFactory {
		public SectionDetailsItem createPropertyBasic(SectionInfoItem item);
	}
	
	private final AnyboxSectionInfoItem mSection;
	
	public AnyboxSectionProperty(AnyboxSectionInfoItem item, PropertyFactory factory) { 
		super(item, ResourceHelper.getResources().getString(R.string.label_action_property));
		mSection = item;
		addDetailsItem(factory.createPropertyBasic(item));
	}
	
	public AnyboxSectionInfoItem getSectionItem() { return mSection; }
	
	@Override
	public CharSequence getLastUpdatedLabel(IActivity activity) {
		ISectionInfoData data = getSectionItem().getSectionData();
		if (data != null) {
			long requestTime = data.getRefreshTime();
			return AppResources.getInstance().formatRefreshTime(requestTime);
		}
		return null;
	}
	
	public static class AnyboxSectionPropertyBasic extends BaseSectionDetailsItem { 
		public AnyboxSectionPropertyBasic(SectionInfoItem item, int nameRes) { 
			super(nameRes);
			
			ISectionInfoData data = item.getSectionData();
			String owner = data.getOwner();
			if (owner == null || owner.length() == 0)
				owner = item.getAccountUser().getAccountName();
			
			SectionDetailsItem.NameValue itemName = addNameValue(R.string.details_filename, data.getName());
			SectionDetailsItem.NameValue itemPath = addNameValue(R.string.details_filepath, data.getPath());
			SectionDetailsItem.NameValue itemType = addNameValue(R.string.details_contenttype, data.getType());
			SectionDetailsItem.NameValue itemOwner = addNameValue(R.string.details_owner, owner);
			SectionDetailsItem.NameValue itemSize = addNameValue(R.string.details_filesize, data.getSizeDetails());
			SectionDetailsItem.NameValue itemModified = addNameValue(R.string.details_lastmodified, 
					AppResources.getInstance().formatReadableTime(data.getModifiedTime()));
			SectionDetailsItem.NameValue itemChecksum = addNameValue(R.string.details_checksum, data.getChecksum());
			
			if (itemName != null) itemName.setEditable(data.supportOperation(FileOperation.Operation.MODIFY));
			if (itemPath != null) itemPath.setEditable(false);
			if (itemType != null) itemType.setEditable(false);
			if (itemOwner != null) itemOwner.setEditable(false);
			if (itemSize != null) itemSize.setEditable(false);
			if (itemModified != null) itemModified.setEditable(false);
			if (itemChecksum != null) itemChecksum.setEditable(false);
		}
	}
	
}
