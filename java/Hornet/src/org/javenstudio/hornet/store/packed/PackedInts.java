package org.javenstudio.hornet.store.packed;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDataInput;
import org.javenstudio.common.indexdb.IDataOutput;
import org.javenstudio.common.indexdb.IIndexInput;
import org.javenstudio.common.indexdb.IIntsMutable;
import org.javenstudio.common.indexdb.IIntsReader;
import org.javenstudio.hornet.codec.CodecUtil;

/**
 * Simplistic compression for array of unsigned long values.
 * Each value is >= 0 and <= a specified maximum value.  The
 * values are stored as packed ints, with each value
 * consuming a fixed number of bits.
 *
 */
public class PackedInts {

	/**
	 * Default amount of memory to use for bulk operations.
	 */
	public static final int DEFAULT_BUFFER_SIZE = 1024; // 1K

	public final static String CODEC_NAME = "PackedInts";
	public final static int VERSION_START = 0;
	public final static int VERSION_CURRENT = VERSION_START;

	/**
	 * Try to find the {@link Format} and number of bits per value that would
	 * restore from disk the fastest reader whose overhead is less than
	 * <code>acceptableOverheadRatio</code>.
	 * </p><p>
	 * The <code>acceptableOverheadRatio</code> parameter makes sense for
	 * random-access {@link Reader}s. In case you only plan to perform
	 * sequential access on this stream later on, you should probably use
	 * {@link PackedInts#COMPACT}.
	 * </p><p>
	 * If you don't know how many values you are going to write, use
	 * <code>valueCount = -1</code>.
	 */
	public static FormatAndBits fastestFormatAndBits(int valueCount, 
			int bitsPerValue, float acceptableOverheadRatio) {
		if (valueCount == -1) 
			valueCount = Integer.MAX_VALUE;

		acceptableOverheadRatio = Math.max(IIntsReader.COMPACT, acceptableOverheadRatio);
		acceptableOverheadRatio = Math.min(IIntsReader.FASTEST, acceptableOverheadRatio);
		
		float acceptableOverheadPerValue = acceptableOverheadRatio * bitsPerValue; // in bits
		int maxBitsPerValue = bitsPerValue + (int) acceptableOverheadPerValue;

		int actualBitsPerValue = -1;
		Format format = Format.PACKED;

		if (bitsPerValue <= 8 && maxBitsPerValue >= 8) {
			actualBitsPerValue = 8;
		} else if (bitsPerValue <= 16 && maxBitsPerValue >= 16) {
			actualBitsPerValue = 16;
		} else if (bitsPerValue <= 32 && maxBitsPerValue >= 32) {
			actualBitsPerValue = 32;
		} else if (bitsPerValue <= 64 && maxBitsPerValue >= 64) {
			actualBitsPerValue = 64;
		} else if (valueCount <= Packed8ThreeBlocks.MAX_SIZE && 
				bitsPerValue <= 24 && maxBitsPerValue >= 24) {
			actualBitsPerValue = 24;
		} else if (valueCount <= Packed16ThreeBlocks.MAX_SIZE && 
				bitsPerValue <= 48 && maxBitsPerValue >= 48) {
			actualBitsPerValue = 48;
			
		} else {
			for (int bpv = bitsPerValue; bpv <= maxBitsPerValue; ++bpv) {
				if (Format.PACKED_SINGLE_BLOCK.isSupported(bpv)) {
					float overhead = Format.PACKED_SINGLE_BLOCK.overheadPerValue(bpv);
					float acceptableOverhead = acceptableOverheadPerValue + bitsPerValue - bpv;
					
					if (overhead <= acceptableOverhead) {
						actualBitsPerValue = bpv;
						format = Format.PACKED_SINGLE_BLOCK;
						break;
					}
				}
			}
			if (actualBitsPerValue < 0) 
				actualBitsPerValue = bitsPerValue;
		}

		return new FormatAndBits(format, actualBitsPerValue);
	}

