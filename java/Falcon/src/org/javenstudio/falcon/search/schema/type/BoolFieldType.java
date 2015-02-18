package org.javenstudio.falcon.search.schema.type;

import java.io.IOException;
import java.io.Reader;

import org.javenstudio.common.indexdb.IAnalyzer;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.analysis.CharToken;
import org.javenstudio.common.indexdb.analysis.TokenComponents;
import org.javenstudio.common.indexdb.analysis.Tokenizer;
import org.javenstudio.common.indexdb.document.Fieldable;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.CharTerm;
import org.javenstudio.common.indexdb.util.CharsRef;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.TextWriter;
import org.javenstudio.falcon.search.analysis.BaseAnalyzer;
import org.javenstudio.falcon.search.query.QueryBuilder;
import org.javenstudio.falcon.search.schema.PrimitiveFieldType;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.source.BoolFieldSource;

public class BoolFieldType extends PrimitiveFieldType {
	
	// avoid instantiating every time...
	public final static char[] TRUE_TOKEN = {'T'};
	public final static char[] FALSE_TOKEN = {'F'};
	
	private static final CharsRef TRUE = new CharsRef("true");
	private static final CharsRef FALSE = new CharsRef("false");

	////////////////////////////////////////////////////////////////////////
	// TODO: look into creating my own queryParser that can more efficiently
	// handle single valued non-text fields (int,bool,etc) if needed.

	private final static IAnalyzer sBoolAnalyzer = new BaseAnalyzer() {
	  
		@Override
		public TokenComponents createComponents(String fieldName, Reader reader) {
			Tokenizer tokenizer = new Tokenizer(reader) {
				private final CharTerm mTerm = new CharTerm();
				private final CharToken mToken = new CharToken(mTerm);
				private boolean mDone = false;

				@Override
				public void reset() throws IOException {
					mDone = false;
				}

				@Override
				public IToken nextToken() throws IOException {
					mToken.clear();
					if (mDone) return null;
					mDone = true;
					int ch = getInput().read();
					if (ch == -1) return null;
					mTerm.copyBuffer(((ch=='t' || ch=='T' || ch=='1') ? TRUE_TOKEN : FALSE_TOKEN)
							, 0, 1);
					return mToken;
				}
			};

			return new TokenComponents(tokenizer);
		}
	};

	@Override
	public ISortField getSortField(SchemaField field, boolean reverse) 
			throws ErrorException {
		field.checkSortability();
		return getStringSort(field,reverse);
	}
	
	@Override
	public ValueSource getValueSource(SchemaField field, QueryBuilder qparser) 
			throws ErrorException {
		field.checkFieldCacheSource(qparser);
		return new BoolFieldSource(field.getName());
	}
	
	@Override
	public IAnalyzer getAnalyzer() {
		return sBoolAnalyzer;
	}

	@Override
	public IAnalyzer getQueryAnalyzer() {
		return sBoolAnalyzer;
	}

	@Override
	public String toInternal(String val) {
		char ch = (val!=null && val.length()>0) ? val.charAt(0) : 0;
		return (ch == '1' || ch == 't' || ch == 'T') ? "T" : "F";
	}

	@Override
	public String toExternal(Fieldable f) {
		return indexedToReadable(f.getStringValue());
	}

	@Override
	public Boolean toObject(Fieldable f) {
		return Boolean.valueOf( toExternal(f) );
	}

	@Override
	public Object toObject(SchemaField sf, BytesRef term) {
		return term.getByteAt(term.getOffset()) == 'T';
	}

	@Override
	public String indexedToReadable(String indexedForm) {
		char ch = indexedForm.charAt(0);
		return ch == 'T' ? "true" : "false";
	}
  
	@Override
	public CharsRef indexedToReadable(BytesRef input, CharsRef charsRef) {
		if (input.getLength() > 0 && input.getByteAt(input.getOffset()) == 'T') 
			charsRef.copyChars(TRUE);
		else 
			charsRef.copyChars(FALSE);
		
		return charsRef;
	}

	@Override
	public void write(TextWriter writer, String name, Fieldable f) throws ErrorException {
		try {
			writer.writeBool(name, f.getStringValue().charAt(0) == 'T');
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
	}
	
}
