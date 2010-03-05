package com.kpz.pomodorotasks.activity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;

public class NotifyingService extends Service {
    private static final int NOTIFICATION_ID = R.layout.task_list;
	private NotificationManager notificationManager;

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

    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
    	
        notificationManager.cancel(NOTIFICATION_ID);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();
	private String mTaskDescription;

    public void showNotification(String note) {
    	showNotification(note, false);
    }

	private void showNotification(String note, boolean beep) {

        Notification notification = new Notification(R.drawable.liltomato, null,
                System.currentTimeMillis());
        notification.flags = Notification.FLAG_ONGOING_EVENT;
        
        if(beep){
        	String packageName = getApplication().getApplicationInfo().packageName;
			notification.sound = Uri.parse("android.resource://"+ packageName + "/" + R.raw.freesoundprojectdotorg_32568__erh__indian_brass_pestle);
        	notification.vibrate = new long[] {0,100,200,300};
            notification.defaults |= Notification.DEFAULT_LIGHTS;
        }
        
        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, TaskBrowserActivity.class), 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.app_name),
                       note, contentIntent);

        // Send the notification.
        // We use a layout id because it is a unique number.  We use it later to cancel.
        notificationManager.notify(NOTIFICATION_ID, notification);
		
	}
	
	public void notifyTimeEnded() {
		showNotification("Time's up. Task - " +  mTaskDescription, true);
	}

	public void notifyTimeStarted(String taskDescription) {

		mTaskDescription = taskDescription;
		showNotification("Clock is ticking. Task - " +  taskDescription, false);
	}

	public void clearTaskNotification() {
		showNotification("");
	}
}

