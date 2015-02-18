package org.javenstudio.falcon.search.schema;

import java.util.Date;

import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.hornet.query.source.LongFieldSource;
import org.javenstudio.hornet.util.MutableValueDate;
import org.javenstudio.hornet.util.MutableValueLong;

public class TrieDateFieldSource extends LongFieldSource {
	
	public TrieDateFieldSource(String field) {
		this(field, null);
	}
	
	public TrieDateFieldSource(String field, ISortField.LongParser parser) {
		super(field, parser);
	}

	@Override
	public String getDescription() {
	    return "date(" + mField + ')';
	}

	@Override
	protected MutableValueLong newMutableValueLong() {
		return new MutableValueDate();
	}

	@Override
	public Object longToObject(long val) {
		return new Date(val);
	}

	@Override
	public long externalToLong(String extVal) {
		try {
			return TrieFieldType.sDateField.parseMath(null, extVal).getTime();
		} catch (ErrorException ex) { 
			throw new RuntimeException(ex);
		}
	}
	
}
