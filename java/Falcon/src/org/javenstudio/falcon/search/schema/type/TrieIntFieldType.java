package org.javenstudio.falcon.search.schema.type;

import org.javenstudio.falcon.search.schema.TrieFieldType;
import org.javenstudio.falcon.search.schema.TrieTypes;

/**
 * A numeric field that can contain 32-bit signed two's complement integer values.
 *
 * <ul>
 *  <li>Min Value Allowed: -2147483648</li>
 *  <li>Max Value Allowed: 2147483647</li>
 * </ul>
 * 
 * @see Integer
 */
public class TrieIntFieldType extends TrieFieldType {
	
	{
		mType = TrieTypes.INTEGER;
	}
	
}
