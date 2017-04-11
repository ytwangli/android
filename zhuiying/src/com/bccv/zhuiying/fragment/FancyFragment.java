package com.bccv.zhuiying.fragment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import com.bccv.zhuiying.R;
import com.bccv.zhuiying.activity.CollectActivity;
import com.bccv.zhuiying.activity.DownloadActivity;
import com.bccv.zhuiying.activity.HistoryActivity;
import com.bccv.zhuiying.activity.MovieInfoActivity;
import com.bccv.zhuiying.activity.MovieSearchActivity;
import com.bccv.zhuiying.activity.Video2DPlayerActivity;
import com.bccv.zhuiying.adapter.MovieListAdapter;
import com.bccv.zhuiying.api.FancyApi;
import com.bccv.zhuiying.api.LoadingApi;
import com.bccv.zhuiying.api.SearchApi;
import com.bccv.zhuiying.model.AppInfo;
import com.bccv.zhuiying.model.HotSearch;
import com.bccv.zhuiying.model.Movie;
import com.bccv.zhuiying.model.MovieModel;
import com.bccv.zhuiying.model.UpdateInfo;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.tendcloud.tenddata.TCAgent;
import com.utils.tools.BaseActivity;
import com.utils.tools.Callback;
import com.utils.tools.GlobalParams;
import com.utils.tools.Logger;
import com.utils.tools.ScreenUtil;
import com.utils.tools.SerializationUtil;
import com.utils.tools.Statistics;
import com.utils.tools.StringUtils;
import com.utils.updatedownload.DownLoadAPI;
import com.utils.views.MyGridView;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FancyFragment extends BaseActivity {
	private MyGridView gridView, gridView1;
	private MovieListAdapter adapter, adapter1;
	private List<Movie> data, tvData;
	private List<Movie> list, tvList;
	private List<Movie> topList;
	private List<Movie> HlistData;
	
	RelativeLayout reH;
	RelativeLayout clear;
	TextView hisContext;
	
	private boolean isStop = false;
	MovieModel movieDodel;
	Button edit;
	
	private ImageButton DownBtn;
	private List<HotSearch> hotSearchList, getHotSearchList;
	private boolean isClear = false;
	private static int timeNum = 10;
	private static Timer timer = new Timer();
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_fancy);
		ImageButton colletChBtn=(ImageButton) findViewById(R.id.titel_collect);
		colletChBtn.setVisibility(View.VISIBLE);
		
		
		
		colletChBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				startActivityWithSlideAnimation(CollectActivity.class);
			}
		});
		
		
		ImageButton hisBtn=(ImageButton) findViewById(R.id.titel_history);
		hisBtn.setVisibility(View.VISIBLE);
		
		
		
		hisBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				startActivityWithSlideAnimation(HistoryActivity.class);
			}
		});
		
		
		reH = (RelativeLayout) findViewById(R.id.fancy_his_re);
		clear = (RelativeLayout) findViewById(R.id.fancy_his_close);
		hisContext = (TextView) findViewById(R.id.fancy_his);
		TimerTask task = new TimerTask() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				mHandler.sendEmptyMessage(0);
			}
		};
		timer = new Timer();
		timer.schedule(task, 0, 1000);
		
		gridView = (MyGridView) findViewById(R.id.fancy_grid);
		gridView.setSelector(new ColorDrawable(android.R.color.transparent));

		gridView1 = (MyGridView) findViewById(R.id.fancy_grid1);
		gridView1.setSelector(new ColorDrawable(android.R.color.transparent));
		data = new ArrayList<Movie>();
		tvData = new ArrayList<Movie>();

		list = SerializationUtil.readMainSerialization(getApplication());
		if (list != null) {
			data.addAll(list);
		}

		adapter = new MovieListAdapter(this, data);

		tvList = SerializationUtil.readMainTVSerialization(getApplication());

		if (tvList != null) {
			tvData.addAll(tvList);
		}

		adapter1 = new MovieListAdapter(this, tvData);
		gridView.setAdapter(adapter);
		gridView1.setAdapter(adapter1);

		edit = (Button) findViewById(R.id.title_search_edit);

		DownBtn = (ImageButton) findViewById(R.id.title_search_btnDown);

		edit.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				startActivityWithSlideAnimation(MovieSearchActivity.class);

			}
		});

		DownBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				startActivityWithSlideAnimation(DownloadActivity.class);
			}
		});

		gridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub

				Intent aIntent = new Intent(FancyFragment.this, MovieInfoActivity.class);

				aIntent.putExtra("movie", data.get(position));

				startActivity(aIntent);

			}
		});
		gridView1.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub

				Intent aIntent = new Intent(FancyFragment.this, MovieInfoActivity.class);

				aIntent.putExtra("movie", tvData.get(position));

				startActivity(aIntent);
			}
		});
		ll_dots = (LinearLayout) findViewById(R.id.ll_point_group);
		headerViewPager = (ViewPager) findViewById(R.id.fancy_viewPager);
		headerViewPager.setOnPageChangeListener(new MyPageChangeListener());
		Thread myThread = new Thread(new Runnable() {

			@Override
			public void run() {
				while (!isStop) {
					// 每个两秒钟发一条消息到主线程，更新viewpager界面
					SystemClock.sleep(5000);
					runOnUiThread(new Runnable() {
						public void run() {
							// 此方法在主线程中执行
							int newindex = headerViewPager.getCurrentItem() + 1;
							headerViewPager.setCurrentItem(newindex);
						}
					});
				}
			}
		});
		myThread.start(); // 用来更细致的划分 比如页面失去焦点时候停止子线程恢复焦点时再开启
		getData();
		getHot();
	}

	
	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			
			if (timeNum == 0) {
				reH.setVisibility(View.GONE);
				isClear = true;
				timer.cancel();
//				reH.startAnimation(slateAnimation2);
			}
			timeNum--;
		};
	};
	
	

	private void getHistory() {
		// TODO Auto-generated method stub

		HlistData = SerializationUtil.readHistoryCache(this.getApplicationContext());
		if (HlistData != null && HlistData.size() > 0) {
			if (!isClear) {
				reH.setVisibility(View.VISIBLE);
			}

			String htext = HlistData.get(0).getTitle();
			String htime = HlistData.get(0).getEpisode_id();
			long time = HlistData.get(0).getPlay_Time();

			if (StringUtils.isEmpty(time + "")) {
				hisContext.setText("您上次看到《" + htext + "》第" + htime + "集" + StringUtils.toDate(time + ""));

			} else {
				hisContext.setText("您上次看到" + "《" + htext + "》" + "第" + htime + "集");

			}
		} else {
			reH.setVisibility(View.GONE);
		}
		clear.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				reH.setVisibility(View.GONE);
				isClear = true;
			}
		});
		reH.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub

				Intent aIntent = new Intent(FancyFragment.this, Video2DPlayerActivity.class);

				aIntent.putExtra("movie", HlistData.get(0));

				startActivity(aIntent);

			}
		});

	

	}
	
	
	
	public void getHot() {

		Callback callback = new Callback() {

			@Override
			public void handleResult(String result) {
				// TODO Auto-generated method stub
				Random random = new Random();
				hotSearchList = new ArrayList<HotSearch>();
				if (getHotSearchList != null && getHotSearchList.size() > 0) {
					hotSearchList.addAll(getHotSearchList);
					int randomInt = random.nextInt(getHotSearchList.size());
					edit.setText("大家都在搜：" + hotSearchList.get(randomInt).getName());
				}

			}
		};

		new DataAsyncTask(callback, false) {

			@Override
			protected String doInBackground(String... params) {
				// TODO Auto-generated method stub
				SearchApi searchApi = new SearchApi();
				getHotSearchList = searchApi.getHotSearchList("10");

				return null;
			}
		}.execute("");

	}

	public void getData() {

		Callback callback = new Callback() {

			@Override
			public void handleResult(String result) {
				// TODO Auto-generated method stub
				if (data != null) {
					data.clear();

				}
				if (tvData != null) {
					tvData.clear();
				}
				if (movieDodel != null) {
					list = movieDodel.getData();
					data.addAll(list);

					SerializationUtil.wirteMainSerialization(getApplication(), (Serializable) data);

					adapter.notifyDataSetChanged();
					tvList = movieDodel.getTv();
					tvData.addAll(tvList);

					SerializationUtil.wirteMainTVSerialization(getApplicationContext(), (Serializable) tvData);
					adapter1.notifyDataSetChanged();
				}

				if (topList != null) {
					setTopView(topList);
				}

			}
		};

		new DataAsyncTask(callback, false) {

			@Override
			protected String doInBackground(String... params) {

				FancyApi api = new FancyApi();

				movieDodel = api.getMovielist();

				topList = api.getTopList();
				return "true";
			}
		}.execute("");
	}

	@SuppressWarnings("unused")
	private static final int CHANGE_VIEWPAGER = 1;
	private List<ImageView> dots; // 图片标题正文的那些点
	private List<String> headpicUrls;// 图片地址
	private LinearLayout ll_dots;

	private ViewPager headerViewPager;
	private int currentItem;
	private MyAdapter myAdapter;

	private void setTopView(List<Movie> topData2) {
		// TODO Auto-generated method stub

		dots = new ArrayList<ImageView>();

		headpicUrls = new ArrayList<String>();

		myAdapter = new MyAdapter(topData2);
		int temp = topData2.size() > 0 ? (myAdapter.getCount()) / 2 % (topData2.size()) : 0;
		currentItem = (myAdapter.getCount()) / 2 - temp;
		initHeadData(topData2);
		headerViewPager.setCurrentItem(currentItem);
		headerViewPager.setAdapter(myAdapter);
		headerViewPager.setCurrentItem(0);

	}

	/**
	 * 初始化头部viewPager
	 * 
	 * @param freshList
	 */
	private void initHeadData(List<Movie> modelList) {

		ll_dots.removeAllViews();
		dots.clear();

		headpicUrls.clear();

		for (Movie model : modelList) {
			ImageView dot = addDot(ll_dots);
			dots.add(dot);

			// 初始化图片url
			headpicUrls.add(model.getImages());

		}
		dots.get(currentItem % (dots.size())).setImageResource(R.drawable.dot_select);

	}

	/**
	 * 添加滑动点
	 * 
	 * @param ll_dots
	 */
	private ImageView addDot(LinearLayout ll_dots) {
		// 初始化圆点

		ImageView view = new ImageView(FancyFragment.this);
		view.setImageResource(R.drawable.dot);
		LinearLayout.LayoutParams mLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		mLayoutParams.rightMargin = ScreenUtil.dp2px(4);
		mLayoutParams.leftMargin = ScreenUtil.dp2px(4);
		mLayoutParams.topMargin = ScreenUtil.dp2px(12);
		mLayoutParams.bottomMargin = ScreenUtil.dp2px(7);
		ll_dots.addView(view, mLayoutParams);
		return view;
	}

	/**
	 * 当ViewPager中页面的状态发生改变时调用
	 * 
	 * @author Administrator
	 * 
	 */
	private class MyPageChangeListener implements OnPageChangeListener {
		private int oldPosition = currentItem;

		/**
		 * This method will be invoked when a new page becomes selected.
		 * position: Position index of the new selected page.
		 */
		public void onPageSelected(int position) {

			currentItem = position;

			dots.get(oldPosition % (dots.size())).setImageResource(R.drawable.dot);
			dots.get(position % (dots.size())).setImageResource(R.drawable.dot_select);
			oldPosition = position;

		}

		public void onPageScrollStateChanged(int position) {
		}

		public void onPageScrolled(int arg0, float arg1, int arg2) {

		}
	}

	/**
	 * 填充ViewPager页面的适配器
	 * 
	 * @author Administrator
	 * 
	 */
	private class MyAdapter extends PagerAdapter {
		private List<Movie> topData;

		public MyAdapter(List<Movie> topData) {
			// TODO Auto-generated constructor stub
			this.topData = topData;
		}

		@Override
		public int getCount() {
			if (topData.size() == 1) {
				return 1;
			}
			return Integer.MAX_VALUE;
		}

		@Override
		public Object instantiateItem(View container, int progress) {
			int curretIndex = (progress) % topData.size();
			ImageView imageView = new ImageView(FancyFragment.this);
			imageView.setScaleType(ScaleType.CENTER_CROP);

			if (topData != null && topData.size() > curretIndex) {
				setOnClick4headPic(imageView, topData.get(curretIndex));
			}

			((ViewPager) container).addView(imageView);

			ImageLoader imageLoader = ImageLoader.getInstance();
			imageLoader.displayImage(headpicUrls.get(curretIndex), imageView, GlobalParams.bannerOptions);

			return imageView;
		}

		@Override
		public void destroyItem(View container, int arg1, Object arg2) {
			((ViewPager) container).removeView((View) arg2);
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == arg1;
		}

		@Override
		public void restoreState(Parcelable arg0, ClassLoader arg1) {

		}

		@Override
		public Parcelable saveState() {
			return null;
		}

		@Override
		public void startUpdate(View arg0) {

		}

		@Override
		public void finishUpdate(View arg0) {

		}
	}

	/**
	 * 
	 * /** 为头部viewPager设置点击事件
	 * 
	 * @param view
	 * @param fresh
	 */
	private void setOnClick4headPic(View view, final Movie model) {

		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				int curretIndex = (currentItem) % topList.size();
				Intent aIntent = new Intent(FancyFragment.this, MovieInfoActivity.class);

				aIntent.putExtra("movie", topList.get(curretIndex));

				startActivity(aIntent);
			}
		});

	}

	@Override
	protected void onDestroy() {
		isStop = true;
		timer.cancel();
		super.onDestroy();
	}

	private AppInfo appInfo;

	private void getInfoData() {
		Callback callback = new Callback() {

			@Override
			public void handleResult(String result) {
				// TODO Auto-generated method stub
				if (appInfo == null) {
					Toast.makeText(getApplicationContext(), "网络连接错误！", Toast.LENGTH_SHORT).show();
				} else {
					GlobalParams.dataUrl = appInfo.getDatadomain();
					GlobalParams.videoUrl = appInfo.getVideodomain();
					// button.setVisibility(View.VISIBLE);
					if (appInfo.getStatus() == 0) {

					} else {
						showUpdateDialog(appInfo.getData(), getApplicationContext(), FancyFragment.this);
					}

				}

			}
		};

		new DataAsyncTask(callback, true) {

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
					getData();
				}
			});

			dialog_cancle.setOnClickListener(new android.view.View.OnClickListener() {
				@Override
				public void onClick(View v) {
					getData();
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
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		getHistory();
		TCAgent.onPageStart(getApplicationContext(), "FancyFragment");
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		TCAgent.onPageEnd(getApplicationContext(), "FancyFragment");
	}

}
