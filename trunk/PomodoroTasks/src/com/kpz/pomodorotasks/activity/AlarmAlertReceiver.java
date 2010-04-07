package com.kpz.pomodorotasks.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmAlertReceiver extends BroadcastReceiver {

	public void onReceive(Context context, Intent intent) {

		AlarmAlertWakeLock.acquire(context);
		
		Intent activityIntent = new Intent(context, TaskBrowserActivity.class);
		activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK 
								| Intent.FLAG_ACTIVITY_SINGLE_TOP);
		context.startActivity(activityIntent);
	}
}
