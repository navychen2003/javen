package org.javenstudio.falcon.search.schema;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContextLoader;
import org.javenstudio.panda.util.ResourceLoader;

/**
 * Configuration for currency. Provides currency exchange rates.
 */
public class FileExchangeRateProvider implements ExchangeRateProvider {
	//private static final Logger LOG = Logger.getLogger(FileExchangeRateProvider.class);
  
	public static final String PARAM_CURRENCY_CONFIG = "currencyConfig";

	private ExchangeRates mRates = new ExchangeRates();
	private String mCurrencyConfigFile;
	private ContextLoader mLoader;

	/**
	 * Returns the currently known exchange rate between two currencies. If a direct rate has been loaded,
	 * it is used. Otherwise, if a rate is known to convert the target currency to the source, the inverse
	 * exchange rate is computed.
	 *
	 * @param sourceCurrencyCode The source currency being converted from.
	 * @param targetCurrencyCode The target currency being converted to.
	 * @return The exchange rate.
	 * @throws ErrorException if the requested currency pair cannot be found
	 */
	@Override
	public double getExchangeRate(String sourceCurrencyCode, String targetCurrencyCode) throws ErrorException {
		if (sourceCurrencyCode == null || targetCurrencyCode == null) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Cannot get exchange rate; currency was null.");
		}
    
		if (sourceCurrencyCode.equals(targetCurrencyCode)) 
			return 1.0f;

		Double directRate = mRates.lookupRate(sourceCurrencyCode, targetCurrencyCode);
		if (directRate != null) 
			return directRate;

		Double symmetricRate = mRates.lookupRate(targetCurrencyCode, sourceCurrencyCode);
		if (symmetricRate != null) 
			return 1.0f / symmetricRate;

		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
				"No available conversion rate between " + sourceCurrencyCode + " to " + targetCurrencyCode);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) 
			return false;

		FileExchangeRateProvider that = (FileExchangeRateProvider) o;

		return !(mRates != null ? !mRates.equals(that.mRates) : that.mRates != null);
	}

	@Override
	public int hashCode() {
		return mRates != null ? mRates.hashCode() : 0;
	}

	@Override
	public String toString() {
		return getClass().getName() + "{" + mRates.size() + " rates}";
	}

	@Override
	public Set<String> listAvailableCurrencies() {
		Set<String> currencies = new HashSet<String>();
		
		for (String from : mRates.getFromNames()) {
			currencies.add(from);
			for (String to : mRates.getToNames(from)) {
				currencies.add(to);
			}
		}
		
		return currencies;
	}

	@Override
	public boolean reload() throws ErrorException {
		// Atomically swap in the new rates map, if it loaded successfully
		mRates = IndexSchemaHelper.readCurrencyConfig(mLoader, mCurrencyConfigFile);
		
		return true;
	}

	@Override
	public void init(Map<String,String> params) throws ErrorException {
		mCurrencyConfigFile = params.get(PARAM_CURRENCY_CONFIG);
		if (mCurrencyConfigFile == null) {
			throw new ErrorException(ErrorException.ErrorCode.NOT_FOUND, 
					"Missing required configuration "+PARAM_CURRENCY_CONFIG);
		}
    
		// Removing config params custom to us
		params.remove(PARAM_CURRENCY_CONFIG);
	}

	@Override
	public void inform(ResourceLoader loader) throws ErrorException {
		if (loader == null) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Needs ResourceLoader in order to load config file");
		}
		
		mLoader = (ContextLoader)loader;
		reload();
	}
	
}
