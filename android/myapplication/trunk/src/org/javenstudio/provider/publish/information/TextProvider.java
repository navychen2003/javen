package org.javenstudio.provider.publish.information;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.net.Uri;

import org.javenstudio.android.data.ReloadType;
import org.javenstudio.android.information.InformationOperation;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.net.http.HttpException;
import org.javenstudio.cocoka.net.http.fetch.FetchHelper;
import org.javenstudio.cocoka.net.http.fetch.HtmlCallback;
import org.javenstudio.cocoka.util.Utils;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.ProviderBase;
import org.javenstudio.provider.ProviderBinder;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.ProviderModel;

public abstract class TextProvider extends ProviderBase {
	private static final Logger LOG = Logger.getLogger(TextProvider.class);
	
	private final TextBinder mBinder;
	private OnTextClickListener mClickListener = null;
	private OnTextClickListener mLongClickListener = null;
	private OnTextClickListener mImageClickListener = null;
	
	protected TextProvider(String name, int iconRes, boolean gridBinder) { 
		super(name, iconRes);
		mBinder = new TextBinder(this, gridBinder);
		
		setOnLongClickListener(new OnTextClickListener() {
				@Override
				public boolean onTextClick(Activity activity, TextItem item) {
					return InformationOperation.openOperation(activity, item);
				}
			});
	}

	public void setOnItemClickListener(OnTextClickListener l) { mClickListener = l; }
	public OnTextClickListener getOnItemClickListener() { return mClickListener; }
	
	public void setOnLongClickListener(OnTextClickListener l) { mLongClickListener = l; }
	public OnTextClickListener getOnLongClickListener() { return mLongClickListener; }
	
	public void setOnViewClickListener(OnTextClickListener l) { mImageClickListener = l; }
	public OnTextClickListener getOnViewClickListener() { return mImageClickListener; }
	
	@Override
	public ProviderBinder getBinder() {
		return mBinder;
	}
	
	public abstract TextDataSets getDataSets();
	
	@Override
	public void reloadOnThread(ProviderCallback callback, 
			ReloadType type, long reloadId) { 
		// do nothing
	}
	
	protected final void startLoad(final ProviderCallback pc, 
			final String location, boolean forceFetch) { 
		if (pc == null || location == null) 
			return;
		
		pc.getController().getModel().callbackInvoke(
				ProviderModel.ACTION_ONFETCHSTART, null); 
		
		try { 
			Uri uri = Uri.parse(location);
			String scheme = uri.getScheme();
			
			if (scheme != null && scheme.equalsIgnoreCase("assets")) { 
				loadFile(pc, location, uri.getHost()); 
				
			} else if (scheme != null && scheme.equalsIgnoreCase("html")) { 
				loadHtml(pc, location); 
				
			} else { 
				fetchHtml(pc, location, forceFetch);
			}
		} catch (Throwable e) { 
			if (LOG.isWarnEnabled())
				LOG.warn("startLoad: error: " + e, e);
		}
		
		pc.getController().getModel().callbackInvoke(
				ProviderModel.ACTION_ONFETCHSTOP, null); 
	}
	
	private void loadHtml(final ProviderCallback pc, 
			final String location) { 
		if (pc == null || location == null) 
			return;
		
		if (LOG.isDebugEnabled())
			LOG.debug("loadHtml: location=" + location);
		
		String content = null;
		
		try {
			
		} catch (Throwable e) { 
			if (LOG.isWarnEnabled())
				LOG.warn("loadHtml: error: " + e, e);
			
		}
		
		onLoaded(pc, location, content);
	}
	
	private void loadFile(final ProviderCallback pc, 
			final String location, String filename) { 
		if (pc == null || location == null || filename == null) 
			return;
		
		if (LOG.isDebugEnabled())
			LOG.debug("loadFile: filename=" + filename);
		
		String content = null;
		InputStream is = null;
		
		try {
			is = ResourceHelper.getContext().getAssets().open(filename);
			if (is != null) { 
				BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"));
				StringBuffer sbuf = new StringBuffer();
				String line = null;
				
				while ((line = br.readLine()) != null) { 
					sbuf.append(line);
					sbuf.append("\r\n");
				}
				
				content = sbuf.toString();
			}
		} catch (IOException e) { 
			if (LOG.isWarnEnabled())
				LOG.warn("loadFile: error: " + e, e);
			
		} finally { 
			Utils.closeSilently(is);
		}
		
		onLoaded(pc, location, content);
	}
	
	private void fetchHtml(final ProviderCallback pc, 
			final String location, boolean forceFetch) { 
		if (pc == null || location == null) 
			return;
		
		if (LOG.isDebugEnabled())
			LOG.debug("fetchHtml: location=" + location);
		
		//pc.getController().getModel().callbackInvoke(
		//		ProviderModel.ACTION_ONFETCHSTART, null); 
		
		HtmlCallback callback = new HtmlCallback() {
				@Override
				public void onHtmlFetched(String content) {
					onLoaded(pc, location, content); 
					//pc.getController().getModel().callbackInvoke(
					//		ProviderModel.ACTION_ONFETCHSTOP, null); 
				}
				@Override
				public void onHttpException(HttpException e) { 
					pc.getController().getModel().onExceptionCatched(e); 
				}
			};
		
		callback.setRefetchContent(forceFetch);
		callback.setSaveContent(true);
		
		FetchHelper.removeFailed(location);
		FetchHelper.fetchHtml(location, callback);
		
		//pc.getController().getModel().callbackInvoke(
		//		ProviderModel.ACTION_ONFETCHSTOP, null); 
	}
	
	protected void onLoaded(ProviderCallback callback, String location, String content) { 
		// do nothing
	}
	
	public static TextProvider createRss(String name, int iconRes, String rssUrl) { 
		return createRss(name, iconRes, rssUrl, false);
	}
	
	public static TextProvider createRss(String name, int iconRes, String rssUrl, boolean gridBinder) { 
		return new RssProvider(name, iconRes, rssUrl, gridBinder);
	}
	
}
