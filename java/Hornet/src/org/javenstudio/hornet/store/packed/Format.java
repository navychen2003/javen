package org.javenstudio.hornet.store.packed;

/**
 * A format to write packed ints.
 */
public enum Format {
    /**
     * Compact format, all bits are written contiguously.
     */
    PACKED(0) {
    	@Override
    	public int nblocks(int bitsPerValue, int values) {
    		return (int) Math.ceil((double) values * bitsPerValue / 64);
    	}
    },

    /**
     * A format that may insert padding bits to improve encoding and decoding
     * speed. Since this format doesn't support all possible bits per value, you
     * should never use it directly, but rather use
     * {@link PackedInts#fastestFormatAndBits(int, int, float)} to find the
     * format that best suits your needs.
     */
    PACKED_SINGLE_BLOCK(1) {
    	@Override
    	public int nblocks(int bitsPerValue, int values) {
    		final int valuesPerBlock = 64 / bitsPerValue;
    		return (int) Math.ceil((double) values / valuesPerBlock);
    	}

    	@Override
    	public boolean isSupported(int bitsPerValue) {
    		return Packed64SingleBlock.isSupported(bitsPerValue);
    	}

    	@Override
    	public float overheadPerValue(int bitsPerValue) {
    		assert isSupported(bitsPerValue);
    		final int valuesPerBlock = 64 / bitsPerValue;
    		final int overhead = 64 % bitsPerValue;
    		return (float) overhead / valuesPerBlock;
    	}
    };

    /**
     * Get a format according to its ID.
     */
    public static Format byId(int id) {
    	for (Format format : Format.values()) {
    		if (format.getId() == id) 
    			return format;
    	}
    	throw new IllegalArgumentException("Unknown format id: " + id);
    }

    private int mId;
    
    private Format(int id) {
    	mId = id;
    }

    /**
     * Returns the ID of the format.
     */
    public int getId() {
      return mId;
    }

    /**
     * Computes how many blocks are needed to store <code>values</code> values
     * of size <code>bitsPerValue</code>.
     */
    public abstract int nblocks(int bitsPerValue, int values);

    /**
     * Tests whether the provided number of bits per value is supported by the
     * format.
     */
    public boolean isSupported(int bitsPerValue) {
    	return bitsPerValue >= 1 && bitsPerValue <= 64;
    }

    /**
     * Returns the overhead per value, in bits.
     */
    public float overheadPerValue(int bitsPerValue) {
    	assert isSupported(bitsPerValue);
    	return 0f;
    }

    /**
     * Returns the overhead ratio (<code>overhead per value / bits per value</code>).
     */
    public final float overheadRatio(int bitsPerValue) {
    	assert isSupported(bitsPerValue);
    	return overheadPerValue(bitsPerValue) / bitsPerValue;
    }
    
}
