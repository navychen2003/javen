package org.javenstudio.cocoka.android;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;

public interface ResourceContext {

	public Context getPackageContext(); 
	public Resources getResources(); 
	
	public String getString(int resId); 
	public String[] getStringArray(int resId); 
	public String getQuantityString(int resId, int quantity, Object... formatArgs); 
	public Drawable getDrawable(int resId); 
	public int getColor(int resId); 
	public ColorStateList getColorStateList(int resId); 
	public XmlResourceParser getXml(int resId); 
	
	public Bitmap decodeBitmap(int resId); 
	public View inflateView(int resource, ViewGroup root, boolean attachToRoot); 
	public View inflateView(int resource, ViewGroup root);
	public View findViewById(View view, int resId); 
	public boolean bindView(int viewId, View view, Object... values); 
	
}
