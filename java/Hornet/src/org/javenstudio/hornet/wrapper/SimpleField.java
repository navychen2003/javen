package org.javenstudio.hornet.wrapper;

import org.javenstudio.common.indexdb.IField;

public class SimpleField {

	private final IField mField;
	
	SimpleField(IField field) { 
		mField = field;
	}
	
	public String getStringValue() { 
		return mField.getStringValue();
	}
	
}
