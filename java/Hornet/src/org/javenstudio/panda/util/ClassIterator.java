package org.javenstudio.panda.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.ServiceConfigurationError;

import org.javenstudio.common.indexdb.util.IOUtils;

/**
 * Helper class for loading SPI classes from classpath (META-INF files).
 * This is a light impl of {@link java.util.ServiceLoader} but is guaranteed to
 * be bug-free regarding classpath order and does not instantiate or initialize
 * the classes found.
 */
public final class ClassIterator<S> implements Iterator<Class<? extends S>> {
	
	private static final String META_INF_SERVICES = "META-INF/services/";

	private final Class<S> mClazz;
	private final ClassLoader mLoader;
	private final Enumeration<URL> mProfilesEnum;
	private final String mProfileDefault;
	private Iterator<String> mLinesIterator;
  
	public static <S> ClassIterator<S> get(Class<S> clazz) { 
		return get(clazz, (String)null);
	}
  
	public static <S> ClassIterator<S> get(Class<S> clazz, String profileDefault) {
		return new ClassIterator<S>(clazz, 
				Thread.currentThread().getContextClassLoader(), profileDefault);
	}
  
	public static <S> ClassIterator<S> get(Class<S> clazz, ClassLoader loader) {
		return get(clazz, loader, (String)null);
	}
  
	public static <S> ClassIterator<S> get(Class<S> clazz, ClassLoader loader, String profileDefault) {
		return new ClassIterator<S>(clazz, loader, profileDefault);
	}
  
	private ClassIterator(Class<S> clazz, ClassLoader loader, String profileDefault) {
		if (loader == null)
			throw new IllegalArgumentException("You must provide a ClassLoader.");
		
		mClazz = clazz;
		mLoader = loader;
		mProfileDefault = profileDefault;
		
		try {
			mProfilesEnum = loader.getResources(META_INF_SERVICES + clazz.getName());
		} catch (IOException ioe) {
			throw new ServiceConfigurationError("Error loading SPI profiles for type " 
					+ clazz.getName() + " from classpath", ioe);
		}
		
		mLinesIterator = Collections.<String>emptySet().iterator();
	}
  
	private boolean loadNextProfile() {
		ArrayList<String> lines = null;
		while (mProfilesEnum.hasMoreElements()) {
			if (lines != null) 
				lines.clear();
			else 
				lines = new ArrayList<String>();
			
			final URL url = mProfilesEnum.nextElement();
			try {
				final InputStream in = url.openStream();
				IOException priorE = null;
				try {
					loadLines(lines, new InputStreamReader(in, IOUtils.CHARSET_UTF_8));
				} catch (IOException ioe) {
					priorE = ioe;
				} finally {
					IOUtils.closeWhileHandlingException(priorE, in);
				}
			} catch (IOException ioe) {
				throw new ServiceConfigurationError("Error loading SPI class list from URL: " + url, ioe);
			}
			
			if (!lines.isEmpty()) {
				mLinesIterator = lines.iterator();
				return true;
			}
		}
		
		if (mProfileDefault != null) { 
			if (lines != null) 
				lines.clear();
			else 
				lines = new ArrayList<String>();
			
			try {
				IOException priorE = null;
				try {
					loadLines(lines, new StringReader(mProfileDefault));
				} catch (IOException ioe) {
					priorE = ioe;
				} finally {
					IOUtils.closeWhileHandlingException(priorE);
				}
			} catch (IOException ioe) {
				throw new ServiceConfigurationError("Error loading SPI class list from default", ioe);
			}
			
			if (!lines.isEmpty()) {
				mLinesIterator = lines.iterator();
				return true;
			}
		}
		
		return false;
	}
  
	private void loadLines(final List<String> lines, final Reader input) throws IOException { 
		final BufferedReader reader = new BufferedReader(input);
		String line;
		while ((line = reader.readLine()) != null) {
			final int pos = line.indexOf('#');
			if (pos >= 0) 
				line = line.substring(0, pos);
			
			line = line.trim();
			if (line.length() > 0) 
				lines.add(line);
		}
	}
  
	@Override
	public boolean hasNext() {
		return mLinesIterator.hasNext() || loadNextProfile();
	}
  
	@Override
	public Class<? extends S> next() {
		// hasNext() implicitely loads the next profile, so it is essential to call this here!
		if (!hasNext()) 
			throw new NoSuchElementException();
		
		assert mLinesIterator.hasNext();
		final String c = mLinesIterator.next();
		try {
			// don't initialize the class (pass false as 2nd parameter):
			return Class.forName(c, false, mLoader).asSubclass(mClazz);
		} catch (ClassNotFoundException cnfe) {
			throw new ServiceConfigurationError(String.format(Locale.ROOT, 
					"A SPI class of type %s with classname %s does not exist, " +
					"please fix the file '%s%1$s' in your classpath.", 
					mClazz.getName(), c, META_INF_SERVICES));
		}
	}
  
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
  
}
