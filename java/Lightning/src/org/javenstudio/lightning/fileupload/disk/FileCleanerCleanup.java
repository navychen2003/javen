package org.javenstudio.lightning.fileupload.disk;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextEvent;

/**
 * A servlet context listener, which ensures that the
 * {@link org.apache.commons.io.FileCleaner FileCleaner's}
 * reaper thread is terminated,
 * when the web application is destroyed.
 *
 * @version $Id: FileCleanerCleanup.java 1454691 2013-03-09 12:15:54Z simonetripodi $
 */
public class FileCleanerCleanup implements ServletContextListener {

    /**
     * Attribute name, which is used for storing an instance of
     * {@link FileCleaningTracker} in the web application.
     */
    public static final String FILE_CLEANING_TRACKER_ATTRIBUTE
        = FileCleanerCleanup.class.getName() + ".FileCleaningTracker";

    /**
     * Returns the instance of {@link FileCleaningTracker}, which is
     * associated with the given {@link ServletContext}.
     *
     * @param pServletContext The servlet context to query
     * @return The contexts tracker
     */
    public static FileCleaningTracker
            getFileCleaningTracker(ServletContext pServletContext) {
        return (FileCleaningTracker)
            pServletContext.getAttribute(FILE_CLEANING_TRACKER_ATTRIBUTE);
    }

    /**
     * Sets the instance of {@link FileCleaningTracker}, which is
     * associated with the given {@link ServletContext}.
     *
     * @param pServletContext The servlet context to modify
     * @param pTracker The tracker to set
     */
    public static void setFileCleaningTracker(ServletContext pServletContext,
            FileCleaningTracker pTracker) {
        pServletContext.setAttribute(FILE_CLEANING_TRACKER_ATTRIBUTE, pTracker);
    }

    /**
     * Called when the web application is initialized. Does
     * nothing.
     *
     * @param sce The servlet context, used for calling
     *   {@link #setFileCleaningTracker(ServletContext, FileCleaningTracker)}.
     */
    public void contextInitialized(ServletContextEvent sce) {
        setFileCleaningTracker(sce.getServletContext(),
                new FileCleaningTracker());
    }

    /**
     * Called when the web application is being destroyed.
     * Calls {@link FileCleaningTracker#exitWhenFinished()}.
     *
     * @param sce The servlet context, used for calling
     *     {@link #getFileCleaningTracker(ServletContext)}.
     */
    public void contextDestroyed(ServletContextEvent sce) {
        getFileCleaningTracker(sce.getServletContext()).exitWhenFinished();
    }

}
