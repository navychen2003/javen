package org.javenstudio.falcon.datum.util;

import java.io.IOException;
import java.util.Arrays;

import org.javenstudio.falcon.datum.data.FileSplit;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.fs.FileStatus;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.FileUtil;
import org.javenstudio.raptor.fs.Path;
import org.javenstudio.raptor.fs.PathFilter;
import org.javenstudio.raptor.io.SequenceFile;
import org.javenstudio.raptor.io.Writable;
import org.javenstudio.raptor.io.WritableComparable;
import org.javenstudio.raptor.io.compress.CompressionCodec;
import org.javenstudio.raptor.util.Progressable;
import org.javenstudio.raptor.util.ReflectionUtils;

@SuppressWarnings("rawtypes")
public class SequenceFormat<K extends WritableComparable, V extends Writable> {

	private final Configuration mConf;
	private final FileSystem mFs;
	private final Class<? extends WritableComparable> mKeyClass;
	private final Class<? extends Writable> mValClass;
	
	public SequenceFormat(Configuration conf, FileSystem fs, 
			Class<? extends WritableComparable> keyClass, 
			Class<? extends Writable> valClass) { 
		mConf = conf;
		mFs = fs;
		mKeyClass = keyClass;
		mValClass = valClass;
	}
	
	public RecordWriter<K, V> getRecordWriter(Path file, 
			Progressable progress) throws IOException {
		// get the path of the temporary output file 
		//Path file = FileOutputFormat.getTaskOutputPath(job, name);

		//FileSystem fs = file.getFileSystem(job);
		CompressionCodec codec = null;
		SequenceFile.CompressionType compressionType = SequenceFile.CompressionType.NONE;
		//if (getCompressOutput(job)) {
		//	// find the kind of compression to do
		//	compressionType = getOutputCompressionType(job);

		//	// find the right codec
		//	Class<? extends CompressionCodec> codecClass = getOutputCompressorClass(job,
		//			DefaultCodec.class);
		//	codec = ReflectionUtils.newInstance(codecClass, job);
		//}
		
		final SequenceFile.Writer out = 
				SequenceFile.createWriter(mFs, mConf, file,
						mKeyClass, //job.getOutputKeyClass(),
						mValClass, //job.getOutputValueClass(),
						compressionType,
						codec,
						progress);

		return new RecordWriter<K, V>() {
				@Override
				public void write(K key, V value) throws IOException {
					out.append(key, value);
				}
				@Override
				public void close() throws IOException { 
					out.close();
				}
			};
	}
	
	public RecordReader<K, V> getRecordReader(FileSplit split) throws IOException {
		return new SequenceRecordReader<K, V>(mConf, mFs, split);
	}
	
	public RecordReader<K, V> getRecordReader(Path file) throws IOException {
		FileStatus status = mFs.getFileStatus(file);
		FileSplit split = new FileSplit(file, 0, status.getLen());
		return getRecordReader(split);
	}
	
	/** Open the output generated by this format. */
	public static SequenceFile.Reader[] getReaders(Configuration conf, 
			Path dir) throws IOException {
		final FileSystem fs = dir.getFileSystem(conf);
		return getReaders(conf, fs, dir); 
	}

	public static SequenceFile.Reader[] getReaders(Configuration conf, 
			final FileSystem fs, Path dir) throws IOException {
		Path[] names = FileUtil.stat2Paths(fs.listStatus(dir, new PathFilter() {
				@Override
				public boolean accept(Path f) {
					try {
						if (fs.isFile(f)) return true; 
					} catch (IOException ioe) {};
					return false;
				}
			}));

		// sort names, so that hash partitioning works
		Arrays.sort(names);
    
		SequenceFile.Reader[] parts = new SequenceFile.Reader[names.length];
		for (int i = 0; i < names.length; i++) {
			parts[i] = new SequenceFile.Reader(fs, names[i], conf);
		}
		
		return parts;
	}
	
	/** An {@link RecordReader} for {@link SequenceFile}s. */
	static class SequenceRecordReader<K, V> implements RecordReader<K, V> {
	  
	  private SequenceFile.Reader in;
	  private long start;
	  private long end;
	  private boolean more = true;
	  protected Configuration conf;

	  public SequenceRecordReader(Configuration conf, FileSystem fs, 
			  FileSplit split) throws IOException {
	    Path path = split.getPath();
	    //FileSystem fs = path.getFileSystem(conf);
	    this.in = new SequenceFile.Reader(fs, path, conf);
	    this.end = split.getStart() + split.getLength();
	    this.conf = conf;

	    if (split.getStart() > in.getPosition())
	      in.sync(split.getStart());                  // sync to start

	    this.start = in.getPosition();
	    more = start < end;
	  }

	  /** The class of key that must be passed to {@link
	   * #next(Object, Object)}.. */
	  public Class<?> getKeyClass() { return in.getKeyClass(); }

	  /** The class of value that must be passed to {@link
	   * #next(Object, Object)}.. */
	  public Class<?> getValueClass() { return in.getValueClass(); }
	  
	  @SuppressWarnings("unchecked")
	  public K createKey() {
	    return (K) ReflectionUtils.newInstance(getKeyClass(), conf);
	  }
	  
	  @SuppressWarnings("unchecked")
	  public V createValue() {
	    return (V) ReflectionUtils.newInstance(getValueClass(), conf);
	  }
	  
	  @Override
	  public synchronized boolean next(K key, V value) throws IOException {
	    if (!more) return false;
	    long pos = in.getPosition();
	    boolean remaining = (in.next(key) != null);
	    if (remaining) {
	      getCurrentValue(value);
	    }
	    if (pos >= end && in.syncSeen()) {
	      more = false;
	    } else {
	      more = remaining;
	    }
	    return more;
	  }
	  
	  protected synchronized boolean next(K key) throws IOException {
	    if (!more) return false;
	    long pos = in.getPosition();
	    boolean remaining = (in.next(key) != null);
	    if (pos >= end && in.syncSeen()) {
	      more = false;
	    } else {
	      more = remaining;
	    }
	    return more;
	  }
	  
	  protected synchronized void getCurrentValue(V value) throws IOException {
	    in.getCurrentValue(value);
	  }
	  
	  /**
	   * Return the progress within the input split
	   * @return 0.0 to 1.0 of the input byte range
	   */
	  @Override
	  public float getProgress() throws IOException {
	    if (end == start) {
	      return 0.0f;
	    } else {
	      return Math.min(1.0f, (in.getPosition() - start) / (float)(end - start));
	    }
	  }
	  
	  @Override
	  public synchronized long getPos() throws IOException {
	    return in.getPosition();
	  }
	  
	  protected synchronized void seek(long pos) throws IOException {
	    in.seek(pos);
	  }
	  
	  @Override
	  public synchronized void close() throws IOException { 
		in.close(); 
	  }
	  
	}
	
}