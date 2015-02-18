package org.javenstudio.falcon.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.CommonParams;
import org.javenstudio.falcon.util.FilenameUtils;
import org.javenstudio.falcon.util.MapParams;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.util.ReturnFields;
import org.javenstudio.hornet.query.FunctionQuery;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.source.QueryValueSource;
import org.javenstudio.falcon.search.query.FunctionQueryBuilder;
import org.javenstudio.falcon.search.query.FunctionQueryBuilderPlugin;
import org.javenstudio.falcon.search.query.QueryBuilder;
import org.javenstudio.falcon.search.query.QueryParsing;
import org.javenstudio.falcon.search.query.StringParser;
import org.javenstudio.falcon.search.transformer.DocTransformer;
import org.javenstudio.falcon.search.transformer.DocTransformers;
import org.javenstudio.falcon.search.transformer.RenameFieldTransformer;
import org.javenstudio.falcon.search.transformer.ScoreAugmenter;
import org.javenstudio.falcon.search.transformer.TransformerFactory;
import org.javenstudio.falcon.search.transformer.ValueSourceAugmenter;

/**
 * A class representing the return fields
 *
 * @since 4.0
 */
public class SearchReturnFields extends ReturnFields {
	static final Logger LOG = Logger.getLogger(SearchReturnFields.class);

	// Special Field Keys
	public static final String SCORE = "score";

	private final List<String> mGlobs = new ArrayList<String>(1);
  
	// The index field names to request from the Searcher
	// Order is important for CSVResponseWriter
	private final Set<String> mFields = new LinkedHashSet<String>();
  
	// Field names that are OK to include in the response.
	// This will include pseudo fields, index fields, and matching globs
	private Set<String> mOkFieldNames = new HashSet<String>(); 

	// The list of explicitly requested fields
	private Set<String> mReqFieldNames = null;
  
	private final ISearchCore mCore;
	
	private DocTransformer mTransformer = null;
	private boolean mWantsScore = false;
	private boolean mWantsAllFields = false;

	public SearchReturnFields(ISearchCore core) {
		mCore = core;
		mWantsAllFields = true;
	}

	public SearchReturnFields(ISearchCore core, 
			ISearchRequest req) throws ErrorException {
		this(core, req.getParams().getParams(CommonParams.FL), req);
	}

	public SearchReturnFields(ISearchCore core, String fl, 
			ISearchRequest req) throws ErrorException {
		mCore = core;
		
		if (fl == null) {
			parseFieldList((String[])null, req);
			
		} else {
			if (fl.trim().length() == 0) {
				// legacy thing to support fl='  ' => fl=*,score!
				// maybe time to drop support for this?
				// See ConvertedLegacyTest
				mWantsScore = true;
				mWantsAllFields = true;
				
				mTransformer = new ScoreAugmenter(SCORE);
				
			} else {
				parseFieldList( new String[]{fl}, req);
			}
		}
	}

	public SearchReturnFields(ISearchCore core, String[] fl, 
			ISearchRequest req) throws ErrorException {
		mCore = core;
		parseFieldList(fl, req);
	}

	public ISearchCore getSearchCore() { return mCore; }
	public DocTransformer getTransformer() { return mTransformer; }
  
	private void parseFieldList(String[] fl, 
			ISearchRequest req) throws ErrorException {
		mWantsScore = false;
		mWantsAllFields = false;
		
		if (fl == null || fl.length == 0 || fl.length == 1 && fl[0].length()==0) {
			mWantsAllFields = true;
			return;
		}

		NamedList<String> rename = new NamedList<String>();
		DocTransformers augmenters = new DocTransformers();
		
		for (String fieldList : fl) {
			add(fieldList, rename, augmenters, req);
		}
		
		for (int i=0; i < rename.size(); i++) {
			String from = rename.getName(i);
			String to = rename.getVal(i);
			
			mOkFieldNames.add(to);
			
			boolean copy = (mReqFieldNames != null && mReqFieldNames.contains(from));
			if (!copy) {
				// Check that subsequent copy/rename requests have the field they need to copy
				for (int j=i+1; j < rename.size(); j++) {
					if (from.equals(rename.getName(j))) {
						rename.setName(j, to); // copy from the current target
						
						if (mReqFieldNames == null) 
							mReqFieldNames = new HashSet<String>();
						
						mReqFieldNames.add(to); // don't rename our current target
					}
				}
			}
			
			augmenters.addTransformer(new RenameFieldTransformer(from, to, copy));     
		}

		if (!mWantsAllFields) {
			if (!mGlobs.isEmpty()) {
				// TODO??? need to fill up the fields with matching field names in the index
				// and add them to okFieldNames?
				// maybe just get all fields?
				// this would disable field selection optimization... i think thatis OK
				// this will get all fields, and use wantsField to limit
				mFields.clear(); 
			}
			
			mOkFieldNames.addAll(mFields);
		}

		if (augmenters.size() == 1) {
			mTransformer = augmenters.getTransformer(0);
			
		} else if (augmenters.size() > 1) {
			mTransformer = augmenters;
		}
	}

