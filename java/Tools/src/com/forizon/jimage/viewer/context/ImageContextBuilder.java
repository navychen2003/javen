package com.forizon.jimage.viewer.context;

import com.forizon.jimage.viewer.JImageView;
import com.forizon.jimage.viewer.imagelist.ImageIdentityListModel;
import com.forizon.jimage.viewer.imagelist.ImageIdentityListModelIterator;
import com.forizon.jimage.viewer.imagelist.wrapspi.DefaultImageWrapper;
import com.forizon.jimage.viewer.imagelist.wrapspi.FileWrapper;
import com.forizon.jimage.viewer.imagelist.wrapspi.JFMFileWrapper;
import com.forizon.jimage.viewer.imagelist.wrapspi.ObjectImageWrapper;
import com.forizon.jimage.viewer.imagelist.wrapspi.ObjectWrapper;
import com.forizon.jimage.viewer.imagelist.wrapspi.URISchemeWrapper;
import com.forizon.jimage.viewer.imagelist.wrapspi.URIWrapper;
import com.forizon.jimage.viewer.imagelist.wrapspi.URLWrapper;

@SuppressWarnings("rawtypes")
public class ImageContextBuilder {
    final protected Context context;
    final protected ImageContext imageContext;

    public ImageContextBuilder(Context context) {
        this.context = context;
        this.imageContext = new ImageContext(context);
    }

    public ImageContext build() {
        imageContext.imageWrapper = createImageObjectWrapper();
        imageContext.imageIdentityListModel = createImageListModel();
        imageContext.imageIterator = createImageIterator();
        initializeImageWrapper();
        initializeImageIdentityListModel();
        initializeImageIterator();

        return imageContext;
    }

	protected FileWrapper createFileWrapper() {
        return new FileWrapper();
    }

    protected JFMFileWrapper createJFMFileWrapper() {
        return new JFMFileWrapper();
    }

    protected ImageIdentityListModel createImageListModel() {
        return new ImageIdentityListModel();
    }

    protected DefaultImageWrapper createImageWrapper() {
        return new DefaultImageWrapper();
    }

    protected ObjectImageWrapper createImageObjectWrapper() {
        return new ObjectImageWrapper();
    }

    protected ImageIdentityListModelIterator createImageIterator() {
        return new ImageIdentityListModelIterator(
                imageContext.getImageIdentityListModel());
    }

    public void initializeImageIdentityListModel() {}

    public void initializeImageIterator() {
        imageContext.imageIterator.setLooping(
              Boolean.parseBoolean(
                context.configuration.getProperty(JImageView.CONFIGURATION_LOOP)));
        context.configuration.addPropertyChangeListener(
            JImageView.CONFIGURATION_LOOP,
            new LoopPropertyChangeListener(imageContext.imageIterator));
    }

    public void initializeImageWrapper() {
        URLWrapper urlWrapper = createURLWrapper();
        FileWrapper fileWrapper = createFileWrapper();
        JFMFileWrapper jfmfileWrapper = createJFMFileWrapper();
        URIWrapper uriWrapper = createURIWrapper(fileWrapper, urlWrapper, jfmfileWrapper);
        imageContext.imageWrapper.put(fileWrapper);
        imageContext.imageWrapper.put(jfmfileWrapper);
        imageContext.imageWrapper.put(uriWrapper);
        imageContext.imageWrapper.put(urlWrapper);
        imageContext.imageWrapper.put(createImageWrapper());
        imageContext.imageWrapper.put(createObjectWrapper(uriWrapper, fileWrapper));
    }

    protected ObjectWrapper createObjectWrapper(URIWrapper uriWrapper, FileWrapper fileWrapper) {
        return new ObjectWrapper(uriWrapper, fileWrapper);
    }

    protected URIWrapper createURIWrapper(URISchemeWrapper fileWrapper, URISchemeWrapper urlWrapper, 
                                          JFMFileWrapper jfmfileWrapper) {
        URIWrapper uriWrapper = new URIWrapper();
        uriWrapper.setSchemeHandler("file", fileWrapper);
        uriWrapper.setSchemeHandler("sftp", jfmfileWrapper);
        uriWrapper.setSchemeHandler("dfs", jfmfileWrapper);
        uriWrapper.setSchemeHandler(null, urlWrapper);
        return uriWrapper;
    }

    protected URLWrapper createURLWrapper() {
        return new URLWrapper();
    }

}

