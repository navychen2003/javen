package org.javenstudio.falcon.search.schema;

import java.util.Map;
import java.util.HashMap;

public abstract class FieldProperties {

	// use a bitfield instead of many different boolean variables since
	// many of the variables are independent or semi-independent.

	// bit values for boolean field properties.
	public static final int INDEXED             = 0x00000001;
	public static final int TOKENIZED           = 0x00000002;
	public static final int STORED              = 0x00000004;
	public static final int BINARY              = 0x00000008;
	public static final int OMIT_NORMS          = 0x00000010;
	public static final int OMIT_TF_POSITIONS   = 0x00000020;
	public static final int STORE_TERMVECTORS   = 0x00000040;
	public static final int STORE_TERMPOSITIONS = 0x00000080;
	public static final int STORE_TERMOFFSETS   = 0x00000100;

	public static final int MULTIVALUED         = 0x00000200;
	public static final int SORT_MISSING_FIRST  = 0x00000400;
	public static final int SORT_MISSING_LAST   = 0x00000800;
  
	public static final int REQUIRED            = 0x00001000;
	public static final int OMIT_POSITIONS      = 0x00002000;

	public static final String[] sPropertyNames = {
			"indexed", "tokenized", "stored",
			"binary", "omitNorms", "omitTermFreqAndPositions",
			"termVectors", "termPositions", "termOffsets",
			"multiValued", "sortMissingFirst", "sortMissingLast", 
			"required", "omitPositions"
		};

	private static final Map<String,Integer> sPropertyMap = 
			new HashMap<String,Integer>();
	
	static {
		for (String prop : sPropertyNames) {
			sPropertyMap.put(prop, propertyNameToInt(prop));
		}
	}

	/** Returns the symbolic name for the property. */
	public static String getPropertyName(int property) {
		return sPropertyNames[Integer.numberOfTrailingZeros(property)];
	}

	public static int propertyNameToInt(String name) {
		for (int i=0; i < sPropertyNames.length; i++) {
			if (sPropertyNames[i].equals(name)) 
				return 1 << i;
		}
		return 0;
	}

	public static String propertiesToString(int properties) {
		StringBuilder sb = new StringBuilder();
		
		boolean first = true;
		while (properties != 0) {
			if (!first) sb.append(',');
			first = false;
			
			int bitpos = Integer.numberOfTrailingZeros(properties);
			sb.append(getPropertyName(1 << bitpos));
			
			properties &= ~(1<<bitpos);  // clear that bit position
		}
		
		return sb.toString();
	}

	public static boolean propertyOn(int bitfield, int props) {
		return (bitfield & props) != 0;
	}

	public static boolean propertyOff(int bitfield, int props) {
		return (bitfield & props) == 0;
	}

	public static int parseProperties(Map<String,String> properties, boolean which) {
		int props = 0;
		
		for (Map.Entry<String, String> entry : properties.entrySet()) {
			String val = entry.getValue();
			if (val == null) continue;
			
			if (Boolean.parseBoolean(val) == which) 
				props |= propertyNameToInt(entry.getKey());
		}
		
		return props;
	}

}
