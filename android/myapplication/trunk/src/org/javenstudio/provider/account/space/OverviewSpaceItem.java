package org.javenstudio.provider.account.space;

import android.graphics.Paint;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.graphics.ChartDrawable;
import org.javenstudio.cocoka.util.Utilities;

public class OverviewSpaceItem extends SpaceItem {

	private final ITotalSpaceData mData;
	
	public OverviewSpaceItem(OverviewSpaceProvider provider, 
			ITotalSpaceData data) {
		super(provider);
		if (data == null) throw new NullPointerException();
		mData = data;
	}
	
	public ITotalSpaceData getData() { return mData; }

	@Override
	public int getViewRes() {
		return R.layout.storagespace_overview;
	}

	@Override
	public void bindView(View view) {
		if (view == null) return;
		
		final TextView titleView = (TextView)view.findViewById(R.id.overviewspace_item_title);
		if (titleView != null) {
			String text = AppResources.getInstance().getResources().getString(R.string.total_space_title);
			String space = Utilities.formatSize(getData().getTotalSpace());
			CharSequence title = String.format(text, space);
			if (title != null) titleView.setText(title);
			titleView.setVisibility(View.VISIBLE);
		}
		
		final TextView subtitleView = (TextView)view.findViewById(R.id.overviewspace_item_subtitle);
		if (subtitleView != null) {
			String text = AppResources.getInstance().getResources().getString(R.string.remaining_space);
			String space = Utilities.formatSize(getData().getRemainingSpace());
			CharSequence title = String.format(text, space);
			if (title != null) subtitleView.setText(title);
			subtitleView.setVisibility(View.VISIBLE);
		}
		
		final ImageView percentView = (ImageView)view.findViewById(R.id.overviewspace_item_percent);
		if (percentView != null) {
			int abovecolorRes = AppResources.getInstance().getColorRes(AppResources.color.overviewspace_percent_color);
			if (abovecolorRes == 0) abovecolorRes = R.color.overviewspace_percent_above_color;
			int abovecolor = AppResources.getInstance().getResources().getColor(abovecolorRes);
			int belowcolorRes = R.color.overviewspace_percent_below_color;
			int belowcolor = AppResources.getInstance().getResources().getColor(belowcolorRes);
			
			ChartDrawable chart = new ChartDrawable(ChartDrawable.CHART_HISTOGRAM);
			chart.getBelowPaint().setColor(belowcolor);
			chart.getBelowPaint().setStyle(Paint.Style.FILL);
			chart.getAbovePaint().setColor(abovecolor);
			chart.getAbovePaint().setStyle(Paint.Style.FILL);
			chart.setPercent(getData().getUsedPercent());
			
			percentView.setImageDrawable(chart);
			percentView.setVisibility(View.VISIBLE);
		}
	}
	
}
