package org.javenstudio.falcon.search.component;

import java.io.IOException;
import java.util.Map;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.InfoMBean;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedListPlugin;
import org.javenstudio.falcon.search.ResponseBuilder;
import org.javenstudio.falcon.search.ISearchCore;
import org.javenstudio.falcon.search.shard.ShardRequest;

/**
 * TODO!
 * 
 * @since 1.3
 */
public abstract class SearchComponent implements NamedListPlugin, InfoMBean {
	
	protected ISearchCore mCore;
	
	public ISearchCore getSearchCore() { 
		return mCore;
	}
	
	/**
	 * Prepare the response.  Guaranteed to be called before any SearchComponent 
	 * {@link #process(ResponseBuilder)} method.
	 * Called for every incoming request.
	 *
	 * The place to do initialization that is request dependent.
	 * @param rb The {@link ResponseBuilder}
	 * @throws IOException If there is a low-level I/O error.
	 */
	public abstract void prepare(ResponseBuilder rb) throws ErrorException;

	/**
	 * Process the request for this component 
	 * @param rb The {@link ResponseBuilder}
	 * @throws IOException If there is a low-level I/O error.
	 */
	public abstract void process(ResponseBuilder rb) throws ErrorException;

	/**
	 * Process for a distributed search.
	 * @return the next stage for this component
	 */
	public int distributedProcess(ResponseBuilder rb) throws ErrorException {
		return ResponseBuilder.STAGE_DONE;
	}

	/** Called after another component adds a request */
	public void modifyRequest(ResponseBuilder rb, SearchComponent who, 
			ShardRequest sreq) throws ErrorException {
		// do nothing
	}

	/** Called after all responses for a single request were received */
	public void handleResponses(ResponseBuilder rb, ShardRequest sreq) 
			throws ErrorException {
		// do nothing
	}

	/** 
	 * Called after all responses have been received for this stage.
	 * Useful when different requests are sent to each shard.
	 */
	public void finishStage(ResponseBuilder rb) throws ErrorException {
		// do nothing
	}

	public String getName() { 
		return getClass().getSimpleName(); 
	}
	
	//////////////////////// NamedListPlugin methods //////////////////////
	@Override
	public void init(NamedList<?> args) throws ErrorException {
		// By default do nothing
	}
  
	//////////////////////// InfoMBeans methods //////////////////////

	@Override
	public String getMBeanKey() {
		return getName();
	}
	
	@Override
	public String getMBeanName() {
		return getClass().getName();
	}
  
	@Override
	public String getMBeanDescription() { 
		return "";
	}
  
	@Override
	public String getMBeanVersion() {
		return "1.0";
	}
	
	@Override
	public String getMBeanCategory() { 
		return InfoMBean.CATEGORY_OTHER;
	}
	
	@Override
	public NamedList<?> getMBeanStatistics() { 
		return null;
	}
	
	@Override
	public String toString() { 
		return getClass().getName();
	}
	
	public static void addIfNotPresent(ISearchCore core, 
			Map<String,SearchComponent> registry, String name, Class<?> c) 
			throws ErrorException {
		if (!registry.containsKey(name)) {
			Object searchComp = core.newInstance(c.getName(), c);
			
			if (searchComp != null && searchComp instanceof SearchComponent) { 
				SearchComponent sc = (SearchComponent)searchComp;
				sc.mCore = core;
				
				if (searchComp instanceof NamedListPlugin)
					((NamedListPlugin)searchComp).init(NamedList.EMPTY);
				
				registry.put(name, sc);
				
			} else {
				String className = (searchComp != null) ? 
						searchComp.getClass().getName() : null;
				
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"wrong SearchComponent: " + className);
			}
		}
	}
	
}
