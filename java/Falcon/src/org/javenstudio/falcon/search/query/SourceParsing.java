package org.javenstudio.falcon.search.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.UnicodeUtil;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.source.DocFreqValueSource;
import org.javenstudio.hornet.query.source.DualFloatFunction;
import org.javenstudio.hornet.query.source.IDFValueSource;
import org.javenstudio.hornet.query.source.MultiBoolFunction;
import org.javenstudio.hornet.query.source.MultiValueSource;
import org.javenstudio.hornet.query.source.QueryValueSource;
import org.javenstudio.hornet.query.source.SimpleBoolFunction;
import org.javenstudio.hornet.query.source.SimpleFloatFunction;
import org.javenstudio.hornet.query.source.SumTotalTermFreqValueSource;
import org.javenstudio.hornet.query.source.TFValueSource;
import org.javenstudio.hornet.query.source.TermFreqValueSource;
import org.javenstudio.hornet.query.source.TotalTermFreqValueSource;
import org.javenstudio.hornet.query.source.VectorValueSource;
import org.javenstudio.hornet.search.query.TermQuery;
import org.javenstudio.falcon.search.query.source.HaversineFunction;
import org.javenstudio.falcon.search.query.source.SquaredEuclideanFunction;
import org.javenstudio.falcon.search.query.source.StringDistanceFunction;
import org.javenstudio.falcon.search.query.source.VectorDistanceFunction;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.falcon.search.schema.SchemaFieldType;
import org.javenstudio.falcon.search.schema.type.StringFieldType;
import org.javenstudio.falcon.search.schema.type.TextFieldType;
import org.javenstudio.panda.util.JaroWinklerDistance;
import org.javenstudio.panda.util.LevensteinDistance;
import org.javenstudio.panda.util.NGramDistance;
import org.javenstudio.panda.util.StringDistance;

public class SourceParsing {

	public static ValueSource parseSleep(FunctionQueryBuilder fp) 
			throws ErrorException {
		int ms = fp.parseInt();
		ValueSource source = fp.parseValueSource();
		
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		return source;
	}
	
	public static ValueSource parseMod(FunctionQueryBuilder fp) 
			throws ErrorException {
		ValueSource a = fp.parseValueSource();
		ValueSource b = fp.parseValueSource();
		
		return new DualFloatFunction(a, b) {
				@Override
				protected String getName() {
					return "mod";
				}
				
				@Override
				protected float callFunc(int doc, 
						FunctionValues aVals, FunctionValues bVals) {
					return aVals.floatVal(doc) % bVals.floatVal(doc);
				}
			};
	}
	
	public static ValueSource parseAbs(FunctionQueryBuilder fp) 
			throws ErrorException {
		ValueSource source = fp.parseValueSource();
		
		return new SimpleFloatFunction(source) {
				@Override
				protected String getName() {
					return "abs";
				}
	
				@Override
				protected float callFunc(int doc, FunctionValues vals) {
					return Math.abs(vals.floatVal(doc));
				}
			};
	}
	
	public static ValueSource parseSub(FunctionQueryBuilder fp) 
			throws ErrorException {
		ValueSource a = fp.parseValueSource();
		ValueSource b = fp.parseValueSource();
		
		return new DualFloatFunction(a, b) {
				@Override
				protected String getName() {
					return "sub";
				}
	
				@Override
				protected float callFunc(int doc, 
						FunctionValues aVals, FunctionValues bVals) {
					return aVals.floatVal(doc) - bVals.floatVal(doc);
				}
			};
	}
	
	// boost(query($q),rating)
	public static ValueSource parseQuery(FunctionQueryBuilder fp) 
			throws ErrorException {
		IQuery q = fp.parseNestedQuery();
		float defVal = 0.0f;
		
		if (fp.hasMoreArguments()) 
			defVal = fp.parseFloat();
		
		return new QueryValueSource(q, defVal);
	}
	
	public static ValueSource parseHsin(FunctionQueryBuilder fp) 
			throws ErrorException {
		double radius = fp.parseDouble();
		
		//BUG-2114, make the convert flag required, since the parser 
		// doesn't support much in the way of lookahead or the ability to 
		// convert a String into a ValueSource
		boolean convert = Boolean.parseBoolean(fp.parseArg());

		MultiValueSource pv1;
		MultiValueSource pv2;

		ValueSource one = fp.parseValueSource();
		ValueSource two = fp.parseValueSource();
		
		if (fp.hasMoreArguments()) {
			List<ValueSource> s1 = new ArrayList<ValueSource>();
			s1.add(one);
			s1.add(two);
			
			pv1 = new VectorValueSource(s1);
			
			ValueSource x2 = fp.parseValueSource();
			ValueSource y2 = fp.parseValueSource();
			
			List<ValueSource> s2 = new ArrayList<ValueSource>();
			s2.add(x2);
			s2.add(y2);
			
			pv2 = new VectorValueSource(s2);
			
		} else {
			//check to see if we have multiValue source
			if (one instanceof MultiValueSource && two instanceof MultiValueSource){
				pv1 = (MultiValueSource) one;
				pv2 = (MultiValueSource) two;
				
			} else {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
						"Input must either be 2 MultiValueSources, or there must be 4 ValueSources");
			}
		}

