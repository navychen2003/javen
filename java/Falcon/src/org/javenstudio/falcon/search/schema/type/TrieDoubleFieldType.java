package org.javenstudio.falcon.search.schema.type;

import org.javenstudio.falcon.search.schema.TrieFieldType;
import org.javenstudio.falcon.search.schema.TrieTypes;

/**
 * A numeric field that can contain double-precision 64-bit IEEE 754 floating 
 * point values.
 *
 * <ul>
 *  <li>Min Value Allowed: 4.9E-324</li>
 *  <li>Max Value Allowed: 1.7976931348623157E308</li>
 * </ul>
 *
 * <b>NOTE:</b> The behavior of this class when given values of 
 * {@link Double#NaN}, {@link Double#NEGATIVE_INFINITY}, or 
 * {@link Double#POSITIVE_INFINITY} is undefined.
 * 
 * @see Double
 * @see <a href="http://java.sun.com/docs/books/jls/third_edition/html/typesValues.html#4.2.3">
 * Java Language Specification, s4.2.3</a>
 */
public class TrieDoubleFieldType extends TrieFieldType {
	
	{
		mType = TrieTypes.DOUBLE;
	}
	
}
