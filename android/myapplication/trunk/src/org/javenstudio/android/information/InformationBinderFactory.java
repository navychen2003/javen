package org.javenstudio.android.information;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.IMenuOperation;
import org.javenstudio.android.app.R;
import org.javenstudio.android.app.ViewType;
import org.javenstudio.android.information.subscribe.SubscribeNavItem;
import org.javenstudio.android.reader.ReaderHelper;
import org.javenstudio.cocoka.widget.model.NavigationInfo;
import org.javenstudio.common.util.Logger;

public abstract class InformationBinderFactory {
	private static final Logger LOG = Logger.getLogger(InformationBinderFactory.class);

	private static InformationBinderFactory sInstance = null;
	
	public static synchronized InformationBinderFactory getInstance() { 
		if (sInstance == null) 
			sInstance = AppResources.getInstance().createInformationBinderFactory();
		return sInstance;
	}
	
	private InformationBinder mListBinder;
	private InformationBinder mLargeListBinder;
	private InformationBinder mPhotoListBinder;
	private InformationBinder mPhotoGridBinder;
	private InformationBinder mNewsListBinder;
	private InformationBinder mCommentListBinder;
	private InformationBinder mDefaultListBinder;
	private InformationBinder mCommentItemBinder;
	private InformationBinder mPhotoItemBinder;
	private InformationBinder mItemBinder;
	
	public InformationBinderFactory() { 
		mListBinder = new ReaderListBinder();
		mLargeListBinder = new ReaderLargeListBinder();
		mPhotoListBinder = new ReaderPhotoListBinder();
		mPhotoGridBinder = new ReaderPhotoGridBinder();
		mNewsListBinder = new ReaderNewsListBinder();
		mCommentListBinder = new ReaderCommentListBinder();
		mDefaultListBinder = new ReaderDefaultListBinder();
		mCommentItemBinder = new ReaderCommentItemBinder();
		mPhotoItemBinder = new ReaderPhotoItemBinder();
		mItemBinder = new ReaderItemBinder();
	}
	
	public boolean onInformationClick(Activity from, InformationOne one) { 
		return false;
	}
	
	public boolean onInformationImageClick(Activity from, InformationOne one) { 
		return false;
	}
	
	public boolean onInformationLongClick(Activity activity, InformationOperation.IInformation item) { 
		return InformationOperation.openOperation(activity, item);
	}
	
	public SubscribeNavItem createDefaultNavItem(NavigationInfo info, boolean selected) { 
		return new SubscribeNavItem(getDefaultListBinder(), info, selected);
	}
	
	public SubscribeNavItem createSubscribeNavItem(NavigationInfo info, boolean selected) { 
		return new SubscribeNavItem(getListBinder(), info, selected);
	}
	
	public SubscribeNavItem createSubscribeLargeNavItem(NavigationInfo info, boolean selected) { 
		return new SubscribeNavItem(getLargeListBinder(), info, selected);
	}
	
	public SubscribeNavItem createSubscribePhotoNavItem(NavigationInfo info, boolean selected) { 
		return new SubscribeNavItem(getPhotoListBinder(), info, selected);
	}
	
	public SubscribeNavItem createFeedNavItem(NavigationInfo info, boolean selected) { 
		return new SubscribeNavItem.FeedNavItem(getListBinder(), info, selected);
	}
	
	public SubscribeNavItem createFeedLargeNavItem(NavigationInfo info, boolean selected) { 
		return new SubscribeNavItem.FeedNavItem(getLargeListBinder(), info, selected);
	}
	
	public SubscribeNavItem createFeedPhotoNavItem(NavigationInfo info, boolean selected) { 
		return new SubscribeNavItem.FeedNavItem(getPhotoListBinder(), info, selected);
	}
	
	public InformationItem.ItemBinder getInformationItemBinder(String location) { 
		if (LOG.isDebugEnabled())
			LOG.debug("getInformationItemBinder: location=" + location);
		
		int itemType = ReaderHelper.getInformationItemType(location);
		if (itemType == Information.ITEMTYPE_PHOTO) 
			return getPhotoItemBinder();
		
		return getItemBinder();
	}
	
	public InformationNavItem.NavBinder getListBinder() { 
		return new InformationNavItem.NavBinder() {
				private final IMenuOperation mMenuOperation = createDefaultListOperation();
				@Override
				public InformationBinder getBinder(InformationNavItem item) {
					return mListBinder;
				}
				@Override
				public IMenuOperation getMenuOperation() {
					return mMenuOperation;
				}
			};
	}
	
	public InformationNavItem.NavBinder getLargeListBinder() { 
		return new InformationNavItem.NavBinder() {
				private final IMenuOperation mMenuOperation = createDefaultListOperation();
				@Override
				public InformationBinder getBinder(InformationNavItem item) {
					return mLargeListBinder;
				}
				@Override
				public IMenuOperation getMenuOperation() {
					return mMenuOperation;
				}
			};
	}
	
