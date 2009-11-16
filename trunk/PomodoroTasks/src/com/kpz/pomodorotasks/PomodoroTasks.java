package com.kpz.pomodorotasks;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class PomodoroTasks extends ListActivity {
    
	private static final String TAG = PomodoroTasks.class.getName();
	
	private static final int ACTIVITY_CREATE=0;
    private static final int ACTIVITY_EDIT=1;
    
    private static final int INSERT_ID = Menu.FIRST;
    private static final int DELETE_ID = Menu.FIRST + 1;

    private NotesDbAdapter mDbHelper;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setTheme(android.R.style.Theme_Light);
        
//        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
//        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.notes_list);
        
        setContentView(R.layout.notes_list);
        
        //getListView().setBackgroundColor(Color.BLUE);
        //getListView().setDividerHeight(4);
        
        final EditText leftTextEdit = (EditText) findViewById(R.id.left_text_edit);
        
        Button leftButton = (Button) findViewById(R.id.left_text_button);

//        leftTextEdit.setWidth(getParent().getWindowManager().getDefaultDisplay().getWidth() - leftButton.getWidth());
  
//        Log.d(TAG,"getParent().getWindowManager().getDefaultDisplay().getWidth():" + getParent().getWindowManager().getDefaultDisplay().getWidth());
        //Log.d(TAG,"leftButton.getWidth()" + leftButton.getWidth());

        
        leftButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
 
                Log.d(TAG,"in listener");

            	String title = leftTextEdit.getText().toString();
            	long id = mDbHelper.createNote(title, "");
            	
                Log.d(TAG,"new id:" + id);

                leftTextEdit.setText(R.string.custom_title_left);
                
                fillData();
                getListView().requestFocus();
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))  
                .hideSoftInputFromWindow(leftTextEdit.getWindowToken(), 0); 
                
//                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))  
//                .showSoftInput(editText, 0);  
            }
        });
        
        
        Log.d(TAG,"leftButton:" + leftButton);

        mDbHelper = new NotesDbAdapter(this);
        mDbHelper.open();
        fillData();
        registerForContextMenu(getListView());
    }
    
    private void fillData() {
        // Get all of the rows from the database and create the item list
    	Cursor notesCursor = mDbHelper.fetchAllNotes();
        startManagingCursor(notesCursor);
        
        // Create an array to specify the fields we want to display in the list (only TITLE)
        String[] from = new String[]{NotesDbAdapter.KEY_TITLE};
        
        // and an array of the fields we want to bind those fields to (in this case just text1)
        int[] to = new int[]{R.id.text1};
        
        // Now create a simple cursor adapter and set it to display
        SimpleCursorAdapter notes = 
        	    new SimpleCursorAdapter(this, R.layout.notes_row, notesCursor, from, to);
        setListAdapter(notes);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, INSERT_ID, 0, R.string.menu_insert);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
        case INSERT_ID:
            createNote();
            return true;
        }
       
        return super.onMenuItemSelected(featureId, item);
    }
	
    @Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, DELETE_ID, 0, R.string.menu_delete);
	}

    @Override
	public boolean onContextItemSelected(MenuItem item) {
		switch(item.getItemId()) {
    	case DELETE_ID:
    		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	        mDbHelper.deleteNote(info.id);
	        fillData();
	        return true;
		}
		return super.onContextItemSelected(item);
	}
	
    private void createNote() {
        Intent i = new Intent(this, NoteEdit.class);
        startActivityForResult(i, ACTIVITY_CREATE);
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent i = new Intent(this, NoteEdit.class);
        i.putExtra(NotesDbAdapter.KEY_ROWID, id);
        startActivityForResult(i, ACTIVITY_EDIT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, 
                                    Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        fillData();
    }
}
