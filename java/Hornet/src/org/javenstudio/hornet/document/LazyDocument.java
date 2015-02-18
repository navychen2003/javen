package org.javenstudio.hornet.document;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.indexdb.IAnalyzer;
import org.javenstudio.common.indexdb.IDocument;
import org.javenstudio.common.indexdb.IField;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.common.indexdb.document.Fieldable;
import org.javenstudio.common.indexdb.util.BytesRef;

/** 
 * Defers actually loading a field's value until you ask
 *  for it.  You must not use the returned Field instances
 *  after the provided reader has been closed. 
 */
public class LazyDocument {
	
	private Map<Integer,Integer> mFields = new HashMap<Integer,Integer>();
	
	private IIndexReader mReader;
	private final int mDocID;

	// null until first field is loaded
	private IDocument mDoc;

	public LazyDocument(IIndexReader reader, int docID) {
		mReader = reader;
		mDocID = docID;
	}

	public Fieldable getField(IFieldInfo fieldInfo) {  
		Integer num = mFields.get(fieldInfo.getNumber());
		if (num == null) 
			num = 0;
		else 
			num++;
		
		mFields.put(fieldInfo.getNumber(), num);

		return new LazyField(fieldInfo.getName(), num);
	}

	private synchronized IDocument getDocument() {
		if (mDoc == null) {
			try {
				mDoc = mReader.getDocument(mDocID);
			} catch (IOException ioe) {
				throw new IllegalStateException("unable to load document", ioe);
			}
			mReader = null;
		}
		return mDoc;
	}

	private class LazyField implements Fieldable {
		private String mName;
		private int mNum;
    
		public LazyField(String name, int num) {
			mName = name;
			mNum = num;
		}

		@Override
		public String getName() {
			return mName;
		}

		@Override
		public float getBoost() {
			return 1.0f;
		}

		@Override
		public BytesRef getBinaryValue() {
			if (mNum == 0) 
				return getDocument().getField(mName).getBinaryValue();
			else 
				return getDocument().getFields(mName)[mNum].getBinaryValue();
		}

		@Override
		public String getStringValue() {
			if (mNum == 0) 
				return getDocument().getField(mName).getStringValue();
			else 
				return getDocument().getFields(mName)[mNum].getStringValue();
		}

		@Override
		public Reader getReaderValue() {
			if (mNum == 0) 
				return getDocument().getField(mName).getReaderValue();
			else 
				return getDocument().getFields(mName)[mNum].getReaderValue();
		}

		@Override
		public Number getNumericValue() {
			if (mNum == 0) 
				return getDocument().getField(mName).getNumericValue();
			else 
				return getDocument().getFields(mName)[mNum].getNumericValue();
		}

		@Override
		public IField.Type getFieldType() {
			if (mNum == 0) 
				return getDocument().getField(mName).getFieldType();
			else 
				return getDocument().getFields(mName)[mNum].getFieldType();
		}

		@Override
		public ITokenStream tokenStream(IAnalyzer analyzer) throws IOException {
			if (mNum == 0) 
				return getDocument().getField(mName).tokenStream(analyzer);
			else 
				return getDocument().getFields(mName)[mNum].tokenStream(analyzer);
		}

		@Override
		public void setBoost(float boost) {
			final Fieldable field;
			if (mNum == 0) 
				field = (Fieldable) getDocument().getField(mName);
			else
				field = (Fieldable) getDocument().getFields(mName)[mNum];
			
			field.setBoost(boost);
		}
	}
	
}
