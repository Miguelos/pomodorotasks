package com.kpz.pomodorotasks.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.kpz.pomodorotasks.map.TaskDatabaseMap;

public class SettingsActivity extends Activity {

    private TaskDatabaseMap taskDatabaseMap;
	private SeekBar mSeekBar;
	private TextView mDurationText;
	private int mDuration;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.settings);
    	
    	initDatabaseConnection();
    	initBackButton();
    	initTimeDurationSetting();
    }

	private void initTimeDurationSetting() {
		mDurationText = (TextView)findViewById(R.id.duration);
        mSeekBar = (SeekBar)findViewById(R.id.seek);
        mDuration = taskDatabaseMap.fetchTaskDurationSetting();
        mDurationText.setText(mDuration + " min");
        mSeekBar.setProgress(mDuration);
        mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
				int duration = progress + 1;
            	mDuration  = duration;
            	mDurationText.setText(duration + " min");
                
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
		});
	}

	private void initBackButton() {
		Button revertButton = (Button) findViewById(R.id.back);
    	revertButton.setOnClickListener(new View.OnClickListener() {

    	    public void onClick(View view) {
    	        setResult(RESULT_OK);
    	        finish();
    	    }
    	});
	}
	
	private void initDatabaseConnection() {
		taskDatabaseMap = new TaskDatabaseMap(this);
    	taskDatabaseMap.open();
	}
    
    @Override
    protected void onPause() {
        super.onPause();
        saveState();
    }
    
    private void saveState() {
        taskDatabaseMap.updateTaskDurationSetting(mDuration);
    }
}
