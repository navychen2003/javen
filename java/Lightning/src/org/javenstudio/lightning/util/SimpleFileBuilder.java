package org.javenstudio.lightning.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import org.javenstudio.common.util.MimeTypes;
import org.javenstudio.hornet.wrapper.SimpleDocument;

public abstract class SimpleFileBuilder<T> {

	private final AtomicLong mCount = new AtomicLong();
	private final LinkedList<File> mDirs = new LinkedList<File>();
	private final LinkedList<File> mFiles = new LinkedList<File>();
	
	public void addPath(String path) throws IOException { 
		if (path == null) return;
		addPath(new File(path));
	}
	
	public void addPath(File path) throws IOException { 
		if (path == null) return;
		
		if (!path.exists()) 
			throw new IOException("Path: " + path + " not existed!");
		
		if (!path.isDirectory()) 
			throw new IOException("Path: " + path + " is not a Directory!");
		
		if (!path.canRead()) 
			throw new IOException("Path: " + path + " cannot access!");
		
		synchronized(this) {
			mDirs.add(path);
		}
	}
	
	private void listFiles(File dir) throws IOException { 
		if (dir == null || !dir.isDirectory())
			return;
		
		File[] files = dir.listFiles();
		if (files == null) 
			return;
		
		for (File file : files) { 
			if (!file.canRead()) 
				continue;
			
			if (file.isDirectory()) 
				mDirs.add(file);
			else 
				mFiles.add(file);
		}
	}
	
    public synchronized T nextDoc() throws IOException {
    	T row = nextFile();
		if (row != null) 
			return row;
		
		while (mDirs.size() > 0) { 
			File dir = mDirs.removeFirst();
			listFiles(dir);
			
			if (mFiles.size() > 0)
				break;
		}
		
    	return nextFile();
    }
	
	public synchronized void close() { 
		mDirs.clear();
		mFiles.clear();
	}
	
	private T nextFile() throws IOException { 
		while (mFiles.size() > 0) { 
			File file = mFiles.removeFirst();
			if (file.isDirectory()) { 
				listFiles(file);
				continue;
			}
			
			return wrapDoc(file);
		}
		
		return null;
	}
	
	private final T wrapDoc(File file) throws IOException { 
		if (file == null) return null;
		
		onWrapDoc(file);
		
		String name = file.getName();
		String filename = name;
		String fileext = null;
		
		int pos = filename.lastIndexOf('.');
		if (pos > 0) { 
			filename = name.substring(0, pos);
			fileext = name.substring(pos+1);
		}
		
		T row = newDoc();
		
		try {
			addField(row, "id", "FILE@" + file.hashCode() + "/" + mCount.incrementAndGet());
			addField(row, "title", name);
			addField(row, "name", filename);
			addField(row, "content_type", MimeTypes.getContentTypeByFilename(name));
			addField(row, "features", file.getPath());
			addField(row, "path", file.getAbsolutePath());
			addField(row, "url", file.toURI().toString());
			addField(row, "price", new Float(file.length()));
			addField(row, "weight", new Long(file.length()));
			addField(row, "pubdate", new Date(file.lastModified()));
			
			if (fileext != null && fileext.length() > 0) 
				addField(row, "cat", fileext.toLowerCase());
			
			String content = loadContent(file);
			if (content != null && content.length() > 0) 
				addField(row, "content", content);
			
			finishFields(row);
		} catch (Throwable ex) { 
			onWrapErr(file, ex);
			return null;
		}
		
		return row;
	}
	
	protected int getFieldFlags(String name) { 
		if (name == null) return 0;
		
		if (name.equals("name")) { 
			return SimpleDocument.FLAG_INDEX | 
        			SimpleDocument.FLAG_TOKENIZE | 
        			SimpleDocument.FLAG_STORE_FIELD |
        			SimpleDocument.FLAG_STORE_TERMVECTORS;
			
		} else if (name.equals("content")) { 
			return SimpleDocument.FLAG_INDEX | 
        			SimpleDocument.FLAG_TOKENIZE | 
        			SimpleDocument.FLAG_STORE_TERMVECTORS;
		}
		
		return SimpleDocument.FLAG_INDEX | 
				SimpleDocument.FLAG_STORE_FIELD | 
    			SimpleDocument.FLAG_STORE_TERMVECTORS;
	}
	
	protected abstract T newDoc() throws IOException;
	protected abstract void addField(T doc, String name, Object val) throws IOException;
	protected abstract void onWrapErr(File file, Throwable ex) throws IOException;
	
	protected void onWrapDoc(File file) throws IOException {}
	
	protected String loadContent(File file) throws IOException { 
		if (file == null) 
			return null;
		
		if (isTextFile(file)) { 
			Reader is = openTextFile(file);
			if (is != null) {
				StringBuilder sbuf = new StringBuilder();
				try {
					BufferedReader br = new BufferedReader(is);
					String line = null;
					while ((line = br.readLine()) != null) { 
						sbuf.append(line);
						sbuf.append("\r\n");
					}
				} catch (Throwable e) { 
					// ignore
				} finally { 
					try { 
						is.close();
					} catch (Throwable ex) { 
						// ignore
					}
				}
				
				return sbuf.toString();
			}
		}
		
		return file.getAbsolutePath();
	}
	
	protected Reader openTextFile(File file) throws IOException { 
		return new FileReader(file);
	}
	
	protected void finishFields(T doc) throws IOException { 
		// do nothing
	}
	
	protected boolean isTextFile(File file) { 
		String name = file.getName().toLowerCase();
		if (name.endsWith(".txt") || name.endsWith(".xml") || name.endsWith(".html") || 
			name.endsWith(".java"))
			return true;
		
		return false;
	}
	
	public static class PostFiles extends SimpleFileBuilder<StringBuilder> {

		@Override
		protected StringBuilder newDoc() {
			StringBuilder doc = new StringBuilder();
			doc.append("<doc>\r\n");
			return doc;
		}

		@Override
		protected void finishFields(StringBuilder doc) { 
			doc.append("</doc>\r\n");
		}
		
		@Override
		protected void addField(StringBuilder doc, String name, Object val) {
			doc.append("  <field name=\"");
			doc.append(HTMLEncode(name));
			doc.append("\">");
			if (val instanceof Date) 
				doc.append(HTMLEncode(sDateFormater.format((Date)val)));
			else
				doc.append(HTMLEncode(val.toString()));
			doc.append("</field>\r\n");
		}

		@Override
		protected void onWrapErr(File file, Throwable ex) throws IOException {
			// ignore
		}
	}
	
	public static String HTMLEncode(String str) {
		if (str == null || str.length() == 0) 
			return str; 

		StringBuilder sbuf = new StringBuilder(); 
		for (int i = 0; i < str.length(); i ++) {
			char chr = str.charAt(i); 
			switch (chr) {
			case '>': 
				sbuf.append("&gt;"); 
				break; 
			case '<': 
				sbuf.append("&lt;"); 
				break; 
			case '&': 
				sbuf.append("&amp;"); 
				break; 
			case '\'': 
				sbuf.append("&apos;"); 
				break; 
			case '\"': 
				sbuf.append("&quot;"); 
				break; 
			default: 
				sbuf.append(chr); 
				break; 
			}
		}
		return sbuf.toString(); 
	}
	
	private static final SimpleDateFormat sDateFormater;
	static { 
		sDateFormater = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT);
		sDateFormater.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
}
