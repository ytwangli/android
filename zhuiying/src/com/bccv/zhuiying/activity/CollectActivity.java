package com.bccv.zhuiying.activity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.bccv.zhuiying.R;
import com.bccv.zhuiying.adapter.MovieListAdapter;
import com.bccv.zhuiying.model.Movie;
import com.tendcloud.tenddata.TCAgent;
import com.utils.tools.BaseActivity;
import com.utils.tools.Callback;
import com.utils.tools.SerializationUtil;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;

public class CollectActivity extends BaseActivity {
	private GridView gridView;
	private MovieListAdapter adapter;
	private List<Movie> data;
	private List<Movie> list;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		TCAgent.onPageStart(getApplicationContext(), "CollectActivity");
		setContentView(R.layout.activity_morelist);

		gridView = (GridView) findViewById(R.id.more_grid);

		data = new ArrayList<Movie>();

		adapter = new MovieListAdapter(this, data);
		gridView.setAdapter(adapter);

		ImageButton backBtn = (ImageButton) findViewById(R.id.titel_back);
		backBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				finish();
			}
		});

		gridView.setSelector(new ColorDrawable(android.R.color.transparent));
		gridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub

				Intent aIntent = new Intent(CollectActivity.this, MovieInfoActivity.class);

				aIntent.putExtra("movie", data.get(position));

				startActivity(aIntent);

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

				SerializationUtil.wirteCollectSerialization(CollectActivity.this, (Serializable) list);
				;

				getData();

			}
		});

	}

	public void getData() {

		Callback callback = new Callback() {

			@Override
			public void handleResult(String result) {
				// TODO Auto-generated method stub
				data.clear();
				if (list != null) {
					data.addAll(list);

					adapter.notifyDataSetChanged();
				}

			}
		};

		new DataAsyncTask(callback, true) {

			@Override
			protected String doInBackground(String... params) {
				list = SerializationUtil.readCollectCache(getApplicationContext());

				return "true";
			}
		}.execute("");
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();

		getData();

	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		TCAgent.onPageEnd(getApplicationContext(), "CollectActivity");
	}

}