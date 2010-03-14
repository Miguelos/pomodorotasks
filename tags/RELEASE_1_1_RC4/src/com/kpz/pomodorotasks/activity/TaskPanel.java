package com.kpz.pomodorotasks.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.CountDownTimer;
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
	
	private static final int ONE_SEC = 1000;
	private static final int FIVE_MIN_IN_SEC = 300;

	private LinearLayout runTaskPanel;
	private ImageButton taskControlButton;
	private ImageButton hideButton;
	private TextView taskDescription;
	private TextView timeLeft;
	private ProgressBar progressBar;
	private TaskDatabaseMap taskDatabaseMap;
	private NotifyingService mBoundService;
	private TaskTimer counter;
	private ServiceConnection connection;
	private Activity activity;
	private boolean isUserTask = false;
	
	private enum BUTTON_STATE {
		PLAY, STOP
	}

	public TaskPanel(Activity pActivity, TaskDatabaseMap pTaskDatabaseMap) {
		runTaskPanel = (LinearLayout)pActivity.findViewById(R.id.runTaskPanel);
		taskControlButton = (ImageButton)pActivity.findViewById(R.id.control_icon);
		hideButton = (ImageButton)pActivity.findViewById(R.id.hide_panel_button);
		hideButton.setVisibility(View.VISIBLE);
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
		if (counter != null){
    		counter.cancel();
    	}
		
		resetProgressControl();
		
		if(mBoundService != null){
			mBoundService.clearTaskNotification();			
		}
	}
	
	private void resetProgressControl() {
		progressBar.setProgress(0);
		progressBar.getLayoutParams().height = 2;
		resetTimeLeftIfTaskNotRunning();
        taskControlButton.setImageResource(R.drawable.play);
        taskControlButton.setTag(BUTTON_STATE.PLAY);
        hideButton.setVisibility(View.VISIBLE);
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
		return progressBar.getProgress() != 0;
	}
	
	private void beginUserTask(){
		
		int totalTime = taskDatabaseMap.fetchTaskDurationSetting() * 60;
		isUserTask = true;
		beginTask(taskDescription.getText().toString(), totalTime);
	}
	
	private void beginBreakTask(){
		
		int totalTime = FIVE_MIN_IN_SEC;
		isUserTask = false;
		beginTask("Take a Break", totalTime);
	}
	
	private void beginTask(final String taskDesc, int totalTime) {
	
		taskDescription.setText(taskDesc);
		progressBar.setMax(totalTime);
		progressBar.getLayoutParams().height = 3;
		counter = new TaskTimer(totalTime * ONE_SEC, ONE_SEC);
		//counter = new ProgressThread(handler);
		counter.start();
		
		hideButton.setVisibility(View.INVISIBLE);
		taskControlButton.setImageResource(R.drawable.stop);
		taskControlButton.setTag(BUTTON_STATE.STOP);

	    connection = new ServiceConnection() {

			public void onServiceConnected(ComponentName className, IBinder service) {
	            // This is called when the connection with the service has been
	            // established, giving us the service object we can use to
	            // interact with the service.  Because we have bound to a explicit
	            // service that we know is running in our own process, we can
	            // cast its IBinder to a concrete class and directly access it.
	            mBoundService = ((NotifyingService.LocalBinder)service).getService();
	            mBoundService.notifyTimeStarted(taskDesc);
	        }

	        public void onServiceDisconnected(ComponentName className) {
	            // This is called when the connection with the service has been
	            // unexpectedly disconnected -- that is, its process crashed.
	            // Because it is running in our same process, we should never
	            // see this happen.
	            mBoundService = null;
	        }
	    };
		
		activity.bindService(new Intent(activity, 
				NotifyingService.class), 
				connection, 
				Context.BIND_AUTO_CREATE);
		
	}

    public class TaskTimer extends CountDownTimer{
	    
		public TaskTimer(long millisInFuture, long countDownInterval) {
	    	super(millisInFuture, countDownInterval);
		}

		@Override
	    public void onTick(long millisUntilFinished) {
	    	
	    	incrementProgress(millisUntilFinished);
	    }
		
		private void incrementProgress(long millisUntilFinished) {
			
			final DateFormat dateFormat = new SimpleDateFormat("mm:ss");
			String timeStr = dateFormat.format(new Date(millisUntilFinished));
            timeLeft.setText(timeStr);
           	progressBar.setProgress(new Long(millisUntilFinished / ONE_SEC).intValue());
		}

		private void beep() {
			Message msg = beepHandler.obtainMessage();
			beepHandler.sendMessage(msg);
		}
		
		@Override
		public void onFinish() {
			beep();
		}
    }

    final Handler beepHandler = new Handler() {
        public void handleMessage(Message msg) {

        	mBoundService.notifyTimeEnded();
	        
        	timeLeft.setText("00:00");
	    	resetProgressControl();
	    	
	    	if(isUserTask){
	    		
	        	final String[] items = {"Take 5 min break", "Skip break"};
	    		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
	    		builder.setItems(items, new DialogInterface.OnClickListener() {
	    		    public void onClick(DialogInterface dialog, int item) {
	    		        
	    		    	//Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_SHORT).show();
	    		    	switch (item) {
	    				case 0:
	    					beginBreakTask();
	    					break;

	    				case 1:
	    					if(mBoundService != null){
	    		    			mBoundService.clearTaskNotification();
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
	    		
	    	} else {
	    		
	    		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
	    		builder.setMessage("      Break complete      ")
	    		       .setCancelable(false)
	    		       .setNeutralButton("OK", new DialogInterface.OnClickListener() {
	    		           public void onClick(DialogInterface dialog, int id) {
	    		        	   if(mBoundService != null){
		    		    			mBoundService.clearTaskNotification();
		    		    		}    
	    		        	   hidePanel();
	    		           }
	    		       });
	    		AlertDialog alert = builder.create();
	    		alert.show();
	    	}
        }
    };
}
