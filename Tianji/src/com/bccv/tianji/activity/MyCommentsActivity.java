package com.bccv.tianji.activity;

import java.util.ArrayList;
import java.util.List;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bccv.tianji.R;
import com.bccv.tianji.adapter.MyCommentsAdapter;
import com.bccv.tianji.api.CommentApi;
import com.bccv.tianji.model.Comment;
import com.utils.net.NetUtil;
import com.utils.pulltorefresh.FooterLoadingLayout;
import com.utils.pulltorefresh.PullToRefreshBase;
import com.utils.pulltorefresh.PullToRefreshBase.OnRefreshListener;
import com.utils.pulltorefresh.PullToRefreshListView;
import com.utils.tools.BaseActivity;
import com.utils.tools.Callback;
import com.utils.tools.GlobalParams;
import com.utils.tools.PromptManager;

public class MyCommentsActivity extends BaseActivity {
	private PullToRefreshListView pullToRefreshListView;
	private MyCommentsAdapter adapter;
	private List<Comment> list, getList;
	
	private int page = 1, count = 10;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mycomments);
		setBack();
		setView();
	}

	private ImageView backTextView;

	private void setBack() {
		backTextView = (ImageView) findViewById(R.id.back_textView);
		backTextView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				finish();		
			}
		});
	}
	
	private void setView(){
		list = new ArrayList<Comment>();
		adapter = new MyCommentsAdapter(getApplicationContext(), this, list);
		
		pullToRefreshListView = (PullToRefreshListView) findViewById(R.id.pullToRefreshListView);
		pullToRefreshListView.getRefreshableView().setAdapter(adapter);
		pullToRefreshListView.setPullLoadEnabled(true);
		pullToRefreshListView.setPullRefreshEnabled(true);
		pullToRefreshListView.getRefreshableView().setSelector(
				new ColorDrawable(android.R.color.transparent));
		pullToRefreshListView.getRefreshableView().setDividerHeight(0);
		pullToRefreshListView.doPullRefreshing(true, 100);
		pullToRefreshListView.setOnRefreshListener(new OnRefreshListener<ListView>() {

			@Override
			public void onPullDownToRefresh(
					PullToRefreshBase<ListView> refreshView) {
				// TODO Auto-generated method stub
				if (!NetUtil.isNetworkAvailable(GlobalParams.context)) {
					// 提示网络不给力,直接完成刷新
					PromptManager.showToast(GlobalParams.context, "网络不给力");
					
					pullToRefreshListView.onPullDownRefreshComplete();
				} else {
					getData(true);
					

				}
			}

			@Override
			public void onPullUpToRefresh(
					PullToRefreshBase<ListView> refreshView) {
				// TODO Auto-generated method stub
				if (NetUtil.isNetworkAvailable(GlobalParams.context)) {
					((FooterLoadingLayout) pullToRefreshListView.getFooterLoadingLayout())
							.getmHintView().setText("数据加载中...");
					getData(false);
				} else {
					PromptManager.showToast(GlobalParams.context, "网络不给力");
					pullToRefreshListView.onPullUpRefreshComplete();
				}
			}

		});
	}
	
	private void getData(final boolean isRefresh){
		if (isRefresh) {
			page = 1;
		}
		Callback callback = new Callback() {
			
			@Override
			public void handleResult(String result) {
				// TODO Auto-generated method stub
				if (getList != null) {
					if (isRefresh) {
						list.clear();
					}
					list.addAll(getList);
					adapter.notifyDataSetChanged();
				}
				if (isRefresh) {
					pullToRefreshListView.onPullDownRefreshComplete();
				}else {
					pullToRefreshListView.onPullUpRefreshComplete();
				}
			}
		};
		
		new DataAsyncTask(callback, false) {
			
			@Override
			protected String doInBackground(String... params) {
				// TODO Auto-generated method stub
				CommentApi commentApi = new CommentApi();
				if (GlobalParams.user!= null) {
					getList = commentApi.getMyCommentsList(GlobalParams.user.getUser_id(), page + "", count + "");
					
				}else{
					getList = commentApi.getMyCommentsList("", page + "", count + "");	
				}
				page++;
				return null;
			}
		}.execute("");
	}
}
