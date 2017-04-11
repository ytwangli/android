package com.bccv.zhuiying.api;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.bccv.zhuiying.api.AppApi;
import com.bccv.zhuiying.api.Url;
import com.utils.net.HttpClientUtil;
import com.utils.tools.Logger;
import com.utils.tools.StringUtils;

public class FeedbackApi extends AppApi {
	public boolean seedFeedback( String version,String question){
		HttpClientUtil util = new HttpClientUtil();
		Map<String, String> params = new HashMap<String, String>();
	params.put("version", version);
		params.put("question", question);

		String result = util.sendGet(Url.FeedBack, params);
		Logger.e("seedFeedback", result);
		if (!StringUtils.isEmpty(result)) {
			try {
				JSONObject jsonObject = new JSONObject(result);
				if (checkResponse(jsonObject)) {
					return true;
				}else {
					return false;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return false;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
