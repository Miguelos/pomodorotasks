package com.kpz.pomodorotasks.activity;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.kpz.pomodorotasks.map.TaskDatabaseMap;
import com.kpz.pomodorotasks.map.TaskDatabaseMap.ConfigType;

public class TaskDurationPreference extends DialogPreference {

	private TaskDatabaseMap taskDatabaseMap;
	private int mDuration;
	private SeekBar mSeekBar;
	private TextView mDurationText;
	private ConfigType taskType;
    
    public TaskDurationPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.task_duration_preference_dialog);
        
        taskDatabaseMap = TaskDatabaseMap.getInstance(context);
    }
    

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

		mDurationText = (TextView)view.findViewById(R.id.task_duration_info);
        mSeekBar = (SeekBar)view.findViewById(R.id.task_duration_seekbar);
        taskType = ConfigType.valueOf(getKey());
		mDuration = taskDatabaseMap.getPreferences().getDurationPreference(taskType);
        mDurationText.setText(mDuration + " min");
        mSeekBar.setProgress(mDuration);
        mSeekBar.setSecondaryProgress(new Integer(taskType.getDefaultValue()).intValue());
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
    
    @Override
    protected void onDialogClosed(boolean positiveResult) {
    	super.onDialogClosed(positiveResult);
    	
    	if (positiveResult){
    		saveState();
    		setSummary(mDuration + " min");
    	}
    }
    
    private void saveState() {
        taskDatabaseMap.getPreferences().updateDurationPreference(taskType, mDuration);
    }
}
