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
package com.radadev.xkcd.database;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.regex.Pattern;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.util.Pair;

import com.radadev.xkcd.Comics;
import com.radadev.xkcd.scraper.ComicScraper;

public class ComicDbAdapter {

  private static final String KEY_NUMBER= Comics.SQL_KEY_NUMBER;
  private static final String KEY_TITLE= Comics.SQL_KEY_TITLE;
  private static final String KEY_TEXT= Comics.SQL_KEY_TEXT;
  private static final String KEY_IMAGE= Comics.SQL_KEY_IMAGE;
  private static final String KEY_FAVORITE= Comics.SQL_KEY_FAVORITE;
  private static final String KEY_MAXNUMBER= "MAX(" + KEY_NUMBER + ")";
  private static final String[] ALL_COLUMNS= Comics.SQL_ALL_COLUMNS;

  private DatabaseHelper dbHelper;
  private SQLiteDatabase database;

  private static final String TAG= "ComicDbAdapter";

  private static final String DATABASE_NAME= "comics.db";
  private static final String DATABASE_TABLE= "comics";
  private static final int DATABASE_VERSION= 2;

  private static final String DATABASE_CREATE= 
    "create table " + DATABASE_TABLE + " ( " + 
    KEY_NUMBER + " integer primary key, " + 
    KEY_TITLE + " text not null, " +
    KEY_TEXT + " text, " +
    KEY_IMAGE + " text, " +
    KEY_FAVORITE + " boolean );";

  private final Context mContext;

  /**
   * Constructor - setup the local context and copy the database if necessary
   * 
   * @param context context within which to work
   */
  public ComicDbAdapter(Context context) {
    mContext= context;
    boolean exists= checkDatabase(DATABASE_NAME);
    
    if (!exists)
      try {
        copyDatabase(DATABASE_NAME);
      } catch (IOException e) {
        // the copy failed... not much else to do except let dbAdapter handle it
      }
  }
  
  /**
   * Check if a given database exists in the applications database folder.
   * 
   * @param database the name of the database to check
   * @return true if it already exists, false otherwise
   */
  private boolean checkDatabase(String database) {
    File file= mContext.getDatabasePath(database).getAbsoluteFile();
    return file.exists();
  }
  
  /**
   * Copy a database from the applications assets to its database folder.
   * It preserves the same name across the copy.
   * 
   * @param database the name of the database to copy
   * @throws IOException if it fails to create the directory, file, or in the copy
   */
  private void copyDatabase(String database) throws IOException {
    
    InputStream myInput = mContext.getAssets().open(database);

    String path = mContext.getDatabasePath(database).getAbsolutePath();

    File dir= new File(path.substring(0, path.lastIndexOf(File.separatorChar)));
    File file= new File(path);
    
    if (!dir.exists())
      dir.mkdirs();
    
    if (!file.exists())
      file.createNewFile();

    //Open the empty db as the output stream
    OutputStream myOutput = new FileOutputStream(file);

    //transfer bytes from the inputfile to the outputfile
    byte[] buffer = new byte[1024];
    int length;
    while ((length = myInput.read(buffer))>0){
      myOutput.write(buffer, 0, length);
    }

    //Close the streams
    myOutput.flush();
    myOutput.close();
    myInput.close();
  }

  /**
   * Open up the database for working with it.
   * 
   * @return this for chaining calls
   * @throws SQLException if the opening/creation of the database fails
   */
  public ComicDbAdapter open() throws SQLException {
    dbHelper= new DatabaseHelper(mContext);
    database= dbHelper.getWritableDatabase();
    return this;
  }

  /**
   * Close the database
   */
  public void close() {
    dbHelper.close();
  }

  /**
   * Insert an entry into the database for a comic.
   * 
   * @param number the number of the comic to insert
   * @param title the title of the comic to insert
   * @return number of the newly inserted comic
   */
  public int insertComic(int number, String title) {
    ContentValues values= new ContentValues();
    values.put(KEY_NUMBER, number);
    values.put(KEY_TITLE, title);
    values.put(KEY_FAVORITE, false);

    return (int) database.insert(DATABASE_TABLE, null, values);
  }
  
