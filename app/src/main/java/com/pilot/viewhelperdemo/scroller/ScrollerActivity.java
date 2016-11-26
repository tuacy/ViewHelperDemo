package com.pilot.viewhelperdemo.scroller;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.pilot.viewhelperdemo.R;

public class ScrollerActivity extends AppCompatActivity implements View.OnClickListener{

	public static void startup(Context context) {
		context.startActivity(new Intent(context, ScrollerActivity.class));
	}

	private ScrollerTextView mScrollerView;
	private     Button           mButtonScrollerTo;
	private     Button           mButtonScrollerBy;
	private     Button           mButtonSpringBack;

	private int mDistance;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scroller);
		initView();
		mDistance = 10;
	}

	private void initView() {
		mScrollerView = (ScrollerTextView) findViewById(R.id.text_scroller_view);
		mButtonScrollerTo = (Button) findViewById(R.id.btn_scroll_to);
		mButtonScrollerTo.setOnClickListener(this);
		mButtonScrollerBy = (Button) findViewById(R.id.btn_scroll_by);
		mButtonScrollerBy.setOnClickListener(this);
		mButtonSpringBack = (Button) findViewById(R.id.btn_spring_back);
		mButtonSpringBack.setOnClickListener(this);
	}


	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btn_scroll_to:
				mScrollerView.scrollTo(mDistance, 0);
				mDistance += 10;
				break;
			case R.id.btn_scroll_by:
				mScrollerView.scrollBy(30, 0);
				break;
			case R.id.btn_spring_back:
				//不知道为什么第一次调用会贴墙，即到达x=0的位置
				mScrollerView.springBack();
				break;
		}
	}
}
