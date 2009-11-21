/*
 * Copyright (C) 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.kpz.pomodorotasks;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class TaskDatabaseAdapter {

	private static final String TAG = "PomodoroTasks";
	
    public static final String KEY_TITLE = "title";
    public static final String KEY_BODY = "body";
    public static final String KEY_SEQUENCE = "sequence";
    public static final String KEY_ROWID = "_id";

    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    
    private static final String DATABASE_TABLE = "tasks";
    private static final String DATABASE_NAME = "pomodorotasks";
    
    private static final String DATABASE_CREATE =
            "create table " + DATABASE_TABLE + " (_id integer primary key autoincrement"
    				+ ", sequence integer"
    				+ ", title text not null" 
                    + ", body text not null);";

    private static final int DATABASE_VERSION = 2;

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS tasks");
            onCreate(db);
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public TaskDatabaseAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    /**
     * Open the tasks database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public TaskDatabaseAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }
    
    public void close() {
        mDbHelper.close();
    }


    /**
     * Create a new task using the title and body provided. If the task is
     * successfully created return the new rowId for that task, otherwise return
     * a -1 to indicate failure.
     * 
     * @param title the title of the task
     * @param body the body of the task
     * @return rowId or -1 if failed
     */
    public long createTask(String title, String body) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_TITLE, title);
        initialValues.put(KEY_BODY, body);

        long taskId = mDb.insert(DATABASE_TABLE, null, initialValues);
        ContentValues args = new ContentValues();
        args.put(KEY_SEQUENCE, taskId);
        mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + taskId, null);
        
		return taskId;
    }

    /**
     * Delete the task with the given rowId
     * 
     * @param rowId id of task to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteTask(long rowId) {

        return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }

    /**
     * Return a Cursor over the list of all tasks in the database
     * 
     * @return Cursor over all tasks
     */
    public Cursor fetchAllTasks() {

        return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_TITLE,
                KEY_BODY, KEY_SEQUENCE}, null, null, null, null, "sequence");
    }

    /**
     * Return a Cursor positioned at the task that matches the given rowId
     * 
     * @param rowId id of task to retrieve
     * @return Cursor positioned to matching task, if found
     * @throws SQLException if task could not be found/retrieved
     */
    public Cursor fetchTask(long rowId) throws SQLException {

        Cursor mCursor =

                mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID,
                        KEY_TITLE, KEY_BODY}, KEY_ROWID + "=" + rowId, null,
                        null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }

    /**
     * Update the task using the details provided. The task to be updated is
     * specified using the rowId, and it is altered to use the title and body
     * values passed in
     * 
     * @param rowId id of task to update
     * @param title value to set task title to
     * @param body value to set task body to
     * @return true if the task was successfully updated, false otherwise
     */
    public boolean updateTask(long rowId, String title, String body) {
        ContentValues args = new ContentValues();
        args.put(KEY_TITLE, title);
        args.put(KEY_BODY, body);

        return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }

	public void move(String fromRowIdStr, String fromSeqStr, String toRowIdStr,
			String toSeqStr) {

		
		int fromSeq = Integer.parseInt(fromSeqStr);
		int toSeq = Integer.parseInt(toSeqStr);
		int fromRowId = Integer.parseInt(fromRowIdStr);
		int toRowId = Integer.parseInt(toRowIdStr);

		if (fromSeq == toSeq){
			return;
		}
		
		if(fromSeq < toSeq){
			
			ContentValues args = new ContentValues();
	        args.put(KEY_SEQUENCE, toSeq);
	        mDb.update(DATABASE_TABLE, args, KEY_SEQUENCE + "=" + fromSeq, null);
	        
	        
	        for (int i = 0; i <  toSeq - fromSeq - 1; i++) {
	        	
	            int fromSeq1 = fromSeq + i + 1;
	            int toSeq1 = fromSeq1 - 1;
	        	args = new ContentValues();
	        	args.put(KEY_SEQUENCE, toSeq1);
				mDb.update(DATABASE_TABLE, args, KEY_SEQUENCE + "=" + fromSeq1, null);	
			}
	        
	        args = new ContentValues();
	    	args.put(KEY_SEQUENCE, toSeq - 1);
			mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + toRowId, null);
			
		} else {
		
	        ContentValues args = new ContentValues();
	        args.put(KEY_SEQUENCE, toSeq);
	        mDb.update(DATABASE_TABLE, args, KEY_SEQUENCE + "=" + fromSeq, null);
	        
	        
	        for (int i = 0; i < fromSeq - toSeq - 1; i++) {
	        	
	            int fromSeq1 = fromSeq - i - 1;
	            int toSeq1 = fromSeq - i;
	        	args = new ContentValues();
	        	args.put(KEY_SEQUENCE, toSeq1);
				mDb.update(DATABASE_TABLE, args, KEY_SEQUENCE + "=" + fromSeq1, null);	
			}
	        
	        args = new ContentValues();
	    	args.put(KEY_SEQUENCE, toSeq + 1);
			mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + toRowId, null);
		
		}
	}
}
