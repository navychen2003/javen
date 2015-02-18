package org.javenstudio.common.entitydb.clause;

import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;

public class NotEqualsClause<K extends IIdentity, T extends IEntity<K>> extends WhereClause<K,T> {

	public NotEqualsClause(String fieldName, Object matchValue) {
		super(fieldName, matchValue, WhereClause.MATCH_NOTEQUALS); 
	}
	
}
