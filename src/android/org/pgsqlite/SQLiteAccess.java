package org.pgsqlite;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class SQLiteAccess {
	private static final String TAG = "SQLiteAccess";
	private Context context;

	private SQLiteAccess(Context context) {
		this.context = context;

	}

	private static SQLiteAccess instance;

	public static SQLiteAccess getInstance(Context context) {
		if (instance == null) {
			instance = new SQLiteAccess(context);
		}
		return instance;
	}

	private HashMap<String, DbAccess> _dbs = new HashMap<String, SQLiteAccess.DbAccess>();

	public SQLiteDatabase requestDb(String dbName) {
		synchronized (this) {
			DbAccess dbAccess = _dbs.get(dbName);
			if (dbAccess == null) {
				File dbfile = this.context.getDatabasePath(dbName);

				if (!dbfile.exists()) {
					dbfile.getParentFile().mkdirs();
				}
				Log.v("info", "Open sqlite db: " + dbfile.getAbsolutePath());

				SQLiteDatabase mydb = SQLiteDatabase.openOrCreateDatabase(
						dbfile, null);

				dbAccess = new DbAccess(mydb);
				_dbs.put(dbName, dbAccess);
			}
			dbAccess.semaphore++;
			return dbAccess.db;
		}
	}

	public void releaseDb(String dbName) {
		synchronized (this) {
			DbAccess dbAccess = _dbs.get(dbName);
			if (dbAccess == null) {
				Log.w(TAG, "db access already removed");
			} else {
				dbAccess.semaphore--;
				if (dbAccess.semaphore <= 0){
					dbAccess.db.close();
					_dbs.remove(dbName);
				}
			}
		}
	}

	private class DbAccess {
		public DbAccess(SQLiteDatabase db) {
			this.db = db;
		}

		public volatile int semaphore = 0;
		public SQLiteDatabase db;
	}
}
