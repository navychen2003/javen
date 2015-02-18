package org.javenstudio.cocoka.app;

import android.graphics.Camera;
import android.graphics.Matrix;
import android.view.animation.Animation;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.Transformation;

public class AnimationHelper {

	static class RotateAnimation extends Animation { 
		@Override
		public void initialize(int width, int height, int parentWidth, int parentHeight) {
			setDuration(1000);
			setInterpolator(new AnticipateOvershootInterpolator());
			super.initialize(width, height, parentWidth, parentHeight);
		}
		
		@Override
	    protected void applyTransformation(float interpolatedTime, Transformation t) {
			super.applyTransformation(interpolatedTime, t);
			
			final Camera camera = new Camera();
			final Matrix matrix = t.getMatrix();
			
			camera.save();
			camera.translate(0.0f, 0.0f, (1300 - 1300.0f * interpolatedTime));
			camera.rotateY(360 * interpolatedTime);
			camera.getMatrix(matrix);
			matrix.preTranslate(-50f, -50f);
			matrix.postTranslate(50f, 50f);
			camera.restore();
	    }
	}
	
	static class ScaleAnimation extends Animation { 
		@Override
		public void initialize(int width, int height, int parentWidth, int parentHeight) {
			setDuration(1000);
			setInterpolator(new OvershootInterpolator());
			super.initialize(width, height, parentWidth, parentHeight);
		}
		
		@Override
	    protected void applyTransformation(float interpolatedTime, Transformation t) {
			super.applyTransformation(interpolatedTime, t);
			
			//final Camera camera = new Camera();
			final Matrix matrix = t.getMatrix();
			
			matrix.setScale(interpolatedTime, interpolatedTime);
	    }
	}
	
}
