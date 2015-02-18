package org.javenstudio.provider.library;

public abstract class SectionPhotoSet extends AbstractPhotoSet {
	//private static final Logger LOG = Logger.getLogger(SectionPhotoSet.class);

	public SectionPhotoSet(ISectionList list, IPhotoData data) {
		int indexHint = 0;
		
		for (int i=0; i < list.getSectionSetCount(); i++) {
			ISectionSet set = list.getSectionSetAt(i);
			if (set == null) continue;
			
			ISectionData[] sections = set.getSections();
			if (sections == null) continue;
			
			for (ISectionData section : sections) {
				if (section == null || section.isFolder()) continue;
				SectionPhotoItem item = createPhotoItem(section);
				if (item != null && (item.getImage() != null || item.getThumbnail() != null)) {
					if (data == section) indexHint = getPhotoCount();
					addPhoto(item);
				}
			}
		}
		
		setIndexHint(indexHint);
	}
	
	public SectionPhotoSet(IPhotoData[] list, IPhotoData data) {
		int indexHint = 0;
		
		for (int i=0; list != null && i < list.length; i++) {
			IPhotoData photo = list[i];
			if (photo == null) continue;
			
			SectionPhotoItem item = createPhotoItem(photo);
			if (item != null && (item.getImage() != null || item.getThumbnail() != null)) {
				if (data == photo) indexHint = getPhotoCount();
				addPhoto(item);
			}
		}
		
		setIndexHint(indexHint);
	}
	
}
