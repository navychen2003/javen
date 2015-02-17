package org.javenstudio.android.mail.controller;

import org.javenstudio.android.mail.content.AccountContent;
import org.javenstudio.android.mail.content.AttachmentContent;
import org.javenstudio.android.mail.content.MailboxContent;
import org.javenstudio.android.mail.content.MessageContent;
import org.javenstudio.cocoka.worker.WorkerTask;
import org.javenstudio.common.util.Logger;
import org.javenstudio.mail.Message;

public final class CallbackMessagingListener implements MessagingListener {
	private static Logger LOG = Logger.getLogger(CallbackMessagingListener.class);

	private final MailController mController; 
	private final MessagingCallback mCallback; 
	private final AccountContent mAccount; 
	private final String mCommand; 
	private String mCommandKey = null;
	
	private WorkerTask<?> mWorkerTask = null; 
	private long mMessageId=-1, mMailboxId=-1, mAttachmentId=-1; 
	private int mTotalCount = 0; 
	private int mFinishedCount = 0; 
	private long mLastEventTime = 0;
	
	public CallbackMessagingListener(MailController controller, String command, AccountContent account, 
			MessagingCallback callback) { 
		mController = controller; 
		mCommand = command; 
		mCallback = callback; 
		mAccount = account; 
	}
	
	public final AccountContent getAccount() { 
		return mAccount; 
	}
	
	public final String getCommand() { 
		return mCommand; 
	}
	
	void setWorkerTask(WorkerTask<?> worker) { 
		mWorkerTask = worker; 
	}
	
	public boolean isRequestStop() { 
		WorkerTask<?> worker = mWorkerTask; 
		return worker != null && worker.isRequestStop(); 
	}
	
	void setMessageId(long id) { mMessageId = id; }
	public final long getMessageId() { return mMessageId; }
	
	void setMailboxId(long id) { mMailboxId = id; }
	public final long getMailboxId() { return mMailboxId; }
	
	void setAttachmentId(long id) { mAttachmentId = id; }
	public final long getAttachmentId() { return mAttachmentId; }
	
	void setTotalCount(int count) { mTotalCount = count; }
	public final int getTotalCount() { return mTotalCount; }
	
	void setFinishedCount(int count) { mFinishedCount = count; }
	void addFinishedCount(int count) { mFinishedCount += count; }
	public final int getFinishedCount() { return mFinishedCount; }
	
	void setCommandKey(String key) { mCommandKey = key; }
	public final String getCommandKey() { 
		String key = mCommandKey; 
		if (key == null || key.length() == 0) { 
			key = mCommand + ":account=" + mAccount.getId() + ":mailbox=" + mMailboxId 
				+ ":message=" + mMessageId + ":attachment=" + mAttachmentId;
			mCommandKey = key;
		}
		return key;
	}
	
	public final long getLastEventTime() { 
		return mLastEventTime;
	}
	
	@Override 
	public final void onMessagingEvent(MessagingEvent event) { 
		MessagingCallback callback = mCallback; 
		if (callback != null && event != null) 
			callback.onMessagingEvent(event); 
		
		mController.getMessagingQueue().notifyEvent(event); 
		mLastEventTime = System.currentTimeMillis();
	}
	
	public final void callbackStarted() { 
		onMessagingEvent(new MessagingEvent.CommandEvent(MessagingEvent.ACTION_STARTED, 
				getAccount(), getCommand())); 
	}
	
	public final void callbackFinished() { 
		onMessagingEvent(new MessagingEvent.CommandEvent(MessagingEvent.ACTION_FINISHED, 
				getAccount(), getCommand())); 
	}
	
	public final void callbackException(Throwable exception) { 
		if (exception != null) 
			LOG.error("messaging task catched exception: "+exception, exception); 
		
		onMessagingEvent(new MessagingEvent.ExceptionEvent(MessagingEvent.ACTION_EXCEPTION, 
				getAccount(), getCommand(), exception)); 
	}
	
	
	public final void callbackSynchronizeMailboxStarted(MailboxContent folder) { 
		onMessagingEvent(new MessagingEvent.SynchronizeMailboxEvent(MessagingEvent.ACTION_STARTED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_DONE, null, folder, 0, 0));
	}
	
