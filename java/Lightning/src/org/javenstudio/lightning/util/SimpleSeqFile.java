package org.javenstudio.lightning.util;

import java.io.InputStream;
import java.util.ArrayList;

import org.javenstudio.falcon.datum.util.RecordReader;
import org.javenstudio.falcon.datum.util.RecordWriter;
import org.javenstudio.falcon.datum.util.SequenceFormat;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.fs.FileStatus;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;
import org.javenstudio.raptor.io.BytesWritable;
import org.javenstudio.raptor.io.Text;

public class SimpleSeqFile extends SimpleShell {

	public static void main(String[] args) throws Exception { 
		Configuration conf = loadConf().getConf();
		FileSystem fs = FileSystem.getLocal(conf);
		
		SequenceFormat<Text,BytesWritable> format = new SequenceFormat<Text,BytesWritable>(
				conf, fs, Text.class, BytesWritable.class);
		
		Path file = new Path(args[0]);
		boolean cmdRead = false;
		ArrayList<Path> dirs = new ArrayList<Path>();
		
		for (int i=1; i < args.length; i++) { 
			if (args[i].equals("-r")) { 
				cmdRead = true; break;
			}
			dirs.add(new Path(args[i]));
		}
		
		if (cmdRead) {
			readData(format, file);
			return;
		}
		
		writeData(fs, format, file, dirs.toArray(new Path[dirs.size()]));
	}
	
	private static void writeData(FileSystem fs, 
			SequenceFormat<Text,BytesWritable> format, Path file, Path[] dirs) 
					throws Exception { 
		RecordWriter<Text,BytesWritable> writer = format.getRecordWriter(file, null);
		
		for (int i=0; dirs != null && i < dirs.length; i++) { 
			Path dir = dirs[i];
			FileStatus status = fs.getFileStatus(dir);
			writeFile(fs, writer, status);
		}
		
		writer.close();
	}
	
	private static void writeFile(FileSystem fs, 
			RecordWriter<Text,BytesWritable> writer, FileStatus status) 
					throws Exception { 
		if (status == null) return;
		
		if (status.isDir()) { 
			FileStatus[] files = fs.listStatus(status.getPath());
			for (int i=0; files != null && i < files.length; i++) { 
				writeFile(fs, writer, files[i]);
			}
		} else { 
			InputStream stream = fs.open(status.getPath());
			try { 
				int size = stream.available();
				if (size > 0) { 
					String key = status.getPath().toString();
					System.out.println(">>>: key=" + key + " valueLen=" + size);
					
					byte[] buffer = new byte[size];
					stream.read(buffer);
					
					writer.write(new Text(key), new BytesWritable(buffer));
				}
			} catch (Throwable e) { 
				e.printStackTrace();
			} finally { 
				if (stream != null) 
					stream.close();
			}
		}
	}
	
	private static void readData(SequenceFormat<Text,BytesWritable> format, 
			Path file) throws Exception { 
		RecordReader<Text,BytesWritable> reader = format.getRecordReader(file);
		
		Text key = new Text();
		BytesWritable value = new BytesWritable();
		
		while (reader.next(key, value)) { 
			System.out.println("<<<: key=" + key + " valueLen=" + value.getLength());
		}
		
		reader.close();
	}
	
}
