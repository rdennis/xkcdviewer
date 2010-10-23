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
package com.radadev.xkcd.compat;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import org.apache.http.util.ByteArrayBuffer;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;

import com.radadev.xkcd.compat.database.ComicDbAdapter;

public final class Comics {

  // 10000 is an arbitrary number just so it isn't 0
  private static final int MSG_BASE= 10000;
  
  public static final int MSG_DONE= MSG_BASE + 0;
  public static final int MSG_SUCCESS= MSG_BASE + 1;
  public static final int MSG_FAILURE= MSG_BASE + 2;
  public static final int MSG_CANCEL= MSG_BASE + 3;
  public static final int MSG_UPDATE= MSG_BASE + 4;
  
  public static final int MSG_DOWNLOAD= MSG_BASE + 5;
  public static final int MSG_DOWNLOAD_ALL= MSG_BASE + 6;
  public static final int MSG_CLEAR= MSG_BASE + 7;
  public static final int MSG_CLEAR_ALL= MSG_BASE + 8;

  public static final int MSG_REGISTER_CLIENT= MSG_BASE + 9;
  public static final int MSG_UNREGISTER_CLIENT= MSG_BASE + 10;
  
  public static final String SQL_KEY_NUMBER= "_id";
  public static final String SQL_KEY_TITLE= "title";
  public static final String SQL_KEY_TEXT= "hover";
  public static final String SQL_KEY_IMAGE= "url";
  public static final String SQL_KEY_FAVORITE= "favorite";
  public static final String[] SQL_ALL_COLUMNS= { SQL_KEY_NUMBER, SQL_KEY_TITLE, SQL_KEY_TEXT, SQL_KEY_IMAGE, SQL_KEY_FAVORITE };
  
  public static final String ACTION_VIEW= "com.radadev.xkcd.compat.View";
  public static final String ACTION_VIEW_FAVORITES= "com.radadev.xkcd.compat.ViewFavorites";
  
  public static final String KEY_LAST_UPDATE= "com.radadev.xkcd.compat.LastUpdate";
  public static final String KEY_NUMBER= "com.radadev.xkcd.compat.Number";
  
  public static final String URL_MAIN= "http://m.xkcd.com/";
  public static final String URL_ARCHIVE= URL_MAIN + "archive/index.html";

  public static final int MAX_THREAD_COUNT= 25;
  public static final int BUFFER_SIZE= 10000;

  public static Random RANDOM= new Random(Calendar.getInstance().getTimeInMillis());
  
  public static final InputStream download(URL url) throws IOException {
    HttpURLConnection conn= (HttpURLConnection) url.openConnection();
    conn.setDoInput(true);
    return conn.getInputStream();
  }
  
  public static final InputStream download(String url) throws MalformedURLException, IOException {
    return download(new URL(url));
  }

  public static void downloadComic(Integer comicNumber, Context context) throws FileNotFoundException, IOException {
    ComicDbAdapter dbAdapter= new ComicDbAdapter(context);
    dbAdapter.open();
    try {
      dbAdapter.updateComic(comicNumber);
      File file= new File(getSdDir(context), comicNumber.toString());
      if (file.length() <= 0) {
        Cursor cursor= dbAdapter.fetchComic(comicNumber);
        String url= cursor.getString(cursor.getColumnIndexOrThrow(Comics.SQL_KEY_IMAGE));

        if (url == null || url.length() == 0) {
          dbAdapter.updateComic(comicNumber);
          cursor.close();
          cursor= dbAdapter.fetchComic(comicNumber);
          url= cursor.getString(cursor.getColumnIndexOrThrow(Comics.SQL_KEY_IMAGE));
        }
        cursor.close();

        InputStream download= Comics.download(url);
        BufferedInputStream inputStream= new BufferedInputStream(download, Comics.BUFFER_SIZE);
        ByteArrayBuffer buffer= new ByteArrayBuffer(50);
        int current;
        while ((current= inputStream.read()) != -1) {
          buffer.append(current);
        }
        FileOutputStream fileStream= new FileOutputStream(file);
        fileStream.write(buffer.toByteArray());
        download.close();
        inputStream.close();
        fileStream.close();
      }
    } finally {
      dbAdapter.close();
    }
  }
  public static final Date getLastUpdate(Context context) {
    SharedPreferences info= context.getSharedPreferences("info", Context.MODE_WORLD_WRITEABLE);
    String dateString= info.getString(KEY_LAST_UPDATE, null);
    return (dateString != null) ? new Date(dateString) : null;
  }
  
  public static final boolean setLastUpdate(Context context, Date date) {
    Editor editor= context.getSharedPreferences("info", Context.MODE_WORLD_READABLE).edit();
    editor.putString(KEY_LAST_UPDATE, date.toGMTString());
    return editor.commit();
  }
  
  public static final File getSdDir(Context context) {
    final String path= context.getFilesDir().getAbsolutePath().replaceFirst("/data", "/sdcard/Android");
    File result= new File(path);
    result.mkdirs();
    return result;
  }
}