	public final void callbackSynchronizeMailboxFinished(MailboxContent folder, int totalMessages, int newMessages) { 
		onMessagingEvent(new MessagingEvent.SynchronizeMailboxEvent(MessagingEvent.ACTION_FINISHED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_DONE, null, folder, totalMessages, newMessages));
	}
	
	
	public final void callbackSynchronizeFoldersStarted() { 
		onMessagingEvent(new MessagingEvent.SynchronizeFoldersEvent(MessagingEvent.ACTION_STARTED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_DONE, null, 0));
	}
	
	public final void callbackSynchronizeFoldersFinished(int newFolders) { 
		onMessagingEvent(new MessagingEvent.SynchronizeFoldersEvent(MessagingEvent.ACTION_FINISHED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_DONE, null, newFolders));
	}
	
	public final void callbackSynchronizeFoldersFailed(int newFolders, Throwable exception) { 
		onMessagingEvent(new MessagingEvent.SynchronizeFoldersEvent(MessagingEvent.ACTION_FINISHED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_EXCEPTION, exception, newFolders));
	}
	
	
	public final void callbackSynchronizeActionsStarted() { 
		onMessagingEvent(new MessagingEvent.SynchronizeActionsEvent(MessagingEvent.ACTION_STARTED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_DONE, null));
	}
	
	public final void callbackSynchronizeActionsFinished() { 
		onMessagingEvent(new MessagingEvent.SynchronizeActionsEvent(MessagingEvent.ACTION_FINISHED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_DONE, null));
	}
	
	public final void callbackSynchronizeActionsFailed(Throwable exception) { 
		onMessagingEvent(new MessagingEvent.SynchronizeActionsEvent(MessagingEvent.ACTION_FINISHED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_EXCEPTION, exception));
	}
	
	
	public final void callbackProcessPendingDeletesStarted() { 
		onMessagingEvent(new MessagingEvent.ProcessPendingDeletesEvent(MessagingEvent.ACTION_STARTED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_DONE, null));
	}
	
	public final void callbackProcessPendingDeletesFinished() { 
		onMessagingEvent(new MessagingEvent.ProcessPendingDeletesEvent(MessagingEvent.ACTION_FINISHED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_DONE, null));
	}
	
	public final void callbackProcessPendingDeletesFailed(Throwable exception) { 
		onMessagingEvent(new MessagingEvent.ProcessPendingDeletesEvent(MessagingEvent.ACTION_FINISHED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_EXCEPTION, exception));
	}
	
	
	public final void callbackProcessPendingUploadsStarted() { 
		onMessagingEvent(new MessagingEvent.ProcessPendingUploadsEvent(MessagingEvent.ACTION_STARTED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_DONE, null));
	}
	
	public final void callbackProcessPendingUploadsFinished() { 
		onMessagingEvent(new MessagingEvent.ProcessPendingUploadsEvent(MessagingEvent.ACTION_FINISHED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_DONE, null));
	}
	
	public final void callbackProcessPendingUploadsFailed(Throwable exception) { 
		onMessagingEvent(new MessagingEvent.ProcessPendingUploadsEvent(MessagingEvent.ACTION_FINISHED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_EXCEPTION, exception));
	}
	
	
	public final void callbackProcessPendingUpdatesStarted() { 
		onMessagingEvent(new MessagingEvent.ProcessPendingUpdatesEvent(MessagingEvent.ACTION_STARTED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_DONE, null));
	}
	
	public final void callbackProcessPendingUpdatesFinished() { 
		onMessagingEvent(new MessagingEvent.ProcessPendingUpdatesEvent(MessagingEvent.ACTION_FINISHED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_DONE, null));
	}
	
