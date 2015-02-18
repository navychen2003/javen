package org.javenstudio.common.entitydb.rdb;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.javenstudio.common.entitydb.DBException;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.util.Log;

/**
 * A helper class to manage database creation and version management.
 *
 * <p>You create a subclass implementing {@link #onCreate}, {@link #onUpgrade} and
 * optionally {@link #onOpen}, and this class takes care of opening the database
 * if it exists, creating it if it does not, and upgrading it as necessary.
 * Transactions are used to make sure the database is always in a sensible state.
 *
 * <p>This class makes it easy for 
 * implementations to defer opening and upgrading the database until first use,
 * to avoid blocking application startup with long-running database upgrades.
 *
 * <p>For an example, see the NotePadProvider class in the NotePad sample application,
 * in the <em>samples/</em> directory of the SDK.</p>
 */
public abstract class DBOpenHelper {
    private static final String TAG = DBOpenHelper.class.getSimpleName();

    protected final String mName;
    protected final int mNewVersion;

    private Database mDatabase = null;
    private boolean mIsInitializing = false;
    
    private final Map<String, DBTable<? extends IIdentity, ? extends IEntity<?>> > mTables; 
    private final DatabaseFactory mFactory; 

    /**
     * Create a helper object to create, open, and/or manage a database.
     * This method always returns very quickly.  The database is not actually
     * created or opened until one of {@link #getWritableDatabase} or
     * {@link #getReadableDatabase} is called.
     *
     * @param context to use to open or create the database
     * @param name of the database file, or null for an in-memory database
     * @param factory to use for creating cursor objects, or null for the default
     * @param version number of the database (starting at 1); if the database is older,
     *     {@link #onUpgrade} will be used to upgrade the database
     */
    public DBOpenHelper(DatabaseFactory factory, String name, int version) {
        if (version < 1) throw new IllegalArgumentException("Version must be >= 1, was " + version);

        mFactory = factory;
        mName = name;
        mNewVersion = version;
        
        mTables = new HashMap<String, DBTable<? extends IIdentity, ? extends IEntity<?>> >(); 
    }

    public final DatabaseFactory getFactory() { 
    	return mFactory;
    }
    
    protected void registerTable(String name, DBTable<? extends IIdentity, ? extends IEntity<?>> table) {
    	if (name == null || table == null) 
    		throw new IllegalArgumentException("table or name is null"); 
    	
    	synchronized (this) {
    		if (mDatabase != null) 
    			throw new DBException("table cannot register if database already opened"); 
    		
    		if (mTables.containsKey(name)) 
    			throw new DBException("table: "+name+" already registered"); 
    		
    		mTables.put(name, table); 
    	}
    }
    
    public String[] getTableNames() {
    	synchronized (this) {
    		Set<String> set = mTables.keySet(); 
    		return set.toArray(new String[set.size()]); 
    	}
    }
    
    @SuppressWarnings({"unchecked"})
    public <K extends IIdentity, T extends IEntity<K>> DBTable<K,T> getTable(String name) {
    	if (name == null) 
    		throw new IllegalArgumentException("table name is null"); 
    	
    	synchronized (this) {
    		return (DBTable<K,T>)mTables.get(name); 
    	}
    }
    
    public String getDatabaseName() {
    	return mName; 
    }
    
    public abstract String getDatabaseDirectory();
    
    public String getDatabasePath() {
    	if (mName == null || mName.length() == 0) 
    		return null; 
    	
    	return (new File(getDatabaseDirectory(), mName)).getAbsolutePath(); 
    }
    
    protected Database openWritableDatabase(String path) {
    	if (path == null) {
    		return mFactory.createDatabase(); 
    	} else {
	        return mFactory.openWritableDatabase(path); 
    	}
    }
    
    protected Database openReadableDatabase(String path) {
    	if (path == null) {
    		return mFactory.createDatabase(); 
    	} else {
	        return mFactory.openReadableDatabase(path); 
    	}
    }
    
