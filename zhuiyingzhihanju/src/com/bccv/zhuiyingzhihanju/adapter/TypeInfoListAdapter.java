package com.bccv.zhuiyingzhihanju.adapter;

import java.util.List;

import com.bccv.zhuiyingzhihanju.R;
import com.bccv.zhuiyingzhihanju.activity.TypeInfoActivity;
import com.bccv.zhuiyingzhihanju.adapter.FilterInfoAdapter.ViewHolder;
import com.bccv.zhuiyingzhihanju.model.Movie;
import com.bccv.zhuiyingzhihanju.model.Special;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.utils.tools.GlobalParams;
import com.utils.tools.ImageUtils;
import com.utils.views.RoundedImageView;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class TypeInfoListAdapter extends BaseAdapter {
	private Context context;
	private List<Movie> list;
	private Activity activity;

	public TypeInfoListAdapter(Context context, Activity activity, List<Movie> list) {
		// TODO Auto-generated constructor stub
		this.context = context;
		this.list = list;
		this.activity = activity;
	}
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		if (list.size() > 0) {
			if (list.size() % 3 != 0) {
				return list.size() / 3 + 1;
			}else{
				return list.size() / 3;
			}
			
		} else {
			return 0;
		}

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
	public View getView(final int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		int rowNum = 3;
		ViewHolder viewHolder = null;
		if (convertView == null) {
			viewHolder = new ViewHolder();
			convertView = View.inflate(context, R.layout.griditem_typeinfo, null);
			viewHolder.first = convertView.findViewById(R.id.first_layout);
			viewHolder.second = convertView.findViewById(R.id.second_layout);
			viewHolder.third = convertView.findViewById(R.id.third_layout);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		
		

		for (int i = 0; i < rowNum; i++) {
			final int num = position * rowNum + i;
			View view = null;
			if (i == 0) {
				view = viewHolder.first;
			} else if (i == 1) {
				view = viewHolder.second;
			} else if (i == 2) {
				view = viewHolder.third;
			}
			if (list.size() > num) {
				view.setVisibility(View.VISIBLE);
				view.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						((TypeInfoActivity)activity).goInfo(num);
					}
				});
			}else{
				view.setVisibility(View.GONE);
				continue;
			}
			Movie item = list.get(num);
			RoundedImageView roundedImageView = (RoundedImageView) view.findViewById(R.id.roundedImageView);
			TextView titleTextView = (TextView) view.findViewById(R.id.title_textView);
			TextView introTextView = (TextView) view.findViewById(R.id.intro_textView);
			TextView scoreTextView = (TextView) view.findViewById(R.id.score_textView);
			scoreTextView.setText(item.getRating());
			if (item.getType_id().equals("tv")) {
				TextView updateTextView = (TextView) view.findViewById(R.id.update_textView);
				updateTextView.setText(item.getDef());
			}
			
			titleTextView.setText(item.getTitle());
			String images = (String) roundedImageView.getTag();
			if (images != null && images.equals(item.getImages())) {
				roundedImageView.setTag(item.getImages());
				ImageLoader imageLoader = ImageLoader.getInstance();
				imageLoader.displayImage(item.getImages(), roundedImageView, GlobalParams.movieOptions);
			} else {
				roundedImageView.setTag(item.getImages());
				ImageLoader imageLoader = ImageLoader.getInstance();
				imageLoader.displayImage(item.getImages(), roundedImageView, GlobalParams.movieOptions);
			}
			

			introTextView.setText(item.getShort_summary());
			
		}

		return convertView;
	}

	class ViewHolder {
		View first;
		View second;
		View third;
	}

}
