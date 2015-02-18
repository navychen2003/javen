package org.javenstudio.falcon.setting.cluster;

import org.javenstudio.common.util.Logger;

public abstract class AnyboxListener extends AnyboxApi.SecretJSONListener {
	private static final Logger LOG = Logger.getLogger(AnyboxListener.class);

	protected AnyboxData mData = null;
	protected ActionError mError = null;
	
	@Override
	public void handleData(AnyboxData data, ActionError error) {
		mData = data; mError = error;
		if (LOG.isDebugEnabled())
			LOG.debug("handleData: data=" + data);
		
		if (error != null) {
			if (LOG.isWarnEnabled()) {
				LOG.warn("handleData: response error: " + error, 
						error.getException());
			}
		}
	}
	
	public final AnyboxData getData() { return mData; }
	public final ActionError getError() { return mError; }
	

	public static class LoginListener extends AnyboxListener {
		@Override
		public ActionError.Action getErrorAction() {
			return ActionError.Action.LOGIN;
		}
	}
	
	public static class AuthListener extends AnyboxListener {
		@Override
		public ActionError.Action getErrorAction() {
			return ActionError.Action.AUTH;
		}
	}
	
	public static class PongListener extends AnyboxListener {
		@Override
		public ActionError.Action getErrorAction() {
			return ActionError.Action.PONG;
		}
	}
	
	public static class JoinListener extends AnyboxListener {
		@Override
		public ActionError.Action getErrorAction() {
			return ActionError.Action.JOIN;
		}
	}
	
	public static class AttachListener extends AnyboxListener {
		@Override
		public ActionError.Action getErrorAction() {
			return ActionError.Action.ATTACH;
		}
	}
	
	public static class GetUserListener extends AnyboxListener {
		@Override
		public ActionError.Action getErrorAction() {
			return ActionError.Action.GETUSER;
		}
	}
	
	public static class PutUserListener extends AnyboxListener {
		@Override
		public ActionError.Action getErrorAction() {
			return ActionError.Action.PUTUSER;
		}
	}
	
	public static class RmUserListener extends AnyboxListener {
		@Override
		public ActionError.Action getErrorAction() {
			return ActionError.Action.RMUSER;
		}
	}
	
	public static class GetNameListener extends AnyboxListener {
		@Override
		public ActionError.Action getErrorAction() {
			return ActionError.Action.GETNAME;
		}
	}
	
	public static class PutNameListener extends AnyboxListener {
		@Override
		public ActionError.Action getErrorAction() {
			return ActionError.Action.PUTNAME;
		}
	}
	
	public static class RmNameListener extends AnyboxListener {
		@Override
		public ActionError.Action getErrorAction() {
			return ActionError.Action.RMNAME;
		}
	}
	
}
