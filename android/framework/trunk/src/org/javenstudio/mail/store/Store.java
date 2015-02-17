package org.javenstudio.mail.store;

import java.util.HashMap;

import org.javenstudio.common.util.Logger;
import org.javenstudio.mail.Flag;
import org.javenstudio.mail.Folder;
import org.javenstudio.mail.MessagingException;

public abstract class Store {
	private static Logger LOG = Logger.getLogger(Store.class); 

	/**
     * String constants for known store schemes.
     */
    public static final String STORE_SCHEME_IMAP = "imap";
    public static final String STORE_SCHEME_POP3 = "pop3";
    public static final String STORE_SCHEME_EAS = "eas";
    public static final String STORE_SCHEME_LOCAL = "local";

    public static final String STORE_SECURITY_SSL = "+ssl";
    public static final String STORE_SECURITY_TLS = "+tls";
    public static final String STORE_SECURITY_TRUST_CERTIFICATES = "+trustallcerts";
    
    
    private static HashMap<String, Store> mStores = new HashMap<String, Store>();
    
    /**
     * Static named constructor.  It should be overrode by extending class.
     * Because this method will be called through reflection, it can not be protected. 
     */
    public static Store newInstance(String uri, PersistentDataCallbacks callbacks)
            throws MessagingException {
        throw new MessagingException("Store.newInstance: Unknown scheme in " + uri);
    }

    private static Store instantiateStore(String className, String uri, PersistentDataCallbacks callbacks)
    		throws MessagingException {
        Object o = null;
        try {
            Class<?> c = Class.forName(className);
            // and invoke "newInstance" class method and instantiate store object.
            java.lang.reflect.Method m =
                c.getMethod("newInstance", String.class, PersistentDataCallbacks.class);
            o = m.invoke(null, uri, callbacks);
        } catch (Exception e) {
        	LOG.error(String.format(
            		"exception when invoking %s.newInstance(String, Context, PersistentDataCallbacks) method for %s",
                    className, uri), e);
            throw new MessagingException("can not instantiate Store object for " + uri, e);
        }
        if (!(o instanceof Store)) {
            throw new MessagingException(
                    uri + ": " + className + " create incompatible object");
        }
        return (Store) o;
    }
    
    /**
     * Get an instance of a mail store. The URI is parsed as a standard URI and
     * the scheme is used to determine which protocol will be used.
     * 
     * Although the URI format is somewhat protocol-specific, we use the following 
     * guidelines wherever possible:
     * 
     * scheme [+ security [+]] :// username : password @ host [ / resource ]
     * 
     * Typical schemes include imap, pop3, local, eas.
     * Typical security models include SSL or TLS.
     * A + after the security identifier indicates "required".
     * 
     * Username, password, and host are as expected.
     * Resource is protocol specific.  For example, IMAP uses it as the path prefix.  EAS uses it
     * as the domain.
     *
     * @param uri The URI of the store.
     * @return an initialized store of the appropriate class
     * @throws MessagingException
     */
    public synchronized static Store getInstance(String uri, PersistentDataCallbacks callbacks) 
    		throws MessagingException {
        Store store = mStores.get(uri);
        if (store == null) {
            StoreInfo info = StoreInfo.getStoreInfo(uri);
            if (info != null) 
                store = instantiateStore(info.getClassName(), uri, callbacks);
            if (store != null) 
                mStores.put(uri, store);
        } else {
            // update the callbacks, which may have been null at creation time.
            store.setPersistentDataCallbacks(callbacks);
        }

        if (store == null) 
            throw new MessagingException("Unable to locate an applicable Store for " + uri);

        return store;
    }
    
    public static Store getInstance(String uri) throws MessagingException {
    	return getInstance(uri, null);
    }
    
    /**
     * Delete an instance of a mail store.
     * 
     * The store should have been notified already by calling delete(), and the caller should
     * also take responsibility for deleting the matching LocalStore, etc.
     * @param storeUri the store to be removed
     */
    public synchronized static void removeInstance(String storeUri) {
    	try { 
	        Store store = mStores.remove(storeUri);
	        if (store != null) 
	        	store.closeConnection();
    	} catch (MessagingException ex) { 
    		LOG.warn("remove store instance error: "+storeUri, ex);
    	}
    }
    
    public synchronized static void removeAllInstances() { 
    	String[] storeUris = mStores.keySet().toArray(new String[0]); 
    	for (int i=0; storeUris != null && i < storeUris.length; i++) { 
    		String storeUri = storeUris[i]; 
    		removeInstance(storeUri);
    	}
    }
    
    /**
     * Get class of sync'er for this Store class
     * @return Message Sync controller, or null to use default
     */
    public StoreSynchronizer getMessageSynchronizer() {
        return null;
    }
    
    /**
     * Some stores cannot download a message based only on the uid, and need the message structure
     * to be preloaded and provided to them.  This method allows a remote store to signal this
     * requirement.  Most stores do not need this and do not need to overload this method, which
     * simply returns "false" in the base class.
     * @return Return true if the remote store requires structure prefetch
     */
    public boolean requireStructurePrefetch() {
        return false;
    }
    
    /**
     * Some protocols require that a sent message be copied (uploaded) into the Sent folder
     * while others can take care of it automatically (ideally, on the server).  This function
     * allows a given store to indicate which mode(s) it supports.
     * @return true if the store requires an upload into "sent", false if this happens automatically
     * for any sent message.
     */
    public boolean requireCopyMessageToSentFolder() {
        return true;
    }
    
    public abstract Folder getFolder(String name) throws MessagingException;
    
    public abstract Folder[] getPersonalNamespaces() throws MessagingException;
    
    public abstract void checkSettings() throws MessagingException;
    
    public abstract void closeFolders(boolean expunge) throws MessagingException;
    
    public abstract void closeConnection() throws MessagingException;
    
    public boolean supportMessageFlag(Flag flag) { 
    	return true;
    }
    
    public boolean supportMessageFlagsAndEnvelope() { 
    	return true;
    }
    
    public boolean supportDeleteMessageFromAnyFolder() { 
    	return false;
    }
    
    /**
     * Delete Store and its corresponding resources.
     * @throws MessagingException
     */
    public void delete() throws MessagingException {
    	// do nothing
    }
    
    /**
     * If a Store intends to implement callbacks, it should be prepared to update them
     * via overriding this method.  They may not be available at creation time (in which case they
     * will be passed in as null.
     * @param callbacks The updated provider of store callbacks
     */
    protected void setPersistentDataCallbacks(PersistentDataCallbacks callbacks) {
    	// do nothing
    }
    
    /**
     * Callback interface by which a Store can read and write persistent data.
     * TODO This needs to be made more generic & flexible
     */
    public interface PersistentDataCallbacks {
        
        /**
         * Provides a small place for Stores to store persistent data.
         * @param key identifier for the data (e.g. "sync.key" or "folder.id")
         * @param value The data to persist.  All data must be encoded into a string,
         * so use base64 or some other encoding if necessary.
         */
        public void setPersistentString(String key, String value);

        /**
         * @param key identifier for the data (e.g. "sync.key" or "folder.id")
         * @param defaultValue The data to return if no data was ever saved for this store
         * @return the data saved by the Store, or null if never set.
         */
        public String getPersistentString(String key, String defaultValue);
    }
	
}
