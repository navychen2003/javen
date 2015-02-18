package org.javenstudio.lightning.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import org.javenstudio.falcon.datum.util.MapFileFormat;
import org.javenstudio.falcon.datum.util.RecordReader;
import org.javenstudio.falcon.datum.util.RecordWriter;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.fs.FileStatus;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;
import org.javenstudio.raptor.io.BytesWritable;
import org.javenstudio.raptor.io.MapFile;
import org.javenstudio.raptor.io.Text;

public class SimpleMapFile extends SimpleShell {

	public static void main(String[] args) throws Exception { 
		Configuration conf = loadConf().getConf();
		FileSystem fs = FileSystem.getLocal(conf);
		
		MapFileFormat<Text,BytesWritable> format = new MapFileFormat<Text,BytesWritable>(
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
		
		ArrayList<FileItem> list = new ArrayList<FileItem>();
		listFiles(fs, dirs.toArray(new Path[dirs.size()]), list);
		
		FileItem[] items = list.toArray(new FileItem[list.size()]);
		Arrays.sort(items, new Comparator<FileItem>() {
				@Override
				public int compare(FileItem o1, FileItem o2) {
					return o1.key.compareTo(o2.key);
				}
			});
		
		writeData(fs, format, file, items);
	}
	
	static class FileItem { 
		public final Text key;
		public final FileStatus status;
		
		public FileItem(Text key, FileStatus status) { 
			this.key = key;
			this.status = status;
		}
	}
	
	private static void listFiles(FileSystem fs, Path[] dirs, 
			Collection<FileItem> items) throws Exception { 
		for (int i=0; dirs != null && i < dirs.length; i++) { 
			Path dir = dirs[i];
			FileStatus status = fs.getFileStatus(dir);
			listFiles(fs, status, items);
		}
	}
	
	private static void listFiles(FileSystem fs, FileStatus status, 
			Collection<FileItem> items) throws Exception { 
		if (status == null) return;
		
		if (status.isDir()) { 
			FileStatus[] files = fs.listStatus(status.getPath());
			for (int i=0; files != null && i < files.length; i++) { 
				listFiles(fs, files[i], items);
			}
		} else { 
			String key = status.getPath().toString();
			FileItem item = new FileItem(new Text(key), status);
			items.add(item);
		}
	}
	
	private static void writeData(FileSystem fs, 
			MapFileFormat<Text,BytesWritable> format, Path file, FileItem[] items) 
					throws Exception { 
		RecordWriter<Text,BytesWritable> writer = format.getRecordWriter(file, null);
		
		for (int i=0; items != null && i < items.length; i++) { 
			FileItem item = items[i];
			writeFile(fs, writer, item);
		}
		
		writer.close();
	}
	
	private static void writeFile(FileSystem fs, 
			RecordWriter<Text,BytesWritable> writer, FileItem item) 
					throws Exception { 
		if (item != null) { 
			InputStream stream = fs.open(item.status.getPath());
			try { 
				int size = stream.available();
				if (size > 0) { 
					Text key = item.key;
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
	
	private static void readData(MapFileFormat<Text,BytesWritable> format, 
			Path file) throws Exception { 
		ArrayList<Text> keys = new ArrayList<Text>();
		if (true) {
			RecordReader<Text,BytesWritable> reader = format.getRecordReader(file);
		
			Text key = new Text();
			BytesWritable value = new BytesWritable();
		
			while (reader.next(key, value)) { 
				keys.add(new Text(key));
				System.out.println("<<<: key=" + key + " valueLen=" + value.getLength());
			}
		
			reader.close();
		}
		if (true) { 
			MapFile.Reader reader = format.getMapReader(file);
			
			BytesWritable value = new BytesWritable();
			for (Text key : keys) { 
				BytesWritable result = (BytesWritable)reader.get(key, value);
				if (result != null) 
					System.out.println("<OK: key=" + key + " valueLen=" + value.getLength());
				else
					System.out.println("<NOTFOUND: key=" + key);
			}
			
			reader.close();
		}
	}
	
}
