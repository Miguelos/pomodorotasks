package com.kpz.pomodorotasks.activity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import com.kpz.pomodorotasks.map.TaskDatabaseMap;

public class NotifyingService extends Service {
	
    private static final String TASK_HEADER = "Task - ";
	private static final int NOTIFICATION_ID = R.layout.task_list;
	private NotificationManager notificationManager;

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();
	private String taskDescription;
	private TaskDatabaseMap taskDatabaseMap;
	private BroadcastReceiver broadcastReceiver;
	private boolean isNotifiedTimeEnded;

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        NotifyingService getService() {
            return NotifyingService.this;
        }
    }
    
    @Override
    public void onCreate() {
        notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        taskDatabaseMap = new TaskDatabaseMap(this);
        
        broadcastReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				
				AlarmAlertWakeLock.acquire(context);
				notifyTimeEnded();
				AlarmAlertWakeLock.release();
			}
		};
        IntentFilter filter = new IntentFilter("com.kpz.pomodorotasks.alert.ALARM_ALERT");
		registerReceiver(broadcastReceiver, filter);
    }
    
// Version 1.5 and below   
//    @Override
//    public void onStart(Intent intent, int startId) {
//    	super.onStart(intent, startId);
//    }

// Version 1.6    
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        // We want this service to continue running until it is explicitly
//        // stopped, so return sticky.
//        return START_STICKY;
//    }

    @Override
    public void onDestroy() {
    	
        notificationManager.cancel(NOTIFICATION_ID);
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

	private void showNotification(String title, String note, boolean isTaskComplete) {

        Notification notification = new Notification(R.drawable.liltomato, null,
                System.currentTimeMillis());
        notification.flags = Notification.FLAG_ONGOING_EVENT;
        
        if(isTaskComplete){
        	
        	notification = new Notification(R.drawable.liltomato_red, null,
                    System.currentTimeMillis());
            notification.flags = Notification.FLAG_ONGOING_EVENT;
        	String ringtone = taskDatabaseMap.getPreferences().getRingtone();
        	if(ringtone == null){
				ringtone = "android.resource://"+ getApplication().getPackageName() + "/" + R.raw.freesoundprojectdotorg_32568__erh__indian_brass_pestle;
			}
        	
        	notification.sound = Uri.parse(ringtone);
        	
			if (taskDatabaseMap.getPreferences().notifyPhoneVibrate()){
				notification.vibrate = new long[] {0,100,200,300};				
			}
            
			notification.tickerText = title;
        	notification.defaults |= Notification.DEFAULT_LIGHTS;
        }
        
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, TaskBrowserActivity.class), 0);
        notification.setLatestEventInfo(this, title, note, contentIntent);
        notificationManager.notify(NOTIFICATION_ID, notification);
	}
	
	public void notifyTimeEnded() {
		
		new Handler().post(new Runnable() {
			
			public void run() {
				
				if(!isNotifiedTimeEnded){
					isNotifiedTimeEnded = true;
					showNotification("Time's up", TASK_HEADER + taskDescription, true);					
				}
			}
		});
	}

	public void notifyTimeStarted(String pTaskDescription) {
		
		isNotifiedTimeEnded = false;
		taskDescription = pTaskDescription;
		showNotification("Clock is ticking...", TASK_HEADER + taskDescription, false);
	}

	public void clearTaskNotification() {
		showNotification(getText(R.string.app_name).toString(), "", false);
	}
}
