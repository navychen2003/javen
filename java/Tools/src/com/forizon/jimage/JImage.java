package com.forizon.jimage;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.JComponent;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>{@link javax.swing.JComponent}</code> which displays images.
 * Transformations can be applied to the displayed canvas using
 * <code>{@link java.awt.geom.AffineTransform}</code>ations. Also note that
 * <code>{@link com.forizon.jimage.JImageHandle}</code> is a wrapper which can
 * be used to simplify common tasks such as rotate, scale, flip, etc.
 *
 * @see com.forizon.jimage.JImageHandle
 */
public class JImage extends JComponent {
	private static final long serialVersionUID = 1L;
	/**
     * <code>PROP_IMAGE</code> indicates an image change
     */
    public static final String PROP_IMAGE = "image";
    /**
     * <code>PROP_BOUNDS</code> indicates an image bounds change
     */
    public static final String PROP_BOUNDS = "bounds";
    /**
     * <code>PROP_ACCEPTDROP</code> indicates the value for acceptDrop has change
     */
    public static final String PROP_ACCEPTDROP = "acceptDrop";
    /**
     * <code>PROP_TRANSFORMATION</code> indicates a change in the transformation used
     */
    public static final String PROP_TRANSFORMATION = "transformation";

    /** Image to be displayed */
    Image image;
    /** Image rectangle after transformations are applied */
    Rectangle bounds;
    /** The <code>{@link java.awt.geom.AffineTransform}</code> used */
    AffineTransform userTransformation;
    /** AffineTransform used to center and skew (fit to bounds) the canvas */
    AffineTransform innerTransformation;
    /** Image bounds before transformations */
    Dimension sourceDimension;
    /** Handles canvas loading event notifications */
    ImageObserver imageObserver;
    /** dnd support */
    DropTarget dropTarget;

