package org.javenstudio.falcon.search.schema;

import org.javenstudio.falcon.ErrorException;

public class DynamicDestCopy extends DynamicCopy {
	
	private final DynamicField mDynamicField;
	private final int mDynamicType;
	private final String mDynamicText;
  
	public DynamicDestCopy(String source, DynamicField dynamic) 
			throws ErrorException {
		this(source, dynamic, CopyField.UNLIMITED);
	}
    
	public DynamicDestCopy(String source, DynamicField dynamic, int maxChars) 
			throws ErrorException {
		super(source, dynamic.getPrototype(), maxChars);
		mDynamicField = dynamic;
    
		String dest = dynamic.getRegex();
		if (dest.startsWith("*")) {
			mDynamicType = ENDS_WITH;
			mDynamicText = dest.substring(1);
			
		} else if (dest.endsWith("*")) {
			mDynamicType = STARTS_WITH;
			mDynamicText = dest.substring(0, dest.length()-1);
			
		} else {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR,
					"dynamic copyField destination name must start or end with *");
		}
	}
  
	@Override
	public SchemaField getTargetField(String sourceField) throws ErrorException {
		String dyn = (mDynamicType == STARTS_WITH) 
				? sourceField.substring(mDynamicText.length())
				: sourceField.substring(0, sourceField.length()-mDynamicText.length());
    
		String name = (mDynamicType == STARTS_WITH) ? (mDynamicText + dyn) : (dyn + mDynamicText);
		return mDynamicField.makeSchemaField(name);
	}

	@Override
	public String toString() {
		return getTargetField().toString();
	}
	
}
