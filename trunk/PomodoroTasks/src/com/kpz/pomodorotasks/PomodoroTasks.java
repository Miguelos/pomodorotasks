package com.kpz.pomodorotasks;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.SimpleCursorAdapter.ViewBinder;

import com.kpz.pomodorotasks.TaskDatabaseAdapter.StatusType;

public class PomodoroTasks extends ListActivity {
    

	public static final String TAG = "PomodoroTasks";
	
	private static final int TOTAL_TASKS_IN_VIEW = 6;
	
	private static final int ACTIVITY_EDIT = 1;
	
	private static final int MAIN_MENU_DELETE_ALL_ID = Menu.FIRST;

	private static final int CONTEXT_MENU_EDIT_ID = Menu.FIRST + 10;
	private static final int CONTEXT_MENU_DELETE_ID = CONTEXT_MENU_EDIT_ID + 1;
	private static final int CONTEXT_MENU_COMPLETE_ID = CONTEXT_MENU_EDIT_ID + 2;
	private static final int CONTEXT_MENU_REOPEN_ID = CONTEXT_MENU_EDIT_ID + 3;
	
    private ListView taskList;
    private TaskDatabaseAdapter mTasksDatabaseHelper;
    
	private static final int ONE_SEC = 1000;

	private TextView mTaskDescription;
	private ProgressBar progressBar;
	private TextView mTimeLeft;
	private int totalTime;

