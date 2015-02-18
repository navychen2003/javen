package org.javenstudio.provider.library.list;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.SelectMode;
import org.javenstudio.provider.library.section.SectionListFactory;

public abstract class LibraryFactory extends SectionListFactory {

	public SelectMode createSelectMode(IActivity activity) {
		return new SelectMode(activity.getActionHelper());
	}
	
}
