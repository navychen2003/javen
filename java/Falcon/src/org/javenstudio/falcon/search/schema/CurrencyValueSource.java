package org.javenstudio.falcon.search.schema;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.falcon.search.query.QueryBuilder;

public class CurrencyValueSource extends ValueSource {

	private final CurrencyFieldType mFieldType;
	private final SchemaField mField;
    private ValueSource mCurrencyValues;
    private ValueSource mAmountValues;
    private String mTargetCurrencyCode;
	
	public CurrencyValueSource(CurrencyFieldType fieldType, SchemaField field, 
			String targetCurrencyCode, QueryBuilder parser) throws ErrorException { 
		mFieldType = fieldType;
		mField = field;
		mTargetCurrencyCode = targetCurrencyCode;
		
		SchemaField amountField = fieldType.getSchema().getField(field.getName() 
				+ CurrencyFieldType.POLY_FIELD_SEPARATOR + CurrencyFieldType.FIELD_SUFFIX_AMOUNT_RAW);
		SchemaField currencyField = fieldType.getSchema().getField(field.getName() 
				+ CurrencyFieldType.POLY_FIELD_SEPARATOR + CurrencyFieldType.FIELD_SUFFIX_CURRENCY);
		
		mCurrencyValues = currencyField.getType().getValueSource(currencyField, parser);
		mAmountValues = amountField.getType().getValueSource(amountField, parser);
	}
	
	public final CurrencyFieldType getFieldType() { 
		return mFieldType;
	}
	
	public final String getTargetCurrencyCode() { 
		return mTargetCurrencyCode;
	}
	
	public String getName() { return "currency"; }
	
	@Override
    public FunctionValues getValues(ValueSourceContext context, IAtomicReaderRef reader) throws IOException {
        final FunctionValues amounts = mAmountValues.getValues(context, reader);
        final FunctionValues currencies = mCurrencyValues.getValues(context, reader);
        
        return new CurrencyFunctionValues(this, amounts, currencies);
    }
	
    @Override
    public String getDescription() {
    	return getName() + "(" + mField.getName() + ")";
    }

    @Override
    public boolean equals(Object o) {
    	if (this == o) return true;
    	if (o == null || getClass() != o.getClass()) 
    		return false;

    	CurrencyValueSource that = (CurrencyValueSource) o;

    	return  equals(mAmountValues, that.mAmountValues) &&
    			equals(mCurrencyValues, that.mCurrencyValues) &&
    			equals(mTargetCurrencyCode, that.mTargetCurrencyCode);
    }

    private boolean equals(Object obj1, Object obj2) { 
    	return !(obj1 != null ? !obj1.equals(obj2) : obj2 != null);
    }
    
    @Override
    public int hashCode() {
    	int result = mTargetCurrencyCode != null ? mTargetCurrencyCode.hashCode() : 0;
    	result = 31 * result + (mCurrencyValues != null ? mCurrencyValues.hashCode() : 0);
    	result = 31 * result + (mAmountValues != null ? mAmountValues.hashCode() : 0);
    	return result;
    }
	
}
