package org.javenstudio.falcon.search.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.RecursiveTimer;
import org.javenstudio.falcon.search.ResponseBuilder;
import org.javenstudio.falcon.search.ISearchCore;
import org.javenstudio.falcon.search.shard.ShardRequest;

public class SearchComponents {
	protected static Logger LOG = Logger.getLogger(SearchComponents.class);
	
	static final String INIT_COMPONENTS 		= "components";
	static final String INIT_FIRST_COMPONENTS 	= "first-components";
	static final String INIT_LAST_COMPONENTS 	= "last-components";
	
	private static final List<String> sDefaultComponentNames = 
			new ArrayList<String>();
	
	private static final Map<String, Class<?>> sDefaultComponentClass = 
			new HashMap<String, Class<?>>();
	
	private static void addComponents(String name, Class<?> clazz) { 
		sDefaultComponentClass.put(name, clazz);
		sDefaultComponentNames.add(name);
	}
	
	private static Class<?> getDefaultComponentClass(String name) { 
		return sDefaultComponentClass.get(name);
	}
	
	private static Collection<String> getDefaultComponentNames() { 
		return sDefaultComponentNames;
	}
	
	static { 
		//addComponents(HighlightComponent.COMPONENT_NAME, HighlightComponent.class);
		addComponents(QueryComponent.COMPONENT_NAME, QueryComponent.class);
		addComponents(FacetComponent.COMPONENT_NAME, FacetComponent.class);
		addComponents(MoreLikeThisComponent.COMPONENT_NAME, MoreLikeThisComponent.class);
		addComponents(StatsComponent.COMPONENT_NAME, StatsComponent.class);
		addComponents(DebugComponent.COMPONENT_NAME, DebugComponent.class);
		//addComponents(RealTimeGetComponent.COMPONENT_NAME, RealTimeGetComponent.class);
	}
	
	public static void loadDefaultComponents(ISearchCore core, 
			Map<String, SearchComponent> components) throws ErrorException { 
		//for (Map.Entry<String, SearchComponent> e : components.entrySet()) {
			//SearchComponent c = e.getValue();
			
			//if (c instanceof HighlightComponent) {
			//	HighlightComponent hl = (HighlightComponent) c;
			//	if (!HighlightComponent.COMPONENT_NAME.equals(e.getKey())) 
			//		components.put(HighlightComponent.COMPONENT_NAME, hl);
			//	
			//	break;
			//}
		//}
		
		for (String name : getDefaultComponentNames()) { 
			SearchComponent.addIfNotPresent(core, 
					components, name, getDefaultComponentClass(name));
		}
	}
	
	protected final ISearchCore mCore;
	protected List<SearchComponent> mComponents = null;
	
	public SearchComponents(ISearchCore core) { 
		mCore = core;
		
		if (core == null) 
			throw new NullPointerException("SearchCore is null");
	}
	
	public ISearchCore getSearchCore() { 
		return mCore;
	}
	
	public List<SearchComponent> getComponents() { 
		return mComponents;
	}
	
	@SuppressWarnings("unchecked")
	public void initComponents(NamedList<?> initArgs) throws ErrorException { 
		if (mComponents != null) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"components already inited");
		}
		
		final ISearchCore core = mCore;
		
		List<String> declaredComponents = (List<String>) initArgs.get(INIT_COMPONENTS);
		List<String> firstComponents = (List<String>) initArgs.get(INIT_FIRST_COMPONENTS);
		List<String> lastComponents  = (List<String>) initArgs.get(INIT_LAST_COMPONENTS);
		
		Collection<String> list = null;
		boolean makeDebugLast = true;
		
		if (declaredComponents == null) {
			// Use the default component list
			list = new ArrayList<String>();
			list.addAll(getDefaultComponentNames());

			if (firstComponents != null) {
				List<String> clist = firstComponents;
				clist.addAll(list);
				list = clist;
			}

			if (lastComponents != null) 
				list.addAll(lastComponents);
			
		} else {
			list = (List<String>)declaredComponents;
			
			if (firstComponents != null || lastComponents != null) {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR,
						"First/Last components only valid if you do not declare 'components'");
			}
			
			makeDebugLast = false;
		}
		
		// Build the component list
		mComponents = new ArrayList<SearchComponent>(list.size());
		
		DebugComponent dbgCmp = null;
		for (String c : list){
			SearchComponent comp = core.getSearchComponent(c);
			if (comp instanceof DebugComponent && makeDebugLast == true){
				dbgCmp = (DebugComponent) comp;
				
			} else {
				mComponents.add(comp);
				
				if (LOG.isDebugEnabled())
					LOG.debug("Adding component: " + comp);
			}
		}
		
		if (makeDebugLast == true && dbgCmp != null){
			mComponents.add(dbgCmp);
			
			if (LOG.isDebugEnabled())
				LOG.debug("Adding debug component: " + dbgCmp);
		}
	}
	
	public void prepare(ResponseBuilder rb) throws ErrorException { 
		// non-debugging prepare phase
		for (SearchComponent c : mComponents) {
			c.prepare(rb);
		}
	}
	
	public void prepare(ResponseBuilder rb, RecursiveTimer timer) 
			throws ErrorException { 
		if (timer == null) { 
			prepare(rb); 
			return;
		}
		
		// debugging prepare phase
		RecursiveTimer subt = timer.sub("prepare");
		
		for (SearchComponent c : mComponents) {
			rb.setTimer(subt.sub(c.getName()));
			
			c.prepare(rb);
			
			rb.getTimer().stop();
		}
		
		subt.stop();
	}
	
	public void process(ResponseBuilder rb) throws ErrorException { 
		// Process
		for (SearchComponent c : mComponents) {
			c.process(rb);
		}
	}
	
	public void process(ResponseBuilder rb, RecursiveTimer timer) 
			throws ErrorException { 
		if (timer == null) { 
			process(rb); 
			return;
		}
		
		// Process
		RecursiveTimer subt = timer.sub("process");
		
		for (SearchComponent c : mComponents) {
			rb.setTimer(subt.sub(c.getName()));
			
			c.process(rb);
			
			rb.getTimer().stop();
		}
		
		subt.stop();
	}
	
	public void modifyRequest(ResponseBuilder rb, SearchComponent me, 
			ShardRequest sreq) throws ErrorException { 
		// if this isn't a private request, let other components modify it.
		for (SearchComponent component : mComponents) {
			if (component != me) 
				component.modifyRequest(rb, me, sreq);
		}
	}
	
	public String getDescription() { 
		StringBuilder sb = new StringBuilder();
		for (SearchComponent c : mComponents) {
			if (sb.length() > 0) 
				sb.append(",");
			sb.append(c.getName());
		}
		return sb.toString();
	}
	
}
