package com.kpz.pomodorotasks.activity;

import android.content.Context;
import android.os.Handler;
import android.os.PowerManager;

public class AlarmAlertWakeLock {

    private static PowerManager.WakeLock wakeLock;
	private static Handler defaultQueueHandler = new Handler();
	private static Runnable releaseCpuLockRunnable = new Runnable() {
		
		public void run() {

	        if (wakeLock != null) {
	        	
	            wakeLock.release();
	            wakeLock = null;
	        }
		}
	};

    static void acquire(Context context) {

        if (wakeLock != null) {
            return;
        }

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
        								| PowerManager.ACQUIRE_CAUSES_WAKEUP 
        								, "PomodoroTasks");
        wakeLock.acquire();
    }

    static void release() {
    	defaultQueueHandler.postDelayed(releaseCpuLockRunnable, 5000);
    }
}