	public final void callbackProcessPendingUpdatesFailed(Throwable exception) { 
		onMessagingEvent(new MessagingEvent.ProcessPendingUpdatesEvent(MessagingEvent.ACTION_FINISHED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_EXCEPTION, exception));
	}
	
	
	public final void callbackUpdateMessageFlagStarted(long messageId, boolean read, boolean favorite) { 
		onMessagingEvent(new MessagingEvent.UpdateMessageFlagEvent(MessagingEvent.ACTION_STARTED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_DONE, null, messageId, read, favorite, 
				getTotalCount(), getFinishedCount()));
	}
	
	public final void callbackUpdateMessageFlagFinished(long messageId, boolean read, boolean favorite) { 
		onMessagingEvent(new MessagingEvent.UpdateMessageFlagEvent(MessagingEvent.ACTION_FINISHED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_DONE, null, messageId, read, favorite, 
				getTotalCount(), getFinishedCount()));
	}
	
	public final void callbackUpdateMessageFlagFailed(long messageId, boolean read, boolean favorite, Throwable exception) { 
		onMessagingEvent(new MessagingEvent.UpdateMessageFlagEvent(MessagingEvent.ACTION_FINISHED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_EXCEPTION, exception, messageId, read, favorite, 
				getTotalCount(), getFinishedCount()));
	}
	
	
	public final void callbackMoveMessageFolderStarted(long messageId, long oldmailboxId, long newmailboxId) { 
		onMessagingEvent(new MessagingEvent.MoveMessageFolderEvent(MessagingEvent.ACTION_STARTED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_DONE, null, messageId, oldmailboxId, newmailboxId, 
				getTotalCount(), getFinishedCount()));
	}
	
	public final void callbackMoveMessageFolderFinished(long messageId, long oldmailboxId, long newmailboxId) { 
		onMessagingEvent(new MessagingEvent.MoveMessageFolderEvent(MessagingEvent.ACTION_FINISHED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_DONE, null, messageId, oldmailboxId, newmailboxId, 
				getTotalCount(), getFinishedCount()));
	}
	
	public final void callbackMoveMessageFolderFailed(long messageId, long oldmailboxId, long newmailboxId, Throwable exception) { 
		onMessagingEvent(new MessagingEvent.MoveMessageFolderEvent(MessagingEvent.ACTION_FINISHED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_EXCEPTION, exception, messageId, oldmailboxId, newmailboxId, 
				getTotalCount(), getFinishedCount()));
	}
	
	
	public final void callbackDeleteMessageStarted(long messageId) { 
		onMessagingEvent(new MessagingEvent.DeleteMessageEvent(MessagingEvent.ACTION_STARTED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_DONE, null, messageId, 
				getTotalCount(), getFinishedCount()));
	}
	
	public final void callbackDeleteMessageFinished(long messageId) { 
		onMessagingEvent(new MessagingEvent.DeleteMessageEvent(MessagingEvent.ACTION_FINISHED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_DONE, null, messageId, 
				getTotalCount(), getFinishedCount()));
	}
	
	public final void callbackDeleteMessageFailed(long messageId, Throwable exception) { 
		onMessagingEvent(new MessagingEvent.DeleteMessageEvent(MessagingEvent.ACTION_FINISHED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_EXCEPTION, exception, messageId, 
				getTotalCount(), getFinishedCount()));
	}
	
	
	public final void callbackMoveMessageToTrashStarted(long messageId, long oldmailboxId, long newmailboxId) { 
		onMessagingEvent(new MessagingEvent.MoveMessageToTrashEvent(MessagingEvent.ACTION_STARTED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_DONE, null, messageId, oldmailboxId, newmailboxId, 
				getTotalCount(), getFinishedCount()));
	}
	
	public final void callbackMoveMessageToTrashFinished(long messageId, long oldmailboxId, long newmailboxId) { 
		onMessagingEvent(new MessagingEvent.MoveMessageToTrashEvent(MessagingEvent.ACTION_FINISHED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_DONE, null, messageId, oldmailboxId, newmailboxId, 
				getTotalCount(), getFinishedCount()));
	}
	
