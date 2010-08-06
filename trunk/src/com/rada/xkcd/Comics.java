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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.http.util.ByteArrayBuffer;

import android.content.Context;
import android.database.Cursor;
import android.widget.Toast;

public final class Comics {
  
  public static final Random RANDOM= new Random(Calendar.getInstance().getTimeInMillis());
  
  public static final Executor BACKGROUND_EXECUTOR= Executors.newCachedThreadPool();
  
  public static final String ACTION_VIEW= "com.rada.xkcd.action.VIEW";
  public static final String ACTION_DOWNLOAD= "com.rada.xkcd.action.DOWNLOAD";
  public static final String ACTION_CLEAR= "com.rada.xkcd.action.CLEAR";
  public static final String ACTION_VIEW_FAVORITES= "com.rada.xkcd.action.VIEW_FAVORITES";
  
  public static final String KEY_NUMBER= "_id";
  public static final String KEY_TITLE= "title";
  public static final String KEY_TEXT= "hover";
  public static final String KEY_URL= "url";
  public static final String KEY_FAVORITE= "favorite";
  public static final String[] ALL_COLUMNS= new String[] { KEY_NUMBER, KEY_TITLE, KEY_TEXT, KEY_URL, KEY_FAVORITE };
  
  public static final int STATUS_SUCCESS= 0;
  public static final int STATUS_FAILURE= 1;
  public static final int STATUS_ERROR= 2;
  public static final int STATUS_CANCELLED= 3;
  
  public static final int MESSAGE_DOWNLOAD= 500;
  public static final int MESSAGE_CLEAR= 501;
  
  public static final int BUFFER_SIZE= 10000;
  
  public static final String SD_DIR_PATH= "/sdcard/xkcd/";
  public static final File SD_DIR= new File(SD_DIR_PATH);
  
  public static final String MAIN_URL= "http://www.xkcd.com/";
  public static final String ARCHIVE_URL= MAIN_URL + "archive/index.html";
  
  public static class ToastRunnable implements Runnable {
    Toast toast;
    
    ToastRunnable(Context context, CharSequence text, int duration) {
      toast= Toast.makeText(context, text, duration);
    }
    
    public void run() {
      toast.show();
    }
  }
  
  public static final InputStream download(URL url) throws IOException {
    HttpURLConnection conn= (HttpURLConnection) url.openConnection();
    conn.setDoInput(true);
    return conn.getInputStream();
  }
  
  public static final InputStream download(String url) throws MalformedURLException, IOException {
    return download(new URL(url));
  }

  public static final void downloadComic(long comicNumber, ComicDbAdapter dbAdapter) throws MalformedURLException, IOException, Exception {
    if (!Comics.SD_DIR.exists())
      Comics.SD_DIR.mkdirs();
    
    File file= new File(SD_DIR_PATH + comicNumber);
    if (file.length() <= 0) {
      Cursor cursor= dbAdapter.fetchComic(comicNumber);
      String url= cursor.getString(cursor.getColumnIndexOrThrow(KEY_URL));

      if (url == null || url.length() == 0) {
        dbAdapter.updateComic(comicNumber);
        cursor= dbAdapter.fetchComic(comicNumber);
        url= cursor.getString(cursor.getColumnIndexOrThrow(KEY_URL));
      }
      cursor.close();

      BufferedInputStream bufferedInStream= new BufferedInputStream(download(url), BUFFER_SIZE);
      ByteArrayBuffer buffer= new ByteArrayBuffer(50);
      int current = 0;
      while ((current = bufferedInStream.read()) != -1) {
        buffer.append((byte) current);
      }

      FileOutputStream ostream= new FileOutputStream(file);
      BufferedOutputStream bo= new BufferedOutputStream(ostream, BUFFER_SIZE);
      bo.write(buffer.toByteArray());
      bo.close();
      ostream.close();
    }
  }
}
