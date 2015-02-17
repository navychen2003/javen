package org.javenstudio.common.entitydb.nosql;

import org.javenstudio.common.entitydb.IDatabase;
import org.javenstudio.common.entitydb.db.AbstractUpdater;

public abstract class SimpleUpdater extends AbstractUpdater {

	public SimpleUpdater(IDatabase db) {
		super(db);
	}
	
}
