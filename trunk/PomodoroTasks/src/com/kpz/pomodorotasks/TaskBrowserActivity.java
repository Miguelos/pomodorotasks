package com.kpz.pomodorotasks;

import android.R.drawable;
import android.app.ListActivity;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.SimpleCursorAdapter.ViewBinder;

import com.kpz.pomodorotasks.TaskDatabaseAdapter.StatusType;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TaskBrowserActivity extends ListActivity {
    

	private static final String LOG_TAG = "PomodoroTasks";
	
	private static final int ACTIVITY_EDIT = 1;
	private static final int ACTIVITY_SET_OPTIONS = 2;
	
	private static final int MAIN_MENU_DELETE_ALL_ID = Menu.FIRST;
	private static final int MAIN_MENU_DELETED_COMPLETED_ID = MAIN_MENU_DELETE_ALL_ID + 1;
	private static final int MAIN_MENU_OPTIONS_ID = MAIN_MENU_DELETE_ALL_ID + 2;
	
	private static final int CONTEXT_MENU_EDIT_ID = Menu.FIRST + 10;
	private static final int CONTEXT_MENU_DELETE_ID = CONTEXT_MENU_EDIT_ID + 1;
	private static final int CONTEXT_MENU_COMPLETE_ID = CONTEXT_MENU_EDIT_ID + 2;
	private static final int CONTEXT_MENU_REOPEN_ID = CONTEXT_MENU_EDIT_ID + 3;
	
	private static final int ONE_SEC = 1000;

	private static final String TAG = "PomodoroTasks";	
    
	private ListView taskList;
    private TaskDatabaseAdapter mTasksDatabaseHelper;
	private TextView mTaskDescription;
	private ProgressBar mProgressBar;
	private TextView mTimeLeft;
	private int totalTime;
	private MyCount counter;
	private ImageButton taskControlButton;
	private Vibrator vibrator;
	private Cursor taskListCursor;
	private LinearLayout runTaskPanel;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
    }

	private void initView() {

		setContentView(R.layout.tasks_list);
        
        vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        
        initDatabaseHelper();        
        initTasksList();
        initAddTaskInput();
        initRunTaskPanel();
    	
        //registerForContextMenu(getListView());
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		
		// doing nothing to the view when screen orientation changes
	}
	
	private void initRunTaskPanel() {
		
		runTaskPanel = (LinearLayout)findViewById(R.id.runTaskPanel);
		runTaskPanel.setVisibility(View.GONE);
	}

	private void showRunTaskPanel(String taskDescription) {
		
		runTaskPanel.setVisibility(View.VISIBLE);
		
		taskControlButton = (ImageButton) findViewById(R.id.control_icon);
		hideButton = (ImageButton) findViewById(R.id.hide_panel_button);
		hideButton.setVisibility(View.VISIBLE);
		hideButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				runTaskPanel.setVisibility(View.GONE);
			}
		});
		
    	mTaskDescription = (TextView) findViewById(R.id.task_description);
    	mTaskDescription.setText(taskDescription);
    	mTimeLeft = (TextView) findViewById(R.id.time_left);
    	mProgressBar = (ProgressBar) findViewById(R.id.progress_horizontal);

    	resetTaskRun(taskControlButton);
    	
    	taskControlButton.setOnClickListener(new View.OnClickListener() {

    	    public void onClick(View view) {

    	    	totalTime = mTasksDatabaseHelper.fetchTaskDurationSetting() * 60;
	    		mProgressBar.setMax(totalTime);
	    		
    	    	if (counter != null && taskControlButton.getTag(R.string.TASK_CONTROL_BUTTON_STATE_TYPE).equals(R.string.TO_STOP_STATE)){
    	    		
    	    		resetTaskRun(taskControlButton);
    	    		
    	    	} else {

    	    		counter = new MyCount(totalTime * ONE_SEC, ONE_SEC, beepHandler);
    	    		//counter = new ProgressThread(handler);
        	        counter.start();
        	        
        	        hideButton.setVisibility(View.INVISIBLE);
        	        taskControlButton.setImageResource(drawable.ic_menu_close_clear_cancel);
        	        taskControlButton.setTag(R.string.TASK_CONTROL_BUTTON_STATE_TYPE, R.string.TO_STOP_STATE);
        	        adjustDimensionsToDefault(taskControlButton);
    	    	}
    	    }
    	});
    	
	}
	
    private void refreshTaskPanel() {
    	
    	String text = mTaskDescription.getText().toString();
    	
    	if (text == null || text.equals("")){
    		return;
    	}
    	
    	boolean exists = false;
    	final int count = getListAdapter().getCount();
        for (int i = count - 1; i >= 0; i--) {

        	Cursor cursor = (Cursor)getListAdapter().getItem(i);
        	String taskDescription = cursor.getString(cursor.getColumnIndex(TaskDatabaseAdapter.KEY_DESCRIPTION));
            if(taskDescription.equals(text)){
				exists = true;
            }
        }    
        
        if (!exists){
        	mTaskDescription.setText("");
        }
	}

	private void resetTaskRun(final ImageButton taskControlButton) {
		if (counter != null){
    		counter.cancel();
    	}
		
		resetProgressControl(taskControlButton);
	}

	private void resetProgressControl(final ImageButton taskControlButton) {
		resetTimeElapsed();
		mProgressBar.setProgress(0);
        taskControlButton.setImageResource(drawable.ic_media_play);
        taskControlButton.setTag(R.string.TASK_CONTROL_BUTTON_STATE_TYPE, R.string.TO_PLAY_STATE);
        adjustDimensionsToDefault(taskControlButton);
        hideButton.setVisibility(View.VISIBLE);
	}

	private void resetTimeElapsed() {
		
		mTimeLeft.setText(mTasksDatabaseHelper.fetchTaskDurationSetting() + ":00");
	}

	private boolean isRunTaskPanelInitialized() {
		return runTaskPanel.getVisibility() == View.VISIBLE;
	}
	
	private boolean isTaskRunning() {
		return mProgressBar.getProgress() != 0;
	}

	private void adjustDimensionsToDefault(final ImageButton taskControlButton) {
		Button leftButton = (Button) findViewById(R.id.left_text_button);
		taskControlButton.getLayoutParams().height = leftButton.getHeight();
		taskControlButton.getLayoutParams().width = leftButton.getWidth();
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
        
        // and an array of the fields we want to bind those fields to (in this case just task_description)
        int[] to = new int[]{R.id.task_description, R.id.taskRow};
        
        SimpleCursorAdapter taskListCursorAdapter = new SimpleCursorAdapter(getApplication(), R.layout.tasks_row, tasksCursor, from, to);
        taskListCursorAdapter.setViewBinder(new ViewBinder() {
			
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				
				if (view.getId() == R.id.taskRow){

					TextView textView = (TextView)view.findViewById(R.id.task_description);
					
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
        
        setListAdapter(taskListCursorAdapter);
        taskListCursor = taskListCursorAdapter.getCursor();
    }

	private void initTasksListViewContainer() {
		taskList = getListView();
//        taskList.setOnCreateContextMenuListener(this);
        ((TouchInterceptor) taskList).setDropListener(mDropListener);
        ((TouchInterceptor) taskList).setCheckOffListener(mCheckOffListener);
        taskList.setCacheColorHint(0);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
//		Log.d(LOG_TAG, "in on Resume");
//		int height = taskList.getHeight();
//        Resources res = getResources();
//        int normalItemHeight = res.getDimensionPixelSize(R.dimen.normal_height);
//        int newHeight = (height/normalItemHeight) * normalItemHeight; 
//        Log.d(LOG_TAG, "height:" + height + " new height:" + newHeight);
//        taskList.getLayoutParams().height = newHeight;
	}
	
	public void checkOffTask(int which, View targetView) {

		TextView textView = (TextView)targetView.findViewById(R.id.task_description);
    	textView.getPaint().setStrikeThruText(true);
    	
    	Cursor cursor = (Cursor)getListAdapter().getItem(which);
		int rowId = Integer.parseInt(cursor.getString(cursor.getColumnIndex(TaskDatabaseAdapter.KEY_ROWID)));
		mTasksDatabaseHelper.updateStatus(rowId, true);
		refreshTasksList();
	}
	
    private boolean refreshTasksList() {

    	return taskListCursor.requery();
	}

	public void uncheckOffTask(int which, View targetView) {

    	TextView textView = (TextView)targetView.findViewById(R.id.task_description);
    	textView.getPaint().setStrikeThruText(false);
    	
    	Cursor cursor = (Cursor)getListAdapter().getItem(which);
		int rowId = Integer.parseInt(cursor.getString(cursor.getColumnIndex(TaskDatabaseAdapter.KEY_ROWID)));
		mTasksDatabaseHelper.updateStatus(rowId, false);
		refreshTasksList();
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
 
        		String noteDescription = leftTextEdit.getText().toString().trim();
        		if (!noteDescription.equals("")){
        			createNewTask(noteDescription);
                    refreshTasksList();
                    resetAddTaskEntryDisplay(leftTextEdit);        			
        		}
            }

			private void resetAddTaskEntryDisplay(final EditText leftTextEdit) {
				leftTextEdit.setText("");
				leftTextEdit.requestFocus();
// to hide keyboard				
//                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(leftTextEdit.getWindowToken(), 0); 
// to display on-screen keyboard                 
//                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))  
//                .showSoftInput(editText, 0);  
			}

			private void createNewTask(final String noteDescription) {
            	mTasksDatabaseHelper.createTask(noteDescription);
			}
        });
	}
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        MenuItem menuItem = menu.add(0, MAIN_MENU_DELETED_COMPLETED_ID, 0, R.string.menu_delete_completed);
        menuItem.setIcon(drawable.ic_menu_agenda);
        
        menuItem = menu.add(0, MAIN_MENU_DELETE_ALL_ID, 0, R.string.menu_delete_all);
        menuItem.setIcon(drawable.ic_menu_delete);
        
        menuItem = menu.add(0, MAIN_MENU_OPTIONS_ID, 0, R.string.menu_options);
        menuItem.setIcon(drawable.ic_menu_preferences);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {

        case MAIN_MENU_DELETED_COMPLETED_ID:
        	mTasksDatabaseHelper.deleteCompleted();
	        refreshTasksList();
	        refreshTaskPanel();
        	return true;
        	
        case MAIN_MENU_DELETE_ALL_ID:
        	mTasksDatabaseHelper.deleteAll();
	        refreshTasksList();
	        refreshTaskPanel();
	        return true;
        	
        case MAIN_MENU_OPTIONS_ID:
        	Intent i = new Intent(this, SettingsActivity.class);
	        startActivityForResult(i, ACTIVITY_SET_OPTIONS);
        	return true;        	
        }
       
        return super.onMenuItemSelected(featureId, item);
    }

