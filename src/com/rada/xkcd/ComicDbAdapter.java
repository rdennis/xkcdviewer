/*
 * Copyright (C) 2010  Alex Avance
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-
 * 1307, USA.
 */
package com.rada.xkcd;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class ComicDbAdapter {

  public static final String KEY_NUMBER= "_id";
  public static final String KEY_TITLE= "title";
  public static final String KEY_TEXT= "text";
  public static final String KEY_URL= "url";
  public static final String KEY_FAVORITE= "favorite";

  private DatabaseHelper mDbHelper;
  private SQLiteDatabase mDb;

  private static final String TAG= "ComicDbAdapter";

  private static final String DATABASE_NAME= "data";
  private static final String DATABASE_TABLE= "comics";
  private static final int DATABASE_VERSION= 2;

  private static final String DATABASE_CREATE= 
    "create table" + DATABASE_TABLE + "(" + 
    KEY_NUMBER + " integer primary key," + 
    KEY_TITLE + " text not null," +
    KEY_TEXT + "text," +
    KEY_URL + "text" + 
    KEY_FAVORITE + "boolean);";

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
      db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
      onCreate(db);
    }
  }

  /**
   * Constructor - setup up the local context
   * 
   * @param ctx context within which to work
   */
  public ComicDbAdapter(Context ctx) {
    mCtx= ctx;
  }

  /**
   * Open up the database for working with it.
   * 
   * @return this for chaining calls
   * @throws SQLException if the opening/creation of the database fails
   */
  public ComicDbAdapter open() throws SQLException {
    mDbHelper= new DatabaseHelper(mCtx);
    mDb= mDbHelper.getWritableDatabase();
    return this;
  }

  /**
   * Close the database
   */
  public void close() {
    mDbHelper.close();
  }

  /**
   * Insert an entry into the database for a comic.
   * 
   * @param number the number of the comic to insert
   * @param title the title of the comic to insert
   * @return number of the newly inserted comic
   */
  public long insertComic(long number, String title) {
    ContentValues values= new ContentValues();
    values.put(KEY_NUMBER, number);
    values.put(KEY_TITLE, title);

    return mDb.insert(DATABASE_TABLE, null, values);
  }

  /**
   * Return a cursor over all the comics in the database. The comics are in
   * descending order by number.
   * 
   * @return Cursor over all the notes
   */
  public Cursor fetchAllComics() {
    return mDb.query(DATABASE_TABLE,
        new String[] { KEY_NUMBER, KEY_TITLE, KEY_TEXT, KEY_URL, KEY_FAVORITE },
        null, null, null, null,
        KEY_NUMBER + " DESC");
  }

  /**
   * Return a cursor to the comic indicated by number.
   * 
   * @param number number of the comic to return
   * @return Cursor pointing to given comic
   * @throws SQLException if the comic could not be found
   */
  public Cursor fetchComic(long number) throws SQLException {
    Cursor mCursor= 
      mDb.query(true, DATABASE_TABLE, 
                new String[] { KEY_NUMBER, KEY_TITLE, KEY_TEXT, KEY_URL, KEY_FAVORITE },
                KEY_NUMBER + "=" + number,
                null, null, null, null, null);

    if (mCursor != null)
      mCursor.moveToFirst();

    return mCursor;
  }

  /**
   * Return a cursor to the most recent comic.
   * 
   * @return Cursor to the most recent comic
   * @throws SQLException if the comic could not be found
   */
  public Cursor fetchMostRecentComic() throws SQLException {
    Cursor mCursor=
      mDb.query(DATABASE_TABLE,
                new String[] { KEY_NUMBER, KEY_TITLE, KEY_TEXT, KEY_URL, KEY_FAVORITE },
                "MAX(" + KEY_NUMBER + ")",
                null, null, null, null);
    if (mCursor != null)
      mCursor.moveToFirst();

    return mCursor;
  }

  /**
   * Update the comic referred to by number to contain the text and url given.
   * 
   * @param number number of the comic to update
   * @param text hover text to add to the comic
   * @param url url of the picture of the comic
   * @return true if successfully updated, false otherwise
   */
  public boolean updateComic(long number, String text, String url) {
    ContentValues values= new ContentValues();
    values.put(KEY_TEXT, text);
    values.put(KEY_URL, url);

    return mDb.update(DATABASE_TABLE, values, KEY_NUMBER + "=" + number, null) > 0;
  }

  /**
   * Update the comic referred to by number to contain the text, url, and
   * favorite status given.
   * 
   * @param number number of the comic to update
   * @param text hover text to add to the comic
   * @param url url of the picture of the comic
   * @param favorite favorite status of the comic
   * @return true if successfully updated, false otherwise
   */
  public boolean updateComic(long number, String text, String url, boolean favorite) {
    ContentValues values= new ContentValues();
    values.put(KEY_TEXT, text);
    values.put(KEY_URL, url);
    values.put(KEY_FAVORITE, favorite);

    return mDb.update(DATABASE_TABLE, values, KEY_NUMBER + "=" + number, null) > 0;
  }

  /**
   * Update the comic referred to by number to contain the favorite status given.
   * 
   * @param number number of the comic to update
   * @param favorite favorite status of the comic
   * @return true if successfully updated, false otherwise
   */
  public boolean updateComic(long number, boolean favorite) {
    ContentValues values= new ContentValues();
    values.put(KEY_NUMBER, favorite);

    return mDb.update(DATABASE_TABLE, values, KEY_NUMBER + "=" + number, null) > 0;
  }
  
  /**
   * Return whether or not the given comic is a favorite.
   * 
   * @param number number of the comic to reference
   * @return true if it is a favorite, false otherwise
   */
  public boolean isFavorite(long number) {
    Cursor mCursor= 
      mDb.query(true, DATABASE_TABLE, 
                new String[] { KEY_NUMBER, KEY_TITLE, KEY_TEXT, KEY_URL },
                KEY_NUMBER + "=" + number,
                null, null, null, null, null);
    boolean result= mCursor.getInt(mCursor.getColumnIndexOrThrow(KEY_FAVORITE)) != 0;
    mCursor.close();
    return result;
  }
}
