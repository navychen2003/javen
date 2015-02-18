package org.javenstudio.falcon.search.analysis;

import java.io.IOException;
import java.io.Reader;

import org.javenstudio.common.indexdb.analysis.Analyzer;

public abstract class BaseAnalyzer extends Analyzer {
	
	private int mPosIncGap = 0;

	public void setPositionIncrementGap(int gap) {
		mPosIncGap = gap;
	}

	@Override
	public int getPositionIncrementGap(String fieldName) {
		return mPosIncGap;
	}

	/** wrap the reader in a CharStream, if appropriate */
	@Deprecated
	public Reader charStream(Reader reader) {
		return reader;
	}

	@Override
	public Reader initReader(String fieldName, Reader reader)
			throws IOException {
		return charStream(reader);
	}
  
}
