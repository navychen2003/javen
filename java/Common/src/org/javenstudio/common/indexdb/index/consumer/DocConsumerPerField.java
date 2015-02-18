package org.javenstudio.common.indexdb.index.consumer;

import java.util.HashMap;

import org.javenstudio.common.indexdb.IField;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.util.ArrayUtil;
import org.javenstudio.common.indexdb.util.JvmUtil;

final class DocConsumerPerField {

	public static class List extends HashMap<String, DocConsumerPerField> { 
		private static final long serialVersionUID = 1L;
		public List() { super(); }
	}
	
	private final IFieldInfo mFieldInfo;
	private final DocFieldConsumerPerField mConsumer;
	DocConsumerPerField mNext = null;
	
	private IField[] mFields = new IField[1];
	private int mFieldCount = 0;
	private int mLastGen = -1;
	
	public DocConsumerPerField(DocConsumer parent, IFieldInfo fieldInfo) { 
		mFieldInfo = fieldInfo;
		mConsumer = parent.getConsumer().addField(fieldInfo);
	}
	
	final IFieldInfo getFieldInfo() { return mFieldInfo; }
	final DocFieldConsumerPerField getConsumer() { return mConsumer; }
	
	final int getLastGen() { return mLastGen; }
	final void setLastGen(int lastGen) { mLastGen = lastGen; }
	
	final IField[] getFields() { return mFields; }
	final int getFieldCount() { return mFieldCount; }
	
	final void clearFields() { mFieldCount = 0; }
	
	final void addField(IField field) {
		if (mFieldCount == mFields.length) {
			int newSize = ArrayUtil.oversize(mFieldCount + 1, JvmUtil.NUM_BYTES_OBJECT_REF);
			IField[] newArray = new IField[newSize];
			System.arraycopy(mFields, 0, newArray, 0, mFieldCount);
			mFields = newArray;
		}

		mFields[mFieldCount++] = field;
	}
	
	final void abort() { 
		mConsumer.abort();
	}
	
}
