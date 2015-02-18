package org.javenstudio.lightning.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

import org.javenstudio.common.util.Log;
import org.javenstudio.hornet.wrapper.SimpleDocument;
import org.javenstudio.hornet.wrapper.SimpleField;
import org.javenstudio.hornet.wrapper.SimpleSearcher;
import org.javenstudio.hornet.wrapper.SimpleTopDocs;
import org.javenstudio.panda.analysis.standard.StandardAnalyzer;

/** Simple command-line based search demo. */
public class SimpleSearchFiles {

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
	
	private SimpleSearchFiles() {}
	
	/** Simple command-line based search demo. */
	public static void main(String[] args) throws Exception {
		String usage =
				"Usage:\tjava SearchFiles [-index dir] [-field f] [-repeat n] [-queries file] [-query string] [-raw] [-paging hitsPerPage]\n";
		if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
			System.out.println(usage);
			System.exit(0);
		}

		String index = "index";
		String field = "name";
		String queries = null;
		int repeat = 0;
		boolean raw = false;
		String queryString = null;
		int hitsPerPage = 10;
    
		for (int i = 0; i < args.length; i++) {
			if ("-index".equals(args[i])) {
				index = args[i+1];
				i++;
			} else if ("-field".equals(args[i])) {
				field = args[i+1];
				i++;
			} else if ("-queries".equals(args[i])) {
				queries = args[i+1];
				i++;
			} else if ("-query".equals(args[i])) {
				queryString = args[i+1];
				i++;
			} else if ("-repeat".equals(args[i])) {
				repeat = Integer.parseInt(args[i+1]);
				i++;
			} else if ("-raw".equals(args[i])) {
				raw = true;
			} else if ("-debug".equals(args[i])) {
	    		Log.setLogDebug(true);
	    		sLogLevel = Log.LOG_LEVEL_V;
			} else if ("-paging".equals(args[i])) {
				hitsPerPage = Integer.parseInt(args[i+1]);
				if (hitsPerPage <= 0) {
					System.err.println("There must be at least 1 hit per page.");
					System.exit(1);
				}
				i++;
			}
		}
    
		SimpleSearcher searcher = new SimpleSearcher(new File(index), 
				new StandardAnalyzer());
		
		BufferedReader in = null;
		if (queries != null) {
			in = new BufferedReader(new InputStreamReader(new FileInputStream(queries), "UTF-8"));
		} else {
			in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
		}
		
		while (true) {
			if (queries == null && queryString == null) {	// prompt the user
				System.out.println("Enter query: ");
			}

			String line = queryString != null ? queryString : in.readLine();
			if (line == null || line.length() == -1) 
				break;

			line = line.trim();
			if (line.length() == 0) 
				break;
      
			System.out.println("Searching for: " + line);
            
			if (repeat > 0) {                           // repeat & time as benchmark
				Date start = new Date();
				for (int i = 0; i < repeat; i++) {
					searcher.search(field, line);
				}
				Date end = new Date();
				System.out.println("Time: "+(end.getTime()-start.getTime())+"ms");
			}

			doPagingSearch(in, searcher, field, line, hitsPerPage, raw, 
					queries == null && queryString == null);

			if (queryString != null) 
				break;
		}
		
		searcher.close();
	}
	
	/**
	 * This demonstrates a typical paging search scenario, where the search engine presents 
	 * pages of size n to the user. The user can then go to the next page if interested in
	 * the next hits.
	 * 
	 * When the query is executed for the first time, then only enough results are collected
	 * to fill 5 result pages. If the user wants to page beyond this limit, then the query
	 * is executed another time and all hits are collected.
	 * 
	 */
	public static void doPagingSearch(BufferedReader in, SimpleSearcher searcher, 
			String field, String query, int hitsPerPage, boolean raw, boolean interactive) 
			throws IOException {
 
		// Collect enough docs to show 5 pages
		SimpleTopDocs results = searcher.search(field, query, 5 * hitsPerPage);
		SimpleTopDocs.ScoreDoc[] hits = results.getScoreDocs();
    
		int numTotalHits = results.getTotalHits();
		System.out.println(numTotalHits + " total matching documents");

		int start = 0;
		int end = Math.min(numTotalHits, hitsPerPage);
        
		while (hits != null) {
			if (end > hits.length) {
				System.out.println("Only results 1 - " + hits.length +" of " + numTotalHits + " total matching documents collected.");
				System.out.println("Collect more (y/n) ?");
				String line = in.readLine();
				if (line.length() == 0 || line.charAt(0) == 'n') 
					break;

				hits = searcher.search(field, query, numTotalHits).getScoreDocs();
			}
      
			end = Math.min(hits.length, start + hitsPerPage);
      
			for (int i = start; i < end; i++) {
				if (raw) {                              // output raw format
					System.out.println("doc="+hits[i].getDoc()+" score="+hits[i].getScore());
					continue;
				}

				SimpleDocument doc = searcher.getDocument(hits[i].getDoc());
				SimpleField path = doc.getField("path");
				if (path != null) {
					System.out.println((i+1) + ". " + path.getStringValue());
					SimpleField title = doc.getField("title");
					if (title != null) 
						System.out.println("   Title: " + title.getStringValue());
					
				} else {
					System.out.println((i+1) + ". " + "No path for this document");
				}
			}

			if (!interactive || end == 0) 
				break;

			if (numTotalHits >= end) {
				boolean quit = false;
				while (true) {
					System.out.print("Press ");
					if (start - hitsPerPage >= 0) 
						System.out.print("(p)revious page, "); 
					if (start + hitsPerPage < numTotalHits) 
						System.out.print("(n)ext page, ");
					System.out.println("(q)uit or enter number to jump to a page.");
          
					String line = in.readLine();
					if (line.length() == 0 || line.charAt(0)=='q') {
						quit = true;
						break;
					}
					
					if (line.charAt(0) == 'p') {
						start = Math.max(0, start - hitsPerPage);
						break;
						
					} else if (line.charAt(0) == 'n') {
						if (start + hitsPerPage < numTotalHits) 
							start+=hitsPerPage;
						break;
						
					} else {
						int page = Integer.parseInt(line);
						if ((page - 1) * hitsPerPage < numTotalHits) {
							start = (page - 1) * hitsPerPage;
							break;
							
						} else 
							System.out.println("No such page");
					}
				}
				
				if (quit) break;
				end = Math.min(numTotalHits, start + hitsPerPage);
			}
		}
	}
	
}
