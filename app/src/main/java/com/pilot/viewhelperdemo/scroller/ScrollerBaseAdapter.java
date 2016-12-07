package com.pilot.viewhelperdemo.scroller;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public abstract class ScrollerBaseAdapter<T extends ScrollerBaseAdapter.ViewHolder> extends BaseAdapter{

	protected Context mContext;

	public ScrollerBaseAdapter(Context context) {
		mContext = context;
	}

	public ScrollerMode getScrollerMode(int position) {
		return ScrollerMode.getDefault();
	}

	public abstract int getContentLayout(int position);

	public abstract int getLeftLayout(int position);

	public abstract int getRightLayout(int position);

	public abstract void bindData(T holder, int position);

	public abstract T obtainHolder(View convertView, int position);

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		T holder;
		if (convertView == null) {
			convertView = new ScrollerItemLayout(mContext, getContentLayout(position), getLeftLayout(position), getRightLayout(position));
			holder = obtainHolder(convertView, position);
			convertView.setTag(holder);
		} else{
			holder = (T) convertView.getTag();
		}
		bindData(holder, position);
		return convertView;
	}

	public static class ViewHolder {

		public ViewHolder(View convertView) {

		}
	}
}
