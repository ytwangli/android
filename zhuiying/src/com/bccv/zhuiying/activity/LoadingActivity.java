package com.bccv.zhuiying.activity;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.bccv.zhuiying.R;
import com.bccv.zhuiying.api.LoadingApi;
import com.bccv.zhuiying.model.AppInfo;
import com.bccv.zhuiying.model.UpdateInfo;
import com.lidroid.xutils.exception.DbException;
import com.tencent.stat.StatService;
import com.tendcloud.tenddata.TCAgent;
import com.utils.download.DownloadManager;
import com.utils.download.DownloadService;
import com.utils.model.DownloadMovie;
import com.utils.tools.BaseActivity;
import com.utils.tools.Callback;
import com.utils.tools.GlobalParams;
import com.utils.tools.Logger;
import com.utils.tools.Statistics;
import com.utils.updatedownload.DownLoadAPI;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("InflateParams")
public class LoadingActivity extends BaseActivity {
	private Button button;
	private DataAsyncTask dataAsyncTask;
	private AppInfo appInfo;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		TCAgent.onPageStart(getApplicationContext(), "LoadingActivity");
		requestWindowFeature(Window.FEATURE_NO_TITLE);  //无title  
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,  
		              WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_loading);
		StatService.trackCustomBeginEvent(this, "playTime", "app");
		setView();
		getData();
	}

	private void setView() {
		button = (Button) findViewById(R.id.button);
		button.setVisibility(View.GONE);
	}

	private void getData() {
		Callback callback = new Callback() {

			@Override
			public void handleResult(String result) {
				// TODO Auto-generated method stub
				if (appInfo == null) {
					goMainActivity();
					Toast.makeText(getApplicationContext(), "网络连接错误！", Toast.LENGTH_SHORT).show();
				} else {
//					GlobalParams.dataUrl = appInfo.getDatadomain();
//					GlobalParams.dataUrl = "http://all.zhuiying.me";
//					GlobalParams.videoUrl = appInfo.getVideodomain();
//					GlobalParams.videoUrl = "http://api.zhensaikeji.com";
//					GlobalParams.magnet = appInfo.getMagnet();
//					String toFriend = appInfo.getTofriend();
//					int index = toFriend.indexOf("\n");
//					GlobalParams.toFriendUrl = toFriend.substring(index + 2);
//					GlobalParams.toFriendText = toFriend.substring(0, index);
//					button.setVisibility(View.VISIBLE);
					if (appInfo.getStatus() == 0) {
						goMainActivity();
					} else {
						GlobalParams.isNewest = false;
						if (timer != null) {
							timer.cancel();
						}
						
						showUpdateDialog(appInfo.getData(), getApplicationContext(), LoadingActivity.this);
					}
				}
			}
		};

		dataAsyncTask = new DataAsyncTask(callback, false) {

			@Override
			protected String doInBackground(String... params) {
				// TODO Auto-generated method stub
				LoadingApi loadingApi = new LoadingApi();
				String channel = Statistics.getChannelCode();
				String upver = Statistics.getPackageInfo(getApplicationContext()).versionCode + "";
				appInfo = loadingApi.getAppInfo(channel, upver);
				DownloadManager downloadManager = DownloadService.getDownloadManager(getApplicationContext());
				Map<String, DownloadMovie> movieMap = downloadManager.getMovieMap();
				for (Map.Entry<String, DownloadMovie> entry : movieMap.entrySet()) {
					DownloadMovie newDownloadMovie = entry.getValue();
					if (!newDownloadMovie.isSuccess()) {
						newDownloadMovie.setDownloading(false);
						newDownloadMovie.setSpeed("暂停");
					}
				}
				return null;
			}
		};
		dataAsyncTask.execute("");

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
					
					goMainActivity();
					
				}
			});
			
			dialog_cancle.setOnClickListener(new android.view.View.OnClickListener() {
				@Override
				public void onClick(View v) {
					goMainActivity();
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

	private Timer timer;

	public void goMainActivity() {
		if (timer != null) {
			timer.cancel();
		}
		timer = new Timer();
		TimerTask task = new TimerTask() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
//				Intent intent = new Intent(getApplicationContext(), MovieListActivity.class);
				Intent intent = new Intent(getApplicationContext(), MainActivity.class);
				startActivity(intent);
				finish();
			}
		};
		timer.schedule(task, 1000);

	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		TCAgent.onPageEnd(getApplicationContext(), "LoadingActivity");
	}
}
