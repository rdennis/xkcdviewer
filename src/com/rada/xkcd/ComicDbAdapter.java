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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.Html;
import android.util.Log;

public class ComicDbAdapter {

  public static final String KEY_NUMBER= "_id";
  public static final String KEY_TITLE= "title";
  public static final String KEY_TEXT= "hover";
  public static final String KEY_URL= "url";
  public static final String KEY_FAVORITE= "favorite";
  public static final String KEY_MAXNUMBER= "MAX(" + KEY_NUMBER + ")";
  private static final String[] ALL_COLUMNS= new String[] { KEY_NUMBER, KEY_TITLE, KEY_TEXT, KEY_URL, KEY_FAVORITE };
  
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
    KEY_URL + " text, " +
    KEY_FAVORITE + " boolean );";

  private final Context mContext;

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
        // TODO Auto-generated catch block
        e.printStackTrace();
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
  public long insertComic(long number, String title) {
    ContentValues values= new ContentValues();
    values.put(KEY_NUMBER, number);
    values.put(KEY_TITLE, title);
    values.put(KEY_FAVORITE, false);

    return database.insert(DATABASE_TABLE, null, values);
  }

  /**
   * Return a cursor over all the comics in the database. The comics are in
   * descending order by number.
   * 
   * @return Cursor over all the notes
   */
  public Cursor fetchAllComics() {
    return database.query(DATABASE_TABLE,
        ALL_COLUMNS,
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
      database.query(true, DATABASE_TABLE, ALL_COLUMNS,
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
  public Cursor fetchMostRecentComic() {
    try {
      String[] columnList= ALL_COLUMNS.clone();
      columnList[0]= KEY_MAXNUMBER;
      
      Cursor mCursor=
        database.query(true, DATABASE_TABLE, columnList,
                  null, null, null, null, null, null);
      if (mCursor != null)
        mCursor.moveToFirst();
  
      return mCursor;
    } catch (SQLException e) {
      throw e;
    }
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
  public boolean updateComic(long number, String text, String url, boolean favorite) {
    ContentValues values= new ContentValues();
    values.put(KEY_TEXT, text);
    values.put(KEY_URL, url);
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
  public boolean updateComic(long number, boolean favorite) {
    ContentValues values= new ContentValues();
    values.put(KEY_NUMBER, favorite);

    return database.update(DATABASE_TABLE, values, KEY_NUMBER + "=" + number, null) > 0;
  }
  
  /**
   * Return whether or not the given comic is a favorite.
   * 
   * @param number number of the comic to reference
   * @return true if it is a favorite, false otherwise
   */
  public boolean isFavorite(long number) {
    Cursor mCursor= 
      database.query(true, DATABASE_TABLE, ALL_COLUMNS,
                KEY_NUMBER + "=" + number,
                null, null, null, null, null);
    boolean result= mCursor.getInt(mCursor.getColumnIndexOrThrow(KEY_FAVORITE)) != 0;
    mCursor.close();
    return result;
  }
  
  public synchronized void updateList() throws MalformedURLException, IOException {
    URL url= new URL("http://www.xkcd.com/archive/index.html");
    HttpURLConnection conn= (HttpURLConnection) url.openConnection();
    conn.setDoInput(true);
    BufferedInputStream bi= new BufferedInputStream(conn.getInputStream());
    DataInputStream archive= new DataInputStream(bi);

    String line;
    for (int i= 0; i < 67 && archive.available() > 0; ++i) {
      line= archive.readLine();
    }

    database.beginTransaction();
    Cursor mostRecentCursor= fetchMostRecentComic();
    long newest= (mostRecentCursor != null) ?
        mostRecentCursor.getLong(mostRecentCursor.getColumnIndexOrThrow(ComicDbAdapter.KEY_MAXNUMBER)) + 1 : 1;
    long number= Long.MAX_VALUE;
    String title;
    Pattern numberPattern= Pattern.compile("(?<=href=\"/).*?(?=/\")"),
    titlePattern= Pattern.compile("(?<=>).*?(?=<)");
    Matcher m;
    while (number > newest && archive.available() > 0) {
      line= archive.readLine();
      if (line.startsWith("<a")) {
        m= numberPattern.matcher(line);
        if (m.find()) {
          number= Long.parseLong(m.group());
          m= titlePattern.matcher(line);
          if (m.find()) {
            title= Html.fromHtml(m.group()).toString();
            insertComic(number, title);
            for (int i= 0; i < 3; ++i)
              archive.readLine();
          }
        }
      }
    }
    database.setTransactionSuccessful();
    database.endTransaction();
  }
}