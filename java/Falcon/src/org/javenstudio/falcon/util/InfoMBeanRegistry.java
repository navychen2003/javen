package org.javenstudio.falcon.util;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;

public class InfoMBeanRegistry {
	static final Logger LOG = Logger.getLogger(InfoMBeanRegistry.class);

	private static Set<String> sCategories = 
			Collections.synchronizedSet(new HashSet<String>());
	
	private final Map<String,WeakReference<InfoMBean>> mInfoRegistry;
	
	public InfoMBeanRegistry() { 
		mInfoRegistry = new ConcurrentHashMap<String, WeakReference<InfoMBean>>();
	}
	
	public Collection<String> getCategories() { 
		return sCategories;
	}
	
	public Collection<String> getKeySet() { 
		return mInfoRegistry.keySet();
	}
	
	public synchronized InfoMBean getInfoMBean(String key) { 
		WeakReference<InfoMBean> ref = mInfoRegistry.get(key);
		if (ref != null) {
			InfoMBean bean = ref.get();
			if (bean == null) 
				mInfoRegistry.remove(key);
			
			return bean;
		}
		
		return null;
	}
	
	public synchronized InfoMBean remove(InfoMBean bean) throws ErrorException { 
		if (bean == null) return null;
		
		final String key = bean.getMBeanKey();
		
		if (mInfoRegistry.containsKey(key)) {
			InfoMBean beanReg = null;
			if (key != null) {
				WeakReference<InfoMBean> ref = mInfoRegistry.get(key);
				beanReg = ref != null ? ref.get() : null;
			}
			
			if (beanReg != bean) { 
				if (LOG.isDebugEnabled())
					LOG.debug("Cannot remove InfoMBean: " + beanReg + " not equals to " + bean);
				
				return null;
			}
			
			InfoMBean beanRm = null;
			if (key != null) {
				WeakReference<InfoMBean> ref = mInfoRegistry.remove(key);
				beanRm = ref != null ? ref.get() : null;
			}
			
			if (LOG.isDebugEnabled())
				LOG.debug("removed InfoMBean: key=" + key + " bean=" + beanRm);
			
			return beanRm;
		}
		
		return null;
	}
	
	public void register(InfoMBean bean) throws ErrorException { 
		register(bean, false);
	}
	
	public void register(InfoMBean bean, boolean replaceExisted) throws ErrorException { 
		if (bean != null) 
			register(bean.getMBeanKey(), bean, replaceExisted);
	}
	
	public synchronized void register(String key, InfoMBean bean, 
			boolean replaceExisted) throws ErrorException { 
		if (key != null && key.length() > 0 && bean != null) { 
			String category = bean.getMBeanCategory();
			
			if (key == null || key.length() == 0) {
				//key = bean.getClass().getName();
				
				if (LOG.isWarnEnabled()) {
					LOG.warn("Cannot register InfoMBean: " + bean.getClass().getName() 
							+ " with empty key");
				}
			}
			
			if (category == null || category.length() == 0)
				category = "DEFAULT";
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("register InfoMBean: key=" + key 
						+ " category=" + category + " bean=" + bean);
			}
			
			if (mInfoRegistry.containsKey(key)) { 
				WeakReference<InfoMBean> ref = mInfoRegistry.get(key);
				InfoMBean existBean = ref != null ? ref.get() : null;
				
				if (existBean != null) {
					if (existBean == bean) { 
						if (LOG.isWarnEnabled()) {
							LOG.warn("InfoMBean already registered, key=" + key 
									+ " bean=" + bean);
						}
						return;
					}
					
					if (!replaceExisted) {
						throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
								"InfoMBean already registered, key=" + key 
								+ " old=" + existBean + " new=" + bean);
						
					} else if (LOG.isDebugEnabled()) {
						LOG.debug("InfoMBean already registered, key=" + key 
								+ " old=" + existBean + " new=" + bean);
					}
				}
			}
			
			sCategories.add(category.toUpperCase());
			mInfoRegistry.put(key, new WeakReference<InfoMBean>(bean));
		}
	}
	
	public synchronized void clear() { 
		sCategories.clear();
		mInfoRegistry.clear();
	}
	
}