/*
******Disabling context menu for the time being as it's intefering with fling gesture!

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
		//taskList.invalidateViews();
		
		if (mCurrentActionCanceled) return;
		
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

			Intent i = new Intent(this, TaskEditActivity.class);
	        i.putExtra(TaskDatabaseAdapter.KEY_ROWID, rowId);
	        startActivityForResult(i, ACTIVITY_EDIT);
	        return true;
	        
		case CONTEXT_MENU_DELETE_ID:
    		
	        mTasksDatabaseHelper.delete(rowId);
	        refreshTasksList();
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
*/

	@Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        
    	super.onListItemClick(l, v, position, id);
        
        TextView textView = (TextView)v.findViewById(R.id.task_description);
        showRunTaskPanel(textView.getText().toString());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, 
                                    Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        
        switch (requestCode) {
			case ACTIVITY_SET_OPTIONS:
				
				if (isRunTaskPanelInitialized() && !isTaskRunning()){
					resetTimeElapsed();
				}
				break;
			case ACTIVITY_EDIT:
				refreshTasksList();
				break;
		}
    }
    
    private TouchInterceptor.DropListener mDropListener = new TouchInterceptor.DropListener() {

    	public void drop(int from, int to) {

    		move(from, to);
        	resetBottomMargin();
        	refreshTasksList();
        }

		private void resetBottomMargin() {
			LinearLayout.LayoutParams viewGroupParams = (LinearLayout.LayoutParams)getListView().getLayoutParams();
			if (viewGroupParams.bottomMargin != 0){
				viewGroupParams.bottomMargin = 0;
	    		getListView().setLayoutParams(viewGroupParams);				
			}
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

    // Define the Handler that receives messages from the thread and update the progress
    final Handler beepHandler = new Handler() {
        public void handleMessage(Message msg) {

    		MediaPlayer mp = MediaPlayer.create(getBaseContext(), R.raw.freesoundprojectdotorg_32568__erh__indian_brass_pestle);
	        mp.start();
	        
	        vibrator.vibrate(1000); 
	        
	        while(mp.isPlaying()){
	        	
	        }
	        
	    	resetProgressControl(taskControlButton);
        }
    };

	private ImageButton hideButton;
	
    public class MyCount extends CountDownTimer{
	    
		private Handler mHandler;

	    public MyCount(long millisInFuture, long countDownInterval, Handler handler) {
	    	super(millisInFuture + ONE_SEC, countDownInterval);
	    	this.mHandler = handler;
		}

		@Override
	    public void onTick(long millisUntilFinished) {
	    	
	    	incrementProgress(millisUntilFinished);
	    }

		private void incrementProgress(long millisUntilFinished) {
			
			final DateFormat dateFormat = new SimpleDateFormat("mm:ss");
            String timeStr = dateFormat.format(new Date(millisUntilFinished - ONE_SEC));
            mTimeLeft.setText(timeStr);
           	mProgressBar.incrementProgressBy(1);
           	
           	if (timeStr.equals("00:00")){
           		beep();
    	    	endTimer();
           	}
		}

		private void beep() {
			Message msg = mHandler.obtainMessage();
			Bundle emptyBundle = new Bundle();
			msg.setData(emptyBundle);
			mHandler.sendMessage(msg);
		}
		
		private void endTimer() {
			cancel();
		}
		@Override
		public void onFinish() {
			// do nothing
		}
    }
}
