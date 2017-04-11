package com.bccv.boxcomic.sns;



import com.bccv.boxcomic.modal.User;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;



public class UserInfoManager {

	@SuppressWarnings("unused")
	private static String TAG = "UserInfoManager";
	
	private static String USERINFO_SP = "userinfo_sp";
	
	private static SharedPreferences sp;
	
	private static String USER_NAME = "user_name";
	private static String USER_ICON = "user_icon";
	private static String USER_TYPE = "user_type";
	private static String USER_BACKDROP = "backdrop";
	private static String USER_GROUP = "user_group";
	private static String USER_MONEY = "user_money";
	private static String USER_SCORE = "user_score";
	private static String USER_ID = "user_id";
	private static String USER_BINDS = "user_binds";
	private static String USER_TOKEN = "user_token";
	
	private static Context mContext;
	/**
	 * 初始化帐户管理类 需在启动应用时做初始化
	 * 
	 * @param context
	 */
	public static void init(Context context) {
		mContext = context;
		sp = mContext.getSharedPreferences(
				USERINFO_SP, Context.MODE_MULTI_PROCESS);
	}
	
	public synchronized static void saveUserInfo(User userInfo){
		if(userInfo!=null){
			Editor edit = sp.edit();
			
			if(userInfo.getUser_id()!=-1){
				edit.putInt(USER_ID, userInfo.getUser_id());
			}else{
				edit.commit();
				return;
			}
			
			switch (userInfo.getUser_type()) {
				case SNSLoginManager.TENCENT_QQ_TYPE:
						edit.putString(USER_TYPE, "QQ");
					break;
				case SNSLoginManager.SINA_WEIBO_TYPE:
						edit.putString(USER_TYPE, "SINA");
					break;
				case SNSLoginManager.TENCENT_WEIXIN_TYPE:
						edit.putString(USER_TYPE, "WEIXIN");
					break;
				default:
					break;
			}
			
			
			if(!TextUtils.isEmpty(userInfo.getUser_name())){
				edit.putString(USER_NAME, userInfo.getUser_name());
			}
			if(!TextUtils.isEmpty(userInfo.getUser_icon())){
				edit.putString(USER_ICON, userInfo.getUser_icon());
			}
			
			

			edit.commit();
		}
	}
	
	/**
	 * 保存userid
	 * @param userId
	 */
	public static void saveUserId(int userId){
		Editor edit = sp.edit();
		edit.putInt(USER_ID, userId);
		edit.commit();
	}
	
	/**
	 * 获取userid
	 * @return
	 */
	public static int getUserId(){
		return sp.getInt(USER_ID, -1);
	}
	
	
	/**
	 * 获取userType
	 * @return
	 */
	public static String getUserType(){
		return sp.getString(USER_TYPE, "");
	}
	
	/**
	 * 保存用户名
	 * @param userName
	 */
	public static void saveUserName(String userName){
		Editor edit = sp.edit();
		edit.putString(USER_NAME, userName);
		edit.commit();
	}
	
	/**
	 * 获取用户名
	 * @return
	 */
	public static String getUserName(){
		return sp.getString(USER_NAME, "");
	}
	
	/**
	 * 保存用户头像
	 * @param userIcon
	 */
	public static void saveUserIcon(String userIcon){
		Editor edit = sp.edit();
		edit.putString(USER_ICON, userIcon);
		edit.commit();
	}
	
	/**
	 * 获取用户头像
	 * @return
	 */
	public static String getUserIcon(){
		return sp.getString(USER_ICON, "");
	}
	
	/**
	 * 保存用户绑定信息
	 * @param userIcon
	 */
	public static void saveUserBinds(String binds){
		Editor edit = sp.edit();
		edit.putString(USER_BINDS, binds);
		edit.commit();
	}
	
	/**
	 * 获取用户绑定信息
	 * @return
	 */
	public static String getUserBinds(){
		return sp.getString(USER_BINDS, "");
	}
	
//	/**
//	 * 保存用户token
//	 * @param userScore
//	 */
//	public static void saveUserToken(String userToken){
//		Editor edit = sp.edit();
//		edit.putString(USER_TOKEN, userToken);
//		edit.commit();
//	}
//	
//	/**
//	 * 获取用户token
//	 * @return
//	 */
//	public static String getUserToken(){
//		return sp.getString(USER_TOKEN, "");
//	}
	
	/**
	 * 是否登录
	 * @return
	 */
	public static boolean isLogin(){
		return getUserId() != -1;

	}
	
	/**
	 * 用户退出
	 */
	public static void logOut(){
		clearUserInfo();
	}
	
	
	public static void clearUserInfo(){
		Editor edit = sp.edit();
		edit.putInt(USER_ID, -1);
		edit.putString(USER_NAME, "");
		edit.putString(USER_ICON, "");
		edit.putString(USER_BINDS, "");
		edit.putString(USER_BACKDROP, "");
		edit.putString(USER_GROUP, "");
		edit.putInt(USER_MONEY, 0);
		edit.putInt(USER_SCORE, 0);
		edit.putString(USER_TOKEN, "");
		edit.commit();

	}
	public enum UserType{
		QQ,TENCENT,SINA,WEIXIN,UNKNOW
	}
}