	public abstract ViewType getViewType();
	public abstract IMenuOperation createDefaultListOperation();
	public abstract IMenuOperation createPhotoListOperation();
	
	public InformationNavItem.NavBinder getPhotoListBinder() { 
		return new InformationNavItem.NavBinder() {
				private final IMenuOperation mMenuOperation = createPhotoListOperation();
				@Override
				public InformationBinder getBinder(InformationNavItem item) {
					ViewType.Type type = getViewType().getSelectType();
					if (type == ViewType.Type.SMALL) return mPhotoGridBinder;
					return mPhotoListBinder;
				}
				@Override
				public IMenuOperation getMenuOperation() {
					return mMenuOperation;
				}
			};
	}
	
	public InformationNavItem.NavBinder getNewsListBinder() { 
		return new InformationNavItem.NavBinder() {
				private final IMenuOperation mMenuOperation = createDefaultListOperation();
				@Override
				public InformationBinder getBinder(InformationNavItem item) {
					return mNewsListBinder;
				}
				@Override
				public IMenuOperation getMenuOperation() {
					return mMenuOperation;
				}
			};
	}
	
	public InformationNavItem.NavBinder getCommentListBinder() { 
		return new InformationNavItem.NavBinder() {
				private final IMenuOperation mMenuOperation = createDefaultListOperation();
				@Override
				public InformationBinder getBinder(InformationNavItem item) {
					return mCommentListBinder;
				}
				@Override
				public IMenuOperation getMenuOperation() {
					return mMenuOperation;
				}
			};
	}
	
	private InformationNavItem.NavBinder getDefaultListBinder() { 
		return new InformationNavItem.NavBinder() {
				private final IMenuOperation mMenuOperation = createDefaultListOperation();
				@Override
				public InformationBinder getBinder(InformationNavItem item) {
					return mDefaultListBinder;
				}
				@Override
				public IMenuOperation getMenuOperation() {
					return mMenuOperation;
				}
			};
	}
	
	public InformationSource.SourceBinder getCommentSourceBinder() { 
		return new InformationSource.SourceBinder() {
				@Override
				public InformationBinder getBinder(InformationSource source) {
					return mCommentItemBinder;
				}
			};
	}
	
	public InformationSource.SourceBinder getPhotoSourceBinder() { 
		return new InformationSource.SourceBinder() {
				@Override
				public InformationBinder getBinder(InformationSource source) {
					return mPhotoItemBinder;
				}
			};
	}
	
	public InformationItem.ItemBinder getItemBinder() { 
		return new InformationItem.ItemBinder() {
				@Override
				public InformationBinder getBinder(InformationItem item) {
					return mItemBinder;
				}
			};
	}
	
	public InformationItem.ItemBinder getPhotoItemBinder() { 
		return new InformationItem.ItemBinder() {
				@Override
				public InformationBinder getBinder(InformationItem item) {
					return mPhotoItemBinder;
				}
			};
	}
	
	protected void onPreBindInformationOne(final IActivity activity, 
			final InformationOne one, boolean clickable, boolean longclickable, 
			boolean imageClickable) { 
		if (one.getOnClickListener() == null && clickable) { 
			one.setOnClickListener(new OnInformationClickListener() {
					@Override
					public boolean onInformationClick(Activity activity, InformationOne one) {
						return InformationBinderFactory.this.onInformationClick(activity, one);
					}
				});
		}
		
		if (one.getOnLongClickListener() == null && longclickable) { 
			one.setOnLongClickListener(new OnInformationClickListener() {
					@Override
					public boolean onInformationClick(Activity activity, InformationOne one) {
						return InformationBinderFactory.this.onInformationLongClick(activity, one);
					}
				});
		}
		
		if (one.getImageClickListener() == null && imageClickable) { 
			one.setImageClickListener(new OnInformationClickListener() {
					@Override
					public boolean onInformationClick(Activity activity, InformationOne one) {
						return InformationBinderFactory.this.onInformationImageClick(activity, one);
					}
				});
		}
	}
	
	private class ReaderListBinder extends InformationListBinder { 
		@Override
		protected void bindInformationOne(IActivity activity, InformationOne one, View view) { 
			onPreBindInformationOne(activity, one, true, true, true);
			super.bindInformationOne(activity, one, view);
		}
	}
	
	private class ReaderLargeListBinder extends InformationListBinder { 
		@Override
		protected void bindInformationOne(IActivity activity, InformationOne one, View view) { 
			onPreBindInformationOne(activity, one, true, true, true);
			super.bindInformationOne(activity, one, view);
		}
		@Override
		protected int getInformationItemViewRes(IActivity activity) { 
			return R.layout.information_item_large; 
		}
		@Override
		protected int getInformationHeaderDimenRes(IActivity activity) { 
			return R.dimen.photo_item_header_height; 
		}
		@Override
		protected int getItemViewHeightDimenRes(IActivity activity) { 
			return R.dimen.list_item_height_large; 
		}
	}
	
