package org.javenstudio.falcon.search.schema.type;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.javenstudio.common.indexdb.document.Fieldable;
import org.javenstudio.common.indexdb.document.StoredField;
import org.javenstudio.common.indexdb.search.SortField;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.Base64Utils;
import org.javenstudio.falcon.util.TextWriter;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.falcon.search.schema.SchemaFieldType;

public class BinaryFieldType extends SchemaFieldType  {

	@Override
	public void write(TextWriter writer, String name, Fieldable f) throws ErrorException {
		try {
			writer.writeString(name, toBase64String(toObject(f)), false);
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
	}

	@Override
	public SortField getSortField(SchemaField field, boolean top) throws ErrorException {
		throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
				"Cannot sort on a Binary field");
	}

	@Override
	public String toExternal(Fieldable f) {
		return toBase64String(toObject(f));
	}

	@Override
	public ByteBuffer toObject(Fieldable f) {
		BytesRef bytes = f.getBinaryValue();
		return ByteBuffer.wrap(bytes.getBytes(), bytes.getOffset(), bytes.getLength());
	}

	@Override
	public Fieldable createField(SchemaField field, Object val, float boost) {
		if (val == null) return null;
		
		if (!field.isStored()) {
			if (LOG.isWarnEnabled())
				LOG.warn("Ignoring unstored binary field: " + field);
			
			return null;
		}
		
		byte[] buf = null;
		int offset = 0, len = 0;
		
		if (val instanceof byte[]) {
			buf = (byte[]) val;
			len = buf.length;
			
		} else if (val instanceof ByteBuffer && ((ByteBuffer)val).hasArray()) {
			ByteBuffer byteBuf = (ByteBuffer) val;
			buf = byteBuf.array();
			offset = byteBuf.position();
			len = byteBuf.limit() - byteBuf.position();
			
		} else {
			String strVal = val.toString();
			//the string has to be a base64 encoded string
			buf = Base64Utils.base64ToByteArray(strVal);
			
			offset = 0;
			len = buf.length;
		}

		Fieldable f = new StoredField(field.getName(), buf, offset, len);
		f.setBoost(boost);
		
		return f;
	}
	
	private String toBase64String(ByteBuffer buf) {
		return Base64Utils.byteArrayToBase64(buf.array(), buf.position(), 
				buf.limit()-buf.position());
	}
  
}
