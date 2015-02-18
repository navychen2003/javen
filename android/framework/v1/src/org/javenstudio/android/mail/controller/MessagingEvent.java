package org.javenstudio.android.mail.controller;

import org.javenstudio.android.mail.content.AccountContent;
import org.javenstudio.android.mail.content.AttachmentContent;
import org.javenstudio.android.mail.content.MailboxContent;
import org.javenstudio.android.mail.content.MessageContent;
import org.javenstudio.mail.Message;

public interface MessagingEvent {

	public static final int ACTION_EXCEPTION = -1; 
	public static final int ACTION_STARTED = 1; 
	public static final int ACTION_FINISHED = 2; 
	public static final int ACTION_FAILED = 3; 
	
	public static final int RESULT_DONE = 1; 
	public static final int RESULT_COMPLETE = 2; 
	public static final int RESULT_NOTFOUND = 3; 
	public static final int RESULT_EXCEPTION = 4; 
	public static final int RESULT_ERROR = 5; 

	public static class InputOutputEvent implements MessagingEvent { 
		private final AccountContent mAccount; 
		private final long mInputBytes; 
		private final long mOutputBytes; 
		
		public InputOutputEvent(AccountContent account, long inputBytes, long outputBytes) { 
			mAccount = account; 
			mInputBytes = inputBytes; 
			mOutputBytes = outputBytes; 
		}
		
		public final AccountContent getAccount() { return mAccount; }
		public final long getInputBytes() { return mInputBytes; }
		public final long getOutputBytes() { return mOutputBytes; }
	}
	
	public static class AttachmentDownloadEvent extends InputOutputEvent { 
		private final MessageContent mMessage; 
		private final AttachmentContent mAttachment; 
		
		public AttachmentDownloadEvent(AccountContent account, MessageContent message, AttachmentContent attachment, 
				long inputBytes, long outputBytes) { 
			super(account, inputBytes, outputBytes); 
			mMessage = message; 
			mAttachment = attachment; 
		}
		
		public final MessageContent getMessage() { return mMessage; }
		public final AttachmentContent getAttachment() { return mAttachment; }
	}
	
	public static class ActionEvent implements MessagingEvent { 
		private final int mAction; 
		private final AccountContent mAccount; 
		private final String mCommand; 
		private final int mResultCode; 
		private final Throwable mException; 
		private final int mTotalCount; 
		private final int mFinishedCount; 
		
		public ActionEvent(int action, AccountContent account, String command) { 
			this(action, account, command, RESULT_DONE, null, 0, 0); 
		}
		
		public ActionEvent(int action, AccountContent account, String command, int resultCode, Throwable exception, int totalCount, int finishedCount) { 
			mAction = action; 
			mAccount = account; 
			mCommand = command; 
			mResultCode = resultCode; 
			mException = exception; 
			mTotalCount = totalCount; 
			mFinishedCount = finishedCount; 
		}
		
		public final int getAction() { return mAction; }
		public final AccountContent getAccount() { return mAccount; }
		public final String getCommand() { return mCommand; }
		public final int getResultCode() { return mResultCode; }
		public final Throwable getException() { return mException; }
		public final int getTotalMessageCount() { return mTotalCount; }
		public final int getFinishedMessageCount() { return mFinishedCount; }
		
		@Override 
		public String toString() { 
			StringBuilder sbuf = new StringBuilder(); 
			
			sbuf.append(getClass().getSimpleName()); 
			sbuf.append("(action="); 
			sbuf.append(getAction()); 
			sbuf.append(" command="); 
			sbuf.append(getCommand()); 
			sbuf.append(" result="); 
			sbuf.append(getResultCode()); 
			sbuf.append(" exception="); 
			sbuf.append(getException()); 
			sbuf.append(")"); 
			
			return sbuf.toString(); 
		}
	}
	
	public static class CommandEvent extends ActionEvent { 
		public CommandEvent(int action, AccountContent account, String command) { 
			super(action, account, command); 
		}
	}
	
	public static class ExceptionEvent extends ActionEvent { 
		public ExceptionEvent(int action, AccountContent account, String command, Throwable exception) { 
			super(action, account, command, RESULT_EXCEPTION, exception, 0, 0); 
		}
	}
	
