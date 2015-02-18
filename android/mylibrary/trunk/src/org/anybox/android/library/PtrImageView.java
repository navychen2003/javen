package org.anybox.android.library;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.widget.ImageView;

import org.javenstudio.common.util.Logger;

public class PtrImageView extends ImageView {
	private static final Logger LOG = Logger.getLogger(PtrImageView.class);

	public PtrImageView(Context context) {
        super(context);
    }

    public PtrImageView(Context context, AttributeSet attrs) {
    	super(context, attrs, 0);
    }

    public PtrImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
	
    @Override
    public void startAnimation(Animation animation) {
    	if (LOG.isDebugEnabled()) LOG.debug("startAnimation: anim=" + animation);
    	super.startAnimation(animation);
    }
    
    @Override
    public void setImageMatrix(Matrix matrix) {
    	//if (LOG.isDebugEnabled()) LOG.debug("setImageMatrix: matrix=" + matrix);
    	super.setImageMatrix(matrix);
    }
    
}
