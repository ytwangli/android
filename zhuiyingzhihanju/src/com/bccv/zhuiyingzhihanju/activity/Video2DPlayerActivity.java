package com.bccv.zhuiyingzhihanju.activity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.alibaba.fastjson.JSONObject;
import com.baidu.cyberplayer.core.BVideoView;
import com.baidu.cyberplayer.core.BVideoView.OnCompletionListener;
import com.baidu.cyberplayer.core.BVideoView.OnErrorListener;
import com.baidu.cyberplayer.core.BVideoView.OnInfoListener;
import com.baidu.cyberplayer.core.BVideoView.OnNetworkSpeedListener;
import com.baidu.cyberplayer.core.BVideoView.OnPlayingBufferCacheListener;
import com.baidu.cyberplayer.core.BVideoView.OnPreparedListener;
import com.bccv.zhuiyingzhihanju.R;
import com.bccv.zhuiyingzhihanju.adapter.DownloadEpisodeAdapter;
import com.bccv.zhuiyingzhihanju.adapter.InfoCommentAdapter;
import com.bccv.zhuiyingzhihanju.adapter.InfoEpisodeAdapter;
import com.bccv.zhuiyingzhihanju.adapter.InfoEpisodeNumAdapter;
import com.bccv.zhuiyingzhihanju.adapter.MovieLikeAdapter;
import com.bccv.zhuiyingzhihanju.adapter.PlayerEpisodeAdapter;
import com.bccv.zhuiyingzhihanju.adapter.PlayerHDAdapter;
import com.bccv.zhuiyingzhihanju.adapter.PlayerSourceAdapter;
import com.bccv.zhuiyingzhihanju.api.CollectApi;
import com.bccv.zhuiyingzhihanju.api.CommentApi;
import com.bccv.zhuiyingzhihanju.api.MovieInfoApi;
import com.bccv.zhuiyingzhihanju.api.MovieUrlApi;
import com.bccv.zhuiyingzhihanju.model.Comment;
import com.bccv.zhuiyingzhihanju.model.HD;
import com.bccv.zhuiyingzhihanju.model.Movie;
import com.bccv.zhuiyingzhihanju.model.MovieEpisode;
import com.bccv.zhuiyingzhihanju.model.MovieEpisodeNum;
import com.bccv.zhuiyingzhihanju.model.MovieInfo;
import com.bccv.zhuiyingzhihanju.model.MovieSource;
import com.bccv.zhuiyingzhihanju.model.MovieType;
import com.bccv.zhuiyingzhihanju.model.MovieUrl;
import com.bccv.zhuiyingzhihanju.model.RealUrl;
import com.bccv.zhuiyingzhihanju.model.User;
import com.tendcloud.tenddata.TCAgent;
import com.utils.net.NetUtil;
import com.utils.share.ShareSDK;
import com.utils.tools.AnimationManager;
import com.utils.tools.AppConfig;
import com.utils.tools.BaseActivity;
import com.utils.tools.BaseFragmentActivity;
import com.utils.tools.Callback;
import com.utils.tools.GlobalParams;
import com.utils.tools.Logger;
import com.utils.tools.M3U8Utils;
import com.utils.tools.PromptManager;
import com.utils.tools.SerializationUtil;
import com.utils.tools.StringUtils;
import com.utils.tools.SystemUtils;
import com.utils.views.HorizontalListView;
import com.utils.views.MyBattery;
import com.utils.views.MyGridView;
import com.utils.views.MyScrollViewh;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager.WakeLock;
import android.os.Process;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint({ "NewApi", "HandlerLeak", "Wakelock", "DefaultLocale", "ClickableViewAccessibility", "InflateParams" })
public class Video2DPlayerActivity extends BaseFragmentActivity implements OnPreparedListener, OnCompletionListener,
		OnErrorListener, OnInfoListener, OnPlayingBufferCacheListener, OnTouchListener {
	private final String TAG = "VideoViewPlayingActivity";

	/**
	 * 您的ak
	 */
	private String AK = GlobalParams.AK;
	/**
	 * //您的sk的前16位
	 */
	private String SK = GlobalParams.SK;

	private String mVideoSource = "";
	private String titleString = "";
	private String movie_id, type_id, episodes_id, image_url, movie_title;
	private int hd = 2, currSourceNum = 0, maxColumn;
	private boolean isPortrait = false, isComment = true;

	InputMethodManager imm;

	private BVideoView mVV = null;
	private LinearLayout mViewHolder = null;

	private boolean mIsHwDecode = false;

	private EventHandler mEventHandler;
	private HandlerThread mHandlerThread;

	private final Object SYNC_Playing = new Object();

	private final int EVENT_PLAY = 0;
	private MovieInfo movieInfo;
	private WakeLock mWakeLock = null;
	private static final String POWER_LOCK = "VideoPlayerActivity";

	/**
	 * 播放状态
	 */
	private enum PLAYER_STATUS {
		PLAYER_IDLE, PLAYER_PREPARING, PLAYER_PREPARED,
	}

	private PLAYER_STATUS mPlayerStatus = PLAYER_STATUS.PLAYER_IDLE;

	/**
	 * 记录播放位置
	 */
	private int mLastPos = 0;

	class EventHandler extends Handler {
		public EventHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case EVENT_PLAY:
				/**
				 * 如果已经播放了，等待上一次播放结束
				 */
				if (mVV == null) {
					return;
				}
				if (mPlayerStatus != PLAYER_STATUS.PLAYER_IDLE) {
					synchronized (SYNC_Playing) {
						try {
							SYNC_Playing.wait();
							Log.v(TAG, "wait player status to idle");
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				isSeeking = false;
				mHandler.sendEmptyMessage(4);

				/**
				 * 设置播放url
				 */
				// mVideoSource =
				// "http://124.207.162.216/0f35f5a8f099e78ef8376eb253886e49.m3u8?type=web.cloudplay&k=e6f2ff234eb61b5432c91056471ea5e1-2b3b-1464863186&cpn=27565&ppyunid=158425217";
				if (!StringUtils.isEmpty(ua)) {
					mVV.setUserAgent(ua);
				}

				mVV.setVideoPath(mVideoSource);

				/**
				 * 续播，如果需要如此
				 */
				if (mLastPos > 0) {

					mVV.seekTo(mLastPos);
					mLastPos = 0;
				}

				/**
				 * 显示或者隐藏缓冲提示
				 */
				mVV.showCacheInfo(false);

				/**
				 * 开始播放
				 */
				mVV.start();

				mPlayerStatus = PLAYER_STATUS.PLAYER_PREPARING;
				mHandler.sendEmptyMessage(6);
				isCatch = true;
				mHandler.sendEmptyMessage(2);
				break;
			default:
				break;
			}
		}
	}

	private int screenWidth, screenHeight;
	private boolean isEpisode = false;
	private boolean isEnd = true, fromLoading = false;
	View main;
	private Movie historyMovie;
	private String varietyString;

	private void tcStart() {
		TCAgent.onPageStart(getApplicationContext(), "Video2DPlayerActivity");
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		tcStart();
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		// getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
		// WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		main = View.inflate(getApplicationContext(), R.layout.activity_video2dandroidplayer, null);
		// main.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
		// main.setOnSystemUiVisibilityChangeListener(new
		// OnSystemUiVisibilityChangeListener() {
		//
		// @Override
		// public void onSystemUiVisibilityChange(int visibility) {
		// // TODO Auto-generated method stub
		// if (visibility != View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) {
		// if (isLock && !isPortrait) {
		// if (!isLockVisible) {
		// isLockVisible = true;
		// lockButton.setVisibility(View.VISIBLE);
		// }
		// return;
		// }
		// isViewcontrollerShow = true;
		// showViewController();
		// }
		// }
		// });
		setContentView(main);
		// PowerManager pm = (PowerManager)
		// getSystemService(Context.POWER_SERVICE);
		// mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
		// PowerManager.ON_AFTER_RELEASE, POWER_LOCK);

		main.requestFocus();

		imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		WindowManager wm = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
		screenWidth = wm.getDefaultDisplay().getWidth();
		screenHeight = wm.getDefaultDisplay().getHeight();

		maxColumn = SystemUtils.getMaxActivityColumn(this);// 最大音量

		mIsHwDecode = getIntent().getBooleanExtra("isHW", false);
		movie_id = getIntent().getStringExtra("movie_id");
		type_id = getIntent().getStringExtra("type_id");
		varietyString = getIntent().getStringExtra("variety");
		episodes_id = getIntent().getStringExtra("episodes_id");

		isEpisode = getIntent().getBooleanExtra("isEpisode", false);
		isEpisode = true;
		fromLoading = getIntent().getBooleanExtra("fromLoading", false);

		movieInfo = (MovieInfo) getIntent().getSerializableExtra("movieInfo");
		if (movieInfo != null) {
			titleString = movieInfo.getTitle();
		}

		if (StringUtils.isEmpty(episodes_id)) {
			episodes_id = "0";
		}

		// movie_id = "1";
		// type_id = "8";
		// movie = (Movie) getIntent().getSerializableExtra("movie");
		//
		// titleString = movie.getTitle();
		// movie_id = movie.getId();
		// type_id = movie.getType_id();
		// episodes_id = movie.getEpisode_id();

		// getMovieInfoData();
		// GlobalParams.hasLogin = true;
		// GlobalParams.user = new User();
		// GlobalParams.user.setUser_id("1102");
		historyList = SerializationUtil.readHistoryCache(getApplicationContext());
		if (historyList == null) {
			historyList = new ArrayList<>();
			historyMovie = new Movie();
			historyMovie.setId(movie_id);
			historyMovie.setType_id(type_id);
			historyMovie.setCurrSourceNum(currSourceNum);
			historyMovie.setImages(movieInfo.getBimages());
			historyMovie.setTitle(movieInfo.getTitle());
			historyMovie.setEpisode_id(episodes_id);

			historyMovie.setPlay_Time(0);
			historyList.add(0, historyMovie);
		} else {
			List<Movie> reMovies = new ArrayList<>();
			for (int i = 0; i < historyList.size(); i++) {
				Movie movie = historyList.get(i);
				if (movie.getId() != null) {
					if (movie.getId().equals(movie_id)) {
						if (movie.getType_id() != null) {
							if (movie.getType_id().equals(type_id)) {
								historyMovie = movie;
								if (historyMovie.getEpisode_id().equals(episodes_id)) {
									mLastPos = (int) historyMovie.getPlay_Time();
								} else {
									historyMovie.setEpisode_id(episodes_id);
									historyMovie.setPlay_Time(0);
									mLastPos = 0;
								}
								titleString = historyMovie.getTitle();
							}
						} else {
							reMovies.add(movie);
						}

					}
				} else {
					reMovies.add(movie);
				}

			}

			if (reMovies.size() > 0) {
				historyList.removeAll(reMovies);
			}

			if (historyMovie == null) {
				historyMovie = new Movie();
				historyMovie.setId(movie_id);
				historyMovie.setType_id(type_id);
				historyMovie.setEpisode_id(episodes_id);
				historyMovie.setPlay_Time(0);
				historyMovie.setCurrSourceNum(currSourceNum);
				historyMovie.setImages(movieInfo.getBimages());
				historyMovie.setTitle(movieInfo.getTitle());
				historyList.add(0, historyMovie);
			} else {
				historyList.remove(historyMovie);
				historyList.add(0, historyMovie);
			}
		}
		historyMovie.setPlay_Date(System.currentTimeMillis());
		
		if (!StringUtils.isEmpty(episodes_id)) {
			isEpisode = true;
			if (type_id.equals("variety")) {
				titleString += "  " + varietyString;
			}else{
				titleString += "第" + episodes_id + "集";
			}
			
			if (episodes_id.equals("0")) {
				isEnd = true;
			} else {
				isEnd = false;
			}

		}
		initUI();
		// getSource();
		// getEpisode();

		mHandlerThread = new HandlerThread("event handler thread", Process.THREAD_PRIORITY_BACKGROUND);
		mHandlerThread.start();
		mEventHandler = new EventHandler(mHandlerThread.getLooper());
		// 添加到历史记录里
		
		// if (historyList != null && historyList.size() > 0 &&
		// historyList.get(0).getPlay_Time() > 0) {
		// showWifiDialog(this);
		// } else {

		getData();// 获取数据
		// }
		// startListener();// 横竖屏切换
	}

	// @Override
	// public void onConfigurationChanged(Configuration newConfig) {
	// // TODO Auto-generated method stub
	// super.onConfigurationChanged(newConfig);
	// if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
	// portTopLayout.setVisibility(View.GONE);
	// isViewcontrollerShow = false;
	// hideViewController();
	// // portBottomLayout.setVisibility(View.GONE);
	// isPortrait = false;
	// holder.setVisibility(View.GONE);
	// closeComment();
	// } else {
	// isViewcontrollerShow = false;
	// hideViewController();
	// portTopLayout.setVisibility(View.VISIBLE);
	// isPortrait = true;
	// holder.setVisibility(View.VISIBLE);
	// }
	// }

	private int currEpisodeNum = 0;
	private List<MovieEpisode> getEpisodeList = new ArrayList<>();
	private List<MovieSource> movieSourceList = new ArrayList<>();

	private void getSource() {
		if (getSourceList == null) {
			return;
		}

		for (int i = 0; i < movieSourceList.size(); i++) {
			MovieSource movieSource = movieSourceList.get(i);
			if (episodes_id.equals("0")) {
				episodes_id = movieSourceList.get(movieSourceList.size() - 1).getEpisodes_id();

			}
			if (movieSource.getEpisodes_id().equals(episodes_id)) {
				List<MovieUrl> sourceTextList = movieSource.getSource_text();
				if (sourceTextList == null) {
					continue;
				}
				currSourceNum = 0;
				for (int j = 0; j < sourceTextList.size(); j++) {
					if (sourceTextList.get(j).getSource_id().equals(getSourceList.get(currSourceNum).getSource_id())) {
						currSourceNum = j;
						sourceTextList.get(j).setSelect(true);
					} else {
						sourceTextList.get(j).setSelect(false);
					}
				}
				sourceTextList.get(currSourceNum).setSelect(true);
				getSourceList = sourceTextList;
				break;
			}

		}

		if (getSourceList != null && getSourceList.size() > 0) {
			sourceList.clear();
			sourceList.addAll(getSourceList);
			currSourceTextView.setText(sourceList.get(currSourceNum).getSource_name());
			playerSourceAdapter.notifyDataSetChanged();
		}
	}

	private void getEpisode() {
		getEpisodeList.clear();
		for (int i = 0; i < movieSourceList.size(); i++) {
			MovieSource movieSource = movieSourceList.get(i);
			MovieEpisode movieEpisode = new MovieEpisode();
			movieEpisode.setId(movieSource.getEpisodes_id());
			movieEpisode.setDes(movieSource.getDes());
			movieEpisode.setPeriods(movieSource.getPeriods());

			if (i == 0) {
				if (!StringUtils.isEmpty(movieSource.getPeriods())) {
					gridViewNum = 2;
				} else if (!StringUtils.isEmpty(movieSource.getDes())) {
					gridViewNum = 3;
				} else {
					gridViewNum = 6;
				}
			}

			if (!StringUtils.isEmpty(movieSource.getEpisodes_id())
					&& movieSource.getEpisodes_id().equals(episodes_id)) {
				movieEpisode.setSelect(true);
				currEpisodeNum = i;
			} else {
				movieEpisode.setSelect(false);
			}

			// getEpisodeList.add(movieEpisode);
			getEpisodeList.add(0, movieEpisode);
		}

		// if (currEpisodeNum == getEpisodeList.size() - 1) {
		// isEnd = true;
		// }
		currEpisodeNum = getEpisodeList.size() - 1 - currEpisodeNum;
		if (currEpisodeNum == 0) {
			isEnd = true;
		}

		if (type_id.equals("movie")) {
			isEnd = true;
		}

		if (!isEnd) {
			nextButton.setVisibility(View.VISIBLE);
		} else {
			nextButton.setVisibility(View.GONE);
		}

		episodeList.clear();
		episodeList.addAll(getEpisodeList);
		episodeListView.setNumColumns(gridViewNum);
		playerEpisodeAdapter.setGridNum(gridViewNum);
		playerEpisodeAdapter.notifyDataSetChanged();
		if (episodeList.size() <= 0) {
			episodeTextView.setVisibility(View.GONE);
		}
		downloadList.clear();
		for (int i = 0; i < getEpisodeList.size(); i++) {

			MovieEpisode movieEpisode = new MovieEpisode();
			movieEpisode.setId(getEpisodeList.get(i).getId());
			movieEpisode.setDes(getEpisodeList.get(i).getDes());
			movieEpisode.setPeriods(getEpisodeList.get(i).getPeriods());
			movieEpisode.setSelect(false);
			downloadList.add(movieEpisode);
		}
		episodeGridView.setNumColumns(gridViewNum);
		episodeDownAdapter.setGridNum(gridViewNum);
		episodeDownAdapter.notifyDataSetChanged();
	}

	private List<Movie> historyList;
	private List<MovieUrl> sourceList, getSourceList;

	private void getData() {
		isCatch = false;
		// if (sourceList != null && sourceList.size() > 0) {
		// MovieUrl movieUrl = sourceList.get(currSourceNum);
		// source_id = movieUrl.getSource_id();
		// getUrl(movieUrl.getSource_id());
		// }

		showLoading();
		Callback callback = new Callback() {

			@Override
			public void handleResult(String result) {
				// TODO Auto-generated method stub
				if (lefttitleTextView == null) {
					return;
				}
				if (getSourceList != null && getSourceList.size() > 0) {
					getSource();
					getEpisode();
					hideLoading();
					if (!episodes_id.equals("0")) {
						historyMovie.setEpisode_id(episodes_id);
					}
					source_id = getSourceList.get(currSourceNum).getSource_url();
					getUrl(source_id);

				} else {
					mVideoSource = "";
					Toast.makeText(getApplicationContext(), "解析失败", Toast.LENGTH_SHORT).show();
					// if (currSourceNum == list.size() - 1) {
					// currSourceNum = 0;
					// } else {
					// currSourceNum++;
					// }
					// sourceHandler.sendEmptyMessage(0);
					hideLoading();
				}

			}
		};
		new DataAsyncTask(callback, false) {

			@Override
			protected String doInBackground(String... params) {
				// TODO Auto-generated method stub
				MovieUrlApi movieUrlApi = new MovieUrlApi();
				currSourceNum = 0;
				if (isEpisode) {
					movieSourceList = movieUrlApi.getMovieEpidsodeUrlList(movie_id, type_id);
					if (movieSourceList != null && movieSourceList.size() > 0) {
						getSourceList = new ArrayList<>();
						getSourceList.addAll(movieSourceList.get(0).getSource_text());
						for (int i = 1; i < movieSourceList.size(); i++) {
							List<MovieUrl> movieUrls = movieSourceList.get(i).getSource_text();
							if (movieUrls != null && !getSourceList.containsAll(movieUrls)) {
								for (int j = 0; j < movieUrls.size(); j++) {
									if (!getSourceList.contains(movieUrls.get(j))) {
										getSourceList.add(movieUrls.get(j));
									}
								}
							}
						}
					}

				} else {
					getSourceList = movieUrlApi.getMovieUrlList(movie_id, type_id);
				}

				// MovieInfoApi movieInfoApi = new MovieInfoApi();
				// movieInfo = movieInfoApi.getMovieInfo(movie_id, type_id);
				//
				// CommentApi commentApi = new CommentApi();
				// getCommentList = commentApi.getCommentList(movie_id, type_id,
				// "1", "5");
				return null;
			}
		}.execute("");

		// Callback infoCallBack = new Callback() {
		//
		// @Override
		// public void handleResult(String result) {
		// // TODO Auto-generated method stub
		// if (movieInfo != null) {
		// titleString = movieInfo.getTitle();
		//
		// titleString += "第" + episodes_id + "集";
		// lefttitleTextView.setText(titleString);
		// setInfoData();
		// if (historyMovie != null) {
		// historyMovie.setCurrSourceNum(currSourceNum);
		// historyMovie.setImages(movieInfo.getBimages());
		// historyMovie.setTitle(movieInfo.getTitle());
		// }
		//
		// } else {
		// titleString = "";
		// Toast.makeText(getApplicationContext(), "未获取到详情信息", 1).show();
		// }
		// }
		// };
		//
		// new DataAsyncTask(infoCallBack, false) {
		//
		// @Override
		// protected String doInBackground(String... params) {
		// // TODO Auto-generated method stub
		//
		// MovieInfoApi movieInfoApi = new MovieInfoApi();
		// movieInfo = movieInfoApi.getMovieInfo(movie_id, type_id);
		//
		// CommentApi commentApi = new CommentApi();
		// getCommentList = commentApi.getCommentList(movie_id, type_id, "1",
		// "5");
		// return null;
		// }
		// }.execute("");

	}

	private List<Comment> getCommentList;

	private void getNext() {
		isCatch = false;
		isChange = true;
		showLoading();
		Callback callback = new Callback() {

			@Override
			public void handleResult(String result) {
				// TODO Auto-generated method stub
				if (!StringUtils.isEmpty(mVideoSource)) {
					mLastPos = 0;
					playMovie();
				} else {
					Toast.makeText(getApplicationContext(), "解析失败", Toast.LENGTH_SHORT).show();
					// if (currSourceNum == list.size() - 1) {
					// currSourceNum = 0;
					// } else {
					// currSourceNum++;
					// }
					// sourceHandler.sendEmptyMessage(0);
				}
				hideLoading();
			}
		};
		new DataAsyncTask(callback, false) {

			@Override
			protected String doInBackground(String... params) {
				// TODO Auto-generated method stub
				MovieUrlApi movieUrlApi = new MovieUrlApi();
				getSourceList = movieUrlApi.getMovieUrlList(movie_id, type_id);
				currSourceNum = 0;
				if (getSourceList != null && getSourceList.size() > 0) {

					RealUrl realUrl = movieUrlApi.getUrl(getSourceList.get(0).getSource_id(), hd + "", false);
					if (realUrl != null) {
						mVideoSource = realUrl.getUrl();
						ua = realUrl.getUseragent();
					} else {
						mVideoSource = "";
					}

				} else {
					mVideoSource = "";
				}

				return null;
			}
		}.execute("");
	}

	private DataAsyncTask getUrlTask;
	private String ua = "";
	private int geturl = 0;

	private void getUrl(final String url) {
		isCatch = false;
		showLoading();
		Callback callback = new Callback() {

			@Override
			public void handleResult(String result) {
				// TODO Auto-generated method stub
				if (!StringUtils.isEmpty(mVideoSource)) {
					// mLastPos = (int) historyList.get(0).getPlay_Time();

					playMovie();
				} else {
					geturl++;
					if (geturl == 2) {
						geturl = 0;
					} else {
						getUrl(url);
						return;
					}

					if (currSourceNum >= sourceList.size() - 1) {
						Toast.makeText(getApplicationContext(), "解析失败", Toast.LENGTH_SHORT).show();
					} else {
						isChange = true;
						sourceList.get(currSourceNum).setSelect(false);
						currSourceNum++;
						int position = currSourceNum;
						sourceList.get(position).setSelect(true);
						currSourceNum = position;
						if (mVV == null) {
							return;
						}
						if (mPlayerStatus == PLAYER_STATUS.PLAYER_PREPARED) {

							mLastPos = mVV.getCurrentPosition();
							historyMovie.setPlay_Time(mLastPos);
						}
						mVV.stopPlayback();
						playerSourceAdapter.notifyDataSetChanged();
						currSourceTextView.setText(sourceList.get(position).getSource_name());
						source_id = sourceList.get(position).getSource_url();
						if (isEpisode) {
							getEpisode();
						}
						getUrl(source_id);
					}
				}
				hideLoading();
			}
		};
		getUrlTask = new DataAsyncTask(callback, false) {

			@Override
			protected String doInBackground(String... params) {
				// TODO Auto-generated method stub
				MovieUrlApi movieUrlApi = new MovieUrlApi();
				RealUrl realUrl = movieUrlApi.getUrl(url, hd + "", false);
				if (realUrl != null) {
					mVideoSource = realUrl.getUrl();
					ua = realUrl.getUseragent();
				} else {
					mVideoSource = "";
				}

				return null;
			}
		};
		getUrlTask.execute("");
	}

	private void playMovie() {
		/**
		 * 开启后台事件处理线程
		 */

		mEventHandler.sendEmptyMessage(EVENT_PLAY);
	}

	private LinearLayout holder;

	/**
	 * 初始化界面
	 */
	private void initUI() {
		mViewHolder = (LinearLayout) findViewById(R.id.videoview_holder);
		holder = (LinearLayout) findViewById(R.id.holder);
		/**
		 * 设置ak及sk的前16位
		 */
		BVideoView.setAKSK(AK, SK);

		/**
		 * 创建BVideoView和BMediaController
		 */
		mVV = new BVideoView(this);
		mViewHolder.addView(mVV);
		mVV.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);

		/**
		 * 注册listener
		 */
		mVV.setOnPreparedListener(this);
		mVV.setOnCompletionListener(this);
		mVV.setOnErrorListener(this);
		mVV.setOnInfoListener(this);
		mVV.setOnPlayingBufferCacheListener(this);
		mVV.setOnNetworkSpeedListener(new OnNetworkSpeedListener() {

			@Override
			public void onNetworkSpeedUpdate(int arg0) {
				// TODO Auto-generated method stub
				netSpeed = arg0;
			}
		});

		// /**
		// * 设置解码模式
		// */
		// mVV.setDecodeMode(BVideoView.DECODE_MHW_AUTO);

		/**
		 * 设置解码模式
		 */
		mVV.setDecodeMode(mIsHwDecode ? BVideoView.DECODE_HW : BVideoView.DECODE_SW);

		mViewHolder.setOnTouchListener(this);
		mVV.start();
		mVV.stopPlayback();
		initViewController();

	}

	private void resetVideo() {
		mViewHolder.removeAllViews();

		mVV = null;

		/**
		 * 创建BVideoView和BMediaController
		 */
		mVV = new BVideoView(this);
		mViewHolder.addView(mVV);
		mVV.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);

		/**
		 * 注册listener
		 */
		mVV.setOnPreparedListener(this);
		mVV.setOnCompletionListener(this);
		mVV.setOnErrorListener(this);
		mVV.setOnInfoListener(this);
		mVV.setOnPlayingBufferCacheListener(this);
		mVV.setOnNetworkSpeedListener(new OnNetworkSpeedListener() {

			@Override
			public void onNetworkSpeedUpdate(int arg0) {
				// TODO Auto-generated method stub
				netSpeed = arg0;
			}
		});

		// /**
		// * 设置解码模式
		// */
		// mVV.setDecodeMode(BVideoView.DECODE_MHW_AUTO);

		/**
		 * 设置解码模式
		 */
		mVV.setDecodeMode(mIsHwDecode ? BVideoView.DECODE_HW : BVideoView.DECODE_SW);

		mVV.start();
		mVV.stopPlayback();
	}

	private LinearLayout bottomLayout, topLayout, portBottomLayout, portTopLayout, sourceLayout, episodeLayout,
			hdLayout, loadingLayout, commentLayout, commentClickLayout, shareLayout, downloadLayout;
	private Button sendButton;
	private EditText commentEditText;

	private GridView episodeGridView;
	private DownloadEpisodeAdapter episodeDownAdapter;
	private List<MovieEpisode> downloadList;
	private int episodeSelectNum;

	private ImageButton leftplayButton, leftbackButton, nextButton, lockButton, portPlayButton, portBackButton;
	private SeekBar leftBar, portSeekBar;
	private TextView leftcurrTextView, leftdurationTextView, portcurrTextView, portdurationTextView, lefttitleTextView,
			leftstateButton, brightTextView, columnTextView, scrollTextView, loadingTextView, episodeTextView,
			currSourceTextView, commentClickEditText, cancelTextView, downloadSure, downloadCancel;
	private ImageView leftLoadingImageView, portFullImageView, shareQQ, shareQZ, shareWX, shareWXC, shareSina;
	private ListView sourceListView, hdListView;
	private GridView episodeListView;
	private List<HD> hdList;
	private List<MovieEpisode> episodeList;
	private PlayerEpisodeAdapter playerEpisodeAdapter;
	private PlayerHDAdapter playerHDAdapter;
	private PlayerSourceAdapter playerSourceAdapter;

	private boolean isPlaying = true;
	private boolean isViewcontrollerShow = false, isHdSelectShow = false;
	private boolean isSeeking = false, isLock = false;

	private MyBattery battery;// 电池

	// 初始化播放器界面
	private void initViewController() {
		battery = (MyBattery) findViewById(R.id.battery_layout);

		shareAnimLayout = (LinearLayout) findViewById(R.id.share_anim_layout);
		downloadAnimLayout = (LinearLayout) findViewById(R.id.download_anim_layout);

		topLayout = (LinearLayout) findViewById(R.id.top_layout);
		bottomLayout = (LinearLayout) findViewById(R.id.bottom_layout);
		portTopLayout = (LinearLayout) findViewById(R.id.port_top_layout);
		portBottomLayout = (LinearLayout) findViewById(R.id.port_bottom_layout);

		portPlayButton = (ImageButton) findViewById(R.id.port_play_button);
		portBackButton = (ImageButton) findViewById(R.id.port_back_button);
		portcurrTextView = (TextView) findViewById(R.id.port_curr_textView);
		portdurationTextView = (TextView) findViewById(R.id.port_duration_textView);
		portSeekBar = (SeekBar) findViewById(R.id.port_seekBar);
		portFullImageView = (ImageView) findViewById(R.id.port_change_imageView);
		portBackButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (fromLoading) {
					Intent intent = new Intent(getApplicationContext(), MainActivity.class);
					startActivity(intent);
				}
				finish();
			}
		});

		portTopLayout.setVisibility(View.GONE);
		// isViewcontrollerShow = false;
		// // hideViewController();
		// isPortrait = false;
		// holder.setVisibility(View.GONE);
		portBottomLayout.clearAnimation();
		portBottomLayout.setVisibility(View.GONE);
		//
		//// Video2DPlayerActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		// mIsLand = true;
		// mClickLand = false;

		// 全屏
		portFullImageView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mClick = true;
				if (!mIsLand) {
					portTopLayout.setVisibility(View.GONE);
					isViewcontrollerShow = false;
					// hideViewController();
					isPortrait = false;
					holder.setVisibility(View.GONE);
					portBottomLayout.clearAnimation();
					portBottomLayout.setVisibility(View.GONE);
					closeComment();
					Video2DPlayerActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
					mIsLand = true;
					mClickLand = false;
				}

			}
		});

		// 评论
		commentLayout = (LinearLayout) findViewById(R.id.comment_layout);
		commentLayout.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				commentLayout.setVisibility(View.GONE);
				imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
			}
		});
		commentEditText = (EditText) findViewById(R.id.comment_editText);
		sendButton = (Button) findViewById(R.id.send_button);
		sendButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (!StringUtils.isEmpty(commentEditText.getText().toString())) {
					Comment comment = new Comment();
					comment.setContent(commentEditText.getText().toString());
					comment.setUser_info(GlobalParams.user);
					comment.setCtime(System.currentTimeMillis() / 1000);
					if (!isComment) {
						User user = new User();
						user.setNick_name(commentJsonObject.getString("from_nickname"));
						comment.setF_user_info(user);
					}
					infoCommentList.add(0, comment);
					infoCommentAdapter.notifyDataSetChanged();
					String platform = android.os.Build.MODEL;
					if (isComment) {
						commentMovie(commentEditText.getText().toString(), "", platform);
					} else {
						commentMovie(commentEditText.getText().toString(), commentJsonObject.getString("from_id"),
								platform);
					}

				}
				commentLayout.setVisibility(View.GONE);
				imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
			}
		});

		commentClickLayout = (LinearLayout) findViewById(R.id.commentclick_layout);
		commentClickEditText = (TextView) findViewById(R.id.commentclick_editText);
		commentClickEditText.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (GlobalParams.hasLogin) {
					commentClickLayout.setVisibility(View.GONE);
					showComment();
				} else {
					startLogin();
				}

			}
		});

		// 分享
		shareLayout = (LinearLayout) findViewById(R.id.share_layout);
		shareLayout.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				hideShare();
			}
		});
		cancelTextView = (TextView) findViewById(R.id.cancel_textView);
		cancelTextView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				shareLayout.setVisibility(View.GONE);
			}
		});

		shareQQ = (ImageView) findViewById(R.id.share_QQ);
		shareQQ.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (shareSDK != null) {
					shareSDK.shareQQ(shareObject.getString("title"), shareObject.getString("content"),
							shareObject.getString("thumb"), shareObject.getString("url"));
				}
			}
		});
		shareQZ = (ImageView) findViewById(R.id.share_QQspace);
		shareQZ.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (shareSDK != null) {
					shareSDK.shareQZone(shareObject.getString("title"), shareObject.getString("content"),
							shareObject.getString("thumb"), shareObject.getString("url"));
				}
			}
		});
		shareWX = (ImageView) findViewById(R.id.share_weixin);
		// shareWX.setOnClickListener(new OnClickListener() {
		//
		// @Override
		// public void onClick(View v) {
		// // TODO Auto-generated method stub
		// if (shareSDK != null) {
		// shareSDK.shareWeiXin(shareObject.getString("title"),
		// shareObject.getString("content"),
		// shareObject.getString("thumb"), shareObject.getString("url"));
		// }
		// }
		// });
		shareWXC = (ImageView) findViewById(R.id.share_winxinquan);
		shareWXC.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (shareSDK != null) {
					shareSDK.shareWeiXinCircle(shareObject.getString("title"), shareObject.getString("content"),
							shareObject.getString("thumb"), shareObject.getString("url"));
				}
			}
		});
		shareSina = (ImageView) findViewById(R.id.share_sina);
		shareSina.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (shareSDK != null) {
					shareSDK.shareSina(shareObject.getString("title"), shareObject.getString("content"),
							shareObject.getString("thumb"), shareObject.getString("url"));
				}
			}
		});

		// 下载
		downloadLayout = (LinearLayout) findViewById(R.id.download_layout);
		downloadLayout.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				hideDownload();
			}
		});

		downloadSure = (TextView) findViewById(R.id.download_sure_textView);
		downloadCancel = (TextView) findViewById(R.id.download_cancel_textView);
		episodeGridView = (MyGridView) findViewById(R.id.episode_gridView);
		downloadList = new ArrayList<>();
		episodeDownAdapter = new DownloadEpisodeAdapter(getApplicationContext(), downloadList);
		episodeDownAdapter.setMovieInfo(movie_id, type_id);
		episodeGridView.setAdapter(episodeDownAdapter);
		episodeGridView.setSelector(new ColorDrawable(android.R.color.transparent));
		episodeGridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub
				boolean isSelect = downloadList.get(position).isSelect();
				downloadList.get(position).setSelect(!isSelect);
				if (isSelect) {
					episodeSelectNum--;
				} else {
					episodeSelectNum++;
				}

				if (episodeSelectNum > 0) {
					downloadSure.setSelected(true);
				} else {
					downloadSure.setSelected(false);
				}
				episodeDownAdapter.notifyDataSetChanged();
			}
		});

		downloadCancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				hideDownload();
			}
		});

		downloadSure.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (downloadSure.isSelected()) {
					startDownload();
				}
			}
		});

		loadingLayout = (LinearLayout) findViewById(R.id.loading_layout);
		loadingTextView = (TextView) findViewById(R.id.loading_textView);

		scrollTextView = (TextView) findViewById(R.id.scroll_textView);
		brightTextView = (TextView) findViewById(R.id.bright_textView);
		columnTextView = (TextView) findViewById(R.id.column_textView);

		// DisplayMetrics dm = new DisplayMetrics();
		// getWindowManager().getDefaultDisplay().getMetrics(dm);
		// int screenH = dm.widthPixels;

		sourceLayout = (LinearLayout) findViewById(R.id.source_layout);
		// ViewGroup.LayoutParams params = sourceLayout.getLayoutParams();
		// params.height = screenH - topLayout.getLayoutParams().height -
		// bottomLayout.getLayoutParams().height;
		// sourceLayout.setLayoutParams(params);
		sourceList = new ArrayList<MovieUrl>();
		sourceListView = (ListView) findViewById(R.id.source_listView);
		sourceListView.setDividerHeight(0);
		sourceListView.setSelector(new ColorDrawable(android.R.color.transparent));
		playerSourceAdapter = new PlayerSourceAdapter(getApplicationContext(), sourceList);
		sourceListView.setAdapter(playerSourceAdapter);
		sourceListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub
				startHideViewControllerTimer();
				isChange = true;
				sourceList.get(currSourceNum).setSelect(false);
				sourceList.get(position).setSelect(true);
				currSourceNum = position;
				if (mVV == null) {
					return;
				}
				if (mPlayerStatus == PLAYER_STATUS.PLAYER_PREPARED) {
					mLastPos = mVV.getCurrentPosition();
					historyMovie.setPlay_Time(mLastPos);
				}
				mVV.stopPlayback();
				playerSourceAdapter.notifyDataSetChanged();
				currSourceTextView.setText(sourceList.get(position).getSource_name());
				source_id = sourceList.get(position).getSource_url();
				if (isEpisode) {
					getEpisode();
				}
				getUrl(source_id);
			}
		});
		currSourceTextView = (TextView) findViewById(R.id.currSource_textView);
		currSourceTextView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				startHideViewControllerTimer();
				if (isSourceShow) {
					hideSourceSelect();
				} else {
					showSourceSelect();
				}
			}
		});
		// 清晰度
		hdLayout = (LinearLayout) findViewById(R.id.hd_layout);
		// ViewGroup.LayoutParams params1 = hdLayout.getLayoutParams();
		// params1.height = screenH - topLayout.getLayoutParams().height -
		// bottomLayout.getLayoutParams().height;
		// hdLayout.setLayoutParams(params1);
		leftstateButton = (TextView) findViewById(R.id.hd_textView);
		leftstateButton.setVisibility(View.VISIBLE);
		leftstateButton.setText("标清");
		hdListView = (ListView) findViewById(R.id.hd_listView);
		hdListView.setDividerHeight(0);
		hdListView.setSelector(new ColorDrawable(android.R.color.transparent));
		hdList = new ArrayList<>();

		HD hd0 = new HD();
		hd0.setId("流畅");
		hd0.setSelect(false);
		hdList.add(hd0);

		HD hd1 = new HD();
		hd1.setId("标清");
		hd1.setSelect(true);
		hdList.add(hd1);

		HD hd2 = new HD();
		hd2.setId("高清");
		hd2.setSelect(false);
		hdList.add(hd2);

		playerHDAdapter = new PlayerHDAdapter(getApplicationContext(), hdList);
		hdListView.setAdapter(playerHDAdapter);
		hdListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub
				startHideViewControllerTimer();
				isChange = true;
				hdList.get(hd - 1).setSelect(false);
				hdList.get(position).setSelect(true);
				leftstateButton.setText(hdList.get(position).getId());
				hd = position + 1;
				if (mVV == null) {
					return;
				}
				if (mPlayerStatus == PLAYER_STATUS.PLAYER_PREPARED) {
					mLastPos = mVV.getCurrentPosition();
					historyMovie.setPlay_Time(mLastPos);
				}
				mVV.stopPlayback();
				playerHDAdapter.notifyDataSetChanged();
				getUrl(source_id);
			}
		});
		// 集数
		episodeLayout = (LinearLayout) findViewById(R.id.episode_layout);
		episodeLayout.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (isEpisodeShow) {
					startHideViewControllerTimer();
					hideEpisodeSelect();
					showViewController();
				} else {
					hideViewController();
					showEpisodeSelect();
				}
			}
		});
		// ViewGroup.LayoutParams params2 = episodeLayout.getLayoutParams();
		// params2.height = screenH - topLayout.getLayoutParams().height -
		// bottomLayout.getLayoutParams().height;
		// episodeLayout.setLayoutParams(params2);
		episodeListView = (GridView) findViewById(R.id.episode_listView);
		episodeListView.setSelector(new ColorDrawable(android.R.color.transparent));
		episodeList = new ArrayList<>();
		playerEpisodeAdapter = new PlayerEpisodeAdapter(getApplicationContext(), episodeList);
		episodeListView.setAdapter(playerEpisodeAdapter);
		episodeListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub
				startHideViewControllerTimer();
				if (mVV == null) {
					return;
				}
				isChange = true;

				episodeList.get(currEpisodeNum).setSelect(false);
				episodeList.get(position).setSelect(true);
				episodes_id = episodeList.get(position).getId();
				if (type_id.equals("variety")) {
					varietyString = episodeList.get(position).getPeriods();
				}
				for (int i = 0; i < infoEpisodeList.size(); i++) {
					if (infoEpisodeList.get(i).getId().equals(episodes_id)) {
						infoEpisodeList.get(i).setSelect(true);
					} else {
						infoEpisodeList.get(i).setSelect(false);
					}

				}
				infoEpisodeAdapter.notifyDataSetChanged();
				if (movieInfo != null) {
					titleString = movieInfo.getTitle();
				} else {
					titleString = "";
				}
				if (type_id.equals("variety")) {
					titleString += "  " + varietyString;
				}else{
					titleString += "第" + episodes_id + "集";
				}
				lefttitleTextView.setText(titleString);
				currEpisodeNum = position;

				if (currEpisodeNum == 0) {
					isEnd = true;
				} else {
					isEnd = false;
				}
				// if (currEpisodeNum == episodeList.size() - 1) {
				// isEnd = true;
				// }

				if (type_id.equals("movie")) {
					isEnd = true;
				}

				if (!isEnd) {
					nextButton.setVisibility(View.VISIBLE);
				} else {
					nextButton.setVisibility(View.GONE);
				}

				if (mPlayerStatus == PLAYER_STATUS.PLAYER_PREPARED) {
					mLastPos = 0;
					historyMovie.setPlay_Time(mLastPos);
					historyMovie.setEpisode_id(episodes_id);
				}
				mVV.stopPlayback();
				playerEpisodeAdapter.notifyDataSetChanged();
				getSource();
				source_id = sourceList.get(currSourceNum).getSource_url();
				getUrl(source_id);
			}
		});
		episodeTextView = (TextView) findViewById(R.id.episode_textView);
		if (!isEpisode) {
			episodeTextView.setVisibility(View.GONE);
		}
		episodeTextView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub

				if (isEpisodeShow) {
					startHideViewControllerTimer();
					hideEpisodeSelect();
					showViewController();
				} else {
					hideViewController();
					showEpisodeSelect();
				}

			}
		});

		leftplayButton = (ImageButton) findViewById(R.id.play_button);
		lockButton = (ImageButton) findViewById(R.id.lock_button);
		lockButton.setSelected(false);
		leftbackButton = (ImageButton) findViewById(R.id.back_button);
		leftBar = (SeekBar) findViewById(R.id.seekBar);
		leftcurrTextView = (TextView) findViewById(R.id.curr_textView);
		leftdurationTextView = (TextView) findViewById(R.id.duration_textView);
		lefttitleTextView = (TextView) findViewById(R.id.title_textView);

		leftLoadingImageView = (ImageView) findViewById(R.id.loading_imageView);

		topLayout.setVisibility(View.GONE);
		bottomLayout.setVisibility(View.GONE);

		leftBar.setEnabled(false);
		portSeekBar.setEnabled(false);

		leftbackButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mClick = true;
				// if (mIsLand) {
				// hideViewController();
				// portTopLayout.setVisibility(View.VISIBLE);
				// isPortrait = true;
				// holder.setVisibility(View.VISIBLE);
				// Video2DPlayerActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				// mIsLand = false;
				// mClickPort = false;
				// }
				if (fromLoading) {
					Intent intent = new Intent(getApplicationContext(), MainActivity.class);
					startActivity(intent);
				}
				finish();
			}
		});

		lefttitleTextView.setText(titleString);

		// 下一集
		nextButton = (ImageButton) findViewById(R.id.next_button);
		nextButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				startHideViewControllerTimer();

				isChange = true;
				int position = currEpisodeNum - 1;
				if (position <= 0) {
					position = 0;
					nextButton.setVisibility(View.GONE);
					isEnd = true;
				}

				episodeList.get(currEpisodeNum).setSelect(false);
				episodeList.get(position).setSelect(true);
				episodes_id = episodeList.get(position).getId();
				if (type_id.equals("variety")) {
					varietyString = episodeList.get(position).getPeriods();
				}
				for (int i = 0; i < infoEpisodeList.size(); i++) {
					if (infoEpisodeList.get(i).getId().equals(episodes_id)) {
						infoEpisodeList.get(i).setSelect(true);
					} else {
						infoEpisodeList.get(i).setSelect(false);
					}

				}
				infoEpisodeAdapter.notifyDataSetChanged();

				if (movieInfo != null) {
					titleString = movieInfo.getTitle();
				} else {
					titleString = "";
				}
				if (type_id.equals("variety")) {
					titleString += "  " + varietyString;
				}else{
					titleString += "第" + episodes_id + "集";
				}
				lefttitleTextView.setText(titleString);
				currEpisodeNum = position;
				if (mPlayerStatus == PLAYER_STATUS.PLAYER_PREPARED) {
					mLastPos = 0;
					historyMovie.setPlay_Time(mLastPos);
					historyMovie.setEpisode_id(episodes_id);
				}
				mVV.stopPlayback();
				resetVideo();
				playerEpisodeAdapter.notifyDataSetChanged();
				getSource();
				source_id = sourceList.get(currSourceNum).getSource_url();
				getUrl(source_id);
			}
		});
		if (!isEnd) {
			nextButton.setVisibility(View.VISIBLE);
		}

		// 详情
		initInfoView();

	}

	private void toNext() {
		if (mVV == null) {
			return;
		}
		seekProgress = 0;
		startHideViewControllerTimer();
		if (currEpisodeNum == 0) {
			currEpisodeNum++;
		}
		int position = currEpisodeNum - 1;
		if (position <= 0) {
			position = 0;
			nextButton.setVisibility(View.GONE);
			isEnd = true;
		}

		episodeList.get(currEpisodeNum).setSelect(false);
		episodeList.get(position).setSelect(true);
		episodes_id = episodeList.get(position).getId();
		if (type_id.equals("variety")) {
			varietyString = episodeList.get(position).getPeriods();
		}
		for (int i = 0; i < infoEpisodeList.size(); i++) {
			if (infoEpisodeList.get(i).getId().equals(episodes_id)) {
				infoEpisodeList.get(i).setSelect(true);
			} else {
				infoEpisodeList.get(i).setSelect(false);
			}

		}
		infoEpisodeAdapter.notifyDataSetChanged();
		if (movieInfo != null) {
			titleString = movieInfo.getTitle();
		} else {
			titleString = "";
		}
		if (type_id.equals("variety")) {
			titleString += "  " + varietyString;
		}else{
			titleString += "第" + episodes_id + "集";
		}

		lefttitleTextView.setText(titleString);
		currEpisodeNum = position;

		if (currEpisodeNum == 0) {
			isEnd = true;
		} else {
			isEnd = false;
		}
		// if (currEpisodeNum == episodeList.size() - 1) {
		// isEnd = true;
		// }

		if (type_id.equals("movie")) {
			isEnd = true;
		}

		if (!isEnd) {
			nextButton.setVisibility(View.VISIBLE);
		} else {
			nextButton.setVisibility(View.GONE);
		}

		if (mPlayerStatus == PLAYER_STATUS.PLAYER_PREPARED) {
			mLastPos = 0;
			historyMovie.setPlay_Time(mLastPos);
			historyMovie.setEpisode_id(episodes_id);
		}
		mVV.stopPlayback();
		playerEpisodeAdapter.notifyDataSetChanged();
		getSource();
		source_id = sourceList.get(currSourceNum).getSource_url();
		getUrl(source_id);
	}

	private LinearLayout shareAnimLayout, downloadAnimLayout;
	private TextView infoTitle, infoCollect, infoShare, infoDownload, infoIntro;
	private ImageView infoDrop;
	private HorizontalListView infoEpisodeNumListView, infoLikeListView;
	private ListView infoCommentListView;
	private GridView infoEpisodeGridView;

	private InfoEpisodeNumAdapter infoEpisodeNumAdapter;
	private InfoEpisodeAdapter infoEpisodeAdapter;
	private InfoCommentAdapter infoCommentAdapter;
	private MovieLikeAdapter movieLikeAdapter;

	private int gridViewNum = 6;

	private List<MovieEpisodeNum> infoEpisodeNumList;
	private List<Movie> infoLikeList;
	private List<MovieEpisode> infoEpisodeList;
	private List<Comment> infoCommentList;
	private MyScrollViewh infoScrollView;

	// 初始化详情界面
	private void initInfoView() {

		infoScrollView = (MyScrollViewh) findViewById(R.id.info_scrollView);
		infoScrollView.setOnTouchListener(new OnTouchListener() {
			private float firstY;

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					firstY = event.getY();
				} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
					if (event.getY() < firstY) {
						openComment();
					} else if (event.getY() > firstY) {
						closeComment();
					}
				}
				return false;
			}
		});

		infoTitle = (TextView) findViewById(R.id.info_title_textView);
		infoCollect = (TextView) findViewById(R.id.info_collect_textView);
		infoShare = (TextView) findViewById(R.id.info_share_textView);
		infoDownload = (TextView) findViewById(R.id.info_download_textView);

		infoIntro = (TextView) findViewById(R.id.info_intro_textView);
		infoDrop = (ImageView) findViewById(R.id.info_drop_imageView);

		infoEpisodeNumListView = (HorizontalListView) findViewById(R.id.info_episode_horizontalListView);
		infoEpisodeGridView = (GridView) findViewById(R.id.info_episode_gridView);
		infoCommentListView = (ListView) findViewById(R.id.info_comment_listView);
		infoLikeListView = (HorizontalListView) findViewById(R.id.info_like_horizontalListView);

		infoEpisodeNumList = new ArrayList<>();
		infoEpisodeNumAdapter = new InfoEpisodeNumAdapter(getApplicationContext(), infoEpisodeNumList);
		infoEpisodeNumListView.setAdapter(infoEpisodeNumAdapter);
		infoEpisodeNumListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub
				for (int i = 0; i < infoEpisodeNumList.size(); i++) {
					infoEpisodeNumList.get(i).setSelect(false);
				}
				infoEpisodeNumList.get(position).setSelect(true);
				infoEpisodeNumAdapter.notifyDataSetChanged();

				getEpisodeData(position);
				infoEpisodeAdapter.notifyDataSetChanged();
			}
		});

		infoEpisodeList = new ArrayList<>();
		infoEpisodeAdapter = new InfoEpisodeAdapter(getApplicationContext(), infoEpisodeList);
		infoEpisodeGridView.setAdapter(infoEpisodeAdapter);
		infoEpisodeGridView.setSelector(new ColorDrawable(android.R.color.transparent));
		infoEpisodeGridView.setNumColumns(6);
		infoEpisodeGridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub
				for (int i = 0; i < infoEpisodeList.size(); i++) {
					infoEpisodeList.get(i).setSelect(false);
				}
				infoEpisodeList.get(position).setSelect(true);
				infoEpisodeAdapter.notifyDataSetChanged();
				changeEpisode(infoEpisodeList.get(position).getId());
			}
		});

		// 猜你喜欢
		infoLikeList = new ArrayList<>();
		movieLikeAdapter = new MovieLikeAdapter(this, infoLikeList);
		infoLikeListView.setAdapter(movieLikeAdapter);
		infoLikeListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub
				Movie movie = infoLikeList.get(position);
				Intent intent = new Intent(getApplicationContext(), Video2DPlayerActivity.class);
				intent.putExtra("movie_id", movie.getId());
				intent.putExtra("type_id", movie.getType_id());
				intent.putExtra("episodes_id", "0");
				intent.putExtra("isEpisode", true);
				startActivity(intent);
				finish();
			}
		});
		;

		// 详情评论
		infoCommentList = new ArrayList<>();
		infoCommentAdapter = new InfoCommentAdapter(this, infoCommentList);
		infoCommentListView.setAdapter(infoCommentAdapter);
		infoCommentListView.setSelector(new ColorDrawable(android.R.color.transparent));
		infoCommentListView.setDividerHeight(0);
		infoCommentListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub
				if (GlobalParams.hasLogin) {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("user_id", GlobalParams.user.getUid());
					jsonObject.put("from_id", infoCommentList.get(position).getId());
					jsonObject.put("from_nickname", infoCommentList.get(position).getUser_info().getNick_name());
					showReply(jsonObject);
				} else {
					startLogin();
				}

			}
		});
	}

	// 设置详情数据
	private void setInfoData() {
		infoTitle.setText(movieInfo.getTitle());

		if (type_id.equals("music") || type_id.equals("5")) {
			infoIntro.setText(movieInfo.getCasts());
		} else if (type_id.equals("news") || type_id.equals("13")) {
			infoIntro.setText(movieInfo.getDes());
		} else {
			infoIntro.setText(movieInfo.getDirectors() + "\n" + movieInfo.getCasts());
		}

		// 简介下拉
		infoDrop.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				infoDrop.setSelected(!infoDrop.isSelected());
				if (infoDrop.isSelected()) {
					if (type_id.equals("music") || type_id.equals("5")) {
						infoIntro.setText(movieInfo.getCasts() + "\n" + "简介：" + movieInfo.getSummary());
					} else if (type_id.equals("news") || type_id.equals("13")) {
						infoIntro.setText(movieInfo.getDes() + "\n" + "简介：" + movieInfo.getSummary());
					} else {
						infoIntro.setText(movieInfo.getDirectors() + "\n" + movieInfo.getCasts() + "\n" + "简介："
								+ movieInfo.getSummary());
					}

				} else {
					if (type_id.equals("music") || type_id.equals("5")) {
						infoIntro.setText(movieInfo.getCasts());
					} else if (type_id.equals("news") || type_id.equals("13")) {
						infoIntro.setText(movieInfo.getDes());
					} else {
						infoIntro.setText(movieInfo.getDirectors() + "\n" + movieInfo.getCasts());
					}
				}
			}
		});

		infoLikeList.addAll(movieInfo.getMore());
		movieLikeAdapter.notifyDataSetChanged();

		int currNum = 0;

		int gridPageNum = gridViewNum * 2;

		for (int i = 0; i < getEpisodeList.size(); i++) {
			if (getEpisodeList.get(i).getId().equals(episodes_id)) {
				currNum = i / gridPageNum;
			}
		}

		int num = (getEpisodeList.size() - 1) / gridPageNum;
		for (int i = 0; i < num + 1; i++) {
			MovieEpisodeNum movieEpisodeNum = new MovieEpisodeNum();
			// if (i * gridPageNum + gridPageNum >= getEpisodeList.size()) {
			// movieEpisodeNum.setId(getEpisodeList.get(i * gridPageNum).getId()
			// + "-" +
			// getEpisodeList.get(getEpisodeList.size() - 1).getId());
			// } else {
			// movieEpisodeNum.setId(getEpisodeList.get(i * gridPageNum).getId()
			// + "-" + getEpisodeList.get(i * gridPageNum + gridPageNum -
			// 1).getId());
			// }
			movieEpisodeNum.setId((i + 1) + "P");
			if (i == currNum) {
				movieEpisodeNum.setSelect(true);
			} else {
				movieEpisodeNum.setSelect(false);
			}
			infoEpisodeNumList.add(movieEpisodeNum);
		}

		getEpisodeData(currNum);

		if (type_id.equals("movie")) {
			infoEpisodeNumListView.setVisibility(View.INVISIBLE);
		}

		infoEpisodeNumAdapter.notifyDataSetChanged();
		infoEpisodeGridView.setNumColumns(gridViewNum);
		infoEpisodeAdapter.setGridNum(gridViewNum);
		infoEpisodeAdapter.notifyDataSetChanged();

		if (getCommentList != null) {
			infoCommentList.addAll(getCommentList);
			infoCommentAdapter.notifyDataSetChanged();
		}

		infoCollect.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (GlobalParams.hasLogin) {
					v.setSelected(!v.isSelected());
					if (v.isSelected()) {
						collectMovie();
					} else {
						discollectMovie();
					}
				} else {
					startLogin();
				}

			}
		});

		if (movieInfo.getLike() == 0) {
			infoCollect.setSelected(false);
		} else {
			infoCollect.setSelected(true);
		}

		infoDownload.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (!GlobalParams.isWifi) {
					if (!NetUtil.isWIFI(getApplicationContext())) {
						showIsWifiDownloadDialog(Video2DPlayerActivity.this);
						return;
					}
				}
				if (GlobalParams.hasLogin) {
					showDownload(movieInfo.getBimages(), movieInfo.getTitle());
				} else {
					startLogin();
				}

			}
		});

		infoShare.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("title", movieInfo.getTitle());
				jsonObject.put("thumb", movieInfo.getImages());
				jsonObject.put("content", movieInfo.getSummary());
				jsonObject.put("url", movieInfo.getShare());

				showShare(jsonObject);
			}
		});

	}

	// 登陆
	private void startLogin() {
		Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
		intent.putExtra("type", "y");
		startActivity(intent);
	}

	private void getEpisodeData(int num) {
		infoEpisodeList.clear();
		for (int i = num * gridViewNum * 2; i < (num + 1) * gridViewNum * 2; i++) {
			if (i < getEpisodeList.size()) {
				MovieEpisode movieEpisode = new MovieEpisode();
				movieEpisode.setId(getEpisodeList.get(i).getId());
				movieEpisode.setDes(getEpisodeList.get(i).getDes());
				movieEpisode.setPeriods(getEpisodeList.get(i).getPeriods());

				if (episodes_id.equals("0")) {
					if (i == 0) {
						movieEpisode.setSelect(true);
					} else {
						movieEpisode.setSelect(false);
					}
				} else {
					if (movieEpisode.getId().equals(episodes_id)) {
						movieEpisode.setSelect(true);
					} else {
						movieEpisode.setSelect(false);
					}
				}

				infoEpisodeList.add(movieEpisode);
			}

		}
	}

	private int seekProgress;

	// 部分按钮事件
	private void setView() {
		leftstateButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				startHideViewControllerTimer();
				if (isHdSelectShow) {
					isHdSelectShow = false;
					hideHdSelect();
				} else {
					isHdSelectShow = true;
					showHdSelect();
				}

				// restartMovie();
			}
		});

		leftplayButton.setOnClickListener(new OnClickListener() {

			@SuppressWarnings("deprecation")
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				startHideViewControllerTimer();
				if (mVV == null) {
					return;
				}
				if (isPlaying) {
					mVV.pause();
					leftplayButton.setImageDrawable(getResources().getDrawable(R.drawable.player_start));
					isPlaying = false;
				} else {
					if (mPlayerStatus == PLAYER_STATUS.PLAYER_IDLE) {
						mEventHandler.sendEmptyMessage(EVENT_PLAY);
					} else {
						mVV.resume();
						mHandler.sendEmptyMessage(1);
					}
					leftplayButton.setImageDrawable(getResources().getDrawable(R.drawable.player_stop));
					isPlaying = true;
				}
			}
		});

		portPlayButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				startHideViewControllerTimer();
				if (mVV == null) {
					return;
				}
				if (isPlaying) {
					mVV.pause();
					portPlayButton.setImageDrawable(getResources().getDrawable(R.drawable.port_play));
					isPlaying = false;
				} else {
					if (mPlayerStatus == PLAYER_STATUS.PLAYER_IDLE) {
						mEventHandler.sendEmptyMessage(EVENT_PLAY);
					} else {
						mVV.resume();
					}
					portPlayButton.setImageDrawable(getResources().getDrawable(R.drawable.port_stop));
					isPlaying = true;
				}
			}
		});

		lockButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				isLock = !lockButton.isSelected();
				lockButton.setSelected(isLock);
				if (isLock) {
					isViewcontrollerShow = false;
					hideViewController();
				} else {
					isViewcontrollerShow = true;
					showViewController();
					startHideViewControllerTimer();
				}
			}
		});

		leftBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				if (mVV == null) {
					return;
				}
				isSeeking = false;
				int progress = seekBar.getProgress();
				seekProgress = progress;
				if (mDu != 0 && seekProgress == mDu) {
					mCurr = mDu;
				}
				startHideViewControllerTimer();
				mVV.seekTo(progress);
				String timeString = toTime(progress);
				leftcurrTextView.setText(timeString);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				seekProgress = 0;
				isSeeking = true;
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				// TODO Auto-generated method stub
				if (isSeeking) {

				}

			}
		});

		leftBar.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					isSeeking = true;
				} else if (event.getAction() == KeyEvent.ACTION_UP) {
					isSeeking = false;
				}

				return false;
			}
		});

		portSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				if (mVV == null) {
					return;
				}
				isSeeking = false;
				int progress = seekBar.getProgress();
				seekProgress = progress;
				if (mDu != 0 && seekProgress == mDu) {
					mCurr = mDu;
				}
				startHideViewControllerTimer();
				mVV.seekTo(progress);
				String timeString = toTime(progress);
				portcurrTextView.setText(timeString);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				isSeeking = true;
				seekProgress = 0;
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				// TODO Auto-generated method stub
				if (isSeeking) {

				}

			}
		});

		portSeekBar.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					isSeeking = true;
				} else if (event.getAction() == KeyEvent.ACTION_UP) {
					isSeeking = false;
				}

				return false;
			}
		});
	}

	// 发表评论
	private void commentMovie(final String comment, final String from_id, final String platform) {
		Callback callback = new Callback() {

			@Override
			public void handleResult(String result) {
				// TODO Auto-generated method stub

			}
		};

		new DataAsyncTask(callback, false) {

			@Override
			protected String doInBackground(String... params) {
				// TODO Auto-generated method stub
				CommentApi commentApi = new CommentApi();
				commentApi.comment(comment, GlobalParams.user.getUid(), movie_id, type_id, from_id,
						GlobalParams.user.getToken(), platform);
				return null;
			}
		}.execute("");
	}

	// 收藏
	private void collectMovie() {
		Callback callback = new Callback() {

			@Override
			public void handleResult(String result) {
				// TODO Auto-generated method stub

			}
		};

		new DataAsyncTask(callback, false) {

			@Override
			protected String doInBackground(String... params) {
				// TODO Auto-generated method stub
				CollectApi collectApi = new CollectApi();
				collectApi.collect(GlobalParams.user.getUid(), movie_id, type_id, GlobalParams.user.getToken());
				return null;
			}
		}.execute("");
	}

	// 取消收藏
	private void discollectMovie() {
		Callback callback = new Callback() {

			@Override
			public void handleResult(String result) {
				// TODO Auto-generated method stub

			}
		};

		new DataAsyncTask(callback, false) {

			@Override
			protected String doInBackground(String... params) {
				// TODO Auto-generated method stub
				CollectApi collectApi = new CollectApi();
				collectApi.deleteCollect(GlobalParams.user.getUid(), movie_id, type_id, GlobalParams.user.getToken());
				return null;
			}
		}.execute("");
	}

	// 点赞
	public void diggMovie(final String comment_id) {
		Callback callback = new Callback() {

			@Override
			public void handleResult(String result) {
				// TODO Auto-generated method stub

			}
		};

		new DataAsyncTask(callback, false) {

			@Override
			protected String doInBackground(String... params) {
				// TODO Auto-generated method stub
				CommentApi commentApi = new CommentApi();
				commentApi.digg(comment_id);
				return null;
			}
		}.execute("");
	}

	private String source_id;

	private boolean isPause = false;

	@SuppressWarnings("deprecation")
	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		Log.v(TAG, "onPause");

		if (battery != null) {
			battery.unregisterBattery();
		}

		portPlayButton.setImageDrawable(getResources().getDrawable(R.drawable.port_play));
		leftplayButton.setImageDrawable(getResources().getDrawable(R.drawable.player_start));
		if (mPlayerStatus == PLAYER_STATUS.PLAYER_PREPARED) {
			if (mVV != null) {
				mLastPos = mVV.getCurrentPosition();
			}

		}
		if (mVV != null) {
			mVV.stopPlayback();
		}

		isPause = true;
		try {
			if (isEpisode) {
				historyMovie.setEpisode_id(episodes_id);
			}
			historyMovie.setPlay_Time(mLastPos);

			SerializationUtil.wirteHistorySerialization(getApplicationContext(), (Serializable) historyList);
		} catch (Exception e) {
			// TODO: handle exception
			Logger.e("SerializationUtil", e.getMessage());
		}

	}

	@SuppressWarnings("deprecation")
	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		Log.v(TAG, "onResume");
		if (null != mWakeLock && (!mWakeLock.isHeld())) {
			mWakeLock.acquire();
		}
		if (battery != null) {
			battery.registerBattery();
		}
		if (isPause) {
			isPause = false;
			isCatch = false;
			showLoading();
			if (mEventHandler == null) {
				if (mHandlerThread != null) {
					mHandlerThread.quit();
				}
				mHandlerThread = new HandlerThread("event handler thread", Process.THREAD_PRIORITY_BACKGROUND);
				mHandlerThread.start();
				mEventHandler = new EventHandler(mHandlerThread.getLooper());
			}
			mEventHandler.sendEmptyMessage(EVENT_PLAY);
			leftplayButton.setImageDrawable(getResources().getDrawable(R.drawable.player_stop));
			portPlayButton.setImageDrawable(getResources().getDrawable(R.drawable.port_stop));
			isPlaying = true;
		}
		// if (mVV != null && leftplayButton != null) {
		// if (isPlaying) {
		// mVV.pause();
		// leftplayButton.setImageDrawable(getResources().getDrawable(R.drawable.player_start));
		// isPlaying = false;
		// } else {
		// if (mPlayerStatus == PLAYER_STATUS.PLAYER_IDLE) {
		// mEventHandler.sendEmptyMessage(EVENT_PLAY);
		// } else {
		// mVV.resume();
		// }
		// leftplayButton.setImageDrawable(getResources().getDrawable(R.drawable.player_stop));
		// isPlaying = true;
		// }
		// }

	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.v(TAG, "onStop");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (newTimer != null) {
			newTimer.cancel();
		}

		if (speedTimer != null) {
			speedTimer.cancel();
		}

		if (mVV != null) {
			mVV.stopPlayback();
			mVV = null;
		}

		if (mOrientationListener != null) {
			mOrientationListener.disable();
		}
		currSourceNum = 0;
		/**
		 * 结束后台事件处理线程
		 */
		if (mHandlerThread != null) {
			mHandlerThread.quit();
		}
		// HeadSetUtil.getInstance().close(getApplicationContext());
		Log.v(TAG, "onDestroy");
		TCAgent.onPageEnd(getApplicationContext(), "Video2DPlayerActivity");
	}

	@Override
	public boolean onInfo(int what, int extra) {
		// TODO Auto-generated method stub
		switch (what) {
		/**
		 * 开始缓冲
		 */
		case BVideoView.MEDIA_INFO_BUFFERING_START:
			isCatch = true;
			mHandler.sendEmptyMessage(2);
			break;
		/**
		 * 结束缓冲
		 */
		case BVideoView.MEDIA_INFO_BUFFERING_END:
			mHandler.sendEmptyMessage(3);
			seekProgress = 0;
			break;
		default:
			mHandler.sendEmptyMessage(3);
			seekProgress = 0;
			break;
		}
		return false;
	}

	/**
	 * 当前缓冲的百分比， 可以配合onInfo中的开始缓冲和结束缓冲来显示百分比到界面
	 */
	@Override
	public void onPlayingBufferCache(int percent) {
		isCatch = true;
		// TODO Auto-generated method stub
		// mHandler.sendEmptyMessage(2);
		// if (percent >= 100) {
		// mHandler.sendEmptyMessage(3);
		// }
	}

	/**
	 * 播放出错
	 */
	@Override
	public boolean onError(int what, int extra) {
		// TODO Auto-generated method stub
		Log.v(TAG, "onError" + "what=" + what + ",extra=" + extra);
		synchronized (SYNC_Playing) {
			SYNC_Playing.notify();
		}
		mPlayerStatus = PLAYER_STATUS.PLAYER_IDLE;
		return true;
	}

	boolean isChange = false;

	/**
	 * 播放完成
	 */
	@Override
	public void onCompletion() {
		// TODO Auto-generated method stub
		Log.v(TAG, "onCompletion");

		if (mCurr == 0 && mDu == 0) {
			synchronized (SYNC_Playing) {
				SYNC_Playing.notify();
			}
			mPlayerStatus = PLAYER_STATUS.PLAYER_IDLE;
			return;
		}

		if (mCurr < mDu - 1) {
			Log.v(TAG, "onCompletionMiddle");
			synchronized (SYNC_Playing) {
				SYNC_Playing.notify();
			}
			mPlayerStatus = PLAYER_STATUS.PLAYER_IDLE;
			return;
		}
		if (!isChange) {
			mHandler.sendEmptyMessage(5);
		}

		synchronized (SYNC_Playing) {
			SYNC_Playing.notify();
		}
		mPlayerStatus = PLAYER_STATUS.PLAYER_IDLE;

	}

	private Timer newTimer;

	/**
	 * 播放准备就绪
	 */
	@Override
	public void onPrepared() {
		// TODO Auto-generated method stub
		Log.v(TAG, "onPrepared");
		setView();
		isChange = false;
		mPlayerStatus = PLAYER_STATUS.PLAYER_PREPARED;
		mHandler.sendEmptyMessage(1);
		mHandler.sendEmptyMessage(3);
		mHandler.sendEmptyMessage(7);

		// 刷新进度条
		if (newTimer == null) {
			newTimer = new Timer();
		} else {
			newTimer.cancel();
			newTimer = new Timer();
		}
		TimerTask task = new TimerTask() {

			@Override
			public void run() {
				// TODO Auto-generated method stub

				if (!isSeeking && isPlaying) {
					mHandler.sendEmptyMessage(0);
				}

			}
		};
		// mHandler.sendEmptyMessage(0);
		newTimer.schedule(task, 100, 200);
	}

	// 转换时间
	public String toTime(int time) {
		int minute = time / 60;
		int hour = minute / 60;
		int second = time % 60;
		minute %= 60;
		if (hour > 0) {
			return String.format("%02d:%02d:%02d", hour, minute, second);
		} else {
			return String.format("%02d:%02d", minute, second);
		}
	}

	// public boolean onKeyDown(int keyCode, KeyEvent event) {
	// if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
	// if (!isViewcontrollerShow) {
	// isViewcontrollerShow = true;
	// showViewController();
	// return false;
	// }
	// }
	// return super.onKeyDown(keyCode, event);
	// };
	private int mCurr, mDu;
	private Handler mHandler = new Handler() {
		@SuppressWarnings("deprecation")
		public void dispatchMessage(Message msg) {
			if (msg.what == 0) {
				// if (isChanging) {
				// isChanging = false;
				// mVV.pause();
				// mVV.resume();
				// }
				// int curr1 = mVV.getCurrentPosition();
				// int duration1 = mVV.getDuration();
				// if (curr1 != 0) {
				// mCurr = curr1;
				// }
				// if (duration1 != 0) {
				// mDu = duration1;
				// }
				if (mVV == null) {
					return;
				}
				if (!isSeeking) {
					int curr = mVV.getCurrentPosition();
					int duration = mVV.getDuration();
					if (curr != 0) {
						mCurr = curr;
					}
					if (duration != 0) {
						mDu = duration;
					}
					if (seekProgress != 0 && seekProgress > curr) {
						curr = seekProgress;
					} else if (seekProgress != 0 && seekProgress < curr) {
						curr = seekProgress;
					}
					String timeString = toTime(curr);
					String durationString = toTime(duration);
					// if (isViewcontrollerShow) {
					if (portSeekBar.getMax() > 0) {
						portSeekBar.setEnabled(true);
					}
					portSeekBar.setMax(duration);
					portSeekBar.setProgress(curr);
					portdurationTextView.setText(durationString);
					portcurrTextView.setText(timeString);
					if (leftBar.getMax() > 0) {
						leftBar.setEnabled(true);
					}
					leftBar.setMax(duration);
					leftBar.setProgress(curr);
					leftdurationTextView.setText(durationString);
					leftcurrTextView.setText(timeString);
					// }

				}

			} else if (msg.what == 1) {
				if (isViewcontrollerShow) {
					isViewcontrollerShow = false;
					hideViewController();
				}

			} else if (msg.what == 2) {
				showLoading();
			} else if (msg.what == 3) {
				hideLoading();
			} else if (msg.what == 4) {
				leftplayButton.setImageDrawable(getResources().getDrawable(R.drawable.player_stop));
				portPlayButton.setImageDrawable(getResources().getDrawable(R.drawable.port_stop));
			} else if (msg.what == 5) {
				if (!isEnd) {
					toNext();
				} else {
					String timeString = toTime(0);
					String durationString = toTime(0);

					portdurationTextView.setText(durationString);
					portcurrTextView.setText(timeString);

					leftdurationTextView.setText(durationString);
					leftcurrTextView.setText(timeString);

					leftplayButton.setImageDrawable(getResources().getDrawable(R.drawable.player_start));
					portPlayButton.setImageDrawable(getResources().getDrawable(R.drawable.port_play));
				}

			} else if (msg.what == 6) {
				if (!isViewcontrollerShow) {
					isViewcontrollerShow = true;
					showViewController();
				}
			}

		};
	};

	Timer viewControllerTimer;

	// 显示播放上下栏
	private void showViewController() {
		isViewcontrollerShow = true;
		if (isPortrait) {
			portBottomLayout.setVisibility(View.VISIBLE);
			portBottomLayout.clearAnimation();
			TranslateAnimation bottomtranslateAnimation = new TranslateAnimation(0, 0, 140, 0);
			bottomtranslateAnimation.setDuration(300);
			bottomtranslateAnimation.setFillAfter(true);

			portBottomLayout.startAnimation(bottomtranslateAnimation);

			startHideViewControllerTimer();
		} else {
			leftplayButton.setVisibility(View.VISIBLE);
			isLockVisible = true;
			lockButton.setVisibility(View.VISIBLE);

			bottomLayout.setVisibility(View.VISIBLE);
			topLayout.setVisibility(View.VISIBLE);

			bottomLayout.clearAnimation();
			TranslateAnimation bottomtranslateAnimation = new TranslateAnimation(0, 0, 140, 0);
			bottomtranslateAnimation.setDuration(300);
			bottomtranslateAnimation.setFillAfter(true);

			bottomLayout.startAnimation(bottomtranslateAnimation);

			topLayout.clearAnimation();
			TranslateAnimation toptranslateAnimation = new TranslateAnimation(0, 0, -140, 0);
			toptranslateAnimation.setDuration(300);
			toptranslateAnimation.setFillAfter(true);

			topLayout.startAnimation(toptranslateAnimation);
			startHideViewControllerTimer();
		}
	}

	private void startHideViewControllerTimer() {
		if (isViewcontrollerShow) {
			if (viewControllerTimer != null) {
				viewControllerTimer.cancel();
			}
			viewControllerTimer = new Timer();
			TimerTask task = new TimerTask() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					mHandler.sendEmptyMessage(1);
				}
			};

			viewControllerTimer.schedule(task, 8000);

		}
	}

	// 隐藏播放上下栏
	private void hideViewController() {
		isViewcontrollerShow = false;
		if (viewControllerTimer != null) {
			viewControllerTimer.cancel();
		}
		if (isPortrait) {
			// main.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
			portBottomLayout.clearAnimation();
			TranslateAnimation bottomtranslateAnimation = new TranslateAnimation(0, 0, 0, 140);
			bottomtranslateAnimation.setDuration(300);
			bottomtranslateAnimation.setFillAfter(true);
			bottomtranslateAnimation.setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationStart(Animation animation) {
					// TODO Auto-generated method stub

				}

				@Override
				public void onAnimationRepeat(Animation animation) {
					// TODO Auto-generated method stub

				}

				@Override
				public void onAnimationEnd(Animation animation) {
					// TODO Auto-generated method stub
					portBottomLayout.clearAnimation();
					portBottomLayout.setVisibility(View.GONE);
				}
			});

			portBottomLayout.startAnimation(bottomtranslateAnimation);
		} else {
			if (isHdSelectShow) {
				isHdSelectShow = false;
				hideHdSelect();
			}

			if (isEpisodeShow) {
				isEpisodeShow = false;
				hideEpisodeSelect();
			}

			if (isSourceShow) {
				isSourceShow = false;
				hideSourceSelect();
			}
			isLockVisible = false;
			lockButton.setVisibility(View.GONE);
			leftplayButton.setVisibility(View.GONE);
			// main.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
			bottomLayout.clearAnimation();
			TranslateAnimation bottomtranslateAnimation = new TranslateAnimation(0, 0, 0, 140);
			bottomtranslateAnimation.setDuration(300);
			bottomtranslateAnimation.setFillAfter(true);
			bottomtranslateAnimation.setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationStart(Animation animation) {
					// TODO Auto-generated method stub

				}

				@Override
				public void onAnimationRepeat(Animation animation) {
					// TODO Auto-generated method stub

				}

				@Override
				public void onAnimationEnd(Animation animation) {
					// TODO Auto-generated method stub
					bottomLayout.clearAnimation();
					bottomLayout.setVisibility(View.GONE);
					lockButton.setVisibility(View.GONE);
					leftplayButton.setVisibility(View.GONE);
				}
			});

			bottomLayout.startAnimation(bottomtranslateAnimation);

			topLayout.clearAnimation();
			TranslateAnimation toptranslateAnimation = new TranslateAnimation(0, 0, 0, -140);
			toptranslateAnimation.setDuration(300);
			toptranslateAnimation.setFillAfter(true);
			toptranslateAnimation.setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationStart(Animation animation) {
					// TODO Auto-generated method stub

				}

				@Override
				public void onAnimationRepeat(Animation animation) {
					// TODO Auto-generated method stub

				}

				@Override
				public void onAnimationEnd(Animation animation) {
					// TODO Auto-generated method stub
					topLayout.clearAnimation();
					topLayout.setVisibility(View.GONE);
				}
			});

			topLayout.startAnimation(toptranslateAnimation);
		}
	}

	// 显示清晰度
	private void showHdSelect() {

		hdLayout.setVisibility(View.VISIBLE);

		hdLayout.clearAnimation();
		TranslateAnimation bottomtranslateAnimation = new TranslateAnimation(550, 0, 0, 0);
		bottomtranslateAnimation.setDuration(300);
		bottomtranslateAnimation.setFillAfter(true);

		hdLayout.startAnimation(bottomtranslateAnimation);

	}

	// 隐藏清晰度
	private void hideHdSelect() {
		hdLayout.clearAnimation();
		TranslateAnimation bottomtranslateAnimation = new TranslateAnimation(0, 550, 0, 0);
		bottomtranslateAnimation.setDuration(300);
		bottomtranslateAnimation.setFillAfter(true);
		bottomtranslateAnimation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationRepeat(Animation animation) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				// TODO Auto-generated method stub
				hdLayout.clearAnimation();
				hdLayout.setVisibility(View.GONE);
			}
		});

		hdLayout.startAnimation(bottomtranslateAnimation);

	}

	private boolean isEpisodeShow = false;

	// 显示剧集
	private void showEpisodeSelect() {
		if (isEpisodeShow) {
			return;
		} else {
			isEpisodeShow = true;
		}
		episodeLayout.setVisibility(View.VISIBLE);
		episodeLayout.clearAnimation();
		AnimationManager.setShowAnimation(episodeLayout, 300, null);

	}

	// 隐藏剧集
	private void hideEpisodeSelect() {
		isEpisodeShow = false;
		episodeLayout.clearAnimation();
		AnimationManager.setHideAnimation(episodeLayout, 300, new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationRepeat(Animation animation) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				// TODO Auto-generated method stub
				episodeLayout.clearAnimation();
				episodeLayout.setVisibility(View.GONE);
			}
		});

	}

	private boolean isSourceShow = false;

	// 显示源
	private void showSourceSelect() {
		if (isSourceShow) {
			return;
		} else {
			isSourceShow = true;
		}
		isLockVisible = false;
		lockButton.setVisibility(View.GONE);
		sourceLayout.setVisibility(View.VISIBLE);

		sourceLayout.clearAnimation();
		TranslateAnimation bottomtranslateAnimation = new TranslateAnimation(-550, 0, 0, 0);
		bottomtranslateAnimation.setDuration(300);
		bottomtranslateAnimation.setFillAfter(true);

		sourceLayout.startAnimation(bottomtranslateAnimation);

	}

	// 隐藏源
	private void hideSourceSelect() {
		isLockVisible = true;
		lockButton.setVisibility(View.VISIBLE);
		isSourceShow = false;
		sourceLayout.clearAnimation();
		TranslateAnimation bottomtranslateAnimation = new TranslateAnimation(0, -550, 0, 0);
		bottomtranslateAnimation.setDuration(300);
		bottomtranslateAnimation.setFillAfter(true);
		bottomtranslateAnimation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationRepeat(Animation animation) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				// TODO Auto-generated method stub
				sourceLayout.clearAnimation();
				sourceLayout.setVisibility(View.GONE);
			}
		});

		sourceLayout.startAnimation(bottomtranslateAnimation);

	}

	private boolean isLoading = false;
	private boolean isCatch = false;

	// private long lastTotalRxBytes = 0;
	// private long lastTimeStamp = 0;
	//
	// private long getTotalRxBytes() {
	// return TrafficStats.getUidRxBytes(getApplicationInfo().uid) ==
	// TrafficStats.UNSUPPORTED ? 0
	// : (TrafficStats.getTotalRxBytes() / 1024);// 转为KB
	// }

	Timer speedTimer = new Timer();
	private int netSpeed = 0;

	// 显示网速
	private void showNetSpeed() {

		// long nowTotalRxBytes = getTotalRxBytes();
		// long nowTimeStamp = System.currentTimeMillis();
		// long speed = ((nowTotalRxBytes - lastTotalRxBytes) * 1000 /
		// (nowTimeStamp - lastTimeStamp));// 毫秒转换
		// lastTimeStamp = nowTimeStamp;
		// lastTotalRxBytes = nowTotalRxBytes;

		Message msg = speedHandler.obtainMessage();
		msg.what = 100;
		if (netSpeed < 1000) {
			msg.obj = String.valueOf(netSpeed) + " b/s";
		} else if (netSpeed < 1000 * 1000) {
			msg.obj = String.valueOf(((int) (netSpeed / 1000.0 * 100)) / 100.0) + " kb/s";
		} else if (netSpeed < 1000 * 1000 * 1000) {
			msg.obj = String.valueOf(((int) (netSpeed / 1000.0 / 1000 * 100)) / 100.0) + " mb/s";
		} else {
			msg.obj = String.valueOf(((int) (netSpeed / 1000.0 / 1000 / 1000 * 100)) / 100.0) + " gb/s";
		}

		speedHandler.sendMessage(msg);// 更新界面
	}

	private Handler speedHandler = new Handler() {
		public void dispatchMessage(Message msg) {
			if (loadingTextView != null) {
				loadingTextView.setText((CharSequence) msg.obj);
			}
		};
	};

	// 显示加载
	private void showLoading() {
		if (!isLoading) {
			isLoading = true;
			loadingLayout.setVisibility(View.VISIBLE);
			Animation operatingAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate);
			LinearInterpolator lin = new LinearInterpolator();
			operatingAnim.setInterpolator(lin);
			leftLoadingImageView.startAnimation(operatingAnim);
			if (isCatch) {
				loadingTextView.setText("0kb/s");
				if (speedTimer != null) {
					speedTimer.cancel();
				}
				speedTimer = new Timer();
				TimerTask speedTask = new TimerTask() {
					@Override
					public void run() {
						showNetSpeed();
					}
				};
				speedTimer.schedule(speedTask, 100, 1000);
			} else {
				loadingTextView.setText("拼命加载中...");
				if (speedTimer != null) {
					speedTimer.cancel();
				}
			}

		}
	}

	// 隐藏加载
	private void hideLoading() {
		if (isLoading) {
			isLoading = false;
			leftLoadingImageView.clearAnimation();
			loadingLayout.setVisibility(View.GONE);
			if (speedTimer != null) {
				speedTimer.cancel();
			}
		}
	}

	private int isMove = 0, moveType = 0;
	private float startX, startY;
	private float currColumn, currBright, tempBright = 1;
	private boolean isLockVisible = false;

	// 屏幕滑动
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		if (event.getAction() == MotionEvent.ACTION_DOWN) {

			if (isLock) {
				Logger.e("touch", "down lock=true");
				return true;
			} else {
				Logger.e("touch", "down lock=false");
			}
			WindowManager wm = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
			screenWidth = wm.getDefaultDisplay().getWidth();
			screenHeight = wm.getDefaultDisplay().getHeight();
			if (viewControllerTimer != null) {
				viewControllerTimer.cancel();
			}
			startX = event.getX();
			startY = event.getY();
			currColumn = SystemUtils.getCurrentActivityColumn(Video2DPlayerActivity.this);

			currBright = tempBright;
			isMove = 0;
			moveType = 0;
		} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
			if (isPortrait) {
				return true;
			}
			if (isLock) {
				return true;
			}

			float distanceX = event.getX() - startX;
			float distanceY = event.getY() - startY;

			if (moveType == 0) {
				if (Math.abs(distanceX) >= Math.abs(distanceY)) {
					if (Math.abs(distanceX) > 30) {
						moveType = 3;
					}
				} else if (startX < screenWidth * 3 / 9) {
					if (Math.abs(distanceY) > 30) {
						moveType = 2;
					}
				} else {
					if (Math.abs(distanceY) > 30 && startX > screenWidth * 6 / 9) {
						moveType = 1;
					}
				}

			}

			if (moveType == 1) {// startX > screenWidth * 6 / 8
				if (!isHdSelectShow) {
					if (distanceY > 30 || distanceY < -30) {
						showColumn();
						isMove = 3;

						float cb = currColumn + -distanceY * 28 / screenHeight;
						if (cb > maxColumn) {
							cb = maxColumn;
						}
						setColumn((int) cb);
						int percent = (int) (cb * 100 / maxColumn);

						if (percent > 100) {
							percent = 100;
						} else if (percent < 0) {
							percent = 0;
						}

						columnTextView.setText(percent + "%");
					}
				} else {
					isMove = 3;
				}

			} else if (moveType == 2) {// startX < screenWidth * 2 / 8
				if (distanceY > 30 || distanceY < -30) {
					showBright();
					isMove = 3;
					float cb = currBright - distanceY * 4 / screenHeight;
					if (cb > 1) {
						cb = 1;
					} else if (cb < 0) {
						cb = 0;
					}
					tempBright = cb;
					setBright(cb);
					brightTextView.setText((int) (cb * 100) + "%");
				}

			} else {
				if (moveType == 3) {// distanceX > 30 || distanceX < -30
					if (leftBar.getMax() > 0) {
						isMove = 1;
						scrollTextView.setVisibility(View.VISIBLE);
						if (leftBar.getProgress() + distanceX > leftBar.getMax()) {
							distanceX = leftBar.getMax() - leftBar.getProgress();
						} else if (leftBar.getProgress() + distanceX < 0) {
							distanceX = 0 - leftBar.getProgress();
						}

						if (distanceX > 0) {
							Drawable goRight = getResources().getDrawable(R.drawable.go_right);
							goRight.setBounds(0, 0, goRight.getMinimumWidth(), goRight.getMinimumHeight());
							scrollTextView.setCompoundDrawables(null, goRight, null, null);
						} else {
							Drawable goLeft = getResources().getDrawable(R.drawable.go_left);
							goLeft.setBounds(0, 0, goLeft.getMinimumWidth(), goLeft.getMinimumHeight());
							scrollTextView.setCompoundDrawables(null, goLeft, null, null);
						}
						int progress = (int) (leftBar.getProgress() + distanceX);
						String timeString = toTime(progress);
						scrollTextView.setText(timeString);
						if (isViewcontrollerShow) {
							leftplayButton.setVisibility(View.INVISIBLE);
						}
					} else {
						isMove = 2;
					}

				} else {
					isMove = 2;
				}
			}

		} else if (event.getAction() == MotionEvent.ACTION_UP) {
			if (isPortrait) {
				if (!isViewcontrollerShow) {
					isViewcontrollerShow = true;
					showViewController();
				} else {
					isViewcontrollerShow = false;
					hideViewController();

				}
				return true;
			}
			if (isLock) {
				if (isLockVisible) {
					isLockVisible = false;
					lockButton.setVisibility(View.GONE);
				} else {
					isLockVisible = true;
					lockButton.setVisibility(View.VISIBLE);
				}

				return true;
			}
			hideBright();
			hideColumn();
			scrollTextView.setVisibility(View.GONE);
			startHideViewControllerTimer();
			if (mVV == null) {
				return true;
			}
			if (isMove == 1) {
				isMove = 0;
				float distanceX = event.getX() - startX;
				seekProgress = 0;
				if (mDu != 0 && (leftBar.getProgress() + distanceX) >= mDu) {
					mCurr = mDu;
				}
				mVV.seekTo(leftBar.getProgress() + distanceX);
				scrollTextView.setVisibility(View.GONE);
				if (isViewcontrollerShow) {
					leftplayButton.setVisibility(View.VISIBLE);
				}

			} else if (isMove == 2 || isMove == 0) {
				if (!isViewcontrollerShow) {
					isViewcontrollerShow = true;
					showViewController();
				} else {
					isViewcontrollerShow = false;
					hideViewController();

				}
			}
		}
		return true;
	}

	// 亮度
	private void setBright(float num) {
		SystemUtils.setCurrentActivityBrightness(Video2DPlayerActivity.this, num);
	}

	private boolean isBrightShow = false;

	private void showBright() {
		if (!isBrightShow) {
			isBrightShow = true;
			brightTextView.setVisibility(View.VISIBLE);
			if (isViewcontrollerShow) {
				leftplayButton.setVisibility(View.INVISIBLE);
			}
		}
	}

	private void hideBright() {
		if (isBrightShow) {
			isBrightShow = false;
			brightTextView.setVisibility(View.GONE);
			if (isViewcontrollerShow) {
				leftplayButton.setVisibility(View.VISIBLE);
			}
		}
	}

	// 声音
	private void setColumn(int num) {

		SystemUtils.setCurrentActivityColumn(Video2DPlayerActivity.this, num, 0);
	}

	private boolean isColumnShow = false;

	private void showColumn() {
		if (!isColumnShow) {
			isColumnShow = true;
			columnTextView.setVisibility(View.VISIBLE);
			if (isViewcontrollerShow) {
				leftplayButton.setVisibility(View.INVISIBLE);
			}
		}
	}

	private void hideColumn() {
		if (isColumnShow) {
			isColumnShow = false;
			columnTextView.setVisibility(View.GONE);
			if (isViewcontrollerShow) {
				leftplayButton.setVisibility(View.VISIBLE);
			}
		}
	}

	// 显示评论
	public void openComment() {
		commentClickLayout.setVisibility(View.VISIBLE);
	}

	// 隐藏评论
	public void closeComment() {
		commentClickLayout.setVisibility(View.GONE);
	}

	// 显示用户进行评论的界面
	private void showComment() {
		isComment = true;
		commentJsonObject = new JSONObject();
		commentJsonObject.put("user_id", GlobalParams.user.getUid());
		commentEditText.setText("");
		commentLayout.setVisibility(View.VISIBLE);
		commentEditText.requestFocus();
		imm.showSoftInput(commentEditText, 0);
	}

	private JSONObject commentJsonObject;

	// 显示用户进行回复的界面
	public void showReply(JSONObject jsonObject) {
		isComment = false;
		commentJsonObject = jsonObject;
		closeComment();
		commentLayout.setVisibility(View.VISIBLE);
		commentEditText.setText("");
		commentEditText.requestFocus();
		imm.showSoftInput(commentEditText, 0);
		commentEditText.setHint("回复" + jsonObject.getString("from_nickname"));
	}

	// 换集数
	public void changeEpisode(String changeEpisode_id) {
		startHideViewControllerTimer();
		if (mVV == null) {
			return;
		}
		if (getUrlTask != null) {
			getUrlTask.cancel(true);
		}
		int position = currEpisodeNum;
		if (episodeList.size() > currEpisodeNum) {
			isChange = true;
			episodeList.get(currEpisodeNum).setSelect(false);
			for (int i = 0; i < episodeList.size(); i++) {
				if (episodeList.get(i).getId().equals(changeEpisode_id)) {
					position = i;
					break;
				}
			}

			episodeList.get(position).setSelect(true);
			episodes_id = episodeList.get(position).getId();
			if (type_id.equals("variety")) {
				varietyString = episodeList.get(position).getPeriods();
			}
			if (movieInfo != null) {
				titleString = movieInfo.getTitle();
			} else {
				titleString = "";
			}
			if (type_id.equals("variety")) {
				titleString += "  " + varietyString;
			}else{
				titleString += "第" + episodes_id + "集";
			}
			lefttitleTextView.setText(titleString);

			currEpisodeNum = position;
			if (currEpisodeNum == 0) {
				isEnd = true;
			} else {
				isEnd = false;
			}
			// if (currEpisodeNum == episodeList.size() - 1) {
			// isEnd = true;
			// }

			if (type_id.equals("movie")) {
				isEnd = true;
			}

			if (!isEnd) {
				nextButton.setVisibility(View.VISIBLE);
			} else {
				nextButton.setVisibility(View.GONE);
			}
			if (mPlayerStatus == PLAYER_STATUS.PLAYER_PREPARED) {
				mLastPos = 0;
				// historyList.get(0).setPlay_Time(mLastPos);
			}
			mVV.stopPlayback();
			playerEpisodeAdapter.notifyDataSetChanged();

			// infoEpisodeList.get(position).setSelect(true);

			getSource();
			source_id = sourceList.get(currSourceNum).getSource_url();
			getUrl(source_id);
		} else {
			Toast.makeText(getApplicationContext(), "获取视频失败", Toast.LENGTH_SHORT).show();
		}

	}

	private JSONObject shareObject;
	private ShareSDK shareSDK;

	// 显示分享
	public void showShare(JSONObject jsonObject) {
		shareObject = jsonObject;
		if (shareSDK == null) {
			shareSDK = new ShareSDK(this);
		}
		shareLayout.setVisibility(View.VISIBLE);
		shareAnimLayout.clearAnimation();
		TranslateAnimation bottomtranslateAnimation = new TranslateAnimation(0, 0, 1500, 0);
		bottomtranslateAnimation.setDuration(300);
		bottomtranslateAnimation.setFillAfter(true);

		shareAnimLayout.startAnimation(bottomtranslateAnimation);
	}

	// 隐藏分享
	private void hideShare() {
		if (shareLayout.getVisibility() == View.GONE) {
			return;
		}

		shareAnimLayout.clearAnimation();
		TranslateAnimation bottomtranslateAnimation = new TranslateAnimation(0, 0, 0, 1500);
		bottomtranslateAnimation.setDuration(300);
		bottomtranslateAnimation.setFillAfter(true);
		bottomtranslateAnimation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationRepeat(Animation animation) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				// TODO Auto-generated method stub
				shareAnimLayout.clearAnimation();
				shareLayout.setVisibility(View.GONE);
			}
		});

		shareAnimLayout.startAnimation(bottomtranslateAnimation);

	}

	// 显示下载
	public void showDownload(String image_url, String movie_title) {
		// isDownloadShow = true;
		this.image_url = image_url;
		this.movie_title = movie_title;
		downloadLayout.setVisibility(View.VISIBLE);

		downloadAnimLayout.clearAnimation();
		TranslateAnimation bottomtranslateAnimation = new TranslateAnimation(0, 0, 1500, 0);
		bottomtranslateAnimation.setDuration(300);
		bottomtranslateAnimation.setFillAfter(true);

		downloadAnimLayout.startAnimation(bottomtranslateAnimation);
	}

	// 隐藏下载
	private void hideDownload() {
		if (downloadLayout.getVisibility() == View.GONE) {
			// isDownloadShow = false;
			return;
		}
		// isDownloadShow = false;
		downloadAnimLayout.clearAnimation();
		TranslateAnimation bottomtranslateAnimation = new TranslateAnimation(0, 0, 0, 1500);
		bottomtranslateAnimation.setDuration(300);
		bottomtranslateAnimation.setFillAfter(true);
		bottomtranslateAnimation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationRepeat(Animation animation) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				// TODO Auto-generated method stub
				downloadAnimLayout.clearAnimation();
				downloadLayout.setVisibility(View.GONE);

			}
		});

		downloadAnimLayout.startAnimation(bottomtranslateAnimation);
		// downloadLayout.setVisibility(View.GONE);

	}

	private int downloadNum = 0;
	private M3U8Utils m3u8Utils;

	private void startDownload() {
		downloadNum = 0;
		m3u8Utils = new M3U8Utils(getApplicationContext(), movie_id, type_id, image_url, movie_title, isEpisode,
				sourceList.get(currSourceNum).getSource_name(), sourceList.get(currSourceNum).getSource_url(), hd);
		if (isEpisode) {
			PromptManager.showDownloadCancelProgressDialog(Video2DPlayerActivity.this);

			for (int i = 0; i < downloadList.size(); i++) {
				MovieEpisode movieEpisode = downloadList.get(i);
				if (movieEpisode.isSelect()) {
					List<MovieUrl> list = movieSourceList.get(Integer.parseInt(movieEpisode.getId()) - 1)
							.getSource_text();
					if (list != null) {
						for (int j = 0; j < list.size(); j++) {
							MovieUrl movieUrl = list.get(j);
							if (movieUrl.getSource_id().equals(sourceList.get(currSourceNum).getSource_id())) {
								downloadNum++;
								getDownloadUrl(movieUrl.getSource_url(), Integer.parseInt(movieEpisode.getId()));
							}
						}

						if (downloadNum == 0 && list.size() > 0) {
							downloadNum++;
							getDownloadUrl(list.get(0).getSource_url(), Integer.parseInt(movieEpisode.getId()));
						}
					}

				}
			}
		} else {
			downloadNum = 1;
			PromptManager.showDownloadCancelProgressDialog(Video2DPlayerActivity.this);
			getDownloadUrl(sourceList.get(currSourceNum).getSource_url(), 0);
		}
	}

	private void getDownloadUrl(final String url, final int episode_id) {
		Callback callback = new Callback() {

			@Override
			public void handleResult(String result) {
				// TODO Auto-generated method stub
				if (StringUtils.isEmpty(result)) {
					Toast.makeText(getApplicationContext(), "下载失败", Toast.LENGTH_SHORT).show();
					;
				} else {
					m3u8Utils.download(episode_id);
				}
				downloadNum--;
				if (downloadNum <= 0) {
					// saveDb();
					hideDownload();
					PromptManager.closeCancelProgressDialog();
				}

			}
		};

		new DataAsyncTask(callback, false) {

			@Override
			protected String doInBackground(String... params) {
				// TODO Auto-generated method stub
				MovieUrlApi movieUrlApi = new MovieUrlApi();
				RealUrl realUrl = movieUrlApi.getUrl(url, hd + "", true);
				String downloadUrl = "";
				boolean isM3U8 = false;
				String useragent = "";
				if (realUrl != null) {
					downloadUrl = realUrl.getUrl();
					if (realUrl.getFormat().equals("m3u8")) {
						isM3U8 = true;
					}
					useragent = realUrl.getUseragent();
				}
				if (!StringUtils.isEmpty(downloadUrl)) {
					// downloadUrl =
					// "http://vapi.saaser.cn/v1/youtu/sm3u8/id/MDAwMDAwMDAwMJKruJKUptWsfZi82ZmNbGSaZ7rZumhtrYFsjJjEZZvXldyf0ouon4-Fu7W2gaFkeI6Iq7O3fZdpiWl3a7GGaNCVqpfVgZTAr5OXk52XsHioiHeV3q2Kga6Cim9v_3.m3u8";
					m3u8Utils.initDownload(downloadUrl, episode_id, isM3U8, useragent);

				}
				return downloadUrl;
			}
		}.execute("");
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (shareSDK != null) {
			shareSDK.onResult(requestCode, resultCode, data);
		}
	}

	public void showWifiDialog(final Activity activity) {

		final Dialog dialog = new Dialog(activity, R.style.MyDialog);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setCancelable(false);
		// 设置它的ContentView
		View view = LayoutInflater.from(activity.getApplicationContext()).inflate(R.layout.isgoon_dialog, null);
		TextView tv = (TextView) view.findViewById(R.id.dialog_message);
		TextView dialog_enter = (TextView) view.findViewById(R.id.dialog_enter);
		TextView dialog_cancle = (TextView) view.findViewById(R.id.dialog_cancle);
		dialog_enter.setOnClickListener(new android.view.View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.cancel();
				getData();
			}
		});

		dialog_cancle.setOnClickListener(new android.view.View.OnClickListener() {
			@Override
			public void onClick(View v) {
				historyList.get(0).setPlay_Time(0);
				getData();
				dialog.cancel();
			}
		});
		tv.setText("存在历史播放记录，是否继续播放？");
		view.setMinimumWidth(600);
		dialog.setContentView(view);
		dialog.show();

	}

	public void showIsWifiDownloadDialog(final Activity activity) {

		final Dialog dialog = new Dialog(activity, R.style.MyDialog);
		dialog.setCanceledOnTouchOutside(true);
		dialog.setCancelable(true);
		// 设置它的ContentView
		View view = LayoutInflater.from(activity.getApplicationContext()).inflate(R.layout.isgoon_dialog, null);
		TextView tv = (TextView) view.findViewById(R.id.dialog_message);
		TextView dialog_enter = (TextView) view.findViewById(R.id.dialog_enter);
		dialog_enter.setText("开启");
		TextView dialog_cancle = (TextView) view.findViewById(R.id.dialog_cancle);
		dialog_enter.setOnClickListener(new android.view.View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.cancel();
			}
		});

		dialog_cancle.setOnClickListener(new android.view.View.OnClickListener() {
			@Override
			public void onClick(View v) {
				GlobalParams.isWifi = true;
				AppConfig.setWifi(GlobalParams.isWifi);
				Toast.makeText(getApplicationContext(), "已开启", 1).show();
				dialog.cancel();
			}
		});
		tv.setText("非wifi环境下载可能导致超额流量，确定开启");
		view.setMinimumWidth(600);
		dialog.setContentView(view);
		dialog.show();

	}

	private OrientationEventListener mOrientationListener; // 屏幕方向改变监听器
	private boolean mIsLand = true; // 是否是横屏
	private boolean mClick = false; // 是否点击
	private boolean mClickLand = true; // 点击进入横屏
	private boolean mClickPort = true; // 点击进入竖屏

	/**
	 * 开启监听器
	 */
	private final void startListener() {
		mOrientationListener = new OrientationEventListener(this) {
			@Override
			public void onOrientationChanged(int rotation) {
				// 设置竖屏
				if (((rotation >= 0) && (rotation <= 30)) || (rotation >= 330)) {
					if (mClick) {
						if (mIsLand && !mClickLand) {
							return;
						} else {
							mClickPort = true;
							mClick = false;
							mIsLand = false;
						}
					} else {
						if (isLock) {
							return;
						}
						if (mIsLand) {
							hideViewController();
							portTopLayout.setVisibility(View.VISIBLE);
							isPortrait = true;
							holder.setVisibility(View.VISIBLE);
							Video2DPlayerActivity.this
									.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
							mIsLand = false;
							mClick = false;
						}
					}
				}
				// 设置横屏
				else if (((rotation >= 230) && (rotation <= 310)) || ((rotation >= 50) && (rotation <= 160))) {
					if (mClick) {
						if (!mIsLand && !mClickPort) {
							return;
						} else {
							mClickLand = true;
							mClick = false;
							mIsLand = true;
						}
					} else {
						if (!mIsLand) {
							portTopLayout.setVisibility(View.GONE);
							isViewcontrollerShow = false;
							// hideViewController();
							isPortrait = false;
							portBottomLayout.clearAnimation();
							portBottomLayout.setVisibility(View.GONE);
							holder.setVisibility(View.GONE);
							closeComment();
							Video2DPlayerActivity.this
									.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
							mIsLand = true;
							mClick = false;
						}
					}
				}
			}
		};
		mOrientationListener.enable();
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
			if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
				// mClick = true;
				// if (mIsLand) {
				// hideViewController();
				// portTopLayout.setVisibility(View.VISIBLE);
				// isPortrait = true;
				// holder.setVisibility(View.VISIBLE);
				// Video2DPlayerActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				// mIsLand = false;
				// mClickPort = false;
				// } else {
				// if (fromLoading) {
				// Intent intent = new Intent(getApplicationContext(),
				// MainActivity.class);
				// startActivity(intent);
				// }
				// finish();
				// }
				if (fromLoading) {
					Intent intent = new Intent(getApplicationContext(), MainActivity.class);
					startActivity(intent);
				}
				finish();
			}
			return true;
		}
		return super.dispatchKeyEvent(event);
	}
}