package org.javenstudio.cocoka.view;

import android.content.Context;
import android.view.MotionEvent;

import org.javenstudio.cocoka.app.BaseResources;
import org.javenstudio.cocoka.app.R;
import org.javenstudio.cocoka.opengl.AnimationTime;
import org.javenstudio.cocoka.opengl.GLCanvas;
import org.javenstudio.cocoka.opengl.GLView;
import org.javenstudio.cocoka.opengl.NinePatchTexture;
import org.javenstudio.cocoka.opengl.ResourceTexture;
import org.javenstudio.cocoka.opengl.StringTexture;
import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.MediaUtils;
import org.javenstudio.cocoka.util.Utils;

public class UndoBarView extends GLView {
    //private static final Logger LOG = Logger.getLogger(UndoBarView.class);

    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFFAAAAAA;

    private final NinePatchTexture mPanel;
    private final StringTexture mUndoText;
    private final StringTexture mDeletedText;
    private final ResourceTexture mUndoIcon;
    private final int mBarHeight;
    private final int mBarMargin;
    private final int mUndoTextMargin;
    private final int mIconSize;
    private final int mIconMargin;
    private final int mSeparatorTopMargin;
    private final int mSeparatorBottomMargin;
    private final int mSeparatorRightMargin;
    private final int mSeparatorWidth;
    private final int mDeletedTextMargin;
    private final int mClickRegion;

    private OnClickListener mOnClickListener;
    private boolean mDownOnButton;

    // This is the layout of UndoBarView. The unit is dp.
    //
    //    +-+----+----------------+-+--+----+-+------+--+-+
    // 48 | |    | Deleted        | |  | <- | | UNDO |  | |
    //    +-+----+----------------+-+--+----+-+------+--+-+
    //     4  16                   1 12  32  8        16 4
    public UndoBarView(Context context, BitmapHolder holder) {
        mBarHeight = MediaUtils.dpToPixel(48);
        mBarMargin = MediaUtils.dpToPixel(4);
        mUndoTextMargin = MediaUtils.dpToPixel(16);
        mIconMargin = MediaUtils.dpToPixel(8);
        mIconSize = MediaUtils.dpToPixel(32);
        mSeparatorRightMargin = MediaUtils.dpToPixel(12);
        mSeparatorTopMargin = MediaUtils.dpToPixel(10);
        mSeparatorBottomMargin = MediaUtils.dpToPixel(10);
        mSeparatorWidth = MediaUtils.dpToPixel(1);
        mDeletedTextMargin = MediaUtils.dpToPixel(16);

        mPanel = new NinePatchTexture(context, holder, 
        		BaseResources.getInstance().getDrawableRes(
        				BaseResources.drawable.panel_undo_background));
        mUndoText = StringTexture.newInstance(holder, context.getString(R.string.label_undo),
                MediaUtils.dpToPixel(12), GRAY, 0, true);
        mDeletedText = StringTexture.newInstance(holder, 
                context.getString(R.string.label_deleted),
                MediaUtils.dpToPixel(16), WHITE);
        mUndoIcon = new ResourceTexture(
                context, holder, BaseResources.getInstance().getDrawableRes(
                		BaseResources.drawable.icon_menu_undo));
        mClickRegion = mBarMargin + mUndoTextMargin + mUndoText.getWidth()
                + mIconMargin + mIconSize + mSeparatorRightMargin;
    }

    public void setOnClickListener(OnClickListener listener) {
        mOnClickListener = listener;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredSize(0 /* unused */, mBarHeight);
    }

    @SuppressWarnings("unused")
	@Override
    protected void render(GLCanvas canvas) {
        super.render(canvas);
        advanceAnimation();

        canvas.save(GLCanvas.SAVE_FLAG_ALPHA);
        canvas.multiplyAlpha(mAlpha);

        int w = getWidth();
        int h = getHeight();
        mPanel.draw(canvas, mBarMargin, 0, w - mBarMargin * 2, mBarHeight);

        int x = w - mBarMargin;
        int y;

        x -= mUndoTextMargin + mUndoText.getWidth();
        y = (mBarHeight - mUndoText.getHeight()) / 2;
        mUndoText.draw(canvas, x, y);

        x -= mIconMargin + mIconSize;
        y = (mBarHeight - mIconSize) / 2;
        mUndoIcon.draw(canvas, x, y, mIconSize, mIconSize);

        x -= mSeparatorRightMargin + mSeparatorWidth;
        y = mSeparatorTopMargin;
        canvas.fillRect(x, y, mSeparatorWidth,
                mBarHeight - mSeparatorTopMargin - mSeparatorBottomMargin, GRAY);

        x = mBarMargin + mDeletedTextMargin;
        y = (mBarHeight - mDeletedText.getHeight()) / 2;
        mDeletedText.draw(canvas, x, y);

        canvas.restore();
    }

    @Override
    protected boolean onTouch(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownOnButton = inUndoButton(event);
                break;
            case MotionEvent.ACTION_UP:
                if (mDownOnButton) {
                    if (mOnClickListener != null && inUndoButton(event)) {
                        mOnClickListener.onClick(this);
                    }
                    mDownOnButton = false;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                mDownOnButton = false;
                break;
        }
        return true;
    }

    // Check if the event is on the right of the separator
    private boolean inUndoButton(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int w = getWidth();
        int h = getHeight();
        return (x >= w - mClickRegion && x < w && y >= 0 && y < h);
    }

    ////////////////////////////////////////////////////////////////////////////
    //  Alpha Animation
    ////////////////////////////////////////////////////////////////////////////

    private static final long NO_ANIMATION = -1;
    private static long ANIM_TIME = 200;
    private long mAnimationStartTime = NO_ANIMATION;
    private float mFromAlpha, mToAlpha;
    private float mAlpha;

    private static float getTargetAlpha(int visibility) {
        return (visibility == VISIBLE) ? 1f : 0f;
    }

    @Override
    public void setVisibility(int visibility) {
        mAlpha = getTargetAlpha(visibility);
        mAnimationStartTime = NO_ANIMATION;
        super.setVisibility(visibility);
        invalidate();
    }

    public void animateVisibility(int visibility) {
        float target = getTargetAlpha(visibility);
        if (mAnimationStartTime == NO_ANIMATION && mAlpha == target) return;
        if (mAnimationStartTime != NO_ANIMATION && mToAlpha == target) return;

        mFromAlpha = mAlpha;
        mToAlpha = target;
        mAnimationStartTime = AnimationTime.startTime();

        super.setVisibility(VISIBLE);
        invalidate();
    }

    private void advanceAnimation() {
        if (mAnimationStartTime == NO_ANIMATION) return;

        float delta = (float) (AnimationTime.get() - mAnimationStartTime) /
                ANIM_TIME;
        mAlpha = mFromAlpha + ((mToAlpha > mFromAlpha) ? delta : -delta);
        mAlpha = Utils.clamp(mAlpha, 0f, 1f);

        if (mAlpha == mToAlpha) {
            mAnimationStartTime = NO_ANIMATION;
            if (mAlpha == 0) {
                super.setVisibility(INVISIBLE);
            }
        }
        invalidate();
    }
}
