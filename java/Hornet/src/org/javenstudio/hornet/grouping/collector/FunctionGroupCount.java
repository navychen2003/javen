package org.javenstudio.hornet.grouping.collector;

import org.javenstudio.hornet.grouping.GroupCount;
import org.javenstudio.hornet.util.MutableValue;

/** 
 * Holds distinct values for a single group.
 */
public class FunctionGroupCount extends GroupCount<MutableValue> {

	public FunctionGroupCount(MutableValue groupValue) {
		super(groupValue);
	}
    
}
