package org.javenstudio.falcon.datum.data;

import org.javenstudio.common.util.MimeType;
import org.javenstudio.common.util.MimeTypes;
import org.javenstudio.falcon.datum.IData;
import org.javenstudio.raptor.fs.FileStatus;

public class SqAcceptor {

	private final FileFilter mFilter;
	
	public SqAcceptor() {
		this(null);
	}
	
	public SqAcceptor(FileFilter filter) { 
		mFilter = filter != null ? filter : new FileFilter();
	}
	
	public FileFilter getFilter() { return mFilter; }
	
	public boolean accept(IData data) {
		if (data != null && data instanceof SqSection)
			return accept((SqSection)data);
		else
			return false;
	}

	public boolean accept(SqSection section) { 
		if (section != null) { 
			//FileStatus status = section.getStatus();
			//if (status != null) 
			//	return accept(status);
		}
		
		return false;
	}
	
	static class PhotoAcceptor extends SqAcceptor { 
		public PhotoAcceptor() { 
			this(new PhotoFilter());
		}
		
		public PhotoAcceptor(FileFilter filter) { 
			super(filter);
		}
		
		@Override
		public boolean accept(SqSection section) { 
			if (section != null && super.accept(section)) { 
				String name = section.getName();
				if (name == null || name.length() == 0 || name.startsWith("."))
					return false;
				
				return false; //section.isDirectory() || (section instanceof FsImage);
			}
			
			return false;
		}
	}
	
	static class PhotoFilter extends FileFilter { 
		@Override
		public boolean accept(FileStatus status) { 
			if (super.accept(status)) { 
				String contentType = MimeTypes.getContentTypeByFilename(
						status.getPath().getName());
				
				if (contentType == null) 
					contentType = MimeType.TYPE_APPLICATION.getType();
				
				if (contentType.startsWith("image/"))
					return true;
			}
			
			return false;
		}
	}
	
	static class MusicAcceptor extends SqAcceptor { 
		public MusicAcceptor() { 
			this(new MusicFilter());
		}
		
		public MusicAcceptor(FileFilter filter) { 
			super(filter);
		}
		
		@Override
		public boolean accept(SqSection section) { 
			if (section != null && super.accept(section)) { 
				String name = section.getName();
				if (name == null || name.length() == 0 || name.startsWith("."))
					return false;
				
				return false; //section.isDirectory() || (section instanceof FsImage);
			}
			
			return false;
		}
	}
	
	static class MusicFilter extends FileFilter { 
		@Override
		public boolean accept(FileStatus status) { 
			if (super.accept(status)) { 
				String contentType = MimeTypes.getContentTypeByFilename(
						status.getPath().getName());
				
				if (contentType == null) 
					contentType = MimeType.TYPE_APPLICATION.getType();
				
				if (contentType.startsWith("audio/"))
					return true;
			}
			
			return false;
		}
	}
	
	static class VideoAcceptor extends SqAcceptor { 
		public VideoAcceptor() { 
			this(new VideoFilter());
		}
		
		public VideoAcceptor(FileFilter filter) { 
			super(filter);
		}
		
		@Override
		public boolean accept(SqSection section) { 
			if (section != null && super.accept(section)) { 
				String name = section.getName();
				if (name == null || name.length() == 0 || name.startsWith("."))
					return false;
				
				return false; //section.isDirectory() || (section instanceof FsImage);
			}
			
			return false;
		}
	}
	
	static class VideoFilter extends FileFilter { 
		@Override
		public boolean accept(FileStatus status) { 
			if (super.accept(status)) { 
				String contentType = MimeTypes.getContentTypeByFilename(
						status.getPath().getName());
				
				if (contentType == null) 
					contentType = MimeType.TYPE_APPLICATION.getType();
				
				if (contentType.startsWith("video/"))
					return true;
			}
			
			return false;
		}
	}
	
}
