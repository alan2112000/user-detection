package com.AlanYu.wallpaper;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;

public class MyPreferencesActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.mylistpreference);
		Preference circlePreference = getPreferenceScreen().findPreference(
				"numberOfCircles");
		// add the validator
		circlePreference.setOnPreferenceChangeListener(numberCheckListener);
	}

	public MyPreferencesActivity() {
		// TODO Auto-generated constructor stub
	}

	Preference.OnPreferenceChangeListener numberCheckListener = new OnPreferenceChangeListener() {

		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			// check that the string is an integer
			if (newValue != null && newValue.toString().length() > 0
					&& newValue.toString().matches("\\d*")) {
				return true;
			}
			// If now create a message to the user
			Toast.makeText(MyPreferencesActivity.this, "Invalid Input",
					Toast.LENGTH_SHORT).show();
			return false;
		}
	};
}
