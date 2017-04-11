package com.bccv.zhuiyingzhihanju.adapter;

import java.util.List;

import com.bccv.zhuiyingzhihanju.R;
import com.bccv.zhuiyingzhihanju.model.MovieEpisode;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class EpisodeAdapter extends BaseAdapter {
	Context context;
	List<MovieEpisode> list;

	public EpisodeAdapter(Context context, List<MovieEpisode> list) {
		// TODO Auto-generated constructor stub
		this.context = context;
		this.list = list;
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return list.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		ViewHolder viewHolder = null;
		if (convertView == null) {
			viewHolder = new ViewHolder();
			convertView = View.inflate(context, R.layout.listitem_episodes, null);
			viewHolder.textView = (TextView) convertView.findViewById(R.id.textView);
			convertView.setTag(viewHolder);
		}else{
			viewHolder = (ViewHolder) convertView.getTag();
		}
		
		viewHolder.textView.setText(list.get(position).getId() + "");
		viewHolder.textView.setSelected(list.get(position).isSelect());
		return convertView;
	}

	class ViewHolder{
		TextView textView;
	}
}
