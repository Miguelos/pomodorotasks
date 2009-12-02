package com.kpz.pomodorotasks;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class TaskEdit extends Activity {

	private EditText mDescription;
    private Long mRowId;
    private TaskDatabaseAdapter mDbHelper;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.task_edit);
    	
    	initDatabaseConnection();
    	populateTaskEditText(savedInstanceState);
    	initDoneButton();
    	initRevertButton();
    }

	private void initDoneButton() {
		Button doneButton = (Button) findViewById(R.id.done);
    	doneButton.setOnClickListener(new View.OnClickListener() {

    	    public void onClick(View view) {
    	    	saveState();
    	        setResult(RESULT_OK);
    	        finish();
    	    }
    	});
	}

	private void initRevertButton() {
		Button revertButton = (Button) findViewById(R.id.revert);
    	revertButton.setOnClickListener(new View.OnClickListener() {

    	    public void onClick(View view) {
    	        setResult(RESULT_OK);
    	        finish();
    	    }
    	});
	}
	
	private void populateTaskEditText(Bundle savedInstanceState) {
		mDescription = (EditText) findViewById(R.id.description);
    	mRowId = savedInstanceState != null ? savedInstanceState.getLong(TaskDatabaseAdapter.KEY_ROWID) 
    	                                    : null;
    	if (mRowId == null) {
    	    Bundle extras = getIntent().getExtras();
    	    mRowId = extras != null ? extras.getLong(TaskDatabaseAdapter.KEY_ROWID)
    	                            : null;
    	}
    	populateFields();
	}

	private void initDatabaseConnection() {
		mDbHelper = new TaskDatabaseAdapter(this);
    	mDbHelper.open();
	}
    
    private void populateFields() {
        if (mRowId != null) {
            Cursor task = mDbHelper.fetch(mRowId);
            startManagingCursor(task);
            mDescription.setText(task.getString(
    	            task.getColumnIndexOrThrow(TaskDatabaseAdapter.KEY_DESCRIPTION)));
        }
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(TaskDatabaseAdapter.KEY_ROWID, mRowId);
    }

    private void saveState() {
        String description = mDescription.getText().toString();

        if (mRowId == null) {
            long id = mDbHelper.createTask(description);
            if (id > 0) {
                mRowId = id;
            }
        } else {
            mDbHelper.update(mRowId, description);
        }
    }
}