  /**
   * Insert a list of comics into the database.
   *
   * @param comics the list of comics to insert
   */
  public void insertComics(Map<Integer, String> comics) {
    Cursor cursor= fetchMostRecentComic();
    try {
      int max= (cursor == null) ? 0 : cursor.getInt(cursor.getColumnIndexOrThrow(KEY_NUMBER));
      database.beginTransaction();
      for (Integer key : comics.keySet()) {
        if (key > max) {
          String value= comics.get(key);
          insertComic(key, value);
        }
      }
      database.setTransactionSuccessful();
      database.endTransaction();
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
  }

  /**
   * Return a cursor over all the comics in the database. The comics are in
   * descending order by number.
   * 
   * @return Cursor over all the comics
   */
  public Cursor fetchAllComics() {
    return database.query(DATABASE_TABLE,
        ALL_COLUMNS,
        null, null, null, null,
        KEY_NUMBER + " DESC");
  }
  
  /**
   * Return a cursor over the favorited comics. The comics are in
   * descending order by number.
   * 
   * @return Cursor over all the comics
   */
  public Cursor fetchFavoriteComics() {
    return database.query(DATABASE_TABLE,
        ALL_COLUMNS,
        KEY_FAVORITE + "=1",
        null, null, null,
        KEY_NUMBER + " DESC");
  }
  
  /**
   * Return a cursor over all comics that match the given query.
   * The comics are in descending order by number.
   * 
   * @param query the search query to run against
   * @return
   */
  public Cursor fetchSearchedComics(String query) {
    String[] split= query.split("[\\s,./(\")|{}\\[\\];\\\\~!@#$%^*()`\\-\\+_=]+");
    StringBuilder builder= new StringBuilder();
    
    for (int i= 0; i < split.length; ++i) {
      if (i > 0) {
        builder.append(" OR ");
      }
      builder.append(KEY_TITLE + " LIKE '%" + split[0] + "%' OR " +
          KEY_TEXT + " LIKE '%" + split[0] + "%'");
      if (Pattern.matches("^\\d{1,4}$", split[i])) {
        builder.append(" OR " + KEY_NUMBER + "=" + split[i]);
      }
    }
    
    String selection= builder.toString();
    
    return database.query(DATABASE_TABLE,
        ALL_COLUMNS,
        selection,
        null, null, null,
        KEY_NUMBER + " DESC");
  }

  /**
   * Return a cursor to the comic indicated by number.
   * 
   * @param number number of the comic to return
   * @return Cursor pointing to given comic
   * @throws SQLException if the comic could not be found
   */
  public Cursor fetchComic(int number) throws SQLException {
    Cursor cursor= 
      database.query(true, DATABASE_TABLE, ALL_COLUMNS,
                KEY_NUMBER + "=" + number,
                null, null, null, null, null);

    if (cursor != null)
      cursor.moveToFirst();

    return cursor;
  }

  /**
   * Return a cursor to the most recent comic.
   * 
   * @return Cursor to the most recent comic
   * @throws SQLException if the comic could not be found
   */
  public Cursor fetchMostRecentComic() {
    Cursor cursor= null;
    try {
      String[] columnList= ALL_COLUMNS.clone();
      columnList[0]= KEY_MAXNUMBER;
      
      cursor=
        database.query(true, DATABASE_TABLE, columnList,
                  null, null, null, null, null, null);
      if (cursor != null) {
        cursor.moveToFirst();
      }
      int number= cursor.getInt(cursor.getColumnIndexOrThrow(KEY_MAXNUMBER));
  
      // odd, I know, this is used to normalize the column names
      return (number > 0) ? fetchComic(number) : null;
    } catch (SQLException e) {
      throw e;
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
  }
  
  public int getMostRecentComicNumber() {
    Cursor cursor= fetchMostRecentComic();
    int result= cursor.getInt(cursor.getColumnIndexOrThrow(KEY_NUMBER));
    cursor.close();
    return result;
  }

  /**
   * Update the comic referred to by number to contain the text and url given.
   * 
   * @param number number of the comic to update
   * @param text hover text to add to the comic
   * @param image filename of the image
   * @return true if successfully updated, false otherwise
   */
  public boolean updateComic(int number, String text, String image) {
    ContentValues values= new ContentValues();
    values.put(KEY_TEXT, text);
    values.put(KEY_IMAGE, image);

    return database.update(DATABASE_TABLE, values, KEY_NUMBER + "=" + number, null) > 0;
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
  public boolean updateComic(int number, String text, String url, boolean favorite) {
    ContentValues values= new ContentValues();
    values.put(KEY_TEXT, text);
    values.put(KEY_IMAGE, url);
    values.put(KEY_FAVORITE, favorite);

    return database.update(DATABASE_TABLE, values, KEY_NUMBER + "=" + number, null) > 0;
  }

  /**
   * Update the comic referred to by number to contain the favorite status given.
   * 
   * @param number number of the comic to update
   * @param favorite favorite status of the comic
   * @return true if successfully updated, false otherwise
   */
  public boolean updateComic(int number, boolean favorite) {
    ContentValues values= new ContentValues();
    values.put(KEY_FAVORITE, favorite);

    return database.update(DATABASE_TABLE, values, KEY_NUMBER + "=" + number, null) > 0;
  }
  
  /**
   * This connects to xkcd.com to update the information about the given comic.
   * 
   * @param number number of the comic to update
   * @return true if successfully updated, false otherwise
   * @throws MalformedURLException if the url is incorrectly formed... this means you screwed up
   * @throws IOException if the connection fails
   */
  public boolean updateComic(int number) throws IOException {
    Pair<String, String> info= ComicScraper.getComicInformation(number);
    String url= info.first;
    String text= info.second;
    return updateComic(number, text, url);
  }
  
  /**
   * Return whether or not the given comic is a favorite.
   * 
   * @param number number of the comic to reference
   * @return true if it is a favorite, false otherwise
   */
  public boolean isFavorite(int number) {
    Cursor cursor= null;
    try {
      cursor= 
        database.query(true, DATABASE_TABLE, ALL_COLUMNS,
            KEY_NUMBER + "=" + number,
            null, null, null, null, null);
      if (cursor != null) {
        cursor.moveToFirst();
        int columnIndex= cursor.getColumnIndexOrThrow(KEY_FAVORITE);
        boolean result= cursor.getInt(columnIndex) != 0;
        return result;
      }
      return false;
    } finally {
      cursor.close();
    }
  }

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
}