package com.pilot.viewhelperdemo.viewdraghelp;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ScrollerCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Interpolator;

import java.util.Arrays;

public class ViewDragHelperCustomer {

	private static final String TAG = "ViewDragHelper";

	/**
	 * A null/invalid pointer ID.
	 */
	public static final int INVALID_POINTER = -1;

	/**
	 * A view is not currently being dragged or animating as a result of a fling/snap.
	 */
	public static final int STATE_IDLE = 0;

	/**
	 * A view is currently being dragged. The position is currently changing as a result of user input or simulated user input.
	 */
	public static final int STATE_DRAGGING = 1;

	/**
	 * A view is currently settling into place as a result of a fling or predefined non-interactive motion.
	 */
	public static final int STATE_SETTLING = 2;

	/**
	 * Edge flag indicating that the left edge should be affected.
	 */
	public static final int EDGE_LEFT = 1 << 0;

	/**
	 * Edge flag indicating that the right edge should be affected.
	 */
	public static final int EDGE_RIGHT = 1 << 1;

	/**
	 * Edge flag indicating that the top edge should be affected.
	 */
	public static final int EDGE_TOP = 1 << 2;

	/**
	 * Edge flag indicating that the bottom edge should be affected.
	 */
	public static final int EDGE_BOTTOM = 1 << 3;

	/**
	 * Edge flag set indicating all edges should be affected.
	 */
	public static final int EDGE_ALL = EDGE_LEFT | EDGE_TOP | EDGE_RIGHT | EDGE_BOTTOM;

	/**
	 * Indicates that a check should occur along the horizontal axis
	 */
	public static final int DIRECTION_HORIZONTAL = 1 << 0;

	/**
	 * Indicates that a check should occur along the vertical axis
	 */
	public static final int DIRECTION_VERTICAL = 1 << 1;

	/**
	 * Indicates that a check should occur along all axes
	 */
	public static final int DIRECTION_ALL = DIRECTION_HORIZONTAL | DIRECTION_VERTICAL;

	private static final int EDGE_SIZE = 20; // dp

	private static final int BASE_SETTLE_DURATION = 256; // ms
	private static final int MAX_SETTLE_DURATION  = 600; // ms

	// Current drag state; idle, dragging or settling
	private int mDragState;

	// Distance to travel before a drag may begin
	private int mTouchSlop;

	// Last known position/pointer tracking
	private int mActivePointerId = INVALID_POINTER;
	private float[] mInitialMotionX;
	private float[] mInitialMotionY;
	private float[] mLastMotionX;
	private float[] mLastMotionY;
	private int[]   mInitialEdgesTouched;
	private int[]   mEdgeDragsInProgress;
	private int[]   mEdgeDragsLocked;
	private int     mPointersDown;

	private VelocityTracker mVelocityTracker;
	private float           mMaxVelocity;
	private float           mMinVelocity;

	private int mEdgeSize;
	private int mTrackingEdges;

	private ScrollerCompat mScroller;

	private final Callback mCallback;

	private View    mCapturedView;
	private boolean mReleaseInProgress;

	private final ViewGroup mParentView;

	/**
	 * A Callback is used as a communication channel with the ViewDragHelper back to the parent view using it. <code>on*</code>methods are
	 * invoked on siginficant events and several accessor methods are expected to provide the ViewDragHelper with more information about the
	 * state of the parent view upon request. The callback also makes decisions governing the range and draggability of child views.
	 */
	public abstract static class Callback {

		/**
		 * 拖拽状态发生了变化，有三种情况 STATE_IDLE，STATE_DRAGGING，STATE_SETTLING
		 *
		 * @param state 新的状态
		 * @see #STATE_IDLE: 闲置状态
		 * @see #STATE_DRAGGING: 拖拽中
		 * @see #STATE_SETTLING:
		 */
		public void onViewDragStateChanged(int state) {
		}

		/**
		 * 捕获的View位置发生改变的回调
		 *
		 * @param changedView 捕获的View
		 * @param left        新的left的位置
		 * @param top         新的top的位置
		 * @param dx          相对前一个位置水平方向改变的位置
		 * @param dy          相对前一个位置垂直方向改变的位置
		 */
		public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
		}

		/**
		 * 当View被捕获时的回调
		 *
		 * @param capturedChild   捕获的View控件
		 * @param activePointerId Pointer id tracking the child capture
		 */
		public void onViewCaptured(View capturedChild, int activePointerId) {
		}

		/**
		 * 当手指释放的时候回调
		 *
		 * @param releasedChild 捕获的View
		 * @param xvel          释放的时候，水平方向的速度
		 * @param yvel          释放的时候，垂直方向的速度
		 */
		public void onViewReleased(View releasedChild, float xvel, float yvel) {
		}

		/**
		 * 触摸到边缘的时候回调
		 *
		 * @param edgeFlags A combination of edge flags describing the edge(s) currently touched
		 * @param pointerId ID of the pointer touching the described edge(s)
		 * @see #EDGE_LEFT
		 * @see #EDGE_TOP
		 * @see #EDGE_RIGHT
		 * @see #EDGE_BOTTOM
		 */
		public void onEdgeTouched(int edgeFlags, int pointerId) {
		}

		/**
		 * 用来设置锁住那些边缘，比如我们不想让左侧边缘能滑动，那么当edgeFlags时左侧边缘的时候，我们可以返回true。
		 *
		 * @param edgeFlags 当前触摸的边缘
		 * @return true 锁住, false 不锁住
		 */
		public boolean onEdgeLock(int edgeFlags) {
			return false;
		}

		/**
		 * 边缘拖动时的回调
		 *
		 * @param edgeFlags A combination of edge flags describing the edge(s) dragged
		 * @param pointerId ID of the pointer touching the described edge(s)
		 * @see #EDGE_LEFT
		 * @see #EDGE_TOP
		 * @see #EDGE_RIGHT
		 * @see #EDGE_BOTTOM
		 */
		public void onEdgeDragStarted(int edgeFlags, int pointerId) {
		}

		/**
		 * 通过这个方法，可以去修改查询的顺序(当有几个View重叠的时候可以打乱他的顺序)
		 *
		 * @param index 查询的顺序
		 * @return 返回我修改之后的顺序
		 */
		public int getOrderedChildIndex(int index) {
			return index;
		}

		/**
		 * 捕获的View可以水平拖动的范围
		 *
		 * @param child 捕获的View
		 * @return range of horizontal motion in pixels
		 */
		public int getViewHorizontalDragRange(View child) {
			return 0;
		}

