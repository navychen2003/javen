package org.javenstudio.android.information;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.javenstudio.android.app.AlertDialogBuilder;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;
import org.javenstudio.android.reader.ReaderHelper;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.parser.util.ParseUtils;

public class InformationOperation {
	
	public static interface IInformation { 
		public String getTitle();
		public String getContent();
		public String getSummary();
		public String getAuthor();
		public Intent getShareIntent();
	}
	
	static class LongClickItem { 
		public final int mTextRes;
		public final int mIconRes;
		public LongClickItem(int textRes, int iconRes) { 
			mTextRes = textRes;
			mIconRes = iconRes;
		}
	}
	
	public static String setInformationClick(InformationOne one) { 
		String location = ReaderHelper.normalizeInformationLocation(one.getLink());
		
		if (!InformationRegistry.existSource(location)) {
			InformationOne[] copyOnes = one.copyOnes(true);
			if (copyOnes != null && copyOnes.length > 0) { 
				InformationOne first = null;
				
				for (int i=0; i < copyOnes.length; i++) { 
					InformationOne copyOne = copyOnes[i];
					if (copyOne == null) continue;
					if (first == null) { 
						first = copyOne; break;
					}
				}
				
				if (first != null) { 
					location = first.getLocation();
					
					InformationRegistry.clearInformationItems();
					InformationRegistry.addInformationItem(location, new InformationItem(
							InformationBinderFactory.getInstance().getInformationItemBinder(location), 
							location, copyOnes));
					
					return location;
				}
			}
			
			return null;
		}
		
		return location;
	}
	
	public static boolean openOperation(final Activity activity, final IInformation item) { 
		if (activity == null || item == null) return false;
		
		final LongClickItem[] items = new LongClickItem[] { 
				new LongClickItem(R.string.label_action_share_information, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_share)), 
				new LongClickItem(R.string.label_action_copy_text, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_copy))
			};
		
		ArrayAdapter<?> adapter = new ArrayAdapter<LongClickItem>(activity, 0, items) { 
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				final LayoutInflater inflater = LayoutInflater.from(getContext());
				
				if (convertView == null) 
					convertView = inflater.inflate(R.layout.dialog_item, null);
				
				LongClickItem item = getItem(position);
				TextView textView = (TextView)convertView.findViewById(R.id.dialog_item_text);
				
				if (item != null && textView != null) { 
					textView.setText(item.mTextRes);
					textView.setCompoundDrawablesWithIntrinsicBounds(item.mIconRes, 0, 0, 0);
				}
				
				return convertView;
			}
		};
		
		AlertDialogBuilder builder = AppResources.getInstance().createDialogBuilder(activity);
		builder.setCancelable(true);
		//builder.setTitle(R.string.label_select_operation);
		builder.setAdapter(adapter, 
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (which == 0) shareInformation(activity, item);
					else if (which == 1) copyInformation(activity, item);
					dialog.dismiss();
				}
			});
		
		builder.show(activity);
		
		return true; 
	}
	
	private static void shareInformation(Activity activity, IInformation item) { 
		if (activity == null || item == null) return;
		
		Intent intent = item.getShareIntent();
		if (intent != null) 
			shareInformation(activity, intent);
	}
	
	public static void shareInformation(Activity activity, Intent intent) { 
		if (activity == null) return;
		
		if (intent != null) { 
			activity.startActivity(Intent.createChooser(intent, 
					activity.getString(R.string.label_action_share_information)));
		}
	}
	
	private static void copyInformation(Activity activity, IInformation item) { 
		if (activity == null || item == null) return;
		
		String html = ParseUtils.trim(item.getContent());
		if (html == null || html.length() == 0) 
			html = ParseUtils.trim(item.getSummary());
		
		if (html == null || html.length() == 0)
			html = ParseUtils.trim(item.getTitle());
		
		String text = ParseUtils.trim(ParseUtils.removeWhiteSpaces(
				ParseUtils.extractContentFromHtml(html)));
		
		if (html == null || text == null || html.length() == 0 || text.length() == 0)
			return;
		
		ClipboardManager clip = (ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);
		if (clip != null) { 
			clip.setPrimaryClip(ClipData.newHtmlText(null, text, html));
			Toast.makeText(activity, R.string.information_copied_message, 
					Toast.LENGTH_SHORT).show();
		}
	}
	
	private static String extractText(String text) { 
		text = ParseUtils.trim(ParseUtils.extractContentFromHtml(text)); 
		text = ParseUtils.trim(ParseUtils.removeWhiteSpaces(text)); 
		return text;
	}
	
	public static String formatShareText(AppResources.Fieldable data, String info) { 
		if (info == null) info = "";
		
		String text = null; 
		
		if (data != null) { 
			String title = extractText(data.getField("title"));
			String content = extractText(data.getField("content"));
			
			text = title;
			
			if (content != null && content.length() > 0) {
				if (text != null && text.length() > 0) 
					text += " " + content;
				else
					text = content;
			}
			
			if (text != null && text.length() > 0) { 
				if (text.length() > 50) 
					text = text.substring(0, 50) + "...";
			} else
				text = "";
			
			String author = extractText(data.getField("author"));
			if (author != null && author.length() > 0) { 
				if (author.length() > 20) 
					author = author.substring(0, 20) + "...";
				
				text += " " + ResourceHelper.getResources().getString(R.string.details_author) 
						+ ": " + author;
			}
			
			String source = extractText(data.getField("source"));
			if (source != null && source.length() > 0) { 
				if (source.length() > 20) 
					source = source.substring(0, 20) + "...";
				
				text += " " + ResourceHelper.getResources().getString(R.string.details_source) 
						+ ": " + source;
			}
			
			String link = data.getField("link");
			if (link != null && link.length() > 0) { 
				text += " " + link;
			}
		}
		
		if (text != null && text.length() > 0) { 
			if (info != null && info.length() > 0)
				info = text + " (" + info + " )";
			else
				info = text;
		}
		
		return info;
	}
	
}
