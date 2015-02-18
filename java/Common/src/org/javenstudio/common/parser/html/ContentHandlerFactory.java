package org.javenstudio.common.parser.html;

public class ContentHandlerFactory implements ContentFactory {

	private final ContentTableFactory mDocumentTableFactory; 
	private final ContentTableFactory mSubTableFactory; 
	private final ContentTableHandler mTableHandler; 
	private final boolean mDebug;
	
	public ContentHandlerFactory(ContentTableFactory factory, 
			ContentTableFactory subfactory, ContentTableHandler handler) { 
		this(factory, subfactory, handler, false);
	}
	
	public ContentHandlerFactory(ContentTableFactory factory, 
			ContentTableFactory subfactory, ContentTableHandler handler, boolean debug) { 
		mDocumentTableFactory = factory; 
		mSubTableFactory = subfactory; 
		mTableHandler = handler; 
		mDebug = debug; 
	}
	
	@Override 
	public final Content newDocumentContent(TagElement start) { 
		if (mDocumentTableFactory != null) 
			return new ContentImpl(start, mDocumentTableFactory); 
		else 
			return null; 
	}
	
	@Override 
	public final Content newSubContent(final TagElement start) { 
		if (mSubTableFactory != null)
			return new ContentImpl(start, mSubTableFactory); 
		else 
			return null; 
	}
	
	@Override
	public final boolean isDebug() { return mDebug; }
	
	class ContentImpl implements Content { 
		private final TagElement mStart; 
		private final ContentHandler mHandler; 
		
		public ContentImpl(TagElement e, ContentTableFactory factory) { 
			mStart = e; 
			mHandler = new ContentHandler(factory); 
			mHandler.handleStart(mStart); 
			mTableHandler.handleTableCreate(mHandler.getContentTable()); 
		}
		
		@Override 
		public TagElement getStart() { 
			return mStart; 
		}
		
		@Override 
		public void handleElementStart(TagElement e) { 
			mHandler.handleElementStart(e); 
		}
		
		@Override 
		public void handleElementEnd(TagElement e, int endLength) { 
			mHandler.handleElementEnd(e, endLength); 
		}
		
		@Override 
		public void handleEnd() { 
			mHandler.handleEnd(mStart); 
			mTableHandler.handleTableFinish(mHandler.getContentTable()); 
		}
	}
	
}