	private class ReaderCommentListBinder extends InformationListBinder { 
		@Override
		protected void bindInformationOne(IActivity activity, InformationOne one, View view) { 
			onPreBindInformationOne(activity, one, true, true, true);
			super.bindInformationOne(activity, one, view);
		}
		@Override
		protected int getItemViewHeight(IActivity activity, ViewGroup container, View view) { 
			return (int)activity.getResources().getDimension(R.dimen.comment_item_height);
		}
		@Override
		protected int getInformationItemViewRes(IActivity activity) { 
			return R.layout.information_item_comment; 
		}
	}
	
	private class ReaderDefaultListBinder extends InformationListBinder { 
		@Override
		protected void bindInformationOne(IActivity activity, InformationOne one, View view) { 
			onPreBindInformationOne(activity, one, false, true, true);
			super.bindInformationOne(activity, one, view);
		}
		@Override
		protected int getInformationItemViewRes(IActivity activity) { 
			return R.layout.information_item; 
		}
		@Override
		protected int getColumnSize(IActivity activity) { 
			return 1;
		}
	}
	
	private class ReaderPhotoListBinder extends InformationListBinder { 
		@Override
		protected void bindInformationOne(IActivity activity, InformationOne one, View view) { 
			onPreBindInformationOne(activity, one, true, true, true);
			super.bindInformationOne(activity, one, view);
		}
		@Override
		protected void onInformationTextBinded(IActivity activity, InformationOne one, 
				TextView titleView, TextView textView) { 
			if (textView != null) { 
				textView.setVisibility(View.GONE);
				textView.setText(null);
			}
			if (titleView != null)
				titleView.setLines(2);
		}
		@Override
		protected int getItemViewHeight(IActivity activity, ViewGroup container, View view) { 
			if (getColumnSize(activity) > 1) 
				return (int)activity.getResources().getDimension(R.dimen.photo_item_height);
			
			return getDefaultItemViewHeight(activity, container, view);
		}
		@Override
		protected int getInformationItemViewRes(IActivity activity) { 
			return R.layout.information_item_photo; 
		}
		@Override
		protected int getInformationHeaderDimenRes(IActivity activity) { 
			return R.dimen.photo_item_header_height; 
		}
		@Override
		protected int getShowImageCount(InformationOne one) { 
			return 1;
		}
	}
	
	private class ReaderPhotoGridBinder extends InformationListBinderPhoto { 
		@Override
		protected void bindInformationOne(IActivity activity, InformationOne one, View view) { 
			onPreBindInformationOne(activity, one, true, true, true);
			super.bindInformationOne(activity, one, view);
		}
		@Override
		protected int getShowImageCount(InformationOne one) { 
			return 1;
		}
		@Override
		protected OnInformationClickListener getInformationImageClickListener(InformationOne one) { 
			Object showList = one.getAttribute(Information.ATTR_GRIDSHOWLIST);
			if (showList != null && showList.equals("true"))
				return getInformationClickListener(one);
			
			return one.getImageClickListener();
		}
	}
	
	private class ReaderNewsListBinder extends InformationListBinder { 
		@Override
		protected void bindInformationOne(IActivity activity, InformationOne one, View view) { 
			onPreBindInformationOne(activity, one, true, true, true);
			super.bindInformationOne(activity, one, view);
		}
	}
	
	private class ReaderCommentItemBinder extends InformationSourceBinder { 
		@Override
		protected void bindInformationOne(IActivity activity, InformationOne one, View view) { 
			onPreBindInformationOne(activity, one, false, true, true);
			super.bindInformationOne(activity, one, view);
		}
	}
	
	private class ReaderPhotoItemBinder extends InformationSourceBinder { 
		@Override
		protected void bindInformationOne(IActivity activity, InformationOne one, View view) { 
			onPreBindInformationOne(activity, one, false, true, true);
			super.bindInformationOne(activity, one, view);
		}
		@Override
		protected int getInformationItemViewRes(IActivity activity) { 
			return R.layout.information_item_photo; 
		}
		@Override
		protected int getInformationHeaderDimenRes(IActivity activity) { 
			return R.dimen.photo_item_header_height; 
		}
	}
	
	private class ReaderItemBinder extends InformationSourceBinder { 
		@Override
		protected void bindInformationOne(IActivity activity, InformationOne one, View view) { 
			onPreBindInformationOne(activity, one, false, true, true);
			super.bindInformationOne(activity, one, view);
		}
	}
	
}
