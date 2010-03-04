package com.kpz.pomodorotasks.activity;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.kpz.pomodorotasks.map.TaskDatabaseMap;

public class TaskEditActivity extends Activity {

	private EditText description;
    private Long mRowId;
    private TaskDatabaseMap taskDatabaseMap;
    
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
    	    	((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(description.getWindowToken(), 0);
    	        setResult(RESULT_OK);
    	        finish();
    	    }
    	});
	}

	private void initRevertButton() {
		Button revertButton = (Button) findViewById(R.id.revert);
    	revertButton.setOnClickListener(new View.OnClickListener() {

    	    public void onClick(View view) {
    	    	((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(description.getWindowToken(), 0);
    	        setResult(RESULT_OK);
    	        finish();
    	    }
    	});
	}
	
	private void populateTaskEditText(Bundle savedInstanceState) {
		description = (EditText) findViewById(R.id.description);
    	mRowId = savedInstanceState != null ? savedInstanceState.getLong(TaskDatabaseMap.KEY_ROWID) 
    	                                    : null;
    	if (mRowId == null) {
    	    Bundle extras = getIntent().getExtras();
    	    mRowId = extras != null ? extras.getLong(TaskDatabaseMap.KEY_ROWID)
    	                            : null;
    	}
    	populateFields();
	}

	private void initDatabaseConnection() {
		taskDatabaseMap = new TaskDatabaseMap(this);
    	taskDatabaseMap.open();
	}
    
    private void populateFields() {
        if (mRowId != null) {
            Cursor task = taskDatabaseMap.fetch(mRowId);
            startManagingCursor(task);
            description.setText(task.getString(
    	            task.getColumnIndexOrThrow(TaskDatabaseMap.KEY_DESCRIPTION)));
        }
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(TaskDatabaseMap.KEY_ROWID, mRowId);
    }

    private void saveState() {
        String desc = description.getText().toString();

        if (mRowId == null) {
            long id = taskDatabaseMap.createTask(desc);
            if (id > 0) {
                mRowId = id;
            }
        } else {
            taskDatabaseMap.update(mRowId, desc);
        }
    }
}
