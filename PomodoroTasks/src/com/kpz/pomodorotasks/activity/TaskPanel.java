package com.kpz.pomodorotasks.activity;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.kpz.pomodorotasks.map.TaskDatabaseMap;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TaskPanel {
	
	private static final int ONE_SEC_IN_MILLI_SEC = 1000;
	private static final int BREAK_TIME_IN_MIN = 5;
	private static final int EVERY_FOUR_BREAK_TIME_IN_MIN = 15;

	private LinearLayout runTaskPanel;
	private ImageButton taskControlButton;
	private ImageButton hideButton;
	private TextView taskDescription;
	private TextView timeLeft;
	private ProgressBar progressBar;
	private PomodoroTrackPanel pomodoroTrackPanel;
	
	private TaskDatabaseMap taskDatabaseMap;
	private NotifyingService notifyingService;
	private TaskTimer counter;
	private ServiceConnection connection;
	private Activity activity;

	private boolean isUserTask = false;
	private long taskEndTime;
	
	private TASK_RUN_STATE currentTaskRunState = TASK_RUN_STATE.NOT_STARTED;
	private enum TASK_RUN_STATE {
		
		NOT_STARTED, RUNNING, PAUSED
	}
	
	private enum BUTTON_STATE {
		PLAY, STOP
	}

	public TaskPanel(Activity pActivity, PomodoroTrackPanel trackPanel, TaskDatabaseMap pTaskDatabaseMap) {
		runTaskPanel = (LinearLayout)pActivity.findViewById(R.id.runTaskPanel);
		taskControlButton = (ImageButton)pActivity.findViewById(R.id.control_icon);
		hideButton = (ImageButton)pActivity.findViewById(R.id.hide_panel_button);
		hideButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				runTaskPanel.setVisibility(View.GONE);
			}
		});
		
    	taskDescription = (TextView)pActivity.findViewById(R.id.task_description);
    	timeLeft = (TextView)pActivity.findViewById(R.id.time_left);
    	progressBar = (ProgressBar)pActivity.findViewById(R.id.task_progress_bar);
    	taskDatabaseMap = pTaskDatabaseMap;
    	activity = pActivity;
    	pomodoroTrackPanel = trackPanel;

	    alarmService = (AlarmManager)activity.getSystemService(Context.ALARM_SERVICE);
	    activityIntent = new Intent("com.kpz.pomodorotasks.alert.ALARM_ALERT");
	    pendingAlarmIntent = PendingIntent.getBroadcast(activity, 0, activityIntent,0);
	}

	public void startTask(String taskDescription) {

		initPanel();
		showPanel(taskDescription);
		resetTaskRun();
		beginUserTask();
	}
	
	public void hidePanel(){
		
		runTaskPanel.setVisibility(View.GONE);
	}
	
	public void updateTaskDescription(String taskDesc) {
		
		if (isUserTask){
			taskDescription.setText(taskDesc);			
		}
	}
	
    public void refreshTaskPanel() {
    	
    	String text = getCurrentTaskText();
    	if (text == null || text.equals("")){
    		return;
    	}
    	
    	taskDescription.setText("");
    	
    	if (isTaskRunning()){
    		resetTaskRun();
    	}
	}
    
    public String getCurrentTaskText() {
		return taskDescription.getText().toString();
	}

	private void initPanel() {
		
    	taskControlButton.setOnClickListener(new View.OnClickListener() {

    	    public void onClick(View view) {

    	    	if (counter != null && taskControlButton.getTag().equals(BUTTON_STATE.STOP)){
    	    		hideButton.setVisibility(View.VISIBLE);
    	    		resetTaskRun();
    	    		if (!isUserTask){
    	    			hidePanel();
    	    		}
    	    	} else {
    	    		beginUserTask();    	    			
    	    	}
    	    }
    	});
	}

	private void showPanel(String taskDesc) {
		
		runTaskPanel.setVisibility(View.VISIBLE);
		taskDescription.setText(taskDesc);
	}
	
	private void resetTaskRun() {
		currentTaskRunState = TASK_RUN_STATE.NOT_STARTED;
		stopAlarm();
		cancelTaskTimer();
		resetProgressControl();
		if(notifyingService != null){
			notifyingService.clearTaskNotification();			
		}
	}

	private void cancelTaskTimer() {
		if (counter != null){
    		counter.cancel();
    	}
	}
	
	private void resetProgressControl() {
		progressBar.setProgress(0);
		progressBar.getLayoutParams().height = 2;
		resetTimeLeftIfTaskNotRunning();
        taskControlButton.setImageResource(R.drawable.play);
        taskControlButton.setTag(BUTTON_STATE.PLAY);
	}
	
	public void resetTimeLeftIfTaskNotRunning() {
		
		if (isPanelVisible() && !isTaskRunning()){
			int minutes = taskDatabaseMap.fetchTaskDurationSetting();
			String minutesString = "" + minutes;
			if (minutes < 10){
				minutesString = "0" + minutesString;
			}
			timeLeft.setText(minutesString + ":00");	
		}
	}

	private boolean isPanelVisible() {
		return runTaskPanel.getVisibility() == View.VISIBLE;
	}
	
	private boolean isTaskRunning() {
		return currentTaskRunState.equals(TASK_RUN_STATE.RUNNING);
	}
	
	private void beginUserTask(){
		
		int totalTimeInMin = taskDatabaseMap.fetchTaskDurationSetting();
		isUserTask = true;
		beginTask(taskDescription.getText().toString(), totalTimeInMin);
	}
	
	private void beginBreakTask(int breakTimeInMin){
		
		isUserTask = false;
		beginTask("Take a Break", breakTimeInMin);
	}
	
	private void beginTask(final String taskDesc, int totalTimeInMin) {
	
		taskDescription.setText(taskDesc);
		int totalTimeInSec = totalTimeInMin * 60;
//		int totalTimeInSec = 10; 
		progressBar.setMax(totalTimeInSec);
		progressBar.getLayoutParams().height = 3;
		long totalTimeInMillis = totalTimeInSec * ONE_SEC_IN_MILLI_SEC;
		taskEndTime = System.currentTimeMillis() + totalTimeInMillis;
		startTaskTimer();
		startAlarm(taskEndTime);
		
		hideButton.setVisibility(View.GONE);
		taskControlButton.setImageResource(R.drawable.stop);
		taskControlButton.setTag(BUTTON_STATE.STOP);

	    connection = new ServiceConnection() {

			public void onServiceConnected(ComponentName className, IBinder service) {
	            // This is called when the connection with the service has been
	            // established, giving us the service object we can use to
	            // interact with the service.  Because we have bound to a explicit
	            // service that we know is running in our own process, we can
	            // cast its IBinder to a concrete class and directly access it.
	            notifyingService = ((NotifyingService.LocalBinder)service).getService();
	            notifyingService.notifyTimeStarted(taskDesc);
	        }

	        public void onServiceDisconnected(ComponentName className) {
	            // This is called when the connection with the service has been
	            // unexpectedly disconnected -- that is, its process crashed.
	            // Because it is running in our same process, we should never
	            // see this happen.
	            notifyingService = null;
	        }
	    };
		
		activity.bindService(new Intent(activity, 
				NotifyingService.class), 
				connection, 
				Context.BIND_AUTO_CREATE);
		
	}

	private void startAlarm(long taskEndTime) {

	    alarmService.set(AlarmManager.RTC_WAKEUP, taskEndTime, pendingAlarmIntent);
	}

	public void stopAlarm() {

		alarmService.cancel(pendingAlarmIntent);
	}
	
	public void pause() {

		if (isTaskRunning()){
			cancelTaskTimer();
			currentTaskRunState = TASK_RUN_STATE.PAUSED;
		}
	}

	public void resume() {

		if (currentTaskRunState.equals(TASK_RUN_STATE.PAUSED)){
			
			startTaskTimer();
		}
	}

	private void startTaskTimer() {
		
		counter = new TaskTimer(taskEndTime, beepHandler);
		counter.start();
		currentTaskRunState = TASK_RUN_STATE.RUNNING;
	}

    private Handler beepHandler = new Handler() {
    	
        public void handleMessage(Message msg) {

    		String taskStatus = msg.getData().getString("TASK_STATUS");
    		if (taskStatus.equals("Running")){
    			
    			notifyTaskRunning(msg.getData());
    			
    		} else {

    			notifyTaskEnd();
    		}
        }

		private void notifyTaskRunning(Bundle data) {

    		long millisUntilFinished = data.getLong("TIME_LEFT_IN_MILLIS");
    		final DateFormat dateFormat = new SimpleDateFormat("mm:ss");
    		String timeStr = dateFormat.format(new Date(millisUntilFinished));
            timeLeft.setText(timeStr);
           	progressBar.setProgress(new Long(millisUntilFinished / ONE_SEC_IN_MILLI_SEC).intValue());
		}

		private void notifyTaskEnd() {
			
			currentTaskRunState = TASK_RUN_STATE.NOT_STARTED;
			
        	timeLeft.setText("00:00");
	    	resetProgressControl();
	    	
	    	if(isUserTask){
	    		
	    		notifyUserTaskEnd();
	    		
	    	} else {
	    		
	    		notifyBreakTaskEnd();
	    	}

	    	notifyingService.notifyTimeEnded();
	    	AlarmAlertWakeLock.release();
		}

		private void notifyUserTaskEnd() {
			pomodoroTrackPanel.addPomodoro();
			
			int count = pomodoroTrackPanel.getCurrentPomodoroCount();
			int _breakTime = BREAK_TIME_IN_MIN;
			if (count % 4 == 0){
				
				_breakTime = EVERY_FOUR_BREAK_TIME_IN_MIN;
			}
			final int breakTime = _breakTime;
			
			final String[] items = {"Take " + breakTime + " min break", "Skip break"};
			AlertDialog.Builder builder = new AlertDialog.Builder(activity);
			builder.setItems(items, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			        
			    	//Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_SHORT).show();
			    	switch (item) {
					case 0:
						beginBreakTask(breakTime);
						break;

					case 1:
						if(notifyingService != null){
			    			notifyingService.clearTaskNotification();
			    		}
						hidePanel();
				        break;
				        
					default:
						break;
					}
			        
			    }
			});
			AlertDialog alert = builder.create();
			alert.show();
		}
		
		private void notifyBreakTaskEnd() {
			AlertDialog.Builder builder = new AlertDialog.Builder(activity);
			builder.setMessage("         Break Complete         ")
			       .setCancelable(false)
			       .setNeutralButton("OK", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   if(notifyingService != null){
				    			notifyingService.clearTaskNotification();
				    		}    
			        	   hidePanel();
			           }
			       });
			AlertDialog alert = builder.create();
			alert.show();
		}
    };
	private AlarmManager alarmService;
	private Intent activityIntent;
	private PendingIntent pendingAlarmIntent;
}