	public static class SynchronizeMailboxEvent extends ActionEvent { 
		private final MailboxContent mFolder; 
		private final int mTotalMessages; 
		private final int mNewMessages; 
		
		public SynchronizeMailboxEvent(int action, AccountContent account, String command, 
				int resultCode, Throwable exception, MailboxContent folder, int totalMessages, int newMessages) { 
			super(action, account, command, resultCode, exception, 0, 0); 
			mFolder = folder; 
			mTotalMessages = totalMessages; 
			mNewMessages = newMessages; 
		}
		
		public final MailboxContent getMailbox() { return mFolder; }
		public final int getTotalMessages() { return mTotalMessages; }
		public final int getNewMessages() { return mNewMessages; }
	}
	
	public static class SynchronizeFoldersEvent extends ActionEvent { 
		private final int mNewFolders; 
		
		public SynchronizeFoldersEvent(int action, AccountContent account, String command, 
				int resultCode, Throwable exception, int newFolders) { 
			super(action, account, command, resultCode, exception, 0, 0); 
			mNewFolders = newFolders; 
		}
		
		public final int getNewFolders() { return mNewFolders; }
	}
	
	public static class SynchronizeActionsEvent extends ActionEvent { 
		public SynchronizeActionsEvent(int action, AccountContent account, String command, 
				int resultCode, Throwable exception) { 
			super(action, account, command, resultCode, exception, 0, 0); 
		}
	}
	
	public static class ProcessPendingDeletesEvent extends ActionEvent { 
		public ProcessPendingDeletesEvent(int action, AccountContent account, String command, 
				int resultCode, Throwable exception) { 
			super(action, account, command, resultCode, exception, 0, 0); 
		}
	}
	
	public static class ProcessPendingUploadsEvent extends ActionEvent { 
		public ProcessPendingUploadsEvent(int action, AccountContent account, String command, 
				int resultCode, Throwable exception) { 
			super(action, account, command, resultCode, exception, 0, 0); 
		}
	}
	
	public static class ProcessPendingUpdatesEvent extends ActionEvent { 
		public ProcessPendingUpdatesEvent(int action, AccountContent account, String command, 
				int resultCode, Throwable exception) { 
			super(action, account, command, resultCode, exception, 0, 0); 
		}
	}
	
	public static class UpdateMessageFlagEvent extends ActionEvent { 
		private final long mMessageId; 
		private final boolean mFlagRead; 
		private final boolean mFlagFavorite; 
		
		public UpdateMessageFlagEvent(int action, AccountContent account, String command, 
				int resultCode, Throwable exception, long messageId, boolean read, boolean favorite, int totalCount, int finishedCount) { 
			super(action, account, command, resultCode, exception, totalCount, finishedCount); 
			mMessageId = messageId; 
			mFlagRead = read; 
			mFlagFavorite = favorite; 
		}
		
		public final long getMessageId() { return mMessageId; }
		public final boolean getFlagRead() { return mFlagRead; }
		public final boolean getFlagFavorite() { return mFlagFavorite; }
	}
	
	public static class MoveMessageFolderEvent extends ActionEvent { 
		private final long mMessageId; 
		private final long mOldMailboxId; 
		private final long mNewMailboxId; 
		
		public MoveMessageFolderEvent(int action, AccountContent account, String command, 
				int resultCode, Throwable exception, long messageId, long oldmailboxId, long newmailboxId, int totalCount, int finishedCount) { 
			super(action, account, command, resultCode, exception, totalCount, finishedCount); 
			mMessageId = messageId; 
			mOldMailboxId = oldmailboxId; 
			mNewMailboxId = newmailboxId; 
		}
		
		public final long getMessageId() { return mMessageId; }
		public final long getOldMailboxId() { return mOldMailboxId; }
		public final long getNewMailboxId() { return mNewMailboxId; }
	}
	
	public static class MoveMessageToTrashEvent extends MoveMessageFolderEvent { 
		public MoveMessageToTrashEvent(int action, AccountContent account, String command, 
				int resultCode, Throwable exception, long messageId, long oldmailboxId, long newmailboxId, int totalCount, int finishedCount) { 
			super(action, account, command, resultCode, exception, messageId, oldmailboxId, newmailboxId, totalCount, finishedCount); 
		}
	}
	
