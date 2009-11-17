package com.kpz.pomodorotasks;

import java.util.Arrays;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.os.Bundle;
import android.util.AttributeSet;
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
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class PomodoroTasks extends ListActivity implements View.OnCreateContextMenuListener {
    
	private static final String TAG = "PomodoroTasks";
	
	private ListView mTrackList;
	
	private static final int ACTIVITY_CREATE=0;
    private static final int ACTIVITY_EDIT=1;
    
    private static final int INSERT_ID = Menu.FIRST;
    private static final int DELETE_ID = Menu.FIRST + 1;

    private NotesDbAdapter mDbHelper;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notes_list);
        
        mTrackList = getListView();
        mTrackList.setOnCreateContextMenuListener(this);
        
        Log.d(TAG, "list type: " + mTrackList.getClass());
        ((TouchInterceptor) mTrackList).setDropListener(mDropListener);
        //((TouchInterceptor) mTrackList).setRemoveListener(mRemoveListener);
        mTrackList.setCacheColorHint(0);
        
        final EditText leftTextEdit = (EditText) findViewById(R.id.left_text_edit);
        
        Button leftButton = (Button) findViewById(R.id.left_text_button);

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
        
        mDbHelper = new NotesDbAdapter(this);
        mDbHelper.open();
        
        fillData();
        
        registerForContextMenu(getListView());
    }
    
    private void fillData() {
    	
    	Log.d(TAG, "inside fillData");

        // Get all of the rows from the database and create the item list
    	Cursor notesCursor = mDbHelper.fetchAllNotes();
        startManagingCursor(notesCursor);
        
        // Create an array to specify the fields we want to display in the list (only TITLE)
        String[] from = new String[]{NotesDbAdapter.KEY_TITLE};
        
        // and an array of the fields we want to bind those fields to (in this case just text1)
        int[] to = new int[]{R.id.text1};
        
        // Now create a simple cursor adapter and set it to display
//        SimpleCursorAdapter notes = 
//        	    new SimpleCursorAdapter(this, R.layout.notes_row, notesCursor, from, to);
        
        TrackListAdapter notes = new TrackListAdapter(this, getApplication(), R.layout.notes_row, notesCursor, from, to);

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
    
    static class TrackListAdapter extends SimpleCursorAdapter implements SectionIndexer {

    	private final StringBuilder mBuilder = new StringBuilder();
        
    	private LayoutInflater mInflater;
        private AlphabetIndexer mIndexer;
        
        private PomodoroTasks mActivity = null;
        private String mConstraint = null;
        private boolean mConstraintIsValid = false;
        
        static class ViewHolder {
            TextView line1;
            TextView line2;
            TextView duration;
            ImageView play_indicator;
            CharArrayBuffer buffer1;
            char [] buffer2;
        }

        TrackListAdapter(PomodoroTasks currentactivity, Context context, 
                int layout, Cursor cursor, String[] from, int[] to
                ) {
            super(context, layout, cursor, from, to);
            mActivity = currentactivity;
        }
        
        public void setActivity(PomodoroTasks newactivity) {
            mActivity = newactivity;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View v = super.newView(context, cursor, parent);
            ImageView iv = (ImageView) v.findViewById(R.id.icon);
            iv.setVisibility(View.VISIBLE);
            iv.setImageResource(R.drawable.ic_mp_move);
//            iv.setVisibility(View.GONE);
            
//            ViewHolder vh = new ViewHolder();
//            vh.line1 = (TextView) v.findViewById(R.id.line1);
//            vh.line2 = (TextView) v.findViewById(R.id.line2);
//            vh.duration = (TextView) v.findViewById(R.id.duration);
//            vh.play_indicator = (ImageView) v.findViewById(R.id.play_indicator);
//            vh.buffer1 = new CharArrayBuffer(100);
//            vh.buffer2 = new char[200];
//            v.setTag(vh);
            return v;
        }
        
        @Override
        public void changeCursor(Cursor cursor) {
        	
        	super.changeCursor(cursor);
//            if (cursor != mActivity.mTrackCursor) {
//                mActivity.mTrackCursor = cursor;
//                super.changeCursor(cursor);
//                getColumnIndices(cursor);
//            }
        }
        
        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        	return super.runQueryOnBackgroundThread(constraint);
//            String s = constraint.toString();
//            if (mConstraintIsValid && (
//                    (s == null && mConstraint == null) ||
//                    (s != null && s.equals(mConstraint)))) {
//                return getCursor();
//            }
//            Cursor c = mActivity.getTrackCursor(mQueryHandler, s, false);
//            mConstraint = s;
//            mConstraintIsValid = true;
//            return c;
        }
        
        // SectionIndexer methods
        
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
    
    private TouchInterceptor.DropListener mDropListener =
        new TouchInterceptor.DropListener() {
        public void drop(int from, int to) {
        	
        	Log.d(TAG, "from: " + from + " to:" + to);
        	
        	Cursor cursor = (Cursor)getListAdapter().getItem(from);
        	String fromSeq = cursor.getString(cursor.getColumnIndex(NotesDbAdapter.KEY_SEQUENCE));
        	String fromRowId = cursor.getString(cursor.getColumnIndex(NotesDbAdapter.KEY_ROWID));
        	String fromTitle = cursor.getString(cursor.getColumnIndex(NotesDbAdapter.KEY_TITLE));
        	
        	cursor = (Cursor)getListAdapter().getItem(to);
        	String toSeq = cursor.getString(cursor.getColumnIndex(NotesDbAdapter.KEY_SEQUENCE));
        	String toRowId = cursor.getString(cursor.getColumnIndex(NotesDbAdapter.KEY_ROWID));
        	String toTitle = cursor.getString(cursor.getColumnIndex(NotesDbAdapter.KEY_TITLE));
        	
        	Log.d(TAG, fromSeq + fromRowId + fromTitle);
        	Log.d(TAG, toSeq + toRowId + toTitle);
        	
        	mDbHelper.move(fromRowId, fromSeq, toRowId, toSeq);
        	
        	fillData();
        	
        	getListView().setSelection(Integer.parseInt(toSeq));
        }
    };
}
