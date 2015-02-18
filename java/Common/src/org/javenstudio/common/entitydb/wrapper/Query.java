package org.javenstudio.common.entitydb.wrapper;

import java.util.Comparator;

import org.javenstudio.common.entitydb.ICursor;
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

public class Query<K extends IIdentity, T extends IEntity<K>> {

	private final AbstractQuery<K,T> mQuery; 
	
	public Query(AbstractQuery<K,T> query) { 
		mQuery = query;
	}
	
	public final ICursor<K,T> queryCursor() { 
		return mQuery.query();
	}
	
	public final int queryCount() { 
		return mQuery.queryCount();
	}
	
	public final void setComparator(Comparator<T> comparator) { 
		mQuery.setComparator(comparator);
	}
	
	public final void whereAnd(IWhereClause<K,T> clause) {
		mQuery.whereAnd(clause);
	}
	
	public final void whereOr(IWhereClause<K,T> clause) {
		mQuery.whereOr(clause);
	}
	
	@SuppressWarnings("unchecked")
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
