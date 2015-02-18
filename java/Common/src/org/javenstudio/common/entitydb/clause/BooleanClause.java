package org.javenstudio.common.entitydb.clause;

import java.util.ArrayList;
import java.util.List;

import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.ITable;
import org.javenstudio.common.entitydb.IWhereClause;

public final class BooleanClause<K extends IIdentity, T extends IEntity<K>> implements IWhereClause<K,T> {

	private final boolean mAnd; 
	private final List<IWhereClause<K,T> > mClauses; 
	
	@SuppressWarnings("unchecked")
	public BooleanClause(boolean and, IWhereClause<K,T>... clauses) {
		mAnd = and; 
		mClauses = new ArrayList<IWhereClause<K,T> >(); 
		
		addClause(clauses); 
	}
	
	public boolean isAnd() { return mAnd; } 
	
	@SuppressWarnings("unchecked")
	public void addClause(IWhereClause<K,T>... clauses) {
		for (int i=0; clauses != null && i < clauses.length; i++) {
			IWhereClause<K,T> clause = clauses[i]; 
			if (clause != null) 
				mClauses.add(clause); 
		}
	}
	
	@Override
	public void bindField(ITable<K,T> table) {
		for (int i=0; i < mClauses.size(); i++) {
			mClauses.get(i).bindField(table); 
		}
	}
	
	@Override
	public boolean match(T data) {
		for (int i=0; i < mClauses.size(); i++) {
			if (mClauses.get(i).match(data)) {
				if (!mAnd) return true; 
			} else {
				if (mAnd) return false; 
			}
		}
		
		return mAnd ? true : false; 
	}
	
	@Override
	public String toSQL() {
		StringBuilder sbuf = new StringBuilder(); 
		sbuf.append("( "); 
		for (int i=0; i < mClauses.size(); i++) {
			if (i > 0) sbuf.append(mAnd?" AND ":" OR "); 
			sbuf.append(mClauses.get(i).toSQL()); 
		}
		sbuf.append(" )"); 
		return sbuf.toString(); 
	}
	
}
