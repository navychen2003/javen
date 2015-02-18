package org.javenstudio.android.information;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.widget.model.BaseController;
import org.javenstudio.cocoka.widget.model.Model;

public class InformationController extends BaseController {

	private static InformationController sInstance = null; 
	private static final Object sLock = new Object(); 
	
	public static InformationController getInstance() { 
		synchronized (sLock) { 
			if (sInstance == null) {
				sInstance = new InformationController(
						ResourceHelper.getApplication(), ResourceHelper.getContext()); 
			}
			return sInstance; 
		}
	}
	
	private final Context mContext; 
	private final InformationModel mModel; 
	private String mLocation = null; 
	private String mFetchLocation = null; 
	private Uri mWebpageUri = null;
	
	private InformationController(Application app, Context context) { 
		mContext = context; 
		mModel = new InformationModel(app); 
	}
	
	public final InformationModel getInformationModel() { 
		return mModel; 
	}
	
	@Override 
	public Model getModel() { 
		return getInformationModel(); 
	}
	
	@Override 
	public final Context getContext() { 
		return mContext; 
	}
	
	public final String getLocation() { 
		return mLocation; 
	}
	
	public final String getFetchLocation() { 
		String location = mFetchLocation; 
		if (location != null && location.length() > 0) 
			return location; 
		
		return getLocation(); 
	}
	
	public final void initialize(Model.Callback callback, String location) { 
		synchronized (this) { 
			mLocation = location; 
			mFetchLocation = null; 
			mWebpageUri = null;
		}
		initialize(callback); 
	}
	
	public InformationSource getInformationSource() { 
		return getInformationModel().getSource(getLocation()); 
	}
	
	private boolean existInformationSource(String location) { 
		return getInformationModel().existSource(location); 
	}
	
	public InformationItem getInformationItem() { 
		return InformationRegistry.getInformationItem(getLocation());
	}
	
	@Override
	public void refreshContent(Model.Callback callback, boolean force) { 
		synchronized (this) { 
			mFetchLocation = null; 
		}
		super.refreshContent(callback, force); 
	}
	
	public void refreshNextPage(Model.Callback callback, String location) { 
		if (location == null || location.length() == 0) 
			return; 
		
		if (!existInformationSource(getLocation())) 
			return; 
		
		InformationSource item = getInformationSource(); 
		if (item != null && !item.existFetchedLocation(location)) { 
			synchronized (this) { 
				mFetchLocation = location; 
			}
			super.refreshContent(callback, true); 
		}
	}
	
	public Uri getWebpageUri() { 
		synchronized (this) { 
			if (mWebpageUri == null) 
				mWebpageUri = getWebpageUriInternal();
			return mWebpageUri;
		}
	}
	
	private Uri getWebpageUriInternal() { 
		String location = getLocation(); 
		if (location != null && location.length() > 0) {
			Uri uri = Uri.parse(location);
			String scheme = uri != null ? uri.getScheme() : null;
			
			if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) 
				return uri;
		}
		
		InformationItem item = getInformationItem();
		if (item != null) { 
			InformationOne one = item.getFirstInformation();
			String link = one != null ? one.getLink() : null;
			if (link != null && link.length() > 0) { 
				Uri uri = Uri.parse(link);
				String scheme = uri != null ? uri.getScheme() : null;
				
				if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) 
					return uri;
			}
		}
		
		return null;
	}
	
	public Intent getShareIntent() { 
		InformationSource source = getInformationSource();
		if (source != null) { 
			Information info = source.getInformationDataSets().getInformationAt(0);
			if (info != null && info instanceof InformationOne) { 
				InformationOne one = (InformationOne)info;
				return one.getShareIntent();
			}
		}
		
		InformationItem item = getInformationItem();
		if (item != null) { 
			InformationOne one = item.getFirstInformation();
			if (one != null) 
				return one.getShareIntent();
		}
		
		return null; 
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{location=" + getLocation() + "}";
	}
	
}
