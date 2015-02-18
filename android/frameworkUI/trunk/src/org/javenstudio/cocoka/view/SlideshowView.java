package org.javenstudio.cocoka.view;

import android.graphics.PointF;

import java.util.Random;
import javax.microedition.khronos.opengles.GL11;

import org.javenstudio.cocoka.opengl.AnimationTime;
import org.javenstudio.cocoka.opengl.BitmapTexture;
import org.javenstudio.cocoka.opengl.CanvasAnimation;
import org.javenstudio.cocoka.opengl.FloatAnimation;
import org.javenstudio.cocoka.opengl.GLCanvas;
import org.javenstudio.cocoka.opengl.GLView;
import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.common.util.Logger;

public class SlideshowView extends GLView {
	private static final Logger LOG = Logger.getLogger(SlideshowView.class);

    private static final int SLIDESHOW_DURATION = 3500;
    private static final int TRANSITION_DURATION = 1000;

    private static final float SCALE_SPEED = 0.20f ;
    private static final float MOVE_SPEED = SCALE_SPEED;

    private int mCurrentRotation;
    private BitmapTexture mCurrentTexture;
    private SlideshowAnimation mCurrentAnimation;

    private int mPrevRotation;
    private BitmapTexture mPrevTexture;
    private SlideshowAnimation mPrevAnimation;

    private final FloatAnimation mTransitionAnimation =
            new FloatAnimation(0, 1, TRANSITION_DURATION);

    private Random mRandom = new Random();
    private final BitmapHolder mHolder;

    public SlideshowView(BitmapHolder holder) { 
    	mHolder = holder;
    }
    
    public void next(BitmapRef bitmap, int rotation) {
    	if (bitmap == null) return; //throw new NullPointerException();
    	
        mTransitionAnimation.start();

        if (mPrevTexture != null) {
            mPrevTexture.getBitmap().recycle();
            mPrevTexture.recycle();
        }

        mPrevTexture = mCurrentTexture;
        mPrevAnimation = mCurrentAnimation;
        mPrevRotation = mCurrentRotation;

        mCurrentRotation = rotation;
        mCurrentTexture = new BitmapTexture(mHolder, bitmap);
        if (((rotation / 90) & 0x01) == 0) {
            mCurrentAnimation = new SlideshowAnimation(
                    mCurrentTexture.getWidth(), mCurrentTexture.getHeight(),
                    mRandom);
        } else {
            mCurrentAnimation = new SlideshowAnimation(
                    mCurrentTexture.getHeight(), mCurrentTexture.getWidth(),
                    mRandom);
        }
        mCurrentAnimation.start();

        if (LOG.isDebugEnabled()) {
        	LOG.debug("next: bitmap=" + mCurrentTexture.getWidth() + "X" 
        			+ mCurrentTexture.getHeight() + " rotation=" + rotation);
        }
        
        invalidate();
    }

    public void release() {
        if (mPrevTexture != null) {
            mPrevTexture.recycle();
            mPrevTexture = null;
        }
        if (mCurrentTexture != null) {
            mCurrentTexture.recycle();
            mCurrentTexture = null;
        }
    }

    @Override
    protected void render(GLCanvas canvas) {
        long animTime = AnimationTime.get();
        boolean requestRender = mTransitionAnimation.calculate(animTime);
        GL11 gl = canvas.getGLInstance();
        gl.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
        float alpha = mPrevTexture == null ? 1f : mTransitionAnimation.get();

        if (mPrevTexture != null && alpha != 1f) {
            requestRender |= mPrevAnimation.calculate(animTime);
            canvas.save(GLCanvas.SAVE_FLAG_ALPHA | GLCanvas.SAVE_FLAG_MATRIX);
            canvas.setAlpha(1f - alpha);
            mPrevAnimation.apply(canvas);
            canvas.rotate(mPrevRotation, 0, 0, 1);
            mPrevTexture.draw(canvas, -mPrevTexture.getWidth() / 2,
                    -mPrevTexture.getHeight() / 2);
            canvas.restore();
        }
        
        if (mCurrentTexture != null) {
            requestRender |= mCurrentAnimation.calculate(animTime);
            canvas.save(GLCanvas.SAVE_FLAG_ALPHA | GLCanvas.SAVE_FLAG_MATRIX);
            canvas.setAlpha(alpha);
            mCurrentAnimation.apply(canvas);
            canvas.rotate(mCurrentRotation, 0, 0, 1);
            mCurrentTexture.draw(canvas, -mCurrentTexture.getWidth() / 2,
                    -mCurrentTexture.getHeight() / 2);
            canvas.restore();
        }
        
        if (requestRender) invalidate();
        gl.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private class SlideshowAnimation extends CanvasAnimation {
        private final int mWidth;
        private final int mHeight;

        private final PointF mMovingVector;
        private float mProgress;

        public SlideshowAnimation(int width, int height, Random random) {
            mWidth = width;
            mHeight = height;
            mMovingVector = new PointF(
                    MOVE_SPEED * mWidth * (random.nextFloat() - 0.5f),
                    MOVE_SPEED * mHeight * (random.nextFloat() - 0.5f));
            
            setDuration(SLIDESHOW_DURATION);
        }

        @Override
        public void apply(GLCanvas canvas) {
            int viewWidth = getWidth();
            int viewHeight = getHeight();

            float initScale = Math.min((float)
                    viewWidth / mWidth, (float) viewHeight / mHeight);
            float scale = initScale * (1 + SCALE_SPEED * mProgress);

            float centerX = viewWidth / 2 + mMovingVector.x * mProgress;
            float centerY = viewHeight / 2 + mMovingVector.y * mProgress;

            canvas.translate(centerX, centerY);
            canvas.scale(scale, scale, 0);
        }

        @Override
        public int getCanvasSaveFlags() {
            return GLCanvas.SAVE_FLAG_MATRIX;
        }

        @Override
        protected void onCalculate(float progress) {
            mProgress = progress;
        }
    }
    
}
