package org.javenstudio.cocoka.opengl;

import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

public class CaptureAnimation {
    // The amount of change for zooming out.
    private static final float ZOOM_DELTA = 0.2f;
    // Pre-calculated value for convenience.
    private static final float ZOOM_IN_BEGIN = 1f - ZOOM_DELTA;

    private static final Interpolator sZoomOutInterpolator =
            new DecelerateInterpolator();
    private static final Interpolator sZoomInInterpolator =
            new AccelerateInterpolator();
    private static final Interpolator sSlideInterpolator =
        new AccelerateDecelerateInterpolator();

    // Calculate the slide factor based on the give time fraction.
    public static float calculateSlide(float fraction) {
        return sSlideInterpolator.getInterpolation(fraction);
    }

    // Calculate the scale factor based on the given time fraction.
    public static float calculateScale(float fraction) {
        float value;
        if (fraction <= 0.5f) {
            // Zoom in for the beginning.
            value = 1f - ZOOM_DELTA *
                    sZoomOutInterpolator.getInterpolation(fraction * 2);
        } else {
            // Zoom out for the ending.
            value = ZOOM_IN_BEGIN + ZOOM_DELTA *
                    sZoomInInterpolator.getInterpolation((fraction - 0.5f) * 2f);
        }
        return value;
    }
}
