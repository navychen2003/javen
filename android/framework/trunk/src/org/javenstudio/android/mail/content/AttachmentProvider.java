package org.javenstudio.android.mail.content;

import java.io.IOException;
import java.io.InputStream;

import org.javenstudio.mail.Body;
import org.javenstudio.mail.MessagingException;

import org.javenstudio.cocoka.storage.MediaStorageProvider;
import org.javenstudio.cocoka.storage.Storage;
import org.javenstudio.cocoka.storage.StorageFile;
import org.javenstudio.cocoka.storage.StorageProvider;
import org.javenstudio.cocoka.util.MimeType;
import org.javenstudio.cocoka.util.MimeTypes;
import org.javenstudio.common.util.Logger;

public abstract class AttachmentProvider implements StorageProvider {
	private static Logger LOG = Logger.getLogger(AttachmentProvider.class);

	protected abstract class AttachmentStore extends MediaStorageProvider { 
		public AttachmentStore(Storage store) { 
			super(store); 
		}
		
		protected AttachmentFile newAttachmentFile(StorageFile file, String contentUri) { 
			return new AttachmentStoreFile(this, file, contentUri); 
		}
		
		protected abstract AttachmentFile saveAttachmentFile(
				String emailAddress, InputStream in, String filename, String contentUri) 
				throws IOException; 
	}
	
	protected class AttachmentStoreFile implements AttachmentFile { 
		private final AttachmentStore mStore; 
		private final StorageFile mFile; 
		private final String mContentUri; 
		
		public AttachmentStoreFile(AttachmentStore store, 
				StorageFile file, String contentUri) { 
			mStore = store; 
			mFile = file; 
			mContentUri = contentUri; 
		}
		
		public AttachmentStore getAttachmentStore() { return mStore; }
		public StorageFile getFile() { return mFile; }
		public String getContentUri() { return mContentUri; }
		
		public String getContentType() { return mFile.getContentType(); }
		public long getContentLength() { return mFile.getContentLength(); }
		public String getLocation() { return mFile.getLocation(); }
		public String getFilePath() { return mFile.getFilePath(); }
		public String getFileName() { return mFile.getFileName(); }
		public InputStream openFile() { return mFile.openFile(); }
		
		public boolean deleteFile() { return false; }
	}
	
	protected abstract AttachmentStore getAttachmentStore(); 
	protected abstract String newAttachmentFilename(long accountId, long messageId); 
	protected abstract String getAttachmentUri(long accountId, String filename); 
	
	@Override 
	public Storage getStorage() { 
		return getAttachmentStore().getStorage(); 
	}
	
	public synchronized AttachmentFile openAttachmentFile(AttachmentContent attachment) 
			throws MessagingException { 
		if (attachment != null) { 
			final String contentUri = attachment.getContentUri();
			if (contentUri != null && !contentUri.equals(AttachmentContent.CONTENTURI_NULL)) { 
				String location = contentUri;
				if (location.startsWith("file://")) 
					location = location.substring(7);
				
				try { 
					if (location != null && location.startsWith("/")) 
						return openLocalAttachmentFile(attachment, location);
					
				} catch (IOException e) { 
					throw new MessagingException("cannot open attachment file: "+location, e);
				}
			}
		}
		
		return null;
	}
	
	protected AttachmentFile openLocalAttachmentFile(AttachmentContent attachment, 
			String filepath) throws IOException { 
		if (attachment != null && filepath != null && filepath.length() > 0) { 
			StorageFile file = null; 
			
			MimeTypes.MimeTypeInfo info = MimeTypes.lookupMimeType(
					attachment.getFileName(), attachment.getMimeType());
			if (info != null) { 
				if (info.getType() == MimeType.TYPE_IMAGE) { 
					file = getAttachmentStore().openImageFile(filepath);
				}
			} else { 
				if (LOG.isDebugEnabled()) 
					LOG.debug("unknown attachment file type: "+attachment.getMimeType());
			}
			
			if (file == null)
				file = getAttachmentStore().openFile(filepath);
			
			return getAttachmentStore().newAttachmentFile(file, attachment.getContentUri());
		}
		
		return null;
	}
	
	public synchronized AttachmentFile saveAttachmentFile(MessageContent localMessage, 
			Body body) throws MessagingException { 
		try { 
			return doSaveAttachmentFile(localMessage, body); 
		} catch (IOException ex) { 
			throw new MessagingException("save attachment file failed", ex); 
		}
	}
	
	protected AttachmentFile doSaveAttachmentFile(MessageContent localMessage, 
			Body body) throws MessagingException, IOException { 
		final MailContentProvider provider = MailContentProvider.getInstance(); 
		
		if (localMessage == null || body == null) 
			return null; 
		
		final AccountContent account = provider.queryAccount(localMessage.getAccountKey()); 
    	if (account == null) { 
    		if (LOG.isDebugEnabled()) 
    			LOG.warn("saveAttachmentFile: account: " + localMessage.getAccountKey() + " not found"); 
    		
    		return null; 
    	}
		
    	AttachmentStore store = getAttachmentStore(); 
    	if (store != null) { 
    		String filename = newAttachmentFilename(account.getId(), localMessage.getId()); 
    		String contentUri = getAttachmentUri(account.getId(), filename); 
    		
    		return store.saveAttachmentFile(account.getEmailAddress(), 
    				body.getInputStream(), filename, contentUri); 
    	}
    	
		return null; 
	}
	
}
