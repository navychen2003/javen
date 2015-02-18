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
import org.javenstudio.raptor.io.MapFile;
import org.javenstudio.raptor.io.SequenceFile;
import org.javenstudio.raptor.io.Writable;
import org.javenstudio.raptor.io.WritableComparable;
import org.javenstudio.raptor.io.compress.CompressionCodec;
import org.javenstudio.raptor.util.Progressable;


/** An {@link OutputFormat} that writes {@link MapFile}s. */
@SuppressWarnings("rawtypes")
public class MapFileFormat<K extends WritableComparable, V extends Writable> {

	private final Configuration mConf;
	private final FileSystem mFs;
	private final Class<? extends WritableComparable> mKeyClass;
	private final Class<? extends Writable> mValClass;
	
	public MapFileFormat(Configuration conf, FileSystem fs, 
			Class<? extends WritableComparable> keyClass, 
			Class<? extends Writable> valClass) { 
		mConf = conf;
		mFs = fs;
		mKeyClass = keyClass;
		mValClass = valClass;
	}
	
	public RecordWriter<K, V> getRecordWriter(Path path, 
			Progressable progress) throws IOException {
		// get the path of the temporary output file 
		//Path file = FileOutputFormat.getTaskOutputPath(job, name);
    
		//FileSystem fs = file.getFileSystem(job);
		CompressionCodec codec = null;
		SequenceFile.CompressionType compressionType = SequenceFile.CompressionType.NONE;
		//if (getCompressOutput(job)) {
		//	// find the kind of compression to do
		//	compressionType = SequenceFileOutputFormat.getOutputCompressionType(job);

		//	// find the right codec
		//	Class<? extends CompressionCodec> codecClass = getOutputCompressorClass(job,
		//			DefaultCodec.class);
		//	codec = ReflectionUtils.newInstance(codecClass, job);
		//}
    
		// ignore the progress parameter, since MapFile is local
		final MapFile.Writer out =
				new MapFile.Writer(mConf, mFs, path.toString(),
						mKeyClass, //job.getOutputKeyClass().asSubclass(WritableComparable.class),
						mValClass, //job.getOutputValueClass().asSubclass(Writable.class),
						compressionType, codec,
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

	private RecordReader<K, V> getRecordReader(FileSplit split) throws IOException {
		return new SequenceFormat.SequenceRecordReader<K, V>(mConf, mFs, split);
	}
	
	public RecordReader<K, V> getRecordReader(Path path) throws IOException {
		Path file = new Path(path, MapFile.DATA_FILE_NAME);
		FileStatus status = mFs.getFileStatus(file);
		FileSplit split = new FileSplit(file, 0, status.getLen());
		return getRecordReader(split);
	}
	
	public MapFile.Reader getMapReader(Path path) throws IOException { 
		FileStatus status = mFs.getFileStatus(path);
		if (!status.isDir()) 
			throw new IOException("MapFile path: " + path + " is'not directory");
		return new MapFile.Reader(mFs, path.toString(), mConf);
	}
	
	/** Open the output generated by this format. */
	public static MapFile.Reader[] getReaders(Configuration conf, 
			Path dir) throws IOException {
		final FileSystem fs = dir.getFileSystem(conf);
		return getReaders(conf, fs, dir); 
	}

	public static MapFile.Reader[] getReaders(Configuration conf, 
			final FileSystem fs, Path dir) throws IOException {
		Path[] names = FileUtil.stat2Paths(fs.listStatus(dir, new PathFilter() {
				@SuppressWarnings("deprecation")
				@Override
				public boolean accept(Path f) {
					try {
						if (fs.isDirectory(f)) return true;
					} catch (IOException ioe) {};
					return false;
				}
			}));

		// sort names, so that hash partitioning works
		Arrays.sort(names);
    
		MapFile.Reader[] parts = new MapFile.Reader[names.length];
		for (int i = 0; i < names.length; i++) {
			parts[i] = new MapFile.Reader(fs, names[i].toString(), conf);
		}
		
		return parts;
	}

}
