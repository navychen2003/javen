package org.javenstudio.falcon.search.dataimport;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.javenstudio.falcon.ErrorException;

/**
 * <p>
 * Pluggable functions for resolving variables
 * Implementations of this abstract class must provide a public no-arg constructor.
 * </p>
 * <b>This API is experimental and may change in the future.</b>
 *
 * @since  1.3
 */
public abstract class VariableEvaluator {

	public static final Pattern IN_SINGLE_QUOTES = Pattern.compile("^'(.*?)'$");
	  
	public static final String DATE_FORMAT_EVALUATOR = "formatDate";
	public static final String URL_ENCODE_EVALUATOR = "encodeUrl";
	public static final String ESCAPE_SOLR_QUERY_CHARS = "escapeQueryChars";
	public static final String SQL_ESCAPE_EVALUATOR = "escapeSql";
	
	/**
	 * Return a String after processing an expression and a 
	 * {@link VariableResolver}
	 *
	 * @see VariableResolver
	 * @param expression string to be evaluated
	 * @param context instance
	 * @return the value of the given expression evaluated using the resolver
	 */
	public abstract String evaluate(ImportContext context, String expression) 
			throws ErrorException;
  
	/**
	 * Parses a string of expression into separate params. 
	 * The values are separated by commas. each value will be
	 * translated into one of the following:
	 * &lt;ol&gt;
	 * &lt;li&gt;If it is in single quotes the value will be translated 
	 * to a String&lt;/li&gt;
	 * &lt;li&gt;If is is not in quotes and is a number a it will be translated 
	 * into a Double&lt;/li&gt;
	 * &lt;li&gt;else it is a variable which can be resolved and it will be 
	 * put in as an instance of VariableWrapper&lt;/li&gt;
	 * &lt;/ol&gt;
	 *
	 * @param expression the expression to be parsed
	 * @param vr the VariableResolver instance for resolving variables
	 *
	 * @return a List of objects which can either be a string, number 
	 * or a variable wrapper
	 */
	protected List<Object> parseParams(ImportContext context, VariableResolver vr, 
			String expression) throws ErrorException {
		List<Object> result = new ArrayList<Object>();
		
		expression = expression.trim();
		String[] ss = expression.split(",");
		
		for (int i = 0; i < ss.length; i++) {
			ss[i] = ss[i].trim();
			
			if (ss[i].startsWith("'")) { //a string param has started
				StringBuilder sb = new StringBuilder();
				while (true) {
					sb.append(ss[i]);
					if (ss[i].endsWith("'")) break;
					i++;
					
					if (i >= ss.length) {
						throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
								"invalid string at " + ss[i - 1] + " in function params: " + expression);
					}
					sb.append(",");
				}
				
				String s = sb.substring(1, sb.length() - 1);
				s = s.replaceAll("\\\\'", "'");
				result.add(s);
				
			} else {
				if (Character.isDigit(ss[i].charAt(0))) {
					try {
						Double doub = Double.parseDouble(ss[i]);
						result.add(doub);
					} catch (NumberFormatException e) {
						if (vr.resolve(ss[i]) == null) {
							throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
									"Invalid number :" + ss[i] + "in parameters  " + expression, e);
						}
					}
				} else {
					result.add(new VariableWrapper(ss[i], vr));
				}
			}
		}
		
		return result;
	}

	static class VariableWrapper {
		
		private VariableResolver mResolver;
		private String mName;

		public VariableWrapper(String s, VariableResolver vr) {
			mName = s;
			mResolver = vr;
		}

		public Object resolve() throws ErrorException {
			return mResolver.resolve(mName);
		}

		@Override
		public String toString() {
			try {
				return mResolver.resolve(mName).toString();
			} catch (Throwable e) { 
				return null;
			}
		}
	}

}
