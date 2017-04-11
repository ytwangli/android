package com.bccv.zhuiying.activity;

import java.io.File;

import com.bccv.zhuiying.R;
import com.bccv.zhuiying.api.LoadingApi;
import com.bccv.zhuiying.model.AppInfo;
import com.bccv.zhuiying.model.UpdateInfo;
import com.igexin.sdk.PushManager;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.tendcloud.tenddata.TCAgent;
import com.utils.tools.AppConfig;
import com.utils.tools.BaseActivity;
import com.utils.tools.Callback;
import com.utils.tools.DataCleanManager;
import com.utils.tools.GlobalParams;
import com.utils.tools.Logger;
import com.utils.tools.Statistics;
import com.utils.updatedownload.DownLoadAPI;
import com.utils.views.ShareDialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class SetActivity extends BaseActivity implements OnClickListener {
	private RelativeLayout rl_friends;
	private RelativeLayout rl_wifi;
	private RelativeLayout rl_about;
	private RelativeLayout rl_feedback;
	private RelativeLayout rl_version;
	private RelativeLayout rl_clear;
	private RelativeLayout rl_tui;
	private TextView cache_size;
	private ToggleButton wifiBtn, tuiBtn;
	ShareDialog shreDia;
private Context context;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		TCAgent.onPageStart(getApplicationContext(), "SetActivity");
		setContentView(R.layout.activity_set);

		TextView text = (TextView) findViewById(R.id.titleName_textView);
		text.setVisibility(View.VISIBLE);
		text.setText("设置");

		TextView isNewesttext = (TextView) findViewById(R.id.rl_isNewest);
		String upver = Statistics.getPackageInfo(getApplicationContext()).versionName + "";
		if (GlobalParams.isNewest) {
			isNewesttext.setText("版本:V "+upver);
		}else{
			isNewesttext.setText("有更新");
		}
		context=getApplicationContext();
		
		ImageButton backBtn = (ImageButton) findViewById(R.id.titel_back);
		backBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				finish();
			}
		});

		rl_friends = (RelativeLayout) findViewById(R.id.rl_friends);
		rl_wifi = (RelativeLayout) findViewById(R.id.rl_wifi);
		rl_about = (RelativeLayout) findViewById(R.id.rl_about);
		rl_feedback = (RelativeLayout) findViewById(R.id.rl_feedback);
		rl_version = (RelativeLayout) findViewById(R.id.rl_version);
		rl_clear = (RelativeLayout) findViewById(R.id.rl_clear);
		rl_tui = (RelativeLayout) findViewById(R.id.rl_tuisong);
		cache_size = (TextView) findViewById(R.id.cache_size);
		rl_friends.setOnClickListener(this);
		rl_wifi.setOnClickListener(this);

		rl_about.setOnClickListener(this);
		rl_feedback.setOnClickListener(this);
		rl_version.setOnClickListener(this);
		rl_clear.setOnClickListener(this);
		rl_tui.setOnClickListener(this);
