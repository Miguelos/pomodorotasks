package com.kpz.pomodorotasks.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.kpz.pomodorotasks.map.TaskDatabaseMap;


public class PomodoroTrackPanel {

	private static final int POMODOROS_PER_ROW = 8;
	private Activity activity;
	private TableLayout trackPanel;
	private TaskDatabaseMap taskDatabaseMap;
	private int count;
	private ImageButton clearButton;
	private TableRow currentTableRow;

	public PomodoroTrackPanel(Activity pActivity, TaskDatabaseMap pTaskDatabaseMap) {
		activity = pActivity;
		trackPanel = (TableLayout)activity.findViewById(R.id.trackPanel);
		taskDatabaseMap = pTaskDatabaseMap;
		clearButton = (ImageButton)activity.findViewById(R.id.pomodoro_clear);
		clearButton.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
	    		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
	    		builder.setMessage("        Clear Pomodoros?        ")
	    		       .setCancelable(true)
	    		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	    		           public void onClick(DialogInterface dialog, int id) {

	    		        	   taskDatabaseMap.updateCurrentPomodoros(0);
	    		        	   removeAllPomodoros();
	    		           }
	    		       })
	    		       .setNegativeButton("No", new DialogInterface.OnClickListener() {
	    		           public void onClick(DialogInterface dialog, int id) {

	    		           }
	    		       });
	    		AlertDialog alert = builder.create();
	    		alert.show();
			}
		});

		resetTrackPanel();
		initPomodoros();
	}
	
	private void initPomodoros() {
		
		count = taskDatabaseMap.fetchCurrentPomodoros();
		for (int i = 0; i < count; i++) {
			addPomodoroToView();
		}
	}

	public void addPomodoro() {
		count++;
		addPomodoroToView();
		taskDatabaseMap.updateCurrentPomodoros(count);
	}

	private void removeAllPomodoros() {
		resetTrackPanel();
	}

	private void resetTrackPanel() {
		count = 0;
		trackPanel.removeAllViews();
		createNewTableRow();
		clearButton.setVisibility(View.INVISIBLE);
	}

	private void createNewTableRow() {
		currentTableRow = (TableRow)activity.getLayoutInflater().inflate(R.layout.pomororo_table_row, null);
		trackPanel.addView(currentTableRow);
	}
	
	private void addPomodoroToView() {
		ImageView pomodoro = (ImageView)activity.getLayoutInflater().inflate(R.layout.pomodoro_icon, null);

		if (currentTableRow.getChildCount() == POMODOROS_PER_ROW){
			
			createNewTableRow();
		}
		
		currentTableRow.addView(pomodoro);
		TableRow.LayoutParams layoutParams = (TableRow.LayoutParams)pomodoro.getLayoutParams();
		layoutParams.height=25;
		layoutParams.width=25;
		
		resetClearButtonVisibility();
	}

	private void resetClearButtonVisibility() {
		
		if(count > 0 && clearButton.getVisibility() != View.VISIBLE){
			
			clearButton.setVisibility(View.VISIBLE);
			
		} else if (count == 0 && clearButton.getVisibility() == View.VISIBLE){
			
			clearButton.setVisibility(View.INVISIBLE);
		}
	}

	public int getCurrentPomodoroCount() {
		return count;
	}
}
