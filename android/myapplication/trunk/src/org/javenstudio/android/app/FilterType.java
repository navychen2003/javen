package org.javenstudio.android.app;

import java.util.ArrayList;
import java.util.List;

import org.javenstudio.cocoka.android.ResourceHelper;

public class FilterType {

	private static final String FILTERTYPE_PREFERENCE_KEY = "general.filtertype";
	
	private static final int FILTERTYPE_ALL = 1;
	private static final int FILTERTYPE_IMAGE = 2;
	private static final int FILTERTYPE_AUDIO = 3;
	private static final int FILTERTYPE_VIDEO = 4;
	
	public static enum Type { 
		ALL, IMAGE, AUDIO, VIDEO
	}
	
	public static interface OnChangeListener {
		public void onChangeFilterType(FilterType.Type type);
	}
	
	private static final Type[] sDefault = new Type[] {
			Type.ALL, Type.IMAGE, Type.AUDIO, Type.VIDEO
		};
	
	static class TypeItem implements AlertDialogHelper.IChoiceItem { 
		public final Type mType;
		public final int mTextRes;
		public final int mIconRes;
		
		public TypeItem(Type type, int textRes, int iconRes) { 
			mType = type;
			mTextRes = textRes; 
			mIconRes = iconRes;
		}
		
		public int getTextRes() { return mTextRes; }
		public int getIconRes() { return mIconRes; }
	}
	
	private final long mIdentity = ResourceHelper.getIdentity();
	
	private final TypeItem[] mItems;
	private final Object mLock = new Object();
	private final Type[] mSupports;
	private Type mCheckedType = null; //Type.LARGE;
	
	public FilterType() { 
		this(sDefault);
	}
	
	public FilterType(Type... supports) { 
		mSupports = (supports == null || supports.length == 0) ? sDefault : supports;
		mItems = new TypeItem[] { 
				new TypeItem(Type.ALL, R.string.label_filterby_all, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_filter_all)), 
				new TypeItem(Type.IMAGE, R.string.label_filterby_image, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_filter_image)), 
				new TypeItem(Type.AUDIO, R.string.label_filterby_audio, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_filter_audio)), 
				new TypeItem(Type.VIDEO, R.string.label_filterby_video, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_filter_video)) 
			};
	}
	
	protected void setPreference(int value) {
		ResourceHelper.setPreference(FILTERTYPE_PREFERENCE_KEY, value);
	}
	
	protected int getPreference() {
		return ResourceHelper.getPreferenceInt(FILTERTYPE_PREFERENCE_KEY, FILTERTYPE_ALL);
	}
	
	public TypeItem[] getItems() { 
		synchronized (mLock) { 
			List<TypeItem> items = new ArrayList<TypeItem>();
			for (TypeItem item : mItems) { 
				if (mSupports != null) { 
					for (Type type : mSupports) { 
						if (type == item.mType) { 
							items.add(item);
							break;
						}
					}
				}
			}
			return items.toArray(new TypeItem[items.size()]);
		}
	}
	
	public Type getSelectType() { 
		synchronized (mLock) { 
			if (mCheckedType == null) { 
				int type = getPreference();
				switch (type) { 
				case FILTERTYPE_ALL: 
					mCheckedType = Type.ALL;
					break;
				case FILTERTYPE_IMAGE: 
					mCheckedType = Type.IMAGE;
					break;
				case FILTERTYPE_AUDIO: 
					mCheckedType = Type.AUDIO;
					break;
				case FILTERTYPE_VIDEO: 
					mCheckedType = Type.VIDEO;
					break;
				}
			}
			return mCheckedType;
		}
	}
	
	public void setSelectType(Type type) { 
		if (type == null) return;
		synchronized (mLock) { 
			mCheckedType = type;
			
			int value = FILTERTYPE_ALL;
			switch (type) { 
			case ALL: 
				value = FILTERTYPE_ALL;
				break;
			case IMAGE: 
				value = FILTERTYPE_IMAGE;
				break;
			case AUDIO: 
				value = FILTERTYPE_AUDIO;
				break;
			case VIDEO: 
				value = FILTERTYPE_VIDEO;
				break;
			}
			setPreference(value);
		}
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "-" + mIdentity + "{}";
	}
	
}
