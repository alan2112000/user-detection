package com.AlanYu.wallpaper.test;

import com.AlanYu.wallpaper.LiveWallPaper;

import android.test.ActivityInstrumentationTestCase2;

public class detectExecutionAppTest extends ActivityInstrumentationTestCase2<LiveWallPaper> {

	
	public detectExecutionAppTest() {
		super("com.AlanYu.wallpaper",LiveWallPaper.class);
	}

	protected void setUp() throws Exception {
		super.setUp();
		
		LiveWallPaper mainActivity = getActivity();
		mainActivity.
	}

}