	public static class DeleteMessageEvent extends ActionEvent { 
		private final long mMessageId; 
		
		public DeleteMessageEvent(int action, AccountContent account, String command, 
				int resultCode, Throwable exception, long messageId, int totalCount, int finishedCount) { 
			super(action, account, command, resultCode, exception, totalCount, finishedCount); 
			mMessageId = messageId; 
		}
		
		public final long getMessageId() { return mMessageId; }
	}
	
	public static class FetchMessageBodyEvent extends ActionEvent { 
		private final long mMessageId; 
		
		public FetchMessageBodyEvent(int action, AccountContent account, String command, 
				int resultCode, Throwable exception, long messageId, int totalCount, int finishedCount) { 
			super(action, account, command, resultCode, exception, totalCount, finishedCount); 
			mMessageId = messageId; 
		}
		
		public final long getMessageId() { return mMessageId; }
	}
	
	public static class FetchMessageAttachmentEvent extends ActionEvent { 
		private final long mMessageId; 
		private final long mAttachmentId; 
		private final AttachmentContent mAttachment;
		
		public FetchMessageAttachmentEvent(int action, AccountContent account, String command, 
				int resultCode, Throwable exception, long messageId, long attachmentId, AttachmentContent attachment, int totalCount, int finishedCount) { 
			super(action, account, command, resultCode, exception, totalCount, finishedCount); 
			mMessageId = messageId; 
			mAttachmentId = attachmentId; 
			mAttachment = attachment; 
		}
		
		public final long getMessageId() { return mMessageId; }
		public final long getAttachmentId() { return mAttachmentId; }
		public final AttachmentContent getAttachment() { return mAttachment; }
	}
	
	public static class UpdateMessageFieldsEvent extends ActionEvent { 
		private final MessageContent mLocalMessage; 
		private final Message mRemoteMessage; 
		
		public UpdateMessageFieldsEvent(int action, AccountContent account, String command, 
				int resultCode, Throwable exception, MessageContent localMessage, Message remoteMessage, int totalCount, int finishedCount) { 
			super(action, account, command, resultCode, exception, totalCount, finishedCount); 
			mLocalMessage = localMessage; 
			mRemoteMessage = remoteMessage; 
		}
		
		public final MessageContent getLocalMessage() { return mLocalMessage; }
		public final Message getRemoteMessage() { return mRemoteMessage; }
	}
	
	public static class CopyOneMessageEvent extends ActionEvent { 
		private final MailboxContent mLocalFolder; 
		private final Message mRemoteMessage; 
		
		public CopyOneMessageEvent(int action, AccountContent account, String command, 
				int resultCode, Throwable exception, MailboxContent localFolder, Message remoteMessage, int totalCount, int finishedCount) { 
			super(action, account, command, resultCode, exception, totalCount, finishedCount); 
			mLocalFolder = localFolder; 
			mRemoteMessage = remoteMessage; 
		}
		
		public final MailboxContent getLocalFolder() { return mLocalFolder; }
		public final Message getRemoteMessage() { return mRemoteMessage; }
	}
	
	public static class SendMessageEvent extends ActionEvent { 
		private final long mMessageId; 
		
		public SendMessageEvent(int action, AccountContent account, String command, 
				int resultCode, Throwable exception, long messageId, int totalCount, int finishedCount) { 
			super(action, account, command, resultCode, exception, totalCount, finishedCount); 
			mMessageId = messageId; 
		}
		
		public final long getMessageId() { return mMessageId; }
	}
	
	public static class UploadMessageEvent extends ActionEvent { 
		private final long mMessageId; 
		
		public UploadMessageEvent(int action, AccountContent account, String command, 
				int resultCode, Throwable exception, long messageId, int totalCount, int finishedCount) { 
			super(action, account, command, resultCode, exception, totalCount, finishedCount); 
			mMessageId = messageId; 
		}
		
		public final long getMessageId() { return mMessageId; }
	}
	
}
