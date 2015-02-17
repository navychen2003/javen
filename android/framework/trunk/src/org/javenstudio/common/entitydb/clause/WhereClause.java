package org.javenstudio.common.entitydb.clause;

import java.lang.reflect.Field;

import org.javenstudio.common.entitydb.DBException;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.ITable;
import org.javenstudio.common.entitydb.IWhereClause;

abstract class WhereClause<K extends IIdentity, T extends IEntity<K>> implements IWhereClause<K,T> {

	public static final int MATCH_EQUALS = 1; 
	public static final int MATCH_BIGGER = 2; 
	public static final int MATCH_BIGGEREQUALS = 3; 
	public static final int MATCH_SMALLER = 4; 
	public static final int MATCH_SMALLEREQUALS = 5; 
	public static final int MATCH_NOTEQUALS = 6; 
	public static final int MATCH_LEFTLIKE = 7; 
	public static final int MATCH_LIKE = 8; 
	
	private final String mFieldName; 
	private final Object mMatchValue; 
	private final int mMatchType; 
	private boolean mIsIdentity = false; 
	private Field mField = null; 
	
	public WhereClause(String fieldName, Object matchValue, int matchType) {
		mFieldName = fieldName; 
		mMatchValue = matchValue; 
		mMatchType = matchType; 
		
		if (mFieldName == null || mMatchValue == null) 
			throw new DBException("field-name or match-value cannot be null"); 
		
		//mIsIdentity = mFieldName.equals(Constants.IDENTITY_FIELDNAME); 
	}
	
	public final String getFieldName() { return mFieldName; } 
	public final Object getMatchValue() { return mMatchValue; } 
	public final int getMatchType() { return mMatchType; } 
	
	@Override
	public void bindField(ITable<K,T> table) {
		if (mField != null) 
			throw new DBException("field: "+mFieldName+" already binded"); 
		
		mIsIdentity = mFieldName.equals(table.getIdentityFieldName()); 
		if (mIsIdentity) {
			if (!(mMatchValue instanceof IIdentity)) 
				throw new DBException("field: "+mFieldName+" can only match IIdentity value"); 
			return; 
		}
		
		Field field = table.getEntityField(mFieldName); 
		if (field == null) 
			throw new DBException("field: "+mFieldName+" not found in table: "+table.getTableName()); 
		
		if (field.getType() != mMatchValue.getClass()) 
			throw new DBException("field: "+mFieldName+" 's match-value must be "+field.getType()); 
		
		if (mMatchType == MATCH_LEFTLIKE || mMatchType == MATCH_LIKE) {
			if (!(mMatchValue instanceof String)) 
				throw new DBException("field: "+mFieldName+" 's match-value must be String"); 
		}
		
		mField = field; 
	}
	
	@Override
	public boolean match(T data) {
		return mIsIdentity ? matchIdentity(data) : matchField(data); 
	}
	
	private boolean matchIdentity(T data) {
		IIdentity id = data.getIdentity(); 
		IIdentity match = (IIdentity)mMatchValue; 
		
		try {
			switch (mMatchType) {
			case MATCH_EQUALS: 
				return match.equals(id); 
			case MATCH_NOTEQUALS: 
				return !match.equals(id); 
			case MATCH_BIGGER: 
				return match.compareTo(id) < 0; 
			case MATCH_BIGGEREQUALS: 
				return match.compareTo(id) <= 0; 
			case MATCH_SMALLER: 
				return match.compareTo(id) > 0; 
			case MATCH_SMALLEREQUALS: 
				return match.compareTo(id) >= 0; 
			}
		} catch (Exception e) {
			// ohmygod
		}
		return false; 
	}
	
	private boolean matchField(T data) {
		try {
			switch (mMatchType) {
			case MATCH_EQUALS: 
				return mMatchValue.equals(mField.get(data)); 
			case MATCH_NOTEQUALS: 
				return !mMatchValue.equals(mField.get(data)); 
			case MATCH_BIGGER: 
			case MATCH_BIGGEREQUALS: 
			case MATCH_SMALLER: 
			case MATCH_SMALLEREQUALS: 
				return matchNumber(mField.get(data)); 
			case MATCH_LEFTLIKE: 
				return matchLeftLike(mField.get(data)); 
			case MATCH_LIKE:
				return matchLike(mField.get(data)); 
			}
		} catch (Exception e) {
			// ohmygod
		}
		return false; 
	}
	
	private boolean matchLeftLike(Object value) {
		if (value == null) return false; 
		
		if (!(value instanceof String) || !(mMatchValue instanceof String)) 
			return false; 
		
		String str = (String)value; 
		String matchStr = (String)mMatchValue; 
		
		if (str.startsWith(matchStr)) 
			return true; 
		
		return false; 
	}
	
	private boolean matchLike(Object value) {
		if (value == null) return false; 
		
		if (!(value instanceof String) || !(mMatchValue instanceof String)) 
			return false; 
		
		String str = (String)value; 
		String matchStr = (String)mMatchValue; 
		
		if (str.indexOf(matchStr) >= 0) 
			return true; 
		
		return false; 
	}
	
	@SuppressWarnings({"unchecked"})
	private boolean matchNumber(Object value) {
		if (value == null) return false; 
		
		if (mMatchValue.getClass() != value.getClass() || !(mMatchValue instanceof Comparable<?>)) 
			return false; 

		int res = ((Comparable<Object>)mMatchValue).compareTo((Comparable<Object>)value); 

		switch (mMatchType) {
		case MATCH_BIGGER: 
			return res < 0; 
		case MATCH_BIGGEREQUALS: 
			return res <= 0; 
		case MATCH_SMALLER: 
			return res > 0; 
		case MATCH_SMALLEREQUALS: 
			return res >= 0; 
		}
		
		return false; 
	}
	
	@Override
	public String toSQL() {
		StringBuilder sbuf = new StringBuilder(); 
		sbuf.append("( "); 
		sbuf.append(mFieldName); 
		switch (mMatchType) {
		case MATCH_EQUALS: 
			sbuf.append(" = "); 
			break; 
		case MATCH_NOTEQUALS: 
			sbuf.append(" <> "); 
			break; 
		case MATCH_BIGGER: 
			sbuf.append(" > "); 
			break; 
		case MATCH_BIGGEREQUALS: 
			sbuf.append(" >= "); 
			break; 
		case MATCH_SMALLER: 
			sbuf.append(" < "); 
			break; 
		case MATCH_SMALLEREQUALS: 
			sbuf.append(" <= "); 
			break; 
		case MATCH_LEFTLIKE: 
		case MATCH_LIKE: 
			sbuf.append(" LIKE "); 
			break; 
		default: 
			sbuf.append(" = "); 
			break; 
		}
		if (mMatchValue instanceof String) {
			sbuf.append("\""); 
			if (mMatchType == MATCH_LIKE) 
				sbuf.append('%'); 
			sbuf.append(mMatchValue); 
			if (mMatchType == MATCH_LIKE || mMatchType == MATCH_LEFTLIKE) 
				sbuf.append('%'); 
			sbuf.append("\" )"); 
		} else {
			sbuf.append(mMatchValue); 
			sbuf.append(" )"); 
		}
		return sbuf.toString(); 
	}
}
