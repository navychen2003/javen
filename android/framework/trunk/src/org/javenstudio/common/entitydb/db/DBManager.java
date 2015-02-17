package org.javenstudio.common.entitydb.db;

import org.javenstudio.common.entitydb.IDatabase;
import org.javenstudio.common.util.Logger;

public final class DBManager {
	private static Logger LOG = Logger.getLogger(DBManager.class);

	private final DBFactory mFactory;
	//private final int mNewVersion;

    private IDatabase mDatabase = null;
    private boolean mIsInitializing = false;
    
    public DBManager(DBFactory factory) {
    	//int version = factory.getVersion(); 
        //if (version < 1) throw new IllegalArgumentException("Version must be >= 1, was " + version);

        mFactory = factory;
        //mNewVersion = version;
    }
    
    public synchronized IDatabase getWritableDatabase() {
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
        IDatabase db = null;
        if (mDatabase != null) mDatabase.lock();
        try {
            mIsInitializing = true;
            db = mFactory.openWritableDatabase();

            //int version = db.getVersion();
            //if (version != mNewVersion) {
            //    db.beginTransaction();
            //    try {
            //        if (version == 0) {
            //            onCreate(db);
            //        } else {
            //            onUpgrade(db, version, mNewVersion);
            //        }
            //        db.setVersion(mNewVersion);
            //        db.setTransactionSuccessful();
            //    } finally {
            //        db.endTransaction();
            //    }
            //}
            
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
                if (mDatabase != null) onOpen(mDatabase);
            } else {
                if (mDatabase != null) mDatabase.unlock();
                if (db != null) db.close();
            }
        }
    }
    
    public synchronized IDatabase getReadableDatabase() {
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
            LOG.error("Couldn't open " + mFactory.getDatabaseName() + " for writing (will try read-only):", e);
        }

        IDatabase db = null;
        try {
            mIsInitializing = true;
            db = mFactory.openReadableDatabase();
            //if (db.getVersion() != mNewVersion) {
            //    Log.e(Constants.getTag(), "Can't upgrade read-only database from version " +
            //            db.getVersion() + " to " + mNewVersion + ": " + mFactory.getDatabasePath());
            //}

            LOG.warn("Opened " + mFactory.getDatabaseName() + " in read-only mode");
            mDatabase = db;
            return mDatabase;
        } finally {
            mIsInitializing = false;
            if (db != null && db != mDatabase) db.close();
            if (mDatabase != null) onOpen(mDatabase);
        }
    }
    
    public synchronized void close() {
        if (mIsInitializing) throw new IllegalStateException("Closed during initialization");

        if (mDatabase != null && mDatabase.isOpen()) {
            mDatabase.close();
            mDatabase = null;
        }
    }
    
    public void onOpen(IDatabase db) {
    	mFactory.onOpenDatabase(db); 
    }
    
}
