package org.javenstudio.common.entitydb.db;

import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.entitydb.IDatabase;
import org.javenstudio.common.util.Logger;

public final class Databases {
	private static Logger LOG = Logger.getLogger(Databases.class);

	private static final Databases sInstance = new Databases(); 
	private final Map<String, DBManager> mManagers; 
	
	private Databases() {
		mManagers = new HashMap<String, DBManager>(); 
	}
	
	private synchronized DBManager ensureOpen(final String factoryClassName) {
		DBManager manager = mManagers.get(factoryClassName); 
		if (manager == null) {
			manager = new DBManager(createFactory(factoryClassName)); 
			mManagers.put(factoryClassName, manager); 
		}
		return manager; 
	}
	
	private DBFactory createFactory(final String className) {
		if (LOG.isDebugEnabled()) 
			LOG.debug("Databases: create DBFactory: "+className);
		
        try {
            Class<?> clazz = Class.forName(className);
            return (DBFactory)clazz.newInstance();
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(
            		className + " could not be loaded", ex);
        } catch (InstantiationException ex) {
            throw new RuntimeException(
            		className + " could not be instantiated", ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(
            		className + " could not be instantiated", ex);
        }
	}
	
	public static IDatabase getWritableDatabase(final String factoryClassName) {
		return sInstance.ensureOpen(factoryClassName).getWritableDatabase(); 
	}
	
	public static IDatabase getReadableDatabase(final String factoryClassName) {
		return sInstance.ensureOpen(factoryClassName).getReadableDatabase(); 
	}
	
}
