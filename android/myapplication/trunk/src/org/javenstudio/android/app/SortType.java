package org.javenstudio.android.app;

import java.util.ArrayList;
import java.util.List;

import org.javenstudio.cocoka.android.ResourceHelper;

public class SortType {

	private static final String SORTTYPE_PREFERENCE_KEY = "general.sorttype";
	
	private static final int SORTTYPE_NAME_ASC = 1;
	private static final int SORTTYPE_NAME_DESC = 2;
	private static final int SORTTYPE_MODIFIED_ASC = 3;
	private static final int SORTTYPE_MODIFIED_DESC = 4;
	private static final int SORTTYPE_SIZE_ASC = 5;
	private static final int SORTTYPE_SIZE_DESC = 6;
	private static final int SORTTYPE_TYPE_ASC = 7;
	private static final int SORTTYPE_TYPE_DESC = 8;
	
	public static enum Type { 
		NAME_ASC, NAME_DESC, 
		MODIFIED_ASC, MODIFIED_DESC, 
		SIZE_ASC, SIZE_DESC, 
		TYPE_ASC, TYPE_DESC 
	}
	
	public static interface OnChangeListener {
		public void onChangeSortType(SortType.Type type);
	}
	
	private static final Type[] sDefault = new Type[] {
			Type.NAME_ASC, Type.NAME_DESC, 
			Type.MODIFIED_ASC, Type.MODIFIED_DESC,
			Type.SIZE_ASC, Type.SIZE_DESC,
			Type.TYPE_ASC, Type.TYPE_DESC
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
	
	public SortType() { 
		this(sDefault);
	}
	
	public SortType(Type... supports) { 
		mSupports = (supports == null || supports.length == 0) ? sDefault : supports;
		mItems = new TypeItem[] { 
				new TypeItem(Type.NAME_ASC, R.string.label_sortby_name_asc, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_sort_name_asc)), 
				new TypeItem(Type.NAME_DESC, R.string.label_sortby_name_desc, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_sort_name_desc)), 
				new TypeItem(Type.MODIFIED_ASC, R.string.label_sortby_modified_asc, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_sort_modified_asc)), 
				new TypeItem(Type.MODIFIED_DESC, R.string.label_sortby_modified_desc, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_sort_modified_desc)), 
				new TypeItem(Type.SIZE_ASC, R.string.label_sortby_size_asc, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_sort_size_asc)), 
				new TypeItem(Type.SIZE_DESC, R.string.label_sortby_size_desc, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_sort_size_desc)), 
				new TypeItem(Type.TYPE_ASC, R.string.label_sortby_type_asc, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_sort_type_asc)), 
				new TypeItem(Type.TYPE_DESC, R.string.label_sortby_type_desc, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_sort_type_desc)), 
			};
	}
	
	protected void setPreference(int value) {
		ResourceHelper.setPreference(SORTTYPE_PREFERENCE_KEY, value);
	}
	
	protected int getPreference() {
		return ResourceHelper.getPreferenceInt(SORTTYPE_PREFERENCE_KEY, SORTTYPE_NAME_ASC);
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
				case SORTTYPE_NAME_ASC: 
					mCheckedType = Type.NAME_ASC;
					break;
				case SORTTYPE_NAME_DESC: 
					mCheckedType = Type.NAME_DESC;
					break;
				case SORTTYPE_MODIFIED_ASC: 
					mCheckedType = Type.MODIFIED_ASC;
					break;
				case SORTTYPE_MODIFIED_DESC: 
					mCheckedType = Type.MODIFIED_DESC;
					break;
				case SORTTYPE_SIZE_ASC: 
					mCheckedType = Type.SIZE_ASC;
					break;
				case SORTTYPE_SIZE_DESC: 
					mCheckedType = Type.SIZE_DESC;
					break;
				case SORTTYPE_TYPE_ASC: 
					mCheckedType = Type.TYPE_ASC;
					break;
				case SORTTYPE_TYPE_DESC: 
					mCheckedType = Type.TYPE_DESC;
					break;
				default: 
					mCheckedType = Type.NAME_ASC;
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
			
			int value = SORTTYPE_NAME_ASC;
			switch (type) { 
			case NAME_ASC: 
				value = SORTTYPE_NAME_ASC;
				break;
			case NAME_DESC: 
				value = SORTTYPE_NAME_DESC;
				break;
			case MODIFIED_ASC: 
				value = SORTTYPE_MODIFIED_ASC;
				break;
			case MODIFIED_DESC: 
				value = SORTTYPE_MODIFIED_DESC;
				break;
			case SIZE_ASC: 
				value = SORTTYPE_SIZE_ASC;
				break;
			case SIZE_DESC: 
				value = SORTTYPE_SIZE_DESC;
				break;
			case TYPE_ASC: 
				value = SORTTYPE_TYPE_ASC;
				break;
			case TYPE_DESC: 
				value = SORTTYPE_TYPE_DESC;
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
