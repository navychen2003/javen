package org.javenstudio.lightning.fileupload;

import java.io.InputStream;
import java.io.IOException;

/**
 * <p>Abstracts access to the request information needed for file uploads. This
 * interface should be implemented for each type of request that may be
 * handled by FileUpload, such as servlets and portlets.</p>
 *
 * @since FileUpload 1.1
 *
 * @version $Id: RequestContext.java 1455861 2013-03-13 10:12:09Z simonetripodi $
 */
public interface RequestContext {

    /**
     * Retrieve the character encoding for the request.
     *
     * @return The character encoding for the request.
     */
    String getCharacterEncoding();

    /**
     * Retrieve the content type of the request.
     *
     * @return The content type of the request.
     */
    String getContentType();

    /**
     * Retrieve the content length of the request.
     *
     * @return The content length of the request.
     * @deprecated 1.3 Use {@link UploadContext#getUploadLength()} instead
     */
    @Deprecated
    int getContentLength();

    /**
     * Retrieve the input stream for the request.
     *
     * @return The input stream for the request.
     *
     * @throws IOException if a problem occurs.
     */
    InputStream getInputStream() throws IOException;

}
