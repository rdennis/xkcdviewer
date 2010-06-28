/*
 * Copyright (C) 2010 Alex Avance
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
package com.rada.xkcdviewer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Simple comic database access helper class. This implements a set of
 * functions for accessing the elements of the database as well as
 * inserting new elements. There are no included functions to modify or
 * delete elements as there should never be any reason to do so.
 * 
 * It is based on the NotesDbAdapter from Google's Notepad tutorial for
 * Android.
 */
public class ComicsDbAdapter {
	
	public static final String KEY_NUMBER= "number";
	public static final String KEY_TITLE= "title";
	public static final String KEY_TEXT= "hover";
	public static final String KEY_PICTURE= "picture";
	public static final String KEY_URL= "url";
	public static final String KEY_ROWID= "_id";
	
	private static final String TAG= "ComicsDbAdapter";
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	
	private static final String DATABASE_NAME= "data";
	private static final String DATABASE_TABLE= "comics";
	private static final int DATABASE_VERSION= 2;
	
	private final Context mCtx;
	
	/**
	 * Database creation sql statement
	 */
	private static final String DATABASE_CREATE=
		"create table " + DATABASE_TABLE + "(" + KEY_ROWID + " integer primary key autoincrement, "
		+ KEY_NUMBER + " integer, " + KEY_TITLE + " text not null, "
		+ KEY_TEXT + " text not null, " + KEY_PICTURE + " text not null, "
		+ KEY_URL + " text not null);";
	
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
            db.execSQL("DROP TABLE IF EXISTS comics");
            onCreate(db);
		}
	}
	
	/**
	 * Constructor - takes the context for the opening/creation of the database
	 * 
	 * @param ctx the Context in which to work
	 */
	public ComicsDbAdapter(Context ctx) {
		this.mCtx= ctx;
	}
	
	/**
     * Open the comics database. If it cannot open, create a new instance. If
     * an instance cannot be created, throw an exception.
     * 
     * @return this (allow call chaining)
     * @throws SQLException if the database could be neither opened or created
     */
	public ComicsDbAdapter open() throws SQLException {
		mDbHelper= new DatabaseHelper(mCtx);
		mDb= mDbHelper.getWritableDatabase();
		return this;
	}
	/**
	 * Close the comics database.
	 */
	public void close() {
		mDbHelper.close();
	}
	
	/**
     * Add a comic to the list using the given number, title, hover text,
     * picture file, and url.
     * 
     * @param number the number of the comic
     * @param title the title of the comic
     * @param text the hover text for the comic
     * @param picture the file for the given comic's picture
     * @param url the url to download the given picture
     * @return rowId or -1 if failed
     */
    public long insertComic(long number, String title, String text, String picture, String url) {
        ContentValues initialValues= new ContentValues();
        initialValues.put(KEY_NUMBER, number);
        initialValues.put(KEY_TITLE, title);
        initialValues.put(KEY_TEXT, text);
        initialValues.put(KEY_PICTURE, picture);
        initialValues.put(KEY_URL, url);

        return mDb.insert(DATABASE_TABLE, null, initialValues);
    }
    
    /**
     * Return a Cursor over the list of all comics in the database
     * 
     * @return Cursor over all comics
     */
    public Cursor fetchAllComics() {
        return mDb.query(DATABASE_TABLE,
                new String[] {KEY_ROWID, KEY_NUMBER, KEY_TITLE, KEY_TEXT, KEY_PICTURE, KEY_URL},
                null, null, null, null, null);
    }
    
    /**
     * Return a Cursor to the row with rowId as the _id field.
     * 
     * @param rowId id to find
     * @return Cursor to row with rowId
     * @throws SQLException if the given rowId is not found
     */
    public Cursor fetchComic(long rowId) throws SQLException {
        return mDb.query(true, DATABASE_TABLE,
                new String[] {KEY_ROWID, KEY_NUMBER, KEY_TITLE, KEY_TITLE, KEY_PICTURE, KEY_URL},
                KEY_ROWID + "=" + rowId, null, null, null, null, null);
    }
    
    /**
     * Return a Cursor to the row with number as the number field
     * 
     * @param number number to find in the table
     * @return Cursor to row with number
     * @throws SQLException if the given number is not found
     */
    public Cursor fetchComicByNumber(long number) throws SQLException {
        return mDb.query(true, DATABASE_TABLE,
                new String[] {KEY_ROWID, KEY_NUMBER, KEY_TITLE, KEY_TITLE, KEY_PICTURE, KEY_URL},
                KEY_NUMBER + "=" + number, null, null, null, null, null);
    }
    
    /**
     * Return the value of the field labeled column in the entry with rowId
     * 
     * @param rowId id to find
     * @param column field to return
     * @return value of the field in the entry
     * @throws SQLException if the rowId is not found in the table
     * @throws IllegalArgumentException if the column is not found in the entry
     */
    private String getColumn(long rowId, String column) throws SQLException, IllegalArgumentException {
        Cursor c= fetchComic(rowId);
        return c.getString(c.getColumnIndexOrThrow(column));
    }
    
    /**
     * Return the value of the field labeled column in the entry with number
     * 
     * @param number number to find
     * @param column field to return
     * @return value of the field in the entry
     * @throws SQLException if the number is not found in the table
     * @throws IllegalArgumentException if the column is not found in the entry
     */
    private String getColumnByNumber(long number, String column) throws SQLException, IllegalArgumentException {
        Cursor c= fetchComicByNumber(number);
        return c.getString(c.getColumnIndexOrThrow(column));
    }
    
    /**
     * Return the picture filename of the entry with rowId
     * 
     * @param rowId id to find
     * @return picture filename of the comic
     * @throws SQLException if the rowId is not found in the table
     * @throws IllegalArgumentException if the column KEY_PICTURE is not found in the entry
     */
    public String getPictureFileName(long rowId) throws SQLException, IllegalArgumentException {
        return getColumn(rowId, KEY_PICTURE);
    }
    
    /**
     * Return the picture filename of the entry with number
     * 
     * @param number number to find
     * @return picture filename of the comic
     * @throws SQLException if the number is not found in the table
     * @throws IllegalArgumentException
     */
    public String getPictureFileNameByNumber(long number) throws SQLException, IllegalArgumentException {
        return getColumnByNumber(number, KEY_PICTURE);
    }
    
    /**
     * Return the hover text of the entry with rowId
     * 
     * @param rowId id to find
     * @return hover text of the comic
     * @throws SQLException if the rowId is not found in the table
     * @throws IllegalArgumentException if the column KEY_TEXT is nto found in the entry
     */
    public String getHoverText(long rowId) throws SQLException, IllegalArgumentException {
        return getColumn(rowId, KEY_TEXT);
    }

    /**
     * Return the hover text of the entry with number
     * 
     * @param number number to find
     * @return hover text of the comic
     * @throws SQLException if the number is not found in the table
     * @throws IllegalArgumentException if the column KEY_TEXT is not found in the entry
     */
    public String getHoverTextByNumber(long number) throws SQLException, IllegalArgumentException {
        return getColumnByNumber(number, KEY_TEXT);
    }
    
    /**
     * Return the url of the picture file of the comic
     * 
     * @param rowId id to find
     * @return url of the comic picture
     * @throws SQLException if the rowId is not found in the table
     * @throws IllegalArgumentException if the column KEY_URL is not found in the entry
     */
    public String getURL(long rowId) throws SQLException, IllegalArgumentException {
        return getColumn(rowId, KEY_URL);
    }

    /**
     * Return the url of the picture file of the comic
     * 
     * @param number number to find
     * @return url of the comic picture
     * @throws SQLException if the number is not found in the table
     * @throws IllegalArgumentException if the column KEY_URL is not found in the entry
     */
    public String getURLByNumber(long number) throws SQLException, IllegalArgumentException {
        return getColumnByNumber(number, KEY_URL);
    }
}
