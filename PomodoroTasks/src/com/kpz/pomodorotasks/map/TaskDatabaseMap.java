package com.kpz.pomodorotasks.map;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;
import android.util.Log;

public class TaskDatabaseMap {

	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	private static final String TAG = "PomodoroTasks";

	private static final String DATABASE_NAME = "pomodorotasks";
	
	private static final String TASKS_TABLE = "task";
	public static final String KEY_DESCRIPTION = "description";
	public static final String KEY_SEQUENCE = "sequence";
	public static final String KEY_ROWID = "_id";
	public static final String KEY_STATUS = "status";
	public static final String[] SELECTION_KEYS = { KEY_ROWID, KEY_DESCRIPTION, KEY_SEQUENCE, KEY_STATUS };
	
	public static final String KEY_CONFIG_NAME = "name";	
	public static final String KEY_CONFIG_VALUE = "value";	

	public enum StatusType {

		OPEN("Open"), COMPLETED("Completed");

		private String description;

		private StatusType(String desc) {
			this.description = desc;
		}

		public String getDescription() {
			return description;
		}

		public static boolean isOpen(String statusDesc) {
			return OPEN.description.equals(statusDesc);
		}

		public static boolean isCompleted(String statusDesc) {
			return COMPLETED.description.equals(statusDesc);
		}
	};
	
	public enum ConfigType {

		TASK_DURATION("25"),
		BREAK_DURATION("5"),
		EVERY_FOURTH_BREAK_DURATION("15"),
		CURRENT_POMODOROS("0"),
		PHONE_VIBRATE_FLAG("TRUE"), 
		NOTIFICATION_RINGTONE("content://settings/system/notification_sound");

		private String defaultValue;

		private ConfigType(String defaultVal) {
			this.defaultValue = defaultVal;
		}

		public String getDefaultValue() {
			return defaultValue;
		}
	};

	private static final String [] DATABASE_CREATE_LIST = 
					{"create table " + TASKS_TABLE 
									+ " (_id integer primary key autoincrement" + ", sequence integer" + ", status text not null default Open" + ", description text not null);" 
					};

	private static final int DATABASE_VERSION = 4;

