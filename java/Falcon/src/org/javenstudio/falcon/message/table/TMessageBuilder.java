package org.javenstudio.falcon.message.table;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.message.IMessage;
import org.javenstudio.falcon.message.IMessage.Builder;
import org.javenstudio.falcon.message.MessageHelper;

final class TMessageBuilder implements IMessage.Builder {

	private final TMessage mMessage;
	
	public TMessageBuilder(TMessage message) {
		if (message == null) throw new NullPointerException();
		mMessage = message;
	}
	
	@Override
	public Builder setAccount(String val) {
		mMessage.setAccount(val);
		return this;
	}
	
	@Override
	public Builder setMessageType(String val) {
		mMessage.setMessageType(val);
		return this;
	}

	@Override
	public Builder setMessageTime(long val) {
		if (val <= 0) { 
			throw new IllegalArgumentException(
					"Message time: " + val + " is wrong");
		}
		mMessage.setMessageTime(val);
		return this;
	}
	
	@Override
	public Builder setUpdateTime(long val) {
		if (val <= 0) {
			throw new IllegalArgumentException(
					"Update time: " + val + " is wrong");
		}
		mMessage.setUpdateTime(val);
		return this;
	}
	
	@Override
	public Builder setFlag(String val) {
		if (val != null && val.length() > 0) {
			if (!IMessage.Util.hasFlag(val)) {
				throw new IllegalArgumentException(
						"Message flag: " + val + " is wrong");
			}
		}
		mMessage.setFlag(val);
		return this;
	}

	@Override
	public Builder setStatus(String val) {
		if (!IMessage.Util.hasStatus(val)) {
			throw new IllegalArgumentException(
					"Message status: " + val + " is wrong");
		}
		mMessage.setStatus(val);
		return this;
	}

	@Override
	public Builder setFolder(String val) {
		if (!mMessage.getService().hasFolderName(val)) {
			throw new IllegalArgumentException(
					"Message folder: " + val + " not found");
		}
		mMessage.setFolder(val);
		return this;
	}
	
	@Override
	public Builder setFolderFrom(String val) {
		if (val != null && val.length() > 0) {
			if (!mMessage.getService().hasFolderName(val)) {
				throw new IllegalArgumentException(
						"Message folder: " + val + " not found");
			}
		}
		mMessage.setFolderFrom(val);
		return this;
	}

	@Override
	public Builder setStreamId(String val) {
		if (val != null && val.length() > 0) {
			if (!MessageHelper.isStreamKeyOkay(val)) {
				throw new IllegalArgumentException(
						"Message streamid: " + val + " is wrong");
			}
		}
		mMessage.setStreamId(val);
		return this;
	}

	@Override
	public Builder setReplyId(String val) {
		mMessage.setReplyId(val);
		return this;
	}

	@Override
	public Builder setFrom(String val) {
		if (val == null || val.length() == 0) {
			throw new IllegalArgumentException(
					"Message From cannot be empty");
		}
		mMessage.setFrom(val);
		return this;
	}

	@Override
	public Builder setTo(String val) {
		//if (val == null || val.length() == 0)
		//	throw new IllegalArgumentException("Message To cannot be empty");
		mMessage.setTo(val);
		return this;
	}

	@Override
	public Builder setCc(String val) {
		mMessage.setCc(val);
		return this;
	}

	@Override
	public Builder setBcc(String val) {
		mMessage.setBcc(val);
		return this;
	}

	@Override
	public Builder setReplyTo(String val) {
		mMessage.setReplyTo(val);
		return this;
	}

	@Override
	public Builder setHeaderLines(String val) {
		mMessage.setHeaderLines(val);
		return this;
	}
	
	@Override
	public Builder setSubject(String val) {
		mMessage.setSubject(val);
		return this;
	}

	@Override
	public Builder setContentType(String val) {
		mMessage.setContentType(val);
		return this;
	}

	@Override
	public Builder setBody(String val) {
		mMessage.setBody(val);
		return this;
	}

	@Override
	public Builder setSourceFile(String val) {
		mMessage.setSourceFile(val);
		return this;
	}
	
	@Override
	public Builder setAttachmentFiles(String[] vals) {
		mMessage.setAttachmentFiles(vals);
		return this;
	}
	
	@Override
	public IMessage save() throws ErrorException {
		return mMessage.getService().getCache().saveMessage(mMessage);
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "{message=" + mMessage + "}";
	}

}