		/**
		 * 捕获的View可以垂直拖动的范围
		 *
		 * @param child 捕获的View
		 * @return range of vertical motion in pixels
		 */
		public int getViewVerticalDragRange(View child) {
			return 0;
		}

		/**
		 * 询问是否可以捕获这个View
		 *
		 * @param child:手指按到的View控件
		 * @param pointerId ID of the pointer attempting the capture
		 * @return true 捕获该View, false 不捕获
		 */
		public abstract boolean tryCaptureView(View child, int pointerId);

		/**
		 * 对捕获的View移动前水平方向的一个处理，可以给调整left做一些边界控制
		 *
		 * @param child 捕获的View
		 * @param left  View将要到达的left位置
		 * @param dx    相对于ACTION_DOWN的时候的偏移位置
		 * @return 新的left的位置
		 */
		public int clampViewPositionHorizontal(View child, int left, int dx) {
			return 0;
		}

		/**
		 * 对捕获的View移动前垂直方向的一个处理，可以给调整top做一些边界控制
		 *
		 * @param child 捕获的View
		 * @param top   View将要到达的top位置
		 * @param dy    相对于ACTION_DOWN的时候的偏移位置
		 * @return 新的top的位置
		 */
		public int clampViewPositionVertical(View child, int top, int dy) {
			return 0;
		}
	}

	/**
	 * Interpolator defining the animation curve for mScroller
	 */
	private static final Interpolator sInterpolator = new Interpolator() {
		@Override
		public float getInterpolation(float t) {
			t -= 1.0f;
			return t * t * t * t * t + 1.0f;
		}
	};

	private final Runnable mSetIdleRunnable = new Runnable() {
		@Override
		public void run() {
			setDragState(STATE_IDLE);
		}
	};

	/**
	 * Factory method to create a new ViewDragHelper.
	 *
	 * @param forParent Parent view to monitor
	 * @param cb        Callback to provide information and receive events
	 * @return a new ViewDragHelper instance
	 */
	public static ViewDragHelperCustomer create(ViewGroup forParent, Callback cb) {
		return new ViewDragHelperCustomer(forParent.getContext(), forParent, cb);
	}

	/**
	 * Factory method to create a new ViewDragHelper.
	 *
	 * @param forParent   Parent view to monitor
	 * @param sensitivity Multiplier for how sensitive the helper should be about detecting the start of a drag. Larger values are more
	 *                    sensitive. 1.0f is normal.
	 * @param cb          Callback to provide information and receive events
	 * @return a new ViewDragHelper instance
	 */
	public static ViewDragHelperCustomer create(ViewGroup forParent, float sensitivity, Callback cb) {
		final ViewDragHelperCustomer helper = create(forParent, cb);
		helper.mTouchSlop = (int) (helper.mTouchSlop * (1 / sensitivity));
		return helper;
	}

	/**
	 * Apps should use ViewDragHelper.create() to get a new instance. This will allow VDH to use internal compatibility implementations for
	 * different platform versions.
	 *
	 * @param context   Context to initialize config-dependent params from
	 * @param forParent Parent view to monitor
	 */
	private ViewDragHelperCustomer(Context context, ViewGroup forParent, Callback cb) {
		if (forParent == null) {
			throw new IllegalArgumentException("Parent view may not be null");
		}
		if (cb == null) {
			throw new IllegalArgumentException("Callback may not be null");
		}

		mParentView = forParent;
		mCallback = cb;

		final ViewConfiguration vc = ViewConfiguration.get(context);
		final float density = context.getResources().getDisplayMetrics().density;
		mEdgeSize = (int) (EDGE_SIZE * density + 0.5f);

		mTouchSlop = vc.getScaledTouchSlop();
		mMaxVelocity = vc.getScaledMaximumFlingVelocity();
		mMinVelocity = vc.getScaledMinimumFlingVelocity();
		mScroller = ScrollerCompat.create(context, sInterpolator);
	}

	/**
	 * Set the minimum velocity that will be detected as having a magnitude greater than zero in pixels per second. Callback methods
	 * accepting a velocity will be clamped appropriately.
	 *
	 * @param minVel Minimum velocity to detect
	 */
	public void setMinVelocity(float minVel) {
		mMinVelocity = minVel;
	}

	/**
	 * Return the currently configured minimum velocity. Any flings with a magnitude less than this value in pixels per second. Callback
	 * methods accepting a velocity will receive zero as a velocity value if the real detected velocity was below this threshold.
	 *
	 * @return the minimum velocity that will be detected
	 */
	public float getMinVelocity() {
		return mMinVelocity;
	}

	/**
	 * Retrieve the current drag state of this helper. This will return one of {@link #STATE_IDLE}, {@link #STATE_DRAGGING} or {@link
	 * #STATE_SETTLING}.
	 *
	 * @return The current drag state
	 */
	public int getViewDragState() {
		return mDragState;
	}

	/**
	 * 设置哪个边缘是可以用的
	 *
	 * @param edgeFlags 四种类型EDGE_LEFT， EDGE_TOP， EDGE_RIGHT， EDGE_BOTTOM
	 */
	public void setEdgeTrackingEnabled(int edgeFlags) {
		mTrackingEdges = edgeFlags;
	}

	/**
	 * Return the size of an edge. This is the range in pixels along the edges of this view that will actively detect edge touches or drags
	 * if edge tracking is enabled.
	 *
	 * @return The size of an edge in pixels
	 * @see #setEdgeTrackingEnabled(int)
	 */
	public int getEdgeSize() {
		return mEdgeSize;
	}

	/**
	 * 捕获指定的View可以拖拽了
	 *
	 * @param childView       Child view to capture
	 * @param activePointerId ID of the pointer that is dragging the captured child view
	 */
	public void captureChildView(View childView, int activePointerId) {
		if (childView.getParent() != mParentView) {
			throw new IllegalArgumentException(
				"captureChildView: parameter must be a descendant " + "of the ViewDragHelper's tracked parent view (" + mParentView + ")");
		}
		// 一些赋值，记录
		mCapturedView = childView;
		mActivePointerId = activePointerId;
		mCallback.onViewCaptured(childView, activePointerId);
		// 设置状态在拖拽中
		setDragState(STATE_DRAGGING);
	}

	/**
	 * @return The currently captured view, or null if no view has been captured.
	 */
	public View getCapturedView() {
		return mCapturedView;
	}

	/**
	 * @return The ID of the pointer currently dragging the captured view, or {@link #INVALID_POINTER}.
	 */
	public int getActivePointerId() {
		return mActivePointerId;
	}

	/**
	 * @return The minimum distance in pixels that the user must travel to initiate a drag
	 */
	public int getTouchSlop() {
		return mTouchSlop;
	}

	/**
	 * The result of a call to this method is equivalent to {@link #processTouchEvent(android.view.MotionEvent)} receiving an ACTION_CANCEL
	 * event.
	 */
	public void cancel() {
		mActivePointerId = INVALID_POINTER;
		clearMotionHistory();

		if (mVelocityTracker != null) {
			mVelocityTracker.recycle();
			mVelocityTracker = null;
		}
	}

	/**
	 * {@link #cancel()}, but also abort all motion in progress and snap to the end of any animation.
	 */
	public void abort() {
		cancel();
		if (mDragState == STATE_SETTLING) {
			final int oldX = mScroller.getCurrX();
			final int oldY = mScroller.getCurrY();
			mScroller.abortAnimation();
			final int newX = mScroller.getCurrX();
			final int newY = mScroller.getCurrY();
			mCallback.onViewPositionChanged(mCapturedView, newX, newY, newX - oldX, newY - oldY);
		}
		setDragState(STATE_IDLE);
	}

	/**
	 * 把指定的View移动到指定位置。
	 *
	 * @param child     Child view to capture and animate
	 * @param finalLeft Final left position of child
	 * @param finalTop  Final top position of child
	 * @return true if animation should continue through {@link #continueSettling(boolean)} calls
	 */
	public boolean smoothSlideViewTo(View child, int finalLeft, int finalTop) {
		mCapturedView = child;
		mActivePointerId = INVALID_POINTER;

		boolean continueSliding = forceSettleCapturedViewAt(finalLeft, finalTop, 0, 0);
		if (!continueSliding && mDragState == STATE_IDLE && mCapturedView != null) {
			// If we're in an IDLE state to begin with and aren't moving anywhere, we
			// end up having a non-null capturedView with an IDLE dragState
			mCapturedView = null;
		}

		return continueSliding;
	}

	/**
	 * 捕获的View移动到指定的位置 这个函数只能在Callback的onViewReleased()函数里面调用
	 *
	 * @param finalLeft Settled left edge position for the captured view
	 * @param finalTop  Settled top edge position for the captured view
	 * @return true if animation should continue through {@link #continueSettling(boolean)} calls
	 */
	public boolean settleCapturedViewAt(int finalLeft, int finalTop) {
		if (!mReleaseInProgress) {
			// 确保函数只能在Callback的onViewReleased()函数里面调用
			throw new IllegalStateException("Cannot settleCapturedViewAt outside of a call to " + "Callback#onViewReleased");
		}

		return forceSettleCapturedViewAt(finalLeft, finalTop, (int) VelocityTrackerCompat.getXVelocity(mVelocityTracker, mActivePointerId),
										 (int) VelocityTrackerCompat.getYVelocity(mVelocityTracker, mActivePointerId));
	}

	/**
	 * Settle the captured view at the given (left, top) position.
	 *
	 * @param finalLeft Target left position for the captured view
	 * @param finalTop  Target top position for the captured view
	 * @param xvel      Horizontal velocity
	 * @param yvel      Vertical velocity
	 * @return true if animation should continue through {@link #continueSettling(boolean)} calls
	 */
	private boolean forceSettleCapturedViewAt(int finalLeft, int finalTop, int xvel, int yvel) {
		final int startLeft = mCapturedView.getLeft();
		final int startTop = mCapturedView.getTop();
		final int dx = finalLeft - startLeft;
		final int dy = finalTop - startTop;

		Log.d("tuacy", "dx = " + dx + "  dy = " + dy);

		if (dx == 0 && dy == 0) {
			// Nothing to do. Send callbacks, be done.
			mScroller.abortAnimation();
			setDragState(STATE_IDLE);
			return false;
		}

		final int duration = computeSettleDuration(mCapturedView, dx, dy, xvel, yvel);
		mScroller.startScroll(startLeft, startTop, dx, dy, duration);

		setDragState(STATE_SETTLING);
		return true;
	}

	private int computeSettleDuration(View child, int dx, int dy, int xvel, int yvel) {
		xvel = clampMag(xvel, (int) mMinVelocity, (int) mMaxVelocity);
		yvel = clampMag(yvel, (int) mMinVelocity, (int) mMaxVelocity);
		final int absDx = Math.abs(dx);
		final int absDy = Math.abs(dy);
		final int absXVel = Math.abs(xvel);
		final int absYVel = Math.abs(yvel);
		final int addedVel = absXVel + absYVel;
		final int addedDistance = absDx + absDy;

		final float xweight = xvel != 0 ? (float) absXVel / addedVel : (float) absDx / addedDistance;
		final float yweight = yvel != 0 ? (float) absYVel / addedVel : (float) absDy / addedDistance;

		int xduration = computeAxisDuration(dx, xvel, mCallback.getViewHorizontalDragRange(child));
		int yduration = computeAxisDuration(dy, yvel, mCallback.getViewVerticalDragRange(child));

		return (int) (xduration * xweight + yduration * yweight);
	}

	private int computeAxisDuration(int delta, int velocity, int motionRange) {
		if (delta == 0) {
			return 0;
		}

		final int width = mParentView.getWidth();
		final int halfWidth = width / 2;
		final float distanceRatio = Math.min(1f, (float) Math.abs(delta) / width);
		final float distance = halfWidth + halfWidth * distanceInfluenceForSnapDuration(distanceRatio);

		int duration;
		velocity = Math.abs(velocity);
		if (velocity > 0) {
			duration = 4 * Math.round(1000 * Math.abs(distance / velocity));
		} else {
			final float range = (float) Math.abs(delta) / motionRange;
			duration = (int) ((range + 1) * BASE_SETTLE_DURATION);
		}
		return Math.min(duration, MAX_SETTLE_DURATION);
	}

	/**
	 * Clamp the magnitude of value for absMin and absMax. If the value is below the minimum, it will be clamped to zero. If the value is
	 * above the maximum, it will be clamped to the maximum.
	 *
	 * @param value  Value to clamp
	 * @param absMin Absolute value of the minimum significant value to return
	 * @param absMax Absolute value of the maximum value to return
	 * @return The clamped value with the same sign as <code>value</code>
	 */
	private int clampMag(int value, int absMin, int absMax) {
		final int absValue = Math.abs(value);
		if (absValue < absMin) {
			return 0;
		}
		if (absValue > absMax) {
			return value > 0 ? absMax : -absMax;
		}
		return value;
	}

	/**
	 * Clamp the magnitude of value for absMin and absMax. If the value is below the minimum, it will be clamped to zero. If the value is
	 * above the maximum, it will be clamped to the maximum.
	 *
	 * @param value  Value to clamp
	 * @param absMin Absolute value of the minimum significant value to return
	 * @param absMax Absolute value of the maximum value to return
	 * @return The clamped value with the same sign as <code>value</code>
	 */
	private float clampMag(float value, float absMin, float absMax) {
		final float absValue = Math.abs(value);
		if (absValue < absMin) {
			return 0;
		}
		if (absValue > absMax) {
			return value > 0 ? absMax : -absMax;
		}
		return value;
	}

	private float distanceInfluenceForSnapDuration(float f) {
		f -= 0.5f; // center the values about 0.
		f *= 0.3f * Math.PI / 2.0f;
		return (float) Math.sin(f);
	}

	/**
	 * 释放的时候给一个fling动作，这个函数也只能在Callback的onViewReleased()函数里面调用
	 *
	 * @param minLeft x方向的最小距离
	 * @param minTop  y方向的最小距离
	 * @param maxLeft x方向的最大距离
	 * @param maxTop  y方向的最大距离
	 */
	public void flingCapturedView(int minLeft, int minTop, int maxLeft, int maxTop) {
		if (!mReleaseInProgress) {
			// 确保函数只能在Callback的onViewReleased()函数里面调用
			throw new IllegalStateException("Cannot flingCapturedView outside of a call to " + "Callback#onViewReleased");
		}

		mScroller.fling(mCapturedView.getLeft(), mCapturedView.getTop(),
						(int) VelocityTrackerCompat.getXVelocity(mVelocityTracker, mActivePointerId),
						(int) VelocityTrackerCompat.getYVelocity(mVelocityTracker, mActivePointerId), minLeft, maxLeft, minTop, maxTop);

		setDragState(STATE_SETTLING);
	}

	/**
	 * Move the captured settling view by the appropriate amount for the current time. If <code>continueSettling</code> returns true, the
	 * caller should call it again on the next frame to continue.
	 *
	 * @param deferCallbacks true if state callbacks should be deferred via posted message. Set this to true if you are calling this method
	 *                       from {@link android.view.View#computeScroll()} or similar methods invoked as part of layout or drawing.
	 * @return true if settle is still in progress
	 */
	public boolean continueSettling(boolean deferCallbacks) {
		if (mDragState == STATE_SETTLING) {
			boolean keepGoing = mScroller.computeScrollOffset();
			final int x = mScroller.getCurrX();
			final int y = mScroller.getCurrY();
			final int dx = x - mCapturedView.getLeft();
			final int dy = y - mCapturedView.getTop();

			if (dx != 0) {
				ViewCompat.offsetLeftAndRight(mCapturedView, dx);
			}
			if (dy != 0) {
				ViewCompat.offsetTopAndBottom(mCapturedView, dy);
			}

			if (dx != 0 || dy != 0) {
				mCallback.onViewPositionChanged(mCapturedView, x, y, dx, dy);
			}

			if (keepGoing && x == mScroller.getFinalX() && y == mScroller.getFinalY()) {
				// Close enough. The interpolator/scroller might think we're still moving
				// but the user sure doesn't.
				mScroller.abortAnimation();
				keepGoing = false;
			}

			if (!keepGoing) {
				if (deferCallbacks) {
					mParentView.post(mSetIdleRunnable);
				} else {
					setDragState(STATE_IDLE);
				}
			}
		}

		return mDragState == STATE_SETTLING;
	}

	/**
	 * Like all callback events this must happen on the UI thread, but release involves some extra semantics. During a release
	 * (mReleaseInProgress) is the only time it is valid to call {@link #settleCapturedViewAt(int, int)} or {@link #flingCapturedView(int,
	 * int, int, int)}.
	 */
	private void dispatchViewReleased(float xvel, float yvel) {
		mReleaseInProgress = true;
		mCallback.onViewReleased(mCapturedView, xvel, yvel);
		mReleaseInProgress = false;

		if (mDragState == STATE_DRAGGING) {
			// onViewReleased didn't call a method that would have changed this. Go idle.
			setDragState(STATE_IDLE);
		}
	}

	private void clearMotionHistory() {
		if (mInitialMotionX == null) {
			return;
		}
		Arrays.fill(mInitialMotionX, 0);
		Arrays.fill(mInitialMotionY, 0);
		Arrays.fill(mLastMotionX, 0);
		Arrays.fill(mLastMotionY, 0);
		Arrays.fill(mInitialEdgesTouched, 0);
		Arrays.fill(mEdgeDragsInProgress, 0);
		Arrays.fill(mEdgeDragsLocked, 0);
		mPointersDown = 0;
	}

	private void clearMotionHistory(int pointerId) {
		if (mInitialMotionX == null || !isPointerDown(pointerId)) {
			return;
		}
		mInitialMotionX[pointerId] = 0;
		mInitialMotionY[pointerId] = 0;
		mLastMotionX[pointerId] = 0;
		mLastMotionY[pointerId] = 0;
		mInitialEdgesTouched[pointerId] = 0;
		mEdgeDragsInProgress[pointerId] = 0;
		mEdgeDragsLocked[pointerId] = 0;
		mPointersDown &= ~(1 << pointerId);
	}

	private void ensureMotionHistorySizeForId(int pointerId) {
		if (mInitialMotionX == null || mInitialMotionX.length <= pointerId) {
			float[] imx = new float[pointerId + 1];
			float[] imy = new float[pointerId + 1];
			float[] lmx = new float[pointerId + 1];
			float[] lmy = new float[pointerId + 1];
			int[] iit = new int[pointerId + 1];
			int[] edip = new int[pointerId + 1];
			int[] edl = new int[pointerId + 1];

			if (mInitialMotionX != null) {
				System.arraycopy(mInitialMotionX, 0, imx, 0, mInitialMotionX.length);
				System.arraycopy(mInitialMotionY, 0, imy, 0, mInitialMotionY.length);
				System.arraycopy(mLastMotionX, 0, lmx, 0, mLastMotionX.length);
				System.arraycopy(mLastMotionY, 0, lmy, 0, mLastMotionY.length);
				System.arraycopy(mInitialEdgesTouched, 0, iit, 0, mInitialEdgesTouched.length);
				System.arraycopy(mEdgeDragsInProgress, 0, edip, 0, mEdgeDragsInProgress.length);
				System.arraycopy(mEdgeDragsLocked, 0, edl, 0, mEdgeDragsLocked.length);
			}

			mInitialMotionX = imx;
			mInitialMotionY = imy;
			mLastMotionX = lmx;
			mLastMotionY = lmy;
			mInitialEdgesTouched = iit;
			mEdgeDragsInProgress = edip;
			mEdgeDragsLocked = edl;
		}
	}

	private void saveInitialMotion(float x, float y, int pointerId) {
		ensureMotionHistorySizeForId(pointerId);
		mInitialMotionX[pointerId] = mLastMotionX[pointerId] = x;
		mInitialMotionY[pointerId] = mLastMotionY[pointerId] = y;
		mInitialEdgesTouched[pointerId] = getEdgesTouched((int) x, (int) y);
		mPointersDown |= 1 << pointerId;
	}

	private void saveLastMotion(MotionEvent ev) {
		final int pointerCount = ev.getPointerCount();
		for (int i = 0; i < pointerCount; i++) {
			final int pointerId = ev.getPointerId(i);
			// If pointer is invalid then skip saving on ACTION_MOVE.
			if (!isValidPointerForActionMove(pointerId)) {
				continue;
			}
			final float x = ev.getX(i);
			final float y = ev.getY(i);
			mLastMotionX[pointerId] = x;
			mLastMotionY[pointerId] = y;
		}
	}

	/**
	 * Check if the given pointer ID represents a pointer that is currently down (to the best of the ViewDragHelper's knowledge).
	 *
	 * <p>The state used to report this information is populated by the methods {@link #shouldInterceptTouchEvent(android.view.MotionEvent)}
	 * or {@link #processTouchEvent(android.view.MotionEvent)}. If one of these methods has not been called for all relevant MotionEvents to
	 * track, the information reported by this method may be stale or incorrect.</p>
	 *
	 * @param pointerId pointer ID to check; corresponds to IDs provided by MotionEvent
	 * @return true if the pointer with the given ID is still down
	 */
	public boolean isPointerDown(int pointerId) {
		return (mPointersDown & 1 << pointerId) != 0;
	}

	void setDragState(int state) {
		mParentView.removeCallbacks(mSetIdleRunnable);
		if (mDragState != state) {
			mDragState = state;
			mCallback.onViewDragStateChanged(state);
			if (mDragState == STATE_IDLE) {
				mCapturedView = null;
			}
		}
	}

	/**
	 * Attempt to capture the view with the given pointer ID. The callback will be involved. This will put us into the "dragging" state. If
	 * we've already captured this view with this pointer this method will immediately return true without consulting the callback.
	 *
	 * @param toCapture View to capture
	 * @param pointerId Pointer to capture with
	 * @return true if capture was successful
	 */
	boolean tryCaptureViewForDrag(View toCapture, int pointerId) {
		if (toCapture == mCapturedView && mActivePointerId == pointerId) {
			// Already done!
			return true;
		}
		if (toCapture != null && mCallback.tryCaptureView(toCapture, pointerId)) {
			mActivePointerId = pointerId;
			captureChildView(toCapture, pointerId);
			return true;
		}
		return false;
	}

	/**
	 * Tests scrollability within child views of v given a delta of dx.
	 *
	 * @param v      View to test for horizontal scrollability
	 * @param checkV Whether the view v passed should itself be checked for scrollability (true), or just its children (false).
	 * @param dx     Delta scrolled in pixels along the X axis
	 * @param dy     Delta scrolled in pixels along the Y axis
	 * @param x      X coordinate of the active touch point
	 * @param y      Y coordinate of the active touch point
	 * @return true if child views of v can be scrolled by delta of dx.
	 */
	protected boolean canScroll(View v, boolean checkV, int dx, int dy, int x, int y) {
		if (v instanceof ViewGroup) {
			final ViewGroup group = (ViewGroup) v;
			final int scrollX = v.getScrollX();
			final int scrollY = v.getScrollY();
			final int count = group.getChildCount();
			// Count backwards - let topmost views consume scroll distance first.
			for (int i = count - 1; i >= 0; i--) {
				// TODO: Add versioned support here for transformed views.
				// This will not work for transformed views in Honeycomb+
				final View child = group.getChildAt(i);
				if (x + scrollX >= child.getLeft() && x + scrollX < child.getRight() && y + scrollY >= child.getTop() &&
					y + scrollY < child.getBottom() &&
					canScroll(child, true, dx, dy, x + scrollX - child.getLeft(), y + scrollY - child.getTop())) {
					return true;
				}
			}
		}

		return checkV && (ViewCompat.canScrollHorizontally(v, -dx) || ViewCompat.canScrollVertically(v, -dy));
	}

	/**
	 * Check if this event as provided to the parent view's onInterceptTouchEvent should cause the parent to intercept the touch event
	 * stream.
	 *
	 * @param ev MotionEvent provided to onInterceptTouchEvent
	 * @return true if the parent view should return true from onInterceptTouchEvent
	 */
	public boolean shouldInterceptTouchEvent(MotionEvent ev) {
		final int action = MotionEventCompat.getActionMasked(ev);
		final int actionIndex = MotionEventCompat.getActionIndex(ev);

		if (action == MotionEvent.ACTION_DOWN) {
			// Reset things for a new event stream, just in case we didn't get
			// the whole previous stream.
			cancel();
		}

		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(ev);

		switch (action) {
			case MotionEvent.ACTION_DOWN: {
				final float x = ev.getX();
				final float y = ev.getY();
				final int pointerId = ev.getPointerId(0);
				saveInitialMotion(x, y, pointerId);

				// 找到对应左边点上的view
				final View toCapture = findTopChildUnder((int) x, (int) y);

				// Catch a settling view if possible.
				if (toCapture == mCapturedView && mDragState == STATE_SETTLING) {
					tryCaptureViewForDrag(toCapture, pointerId);
				}

				final int edgesTouched = mInitialEdgesTouched[pointerId];
				// edgesTouched有五中情况，EDGE_LEFT， EDGE_TOP，EDGE_RIGHT，EDGE_BOTTOM，0
				if ((edgesTouched & mTrackingEdges) != 0) {
					// 判断边缘是否可用(见setEdgeTrackingEnabled()函数)。
					mCallback.onEdgeTouched(edgesTouched & mTrackingEdges, pointerId);
				}
				break;
			}

			case MotionEventCompat.ACTION_POINTER_DOWN: {
				final int pointerId = ev.getPointerId(actionIndex);
				final float x = ev.getX(actionIndex);
				final float y = ev.getY(actionIndex);

				saveInitialMotion(x, y, pointerId);

				// A ViewDragHelper can only manipulate one view at a time.
				if (mDragState == STATE_IDLE) {
					final int edgesTouched = mInitialEdgesTouched[pointerId];
					if ((edgesTouched & mTrackingEdges) != 0) {
						mCallback.onEdgeTouched(edgesTouched & mTrackingEdges, pointerId);
					}
				} else if (mDragState == STATE_SETTLING) {
					// Catch a settling view if possible.
					final View toCapture = findTopChildUnder((int) x, (int) y);
					if (toCapture == mCapturedView) {
						tryCaptureViewForDrag(toCapture, pointerId);
					}
				}
				break;
			}

			case MotionEvent.ACTION_MOVE: {
				if (mInitialMotionX == null || mInitialMotionY == null) {
					break;
				}
				// First to cross a touch slop over a draggable view wins. Also report edge drags.
				final int pointerCount = ev.getPointerCount();
				for (int i = 0; i < pointerCount; i++) {
					final int pointerId = ev.getPointerId(i);

					// If pointer is invalid then skip the ACTION_MOVE.
					if (!isValidPointerForActionMove(pointerId)) {
						continue;
					}

					final float x = ev.getX(i);
					final float y = ev.getY(i);
					final float dx = x - mInitialMotionX[pointerId];
					final float dy = y - mInitialMotionY[pointerId];

					final View toCapture = findTopChildUnder((int) x, (int) y);
					final boolean pastSlop = toCapture != null && checkTouchSlop(toCapture, dx, dy);
					// 这里要看getViewHorizontalDragRange和getViewVerticalDragRange的重写情况
					if (pastSlop) {
						final int oldLeft = toCapture.getLeft();
						final int targetLeft = oldLeft + (int) dx;
						final int newLeft = mCallback.clampViewPositionHorizontal(toCapture, targetLeft, (int) dx);
						final int oldTop = toCapture.getTop();
						final int targetTop = oldTop + (int) dy;
						final int newTop = mCallback.clampViewPositionVertical(toCapture, targetTop, (int) dy);
						final int horizontalDragRange = mCallback.getViewHorizontalDragRange(toCapture);
						final int verticalDragRange = mCallback.getViewVerticalDragRange(toCapture);
						if ((horizontalDragRange == 0 || horizontalDragRange > 0 && newLeft == oldLeft) &&
							(verticalDragRange == 0 || verticalDragRange > 0 && newTop == oldTop)) {
							// 没有移动的情况
							break;
						}
					}
					reportNewEdgeDrags(dx, dy, pointerId);
					if (mDragState == STATE_DRAGGING) {
						// Callback might have started an edge drag
						break;
					}

					if (pastSlop && tryCaptureViewForDrag(toCapture, pointerId)) {
						break;
					}
				}
				saveLastMotion(ev);
				break;
			}

			case MotionEventCompat.ACTION_POINTER_UP: {
				final int pointerId = ev.getPointerId(actionIndex);
				clearMotionHistory(pointerId);
				break;
			}

			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL: {
				cancel();
				break;
			}
		}

		return mDragState == STATE_DRAGGING;
	}

	/**
	 * Process a touch event received by the parent view. This method will dispatch callback events as needed before returning. The parent
	 * view's onTouchEvent implementation should call this.
	 *
	 * @param ev The touch event received by the parent view
	 */
	public void processTouchEvent(MotionEvent ev) {
		final int action = MotionEventCompat.getActionMasked(ev);
		final int actionIndex = MotionEventCompat.getActionIndex(ev);
		if (action == MotionEvent.ACTION_DOWN) {
			// Reset things for a new event stream, just in case we didn't get
			// the whole previous stream.
			cancel();
		}

		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(ev);

		switch (action) {
			case MotionEvent.ACTION_DOWN: {
				final float x = ev.getX();
				final float y = ev.getY();
				final int pointerId = ev.getPointerId(0);
				final View toCapture = findTopChildUnder((int) x, (int) y);

				saveInitialMotion(x, y, pointerId);

				// Since the parent is already directly processing this touch event,
				// there is no reason to delay for a slop before dragging.
				// Start immediately if possible.
				tryCaptureViewForDrag(toCapture, pointerId);

				final int edgesTouched = mInitialEdgesTouched[pointerId];
				if ((edgesTouched & mTrackingEdges) != 0) {
					mCallback.onEdgeTouched(edgesTouched & mTrackingEdges, pointerId);
				}
				break;
			}

			case MotionEventCompat.ACTION_POINTER_DOWN: {
				final int pointerId = ev.getPointerId(actionIndex);
				final float x = ev.getX(actionIndex);
				final float y = ev.getY(actionIndex);

				saveInitialMotion(x, y, pointerId);

				// A ViewDragHelper can only manipulate one view at a time.
				if (mDragState == STATE_IDLE) {
					// If we're idle we can do anything! Treat it like a normal down event.

					final View toCapture = findTopChildUnder((int) x, (int) y);
					tryCaptureViewForDrag(toCapture, pointerId);

					final int edgesTouched = mInitialEdgesTouched[pointerId];
					if ((edgesTouched & mTrackingEdges) != 0) {
						mCallback.onEdgeTouched(edgesTouched & mTrackingEdges, pointerId);
					}
				} else if (isCapturedViewUnder((int) x, (int) y)) {
					// We're still tracking a captured view. If the same view is under this
					// point, we'll swap to controlling it with this pointer instead.
					// (This will still work if we're "catching" a settling view.)

					tryCaptureViewForDrag(mCapturedView, pointerId);
				}
				break;
			}

			case MotionEvent.ACTION_MOVE: {
				if (mDragState == STATE_DRAGGING) {
					// If pointer is invalid then skip the ACTION_MOVE.
					if (!isValidPointerForActionMove(mActivePointerId)) {
						break;
					}

					final int index = ev.findPointerIndex(mActivePointerId);
					final float x = ev.getX(index);
					final float y = ev.getY(index);
					final int idx = (int) (x - mLastMotionX[mActivePointerId]);
					final int idy = (int) (y - mLastMotionY[mActivePointerId]);

					dragTo(mCapturedView.getLeft() + idx, mCapturedView.getTop() + idy, idx, idy);

					saveLastMotion(ev);
				} else {
					// Check to see if any pointer is now over a draggable view.
					final int pointerCount = ev.getPointerCount();
					for (int i = 0; i < pointerCount; i++) {
						final int pointerId = ev.getPointerId(i);

						// If pointer is invalid then skip the ACTION_MOVE.
						if (!isValidPointerForActionMove(pointerId)) {
							continue;
						}

						final float x = ev.getX(i);
						final float y = ev.getY(i);
						final float dx = x - mInitialMotionX[pointerId];
						final float dy = y - mInitialMotionY[pointerId];

						Log.d("tuacy", "reportNewEdgeDrags");
						reportNewEdgeDrags(dx, dy, pointerId);
						if (mDragState == STATE_DRAGGING) {
							// Callback might have started an edge drag.
							break;
						}

						final View toCapture = findTopChildUnder((int) x, (int) y);
						if (checkTouchSlop(toCapture, dx, dy) && tryCaptureViewForDrag(toCapture, pointerId)) {
							break;
						}
					}
					saveLastMotion(ev);
				}
				break;
			}

			case MotionEventCompat.ACTION_POINTER_UP: {
				final int pointerId = ev.getPointerId(actionIndex);
				if (mDragState == STATE_DRAGGING && pointerId == mActivePointerId) {
					// Try to find another pointer that's still holding on to the captured view.
					int newActivePointer = INVALID_POINTER;
					final int pointerCount = ev.getPointerCount();
					for (int i = 0; i < pointerCount; i++) {
						final int id = ev.getPointerId(i);
						if (id == mActivePointerId) {
							// This one's going away, skip.
							continue;
						}

						final float x = ev.getX(i);
						final float y = ev.getY(i);
						if (findTopChildUnder((int) x, (int) y) == mCapturedView && tryCaptureViewForDrag(mCapturedView, id)) {
							newActivePointer = mActivePointerId;
							break;
						}
					}

					if (newActivePointer == INVALID_POINTER) {
						// We didn't find another pointer still touching the view, release it.
						releaseViewForPointerUp();
					}
				}
				clearMotionHistory(pointerId);
				break;
			}

			case MotionEvent.ACTION_UP: {
				if (mDragState == STATE_DRAGGING) {
					releaseViewForPointerUp();
				}
				cancel();
				break;
			}

			case MotionEvent.ACTION_CANCEL: {
				if (mDragState == STATE_DRAGGING) {
					dispatchViewReleased(0, 0);
				}
				cancel();
				break;
			}
		}
	}

	private void reportNewEdgeDrags(float dx, float dy, int pointerId) {
		int dragsStarted = 0;
		if (checkNewEdgeDrag(dx, dy, pointerId, EDGE_LEFT)) {
			dragsStarted |= EDGE_LEFT;
		}
		if (checkNewEdgeDrag(dy, dx, pointerId, EDGE_TOP)) {
			dragsStarted |= EDGE_TOP;
		}
		if (checkNewEdgeDrag(dx, dy, pointerId, EDGE_RIGHT)) {
			dragsStarted |= EDGE_RIGHT;
		}
		if (checkNewEdgeDrag(dy, dx, pointerId, EDGE_BOTTOM)) {
			dragsStarted |= EDGE_BOTTOM;
		}

		if (dragsStarted != 0) {
			mEdgeDragsInProgress[pointerId] |= dragsStarted;
			mCallback.onEdgeDragStarted(dragsStarted, pointerId);
		}
	}

	private boolean checkNewEdgeDrag(float delta, float odelta, int pointerId, int edge) {
		final float absDelta = Math.abs(delta);
		final float absODelta = Math.abs(odelta);

		if ((mInitialEdgesTouched[pointerId] & edge) != edge || (mTrackingEdges & edge) == 0 ||
			(mEdgeDragsLocked[pointerId] & edge) == edge || (mEdgeDragsInProgress[pointerId] & edge) == edge ||
			(absDelta <= mTouchSlop && absODelta <= mTouchSlop)) {
			return false;
		}
		if (absDelta < absODelta * 0.5f && mCallback.onEdgeLock(edge)) {
			mEdgeDragsLocked[pointerId] |= edge;
			return false;
		}
		return (mEdgeDragsInProgress[pointerId] & edge) == 0 && absDelta > mTouchSlop;
	}

	/**
	 * Check if we've crossed a reasonable touch slop for the given child view. If the child cannot be dragged along the horizontal or
	 * vertical axis, motion along that axis will not count toward the slop check.
	 *
	 * @param child Child to check
	 * @param dx    Motion since initial position along X axis
	 * @param dy    Motion since initial position along Y axis
	 * @return true if the touch slop has been crossed
	 */
	private boolean checkTouchSlop(View child, float dx, float dy) {
		if (child == null) {
			return false;
		}

		// 获取是否可以水平或者垂直拖动
		final boolean checkHorizontal = mCallback.getViewHorizontalDragRange(child) > 0;
		final boolean checkVertical = mCallback.getViewVerticalDragRange(child) > 0;
		if (checkHorizontal && checkVertical) {
			return dx * dx + dy * dy > mTouchSlop * mTouchSlop;
		} else if (checkHorizontal) {
			return Math.abs(dx) > mTouchSlop;
		} else if (checkVertical) {
			return Math.abs(dy) > mTouchSlop;
		}
		return false;
	}

	/**
	 * Check if any pointer tracked in the current gesture has crossed the required slop threshold.
	 *
	 * <p>This depends on internal state populated by {@link #shouldInterceptTouchEvent(android.view.MotionEvent)} or {@link
	 * #processTouchEvent(android.view.MotionEvent)}. You should only rely on the results of this method after all currently available touch
	 * data has been provided to one of these two methods.</p>
	 *
	 * @param directions Combination of direction flags, see {@link #DIRECTION_HORIZONTAL}, {@link #DIRECTION_VERTICAL}, {@link
	 *                   #DIRECTION_ALL}
	 * @return true if the slop threshold has been crossed, false otherwise
	 */
	public boolean checkTouchSlop(int directions) {
		final int count = mInitialMotionX.length;
		for (int i = 0; i < count; i++) {
			if (checkTouchSlop(directions, i)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if the specified pointer tracked in the current gesture has crossed the required slop threshold.
	 *
	 * <p>This depends on internal state populated by {@link #shouldInterceptTouchEvent(android.view.MotionEvent)} or {@link
	 * #processTouchEvent(android.view.MotionEvent)}. You should only rely on the results of this method after all currently available touch
	 * data has been provided to one of these two methods.</p>
	 *
	 * @param directions Combination of direction flags, see {@link #DIRECTION_HORIZONTAL}, {@link #DIRECTION_VERTICAL}, {@link
	 *                   #DIRECTION_ALL}
	 * @param pointerId  ID of the pointer to slop check as specified by MotionEvent
	 * @return true if the slop threshold has been crossed, false otherwise
	 */
	public boolean checkTouchSlop(int directions, int pointerId) {
		if (!isPointerDown(pointerId)) {
			return false;
		}

		final boolean checkHorizontal = (directions & DIRECTION_HORIZONTAL) == DIRECTION_HORIZONTAL;
		final boolean checkVertical = (directions & DIRECTION_VERTICAL) == DIRECTION_VERTICAL;

		final float dx = mLastMotionX[pointerId] - mInitialMotionX[pointerId];
		final float dy = mLastMotionY[pointerId] - mInitialMotionY[pointerId];

		if (checkHorizontal && checkVertical) {
			return dx * dx + dy * dy > mTouchSlop * mTouchSlop;
		} else if (checkHorizontal) {
			return Math.abs(dx) > mTouchSlop;
		} else if (checkVertical) {
			return Math.abs(dy) > mTouchSlop;
		}
		return false;
	}

	/**
	 * Check if any of the edges specified were initially touched in the currently active gesture. If there is no currently active gesture
	 * this method will return false.
	 *
	 * @param edges Edges to check for an initial edge touch. See {@link #EDGE_LEFT}, {@link #EDGE_TOP}, {@link #EDGE_RIGHT}, {@link
	 *              #EDGE_BOTTOM} and {@link #EDGE_ALL}
	 * @return true if any of the edges specified were initially touched in the current gesture
	 */
	public boolean isEdgeTouched(int edges) {
		final int count = mInitialEdgesTouched.length;
		for (int i = 0; i < count; i++) {
			if (isEdgeTouched(edges, i)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if any of the edges specified were initially touched by the pointer with the specified ID. If there is no currently active
	 * gesture or if there is no pointer with the given ID currently down this method will return false.
	 *
	 * @param edges Edges to check for an initial edge touch. See {@link #EDGE_LEFT}, {@link #EDGE_TOP}, {@link #EDGE_RIGHT}, {@link
	 *              #EDGE_BOTTOM} and {@link #EDGE_ALL}
	 * @return true if any of the edges specified were initially touched in the current gesture
	 */
	public boolean isEdgeTouched(int edges, int pointerId) {
		return isPointerDown(pointerId) && (mInitialEdgesTouched[pointerId] & edges) != 0;
	}

	private void releaseViewForPointerUp() {
		mVelocityTracker.computeCurrentVelocity(1000, mMaxVelocity);
		final float xvel = clampMag(VelocityTrackerCompat.getXVelocity(mVelocityTracker, mActivePointerId), mMinVelocity, mMaxVelocity);
		final float yvel = clampMag(VelocityTrackerCompat.getYVelocity(mVelocityTracker, mActivePointerId), mMinVelocity, mMaxVelocity);
		dispatchViewReleased(xvel, yvel);
	}

	private void dragTo(int left, int top, int dx, int dy) {
		int clampedX = left;
		int clampedY = top;
		final int oldLeft = mCapturedView.getLeft();
		final int oldTop = mCapturedView.getTop();
		if (dx != 0) {
			clampedX = mCallback.clampViewPositionHorizontal(mCapturedView, left, dx);
			// 移动View
			ViewCompat.offsetLeftAndRight(mCapturedView, clampedX - oldLeft);
		}
		if (dy != 0) {
			clampedY = mCallback.clampViewPositionVertical(mCapturedView, top, dy);
			// 移动View
			ViewCompat.offsetTopAndBottom(mCapturedView, clampedY - oldTop);
		}

		if (dx != 0 || dy != 0) {
			final int clampedDx = clampedX - oldLeft;
			final int clampedDy = clampedY - oldTop;
			mCallback.onViewPositionChanged(mCapturedView, clampedX, clampedY, clampedDx, clampedDy);
		}
	}

	/**
	 * Determine if the currently captured view is under the given point in the parent view's coordinate system. If there is no captured
	 * view this method will return false.
	 *
	 * @param x X position to test in the parent's coordinate system
	 * @param y Y position to test in the parent's coordinate system
	 * @return true if the captured view is under the given point, false otherwise
	 */
	public boolean isCapturedViewUnder(int x, int y) {
		return isViewUnder(mCapturedView, x, y);
	}

	/**
	 * Determine if the supplied view is under the given point in the parent view's coordinate system.
	 *
	 * @param view Child view of the parent to hit test
	 * @param x    X position to test in the parent's coordinate system
	 * @param y    Y position to test in the parent's coordinate system
	 * @return true if the supplied view is under the given point, false otherwise
	 */
	public boolean isViewUnder(View view, int x, int y) {
		if (view == null) {
			return false;
		}
		return x >= view.getLeft() && x < view.getRight() && y >= view.getTop() && y < view.getBottom();
	}

	/**
	 * 找出第一坐标点上的View
	 *
	 * @param x X position to test in the parent's coordinate system
	 * @param y Y position to test in the parent's coordinate system
	 * @return The topmost child view under (x, y) or null if none found.
	 */
	public View findTopChildUnder(int x, int y) {
		final int childCount = mParentView.getChildCount();
		for (int i = childCount - 1; i >= 0; i--) {
			final View child = mParentView.getChildAt(mCallback.getOrderedChildIndex(i));
			if (x >= child.getLeft() && x < child.getRight() && y >= child.getTop() && y < child.getBottom()) {
				return child;
			}
		}
		return null;
	}

	private int getEdgesTouched(int x, int y) {
		int result = 0;

		if (x < mParentView.getLeft() + mEdgeSize) {
			result |= EDGE_LEFT;
		}
		if (y < mParentView.getTop() + mEdgeSize) {
			result |= EDGE_TOP;
		}
		if (x > mParentView.getRight() - mEdgeSize) {
			result |= EDGE_RIGHT;
		}
		if (y > mParentView.getBottom() - mEdgeSize) {
			result |= EDGE_BOTTOM;
		}

		return result;
	}

	private boolean isValidPointerForActionMove(int pointerId) {
		if (!isPointerDown(pointerId)) {
			Log.e(TAG, "Ignoring pointerId=" + pointerId + " because ACTION_DOWN was not received " +
					   "for this pointer before ACTION_MOVE. It likely happened because " +
					   " ViewDragHelper did not receive all the events in the event stream.");
			return false;
		}
		return true;
	}
}