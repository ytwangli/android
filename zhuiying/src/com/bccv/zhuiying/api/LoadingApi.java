package com.bccv.zhuiying.api;

import java.util.HashMap;
import java.util.Map;


import com.alibaba.fastjson.JSON;
import com.bccv.zhuiying.model.AppInfo;
import com.utils.net.HttpClientUtil;
import com.utils.tools.StringUtils;

import android.util.Log;

public class LoadingApi extends AppApi {
	public AppInfo getAppInfo(String channel, String upver) {
		HttpClientUtil util = new HttpClientUtil();
		Map<String, String> params = new HashMap<String, String>();
		params.put("channel", channel);
		params.put("upver", upver);
		params.put("appid", "1");

		String result = util.sendGet("https://log.zhuiying.me/update/?a=a", params);
		if (result != null) {
			Log.e("getAppInfo", result);
		} else {
			Log.e("getAppInfo", "null");
		}
		if (!StringUtils.isEmpty(result)) {
			try {
				AppInfo list = null;
				list = JSON.parseObject(result, AppInfo.class);
				return list;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return null;
	}
}
