package org.javenstudio.falcon.search.schema;

import java.util.Currency;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.hornet.query.FunctionValues;

public class CurrencyFunctionValues extends FunctionValues {

    public static final int MAX_CURRENCIES_TO_CACHE = 256;
    
    private final int[] mFractionDigitCache = new int[MAX_CURRENCIES_TO_CACHE];
    private final String[] mCurrencyOrdToCurrencyCache = new String[MAX_CURRENCIES_TO_CACHE];
    private final double[] mExchangeRateCache = new double[MAX_CURRENCIES_TO_CACHE];
    
    private final CurrencyValueSource mSource;
    private final FunctionValues mAmounts;
    private final FunctionValues mCurrencies;
    
    private int mTargetFractionDigits = -1;
    private int mTargetCurrencyOrd = -1;
    private boolean mInitializedCache;

    public CurrencyFunctionValues(CurrencyValueSource source, 
    		FunctionValues amounts, FunctionValues currencies) { 
    	mSource = source;
    	mAmounts = amounts;
    	mCurrencies = currencies;
    }
    
	private String getDocCurrencyCode(int doc, int currencyOrd) {
    	if (currencyOrd < MAX_CURRENCIES_TO_CACHE) {
    		String currency = mCurrencyOrdToCurrencyCache[currencyOrd];

    		if (currency == null) {
    			mCurrencyOrdToCurrencyCache[currencyOrd] = 
    					currency = mCurrencies.stringVal(doc);
    		}
        
    		if (currency == null) 
    			currency = mSource.getFieldType().getDefaultCurrency();

    		if (mTargetCurrencyOrd == -1 && currency.equals(mSource.getTargetCurrencyCode())) 
    			mTargetCurrencyOrd = currencyOrd;

    		return currency;
    	}
    	
    	return mCurrencies.stringVal(doc);
    }
	
	private long getLongVal(int doc) throws ErrorException {
    	if (!mInitializedCache) {
    		for (int i = 0; i < mFractionDigitCache.length; i++) {
    			mFractionDigitCache[i] = -1;
    		}

    		mInitializedCache = true;
    	}

    	long amount = mAmounts.longVal(doc);
    	int currencyOrd = mCurrencies.ordVal(doc);

    	if (currencyOrd == mTargetCurrencyOrd) 
    		return amount;

    	double exchangeRate;
    	int sourceFractionDigits;

    	if (mTargetFractionDigits == -1) {
    		mTargetFractionDigits = Currency.getInstance(
    				mSource.getTargetCurrencyCode()).getDefaultFractionDigits();
    	}

    	if (currencyOrd < MAX_CURRENCIES_TO_CACHE) {
    		exchangeRate = mExchangeRateCache[currencyOrd];

    		if (exchangeRate <= 0.0) {
    			String sourceCurrencyCode = getDocCurrencyCode(doc, currencyOrd);
    			exchangeRate = mExchangeRateCache[currencyOrd] = 
    					mSource.getFieldType().getProvider().getExchangeRate(
    							sourceCurrencyCode, mSource.getTargetCurrencyCode());
    		}

    		sourceFractionDigits = mFractionDigitCache[currencyOrd];

    		if (sourceFractionDigits == -1) {
    			String sourceCurrencyCode = getDocCurrencyCode(doc, currencyOrd);
    			sourceFractionDigits = mFractionDigitCache[currencyOrd] = 
    					Currency.getInstance(sourceCurrencyCode).getDefaultFractionDigits();
    		}
    		
    	} else {
    		String sourceCurrencyCode = getDocCurrencyCode(doc, currencyOrd);
    		exchangeRate = mSource.getFieldType().getProvider().getExchangeRate(
    				sourceCurrencyCode, mSource.getTargetCurrencyCode());
    		sourceFractionDigits = Currency.getInstance(sourceCurrencyCode).getDefaultFractionDigits();
    	}

    	return CurrencyValue.convertAmount(exchangeRate, sourceFractionDigits, 
    			amount, mTargetFractionDigits);
	}

	private long getLongValNoThrow(int doc) { 
		try { 
			return getLongVal(doc);
		} catch (ErrorException ex) { 
			throw new RuntimeException(ex);
		}
	}
	
	@Override
    public long longVal(int doc) {
		return getLongValNoThrow(doc);
	}
	
	@Override
    public int intVal(int doc) {
    	return (int) longVal(doc);
    }

	@Override
    public double doubleVal(int doc) {
    	return (double) longVal(doc);
    }

	@Override
    public float floatVal(int doc) {
    	return (float) longVal(doc);
    }

	@Override
	public String stringVal(int doc) {
    	return Long.toString(longVal(doc));
    }

	@Override
    public String toString(int doc) {
    	return mSource.getName() + '(' + mAmounts.toString(doc) + ',' + mCurrencies.toString(doc) + ')';
    }
	
}
