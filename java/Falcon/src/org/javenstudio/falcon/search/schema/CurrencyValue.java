package org.javenstudio.falcon.search.schema;

import java.util.Currency;

import org.javenstudio.falcon.ErrorException;

/**
 * Represents a Currency field value, which includes a long amount and ISO currency code.
 */
public class CurrencyValue {
	
	private String mCurrencyCode;
	private long mAmount;

	/**
	 * Constructs a new currency value.
	 *
	 * @param amount       The amount.
	 * @param currencyCode The currency code.
	 */
	public CurrencyValue(long amount, String currencyCode) {
		mAmount = amount;
		mCurrencyCode = currencyCode;
	}

	/**
	 * Constructs a new currency value by parsing the specific input.
	 * <p/>
	 * Currency values are expected to be in the format &lt;amount&gt;,&lt;currency code&gt;,
	 * for example, "500,USD" would represent 5 U.S. Dollars.
	 * <p/>
	 * If no currency code is specified, the default is assumed.
	 *
	 * @param externalVal The value to parse.
	 * @param defaultCurrency The default currency.
	 * @return The parsed CurrencyValue.
	 */
	public static CurrencyValue parse(String externalVal, String defaultCurrency) 
			throws ErrorException {
		if (externalVal == null) 
			return null;
		
		String amount = externalVal;
		String code = defaultCurrency;

		if (externalVal.contains(",")) {
			String[] amountAndCode = externalVal.split(",");
			amount = amountAndCode[0];
			code = amountAndCode[1];
		}

		if (amount.equals("*")) 
			return null;
    
		Currency currency = java.util.Currency.getInstance(code);
		if (currency == null) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Invalid currency code " + code);
		}

		try {
			double value = Double.parseDouble(amount);
			long currencyValue = Math.round(value * Math.pow(10.0, currency.getDefaultFractionDigits()));

			return new CurrencyValue(currencyValue, code);
		} catch (NumberFormatException e) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, e);
		}
	}

	/**
	 * The amount of the CurrencyValue.
	 *
	 * @return The amount.
	 */
	public long getAmount() {
		return mAmount;
	}

	/**
	 * The ISO currency code of the CurrencyValue.
	 *
	 * @return The currency code.
	 */
	public String getCurrencyCode() {
		return mCurrencyCode;
	}

	/**
	 * Performs a currency conversion & unit conversion.
	 *
	 * @param exchangeRates      Exchange rates to apply.
	 * @param sourceCurrencyCode The source currency code.
	 * @param sourceAmount       The source amount.
	 * @param targetCurrencyCode The target currency code.
	 * @return The converted indexable units after the exchange rate 
	 * and currency fraction digits are applied.
	 */
	public static long convertAmount(ExchangeRateProvider exchangeRates, 
			String sourceCurrencyCode, long sourceAmount, String targetCurrencyCode) throws ErrorException {
		double exchangeRate = exchangeRates.getExchangeRate(sourceCurrencyCode, targetCurrencyCode);
		return convertAmount(exchangeRate, sourceCurrencyCode, sourceAmount, targetCurrencyCode);
	}

	/**
	 * Performs a currency conversion & unit conversion.
	 *
	 * @param exchangeRate         Exchange rate to apply.
	 * @param sourceFractionDigits The fraction digits of the source.
	 * @param sourceAmount         The source amount.
	 * @param targetFractionDigits The fraction digits of the target.
	 * @return The converted indexable units after the exchange rate and currency fraction digits are applied.
	 */
	public static long convertAmount(final double exchangeRate, final int sourceFractionDigits, 
			final long sourceAmount, final int targetFractionDigits) {
		int digitDelta = targetFractionDigits - sourceFractionDigits;
		double value = ((double) sourceAmount * exchangeRate);

		if (digitDelta != 0) {
			if (digitDelta < 0) {
				for (int i = 0; i < -digitDelta; i++) {
					value *= 0.1;
				}
			} else {
				for (int i = 0; i < digitDelta; i++) {
					value *= 10.0;
				}
			}
		}

		return (long) value;
	}

	/**
	 * Performs a currency conversion & unit conversion.
	 *
	 * @param exchangeRate       Exchange rate to apply.
	 * @param sourceCurrencyCode The source currency code.
	 * @param sourceAmount       The source amount.
	 * @param targetCurrencyCode The target currency code.
	 * @return The converted indexable units after the exchange rate and currency fraction digits are applied.
	 */
	public static long convertAmount(double exchangeRate, String sourceCurrencyCode, 
			long sourceAmount, String targetCurrencyCode) {
		if (targetCurrencyCode.equals(sourceCurrencyCode)) 
			return sourceAmount;

		int sourceFractionDigits = Currency.getInstance(sourceCurrencyCode).getDefaultFractionDigits();
		Currency targetCurrency = Currency.getInstance(targetCurrencyCode);
		int targetFractionDigits = targetCurrency.getDefaultFractionDigits();
		
		return convertAmount(exchangeRate, sourceFractionDigits, sourceAmount, targetFractionDigits);
	}

	/**
	 * Returns a new CurrencyValue that is the conversion of this CurrencyValue to the specified currency.
	 *
	 * @param exchangeRates      The exchange rate provider.
   	 * @param targetCurrencyCode The target currency code to convert this CurrencyValue to.
   	 * @return The converted CurrencyValue.
   	 */
	public CurrencyValue convertTo(ExchangeRateProvider exchangeRates, 
			String targetCurrencyCode) throws ErrorException {
		return new CurrencyValue(
				convertAmount(exchangeRates, getCurrencyCode(), getAmount(), targetCurrencyCode), 
				targetCurrencyCode);
	}

	@Override
	public String toString() {
		return "CurrencyValue{" + String.valueOf(mAmount) + "," + mCurrencyCode + "}";
	}
	
}
