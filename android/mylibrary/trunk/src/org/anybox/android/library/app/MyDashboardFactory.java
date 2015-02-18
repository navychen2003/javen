package org.anybox.android.library.app;

import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import org.anybox.android.library.R;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.information.InformationHelper;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.provider.account.dashboard.DashboardBinder;
import org.javenstudio.provider.account.dashboard.DashboardFactory;
import org.javenstudio.provider.account.dashboard.DashboardItem;
import org.javenstudio.provider.account.dashboard.DashboardProvider;
import org.javenstudio.provider.app.anybox.user.AnyboxDashboardProvider;

public class MyDashboardFactory extends DashboardFactory {

	public static final MyDashboardFactory FACTORY = new MyDashboardFactory();
	
	private MyDashboardFactory() {}
	
	@Override
	public DashboardBinder createDashboardBinder(DashboardProvider p) {
		return new MyDashboardBinder((AnyboxDashboardProvider)p);
	}
	
	@Override
	public DashboardItem createEmptyItem(DashboardProvider p) {
		return new DashboardEmptyItem(p);
	}
	
	static class TipData {
		private final int mTitleRes;
		private final int mSubTitleRes;
		private final int mImageRes;
		
		public TipData(int title, int subtitle, int image) {
			mTitleRes = title;
			mSubTitleRes = subtitle;
			mImageRes = image;
		}
		
		public int getTitleRes() { return mTitleRes; }
		public int getSubTitleRes() { return mSubTitleRes; }
		public int getImageRes() { return mImageRes; }
		
		public CharSequence getTitle() {
			return ResourceHelper.getResources().getString(mTitleRes);
		}
		
		public CharSequence getSubTitle() {
			String text = ResourceHelper.getResources().getString(mSubTitleRes);
			return InformationHelper.formatContentSpanned(text);
		}
		
		public Drawable getImage() {
			if (mImageRes != 0) return ResourceHelper.getResources().getDrawable(mImageRes);
			return null;
		}
	}
	
	static class DashboardEmptyItem extends DashboardItem {
		private final TipData[] mTips;
		private volatile int mTipIdx = 0;
		
		public DashboardEmptyItem(final DashboardProvider p) { 
			super(p);
			mTips = new TipData[] { 
				new TipData(R.string.dashboard_empty_title0, 
						R.string.dashboard_empty_subtitle0, 0) { 
						@Override	
						public CharSequence getSubTitle() {
							String text = ResourceHelper.getResources().getString(getSubTitleRes());
							text = String.format(text, AppResources.getInstance().formatReadableBytes(p.getAccountUser().getTotalCapacitySpace()));
							return InformationHelper.formatContentSpanned(text);
						}
					},
				new TipData(R.string.dashboard_empty_title1, 
						R.string.dashboard_empty_subtitle1, 0)
			};
		}

		private void nextTip() { mTipIdx ++; }
		
		private TipData getTipData() {
			if (mTipIdx < 0 || mTipIdx >= mTips.length) 
				mTipIdx = 0;
			return mTips[mTipIdx];
		}
		
		@Override
		public int getViewRes() {
			return R.layout.dashboard_empty;
		}

		@Override
		public void bindView(final IActivity activity, 
				final DashboardBinder binder, final View view) {
			if (activity == null || binder == null || view == null)
				return;
			
			TipData tip = getTipData();
			
			final ImageView imageView = (ImageView)view.findViewById(R.id.dashboard_empty_image);
			if (imageView != null) {
				//int imageRes = R.drawable.emptystate_dashboard;
				//if (imageRes != 0) imageView.setImageResource(imageRes);
			}
			
			final TextView titleView = (TextView)view.findViewById(R.id.dashboard_empty_title);
			if (titleView != null) {
				if (tip != null) titleView.setText(tip.getTitle());
				titleView.setVisibility(View.VISIBLE);
			}
			
			final TextView subtitleView = (TextView)view.findViewById(R.id.dashboard_empty_subtitle);
			if (subtitleView != null) {
				if (tip != null) subtitleView.setText(tip.getSubTitle());
				subtitleView.setVisibility(View.VISIBLE);
			}
			
			final TextView nextView = (TextView)view.findViewById(R.id.dashboard_empty_next);
			if (nextView != null) {
				nextView.setText(R.string.dashboard_empty_next_label);
				nextView.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
				nextView.setVisibility(View.VISIBLE);
				nextView.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							bindNextTips(activity, binder, view);
						}
					});
			}
			
			final TextView actionView = (TextView)view.findViewById(R.id.dashboard_empty_action);
			if (actionView != null) {
				actionView.setText(R.string.dashboard_empty_upload_label);
				actionView.setVisibility(View.VISIBLE);
				actionView.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
						}
					});
			}
		}

		@Override
		public void updateView(View view, boolean restartSlide) {
		}
		
		private void bindNextTips(IActivity activity, DashboardBinder binder, View view) {
			nextTip();
			bindView(activity, binder, view);
			if (view != null) {
				int animRes = AppResources.getInstance().getAnimRes(AppResources.anim.dashboard_item_show_animation);
				if (animRes == 0) animRes = R.anim.slide_out_up;
				Animation ani = AnimationUtils.loadAnimation(activity.getActivity(), animRes);
				view.startAnimation(ani);
			}
		}
	}
	
}