    /** Create a <code>JImage</code> */
    public JImage() {
        super();
        bounds = new Rectangle();
        sourceDimension = new Dimension();
        imageObserver = this;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                dropTarget = new DropTarget(JImage.this, new DropTargetListener());
            }
        });
    }

    /**
     * Enables notifications from this component to the specified listener when
     * an canvas is dropped onto and accepted by the component.
     */
    public synchronized
            void addURIDropListener(URIDropListener listener) {
        listenerList.add(URIDropListener.class, listener);
    }

    /**
     * Disables notifications from this component to the specified listener when
     * an canvas is dropped onto and accepted by the component.
     */
    public synchronized
            void removeURIDropListener(URIDropListener listener) {
        listenerList.remove(URIDropListener.class, listener);
    }

    /**
     * Sets whether <code>{@link DropTargetListener}</code> is active and used
     * to handle drag and drop operations.
     */
    public void setAcceptDrop(boolean value) {
        boolean old = dropTarget.isActive();
        dropTarget.setActive(value);
        firePropertyChange(PROP_ACCEPTDROP, old, value);
    }

    /**
     * Sets the <code>ImageObserver</code> used by this component
     */
    public void setImageObserver (ImageObserver aImageObserver) {
        imageObserver = aImageObserver;
    }

    /**
     * Sets the <code>{@link java.awt.Image}</code> displayed by this component.
     * @param value the canvas. If set to null, nothing will be displayed.
     */
    public void setImage(Image value) {
        // Allow the old canvas memory to be freed before loading the new canvas
        // if not needed
        if (getPropertyChangeListeners(PROP_IMAGE).length == 0) {
            image = null;
        }

        Image old = image;
        setImageSilent(value);
        firePropertyChange(PROP_IMAGE, old, image);
    }

    /**
     * Sets the <code>{@link java.awt.Image}</code> displayed by this component.
     * <p> 
     * Converts given <code>{@link javax.swing.Icon}</code> to <code>Image</code>
     * and then passes <code>Image</code> to
     * <code>{@link #setImage(java.awt.Image)}</code>.
     * @param newImage an image encapsulated by an <code>Icon</code>. If set to
     * null, no image will be displayed.
     */
    public void setImage(Icon newImage) {
        if (newImage == null) {
            setImage((Image)null);
        } else if (newImage instanceof ImageIcon) {
            setImage(((ImageIcon)newImage).getImage());
        } else {
            // Adapted from code posted by "DrLaszloJamf" located here:
            // http://forums.sun.com/thread.jspa?messageID=9548703#9548703
            int w = newImage.getIconWidth();
            int h = newImage.getIconHeight();
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            BufferedImage canvas = gc.createCompatibleImage(w, h);
            Graphics2D g = canvas.createGraphics();
            newImage.paintIcon(null, g, 0, 0);
            g.dispose();
            setImage(canvas);
        }
    }

    /**
     * Sets the image displayed by this component.
     * <p> 
     * This method creates an <code>{@link java.awt.Image}</code> using
     * <code>{@link javax.imageio.ImageIO#read(java.io.InputStream)}</code>
     * and then calls <code>{@link #setImage(java.awt.Image)}</code>.
     *
     * @see javax.imageio.ImageIO#read(java.io.InputStream)
     * @see #setImage(java.awt.Image)
     * @throws IOException if the canvas could not be read
     * @param newImage an <code>{@link java.io.InputStream}</code> source for an
     * canvas. If set to null, no image will be displayed.
     */
    public void setImage (InputStream newImage)
      throws IOException {
        if (newImage == null) {
            setImage((Image)null);
        } else {
            setImage(ImageIO.read(newImage));
        }
    }

    /**
     * Sets the image displayed by this component.
     * <p> 
     * This method creates an <code>{@link java.awt.Image}</code> using
     * <code>{@link javax.imageio.ImageIO#read(java.io.File)}</code>
     * and then calls <code>{@link #setImage(java.awt.Image)}</code>.
     *
     * @see javax.imageio.ImageIO#read(java.io.File)
     * @see #setImage(java.awt.Image)
     * @throws IOException if the canvas could not be read
     * @param newImage a <code>{@link java.io.File}</code> source for an canvas.
     * If set to null, no image will be displayed.
     */
    public void setImage (File newImage)
      throws IOException {
        if (newImage == null) {
            setImage((Image)null);
        } else {
            setImage(ImageIO.read(newImage));
        }
    }

    /**
     * Sets the image displayed by this component.
     * <p> 
     * This method creates an <code>{@link java.awt.Image}</code> using
     * <code>{@link javax.imageio.ImageIO#read(java.net.URL)}</code>
     * and then calls <code>{@link #setImage(java.awt.Image)}</code>.
     *
     * @see javax.imageio.ImageIO#read(java.net.URL)
     * @see #setImage(java.awt.Image)
     * @throws IOException if the canvas could not be read
     * @param newImage a <code>{@link java.net.URL}</code> source for an canvas.
     * If set to null, no image will be displayed.
     */
    public void setImage (URL newImage)
      throws IOException {
        if (newImage == null) {
            setImage((Image)null);
        } else {
            setImage(ImageIO.read(newImage));
        }
    }

    /**
     * Sets the image displayed by this component.
     * <p> 
     * This method creates an <code>{@link java.awt.Image}</code> using
     * <code>{@link javax.imageio.ImageIO#read(java.net.URL)}</code>
     * and then calls <code>{@link #setImage(java.awt.Image)}</code>.
     *
     * @see javax.imageio.ImageIO#read(java.net.URL)
     * @see #setImage(java.awt.Image)
     * @throws IOException if the canvas could not be read
     * @throws IllegalArgumentException if the URI is not absolute
     * @throws MalformedURLException if the URI can not be converted to a URL
     * @param newImage a <code>{@link java.net.URL}</code> source for an canvas.
     * If set to null, no image will be displayed.
     */
    public void setImage (URI newImage)
      throws IOException {
        if (newImage == null) {
            setImage((Image)null);
        } else {
            setImage(newImage.toURL());
        }
    }

    /**
     * Sets the image displayed by this component.
     * <p> 
     * This method creates an <code>{@link java.awt.Image}</code> using
     * <code>{@link javax.imageio.ImageIO#read(java.io.File)}</code> which
     * represents the file specified by the given argument and then calls
     * <code>{@link #setImage(java.awt.Image)}</code>.
     *
     * @see java.io.File
     * @see javax.imageio.ImageIO#read(java.io.File)
     * @see #setImage(java.awt.Image)
     * @throws IOException if the canvas could not be read
     * @param newImage a filesource for an canvas represented as a string path.
     * If set to null, no image will be displayed.
     */
    public void setImage (String newImage)
      throws IOException {
        if (newImage == null) {
            setImage((Image)null);
        } else {
            setImage(new File(newImage));
        }
    }

    /**
     * Sets the <code>{@link java.awt.geom.AffineTransform}<code> used to
     * transform the canvas before being displayed.
     * 
     * @param aTransformation userTransformation to apply when displaying the\
     * canvas
     */
    public void setTransformation(AffineTransform aTransformation) {
        AffineTransform old = userTransformation;
        userTransformation = aTransformation;
        updateBounds();
        revalidate();
        repaint();
        firePropertyChange(PROP_TRANSFORMATION, old, userTransformation);
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        boolean resized = width != getWidth() || height != getHeight();
        super.setBounds(x, y, width, height);
        if (resized) {
            prepareCenteredTransformation();
        }
    }

    /**
     * Returns whether the component accepts dnd
     * @return whether the component accepts dnd
     */
    public boolean getAcceptDrop() {
        return dropTarget.isActive();
    }

    /**
     * Returns a clone of the canvas userTransformation used to display the canvas
     * @return a clone of the canvas userTransformation used to display the canvas
     */
    public AffineTransform getTransformation() {
        AffineTransform result = null;
        if (userTransformation != null) {
            result = (AffineTransform)userTransformation.clone();
        }
        return result;
    }

    /**
     * Returns true if an canvas is set, false otherwise
     * @return true if an canvas is set, false otherwise
     */
    public boolean isSetImage() {
        return image != null;
    }

    /**
     * Returns a clone of canvas being displayed
     * @return a clone of canvas being displayed
     */
    public Image getImage() {
        return image;
    }

    /**
     * Returns the size of the source canvas
     * @return the size of the source canvas
     */
    public Dimension getSourceImageSize() {
        return (Dimension)sourceDimension.clone();
    }

    /**
     * Returns the size of the canvas after being transformed
     * @return the size of the canvas after being transformed
     */
    public Dimension getTransformedImageSize() {
        return bounds.getSize();
    }

    /**
     * If the <code>preferredSize</code> has been set to a non-<code>null</code>
     * value just returns it. Otherwise returns the result of
     * <code>{@link #getTransformedImageSize()}</code>.
     * @see #getTransformedImageSize()
     * @return the result of <code>getTransformedImageSize()</code>
     */
    @Override
    public Dimension getPreferredSize() {
        return (image == null || isPreferredSizeSet())
                ? super.getPreferredSize()
                : getTransformedImageSize();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D)g;
        g2d.setBackground(getBackground());
        g2d.clearRect(0, 0, getWidth(), getHeight());
        if (image != null) {
            // if an canvas has been set, draw it
            g2d.drawImage(image, innerTransformation, imageObserver);
        }
        // dispose the canvas
        g.dispose();
    }

    /**
     * Sets the <code>{@link java.awt.Image}</code> displayed by this component.
     * @param value the canvas. If set to null, no image will be displayed.
     */
    protected void setImageSilent(Image value) {
        image = value;
        sourceDimension = new Dimension();
        if (image != null) {
            sourceDimension.width = image.getWidth(imageObserver);
            sourceDimension.height = image.getHeight(imageObserver);
        }
        updateBounds();
        revalidate();
        repaint();
    }

    /**
     * Creates the translation needed to center the displayed canvas concatenated
     * with the component's userTransformation.
     * 
     * @return <code>AffineTransform</code> that centers the canvas
     */
    void prepareCenteredTransformation () {
        innerTransformation = new AffineTransform();
        if (image != null) {
            // Apply transformations
            Dimension size = getSize();
            // Tracks the lengths needed to center canvas
            int x = -bounds.x, y = -bounds.y;
            float xScale = 1, yScale = 1;
            if (bounds.height < size.height) {
                y += (size.height - bounds.height) / 2;
            } else if (bounds.height > 0) {
                yScale = (float)size.height / bounds.height;
            }
            if (bounds.width < size.width) {
                x += (size.width - bounds.width) / 2;
            } else if (bounds.width > 0) {
                xScale = (float)size.width / bounds.width;
            }
            innerTransformation.scale(xScale, yScale);
            innerTransformation.translate(x, y);
            if (userTransformation != null) {
                innerTransformation.concatenate(userTransformation);
            }
        }
    }

    /**
     * Update the size and location of the displayed canvas after the
     * userTransformation is applied
     */
    void updateBounds() {
        Rectangle old = bounds;
        bounds = new Rectangle(sourceDimension);
        if (image != null && userTransformation != null) {
            // Determine the bounding rectangle of the canvas before
            // userTransformation
            Point[] points = new Point[] {
              new Point(0, 0),
              new Point(sourceDimension.width, 0),
              new Point(0, sourceDimension.height),
              new Point(sourceDimension.width, sourceDimension.height)
            };
            Point bottomRight = new Point();
            for (Point current: points) {
                userTransformation.deltaTransform(current, current);
                if (bounds.x > current.x) {
                    bounds.x = current.x;
                }
                if (bounds.y > current.y) {
                    bounds.y = current.y;
                }
                if (bottomRight.x < current.x) {
                    bottomRight.x = current.x;
                }
                if (bottomRight.y < current.y) {
                    bottomRight.y = current.y;
                }
            }
            bounds.width = Math.abs(bottomRight.x - bounds.x);
            bounds.height = Math.abs(bottomRight.y - bounds.y);
        }
        firePropertyChange(PROP_BOUNDS, old, bounds);
        prepareCenteredTransformation();
    }
}

