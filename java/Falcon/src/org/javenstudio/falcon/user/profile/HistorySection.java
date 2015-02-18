package org.javenstudio.falcon.user.profile;

import org.javenstudio.falcon.datum.ISection;
import org.javenstudio.falcon.user.IUser;

public class HistorySection {

	private final HistoryItem mItem;
	private final ISection mSection;
	private final IUser mOwner;
	
	public HistorySection(HistoryItem item, ISection section, IUser owner) {
		if (item == null || section == null) throw new NullPointerException();
		mItem = item;
		mSection = section;
		mOwner = owner;
	}
	
	public HistoryItem getItem() { return mItem; }
	public ISection getSection() { return mSection; }
	public IUser getOwner() { return mOwner; }
	
	public String getContentId() { return getItem().getContentId(); }
	
}
