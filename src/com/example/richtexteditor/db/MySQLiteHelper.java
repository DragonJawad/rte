package com.example.richtexteditor.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Class MySQLiteHelper.
 */
public class MySQLiteHelper extends SQLiteOpenHelper {

  public static final String TABLE_TEXT_BLOCKS = "sd_text_blocks";
  
  /** local, int */
  public static final String COLUMN_ID = "_id";
  /** local, long */
  public static final String COLUMN_BLOCK_TIME = "block_time";
  /** local, int */
  public static final String COLUMN_BLOCK_VERSION = "block_version";
  /** from server, String */
  public static final String COLUMN_BLOCK_ID = "block_id";
  /** from server, String */
  public static final String COLUMN_BLOCK_CONTENT = "block_content";

  private static final String DATABASE_NAME = "sd_texts.db";
  private static final int DATABASE_VERSION = 1;

  private static final String DATABASE_CREATE = "create table " 
      + TABLE_TEXT_BLOCKS + "("
      + COLUMN_ID + " integer primary key autoincrement, "
      + COLUMN_BLOCK_TIME + " timestamp default current_timestamp, "
      + COLUMN_BLOCK_VERSION + " integer default 0, "
      + COLUMN_BLOCK_ID + " text not null, "
      + COLUMN_BLOCK_CONTENT + " text not null"
      + ");";

  /**
   * Instantiates a new SDSQLiteHelper.
   * 
   * @param context
   *          the context
   */
  public MySQLiteHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  /* (non-Javadoc)
   * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
   */
  @Override
  public void onCreate(SQLiteDatabase database) {
    database.execSQL(DATABASE_CREATE);
  }

  /* (non-Javadoc)
   * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
   */
  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    Log.i(MySQLiteHelper.class.getName(), "Upgrading database from version "
        + oldVersion + " to " + newVersion
        + ", which will destroy all old data");

    db.execSQL("DROP TABLE IF EXISTS " + TABLE_TEXT_BLOCKS);
    onCreate(db);
  }
}