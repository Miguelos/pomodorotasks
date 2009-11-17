package com.kpz.pomodorotasks;

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
        //setTheme(android.R.style.Theme_Light);
        
//        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
//        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.notes_list);
        
        setContentView(R.layout.notes_list);
        
        Log.d(TAG, "before caling getListView()");

        mTrackList = getListView();
        mTrackList.setOnCreateContextMenuListener(this);
        
        Log.d(TAG, "list type: " + mTrackList.getClass());
//        if (mEditMode) {
            ((TouchInterceptor) mTrackList).setDropListener(mDropListener);
            //((TouchInterceptor) mTrackList).setRemoveListener(mRemoveListener);
            mTrackList.setCacheColorHint(0);
//        } else {
//            mTrackList.setTextFilterEnabled(true);
//        }
        
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
    
    private TouchInterceptor.DropListener mDropListener =
        new TouchInterceptor.DropListener() {
        public void drop(int from, int to) {
        	
        	Log.d(TAG, "inside DropListener.drop method");
        	
//            if (mTrackCursor instanceof NowPlayingCursor) {
//                // update the currently playing list
//                NowPlayingCursor c = (NowPlayingCursor) mTrackCursor;
//                c.moveItem(from, to);
//                ((TrackListAdapter)getListAdapter()).notifyDataSetChanged();
//                getListView().invalidateViews();
//                mDeletedOneRow = true;
//            } else {
//                // update a saved playlist
//                Uri baseUri = MediaStore.Audio.Playlists.Members.getContentUri("external",
//                        Long.valueOf(mPlaylist));
//                ContentValues values = new ContentValues();
//                String where = MediaStore.Audio.Playlists.Members._ID + "=?";
//                String [] wherearg = new String[1];
//                ContentResolver res = getContentResolver();
//                
//                int colidx = mTrackCursor.getColumnIndexOrThrow(
//                        MediaStore.Audio.Playlists.Members.PLAY_ORDER);
//                if (from < to) {
//                    // move the item to somewhere later in the list
//                    mTrackCursor.moveToPosition(to);
//                    long toidx = mTrackCursor.getLong(colidx);
//                    mTrackCursor.moveToPosition(from);
//                    values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, toidx);
//                    wherearg[0] = mTrackCursor.getString(0);
//                    res.update(baseUri, values, where, wherearg);
//                    for (int i = from + 1; i <= to; i++) {
//                        mTrackCursor.moveToPosition(i);
//                        values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, i - 1);
//                        wherearg[0] = mTrackCursor.getString(0);
//                        res.update(baseUri, values, where, wherearg);
//                    }
//                } else if (from > to) {
//                    // move the item to somewhere earlier in the list
//                    mTrackCursor.moveToPosition(to);
//                    long toidx = mTrackCursor.getLong(colidx);
//                    mTrackCursor.moveToPosition(from);
//                    values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, toidx);
//                    wherearg[0] = mTrackCursor.getString(0);
//                    res.update(baseUri, values, where, wherearg);
//                    for (int i = from - 1; i >= to; i--) {
//                        mTrackCursor.moveToPosition(i);
//                        values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, i + 1);
//                        wherearg[0] = mTrackCursor.getString(0);
//                        res.update(baseUri, values, where, wherearg);
//                    }
//                }
//            }
        }
    };

    
    private void fillData() {
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
    
    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
    	// TODO Auto-generated method stub
    	return super.onCreateView(name, context, attrs);
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

//        @Override
//        public View getView(int position, View convertView, ViewGroup parent) {
//
//        	
//        	
//            // A ViewHolder keeps references to children views to avoid unneccessary calls
//            // to findViewById() on each row.
//            ViewHolder holder;
//
//            // When convertView is not null, we can reuse it directly, there is no need
//            // to reinflate it. We only inflate a new View when the convertView supplied
//            // by ListView is null.
//            if (convertView == null) {
//                convertView = mInflater.inflate(R.layout.list_item_icon_text, null);
//
//                // Creates a ViewHolder and store references to the two children views
//                // we want to bind data to.
//                holder = new ViewHolder();
//                holder.text = (TextView) convertView.findViewById(R.id.text);
//                holder.icon = (ImageView) convertView.findViewById(R.id.icon);
//
//                convertView.setTag(holder);
//            } else {
//                // Get the ViewHolder back to get fast access to the TextView
//                // and the ImageView.
//                holder = (ViewHolder) convertView.getTag();
//            }
//
//            // Bind the data efficiently with the holder.
//            holder.text.setText(DATA[position]);
//            holder.icon.setImageBitmap((position & 1) == 1 ? mIcon1 : mIcon2);
//
//            return convertView;
//        
//        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View v = super.newView(context, cursor, parent);
            ImageView iv = (ImageView) v.findViewById(R.id.icon);
//            if (mActivity.mEditMode) {
                iv.setVisibility(View.VISIBLE);
                iv.setImageResource(R.drawable.ic_mp_move);
//            } else {
//                iv.setVisibility(View.GONE);
//            }
            
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
        public void bindView(View view, Context context, Cursor cursor) {
            
        	super.bindView(view, context, cursor);
        	
//            ViewHolder vh = (ViewHolder) view.getTag();
//            
//            cursor.copyStringToBuffer(mTitleIdx, vh.buffer1);
//            vh.line1.setText(vh.buffer1.data, 0, vh.buffer1.sizeCopied);
//            
//            int secs = cursor.getInt(mDurationIdx) / 1000;
//            if (secs == 0) {
//                vh.duration.setText("");
//            } else {
//                vh.duration.setText(MusicUtils.makeTimeString(context, secs));
//            }
//            
//            final StringBuilder builder = mBuilder;
//            builder.delete(0, builder.length());
//
//            String name = cursor.getString(mArtistIdx);
//            if (name == null || name.equals(MediaFile.UNKNOWN_STRING)) {
//                builder.append(mUnknownArtist);
//            } else {
//                builder.append(name);
//            }
//            int len = builder.length();
//            if (vh.buffer2.length < len) {
//                vh.buffer2 = new char[len];
//            }
//            builder.getChars(0, len, vh.buffer2, 0);
//            vh.line2.setText(vh.buffer2, 0, len);
//
//            ImageView iv = vh.play_indicator;
//            long id = -1;
//            if (MusicUtils.sService != null) {
//                // TODO: IPC call on each bind??
//                try {
//                    if (mIsNowPlaying) {
//                        id = MusicUtils.sService.getQueuePosition();
//                    } else {
//                        id = MusicUtils.sService.getAudioId();
//                    }
//                } catch (RemoteException ex) {
//                }
//            }
            
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
}
