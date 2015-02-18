package org.javenstudio.falcon.search.component;

import java.util.List;

import org.javenstudio.common.indexdb.ICollector;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.falcon.ErrorException;

/**
 * Defines a grouping command.
 * This is an abstraction on how the {@link Collector} instances are created
 * and how the results are retrieved from the {@link Collector} instances.
 *
 */
public interface SearchCommand<T> {

	/**
	 * Returns a list of {@link Collector} instances to be
	 * included in the search based on the .
	 *
	 * @return a list of {@link Collector} instances
	 * @throws IOException If I/O related errors occur
	 */
	public List<ICollector> createCollectors() throws ErrorException;

	/**
	 * Returns the results that the collectors created
	 * by {@link #create()} contain after a search has been executed.
	 *
	 * @return The results of the collectors
	 */
	public T getResult();

	/**
	 * @return The key of this command to uniquely identify itself
	 */
	public String getKey();

	/**
	 * @return The group sort (overall sort)
	 */
	public ISort getGroupSort();

	/**
	 * @return The sort inside a group
	 */
	public ISort getSortWithinGroup();

}