	public final void callbackMoveMessageToTrashFailed(long messageId, long oldmailboxId, long newmailboxId, Throwable exception) { 
		onMessagingEvent(new MessagingEvent.MoveMessageToTrashEvent(MessagingEvent.ACTION_FINISHED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_EXCEPTION, exception, messageId, oldmailboxId, newmailboxId, 
				getTotalCount(), getFinishedCount()));
	}
	
	
	public final void callbackFetchMessageBodyStarted(long messageId) { 
		onMessagingEvent(new MessagingEvent.FetchMessageBodyEvent(MessagingEvent.ACTION_STARTED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_DONE, null, messageId, 
				getTotalCount(), getFinishedCount()));
	}
	
	public final void callbackFetchMessageBodyFinished(long messageId) { 
		onMessagingEvent(new MessagingEvent.FetchMessageBodyEvent(MessagingEvent.ACTION_FINISHED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_DONE, null, messageId, 
				getTotalCount(), getFinishedCount()));
	}
	
	public final void callbackFetchMessageBodyFinished(long messageId, int resultCode) { 
		onMessagingEvent(new MessagingEvent.FetchMessageBodyEvent(MessagingEvent.ACTION_FINISHED, 
				getAccount(), getCommand(), resultCode, null, messageId, 
				getTotalCount(), getFinishedCount()));
	}
	
	public final void callbackFetchMessageBodyFailed(long messageId, int resultCode, Throwable exception) { 
		onMessagingEvent(new MessagingEvent.FetchMessageBodyEvent(MessagingEvent.ACTION_FAILED, 
				getAccount(), getCommand(), resultCode, exception, messageId, 
				getTotalCount(), getFinishedCount()));
	}
	
	
	public final void callbackFetchMessageAttachmentStarted(long messageId, long attachmentId, AttachmentContent attachment) { 
		onMessagingEvent(new MessagingEvent.FetchMessageAttachmentEvent(MessagingEvent.ACTION_STARTED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_DONE, null, messageId, attachmentId, attachment, 
				getTotalCount(), getFinishedCount()));
	}
	
	public final void callbackFetchMessageAttachmentFinished(long messageId, long attachmentId, AttachmentContent attachment) { 
		onMessagingEvent(new MessagingEvent.FetchMessageAttachmentEvent(MessagingEvent.ACTION_FINISHED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_DONE, null, messageId, attachmentId, attachment, 
				getTotalCount(), getFinishedCount()));
	}
	
	public final void callbackFetchMessageAttachmentFinished(long messageId, long attachmentId, AttachmentContent attachment, int resultCode) { 
		onMessagingEvent(new MessagingEvent.FetchMessageAttachmentEvent(MessagingEvent.ACTION_FINISHED, 
				getAccount(), getCommand(), resultCode, null, messageId, attachmentId, attachment, 
				getTotalCount(), getFinishedCount()));
	}
	
	public final void callbackFetchMessageAttachmentFailed(long messageId, long attachmentId, AttachmentContent attachment, int resultCode, Throwable exception) { 
		onMessagingEvent(new MessagingEvent.FetchMessageAttachmentEvent(MessagingEvent.ACTION_FAILED, 
				getAccount(), getCommand(), resultCode, exception, messageId, attachmentId, attachment, 
				getTotalCount(), getFinishedCount()));
	}
	
	
	public final void callbackUpdateMessageFieldsStarted(MessageContent localMessage, Message remoteMessage) { 
		onMessagingEvent(new MessagingEvent.UpdateMessageFieldsEvent(MessagingEvent.ACTION_STARTED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_DONE, null, localMessage, remoteMessage, 
				getTotalCount(), getFinishedCount()));
	}
	
	public final void callbackUpdateMessageFieldsFinished(MessageContent localMessage, Message remoteMessage) { 
		onMessagingEvent(new MessagingEvent.UpdateMessageFieldsEvent(MessagingEvent.ACTION_FINISHED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_DONE, null, localMessage, remoteMessage, 
				getTotalCount(), getFinishedCount()));
	}
	
