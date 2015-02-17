package org.javenstudio.common.entitydb.nosql;

import org.javenstudio.common.entitydb.IDatabase;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.IWhereClause;
import org.javenstudio.common.entitydb.clause.BiggerClause;
import org.javenstudio.common.entitydb.clause.BiggerEqualsClause;
import org.javenstudio.common.entitydb.clause.BooleanClause;
import org.javenstudio.common.entitydb.clause.EqualsClause;
import org.javenstudio.common.entitydb.clause.IsEmptyClause;
import org.javenstudio.common.entitydb.clause.LeftLikeClause;
import org.javenstudio.common.entitydb.clause.LikeClause;
import org.javenstudio.common.entitydb.clause.NotEqualsClause;
import org.javenstudio.common.entitydb.clause.SmallerClause;
import org.javenstudio.common.entitydb.clause.SmallerEqualsClause;
import org.javenstudio.common.entitydb.db.AbstractQuery;

public class SimpleQuery<K extends IIdentity, T extends IEntity<K>> extends AbstractQuery<K,T> {

	public SimpleQuery(IDatabase db, Class<T> entityClass) {
		super(db.getTable(entityClass));
	}
	
	protected BooleanClause<K,T> newBooleanClause(boolean and, IWhereClause<K,T>... clauses) { 
		return new BooleanClause<K,T>(and, clauses);
	}
	
	protected EqualsClause<K,T> newEqualsClause(String fieldName, Object value) { 
		return new EqualsClause<K,T>(fieldName, value);
	}
	
	protected NotEqualsClause<K,T> newNotEqualsClause(String fieldName, Object value) { 
		return new NotEqualsClause<K,T>(fieldName, value);
	}
	
	protected BiggerClause<K,T> newBiggerClause(String fieldName, Object value) { 
		return new BiggerClause<K,T>(fieldName, value);
	}
	
	protected BiggerEqualsClause<K,T> newBiggerEqualsClause(String fieldName, Object value) { 
		return new BiggerEqualsClause<K,T>(fieldName, value);
	}
	
	protected SmallerClause<K,T> newSmallerClause(String fieldName, Object value) { 
		return new SmallerClause<K,T>(fieldName, value);
	}
	
	protected SmallerEqualsClause<K,T> newSmallerEqualsClause(String fieldName, Object value) { 
		return new SmallerEqualsClause<K,T>(fieldName, value);
	}
	
	protected LikeClause<K,T> newLikeClause(String fieldName, Object value) { 
		return new LikeClause<K,T>(fieldName, value);
	}
	
	protected LeftLikeClause<K,T> newLeftLikeClause(String fieldName, Object value) { 
		return new LeftLikeClause<K,T>(fieldName, value);
	}
	
	protected IsEmptyClause<K,T> newIsEmptyClause(String fieldName) { 
		return new IsEmptyClause<K,T>(fieldName);
	}
	
}
