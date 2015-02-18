package org.javenstudio.falcon.search.shard;

import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;

/**
 * A <code>ShardResultTransformer</code> is responsible for transforming 
 * a grouped shard result into group related
 * structures (such as {@link TopGroups} and {@link SearchGroup})
 * and visa versa.
 *
 */
public interface ShardTransformer<T, R> {

	/**
	 * Transforms data to a {@link NamedList} structure for serialization purposes.
	 *
	 * @param data The data to be transformed
	 * @return {@link NamedList} structure
	 * @throws IOException If I/O related errors occur during transforming
	 */
	public NamedList<?> transform(T data) throws ErrorException;

	/**
	 * Transforms the specified shard response into native structures.
	 *
	 * @param shardResponse The shard response containing data in a {@link NamedList} structure
	 * @param groupSort The group sort
	 * @param sortWithinGroup The sort inside a group
	 * @param shard The shard address where the response originated from
	 * @return native structure of the data
	 */
	public R transformToNative(NamedList<NamedList<?>> shardResponse, 
			ISort groupSort, ISort sortWithinGroup, String shard) throws ErrorException;

}