	public final void callbackUpdateMessageFieldsFailed(MessageContent localMessage, Message remoteMessage, Throwable exception) { 
		onMessagingEvent(new MessagingEvent.UpdateMessageFieldsEvent(MessagingEvent.ACTION_FAILED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_EXCEPTION, exception, localMessage, remoteMessage, 
				getTotalCount(), getFinishedCount()));
	}
	
	
	public final void callbackCopyOneMessageStarted(MailboxContent localFolder, Message remoteMessage) { 
		onMessagingEvent(new MessagingEvent.CopyOneMessageEvent(MessagingEvent.ACTION_STARTED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_DONE, null, localFolder, remoteMessage, 
				getTotalCount(), getFinishedCount()));
	}
	
	public final void callbackCopyOneMessageFinished(MailboxContent localFolder, Message remoteMessage) { 
		onMessagingEvent(new MessagingEvent.CopyOneMessageEvent(MessagingEvent.ACTION_FINISHED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_DONE, null, localFolder, remoteMessage, 
				getTotalCount(), getFinishedCount()));
	}
	
	public final void callbackCopyOneMessageFailed(MailboxContent localFolder, Message remoteMessage, Throwable exception) { 
		onMessagingEvent(new MessagingEvent.CopyOneMessageEvent(MessagingEvent.ACTION_FAILED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_EXCEPTION, exception, localFolder, remoteMessage, 
				getTotalCount(), getFinishedCount()));
	}
	
	
	public final void callbackAttachmentDownloadBytes(MessageContent localMessage, AttachmentContent localAttachment, long inputBytes, long outputBytes) { 
		onMessagingEvent(new MessagingEvent.AttachmentDownloadEvent(getAccount(), localMessage, localAttachment, 
				inputBytes, outputBytes)); 
	}
	
	
	public final void callbackSendMessageStarted(long messageId) { 
		onMessagingEvent(new MessagingEvent.SendMessageEvent(MessagingEvent.ACTION_STARTED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_DONE, null, messageId, 
				getTotalCount(), getFinishedCount()));
	}
	
	public final void callbackSendMessageFinished(long messageId) { 
		onMessagingEvent(new MessagingEvent.SendMessageEvent(MessagingEvent.ACTION_FINISHED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_DONE, null, messageId, 
				getTotalCount(), getFinishedCount()));
	}
	
	public final void callbackSendMessageFinished(long messageId, int resultCode) { 
		onMessagingEvent(new MessagingEvent.SendMessageEvent(MessagingEvent.ACTION_FINISHED, 
				getAccount(), getCommand(), resultCode, null, messageId, 
				getTotalCount(), getFinishedCount()));
	}
	
	public final void callbackSendMessageFailed(long messageId, int resultCode, Throwable exception) { 
		onMessagingEvent(new MessagingEvent.SendMessageEvent(MessagingEvent.ACTION_FAILED, 
				getAccount(), getCommand(), resultCode, exception, messageId, 
				getTotalCount(), getFinishedCount()));
	}
	
	
	public final void callbackUploadMessageStarted(long messageId) { 
		onMessagingEvent(new MessagingEvent.UploadMessageEvent(MessagingEvent.ACTION_STARTED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_DONE, null, messageId, 
				getTotalCount(), getFinishedCount()));
	}
	
	public final void callbackUploadMessageFinished(long messageId) { 
		onMessagingEvent(new MessagingEvent.UploadMessageEvent(MessagingEvent.ACTION_FINISHED, 
				getAccount(), getCommand(), MessagingEvent.RESULT_DONE, null, messageId, 
				getTotalCount(), getFinishedCount()));
	}
	
	public final void callbackUploadMessageFinished(long messageId, int resultCode) { 
		onMessagingEvent(new MessagingEvent.UploadMessageEvent(MessagingEvent.ACTION_FINISHED, 
				getAccount(), getCommand(), resultCode, null, messageId, 
				getTotalCount(), getFinishedCount()));
	}
	
	public final void callbackUploadMessageFailed(long messageId, int resultCode, Throwable exception) { 
		onMessagingEvent(new MessagingEvent.UploadMessageEvent(MessagingEvent.ACTION_FAILED, 
				getAccount(), getCommand(), resultCode, exception, messageId, 
				getTotalCount(), getFinishedCount()));
	}
	
}
