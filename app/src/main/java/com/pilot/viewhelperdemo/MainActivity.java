package com.pilot.viewhelperdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.pilot.viewhelperdemo.scroller.ScrollerActivity;
import com.pilot.viewhelperdemo.viewdraghelp.ViewDragHelpActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initView();
	}

	private void initView() {
		findViewById(R.id.button_scroller).setOnClickListener(this);
		findViewById(R.id.button_view_drag_help).setOnClickListener(this);
	}


	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.button_scroller:
				ScrollerActivity.startup(this);
				break;
			case R.id.button_view_drag_help:
				ViewDragHelpActivity.startup(this);
				break;
		}
	}
}
