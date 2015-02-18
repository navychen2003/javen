package org.javenstudio.falcon.search.schema;

import java.util.Map;
import java.util.Set;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.panda.util.ResourceLoader;

/**
 * Interface for providing pluggable exchange rate providers to @CurrencyField
 */
public interface ExchangeRateProvider {
	
	/**
	 * Get the exchange rate betwen the two given currencies
	 * @return the exchange rate as a double
	 * @throws ErrorException if the rate is not defined in the provider
	 */
	public double getExchangeRate(String sourceCurrencyCode, String targetCurrencyCode) 
			throws ErrorException;
  
	/**
	 * List all configured currency codes which are valid as source/target for this Provider
	 * @return a Set of <a href="http://en.wikipedia.org/wiki/ISO_4217">ISO 4217</a> currency code strings
	 */
	public Set<String> listAvailableCurrencies();

	/**
	 * Ask the currency provider to explicitly reload/refresh its configuration.
	 * If this does not make sense for a particular provider, simply do nothing
	 * @throws ErrorException if there is a problem reloading
	 * @return true if reload of rates succeeded, else false
	 */
	public boolean reload() throws ErrorException;

	/**
	 * Initializes the provider by passing in a set of key/value configs as a map.
	 * Note that the map also contains other fieldType parameters, so make sure to
	 * avoid name clashes.
	 * <p>
	 * Important: Custom config params must be removed from the map before returning
	 * @param args a @Map of key/value config params to initialize the provider
	 */
	public void init(Map<String,String> args) throws ErrorException;

	/**
	 * Passes a ResourceLoader, used to read config files from e.g. ZooKeeper.
	 * Implementations not needing resource loader can implement this as NOOP.
	 * <p>Typically called after init
	 * @param loader a @ResourceLoader instance
	 */
	public void inform(ResourceLoader loader) throws ErrorException;
	
}