		return new HaversineFunction(pv1, pv2, radius, convert);
	}
	
	public static ValueSource parseStrdist(FunctionQueryBuilder fp) 
			throws ErrorException {
		ValueSource str1 = fp.parseValueSource();
		ValueSource str2 = fp.parseValueSource();
		String distClass = fp.parseArg();

		StringDistance dist = null;
		if (distClass.equalsIgnoreCase("jw")) {
			dist = new JaroWinklerDistance();
			
		} else if (distClass.equalsIgnoreCase("edit")) {
			dist = new LevensteinDistance();
			
		} else if (distClass.equalsIgnoreCase("ngram")) {
			int ngram = 2;
			if (fp.hasMoreArguments()) 
				ngram = fp.parseInt();
			
			dist = new NGramDistance(ngram);
			
		} else {
			dist = fp.getSearchCore().newInstance(distClass, 
					StringDistance.class);
		}
		
		return new StringDistanceFunction(str1, str2, dist);
	}
	
	public static ValueSource parseExists(FunctionQueryBuilder fp) 
			throws ErrorException {
		ValueSource vs = fp.parseValueSource();
		return new SimpleBoolFunction(vs) {
			@Override
			protected String getName() {
				return "exists";
			}
			@Override
			protected boolean callFunc(int doc, FunctionValues vals) {
				return vals.exists(doc);
			}
		};
	}
	
	public static ValueSource parseNot(FunctionQueryBuilder fp) 
			throws ErrorException {
		ValueSource vs = fp.parseValueSource();
		return new SimpleBoolFunction(vs) {
			@Override
			protected boolean callFunc(int doc, FunctionValues vals) {
				return !vals.boolVal(doc);
			}
			@Override
			protected String getName() {
				return "not";
			}
		};
	}
	
	public static ValueSource parseAnd(FunctionQueryBuilder fp) 
			throws ErrorException {
		List<ValueSource> sources = fp.parseValueSourceList();
		return new MultiBoolFunction(sources) {
			@Override
			protected String getName() {
				return "and";
			}
			@Override
			protected boolean callFunc(int doc, FunctionValues[] vals) {
				for (FunctionValues dv : vals)
					if (!dv.boolVal(doc)) return false;
				return true;
			}
		};
	}
	
	public static ValueSource parseOr(FunctionQueryBuilder fp) 
			throws ErrorException {
		List<ValueSource> sources = fp.parseValueSourceList();
		return new MultiBoolFunction(sources) {
			@Override
			protected String getName() {
				return "or";
			}
			@Override
			protected boolean callFunc(int doc, FunctionValues[] vals) {
				for (FunctionValues dv : vals)
					if (dv.boolVal(doc)) return true;
				return false;
			}
		};
	}
	
	public static ValueSource parseXor(FunctionQueryBuilder fp) 
			throws ErrorException {
		List<ValueSource> sources = fp.parseValueSourceList();
		return new MultiBoolFunction(sources) {
			@Override
			protected String getName() {
				return "xor";
			}
			@Override
			protected boolean callFunc(int doc, FunctionValues[] vals) {
				int nTrue=0, nFalse=0;
				for (FunctionValues dv : vals) {
					if (dv.boolVal(doc)) nTrue++;
					else nFalse++;
				}
				return nTrue != 0 && nFalse != 0;
			}
		};
	}
	
	static TInfo parseTerm(FunctionQueryBuilder fp) throws ErrorException {
	    TInfo tinfo = new TInfo();

	    tinfo.mIndexedField = tinfo.mField = fp.parseArg();
	    tinfo.mValue = fp.parseArg();
	    tinfo.mIndexedBytes = new BytesRef();

	    SchemaFieldType ft = fp.getSearchCore().getSchema().getFieldTypeNoEx(tinfo.mField);
	    if (ft == null) 
	    	ft = new StringFieldType();

	    if (ft instanceof TextFieldType) {
	    	// need to do analysis on the term
	    	String indexedVal = tinfo.mValue;
	    	SchemaField field = fp.getSearchCore().getSchema().getFieldOrNull(tinfo.mField);
	    	IQuery q = ft.getFieldQuery(fp, field, tinfo.mValue);
	    	
	    	if (q instanceof TermQuery) {
	    		ITerm term = ((TermQuery)q).getTerm();
	    		tinfo.mIndexedField = term.getField();
	    		indexedVal = term.getText();
	    	}
	    	
	    	UnicodeUtil.UTF16toUTF8(indexedVal, 0, indexedVal.length(), 
	    			tinfo.mIndexedBytes);
	    	
	    } else {
	    	ft.readableToIndexed(tinfo.mValue, tinfo.mIndexedBytes);
	    }

	    return tinfo;
	}

	public static ValueSource parseSqedist(FunctionQueryBuilder fp) 
			throws ErrorException {
		List<ValueSource> sources = fp.parseValueSourceList();
		MVResult mvr = getMultiValueSources(sources);

		return new SquaredEuclideanFunction(mvr.mSource1, mvr.mSource2);
	}
	
	public static ValueSource parseDist(FunctionQueryBuilder fp) 
			throws ErrorException {
		float power = fp.parseFloat();
		List<ValueSource> sources = fp.parseValueSourceList();
		MVResult mvr = getMultiValueSources(sources);
		return new VectorDistanceFunction(power, mvr.mSource1, mvr.mSource2);
	}
	
	public static ValueSource parseDocfreq(FunctionQueryBuilder fp) 
			throws ErrorException {
		TInfo tinfo = parseTerm(fp);
		return new DocFreqValueSource(tinfo.mField, tinfo.mValue, 
				tinfo.mIndexedField, tinfo.mIndexedBytes);
	}
	
	public static ValueSource parseTotaltermfreq(FunctionQueryBuilder fp) 
			throws ErrorException {
		TInfo tinfo = parseTerm(fp);
		return new TotalTermFreqValueSource(tinfo.mField, tinfo.mValue, 
				tinfo.mIndexedField, tinfo.mIndexedBytes);
	}
	
	public static ValueSource parseSumtotaltermfreq(FunctionQueryBuilder fp) 
			throws ErrorException {
		String field = fp.parseArg();
		return new SumTotalTermFreqValueSource(field);
	}
	
	public static ValueSource parseIdf(FunctionQueryBuilder fp) 
			throws ErrorException {
		TInfo tinfo = parseTerm(fp);
		return new IDFValueSource(tinfo.mField, tinfo.mValue, 
				tinfo.mIndexedField, tinfo.mIndexedBytes);
	}
	
	public static ValueSource parseTermfreq(FunctionQueryBuilder fp) 
			throws ErrorException {
		TInfo tinfo = parseTerm(fp);
		return new TermFreqValueSource(tinfo.mField, tinfo.mValue, 
				tinfo.mIndexedField, tinfo.mIndexedBytes);
	}
	
	public static ValueSource parseTF(FunctionQueryBuilder fp) 
			throws ErrorException {
		TInfo tinfo = parseTerm(fp);
		return new TFValueSource(tinfo.mField, tinfo.mValue, 
				tinfo.mIndexedField, tinfo.mIndexedBytes);
	}
	
	static void splitSources(int dim, List<ValueSource> sources, 
			List<ValueSource> dest1, List<ValueSource> dest2) {
		//Get dim value sources for the first vector
	    for (int i = 0; i < dim; i++) {
	    	dest1.add(sources.get(i));
	    }
	    
	    //Get dim value sources for the second vector
	    for (int i = dim; i < sources.size(); i++) {
	    	dest2.add(sources.get(i));
	    }
	}

	static MVResult getMultiValueSources(List<ValueSource> sources) 
			throws ErrorException {
	    MVResult mvr = new MVResult();
	    if (sources.size() % 2 != 0) {
	    	throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
	    			"Illegal number of sources. There must be an even number of sources");
	    }
	    
	    if (sources.size() == 2) {
	    	//check to see if these are MultiValueSource
	    	boolean s1MV = sources.get(0) instanceof MultiValueSource;
	    	boolean s2MV = sources.get(1) instanceof MultiValueSource;
	    	
	    	if (s1MV && s2MV) {
	    		mvr.mSource1 = (MultiValueSource) sources.get(0);
	    		mvr.mSource2 = (MultiValueSource) sources.get(1);
	    		
	    	} else if (s1MV || s2MV) {
	    		//if one is a MultiValueSource, than the other one needs to be too.
	    		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
	    				"Illegal number of sources. There must be an even number of sources");
	    		
	    	} else {
	    		mvr.mSource1 = new VectorValueSource(Collections.singletonList(sources.get(0)));
	    		mvr.mSource2 = new VectorValueSource(Collections.singletonList(sources.get(1)));
	    	}
	    	
	    } else {
	    	int dim = sources.size() / 2;
	    	
	    	List<ValueSource> sources1 = new ArrayList<ValueSource>(dim);
	    	List<ValueSource> sources2 = new ArrayList<ValueSource>(dim);
	    	
	    	//Get dim value sources for the first vector
	    	splitSources(dim, sources, sources1, sources2);
	    	
	    	mvr.mSource1 = new VectorValueSource(sources1);
	    	mvr.mSource2 = new VectorValueSource(sources2);
	    }

	    return mvr;
	}

	static class MVResult {
		MultiValueSource mSource1;
		MultiValueSource mSource2;
	}

	static class TInfo {
		String mField;
		String mValue;
		String mIndexedField;
		BytesRef mIndexedBytes;
	}
	
}
