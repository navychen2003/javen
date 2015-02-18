package org.javenstudio.cocoka.view;

import org.javenstudio.cocoka.app.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

public class PhotoProgressBar {
    private ViewGroup mContainer;
    private View mProgress;

    public PhotoProgressBar(Context context, RelativeLayout parentLayout) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContainer = (ViewGroup) inflater.inflate(R.layout.photopage_progress_bar, parentLayout,
                false);
        parentLayout.addView(mContainer);
        mProgress = mContainer.findViewById(R.id.photopage_progress_foreground);
    }

    public void setProgress(int progressPercent) {
        mContainer.setVisibility(View.VISIBLE);
        LayoutParams layoutParams = mProgress.getLayoutParams();
        layoutParams.width = mContainer.getWidth() * progressPercent / 100;
        mProgress.setLayoutParams(layoutParams);
    }

    public void hideProgress() {
        mContainer.setVisibility(View.INVISIBLE);
    }
}
