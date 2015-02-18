package org.javenstudio.cocoka.view;

import android.graphics.Rect;
import android.graphics.RectF;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import java.util.ArrayList;

import org.javenstudio.cocoka.data.IMediaItem;
import org.javenstudio.cocoka.opengl.Animation;
import org.javenstudio.cocoka.opengl.AnimationTime;
import org.javenstudio.cocoka.opengl.GLCanvas;
import org.javenstudio.cocoka.opengl.RawTexture;

public class PhotoFallbackEffect extends Animation /*implements SlotFilter*/ {

    private static final int ANIM_DURATION = 300;
    private static final Interpolator ANIM_INTERPOLATE = new DecelerateInterpolator(1.5f);

    public static class Entry {
        public int index;
        public IMediaItem item;
        public Rect source;
        public Rect dest;
        public RawTexture texture;

        public Entry(IMediaItem item, Rect source, RawTexture texture) {
            this.item = item;
            this.source = source;
            this.texture = texture;
        }
    }

    public interface PositionProvider {
        public Rect getPosition(int index);
        public int getItemIndex(IMediaItem item);
    }

    private RectF mSource = new RectF();
    private RectF mTarget = new RectF();
    private float mProgress;
    private PositionProvider mPositionProvider;

    private ArrayList<Entry> mList = new ArrayList<Entry>();

    public PhotoFallbackEffect() {
        setDuration(ANIM_DURATION);
        setInterpolator(ANIM_INTERPOLATE);
    }

    public void addEntry(IMediaItem item, Rect rect, RawTexture texture) {
        mList.add(new Entry(item, rect, texture));
    }

    public Entry getEntry(IMediaItem item) {
        for (int i = 0, n = mList.size(); i < n; ++i) {
            Entry entry = mList.get(i);
            if (entry.item == item) return entry;
        }
        return null;
    }

    public boolean draw(GLCanvas canvas) {
        boolean more = calculate(AnimationTime.get());
        for (int i = 0, n = mList.size(); i < n; ++i) {
            Entry entry = mList.get(i);
            if (entry.index < 0) continue;
            entry.dest = mPositionProvider.getPosition(entry.index);
            drawEntry(canvas, entry);
        }
        return more;
    }

    private void drawEntry(GLCanvas canvas, Entry entry) {
        if (!entry.texture.isLoaded()) return;

        int w = entry.texture.getWidth();
        int h = entry.texture.getHeight();

        Rect s = entry.source;
        Rect d = entry.dest;

        // the following calculation is based on d.width() == d.height()

        float p = mProgress;

        float fullScale = (float) d.height() / Math.min(s.width(), s.height());
        float scale = fullScale * p + 1 * (1 - p);

        float cx = d.centerX() * p + s.centerX() * (1 - p);
        float cy = d.centerY() * p + s.centerY() * (1 - p);

        float ch = s.height() * scale;
        float cw = s.width() * scale;

        if (w > h) {
            // draw the center part
            mTarget.set(cx - ch / 2, cy - ch / 2, cx + ch / 2, cy + ch / 2);
            mSource.set((w - h) / 2, 0, (w + h) / 2, h);
            canvas.drawTexture(entry.texture, mSource, mTarget);

            canvas.save(GLCanvas.SAVE_FLAG_ALPHA);
            canvas.multiplyAlpha(1 - p);

            // draw the left part
            mTarget.set(cx - cw / 2, cy - ch / 2, cx - ch / 2, cy + ch / 2);
            mSource.set(0, 0, (w - h) / 2, h);
            canvas.drawTexture(entry.texture, mSource, mTarget);

            // draw the right part
            mTarget.set(cx + ch / 2, cy - ch / 2, cx + cw / 2, cy + ch / 2);
            mSource.set((w + h) / 2, 0, w, h);
            canvas.drawTexture(entry.texture, mSource, mTarget);

            canvas.restore();
        } else {
            // draw the center part
            mTarget.set(cx - cw / 2, cy - cw / 2, cx + cw / 2, cy + cw / 2);
            mSource.set(0, (h - w) / 2, w, (h + w) / 2);
            canvas.drawTexture(entry.texture, mSource, mTarget);

            canvas.save(GLCanvas.SAVE_FLAG_ALPHA);
            canvas.multiplyAlpha(1 - p);

            // draw the upper part
            mTarget.set(cx - cw / 2, cy - ch / 2, cx + cw / 2, cy - cw / 2);
            mSource.set(0, 0, w, (h - w) / 2);
            canvas.drawTexture(entry.texture, mSource, mTarget);

            // draw the bottom part
            mTarget.set(cx - cw / 2, cy + cw / 2, cx + cw / 2, cy + ch / 2);
            mSource.set(0, (w + h) / 2, w, h);
            canvas.drawTexture(entry.texture, mSource, mTarget);

            canvas.restore();
        }
    }

    @Override
    protected void onCalculate(float progress) {
        mProgress = progress;
    }

    public void setPositionProvider(PositionProvider provider) {
        mPositionProvider = provider;
        if (mPositionProvider != null) {
            for (int i = 0, n = mList.size(); i < n; ++i) {
                Entry entry = mList.get(i);
                entry.index = mPositionProvider.getItemIndex(entry.item);
            }
        }
    }

    //@Override
    public boolean acceptSlot(int index) {
        for (int i = 0, n = mList.size(); i < n; ++i) {
            Entry entry = mList.get(i);
            if (entry.index == index) return false;
        }
        return true;
    }
}
