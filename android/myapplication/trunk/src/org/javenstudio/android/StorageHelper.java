package org.javenstudio.android;

import android.graphics.drawable.Drawable;

import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.util.MimeType;
import org.javenstudio.cocoka.util.MimeTypes;

public class StorageHelper {
	
	public static void registerMimeTypes() { 
		{
			MimeTypes.MimeTypeInfo info = MimeTypes.registerMimeType(
					MimeType.TYPE_TEXT.getType(), MimeType.TYPE_TEXT); 
			
			info.setResource(R.drawable.ic_type_txt, 0);
			
			info.addFileType("txt").setResource(R.drawable.ic_type_txt, 0); 
			info.addFileType("xml").setResource(R.drawable.ic_type_xml, 0); 
			info.addFileType("htm").setResource(R.drawable.ic_type_html, 0); 
			info.addFileType("html").setResource(R.drawable.ic_type_html, 0); 
			info.addFileType("php").setResource(R.drawable.ic_type_php, 0);
			info.addFileType("sql").setResource(R.drawable.ic_type_sql, 0);
			info.addFileType("java").setResource(R.drawable.ic_type_java, 0);
			info.addFileType("py").setResource(R.drawable.ic_type_py, 0);
			info.addFileType("rb").setResource(R.drawable.ic_type_rb, 0);
			info.addFileType("asp").setResource(R.drawable.ic_type_asp, 0);
			info.addFileType("yml").setResource(R.drawable.ic_type_yml, 0);
			info.addFileType("c").setResource(R.drawable.ic_type_c, 0);
			info.addFileType("cpp").setResource(R.drawable.ic_type_cpp, 0);
			info.addFileType("h").setResource(R.drawable.ic_type_h, 0);
			info.addFileType("css").setResource(R.drawable.ic_type_css, 0);
		}
		
		{
			MimeTypes.MimeTypeInfo info = MimeTypes.registerMimeType(
					MimeType.TYPE_IMAGE.getType(), MimeType.TYPE_IMAGE); 
			
			//info.setResource(R.drawable.ic_type_img, 0); 
			
			info.addFileType("jpg").setResource(R.drawable.ic_type_jpg, 0); 
			info.addFileType("jpeg").setResource(R.drawable.ic_type_jpg, 0); 
			info.addFileType("png").setResource(R.drawable.ic_type_png, 0); 
			info.addFileType("bmp").setResource(R.drawable.ic_type_bmp, 0); 
			info.addFileType("gif").setResource(R.drawable.ic_type_gif, 0); 
			info.addFileType("tif").setResource(R.drawable.ic_type_tiff, 0); 
			info.addFileType("tiff").setResource(R.drawable.ic_type_tiff, 0); 
		}
		
		{
			MimeTypes.MimeTypeInfo info = MimeTypes.registerMimeType(
					MimeType.TYPE_VIDEO.getType(), MimeType.TYPE_VIDEO); 
			
			//info.setResource(R.drawable.ic_type_avi, 0);
			
			info.addFileType("avi").setResource(R.drawable.ic_type_avi, 0); 
			info.addFileType("mp4").setResource(R.drawable.ic_type_mp4, 0); 
			info.addFileType("wmv"); //.setResource(R.drawable.ic_type_wmv, 0); 
			info.addFileType("rmvb"); //.setResource(R.drawable.ic_type_rmvb, 0); 
			info.addFileType("rm"); //.setResource(R.drawable.ic_type_rm, 0); 
			info.addFileType("mkv"); //.setResource(R.drawable.ic_type_mkv, 0); 
			info.addFileType("mov").setResource(R.drawable.ic_type_mov, 0); 
			info.addFileType("flv").setResource(R.drawable.ic_type_flv, 0); 
			info.addFileType("qt").setResource(R.drawable.ic_type_qt, 0); 
			info.addFileType("m4v").setResource(R.drawable.ic_type_m4v, 0); 
			info.addFileType("mpg").setResource(R.drawable.ic_type_mpg, 0); 
		}
		
		{
			MimeTypes.MimeTypeInfo info = MimeTypes.registerMimeType(
					MimeType.TYPE_AUDIO.getType(), MimeType.TYPE_AUDIO); 
			
			//info.setResource(R.drawable.ic_type_audio, 0); 
			
			info.addFileType("amr"); //.setResource(R.drawable.ic_type_amr, 0); 
			info.addFileType("mp3").setResource(R.drawable.ic_type_mp3, 0); 
			info.addFileType("mid").setResource(R.drawable.ic_type_mid, 0); 
			info.addFileType("aac").setResource(R.drawable.ic_type_aac, 0); 
			info.addFileType("aiff").setResource(R.drawable.ic_type_aiff, 0); 
			info.addFileType("wav").setResource(R.drawable.ic_type_wav, 0); 
			info.addFileType("m4a").setResource(R.drawable.ic_type_m4a, 0);
		}
		
		{
			MimeTypes.MimeTypeInfo info = MimeTypes.registerMimeType(
					MimeType.TYPE_APPLICATION.getType(), MimeType.TYPE_APPLICATION); 
			
			//info.setResource(R.drawable.ic_type_dat, 0); 
			
			info.addFileType("apk"); //.setResource(R.drawable.ic_type_apk, 0);
			info.addFileType("doc").setResource(R.drawable.ic_type_doc, 0);
			info.addFileType("docx").setResource(R.drawable.ic_type_docx, 0);
			info.addFileType("ods").setResource(R.drawable.ic_type_ods, 0);
			info.addFileType("note"); //.setResource(R.drawable.ic_type_nte, 0);
			info.addFileType("pdf").setResource(R.drawable.ic_type_pdf, 0);
			info.addFileType("ppt").setResource(R.drawable.ic_type_ppt, 0);
			info.addFileType("pptx").setResource(R.drawable.ic_type_ppt, 0);
			info.addFileType("dot").setResource(R.drawable.ic_type_dot, 0);
			info.addFileType("dotx").setResource(R.drawable.ic_type_dotx, 0);
			info.addFileType("pps").setResource(R.drawable.ic_type_pps, 0);
			info.addFileType("rtf").setResource(R.drawable.ic_type_rtf, 0);
			info.addFileType("key").setResource(R.drawable.ic_type_key, 0);
			info.addFileType("odp").setResource(R.drawable.ic_type_odp, 0);
			info.addFileType("otp").setResource(R.drawable.ic_type_otp, 0);
			info.addFileType("ott").setResource(R.drawable.ic_type_ott, 0);
			info.addFileType("odt").setResource(R.drawable.ic_type_odt, 0);
			info.addFileType("ots").setResource(R.drawable.ic_type_ots, 0);
			info.addFileType("xls").setResource(R.drawable.ic_type_xls, 0);
			info.addFileType("xlsx").setResource(R.drawable.ic_type_xls, 0);
			info.addFileType("rar").setResource(R.drawable.ic_type_rar, 0);
			info.addFileType("zip").setResource(R.drawable.ic_type_zip, 0);
			info.addFileType("dmg").setResource(R.drawable.ic_type_dmg, 0);
			info.addFileType("tgz").setResource(R.drawable.ic_type_tgz, 0);
			info.addFileType("gz").setResource(R.drawable.ic_type_gz, 0);
			info.addFileType("apk").setResource(R.drawable.ic_type_apk, 0);
			info.addFileType("iso").setResource(R.drawable.ic_type_iso, 0);
			info.addFileType("psd").setResource(R.drawable.ic_type_psd, 0); 
			info.addFileType("tga").setResource(R.drawable.ic_type_tga, 0); 
			info.addFileType("eps").setResource(R.drawable.ic_type_eps, 0); 
			info.addFileType("dxf").setResource(R.drawable.ic_type_dxf, 0); 
			info.addFileType("ai").setResource(R.drawable.ic_type_ai, 0); 
			info.addFileType("dwg").setResource(R.drawable.ic_type_dwg, 0); 
			info.addFileType("dat").setResource(R.drawable.ic_type_dat, 0);
			info.addFileType("exe").setResource(R.drawable.ic_type_exe, 0);
			info.addFileType("ics").setResource(R.drawable.ic_type_ics, 0);
		}
	}
	
