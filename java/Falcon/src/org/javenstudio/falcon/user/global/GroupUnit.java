package org.javenstudio.falcon.user.global;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IDatabase;

public class GroupUnit extends Unit {

	public GroupUnit(String key, String name) { 
		super(key, name);
	}
	
	@Override
	public final String getType() { 
		return IUnit.TYPE_GROUP;
	}
	
	@Override
	void putFields(IDatabase.Row row) throws ErrorException { 
		super.putFields(row);
	}
	
	@Override
	void getFields(IDatabase.Result res) throws ErrorException { 
		super.getFields(res);
	}
	
}
