package com.pilot.viewhelperdemo.scroller;


public enum  ScrollerMode {
	NONE(0x0),
	LEFT(0x1),
	RIGHT(0x2),
	BOTH(0x3);

	static ScrollerMode int2Value(final int modeInt) {
		for (ScrollerMode value : ScrollerMode.values()) {
			if (modeInt == value.getValue()) {
				return value;
			}
		}
		// If not, return default
		return getDefault();
	}

	public static ScrollerMode getDefault() {
		return NONE;
	}

	private int mValue;

	ScrollerMode(int value) {
		mValue = value;
	}

	int getValue() {
		return mValue;
	}
}
