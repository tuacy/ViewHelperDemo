package com.pilot.viewhelperdemo.scroller;


import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

/**
 * ListView带有侧滑功能的item
 */
public class ScrollerItemLayout extends ViewGroup{


	public ScrollerItemLayout(Context context,
							int contentId,
							int leftId,
							int rightId) {
		super(context);
		/**
		 * 如果不设置，当item中含有button之类的控件的时候，item响应不了点击事件。
		 */
		setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {

	}
}
