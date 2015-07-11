package mi.rssKoelbl;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

public class ContentProviderDb extends ContentProvider{
	private DatabaseHelper dbHelper;
	private static final String AUTHORITY = "contentProviderAuthorities";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
	
	private static final String TAG = "RssDB";
	private static final String FEEDS_TABLE = "feeds";
	private static final String ARTICLES_TABLE = "articles";
	private static final String DATABASE_NAME = "rss";
	private static final int DATABASE_VERSION = 1;
	
	private static final String CREATE_TABLE_FEEDS = "create table " + FEEDS_TABLE + "(feed_id integer primary key autoincrement, title string, rss_url string not null, notify boolean not null default true, widgetable boolean not null default false );";
	private static final String CREATE_TABLE_ARTICLES = "create table " + ARTICLES_TABLE + "(article_id integer primary key autoincrement, feed_id integer not null, title string not null, published_at string, url string not null, already_read boolean not null default false, guid string);";
	
	private static class DatabaseHelper extends SQLiteOpenHelper {
		
		public DatabaseHelper(Context ctx) {
			super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(CREATE_TABLE_FEEDS);
			db.execSQL(CREATE_TABLE_ARTICLES);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion 
	                  + " to "
	                  + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + FEEDS_TABLE + "; DROP TABLE IF EXISTS " + ARTICLES_TABLE + ";");
            onCreate(db);
		}
	}
	
	@Override
	public boolean onCreate() {
		dbHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		String table = getTableName(uri);
		SQLiteDatabase database = dbHelper.getReadableDatabase();
		Cursor cursor = database.query(table, projection, selection, selectionArgs, null, null, sortOrder);
		return cursor;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		String table = getTableName(uri);
		SQLiteDatabase database = dbHelper.getWritableDatabase();
		long value = database.insert(table, null, initialValues);
		return Uri.withAppendedPath(CONTENT_URI, String.valueOf(value));
	}

	@Override
	public int delete(Uri uri, String where, String[] args) {
		String table = getTableName(uri);
		SQLiteDatabase database = dbHelper.getWritableDatabase();
		return database.delete(table, where, args);
	}

	@Override
	public int update(Uri uri, ContentValues values, String whereClause, String[] whereArgs) {
		String table = getTableName(uri);
		SQLiteDatabase database = dbHelper.getWritableDatabase();
		return database.update(table, values, whereClause, whereArgs);
	}

	private String getTableName(Uri uri) {
		String value = uri.getPath();
		value = value.replace("/", "");
		return value;
	}

}
