package org.javenstudio.common.entitydb.clause;

import java.lang.reflect.Field;

import org.javenstudio.common.entitydb.DBException;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.ITable;
import org.javenstudio.common.entitydb.IWhereClause;

public class IsEmptyClause<K extends IIdentity, T extends IEntity<K>> implements IWhereClause<K,T> {

	private final String mFieldName; 
	private Field mField = null; 
	
	public IsEmptyClause(String fieldName) { 
		mFieldName = fieldName; 
		
		if (mFieldName == null) 
			throw new DBException("field-name cannot be null"); 
		
		//if (mFieldName.equals(Constants.IDENTITY_FIELDNAME)) 
		//	throw new DBException("field-name cannot be identity field");
	}
	
	public final String getFieldName() { return mFieldName; } 
	
	@Override
	public void bindField(ITable<K,T> table) {
		if (mField != null) 
			throw new DBException("field: "+mFieldName+" already binded"); 
		
		Field field = table.getEntityField(mFieldName); 
		if (field == null) 
			throw new DBException("field: "+mFieldName+" not found in table: "+table.getTableName()); 
		
		if (mFieldName.equals(table.getIdentityFieldName())) 
			throw new DBException("field-name cannot be identity field");
		
		mField = field; 
	}
	
	@Override
	public boolean match(T data) {
		try {
			return isObjectEmpty(mField.get(data));
		} catch (Exception e) {
			// ohmygod
		}
		return false; 
	}
	
	protected boolean isObjectEmpty(Object value) { 
		if (value != null) { 
			if (value instanceof String) { 
				String text = (String)value; 
				if (text.length() == 0) 
					return true;
			}
			return false;
		}
		return true;
	}
	
	@Override
	public String toSQL() {
		StringBuilder sbuf = new StringBuilder(); 
		sbuf.append("( "); 
		sbuf.append(mFieldName); 
		sbuf.append(" IS NULL OR "); 
		sbuf.append(mFieldName); 
		sbuf.append(" =''"); 
		sbuf.append(" )"); 
		return sbuf.toString(); 
	}
	
}
