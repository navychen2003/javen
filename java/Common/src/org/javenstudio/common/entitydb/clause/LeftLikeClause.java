package org.javenstudio.common.entitydb.clause;

import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;

public class LeftLikeClause<K extends IIdentity, T extends IEntity<K>> extends WhereClause<K,T> {

	public LeftLikeClause(String fieldName, Object matchValue) {
		super(fieldName, matchValue, WhereClause.MATCH_LEFTLIKE); 
	}
	
}
