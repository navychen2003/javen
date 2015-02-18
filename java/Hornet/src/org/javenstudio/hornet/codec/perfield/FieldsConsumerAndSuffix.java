package org.javenstudio.hornet.codec.perfield;

import java.io.Closeable;
import java.io.IOException;

import org.javenstudio.hornet.codec.FieldsConsumer;

final class FieldsConsumerAndSuffix implements Closeable {
	
	protected FieldsConsumer mConsumer;
	protected int mSuffix;

	@Override
	public void close() throws IOException {
		mConsumer.close();
	}
	
}
