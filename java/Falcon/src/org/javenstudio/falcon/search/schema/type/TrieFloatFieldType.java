package org.javenstudio.falcon.search.schema.type;

import org.javenstudio.falcon.search.schema.TrieFieldType;
import org.javenstudio.falcon.search.schema.TrieTypes;

/**
 * A numeric field that can contain single-precision 32-bit IEEE 754 
 * floating point values.
 *
 * <ul>
 *  <li>Min Value Allowed: 1.401298464324817E-45</li>
 *  <li>Max Value Allowed: 3.4028234663852886E38</li>
 * </ul>
 *
 * <b>NOTE:</b> The behavior of this class when given values of 
 * {@link Float#NaN}, {@link Float#NEGATIVE_INFINITY}, or 
 * {@link Float#POSITIVE_INFINITY} is undefined.
 * 
 * @see Float
 * @see <a href="http://java.sun.com/docs/books/jls/third_edition/html/typesValues.html#4.2.3">
 * Java Language Specification, s4.2.3</a>
 */
public class TrieFloatFieldType extends TrieFieldType {
	
	{
		mType = TrieTypes.FLOAT;
	}
	
}