/**
 * This <code>{@link java.awt.dnd.DropTargetListener}>/code> accepts objects
 * "dropped" onto <code>{@link JImage}>/code> components and tries to load the
 * object into the recieving JImage.
 */
class DropTargetListener extends java.awt.dnd.DropTargetAdapter {
    @SuppressWarnings("unchecked")
	@Override
    public void drop(DropTargetDropEvent dropTargetDropEvent) {
        JImage jImage = (JImage)((DropTarget)dropTargetDropEvent.getSource()).getComponent();
        Transferable tr = dropTargetDropEvent.getTransferable();
        boolean isSearchingSupported = true;
        if (tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            dropTargetDropEvent.acceptDrop(DnDConstants.ACTION_COPY);
            try {
                List<File> files = (List<File>)tr.getTransferData(DataFlavor.javaFileListFlavor);
                acceptFileList(files, jImage);
                isSearchingSupported = false;
            }
            catch (IOException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
            } catch (UnsupportedFlavorException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
            }
        }
        if (isSearchingSupported && tr.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            dropTargetDropEvent.acceptDrop(DnDConstants.ACTION_COPY);
            try {
                String[] files = ((String)tr.getTransferData(DataFlavor.stringFlavor)).split("[\n\r]+");
                acceptString(files, jImage);
                isSearchingSupported = false;
            }
            catch (IOException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
            } catch (UnsupportedFlavorException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
            }
        }
        if (isSearchingSupported) {
            dropTargetDropEvent.rejectDrop();
        }
        dropTargetDropEvent.dropComplete(true);
    }

