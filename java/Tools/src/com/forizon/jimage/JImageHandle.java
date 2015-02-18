package com.forizon.jimage;

import java.awt.geom.AffineTransform;
import java.awt.Dimension;

/**
 * Provides a simplified interface for manipulating the image displayed by
 * </code>{@link com.forizon.jimage.JImage}</code>s. Multiple
 * <code>JImageHandler</code>s should not handle the same
 * </code>{@link com.forizon.jimage.JImage}</code>.
 */
public class JImageHandle {
    /** </code>{@link com.forizon.jimage.JImage}</code> handled */
    final protected JImage jImage;
    /**
     * Keeps track of whether the image is flipped with
     * {@link #flipHorizontally()}
     */
    protected boolean flippedHorizontally;
    /**
     * Keeps track of whether the image is flipped with
     * {@link #flipVertically()}
     */
    protected boolean flippedVertically;
    /** Keeps track of the image's rotation */
    protected double rotation;
    /** Generated <code>{@link java.awt.geom.AffineTransform}</code> */
    protected AffineTransform transformation;

    /**
     * @param aJImage the jImage to be manipulated
     */
    public JImageHandle (JImage aJImage) {
        jImage = aJImage;
        flippedHorizontally = false;
        flippedVertically = false;
        rotation = 0;
    }

    /**
     * Returns the JImage handled by this handler
     * @return the JImage handled by this handler
     */
    public JImage getJImage() {
        return jImage;
    }

    /**
     * Rotates the currently displayed image clockwise by a radian angle.
     * 
     * @param theta the angle of rotation (in radians). A negative theta is a
     * counter-clockwise rotation.
     */
    public void rotate (double theta) {
        if (transformation == null) {
            transformation = new AffineTransform();
        }
        transformation.rotate(theta);
        rotation += theta;
        update();
    }

    /**
     * Rotates the currently displayed image clockwise by a radian angle.
     * This method will first revert all rotation changes.
     * 
     * @param theta the angle of rotation (in radians)
     */
    public void setRotation (double theta) {
        if (transformation == null) {
            transformation = new AffineTransform();
        }
        transformation.rotate(theta-rotation);
        rotation = theta;
        update();
    }

    /**
     * Shears the currently displayed image.
     *
     * @param shx the horizontal factor
     * @param shy the vertical factor
     */
    public void shear (double shx, double shy) {
        if (transformation == null) {
            transformation = new AffineTransform();
        }
        transformation.shear(shx, shy);
        update();
    }

    /**
     * Shears the currently displayed image.
     * This method will first revert all shear changes.
     *
     * @param shx the horizontal factor
     * @param shy the vertical factor
     */
    public void setShear (double shx, double shy) {
        if (transformation == null) {
            transformation = new AffineTransform();
        }
        transformation.setToShear(shx, shy);
        update();
    }

    /**
     * Scales the currently displayed image. Each axis can be scaled
     * differently.
     *
     * @param sx the muliplier for the horizontal axis; a negative number will
     * cause the image to be flipped on the x axis
     * @param sy the muliplier for the vertical axis; a negative number will
     * cause the image to be flipped on the y axis
     */
    public void scale (double sx, double sy) {
        if (transformation == null) {
            transformation = new AffineTransform();
        }
        transformation.scale(sx, sy);
        update();
    }

    /**
     * Scales the currently displayed image. Each axis can be scaled
     * differently.
     * This method will first revert all scale changes.
     *
     * @param sx the muliplier for the horizontal axis; a negative number will
     * cause the image to be flipped on the x axis
     * @param sy the muliplier for the vertical axis; a negative number will
     * cause the image to be flipped on the y axis
     */
    public void setScale(double sx, double sy) {
        if (transformation == null) {
            transformation = new AffineTransform();
        }
        transformation.setToScale(sx, sy);
        update();
    }
    
