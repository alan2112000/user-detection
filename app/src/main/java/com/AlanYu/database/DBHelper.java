package com.AlanYu.database;

import com.AlanYu.wallpaper.LiveWallPaper.TouchEngine;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

@SuppressLint("NewApi")

public class DBHelper extends SQLiteOpenHelper {

	private static final int VERSION = 18; 
	private static final String DATABASE_NAME = "sensorData.db";
	private Context myContext;
	private static final String CREATE_SENSOR_DATA_DLL = "CREATE TABLE SENSOR_MAIN ("+"_ID INTEGER PRIMARY KEY,"
												+ "SENSOR_TYPE INTEGER,"+"X REAL,"+"Y REAL,"+"Z REAL,"+"TIME_STAMP INTEGER,"+"LABEL TEXT,"+"APP TEXT);"; 
	private static final String DELETE_SENSOR_DATA_DLL = "DROP TABLE IF EXISTS SENSOR_MAIN;";
	private static final String CREATE_TOUCH_DATA_DLL = "CREATE TABLE TOUCH ("+"_ID INTEGER PRIMARY KEY,"
												+"X REAL,"+"Y REAL,"+"ACTION TEXT,"+"PRESSURE REAL,"+"LABEL TEXT,"+"SIZE REAL,"+"TIMESTAMP INTEGER);";
	private static final String DELETE_TOUCH_DLL = "DROP TABLE IF EXISTS TOUCH;";
	public DBHelper(Context context) {
		super(context, DATABASE_NAME, null, VERSION);
		myContext = context; 
	}


	@Override
	public void onCreate(SQLiteDatabase db) {
	db.execSQL(CREATE_SENSOR_DATA_DLL);
	db.execSQL(CREATE_TOUCH_DATA_DLL);
	Log.d("Create DB", DATABASE_NAME);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(DELETE_SENSOR_DATA_DLL);
			db.execSQL(CREATE_SENSOR_DATA_DLL);
			db.execSQL(DELETE_TOUCH_DLL);
			db.execSQL(CREATE_TOUCH_DATA_DLL);
			Log.d("Upgrade DB",DATABASE_NAME);
	}
	

}
