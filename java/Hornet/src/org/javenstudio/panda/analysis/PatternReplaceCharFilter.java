package org.javenstudio.panda.analysis;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CharFilter that uses a regular expression for the target of replace string.
 * The pattern match will be done in each "block" in char stream.
 * 
 * <p>
 * ex1) source="aa&nbsp;&nbsp;bb&nbsp;aa&nbsp;bb", pattern="(aa)\\s+(bb)" replacement="$1#$2"<br/>
 * output="aa#bb&nbsp;aa#bb"
 * </p>
 * 
 * NOTE: If you produce a phrase that has different length to source string
 * and the field is used for highlighting for a term of the phrase, you will
 * face a trouble.
 * 
 * <p>
 * ex2) source="aa123bb", pattern="(aa)\\d+(bb)" replacement="$1&nbsp;$2"<br/>
 * output="aa&nbsp;bb"<br/>
 * and you want to search bb and highlight it, you will get<br/>
 * highlight snippet="aa1&lt;em&gt;23bb&lt;/em&gt;"
 * </p>
 * 
 * @since Solr 1.5
 */
public class PatternReplaceCharFilter extends CharFilterBase {
	
	@Deprecated
	public static final int DEFAULT_MAX_BLOCK_CHARS = 10000;

	private final Pattern mPattern;
	private final String mReplacement;
	private Reader mTransformedInput;

	public PatternReplaceCharFilter(Pattern pattern, String replacement, Reader in) {
		super(in);
		mPattern = pattern;
		mReplacement = replacement;
	}

	@Deprecated
	public PatternReplaceCharFilter(Pattern pattern, String replacement, 
			int maxBlockChars, String blockDelimiter, Reader in) {
		this(pattern, replacement, in);
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		// Buffer all input on the first call.
		if (mTransformedInput == null) 
			fill();

		return mTransformedInput.read(cbuf, off, len);
	}
  
	private void fill() throws IOException {
		StringBuilder buffered = new StringBuilder();
		char[] temp = new char[1024];
		for (int cnt = getInput().read(temp); cnt > 0; cnt = getInput().read(temp)) {
			buffered.append(temp, 0, cnt);
		}
		mTransformedInput = new StringReader(processPattern(buffered).toString());
	}

	@Override
	public int read() throws IOException {
		if (mTransformedInput == null) 
			fill();
    
		return mTransformedInput.read();
	}

	@Override
	protected int correct(int currentOff) {
		return Math.max(0, super.correct(currentOff));
	}

	/**
	 * Replace pattern in input and mark correction offsets. 
	 */
	protected CharSequence processPattern(CharSequence input) {
		final Matcher m = mPattern.matcher(input);
		final StringBuffer cumulativeOutput = new StringBuffer();
		
		int cumulative = 0;
		int lastMatchEnd = 0;
		
		while (m.find()) {
			final int groupSize = m.end() - m.start();
			final int skippedSize = m.start() - lastMatchEnd;
			
			lastMatchEnd = m.end();

			final int lengthBeforeReplacement = cumulativeOutput.length() + skippedSize;
			
			m.appendReplacement(cumulativeOutput, mReplacement);
			
			// Matcher doesn't tell us how many characters have been appended before the replacement.
			// So we need to calculate it. Skipped characters have been added as part of appendReplacement.
			final int replacementSize = cumulativeOutput.length() - lengthBeforeReplacement;

			if (groupSize != replacementSize) {
				if (replacementSize < groupSize) {
					// The replacement is smaller. 
					// Add the 'backskip' to the next index after the replacement (this is possibly 
					// after the end of string, but it's fine -- it just means the last character 
					// of the replaced block doesn't reach the end of the original string.
					cumulative += groupSize - replacementSize;
					
					int atIndex = lengthBeforeReplacement + replacementSize;
					addOffCorrectMap(atIndex, cumulative);
					
				} else {
					// The replacement is larger. Every new index needs to point to the last
					// element of the original group (if any).
					for (int i = groupSize; i < replacementSize; i++) {
						addOffCorrectMap(lengthBeforeReplacement + i, --cumulative);
					}
				}
			}
		}

		// Append the remaining output, no further changes to indices.
		m.appendTail(cumulativeOutput);
		
		return cumulativeOutput;    
	}
	
}
