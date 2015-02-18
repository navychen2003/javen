package org.javenstudio.falcon.user.global;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IDatabase;

public abstract class Unit implements IUnit {

	private final String mKey;
	private final String mName;
	
	private String mCategory = null;
	private String mStatus = null;
	private String mOwner = null;
	
	public Unit(String key, String name) { 
		if (key == null || name == null) throw new NullPointerException();
		mKey = key;
		mName= name;
	}
	
	public String getKey() { return mKey; }
	public String getName() { return mName; }
	
	public String getCategory() { return mCategory; }
	public void setCategory(String val) { mCategory = val; }
	
	public String getStatus() { return mStatus; }
	public void setStatus(String val) { mStatus = val; }
	
	public String getOwner() { return mOwner; }
	public void setOwner(String val) { mOwner = val; }
	
	void putFields(IDatabase.Row row) throws ErrorException { 
		if (row == null) return;
		
		UnitTable.addAttr(row, UnitTable.KEY_QUALIFIER, getKey());
		UnitTable.addAttr(row, UnitTable.NAME_QUALIFIER, getName());
		UnitTable.addAttr(row, UnitTable.TYPE_QUALIFIER, getType());
		UnitTable.addAttr(row, UnitTable.STATUS_QUALIFIER, getStatus());
		UnitTable.addAttr(row, UnitTable.OWNER_QUALIFIER, getOwner());
		UnitTable.addAttr(row, UnitTable.CATEGORY_QUALIFIER, getCategory());
	}
	
	void getFields(IDatabase.Result res) throws ErrorException { 
		if (res == null) return;
		
		setStatus(UnitTable.getAttrString(res, UnitTable.STATUS_QUALIFIER));
		setOwner(UnitTable.getAttrString(res, UnitTable.OWNER_QUALIFIER));
		setCategory(UnitTable.getAttrString(res, UnitTable.CATEGORY_QUALIFIER));
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{key=" 
				+ mKey + ",name=" + mName + "}";
	}
	
}
