package org.javenstudio.lightning.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.javenstudio.common.util.Log;
import org.javenstudio.hornet.wrapper.SimpleDocument;
import org.javenstudio.hornet.wrapper.SimpleIndexer;
import org.javenstudio.panda.analysis.standard.StandardAnalyzer;

/** 
 * Index all text files under a directory.
 * <p>
 * This is a command-line application demonstrating simple Lucene indexing.
 * Run it with no command-line arguments for usage information.
 */
public class SimpleIndexFiles {

	static int sLogLevel = Log.LOG_LEVEL_W;
	
	static { 
		Log.setLogDebug(false);
		Log.setLogImpl(new Log.LogImpl() {
				@Override
				public void log(int level, String tag, String message, Throwable e) {
					if (sLogLevel >= level) { 
						System.err.println(""+Log.toString(level)+"/"+tag+": "+message);
						if (e != null) 
							e.printStackTrace(System.err);
					}
				}
			});
	}
	
	/** Index all text files under a directory. */
	public static void main(String[] args) throws Exception {
	    String usage = "java org.javenstudio.common.indexdb.example.IndexFiles"
                + " [-index INDEX_PATH] [-docs DOCS_PATH...] [-update] [-withContents]\n\n"
                + "This indexes the documents in DOCS_PATH, creating a Lucene index"
                + "in INDEX_PATH that can be searched with SearchFiles";
	    
	    String indexPath = "index";
	    ArrayList<File> docsPaths = new ArrayList<File>();
	    boolean withContents = false;
	    boolean create = true;
	    
	    for (int i=0; i < args.length;i++) {
	    	if ("-index".equals(args[i])) {
	    		indexPath = args[i+1];
	    		i++;
	    	} else if ("-docs".equals(args[i])) {
	    		docsPaths.add(new File(args[i+1]));
	    		i++;
	    	} else if ("-update".equals(args[i])) {
	    		create = false;
	    	} else if ("-withContents".equals(args[i])) { 
	    		withContents = true;
	    	} else if ("-debug".equals(args[i])) {
	    		Log.setLogDebug(true);
	    		sLogLevel = Log.LOG_LEVEL_V;
	    	} else { 
	    		docsPaths.add(new File(args[i]));
	    	}
	    }
		
	    if (docsPaths.size() == 0) {
	    	System.err.println("Usage: " + usage);
	        System.exit(1);
	    }

	    for (File docDir : docsPaths) {
		    //final File docDir = new File(docsPath);
		    if (!docDir.exists() || !docDir.canRead()) {
		    	System.out.println("Document directory '" + docDir.getAbsolutePath() + "'" + 
		    			" does not exist or is not readable, please check the path");
		    	System.exit(1);
		    }
	    }
		
	    Date start = new Date();
	    try {
		    System.out.println("Indexing to directory '" + indexPath + "'...");
		    
		    SimpleIndexer indexer = new SimpleIndexer(new File(indexPath), 
		    		new StandardAnalyzer());
		    indexPaths(indexer, docsPaths, create, withContents);
		    indexer.close();
		    
		    Date end = new Date();
		    System.out.println(end.getTime() - start.getTime() + " total milliseconds");
		    
	    } catch (IOException e) { 
	        System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
	        throw e;
	    }
	}
	
