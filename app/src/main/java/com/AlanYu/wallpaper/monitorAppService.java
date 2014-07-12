package com.AlanYu.wallpaper;

import android.app.ActivityManager;
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
import android.widget.Toast;

import com.AlanYu.Filter.DecisionMaker;
import com.AlanYu.Filter.J48ClassiferForAC;
import com.AlanYu.database.DBHelper;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class monitorAppService extends IntentService implements
        SensorEventListener {

    private String errorTag = "Exception";
    private String sensorTag = "Sensor data";
    private SensorManager sensorManager;
    private Vector<Cursor> vc;
    private String userType;
    private String appType;
    private int mode;
    private int ownerLabelNumber = 0;
    private int otherLabelNumber = 0;
    private float confidence = 0;

    //todo: this parameter should be set from the contrl activity
    private float weight = (float) 0.5;
    private int number_of_data = 0;
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
        this.setConfidence(settings.getFloat("CONFIDENCE", (float) 0.5));
        mode = settings.getInt("Mode", DecisionMaker.IS_OWNER);
        // catach the the acceleter data app name is appType and
        //record data
        if (mode == DecisionMaker.TRAINING)
            ;
        else {
            try {
                readDatabase(appType);
                j48.trainingData();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        Log.d("on startCommand", "finish readDtabase");
    }
    if (intent != null) {
        Log.d("monitorAppService", "onStartCommand");
    } else
            Log.d("monitorAppService", "intent is null");
    return START_STICKY;
}

    /**
     * get now runninig apps information
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

    /**
     * find the pid of the Process for purpose to kill process
     *
     * @param ps ps is the apps short name
     * @return pid of the process
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

    /**
     * pass the pid value to delete the process
     *
     * @param ps
     * @return
     */
    private boolean deleteProcessByName(String ps) {
        int pid = findMatchProcessByName(ps);
        if (pid != 0) {
            android.os.Process.killProcess(pid);
            return true;
        } else
            return false;
    }

    /**
     *
     */
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

    /**
     * detect the suer action here
     *
     * @param event
     */
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
        if (mode == DecisionMaker.TRAINING)
            writeDataBase(event);
        else {
            this.setNumber_of_data(this.getNumber_of_data() + 1);
            FastVector fv = j48.getFvWekaAttributes();
            Instance iExample = new DenseInstance(4);
            iExample.setValue((Attribute) fv.elementAt(0), values[0]);
            iExample.setValue((Attribute) fv.elementAt(1), values[1]);
            iExample.setValue((Attribute) fv.elementAt(2), values[2]);

            Instances dataUnLabeled = new Instances("TestInstances", fv, 10);
            dataUnLabeled.add(iExample);
            dataUnLabeled.setClassIndex(dataUnLabeled.numAttributes() - 1);
            double[] prediction;
            try {
                prediction = j48.returnClassifier().distributionForInstance(
                        dataUnLabeled.firstInstance());
                // output predictions
//                for (int i = 0; i < prediction.length; i++) {
//                    System.out.println("Probability of class "
//                            + dataUnLabeled.classAttribute().value(i) + " : "
//                            + Double.toString(prediction[i]));
//                }

                if (prediction[DecisionMaker.IS_OWNER] > prediction[DecisionMaker.IS_OTHER])
                    ownerLabelNumber++;
                else
                    otherLabelNumber++;
                // do a decision by 5 seconds
                if (this.getNumber_of_data() % 25 == 0) {
                    Thread myThread = new CaculateThread();
                    myThread.run();
                }
//                Log.d("DecisionMaker", "Now Confidence is :" + Double.toString(precision));
            } catch (Exception e) {
                e.printStackTrace();
            }
            dataUnLabeled.clear();
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
        Log.d("writeDatabase Event", "id =" + rowid + "x : " + String.valueOf(values[0]) + "y : " + String.valueOf(values[1]) + " z: " + String.valueOf(values[2]) + "app :" + appType + "user :" + userType);
        writeSource.close();
    }

    /**
     * read training data from database
     *
     * @param appName
     * @throws SQLException
     */
    private void readDatabase(String appName) throws SQLException {
        DBHelper db = new DBHelper(this);
        String selectionArgs[] = new String[1];
        selectionArgs[0] = appName ;
        SQLiteDatabase readSource = db.getReadableDatabase();
        Cursor cursor = readSource.query(tableName, new String[]{xColumn,
                        yColumn, zColumn, labelColumn, appColumn},"APP=?", selectionArgs, null,
                null, null
        );
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
                    iExample.setValue((Attribute) fv.elementAt(0),x);
                    iExample.setValue((Attribute) fv.elementAt(1),y);
                    iExample.setValue((Attribute) fv.elementAt(2),z);

                    if (cursor.getString(cursor.getColumnIndex(labelColumn))
                            .contains("owner"))
                        iExample.setValue((Attribute) fv.elementAt(3), label);
                    else
                        iExample.setValue((Attribute) fv.elementAt(3), "other");
                }
                j48.addInstanceToTrainingData(iExample);
                cursor.moveToNext();
            }
            cursor.close();
        }
    }

    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    public int getNumber_of_data() {
        return number_of_data;
    }

    public void setNumber_of_data(int number_of_data) {
        this.number_of_data = number_of_data;

    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }
    public class CaculateThread extends Thread{

        private void predictionPolicy() {
            double precision = (double) ownerLabelNumber / (otherLabelNumber + ownerLabelNumber);
            double now_confidence = getWeight() * getConfidence() + (1-getWeight())*precision ;
            if(now_confidence <0.5){
                Toast.makeText(monitorAppService.this, " You are not the owner", Toast.LENGTH_SHORT).show();
                Log.d("monitorService","you are the other");
                onDestroy();
            }
            if(now_confidence >= getConfidence()) {
                Toast.makeText(monitorAppService.this, " You are the owner", Toast.LENGTH_SHORT).show();
                Log.d("monitorService","confirm you are the owner");
                onDestroy();
            }
        }

        @Override
        public void run() {
            super.run();
            predictionPolicy();

        }
    }
}