	/**
	 * Expert: Restore a {@link Reader} from a stream without reading metadata at
	 * the beginning of the stream. This method is useful to restore data from
	 * streams which have been created using
	 * {@link PackedInts#getWriterNoHeader(DataOutput, Format, int, int, int)}.
	 *
	 * @param in           the stream to read data from, positioned at the beginning of the packed values
   	 * @param format       the format used to serialize
   	 * @param version      the version used to serialize the data
   	 * @param valueCount   how many values the stream holds
   	 * @param bitsPerValue the number of bits per value
   	 * @return             a Reader
   	 * @throws IOException
   	 * @see PackedInts#getWriterNoHeader(DataOutput, Format, int, int, int)
   	 */
	public static IIntsReader getReaderNoHeader(IDataInput in, Format format, int version,
			int valueCount, int bitsPerValue) throws IOException {
		switch (format) {
		case PACKED_SINGLE_BLOCK:
			return Packed64SingleBlock.create(in, valueCount, bitsPerValue);
		case PACKED:
			switch (bitsPerValue) {
			case 8:
				return new Direct8(in, valueCount);
			case 16:
				return new Direct16(in, valueCount);
			case 32:
				return new Direct32(in, valueCount);
			case 64:
				return new Direct64(in, valueCount);
			case 24:
				if (valueCount <= Packed8ThreeBlocks.MAX_SIZE) 
					return new Packed8ThreeBlocks(in, valueCount);
				break;
			case 48:
				if (valueCount <= Packed16ThreeBlocks.MAX_SIZE) 
					return new Packed16ThreeBlocks(in, valueCount);
				break;
			}
			return new Packed64(in, valueCount, bitsPerValue);
		default:
			throw new AssertionError("Unknwown Writer format: " + format);
		}
	}

	/**
	 * Restore a {@link Reader} from a stream.
	 *
	 * @param in           the stream to read data from
	 * @return             a Reader
	 * @throws IOException
	 */
	public static IIntsReader getReader(IDataInput in) throws IOException {
		final int version = CodecUtil.checkHeader(in, CODEC_NAME, VERSION_START, VERSION_CURRENT);
		final int bitsPerValue = in.readVInt();
		assert bitsPerValue > 0 && bitsPerValue <= 64: "bitsPerValue=" + bitsPerValue;
		
		final int valueCount = in.readVInt();
		final Format format = Format.byId(in.readVInt());

		return getReaderNoHeader(in, format, version, valueCount, bitsPerValue);
	}

	/**
	 * Expert: Restore a {@link ReaderIterator} from a stream without reading
	 * metadata at the beginning of the stream. This method is useful to restore
	 * data from streams which have been created using
	 * {@link PackedInts#getWriterNoHeader(DataOutput, Format, int, int, int)}.
	 *
	 * @param in           the stream to read data from, positioned at the beginning of the packed values
	 * @param format       the format used to serialize
	 * @param version      the version used to serialize the data
	 * @param valueCount   how many values the stream holds
	 * @param bitsPerValue the number of bits per value
	 * @param mem          how much memory the iterator is allowed to use to read-ahead (likely to speed up iteration)
	 * @return             a ReaderIterator
	 * @throws IOException
	 * @see PackedInts#getWriterNoHeader(DataOutput, Format, int, int, int)
	 */
	public static ReaderIterator getReaderIteratorNoHeader(IDataInput in, Format format, int version,
			int valueCount, int bitsPerValue, int mem) throws IOException {
		return new PackedReaderIteratorImpl(format, valueCount, bitsPerValue, in, mem);
	}