    /**
     * Treats the "dropped" object as a <code>{@link java.io.File}>/code> list
     * and makes the source <code>{@link JImage}</code> component attempt to
     * load each file, stopping once one has successfully been loaded.
     *
     * @param tr the transfered object
     * @param jImage the source JImage component
     * @throws java.io.IOException if the data is no longer avaliable as strings
     * @throws java.awt.datatransfer.UnsupportedFlavorException if the data does
     * not support the string flavor
     */
    void acceptFileList(List<File> files, JImage jImage) {
        boolean isFiring = true;
        // Iterate through each file and attempt to laod as a URI
        for (int i = 0; i < files.size() && isFiring; i++) {
            fireURIDropEvent(files.get(i).toURI(), jImage);
            isFiring = false;
        }
    }

    /**
     * Treats the "dropped" object as a string list and makes the source
     * <code>{@link JImage}</code> component attempt load each.
     *
     * @param tr the transfered object
     * @param jImage the source JImage component
     * @throws java.io.IOException if the data is no longer avaliable as strings
     * @throws java.awt.datatransfer.UnsupportedFlavorException if the data does
     * not support the files list flavor
     */
    void acceptString(String[] files, JImage jImage) {
        boolean prime = true;
        // Iterate through each string and attempt to laod as a URI
        for (int i = 0; i < files.length && prime; i++) {
            try {
                fireURIDropEvent(new URI(files[i]), jImage);
                prime = false;
            } catch (URISyntaxException e) {
                logger.log(Level.INFO, e.getMessage(), e);
            }
        }
    }
    /**
     * Notifies all <code>URIDropListener</code> that an image has been
     * dropped and accepted by the component.
     *
     * @param uri the URI of the dropped image
     * @param jImage the source JImage component
     */
    void fireURIDropEvent(URI uri, JImage jImage) {
        URIDropListener[] targets;
        synchronized (jImage) {
            targets = (URIDropListener[])jImage.getListeners(URIDropListener.class);
        }

        URIDropEvent event = new URIDropEvent(this, uri);
        for (int i = 0; i < targets.length; i++) {
            targets[i].drop(event);
        }
    }

    private static final Logger logger = Logger.getLogger(
            DropTargetListener.class.getPackage().toString());
}

