package org.javenstudio.falcon.search.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.hornet.query.FunctionQuery;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.source.ConstValueSource;
import org.javenstudio.hornet.query.source.DoubleConstValueSource;
import org.javenstudio.hornet.query.source.LiteralValueSource;
import org.javenstudio.hornet.query.source.QueryValueSource;
import org.javenstudio.hornet.query.source.VectorValueSource;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.query.source.BoolConstSource;
import org.javenstudio.falcon.search.query.source.LongConstSource;
import org.javenstudio.falcon.search.schema.SchemaField;

public class FunctionQueryBuilder extends QueryBuilder {
	static final Logger LOG = Logger.getLogger(FunctionQueryBuilder.class);

  	private StringParser mParser;
  	private boolean mParseMultipleSources = true;
  	private boolean mParseToEnd = true;
  	private boolean mArgWasQuoted;

  	public FunctionQueryBuilder(String qstr, Params localParams, Params params, 
  			ISearchRequest req) throws ErrorException {
  		super(qstr, localParams, params, req);
  	}

  	public StringParser getParser() { return mParser; }
  	
  	public void setParseMultipleSources(boolean parseMultipleSources) {
  		mParseMultipleSources = parseMultipleSources;  
  	}

  	/** parse multiple comma separated value sources */
  	public boolean getParseMultipleSources() {
  		return mParseMultipleSources;
  	}

  	public void setParseToEnd(boolean parseToEnd) {
  		mParseToEnd = parseToEnd;
  	}

  	/** 
  	 * throw exception if there is extra stuff at the end of 
  	 * the parsed valuesource(s). 
  	 */
  	public boolean getParseToEnd() {
  		return mParseMultipleSources;
  	}

  	@Override
  	public IQuery parse() throws ErrorException {
  		mParser = new StringParser(getQueryString());

  		ValueSource vs = null;
  		List<ValueSource> lst = null;

  		for (;;) {
  			ValueSource valsource = parseValueSource(false);
  			mParser.eatws();
  			
  			if (!mParseMultipleSources) {
  				vs = valsource; 
  				break;
  				
  			} else {
  				if (lst != null) 
  					lst.add(valsource);
  				else 
  					vs = valsource;
  			}

  			// check if there is a "," separator
  			if (mParser.peek() != ',') 
  				break;

  			consumeArgumentDelimiter();

  			if (lst == null) {
  				lst = new ArrayList<ValueSource>(2);
  				lst.add(valsource);
  			}
  		}

  		if (mParseToEnd && mParser.getPos() < mParser.getEnd()) {
  			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
  					"Unexpected text after function: " 
  					+ mParser.getValue().substring(mParser.getPos(), mParser.getEnd()));
  		}

  		if (lst != null) 
  			vs = new VectorValueSource(lst);

  		if (LOG.isDebugEnabled()) {
  			LOG.debug("parsed FunctionQuery: valueSource=" + vs.getClass().getName() 
  					+ " queryString=" + getQueryString());
  		}
  		
