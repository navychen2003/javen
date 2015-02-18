package org.javenstudio.lightning.core.user;

import java.util.ArrayList;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.setting.ISettingManager;
import org.javenstudio.falcon.setting.Setting;
import org.javenstudio.falcon.setting.SettingCategory;
import org.javenstudio.falcon.setting.SettingCheckbox;
import org.javenstudio.falcon.setting.SettingGroup;
import org.javenstudio.falcon.setting.SettingSelect;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.lightning.Constants;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public class UserSettingHandler extends UserHandlerBase {
	private static final Logger LOG = Logger.getLogger(UserSettingHandler.class);

	public static RequestHandler createHandler(UserCore core) { 
		return new UserSettingHandler(core);
	}
	
	public UserSettingHandler(UserCore core) { 
		super(core);
	}
	
	@Override
	public void handleRequestBody(Request req, Response rsp)
			throws ErrorException {
		ISettingManager manager = checkUserSetting(
				UserHelper.getUserKeyTokens(req), IUserClient.Op.ACCESS);
		if (manager == null) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Cannot load user settings");
		}
		
		String action = trim(req.getParam("action"));
		String categoryName = trim(req.getParam("category"));
		String groupName = trim(req.getParam("group"));
		
		if (categoryName == null) categoryName = "";
		if (groupName == null) groupName = "";
		
		if (action != null) { 
			if (action.equalsIgnoreCase("update")) { 
				SettingCategory category = manager.getCategory(categoryName);
				if (category == null) {
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"Setting category: " + categoryName + " not found");
				}
				
				SettingGroup group = category.getGroup(groupName);
				if (group == null) {
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"Setting group: " + categoryName + "/" + groupName + " not found");
				}
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("handleRequestBody: update setting, category=" 
							+ categoryName + " group=" + groupName + " instance=" + group);
				}
				
				SettingGroup.UpdateHandler handler = group.getUpdateHandler();
				if (handler == null) { 
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
							"Setting group: " + categoryName + "/" + groupName + " has no update handler");
				}
				
				if (handler.updateSetting(group, req))
					category.getManager().saveSettings();
			} else {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Unsupported Action: " + action);
			}
		}
		
		ArrayList<Object> categoryItems = new ArrayList<Object>();
		ArrayList<Object> groupItems = new ArrayList<Object>();
		ArrayList<Object> settingItems = new ArrayList<Object>();
		
		SettingCategory[] categories = manager.getCategories();
		SettingCategory firstCategory = null;
		SettingCategory selectCategory = null;
		
		for (int i=0; categories != null && i < categories.length; i++) { 
			SettingCategory category = categories[i];
			NamedList<Object> info = getCategoryInfo(category);
			
			if (category != null && info != null) {
				categoryItems.add(info);
				
				if (firstCategory == null)
					firstCategory = category;
				
				if (categoryName.equals(category.getName()))
					selectCategory = category;
			}
		}
		
		if (selectCategory == null) 
			selectCategory = firstCategory;
		
		String groupTitle = "";
		String groupDesc = "";
		
		if (selectCategory != null) { 
			categoryName = selectCategory.getName();
			SettingGroup[] groups = selectCategory.getGroups();
			SettingGroup firstGroup = null;
			SettingGroup selectGroup = null;
			
			for (int i=0; groups != null && i < groups.length; i++) { 
				SettingGroup group = groups[i];
				NamedList<Object> info = getGroupInfo(group);
				
				if (group != null && info != null) {
					groupItems.add(info);
					
					if (firstGroup == null)
						firstGroup = group;
					
					if (groupName.equals(group.getName()))
						selectGroup = group;
				}
			}
			
			if (selectGroup == null) 
				selectGroup = firstGroup;
			
			if (selectGroup != null) { 
				groupName = selectGroup.getName();
				groupTitle = selectGroup.getTitle();
				groupDesc = selectGroup.getDescription();
				
				Setting[] settings = selectGroup.getSettings();
				
				for (int j=0; settings != null && j < settings.length; j++) { 
					Setting setting = settings[j];
					NamedList<Object> info = getSettingInfo(setting);
					if (setting != null && info != null) 
						settingItems.add(info);
				}
			}
		}
		
		rsp.add("version", toString(getVersion()));
		rsp.add("category", toString(categoryName));
		rsp.add("group", toString(groupName));
		rsp.add("group_title", toString(groupTitle));
		rsp.add("group_desc", toString(groupDesc));
		rsp.add("categories", categoryItems.toArray(new Object[categoryItems.size()]));
		rsp.add("groups", groupItems.toArray(new Object[groupItems.size()]));
		rsp.add("settings", settingItems.toArray(new Object[settingItems.size()]));
	}

	static String getVersion() { 
		Package lightningPackage = org.javenstudio.lightning.Constants.class.getPackage();
		if (lightningPackage != null) {
			String specVersion = lightningPackage.getSpecificationVersion();
			String implVersion = lightningPackage.getImplementationVersion();
			
			if (specVersion == null || implVersion == null) { 
				specVersion = Constants.SPECIFICATION_VERSION;
				implVersion = Constants.IMPLEMENTS_VERSION;
			}
			
			return specVersion;
		}
		
		return "";
	}
	
	static NamedList<Object> getCategoryInfo(SettingCategory item) 
			throws ErrorException { 
		if (item == null) return null;
		NamedList<Object> info = new NamedMap<Object>();
		
		if (item != null) { 
			info.add("name", toString(item.getName()));
			info.add("title", toString(item.getTitle()));
			info.add("desc", toString(item.getDescription()));
		}
		
		return info;
	}
	
	static NamedList<Object> getGroupInfo(SettingGroup item) 
			throws ErrorException { 
		if (item == null) return null;
		NamedList<Object> info = new NamedMap<Object>();
		
		if (item != null) { 
			info.add("name", toString(item.getName()));
			info.add("title", toString(item.getTitle()));
			info.add("desc", toString(item.getDescription()));
		}
		
		return info;
	}
	
	static NamedList<Object> getSettingInfo(Setting item) 
			throws ErrorException { 
		if (item == null) return null;
		NamedList<Object> info = new NamedMap<Object>();
		
		if (item != null) { 
			info.add("name", toString(item.getName()));
			info.add("type", toString(item.getType()));
			info.add("title", toString(item.getTitle()));
			info.add("desc", toString(item.getDescription()));
			info.add("value", toString(item.getValue()));
			
			if (item instanceof SettingCheckbox) { 
				SettingCheckbox checkbox = (SettingCheckbox)item;
				boolean checked = checkbox.isChecked();
				
				info.add("checked", checked);
				
			} else if (item instanceof SettingSelect) { 
				SettingSelect select = (SettingSelect)item;
				SettingSelect.Option[] options = select.getOptions();
				
				ArrayList<Object> list = new ArrayList<Object>();
				
				for (int i=0; options != null && i < options.length; i++) { 
					SettingSelect.Option option = options[i];
					if (option != null) { 
						NamedList<Object> optionInfo = new NamedMap<Object>();
						optionInfo.add("value", toString(option.getValue()));
						optionInfo.add("title", toString(option.getTitle()));
						
						list.add(optionInfo);
					}
				}
				
				info.add("options", list.toArray(new Object[list.size()]));
			}
		}
		
		return info;
	}
	
}
