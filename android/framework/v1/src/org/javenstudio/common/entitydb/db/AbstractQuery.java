package org.javenstudio.common.entitydb.db;

import java.util.ArrayList;
import java.util.Comparator;

import org.javenstudio.common.entitydb.DBException;
import org.javenstudio.common.entitydb.ICursor;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.IQuery;
import org.javenstudio.common.entitydb.ITable;
import org.javenstudio.common.entitydb.IWhereClause;
import org.javenstudio.common.entitydb.clause.BooleanClause;

public abstract class AbstractQuery<K extends IIdentity, T extends IEntity<K>> implements IQuery<K,T> {

	private final ITable<K,T> mTable; 
	private IWhereClause<K,T> mWhereClause; 
	private Comparator<T> mComparator; 
	private boolean mProcessed; 
	
	public AbstractQuery(ITable<K,T> table) {
		mTable = table; 
		mWhereClause = null; 
		mProcessed = false; 
		
		mComparator = new Comparator<T>() {
				public int compare(T a, T b) {
					if (a == null) 
						return b == null ? 0 : -1; 
					else if (b == null) 
						return 1; 
					return a.getIdentity().compareTo(b.getIdentity()); 
				}
			};
	}
	
	@Override
	public final ICursor<K,T> query() {
		synchronized (this) {
			if (mProcessed) throw new DBException("already requested"); 
			mProcessed = true; 
			return mTable.query(this); 
		}
	}
	
	@Override
	public final int queryCount() {
		synchronized (this) {
			if (mProcessed) throw new DBException("already requested"); 
			mProcessed = true; 
			return mTable.queryCount(this); 
		}
	}
	
	@Override
	public final int deleteMany() {
		synchronized (this) {
			if (mProcessed) throw new DBException("already requested"); 
			mProcessed = true; 
			return mTable.deleteMany(this); 
		}
	}
	
	@Override 
	public final int deleteManyIndividually() {
		synchronized (this) {
			if (mProcessed) throw new DBException("already requested"); 
			
			ArrayList<K> list = new ArrayList<K>(); 
			ICursor<K,T> cursor = query(); 
			while (cursor.hasNext()) 
				list.add(cursor.next().getIdentity()); 
			cursor.close(); 
			
			int count = 0; 
			for (K id : list) {
				if (mTable.delete(id)) count ++; 
			}
			
			mProcessed = true; 
			
			return count; 
		}
	}
	
	@Override
	public synchronized Comparator<T> getComparator() {
		return mComparator;
	}
	
	public final synchronized void setComparator(Comparator<T> comparator) { 
		if (comparator != null && comparator != mComparator) 
			mComparator = comparator;
	}
	
	@Override
	public final boolean match(T data) {
		if (data == null) return false; 
		
		IWhereClause<K,T> clause = getWhereClause(); 
		if (clause != null) 
			return clause.match(data); 
		
		return true; 
	}
	
	@Override
	public IWhereClause<K,T> getWhereClause() {
		return mWhereClause; 
	}
	
	public void clearWhereClause() {
		synchronized (this) {
			mWhereClause = null; 
		}
	}
	
	public void whereAnd(IWhereClause<K,T> clause) {
		where(clause, true); 
	}
	
	public void whereOr(IWhereClause<K,T> clause) {
		where(clause, false); 
	}
	
	@SuppressWarnings("unchecked")
	private void where(IWhereClause<K,T> clause, boolean and) {
		synchronized (this) {
			if (clause == null || mProcessed) return; 
			
			clause.bindField(mTable); 
			
			if (mWhereClause != null) {
				if (mWhereClause instanceof BooleanClause) {
					BooleanClause<K,T> boolclause = ((BooleanClause<K,T>)mWhereClause); 
					if (boolclause.isAnd() == and) {
						boolclause.addClause(clause); 
						return; 
					}
				}
				mWhereClause = new BooleanClause<K,T>(and, mWhereClause, clause); 
				
			} else 
				mWhereClause = clause; 
				
		}
	}
	
	@Override
	public String toSQL() {
		String[] names = mTable.getEntityFieldNames(); 

		StringBuilder sbuf = new StringBuilder(); 
		sbuf.append("SELECT "); 
		sbuf.append(mTable.getIdentityFieldName()); 
		
		for (int i=0; names != null && i < names.length; i++) {
			sbuf.append(", "); 
			sbuf.append(names[i]); 
		}
		
		sbuf.append(" FROM "); 
		sbuf.append(mTable.getTableName()); 
		
		String clause = toWhereSQL(); 
		if (clause != null && clause.length() > 0) {
			sbuf.append(" WHERE "); 
			sbuf.append(clause); 
		}
		
		return sbuf.toString(); 
	}
	
	@Override
	public String toWhereSQL() {
		IWhereClause<K,T> clause = mWhereClause; 
		if (clause != null) {
			return clause.toSQL(); 
		}
		return null; 
	}
	
}
