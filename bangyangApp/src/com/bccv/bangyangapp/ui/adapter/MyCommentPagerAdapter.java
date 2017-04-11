package com.bccv.bangyangapp.ui.adapter;

import java.util.ArrayList;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import com.bccv.bangyangapp.ui.view.CommentChildPage;

public class MyCommentPagerAdapter extends PagerAdapter {

	private ArrayList<CommentChildPage> pageList;
	
	public MyCommentPagerAdapter(ArrayList<CommentChildPage> pageList){
		this.pageList = pageList;
	}
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return pageList==null?0:pageList.size();
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		// TODO Auto-generated method stub
		return view == object;
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		// TODO Auto-generated method stub
		View view = pageList.get(position).getView();
		container.addView(view);
		pageList.get(position).onCreate();
		return view;
	}
	
    @Override  
    public void destroyItem(ViewGroup container, int position,  
            Object object)  
    {  
        container.removeView(pageList.get(position).getView());  
    }  
    
}
