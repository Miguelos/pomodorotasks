package com.kpz.pomodorotasks;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AlphabetIndexer;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class PomodoroTasks extends ListActivity implements View.OnCreateContextMenuListener {
    

	private static final String TAG = "PomodoroTasks";
	
	private static final int TOTAL_TASKS_IN_VIEW = 6;
	
	private static final int ACTIVITY_CREATE=0;
    private static final int ACTIVITY_RUN=1;
    
    private static final int INSERT_ID = Menu.FIRST;
    private static final int DELETE_ID = Menu.FIRST + 1;

    private ListView mTrackList;
    private TaskDatabaseAdapter mTasksDatabaseHelper;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.tasks_list);
        initDatabaseHelper();        
        initTasksList();
        initAddTaskInput();
        registerForContextMenu(getListView());
    }

	private void initTasksList() {
		initTasksListViewContainer();
        populateTasksList();
	}

	private void initTasksListViewContainer() {
		mTrackList = getListView();
        mTrackList.setOnCreateContextMenuListener(this);
        ((TouchInterceptor) mTrackList).setDropListener(mDropListener);
        mTrackList.setCacheColorHint(0);
	}

	private void initDatabaseHelper() {
		mTasksDatabaseHelper = new TaskDatabaseAdapter(this);
        mTasksDatabaseHelper.open();
	}

	private void initAddTaskInput() {
		final EditText leftTextEdit = (EditText) findViewById(R.id.left_text_edit);
        Button leftButton = (Button) findViewById(R.id.left_text_button);
        leftButton.setOnClickListener(new OnClickListener() {
            
        	public void onClick(View v) {
 
            	createNewTask(leftTextEdit);
                populateTasksList();
                resetAddTaskEntryDisplay(leftTextEdit);
            }

			private void resetAddTaskEntryDisplay(final EditText leftTextEdit) {
				leftTextEdit.setText("");
                getListView().requestFocus();
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(leftTextEdit.getWindowToken(), 0); 

// to display on-screen keyboard                 
//                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))  
//                .showSoftInput(editText, 0);  
			}

			private void createNewTask(final EditText leftTextEdit) {
				String title = leftTextEdit.getText().toString();
            	mTasksDatabaseHelper.createTask(title, "");
			}
        });
	}
    
    private void populateTasksList() {
    	
    	Cursor tasksCursor = mTasksDatabaseHelper.fetchAllTasks();
        startManagingCursor(tasksCursor);
        
        // Create an array to specify the fields we want to display in the list (only TITLE)
        String[] from = new String[]{TaskDatabaseAdapter.KEY_TITLE};
        
        // and an array of the fields we want to bind those fields to (in this case just text1)
        int[] to = new int[]{R.id.text1};
        
        TrackListAdapter tasks = new TrackListAdapter(getApplication(), R.layout.tasks_row, tasksCursor, from, to);
        
        setListAdapter(tasks);
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
            createTask();
            return true;
        }
       
        return super.onMenuItemSelected(featureId, item);
    }
	
//    @Override
//	public void onCreateContextMenu(ContextMenu menu, View v,
//			ContextMenuInfo menuInfo) {
//		super.onCreateContextMenu(menu, v, menuInfo);
//        menu.add(0, DELETE_ID, 0, R.string.menu_delete);
//	}

    @Override
	public boolean onContextItemSelected(MenuItem item) {
		switch(item.getItemId()) {
    	case DELETE_ID:
    		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	        mTasksDatabaseHelper.deleteTask(info.id);
	        populateTasksList();
	        return true;
		}
		return super.onContextItemSelected(item);
	}
	
    private void createTask() {
        Intent i = new Intent(this, TaskEdit.class);
        startActivityForResult(i, ACTIVITY_CREATE);
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent i = new Intent(this, TaskRun.class);
        i.putExtra(TaskDatabaseAdapter.KEY_ROWID, id);
        startActivityForResult(i, ACTIVITY_RUN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, 
                                    Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        populateTasksList();
    }
    
    static class TrackListAdapter extends SimpleCursorAdapter implements SectionIndexer {

        private AlphabetIndexer mIndexer;
        
        TrackListAdapter(Context context, 
                int layout, Cursor cursor, String[] from, int[] to) {
        	
            super(context, layout, cursor, from, to);
        }
        
        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        	return super.runQueryOnBackgroundThread(constraint);
        }
        
        public Object[] getSections() {
            if (mIndexer != null) { 
                return mIndexer.getSections();
            } else {
                return null;
            }
        }
        
        public int getPositionForSection(int section) {
            int pos = mIndexer.getPositionForSection(section);
            return pos;
        }
        
        public int getSectionForPosition(int position) {
            return 0;
        }        
    }
    
    private TouchInterceptor.DropListener mDropListener = new TouchInterceptor.DropListener() {

    	public void drop(int from, int to) {

    		move(from, to);
        	int firstVisiblePosition = getFirstVisiblePostionBeforeRefresh();        	
        	populateTasksList();
        	
        	resetBottomMargin();
        	scrollBackToViewingTasks(to, firstVisiblePosition);
        }

		private void resetBottomMargin() {
			LinearLayout.LayoutParams viewGroupParams = (LinearLayout.LayoutParams)getListView().getLayoutParams();
			if (viewGroupParams.bottomMargin != 0){
				viewGroupParams.bottomMargin = 0;
	    		getListView().setLayoutParams(viewGroupParams);				
			}
		}

		private void scrollBackToViewingTasks(int to, int firstVisiblePosition) {
			
			int total = mTrackList.getCount();
			if (total > TOTAL_TASKS_IN_VIEW){
				if(total -1 == to){
					
					getListView().setSelectionFromTop(total  - 6, 0);
				} else {
					getListView().setSelectionFromTop(firstVisiblePosition, 0);
				}
        	}
		}

		private int getFirstVisiblePostionBeforeRefresh() {
			
			return mTrackList.getFirstVisiblePosition();
		}

		private void move(int from, int to) {
			
			Cursor cursor = (Cursor)getListAdapter().getItem(from);
        	String fromSeq = cursor.getString(cursor.getColumnIndex(TaskDatabaseAdapter.KEY_SEQUENCE));
        	String fromRowId = cursor.getString(cursor.getColumnIndex(TaskDatabaseAdapter.KEY_ROWID));
        	
        	cursor = (Cursor)getListAdapter().getItem(to);
        	String toSeq = cursor.getString(cursor.getColumnIndex(TaskDatabaseAdapter.KEY_SEQUENCE));
        	String toRowId = cursor.getString(cursor.getColumnIndex(TaskDatabaseAdapter.KEY_ROWID));
        	
        	mTasksDatabaseHelper.move(fromRowId, fromSeq, toRowId, toSeq);
		}
    };
}
