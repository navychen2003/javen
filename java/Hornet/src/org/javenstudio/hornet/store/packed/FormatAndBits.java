package org.javenstudio.hornet.store.packed;

/**
 * Simple class that holds a format and a number of bits per value.
 */
public class FormatAndBits {
	
    private final Format mFormat;
    private final int mBitsPerValue;
    
    public FormatAndBits(Format format, int bitsPerValue) {
    	mFormat = format;
    	mBitsPerValue = bitsPerValue;
    }
    
    public final Format getFormat() { return mFormat; }
    public final int getBitsPerValue() { return mBitsPerValue; }
    
}
