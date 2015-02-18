package org.javenstudio.android;

import java.io.PrintWriter;
import java.io.StringWriter;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.net.Uri;
import android.widget.Toast;

import org.apache.http.HttpStatus;
import org.apache.http.conn.ConnectTimeoutException;
import org.javenstudio.android.app.AlertDialogBuilder;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.DataException;
import org.javenstudio.android.data.media.local.MediaDetails;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.net.http.HttpException;

public class MessageHelper {

	public static void showMessageDetails(final Activity activity, final Object params) {
		if (activity == null || params == null) return;
		if (activity.isDestroyed()) return;
		
		AlertDialogBuilder builder = AppResources.getInstance().createDialogBuilder(activity)
			.setTitle(AppResources.getInstance().getStringRes(AppResources.string.dialog_warning_title))
			.setIcon(AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_dialog_warning))
			.setCancelable(false);
		
		builder.setNegativeButton(R.string.dialog_copy_button, 
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					//dialog.dismiss();
					copyMessageDetails(activity, params);
				}
			});
		
		builder.setPositiveButton(R.string.dialog_ok_button, 
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
		
		CharSequence details = toMessageDetails(params);
		builder.setMessage(details);
		builder.show(activity);
	}
	
	private static void copyMessageDetails(Activity activity, Object params) {
		if (activity == null || params == null) return;
		
		CharSequence details = toMessageDetails(params);
		if (details != null && details.length() > 0) {
			ClipboardManager cmb = (ClipboardManager)activity.getSystemService(Activity.CLIPBOARD_SERVICE);
			if (cmb != null) {
				ClipData data = ClipData.newPlainText(null, details);
				cmb.setPrimaryClip(data);
				
				Toast.makeText(activity, R.string.dialog_copied_trace_message, 
						Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	private static CharSequence toMessageDetails(Object params) {
		if (params == null) return null;
		
		if (params instanceof ActionError) {
			ActionError error = (ActionError)params;
			return toErrorDetails(error);
			
		} else if (params instanceof Throwable) {
			Throwable exception = (Throwable)params;
			return toExceptionDetails(exception);
			
		} else {
			return params.toString();
		}
	}
	
	private static CharSequence toErrorDetails(ActionError error) {
		if (error == null) return null;
		
		String trace = error.getTrace();
		if (trace != null && trace.length() > 0) {
			return trace;
		}
		
		Throwable exception = error.getException();
		if (exception != null) {
			return toExceptionDetails(exception);
		}
		
		String message = error.getMessage();
		if (message != null && message.length() > 0) {
			return message;
		}
		
		return error.toString();
	}
	
	private static CharSequence toExceptionDetails(Throwable exception) {
		if (exception == null) return null;
		
		StringWriter writer = new StringWriter();
		PrintWriter err = new PrintWriter(writer);
		exception.printStackTrace(err);
		err.flush();
		writer.flush();
		
		return writer.toString();
	}
	
	public static boolean isExceptionClass(Throwable exception, Class<?>... clazzs) { 
		if (clazzs != null && exception != null) { 
			Throwable cause = exception.getCause(); 
			
			for (int i=0; i < clazzs.length; i++) { 
				Class<?> clazz = clazzs[i]; 
				
				if (exception.getClass() == clazz) 
					return true;
				
				if (cause != null && cause.getClass() == clazz) 
					return true;
			}
		}
		
		return false;
	}
	
	public static boolean isExceptionClass(Throwable exception, String... classNames) { 
		if (classNames != null && exception != null) { 
			Throwable cause = exception.getCause(); 
			
			for (int i=0; i < classNames.length; i++) { 
				String className = classNames[i]; 
				if (className == null) continue;
				
				if (exception.getClass().getName().indexOf(className) >= 0) 
					return true;
				
				if (cause != null && cause.getClass().getName().indexOf(className) >= 0) 
					return true;
			}
		}
		
		return false;
	}
	
	public static String formatHttpStatus(int statusCode, String message) { 
		final Resources res = ResourceHelper.getResources();
		
		if (statusCode == HttpStatus.SC_NOT_FOUND) {
			return res.getString(R.string.fetch_not_found); 
			
		} else if (statusCode == HttpStatus.SC_FORBIDDEN) {
			return res.getString(R.string.fetch_forbidden); 
			
		} else if (statusCode == HttpStatus.SC_BAD_REQUEST) {
			return res.getString(R.string.fetch_bad_request); 
			
		} else if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
			return res.getString(R.string.fetch_unauthorized); 
			
		} else if (statusCode == HttpStatus.SC_REQUEST_TIMEOUT) {
			return res.getString(R.string.fetch_request_timeout); 
			
		} else if (statusCode == HttpStatus.SC_METHOD_NOT_ALLOWED) {
			return res.getString(R.string.fetch_method_not_allowed); 
			
		} else if (statusCode == HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE) {
			return res.getString(R.string.fetch_unsupported_media_type); 
			
		} else if (statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
			return res.getString(R.string.fetch_internal_server_error); 
			
		} else if (statusCode == HttpStatus.SC_SERVICE_UNAVAILABLE) {
			return res.getString(R.string.fetch_service_unavailable); 
			
		} else if (statusCode == HttpStatus.SC_BAD_GATEWAY) {
			return res.getString(R.string.fetch_bad_gateway); 
			
		} else if (statusCode == HttpStatus.SC_GATEWAY_TIMEOUT) {
			return res.getString(R.string.fetch_gateway_timeout); 
		}
		
		if (message != null && message.indexOf("ConnectTimeoutException") >= 0) {
			return res.getString(R.string.fetch_connect_timeout); 
		}
		
		return null;
	}
	
	public static String formatHttpException(Throwable exception) { 
		if (exception != null) { 
			String location = null; 
			int statusCode = 0; 
			
			if (exception instanceof HttpException) { 
				HttpException exp = (HttpException)exception; 
				location = exp.getLocation();
				statusCode = exp.getStatusCode(); 
			}
			
			Throwable cause = exception.getCause(); 
			if (cause != null) { 
				if (location == null && cause instanceof HttpException) { 
					HttpException exp = (HttpException)cause; 
					location = exp.getLocation();
					statusCode = exp.getStatusCode(); 
					cause = exp.getCause();
				}
			}
			
			if (location != null && location.length() > 0) { 
				String host;
				try {
					Uri uri = Uri.parse(location); 
					host = uri != null ? uri.getHost() : location;
				} catch (Throwable e) { 
					host = location;
				}
				
				final Resources res = ResourceHelper.getResources();
				
				if (statusCode == HttpStatus.SC_NOT_FOUND) {
					return res.getString(R.string.fetch_not_found) 
							+ ": " + location; 
					
				} else if (statusCode == HttpStatus.SC_FORBIDDEN) {
					return res.getString(R.string.fetch_forbidden) 
							+ ": " + location; 
					
				} else if (statusCode == HttpStatus.SC_BAD_REQUEST) {
					return res.getString(R.string.fetch_bad_request) 
							+ ": " + location; 
					
				} else if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
					return res.getString(R.string.fetch_unauthorized) 
							+ ": " + location; 
					
				} else if (statusCode == HttpStatus.SC_REQUEST_TIMEOUT) {
					return res.getString(R.string.fetch_request_timeout) 
							+ ": " + location; 
					
				} else if (statusCode == HttpStatus.SC_METHOD_NOT_ALLOWED) {
					return res.getString(R.string.fetch_method_not_allowed) 
							+ ": " + location; 
					
				} else if (statusCode == HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE) {
					return res.getString(R.string.fetch_unsupported_media_type) 
							+ ": " + location; 
					
				} else if (statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
					return res.getString(R.string.fetch_internal_server_error) 
							+ ": " + location; 
					
				} else if (statusCode == HttpStatus.SC_SERVICE_UNAVAILABLE) {
					return res.getString(R.string.fetch_service_unavailable) 
							+ ": " + location; 
					
				} else if (statusCode == HttpStatus.SC_BAD_GATEWAY) {
					return res.getString(R.string.fetch_bad_gateway) 
							+ ": " + location; 
					
				} else if (statusCode == HttpStatus.SC_GATEWAY_TIMEOUT) {
					return res.getString(R.string.fetch_gateway_timeout) 
							+ ": " + location; 
				}
				
				if (cause != null && isExceptionClass(cause, ConnectTimeoutException.class)) {
					return res.getString(R.string.fetch_connect_timeout) 
							+ ": " + host; 
				}
				
				String code = Integer.toString(statusCode);
				if (statusCode == -1 && cause != null) 
					code = cause.getClass().getSimpleName();
				
				String text = res.getString(R.string.fetch_http_error); 
				return String.format(text, host, code); 
			}
		}
		
		return null;
	}
	
	public static String formatDataException(Throwable exception) { 
		if (exception != null) { 
			//Throwable cause = exception;
			int statusCode = 0; 
			
			if (exception instanceof DataException) { 
				DataException exp = (DataException)exception; 
				statusCode = exp.getStatusCode(); 
			}
			
			if (statusCode == DataException.CODE_ALBUM_NOTEMPTY) { 
				return ResourceHelper.getResources().getString(R.string.album_delete_notempty_error); 
			}
		}
		
		return null;
	}
	
	public static String formatActionError(ActionError error) {
		if (error == null) return null;
		
		//int code = error.getCode();
		String message = error.getMessage();
		Throwable exception = error.getException();
		
		if (message != null && message.length() > 0) {
			String txt = message.toLowerCase();
			
			if (txt.indexOf("not found") >= 0) {
				if (txt.indexOf("user") >= 0)
					message = strings(R.string.error_user_not_found);
			  	else if (txt.indexOf("group") >= 0)
			  		message = strings(R.string.error_group_not_found);
				else if (txt.indexOf("file") >= 0)
					message = strings(R.string.error_file_not_found);
				else if (txt.indexOf("section") >= 0)
					message = strings(R.string.error_file_not_found);
			 	else if (txt.indexOf("item") >= 0)
			 		message = strings(R.string.error_file_not_found);
			   	else if (txt.indexOf("data") >= 0)
			   		message = strings(R.string.error_file_not_found);
			  	else if (txt.indexOf("channel") >= 0)
			  		message = strings(R.string.error_channel_not_found);
				
				return message;
			}
			
			if (exception != null && (message.equals(exception.getMessage()) || 
				message.equals(exception.toString()) || txt.indexOf("exception") >= 0)) {
			} else
				return message;
		}
		
		return formatExceptionObject(exception);
	}
	
	private static String strings(int resId) {
		int stringRes = AppResources.getInstance().getStringRes(resId);
		if (stringRes != 0) return ResourceHelper.getResources().getString(stringRes);
		if (resId != 0) return ResourceHelper.getResources().getString(resId);
		return null;
	}
	
	public static String formatExceptionObject(Object param) { 
		if (param == null) return null; 
		
		if (param instanceof Throwable) { 
			final Throwable exception = (Throwable)param;
			
			Throwable cause = exception;
			String text = formatExceptionOrNull(cause); 
			int deep = 10;
			
			while (text == null && deep > 0) { 
				cause = cause.getCause();
				if (cause == null) break;
				text = formatExceptionOrNull(cause); 
				deep --;
			}
			
			if (text == null) {
				//String message = ResourceHelper.getResources().getString(R.string.report_message_error); 
				//text = String.format(message, param.toString());
				text = ResourceHelper.getResources().getString(R.string.retry_again_message); 
			}
			
			return text;
		}
		
		return null;
	}
	
	public static String formatExceptionOrNull(Throwable e) { 
		if (e == null) return null; 
		
		if (isExceptionClass(e, java.net.SocketException.class, java.net.ConnectException.class)) { 
			return ResourceHelper.getResources().getString(R.string.socket_network_unreachable); 
			
		} else if (isExceptionClass(e, java.net.SocketTimeoutException.class)) { 
			return ResourceHelper.getResources().getString(R.string.socket_connect_timeout); 
			
		} else if (isExceptionClass(e, javax.net.ssl.SSLException.class)) { 
			return ResourceHelper.getResources().getString(R.string.socket_network_error); 
			
		} else if (isExceptionClass(e, java.lang.UnsupportedOperationException.class)) { 
			return ResourceHelper.getResources().getString(R.string.unsupported_operation); 
			
		} else if (isExceptionClass(e, java.lang.OutOfMemoryError.class)) { 
			return ResourceHelper.getResources().getString(R.string.out_of_memory_error); 
			
		} else if (isExceptionClass(e, "HttpException", "HttpHostConnectException")) {
			return ResourceHelper.getResources().getString(R.string.socket_connect_error); 
			
		} else if (isExceptionClass(e, "ParseException")) { 
			return ResourceHelper.getResources().getString(R.string.response_parse_error); 
			
		}
		
		String text = formatDataException(e);
		if (text != null) 
			return text;
		
		text = formatHttpException(e); 
		if (text != null) 
			return text;
		
		return null; 
	}
	
    // Returns a (localized) string for the given duration (in seconds).
    public static String formatDuration(final Context context, int duration) {
        return MediaDetails.formatDuration(context, duration);
    }
	
	//public static String formatMessageTime(long timestamp) {
	//	return TimeResources.getInstance().formatTime(timestamp, TimeResources.SECS_HOUR); 
	//}
	
	//public static String formatPlayingTime(long elapsed, long total) {
	//	return TimeResources.getInstance().formatElapsedTime(elapsed, false); 
	//}
	
	//public static String formatElapsedTime(long elapsed) {
	//	return TimeResources.getInstance().formatElapsedTime(elapsed, true); 
	//}
	
	//public static String formatTime(long timestamp) { 
	//	return Utilities.formatDate(timestamp);
	//}
    
}
