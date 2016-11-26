package com.pilot.viewhelperdemo.scroller;


import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.animation.BounceInterpolator;
import android.widget.TextView;

import com.pilot.viewhelperdemo.OverScrollerCustomer;

public class ScrollerTextView extends TextView {

	private OverScrollerCustomer mScroller;

	private float lastX;
	private float lastY;

	private float           startX;
	private float           startY;
	private VelocityTracker mVelocityTracker;
	private int             mScaledMinimumFlingVelocity;
	private int             mScaledMaximumFlingVelocity;

	public ScrollerTextView(Context context) {
		this(context, null);
	}

	public ScrollerTextView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ScrollerTextView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mScroller = new OverScrollerCustomer(context, new BounceInterpolator());
		mScaledMinimumFlingVelocity = ViewConfiguration.get(getContext()).getScaledMinimumFlingVelocity();
		mScaledMaximumFlingVelocity = ViewConfiguration.get(getContext()).getScaledMaximumFlingVelocity();
		mVelocityTracker = VelocityTracker.obtain();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		mVelocityTracker.addMovement(event);
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				lastX = event.getRawX();
				lastY = event.getRawY();
				break;
			case MotionEvent.ACTION_MOVE:
				float disX = event.getRawX() - lastX;
				float disY = event.getRawY() - lastY;

				offsetLeftAndRight((int) disX);
				offsetTopAndBottom((int) disY);
				lastX = event.getRawX();
				lastY = event.getRawY();
				break;
			case MotionEvent.ACTION_UP:
				mVelocityTracker.computeCurrentVelocity(1000, mScaledMaximumFlingVelocity);
				float yVelocity = mVelocityTracker.getYVelocity();
				mVelocityTracker.clear();
				mScroller.fling((int) getX(), (int) getY(), 0, 1000,//x加速度，y加速度
								0, (int) getX(),//  x方向fling的范围
								0, (int) getY() - 200);// y方向fling的范围
				//				mScroller.startScroll((int) getX(), (int) getY(), -(int) (getX() - startX), -(int) (getY() - startY));
				invalidate();
				break;
		}

		return true;
	}

	@Override
	public void computeScroll() {
		if (mScroller.computeScrollOffset()) {
			setX(mScroller.getCurrX());
			setY(mScroller.getCurrY());
			//			Log.d("tuacy", "mScroller.getCurrY() = " + mScroller.getCurrY());
			invalidate();
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		startX = getX();
		startY = getY();
	}

	public void springBack() {
		if (mScroller.springBack((int) getX(), (int) getY(), 0, (int) getX(), 0, (int) getY() - 100)) {
			invalidate();
		}

	}
}
