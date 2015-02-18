package org.javenstudio.falcon.search.schema;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.indexdb.analysis.Analyzer;
import org.javenstudio.common.indexdb.search.Similarity;
import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContextList;
import org.javenstudio.falcon.util.ContextLoader;
import org.javenstudio.falcon.util.ContextNode;
import org.javenstudio.falcon.util.ContextResource;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.util.PluginLoader;
import org.javenstudio.falcon.search.analysis.TokenizerChain;
import org.javenstudio.falcon.search.schema.type.TextFieldType;
import org.javenstudio.falcon.search.similarity.DefaultSimilarityFactory;
import org.javenstudio.falcon.search.similarity.SimilarityFactory;
import org.javenstudio.panda.analysis.CharFilterFactory;
import org.javenstudio.panda.analysis.TokenFilterFactory;
import org.javenstudio.panda.analysis.TokenizerFactory;
import org.javenstudio.panda.util.ResourceLoader;
import org.javenstudio.panda.util.ResourceLoaderAware;

public class IndexSchemaHelper {
	static final Logger LOG = Logger.getLogger(IndexSchemaHelper.class);

	static void readSchema(IndexSchema schema, InputStream is) throws ErrorException {
		if (LOG.isInfoEnabled())
			LOG.info("Reading Index Schema: " + schema.mResourceName);

		// pass the config resource loader to avoid building an empty one for no reason:
		// in the current case though, the stream is valid so we wont load the resource by name
		final ContextResource schemaConf = schema.getContextLoader().openResource(
				schema.getResourceName(), is, "/schema/");
		
		final List<SchemaAware> schemaAware = new ArrayList<SchemaAware>();
  
		ContextNode nd = schemaConf.getNode("/schema/@name");
		if (nd == null) {
			if (LOG.isWarnEnabled())
				LOG.warn("schema has no name!");
			
		} else {
			schema.mName = nd.getNodeValue();
			
			if (LOG.isInfoEnabled())
				LOG.info("Schema name=" + schema.mName);
		}

		schema.mVersion = schemaConf.getFloat("/schema/@version", 1.0f);

		// load the Field Types
		final FieldTypeLoader typeLoader = new FieldTypeLoader(schema, 
				schema.mFieldTypes, schemaAware);

		String expression = "/schema/types/fieldtype | /schema/types/fieldType";
		ContextList nodes = schemaConf.getNodes(expression);
		typeLoader.load(schema.mLoader, nodes);

		// load the Fields
		// Hang on to the fields that say if they are required -- this lets us set 
		// a reasonable default for the unique key
		Map<String,Boolean> explicitRequiredProp = new HashMap<String, Boolean>();
		ArrayList<DynamicField> dFields = new ArrayList<DynamicField>();
		
		expression = "/schema/fields/field | /schema/fields/dynamicField";
		nodes = schemaConf.getNodes(expression);

		for (int i=0; i < nodes.getLength(); i++) {
			ContextNode node = nodes.getNodeAt(i);

			String name = node.getAttribute("name", "field definition");
			if (LOG.isDebugEnabled())
				LOG.debug("reading field def " + name);
			
			String type = node.getAttribute("type", "field " + name);
			SchemaFieldType ft = schema.mFieldTypes.get(type);
			if (ft == null) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Unknown fieldtype '" + type + "' specified on field " + name);
			}

			Map<String,String> args = node.getAttributes("name", "type");
			if (args.get("required") != null ) 
				explicitRequiredProp.put(name, Boolean.valueOf(args.get("required")));

			SchemaField f = SchemaField.create(name, ft, args);

			if (node.getNodeName().equals("field")) {
				SchemaField old = schema.mFields.put(f.getName(), f);
				if (old != null) {
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
							"[schema.xml] Duplicate field definition for '" + f.getName() + 
							"' [[[" + old.toString() + "]]] and [[[" + f.toString() + "]]]");
				}
				
				if (LOG.isDebugEnabled())
					LOG.debug("field defined: " + f);
				
				if (f.getDefaultValue() != null) {
					if (LOG.isDebugEnabled())
						LOG.debug(name + " contains default value: " + f.getDefaultValue());
					
					schema.mFieldsWithDefaultValue.add(f);
				}
				
				if (f.isRequired()) {
					if (LOG.isDebugEnabled())
						LOG.debug(name + " is required in this schema");
					
					schema.mRequiredFields.add(f);
				}
				
			} else if (node.getNodeName().equals("dynamicField")) {
				// make sure nothing else has the same path
				schema.addDynamicField(dFields, f);
				
			} else {
				// we should never get here
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"Unknown field type");
			}
		}
  
		//fields with default values are by definition required
		//add them to required fields, and we only have to loop once
		// in DocumentBuilder.getDoc()
		schema.mRequiredFields.addAll(schema.getFieldsWithDefaultValue());

		// OK, now sort the dynamic fields largest to smallest size so we don't get
		// any false matches.  We want to act like a compiler tool and try and match
		// the largest string possible.
		Collections.sort(dFields);

		if (LOG.isDebugEnabled())
			LOG.debug("Dynamic Field Ordering: " + dFields);

		// stuff it in a normal array for faster access
		schema.mDynamicFields = dFields.toArray(new DynamicField[dFields.size()]);

		ContextNode node = schemaConf.getNode("/schema/similarity");
		SimilarityFactory simFactory = readSimilarity(schema.mLoader, node);
		if (simFactory == null) 
			simFactory = new DefaultSimilarityFactory();
		
		if (simFactory instanceof SchemaAware) {
			((SchemaAware)simFactory).inform(schema);
			
		} else {
			// if the sim facotry isn't schema aware, then we are responsible for
			// erroring if a field type is trying to specify a sim.
			for (SchemaFieldType ft : schema.mFieldTypes.values()) {
				if (ft.getSimilarity() != null) {
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
							"FieldType '" + ft.getTypeName() + "' is configured with a similarity, " + 
							"but the global similarity does not support it: " + simFactory.getClass());
				}
			}
		}
		
		schema.mSimilarity = simFactory.getSimilarity();

		node = schemaConf.getNode("/schema/defaultSearchField/text()");
		if (node == null) {
			if (LOG.isWarnEnabled())
				LOG.warn("no default search field specified in schema.");
			
		} else {
			schema.mDefaultSearchFieldName = node.getNodeValue().trim();
			
			// throw exception if specified, but not found or not indexed
			if (schema.mDefaultSearchFieldName != null) {
				SchemaField defaultSearchField = schema.getFields().get(schema.mDefaultSearchFieldName);
				if ((defaultSearchField == null) || !defaultSearchField.isIndexed()) {
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
							"default search field '" + schema.mDefaultSearchFieldName + "' not defined or not indexed");
				}
			}
			
			if (LOG.isInfoEnabled())
				LOG.info("default search field in schema is " + schema.mDefaultSearchFieldName);
		}

		node = schemaConf.getNode("/schema/queryParser/@defaultOperator");
		if (node == null) {
			if (LOG.isInfoEnabled())
				LOG.info("using default query parser operator (OR)");
			
		} else {
			schema.mQueryParserDefaultOperator = node.getNodeValue().trim();
			
			if (LOG.isInfoEnabled())
				LOG.info("query parser default operator is " + schema.mQueryParserDefaultOperator);
		}

		node = schemaConf.getNode("/schema/uniqueKey/text()");
		if (node == null) {
			if (LOG.isWarnEnabled())
				LOG.warn("no uniqueKey specified in schema.");
			
		} else {
			schema.mUniqueKeyField = schema.getIndexedField(node.getNodeValue().trim());
			if (schema.mUniqueKeyField.getDefaultValue() != null) {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"uniqueKey field (" + schema.mUniqueKeyFieldName + ") can not be configured with " + 
						"a default value (" + schema.mUniqueKeyField.getDefaultValue() + ")");
			}

			if (!schema.mUniqueKeyField.isStored()) {
				if (LOG.isErrorEnabled())
					LOG.error("uniqueKey is not stored - distributed search will not work");
			}
			
			if (schema.mUniqueKeyField.isMultiValued()) {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"uniqueKey field (" + schema.mUniqueKeyFieldName + ") can not be configured to be multivalued");
			}
			
			schema.mUniqueKeyFieldName = schema.mUniqueKeyField.getName();
			schema.mUniqueKeyFieldType = schema.mUniqueKeyField.getType();
			
			if (LOG.isInfoEnabled())
				LOG.info("unique key field: " + schema.mUniqueKeyFieldName);
  
			// Unless the uniqueKeyField is marked 'required=false' then make sure it exists
			if (Boolean.FALSE != explicitRequiredProp.get(schema.mUniqueKeyFieldName)) {
				schema.mUniqueKeyField.setRequired(true);
				schema.mRequiredFields.add(schema.mUniqueKeyField);
			}
		}

		/////////////// parse out copyField commands ///////////////
		// Map<String,ArrayList<SchemaField>> cfields = new HashMap<String,ArrayList<SchemaField>>();
		// expression = "/schema/copyField";

		schema.mDynamicCopyFields = new DynamicCopy[] {};
		expression = "//copyField";
		nodes = schemaConf.getNodes(expression);

		for (int i=0; i < nodes.getLength(); i++) {
			node = nodes.getNodeAt(i);

			String source = node.getAttribute("source", "copyField definition");
			String dest   = node.getAttribute("dest",   "copyField definition");
			
			String maxChars = node.getAttribute("maxChars");
			int maxCharsInt = CopyField.UNLIMITED;
			
			if (maxChars != null) {
				try {
					maxCharsInt = Integer.parseInt(maxChars);
				} catch (NumberFormatException e) {
					if (LOG.isWarnEnabled()) {
						LOG.warn("Couldn't parse maxChars attribute for copyField from " + source 
								+ " to " + dest + " as integer. The whole field will be copied.");
					}
				}
			}

			if (dest.equals(schema.mUniqueKeyFieldName)) {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"uniqueKey field (" + schema.mUniqueKeyFieldName + ") can not be the dest of " + 
						"a copyField (src=" + source + ")");
			}

			schema.registerCopyField(source, dest, maxCharsInt);
		}
  
		for (Map.Entry<SchemaField, Integer> entry : schema.mCopyFieldTargetCounts.entrySet()) {
			if (entry.getValue() > 1 && !entry.getKey().isMultiValued())  {
				if (LOG.isWarnEnabled()) {
					LOG.warn("Field " + entry.getKey().getName() + " is not multivalued " +
							"and destination for multiple copyFields (" + entry.getValue() + ")");
				}
			}
		}

		//Run the callbacks on SchemaAware now that everything else is done
		for (SchemaAware aware : schemaAware) {
			aware.inform(schema);
		}
		
		// create the field analyzers
		schema.refreshAnalyzers();
	}
	
	static SimilarityFactory readSimilarity(ContextLoader loader, ContextNode node) 
			throws ErrorException {
		if (node == null) 
			return null;
		
		final SimilarityFactory similarityFactory;
		final Object obj;
		try {
			obj = loader.newInstance(node.getAttribute("class"), Object.class);
		} catch (ClassNotFoundException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
		
		if (obj instanceof SimilarityFactory) {
			// configure a factory, get a similarity back
			Params params = Params.toParams(node.getChildNodesAsNamedList());
			similarityFactory = (SimilarityFactory)obj;
			similarityFactory.init(params);
			
		} else {
			// just like always, assume it's a Similarity 
			// and get a ClassCastException - reasonable error handling
			similarityFactory = new SimilarityFactory() {
					@Override
					public Similarity getSimilarity() {
						return (Similarity) obj;
					}
				};
		}
		
		return similarityFactory;
	}
	
	static SchemaFieldType createFieldType(FieldTypeLoader typeLoader, 
			ContextLoader loader, String name, String className, ContextNode node) 
			throws ErrorException { 
		SchemaFieldType ft = null; 
		try {
			ft = loader.newInstance(className, SchemaFieldType.class);
			ft.setTypeName(name);
		} catch (ClassNotFoundException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}

		ContextNode anode = node.getChildNode("./analyzer[@type='query']");
		Analyzer queryAnalyzer = readAnalyzer(typeLoader.getSchema(), anode);

		anode = node.getChildNode("./analyzer[@type='multiterm']");
		Analyzer multiAnalyzer = readAnalyzer(typeLoader.getSchema(), anode);

		// An analyzer without a type specified, or with type="index"
		anode = node.getChildNode("./analyzer[not(@type)] | ./analyzer[@type='index']");
		Analyzer analyzer = readAnalyzer(typeLoader.getSchema(), anode);

		// a custom similarity[Factory]
		anode = node.getChildNode("./similarity");
		SimilarityFactory simFactory = IndexSchemaHelper.readSimilarity(loader, anode);

		if (queryAnalyzer == null) 
			queryAnalyzer = analyzer;
		if (analyzer == null) 
			analyzer = queryAnalyzer;
		
		if (multiAnalyzer == null) 
			multiAnalyzer = FieldTypeLoader.constructMultiTermAnalyzer(queryAnalyzer);
		
		if (analyzer != null) {
			ft.setAnalyzer(analyzer);
			ft.setQueryAnalyzer(queryAnalyzer);
			if (ft instanceof TextFieldType)
				((TextFieldType)ft).setMultiTermAnalyzer(multiAnalyzer);
		}
		
		if (simFactory != null) 
			ft.setSimilarity(simFactory.getSimilarity());
		
		if (ft instanceof SchemaAware) 
			typeLoader.getSchemaAwares().add((SchemaAware) ft);
		
		return ft;
	}
	
  	// <analyzer><tokenizer class="...."/><tokenizer class="...." arg="....">
  	static Analyzer readAnalyzer(IndexSchema schema, ContextNode node) throws ErrorException {
  		final ContextLoader loader = schema.getContextLoader();
  		if (node == null) 
  			return null;
  		
  		String analyzerName = node.getAttribute("class");

  		// check for all of these up front, so we can error if used in 
  		// conjunction with an explicit analyzer class.
  		ContextList charFilterNodes = node.getChildNodes("./charFilter");
  		ContextList tokenizerNodes = node.getChildNodes("./tokenizer");
  		ContextList tokenFilterNodes = node.getChildNodes("./filter");
      
  		if (analyzerName != null) {
  			// explicitly check for child analysis factories instead of
  			// just any child nodes, because the user might have their
  			// own custom nodes (ie: <description> or something like that)
  			if (0 != charFilterNodes.getLength() ||
  				0 != tokenizerNodes.getLength() ||
  				0 != tokenFilterNodes.getLength()) {
  				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR,
  						"Configuration Error: Analyzer class='" + analyzerName +
  						"' can not be combined with nested analysis factories");
  			}

  			try {
  				// No need to be core-aware as Analyzers are not in the core-aware list
  				final Class<? extends Analyzer> clazz = 
  						loader.findClass(analyzerName, Analyzer.class);
        
  				try {
  					// first try to use a ctor with version parameter 
  					// (needed for many new Analyzers that have no default one anymore)
  					Constructor<? extends Analyzer> cnstr = clazz.getConstructor();
  					
  					return cnstr.newInstance();
  				} catch (NoSuchMethodException nsme) {
  					// otherwise use default ctor
  					return clazz.newInstance();
  				}
  			} catch (Exception e) {
  				//LOG.error("Cannot load analyzer: "+analyzerName, e);
  				throw new ErrorException( ErrorException.ErrorCode.SERVER_ERROR,
  						"Cannot load analyzer: " + analyzerName, e);
  			}
  		}

  		// Load the CharFilters
  		final ArrayList<CharFilterFactory> charFilters = new ArrayList<CharFilterFactory>();
  		
  		PluginLoader<CharFilterFactory> charFilterLoader = new PluginLoader<CharFilterFactory>
  			("[schema.xml] analyzer/charFilter", CharFilterFactory.class, false, false) {

  				@Override
  				protected void init(CharFilterFactory plugin, ContextNode node) throws ErrorException {
  					if (plugin != null) {
  						final Map<String,String> params = node.getAttributes("class");

  						plugin.init(params);
  						charFilters.add(plugin);
  					}
  				}

  				@Override
  				protected CharFilterFactory register(String name, CharFilterFactory plugin) {
  					return null; // used for map registration
  				}
  			};

  		charFilterLoader.load(loader, charFilterNodes);

  		// Load the Tokenizer
  		// Although an analyzer only allows a single Tokenizer, we load a list to make sure
  		// the configuration is ok
  		final ArrayList<TokenizerFactory> tokenizers = new ArrayList<TokenizerFactory>(1);
  		
  		PluginLoader<TokenizerFactory> tokenizerLoader = new PluginLoader<TokenizerFactory>
  			("[schema.xml] analyzer/tokenizer", TokenizerFactory.class, false, false) {
  			
  				@Override
  				protected void init(TokenizerFactory plugin, ContextNode node) throws ErrorException {
  					if (!tokenizers.isEmpty()) {
  						throw new ErrorException( ErrorException.ErrorCode.SERVER_ERROR,
  								"The schema defines multiple tokenizers for: "+node );
  					}
  					
  					final Map<String,String> params = node.getAttributes("class");

  					plugin.init(params);
  					tokenizers.add(plugin);
  				}

  				@Override
  				protected TokenizerFactory register(String name, TokenizerFactory plugin) {
  					return null; // used for map registration
  				}
  			};

  		tokenizerLoader.load(loader, tokenizerNodes);
    
  		// Make sure something was loaded
  		if (tokenizers.isEmpty()) {
  			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
  					"analyzer without class or tokenizer");
  		}

  		// Load the Filters
  		final ArrayList<TokenFilterFactory> filters = new ArrayList<TokenFilterFactory>();

  		PluginLoader<TokenFilterFactory> filterLoader = new PluginLoader<TokenFilterFactory>( 
  			"[schema.xml] analyzer/filter", TokenFilterFactory.class, false, false) {
  			
  				@Override
  				protected void init(TokenFilterFactory plugin, ContextNode node) throws ErrorException {
  					if (plugin != null) {
  						final Map<String,String> params = node.getAttributes("class");

  						plugin.init(params);
  						filters.add(plugin);
  					}
  				}

  				@Override
  				protected TokenFilterFactory register(String name, TokenFilterFactory plugin) {
  					return null; // used for map registration
  				}
  			};
  			
  		filterLoader.load(loader, tokenFilterNodes);
    
  		informPluginAware(loader, tokenizers, charFilters, filters);
  		
  		return new TokenizerChain(tokenizers.get(0), 
  				filters.toArray(new TokenFilterFactory[filters.size()]), 
  				charFilters.toArray(new CharFilterFactory[charFilters.size()]));
  	}
	
  	static void informPluginAware(ContextLoader loader, Collection<?>... list) throws ErrorException { 
  		if (list == null || loader == null) return;
  		
  		for (Collection<?> instances : list) { 
	  		for (Object instance : instances) { 
	  			if (instance == null) continue;
	  			
	  			try { 
					if (instance instanceof ResourceLoaderAware && 
						loader instanceof ResourceLoader) {
						if (LOG.isDebugEnabled()) {
							LOG.debug("inform ResourceLoaderAware instance, class=" 
									+ instance.getClass().getName());
						}
						
						((ResourceLoaderAware)instance).inform((ResourceLoader)loader);
					}
				} catch (Throwable ex) { 
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
							"Error inform instance of class: '" + instance.getClass().getName() + "'", ex);
				}
	  		}
  		}
  	}
  	
  	static ExchangeRates readCurrencyConfig(ContextLoader loader, String currencyConfig) 
  			throws ErrorException { 
  		ExchangeRates rates = new ExchangeRates();
  		InputStream is = null;
  		
  		try {
			LOG.info("Reloading exchange rates from file " + currencyConfig);
			is = loader.openResourceAsStream(currencyConfig);
			
			ContextResource conf = loader.openResource(currencyConfig, is, null);
		
			// Parse exchange rates.
			ContextList nodes = conf.getNodes("/currencyConfig/rates/rate");
	
			for (int i = 0; i < nodes.getLength(); i++) {
				ContextNode rateNode = nodes.getNodeAt(i);
				
				String from = rateNode.getAttribute("from");
				String to = rateNode.getAttribute("to");
				String rate = rateNode.getAttribute("rate");
	  
				if (from == null || to == null || rate == null) {
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"Exchange rate missing attributes (required: from, to, rate) " + rateNode);
				}
	  
				String fromCurrency = from;
				String toCurrency = to;
				Double exchangeRate;
	  
				if (java.util.Currency.getInstance(fromCurrency) == null ||
						java.util.Currency.getInstance(toCurrency) == null) {
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"Could not find from currency specified in exchange rate: " + rateNode);
				}
	  
				try {
					exchangeRate = Double.parseDouble(rate);
				} catch (NumberFormatException e) {
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"Could not parse exchange rate: " + rateNode, e);
				}
	  
				rates.addRate(fromCurrency, toCurrency, exchangeRate);
			}
			
		} catch (IOException e) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Error while opening Currency configuration file " + currencyConfig, e);
			
		} finally {
			try {
				if (is != null) 
					is.close();
			} catch (IOException e) {
				// ignore
			}
		}
  		
  		return rates;
  	}
  	
}
