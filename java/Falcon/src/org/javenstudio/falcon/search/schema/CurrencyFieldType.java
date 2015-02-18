package org.javenstudio.falcon.search.schema;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.indexdb.document.FieldType;
import org.javenstudio.common.indexdb.document.Fieldable;
import org.javenstudio.common.indexdb.search.Query;
import org.javenstudio.common.indexdb.search.SortField;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.TextWriter;
import org.javenstudio.hornet.search.query.ConstantScoreQuery;
import org.javenstudio.falcon.search.filter.ValueSourceRangeFilter;
import org.javenstudio.falcon.search.query.QueryBuilder;
import org.javenstudio.falcon.search.schema.type.StringFieldType;
import org.javenstudio.falcon.search.schema.type.TrieLongFieldType;
import org.javenstudio.panda.util.ResourceLoader;
import org.javenstudio.panda.util.ResourceLoaderAware;

/**
 * Field type for support of monetary values.
 * 
 */
public class CurrencyFieldType extends SchemaFieldType 
		implements SchemaAware, ResourceLoaderAware {
	//static final Logger LOG = Logger.getLogger(CurrencyFieldType.class);
	
	public static final String PARAM_DEFAULT_CURRENCY      = "defaultCurrency";
	public static final String PARAM_RATE_PROVIDER_CLASS   = "providerClass";
	public static final Object PARAM_PRECISION_STEP        = "precisionStep";
	public static final String DEFAULT_RATE_PROVIDER_CLASS = "lightning.FileExchangeRateProvider";
	public static final String DEFAULT_DEFAULT_CURRENCY    = "USD";
	public static final String DEFAULT_PRECISION_STEP      = "0";
	public static final String FIELD_SUFFIX_AMOUNT_RAW     = "_amount_raw";
	public static final String FIELD_SUFFIX_CURRENCY       = "_currency";

	private IndexSchema mSchema;
	private ExchangeRateProvider mProvider;
	private SchemaFieldType mFieldTypeCurrency;
	private SchemaFieldType mFieldTypeAmountRaw;
	private String mExchangeRateProviderClass;
	private String mDefaultCurrency;

	public final String getDefaultCurrency() { return mDefaultCurrency; }
	public final ExchangeRateProvider getProvider() { return mProvider; }
	public final IndexSchema getSchema() { return mSchema; }
	
	@Override
	public boolean isPolyField() {
		return true;
	}
	
	@Override
	public void init(IndexSchema schema, Map<String, String> args) throws ErrorException {
		super.init(schema, args);
		
		if (isMultiValued()) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"CurrencyField types can not be multiValued: " + getTypeName());
		}
		
		mSchema = schema;
		mExchangeRateProviderClass = args.get(PARAM_RATE_PROVIDER_CLASS);
		mDefaultCurrency = args.get(PARAM_DEFAULT_CURRENCY);

		if (mDefaultCurrency == null) 
			mDefaultCurrency = DEFAULT_DEFAULT_CURRENCY;
    
		if (mExchangeRateProviderClass == null) 
			mExchangeRateProviderClass = DEFAULT_RATE_PROVIDER_CLASS;

		if (java.util.Currency.getInstance(mDefaultCurrency) == null) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Invalid currency code " + mDefaultCurrency);
		}

		String precisionStepString = args.get(PARAM_PRECISION_STEP);
		if (precisionStepString == null) 
			precisionStepString = DEFAULT_PRECISION_STEP;

		// Initialize field type for amount
		mFieldTypeAmountRaw = new TrieLongFieldType();
		mFieldTypeAmountRaw.setTypeName("amount_raw_type_tlong");
		
		Map<String,String> map = new HashMap<String,String>(1);
		map.put("precisionStep", precisionStepString);
		mFieldTypeAmountRaw.init(schema, map);
    
		// Initialize field type for currency string
		mFieldTypeCurrency = new StringFieldType();
		mFieldTypeCurrency.setTypeName("currency_type_string");
		mFieldTypeCurrency.init(schema, new HashMap<String,String>());
    
		args.remove(PARAM_RATE_PROVIDER_CLASS);
		args.remove(PARAM_DEFAULT_CURRENCY);
		args.remove(PARAM_PRECISION_STEP);

		try {
			Class<? extends ExchangeRateProvider> c = schema.getContextLoader().findClass(
					mExchangeRateProviderClass, ExchangeRateProvider.class);
			if (c == null) 
				c = FileExchangeRateProvider.class; 
			
			mProvider = c.newInstance();
			mProvider.init(args);
		} catch (Exception e) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Error instansiating exhange rate provider " + mExchangeRateProviderClass + 
					". Please check your FieldType configuration", e);
		}
	}

	@Override
	public void checkSchemaField(final SchemaField field) throws ErrorException {
		super.checkSchemaField(field);
		if (field.isMultiValued()) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"CurrencyFields can not be multiValued: " + field.getName());
		}
	}

	@Override
	public Fieldable[] createFields(SchemaField field, Object externalVal, 
			float boost) throws ErrorException {
		CurrencyValue value = CurrencyValue.parse(externalVal.toString(), mDefaultCurrency);
		Fieldable[] f = new Fieldable[field.isStored() ? 3 : 2];
		
		SchemaField amountField = getAmountField(field);
		f[0] = amountField.createField(String.valueOf(value.getAmount()), 
				amountField.isIndexed() && !amountField.isOmitNorms() ? boost : 1F);
		
		SchemaField currencyField = getCurrencyField(field);
		f[1] = currencyField.createField(value.getCurrencyCode(), 
				currencyField.isIndexed() && !currencyField.isOmitNorms() ? boost : 1F);

		if (field.isStored()) {
			FieldType customType = new FieldType();
			assert !customType.isOmitNorms();
			
			customType.setStored(true);
			String storedValue = externalVal.toString().trim();
			
			if (storedValue.indexOf(",") < 0) 
				storedValue += "," + mDefaultCurrency;
			
			f[2] = createField(field.getName(), storedValue, customType, 1F);
		}

		return f;
	}

	protected SchemaField getAmountField(SchemaField field) throws ErrorException {
		return mSchema.getField(field.getName() + POLY_FIELD_SEPARATOR + FIELD_SUFFIX_AMOUNT_RAW);
	}

	protected SchemaField getCurrencyField(SchemaField field) throws ErrorException {
		return mSchema.getField(field.getName() + POLY_FIELD_SEPARATOR + FIELD_SUFFIX_CURRENCY);
	}

	protected void createDynamicCurrencyField(String suffix, SchemaFieldType type) 
			throws ErrorException {
		String name = "*" + POLY_FIELD_SEPARATOR + suffix;
		
		Map<String, String> props = new HashMap<String, String>();
		props.put("indexed", "true");
		props.put("stored", "false");
		props.put("multiValued", "false");
		props.put("omitNorms", "true");
		
		int p = SchemaField.calcProps(name, type, props);
		mSchema.registerDynamicField(SchemaField.create(name, type, p, null));
	}

	/**
	 * When index schema is informed, add dynamic fields.
	 *
	 * @param indexSchema The index schema.
	 */
	@Override
	public void inform(IndexSchema indexSchema) throws ErrorException {
		createDynamicCurrencyField(FIELD_SUFFIX_CURRENCY,   mFieldTypeCurrency);
		createDynamicCurrencyField(FIELD_SUFFIX_AMOUNT_RAW, mFieldTypeAmountRaw);
	}

	/**
	 * Load the currency config when resource loader initialized.
	 *
	 * @param resourceLoader The resource loader.
	 */
	@Override
	public void inform(ResourceLoader resourceLoader) throws IOException {
		try {
			mProvider.inform(resourceLoader);
			boolean reloaded = mProvider.reload();
			if (!reloaded) 
				LOG.warn("Failed reloading currencies");
			
		} catch (ErrorException ex) { 
			throw new IOException(ex);
		}
	}

	@Override
	public Query getFieldQuery(QueryBuilder parser, SchemaField field, String externalVal) 
			throws ErrorException {
		CurrencyValue value = CurrencyValue.parse(externalVal, mDefaultCurrency);
		CurrencyValue valueDefault = value.convertTo(mProvider, mDefaultCurrency);

		return getRangeQuery(parser, field, valueDefault, valueDefault, true, true);
	}

	@Override
	public Query getRangeQuery(QueryBuilder parser, SchemaField field, String part1, String part2, 
			final boolean minInclusive, final boolean maxInclusive) throws ErrorException {
		final CurrencyValue p1 = CurrencyValue.parse(part1, mDefaultCurrency);
		final CurrencyValue p2 = CurrencyValue.parse(part2, mDefaultCurrency);

		if (p1 != null && p2 != null && !p1.getCurrencyCode().equals(p2.getCurrencyCode())) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
					"Cannot parse range query " + part1 + " to " + part2 +
					": range queries only supported when upper and lower bound have same currency.");
		}

		return getRangeQuery(parser, field, p1, p2, minInclusive, maxInclusive);
	}

	public Query getRangeQuery(QueryBuilder parser, SchemaField field, final CurrencyValue p1, 
			final CurrencyValue p2, final boolean minInclusive, final boolean maxInclusive) 
			throws ErrorException {
		String currencyCode = (p1 != null) ? p1.getCurrencyCode() :
		                      (p2 != null) ? p2.getCurrencyCode() : mDefaultCurrency;
		                      
		final CurrencyValueSource vs = new CurrencyValueSource(this, 
				field, currencyCode, parser);

		return new ConstantScoreQuery(new ValueSourceRangeFilter(vs,
				(p1 == null ? null : p1.getAmount() + ""), 
				(p2 == null ? null : p2.getAmount() + ""), 
				minInclusive, maxInclusive));
	}

	@Override
	public SortField getSortField(SchemaField field, boolean reverse) throws ErrorException {
		try {
			// Convert all values to default currency for sorting.
			return (new CurrencyValueSource(this, field, mDefaultCurrency, null)).getSortField(reverse);
		} catch (IOException e) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, e);
		}
	}

	@Override
	public void write(TextWriter writer, String name, Fieldable field) throws ErrorException {
		try {
			writer.writeString(name, field.getStringValue(), false);
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
	}

}
