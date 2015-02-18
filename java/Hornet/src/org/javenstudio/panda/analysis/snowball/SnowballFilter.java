package org.javenstudio.panda.analysis.snowball;

import java.io.IOException;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.common.indexdb.analysis.CharToken;
import org.javenstudio.common.indexdb.analysis.TokenFilter;
import org.javenstudio.panda.snowball.SnowballProgram;

/**
 * A filter that stems words using a Snowball-generated stemmer.
 *
 * Available stemmers are listed in {@link org.javenstudio.panda.snowball.ext}.
 * <p><b>NOTE</b>: SnowballFilter expects lowercased text.
 * <ul>
 *  <li>For the Turkish language, see {@link TurkishLowerCaseFilter}.
 *  <li>For other languages, see {@link LowerCaseFilter}.
 * </ul>
 * </p>
 */
public final class SnowballFilter extends TokenFilter {

	private final SnowballProgram mStemmer;

	public SnowballFilter(ITokenStream input, SnowballProgram stemmer) {
		super(input);
		mStemmer = stemmer;
	}

	/**
	 * Construct the named stemming filter.
	 *
	 * Available stemmers are listed in {@link org.javenstudio.panda.snowball.ext}.
	 * The name of a stemmer is the part of the class name before "Stemmer",
	 * e.g., the stemmer in {@link org.javenstudio.panda.snowball.ext.EnglishStemmer} is named "English".
	 *
	 * @param in the input tokens to stem
	 * @param name the name of a stemmer
	 */
	public SnowballFilter(ITokenStream in, String name) {
		super(in);
		//Class.forName is frowned upon in place of the ResourceLoader but in this case,
		// the factory will use the other constructor so that the program is already loaded.
		try {
			Class<? extends SnowballProgram> stemClass =
					Class.forName("org.tartarus.snowball.ext." + name + "Stemmer")
						.asSubclass(SnowballProgram.class);
			mStemmer = stemClass.newInstance();
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid stemmer class specified: " + name, e);
		}
	}

	/** Returns the next input Token, after being stemmed */
	@Override
	public final IToken nextToken() throws IOException {
		CharToken token = (CharToken)mInput.nextToken();
		
		if (token != null) {
			if (!token.isKeyword()) {
				char termBuffer[] = token.getTerm().buffer();
				int length = token.getTerm().length();
				
				mStemmer.setCurrent(termBuffer, length);
				mStemmer.stem();
				
				final char finalTerm[] = mStemmer.getCurrentBuffer();
				final int newLength = mStemmer.getCurrentBufferLength();
				
				if (finalTerm != termBuffer)
					token.getTerm().copyBuffer(finalTerm, 0, newLength);
				else
					token.getTerm().setLength(newLength);
			}
			
			return token;
		}
		
		return null;
	}
	
}
