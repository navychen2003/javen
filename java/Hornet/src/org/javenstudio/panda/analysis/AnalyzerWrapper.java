package org.javenstudio.panda.analysis;

import java.io.IOException;
import java.io.Reader;

import org.javenstudio.common.indexdb.IAnalyzer;
import org.javenstudio.common.indexdb.analysis.Analyzer;
import org.javenstudio.common.indexdb.analysis.PerFieldReuseStrategy;
import org.javenstudio.common.indexdb.analysis.TokenComponents;

/**
 * Extension to {@link Analyzer} suitable for Analyzers which wrap
 * other Analyzers.
 * <p/>
 * {@link #getWrappedAnalyzer(String)} allows the Analyzer
 * to wrap multiple Analyzers which are selected on a per field basis.
 * <p/>
 * {@link #wrapComponents(String, HTMLHandler.TokenStreamComponents)} allows the
 * TokenStreamComponents of the wrapped Analyzer to then be wrapped
 * (such as adding a new {@link TokenFilter} to form new TokenStreamComponents.
 */
public abstract class AnalyzerWrapper extends Analyzer {

	/**
	 * Creates a new AnalyzerWrapper. Since the {@link HTMLHandler.ReuseStrategy} of
	 * the wrapped Analyzers are unknown, {@link HTMLHandler.PerFieldReuseStrategy} is assumed
	 */
	protected AnalyzerWrapper() {
		super(new PerFieldReuseStrategy());
	}

	/**
	 * Retrieves the wrapped Analyzer appropriate for analyzing the field with
	 * the given name
	 *
	 * @param fieldName Name of the field which is to be analyzed
	 * @return Analyzer for the field with the given name.  Assumed to be non-null
	 */
	protected abstract IAnalyzer getWrappedAnalyzer(String fieldName) 
			throws IOException;

	/**
	 * Wraps / alters the given TokenStreamComponents, taken from the wrapped
	 * Analyzer, to form new components.  It is through this method that new
	 * TokenFilters can be added by AnalyzerWrappers.
	 *
	 * @param fieldName Name of the field which is to be analyzed
	 * @param components TokenStreamComponents taken from the wrapped Analyzer
	 * @return Wrapped / altered TokenStreamComponents.
	 */
	protected abstract TokenComponents wrapComponents(String fieldName, 
			TokenComponents components) throws IOException;

	@Override
	public final TokenComponents createComponents(String fieldName, 
			Reader reader) throws IOException {
		return wrapComponents(fieldName, 
				((Analyzer)getWrappedAnalyzer(fieldName)).createComponents(fieldName, reader));
	}

	@Override
	public final int getPositionIncrementGap(String fieldName) throws IOException {
		return getWrappedAnalyzer(fieldName).getPositionIncrementGap(fieldName);
	}

	@Override
	public final int getOffsetGap(String fieldName) throws IOException {
		return getWrappedAnalyzer(fieldName).getOffsetGap(fieldName);
	}

	@Override
	public final Reader initReader(String fieldName, Reader reader) 
			throws IOException {
		return ((Analyzer)getWrappedAnalyzer(fieldName)).initReader(fieldName, reader);
	}
  
}
