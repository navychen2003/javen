package org.javenstudio.android.data;

import org.javenstudio.android.ActionError;

public interface ReloadCallback {

	public static final String PARAM_QUERYTEXT = "queryText";
	public static final String PARAM_QUERYTAG = "queryTag";
	
	public String getParam(String name);
	public void setParam(String name, String value);
	public void clearParams();
	
	//public void onExceptionCatched(Throwable e);
	public void onActionError(ActionError error);
	public void showContentMessage(CharSequence message);
	public void showProgressDialog(CharSequence message);
	public void hideProgressDialog();
	
	public boolean isActionProcessing();
	
}
