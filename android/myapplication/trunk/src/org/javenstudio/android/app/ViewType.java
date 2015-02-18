package org.javenstudio.android.app;

import java.util.ArrayList;
import java.util.List;

import org.javenstudio.cocoka.android.ResourceHelper;

public final class ViewType {

	private static final String VIEWTYPE_PREFERENCE_KEY = "general.viewtype";
	
	private static final int VIEWTYPE_LARGE = 1;
	private static final int VIEWTYPE_LIST = 2;
	private static final int VIEWTYPE_SMALL = 3;
	private static final int VIEWTYPE_GRID = 4;
	
	public static enum Type { LARGE, LIST, SMALL, GRID }
	
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
	
	public ViewType() { 
		this(Type.LARGE, Type.LIST, Type.SMALL);
	}
	
	public ViewType(Type... supports) { 
		mSupports = supports == null || supports.length == 0 ? 
				new Type[] {Type.LARGE, Type.LIST, Type.SMALL} : supports;
		mItems = new TypeItem[] { 
				new TypeItem(Type.LARGE, R.string.label_action_viewtype_large, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_viewlarge)), 
				new TypeItem(Type.LIST, R.string.label_action_viewtype_list, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_viewlist)), 
				new TypeItem(Type.SMALL, R.string.label_action_viewtype_small, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_viewsmall))
			};
	}
	
	protected void setPreference(int value) {
		ResourceHelper.setPreference(VIEWTYPE_PREFERENCE_KEY, value);
	}
	
	protected int getPreference() {
		return ResourceHelper.getPreferenceInt(VIEWTYPE_PREFERENCE_KEY, VIEWTYPE_LARGE);
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
				case VIEWTYPE_LIST: 
					mCheckedType = Type.LIST;
					break;
				case VIEWTYPE_SMALL: 
					mCheckedType = Type.SMALL;
					break;
				case VIEWTYPE_GRID:
					mCheckedType = Type.GRID;
					break;
				default: 
					mCheckedType = Type.LARGE;
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
			
			int value = VIEWTYPE_LARGE;
			switch (type) { 
			case LARGE: 
				value = VIEWTYPE_LARGE;
				break;
			case LIST: 
				value = VIEWTYPE_LIST;
				break;
			case SMALL:
				value = VIEWTYPE_SMALL;
				break;
			case GRID:
				value = VIEWTYPE_GRID;
				break;
			}
			setPreference(value);
		}
	}
	
	public boolean isLargeMode() { 
		return getSelectType() == Type.LARGE;
	}
	
	public boolean isListMode() { 
		return getSelectType() == Type.LIST;
	}
	
	public boolean isSmallMode() { 
		return getSelectType() == Type.SMALL;
	}
	
	public boolean isGridMode() {
		return getSelectType() == Type.GRID;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "-" + mIdentity + "{}";
	}
	
}
