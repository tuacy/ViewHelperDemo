package com.pilot.viewhelperdemo.viewdraghelp;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import static com.pilot.viewhelperdemo.viewdraghelp.ViewDragHelperCustomer.EDGE_LEFT;


public class DragLayout extends LinearLayout {

	private ViewDragHelperCustomer mDrag;

	public DragLayout(Context context) {
		this(context, null);
	}

	public DragLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public DragLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mDrag = ViewDragHelperCustomer.create(this, 1.0f, new ViewDragHelperCustomer.Callback() {
			@Override
			public boolean tryCaptureView(View child, int pointerId) {
				return true;
			}

			@Override
			public int clampViewPositionHorizontal(View child, int left, int dx) {
				return left;
			}

			@Override
			public int clampViewPositionVertical(View child, int top, int dy) {
				return top;
			}

			@Override
			public void onViewDragStateChanged(int state) {
				super.onViewDragStateChanged(state);
			}

			@Override
			public boolean onEdgeLock(int edgeFlags) {
				return super.onEdgeLock(edgeFlags);
			}

			@Override
			public void onEdgeDragStarted(int edgeFlags, int pointerId) {
				super.onEdgeDragStarted(edgeFlags, pointerId);
			}

			@Override
			public void onEdgeTouched(int edgeFlags, int pointerId) {
				super.onEdgeTouched(edgeFlags, pointerId);
			}
		});
		mDrag.setEdgeTrackingEnabled(EDGE_LEFT);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		return mDrag.shouldInterceptTouchEvent(event);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mDrag.processTouchEvent(event);
		return true;
	}

	@Override
	public void computeScroll() {
		if (mDrag.continueSettling(true)) {
			ViewCompat.postInvalidateOnAnimation(this);
		}
	}
}
