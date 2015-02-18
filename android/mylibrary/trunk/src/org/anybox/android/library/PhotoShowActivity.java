package org.anybox.android.library;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.javenstudio.android.app.PhotoActivity;
import org.javenstudio.cocoka.data.IMediaSet;

public class PhotoShowActivity extends PhotoActivity {

	private static IMediaSet sPhotoList = null;
	
	public static void actionShow(Activity from, IMediaSet images) { 
		Intent intent = new Intent(from, PhotoShowActivity.class); 
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
		
		sPhotoList = images;
		
		if (sPhotoList != null)
			from.startActivity(intent); 
	}
	
	private IMediaSet mPhotoList = null;
	
	@Override
	protected IMediaSet getPhotoObject() {
		return mPhotoList;
	}

	@Override
    protected void onCreate(Bundle savedInstanceState) {
		mPhotoList = sPhotoList;
        super.onCreate(savedInstanceState);
        
        setActionBarIcon(R.drawable.ic_home_anybox);
		setHomeAsUpIndicator(R.drawable.ic_ab_back_holo_dark);
		setActionBarTitleColor(getResources().getColor(R.color.actionbar_title_photoshow));
	}
	
}
