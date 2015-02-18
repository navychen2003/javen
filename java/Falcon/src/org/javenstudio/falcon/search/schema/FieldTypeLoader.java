package org.javenstudio.falcon.search.schema;

import java.util.Collection;
import java.util.Map;

import org.javenstudio.common.indexdb.analysis.Analyzer;
import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContextLoader;
import org.javenstudio.falcon.util.ContextNode;
import org.javenstudio.falcon.util.PluginLoader;
import org.javenstudio.falcon.search.analysis.TokenizerChain;
import org.javenstudio.panda.analysis.CharFilterFactory;
import org.javenstudio.panda.analysis.KeywordAnalyzer;
import org.javenstudio.panda.analysis.TokenFilterFactory;

public final class FieldTypeLoader extends PluginLoader<SchemaFieldType> {
  	private final static Logger LOG = Logger.getLogger(FieldTypeLoader.class);

  	private final IndexSchema mSchema;
  	private final Map<String, SchemaFieldType> mFieldTypes;
  	private final Collection<SchemaAware> mSchemaAware;
  	
  	/**
  	 * @param schema The schema that will be used to initialize the FieldTypes
  	 * @param fieldTypes All FieldTypes that are instantiated by 
  	 *        this Plugin Loader will be added to this Map
  	 * @param schemaAware Any SchemaAware objects that are instantiated by 
  	 *        this Plugin Loader will be added to this collection.
  	 */
  	public FieldTypeLoader(final IndexSchema schema,
  			final Map<String, SchemaFieldType> fieldTypes,
  			final Collection<SchemaAware> schemaAware) {
  		super("[schema.xml] fieldType", SchemaFieldType.class, true, true);
  		mSchema = schema;
  		mFieldTypes = fieldTypes;
  		mSchemaAware = schemaAware;
  	}

  	final IndexSchema getSchema() { return mSchema; }
  	
  	final Collection<SchemaAware> getSchemaAwares() { 
  		return mSchemaAware;
  	}
  	
  	@Override
  	protected SchemaFieldType create(ContextLoader loader, 
  			String name, String className, ContextNode node) throws ErrorException {
  		return IndexSchemaHelper.createFieldType(this, loader, name, className, node);
  	}
  
  	@Override
  	protected void init(SchemaFieldType plugin, ContextNode node) throws ErrorException {
  		Map<String,String> params = node.getAttributes("name", "class");
  		plugin.setArgs(mSchema, params);
  	}
  
  	@Override
  	protected SchemaFieldType register(String name, 
  			SchemaFieldType plugin) throws ErrorException {
  		if (LOG.isDebugEnabled())
  			LOG.debug("fieldtype defined: " + plugin);
  		
  		return mFieldTypes.put( name, plugin );
  	}

  	// The point here is that, if no multiterm analyzer was specified in the schema file, 
  	// do one of several things:
  	// 1> If legacyMultiTerm == false, assemble a new analyzer composed of all of the charfilters,
  	//    lowercase filters and asciifoldingfilter.
  	// 2> If legacyMultiTerm == true just construct the analyzer from a KeywordTokenizer. 
  	//    That should mimic current behavior.
  	//    Do the same if they've specified that the old behavior is required (legacyMultiTerm="true")

  	static Analyzer constructMultiTermAnalyzer(Analyzer queryAnalyzer) throws ErrorException {
  		if (queryAnalyzer == null) 
  			return null;

  		if (!(queryAnalyzer instanceof TokenizerChain)) 
  			return new KeywordAnalyzer();

  		TokenizerChain tc = (TokenizerChain) queryAnalyzer;
  		MultiTermChainBuilder builder = new MultiTermChainBuilder();

  		CharFilterFactory[] charFactories = tc.getCharFilterFactories();
  		if (charFactories != null) {
  			for (CharFilterFactory fact : charFactories) {
  				builder.add(fact);
  			}
  		}

  		builder.add(tc.getTokenizerFactory());

  		for (TokenFilterFactory fact : tc.getTokenFilterFactories()) {
  			builder.add(fact);
  		}

  		return builder.build();
  	}
  	
}
