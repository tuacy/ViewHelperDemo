package com.pilot.viewhelperdemo.viewdraghelp;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import com.pilot.viewhelperdemo.R;

/**
 * 左右可以侧拉的控件 里面三个控件 id_slide_content, id_slide_right, id_slide_left
 */

public class SlideLayout extends ViewGroup {

	enum SlideState {
		NORMAL,
		// 正常状态，没有菜单打开
		LEFT_OPEN,
		// 左侧菜单打开
		RIGHT_OPEN // 右侧菜单打开
	}

	// 主内容View
	private ViewGroup              mContentView;
	// 左侧菜单View
	private ViewGroup              mLeftView;
	// 右侧菜单View
	private ViewGroup              mRightView;
	// 左侧菜单的ViewDragHelper
	private ViewDragHelperCustomer mDragHelper;
	private SlideState             mCurrentState;

	private int mTouchSlop;

	public SlideLayout(Context context) {
		this(context, null);
	}

	public SlideLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public SlideLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	private void init() {
		mDragHelper = ViewDragHelperCustomer.create(this, mDragCallback);
		mDragHelper.setEdgeTrackingEnabled(ViewDragHelperCustomer.EDGE_LEFT | ViewDragHelperCustomer.EDGE_RIGHT);
		mCurrentState = SlideState.NORMAL;

		ViewConfiguration vc = ViewConfiguration.get(getContext());
		// 最小滑动距离
		mTouchSlop = vc.getScaledTouchSlop();
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		mContentView = (ViewGroup) findViewById(R.id.id_slide_content);
		if (mContentView == null) {
			throw new NullPointerException("we need content view");
		}
		// 把mContentView放到最上层
		bringChildToFront(mContentView);
		mLeftView = (ViewGroup) findViewById(R.id.id_slide_left);
		mRightView = (ViewGroup) findViewById(R.id.id_slide_right);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);
		// 先确定ViewGroup的大小，直接给了上面建议下来的大小
		setMeasuredDimension(widthSize, heightSize);
		// 确定ViewGroup里面的子View的大小
		// content view
		final int contentWidthSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);
		final int contentHeightSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY);
		mContentView.measure(contentWidthSpec, contentHeightSpec);
		// left view
		if (mLeftView != null) {
			final ViewGroup.LayoutParams leftLp = mLeftView.getLayoutParams();
			final int leftWidthSpec = getChildMeasureSpec(widthMeasureSpec, 0, leftLp.width);
			final int leftHeightSpec = getChildMeasureSpec(heightMeasureSpec, 0, leftLp.height);
			mLeftView.measure(leftWidthSpec, leftHeightSpec);
		}
		// right view
		if (mRightView != null) {
			final ViewGroup.LayoutParams rightLp = mRightView.getLayoutParams();
			final int rightWidthSpec = getChildMeasureSpec(widthMeasureSpec, 0, rightLp.width);
			final int rightHeightSpec = getChildMeasureSpec(heightMeasureSpec, 0, rightLp.height);
			mRightView.measure(rightWidthSpec, rightHeightSpec);
		}

	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// content view
		mContentView.layout(0, 0, mContentView.getMeasuredWidth(), mContentView.getMeasuredHeight());
		// left 把left放到左侧的屏幕之外吧
		if (mLeftView != null) {
			int leftViewLeft = -mLeftView.getMeasuredWidth();
			mLeftView.layout(leftViewLeft, 0, leftViewLeft + mLeftView.getMeasuredWidth(), mLeftView.getMeasuredHeight());
		}
		// right 把right放到右侧的屏幕之外吧
		if (mRightView != null) {
			int rightViewLeft = getMeasuredWidth();//getMeasuredWidth() - mRightView.getMeasuredWidth();
			mRightView.layout(rightViewLeft, 0, rightViewLeft + mRightView.getMeasuredWidth(), mRightView.getMeasuredHeight());
		}
	}

	private float mDownX, mDownY;

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		final int action = MotionEventCompat.getActionMasked(event);
		switch (action) {
			case MotionEvent.ACTION_DOWN:
				mDownX = event.getX();
				mDownY = event.getY();
				if (mCurrentState != SlideState.NORMAL && mDragHelper.findTopChildUnder((int) mDownX, (int) mDownY) == mContentView) {
					// 当菜单是打开的状态，并且触摸点是在content view上面的时候，事件拦截下来，不给content view里面去相应了。
					mDragHelper.shouldInterceptTouchEvent(event);
					return true;
				}
				break;
			case MotionEvent.ACTION_MOVE:
				final float x = event.getX();
				final float y = event.getY();
				final float dx = x - mDownX;
				final float dy = y - mDownY;
				if (dx * dx + dy * dy > mTouchSlop * mTouchSlop) {
					// 产生了滑动
					if ((mCurrentState == SlideState.LEFT_OPEN && mDragHelper.findTopChildUnder((int) x, (int) y) == mLeftView) ||
						(mCurrentState == SlideState.RIGHT_OPEN && mDragHelper.findTopChildUnder((int) x, (int) y) == mRightView)) {
						// 当左侧或者右侧菜单打开了，并且你在上面产生了滑动时间，这个时候事件不能给子View去处理了。
						mDragHelper.shouldInterceptTouchEvent(event);
						return true;
					}
				}
				break;
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
				mDragHelper.cancel();
				return false;
		}
		return mDragHelper.shouldInterceptTouchEvent(event);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mDragHelper.processTouchEvent(event);
		return true;
	}

	@Override
	public void computeScroll() {
		if (mDragHelper.continueSettling(true)) {
			ViewCompat.postInvalidateOnAnimation(this);
		}
	}

	ViewDragHelperCustomer.Callback mDragCallback = new ViewDragHelperCustomer.Callback() {

		@Override
		public boolean tryCaptureView(View child, int pointerId) {
			// 这里我们应该是可以捕获left view 和 content view 和 right view
			return (mLeftView != null && child == mLeftView) || (mRightView != null && child == mRightView) || child == mContentView;
		}

		@Override
		public void onEdgeDragStarted(int edgeFlags, int pointerId) {
			// 当触摸到边缘的时候转换给其他的View
			View captureView;
			if ((edgeFlags & ViewDragHelper.EDGE_LEFT) == ViewDragHelper.EDGE_LEFT && mLeftView != null &&
				mCurrentState == SlideState.LEFT_OPEN) {
				captureView = mLeftView;
			} else if ((edgeFlags & ViewDragHelper.EDGE_RIGHT) == ViewDragHelper.EDGE_RIGHT && mRightView != null &&
					   mCurrentState == SlideState.RIGHT_OPEN) {
				captureView = mRightView;
			} else {
				captureView = mContentView;
			}
			mDragHelper.captureChildView(captureView, pointerId);
		}

		@Override
		public int clampViewPositionHorizontal(View child, int left, int dx) {
			int captureViewLeft = left;
			if (mLeftView != null && child == mLeftView) {
				captureViewLeft = left > 0 ? 0 : left;
				captureViewLeft = captureViewLeft < -mLeftView.getWidth() ? -mLeftView.getWidth() : captureViewLeft;
			} else if (mRightView != null && child == mRightView) {
				captureViewLeft = left < mContentView.getWidth() - mRightView.getWidth() ? mContentView.getWidth() - mRightView.getWidth() :
								  left;
				captureViewLeft = captureViewLeft > mContentView.getWidth() ? mContentView.getWidth() : captureViewLeft;
			} else if (mContentView == child) {
				// contentView left的初始位置是0
				if (mLeftView != null && left >= 0) {
					// 手指往右侧滑动 不能超过left view的宽度
					captureViewLeft = left > mLeftView.getWidth() ? mLeftView.getWidth() : left;
				}
				if (mRightView != null && left < 0) {
					// 手指往左侧滑动 不能超过right view的宽度
					captureViewLeft = left > -mRightView.getMeasuredWidth() ? left : -mRightView.getWidth();
				}
			}
			return captureViewLeft;
		}

		@Override
		public int clampViewPositionVertical(View child, int top, int dy) {
			// 垂直方向是不移动的
			return 0;
		}

		@Override
		public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
			if (mLeftView != null && changedView == mLeftView) {
				if (mRightView != null) {
					ViewCompat.offsetLeftAndRight(mRightView, dx);
				}
				ViewCompat.offsetLeftAndRight(mContentView, dx);
			} else if (mRightView != null && changedView == mRightView) {
				if (mLeftView != null) {
					ViewCompat.offsetLeftAndRight(mLeftView, dx);
				}
				ViewCompat.offsetLeftAndRight(mContentView, dx);
			} else if (mContentView == changedView) {
				if (mLeftView != null) {
					ViewCompat.offsetLeftAndRight(mLeftView, dx);
				}
				if (mRightView != null) {
					ViewCompat.offsetLeftAndRight(mRightView, dx);
				}
			}
		}

		@Override
		public void onViewReleased(View releasedChild, float xvel, float yvel) {
			// 当手指释放的时候调用
			int finalLeft = releasedChild.getLeft();
			if (mLeftView != null && releasedChild == mLeftView) {
				finalLeft = Math.abs(mLeftView.getLeft()) > mLeftView.getWidth() / 2 ? -mLeftView.getWidth() : 0;
				mDragHelper.smoothSlideViewTo(releasedChild, finalLeft, 0);
				invalidate();
			} else if (mRightView != null && releasedChild == mRightView) {
				finalLeft = Math.abs(mRightView.getLeft() - mContentView.getWidth()) > mRightView.getWidth() / 2 ?
							mContentView.getWidth() - mRightView.getWidth() : mContentView.getWidth();
				mDragHelper.smoothSlideViewTo(releasedChild, finalLeft, 0);
				invalidate();
			} else if (mContentView == releasedChild) {
				// contentView left的初始位置是0
				if (releasedChild.getLeft() > 0 && mLeftView != null) {
					finalLeft = releasedChild.getLeft() > mLeftView.getWidth() / 2 ? mLeftView.getWidth() : 0;
					mCurrentState = finalLeft == 0 ? SlideState.NORMAL : SlideState.LEFT_OPEN;
				}

				if (releasedChild.getLeft() < 0 && mRightView != null) {
					finalLeft = releasedChild.getLeft() < -mRightView.getWidth() / 2 ? -mRightView.getWidth() : 0;
					mCurrentState = finalLeft == 0 ? SlideState.NORMAL : SlideState.RIGHT_OPEN;
				}
				mDragHelper.smoothSlideViewTo(releasedChild, finalLeft, 0);
				invalidate();
			}
		}

		@Override
		public int getViewHorizontalDragRange(View child) {
			if (mLeftView != null && child == mLeftView) {
				return mLeftView.getWidth();
			}
			if (mRightView != null && child == mRightView) {
				return mRightView.getWidth();
			}
			return super.getViewHorizontalDragRange(child);
		}
	};

	public void toggle() {
		if (mCurrentState == SlideState.NORMAL) {
			// 如果是没有菜单打开的状态
			if (mLeftView != null) {
				// 打开左侧菜单
				mCurrentState = SlideState.LEFT_OPEN;
				mDragHelper.smoothSlideViewTo(mContentView, mLeftView.getWidth(), 0);
				invalidate();
			} else if (mRightView != null) {
				// 打开右侧菜单
				mCurrentState = SlideState.RIGHT_OPEN;
				mDragHelper.smoothSlideViewTo(mContentView, -mRightView.getWidth(), 0);
				invalidate();
			}
		} else {
			// 打开左侧菜单
			mCurrentState = SlideState.NORMAL;
			mDragHelper.smoothSlideViewTo(mContentView, 0, 0);
			invalidate();
		}
	}

}
