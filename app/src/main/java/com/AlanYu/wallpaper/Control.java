package com.AlanYu.wallpaper;

import com.AlanYu.Filter.DecisionMaker;
import com.AlanYu.Filter.TestFilter;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class Control extends Activity {

	static final int RESULT_ENABLE = 1;

	// TODO set protected list from here
	// TODO set mode from here
	// TODO set threshold from here

	Button startButton;
	Button stopButton;
	Button authorizeButton;
	Button disableDeviceManager;
	Button lockButton;
	EditText userType;
	EditText threshold;
	DevicePolicyManager mDPM;
	ActivityManager mAM;
	ComponentName mDeviceComponentName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.control);

		mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		mAM = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		mDeviceComponentName = new ComponentName(Control.this,
				deviceAdminReceiver.class);
		findView();
		setListener();
	}

	public Control() {
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case RESULT_ENABLE:
			if (resultCode == Activity.RESULT_OK) {
				Log.v("DeviceEnable", "deviceAdmin:enable");
			} else {
				Log.v("DeviceEnable", "deviceAdmin:disable");
			}
			break;
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	/*
	 * enable application to monitor the whole data
	 */
	private OnClickListener start = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent intent = new Intent(
					WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
			intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
					new ComponentName(com.AlanYu.wallpaper.Control.this,
							LiveWallPaper.class));
			setPreferences();
			SharedPreferences settings = getSharedPreferences("Preference", 0);
			Editor editor = settings.edit();

			if (userType.getText().toString().contains("owner")) {
				editor.putInt("Mode", DecisionMaker.TRAINING);
			}
			else
				editor.putInt("Mode", DecisionMaker.TEST);
			
			editor.putString("name", userType.getText().toString());
			editor.commit();
			startActivity(intent);
		}

		private void setPreferences() {
			// TODO Auto-generated method stub
			
		}
	};

	private OnClickListener authorize = new OnClickListener() {
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(
					DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
			intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
					mDeviceComponentName);
			intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
					"other more information");
			startActivityForResult(intent, RESULT_ENABLE);
		}
	};

	private OnClickListener stop = new OnClickListener() {
		@Override
		public void onClick(View v) {
			TestFilter test = new TestFilter();
		}
	};

	private OnClickListener disableDevice = new OnClickListener() {
		@Override
		public void onClick(View v) {
			mDPM.removeActiveAdmin(mDeviceComponentName);
		}
	};

	private OnClickListener forceLock = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (mAM.isUserAMonkey()) {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						Control.this);
				builder.setMessage("You can't not use this function , because you are not Admin");
				builder.setPositiveButton("I admit defeat", null);
				builder.show();
				return;
			}
			boolean active = mDPM.isAdminActive(mDeviceComponentName);
			if (active) {
				mDPM.lockNow();
			}
		}

	};

	private void findView() {
		startButton = (Button) findViewById(R.id.start);
		stopButton = (Button) findViewById(R.id.stop);
		lockButton = (Button) findViewById(R.id.lock);
		userType = (EditText) findViewById(R.id.userType);
		authorizeButton = (Button) findViewById(R.id.authorize);
		disableDeviceManager = (Button) findViewById(R.id.stopAuthorize);
		threshold  = (EditText) findViewById(R.id.threshold);
	}

	private void setListener() {
		startButton.setOnClickListener(start);
		stopButton.setOnClickListener(stop);
		authorizeButton.setOnClickListener(authorize);
		disableDeviceManager.setOnClickListener(disableDevice);
		lockButton.setOnClickListener(forceLock);
	}

}
