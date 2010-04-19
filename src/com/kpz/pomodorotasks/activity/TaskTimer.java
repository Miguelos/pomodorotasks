package com.kpz.pomodorotasks.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class TaskTimer {
    
	private long endTime;
	private boolean isCancelled = false;

	private Handler defaultQueueHandler = new Handler();
	private Runnable tickRunnable = new Runnable() {
		
		public void run() {
			tick();
		}
	};
	
	public TaskTimer(long pTaskEndTime, Handler pHandler) {
		
		endTime = pTaskEndTime;
		defaultQueueHandler = pHandler;
	}

	public void start(){

		tick();
	}
	
	public void cancel(){
		
		isCancelled = true;
	}

	private void tick() {
		
		if (isCancelled){
			return;
		}
		
		long millisUntilFinished = endTime - System.currentTimeMillis();
		if(millisUntilFinished > 0){
			
			incrementProgress(millisUntilFinished);
			defaultQueueHandler.postDelayed(tickRunnable, 1000);
			
		} else {
			
			finish();
		}
	}
	
	private void incrementProgress(long millisUntilFinished) {
		
		Message msg = defaultQueueHandler.obtainMessage();
		Bundle data = new Bundle();
		data.putString("TASK_STATUS", "Running");
		data.putLong("TIME_LEFT_IN_MILLIS", millisUntilFinished);
		msg.setData(data);
		defaultQueueHandler.sendMessage(msg);
	}

	private void finish() {
		Message msg = defaultQueueHandler.obtainMessage();
		Bundle data = new Bundle();
		data.putString("TASK_STATUS", "Finished");
		msg.setData(data);
		defaultQueueHandler.sendMessage(msg);
	}

}
