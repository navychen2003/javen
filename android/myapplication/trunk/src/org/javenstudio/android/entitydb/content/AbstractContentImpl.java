package org.javenstudio.android.entitydb.content;

import org.javenstudio.cocoka.database.AbstractContent;

abstract class AbstractContentImpl extends AbstractContent {

	static final Object sContentLock = new Object();
	
	protected AbstractContentImpl(boolean updateable) { 
		super(updateable); 
	}
	
}
