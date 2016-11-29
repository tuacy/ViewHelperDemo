package com.pilot.viewhelperdemo.viewdraghelp;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.pilot.viewhelperdemo.R;

import java.util.List;

public class ContentAdapter extends BaseAdapter {

	private List<String> mData;
	private Context      mContext;

	public ContentAdapter(Context context, List<String> data) {
		mContext = context;
		mData = data;
	}

	@Override
	public int getCount() {
		return mData == null ? 0 : mData.size();
	}

	@Override
	public String getItem(int position) {
		return mData.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			convertView = LayoutInflater.from(mContext).inflate(R.layout.item_content_list, parent, false);
			holder = new ViewHolder();
			holder.mTextItemContent = (TextView) convertView.findViewById(R.id.text_content_item);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		holder.mTextItemContent.setText(mData.get(position));
		return convertView;
	}

	private static class ViewHolder {
		TextView mTextItemContent;
	}
}