	/**
	 * Retrieve PackedInts as a {@link ReaderIterator}
	 * @param in positioned at the beginning of a stored packed int structure.
	 * @param mem how much memory the iterator is allowed to use to read-ahead (likely to speed up iteration)
	 * @return an iterator to access the values
	 * @throws IOException if the structure could not be retrieved.
	 */
	public static ReaderIterator getReaderIterator(IDataInput in, int mem) throws IOException {
		final int version = CodecUtil.checkHeader(in, CODEC_NAME, VERSION_START, VERSION_CURRENT);
		final int bitsPerValue = in.readVInt();
		assert bitsPerValue > 0 && bitsPerValue <= 64: "bitsPerValue=" + bitsPerValue;
		
		final int valueCount = in.readVInt();
		final Format format = Format.byId(in.readVInt());
		
		return getReaderIteratorNoHeader(in, format, version, valueCount, bitsPerValue, mem);
	}

	/**
	 * Expert: Construct a direct {@link Reader} from a stream without reading
	 * metadata at the beginning of the stream. This method is useful to restore
	 * data from streams which have been created using
	 * {@link PackedInts#getWriterNoHeader(DataOutput, Format, int, int, int)}.
	 * </p><p>
	 * The returned reader will have very little memory overhead, but every call
	 * to {@link Reader#get(int)} is likely to perform a disk seek.
	 *
	 * @param in           the stream to read data from
	 * @param format       the format used to serialize
	 * @param version      the version used to serialize the data
	 * @param valueCount   how many values the stream holds
   	* @param bitsPerValue the number of bits per value
   	* @return a direct Reader
   	* @throws IOException
   	*/
	public static IIntsReader getDirectReaderNoHeader(IIndexInput in, Format format,
			int version, int valueCount, int bitsPerValue) throws IOException {
		switch (format) {
		case PACKED:
			return new DirectPackedReader(bitsPerValue, valueCount, in);
		case PACKED_SINGLE_BLOCK:
			return new DirectPacked64SingleBlockReader(bitsPerValue, valueCount, in);
		default:
			throw new AssertionError("Unknwown format: " + format);
		}
	}

	/**
	 * Construct a direct {@link Reader} from an {@link IndexInput}. This method
	 * is useful to restore data from streams which have been created using
	 * {@link PackedInts#getWriter(DataOutput, int, int, float)}.
	 * </p><p>
	 * The returned reader will have very little memory overhead, but every call
	 * to {@link Reader#get(int)} is likely to perform a disk seek.
	 *
	 * @param in           the stream to read data from
	 * @return a direct Reader
	 * @throws IOException
	 */
	public static IIntsReader getDirectReader(IIndexInput in) throws IOException {
		final int version = CodecUtil.checkHeader(in, CODEC_NAME, VERSION_START, VERSION_CURRENT);
		final int bitsPerValue = in.readVInt();
		assert bitsPerValue > 0 && bitsPerValue <= 64: "bitsPerValue=" + bitsPerValue;
		
		final int valueCount = in.readVInt();
		final Format format = Format.byId(in.readVInt());
		
		return getDirectReaderNoHeader(in, format, version, valueCount, bitsPerValue);
	}
  
	/**
	 * Create a packed integer array with the given amount of values initialized
	 * to 0. the valueCount and the bitsPerValue cannot be changed after creation.
	 * All Mutables known by this factory are kept fully in RAM.
	 * </p><p>
	 * Positive values of <code>acceptableOverheadRatio</code> will trade space
	 * for speed by selecting a faster but potentially less memory-efficient
	 * implementation. An <code>acceptableOverheadRatio</code> of
	 * {@link PackedInts#COMPACT} will make sure that the most memory-efficient
	 * implementation is selected whereas {@link PackedInts#FASTEST} will make sure
	 * that the fastest implementation is selected.
	 *
	 * @param valueCount   the number of elements
	 * @param bitsPerValue the number of bits available for any given value
	 * @param acceptableOverheadRatio an acceptable overhead
	 *        ratio per value
	 * @return a mutable packed integer array
	 */
	public static IIntsMutable getMutable(int valueCount, int bitsPerValue, 
			float acceptableOverheadRatio) {
		assert valueCount >= 0;

		final FormatAndBits formatAndBits = fastestFormatAndBits(
				valueCount, bitsPerValue, acceptableOverheadRatio);
		
		switch (formatAndBits.getFormat()) {
		case PACKED_SINGLE_BLOCK:
			return Packed64SingleBlock.create(valueCount, formatAndBits.getBitsPerValue());
			
		case PACKED:
			switch (formatAndBits.getBitsPerValue()) {
			case 8:
				return new Direct8(valueCount);
			case 16:
				return new Direct16(valueCount);
			case 32:
				return new Direct32(valueCount);
			case 64:
				return new Direct64(valueCount);
			case 24:
				if (valueCount <= Packed8ThreeBlocks.MAX_SIZE) 
					return new Packed8ThreeBlocks(valueCount);
				break;
			case 48:
				if (valueCount <= Packed16ThreeBlocks.MAX_SIZE) 
					return new Packed16ThreeBlocks(valueCount);
				break;
			}
			return new Packed64(valueCount, formatAndBits.getBitsPerValue());
			
		default:
			throw new AssertionError();
		}
	}

