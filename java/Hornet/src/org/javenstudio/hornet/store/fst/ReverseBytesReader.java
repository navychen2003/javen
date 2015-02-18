package org.javenstudio.hornet.store.fst;

final class ReverseBytesReader extends BytesReader {

    public ReverseBytesReader(byte[] bytes, int pos) {
    	super(bytes, pos);
    }

    @Override
    public byte readByte() {
    	return mBytes[mPos--];
    }

    @Override
    public void readBytes(byte[] b, int offset, int len) {
    	for (int i=0; i < len; i++) {
    		b[offset+i] = mBytes[mPos--];
    	}
    }

    public void skip(int count) {
    	mPos -= count;
    }

    public void skip(int base, int count) {
    	mPos = base - count;
    }
}
