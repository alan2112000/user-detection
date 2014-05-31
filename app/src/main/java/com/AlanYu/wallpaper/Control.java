package com.AlanYu.wallpaper;

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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.AlanYu.Filter.DecisionMaker;

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
    RadioButton trainingButton;
    RadioButton experimentButton;
    DevicePolicyManager mDPM;
    ActivityManager mAM;
    ComponentName mDeviceComponentName;




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
    RadioGroup radioGroup;
    private Intent intent;


    /*
     * enable application to monitor the whole data
     */
    private OnClickListener start = new OnClickListener() {


        @Override
        public void onClick(View v) {
            intent = new Intent(
                    WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);


            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    new ComponentName(com.AlanYu.wallpaper.Control.this,
                            LiveWallPaper.class)
            );
            setPreferences();
            startActivity(intent);
        }
    };
    private OnClickListener stop = new OnClickListener() {
        @Override
        public void onClick(View v) {
            stopService(intent);
        }
    };

    private RadioGroup.OnCheckedChangeListener mChangeRadio = new RadioGroup.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(RadioGroup radioGroup, int checkId) {
            if (checkId == R.id.trainingButton) {
                    trainingButton.setChecked(true);
                    experimentButton.setChecked(false);
                    Toast.makeText(Control.this, "traingin Button selected", Toast.LENGTH_SHORT).show();
            } else if (checkId == R.id.experimentButton){
                    experimentButton.setChecked(true);
                    trainingButton.setChecked(false);
                    Toast.makeText(Control.this, "experiment buttuon selected ", Toast.LENGTH_SHORT).show();
                }
            else {
                normalButton.setChecked(true);
                Toast.makeText(Control.this,"System will prompt lock if it think you are not the owner",Toast.LENGTH_LONG).show();
            }

        }
    };


    private SharedPreferences settings;
    private RadioButton normalButton;


    public Control() {
    }


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

    private void setPreferences() {


        settings = getSharedPreferences("Preference", 0);
        Editor editor = settings.edit();
        editor.clear();
        if (trainingButton.isChecked()) {
            editor.putInt("Mode", DecisionMaker.TRAINING);

        } else {
            editor.putInt("Mode", DecisionMaker.TEST);

        }
        if (experimentButton.isChecked()) {
            Log.d("in control ", "experiment is set true");
            editor.putBoolean("Experiment", true);
        }

        editor.putFloat("Threshold", Float.valueOf(threshold.getText().toString()));
        editor.putString("name", userType.getText().toString());
        editor.commit();
    }

    private void findView() {
        startButton = (Button) findViewById(R.id.start);
        stopButton = (Button) findViewById(R.id.stop);
        lockButton = (Button) findViewById(R.id.lock);
        userType = (EditText) findViewById(R.id.userType);
        authorizeButton = (Button) findViewById(R.id.authorize);
        disableDeviceManager = (Button) findViewById(R.id.stopAuthorize);
        threshold = (EditText) findViewById(R.id.threshold);
        trainingButton = (RadioButton) findViewById(R.id.trainingButton);
        experimentButton = (RadioButton) findViewById(R.id.experimentButton);
        radioGroup = (RadioGroup) findViewById(R.id.radiogroup);
        normalButton = (RadioButton) findViewById(R.id.normalModeButton);
    }

    private void setListener() {
        startButton.setOnClickListener(start);
        stopButton.setOnClickListener(stop);
        authorizeButton.setOnClickListener(authorize);
        disableDeviceManager.setOnClickListener(disableDevice);
        lockButton.setOnClickListener(forceLock);
        radioGroup.setOnCheckedChangeListener(mChangeRadio);
    }

}
