package com.bccv.zhuiying.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import com.alibaba.fastjson.JSON;
import com.bccv.zhuiying.model.MovieInfo;
import com.utils.net.HttpClientUtil;
import com.utils.tools.StringUtils;

import android.util.Log;

public class MovieInfoApi extends AppApi {
	
	
	/**
	 * 
	 * 详情介绍
	 * 
	 * 
	 **/

	public MovieInfo getMovieInfo(String movie_id, String type_id) {
		HttpClientUtil util = new HttpClientUtil();
		Map<String, String> params = new HashMap<String, String>();
		params.put("video_id", movie_id);
		params.put("type_id", type_id);

		String result = util.sendGet(Url.Info, params);
		
		if (result != null) {
			Log.e("getMovieInfo", result);
		} else {
			Log.e("getMovieInfo", "null");
		}
		
		if (!StringUtils.isEmpty(result)) {
			try {
				JSONObject jsonObject = new JSONObject(result);
				if (checkResponse(jsonObject)) {
					String rtnStr = jsonObject.getString("data");

					if (!StringUtils.isEmpty(rtnStr) && !rtnStr.equals("null")) {

						MovieInfo list = null;
						list = JSON.parseObject(rtnStr, MovieInfo.class);
						return list;
					}

				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return null;
	}
}
