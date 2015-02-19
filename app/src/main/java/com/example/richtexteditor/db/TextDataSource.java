package com.example.richtexteditor.db;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/**
 * Class TextBlocksDataSource. Das DAO.
 */
public class TextDataSource {

  private SQLiteDatabase database;
  private MySQLiteHelper dbHelper;
  private String[] allColumns = { MySQLiteHelper.COLUMN_ID, MySQLiteHelper.COLUMN_BLOCK_TIME, MySQLiteHelper.COLUMN_BLOCK_VERSION, MySQLiteHelper.COLUMN_BLOCK_ID, MySQLiteHelper.COLUMN_BLOCK_CONTENT };
  private String[] idColumns = { MySQLiteHelper.COLUMN_ID, MySQLiteHelper.COLUMN_BLOCK_ID };
  private String[] contentColumn = { MySQLiteHelper.COLUMN_BLOCK_CONTENT };
  SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 

  /**
   * Instantiates a new text blocks data source.
   * 
   * @param context
   */
  public TextDataSource(Context context) {
    dbHelper = new MySQLiteHelper(context);
  }

  /**
   * Open DB.
   * 
   * @throws SQLException
   */
  public void open() throws SQLException {
    database = dbHelper.getWritableDatabase();
  }

  /**
   * Close DB.
   */
  public void close() {
    dbHelper.close();
  }

  /**
   * Passing database status to MainActivity.
   * 
   * @return database.isOpen
   */
  public boolean isOpen() {
    return database.isOpen();
  }
 

  /**
   * Checks for text block.
   * 
   * @param blockId
   * @return true, if successful
   */
  public boolean hasTextBlock(String blockId) {
    if((database == null) || (!database.isOpen())) {
      return false;
    }

    Cursor cursor = database.query(MySQLiteHelper.TABLE_TEXT_BLOCKS, idColumns,
        MySQLiteHelper.COLUMN_BLOCK_ID + " = " + "'" + blockId + "'", null, null, null, null);

    if (cursor == null) {
      return false;
    }

    if (cursor.getCount() > 0) {
      cursor.close();
      return true;
    } else {
      cursor.close();
      return false;
    }
  }

  /**
   * Gets the text block.
   * 
   * @param blockId
   * @return text block
   */
  public String getTextBlock(String blockId) {
    Cursor cursor = database.query(MySQLiteHelper.TABLE_TEXT_BLOCKS, contentColumn,
        MySQLiteHelper.COLUMN_BLOCK_ID + " = " + "'" + blockId + "'", null, null, null, null);
    if (cursor.getCount() > 0) {
      cursor.moveToFirst();
      String s = cursor.getString(0);
      cursor.close();
      return s;
    } else {
      cursor.close();
      return null;
    }
  }

  /**
   * Creates TextBlocks table.
   * 
   * @param blockId
   * @param blockContent
   * @return true, if successful
   */
  public boolean addTextBlock(String blockId, String blockContent) {
    int r = 0;
    Cursor cursor = database.query(MySQLiteHelper.TABLE_TEXT_BLOCKS, idColumns,
        MySQLiteHelper.COLUMN_BLOCK_ID + " = " + "'" + blockId + "'", null, null, null, null);

    ContentValues values = new ContentValues();

    if (cursor.getCount() > 0) {
      values.put(MySQLiteHelper.COLUMN_BLOCK_TIME, mDateFormat.format(System.currentTimeMillis()));
      values.put(MySQLiteHelper.COLUMN_BLOCK_ID, blockId);
      values.put(MySQLiteHelper.COLUMN_BLOCK_CONTENT, blockContent);
      r = database.update(MySQLiteHelper.TABLE_TEXT_BLOCKS, values, MySQLiteHelper.COLUMN_BLOCK_ID + " = " + "'" + blockId + "'", null);
    } else {
      values.put(MySQLiteHelper.COLUMN_BLOCK_ID, blockId);
      values.put(MySQLiteHelper.COLUMN_BLOCK_CONTENT, blockContent);
      r = (int) database.insert(MySQLiteHelper.TABLE_TEXT_BLOCKS, null, values);
    }
    cursor.close();
    
    if (r == 0) {
      return false;
    } else {
      return true;
    }
  }

  /**
   * Delete row in TextBlocks table.
   * 
   * @param blockId
   */
  public void deleteTextBlock(String blockId) {
    database.delete(MySQLiteHelper.TABLE_TEXT_BLOCKS, MySQLiteHelper.COLUMN_BLOCK_ID + " = " + "'" + blockId + "'", null);
  }

  /**
   * Return all text blocks from TextBlocks table.
   * 
   * @return all text blocks
   */
  public List<TextBlocks> getAllTextBlocks() {
    List<TextBlocks> tbs = new ArrayList<TextBlocks>();

    Cursor cursor = database.query(MySQLiteHelper.TABLE_TEXT_BLOCKS, allColumns, null, null, null, null, null);

    cursor.moveToFirst();
    while (!cursor.isAfterLast()) {
      TextBlocks tb = cursorToTextBlock(cursor);
      tbs.add(tb);
      cursor.moveToNext();
    }
    cursor.close();
    return tbs;
  }

  /**
   * Cursor in TextBlocks table. Helper method.
   * 
   * @param cursor
   * @return text blocks
   */
  private TextBlocks cursorToTextBlock(Cursor cursor) {
    TextBlocks tb = new TextBlocks();
    tb.setId(cursor.getInt(0));
    tb.setBlockId(cursor.getString(3));
    tb.setBlockContent(cursor.getString(4));
    return tb;
  }
}