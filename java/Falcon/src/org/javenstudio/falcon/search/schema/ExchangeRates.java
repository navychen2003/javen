package org.javenstudio.falcon.search.schema;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ExchangeRates {

	// Exchange rate map, maps Currency Code -> Currency Code -> Rate
	private Map<String, Map<String, Double>> mRates = 
			new HashMap<String, Map<String, Double>>();
	
	public ExchangeRates() {}
	
	public final int size() { return mRates.size(); }
	
	public final Set<String> getFromNames() { 
		return mRates.keySet();
	}
	
	public final Set<String> getToNames(String from) { 
		Map<String, Double> toMap = mRates.get(from);
		if (toMap != null) 
			return toMap.keySet();
		
		return null;
	}
	
	/**
	 * Looks up the current known rate, if any, between the source and target currencies.
	 *
	 * @param sourceCurrencyCode The source currency being converted from.
	 * @param targetCurrencyCode The target currency being converted to.
	 * @return The exchange rate, or null if no rate has been registered.
	 */
	public Double lookupRate(String sourceCurrencyCode, String targetCurrencyCode) {
		Map<String, Double> rhs = mRates.get(sourceCurrencyCode);

		if (rhs != null) 
			return rhs.get(targetCurrencyCode);

		return null;
	}
	
	/**
	 * Registers the specified exchange rate.
	 *
	 * @param ratesMap           The map to add rate to
	 * @param sourceCurrencyCode The source currency.
	 * @param targetCurrencyCode The target currency.
	 * @param rate               The known exchange rate.
	 */
	public void addRate(String sourceCurrencyCode, String targetCurrencyCode, double rate) {
		Map<String, Double> rhs = mRates.get(sourceCurrencyCode);

		if (rhs == null) {
			rhs = new HashMap<String, Double>();
			mRates.put(sourceCurrencyCode, rhs);
		}

		rhs.put(targetCurrencyCode, rate);
	}
	
}
