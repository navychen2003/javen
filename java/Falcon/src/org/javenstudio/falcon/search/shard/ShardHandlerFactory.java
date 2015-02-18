package org.javenstudio.falcon.search.shard;

import org.javenstudio.falcon.util.PluginFactory;

public abstract class ShardHandlerFactory implements PluginFactory {

	public abstract ShardHandler getShardHandler();

	public abstract void close();
	
}
