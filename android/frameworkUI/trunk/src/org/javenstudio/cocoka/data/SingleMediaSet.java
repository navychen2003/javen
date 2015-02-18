package org.javenstudio.cocoka.data;

import java.util.ArrayList;
import java.util.List;

public class SingleMediaSet extends AbstractMediaSet {

	private final IMediaItem mItem;
	
	public SingleMediaSet(IMediaItem item) { 
		mItem = item;
	}
	
	@Override
	public int getItemCount() {
		return 1;
	}

	@Override
	public IMediaItem findItem(int index) {
		return index == 0 ? mItem : null;
	}

	@Override
	public List<IMediaItem> getItemList(int start, int count) { 
		List<IMediaItem> list = new ArrayList<IMediaItem>();
		if (start == 0) list.add(mItem);
		return list;
	}
	
}