	public static Drawable getFileTypeIcon(String filename) {
		return getFileTypeIcon(filename, null, null);
	}
	
	public static Drawable getFileTypeIcon(String filename, 
			String mimetype, String extension) { 
		MimeTypes.FileTypeInfo extInfo = MimeTypes.getExtensionFileType(extension);
		if (extInfo != null) {
			Drawable icon = extInfo.getIconDrawable(); 
			if (icon != null) 
				return icon; 
		}
		
		MimeTypes.FileTypeInfo fileInfo = MimeTypes.lookupFileType(filename); 
		if (fileInfo != null) { 
			Drawable icon = fileInfo.getIconDrawable(); 
			if (icon != null) 
				return icon; 
		}
		
		MimeTypes.MimeTypeInfo mimeInfo = MimeTypes.lookupMimeType(mimetype); 
		if (mimeInfo != null) { 
			Drawable icon = mimeInfo.getIconDrawable(); 
			if (icon != null) 
				return icon; 
		}
		
		return ResourceHelper.getResources().getDrawable(R.drawable.ic_type_blank); 
	}
	
	public static Drawable getFolderTypeIcon(String mimetype) {
		int iconRes = R.drawable.ic_type_folder;
		if (mimetype != null) {
			if (mimetype.indexOf("/x-recycle") >= 0)
				iconRes = R.drawable.ic_type_folder_recycle;
			else if (mimetype.indexOf("/x-share") >= 0)
				iconRes = R.drawable.ic_type_folder_share;
			else if (mimetype.indexOf("/x-upload") >= 0)
				iconRes = R.drawable.ic_type_folder_upload;
		}
		return ResourceHelper.getResources().getDrawable(iconRes);
	}
	
}
