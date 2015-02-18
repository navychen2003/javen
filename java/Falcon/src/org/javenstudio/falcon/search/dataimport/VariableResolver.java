package org.javenstudio.falcon.search.dataimport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.javenstudio.falcon.ErrorException;

/**
 * <p>
 * A set of nested maps that can resolve variables by namespaces. Variables are
 * enclosed with a dollar sign then an opening curly brace, ending with a
 * closing curly brace. Namespaces are delimited with '.' (period).
 * </p>
 * <p>
 * This class also has special logic to resolve evaluator calls by recognizing
 * the reserved function namespace: dataimporter.functions.xxx
 * </p>
 * <p>
 * This class caches strings that have already been resolved from the current
 * dih import.
 * </p>
 * <b>This API is experimental and may change in the future.</b>
 * 
 * @since 1.3
 */
public class VariableResolver {
  
	public static final String FUNCTIONS_NAMESPACE = "dataimporter.functions.";
	public static final String FUNCTIONS_NAMESPACE_SHORT = "dih.functions.";
	  
	private static final Pattern DOT_PATTERN = Pattern.compile("[.]");
	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("[$][{](.*?)[}]");
	private static final Pattern EVALUATOR_FORMAT_PATTERN = Pattern.compile("^(\\w*?)\\((.*?)\\)$");
	
	private Map<String,Resolved> mCache = new WeakHashMap<String,Resolved>();
	private Map<String,Object> mRootNamespace;
	private Map<String,VariableEvaluator> mEvaluators;
	private final ImportContext mContext;
	
	private class Resolved {
		private final List<Integer> mStartIndexes = new ArrayList<Integer>(2);
		private final List<Integer> mEndOffsets = new ArrayList<Integer>(2);
		private final List<String> mVariables = new ArrayList<String>(2);
	}
  
	public VariableResolver(ImportContext context) {
		this(context, (Properties)null);
	}
  
	public VariableResolver(ImportContext context, Properties defaults) {
		mContext = context;
		mRootNamespace = new HashMap<String,Object>();
		
		if (defaults != null) {
			for (Map.Entry<Object,Object> entry : defaults.entrySet()) {
				mRootNamespace.put(entry.getKey().toString(), entry.getValue());
			}
		}
	}
  
	public VariableResolver(ImportContext context, Map<String,Object> defaults) {
		mContext = context;
		mRootNamespace = new HashMap<String,Object>(defaults);
	}
  
	/**
	 * Resolves a given value with a name
	 * 
	 * @param name the String to be resolved
	 * @return an Object which is the result of evaluation of given name
	 */
	public Object resolve(String name) throws ErrorException {
		Object r = null;
		
		if (name != null) {
			String[] nameParts = DOT_PATTERN.split(name);
			Map<String,Object> currentLevel = currentLevelMap(nameParts,
					mRootNamespace, false);
			
			r = currentLevel.get(nameParts[nameParts.length - 1]);
			
			if (r == null && name.startsWith(FUNCTIONS_NAMESPACE) && 
					name.length() > FUNCTIONS_NAMESPACE.length()) {
				return resolveEvaluator(FUNCTIONS_NAMESPACE, name);
			}
			
			if (r == null && name.startsWith(FUNCTIONS_NAMESPACE_SHORT) && 
					name.length() > FUNCTIONS_NAMESPACE_SHORT.length()) {
				return resolveEvaluator(FUNCTIONS_NAMESPACE_SHORT, name);
			}
			
			if (r == null) 
				r = System.getProperty(name);
		}
		
		return r == null ? "" : r;
	}
  
	private Object resolveEvaluator(String namespace, String name) 
			throws ErrorException {
		if (mEvaluators == null) 
			return "";
    
		Matcher m = EVALUATOR_FORMAT_PATTERN.matcher(
				name.substring(namespace.length()));
		
		if (m.find()) {
			String fname = m.group(1);
			VariableEvaluator evaluator = mEvaluators.get(fname);
			if (evaluator == null) 
				return "";
			
			String g2 = m.group(2);
			return evaluator.evaluate(mContext, g2);
		}
		
		return "";
	}
  
	/**
	 * Given a String with place holders, replace them with the value tokens.
	 * 
	 * @return the string with the placeholders replaced with their values
	 */
	public String replaceTokens(String template) throws ErrorException {
		if (template == null) 
			return null;
    
		Resolved r = getResolved(template);
		
		if (r.mStartIndexes != null) {
			StringBuilder sb = new StringBuilder(template);
			
			for (int i = r.mStartIndexes.size() - 1; i >= 0; i--) {
				String replacement = resolve(r.mVariables.get(i)).toString();
				sb.replace(r.mStartIndexes.get(i), r.mEndOffsets.get(i), replacement);
			}
			
			return sb.toString();
		}
		
		return template;
	}
  
	private Resolved getResolved(String template) {
		Resolved r = mCache.get(template);
		if (r == null) {
			r = new Resolved();
			Matcher m = PLACEHOLDER_PATTERN.matcher(template);
			
			while (m.find()) {
				String variable = m.group(1);
				r.mStartIndexes.add(m.start(0));
				r.mEndOffsets.add(m.end(0));
				r.mVariables.add(variable);
			}
			
			mCache.put(template, r);
		}
		
		return r;
	}
	
	/**
	 * Get a list of variables embedded in the template string.
	 */
	public List<String> getVariables(String template) {
		Resolved r = getResolved(template);
		if (r == null) 
			return Collections.emptyList();
    
		return new ArrayList<String>(r.mVariables);
	}
  
	public void addNamespace(String name, Map<String,Object> newMap) {
		if (newMap != null) {
			if (name != null) {
				String[] nameParts = DOT_PATTERN.split(name);
				Map<String,Object> nameResolveLevel = currentLevelMap(nameParts,
						mRootNamespace, false);
				
				nameResolveLevel.put(nameParts[nameParts.length - 1], newMap);
				
			} else {
				for (Map.Entry<String,Object> entry : newMap.entrySet()) {
					String[] keyParts = DOT_PATTERN.split(entry.getKey());
					Map<String,Object> currentLevel = mRootNamespace;
					
					currentLevel = currentLevelMap(keyParts, currentLevel, false);
					currentLevel.put(keyParts[keyParts.length - 1], entry.getValue());
				}
			}
		}
	}
  
	private Map<String,Object> currentLevelMap(String[] keyParts,
			Map<String,Object> currentLevel, boolean includeLastLevel) {
		int j = includeLastLevel ? keyParts.length : keyParts.length - 1;
		
		for (int i = 0; i < j; i++) {
			Object o = currentLevel.get(keyParts[i]);
			if (o == null) {
				Map<String,Object> nextLevel = new HashMap<String,Object>();
				currentLevel.put(keyParts[i], nextLevel);
				currentLevel = nextLevel;
				
			} else if (o instanceof Map<?,?>) {
				@SuppressWarnings("unchecked")
				Map<String,Object> nextLevel = (Map<String,Object>) o;
				currentLevel = nextLevel;
				
			} else {
				throw new AssertionError("Non-leaf nodes should be of type java.util.Map");
			}
		}
		
		return currentLevel;
	}
  
	public void removeNamespace(String name) {
		mRootNamespace.remove(name);
	}
  
	public void setEvaluators(Map<String,VariableEvaluator> evaluators) {
		mEvaluators = evaluators;
	}
	
}
