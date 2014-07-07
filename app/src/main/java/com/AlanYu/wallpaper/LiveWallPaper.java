package com.AlanYu.wallpaper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.PowerManager;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.widget.Toast;

import com.AlanYu.Filter.DecisionMaker;
import com.AlanYu.Filter.J48Classifier;
import com.AlanYu.database.DBHelper;
import com.AlanYu.database.TouchDataNode;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;

@SuppressLint("ShowToast")
public class LiveWallPaper extends WallpaperService {

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
    public static final int PERIOD = 13000;
    public static final double SPLIT_PERCENTAGE = 0.4;
    public static final int NUMBER_OF_TRAINGING_INSTANCES = 1000;
    public static final int TOUCH = 0;
    public static final int SLIDE = 1;

    /* Parameters from Control Activity */
    private static float THRESHOLD;
    private static String nowLabel;
    protected Vector<TouchDataNode> vc;
    KeyguardManager keyguardManager;
    KeyguardLock keylock;
    PowerManager powerManager;
    DevicePolicyManager mDPM;
    ComponentName mDeviceAdminSample;
    Vector<Double> timeStamp;
    private boolean is_experiment = false;
    private int mode = DecisionMaker.TEST;
    private int pid = 0;
    private String deleteProcessName = null;
    private Instances trainingData;
    private Instances testData;
    private Instances accessData;
    Vector<Vector> testDataNodes= new Vector<Vector>();
    private J48Classifier j48;
    private DecisionMaker decisionMaker;
    private String[] PROTECTED_LIST = {"vending", "gm", "mms", "contact",
            "gallery"};

