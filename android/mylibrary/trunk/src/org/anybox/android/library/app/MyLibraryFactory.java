package org.anybox.android.library.app;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.anybox.android.library.MyResources;
import org.anybox.android.library.R;
import org.javenstudio.android.app.FilterType;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.SortType;
import org.javenstudio.android.app.ViewType;
import org.javenstudio.provider.app.anybox.library.AnyboxLibraryFactory;
import org.javenstudio.provider.app.anybox.library.AnyboxLibraryProvider;
import org.javenstudio.provider.library.ISectionList;
import org.javenstudio.provider.library.section.SectionGridBinder;
import org.javenstudio.provider.library.section.SectionListBinder;
import org.javenstudio.provider.library.section.SectionListItem;
import org.javenstudio.provider.library.section.SectionListProvider;

public class MyLibraryFactory extends AnyboxLibraryFactory {

	static MyLibraryFactory FACTORY = new MyLibraryFactory();
	
	private MyLibraryFactory() {}
	
	public ViewType getViewType() { return MyResources.sViewType; }
	public SortType getSortType() { return MyResources.sSortType; }
	public FilterType getFilterType() { return MyResources.sFilterType; }
	
	@Override
	public SectionListItem createEmptyItem(SectionListProvider provider) {
		return new FolderEmptyItem(provider);
	}
	
	@Override
	public SectionListBinder createSectionListBinder(SectionListProvider provider) {
		return new MyLibraryListBinder((AnyboxLibraryProvider)provider, this);
	}
	
	@Override
	public SectionGridBinder createSectionGridBinder(SectionListProvider provider) {
		return new MyLibraryGridBinder((AnyboxLibraryProvider)provider, this);
	}

	static class FolderEmptyItem extends SectionListItem {
		public FolderEmptyItem(SectionListProvider p) { 
			super(p);
		}

		static int getViewRes() {
			return R.layout.section_empty;
		}

		static void bindViews(IActivity activity, FolderEmptyItem item, View view) {
			if (activity == null || item == null || view == null)
				return;
			
			final ImageView imageView = (ImageView)view.findViewById(R.id.section_empty_image);
			if (imageView != null) {
				//int imageRes = R.drawable.emptystate_folder;
				//if (imageRes != 0) imageView.setImageResource(imageRes);
				imageView.setVisibility(View.GONE);
			}
			
			final TextView titleView = (TextView)view.findViewById(R.id.section_empty_title);
			if (titleView != null) {
				titleView.setText(R.string.library_empty_title);
				titleView.setVisibility(View.VISIBLE);
			}
			
			final TextView subtitleView = (TextView)view.findViewById(R.id.section_empty_subtitle);
			if (subtitleView != null) {
				ISectionList list = item.getProvider().getSectionList();
				int textRes = R.string.library_empty_subtitle;
				if (list != null) {
					if (list.isRecycleBin()) 
						textRes = R.string.library_empty_trash_subtitle;
					else if (list.isSearchResult())
						textRes = R.string.library_empty_search_subtitle;
				}
				String text = activity.getResources().getString(textRes);
				//text = InformationHelper.formatContentSpanned(text);
				subtitleView.setText(text);
				subtitleView.setVisibility(View.VISIBLE);
			}
		}

		static void updateImageView(FolderEmptyItem item, View view, boolean restartSlide) {
		}
	}
	
}
