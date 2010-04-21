package com.kpz.pomodorotasks.map;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;

public class TaskDatabaseMap {

	private static TaskDatabaseMap mInstance = new TaskDatabaseMap();

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
		CURRENT_POMODOROS("5"),
		PHONE_VIBRATE_FLAG("TRUE"), 
		LEGACY_CONFIG_UPGRADE_FLAG("FALSE"), 
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

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {

			for (String sql : DATABASE_CREATE_LIST) {
				db.execSQL(sql);
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + TASKS_TABLE);
			onCreate(db);
		}
	}

	private DatabaseHelper mDbHelper;
	private Context mCtx;
	private PreferenceMap preferenceMap;

	private TaskDatabaseMap() {
	}
	
	public static TaskDatabaseMap getInstance(Context ctx) {
		
		if (mInstance.mCtx == null){
				
			mInstance.mCtx = ctx;
			mInstance.mDbHelper = new DatabaseHelper(mInstance.mCtx);
			mInstance.preferenceMap = mInstance.new PreferenceMap(PreferenceManager.getDefaultSharedPreferences(mInstance.mCtx));
			if (!mInstance.preferenceMap.legacyUpgradeComplete()){
				mInstance.upgradeLegacyData();			
			}
		}

		return mInstance;
	}

	private void upgradeLegacyData() {
		
		try{

			SQLiteDatabase connection = mDbHelper.getReadableDatabase();
			Cursor cursor = connection.query(true, "config", new String[] {"value"}, "name" + "= '" +  "CURRENT_POMODOROS" + "'", null, null, null, null, null);
			if (cursor != null) {
				cursor.moveToFirst();
			}
			int currentPomodoroCount = Integer.parseInt(cursor.getString(cursor.getColumnIndex("value")));
			preferenceMap.updateCurrentPomodoros(currentPomodoroCount);

			cursor = connection.query(true, "config", new String[] {"value"}, "name" + "= '" +  "TIME_DURATION" + "'", null, null, null, null, null);
			if (cursor != null) {
				cursor.moveToFirst();
			}
			int taskDuration = Integer.parseInt(cursor.getString(cursor.getColumnIndex("value")));
			preferenceMap.updateDurationPreference(ConfigType.TASK_DURATION, taskDuration);

			
		} catch (SQLiteException ex) {
			
			// Exception expected when its not an upgrade from previous version and the config table was not found.
		} finally {
			// Set one time upgrade to complete 
			preferenceMap.setLegacyUpgradeComplete(true);
		}
	}

	private SQLiteDatabase getConnection() {
		return mDbHelper.getWritableDatabase();
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

		long taskId = getConnection().insert(TASKS_TABLE, null, initialValues);
		ContentValues args = new ContentValues();
		args.put(KEY_SEQUENCE, taskId);
		getConnection().update(TASKS_TABLE, args, KEY_ROWID + "=" + taskId, null);

		return taskId;
	}

	public boolean delete(long rowId) {

		return getConnection().delete(TASKS_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public boolean deleteAll() {
		return getConnection().delete(TASKS_TABLE, null, null) > 0;
	}
	
	public boolean deleteCompleted() {
		return getConnection().delete(TASKS_TABLE, KEY_STATUS + " = ? " , new String[] { StatusType.COMPLETED.getDescription() }) > 0;
	}

	/**
	 * Return a Cursor over the list of all tasks in the database
	 * 
	 * @return Cursor over all tasks
	 */
	public Cursor fetchAll() {

		return getConnection().query(TASKS_TABLE, SELECTION_KEYS, null, null, null, null, "sequence");
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

		Cursor mCursor = getConnection().query(true, TASKS_TABLE, SELECTION_KEYS, KEY_ROWID + "=" + rowId, null, null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;

	}

	public boolean update(long rowId, String description) {
		ContentValues args = new ContentValues();
		args.put(KEY_DESCRIPTION, description);

		return getConnection().update(TASKS_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public boolean updateStatus(int rowId, boolean completed) {

		ContentValues args = new ContentValues();
		args.put(KEY_STATUS, completed ? StatusType.COMPLETED.getDescription() : StatusType.OPEN.getDescription());

		return getConnection().update(TASKS_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public void move(int fromSeq, int toRowId, int toSeq) {

		if (fromSeq == toSeq) {
			return;
		}

		if (fromSeq < toSeq) {

			ContentValues args = new ContentValues();
			args.put(KEY_SEQUENCE, toSeq);
			getConnection().update(TASKS_TABLE, args, KEY_SEQUENCE + "=" + fromSeq, null);

			for (int i = 0; i < toSeq - fromSeq - 1; i++) {

				int fromSeq1 = fromSeq + i + 1;
				int toSeq1 = fromSeq1 - 1;
				args = new ContentValues();
				args.put(KEY_SEQUENCE, toSeq1);
				getConnection().update(TASKS_TABLE, args, KEY_SEQUENCE + "=" + fromSeq1, null);
			}

			args = new ContentValues();
			args.put(KEY_SEQUENCE, toSeq - 1);
			getConnection().update(TASKS_TABLE, args, KEY_ROWID + "=" + toRowId, null);

		} else {

			ContentValues args = new ContentValues();
			args.put(KEY_SEQUENCE, toSeq);
			getConnection().update(TASKS_TABLE, args, KEY_SEQUENCE + "=" + fromSeq, null);

			for (int i = 0; i < fromSeq - toSeq - 1; i++) {

				int fromSeq1 = fromSeq - i - 1;
				int toSeq1 = fromSeq - i;
				args = new ContentValues();
				args.put(KEY_SEQUENCE, toSeq1);
				getConnection().update(TASKS_TABLE, args, KEY_SEQUENCE + "=" + fromSeq1, null);
			}

			args = new ContentValues();
			args.put(KEY_SEQUENCE, toSeq + 1);
			getConnection().update(TASKS_TABLE, args, KEY_ROWID + "=" + toRowId, null);

		}
	}

	public class PreferenceMap {
		
		private SharedPreferences applicationPreferences;

		public PreferenceMap(SharedPreferences sharedPreferences) {
			this.applicationPreferences = sharedPreferences;
		}

		public boolean legacyUpgradeComplete() {
			return fetchValueInBoolean(ConfigType.LEGACY_CONFIG_UPGRADE_FLAG);
		}

		public boolean setLegacyUpgradeComplete(boolean b) {
			
			return updateValueInBoolean(ConfigType.LEGACY_CONFIG_UPGRADE_FLAG, Boolean.TRUE);
		}
		
		public int getDurationPreference(ConfigType configType) {
			
			return fetchValueInInteger(configType);
		}
		
		public boolean updateDurationPreference(ConfigType configType, int duration) {

			return updateValueInString(configType, Integer.toString(duration));
		}
		
		public int getCurrentPomodoros() {
			
			return fetchValueInInteger(ConfigType.CURRENT_POMODOROS);
		}

		public boolean updateCurrentPomodoros(int count) {

			return updateValueInString(ConfigType.CURRENT_POMODOROS, Integer.toString(count));
		}		

		public boolean notifyPhoneVibrate() {
			return fetchValueInBoolean(ConfigType.PHONE_VIBRATE_FLAG);
		}

		public String getRingtone() {

			return applicationPreferences.getString(ConfigType.NOTIFICATION_RINGTONE.name(), ConfigType.NOTIFICATION_RINGTONE.defaultValue);
		}

		public void updateRingtone(String value) {
			updateValueInString(ConfigType.NOTIFICATION_RINGTONE, value);
		}
		
		private int fetchValueInInteger(ConfigType configType) {
			return new Integer(applicationPreferences.getString(configType.name(), configType.defaultValue)).intValue();
		}
		
		private boolean fetchValueInBoolean(ConfigType configType) {
			return applicationPreferences.getBoolean(configType.name(), new Boolean(configType.defaultValue));
		}

		private boolean updateValueInString(ConfigType configType, String value) {

			Editor editor = applicationPreferences.edit();
			editor.putString(configType.name(), value);
			return editor.commit(); 
		}
		
		private boolean updateValueInBoolean(ConfigType configType, Boolean value) {

			Editor editor = applicationPreferences.edit();
			editor.putBoolean(configType.name(), value);
			return editor.commit(); 
		}
	}
}
