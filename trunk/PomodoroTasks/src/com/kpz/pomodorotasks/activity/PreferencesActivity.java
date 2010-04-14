package com.kpz.pomodorotasks.activity;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import com.kpz.pomodorotasks.map.TaskDatabaseMap;
import com.kpz.pomodorotasks.map.TaskDatabaseMap.ConfigType;

public class PreferencesActivity extends PreferenceActivity{

	private TaskDatabaseMap taskDatabaseMap;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        taskDatabaseMap = new TaskDatabaseMap(this);
    	taskDatabaseMap.open();
		
		addPreferencesFromResource(R.xml.preferences);
		
		initDurationPreference(ConfigType.TASK_DURATION);
		initDurationPreference(ConfigType.BREAK_DURATION);
		initDurationPreference(ConfigType.EVERY_FOURTH_BREAK_DURATION);
		
//		CheckBoxPreference useCustomPhoneSettingsPreference = (CheckBoxPreference)findPreference(ConfigType.USE_CUSTOM_PHONE_SETTINGS.name());
//		useCustomPhoneSettingsPreference.setChecked(taskDatabaseMap.getPreferences().fetchUseCustomPhoneSettings());
		//useCustomPhoneSettingsPreference.setKey(key)

//		CheckBoxPreference useCustomPhoneSettingsPreference = (CheckBoxPreference)findPreference(ConfigType.PHONE_VIBRATE_FLAG.name());
//		useCustomPhoneSettingsPreference.setChecked(taskDatabaseMap.getPreferences().fetchUseCustomPhoneSettings());
//		useCustomPhoneSettingsPreference.setKey(key)

	}


	private void initDurationPreference(ConfigType configType) {
		Preference durationPreference = findPreference(configType.name());
		int duration = taskDatabaseMap.getPreferences().getDurationPreference(configType);
		durationPreference.setSummary(duration + " min");
	}
	
	@Override
	protected void onDestroy() {
		
		taskDatabaseMap.close();
		super.onDestroy();
	}
	
}