///storage/sdcard0/Android/data/com.bccv.zhuiying/cache/uil-images/1318520130

		try {

			cache_size.setText(DataCleanManager.getCacheSize(context.getExternalCacheDir()));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		wifiBtn = (ToggleButton) findViewById(R.id.set_wifi);
		tuiBtn = (ToggleButton) findViewById(R.id.set_tui);

		wifiBtn.setChecked(GlobalParams.isWifi);
		wifiBtn.setBackgroundResource(GlobalParams.isWifi ? R.drawable.set_open : R.drawable.set_close);
		wifiBtn.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				// TODO Auto-generated method stub
				wifiBtn.setChecked(arg1);
				wifiBtn.setBackgroundResource(arg1 ? R.drawable.set_open : R.drawable.set_close);
				GlobalParams.isWifi = !GlobalParams.isWifi;
				AppConfig.setWifi(GlobalParams.isWifi);
			}
		});

		tuiBtn.setChecked(GlobalParams.isTui);
		tuiBtn.setBackgroundResource(GlobalParams.isTui ? R.drawable.set_open : R.drawable.set_close);
		tuiBtn.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				// TODO Auto-generated method stub
				tuiBtn.setChecked(arg1);
				tuiBtn.setBackgroundResource(arg1 ? R.drawable.set_open : R.drawable.set_close);
				if (arg1) {
					// 开启
					PushManager.getInstance().turnOnPush(SetActivity.this.getApplicationContext());

				} else {
					// 关闭
					PushManager.getInstance().turnOffPush(SetActivity.this.getApplicationContext());
				}

				GlobalParams.isTui = !GlobalParams.isTui;
				AppConfig.setTui(GlobalParams.isTui);
			}
		});

	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub

		switch (v.getId()) {
		case R.id.rl_friends:
			shreDia = new ShareDialog();
			shreDia.showUpdateDialog(GlobalParams.toFriendText , GlobalParams.toFriendUrl, getApplicationContext(),
					SetActivity.this);

			break;

		case R.id.rl_about:
			Intent aboutIntent = new Intent(this, AboutUsActivity.class);

			startActivity(aboutIntent);

			break;
		case R.id.rl_feedback:
			Intent aIntent = new Intent(this, FeedBackActivity.class);

			startActivity(aIntent);

			break;
		case R.id.rl_version:
			if (!GlobalParams.isNewest) {
				checkUpdate();
			}else{
				Toast.makeText(getApplicationContext(), "已是最新版本", Toast.LENGTH_SHORT).show();
			}
			
			break;
		case R.id.rl_clear:
			
		
			  ImageLoader.getInstance().clearDiscCache();
			  ImageLoader.getInstance().clearMemoryCache();
			cache_size.setText("0KB");
			showShortToast("已清理缓存");

			
			
			
			
			
			
			
			
			
			break;

		default:
			break;
		}

	}

	private AppInfo appInfo;

	private void checkUpdate() {
		Callback callback = new Callback() {

			@Override
			public void handleResult(String result) {
				// TODO Auto-generated method stub
				if (appInfo == null) {
					Toast.makeText(getApplicationContext(), "网络连接错误！", Toast.LENGTH_SHORT).show();
				} else {
					if (appInfo.getStatus() == 0) {
						Toast.makeText(getApplicationContext(), "已是最新版本", Toast.LENGTH_SHORT).show();
					}else{
						showUpdateDialog(appInfo.getData(), getApplicationContext(), SetActivity.this);
					}
					

				}
			}
		};

		new DataAsyncTask(callback, false) {

			@Override
			protected String doInBackground(String... params) {
				// TODO Auto-generated method stub
				LoadingApi loadingApi = new LoadingApi();
				String channel = Statistics.getChannelCode();
				String upver = Statistics.getPackageInfo(getApplicationContext()).versionCode + "";
				appInfo = loadingApi.getAppInfo(channel, upver);
				return null;
			}
		}.execute("");

	}
	
	/**
	 * 解析升级信息 显示升级对话框
	 * 
	 * @param data
	 *            升级信息
	 * @param context
	 */
	public void showUpdateDialog(final UpdateInfo data, final Context context, final Activity activity) {

		if (data != null) {
			final String downloadUrl = data.getDown_url();
			final String version_name = data.getDes_ver();
			String desString = data.getDes();
			final int new_version = data.getUpver();
			final Dialog dialog = new Dialog(activity, R.style.MyDialog);
			dialog.setCanceledOnTouchOutside(false);
			dialog.setCancelable(false);
			// 设置它的ContentView
			View view = LayoutInflater.from(context).inflate(R.layout.custome_dialog, null);
			TextView tv = (TextView) view.findViewById(R.id.dialog_message);
			TextView dialog_enter = (TextView) view.findViewById(R.id.dialog_enter);
			dialog_enter.setSelected(true);
			TextView dialog_cancle = (TextView) view.findViewById(R.id.dialog_cancle);
			dialog_enter.setOnClickListener(new android.view.View.OnClickListener() {
				@Override
				public void onClick(View v) {
					dialog.cancel();
					if (downloadUrl != null && !downloadUrl.equals("")) {
						DownLoadAPI.downLoadApk(downloadUrl,
								activity.getApplicationContext().getResources().getString(R.string.app_name),
								activity.getApplicationContext().getPackageName(), new_version, true, true);
					} else {
						Logger.e("showUpdateDialog", " 下载路径出错  downloadUrl : " + downloadUrl);
					}
					
				}
			});
			
			dialog_cancle.setOnClickListener(new android.view.View.OnClickListener() {
				@Override
				public void onClick(View v) {
					dialog.cancel();
				}
			});
			desString = desString.replace("\\n", "\n");

			tv.setText("新版本: " + version_name + "\n" + desString);
			view.setMinimumWidth(600);
			dialog.setContentView(view);
			dialog.show();
		}
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		TCAgent.onPageEnd(getApplicationContext(), "SetActivity");
	}

}
