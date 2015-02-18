package org.javenstudio.falcon.datum.data;

import java.io.IOException;
import java.io.DataInput;
import java.io.DataOutput;

import org.javenstudio.raptor.fs.Path;
import org.javenstudio.raptor.io.UTF8;
import org.javenstudio.raptor.io.Writable;

/** 
 * A section of an input file.  Returned by {@link
 * InputFormat#getSplits(JobConf, int)} and passed to
 * {@link InputFormat#getRecordReader(InputSplit,JobConf,Reporter)}. 
 */
@SuppressWarnings("deprecation")
public class FileSplit implements Writable {
	
	private Path mFile;
	private long mStart;
	private long mLength;
  
	private FileSplit() {}

	/** 
	 * Constructs a split.
	 * @param file the file name
	 * @param start the position of the first byte in the file to process
	 * @param length the number of bytes in the file to process
	 */
	public FileSplit(Path file, long start, long length) {
		if (file == null) throw new NullPointerException();
		this.mFile = file;
		this.mStart = start;
		this.mLength = length;
	}

	/** The file containing this split's data. */
	public Path getPath() { return mFile; }
  
	/** The position of the first byte in the file to process. */
	public long getStart() { return mStart; }
  
	/** The number of bytes in the file to process. */
	public long getLength() { return mLength; }

	@Override
	public String toString() { 
		return mFile + ":" + mStart + "+" + mLength; 
	}

	////////////////////////////////////////////
	// Writable methods
	////////////////////////////////////////////

	@Override
	public void write(DataOutput out) throws IOException {
		UTF8.writeString(out, mFile.toString());
		out.writeLong(mStart);
		out.writeLong(mLength);
	}
	
	@Override
	public void readFields(DataInput in) throws IOException {
		mFile = new Path(UTF8.readString(in));
		mStart = in.readLong();
		mLength = in.readLong();
	}

	public static FileSplit read(DataInput in) throws IOException { 
		FileSplit data = new FileSplit();
		data.readFields(in);
		return data;
	}
  
}
