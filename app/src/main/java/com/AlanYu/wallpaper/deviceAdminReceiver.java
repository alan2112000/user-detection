package com.AlanYu.wallpaper;


import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

public class deviceAdminReceiver extends DeviceAdminReceiver {

    /**
     * get the storing info of the device 
     * 
     * @param context
     * @return
     */
    public static SharedPreferences getDevicePreference(Context context) {
        return context.getSharedPreferences(
                DeviceAdminReceiver.class.getName(), 0);
    }

    public static String PREF_PASSWORD_QUALITY = "password_quality";
    public static String PREF_PASSWORD_LENGTH = "password_length";
    public static String PREF_MAX_FAILED_PW = "max_failed_pw";

    void showToast(Context context, CharSequence text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEnabled(Context context, Intent intent) {
        showToast(context, "Device powerManager :enable");
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        showToast(context, "Device Manager ：disable");
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return "这是一个可选的消息，警告有关禁止用户的请求";
    }

    @Override
    public void onPasswordChanged(Context context, Intent intent) {
        showToast(context, "Device Manager：password changed");
    }

    @Override
    public void onPasswordFailed(Context context, Intent intent) {
        showToast(context, "Device Manager：changed password failed");
    }

    @Override
    public void onPasswordSucceeded(Context context, Intent intent) {
        showToast(context, "Device Manager：changed password success");
    }

}