package org.javenstudio.common.entitydb.nosql;

import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.db.AbstractEntity;

public abstract class SimpleEntity<K extends IIdentity> extends AbstractEntity<K> {

	public SimpleEntity() {
		super(); 
	}
	
	public SimpleEntity(K id) {
		super(id); 
	}
	
}