	private final Context mCtx;
	private PreferenceMap preferenceMap;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {

			Log.d(TAG, "create stat: " + DATABASE_CREATE_LIST);
			for (String sql : DATABASE_CREATE_LIST) {
				db.execSQL(sql);
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + TASKS_TABLE);
			onCreate(db);
		}
	}

	/**
	 * Constructor - takes the context to allow the database to be
	 * opened/created
	 * 
	 * @param ctx
	 *            the Context within which to work
	 */
	public TaskDatabaseMap(Context ctx) {
		this.mCtx = ctx;
		preferenceMap = new PreferenceMap(PreferenceManager.getDefaultSharedPreferences(mCtx));
	}

	/**
	 * Open the tasks database. If it cannot be opened, try to create a new
	 * instance of the database. If it cannot be created, throw an exception to
	 * signal the failure
	 * 
	 * @return this (self reference, allowing this to be chained in an
	 *         initialization call)
	 * @throws SQLException
	 *             if the database could be neither opened or created
	 */
	public TaskDatabaseMap open() throws SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		mDbHelper.close();
	}
	
	public PreferenceMap getPreferences(){
		
		return preferenceMap;
	}

	public long createTask(String description) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_DESCRIPTION, description);

		long taskId = mDb.insert(TASKS_TABLE, null, initialValues);
		ContentValues args = new ContentValues();
		args.put(KEY_SEQUENCE, taskId);
		mDb.update(TASKS_TABLE, args, KEY_ROWID + "=" + taskId, null);

		return taskId;
	}

	public boolean delete(long rowId) {

		return mDb.delete(TASKS_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public boolean deleteAll() {
		return mDb.delete(TASKS_TABLE, null, null) > 0;
	}
	
	public boolean deleteCompleted() {
		return mDb.delete(TASKS_TABLE, KEY_STATUS + " = ? " , new String[] { StatusType.COMPLETED.getDescription() }) > 0;
	}

	/**
	 * Return a Cursor over the list of all tasks in the database
	 * 
	 * @return Cursor over all tasks
	 */
	public Cursor fetchAll() {

		return mDb.query(TASKS_TABLE, SELECTION_KEYS, null, null, null, null, "sequence");
	}

	/**
	 * Return a Cursor positioned at the task that matches the given rowId
	 * 
	 * @param rowId
	 *            id of task to retrieve
	 * @return Cursor positioned to matching task, if found
	 * @throws SQLException
	 *             if task could not be found/retrieved
	 */
	public Cursor fetch(long rowId) throws SQLException {

		Cursor mCursor = mDb.query(true, TASKS_TABLE, SELECTION_KEYS, KEY_ROWID + "=" + rowId, null, null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;

	}

	public boolean update(long rowId, String description) {
		ContentValues args = new ContentValues();
		args.put(KEY_DESCRIPTION, description);

		return mDb.update(TASKS_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public boolean updateStatus(int rowId, boolean completed) {

		ContentValues args = new ContentValues();
		args.put(KEY_STATUS, completed ? StatusType.COMPLETED.getDescription() : StatusType.OPEN.getDescription());

		return mDb.update(TASKS_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public void move(int fromSeq, int toRowId, int toSeq) {

		if (fromSeq == toSeq) {
			return;
		}

		if (fromSeq < toSeq) {

			ContentValues args = new ContentValues();
			args.put(KEY_SEQUENCE, toSeq);
			mDb.update(TASKS_TABLE, args, KEY_SEQUENCE + "=" + fromSeq, null);

			for (int i = 0; i < toSeq - fromSeq - 1; i++) {

				int fromSeq1 = fromSeq + i + 1;
				int toSeq1 = fromSeq1 - 1;
				args = new ContentValues();
				args.put(KEY_SEQUENCE, toSeq1);
				mDb.update(TASKS_TABLE, args, KEY_SEQUENCE + "=" + fromSeq1, null);
			}

			args = new ContentValues();
			args.put(KEY_SEQUENCE, toSeq - 1);
			mDb.update(TASKS_TABLE, args, KEY_ROWID + "=" + toRowId, null);

		} else {

			ContentValues args = new ContentValues();
			args.put(KEY_SEQUENCE, toSeq);
			mDb.update(TASKS_TABLE, args, KEY_SEQUENCE + "=" + fromSeq, null);

			for (int i = 0; i < fromSeq - toSeq - 1; i++) {

				int fromSeq1 = fromSeq - i - 1;
				int toSeq1 = fromSeq - i;
				args = new ContentValues();
				args.put(KEY_SEQUENCE, toSeq1);
				mDb.update(TASKS_TABLE, args, KEY_SEQUENCE + "=" + fromSeq1, null);
			}

			args = new ContentValues();
			args.put(KEY_SEQUENCE, toSeq + 1);
			mDb.update(TASKS_TABLE, args, KEY_ROWID + "=" + toRowId, null);

		}
	}

	public class PreferenceMap {
		
		private SharedPreferences applicationPreferences;

		public PreferenceMap(SharedPreferences sharedPreferences) {
			this.applicationPreferences = sharedPreferences;
		}

		public int getDurationPreference(ConfigType configType) {
			
			return fetchValueInInteger(configType);
		}
		
		public boolean updateDurationPreference(ConfigType configType, int duration) {

			return updateValue(configType, Integer.toString(duration));
		}
		
		public int getCurrentPomodoros() {
			
			return fetchValueInInteger(ConfigType.CURRENT_POMODOROS);
		}

		public boolean updateCurrentPomodoros(int count) {

			return updateValue(ConfigType.CURRENT_POMODOROS, Integer.toString(count));
		}		

		public boolean notifyPhoneVibrate() {
			return fetchValueInBoolean(ConfigType.PHONE_VIBRATE_FLAG);
		}

		public String getRingtone() {

			return applicationPreferences.getString(ConfigType.NOTIFICATION_RINGTONE.name(), ConfigType.NOTIFICATION_RINGTONE.defaultValue);
		}

		public void updateRingtone(String value) {
			updateValue(ConfigType.NOTIFICATION_RINGTONE, value);
		}
		
		private int fetchValueInInteger(ConfigType configType) {
			return new Integer(applicationPreferences.getString(configType.name(), configType.defaultValue)).intValue();
		}
		
		private boolean fetchValueInBoolean(ConfigType configType) {
			return applicationPreferences.getBoolean(configType.name(), new Boolean(configType.defaultValue));
		}

		private boolean updateValue(ConfigType configType, String value) {

			Editor editor = applicationPreferences.edit();
			editor.putString(configType.name(), value);
			return editor.commit(); 
		}
	}
}