    /**
     * Create and/or open a database that will be used for reading and writing.
     * The first time this is called, the database will be opened and
     * {@link #onCreate}, {@link #onUpgrade} and/or {@link #onOpen} will be
     * called.
     *
     * <p>Once opened successfully, the database is cached, so you can
     * call this method every time you need to write to the database.
     * (Make sure to call {@link #close} when you no longer need the database.)
     * Errors such as bad permissions or a full disk may cause this method
     * to fail, but future attempts may succeed if the problem is fixed.</p>
     *
     * <p class="caution">Database upgrade may take a long time, you
     * should not call this method from the application main thread
     *
     * @throws SQLiteException if the database cannot be opened for writing
     * @return a read/write database object valid until {@link #close} is called
     */
    public synchronized Database getWritableDatabase() {
        if (mDatabase != null && mDatabase.isOpen() && !mDatabase.isReadOnly()) {
            return mDatabase;  // The database is already open for business
        }

        if (mIsInitializing) {
            throw new IllegalStateException("getWritableDatabase called recursively");
        }

        // If we have a read-only database open, someone could be using it
        // (though they shouldn't), which would cause a lock to be held on
        // the file, and our attempts to open the database read-write would
        // fail waiting for the file lock.  To prevent that, we acquire the
        // lock on the read-only database, which shuts out other users.

        boolean success = false;
        Database db = null;
        if (mDatabase != null) mDatabase.lock();
        try {
            mIsInitializing = true;
            db = openWritableDatabase(getDatabasePath());

            int version = db.getVersion();
            if (version != mNewVersion) {
                db.beginTransaction();
                try {
                    if (version == 0) {
                        onCreate(db);
                    } else {
                        onUpgrade(db, version, mNewVersion);
                    }
                    db.setVersion(mNewVersion);
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            onOpen(db);
            success = true;
            return db;
        } finally {
            mIsInitializing = false;
            if (success) {
                if (mDatabase != null) {
                    try { mDatabase.close(); } catch (Exception e) { }
                    mDatabase.unlock();
                }
                mDatabase = db;
            } else {
                if (mDatabase != null) mDatabase.unlock();
                if (db != null) db.close();
            }
        }
    }
    
    /**
     * Create and/or open a database.  This will be the same object returned by
     * {@link #getWritableDatabase} unless some problem, such as a full disk,
     * requires the database to be opened read-only.  In that case, a read-only
     * database object will be returned.  If the problem is fixed, a future call
     * to {@link #getWritableDatabase} may succeed, in which case the read-only
     * database object will be closed and the read/write object will be returned
     * in the future.
     *
     * <p class="caution">Like {@link #getWritableDatabase}, this method may
     * take a long time to return, so you should not call it from the
     * application main thread
     *
     * @throws SQLiteException if the database cannot be opened
     * @return a database object valid until {@link #getWritableDatabase}
     *     or {@link #close} is called.
     */
    public synchronized Database getReadableDatabase() {
        if (mDatabase != null && mDatabase.isOpen()) {
            return mDatabase;  // The database is already open for business
        }

        if (mIsInitializing) {
            throw new IllegalStateException("getReadableDatabase called recursively");
        }

        try {
            return getWritableDatabase();
        } catch (Exception e) {
            //if (mName == null) throw e;  // Can't open a temp database read-only!
            Log.e(TAG, "Couldn't open " + mName + " for writing (will try read-only):", e);
        }

        Database db = null;
        try {
            mIsInitializing = true;
            String path = getDatabasePath(); 
            db = openReadableDatabase(path);
            if (db.getVersion() != mNewVersion) {
                Log.e(TAG, "Can't upgrade read-only database from version " +
                        db.getVersion() + " to " + mNewVersion + ": " + path);
            }

            onOpen(db);
            Log.w(TAG, "Opened " + mName + " in read-only mode");
            mDatabase = db;
            return mDatabase;
        } finally {
            mIsInitializing = false;
            if (db != null && db != mDatabase) db.close();
        }
    }

    /**
     * Close any open database object.
     */
    public synchronized void close() {
        if (mIsInitializing) throw new IllegalStateException("Closed during initialization");

        if (mDatabase != null && mDatabase.isOpen()) {
            mDatabase.close();
            mDatabase = null;
        }
    }

    /**
     * Called when the database is created for the first time. This is where the
     * creation of tables and the initial population of the tables should happen.
     *
     * @param db The database.
     */
    public void onCreate(Database db) {
    	String[] names = getTableNames(); 
		for (int i=0; names != null && i < names.length; i++) {
			db.createTable(getTable(names[i]));
		} 
    }

    /**
     * Called when the database needs to be upgraded. The implementation
     * should use this method to drop tables, add tables, or do anything else it
     * needs to upgrade to the new schema version.
     *
     * <p>The SQLite ALTER TABLE documentation can be found
     * <a href="http://sqlite.org/lang_altertable.html">here</a>. If you add new columns
     * you can use ALTER TABLE to insert them into a live table. If you rename or remove columns
     * you can use ALTER TABLE to rename the old table, then create the new table and then
     * populate the new table with the contents of the old table.
     *
     * @param db The database.
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     */
    public abstract void onUpgrade(Database db, int oldVersion, int newVersion);

    /**
     * Called when the database has been opened.  The implementation
     * should check {@link Database#isReadOnly} before updating the
     * database.
     *
     * @param db The database.
     */
    public void onOpen(Database db) {}
}