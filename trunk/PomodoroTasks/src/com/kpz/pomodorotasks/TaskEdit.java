package com.kpz.pomodorotasks;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class TaskEdit extends Activity {

	private EditText mTitleText;
    private EditText mBodyText;
    private Long mRowId;
    private TaskDatabaseAdapter mDbHelper;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	 
    	mDbHelper = new TaskDatabaseAdapter(this);
    	mDbHelper.open();
    	 
    	setContentView(R.layout.task_edit);
    	 
    	mTitleText = (EditText) findViewById(R.id.title);
    	mBodyText = (EditText) findViewById(R.id.body);
    	 
    	Button confirmButton = (Button) findViewById(R.id.confirm);
    	 
    	mRowId = savedInstanceState != null ? savedInstanceState.getLong(TaskDatabaseAdapter.KEY_ROWID) 
    	                                    : null;
    	if (mRowId == null) {
    	    Bundle extras = getIntent().getExtras();
    	    mRowId = extras != null ? extras.getLong(TaskDatabaseAdapter.KEY_ROWID) 
    	                            : null;
    	}
    	 
    	populateFields();
    	 
    	confirmButton.setOnClickListener(new View.OnClickListener() {

    	    public void onClick(View view) {
    	        setResult(RESULT_OK);
    	        finish();
    	    }
    	     
    	});
    }
    
    private void populateFields() {
        if (mRowId != null) {
            Cursor task = mDbHelper.fetchTask(mRowId);
            startManagingCursor(task);
            mTitleText.setText(task.getString(
    	            task.getColumnIndexOrThrow(TaskDatabaseAdapter.KEY_TITLE)));
            mBodyText.setText(task.getString(
                    task.getColumnIndexOrThrow(TaskDatabaseAdapter.KEY_BODY)));
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
        String title = mTitleText.getText().toString();
        String body = mBodyText.getText().toString();

        if (mRowId == null) {
            long id = mDbHelper.createTask(title, body);
            if (id > 0) {
                mRowId = id;
            }
        } else {
            mDbHelper.updateTask(mRowId, title, body);
        }
    }
}
