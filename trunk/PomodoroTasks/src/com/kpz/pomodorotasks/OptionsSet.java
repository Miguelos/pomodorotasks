package com.kpz.pomodorotasks;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class OptionsSet extends Activity {

    private TaskDatabaseAdapter mDbHelper;
	private SeekBar mSeekBar;
	private TextView mDurationText;
	private int mDuration;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.options_set);
    	
    	initDatabaseConnection();
    	initBackButton();

    	mDurationText = (TextView)findViewById(R.id.duration);
        mSeekBar = (SeekBar)findViewById(R.id.seek);
        mDuration = mDbHelper.fetchTaskDurationSetting();
        mDurationText.setText(mDuration + " min");
        mSeekBar.setProgress(mDuration);
        mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
            	mDuration  = progress;
            	mDurationText.setText(progress + " min");
                
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
		mDbHelper = new TaskDatabaseAdapter(this);
    	mDbHelper.open();
	}
    
    @Override
    protected void onPause() {
        super.onPause();
        saveState();
    }
    
    private void saveState() {
        mDbHelper.updateTaskDurationSetting(mDuration);
    }
}
