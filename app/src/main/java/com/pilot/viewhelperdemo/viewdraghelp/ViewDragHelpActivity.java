package com.pilot.viewhelperdemo.viewdraghelp;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.pilot.viewhelperdemo.R;

public class ViewDragHelpActivity extends AppCompatActivity {

	public static void startup(Context context) {
		context.startActivity(new Intent(context, ViewDragHelpActivity.class));
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_view_drag_help);
	}
}
