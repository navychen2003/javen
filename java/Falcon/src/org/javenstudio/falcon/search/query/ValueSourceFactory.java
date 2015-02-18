package org.javenstudio.falcon.search.query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.hornet.query.BoostedQuery;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.source.DefFunction;
import org.javenstudio.hornet.query.source.DivFloatFunction;
import org.javenstudio.hornet.query.source.DoubleConstValueSource;
import org.javenstudio.hornet.query.source.IfFunction;
import org.javenstudio.hornet.query.source.JoinDocFreqValueSource;
import org.javenstudio.hornet.query.source.LinearFloatFunction;
import org.javenstudio.hornet.query.source.LiteralValueSource;
import org.javenstudio.hornet.query.source.MaxDocValueSource;
import org.javenstudio.hornet.query.source.MaxFloatFunction;
import org.javenstudio.hornet.query.source.MinFloatFunction;
import org.javenstudio.hornet.query.source.NormValueSource;
import org.javenstudio.hornet.query.source.NumDocsValueSource;
import org.javenstudio.hornet.query.source.OrdFieldSource;
import org.javenstudio.hornet.query.source.ProductFloatFunction;
import org.javenstudio.hornet.query.source.QueryValueSource;
import org.javenstudio.hornet.query.source.RangeMapFloatFunction;
import org.javenstudio.hornet.query.source.ReciprocalFloatFunction;
import org.javenstudio.hornet.query.source.ReverseOrdFieldSource;
import org.javenstudio.hornet.query.source.ScaleFloatFunction;
import org.javenstudio.hornet.query.source.SumFloatFunction;
import org.javenstudio.hornet.query.source.VectorValueSource;
import org.javenstudio.falcon.search.ISearchCore;
import org.javenstudio.falcon.search.query.source.BoolConstSource;
import org.javenstudio.falcon.search.query.source.DateSourceParser;
import org.javenstudio.falcon.search.query.source.Double2SourceParser;
import org.javenstudio.falcon.search.query.source.DoubleSourceParser;
import org.javenstudio.falcon.search.query.source.GeohashFunction;
import org.javenstudio.falcon.search.query.source.GeohashHaversineFunction;
import org.javenstudio.falcon.search.query.source.HaversineConstFunction;
import org.javenstudio.falcon.search.query.source.LongConstSource;
import org.javenstudio.falcon.search.query.source.NamedSourceParser;
import org.javenstudio.falcon.search.query.source.TestValueSource;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.falcon.search.schema.spatial.DistanceUtils;

public class ValueSourceFactory {

	/** standard functions */
	private static Map<String, ValueSourceParser> sStandardValueSourceParsers = 
			new HashMap<String, ValueSourceParser>();

	static { initStandardParsers(); }
	
	/** 
	 * Adds a new parser for the name and returns any existing one that was overriden.
	 *  This is not thread safe.
	 */
	public static ValueSourceParser addParser(String name, ValueSourceParser p) {
		synchronized (sStandardValueSourceParsers) {
			return sStandardValueSourceParsers.put(name, p);
		}
	}

	/** 
	 * Adds a new parser for the name and returns any existing one that was overriden.
	 *  This is not thread safe.
	 */
	public static ValueSourceParser addParser(NamedSourceParser p) {
		synchronized (sStandardValueSourceParsers) {
			return sStandardValueSourceParsers.put(p.getName(), p);
		}
	}

	public static void alias(String source, String dest) {
		synchronized (sStandardValueSourceParsers) {
			sStandardValueSourceParsers.put(dest, sStandardValueSourceParsers.get(source));
		}
	}
	
	private final ISearchCore mCore; 
	private final Map<String,ValueSourceParser> mParsers;
	
	public ValueSourceFactory(ISearchCore core) throws ErrorException { 
		mCore = core;
		mParsers = new HashMap<String,ValueSourceParser>();
		
		initParsers();
	}
	
	public final ISearchCore getSearchCore() { 
		if (mCore == null) 
			throw new NullPointerException("SearchCore not set");
		
		return mCore;
	}
	
