package com.kpz.pomodorotasks.activity;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.RingtonePreference;
import android.preference.Preference.OnPreferenceChangeListener;

import com.kpz.pomodorotasks.map.TaskDatabaseMap;
import com.kpz.pomodorotasks.map.TaskDatabaseMap.ConfigType;

public class PreferencesActivity extends PreferenceActivity{

	private TaskDatabaseMap taskDatabaseMap;
	private RingtonePreference ringTonePreference;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        taskDatabaseMap = TaskDatabaseMap.getInstance(this);
		
		addPreferencesFromResource(R.xml.preferences);
		
		initDurationPreference(ConfigType.TASK_DURATION);
		initDurationPreference(ConfigType.BREAK_DURATION);
		initDurationPreference(ConfigType.EVERY_FOURTH_BREAK_DURATION);
		
		ringTonePreference = (RingtonePreference) findPreference(ConfigType.NOTIFICATION_RINGTONE.name());
		ringTonePreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			public boolean onPreferenceChange(Preference preference, Object newValue) {

				updateRingtonePreferenceSummary((String)newValue);
				taskDatabaseMap.getPreferences().updateRingtone((String)newValue);
				return false;
			}
		});
		
		String selectedRingtone = taskDatabaseMap.getPreferences().getRingtone();
		if (selectedRingtone != null){
			updateRingtonePreferenceSummary(selectedRingtone);
		}
	}

	private void initDurationPreference(ConfigType configType) {
		Preference durationPreference = findPreference(configType.name());
		int duration = taskDatabaseMap.getPreferences().getDurationPreference(configType);
		durationPreference.setSummary(duration + " min");
	}
	
	private void updateRingtonePreferenceSummary(String ringtoneUrl) {

		if(ringtoneUrl != null && ringtoneUrl.trim().equals("")){
			ringTonePreference.setSummary("Silent");
			return;
		}
		
		Uri ringtoneUri = Uri.parse(ringtoneUrl);
		Ringtone ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
		ringTonePreference.setSummary(ringtone.getTitle(this));
	}
}
