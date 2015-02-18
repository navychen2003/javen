package org.javenstudio.falcon.search.stats;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.falcon.search.schema.SchemaFieldType;
import org.javenstudio.falcon.search.schema.TrieFieldType;
import org.javenstudio.falcon.search.schema.type.ByteFieldType;
import org.javenstudio.falcon.search.schema.type.DateFieldType;
import org.javenstudio.falcon.search.schema.type.DoubleFieldType;
import org.javenstudio.falcon.search.schema.type.FloatFieldType;
import org.javenstudio.falcon.search.schema.type.IntFieldType;
import org.javenstudio.falcon.search.schema.type.LongFieldType;
import org.javenstudio.falcon.search.schema.type.ShortFieldType;
import org.javenstudio.falcon.search.schema.type.StringFieldType;

/**
 * Factory class for creating instance of {@link StatsValues}
 */
public class StatsValuesFactory {

	/**
	 * Creates an instance of StatsValues which supports values 
	 * from a field of the given FieldType
	 *
	 * @param sf SchemaField for the field whose statistics 
	 * will be created by the resulting StatsValues
	 * @return Instance of StatsValues that will create statistics 
	 * from values from a field of the given type
	 */
	public static StatsValues createStatsValues(SchemaField sf) throws ErrorException {
		SchemaFieldType fieldType = sf.getType();
		if (DoubleFieldType.class.isInstance(fieldType) ||
			IntFieldType.class.isInstance(fieldType) ||
			LongFieldType.class.isInstance(fieldType) ||
			ShortFieldType.class.isInstance(fieldType) ||
			FloatFieldType.class.isInstance(fieldType) ||
			ByteFieldType.class.isInstance(fieldType) ||
			TrieFieldType.class.isInstance(fieldType)) {
			
			return new NumericStatsValues(sf);
			
		} else if (DateFieldType.class.isInstance(fieldType)) {
			return new DateStatsValues(sf);
			
		} else if (StringFieldType.class.isInstance(fieldType)) {
			return new StringStatsValues(sf);
			
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Field type " + fieldType + " is not currently supported");
		}
	}
	
}
