package org.javenstudio.hornet.store.fst;

// TODO: can we use just ByteArrayDataInput...?  need to
// add a .skipBytes to DataInput.. hmm and .setPosition
final class ForwardBytesReader extends BytesReader {

    public ForwardBytesReader(byte[] bytes, int pos) {
    	super(bytes, pos);
    }

    @Override
    public byte readByte() {
    	return mBytes[mPos++];
    }

    @Override
    public void readBytes(byte[] b, int offset, int len) {
    	System.arraycopy(mBytes, mPos, b, offset, len);
    	mPos += len;
    }

    public void skip(int count) {
    	mPos += count;
    }

    public void skip(int base, int count) {
    	mPos = base + count;
    }
    
}