	private MyCount counter;
	private ImageButton taskControlButton;
	private Vibrator vibrator;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.tasks_list);
        
        vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        
        initDatabaseHelper();        
        initTasksList();
        initAddTaskInput();
        initRunTaskPanel();
    	
        registerForContextMenu(getListView());
    }
    
	private void initRunTaskPanel() {
		
    	RelativeLayout runTaskPanel = (RelativeLayout)findViewById(R.id.runTaskPanel);
		runTaskPanel.setVisibility(View.GONE);
	}

	private void showRunTaskPanel(String taskDescription) {
		
		RelativeLayout runTaskPanel = (RelativeLayout)findViewById(R.id.runTaskPanel);
		runTaskPanel.setVisibility(View.VISIBLE);
		
		taskControlButton = (ImageButton) findViewById(R.id.control_icon);
		
    	mTaskDescription = (TextView) findViewById(R.id.task_description);
    	mTaskDescription.setText(taskDescription);
    	mTimeLeft = (TextView) findViewById(R.id.time_left);

    	totalTime = 10;
    	progressBar = (ProgressBar) findViewById(R.id.progress_horizontal);
    	progressBar.setMax(totalTime - 1);

    	//progressBar.setv = defaultButtonHeight;
    	//populateFields();
    	
    	resetTaskRun(taskControlButton);
    	
    	taskControlButton.setOnClickListener(new View.OnClickListener() {

    	    public void onClick(View view) {
    	    	
    	    	if (counter != null && taskControlButton.getTag(R.string.TASK_CONTROL_BUTTON_STATE_TYPE).equals(R.string.TO_STOP_STATE)){
    	    		
    	    		resetTaskRun(taskControlButton);
    	    		
    	    	} else {
    	    		
    	    		counter = new MyCount(totalTime * ONE_SEC, ONE_SEC);
        	        counter.start();
        	        
        	        taskControlButton.setImageResource(R.drawable.ic_media_pause);
        	        taskControlButton.setTag(R.string.TASK_CONTROL_BUTTON_STATE_TYPE, R.string.TO_STOP_STATE);
        	        adjustHeightToDefault(taskControlButton);
    	    	}
    	    }
    	});
    	
	}

	private void resetTaskRun(final ImageButton taskControlButton) {
		if (counter != null){
    		counter.cancel();
    	}
		
		resetProgressControl(taskControlButton);
	}

	private void resetProgressControl(final ImageButton taskControlButton) {
		mTimeLeft.setText(R.string.zeroTime);
		progressBar.setProgress(0);
        taskControlButton.setImageResource(R.drawable.ic_media_play);
        taskControlButton.setTag(R.string.TASK_CONTROL_BUTTON_STATE_TYPE, R.string.TO_PLAY_STATE);
        adjustHeightToDefault(taskControlButton);
	}

	private void adjustHeightToDefault(final ImageButton taskControlButton) {
		Button leftButton = (Button) findViewById(R.id.left_text_button);
		int defaultButtonHeight = leftButton.getHeight();
		taskControlButton.getLayoutParams().height = defaultButtonHeight;
	}
	
	private void initTasksList() {
		initTasksListViewContainer();
        populateTasksList();
	}
	
    private void populateTasksList() {
    	
    	Cursor tasksCursor = mTasksDatabaseHelper.fetchAll();
        startManagingCursor(tasksCursor);
        
        // Create an array to specify the fields we want to display in the list (only Description)
        String[] from = new String[]{TaskDatabaseAdapter.KEY_DESCRIPTION, TaskDatabaseAdapter.KEY_STATUS};
        
        // and an array of the fields we want to bind those fields to (in this case just text1)
        int[] to = new int[]{R.id.text1, R.id.taskRow};
        
        SimpleCursorAdapter tasks = new SimpleCursorAdapter(getApplication(), R.layout.tasks_row, tasksCursor, from, to);
        
        tasks.setViewBinder(new ViewBinder() {
			
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				
				if (view.getId() == R.id.taskRow){

					TextView textView = (TextView)view.findViewById(R.id.text1);
					
					String statusText = cursor.getString(columnIndex);
					if (StatusType.OPEN.getDescription().equals(statusText)){
						textView.getPaint().setStrikeThruText(false);
					} else if (StatusType.COMPLETED.getDescription().equals(statusText)){
						textView.getPaint().setStrikeThruText(true);
					}
					
					return true;
				}
				
				return false;
			}
		});
        
        setListAdapter(tasks);
        taskListCursor = tasks.getCursor();
    }

	private void initTasksListViewContainer() {
		taskList = getListView();
        taskList.setOnCreateContextMenuListener(this);
        ((TouchInterceptor) taskList).setDropListener(mDropListener);
        ((TouchInterceptor) taskList).setCheckOffListener(mCheckOffListener);
        taskList.setCacheColorHint(0);
	}
	
	private void scrollBackToViewingTasks(int to, int firstVisiblePosition) {
		
		int total = taskList.getCount();
		if (total > TOTAL_TASKS_IN_VIEW){
			if(total -1 == to){
				
				getListView().setSelectionFromTop(total  - 6, 0);
			} else {
				getListView().setSelectionFromTop(firstVisiblePosition, 0);
			}
    	}
	}
	
	public void checkOffTask(int which, View targetView) {

		TextView textView = (TextView)targetView.findViewById(R.id.text1);
    	textView.getPaint().setStrikeThruText(true);
    	
    	Cursor cursor = (Cursor)getListAdapter().getItem(which);
		int rowId = Integer.parseInt(cursor.getString(cursor.getColumnIndex(TaskDatabaseAdapter.KEY_ROWID)));
		mTasksDatabaseHelper.updateStatus(rowId, true);
		
		if (!taskListCursor.requery()){
			
			int firstVisiblePosition = getFirstVisiblePostionBeforeRefresh();
        	populateTasksList();
        	scrollBackToViewingTasks(which, firstVisiblePosition);
		}
	}
	
    public void uncheckOffTask(int which, View targetView) {

    	TextView textView = (TextView)targetView.findViewById(R.id.text1);
    	textView.getPaint().setStrikeThruText(false);
    	
    	Cursor cursor = (Cursor)getListAdapter().getItem(which);
		int rowId = Integer.parseInt(cursor.getString(cursor.getColumnIndex(TaskDatabaseAdapter.KEY_ROWID)));
		mTasksDatabaseHelper.updateStatus(rowId, false);
		
		if (!taskListCursor.requery()){
			
			int firstVisiblePosition = getFirstVisiblePostionBeforeRefresh();
        	populateTasksList();
        	scrollBackToViewingTasks(which, firstVisiblePosition);
		}
	}
	
	private int getFirstVisiblePostionBeforeRefresh() {
		
		return taskList.getFirstVisiblePosition();
	}

	
    private TouchInterceptor.CheckOffListener mCheckOffListener = new TouchInterceptor.CheckOffListener() {
    	
        public void checkOff(int which) {

        	View targetView = (View)taskList.getChildAt(which - taskList.getFirstVisiblePosition());
        	checkOffTask(which, targetView);
        }
        
		public void uncheckOff(int which) {

			View targetView = (View)taskList.getChildAt(which - taskList.getFirstVisiblePosition());
			uncheckOffTask(which, targetView);
		}
    };

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