	/**
	 * Expert: Create a packed integer array writer for the given output, format,
	 * value count, and number of bits per value.
	 * </p><p>
	 * The resulting stream will be long-aligned. This means that depending on
	 * the format which is used, up to 63 bits will be wasted. An easy way to
	 * make sure that no space is lost is to always use a <code>valueCount</code>
	 * that is a multiple of 64.
	 * </p><p>
	 * This method does not write any metadata to the stream, meaning that it is
	 * your responsibility to store it somewhere else in order to be able to
	 * recover data from the stream later on:
	 * <ul>
	 *   <li><code>format</code> (using {@link Format#getId()}),</li>
	 *   <li><code>valueCount</code>,</li>
	 *   <li><code>bitsPerValue</code>,</li>
	 *   <li>{@link #VERSION_CURRENT}.</li>
	 * </ul>
	 * </p><p>
	 * It is possible to start writing values without knowing how many of them you
	 * are actually going to write. To do this, just pass <code>-1</code> as
	 * <code>valueCount</code>. On the other hand, for any positive value of
	 * <code>valueCount</code>, the returned writer will make sure that you don't
	 * write more values than expected and pad the end of stream with zeros in
	 * case you have written less than <code>valueCount</code> when calling
	 * {@link Writer#finish()}.
	 * </p><p>
	 * The <code>mem</code> parameter lets you control how much memory can be used
	 * to buffer changes in memory before flushing to disk. High values of
	 * <code>mem</code> are likely to improve throughput. On the other hand, if
	 * speed is not that important to you, a value of <code>0</code> will use as
	 * little memory as possible and should already offer reasonable throughput.
	 *
	 * @param out          the data output
	 * @param format       the format to use to serialize the values
	 * @param valueCount   the number of values
	 * @param bitsPerValue the number of bits per value
	 * @param mem          how much memory (in bytes) can be used to speed up serialization
	 * @return             a Writer
	 * @throws IOException
	 * @see PackedInts#getReaderIteratorNoHeader(DataInput, Format, int, int, int, int)
	 * @see PackedInts#getReaderNoHeader(DataInput, Format, int, int, int)
	 */
	public static PackedWriter getWriterNoHeader(IDataOutput out, Format format, 
			int valueCount, int bitsPerValue, int mem) throws IOException {
		return new PackedWriterImpl(format, out, valueCount, bitsPerValue, mem);
	}

