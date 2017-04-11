package com.bccv.zhuiying.activity;

import java.util.Timer;
import java.util.TimerTask;
import com.baidu.cyberplayer.core.BVideoView;
import com.baidu.cyberplayer.core.BVideoView.OnCompletionListener;
import com.baidu.cyberplayer.core.BVideoView.OnErrorListener;
import com.baidu.cyberplayer.core.BVideoView.OnInfoListener;
import com.baidu.cyberplayer.core.BVideoView.OnNetworkSpeedListener;
import com.baidu.cyberplayer.core.BVideoView.OnPlayingBufferCacheListener;
import com.baidu.cyberplayer.core.BVideoView.OnPreparedListener;
import com.bccv.zhuiying.R;
import com.tendcloud.tenddata.TCAgent;
import com.utils.headset.HeadSetUtil;
import com.utils.tools.BaseActivity;
import com.utils.tools.GlobalParams;
import com.utils.tools.Logger;
import com.utils.tools.StringUtils;
import com.utils.tools.SystemUtils;
import android.annotation.SuppressLint;
import android.content.Context;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Process;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint({ "NewApi", "HandlerLeak", "Wakelock", "DefaultLocale", "ClickableViewAccessibility", "InflateParams" })
public class VideoMagnetPlayerActivity extends BaseActivity implements OnPreparedListener, OnCompletionListener,
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

	private BVideoView mVV = null;
	private RelativeLayout mViewHolder = null;

	private boolean mIsHwDecode = false;
	private int maxColumn;

	private EventHandler mEventHandler;
	private HandlerThread mHandlerThread;

	private final Object SYNC_Playing = new Object();

	private final int EVENT_PLAY = 0;

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
				// "http://sh.ctfs.ftn.caomijuan.com/ftn_handler/d5916f1a31a0541efeebb740c54c7232a67cac1a8d1e072154afbcc6875bc38d2ed9469b40458dd9c0df944f18e327fa55b9c70836057c02a1e2513b429b735c/player.mp4";
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
	View main;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		TCAgent.onPageStart(getApplicationContext(), "VideoMagnetPlayerActivity");
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		main = View.inflate(getApplicationContext(), R.layout.activity_videomagnetplayer, null);
		main.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

		setContentView(main);
		// Video2DPlayerActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, POWER_LOCK);

		WindowManager wm = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);

		screenWidth = wm.getDefaultDisplay().getWidth();
		screenHeight = wm.getDefaultDisplay().getHeight();
		maxColumn = SystemUtils.getMaxActivityColumn(this);
		mIsHwDecode = getIntent().getBooleanExtra("isHW", false);
		titleString = getIntent().getStringExtra("title");
		if (StringUtils.isEmpty(titleString)) {
			titleString = "";
		}
		TextView titleTextView = (TextView) findViewById(R.id.title_textView);
		titleTextView.setText(titleString);
		initUI();
		mVideoSource = getIntent().getStringExtra("url");
		if (StringUtils.isEmpty(mVideoSource)) {
			Toast.makeText(getApplicationContext(), "地址为空", Toast.LENGTH_SHORT).show();
		} else {
			playMovie();
		}

	}

	private void playMovie() {
		/**
		 * 开启后台事件处理线程
		 */
		mHandlerThread = new HandlerThread("event handler thread", Process.THREAD_PRIORITY_BACKGROUND);
		mHandlerThread.start();
		mEventHandler = new EventHandler(mHandlerThread.getLooper());

		mEventHandler.sendEmptyMessage(EVENT_PLAY);
	}

	/**
	 * 初始化界面
	 */
	private void initUI() {
		mViewHolder = (RelativeLayout) findViewById(R.id.videoview_holder);

		/**
		 * 设置ak及sk的前16位
		 */
		BVideoView.setAKSK(AK, SK);

		/**
		 * 创建BVideoView和BMediaController
		 */
		mVV = new BVideoView(this);
		mViewHolder.addView(mVV);

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

		// mViewHolder.setOnClickListener(new OnClickListener() {
		//
		// @Override
		// public void onClick(View v) {
		//
		// if (!isViewcontrollerShow) {
		// isViewcontrollerShow = true;
		// showViewController();
		// } else {
		// isViewcontrollerShow = false;
		// hideViewController();
		// if (isHdSelectShow) {
		// isHdSelectShow = false;
		// hideHdSelect();
		// }
		// }
		//
		// }
		// });
		mViewHolder.setOnTouchListener(this);

		initViewController();

	}

	private LinearLayout bottomLayout, topLayout, loadingLayout;

	private ImageButton leftplayButton, leftbackButton;
	private SeekBar leftBar;
	private TextView leftcurrTextView, leftdurationTextView, lefttitleTextView, brightTextView, columnTextView,
			scrollTextView, loadingTextView;
	private ImageView leftLoadingImageView;

	private boolean isPlaying = true;
	private boolean isViewcontrollerShow = false, isHdSelectShow = false;
	private boolean isSeeking = false;

	private void initViewController() {
		loadingLayout = (LinearLayout) findViewById(R.id.loading_layout);
		loadingTextView = (TextView) findViewById(R.id.loading_textView);

		scrollTextView = (TextView) findViewById(R.id.scroll_textView);
		brightTextView = (TextView) findViewById(R.id.bright_textView);
		columnTextView = (TextView) findViewById(R.id.column_textView);

		topLayout = (LinearLayout) findViewById(R.id.top_layout);
		bottomLayout = (LinearLayout) findViewById(R.id.bottom_layout);
		leftplayButton = (ImageButton) findViewById(R.id.play_button);
		leftbackButton = (ImageButton) findViewById(R.id.back_button);
		leftBar = (SeekBar) findViewById(R.id.seekBar);
		leftcurrTextView = (TextView) findViewById(R.id.curr_textView);
		leftdurationTextView = (TextView) findViewById(R.id.duration_textView);
		lefttitleTextView = (TextView) findViewById(R.id.title_textView);

		leftLoadingImageView = (ImageView) findViewById(R.id.loading_imageView);

		topLayout.setVisibility(View.GONE);
		bottomLayout.setVisibility(View.GONE);

		leftBar.setEnabled(false);

		leftbackButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				finish();
			}
		});

		lefttitleTextView.setText(titleString);

	}

	private void setView() {

		leftplayButton.setOnClickListener(new OnClickListener() {

			@SuppressWarnings("deprecation")
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (isPlaying) {
					mVV.pause();
					leftplayButton.setImageDrawable(getResources().getDrawable(R.drawable.player_start));
					isPlaying = false;
				} else {
					if (mPlayerStatus == PLAYER_STATUS.PLAYER_IDLE) {
						mEventHandler.sendEmptyMessage(EVENT_PLAY);
					} else {
						mVV.resume();
					}
					leftplayButton.setImageDrawable(getResources().getDrawable(R.drawable.player_stop));
					isPlaying = true;
				}
			}
		});

		leftBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				isSeeking = false;
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				isSeeking = true;
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				// TODO Auto-generated method stub
				if (isSeeking) {
					mVV.seekTo(progress);
					String timeString = toTime(progress);
					leftcurrTextView.setText(timeString);
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
	}

	private boolean isPause = false;

	@SuppressWarnings("deprecation")
	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		Log.v(TAG, "onPause");
		leftplayButton.setImageDrawable(getResources().getDrawable(R.drawable.player_start));
		if (mPlayerStatus == PLAYER_STATUS.PLAYER_PREPARED) {
			mLastPos = mVV.getCurrentPosition();
		}
		mVV.stopPlayback();
		isPause = true;

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
		if (isPause) {
			isPause = false;
			isCatch = false;
			showLoading();
			mEventHandler.sendEmptyMessage(EVENT_PLAY);
			leftplayButton.setImageDrawable(getResources().getDrawable(R.drawable.player_stop));
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
		if (speedTimer != null) {
			speedTimer.cancel();
		}
		/**
		 * 结束后台事件处理线程
		 */
		if (mHandlerThread != null) {
			mHandlerThread.quit();
		}
		HeadSetUtil.getInstance().close(getApplicationContext());
		Log.v(TAG, "onDestroy");
		TCAgent.onPageEnd(getApplicationContext(), "VideoMagnetPlayerActivity");
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
			break;
		default:
			mHandler.sendEmptyMessage(3);
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

	/**
	 * 播放完成
	 */
	@Override
	public void onCompletion() {
		// TODO Auto-generated method stub
		Log.v(TAG, "onCompletion");
		mHandler.sendEmptyMessage(5);
		synchronized (SYNC_Playing) {
			SYNC_Playing.notify();
		}
		mPlayerStatus = PLAYER_STATUS.PLAYER_IDLE;

	}

	/**
	 * 播放准备就绪
	 */
	@Override
	public void onPrepared() {
		// TODO Auto-generated method stub
		Log.v(TAG, "onPrepared");
		setView();
		mPlayerStatus = PLAYER_STATUS.PLAYER_PREPARED;
		mHandler.sendEmptyMessage(1);
		mHandler.sendEmptyMessage(3);
		mHandler.sendEmptyMessage(7);
		Timer timer = new Timer();
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
		timer.schedule(task, 100, 200);
	}

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

	private Timer timer = null;

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (timer != null) {
			timer.cancel();
		}
		timer = new Timer();
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				mHandler.sendEmptyMessage(1);
			}
		};
		timer.schedule(task, 5000);
		if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
			if (!isViewcontrollerShow) {
				isViewcontrollerShow = true;
				showViewController();
				return false;
			}
		}
		return super.onKeyDown(keyCode, event);
	};

	private Handler mHandler = new Handler() {
		@SuppressWarnings("deprecation")
		public void dispatchMessage(Message msg) {
			if (msg.what == 0) {
				// if (isChanging) {
				// isChanging = false;
				// mVV.pause();
				// mVV.resume();
				// }

				if (!isSeeking) {
					int curr = mVV.getCurrentPosition();
					int duration = mVV.getDuration();

					String timeString = toTime(curr);
					String durationString = toTime(duration);
					if (isViewcontrollerShow) {
						if (leftBar.getMax() > 0) {
							leftBar.setEnabled(true);
						}
						leftBar.setMax(duration);
						leftBar.setProgress(curr);
						leftdurationTextView.setText(durationString);
						leftcurrTextView.setText(timeString);
					}

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
			} else if (msg.what == 5) {
				leftplayButton.setImageDrawable(getResources().getDrawable(R.drawable.player_start));
			} else if (msg.what == 6) {
				if (!isViewcontrollerShow) {
					isViewcontrollerShow = true;
					showViewController();
				}
			}

		};
	};

	private Timer viewControllerTimer;

	private void showViewController() {

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

		viewControllerTimer.schedule(task, 5000);
	}

	private void hideViewController() {
		main.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
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

	private boolean isLoading = false;
	private boolean isCatch = false;

//	private long lastTotalRxBytes = 0;
//	private long lastTimeStamp = 0;
//
//	private long getTotalRxBytes() {
//		return TrafficStats.getUidRxBytes(getApplicationInfo().uid) == TrafficStats.UNSUPPORTED ? 0
//				: (TrafficStats.getTotalRxBytes() / 1024);// 转为KB
//	}

	Timer speedTimer = new Timer();
	private int netSpeed = 0;

	private void showNetSpeed() {

//		long nowTotalRxBytes = getTotalRxBytes();
//		long nowTimeStamp = System.currentTimeMillis();
//		long speed = ((nowTotalRxBytes - lastTotalRxBytes) * 1000 / (nowTimeStamp - lastTimeStamp));// 毫秒转换
//		lastTimeStamp = nowTimeStamp;
//		lastTotalRxBytes = nowTotalRxBytes;

		Message msg = speedHandler.obtainMessage();
		msg.what = 100;
		msg.obj = netSpeed + " kb/s";

		speedHandler.sendMessage(msg);// 更新界面
	}

	private Handler speedHandler = new Handler() {
		public void dispatchMessage(Message msg) {
			loadingTextView.setText((CharSequence) msg.obj);
		};
	};

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

	private void hideLoading() {
		if (isLoading) {
			isLoading = false;
			leftLoadingImageView.clearAnimation();
			loadingLayout.setVisibility(View.GONE);

		}
	}

	private int isMove = 0;
	private float startX, startY;
	private float currColumn, currBright, tempBright = 1;

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			startX = event.getX();
			startY = event.getY();
			currColumn = SystemUtils.getCurrentActivityColumn(VideoMagnetPlayerActivity.this);

			currBright = tempBright;
			isMove = 0;
		} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
			float distanceX = event.getX() - startX;
			float distanceY = event.getY() - startY;

			if (startX > screenWidth * 2 / 3 && Math.abs(distanceX) < Math.abs(distanceY)) {
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

			} else if (startX < screenWidth / 3 && Math.abs(distanceX) < Math.abs(distanceY)) {
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
				if (distanceX > 30 || distanceX < -30 && Math.abs(distanceX) > Math.abs(distanceY)) {
					if (leftBar.getMax() > 0) {
						isMove = 1;
						scrollTextView.setVisibility(View.VISIBLE);
						if (leftBar.getProgress() + distanceX > leftBar.getMax()) {
							distanceX = leftBar.getMax() - leftBar.getProgress();
						} else if (leftBar.getProgress() + distanceX < 0) {
							distanceX = 0 - leftBar.getProgress();
						}
						int progress = (int) (leftBar.getProgress() + distanceX);
						String timeString = toTime(progress);
						scrollTextView.setText(timeString);
					} else {
						isMove = 2;
					}

				} else {
					isMove = 2;
				}
			}

		} else if (event.getAction() == MotionEvent.ACTION_UP) {
			hideBright();
			hideColumn();
			scrollTextView.setVisibility(View.GONE);
			if (isMove == 1) {
				isMove = 0;
				float distanceX = event.getX() - startX;
				mVV.seekTo(leftBar.getProgress() + distanceX);
				scrollTextView.setVisibility(View.GONE);

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

	private void setBright(float num) {
		SystemUtils.setCurrentActivityBrightness(VideoMagnetPlayerActivity.this, num);
	}

	private boolean isBrightShow = false;

	private void showBright() {
		if (!isBrightShow) {
			isBrightShow = true;
			brightTextView.setVisibility(View.VISIBLE);
		}
	}

	private void hideBright() {
		if (isBrightShow) {
			isBrightShow = false;
			brightTextView.setVisibility(View.GONE);
		}
	}

	private void setColumn(int num) {

		SystemUtils.setCurrentActivityColumn(VideoMagnetPlayerActivity.this, num, 0);
	}

	private boolean isColumnShow = false;

	private void showColumn() {
		if (!isColumnShow) {
			isColumnShow = true;
			columnTextView.setVisibility(View.VISIBLE);
		}
	}

	private void hideColumn() {
		if (isColumnShow) {
			isColumnShow = false;
			columnTextView.setVisibility(View.GONE);
		}
	}

}
