package com.pilot.viewhelperdemo.scroller;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

public class ScrollerListView extends ListView{

	public ScrollerListView(Context context) {
		this(context, null);
	}

	public ScrollerListView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ScrollerListView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}
}
