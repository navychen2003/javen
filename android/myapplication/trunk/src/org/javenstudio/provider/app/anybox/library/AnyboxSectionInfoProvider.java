package org.javenstudio.provider.app.anybox.library;

import org.javenstudio.provider.app.anybox.AnyboxAccount;
import org.javenstudio.provider.library.BaseSectionInfoProvider;
import org.javenstudio.provider.library.ISectionInfoData;
import org.javenstudio.provider.library.details.SectionInfoBinder;
import org.javenstudio.provider.library.details.SectionInfoItem;

public class AnyboxSectionInfoProvider extends BaseSectionInfoProvider {

	private final SectionInfoItem mSectionItem;
	
	public AnyboxSectionInfoProvider(AnyboxAccount account, 
			String name, int iconRes, int indicatorRes, ISectionInfoData data) {
		super(account.getApp(), account, name, iconRes);
		mSectionItem = createSectionItem(data);
		//setOptionsMenu(new AnyboxAccountOptionsMenu(app, null));
		setHomeAsUpIndicator(indicatorRes);
	}

	public SectionInfoItem getSectionItem() { return mSectionItem; }
	
	@Override
	protected SectionInfoBinder createDetailsBinder() {
		return new AnyboxSectionInfoBinder(this);
	}
	
	protected SectionInfoItem createSectionItem(ISectionInfoData data) {
		return new AnyboxSectionInfoItem(this, data, new AnyboxSectionInfoFactory());
	}
	
}
