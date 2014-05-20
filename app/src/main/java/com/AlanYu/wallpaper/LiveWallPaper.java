package com.AlanYu.wallpaper;

import java.util.Iterator;
import java.util.Random;
import java.util.Vector;
import java.util.List;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

import com.AlanYu.Filter.AbstractFilter;
import com.AlanYu.Filter.DecisionMaker;
import com.AlanYu.Filter.DecisionTableFilter;
import com.AlanYu.Filter.J48Classifier;
import com.AlanYu.Filter.KStarClassifier;
import com.AlanYu.Filter.RandomForestClassifier;
import com.AlanYu.Filter.TestFilter;
import com.AlanYu.Filter.kNNClassifier;
import com.AlanYu.database.DBHelper;
import com.AlanYu.database.TouchDataNode;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.SensorEvent;
import android.os.PowerManager;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.widget.Toast;

@SuppressLint("ShowToast")
public class LiveWallPaper extends WallpaperService {

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}

	private int mode = DecisionMaker.TEST;
	private int pid = 0;
	private String deleteProcessName = null;
	protected Vector<TouchDataNode> vc;
	private Instances trainingData;
	private Instances testData;
	private J48Classifier j48;
	private DecisionMaker decisionMaker;
	KeyguardManager keyguardManager;
	KeyguardLock k1;
	KeyguardLock Keylock;
	PowerManager manager;
	DevicePolicyManager mDPM;
	ComponentName mDeviceAdminSample;

	/* Parameters from Control Activity */
	private static float THRESHOLD;
	private String[] PROTECTED_LIST = { "vending", "gm", "mms", "contact",
			"gallery" };
	private static String nowLabel;
	/*
	 * Parameters for Database Query
	 */
	// TODO Refactor code
	private static final String TOUCH_TABLE_NAME = "TOUCH";
	private static final String ID = "_ID";
	private static final String ACTION_TYPE = "ACTION";
	private static final String X = "X";
	private static final String Y = "Y";
	private static final String SIZE = "SIZE";
	private static final String PRESSURE = "PRESSURE";
	private static final String LABEL = "LABEL";
	private static final String TIMESTAMP = "TIMESTAMP";
	private static final String OWNER_LABEL = "owner";
	private static final String OTHER_LABEL = "other";


	@Override
	public void onCreate() {
		init();
		// TODO set parameter from the control activity
		// TODO   1.  test option for precision recall and F measure
		// TODO   2.  
		super.onCreate();
	}

	public class TouchEngine extends Engine {

		@Override
		public void onCreate(SurfaceHolder surfaceHolder) {
			super.onCreate(surfaceHolder);
			setTouchEventsEnabled(true);

		}

		@Override
		public void onTouchEvent(MotionEvent event) {
			if (mode == DecisionMaker.TRAINING)
				writeDataBase(event);
			else {
				// percentage split test precision
//				Log.d("on create","num of test instances"+testData.numInstances());
//				decisionMaker.evaluation(testData);
//				try {
//					decisionMaker.evaluationEachClassifier(testData);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
			
			}
			super.onTouchEvent(event);
		}
		
		@SuppressLint("NewApi")
		@Override
		public void onVisibilityChanged(boolean visible) {
			Intent intent = new Intent(LiveWallPaper.this,
					monitorAppService.class);

			if (visible) {
				stopService(intent);
				Keylock.disableKeyguard();
//				SharedPreferences settings = getSharedPreferences("Preference",
//						0);
//				String name = settings.getString("name", "");
				Log.d("visible", "true now user : " + nowLabel);
			} else {
				/*
				 * ============================================================ if
				 * wallpaperService is forebackground then kill monitorAppsService else
				 * start monitorAppsService
				 * ============================================================
				 */
				if (isInProtectList()) {
					decisionMaker.setThreshold(getThreshold());
					Log.d("invisible",
							"executed process is in the protect list ");

					if (DecisionMaker.IS_OTHER == decisionMaker
							.getFinalLabel(testData)) {
						startService(intent);
						Log.d("Decision making ", "You are other");
					} else
						Log.d("Decision making ", "You are owner");
					// TODO to record the recently precision and if precision
					// always drop below
					// 0.5 remember to add some retraining policy
					// TODO below is lock screen policy
					// if ((DecisionMaker.IS_OTHER == decisionMaker
					// .getFinalLabel(testData))) {
					// if (keyguardManager.) {
					// Log.d("lock screen",
					// "You are not the owner but u just unlock screen");
					// Keylock.disableKeyguard();
					// }
					// else{
					// Log.d("lock screen", "You are not the user");
					// Keylock.reenableKeyguard();
					// mDPM.lockNow();
					// }
					// // You are Owner
					// } else {
					// Keylock.disableKeyguard();
					// Log.d("invisible",
					// "it's owner and apps is also  in protected list ");
					// }
				}
			}
			super.onVisibilityChanged(visible);
		}
	}

	@Override
	public Engine onCreateEngine() {
		return new TouchEngine();
	}

	private boolean recentlyRunningApps(String processName) {
		ActivityManager service = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		List<RecentTaskInfo> recentTasks = service.getRecentTasks(1,
				ActivityManager.RECENT_WITH_EXCLUDED);
		for (RecentTaskInfo recentTaskInfo : recentTasks) {
			// System.out.println(recentTaskInfo.baseIntent);
			if (recentTaskInfo.baseIntent.toString().contains(processName)) {
				SharedPreferences settings = getSharedPreferences("Preference",
						0);
				settings.edit().putString("APP", processName).commit();
				return true;
            }
		}
		return false;
	}

	private boolean isInProtectList() {
		for (String processName : PROTECTED_LIST) {
			if (recentlyRunningApps(processName))
				return true;
		}
		return false;
	}

	private int findMatchProcessByName(String ps) {
		int notFound = 0;
		ActivityManager am = (ActivityManager) this
				.getSystemService(ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> l = am.getRunningAppProcesses();
		Iterator<RunningAppProcessInfo> i = l.iterator();
		PackageManager pm = this.getPackageManager();
		while (i.hasNext()) {
			ActivityManager.RunningAppProcessInfo info = (ActivityManager.RunningAppProcessInfo) (i
					.next());
			try {
				CharSequence c = pm.getApplicationLabel(pm.getApplicationInfo(
						info.processName, PackageManager.GET_META_DATA));
				String processName = info.processName;
				if (c.toString().equalsIgnoreCase(ps)) {
					return info.pid;
				}
			} catch (Exception e) {
				Log.e("errorTag", e.toString());
			}
		}
		return notFound;
	}

	/*
	 * ===================================================================== Get
	 * Now Running Apps Information
	 * =====================================================================
	 */
	private void getAppsInfo() {
		ActivityManager am = (ActivityManager) this
				.getSystemService(ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> l = am.getRunningAppProcesses();
		Iterator<RunningAppProcessInfo> i = l.iterator();
		PackageManager pm = this.getPackageManager();
		while (i.hasNext()) {
			ActivityManager.RunningAppProcessInfo info = (ActivityManager.RunningAppProcessInfo) (i
					.next());
			try {
				CharSequence c = pm.getApplicationLabel(pm.getApplicationInfo(
						info.processName, PackageManager.GET_META_DATA));
				String processName = info.processName;
				Log.d("Process", "Id: " + info.pid + " ProcessName: "
						+ info.processName + "  Label: " + c.toString());
			} catch (Exception e) {
				Log.e("Error tag", e.toString());
			}
		}

	}

	private void writeDataBase(MotionEvent event) {

		DBHelper db = new DBHelper(this);
		SQLiteDatabase writeSource = db.getWritableDatabase();

		ContentValues args = new ContentValues();
		args.put(X, String.valueOf(event.getX()));
		args.put(Y, String.valueOf(event.getY()));
		args.put(ACTION_TYPE, event.getAction());
		args.put(PRESSURE, String.valueOf(event.getPressure()));
		args.put(SIZE, String.valueOf(event.getSize()));
		args.put(TIMESTAMP, String.valueOf(event.getEventTime()));

		if (mode == DecisionMaker.TRAINING) {
			args.put(LABEL, OWNER_LABEL);
		} else {
			args.put(LABEL, nowLabel);
		}

		long rowid = writeSource.insert(TOUCH_TABLE_NAME, null, args);
		Log.d("writeDatabase Event",
				"id =" + rowid + " x:" + event.getX() + " y:" + event.getY()
						+ " Pressure" + event.getPressure() + " Size:"
						+ event.getSize() + " TimeStamp:"
						+ event.getEventTime() + "label:" + nowLabel);
		writeSource.close();
	}

	private void readDatabase() {
		DBHelper db = new DBHelper(this);
		SQLiteDatabase readSource = db.getReadableDatabase();
		Cursor cursor = readSource.query(TOUCH_TABLE_NAME, new String[] { ID,
				X, Y, PRESSURE, LABEL, SIZE, TIMESTAMP, ACTION_TYPE }, null,
				null, null, null, null);
		Log.d("readDatabase", "reading database");
		FastVector fv = decisionMaker.getWekaAttributes();

		try {
			if (cursor.moveToFirst()) {
				do {
					TouchDataNode touchData = new TouchDataNode();
					touchData
							.setId(cursor.getString(cursor.getColumnIndex(ID)));
					touchData.setX(cursor.getString(cursor.getColumnIndex(X)));
					touchData.setY(cursor.getString(cursor.getColumnIndex(Y)));
					touchData.setSize(cursor.getString(cursor
							.getColumnIndex(SIZE)));
					touchData.setPressure(cursor.getString(cursor
							.getColumnIndex(PRESSURE)));
					touchData.setTimestamp(cursor.getString(cursor
							.getColumnIndex(TIMESTAMP)));
					touchData.setLabel(cursor.getString(cursor
							.getColumnIndex(LABEL)));
					touchData.setActionType(cursor.getString(cursor
							.getColumnIndex(ACTION_TYPE)));

					if (touchData.getLabel().contains("domo")
							|| touchData.getLabel().contains("Jorge")
							|| touchData.getLabel().contains("CY")) {
					} else {
						Instance iExample = new DenseInstance(5);

						iExample.setValue((Attribute) fv.elementAt(0), Double
								.valueOf(cursor.getString(cursor
										.getColumnIndex(X))));
						iExample.setValue((Attribute) fv.elementAt(1), Double
								.valueOf(cursor.getString(cursor
										.getColumnIndex(Y))));
						iExample.setValue((Attribute) fv.elementAt(2), Double
								.valueOf(cursor.getString(cursor
										.getColumnIndex(PRESSURE))));
						iExample.setValue((Attribute) fv.elementAt(3), Double
								.valueOf(cursor.getString(cursor
										.getColumnIndex(SIZE))));
						if (touchData.getLabel().contains("owner"))
							iExample.setValue((Attribute) fv.elementAt(4),
									cursor.getString(cursor
											.getColumnIndex(LABEL)));
						else
							iExample.setValue((Attribute) fv.elementAt(4),
									OTHER_LABEL);

						trainingData.add(iExample);
					}

				} while (cursor.moveToNext());
			}
		} catch (Exception e1) {
			System.out.println(e1);
		} finally {
			cursor.close();
		}
		cursor.close();
		readSource.close();
	}

	private String[] getParameterFromControl() {
		String[] param = null;
		return param;
	}

	private void init() {

		// Build System Manager
		keyguardManager = (KeyguardManager) getSystemService(Activity.KEYGUARD_SERVICE);
		Keylock = keyguardManager.newKeyguardLock(Activity.KEYGUARD_SERVICE);
		manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		mDeviceAdminSample = new ComponentName(LiveWallPaper.this,
				deviceAdminReceiver.class);

		// TODO put the build model in asynTask
		decisionMaker = new DecisionMaker();
		testData = new Instances("TestData", decisionMaker.getWekaAttributes(),
				1000);
		trainingData = new Instances("TrainingData",
				decisionMaker.getWekaAttributes(), 1000);
		trainingData.setClassIndex(trainingData.numAttributes() - 1);
		testData.setClassIndex(testData.numAttributes() - 1);
		getSharedPreferenceSetting();
		
		//only in training mode need to read databse to build classifiers 
		if (mode == DecisionMaker.TEST){
			readDatabase();
			percentageSplit();
			decisionMaker.addDataToTraining(trainingData);
			decisionMaker.buildClassifier();
		}
	}

	private void getSharedPreferenceSetting() {
		SharedPreferences setting = getSharedPreferences("Preference", 0);
		nowLabel = setting.getString("name", OTHER_LABEL);
		THRESHOLD = setting.getFloat("Threshold", (float) 0.5);
		mode = setting.getInt("Mode", DecisionMaker.TEST);
		Log.d("in geshared prefernece ", "mode :"+mode);
		// TODO get protect list from apps
	}

	private void percentageSplit() {
		Instances inst = trainingData;
		Random ran = new Random(System.currentTimeMillis());
		inst.randomize(ran);
		float percent = (float) 0.6;
		int trainSize = (int) Math.round(inst.numInstances() * percent);
		int testSize = inst.numInstances() - trainSize;
		trainingData = new Instances(inst, 0, trainSize);
		testData = new Instances(inst, trainSize, testSize);
		Log.d("split", "testData size " + testData.numInstances()
				+ "traindata size " + trainingData.numInstances());
	}

	public static double getThreshold() {
		return THRESHOLD;
	}
}