    public static double getThreshold() {
        return THRESHOLD;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        init();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        init();
        // TODO build control policy version
        super.onCreate();
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

    /**
     * geting the information of running apps
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
        args.put(LABEL, nowLabel);
        long rowid = writeSource.insert(TOUCH_TABLE_NAME, null, args);

        Log.d("writeDatabase Event",
                "id =" + rowid + " x:" + event.getX() + " y:" + event.getY()
                        + " Pressure" + event.getPressure() + " Size:"
                        + event.getSize() + " TimeStamp:"
                        + event.getEventTime() + "label:" + nowLabel
        );
        writeSource.close();
    }

    private void readDatabase() {
        DBHelper db = new DBHelper(this);
        SQLiteDatabase readSource = db.getReadableDatabase();
        Cursor cursor = null;
        if (readSource != null) {
            cursor = readSource.query(TOUCH_TABLE_NAME, new String[]{ID,
                            X, Y, PRESSURE, LABEL, SIZE, TIMESTAMP, ACTION_TYPE}, null,
                    null, null, null, null
            );
        }
        Log.d("readDatabase", "reading database");
        FastVector fv = decisionMaker.getWekaAttributes();
        try {
            if (cursor.moveToFirst()) {
                do {
                    String tmplabel = cursor.getString(cursor.getColumnIndex(LABEL));
                    if (tmplabel.contains("domo") || tmplabel.contains("Jorge") || tmplabel.contains("CY")) {
                        //skip  cuz their data has problem
                    } else {
                        Instance iExample = new DenseInstance(decisionMaker.getWekaAttributes().size());

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
                        timeStamp.add(Double.valueOf(cursor.getString(cursor.getColumnIndex(TIMESTAMP))));
                        if (tmplabel.contains("owner"))
                            iExample.setValue((Attribute) fv.elementAt(4),
                                    cursor.getString(cursor
                                            .getColumnIndex(LABEL))
                            );
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

    /**
     * Preprocess data to per access data in one Vector with touchNode data type
     *
     * @param cursor
     */
    private void preprocessData(Cursor cursor) {
        boolean isTraining = true;
        int trainingNo = 0;
        if (cursor.moveToFirst()) {
            Vector perAccess = new Vector();
            String previousLabel = cursor.getString(cursor.getColumnIndex(LABEL));
            String label = null;
            double nowTimeStamp = 0;
            double previousTimeStamp = Double.valueOf(cursor.getString(cursor.getColumnIndex(TIMESTAMP)));
            while (cursor.moveToNext()) {

                label = cursor.getString(cursor.getColumnIndex(LABEL));
                nowTimeStamp = Double.valueOf(cursor.getString(cursor.getColumnIndex(TIMESTAMP)));

                // in same label and is in the same access
                if (label.contains(previousLabel)) {
                    System.out.println("in same label durantion :" + (nowTimeStamp - previousTimeStamp));
                    if (nowTimeStamp - previousTimeStamp < PERIOD) {
                        //the same label and same access add to access data
                        if (!label.contains("owner"))
                            label = "other";

                        TouchDataNode perTouch = new TouchDataNode();
                        perTouch.setX(cursor.getString(cursor.getColumnIndex(X)));
                        perTouch.setY(cursor.getString(cursor.getColumnIndex(Y)));
                        perTouch.setActionType(cursor.getString(cursor.getColumnIndex(ACTION_TYPE)));
                        perTouch.setLabel(label);
                        perTouch.setPressure(cursor.getString(cursor.getColumnIndex(PRESSURE)));
                        perTouch.setSize(cursor.getString(cursor.getColumnIndex(SIZE)));
                        perTouch.setTimestamp(cursor.getString(cursor.getColumnIndex(TIMESTAMP)));
                        perAccess.add(perTouch);
                        trainingNo++;
                        System.out.println("in same access");
                        previousTimeStamp = nowTimeStamp;
                    } else {
                        System.out.println("finish detecting one aceess");
                        //the same label but different access ,  throw access data to preprocess and add this touch to next access
                        previousLabel = cursor.getString(cursor.getColumnIndex(LABEL));
                        previousTimeStamp = Double.valueOf(cursor.getString(cursor.getColumnIndex(TIMESTAMP)));
                        cursor.moveToPrevious();
                        if(trainingNo < NUMBER_OF_TRAINGING_INSTANCES)
                            this.slingAndTouch(perAccess , isTraining);
                        else
                            testDataNodes.add(perAccess);
                        perAccess = new Vector();
                    }
                } else {
                    //different label throw access data to preprocess and add this touch to next access
                    System.out.println("finish detecting one aceess");
                    previousLabel = cursor.getString(cursor.getColumnIndex(LABEL));
                    previousTimeStamp = Double.valueOf(cursor.getString(cursor.getColumnIndex(TIMESTAMP)));
                    cursor.moveToPrevious();
                    if(trainingNo < NUMBER_OF_TRAINGING_INSTANCES)
                    this.slingAndTouch(perAccess,isTraining);
                    else
                    testDataNodes.add(perAccess);
                    perAccess = new Vector();
                }
            }
        }
    }

    /**
     * Process Per access data to get average distance of X , Y and velocity and input to training data
     *
     * @param perAccess  : consist of the touch event data in vector
     */
    private void slingAndTouch(Vector perAccess,boolean isTraining) {
        Iterator it = perAccess.iterator();
        int start_x = 0, start_y = 0, end_x = 0, end_y = 0;
        double startTime = 0, endTime = 0, averageSize = 0, averagePressure = 0;
        int counter = 0;
        boolean keepDetecting = false;
        boolean finishGesture = false;
        FastVector fv = decisionMaker.getWekaAttributes();
//        System.out.println("in procees sling and touch ");
        Instance iExample = new DenseInstance(decisionMaker.getWekaAttributes().size());
        while (it.hasNext()) {
            TouchDataNode touchData = (TouchDataNode) it.next();
            switch (touchData.getActionType()) {
                case ACTION_DOWN:
                    start_x = touchData.getX();
                    start_y = touchData.getY();
                    startTime = touchData.getTimestamp();
                    averagePressure = touchData.getPressure();
                    averageSize = touchData.getSize();
                    counter++;
                    break;
                // start per touch
                case ACTION_MOVE:
                    keepDetecting = true;
                    counter++;
                    break;
                // keep detect per touch
                case ACTION_UP:
                    end_x = touchData.getX();
                    end_y = touchData.getY();
                    endTime = touchData.getTimestamp();
                    averagePressure += touchData.getPressure();
                    averageSize += touchData.getSize();
                    finishGesture = true;
                    counter++;
                    break;
                default:
                    System.out.println("Meets Error in detecting action type :" + touchData.getActionType());
                    break;
            }
            if (finishGesture) {
                int distanceX = end_x - start_x;
                int distanceY = end_y - start_y;
                double duration = endTime - startTime;
                double velocity = distanceX / duration;
                averagePressure = averagePressure / counter;
                averageSize = averageSize / counter;
                iExample.setValue((Attribute) fv.elementAt(0), distanceX);
                iExample.setValue((Attribute) fv.elementAt(1), distanceY);
                iExample.setValue((Attribute) fv.elementAt(2), averagePressure);
                iExample.setValue((Attribute) fv.elementAt(3), averageSize);
                iExample.setValue((Attribute) fv.elementAt(4), duration);
                iExample.setValue((Attribute) fv.elementAt(5), velocity);
                if (keepDetecting) {
                    int touchType = SLIDE;
                    iExample.setValue((Attribute) fv.elementAt(6), touchType);
                } else {
                    int touchType = TOUCH;
                    iExample.setValue((Attribute) fv.elementAt(6), touchType);
                }
                iExample.setValue((Attribute) fv.elementAt(7), (String) touchData.getLabel());
//                System.out.println("x :" + distanceX + " Y : " + distanceY + "averagePressure : " + averagePressure + " average size " + averageSize + "touch type :" + keepDetecting + "label :" + touchData.getLabel());
                if(isTraining)
                trainingData.add(iExample);
                else
                testData.add(iExample);
                finishGesture = false;
                keepDetecting = false;
                counter = 0;
            }

        }
    }

    private void init() {
        // Build System Manager
        timeStamp = new Vector();
        keyguardManager = (KeyguardManager) getSystemService(Activity.KEYGUARD_SERVICE);
        keylock = keyguardManager.newKeyguardLock(Activity.KEYGUARD_SERVICE);
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        mDeviceAdminSample = new ComponentName(LiveWallPaper.this,
                deviceAdminReceiver.class);

        // TODO put the build model in asynTask
        decisionMaker = new DecisionMaker();
        //for split the data to per-access data
        accessData = new Instances("accessData", decisionMaker.getWekaAttributes(), 1000);
        testData = new Instances("TestData", decisionMaker.getWekaAttributes(),
                1000);
        trainingData = new Instances("TrainingData",
                decisionMaker.getWekaAttributes(), 1000);
        accessData.setClassIndex(accessData.numAttributes() - 1);
        trainingData.setClassIndex(trainingData.numAttributes() - 1);
        testData.setClassIndex(testData.numAttributes() - 1);
        getSharedPreferenceSetting();
        if (is_experiment) {
            readDatabase();
            percentageSplit();
            decisionMaker.addDataToTraining(trainingData);
            decisionMaker.buildClassifier();

        } else {
            // training mode collect all event data to database
            if (mode == DecisionMaker.TRAINING) {
            }
            // test every touch event
            else {
                readDatabase();
                decisionMaker.addDataToTraining(trainingData);
                decisionMaker.buildClassifier();
            }
        }

    }

    private void getSharedPreferenceSetting() {
        SharedPreferences setting = getSharedPreferences("Preference", 0);
        nowLabel = setting.getString("name", OTHER_LABEL);
        THRESHOLD = setting.getFloat("Threshold", (float) 0.5);
        decisionMaker.setThreshold(getThreshold());
        mode = setting.getInt("Mode", DecisionMaker.TEST);
        Log.d("sharePreference ", "is_experiment :" + is_experiment);
        is_experiment = setting.getBoolean("Experiment", false);

        Log.d("Get preference setting  ", "mode:" + mode + " is_experiment: " + is_experiment + " threshold : " + THRESHOLD + " now label :" + nowLabel);
        // TODO get protected list from apps
        // TODO the is_experiment can't get real data
    }

    private void percentageSplit() {
        // split from data from the database by the number of training instance
//        Instances inst = trainingData;
//        Random ran = new Random(System.currentTimeMillis());
//        int testSize = trainingData.numInstances() - NUMBER_OF_TRAINGING_INSTANCES;
//        trainingData = new Instances(inst, 0, NUMBER_OF_TRAINGING_INSTANCES);
//        testData = new Instances(inst, NUMBER_OF_TRAINGING_INSTANCES + 1, testSize - 1);
//        Log.d("split", "testData size " + testData.numInstances()
//                + "traindata size " + trainingData.numInstances());

        Instances inst = trainingData;
//        int trainSize = (int) Math.round(trainingData.numInstances() * SPLIT_PERCENTAGE);
        int trainSize = NUMBER_OF_TRAINGING_INSTANCES ;
        int testSize = trainingData.numInstances() - trainSize;
//        System.out.println("training data number of instance "+ trainingData.numInstances());
        trainingData = new Instances(inst, 0, trainSize);
        testData = new Instances(inst, trainSize, testSize);
        Log.d("split", "testData size " + testData.numInstances()
                + "traindata size " + trainingData.numInstances());

    }

    /**
     * function : to set instances from touch event
     *
     * @param event
     * @param instances
     */
    private void setInstancesFromEvent(MotionEvent event, Instances instances) {
        Instance iExample = new
                DenseInstance(DecisionMaker.ATTRIBUTE_SIZE);
        FastVector fv = decisionMaker.getWekaAttributes();
        iExample.setValue((Attribute) fv.elementAt(0), (double) event.getX());
        iExample.setValue((Attribute) fv.elementAt(1), (double) event.getY());
        iExample.setValue((Attribute) fv.elementAt(2), (double) event.getPressure());
        iExample.setValue((Attribute) fv.elementAt(3), (double) event.getSize());
        instances.add(iExample);
    }

    public class TouchEngine extends Engine {

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            setTouchEventsEnabled(true);
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            if (event.getAction() == ACTION_DOWN) {

                if (is_experiment) {
                    try {
                        Thread myThread = new CaculateThread();
                        myThread.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            if (mode == DecisionMaker.TRAINING)
                writeDataBase(event);
            else {
                setInstancesFromEvent(event, testData);
            }
            super.onTouchEvent(event);
        }

        @SuppressLint("NewApi")
        @Override
        public void onVisibilityChanged(boolean visible) {
            Intent intent = new Intent(LiveWallPaper.this,
                    monitorAppService.class);

            if (visible) {
                getSharedPreferenceSetting();
                stopService(intent);
                keylock.disableKeyguard();
                Log.d("visible", "true now user : " + nowLabel);
            } else {
                /*
                 * ============================================================ if
				 * wallpaperService is forebackground then kill monitorAppsService else
				 * start monitorAppsService
				 * ============================================================
				 */
                if (isInProtectList()) {
                    if (mode == DecisionMaker.TEST) {
                        decisionMaker.setThreshold(getThreshold());
                        Log.d("invisible",
                                "executed process is in the protect list ");
                        if (DecisionMaker.IS_OTHER == decisionMaker
                                .getFinalLabel(testData)) {
                            startService(intent);
                            Log.d("Decision making ", "You are other");
                            Toast.makeText(LiveWallPaper.this, " You are not the owner you mother fucker", Toast.LENGTH_LONG).show();
                            //TODO  1. lock screen 2.
                        } else
                            Log.d("Decision making ", "You are owner");
                        // TODO   1. add 1 success access to database for the future to deciside when time to retraining classifier
                    } else if (mode == DecisionMaker.TRAINING) {
                        startService(intent);
                        Log.d("LiveWall paper", "mode is training and start intent");
                    }
                    // TODO to record the recently precision and if precision decrease
                    // always drop below
                    // 0.5 remember to add some retraining policy
                    // TODO below is lock screen policy
//                    if ((DecisionMaker.IS_OTHER == decisionMaker.getFinalLabel(testData))) {
//                        keylock.reenableKeyguard();
//                        mDPM.lockNow();
//
//                        /* below code is manage the screen */
////                        if (keyguardManager.) {
////                            Log.d("lock screen",
////                                    "You are not the owner but u just unlock screen");
////                            keylock.disableKeyguard();
////                        } else {
////                            Log.d("lock screen", "You are not the user");
////                            keylock.reenableKeyguard();
////                            mDPM.lockNow();
////                        }
//                        // You are Owner
//                    } else {
//                        keylock.disableKeyguard();
//                        Log.d("invisible",
//                                "it's owner and apps is also  in protected list ");
//                    }
                }
            }
            super.onVisibilityChanged(visible);
        }
    }

    public class CaculateThread extends Thread {

        private void evaluationPerClassifierEveryInstances() {
            try {
                decisionMaker.evaluationEachClassifier(testData);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * extract per access data from testData and throw the accessData to decisionMaker
         */
        public void evaluatoinPerAccess() {
            super.run();
            for (int j = 1; j < 10; j++) {
                decisionMaker.setThreshold((double)j/10);
                int[] result = new int[4];
                try {
                    int startIndex = trainingData.numInstances();
                    accessData.add(testData.get(0));
                    for (int i = 1; i < testData.numInstances(); i++, startIndex++) {
//                    Log.d("decide per touch"," in one access i:"+i+ "label = "+testData.instance(i).classValue());
                        if (testData.instance(i).classValue() == testData.instance(i - 1).classValue()) {
                            if (timeStamp.elementAt(startIndex) - timeStamp.elementAt(startIndex - 1) < PERIOD) {
                                accessData.add(testData.get(i));
                            } else {
                                int trueLabel = (int) testData.instance(i - 1).classValue();
                                if (accessData.numInstances() >= 2) {
//                                    Log.d("one access", "now i = " + i + "Number of accessData instances :" + String.valueOf(accessData.numInstances()));
                                    int label = decisionMaker.getFinalLabel(accessData);
                                    result = decisionMaker.evaluation(trueLabel, label, result);
                                } else {
//                                    System.out.println("access data smaller than 2 ");
                                    ;
                                }
                                accessData.clear();
                                accessData.add(testData.get(i));
                            }
                        } else {
                            int trueLabel = (int) testData.instance(i - 1).classValue();
                            if (accessData.numInstances() >= 2) {
//                                Log.d("one access", "now i = " + i + "Number of accessData instances :" + String.valueOf(accessData.numInstances()));
                                int label = decisionMaker.getFinalLabel(accessData);
                                result = decisionMaker.evaluation(trueLabel, label, result);
                            } else {
//                                System.out.println("access data smaller than 2 ");
                                ;
                            }
                            accessData.clear();
                            accessData.add(testData.get(i));
                        }
                    }



//                    System.out.println(" Total test instances number is : " + testData.numInstances() + "Training instances number is " + trainingData.numInstances());
//                    int owner = 0;
//                    for (int i = 0; i < trainingData.numInstances(); i++) {
//                        if (trainingData.instance(i).classValue() == DecisionMaker.IS_OWNER) {
//                            owner++;
//                        }
//                    }
//                    System.out.println("Training data owner instances is : " + owner);
                    decisionMaker.printStatistics(result);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        public void evalution(Instances perAccess) {
//            decisionMaker.evaluationWithMajorityVoting(perAccess);
            try {
                decisionMaker.evaluationEachClassifier(perAccess);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void evaluationProcessDataPerAccess(Vector data)
        {

            for (int i = 1; i <10 ; i++) {
                decisionMaker.setThreshold((double)i / 10);
                boolean isTraing = false;
                int predictLabel = 0;
                int trueLabel = 0;
                int[] result = new int[4];
                Iterator it = data.iterator();
                while (it.hasNext()) {
                    Vector perAccess = (Vector) it.next();
                    TouchDataNode touchDataNode = (TouchDataNode) perAccess.get(0);
                    if (touchDataNode.getLabel().contains("owner"))
                        trueLabel = DecisionMaker.IS_OWNER;
                    else
                        trueLabel = DecisionMaker.IS_OTHER;

                    slingAndTouch(perAccess, isTraing);
                    predictLabel = decisionMaker.getFinalLabel(testData);
                    result = decisionMaker.evaluation(trueLabel, predictLabel, result);
                    testData.clear();
                }
                decisionMaker.printStatistics(result);
            }
        }
        @Override
        public void run() {
            evaluatoinPerAccess();
//            evaluationPerClassifierEveryInstances();
//            evalution(testData);
//            evaluationProcessDataPerAccess(testDataNodes);

        }
    }

}
