package org.javenstudio.android.information;

import org.javenstudio.cocoka.widget.model.NavigationInfo;
import org.javenstudio.common.parser.util.Response;
import org.javenstudio.common.parser.util.ResponseHelper;

public class ResponseNavItem extends BaseInformationNavItem {

	public ResponseNavItem(NavBinder res, NavigationInfo info) { 
		this(res, info, false); 
	}
	
	public ResponseNavItem(NavBinder res, NavigationInfo info, boolean selected) { 
		super(res, info, selected); 
	}
	
	@Override 
	protected void parseInformation(String location, String content, boolean first) { 
		if (location == null || content == null || content.length() == 0) 
			return; 

		final Response resp = parseResponse(content); 
		final Response.ResultSet resultSet = resp != null ? resp.getResultSet() : null; 
		if (resultSet == null)  
			return; 

		for (int i = resultSet.getPositionFrom(); i < resultSet.getPositionTo(); i++) { 
			Response.Result result = resultSet.getResult(i); 
			if (result != null) 
				addInformationResult(result); 
		}
	}
	
	protected Response parseResponse(String content) { 
		return ResponseHelper.parseXml(content); 
	}
	
	// should override
	protected void addInformationResult(Response.Result result) { 
		NavModel model = getModel();
		if (model == null || result == null) 
			return; 
		
		InformationOne p = new InformationOne(this, getLocation()); 
		p.setTitle(result.getFieldValue("name")); 
		
		postAddInformation(model, p); 
	}

}
