package org.javenstudio.common.indexdb.analysis;

import org.javenstudio.common.indexdb.AlreadyClosedException;
import org.javenstudio.common.indexdb.util.CloseableThreadLocal;

/**
 * Strategy defining how TokenStreamComponents are reused per call to
 * {@link Analyzer#tokenStream(String, java.io.Reader)}.
 */
public abstract class ReuseStrategy {

	private CloseableThreadLocal<Object> mStoredValue = new CloseableThreadLocal<Object>();

    /**
     * Gets the reusable TokenStreamComponents for the field with the given name
     *
     * @param fieldName Name of the field whose reusable TokenStreamComponents
     *        are to be retrieved
     * @return Reusable TokenStreamComponents for the field, or {@code null}
     *         if there was no previous components for the field
     */
    public abstract TokenComponents getReusableComponents(String fieldName);

    /**
     * Stores the given TokenStreamComponents as the reusable components for the
     * field with the give name
     *
     * @param fieldName Name of the field whose TokenStreamComponents are being set
     * @param components TokenStreamComponents which are to be reused for the field
     */
    public abstract void setReusableComponents(String fieldName, TokenComponents components);

    /**
     * Returns the currently stored value
     *
     * @return Currently stored value or {@code null} if no value is stored
     */
    protected final Object getStoredValue() {
    	try {
    		return mStoredValue.get();
    	} catch (NullPointerException npe) {
    		if (mStoredValue == null) 
    			throw new AlreadyClosedException("this Analyzer is closed");
    		else 
    			throw npe;
    	}
    }

    /**
     * Sets the stored value
     *
     * @param storedValue Value to store
     */
    protected final void setStoredValue(Object storedValue) {
    	try {
    		mStoredValue.set(storedValue);
    	} catch (NullPointerException npe) {
    		if (storedValue == null) 
    			throw new AlreadyClosedException("this Analyzer is closed");
    		else 
    			throw npe;
    	}
    }

    /**
     * Closes the ReuseStrategy, freeing any resources
     */
    public void close() {
    	mStoredValue.close();
    	mStoredValue = null;
    }
    
}
