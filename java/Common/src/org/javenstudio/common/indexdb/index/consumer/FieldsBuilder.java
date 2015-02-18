package org.javenstudio.common.indexdb.index.consumer;

import java.io.IOException;

import org.javenstudio.common.indexdb.IField;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IFieldsBuilder;
import org.javenstudio.common.indexdb.index.DocumentWriter;
import org.javenstudio.common.indexdb.util.ArrayUtil;
import org.javenstudio.common.indexdb.util.JvmUtil;

public abstract class FieldsBuilder implements IFieldsBuilder {
	
	private final DocumentWriter mWriter;
	
	protected int mNumStoredFields = 0;
	protected IField[] mStoredFields = null;
	protected IFieldInfo[] mFieldInfos = null;
	protected int mLastDocID = 0;
	
	protected FieldsBuilder(DocumentWriter writer) { 
		mWriter = writer;
	}
	
	protected final DocumentWriter getDocumentWriter() { 
		return mWriter;
	}
	
	public void startDocument() { reset(); }
	
	public abstract void finishDocument() throws IOException;
	public abstract void flush() throws IOException;
	
	public void abort() {}
	
	public void addField(IFieldInfo fieldInfo, IField field) throws IOException { 
		if (mNumStoredFields == mStoredFields.length) {
			int newSize = ArrayUtil.oversize(mNumStoredFields + 1, JvmUtil.NUM_BYTES_OBJECT_REF);
	        IField[] newArray = new IField[newSize];
	        System.arraycopy(mStoredFields, 0, newArray, 0, mNumStoredFields);
	        mStoredFields = newArray;
	        
	        IFieldInfo[] newInfoArray = new IFieldInfo[newSize];
	        System.arraycopy(mFieldInfos, 0, newInfoArray, 0, mNumStoredFields);
	        mFieldInfos = newInfoArray;
		}

		mStoredFields[mNumStoredFields] = field;
		mFieldInfos[mNumStoredFields] = fieldInfo;
		mNumStoredFields ++;
	}
	
	public void reset() {
		mNumStoredFields = 0;
		mStoredFields = new IField[1];
		mFieldInfos = new IFieldInfo[1];
	}
	
}
