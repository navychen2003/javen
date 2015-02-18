package org.javenstudio.provider.app.anybox.library;

import java.util.ArrayList;

import org.javenstudio.android.app.R;
import org.javenstudio.provider.library.ILibraryData;
import org.javenstudio.provider.library.INameValue;
import org.javenstudio.provider.library.ISectionInfoData;
import org.javenstudio.provider.library.details.SectionDetailsItem;
import org.javenstudio.provider.library.details.SectionInfoItem;

public class AnyboxSectionInfoFactory implements AnyboxSectionInfoItem.SectionInfoFactory {
	
	public int getMaxTags() { return 10; }
	public int getMaxTagValueLength() { return 1000; }
	
	public int getDetailsCardBackgroundRes() { return 0; }
	public int getDetailsCardItemBackgroundRes() { return 0; }
	
	public int getTagValueLength(INameValue nameValue) {
		CharSequence value = nameValue != null ? nameValue.getValue() : null;
		return value != null ? value.length() : 0;
	}
	
	@Override
	public SectionDetailsItem createPropertyBasic(SectionInfoItem item) {
		if (item == null) return null;
		ISectionInfoData data = item.getSectionData();
		int nameRes = R.string.sectioninfo_details_file_title;
		if (data != null) {
			if (data instanceof ILibraryData) { 
				nameRes = R.string.sectioninfo_details_library_title;
			} else if (data.isFolder()) { 
				nameRes = R.string.sectioninfo_details_folder_title;
			} else {
				String type = data.getType();
				if (type != null) {
					if (type.startsWith("image/"))
						nameRes = R.string.sectioninfo_details_image_title;
					else if (type.startsWith("audio/"))
						nameRes = R.string.sectioninfo_details_audio_title;
					else if (type.startsWith("video/"))
						nameRes = R.string.sectioninfo_details_video_title;
				}
			}
		}
		return new AnyboxSectionProperty.AnyboxSectionPropertyBasic(item, nameRes) { 
				@Override
				protected int getCardBackgroundRes() {
					int backgroundRes = getDetailsCardBackgroundRes();
					if (backgroundRes != 0) return backgroundRes;
					return super.getCardBackgroundRes();
				}
				@Override
				protected int getCardItemBackgroundRes() {
					int backgroundRes = getDetailsCardItemBackgroundRes();
					if (backgroundRes != 0) return backgroundRes;
					return super.getCardItemBackgroundRes();
				}
			};
	}
	
	protected SectionDetailsItem createMediaTags(SectionInfoItem item, 
			int nameRes, INameValue[] nameValues) {
		if (item == null || nameValues == null || nameValues.length == 0)
			return null;
		return new AnyboxSectionMedia.AnyboxSectionMediaTags(item, nameRes, nameValues) { 
				@Override
				protected int getCardBackgroundRes() {
					int backgroundRes = getDetailsCardBackgroundRes();
					if (backgroundRes != 0) return backgroundRes;
					return super.getCardBackgroundRes();
				}
				@Override
				protected int getCardItemBackgroundRes() {
					int backgroundRes = getDetailsCardItemBackgroundRes();
					if (backgroundRes != 0) return backgroundRes;
					return super.getCardItemBackgroundRes();
				}
			};
	}
	
	@Override
	public SectionDetailsItem createMediaInfos(SectionInfoItem item, 
			INameValue[] nameValues) {
		return createMediaTags(item, 
				R.string.sectioninfo_details_media_infos_title, 
				nameValues);
	}
	
	@Override
	public SectionDetailsItem[] createMediaTags(SectionInfoItem item, 
			INameValue[] nameValues) {
		if (item == null) return null;
		
		ArrayList<INameValue> list = new ArrayList<INameValue>();
		ArrayList<SectionDetailsItem> items = new ArrayList<SectionDetailsItem>();
		int valueLength = 0;
		
		for (INameValue nameValue : nameValues) {
			if (nameValue == null) continue;
			list.add(nameValue);
			valueLength += getTagValueLength(nameValue);
			
			if (list.size() >= getMaxTags() || valueLength >= getMaxTagValueLength()) {
				INameValue[] nvs = list.toArray(new INameValue[list.size()]);
				list.clear();
				valueLength = 0;
				
				items.add(createMediaTags(item, 
						R.string.sectioninfo_details_media_tags_title, 
						nvs));
			}
		}
		
		if (list.size() > 0) {
			INameValue[] nvs = list.toArray(new INameValue[list.size()]);
			list.clear();
			
			items.add(createMediaTags(item, 
					R.string.sectioninfo_details_media_tags_title, 
					nvs));
		}
		
		return items.toArray(new SectionDetailsItem[items.size()]);
	}
	
	protected SectionDetailsItem createMetadataTags(SectionInfoItem item, 
			int nameRes, INameValue[] nameValues) {
		if (item == null || nameValues == null || nameValues.length == 0)
			return null;
		return new AnyboxSectionMetadata.AnyboxSectionMetadataTags(item, nameRes, nameValues) { 
				@Override
				protected int getCardBackgroundRes() {
					int backgroundRes = getDetailsCardBackgroundRes();
					if (backgroundRes != 0) return backgroundRes;
					return super.getCardBackgroundRes();
				}
				@Override
				protected int getCardItemBackgroundRes() {
					int backgroundRes = getDetailsCardItemBackgroundRes();
					if (backgroundRes != 0) return backgroundRes;
					return super.getCardItemBackgroundRes();
				}
			};
	}
	
	@Override
	public SectionDetailsItem createFileInfos(SectionInfoItem item, 
			INameValue[] nameValues) {
		return createMetadataTags(item, 
				R.string.sectioninfo_details_metadata_infos_title, 
				nameValues);
	}
	
	@Override
	public SectionDetailsItem[] createFileTags(SectionInfoItem item, 
			INameValue[] nameValues) {
		if (item == null) return null;
		
		ArrayList<INameValue> list = new ArrayList<INameValue>();
		ArrayList<SectionDetailsItem> items = new ArrayList<SectionDetailsItem>();
		int valueLength = 0;
		
		for (INameValue nameValue : nameValues) {
			if (nameValue == null) continue;
			list.add(nameValue);
			valueLength += getTagValueLength(nameValue);
			
			if (list.size() >= getMaxTags() || valueLength >= getMaxTagValueLength()) {
				INameValue[] nvs = list.toArray(new INameValue[list.size()]);
				list.clear();
				valueLength = 0;
				
				items.add(createMetadataTags(item, 
						R.string.sectioninfo_details_metadata_tags_title, 
						nvs));
			}
		}
		
		if (list.size() > 0) {
			INameValue[] nvs = list.toArray(new INameValue[list.size()]);
			list.clear();
			
			items.add(createMetadataTags(item, 
					R.string.sectioninfo_details_metadata_tags_title, 
					nvs));
		}
		
		return items.toArray(new SectionDetailsItem[items.size()]);
	}
	
}