// to hide keyboard				
//                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(leftTextEdit.getWindowToken(), 0); 
// to display on-screen keyboard                 
//                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))  
//                .showSoftInput(editText, 0);  
			}

			private void createNewTask(final EditText leftTextEdit) {
				String noteDescription = leftTextEdit.getText().toString();
            	mTasksDatabaseHelper.createTask(noteDescription);
			}
        });
	}
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MAIN_MENU_DELETE_ALL_ID, 0, R.string.menu_delete_all);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {

        case MAIN_MENU_DELETE_ALL_ID:
        	mTasksDatabaseHelper.deleteAll();
	        populateTasksList();
        	return true;
        }
       
        return super.onMenuItemSelected(featureId, item);
    }
	
    @Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
		Log.d(TAG, "onCreateContextMenu");
    	Cursor cursor = (Cursor)getListAdapter().getItem(new Long(((AdapterContextMenuInfo)menuInfo).position).intValue());
		String status = cursor.getString(cursor.getColumnIndex(TaskDatabaseAdapter.KEY_STATUS));
		
		if (StatusType.isOpen(status)){
			
			menu.add(0, CONTEXT_MENU_COMPLETE_ID, 0, R.string.menu_complete);
			
		} else if (StatusType.isCompleted(status)){
			
			menu.add(0, CONTEXT_MENU_REOPEN_ID, 0, R.string.menu_reopen);
		}
		
		
		menu.add(0, CONTEXT_MENU_EDIT_ID, 0, R.string.menu_edit);
		menu.add(0, CONTEXT_MENU_DELETE_ID, 0, R.string.menu_delete);
	}

    @Override
	public boolean onContextItemSelected(MenuItem item) {
		
    	AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    	Cursor cursor = (Cursor)getListAdapter().getItem(new Long(info.position).intValue());
    	Long rowId = new Long(cursor.getString(cursor.getColumnIndex(TaskDatabaseAdapter.KEY_ROWID)));
    	
    	switch(item.getItemId()) {
		case CONTEXT_MENU_EDIT_ID:

			Intent i = new Intent(this, TaskEdit.class);
	        i.putExtra(TaskDatabaseAdapter.KEY_ROWID, rowId);
	        startActivityForResult(i, ACTIVITY_EDIT);
	        return true;
	        
		case CONTEXT_MENU_DELETE_ID:
    		
	        mTasksDatabaseHelper.delete(rowId);
	        populateTasksList();
	        return true;
	        
		case CONTEXT_MENU_COMPLETE_ID:
			
			checkOffTask(info.position, info.targetView);
			return true;
	        
		case CONTEXT_MENU_REOPEN_ID:
			
			uncheckOffTask(info.position, info.targetView);
	        return true;    
		}
		return super.onContextItemSelected(item);
	}
	
	@Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        
    	super.onListItemClick(l, v, position, id);
        
        TextView textView = (TextView)v.findViewById(R.id.text1);
        showRunTaskPanel(textView.getText().toString());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, 
                                    Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        populateTasksList();
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

		private int getFirstVisiblePostionBeforeRefresh() {
			
			return taskList.getFirstVisiblePosition();
		}

		private void move(int from, int to) {
			
			Cursor cursor = (Cursor)getListAdapter().getItem(from);
        	int fromSeq = Integer.parseInt(cursor.getString(cursor.getColumnIndex(TaskDatabaseAdapter.KEY_SEQUENCE)));

        	cursor = (Cursor)getListAdapter().getItem(to);
        	int toSeq = Integer.parseInt(cursor.getString(cursor.getColumnIndex(TaskDatabaseAdapter.KEY_SEQUENCE)));
    		int toRowId = Integer.parseInt(cursor.getString(cursor.getColumnIndex(TaskDatabaseAdapter.KEY_ROWID)));
        	
        	mTasksDatabaseHelper.move(fromSeq, toRowId, toSeq);
		}
    };

	private Cursor taskListCursor;

    public class MyCount extends CountDownTimer{
	    
		public MyCount(long millisInFuture, long countDownInterval) {
    		super(millisInFuture, countDownInterval);
	    }
	    
	    @Override
	    public void onFinish() {
	    	
	        MediaPlayer mp = MediaPlayer.create(getBaseContext(), R.raw.freesoundprojectdotorg_32568__erh__indian_brass_pestle);
	        mp.start();
	        
	        vibrator.vibrate(1000); 
	        
	        while(mp.isPlaying()){
	        	
	        }
	        
	    	resetProgressControl(taskControlButton);
	    }
	    
	    @Override
	    public void onTick(long millisUntilFinished) {
	    	
	    	final DateFormat dateFormat = new SimpleDateFormat("mm:ss");
            String timeStr = dateFormat.format(new Date(millisUntilFinished));
	    	mTimeLeft.setText(timeStr);
	    	progressBar.incrementProgressBy(1);
	    }
    }
}