	static void indexPaths(SimpleIndexer indexer, List<File> paths, 
			boolean create, final boolean withContents) throws IOException { 
		SimpleFileBuilder<SimpleDocument> builder = new SimpleFileBuilder<SimpleDocument>() {
				@Override
				protected SimpleDocument newDoc() throws IOException {
					return new SimpleDocument();
				}
	
				@Override
				protected void addField(SimpleDocument doc, String name, Object val)
						throws IOException {
					if (val == null) return;
					
					if (val instanceof Date) {
						doc.addField(name, ((Date)val).getTime(), true);
						
					} else if (val instanceof Number) { 
						doc.addField(name, ((Number)val).floatValue(), true);
						
					} else { 
						doc.addField(name, val.toString(), getFieldFlags(name));
					}
				}
	
				@Override
				protected String loadContent(File file) throws IOException { 
					if (!withContents) return null;
					return super.loadContent(file);
				}
				
				@Override
				protected void onWrapErr(File file, Throwable ex)
						throws IOException {
					// ignore
				}
				
				@Override
				protected void onWrapDoc(File file) throws IOException { 
					System.out.println("adding " + file);
				}
			};
		
		for (File docDir : paths) {
	    	//indexDocs(indexer, docDir, create, withContents);
			builder.addPath(docDir);
	    }
		
		SimpleDocument doc = null;
		while ((doc = builder.nextDoc()) != null) { 
        	indexer.addDocument(doc);
		}
		
		builder.close();
	}
	
	static void indexDocs(SimpleIndexer indexer, File file, 
			boolean create, boolean withContents) throws IOException {
		if (!file.canRead()) return;
		
		if (file.isDirectory()) {
	        String[] files = file.list();
	        // an IO error could occur
	        if (files != null) {
	        	for (int i = 0; i < files.length; i++) {
	        		indexDocs(indexer, new File(file, files[i]), create, withContents);
	        	}
	        }
	        return;
		}
		
		FileInputStream fis;
        try {
        	fis = new FileInputStream(file);
        } catch (FileNotFoundException fnfe) {
        	// at least on windows, some temporary files raise this exception with an "access denied" message
        	// checking if the file can be read doesn't help
        	return;
        }
        
        try {
        	// make a new, empty document
        	SimpleDocument doc = new SimpleDocument();
        	
            // Add the path of the file as a field named "path".  Use a
            // field that is indexed (i.e. searchable), but don't tokenize 
            // the field into separate words and don't index term frequency
            // or positional information:
        	doc.addField("path", file.getAbsolutePath(), 
        			SimpleDocument.FLAG_INDEX | 
        			SimpleDocument.FLAG_STORE_FIELD);
        	
        	doc.addField("contents", new StringReader(getContentName(file)), 
        			SimpleDocument.FLAG_INDEX | 
        			SimpleDocument.FLAG_TOKENIZE | 
        			SimpleDocument.FLAG_STORE_TERMVECTORS);
        	
        	// Add the last modified date of the file a field named "modified".
            // Use a LongField that is indexed (i.e. efficiently filterable with
            // NumericRangeFilter).  This indexes to milli-second resolution, which
            // is often too fine.  You could instead create a number based on
            // year/month/day/hour/minutes/seconds, down the resolution you require.
            // For example the long value 2011021714 would mean
            // February 17, 2011, 2-3 PM.
        	doc.addField("modified", file.lastModified(), true);
        	
            // Add the contents of the file to a field named "contents".  Specify a Reader,
            // so that the text of the file is tokenized and indexed, but not stored.
            // Note that FileReader expects the file to be in UTF-8 encoding.
            // If that's not the case searching for special characters will fail.
        	if (withContents && isTextFile(file)) { 
	        	doc.addField("contents", new BufferedReader(new InputStreamReader(fis, "UTF-8")), 
	        			SimpleDocument.FLAG_INDEX | 
	        			SimpleDocument.FLAG_TOKENIZE | 
	        			SimpleDocument.FLAG_STORE_TERMVECTORS);
        	}
        	
        	//System.out.println("adding " + file);
        	indexer.addDocument(doc);
        	
        } finally {
            fis.close();
        }
	}
	
	private static boolean isTextFile(File file) { 
		String name = file.getName().toLowerCase();
		if (name.endsWith(".txt") || name.endsWith(".xml") || name.endsWith(".html") || 
			name.endsWith(".java"))
			return true;
		
		return false;
	}
	
	private static String getContentName(File file) { 
		String filename = file.getName();
		if (filename == null) filename = "";
		
		int pos = filename.lastIndexOf('.');
		if (pos > 0) 
			filename = filename.substring(0, pos);
		
		return filename;
	}
	
}
