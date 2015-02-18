package org.javenstudio.provider.library.details;

import android.graphics.drawable.Drawable;
import android.view.View;

import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.image.http.HttpImage;
import org.javenstudio.android.data.image.http.HttpImageItem;
import org.javenstudio.android.data.image.http.HttpResource;
import org.javenstudio.provider.library.ISectionInfoData;

public class SectionInfoItemBase extends SectionInfoItem {

	private final SectionInfoProvider mProvider;
	private final ISectionInfoData mSectionData;
	
	private HttpImage mPoster = null;
	private HttpImage mBackground = null;
	
	public SectionInfoItemBase(SectionInfoProvider p, ISectionInfoData data) { 
		super(p.getAccountApp(), p.getAccountUser());
		if (p == null || data == null) throw new NullPointerException();
		mProvider = p;
		mSectionData = data;
	}
	
	public SectionInfoProvider getProvider() { return mProvider; }
	public ISectionInfoData getSectionData() { return mSectionData; }
	
	@Override
	public Drawable getProviderIcon() { 
		return getProvider().getIcon();
	}
	
	@Override
	protected void onUpdateViewOnVisible(boolean restartSlide) { 
		SectionInfoBinder binder = getSectionInfoBinder();
		if (binder != null) binder.onUpdateView(this, getBindView(), restartSlide);
	}
	
	private SectionInfoBinder getSectionInfoBinder() { 
		final SectionInfoBinder binder = (SectionInfoBinder)getProvider().getBinder();
		final View view = getBindView();
		
		if (binder == null || view == null) 
			return null;
		
		return binder;
	}

	@Override
	public int getImageWidth() { 
		return getSectionData().getWidth(); 
	}
	
	@Override
	public int getImageHeight() { 
		return getSectionData().getHeight(); 
	}
	
	@Override
	public synchronized Image getPosterImage() { 
		if (mPoster == null) { 
			String imageURL = getSectionData().getPosterURL();
			if (imageURL != null && imageURL.length() > 0) { 
				mPoster = HttpResource.getInstance().getImage(imageURL);
				mPoster.addListener(this);
				
				HttpImageItem.requestDownload(mPoster, false);
			}
		}
		return mPoster; 
	}
	
	@Override
	public synchronized Image getBackgroundImage() { 
		if (mBackground == null) { 
			String imageURL = getSectionData().getBackgroundURL();
			if (imageURL != null && imageURL.length() > 0) { 
				mBackground = HttpResource.getInstance().getImage(imageURL);
				mBackground.addListener(this);
				
				HttpImageItem.requestDownload(mBackground, false);
			}
		}
		return mBackground; 
	}
	
	@Override
	public String getPosterLocation() { 
		Image avatarImage = getPosterImage();
		if (avatarImage != null) return avatarImage.getLocation();
		return null; 
	}
	
	@Override
	public String getBackgroundLocation() { 
		Image bgImage = getBackgroundImage();
		if (bgImage != null) return bgImage.getLocation();
		return null; 
	}
	
}
