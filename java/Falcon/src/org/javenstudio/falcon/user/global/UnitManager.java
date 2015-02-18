package org.javenstudio.falcon.user.global;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;

public class UnitManager {
	private static final Logger LOG = Logger.getLogger(UnitManager.class);
	
	private final IUnitStore mStore;
	private volatile boolean mClosed = false;
	
	public UnitManager(IUnitStore store) { 
		if (store == null) throw new NullPointerException();
		mStore = store;
	}
	
	public IUnitStore getStore() { return mStore; }
	public boolean isClosed() { return mClosed; }
	
	public void loadUnits(boolean force) throws ErrorException {
		if (LOG.isDebugEnabled()) LOG.debug("loadUnits: force=" + force);
	}
	
	public void addUnit(Unit item) throws ErrorException {
		if (item == null) return;
		UnitTable.addUnit(this, item);
	}
	
	public IUnit[] getUnits(String type, String category) throws ErrorException {
		if (LOG.isDebugEnabled()) 
			LOG.debug("getUnits: type=" + type + ", category=" + category);
		
		return UnitTable.loadUnits(this, type, category);
	}
	
	public void saveUnits() throws ErrorException {
		if (LOG.isDebugEnabled()) LOG.debug("saveUnits");
		UnitTable.saveUnits(this);
	}
	
	public synchronized void close() { 
		if (LOG.isDebugEnabled()) LOG.debug("close");
		mClosed = true;
	}
	
}