	/**
	 * Create a packed integer array writer for the given output, format, value
	 * count, and number of bits per value.
	 * </p><p>
	 * The resulting stream will be long-aligned. This means that depending on
	 * the format which is used under the hoods, up to 63 bits will be wasted.
	 * An easy way to make sure that no space is lost is to always use a
	 * <code>valueCount</code> that is a multiple of 64.
	 * </p><p>
	 * This method writes metadata to the stream, so that the resulting stream is
	 * sufficient to restore a {@link Reader} from it. You don't need to track
	 * <code>valueCount</code> or <code>bitsPerValue</code> by yourself. In case
	 * this is a problem, you should probably look at
	 * {@link #getWriterNoHeader(DataOutput, Format, int, int, int)}.
	 * </p><p>
	 * The <code>acceptableOverheadRatio</code> parameter controls how
	 * readers that will be restored from this stream trade space
	 * for speed by selecting a faster but potentially less memory-efficient
	 * implementation. An <code>acceptableOverheadRatio</code> of
	 * {@link PackedInts#COMPACT} will make sure that the most memory-efficient
	 * implementation is selected whereas {@link PackedInts#FASTEST} will make sure
	 * that the fastest implementation is selected. In case you are only interested
	 * in reading this stream sequentially later on, you should probably use
	 * {@link PackedInts#COMPACT}.
	 *
	 * @param out          the data output
	 * @param valueCount   the number of values
	 * @param bitsPerValue the number of bits per value
	 * @param acceptableOverheadRatio an acceptable overhead ratio per value
	 * @return             a Writer
	 * @throws IOException
	 */
	public static PackedWriter getWriter(IDataOutput out,
			int valueCount, int bitsPerValue, float acceptableOverheadRatio) throws IOException {
		assert valueCount >= 0;

		final FormatAndBits formatAndBits = fastestFormatAndBits(
				valueCount, bitsPerValue, acceptableOverheadRatio);
		
		final PackedWriter writer = getWriterNoHeader(out, formatAndBits.getFormat(), 
				valueCount, formatAndBits.getBitsPerValue(), DEFAULT_BUFFER_SIZE);
		
		writer.writeHeader();
		
		return writer;
	}

	/** 
	 * Returns how many bits are required to hold values up
	 *  to and including maxValue
	 * @param maxValue the maximum value that should be representable.
	 * @return the amount of bits needed to represent values from 0 to maxValue.
	 */
	public static int bitsRequired(long maxValue) {
		if (maxValue < 0) 
			throw new IllegalArgumentException("maxValue must be non-negative (got: " + maxValue + ")");
		
		return Math.max(1, 64 - Long.numberOfLeadingZeros(maxValue));
	}

	/**
	 * Calculates the maximum unsigned long that can be expressed with the given
	 * number of bits.
	 * @param bitsPerValue the number of bits available for any given value.
	 * @return the maximum value for the given bits.
	 */
	public static long maxValue(int bitsPerValue) {
		return bitsPerValue == 64 ? Long.MAX_VALUE : ~(~0L << bitsPerValue);
	}

	/**
	 * Copy <code>src[srcPos:srcPos+len]</code> into
	 * <code>dest[destPos:destPos+len]</code> using at most <code>mem</code>
	 * bytes.
	 */
	public static void copy(IIntsReader src, int srcPos, IIntsMutable dest, 
			int destPos, int len, int mem) {
		assert srcPos + len <= src.size();
		assert destPos + len <= dest.size();
		
		final int capacity = mem >>> 3;
		if (capacity == 0) {
			for (int i = 0; i < len; ++i) {
				dest.set(destPos++, src.get(srcPos++));
			}
			
		} else {
			// use bulk operations
			long[] buf = new long[Math.min(capacity, len)];
			int remaining = 0;
			
			while (len > 0) {
				final int read = src.get(srcPos, buf, remaining, Math.min(len, buf.length - remaining));
				assert read > 0;
				
				srcPos += read;
				len -= read;
				remaining += read;
				
				final int written = dest.set(destPos, buf, 0, remaining);
				assert written > 0;
				
				destPos += written;
				if (written < remaining) {
					System.arraycopy(buf, written, buf, 0, remaining - written);
				}
				
				remaining -= written;
			}
			
			while (remaining > 0) {
				final int written = dest.set(destPos, buf, 0, remaining);
				remaining -= written;
			}
		}
	}

}