	// like getId, but also accepts dashes for legacy fields
	protected String getFieldName(StringParser sp) {
		sp.eatws();
		
		int id_start = sp.getPos();
		char ch;
		
		if (sp.getPos() < sp.getEnd() && (ch = sp.getValue().charAt(sp.getPos())) != '$' && 
				Character.isJavaIdentifierStart(ch)) {
			sp.increasePos(1);
			
			while (sp.getPos() < sp.getEnd()) {
				ch = sp.getValue().charAt(sp.getPos());
				if (!Character.isJavaIdentifierPart(ch) && ch != '.' && ch != '-') 
					break;
				
				sp.increasePos(1);
			}
			
			return sp.getValue().substring(id_start, sp.getPos());
		}

		return null;
	}

	private void add(String fl, NamedList<String> rename, DocTransformers augmenters, 
			ISearchRequest req) throws ErrorException {
		if (fl == null) return;
		StringParser sp = new StringParser(fl);

		for (;;) {
			sp.opt(',');
			sp.eatws();
			
			if (sp.getPos() >= sp.getEnd()) 
				break;

			int start = sp.getPos();

			// short circuit test for a really simple field name
			String key = null;
			String field = getFieldName(sp);
			char ch = sp.ch();

			if (field != null) {
				if (sp.opt(':')) {
					// this was a key, not a field name
					key = field;
					field = null;
					
					sp.eatws();
					start = sp.getPos();
					
				} else {
					if (Character.isWhitespace(ch) || ch == ',' || ch==0) {
						addField( field, key, augmenters, req );
						continue;
					}
					
					// an invalid field name... reset the position pointer to retry
					sp.setPos(start);
					field = null;
				}
			}

			if (key != null) {
				// we read "key : "
				field = sp.getId(null);
				ch = sp.ch();
				
				if (field != null && (Character.isWhitespace(ch) || ch == ',' || ch==0)) {
					rename.add(field, key);
					addField( field, key, augmenters, req );
					continue;
				}
				
				// an invalid field name... reset the position pointer to retry
				sp.setPos(start);
				field = null;
			}

			if (field == null) {
				// We didn't find a simple name, so let's see if it's a globbed field name.
				// Globbing only works with field names of the recommended form 
				// (roughly like java identifiers)

				field = sp.getGlobbedId(null);
				ch = sp.ch();
				
				if (field != null && (Character.isWhitespace(ch) || ch == ',' || ch==0)) {
					// "*" looks and acts like a glob, but we give it special treatment
					if ("*".equals(field)) 
						mWantsAllFields = true;
					else 
						mGlobs.add(field);
					
					continue;
				}

				// an invalid glob
				sp.setPos(start);
			}

			String funcStr = sp.getValue().substring(start);

			// Is it an augmenter of the form [augmenter_name foo=1 bar=myfield]?
			// This is identical to localParams syntax except it uses [] instead of {!}

			if (funcStr.startsWith("[")) {
				Map<String,String> augmenterArgs = new HashMap<String,String>();
				int end = QueryParsing.parseLocalParams(funcStr, 0, augmenterArgs, req.getParams(), "[", ']');
				sp.increasePos(end);
          
				// [foo] is short for [type=foo] in localParams syntax
				String augmenterName = augmenterArgs.remove("type"); 
				String disp = key;
				if (disp == null) 
					disp = '[' + augmenterName + ']';
				
				TransformerFactory factory = getSearchCore().getTransformerFactory(augmenterName);
				if (factory != null) {
					MapParams augmenterParams = new MapParams(augmenterArgs);
					augmenters.addTransformer(factory.create(disp, augmenterParams, req));
				}
				
				addField(field, disp, augmenters, req);
				continue;
			}

			// let's try it as a function instead
			QueryBuilder parser = getSearchCore().getQueryFactory()
					.getQueryBuilder(funcStr, FunctionQueryBuilderPlugin.NAME, req);
			
			IQuery q = null;
			ValueSource vs = null;

			try {
				if (parser instanceof FunctionQueryBuilder) {
					FunctionQueryBuilder fparser = (FunctionQueryBuilder)parser;
					fparser.setParseMultipleSources(false);
					fparser.setParseToEnd(false);

					q = fparser.getQuery();

					if (fparser.getLocalParams() != null) {
						if (fparser.isFollowedParams()) {
							// need to find the end of the function query via the string parser
							int leftOver = fparser.getParser().getEnd() - fparser.getParser().getPos();
							// reset our parser to the same amount of leftover
							sp.setPos(sp.getEnd() - leftOver); 
							
						} else {
							// the value was via the "v" param in localParams, so we need to find
							// the end of the local params themselves to pick up where we left off
							sp.setPos(start + fparser.getLocalParamsEnd());
						}
						
					} else {
						// need to find the end of the function query via the string parser
						int leftOver = fparser.getParser().getEnd() - fparser.getParser().getPos();
						// reset our parser to the same amount of leftover
						sp.setPos(sp.getEnd() - leftOver); 
					}
					
				} else {
					// A QParser that's not for function queries.
					// It must have been specified via local params.
					q = parser.getQuery();

					assert parser.getLocalParams() != null;
					sp.setPos(start + parser.getLocalParamsEnd());
				}

				if (q instanceof FunctionQuery) {
					vs = ((FunctionQuery)q).getValueSource();
				} else {
					vs = new QueryValueSource(q, 0.0f);
				}

				if (key == null) {
					Params localParams = parser.getLocalParams();
					if (localParams != null) 
						key = localParams.get("key");
					
					if (key == null) {
						// use the function name itself as the field name
						key = sp.getValue().substring(start, sp.getPos());
					}
				}

				if (key == null) 
					key = funcStr;
          
				mOkFieldNames.add(key);
				mOkFieldNames.add(funcStr);
				
				augmenters.addTransformer(new ValueSourceAugmenter(key, parser, vs));
				
			} catch (Exception e) {
				// try again, simple rules for a field name with no whitespace
				sp.setPos(start);
				field = sp.getSimpleString();

				if (getSearchCore().getSchema().getFieldOrNull(field) != null) {
					// OK, it was an oddly named field
					mFields.add(field);
					if (key != null) 
						rename.add(field, key);
					
				} else {
					if (e instanceof ErrorException) {
						throw (ErrorException)e; 
					} else {
						throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
								"Error parsing fieldname: " + e.getMessage(), e);
					}
				}
			}
		}
	}

	private void addField(String field, String key, 
			DocTransformers augmenters, ISearchRequest req) {
		if (key == null) {
			if (mReqFieldNames == null) 
				mReqFieldNames = new HashSet<String>();
			
			mReqFieldNames.add(field);
		}
    
		// need to put in the map to maintain order for things like CSVResponseWriter
		mFields.add(field); 
		mOkFieldNames.add(field);
		mOkFieldNames.add(key);
		
		// a valid field name
		if (SCORE.equals(field)) {
			mWantsScore = true;

			String disp = (key == null) ? field : key;
			augmenters.addTransformer(new ScoreAugmenter(disp));
		}
	}

	public Set<String> getIndexFieldNames() {
		return (mWantsAllFields || mFields.isEmpty()) ? null : mFields;
	}

	public boolean wantsAllFields() { return mWantsAllFields; }
	public boolean wantsScore() { return mWantsScore; }

	@Override
	public boolean wantsField(String name) {
		if (mWantsAllFields || mOkFieldNames.contains(name)) 
			return true;
    
		for (String s : mGlobs) {
			// TODO something better?
			if (FilenameUtils.wildcardMatch(name, s)) {
				mOkFieldNames.add(name); // Don't calculate it again
				return true;
			}
		}
		
		return false;
	}
	
}
