package com.pilot.viewhelperdemo.viewdraghelp;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.pilot.viewhelperdemo.R;

import java.util.ArrayList;
import java.util.List;

public class ViewDragHelpActivity extends AppCompatActivity {

	public static void startup(Context context) {
		context.startActivity(new Intent(context, ViewDragHelpActivity.class));
	}

	private Context      mContext;
	private SlideLayout mLayoutSlide;
	private List<String> mContentDataList;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_slide_layout);
		mContext = this;
		initView();
		initData();
	}

	private void initView() {
		mLayoutSlide = (SlideLayout) findViewById(R.id.layout_slide);
		ListView listViewContent = (ListView) findViewById(R.id.list_content);
		mContentDataList = obtainContentTextData();
		listViewContent.setAdapter(new ContentAdapter(mContext, mContentDataList));
		listViewContent.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Toast.makeText(mContext, mContentDataList.get(position), Toast.LENGTH_LONG).show();
			}
		});
		LinearLayout leftSetting = (LinearLayout) findViewById(R.id.layout_left_setting);
		leftSetting.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(mContext, "点击了左侧菜单设置按钮", Toast.LENGTH_LONG).show();
			}
		});
	}

	private void initData() {

	}

	private List<String> obtainContentTextData() {
		List<String> textData = new ArrayList<>();
		textData.add("江西");
		textData.add("南昌");
		textData.add("萍乡");
		textData.add("赣州");
		textData.add("宜春");
		textData.add("高安");
		return textData;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_MENU:
				mLayoutSlide.toggle();
				return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}
