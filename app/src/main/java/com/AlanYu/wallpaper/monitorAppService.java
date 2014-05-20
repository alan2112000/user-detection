package com.AlanYu.wallpaper;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import com.AlanYu.Filter.DecisionMaker;
import com.AlanYu.Filter.J48ClassiferForAC;
import com.AlanYu.database.DBHelper;

import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

public class monitorAppService extends IntentService implements
		SensorEventListener {

	private String errorTag = "Exception";
	private String sensorTag = "Sensor data";
	private SensorManager sensorManager;
	private Vector<Cursor> vc;
	private String userType;
	private String appType;
	private boolean TRAING_MODE = false;
	private int ownerLabelNumber = 0;
	private int otherLabelNumber = 0;

	// filter
	J48ClassiferForAC j48;
	// Database parameter
	private static final String tableName = "SENSOR_MAIN";
	private static final String xColumn = "X";
	private static final String yColumn = "Y";
	private static final String zColumn = "Z";
	private static final String appColumn = "APP";
	private static final String labelColumn = "LABEL";

	public monitorAppService() {
		super("monitorAppService");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		Log.d("monitorAppService", "onCreate");
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		j48 = new J48ClassiferForAC();
		setSensor();
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		SharedPreferences settings = getSharedPreferences("Preference", 0);

		userType = settings.getString("name", "");
		appType = settings.getString("APP", "");

		// catach the the acceleter data app name is appType and
		try {
			readDatabase(appType);
			j48.trainingData();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		Log.d("on startCommand", "finish readDtabase");

		// build the model
		if (intent != null) {
			Log.d("monitorAppService", "onStartCommand");
		}

		else
			Log.d("monitorAppService", "intent is null");
		return START_STICKY;
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
				Log.e(errorTag, e.toString());
			}
		}

	}

	/*
	 * =====================================================================
	 * find the pid of the Process for purpose to kill process
	 * 
	 * =====================================================================
	 */
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
				Log.e(errorTag, e.toString());
			}
		}
		return notFound;
	}

	/*
	 * pass the pid value to delete the process
	 */
	private boolean deleteProcessByName(String ps) {
		int pid = findMatchProcessByName(ps);
		if (pid != 0) {
			android.os.Process.killProcess(pid);
			return true;
		} else
			return false;
	}

	private void setSensor() {
		List<Sensor> sensors = sensorManager
				.getSensorList(Sensor.TYPE_ACCELEROMETER);
		if (sensors.size() > 0) {
			sensorManager.registerListener(this, sensors.get(0),
					SensorManager.SENSOR_DELAY_NORMAL);
		}
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d("intentService", "in onHandleIntent");
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onDestroy() {
		sensorManager.unregisterListener(this);
		super.onDestroy();
	}

	@Override
	public void onSensorChanged(SensorEvent event) {

		float[] values = event.values;
		int timeStamp = (int) (event.timestamp / 1000000000);
//		Log.d(sensorTag,
//				"X :" + String.valueOf(values[0]) + " Y: "
//						+ String.valueOf(values[1]) + "z :"
//						+ String.valueOf(values[2]) + "TimeStamp :"
//						+ String.valueOf(timeStamp) + " use type :" + userType
//						+ "app: " + appType);
		// set instance
		
		// training Mode
		if (TRAING_MODE)
			writeDataBase(event);
		else {
			FastVector fv = j48.getFvWekaAttributes();
			Instance iExample = new DenseInstance(4);
			iExample.setValue((Attribute) fv.elementAt(0), values[0]);
			iExample.setValue((Attribute) fv.elementAt(1), values[1]);
			iExample.setValue((Attribute) fv.elementAt(2), values[2]);

			Instances dataUnLabeled = new Instances("TestInstances", fv, 10);
			dataUnLabeled.add(iExample);
			dataUnLabeled.setClassIndex(dataUnLabeled.numAttributes() - 1);
			double[] prediction;
			double prediction2;
			try {
				prediction = j48.returnClassifier().distributionForInstance(
						dataUnLabeled.firstInstance());
				// output predictions
				for (int i = 0; i < prediction.length; i++) {
					System.out.println("Probability of class "
							+ dataUnLabeled.classAttribute().value(i) + " : "
							+ Double.toString(prediction[i]));
				}
				
				if(prediction[DecisionMaker.IS_OWNER] > prediction[DecisionMaker.IS_OTHER])
					ownerLabelNumber++; 
				else 
					otherLabelNumber++; 
				
				double precision = (double) ownerLabelNumber /(otherLabelNumber+ownerLabelNumber); 
				Log.d("DecisionMaker", "Now Confidence is :"+Double.toString(precision));
			} catch (Exception e) {
				e.printStackTrace();
			}

//			j48.predictInstance(iExample);

		}
	}

	private void writeDataBase(SensorEvent event) {

		DBHelper db = new DBHelper(this);
		SQLiteDatabase writeSource = db.getWritableDatabase();
		float[] values = event.values;
		ContentValues args = new ContentValues();
		int timeStamp = (int) (event.timestamp / 1000000000);
		args.put("SENSOR_TYPE", "AC");
		args.put("X", String.valueOf(values[0]));
		args.put("Y", String.valueOf(values[1]));
		args.put("Z", String.valueOf(values[2]));
		args.put("TIME_STAMP", String.valueOf(timeStamp));
		args.put("LABEL", userType);
		args.put("APP", appType);
		long rowid = writeSource.insert("SENSOR_MAIN", null, args);
		Log.d("writeDatabase Event", "id =" + rowid);
		writeSource.close();
	}

	private void readDatabase(String appName) throws SQLException {
		DBHelper db = new DBHelper(this);
		SQLiteDatabase readSource = db.getReadableDatabase();
		Cursor cursor = readSource.query(tableName, new String[] { xColumn,
				yColumn, zColumn, labelColumn, appColumn }, null, null, null,
				null, null);
		double x = 0, y = 0, z = 0;
		if (cursor != null) {
			cursor.moveToFirst();
			while (cursor.isAfterLast() == false) {
				x = Double.valueOf(cursor.getString(cursor
						.getColumnIndex(xColumn)));
				y = Double.valueOf(cursor.getString(cursor
						.getColumnIndex(yColumn)));
				z = Double.valueOf(cursor.getString(cursor
						.getColumnIndex(zColumn)));
				String label = cursor.getString(cursor
						.getColumnIndex(labelColumn));
				// Log.d("Read Database", "x =" + x + "y=" + y + "z=" + z +
				// " label : "+label);

				FastVector fv = j48.getFvWekaAttributes();
				Instance iExample = new DenseInstance(4);
				if (label.contains("domo") || label.contains("CY")
						|| label.contains("Jorge"))
					;
				else {
					iExample.setValue((Attribute) fv.elementAt(0), Double
							.valueOf(cursor.getString(cursor
									.getColumnIndex(xColumn))));
					iExample.setValue((Attribute) fv.elementAt(1), Double
							.valueOf(cursor.getString(cursor
									.getColumnIndex(yColumn))));
					iExample.setValue((Attribute) fv.elementAt(2), Double
							.valueOf(cursor.getString(cursor
									.getColumnIndex(zColumn))));

					if (cursor.getString(cursor.getColumnIndex(labelColumn))
							.contains("owner"))
						iExample.setValue((Attribute) fv.elementAt(3), label);
					else
						iExample.setValue((Attribute) fv.elementAt(3), "other");
				}

				// vc record all database let SVM.class to analyze
				// vc.add(cursor);
				j48.addInstanceToTrainingData(iExample);
				cursor.moveToNext();
			}
			cursor.close();
		}
	}
}
