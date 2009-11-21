package com.kpz.pomodorotasks;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class TaskRun extends Activity {

	private static final int ONE_SEC = 1000;
	private TextView mTitleText;
    private TextView mBodyText;
    private Long mRowId;
    private TaskDatabaseAdapter mDbHelper;
	private ProgressBar progressHorizontal;
	private TextView mTimeLeft;
	private int totalTime;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	 
    	mDbHelper = new TaskDatabaseAdapter(this);
    	mDbHelper.open();
    	 
    	setContentView(R.layout.task_run);
    	 
    	mTitleText = (TextView) findViewById(R.id.task_title);
    	mBodyText = (TextView) findViewById(R.id.task_body);
    	mTimeLeft = (TextView) findViewById(R.id.time_left);

    	totalTime = 20;
    	progressHorizontal = (ProgressBar) findViewById(R.id.progress_horizontal);
    	progressHorizontal.setMax(totalTime - 1);
    	
    	Button startButton = (Button) findViewById(R.id.start);
    	 
    	mRowId = savedInstanceState != null ? savedInstanceState.getLong(TaskDatabaseAdapter.KEY_ROWID) 
    	                                    : null;
    	if (mRowId == null) {
    	    Bundle extras = getIntent().getExtras();
    	    mRowId = extras != null ? extras.getLong(TaskDatabaseAdapter.KEY_ROWID) 
    	                            : null;
    	}
    	 
    	populateFields();
    	 
    	startButton.setOnClickListener(new View.OnClickListener() {

    	    public void onClick(View view) {
    	        
    			MyCount counter = new MyCount(totalTime * ONE_SEC, ONE_SEC);
    	        counter.start();
    	    }
    	});
    }
    
    private void populateFields() {
        if (mRowId != null) {
            Cursor task = mDbHelper.fetchTask(mRowId);
            startManagingCursor(task);
            mTitleText.setText(task.getString(task.getColumnIndexOrThrow(TaskDatabaseAdapter.KEY_TITLE)));
            mBodyText.setText(task.getString(task.getColumnIndexOrThrow(TaskDatabaseAdapter.KEY_BODY)));
        }
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(TaskDatabaseAdapter.KEY_ROWID, mRowId);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        saveState();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        populateFields();
    }
    
    private void saveState() {
//        String title = mTitleText.getText().toString();
//        String body = mBodyText.getText().toString();
//
//        if (mRowId == null) {
//            long id = mDbHelper.createTask(title, body);
//            if (id > 0) {
//                mRowId = id;
//            }
//        } else {
//            mDbHelper.updateTask(mRowId, title, body);
//        }
    }

    public class MyCount extends CountDownTimer{
	    
    	public MyCount(long millisInFuture, long countDownInterval) {
    		super(millisInFuture, countDownInterval);
	    }
	    
	    @Override
	    public void onFinish() {
	    	mTimeLeft.setText("done!");
	    }
	    
	    @Override
	    public void onTick(long millisUntilFinished) {
	    	mTimeLeft.setText("Time Left: "+ millisUntilFinished/ONE_SEC);
	    	progressHorizontal.incrementProgressBy(1);
	    }
    }
}