  		return new FunctionQuery(vs);
  	}

  	/**
  	 * Are there more arguments in the argument list being parsed?
  	 * 
   	 * @return whether more args exist
   	 */
  	public boolean hasMoreArguments() throws ErrorException {
  		int ch = mParser.peek();
  		/** determine whether the function is ending with a paren or end of str */
  		return (!(ch == 0 || ch == ')'));
  	}
  
  	/**
  	 * TODO: Doc
  	 */
  	public String parseId() throws ErrorException {
  		String value = parseArg();
  		if (mArgWasQuoted) {
  			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
  					"Expected identifier instead of quoted string:" + value);
  		}
  		
  		return value;
  	}
  
  	/**
  	 * Parse a float.
  	 * 
  	 * @return Float
  	 */
  	public Float parseFloat() throws ErrorException {
  		String str = parseArg();
  		if (argWasQuoted()) {
  			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
  					"Expected float instead of quoted string:" + str);
  		}
  		
  		float value = Float.parseFloat(str);
  		return value;
  	}

  	/**
  	 * Parse a Double
  	 * @return double
  	 */
  	public double parseDouble() throws ErrorException {
  		String str = parseArg();
  		if (argWasQuoted()) {
  			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
  					"Expected double instead of quoted string:" + str);
  		}
  		
  		double value = Double.parseDouble(str);
  		return value;
  	}

  	/**
  	 * Parse an integer
  	 * @return An int
  	 */
  	public int parseInt() throws ErrorException {
  		String str = parseArg();
  		if (argWasQuoted()) {
  			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
  					"Expected double instead of quoted string:" + str);
  		}
  		
  		int value = Integer.parseInt(str);
  		return value;
  	}

  	public boolean argWasQuoted() {
  		return mArgWasQuoted;
  	}

  	public String parseArg() throws ErrorException {
  		mArgWasQuoted = false;
  		mParser.eatws();
  		
  		char ch = mParser.peek();
  		String val = null;
  		switch (ch) {
  		case ')': return null;
  		case '$':
  			mParser.increasePos(1);
  			String param = mParser.getId();
  			val = getParam(param);
  			break;
  			
  		case '\'':
  		case '"':
  			val = mParser.getQuotedString();
  			mArgWasQuoted = true;
  			break;
  			
  		default:
  			// read unquoted literal ended by whitespace ',' or ')'
  			// there is no escaping.
  			int valStart = mParser.getPos();
  			for (;;) {
  				if (mParser.getPos() >= mParser.getEnd()) {
  					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
  							"Missing end to unquoted value starting at " 
  							+ valStart + " str='" + mParser.getValue() +"'");
  				}
  				
  				char c = mParser.getValue().charAt(mParser.getPos());
  				if (c == ')' || c == ',' || Character.isWhitespace(c)) {
  					val = mParser.getValue().substring(valStart, mParser.getPos());
  					break;
  				}
  				
  				mParser.increasePos(1);
  			}
  		}

  		mParser.eatws();
  		consumeArgumentDelimiter();
  		
  		return val;
  	}

  	/**
  	 * Parse a list of ValueSource.  Must be the final set of arguments
  	 * to a ValueSource.
  	 * 
  	 * @return List&lt;ValueSource&gt;
  	 */
  	public List<ValueSource> parseValueSourceList() throws ErrorException {
  		List<ValueSource> sources = new ArrayList<ValueSource>(3);
  		while (hasMoreArguments()) {
  			sources.add(parseValueSource(true));
  		}
  		return sources;
  	}

  	/**
  	 * Parse an individual ValueSource.
  	 */
  	public ValueSource parseValueSource() throws ErrorException {
  		/** consume the delimiter afterward for an external call to parseValueSource */
  		return parseValueSource(true);
  	}
  
  	/**
  	 * TODO: Doc
  	 */
  	public IQuery parseNestedQuery() throws ErrorException {
  		IQuery nestedQuery;
    
  		if (mParser.opt("$")) {
  			String param = mParser.getId();
  			String qstr = getParam(param);
  			qstr = qstr == null ? "" : qstr;
  			
  			nestedQuery = subQuery(qstr, null).getQuery();
  			
  		} else {
  			int start = mParser.getPos();
  			String v = mParser.getValue();
  			String qs = v;
  			
  			Map<String,String> nestedLocalParams = new HashMap<String,String>();
  			int end = QueryParsing.parseLocalParams(qs, 
  					start, nestedLocalParams, getParams());
  
  			QueryBuilder sub;
  			if (end > start) {
  				if (nestedLocalParams.get(QueryParsing.V) != null) {
  					// value specified directly in local params... so the end of the
  					// query should be the end of the local params.
  					sub = subQuery(qs.substring(start, end), null);
  					
  				} else {
  					// value here is *after* the local params... ask the parser.
  					sub = subQuery(qs, null);
  					
  					// int subEnd = sub.findEnd(')');
  					// TODO.. implement functions to find the end of a nested query
  					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
  							"Nested local params must have value in v parameter. got '" 
  							+ qs + "'");
  				}
  			} else {
  				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
  						"Nested function query must use $param or {!v=value} forms. got '" 
  						+ qs + "'");
  			}
  
  			mParser.increasePos(end-start);  // advance past nested query
  			nestedQuery = sub.getQuery();
  		}
  		
  		consumeArgumentDelimiter();
    
  		return nestedQuery;
  	}

  	/**
  	 * Parse an individual value source.
  	 * 
  	 * @param doConsumeDelimiter whether to consume a delimiter following the ValueSource  
  	 */
  	protected ValueSource parseValueSource(boolean doConsumeDelimiter) 
  			throws ErrorException {
  		ValueSource valueSource;
    
  		int ch = mParser.peek();
  		if (ch >= '0' && ch <= '9'  || ch == '.' || ch == '+' || ch == '-') {
  			Number num = mParser.getNumber();
  			if (num instanceof Long) {
  				valueSource = new LongConstSource(num.longValue());
  			} else if (num instanceof Double) {
  				valueSource = new DoubleConstValueSource(num.doubleValue());
  			} else {
  				// shouldn't happen
  				valueSource = new ConstValueSource(num.floatValue());
  			}
  			
  		} else if (ch == '"' || ch == '\''){
  			valueSource = new LiteralValueSource(mParser.getQuotedString());
  			
  		} else if (ch == '$') {
  			mParser.increasePos(1);
  			
  			String param = mParser.getId();
  			String val = getParam(param);
  			if (val == null) {
  				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
  						"Missing param " + param + " while parsing function '" 
  						+ mParser.getValue() + "'");
  			}

  			QueryBuilder subParser = subQuery(val, "func");
  			if (subParser instanceof FunctionQueryBuilder) 
  				((FunctionQueryBuilder)subParser).setParseMultipleSources(true);
  			
  			IQuery subQuery = subParser.getQuery();
  			if (subQuery instanceof FunctionQuery) {
  				valueSource = ((FunctionQuery) subQuery).getValueSource();
  			} else {
  				valueSource = new QueryValueSource(subQuery, 0.0f);
  			}

  			/***
       		// dereference *simple* argument (i.e., can't currently be a function)
       		// In the future we could support full function dereferencing via 
       		// a stack of ValueSource (or StringParser) objects
      		ch = val.length()==0 ? '\0' : val.charAt(0);

      		if (ch>='0' && ch<='9'  || ch=='.' || ch=='+' || ch=='-') {
        		QueryParsing.StrParser sp = new QueryParsing.StrParser(val);
        		Number num = sp.getNumber();
        		if (num instanceof Long) {
          			valueSource = new LongConstValueSource(num.longValue());
        		} else if (num instanceof Double) {
          			valueSource = new DoubleConstValueSource(num.doubleValue());
        		} else {
          			// shouldn't happen
          			valueSource = new ConstValueSource(num.floatValue());
        		}
      		} else if (ch == '"' || ch == '\'') {
        		QueryParsing.StrParser sp = new QueryParsing.StrParser(val);
        		val = sp.getQuotedString();
        		valueSource = new LiteralValueSource(val);
      		} else {
        		if (val.length()==0) {
          			valueSource = new LiteralValueSource(val);
        		} else {
          			String id = val;
          			SchemaField f = req.getSchema().getField(id);
          			valueSource = f.getType().getValueSource(f, this);
        		}
      		}
  			 ***/

  		} else {
  			String id = mParser.getId();
  			if (mParser.opt("(")) {
  				// a function... look it up.
  				ValueSourceParser argParser = getSearchCore().getValueSourceFactory().getParser(id);
  				if (argParser == null) {
  					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
  							"Unknown function " + id + " in FunctionQuery(" + mParser + ")");
  				}
  				
  				valueSource = argParser.parse(this);
  				mParser.expect(")");
  				
  			} else {
  				if ("true".equals(id)) {
  					valueSource = new BoolConstSource(true);
  				} else if ("false".equals(id)) {
  					valueSource = new BoolConstSource(false);
  				} else {
  					SchemaField f = getSearchCore().getSchema().getField(id);
  					valueSource = f.getType().getValueSource(f, this);
  					
  					if (LOG.isDebugEnabled()) { 
  						LOG.debug("parseValueSource: field=" + id 
  								+ " type=" + f.getType().getClass().getName() 
  								+ " source=" + valueSource.getClass().getName());
  					}
  				}
  			}
  		}
    
  		if (doConsumeDelimiter)
  			consumeArgumentDelimiter();
    
  		return valueSource;
  	}

  	/**
  	 * Consume an argument delimiter (a comma) from the token stream.
  	 * Only consumes if more arguments should exist (no ending parens or end of string).
  	 * 
  	 * @return whether a delimiter was consumed
  	 */
  	protected boolean consumeArgumentDelimiter() throws ErrorException {
  		/** if a list of args is ending, don't expect the comma */
  		if (hasMoreArguments()) {
  			mParser.expect(",");
  			return true;
  		}
   
  		return false;
  	}
	
}
