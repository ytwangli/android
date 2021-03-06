package com.bccv.zhuiying.activity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.bccv.zhuiying.R;
import com.bccv.zhuiying.adapter.HistoryListAdapter;
import com.bccv.zhuiying.model.Movie;
import com.tendcloud.tenddata.TCAgent;
import com.utils.tools.SerializationUtil;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

public class HistoryActivity extends Activity implements OnClickListener {

	private List<Movie> list;

	ListView pList;

	HistoryListAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		tcStart();
		setContentView(R.layout.activity_history);

		ImageButton backBtn = (ImageButton) findViewById(R.id.titel_back);
		backBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				finish();
			}
		});
		ImageButton clearBtn = (ImageButton) findViewById(R.id.titel_search);
		clearBtn.setVisibility(View.INVISIBLE);
		TextView cleartext = (TextView) findViewById(R.id.titel_clear);
		cleartext.setVisibility(View.VISIBLE);

		cleartext.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				list.clear();

				SerializationUtil.wirteHistorySerialization(HistoryActivity.this, (Serializable) list);
				;

				adapter.notifyDataSetChanged();

			}
		});

		TextView text = (TextView) findViewById(R.id.titleName_textView);
		text.setVisibility(View.VISIBLE);
		text.setText("历史记录");

		pList = (ListView) findViewById(R.id.pList);
		pList.setSelector(new ColorDrawable(android.R.color.transparent));

		pList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub

				Movie movie = list.get(position);
				list.remove(movie);
				list.add(0, movie);
				SerializationUtil.wirteHistorySerialization(HistoryActivity.this, (Serializable) list);

				Intent aIntent = new Intent(HistoryActivity.this, Video2DPlayerActivity.class);

				aIntent.putExtra("movie", movie);

				startActivity(aIntent);
			}
		});
		pList.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub

				DeleteDialog(position);

				return true;
			}
		});

	}

	/**
	 * 长按删除
	 * 
	 * @param position
	 */
	private void DeleteDialog(final int position) {
		AlertDialog.Builder builder = new Builder(HistoryActivity.this);

		builder.setMessage("确定删除文件？");
		builder.setTitle("提示");

		builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {

				list.remove(position);
				SerializationUtil.wirteHistorySerialization(HistoryActivity.this, (Serializable) list);
				;

				adapter.notifyDataSetChanged();

			}
		});

		builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {

			}
		});
		builder.create().show();
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();

		list = SerializationUtil.readHistoryCache(this.getApplicationContext());
		if (list == null) {
			list = new ArrayList<Movie>();
		}

		adapter = new HistoryListAdapter(this, list);

		pList.setAdapter(adapter);

	}
	
	private void tcStart(){
		TCAgent.onPageStart(getApplicationContext(), "HistoryActivity");
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		TCAgent.onPageEnd(getApplicationContext(), "HistoryActivity");
	}
}