    /**
     * Scales the currently displayed image.
     *
     * @param s the scale factor; a negative number will cause the image to be
     * flipped on both the x and y axes.
     */
    public void scale (double s) {
        scale(s, s);
    }

    /**
     * Scales the currently displayed image.
     * This method will first revert all scale changes.
     *
     * @param s the scale factor
     */
    public void setScale(double s) {
        setScale(s, s);
    }

    /**
     * Scales the currently displayed image, but ensures that if the image is
     * not flipped on any axis, it does not get flipped and vice-versa after
     * being zoomed.
     *
     * @param z the zoom factor
     */
    public void zoom(double z) {
        if (transformation == null) {
            transformation = new AffineTransform();
        }
        double x = transformation.getScaleX(),
               y = transformation.getScaleY();
        // If the scale starts at 0, we do not know if the image is currently
        // flipped, so it is prohibited
        if (x != 0 && y != 0) {
            x += (x < 0)? ((x - z >= 0)? 0 : -z)
                        : ((x + z <= 0)? 0 : z);
            y += (y < 0)? ((y - z >= 0)? 0 : -z)
                        : ((y + z <= 0)? 0 : z);
            setScale(x, y);
        }
    }

    /**
     * Scales the currently displayed image, but ensures that if the image is
     * not flipped on any axis, it does not get flipped and vice-versa after
     * being zoomed.
     * This method works like {@link #scale(double)}, except that the zoom can
     * not be 0 (which will flip the image).
     *
     * @param z the zoom factor
     */
    public void setZoom(double z) {
        if (transformation == null) {
            transformation = new AffineTransform();
        }
        double x = transformation.getScaleX(),
               y = transformation.getScaleY();
        if (z != 0 && x != 0 && y != 0) {
            x = (x < 0)? -z: z;
            y = (y < 0)? -z: z;
            setScale(x, y);
        }
    }

    /**
     * Sets the zoom so that the image is displayed such that as much of the
     * given size is used while maintaining the width/height ratio
     *
     * @param size the dimension that the image should fill
     */
    public void fit (Dimension size) {
        Dimension sourceDimension = jImage.getSourceImageSize();
        double sourceScale = sourceDimension.width / (double)sourceDimension.height,
               targetScale = size.getWidth() / (double)size.getHeight(),
               zoom = 1.0;
        if (sourceScale > targetScale) {
            zoom = size.width / (double)sourceDimension.width;
        } else if (sourceScale < targetScale) {
            zoom = size.height / (double)sourceDimension.height;
        }
        setZoom(zoom);
    }

    /**
     * Equivalent to
     * <code>{@link #scale(double, double) scale(-1.0, 1.0)}</code>
     */
    public void flipVertically () {
        scale(1.0, -1.0);
    }

    /**
     * Performs {@link #flipVertically()} if the given value is not the same
     * as whether or not the image is already flipped on the vertical axis.
     *
     * @param value
     */
    public void setFlipVertically (boolean value) {
        if (value != flippedVertically) {
            flipVertically();
            flippedVertically = value;
        }
    }

    /**
     * Equivalent to {@link #scale(double, double) scale(-1.0, 1.0)}.
     */
    public void flipHorizontally () {
        scale(-1.0, 1.0);
    }

    /**
     * Performs {@link #flipHorizontally()} if the given value is not the same
     * as whether or not the image is already flipped on the horizontal axis.
     *
     * @param value
     */
    public void setFlipHorizontally (boolean value) {
        if (value != flippedHorizontally) {
            flipHorizontally();
            flippedHorizontally = value;
        }
    }

    /**
     * Revert all changes made to the image.
     */
    public void reset() {
        transformation = null;
        jImage.setTransformation(null);
        flippedHorizontally = false;
        flippedVertically = false;
        rotation = 0;
    }

    /**
     * Have the wrapped <code>JImage</code> use <code>transformation</code> to
     * update the transformation used
     */
    protected void update() {
        jImage.setTransformation((AffineTransform)transformation.clone());
    }
}
