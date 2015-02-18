package org.javenstudio.panda.analysis;

import java.io.Reader;
import java.util.Map;

import org.javenstudio.common.indexdb.analysis.Tokenizer;

/**
 * Factory for {@link PathHierarchyTokenizer}. 
 * <p>
 * This factory is typically configured for use only in the <code>index</code> 
 * Analyzer (or only in the <code>query</code> Analyzer, but never both).
 * </p>
 * <p>
 * For example, in the configuration below a query for 
 * <code>Books/NonFic</code> will match documents indexed with values like 
 * <code>Books/NonFic</code>, <code>Books/NonFic/Law</code>, 
 * <code>Books/NonFic/Science/Physics</code>, etc. But it will not match 
 * documents indexed with values like <code>Books</code>, or 
 * <code>Books/Fic</code>...
 * </p>
 *
 * <pre class="prettyprint" >
 * &lt;fieldType name="descendent_path" class="solr.TextField"&gt;
 *   &lt;analyzer type="index"&gt;
 *     &lt;tokenizer class="solr.PathHierarchyTokenizerFactory" delimiter="/" /&gt;
 *   &lt;/analyzer&gt;
 *   &lt;analyzer type="query"&gt;
 *     &lt;tokenizer class="solr.KeywordTokenizerFactory" /&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;
 * </pre>
 * <p>
 * In this example however we see the oposite configuration, so that a query 
 * for <code>Books/NonFic/Science/Physics</code> would match documents 
 * containing <code>Books/NonFic</code>, <code>Books/NonFic/Science</code>, 
 * or <code>Books/NonFic/Science/Physics</code>, but not 
 * <code>Books/NonFic/Science/Physics/Theory</code> or 
 * <code>Books/NonFic/Law</code>.
 * </p>
 * <pre class="prettyprint" >
 * &lt;fieldType name="descendent_path" class="solr.TextField"&gt;
 *   &lt;analyzer type="index"&gt;
 *     &lt;tokenizer class="solr.KeywordTokenizerFactory" /&gt;
 *   &lt;/analyzer&gt;
 *   &lt;analyzer type="query"&gt;
 *     &lt;tokenizer class="solr.PathHierarchyTokenizerFactory" delimiter="/" /&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;
 * </pre>
 */
public class PathHierarchyTokenizerFactory extends TokenizerFactory {
  
	private int mSkip =  PathHierarchyTokenizer.DEFAULT_SKIP;
	private char mDelimiter;
	private char mReplacement;
	private boolean mReverse = false;
	
	/**
	 * Require a configured pattern
	 */
	@Override
	public void init(Map<String,String> args){
		super.init( args );
    
		String v = args.get( "delimiter" );
		if (v != null) {
			if (v.length() != 1) 
				throw new IllegalArgumentException("delimiter should be a char. \"" + v + "\" is invalid");
			else 
				mDelimiter = v.charAt(0);
			
		} else {
			mDelimiter = PathHierarchyTokenizer.DEFAULT_DELIMITER;
		}
    
		v = args.get("replace");
		if (v != null) {
			if (v.length() != 1) 
				throw new IllegalArgumentException("replace should be a char. \"" + v + "\" is invalid");
			else
				mReplacement = v.charAt(0);
			
		} else 
			mReplacement = mDelimiter;
    
		v = args.get("reverse");
		if (v != null) 
			mReverse = "true".equals(v);

		v = args.get("skip");
		if (v != null) 
			mSkip = Integer.parseInt(v);
	}

	@Override
	public Tokenizer create(Reader input) {
		if (mReverse) { 
			return new ReversePathHierarchyTokenizer(input, 
					mDelimiter, mReplacement, mSkip);
			
		} else { 
			return new PathHierarchyTokenizer(input, 
					mDelimiter, mReplacement, mSkip);
		}
	}
	
}


