package org.javenstudio.falcon.search.schema.type;

import org.javenstudio.falcon.search.schema.TrieFieldType;
import org.javenstudio.falcon.search.schema.TrieTypes;

/**
 * A numeric field that can contain 64-bit signed two's complement integer values.
 *
 * <ul>
 *  <li>Min Value Allowed: -9223372036854775808</li>
 *  <li>Max Value Allowed: 9223372036854775807</li>
 * </ul>
 * 
 * @see Long
 */
public class TrieLongFieldType extends TrieFieldType {
	
	{
		mType = TrieTypes.LONG;
	}
	
}