	private void initParsers() throws ErrorException { 
		mCore.initPlugins(mParsers, ValueSourceParser.class);
		
		synchronized (sStandardValueSourceParsers) { 
			// default value source parsers
			for (Map.Entry<String,ValueSourceParser> entry : sStandardValueSourceParsers.entrySet()) { 
				try {
					String name = entry.getKey();
					if (!mParsers.containsKey(name)) { 
						ValueSourceParser parser = entry.getValue();
						parser.mCore = mCore;
						mParsers.put(name, parser);
						parser.init(NamedList.EMPTY);
					}
				} catch (Exception ex) { 
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
				}
			}
		}
	}
	
	public ValueSourceParser getParser(String parserName) { 
		return mParsers.get(parserName);
	}
	
	private static void initStandardParsers() {
		addParser("testfunc", new TestValueSource.Parser());
		
		addParser("ord", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					String field = fp.parseId();
					return new OrdFieldSource(field);
				}
			});
    
		addParser("literal", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					return new LiteralValueSource(fp.parseArg());
				}
			});
    
		addParser("threadid", new LongConstSource.Parser());
    
		addParser("sleep", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					return SourceParsing.parseSleep(fp);
				}
			});
    
		addParser("rord", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					String field = fp.parseId();
					return new ReverseOrdFieldSource(field);
				}
			});
    
		addParser("top", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					// top(vs) is now a no-op
					ValueSource source = fp.parseValueSource();
					return source;
				}
			});
    
		addParser("linear", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					ValueSource source = fp.parseValueSource();
					float slope = fp.parseFloat();
					float intercept = fp.parseFloat();
					return new LinearFloatFunction(source, slope, intercept);
				}
			});
    
		addParser("recip", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					ValueSource source = fp.parseValueSource();
					float m = fp.parseFloat();
					float a = fp.parseFloat();
					float b = fp.parseFloat();
					return new ReciprocalFloatFunction(source, m, a, b);
				}
			});
    
		addParser("scale", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					ValueSource source = fp.parseValueSource();
					float min = fp.parseFloat();
					float max = fp.parseFloat();
					return new ScaleFloatFunction(source, min, max);
				}
			});
    
		addParser("div", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					ValueSource a = fp.parseValueSource();
					ValueSource b = fp.parseValueSource();
					return new DivFloatFunction(a, b);
				}
			});
    
		addParser("mod", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					return SourceParsing.parseMod(fp);
				}
			});
    
		addParser("map", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					ValueSource source = fp.parseValueSource();
					float min = fp.parseFloat();
					float max = fp.parseFloat();
					float target = fp.parseFloat();
					Float def = fp.hasMoreArguments() ? fp.parseFloat() : null;
					return new RangeMapFloatFunction(source, min, max, target, def);
				}
			});

		addParser("abs", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					return SourceParsing.parseAbs(fp);
				}
			});
    
		addParser("sum", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					List<ValueSource> sources = fp.parseValueSourceList();
					return new SumFloatFunction(sources.toArray(new ValueSource[sources.size()]));
				}
			});
		
		alias("sum","add");    

		addParser("product", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					List<ValueSource> sources = fp.parseValueSourceList();
					return new ProductFloatFunction(sources.toArray(new ValueSource[sources.size()]));
				}
			});
		
		alias("product","mul");

		addParser("sub", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					return SourceParsing.parseSub(fp);
				}
			});
    
		addParser("vector", new ValueSourceParser(){
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					return new VectorValueSource(fp.parseValueSourceList());
				}
			});
    
		addParser("query", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					return SourceParsing.parseQuery(fp);
				}
			});
    
		addParser("boost", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					IQuery q = fp.parseNestedQuery();
					ValueSource vs = fp.parseValueSource();
					BoostedQuery bq = new BoostedQuery(q, vs);
					return new QueryValueSource(bq, 0.0f);
				}
			});
    
		addParser("joindf", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					String f0 = fp.parseArg();
					String qf = fp.parseArg();
					return new JoinDocFreqValueSource(f0, qf);
				}
			});

		addParser("geodist", HaversineConstFunction.getDefaultParser());

		addParser("hsin", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					return SourceParsing.parseHsin(fp);
				}
			});

		addParser("ghhsin", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					double radius = fp.parseDouble();
	
					ValueSource gh1 = fp.parseValueSource();
					ValueSource gh2 = fp.parseValueSource();
	
					return new GeohashHaversineFunction(gh1, gh2, radius);
				}
			});

		addParser("geohash", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					ValueSource lat = fp.parseValueSource();
					ValueSource lon = fp.parseValueSource();
	
					return new GeohashFunction(lat, lon);
				}
			});
    
		addParser("strdist", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					return SourceParsing.parseStrdist(fp);
				}
		    });
    
		addParser("field", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					String fieldName = fp.parseArg();
					SchemaField f = getSearchCore().getSchema().getField(fieldName);
					return f.getType().getValueSource(f, fp);
				}
			});

		addParser(new DoubleSourceParser("rad") {
				@Override
				public double callFunc(int doc, FunctionValues vals) {
					return vals.doubleVal(doc) * DistanceUtils.DEGREES_TO_RADIANS;
				}
			});
    
		addParser(new DoubleSourceParser("deg") {
				@Override
				public double callFunc(int doc, FunctionValues vals) {
					return vals.doubleVal(doc) * DistanceUtils.RADIANS_TO_DEGREES;
				}
			});
    
		addParser(new DoubleSourceParser("sqrt") {
				@Override
				public double callFunc(int doc, FunctionValues vals) {
					return Math.sqrt(vals.doubleVal(doc));
				}
			});
		
		addParser(new DoubleSourceParser("cbrt") {
				@Override
				public double callFunc(int doc, FunctionValues vals) {
					return Math.cbrt(vals.doubleVal(doc));
				}
			});
    
		addParser(new DoubleSourceParser("log") {
				@Override
				public double callFunc(int doc, FunctionValues vals) {
					return Math.log10(vals.doubleVal(doc));
				}
			});
		
		addParser(new DoubleSourceParser("ln") {
				@Override
				public double callFunc(int doc, FunctionValues vals) {
					return Math.log(vals.doubleVal(doc));
				}
		});
    
		addParser(new DoubleSourceParser("exp") {
				@Override
				public double callFunc(int doc, FunctionValues vals) {
					return Math.exp(vals.doubleVal(doc));
				}
			});
		
		addParser(new DoubleSourceParser("sin") {
				@Override
				public double callFunc(int doc, FunctionValues vals) {
					return Math.sin(vals.doubleVal(doc));
				}
			});
    
		addParser(new DoubleSourceParser("cos") {
				@Override
				public double callFunc(int doc, FunctionValues vals) {
					return Math.cos(vals.doubleVal(doc));
				}
			});
		
		addParser(new DoubleSourceParser("tan") {
				@Override
				public double callFunc(int doc, FunctionValues vals) {
					return Math.tan(vals.doubleVal(doc));
				}
			});
		
		addParser(new DoubleSourceParser("asin") {
				@Override
				public double callFunc(int doc, FunctionValues vals) {
					return Math.asin(vals.doubleVal(doc));
				}
			});
		
		addParser(new DoubleSourceParser("acos") {
				@Override
				public double callFunc(int doc, FunctionValues vals) {
					return Math.acos(vals.doubleVal(doc));
				}
			});
		
		addParser(new DoubleSourceParser("atan") {
				@Override
				public double callFunc(int doc, FunctionValues vals) {
					return Math.atan(vals.doubleVal(doc));
				}
			});
		
		addParser(new DoubleSourceParser("sinh") {
				@Override
				public double callFunc(int doc, FunctionValues vals) {
					return Math.sinh(vals.doubleVal(doc));
				}
			});
		
		addParser(new DoubleSourceParser("cosh") {
				@Override
				public double callFunc(int doc, FunctionValues vals) {
					return Math.cosh(vals.doubleVal(doc));
				}
			});
		
		addParser(new DoubleSourceParser("tanh") {
				@Override
				public double callFunc(int doc, FunctionValues vals) {
					return Math.tanh(vals.doubleVal(doc));
				}
			});
		
		addParser(new DoubleSourceParser("ceil") {
				@Override
				public double callFunc(int doc, FunctionValues vals) {
					return Math.ceil(vals.doubleVal(doc));
				}
			});
		
		addParser(new DoubleSourceParser("floor") {
				@Override
				public double callFunc(int doc, FunctionValues vals) {
					return Math.floor(vals.doubleVal(doc));
				}
			});
		
		addParser(new DoubleSourceParser("rint") {
				@Override
				public double callFunc(int doc, FunctionValues vals) {
					return Math.rint(vals.doubleVal(doc));
				}
			});
		
		addParser(new Double2SourceParser("pow") {
				@Override
				public double callFunc(int doc, FunctionValues a, FunctionValues b) {
					return Math.pow(a.doubleVal(doc), b.doubleVal(doc));
				}
			});
		
		addParser(new Double2SourceParser("hypot") {
				@Override
				public double callFunc(int doc, FunctionValues a, FunctionValues b) {
					return Math.hypot(a.doubleVal(doc), b.doubleVal(doc));
				}
			});
		
		addParser(new Double2SourceParser("atan2") {
				@Override
				public double callFunc(int doc, FunctionValues a, FunctionValues b) {
					return Math.atan2(a.doubleVal(doc), b.doubleVal(doc));
				}
			});
		
		addParser("max", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					List<ValueSource> sources = fp.parseValueSourceList();
					return new MaxFloatFunction(sources.toArray(new ValueSource[sources.size()]));
				}
			});
		
		addParser("min", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					List<ValueSource> sources = fp.parseValueSourceList();
					return new MinFloatFunction(sources.toArray(new ValueSource[sources.size()]));
				}
			});
		
		addParser("sqedist", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					return SourceParsing.parseSqedist(fp);
				}
			});

		addParser("dist", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					return SourceParsing.parseDist(fp);
				}
			});
		
		addParser("ms", new DateSourceParser());

		addParser("pi", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) {
					return new DoubleConstValueSource(Math.PI);
				}
			});
		
		addParser("e", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) {
					return new DoubleConstValueSource(Math.E);
				}
			});

		addParser("docfreq", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					return SourceParsing.parseDocfreq(fp);
				}
			});

		addParser("totaltermfreq", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					return SourceParsing.parseTotaltermfreq(fp);
				}
			});
		
		alias("totaltermfreq", "ttf");

		addParser("sumtotaltermfreq", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					return SourceParsing.parseSumtotaltermfreq(fp);
				}
			});
		
		alias("sumtotaltermfreq","sttf");

		addParser("idf", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					return SourceParsing.parseIdf(fp);
				}
			});

		addParser("termfreq", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					return SourceParsing.parseTermfreq(fp);
				}
			});

		addParser("tf", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					return SourceParsing.parseTF(fp);
				}
			});

		addParser("norm", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					String field = fp.parseArg();
					return new NormValueSource(field);
				}
			});

		addParser("maxdoc", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) {
					return new MaxDocValueSource();
				}
			});

		addParser("numdocs", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) {
					return new NumDocsValueSource();
				}
			});

		addParser("true", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) {
					return new BoolConstSource(true);
				}
			});

		addParser("false", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) {
					return new BoolConstSource(false);
				}
			});

		addParser("exists", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					return SourceParsing.parseExists(fp);
				}
			});

		addParser("not", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					return SourceParsing.parseNot(fp);
				}
			});

		addParser("and", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					return SourceParsing.parseAnd(fp);
				}
			});

		addParser("or", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					return SourceParsing.parseOr(fp);
				}
			});

		addParser("xor", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					return SourceParsing.parseXor(fp);
				}
			});

		addParser("if", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					ValueSource ifValueSource = fp.parseValueSource();
					ValueSource trueValueSource = fp.parseValueSource();
					ValueSource falseValueSource = fp.parseValueSource();
	
					return new IfFunction(ifValueSource, trueValueSource, falseValueSource);
				}
			});

		addParser("def", new ValueSourceParser() {
				@Override
				public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
					return new DefFunction(fp.parseValueSourceList());
				}
			});

	}
	
}
