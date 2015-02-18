package org.javenstudio.provider.library;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.image.FileInfo;
import org.javenstudio.cocoka.storage.fs.IFile;

public class SectionHelper {

	public static String getFolderModifiedInfo(long modifiedTime) {
		if (modifiedTime <= 50000000000L) return null;
		String timeAgo = AppResources.getInstance().formatTimeAgo(System.currentTimeMillis() 
				- modifiedTime);
		int infoRes = AppResources.getInstance().getStringRes(AppResources.string.section_modified_information_label);
		if (infoRes == 0) infoRes = R.string.modified_information_label;
		String text = AppResources.getInstance().getResources().getString(infoRes);
		return String.format(text, timeAgo);
	}
	
	public static String getFolderCountModifiedInfo(ISectionFolder data) {
		if (data == null) return null;
		String text = null;
		int subcount = data.getSubCount();
		long sublen = data.getSubLength();
		String timeAgo = AppResources.getInstance().formatTimeAgo(System.currentTimeMillis() 
				- data.getModifiedTime());
		if (subcount > 0 || sublen > 0) {
			String sizeInfo = AppResources.getInstance().formatReadableBytes(sublen);
			int infoRes = AppResources.getInstance().getStringRes(AppResources.string.section_folder_information_label);
			if (infoRes == 0) infoRes = R.string.folder_information_label;
			text = AppResources.getInstance().getResources().getString(infoRes);
			text = String.format(text, ""+subcount, sizeInfo, timeAgo);
		} else {
			int infoRes = AppResources.getInstance().getStringRes(AppResources.string.section_modified_information_label);
			if (infoRes == 0) infoRes = R.string.modified_information_label;
			text = AppResources.getInstance().getResources().getString(infoRes);
			text = String.format(text, timeAgo);
		}
		return text;
	}
	
	public static String getFolderCountInfo(ISectionFolder data) {
		if (data == null) return null;
		return getFolderCountInfo(data.getSubCount(), data.getSubLength());
	}
	
	public static String getFolderCountInfo(int subcount, long sublen) {
		String text = null;
		if (subcount > 0 || sublen > 0) {
			String sizeInfo = AppResources.getInstance().formatReadableBytes(sublen);
			int infoRes = AppResources.getInstance().getStringRes(AppResources.string.section_folder_count_information_label);
			if (infoRes == 0) infoRes = R.string.folder_count_information_label;
			text = AppResources.getInstance().getResources().getString(infoRes);
			text = String.format(text, ""+subcount, sizeInfo);
		} else {
			int infoRes = AppResources.getInstance().getStringRes(AppResources.string.section_folder_empty_information_label);
			if (infoRes == 0) infoRes = R.string.folder_empty_information_label;
			text = AppResources.getInstance().getResources().getString(infoRes);
		}
		return text;
	}
	
	public static String getFileSizeInfo(ISectionData data) {
		if (data == null) return null;
		String sizeInfo = data.getSizeInfo();
		String timeAgo = AppResources.getInstance().formatTimeAgo(System.currentTimeMillis() 
				- data.getModifiedTime());
		int infoRes = AppResources.getInstance().getStringRes(AppResources.string.section_file_information_label);
		if (infoRes == 0) infoRes = R.string.file_information_label;
		String text = AppResources.getInstance().getResources().getString(infoRes);
		return String.format(text, sizeInfo, timeAgo);
	}
	
	public static String getFileSizeInfo(IFile data) {
		if (data == null) return null;
		String sizeInfo = AppResources.getInstance().formatReadableBytes(data.length());
		String timeAgo = AppResources.getInstance().formatTimeAgo(System.currentTimeMillis() 
				- data.lastModified());
		int infoRes = AppResources.getInstance().getStringRes(AppResources.string.section_file_information_label);
		if (infoRes == 0) infoRes = R.string.file_information_label;
		String text = AppResources.getInstance().getResources().getString(infoRes);
		return String.format(text, sizeInfo, timeAgo);
	}
	
	public static String getFileSizeInfo(FileInfo data, String timeAgo) {
		if (data == null) return null;
		String sizeInfo = AppResources.getInstance().formatReadableBytes(data.getFileLength());
		if (timeAgo == null) {
			timeAgo = AppResources.getInstance().formatTimeAgo(System.currentTimeMillis() 
				- data.getModifiedTime());
		}
		int infoRes = AppResources.getInstance().getStringRes(AppResources.string.section_file_information_label);
		if (infoRes == 0) infoRes = R.string.file_information_label;
		String text = AppResources.getInstance().getResources().getString(infoRes);
		return String.format(text, sizeInfo, timeAgo);
	}
	
}
